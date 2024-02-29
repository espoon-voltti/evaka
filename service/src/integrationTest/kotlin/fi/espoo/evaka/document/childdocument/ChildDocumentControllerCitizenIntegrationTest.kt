// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.Section
import fi.espoo.evaka.document.getTemplate
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevChildDocument
import fi.espoo.evaka.shared.dev.DevDocumentTemplate
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ChildDocumentControllerCitizenIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
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
                        questions =
                            listOf(
                                Question.TextQuestion(id = "q1", label = "kysymys 1"),
                            )
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
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(DevGuardian(testAdult_1.id, testChild_1.id))
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = clock.today(),
                endDate = clock.today().plusDays(5)
            )
            tx.insert(
                DevDocumentTemplate(
                    id = templateId,
                    type = DocumentType.PEDAGOGICAL_ASSESSMENT,
                    name = "Pedagoginen arvio 2023",
                    validity = DateRange(clock.today(), clock.today()),
                    content = templateContent
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
                    publishedAt = null
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
        db.transaction { tx -> tx.publishChildDocument(documentId, clock.now()) }
        val template = db.read { it.getTemplate(templateId)!! }

        assertEquals(mapOf(testChild_1.id to 1), getUnreadCount())
        assertEquals(
            listOf(
                ChildDocumentCitizenSummary(
                    id = documentId,
                    type = DocumentType.PEDAGOGICAL_ASSESSMENT,
                    publishedAt = clock.now(),
                    templateName = "Pedagoginen arvio 2023",
                    status = DocumentStatus.DRAFT,
                    unread = true
                )
            ),
            getDocumentsByChild(testChild_1.id)
        )
        assertEquals(
            ChildDocumentCitizenDetails(
                id = documentId,
                status = DocumentStatus.DRAFT,
                publishedAt = clock.now(),
                content = documentContent,
                child =
                    ChildBasics(
                        id = testChild_1.id,
                        firstName = testChild_1.firstName,
                        lastName = testChild_1.lastName,
                        dateOfBirth = testChild_1.dateOfBirth
                    ),
                template = template
            ),
            getDocument(documentId)
        )

        putDocumentRead(documentId)
        assertEquals(mapOf(testChild_1.id to 0), getUnreadCount())
        assertFalse(getDocumentsByChild(testChild_1.id).first().unread)
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

    private fun getUnreadCount() = controller.getUnreadDocumentsCount(dbInstance(), citizen, clock)

    private fun getDocumentsByChild(childId: ChildId) =
        controller.getDocuments(dbInstance(), citizen, clock, childId)

    private fun getDocument(id: ChildDocumentId) =
        controller.getDocument(dbInstance(), citizen, clock, id)

    private fun putDocumentRead(id: ChildDocumentId) =
        controller.putDocumentRead(dbInstance(), citizen, clock, id)

    private fun publishDocument(id: ChildDocumentId) =
        employeeController.publishDocument(dbInstance(), employeeUser, clock, id)

    private fun updateDocumentContent(id: ChildDocumentId, content: DocumentContent) =
        employeeController.updateDocumentContent(dbInstance(), employeeUser, clock, id, content)
}
