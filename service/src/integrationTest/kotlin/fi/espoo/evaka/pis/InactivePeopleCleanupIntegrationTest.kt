// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.application.syncApplicationOtherGuardians
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.Section
import fi.espoo.evaka.document.childdocument.AnsweredQuestion
import fi.espoo.evaka.document.childdocument.DocumentContent
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.incomestatement.IncomeStatementBody
import fi.espoo.evaka.messaging.MessageType
import fi.espoo.evaka.messaging.getCitizenMessageAccount
import fi.espoo.evaka.messaging.insertMessage
import fi.espoo.evaka.messaging.insertMessageContent
import fi.espoo.evaka.messaging.insertRecipients
import fi.espoo.evaka.messaging.insertThread
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.pis.service.blockGuardian
import fi.espoo.evaka.pis.service.deleteGuardianRelationship
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChildDocument
import fi.espoo.evaka.shared.dev.DevChildDocumentPublishedVersion
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDocumentTemplate
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevIncomeStatement
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPedagogicalDocument
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InactivePeopleCleanupIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val testDate = LocalDate.of(2020, 3, 1)
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val adult1 = DevPerson()
    private val adult2 = DevPerson()
    private val child1 = DevPerson()
    private val child2 = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insert(area)
            it.insert(daycare)
        }
    }

    @Test
    fun `adult with no family is cleaned up`() {
        db.transaction { tx -> tx.insert(adult1, DevPersonType.RAW_ROW) }

        assertCleanedUpPeople(testDate, setOf(adult1.id))
    }

    @Test
    fun `guardian and their child are cleaned up when neither have archive data`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.RAW_ROW)
            tx.insertGuardian(adult1.id, child1.id)
        }

        assertCleanedUpPeople(testDate, setOf(adult1.id, child1.id))
    }

    @Test
    fun `blocked guardian is not cleaned up without any archived data`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.RAW_ROW)
            tx.blockGuardian(child1.id, adult1.id)
        }

        assertCleanedUpPeople(testDate, setOf(child1.id))
    }

    @Test
    fun `head of family and their child are cleaned up when neither have archive data`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.RAW_ROW)
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
        }

        assertCleanedUpPeople(testDate, setOf(adult1.id, child1.id))
    }

    @Test
    fun `head of family and their partner are cleaned up when neither have archive data`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(adult2, DevPersonType.RAW_ROW)
            tx.insertTestPartnership(adult1 = adult1.id, adult2 = adult2.id)
        }

        assertCleanedUpPeople(testDate, setOf(adult1.id, adult2.id))
    }

    @Test
    fun `adult with no family that has logged in recently is not cleaned up`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.execute {
                sql(
                    "UPDATE person SET last_login = ${bind(testDate.minusMonths(2))} WHERE id = ${bind(adult1.id)}"
                )
            }
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `guardian and their child are not cleaned up when guardian has logged in recently`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.RAW_ROW)
            tx.insertGuardian(adult1.id, child1.id)
            tx.execute {
                sql(
                    "UPDATE person SET last_login = ${bind(testDate.minusDays(14))} WHERE id = ${bind(adult1.id)}"
                )
            }
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult who is saved in the application other guardian table is not cleaned up`() {
        lateinit var otherGuardian: PersonId
        db.transaction { tx ->
            val applicationOwner = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            otherGuardian = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            val child = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            tx.insert(DevGuardian(guardianId = applicationOwner, childId = child))
            tx.insert(DevGuardian(guardianId = otherGuardian, childId = child))
            val application =
                tx.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    guardianId = applicationOwner,
                    childId = child,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            child = Child(dateOfBirth = null),
                            guardian = Adult(),
                            apply = Apply(preferredUnits = listOf(daycare.id)),
                        ),
                )
            tx.syncApplicationOtherGuardians(application, testDate)
            tx.deleteGuardianRelationship(childId = child, guardianId = otherGuardian)
        }
        assertCleanedUpPeople(testDate, setOf())

        db.transaction { tx ->
            tx.createUpdate {
                    sql(
                        "DELETE FROM application_other_guardian WHERE guardian_id = ${bind(otherGuardian)}"
                    )
                }
                .execute()
        }
        assertCleanedUpPeople(testDate, setOf(otherGuardian))
    }

    @Test
    fun `head of family and their child are not cleaned up when child has a placement`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(DevPlacement(childId = child1.id, unitId = daycare.id))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `family with two children is not cleaned up when one of the children has a placement`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(DevPlacement(childId = child1.id, unitId = daycare.id))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `family with two children and two adults is not cleaned up when one of the children has a placement`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(adult2, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insertTestPartnership(adult1 = adult1.id, adult2 = adult2.id)
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(DevPlacement(childId = child1.id, unitId = daycare.id))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `family with two children with separate heads of family is not cleaned up when one of the children has a placement`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(adult2, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insertTestPartnership(adult1 = adult1.id, adult2 = adult2.id)
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(DevPlacement(childId = child1.id, unitId = daycare.id))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult with income statement is not cleaned up`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(
                DevIncomeStatement(
                    personId = adult1.id,
                    data = IncomeStatementBody.HighestFee(LocalDate.now(), null),
                )
            )
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult and child with pedagogical document is not cleaned up`() {
        db.transaction { tx ->
            tx.insert(child1, DevPersonType.RAW_ROW)
            val docId = PedagogicalDocumentId(UUID.randomUUID())
            tx.insert(DevPedagogicalDocument(docId, child1.id, "document"))
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.createUpdate {
                    sql(
                        "INSERT INTO pedagogical_document_read (person_id, pedagogical_document_id, read_at) VALUES (${bind(adult1.id)}, ${bind(docId)}, ${bind(testDate)})"
                    )
                }
                .execute()
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult and child with child document is not cleaned up`() {
        val templateContent =
            DocumentTemplateContent(
                sections =
                    listOf(
                        Section(
                            id = "s1",
                            label = "Eka",
                            questions =
                                listOf(Question.TextQuestion(id = "q1", label = "kysymys 1")),
                        )
                    )
            )
        val documentContent =
            DocumentContent(
                answers = listOf(AnsweredQuestion.TextAnswer(questionId = "q1", answer = "foobar"))
            )
        val employee = DevEmployee()
        val employeeUser = AuthenticatedUser.Employee(employee.id, setOf(UserRole.ADMIN))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(child1, DevPersonType.RAW_ROW)
            val template =
                tx.insert(
                    DevDocumentTemplate(
                        validity = DateRange(testDate, null),
                        content = templateContent,
                    )
                )
            val document =
                tx.insert(
                    DevChildDocument(
                        status = DocumentStatus.COMPLETED,
                        childId = child1.id,
                        templateId = template,
                        content = documentContent,
                        modifiedAt = HelsinkiDateTime.now(),
                        modifiedBy = employeeUser.evakaUserId,
                        contentLockedAt = HelsinkiDateTime.now(),
                        contentLockedBy = employeeUser.id,
                        publishedVersions =
                            listOf(
                                DevChildDocumentPublishedVersion(
                                    versionNumber = 1,
                                    createdAt = HelsinkiDateTime.now(),
                                    createdBy = employeeUser.evakaUserId,
                                    publishedContent = documentContent,
                                )
                            ),
                    )
                )
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.createUpdate {
                    sql(
                        "INSERT INTO child_document_read (person_id, document_id, read_at) VALUES (${bind(adult1.id)}, ${bind(document)}, ${bind(testDate)})"
                    )
                }
                .execute()
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult with received messages is cleaned up`() {
        val now = HelsinkiDateTime.now()
        db.transaction { tx ->
            val supervisor = DevEmployee()
            tx.insert(supervisor)
            val employeeAccount = tx.upsertEmployeeMessageAccount(supervisor.id)

            tx.insert(adult1, DevPersonType.ADULT)
            val personAccount = tx.getCitizenMessageAccount(adult1.id)

            val contentId = tx.insertMessageContent("content", employeeAccount)
            val threadId =
                tx.insertThread(
                    MessageType.MESSAGE,
                    "title",
                    urgent = false,
                    sensitive = false,
                    isCopy = false,
                )
            val messageId =
                tx.insertMessage(
                    now = now,
                    contentId = contentId,
                    threadId = threadId,
                    sender = employeeAccount,
                    sentAt = now,
                    recipientNames = listOf("recipient name"),
                    municipalAccountName = "Espoo",
                    serviceWorkerAccountName = "Espoon palveluohjaus",
                    financeAccountName = "Espoon asiakasmaksut",
                )
            tx.insertRecipients(listOf(messageId to setOf(personAccount)))
        }

        assertCleanedUpPeople(testDate, setOf(adult1.id))
    }

    @Test
    fun `adult with sent messages is not cleaned up`() {
        val now = HelsinkiDateTime.now()
        db.transaction { tx ->
            val supervisor = DevEmployee()
            tx.insert(supervisor)
            val employeeAccount = tx.upsertEmployeeMessageAccount(supervisor.id)

            tx.insert(adult1, DevPersonType.ADULT)
            val personAccount = tx.getCitizenMessageAccount(adult1.id)

            val contentId = tx.insertMessageContent("content", personAccount)
            val threadId =
                tx.insertThread(
                    MessageType.MESSAGE,
                    "title",
                    urgent = false,
                    sensitive = false,
                    isCopy = false,
                )
            val messageId =
                tx.insertMessage(
                    now = now,
                    contentId = contentId,
                    threadId = threadId,
                    sender = personAccount,
                    sentAt = now,
                    recipientNames = listOf("employee name"),
                    municipalAccountName = "Espoo",
                    serviceWorkerAccountName = "Espoon palveluohjaus",
                    financeAccountName = "Espoon asiakasmaksut",
                )
            tx.insertRecipients(listOf(messageId to setOf(employeeAccount)))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    private fun assertCleanedUpPeople(queryDate: LocalDate, cleanedUpPeople: Set<PersonId>) {
        val result = db.transaction { cleanUpInactivePeople(it, queryDate) }

        assertEquals(cleanedUpPeople, result)
    }
}
