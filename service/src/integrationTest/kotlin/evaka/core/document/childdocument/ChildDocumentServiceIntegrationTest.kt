// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.document.childdocument

import evaka.core.FullApplicationTest
import evaka.core.daycare.domain.Language
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.Question
import evaka.core.document.Section
import evaka.core.emailclient.Email
import evaka.core.emailclient.MockEmailClient
import evaka.core.pis.service.insertGuardian
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.DocumentTemplateId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.UserRole
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ChildDocumentServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var service: ChildDocumentService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired lateinit var controller: ChildDocumentController

    final val clock = MockEvakaClock(2022, 1, 1, 15, 0)

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id, language = Language.sv)
    private val adult = DevPerson(email = "joan.doe@example.com")
    private val child = DevPerson()

    final val activeHojksTemplateId = DocumentTemplateId(UUID.randomUUID())
    final val expiredHojksTemplateId = DocumentTemplateId(UUID.randomUUID())
    final val expiredDecisionTemplateId = DocumentTemplateId(UUID.randomUUID())

    final val activeHojksDocumentId = ChildDocumentId(UUID.randomUUID())
    final val expiredHojksDocumentId = ChildDocumentId(UUID.randomUUID())
    final val alreadyCompletedHojksDocumentId = ChildDocumentId(UUID.randomUUID())
    final val expiredDecisionDocumentId = ChildDocumentId(UUID.randomUUID())
    final val templateIdCitizenBasic = DocumentTemplateId(UUID.randomUUID())
    final val templateIdPed = DocumentTemplateId(UUID.randomUUID())

    val templateContent =
        DocumentTemplateContent(
            listOf(
                Section(
                    id = "s1",
                    label = "s1",
                    questions = listOf(Question.TextQuestion(id = "q1", label = "q1")),
                )
            )
        )

    val content =
        DocumentContent(
            answers = listOf(AnsweredQuestion.TextAnswer(questionId = "q1", answer = "answer"))
        )

    val updatedContent =
        DocumentContent(
            answers = listOf(AnsweredQuestion.TextAnswer(questionId = "q1", answer = "updated"))
        )

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.RAW_ROW)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = adult.id, childId = child.id))
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusDays(5),
                )
            )
            tx.insert(
                DevDocumentTemplate(
                    id = activeHojksTemplateId,
                    type = ChildDocumentType.HOJKS,
                    name = "HOJKS uusi",
                    validity = DateRange(clock.today().minusYears(1), clock.today()),
                    content = templateContent,
                )
            )
            tx.insert(
                DevDocumentTemplate(
                    id = expiredHojksTemplateId,
                    type = ChildDocumentType.HOJKS,
                    name = "HOJKS",
                    validity = DateRange(clock.today().minusYears(1), clock.today().minusDays(1)),
                    content = templateContent,
                )
            )
            tx.insert(
                DevDocumentTemplate(
                    id = expiredDecisionTemplateId,
                    type = ChildDocumentType.OTHER_DECISION,
                    name = "Tuen päätös",
                    validity = DateRange(clock.today().minusYears(1), clock.today().minusDays(1)),
                    content = templateContent,
                    endDecisionWhenUnitChanges = true,
                )
            )
        }
    }

    @Test
    fun `expired vasu, leops and hojks documents are completed and published`() {
        val employee = DevEmployee()
        // given
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(
                DevChildDocument(
                    id = activeHojksDocumentId,
                    status = DocumentStatus.DRAFT,
                    childId = child.id,
                    templateId = activeHojksTemplateId,
                    content = content,
                    modifiedAt = clock.now(),
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = clock.now(),
                    contentLockedBy = null,
                    answeredAt = null,
                    answeredBy = null,
                )
            )
            tx.insert(
                DevChildDocument(
                    id = expiredHojksDocumentId,
                    status = DocumentStatus.DRAFT,
                    childId = child.id,
                    templateId = expiredHojksTemplateId,
                    content = content,
                    modifiedAt = clock.now(),
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = clock.now(),
                    contentLockedBy = null,
                    answeredAt = null,
                    answeredBy = null,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt =
                                    HelsinkiDateTime.of(
                                        clock.today().minusMonths(1),
                                        LocalTime.of(8, 0),
                                    ),
                                createdBy = employee.evakaUserId,
                                publishedContent = updatedContent,
                            )
                        ),
                )
            )
            tx.insert(
                DevChildDocument(
                    id = alreadyCompletedHojksDocumentId,
                    status = DocumentStatus.COMPLETED,
                    childId = child.id,
                    templateId = expiredHojksTemplateId,
                    content = content,
                    modifiedAt = clock.now().minusMonths(1),
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = clock.now().minusMonths(1),
                    contentLockedBy = null,
                    answeredAt = null,
                    answeredBy = null,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt = clock.now().minusMonths(1),
                                createdBy = employee.evakaUserId,
                                publishedContent = updatedContent,
                            )
                        ),
                )
            )
            tx.insert(
                DevChildDocument(
                    id = expiredDecisionDocumentId,
                    status = DocumentStatus.DRAFT,
                    childId = child.id,
                    templateId = expiredDecisionTemplateId,
                    content = content,
                    modifiedAt = clock.now(),
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = clock.now(),
                    contentLockedBy = null,
                    answeredAt = null,
                    answeredBy = null,
                )
            )
        }

        val initialTime = clock.now()
        clock.tick()

        // when
        db.transaction { service.completeAndPublishChildDocumentsAtEndOfTerm(it, clock.now()) }
        asyncJobRunner.runPendingJobsSync(clock)

        // then
        db.read { tx ->
            with(tx.getChildDocument(activeHojksDocumentId)!!) {
                assertEquals(DocumentStatus.DRAFT, status)
                assertNull(publishedAt)
            }
            assertEquals(initialTime, tx.getStatusModifiedAt(activeHojksDocumentId))

            with(tx.getChildDocument(expiredHojksDocumentId)!!) {
                assertEquals(DocumentStatus.COMPLETED, status)
                assertEquals(clock.now(), publishedAt)
            }
            assertEquals(clock.now(), tx.getStatusModifiedAt(expiredHojksDocumentId))
            assertNotEquals(initialTime, tx.getStatusModifiedAt(expiredHojksDocumentId))

            with(tx.getChildDocument(alreadyCompletedHojksDocumentId)!!) {
                assertEquals(DocumentStatus.COMPLETED, status)
                assertEquals(initialTime.minusMonths(1), publishedAt)
            }
            assertEquals(
                initialTime.minusMonths(1),
                tx.getStatusModifiedAt(alreadyCompletedHojksDocumentId),
            )

            with(tx.getChildDocument(expiredDecisionDocumentId)!!) {
                assertEquals(DocumentStatus.DRAFT, status)
                assertNull(publishedAt)
            }
            assertEquals(initialTime, tx.getStatusModifiedAt(expiredDecisionDocumentId))
        }
        assertEquals(1, MockEmailClient.emails.size)
        assertEquals(adult.email, MockEmailClient.emails.first().toAddress)
    }

    private fun Database.Read.getStatusModifiedAt(id: ChildDocumentId): HelsinkiDateTime =
        createQuery { sql("SELECT status_modified_at FROM child_document WHERE id = ${bind(id)}") }
            .exactlyOne()

    @Test
    fun `child document notification email is not sent if placement has ended and document won't be visible anyway`() {
        val employee = DevEmployee()
        // given
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(
                DevChildDocument(
                    id = expiredHojksDocumentId,
                    status = DocumentStatus.DRAFT,
                    childId = child.id,
                    templateId = expiredHojksTemplateId,
                    content = content,
                    modifiedAt = clock.now(),
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = clock.now(),
                    contentLockedBy = null,
                    answeredAt = null,
                    answeredBy = null,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt =
                                    HelsinkiDateTime.of(
                                        clock.today().minusMonths(1),
                                        LocalTime.of(8, 0),
                                    ),
                                createdBy = employee.evakaUserId,
                                publishedContent = updatedContent,
                            )
                        ),
                )
            )

            tx.execute {
                sql(
                    """
                UPDATE placement 
                SET start_date = ${bind(clock.today().minusMonths(1))}, 
                    end_date = ${bind(clock.today().minusDays(1))}
            """
                )
            }
        }

        // when
        db.transaction { service.completeAndPublishChildDocumentsAtEndOfTerm(it, clock.now()) }
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(0, MockEmailClient.emails.size)
    }

    @Test
    fun `email is not sent on publish if content was already up to date`() {
        val employee = DevEmployee()
        // given
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(
                DevChildDocument(
                    id = expiredHojksDocumentId,
                    status = DocumentStatus.DRAFT,
                    childId = child.id,
                    templateId = expiredHojksTemplateId,
                    content = content,
                    modifiedAt = clock.now(),
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = clock.now(),
                    contentLockedBy = null,
                    answeredAt = null,
                    answeredBy = null,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt =
                                    HelsinkiDateTime.of(
                                        clock.today().minusMonths(1),
                                        LocalTime.of(8, 0),
                                    ),
                                createdBy = employee.evakaUserId,
                                publishedContent = content,
                            )
                        ),
                )
            )
        }

        // when
        db.transaction { service.completeAndPublishChildDocumentsAtEndOfTerm(it, clock.now()) }
        asyncJobRunner.runPendingJobsSync(clock)

        // then
        db.read { tx ->
            with(tx.getChildDocument(expiredHojksDocumentId)!!) {
                assertEquals(DocumentStatus.COMPLETED, status)
            }
        }
        assertEquals(0, MockEmailClient.emails.size)
    }

    @Test
    fun `citizen basic notification email is sent if document is fillable by guardian`() {
        val testAdult = DevPerson(email = "john.doe@example.com")
        val testChild = DevPerson()
        val employeeUser = DevEmployee(roles = setOf(UserRole.ADMIN))
        db.transaction { tx ->
            tx.insert(
                DevDocumentTemplate(
                    id = templateIdCitizenBasic,
                    type = ChildDocumentType.CITIZEN_BASIC,
                    name = "Perustietolomake",
                    content = templateContent,
                    validity = DateRange(clock.today(), clock.today().plusDays(1)),
                )
            )
            tx.insert(employeeUser)
            tx.insert(testAdult, DevPersonType.ADULT)
            tx.insert(testChild, DevPersonType.CHILD)
            tx.insertGuardian(testAdult.id, testChild.id)
            tx.insert(
                DevPlacement(
                    childId = testChild.id,
                    unitId = daycare.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusDays(5),
                )
            )
        }
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild.id, templateIdCitizenBasic),
            )

        controller.nextDocumentStatus(
            dbInstance(),
            employeeUser.user,
            clock,
            documentId,
            ChildDocumentController.StatusChangeRequest(DocumentStatus.CITIZEN_DRAFT),
        )

        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(1, getCitizenBasicNotificationEmails().size)
        assertEquals(0, getChildDocumentNotificationEmails().size)
    }

    @Test
    fun `child document notification email is sent if document is not fillable by guardian`() {
        val testAdult = DevPerson(email = "john.doe@example.com")
        val testChild = DevPerson()
        val employeeUser = DevEmployee(roles = setOf(UserRole.ADMIN))
        db.transaction { tx ->
            tx.insert(
                DevDocumentTemplate(
                    id = templateIdPed,
                    type = ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
                    name = "Pedagoginen arvio",
                    content = templateContent,
                    validity = DateRange(clock.today(), clock.today().plusDays(1)),
                )
            )
            tx.insert(employeeUser)
            tx.insert(testAdult, DevPersonType.ADULT)
            tx.insert(testChild, DevPersonType.CHILD)
            tx.insertGuardian(testAdult.id, testChild.id)
            tx.insert(
                DevPlacement(
                    childId = testChild.id,
                    unitId = daycare.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusDays(5),
                )
            )
        }

        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild.id, templateIdPed),
            )

        controller.nextDocumentStatus(
            dbInstance(),
            employeeUser.user,
            clock,
            documentId,
            ChildDocumentController.StatusChangeRequest(DocumentStatus.COMPLETED),
        )

        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(0, getCitizenBasicNotificationEmails().size)
        assertEquals(1, getChildDocumentNotificationEmails().size)
    }

    private fun getCitizenBasicNotificationEmails(): List<Email> {
        val emails =
            MockEmailClient.emails.filter {
                it.content.subject ==
                    "Uusi täytettävä asiakirja eVakassa / Nytt ifyllnadsdokument i eVaka / New fillable document in eVaka"
            }
        return emails
    }

    private fun getChildDocumentNotificationEmails(): List<Email> {
        val emails =
            MockEmailClient.emails.filter {
                it.content.subject ==
                    "Uusi asiakirja eVakassa / Nytt dokument i eVaka / New document in eVaka"
            }
        return emails
    }
}
