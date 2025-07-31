// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.caseprocess.DocumentConfidentiality
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.Section
import fi.espoo.evaka.document.getTemplate
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChildDocument
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDocumentTemplate
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.toEvakaUser
import fi.espoo.evaka.user.EvakaUser
import fi.espoo.evaka.user.EvakaUserType
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ChildDocumentControllerCitizenIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired lateinit var controller: ChildDocumentControllerCitizen
    @Autowired lateinit var employeeController: ChildDocumentController

    val clock = MockEvakaClock(2022, 1, 1, 15, 0)

    val citizen = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)
    val employeeUser = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))

    val templateId = DocumentTemplateId(UUID.randomUUID())

    val templateContent =
        DocumentTemplateContent(
            sections =
                listOf(
                    Section(
                        id = "s1",
                        label = "Eka",
                        questions = listOf(Question.TextQuestion(id = "q1", label = "kysymys 1")),
                    )
                )
        )

    val documentId = ChildDocumentId(UUID.randomUUID())

    val documentContent =
        DocumentContent(
            answers = listOf(AnsweredQuestion.TextAnswer(questionId = "q1", answer = "foobar"))
        )

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare.copy(language = Language.sv))
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            tx.insert(DevGuardian(testAdult_1.id, testChild_1.id))
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusDays(5),
                )
            )
            tx.insert(
                DevDocumentTemplate(
                    id = templateId,
                    type = ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
                    name = "Pedagoginen arvio 2023",
                    validity = DateRange(clock.today(), clock.today()),
                    content = templateContent,
                    confidentiality = DocumentConfidentiality(100, "JulkL 24.1 § 25 ja 30 kohdat"),
                )
            )
            tx.insert(
                DevChildDocument(
                    id = documentId,
                    status = DocumentStatus.DRAFT,
                    childId = testChild_1.id,
                    templateId = templateId,
                    content = documentContent,
                    publishedContent = null,
                    modifiedAt = clock.now(),
                    contentModifiedAt = clock.now(),
                    contentModifiedBy = employeeUser.id,
                    publishedAt = null,
                    answeredAt = null,
                    answeredBy = null,
                )
            )
        }
    }

    @Test
    fun `Unpublished document is not shown`() {
        assertEquals(mapOf(testChild_1.id to 0), getUnreadCount())
        assertEquals(emptyList(), getDocumentsByChild(testChild_1.id))
        assertThrows<Forbidden> { getDocument(documentId) }
    }

    @Test
    fun `Published document is shown`() {
        publishDocument(documentId)
        asyncJobRunner.runPendingJobsSync(clock)
        val template = db.read { it.getTemplate(templateId)!! }
        assertEquals(
            DocumentConfidentiality(durationYears = 100, basis = "JulkL 24.1 § 25 ja 30 kohdat"),
            template.confidentiality,
        )

        assertEquals(mapOf(testChild_1.id to 1), getUnreadCount())
        assertEquals(
            listOf(
                ChildDocumentCitizenSummary(
                    id = documentId,
                    type = ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
                    publishedAt = clock.now(),
                    templateName = "Pedagoginen arvio 2023",
                    status = DocumentStatus.DRAFT,
                    unread = true,
                    child =
                        ChildBasics(
                            id = testChild_1.id,
                            firstName = testChild_1.firstName,
                            lastName = testChild_1.lastName,
                            dateOfBirth = testChild_1.dateOfBirth,
                        ),
                    answeredAt = null,
                    answeredBy = null,
                )
            ),
            getDocumentsByChild(testChild_1.id),
        )
        assertEquals(
            ChildDocumentCitizenDetails(
                id = documentId,
                status = DocumentStatus.DRAFT,
                publishedAt = clock.now(),
                downloadable = true,
                content = documentContent,
                child =
                    ChildBasics(
                        id = testChild_1.id,
                        firstName = testChild_1.firstName,
                        lastName = testChild_1.lastName,
                        dateOfBirth = testChild_1.dateOfBirth,
                    ),
                template = template,
            ),
            getDocument(documentId),
        )

        putDocumentRead(documentId)
        assertEquals(mapOf(testChild_1.id to 0), getUnreadCount())
        assertFalse(getDocumentsByChild(testChild_1.id).first().unread)

        assertEquals(200, downloadChildDocument(documentId).statusCode.value())
    }

    @Test
    fun `Updates after publish are shown only after republish`() {
        publishDocument(documentId)

        assertEquals(mapOf(testChild_1.id to 1), getUnreadCount())
        assertEquals(documentContent, getDocument(documentId).content)
        putDocumentRead(documentId)
        assertEquals(mapOf(testChild_1.id to 0), getUnreadCount())

        val updatedContent =
            DocumentContent(
                answers = listOf(AnsweredQuestion.TextAnswer(questionId = "q1", answer = "updated"))
            )
        updateDocumentContent(documentId, updatedContent)

        assertEquals(mapOf(testChild_1.id to 0), getUnreadCount())
        assertEquals(documentContent, getDocument(documentId).content)

        publishDocument(documentId)

        assertEquals(mapOf(testChild_1.id to 1), getUnreadCount())
        assertEquals(updatedContent, getDocument(documentId).content)
    }

    @Test
    fun `updateChildDocument updates status and content`() {
        val template =
            DevDocumentTemplate(
                type = ChildDocumentType.CITIZEN_BASIC,
                name = "Medialupa",
                validity = DateRange(clock.today(), clock.today()),
                content = templateContent,
            )
        val documentId =
            db.transaction { tx ->
                val templateId = tx.insert(template)
                tx.insert(
                    DevChildDocument(
                        status = DocumentStatus.CITIZEN_DRAFT,
                        childId = testChild_1.id,
                        templateId = templateId,
                        content = documentContent,
                        publishedContent = documentContent,
                        modifiedAt = clock.now(),
                        contentModifiedAt = clock.now(),
                        contentModifiedBy = employeeUser.id,
                        publishedAt = clock.now(),
                        answeredAt = null,
                        answeredBy = null,
                    )
                )
            }
        assertEquals(
            listOf(
                ChildDocumentCitizenSummary(
                    id = documentId,
                    status = DocumentStatus.CITIZEN_DRAFT,
                    type = template.type,
                    templateName = template.name,
                    publishedAt = clock.now(),
                    unread = true,
                    child =
                        ChildBasics(
                            id = testChild_1.id,
                            firstName = testChild_1.firstName,
                            lastName = testChild_1.lastName,
                            dateOfBirth = testChild_1.dateOfBirth,
                        ),
                    answeredAt = null,
                    answeredBy = null,
                )
            ),
            getUnansweredChildDocuments(),
        )
        clock.tick()

        val content =
            DocumentContent(
                answers =
                    listOf(
                        AnsweredQuestion.TextAnswer(questionId = "q1", answer = "huoltajan vastaus")
                    )
            )
        updateDocument(
            documentId,
            ChildDocumentControllerCitizen.UpdateChildDocumentRequest(
                status = DocumentStatus.COMPLETED,
                content = content,
            ),
        )

        val documents = getDocumentsByChild(testChild_1.id)
        assertEquals(
            listOf(
                ChildDocumentCitizenSummary(
                    id = documentId,
                    status = DocumentStatus.COMPLETED,
                    type = template.type,
                    templateName = template.name,
                    publishedAt = clock.now(),
                    unread = true,
                    child =
                        ChildBasics(
                            id = testChild_1.id,
                            firstName = testChild_1.firstName,
                            lastName = testChild_1.lastName,
                            dateOfBirth = testChild_1.dateOfBirth,
                        ),
                    answeredAt = clock.now(),
                    answeredBy = testAdult_1.toEvakaUser(EvakaUserType.CITIZEN),
                )
            ),
            documents.filter { it.type == ChildDocumentType.CITIZEN_BASIC },
        )
        val document = getDocument(documentId)
        assertEquals(
            ChildDocumentCitizenDetails(
                id = documentId,
                status = DocumentStatus.COMPLETED,
                publishedAt = clock.now(),
                downloadable = false,
                content = content,
                child =
                    ChildBasics(
                        id = testChild_1.id,
                        firstName = testChild_1.firstName,
                        lastName = testChild_1.lastName,
                        dateOfBirth = testChild_1.dateOfBirth,
                    ),
                template = template.toDocumentTemplate(),
            ),
            document,
        )
        assertEquals(emptyList(), getUnansweredChildDocuments())
    }

    @Test
    fun `guardian sees child documents only for own child`() {
        publishDocument(documentId)
        asyncJobRunner.runPendingJobsSync(clock)
        val template =
            DevDocumentTemplate(
                    type = ChildDocumentType.CITIZEN_BASIC,
                    name = "Medialupa",
                    validity = DateRange(clock.today(), clock.today()),
                    content = templateContent,
                )
                .also { db.transaction { tx -> tx.insert(it) } }
        val child1DocumentId =
            db.transaction { tx ->
                tx.insert(
                    DevChildDocument(
                        status = DocumentStatus.CITIZEN_DRAFT,
                        childId = testChild_1.id,
                        templateId = template.id,
                        content = documentContent,
                        publishedContent = documentContent,
                        modifiedAt = clock.now(),
                        contentModifiedAt = clock.now(),
                        contentModifiedBy = employeeUser.id,
                        publishedAt = clock.now(),
                        answeredAt = null,
                        answeredBy = null,
                    )
                )
            }
        val child2DocumentId =
            db.transaction { tx ->
                tx.insert(testChild_2, DevPersonType.CHILD)
                tx.insert(testAdult_2, DevPersonType.ADULT)
                tx.insert(DevGuardian(testAdult_2.id, testChild_2.id))
                tx.insert(
                    DevPlacement(
                        childId = testChild_2.id,
                        unitId = testDaycare.id,
                        startDate = clock.today(),
                        endDate = clock.today().plusDays(5),
                    )
                )
                tx.insert(
                    DevChildDocument(
                        status = DocumentStatus.CITIZEN_DRAFT,
                        childId = testChild_2.id,
                        templateId = template.id,
                        content = documentContent,
                        publishedContent = documentContent,
                        modifiedAt = clock.now(),
                        contentModifiedAt = clock.now(),
                        contentModifiedBy = employeeUser.id,
                        publishedAt = clock.now(),
                        answeredAt = null,
                        answeredBy = null,
                    )
                )
            }

        val summary11 =
            ChildDocumentCitizenSummary(
                id = documentId,
                type = ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
                publishedAt = clock.now(),
                templateName = "Pedagoginen arvio 2023",
                status = DocumentStatus.DRAFT,
                unread = true,
                child =
                    ChildBasics(
                        id = testChild_1.id,
                        firstName = testChild_1.firstName,
                        lastName = testChild_1.lastName,
                        dateOfBirth = testChild_1.dateOfBirth,
                    ),
                answeredAt = null,
                answeredBy = null,
            )
        val summary12 =
            ChildDocumentCitizenSummary(
                id = child1DocumentId,
                status = DocumentStatus.CITIZEN_DRAFT,
                type = template.type,
                templateName = template.name,
                publishedAt = clock.now(),
                unread = true,
                child =
                    ChildBasics(
                        id = testChild_1.id,
                        firstName = testChild_1.firstName,
                        lastName = testChild_1.lastName,
                        dateOfBirth = testChild_1.dateOfBirth,
                    ),
                answeredAt = null,
                answeredBy = null,
            )
        assertEquals(mapOf(testChild_1.id to 2), getUnreadCount())
        assertEquals(
            listOf(summary11, summary12),
            getDocumentsByChild(testChild_1.id).sortedBy { it.type },
        )
        assertEquals(listOf(summary12), getUnansweredChildDocuments())
        assertThrows<Forbidden> { getDocumentsByChild(testChild_2.id) }
        val citizen2 = AuthenticatedUser.Citizen(testAdult_2.id, CitizenAuthLevel.STRONG)
        val summary2 =
            ChildDocumentCitizenSummary(
                id = child2DocumentId,
                status = DocumentStatus.CITIZEN_DRAFT,
                type = template.type,
                templateName = template.name,
                publishedAt = clock.now(),
                unread = true,
                child =
                    ChildBasics(
                        id = testChild_2.id,
                        firstName = testChild_2.firstName,
                        lastName = testChild_2.lastName,
                        dateOfBirth = testChild_2.dateOfBirth,
                    ),
                answeredAt = null,
                answeredBy = null,
            )
        assertEquals(mapOf(testChild_2.id to 1), getUnreadCount(user = citizen2))
        assertEquals(listOf(summary2), getDocumentsByChild(testChild_2.id, user = citizen2))
        assertEquals(listOf(summary2), getUnansweredChildDocuments(user = citizen2))
        assertThrows<Forbidden> { getDocumentsByChild(testChild_1.id, user = citizen2) }
    }

    @Test
    fun `guardian can't get documents if child's placement has ended`() {
        val template =
            DevDocumentTemplate(
                    type = ChildDocumentType.CITIZEN_BASIC,
                    name = "Medialupa",
                    validity = DateRange(clock.today(), clock.today()),
                    content = templateContent,
                )
                .also { db.transaction { tx -> tx.insert(it) } }

        val adult =
            DevPerson(PersonId(UUID.randomUUID())).also {
                db.transaction { tx -> tx.insert(it, DevPersonType.ADULT) }
            }
        val child =
            DevPerson(PersonId(UUID.randomUUID())).also {
                db.transaction { tx -> tx.insert(it, DevPersonType.CHILD) }
            }
        db.transaction { tx -> tx.insert(DevGuardian(adult.id, child.id)) }

        val adultUser = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG)
        val employee =
            DevEmployee(EmployeeId(UUID.randomUUID())).also {
                db.transaction { tx -> tx.insert(it) }
            }
        val employeeUser = AuthenticatedUser.Employee(employee.id, setOf(UserRole.ADMIN))

        val careArea =
            DevCareArea(shortName = "test_care_area").also {
                db.transaction { tx -> tx.insert(it) }
            }
        val daycare =
            DevDaycare(areaId = careArea.id).also { db.transaction { tx -> tx.insert(it) } }

        db.transaction { tx ->
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.CITIZEN_DRAFT,
                    childId = child.id,
                    templateId = template.id,
                    content = documentContent,
                    publishedContent = documentContent,
                    modifiedAt = clock.now(),
                    contentModifiedAt = clock.now(),
                    contentModifiedBy = employeeUser.id,
                    publishedAt = clock.now(),
                    answeredAt = null,
                    answeredBy = null,
                )
            )

            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = clock.today().minusDays(1),
                    endDate = clock.today().minusDays(1),
                )
            )
        }

        assertThrows<Forbidden> { getDocumentsByChild(child.id, adultUser) }
    }

    @Test
    fun `answered by employee user name is not exposed`() {
        val template =
            DevDocumentTemplate(
                    type = ChildDocumentType.CITIZEN_BASIC,
                    name = "Medialupa",
                    validity = DateRange(clock.today(), clock.today()),
                    content = templateContent,
                )
                .also { db.transaction { tx -> tx.insert(it) } }
        val now = clock.now()
        val childDocumentBase =
            DevChildDocument(
                status = DocumentStatus.COMPLETED,
                childId = testChild_1.id,
                templateId = template.id,
                content = documentContent,
                publishedContent = documentContent,
                modifiedAt = now,
                contentModifiedAt = now,
                contentModifiedBy = employeeUser.id,
                publishedAt = now,
                answeredAt = now,
                answeredBy = null,
            )
        val answeredByCitizenId =
            db.transaction { tx ->
                tx.insert(
                    childDocumentBase.copy(
                        id = ChildDocumentId(UUID.randomUUID()),
                        answeredBy = citizen.evakaUserId,
                    )
                )
            }
        val answeredByEmployeeId =
            db.transaction { tx ->
                tx.insert(
                    childDocumentBase.copy(
                        id = ChildDocumentId(UUID.randomUUID()),
                        answeredBy = employeeUser.evakaUserId,
                    )
                )
            }

        assertThat(getUnansweredChildDocuments()).isEmpty()
        assertThat(getDocumentsByChild(testChild_1.id))
            .extracting({ it.id }, { it.answeredAt }, { it.answeredBy })
            .containsExactlyInAnyOrder(
                Tuple(answeredByCitizenId, now, testAdult_1.toEvakaUser(EvakaUserType.CITIZEN)),
                Tuple(
                    answeredByEmployeeId,
                    now,
                    EvakaUser(employeeUser.evakaUserId, "", EvakaUserType.EMPLOYEE),
                ),
            )
    }

    private fun getUnreadCount(user: AuthenticatedUser.Citizen = citizen) =
        controller.getUnreadDocumentsCount(dbInstance(), user, clock)

    private fun getDocumentsByChild(childId: ChildId, user: AuthenticatedUser.Citizen = citizen) =
        controller.getDocuments(dbInstance(), user, clock, childId)

    private fun getDocument(id: ChildDocumentId) =
        controller.getDocument(dbInstance(), citizen, clock, id)

    private fun downloadChildDocument(id: ChildDocumentId) =
        controller.downloadChildDocument(dbInstance(), citizen, clock, id)

    private fun putDocumentRead(id: ChildDocumentId) =
        controller.putDocumentRead(dbInstance(), citizen, clock, id)

    private fun getUnansweredChildDocuments(user: AuthenticatedUser.Citizen = citizen) =
        controller.getUnansweredChildDocuments(dbInstance(), user, clock)

    private fun updateDocument(
        id: ChildDocumentId,
        body: ChildDocumentControllerCitizen.UpdateChildDocumentRequest,
    ) = controller.updateChildDocument(dbInstance(), citizen, clock, id, body)

    private fun publishDocument(id: ChildDocumentId) =
        employeeController.publishDocument(dbInstance(), employeeUser, clock, id)

    private fun updateDocumentContent(id: ChildDocumentId, content: DocumentContent) =
        employeeController.updateDocumentContent(dbInstance(), employeeUser, clock, id, content)
}
