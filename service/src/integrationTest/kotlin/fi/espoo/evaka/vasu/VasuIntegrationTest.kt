// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.vasu.VasuDocumentEventType.MOVED_TO_CLOSED
import fi.espoo.evaka.vasu.VasuDocumentEventType.MOVED_TO_READY
import fi.espoo.evaka.vasu.VasuDocumentEventType.MOVED_TO_REVIEWED
import fi.espoo.evaka.vasu.VasuDocumentEventType.PUBLISHED
import fi.espoo.evaka.vasu.VasuDocumentEventType.RETURNED_TO_READY
import fi.espoo.evaka.vasu.VasuDocumentEventType.RETURNED_TO_REVIEWED
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VasuIntegrationTest : FullApplicationTest() {
    private val adminUser = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))

    lateinit var templateId: UUID
    lateinit var template: VasuTemplate

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }

        postVasuTemplate(
            VasuTemplateController.CreateTemplateRequest(
                name = "vasu",
                valid = FiniteDateRange(LocalDate.now(), LocalDate.now().plusYears(1)),
                language = VasuLanguage.FI
            )
        )

        templateId = getVasuTemplates().first().id
        template = getVasuTemplate(templateId)
    }

    @Test
    fun `creating new document`() {
        val documentId = postVasuDocument(
            VasuController.CreateDocumentRequest(
                childId = testChild_1.id,
                templateId = templateId
            )
        )

        getVasuSummaries(testChild_1.id)
            .also { assertEquals(1, it.size) }
            .first()
            .let { summary ->
                assertEquals(documentId, summary.id)
                assertEquals("vasu", summary.name)
                assertEquals(0, summary.events.size)
            }

        getVasuDocument(documentId).let { doc ->
            assertEquals("vasu", doc.templateName)
            assertEquals(
                VasuDocumentChild(
                    id = testChild_1.id,
                    firstName = testChild_1.firstName,
                    lastName = testChild_1.lastName
                ),
                doc.child
            )
            assertEquals(0, doc.events.size)
            assertEquals(template.content, doc.content)
            assertEquals(
                AuthorsContent(
                    primaryAuthor = AuthorInfo(),
                    otherAuthors = listOf(AuthorInfo())
                ),
                doc.authorsContent
            )
            assertEquals(VasuDiscussionContent(), doc.vasuDiscussionContent)
            assertEquals(EvaluationDiscussionContent(), doc.evaluationDiscussionContent)
        }

        // vasu template cannot be deleted if it has been used
        deleteVasuTemplate(templateId, expectedStatus = 404)
    }

    @Test
    fun `updating document content and authors`() {
        val documentId = postVasuDocument(
            VasuController.CreateDocumentRequest(
                childId = testChild_1.id,
                templateId = templateId
            )
        )

        val content = getVasuDocument(documentId).content
        val updatedContent = content.copy(
            sections = content.sections.dropLast(1) + content.sections.last().copy(
                questions = content.sections.last().questions.map { q ->
                    if (q is VasuQuestion.MultiSelectQuestion) {
                        q.copy(
                            value = q.options.map { it.key }
                        )
                    } else q
                }
            )
        )
        assertNotEquals(content, updatedContent)

        val updatedAuthors = AuthorsContent(
            primaryAuthor = AuthorInfo(name = "foo"),
            otherAuthors = listOf(AuthorInfo(name = "bar", phone = "555"))
        )

        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(
                content = updatedContent,
                authorsContent = updatedAuthors,
                vasuDiscussionContent = VasuDiscussionContent(),
                evaluationDiscussionContent = EvaluationDiscussionContent()
            )
        )

        getVasuDocument(documentId).let {
            assertEquals(updatedContent, it.content)
            assertEquals(updatedAuthors, it.authorsContent)
        }
    }

    @Test
    fun `publishing and state transitions`() {
        val documentId = postVasuDocument(
            VasuController.CreateDocumentRequest(
                childId = testChild_1.id,
                templateId = templateId
            )
        )
        val content = getVasuDocument(documentId).content

        // first update and publish
        val updatedAuthors = AuthorsContent(
            primaryAuthor = AuthorInfo(name = "foo"),
            otherAuthors = listOf(AuthorInfo(name = "bar", phone = "555"))
        )
        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(
                content = content,
                authorsContent = updatedAuthors,
                vasuDiscussionContent = VasuDiscussionContent(),
                evaluationDiscussionContent = EvaluationDiscussionContent()
            )
        )
        assertNull(db.read { it.getLatestPublishedVasuDocument(documentId) })
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(
                PUBLISHED
            )
        )
        assertNotNull(db.read { it.getLatestPublishedVasuDocument(documentId) })
        getVasuDocument(documentId).let { doc ->
            assertEquals(updatedAuthors, doc.authorsContent)
            assertEquals(
                listOf(PUBLISHED),
                doc.events.map { it.eventType }
            )
        }

        // vasu discussion and move to ready
        val vasuDiscussionContent = VasuDiscussionContent(
            discussionDate = LocalDate.now(),
            participants = "matti ja teppo",
            guardianViewsAndCollaboration = "evvk"
        )
        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(
                content = content,
                authorsContent = updatedAuthors,
                vasuDiscussionContent = vasuDiscussionContent,
                evaluationDiscussionContent = EvaluationDiscussionContent()
            )
        )
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(
                MOVED_TO_READY
            )
        )
        getVasuDocument(documentId).let { doc ->
            assertEquals(vasuDiscussionContent, doc.vasuDiscussionContent)
            assertEquals(
                listOf(PUBLISHED, PUBLISHED, MOVED_TO_READY),
                doc.events.map { it.eventType }
            )
        }

        // evaluation discussion and move to reviewed
        val evaluationDiscussionContent = EvaluationDiscussionContent(
            discussionDate = LocalDate.now(),
            participants = "matti ja teppo",
            guardianViewsAndCollaboration = "evvk",
            evaluation = "wow such great"
        )
        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(
                content = content,
                authorsContent = updatedAuthors,
                vasuDiscussionContent = vasuDiscussionContent,
                evaluationDiscussionContent = evaluationDiscussionContent
            )
        )
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(
                MOVED_TO_REVIEWED
            )
        )
        getVasuDocument(documentId).let { doc ->
            assertEquals(evaluationDiscussionContent, doc.evaluationDiscussionContent)
            assertEquals(
                listOf(PUBLISHED, PUBLISHED, MOVED_TO_READY, PUBLISHED, MOVED_TO_REVIEWED),
                doc.events.map { it.eventType }
            )
        }

        // other transitions
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(
                MOVED_TO_CLOSED
            )
        )
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(
                RETURNED_TO_REVIEWED
            )
        )
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(
                RETURNED_TO_READY
            )
        )
        assertEquals(
            listOf(PUBLISHED, PUBLISHED, MOVED_TO_READY, PUBLISHED, MOVED_TO_REVIEWED, MOVED_TO_CLOSED, RETURNED_TO_REVIEWED, RETURNED_TO_READY),
            getVasuDocument(documentId).events.map { it.eventType }
        )

        // verify the last published version
        val authorsAfterLastPublish = AuthorsContent(
            primaryAuthor = AuthorInfo(name = "new author"),
            otherAuthors = emptyList()
        )
        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(
                content = content,
                authorsContent = authorsAfterLastPublish,
                vasuDiscussionContent = vasuDiscussionContent,
                evaluationDiscussionContent = evaluationDiscussionContent
            )
        )
        assertEquals(authorsAfterLastPublish, getVasuDocument(documentId).authorsContent)
        db.read {
            it.getLatestPublishedVasuDocument(documentId).let { doc ->
                assertNotNull(doc)
                assertEquals(content, doc.content)
                assertEquals(updatedAuthors, doc.authorsContent)
                assertEquals(vasuDiscussionContent, doc.vasuDiscussionContent)
                assertEquals(evaluationDiscussionContent, doc.evaluationDiscussionContent)
            }
        }
    }

    private fun postVasuDocument(request: VasuController.CreateDocumentRequest): UUID {
        val (_, res, result) = http.post("/vasu")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(adminUser)
            .responseObject<UUID>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getVasuSummaries(childId: UUID): List<VasuDocumentSummary> {
        val (_, res, result) = http.get("/children/$childId/vasu-summaries")
            .asUser(adminUser)
            .responseObject<List<VasuDocumentSummary>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getVasuDocument(id: UUID): VasuDocument {
        val (_, res, result) = http.get("/vasu/$id")
            .asUser(adminUser)
            .responseObject<VasuDocument>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun putVasuDocument(id: UUID, request: VasuController.UpdateDocumentRequest) {
        val (_, res, _) = http.put("/vasu/$id")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(adminUser)
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun postVasuDocumentState(id: UUID, request: VasuController.ChangeDocumentStateRequest) {
        val (_, res, _) = http.post("/vasu/$id/update-state")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(adminUser)
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun postVasuTemplate(request: VasuTemplateController.CreateTemplateRequest) {
        val (_, res, _) = http.post("/vasu/templates")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(adminUser)
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun getVasuTemplates(): List<VasuTemplateSummary> {
        val (_, res, result) = http.get("/vasu/templates")
            .asUser(adminUser)
            .responseObject<List<VasuTemplateSummary>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getVasuTemplate(id: UUID): VasuTemplate {
        val (_, res, result) = http.get("/vasu/templates/$id")
            .asUser(adminUser)
            .responseObject<VasuTemplate>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun deleteVasuTemplate(id: UUID, expectedStatus: Int = 200) {
        val (_, res, _) = http.delete("/vasu/templates/$id")
            .asUser(adminUser)
            .response()

        assertEquals(expectedStatus, res.statusCode)
    }
}
