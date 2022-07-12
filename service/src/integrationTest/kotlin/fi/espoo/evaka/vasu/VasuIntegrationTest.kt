// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testAdult_1
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
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VasuIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val adminUser = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))

    lateinit var daycareTemplate: VasuTemplate
    lateinit var preschoolTemplate: VasuTemplate

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }

        daycareTemplate = let {
            val templateId = postVasuTemplate(
                VasuTemplateController.CreateTemplateRequest(
                    name = "vasu",
                    valid = FiniteDateRange(LocalDate.now(), LocalDate.now().plusYears(1)),
                    type = CurriculumType.DAYCARE,
                    language = VasuLanguage.FI
                )
            )
            putVasuTemplateContent(
                templateId,
                getDefaultVasuContent(VasuLanguage.FI)
            )
            getVasuTemplate(templateId)
        }

        preschoolTemplate = let {
            val templateId = postVasuTemplate(
                VasuTemplateController.CreateTemplateRequest(
                    name = "vasu",
                    valid = FiniteDateRange(LocalDate.now(), LocalDate.now().plusYears(1)),
                    type = CurriculumType.PRESCHOOL,
                    language = VasuLanguage.FI
                )
            )
            putVasuTemplateContent(
                templateId,
                getDefaultLeopsContent()
            )
            getVasuTemplate(templateId)
        }
    }

    private fun getTemplate(type: CurriculumType) = when (type) {
        CurriculumType.DAYCARE -> daycareTemplate
        CurriculumType.PRESCHOOL -> preschoolTemplate
    }

    @Test
    fun `creating new daycare document`() {
        createNewDocument(CurriculumType.DAYCARE)
    }

    @Test
    fun `creating new preschool document`() {
        createNewDocument(CurriculumType.PRESCHOOL)
    }

    @Test
    fun `guardian gives permission to share the document`() {
        val guardian = testAdult_1
        val child = testChild_1
        db.transaction { it.insertGuardian(guardian.id, child.id) }
        val template = getTemplate(CurriculumType.PRESCHOOL)
        val documentId = postVasuDocument(
            child.id,
            VasuController.CreateDocumentRequest(
                templateId = template.id
            )
        )

        val withoutPermission = getVasuDocument(documentId)
        assertEquals(1, withoutPermission.basics.guardians.size)
        assertEquals(false, withoutPermission.basics.guardians[0].hasGivenPermissionToShare)

        givePermissionToShare(documentId, AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.STRONG))
        val withPermission = getVasuDocument(documentId)
        assertEquals(1, withPermission.basics.guardians.size)
        assertEquals(true, withPermission.basics.guardians[0].hasGivenPermissionToShare)
    }

    private fun createNewDocument(type: CurriculumType) {
        val template = getTemplate(type)

        val documentId = postVasuDocument(
            testChild_1.id,
            VasuController.CreateDocumentRequest(
                templateId = template.id
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
                VasuChild(
                    id = testChild_1.id,
                    firstName = testChild_1.firstName,
                    lastName = testChild_1.lastName,
                    dateOfBirth = testChild_1.dateOfBirth
                ),
                doc.basics.child
            )
            assertEquals(0, doc.events.size)
            assertEquals(template.content, doc.content)
        }

        // vasu template cannot be deleted if it has been used
        deleteVasuTemplate(template.id, expectedStatus = 404)
    }

    @Test
    fun `updating daycare document content`() {
        updateDocumentContent(CurriculumType.DAYCARE)
    }

    @Test
    fun `updating preschool document content`() {
        updateDocumentContent(CurriculumType.PRESCHOOL)
    }

    private fun updateDocumentContent(type: CurriculumType) {
        val template = getTemplate(type)
        val childLanguage = when (type) {
            CurriculumType.DAYCARE -> null
            CurriculumType.PRESCHOOL -> ChildLanguage("kiina", "kiina")
        }

        val documentId = postVasuDocument(
            testChild_1.id,
            VasuController.CreateDocumentRequest(
                templateId = template.id
            )
        )

        val content = getVasuDocument(documentId).content
        val updatedContent = content.copy(
            sections = content.sections.dropLast(1) + content.sections.last().copy(
                questions = content.sections.last().questions.map { q ->
                    if (q is VasuQuestion.TextQuestion) {
                        q.copy(
                            value = "hello"
                        )
                    } else q
                }
            )
        )
        assertNotEquals(content, updatedContent)

        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(content = updatedContent, childLanguage = childLanguage)
        )

        getVasuDocument(documentId).let {
            assertEquals(updatedContent, it.content)
        }
    }

    @Test
    fun `daycare document publishing and state transitions`() {
        documentPublishingAndStateTransitions(CurriculumType.DAYCARE)
    }

    @Test
    fun `preschool document publishing and state transitions`() {
        documentPublishingAndStateTransitions(CurriculumType.PRESCHOOL)
    }

    private fun documentPublishingAndStateTransitions(type: CurriculumType) {
        val template = getTemplate(type)
        val childLanguage = when (type) {
            CurriculumType.DAYCARE -> null
            CurriculumType.PRESCHOOL -> ChildLanguage("kiina", "kiina")
        }

        val documentId = postVasuDocument(
            testChild_1.id,
            VasuController.CreateDocumentRequest(
                templateId = template.id
            )
        )
        val content = getVasuDocument(documentId).content

        // first update and publish
        val updatedContent = content.copy(
            sections = listOf(
                content.sections.first().copy(
                    questions = content.sections.first().questions.map { q ->
                        if (q is VasuQuestion.MultiField) {
                            q.copy(value = q.value.map { "primary author" })
                        } else q
                    }
                )
            ) + content.sections.drop(1)
        )
        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(content = updatedContent, childLanguage = childLanguage)
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
            assertEquals(
                listOf(PUBLISHED),
                doc.events.map { it.eventType }
            )
        }

        // vasu discussion and move to ready
        val contentWithUpdatedDiscussion = updatedContent.copy(
            sections = updatedContent.sections.map { section ->
                if (
                    section.name == "Lapsen varhaiskasvatussuunnitelmakeskustelu" ||
                    section.name == "Lapsen esiopetuksen oppimissuunnitelmakeskustelut"
                ) {
                    section.copy(
                        questions = section.questions.map { question ->
                            when (question) {
                                is VasuQuestion.DateQuestion -> question.copy(value = LocalDate.now())
                                is VasuQuestion.TextQuestion -> question.copy(value = "evvk")
                                else -> question
                            }
                        }
                    )
                } else section
            }
        )
        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(content = contentWithUpdatedDiscussion, childLanguage = childLanguage)
        )
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(
                MOVED_TO_READY
            )
        )
        getVasuDocument(documentId).let { doc ->
            assertNotEquals(updatedContent, doc.content)
            assertEquals(contentWithUpdatedDiscussion, doc.content)
            assertEquals(
                listOf(PUBLISHED, PUBLISHED, MOVED_TO_READY),
                doc.events.map { it.eventType }
            )
        }

        // evaluation discussion and move to reviewed
        val contentWithUpdatedEvaluation = updatedContent.copy(
            sections = updatedContent.sections.map { section ->
                if (section.name == "Toteutumisen arviointi" || section.name == "Perusopetukseen siirtyminen") {
                    section.copy(
                        questions = section.questions.map { question ->
                            when (question) {
                                is VasuQuestion.DateQuestion -> question.copy(value = LocalDate.now())
                                is VasuQuestion.TextQuestion -> question.copy(value = "evvk")
                                else -> question
                            }
                        }
                    )
                } else section
            }
        )
        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(content = contentWithUpdatedEvaluation, childLanguage = childLanguage)
        )
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(
                MOVED_TO_REVIEWED
            )
        )
        getVasuDocument(documentId).let { doc ->
            assertNotEquals(contentWithUpdatedDiscussion, doc.content)
            assertEquals(contentWithUpdatedEvaluation, doc.content)
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

        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(content = contentWithUpdatedEvaluation, childLanguage = childLanguage)
        )
        db.read {
            it.getLatestPublishedVasuDocument(documentId).let { doc ->
                assertNotNull(doc)
                assertEquals(contentWithUpdatedEvaluation, doc.content)
            }
        }
    }

    private fun postVasuDocument(childId: ChildId, request: VasuController.CreateDocumentRequest): VasuDocumentId {
        val (_, res, result) = http.post("/children/$childId/vasu")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(adminUser)
            .responseObject<VasuDocumentId>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getVasuSummaries(childId: ChildId): List<VasuDocumentSummary> {
        val (_, res, result) = http.get("/children/$childId/vasu-summaries")
            .asUser(adminUser)
            .responseObject<List<VasuDocumentSummary>>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getVasuDocument(id: VasuDocumentId): VasuDocument {
        val (_, res, result) = http.get("/vasu/$id")
            .asUser(adminUser)
            .responseObject<VasuDocument>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun putVasuDocument(id: VasuDocumentId, request: VasuController.UpdateDocumentRequest) {
        val (_, res, _) = http.put("/vasu/$id")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(adminUser)
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun postVasuDocumentState(id: VasuDocumentId, request: VasuController.ChangeDocumentStateRequest) {
        val (_, res, _) = http.post("/vasu/$id/update-state")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(adminUser)
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun postVasuTemplate(request: VasuTemplateController.CreateTemplateRequest): VasuTemplateId {
        val (_, res, result) = http.post("/vasu/templates")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(adminUser)
            .responseObject<VasuTemplateId>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun putVasuTemplateContent(id: VasuTemplateId, request: VasuContent) {
        val (_, res, _) = http.put("/vasu/templates/$id/content")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(adminUser)
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun getVasuTemplates(): List<VasuTemplateSummary> {
        val (_, res, result) = http.get("/vasu/templates")
            .asUser(adminUser)
            .responseObject<List<VasuTemplateSummary>>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getVasuTemplate(id: VasuTemplateId): VasuTemplate {
        val (_, res, result) = http.get("/vasu/templates/$id")
            .asUser(adminUser)
            .responseObject<VasuTemplate>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun deleteVasuTemplate(id: VasuTemplateId, expectedStatus: Int = 200) {
        val (_, res, _) = http.delete("/vasu/templates/$id")
            .asUser(adminUser)
            .response()

        assertEquals(expectedStatus, res.statusCode)
    }

    private fun givePermissionToShare(id: VasuDocumentId, user: AuthenticatedUser, expectedStatus: Int = 200) {
        val (_, res, _) = http.post("/citizen/vasu/$id/give-permission-to-share")
            .asUser(user)
            .response()

        assertEquals(expectedStatus, res.statusCode)
    }
}
