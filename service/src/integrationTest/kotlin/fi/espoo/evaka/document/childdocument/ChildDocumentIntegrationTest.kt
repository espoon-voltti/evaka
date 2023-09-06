// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.document.CheckboxGroupQuestionOption
import fi.espoo.evaka.document.DocumentTemplate
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.RadioButtonGroupQuestionOption
import fi.espoo.evaka.document.Section
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDocumentTemplate
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDocumentTemplate
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ChildDocumentIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var controller: ChildDocumentController

    lateinit var employeeUser: AuthenticatedUser

    final val now = MockEvakaClock(2022, 1, 1, 15, 0)

    final val templateIdPed = DocumentTemplateId(UUID.randomUUID())
    final val templateIdHojks = DocumentTemplateId(UUID.randomUUID())

    final val templateContent =
        DocumentTemplateContent(
            sections =
                listOf(
                    Section(
                        id = "s1",
                        label = "Eka",
                        questions =
                            listOf(
                                Question.TextQuestion(id = "q1", label = "kysymys 1"),
                                Question.CheckboxQuestion(id = "q2", label = "kysymys 2"),
                                Question.CheckboxGroupQuestion(
                                    id = "q3",
                                    label = "kysymys 3",
                                    options =
                                        listOf(
                                            CheckboxGroupQuestionOption("a", "eka"),
                                            CheckboxGroupQuestionOption("b", "toka"),
                                            CheckboxGroupQuestionOption("c", "kolmas"),
                                        )
                                ),
                                Question.RadioButtonGroupQuestion(
                                    id = "q4",
                                    label = "kysymys 4",
                                    options =
                                        listOf(
                                            RadioButtonGroupQuestionOption("a", "eka"),
                                            RadioButtonGroupQuestionOption("b", "toka"),
                                            RadioButtonGroupQuestionOption("c", "kolmas"),
                                        )
                                )
                            )
                    )
                )
        )

    val devTemplatePed =
        DevDocumentTemplate(
            id = templateIdPed,
            type = DocumentType.PEDAGOGICAL_ASSESSMENT,
            name = "Pedagoginen arvio 2023",
            validity = DateRange(now.today(), now.today()),
            content = templateContent
        )

    val devTemplateHojks =
        DevDocumentTemplate(
            id = templateIdHojks,
            type = DocumentType.HOJKS,
            name = "HOJKS 2023",
            validity = DateRange(now.today(), now.today()),
            content = templateContent
        )

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            employeeUser =
                tx.insertTestEmployee(DevEmployee()).let {
                    AuthenticatedUser.Employee(it, setOf(UserRole.ADMIN))
                }
            tx.insertTestCareArea(testArea)
            tx.insertTestDaycare(testDaycare.copy(language = Language.sv))
            tx.insertTestPerson(testChild_1)
            tx.insertTestChild(DevChild(testChild_1.id))
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = now.today(),
                endDate = now.today().plusDays(5)
            )
            tx.insertTestDocumentTemplate(devTemplatePed)
            tx.insertTestDocumentTemplate(devTemplateHojks)
        }
    }

    @Test
    fun `creating new document and fetching it`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(childId = testChild_1.id, templateId = templateIdPed)
            )

        val document = controller.getDocument(dbInstance(), employeeUser, now, documentId)
        assertEquals(
            ChildDocumentWithPermittedActions(
                data =
                    ChildDocumentDetails(
                        id = documentId,
                        status = DocumentStatus.DRAFT,
                        publishedAt = null,
                        content = DocumentContent(answers = emptyList()),
                        child =
                            ChildBasics(
                                id = testChild_1.id,
                                firstName = testChild_1.firstName,
                                lastName = testChild_1.lastName,
                                dateOfBirth = testChild_1.dateOfBirth
                            ),
                        template =
                            DocumentTemplate(
                                id = templateIdPed,
                                name = devTemplatePed.name,
                                type = devTemplatePed.type,
                                language = devTemplatePed.language,
                                confidential = devTemplatePed.confidential,
                                legalBasis = devTemplatePed.legalBasis,
                                validity = devTemplatePed.validity,
                                published = devTemplatePed.published,
                                content = templateContent
                            )
                    ),
                permittedActions =
                    setOf(
                        Action.ChildDocument.DELETE,
                        Action.ChildDocument.PUBLISH,
                        Action.ChildDocument.READ,
                        Action.ChildDocument.UPDATE,
                        Action.ChildDocument.NEXT_STATE,
                        Action.ChildDocument.PREV_STATE
                    )
            ),
            document
        )

        val summaries = controller.getDocuments(dbInstance(), employeeUser, now, testChild_1.id)
        assertEquals(
            listOf(
                ChildDocumentSummary(
                    id = documentId,
                    status = DocumentStatus.DRAFT,
                    type = devTemplatePed.type,
                    templateName = devTemplatePed.name,
                    publishedAt = null
                )
            ),
            summaries
        )
    }

    @Test
    fun `creating new document not allowed for expired document`() {
        val template2 =
            db.transaction {
                it.insertTestDocumentTemplate(
                    DevDocumentTemplate(
                        validity = DateRange(now.today().minusDays(9), now.today().minusDays(1)),
                        content = templateContent
                    )
                )
            }
        assertThrows<BadRequest> {
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, template2)
            )
        }
    }

    @Test
    fun `creating new document not allowed for unpublished document`() {
        val template2 =
            db.transaction {
                it.insertTestDocumentTemplate(
                    DevDocumentTemplate(
                        validity = DateRange(now.today(), now.today()),
                        published = false,
                        content = templateContent
                    )
                )
            }
        assertThrows<BadRequest> {
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, template2)
            )
        }
    }

    @Test
    fun `publishing document`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        controller.publishDocument(dbInstance(), employeeUser, now, documentId)
        assertEquals(
            now.now(),
            controller.getDocument(dbInstance(), employeeUser, now, documentId).data.publishedAt
        )
    }

    @Test
    fun `deleting draft document`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        controller.deleteDraftDocument(dbInstance(), employeeUser, now, documentId)
        assertThrows<NotFound> {
            controller.getDocument(dbInstance(), employeeUser, now, documentId)
        }
    }

    @Test
    fun `updating content with all answers`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        val content =
            DocumentContent(
                answers =
                    listOf(
                        AnsweredQuestion.TextAnswer("q1", "hello"),
                        AnsweredQuestion.CheckboxAnswer("q2", true),
                        AnsweredQuestion.CheckboxGroupAnswer(
                            "q3",
                            listOf(CheckboxGroupAnswerContent("a"), CheckboxGroupAnswerContent("c"))
                        ),
                        AnsweredQuestion.RadioButtonGroupAnswer("q4", "b")
                    )
            )
        controller.updateDocumentContent(dbInstance(), employeeUser, now, documentId, content)
        assertEquals(
            content,
            controller.getDocument(dbInstance(), employeeUser, now, documentId).data.content
        )
    }

    @Test
    fun `updating content with partial but valid answers is ok`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        val content = DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello")))
        controller.updateDocumentContent(dbInstance(), employeeUser, now, documentId, content)
        assertEquals(
            content,
            controller.getDocument(dbInstance(), employeeUser, now, documentId).data.content
        )
    }

    @Test
    fun `updating content of completed document fails`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        controller.nextState(
            dbInstance(),
            employeeUser,
            now,
            documentId,
            ChildDocumentController.StatusChangeRequest(DocumentStatus.COMPLETED)
        )
        val content = DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello")))
        assertThrows<Forbidden> {
            controller.updateDocumentContent(dbInstance(), employeeUser, now, documentId, content)
        }
    }

    @Test
    fun `updating content fails when answering nonexistent question`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        val content =
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q999", "hello")))
        assertThrows<BadRequest> {
            controller.updateDocumentContent(dbInstance(), employeeUser, now, documentId, content)
        }
    }

    @Test
    fun `updating content fails when answering question with wrong type`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        val content = DocumentContent(answers = listOf(AnsweredQuestion.CheckboxAnswer("q1", true)))
        assertThrows<BadRequest> {
            controller.updateDocumentContent(dbInstance(), employeeUser, now, documentId, content)
        }
    }

    @Test
    fun `updating content fails when answering checkbox group question with unknown option`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        val content =
            DocumentContent(
                answers =
                    listOf(
                        AnsweredQuestion.CheckboxGroupAnswer(
                            "q3",
                            listOf(CheckboxGroupAnswerContent("a"), CheckboxGroupAnswerContent("d"))
                        )
                    )
            )
        assertThrows<BadRequest> {
            controller.updateDocumentContent(dbInstance(), employeeUser, now, documentId, content)
        }
    }

    @Test
    fun `updating content fails when answering radio button group question with unknown option`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        val content =
            DocumentContent(answers = listOf(AnsweredQuestion.RadioButtonGroupAnswer("q3", "d")))
        assertThrows<BadRequest> {
            controller.updateDocumentContent(dbInstance(), employeeUser, now, documentId, content)
        }
    }

    @Test
    fun `hojks status flow`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateIdHojks)
            )
        assertEquals(DocumentStatus.DRAFT, getDocument(documentId).status)
        assertNull(getDocument(documentId).publishedAt)

        // cannot skip states
        assertThrows<Conflict> { nextState(documentId, DocumentStatus.COMPLETED) }

        nextState(documentId, DocumentStatus.PREPARED)
        assertEquals(DocumentStatus.PREPARED, getDocument(documentId).status)
        assertNotNull(getDocument(documentId).publishedAt)
        nextState(documentId, DocumentStatus.COMPLETED)
        assertEquals(DocumentStatus.COMPLETED, getDocument(documentId).status)
        prevState(documentId, DocumentStatus.PREPARED)
        assertEquals(DocumentStatus.PREPARED, getDocument(documentId).status)
        prevState(documentId, DocumentStatus.DRAFT)
        assertEquals(DocumentStatus.DRAFT, getDocument(documentId).status)
        assertNotNull(getDocument(documentId).publishedAt)
    }

    @Test
    fun `pedagogical doc status flow`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        assertEquals(DocumentStatus.DRAFT, getDocument(documentId).status)
        assertNull(getDocument(documentId).publishedAt)

        assertThrows<Conflict> { nextState(documentId, DocumentStatus.PREPARED) }

        nextState(documentId, DocumentStatus.COMPLETED)
        assertEquals(DocumentStatus.COMPLETED, getDocument(documentId).status)
        assertNotNull(getDocument(documentId).publishedAt)

        assertThrows<Conflict> { prevState(documentId, DocumentStatus.PREPARED) }

        prevState(documentId, DocumentStatus.DRAFT)
        assertEquals(DocumentStatus.DRAFT, getDocument(documentId).status)
        assertNotNull(getDocument(documentId).publishedAt)
    }

    private fun getDocument(id: ChildDocumentId) =
        controller.getDocument(dbInstance(), employeeUser, now, id).data
    private fun nextState(id: ChildDocumentId, status: DocumentStatus) =
        controller.nextState(
            dbInstance(),
            employeeUser,
            now,
            id,
            ChildDocumentController.StatusChangeRequest(status)
        )
    private fun prevState(id: ChildDocumentId, status: DocumentStatus) =
        controller.prevState(
            dbInstance(),
            employeeUser,
            now,
            id,
            ChildDocumentController.StatusChangeRequest(status)
        )
}
