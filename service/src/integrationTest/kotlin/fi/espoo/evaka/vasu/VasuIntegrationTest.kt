// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.checkAndCreateGroupPlacement
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.vasu.VasuDocumentEventType.MOVED_TO_CLOSED
import fi.espoo.evaka.vasu.VasuDocumentEventType.MOVED_TO_READY
import fi.espoo.evaka.vasu.VasuDocumentEventType.MOVED_TO_REVIEWED
import fi.espoo.evaka.vasu.VasuDocumentEventType.PUBLISHED
import fi.espoo.evaka.vasu.VasuDocumentEventType.RETURNED_TO_READY
import fi.espoo.evaka.vasu.VasuDocumentEventType.RETURNED_TO_REVIEWED
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class VasuIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val adminUser =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))

    @Autowired private lateinit var vasuController: VasuController
    @Autowired private lateinit var vasuControllerCitizen: VasuControllerCitizen
    @Autowired private lateinit var vasuTemplateController: VasuTemplateController

    lateinit var daycareTemplate: VasuTemplate
    lateinit var preschoolTemplate: VasuTemplate

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }

        daycareTemplate = let {
            val templateId =
                postVasuTemplate(
                    VasuTemplateController.CreateTemplateRequest(
                        name = "vasu",
                        valid = FiniteDateRange(LocalDate.now(), LocalDate.now().plusYears(1)),
                        type = CurriculumType.DAYCARE,
                        language = VasuLanguage.FI
                    )
                )
            putVasuTemplateContent(templateId, getDefaultVasuContent(VasuLanguage.FI))
            getVasuTemplate(templateId)
        }

        preschoolTemplate = let {
            val templateId =
                postVasuTemplate(
                    VasuTemplateController.CreateTemplateRequest(
                        name = "vasu",
                        valid = FiniteDateRange(LocalDate.now(), LocalDate.now().plusYears(1)),
                        type = CurriculumType.PRESCHOOL,
                        language = VasuLanguage.FI
                    )
                )
            putVasuTemplateContent(templateId, getDefaultLeopsContent(VasuLanguage.FI))
            getVasuTemplate(templateId)
        }
    }

    private fun getTemplate(type: CurriculumType) =
        when (type) {
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
        val documentId =
            postVasuDocument(
                child.id,
                VasuController.CreateDocumentRequest(templateId = template.id)
            )

        val withoutPermission = getVasuDocument(documentId)
        assertEquals(1, withoutPermission.basics.guardians.size)
        assertEquals(false, withoutPermission.basics.guardians[0].hasGivenPermissionToShare)

        givePermissionToShare(
            documentId,
            AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.STRONG)
        )
        val withPermission = getVasuDocument(documentId)
        assertEquals(1, withPermission.basics.guardians.size)
        assertEquals(true, withPermission.basics.guardians[0].hasGivenPermissionToShare)
    }

    private fun createNewDocument(type: CurriculumType) {
        val template = getTemplate(type)

        val documentId =
            postVasuDocument(
                testChild_1.id,
                VasuController.CreateDocumentRequest(templateId = template.id)
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
        assertThrows<NotFound> { deleteVasuTemplate(template.id) }
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
        val childLanguage =
            when (type) {
                CurriculumType.DAYCARE -> null
                CurriculumType.PRESCHOOL -> ChildLanguage("kiina", "kiina")
            }

        val documentId =
            postVasuDocument(
                testChild_1.id,
                VasuController.CreateDocumentRequest(templateId = template.id)
            )

        val content = getVasuDocument(documentId).content
        val updatedContent =
            content.copy(
                sections =
                    content.sections.dropLast(1) +
                        content.sections
                            .last()
                            .copy(
                                questions =
                                    content.sections.last().questions.map { q ->
                                        if (q is VasuQuestion.TextQuestion) {
                                            q.copy(value = "hello")
                                        } else {
                                            q
                                        }
                                    }
                            )
            )
        assertNotEquals(content, updatedContent)

        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(
                content = updatedContent,
                childLanguage = childLanguage
            )
        )

        getVasuDocument(documentId).let { assertEquals(updatedContent, it.content) }
    }

    @Test
    fun `daycare document publishing and state transitions`() {
        documentPublishingAndStateTransitions(CurriculumType.DAYCARE)
    }

    @Test
    fun `preschool document publishing and state transitions`() {
        documentPublishingAndStateTransitions(CurriculumType.PRESCHOOL)
    }

    @Test
    fun `daycare document shows all placements even when the child moves from one group to another`() {
        val template = getTemplate(CurriculumType.DAYCARE)
        val documentId =
            postVasuDocument(
                testChild_1.id,
                VasuController.CreateDocumentRequest(templateId = template.id)
            )

        val (groupId1, groupId2) =
            db.transaction { tx ->
                val testDaycareGroup =
                    DevDaycareGroup(daycareId = testDaycare.id, name = "first group")
                val testDaycareGroup2 =
                    DevDaycareGroup(daycareId = testDaycare.id, name = "second group")
                val placementStart = LocalDate.now()
                val placementEnd = placementStart.plusMonths(9)
                val placementId =
                    tx.insertTestPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd
                    )
                val groupId1 = tx.insertTestDaycareGroup(testDaycareGroup)
                tx.checkAndCreateGroupPlacement(
                    daycarePlacementId = placementId,
                    groupId = groupId1,
                    startDate = placementStart,
                    endDate = placementStart.plusMonths(1)
                )

                val groupId2 = tx.insertTestDaycareGroup(testDaycareGroup2)
                tx.checkAndCreateGroupPlacement(
                    daycarePlacementId = placementId,
                    groupId = groupId2,
                    startDate = placementStart.plusMonths(1).plusDays(1),
                    endDate = placementEnd
                )
                Pair(groupId1, groupId2)
            }

        val basics = getVasuDocument(documentId).basics
        assertEquals(2, basics.placements?.size)
        assertEquals(groupId1, basics.placements?.first()?.groupId)
        assertEquals(groupId2, basics.placements?.last()?.groupId)
    }

    @Test
    fun `daycare document shows all placements even when the child moves from one unit to another`() {
        val template = getTemplate(CurriculumType.DAYCARE)
        val documentId =
            postVasuDocument(
                testChild_1.id,
                VasuController.CreateDocumentRequest(templateId = template.id)
            )

        db.transaction { tx ->
            val testDaycareGroup = DevDaycareGroup(daycareId = testDaycare.id, name = "first group")
            val testDaycareGroup2 =
                DevDaycareGroup(daycareId = testDaycare2.id, name = "second group")
            var placementStart = LocalDate.now()
            var placementEnd = placementStart.plusMonths(1)
            val placementId1 =
                tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            val groupId1 = tx.insertTestDaycareGroup(testDaycareGroup)
            tx.checkAndCreateGroupPlacement(
                daycarePlacementId = placementId1,
                groupId = groupId1,
                startDate = placementStart,
                endDate = placementEnd
            )

            placementStart = placementEnd.plusDays(1)
            placementEnd = placementStart.plusMonths(1)
            val placementId2 =
                tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            val groupId2 = tx.insertTestDaycareGroup(testDaycareGroup2)
            tx.checkAndCreateGroupPlacement(
                daycarePlacementId = placementId2,
                groupId = groupId2,
                startDate = placementStart,
                endDate = placementEnd
            )
        }

        val basics = getVasuDocument(documentId).basics
        assertEquals(2, basics.placements?.size)
        assertEquals(testDaycare.id, basics.placements?.first()?.unitId)
        assertEquals(testDaycare2.id, basics.placements?.last()?.unitId)
    }

    private fun documentPublishingAndStateTransitions(type: CurriculumType) {
        val template = getTemplate(type)
        val childLanguage =
            when (type) {
                CurriculumType.DAYCARE -> null
                CurriculumType.PRESCHOOL -> ChildLanguage("kiina", "kiina")
            }

        val duplicateId =
            db.transaction { tx ->
                tx.insertTestPerson(
                    testChild_1.copy(
                        id = PersonId(UUID.randomUUID()),
                        ssn = null,
                        duplicateOf = testChild_1.id
                    )
                )
            }
        val documentId =
            postVasuDocument(
                testChild_1.id,
                VasuController.CreateDocumentRequest(templateId = template.id)
            )
        val content = getVasuDocument(documentId).content

        // first update and publish
        val updatedContent =
            content.copy(
                sections =
                    listOf(
                        content.sections
                            .first()
                            .copy(
                                questions =
                                    content.sections.first().questions.map { q ->
                                        if (q is VasuQuestion.MultiField) {
                                            q.copy(value = q.value.map { "primary author" })
                                        } else {
                                            q
                                        }
                                    }
                            )
                    ) + content.sections.drop(1)
            )
        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(
                content = updatedContent,
                childLanguage = childLanguage
            )
        )
        assertNull(db.read { it.getLatestPublishedVasuDocument(documentId) })
        postVasuDocumentState(documentId, VasuController.ChangeDocumentStateRequest(PUBLISHED))
        assertNotNull(db.read { it.getLatestPublishedVasuDocument(documentId) })
        getVasuDocument(documentId).let { doc ->
            assertEquals(listOf(PUBLISHED), doc.events.map { it.eventType })
        }

        // vasu discussion and move to ready
        val contentWithUpdatedDiscussion =
            updatedContent.copy(
                sections =
                    updatedContent.sections.map { section ->
                        if (
                            section.name == "Lapsen varhaiskasvatussuunnitelmakeskustelu" ||
                                section.name == "Lapsen esiopetuksen oppimissuunnitelmakeskustelu"
                        ) {
                            section.copy(
                                questions =
                                    section.questions.map { question ->
                                        when (question) {
                                            is VasuQuestion.DateQuestion ->
                                                question.copy(value = LocalDate.now())
                                            is VasuQuestion.TextQuestion ->
                                                question.copy(value = "evvk")
                                            else -> question
                                        }
                                    }
                            )
                        } else {
                            section
                        }
                    }
            )
        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(
                content = contentWithUpdatedDiscussion,
                childLanguage = childLanguage
            )
        )
        postVasuDocumentState(documentId, VasuController.ChangeDocumentStateRequest(MOVED_TO_READY))
        getVasuDocument(documentId).let { doc ->
            assertNotEquals(updatedContent, doc.content)
            assertEquals(contentWithUpdatedDiscussion, doc.content)
            assertEquals(
                listOf(PUBLISHED, PUBLISHED, MOVED_TO_READY),
                doc.events.map { it.eventType }
            )
        }

        // evaluation discussion and move to reviewed
        val contentWithUpdatedEvaluation =
            updatedContent.copy(
                sections =
                    updatedContent.sections.map { section ->
                        if (
                            section.name == "Toteutumisen arviointi" ||
                                section.name == "Perusopetukseen siirtyminen"
                        ) {
                            section.copy(
                                questions =
                                    section.questions.map { question ->
                                        when (question) {
                                            is VasuQuestion.DateQuestion ->
                                                question.copy(value = LocalDate.now())
                                            is VasuQuestion.TextQuestion ->
                                                question.copy(value = "evvk")
                                            else -> question
                                        }
                                    }
                            )
                        } else {
                            section
                        }
                    }
            )
        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(
                content = contentWithUpdatedEvaluation,
                childLanguage = childLanguage
            )
        )
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(MOVED_TO_REVIEWED)
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
            VasuController.ChangeDocumentStateRequest(MOVED_TO_CLOSED)
        )
        db.read { tx ->
            val original = tx.getVasuDocumentMaster(documentId)
            val summaries = tx.getVasuDocumentSummaries(duplicateId)
            assertThat(summaries).hasSize(1)
            val duplicate = tx.getVasuDocumentMaster(summaries[0].id)
            assertThat(duplicate)
                .usingRecursiveComparison()
                .ignoringFields(
                    "id",
                    "basics.child.id",
                    "events.id",
                    "events.created",
                    "modifiedAt"
                )
                .isEqualTo(original)
        }
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(RETURNED_TO_REVIEWED)
        )
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(RETURNED_TO_READY)
        )
        assertEquals(
            listOf(
                PUBLISHED,
                PUBLISHED,
                MOVED_TO_READY,
                PUBLISHED,
                MOVED_TO_REVIEWED,
                MOVED_TO_CLOSED,
                RETURNED_TO_REVIEWED,
                RETURNED_TO_READY
            ),
            getVasuDocument(documentId).events.map { it.eventType }
        )

        putVasuDocument(
            documentId,
            VasuController.UpdateDocumentRequest(
                content = contentWithUpdatedEvaluation,
                childLanguage = childLanguage
            )
        )
        db.read {
            it.getLatestPublishedVasuDocument(documentId).let { doc ->
                assertNotNull(doc)
                assertEquals(contentWithUpdatedEvaluation, doc.content)
            }
        }
    }

    private fun postVasuDocument(
        childId: ChildId,
        request: VasuController.CreateDocumentRequest
    ): VasuDocumentId {
        return vasuController.createDocument(
            dbInstance(),
            adminUser,
            RealEvakaClock(),
            childId,
            request
        )
    }

    private fun getVasuSummaries(childId: ChildId): List<VasuDocumentSummary> {
        return vasuController
            .getVasuSummariesByChild(dbInstance(), adminUser, RealEvakaClock(), childId)
            .map { it.data }
    }

    private fun getVasuDocument(id: VasuDocumentId): VasuDocument {
        return vasuController.getDocument(dbInstance(), adminUser, RealEvakaClock(), id).data
    }

    private fun putVasuDocument(id: VasuDocumentId, request: VasuController.UpdateDocumentRequest) {
        vasuController.putDocument(dbInstance(), adminUser, RealEvakaClock(), id, request)
    }

    private fun postVasuDocumentState(
        id: VasuDocumentId,
        request: VasuController.ChangeDocumentStateRequest
    ) {
        vasuController.updateDocumentState(dbInstance(), adminUser, RealEvakaClock(), id, request)
    }

    private fun postVasuTemplate(
        request: VasuTemplateController.CreateTemplateRequest
    ): VasuTemplateId {
        return vasuTemplateController.postTemplate(
            dbInstance(),
            adminUser,
            RealEvakaClock(),
            request
        )
    }

    private fun putVasuTemplateContent(id: VasuTemplateId, request: VasuContent) {
        vasuTemplateController.putTemplateContent(
            dbInstance(),
            adminUser,
            RealEvakaClock(),
            id,
            request
        )
    }

    private fun getVasuTemplate(id: VasuTemplateId): VasuTemplate {
        return vasuTemplateController.getTemplate(dbInstance(), adminUser, RealEvakaClock(), id)
    }

    private fun deleteVasuTemplate(id: VasuTemplateId) {
        vasuTemplateController.deleteTemplate(dbInstance(), adminUser, RealEvakaClock(), id)
    }

    private fun givePermissionToShare(
        id: VasuDocumentId,
        user: AuthenticatedUser.Citizen,
    ) {
        vasuControllerCitizen.givePermissionToShare(dbInstance(), user, RealEvakaClock(), id)
    }
}
