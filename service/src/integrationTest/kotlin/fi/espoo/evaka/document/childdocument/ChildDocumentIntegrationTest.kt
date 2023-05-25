// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.document.DocumentTemplate
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.MultiselectOption
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.Section
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
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.util.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ChildDocumentIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var controller: ChildDocumentController

    lateinit var employeeUser: AuthenticatedUser

    val now = MockEvakaClock(2022, 1, 1, 15, 0)

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
                                Question.CheckboxQuestion(id = "q2", label = "kysymys 2"),
                                Question.CheckboxGroupQuestion(
                                    id = "q3",
                                    label = "kysymys 3",
                                    options =
                                        listOf(
                                            MultiselectOption("a", "eka"),
                                            MultiselectOption("b", "toka"),
                                            MultiselectOption("c", "kolmas"),
                                        )
                                ),
                                Question.RadioButtonGroupQuestion(
                                    id = "q4",
                                    label = "kysymys 4",
                                    options =
                                        listOf(
                                            MultiselectOption("a", "eka"),
                                            MultiselectOption("b", "toka"),
                                            MultiselectOption("c", "kolmas"),
                                        )
                                )
                            )
                    )
                )
        )

    val devTemplate =
        DevDocumentTemplate(
            id = templateId,
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
            tx.insertTestDocumentTemplate(devTemplate)
        }
    }

    @Test
    fun `creating new document and fetching it`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(childId = testChild_1.id, templateId = templateId)
            )

        val document = controller.getDocument(dbInstance(), employeeUser, now, documentId)
        assertEquals(
            ChildDocumentWithPermittedActions(
                data =
                    ChildDocumentDetails(
                        id = documentId,
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
                                id = templateId,
                                name = devTemplate.name,
                                type = devTemplate.type,
                                language = devTemplate.language,
                                confidential = devTemplate.confidential,
                                legalBasis = devTemplate.legalBasis,
                                validity = devTemplate.validity,
                                published = devTemplate.published,
                                content = templateContent
                            )
                    ),
                permittedActions =
                    setOf(
                        Action.ChildDocument.DELETE,
                        Action.ChildDocument.PUBLISH,
                        Action.ChildDocument.READ,
                        Action.ChildDocument.UPDATE,
                        Action.ChildDocument.UNPUBLISH
                    )
            ),
            document
        )

        val summaries = controller.getDocuments(dbInstance(), employeeUser, now, testChild_1.id)
        assertEquals(
            listOf(
                ChildDocumentSummary(
                    id = documentId,
                    type = devTemplate.type,
                    templateName = devTemplate.name,
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
    fun `publishing and unpublishing document`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateId)
            )
        controller.publishDocument(dbInstance(), employeeUser, now, documentId)
        assertEquals(
            now.now(),
            controller.getDocument(dbInstance(), employeeUser, now, documentId).data.publishedAt
        )
        controller.unpublishDocument(dbInstance(), employeeUser, now, documentId)
        assertEquals(
            null,
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
                ChildDocumentCreateRequest(testChild_1.id, templateId)
            )
        controller.deleteDraftDocument(dbInstance(), employeeUser, now, documentId)
        assertThrows<NotFound> {
            controller.getDocument(dbInstance(), employeeUser, now, documentId)
        }
    }

    @Test
    fun `deleting published document fails`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateId)
            )
        controller.publishDocument(dbInstance(), employeeUser, now, documentId)
        assertThrows<BadRequest> {
            controller.deleteDraftDocument(dbInstance(), employeeUser, now, documentId)
        }
    }

    @Test
    fun `updating content with all answers`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateId)
            )
        val content =
            DocumentContent(
                answers =
                    listOf(
                        AnsweredQuestion.TextAnswer("q1", "hello"),
                        AnsweredQuestion.CheckboxAnswer("q2", true),
                        AnsweredQuestion.CheckboxGroupAnswer("q3", listOf("a", "c")),
                        AnsweredQuestion.RadioButtonGroupAnswer("q4", "b")
                    )
            )
        controller.updateDraftDocumentContent(dbInstance(), employeeUser, now, documentId, content)
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
                ChildDocumentCreateRequest(testChild_1.id, templateId)
            )
        val content = DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello")))
        controller.updateDraftDocumentContent(dbInstance(), employeeUser, now, documentId, content)
        assertEquals(
            content,
            controller.getDocument(dbInstance(), employeeUser, now, documentId).data.content
        )
    }

    @Test
    fun `updating content of published document fails`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateId)
            )
        controller.publishDocument(dbInstance(), employeeUser, now, documentId)
        val content = DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello")))
        assertThrows<BadRequest> {
            controller.updateDraftDocumentContent(
                dbInstance(),
                employeeUser,
                now,
                documentId,
                content
            )
        }
    }

    @Test
    fun `updating content fails when answering nonexistent question`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateId)
            )
        val content =
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q999", "hello")))
        assertThrows<BadRequest> {
            controller.updateDraftDocumentContent(
                dbInstance(),
                employeeUser,
                now,
                documentId,
                content
            )
        }
    }

    @Test
    fun `updating content fails when answering question with wrong type`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateId)
            )
        val content = DocumentContent(answers = listOf(AnsweredQuestion.CheckboxAnswer("q1", true)))
        assertThrows<BadRequest> {
            controller.updateDraftDocumentContent(
                dbInstance(),
                employeeUser,
                now,
                documentId,
                content
            )
        }
    }

    @Test
    fun `updating content fails when answering checkbox group question with unknown option`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateId)
            )
        val content =
            DocumentContent(
                answers = listOf(AnsweredQuestion.CheckboxGroupAnswer("q3", listOf("a", "d")))
            )
        assertThrows<BadRequest> {
            controller.updateDraftDocumentContent(
                dbInstance(),
                employeeUser,
                now,
                documentId,
                content
            )
        }
    }

    @Test
    fun `updating content fails when answering radio button group question with unknown option`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                now,
                ChildDocumentCreateRequest(testChild_1.id, templateId)
            )
        val content =
            DocumentContent(answers = listOf(AnsweredQuestion.RadioButtonGroupAnswer("q3", "d")))
        assertThrows<BadRequest> {
            controller.updateDraftDocumentContent(
                dbInstance(),
                employeeUser,
                now,
                documentId,
                content
            )
        }
    }
}
