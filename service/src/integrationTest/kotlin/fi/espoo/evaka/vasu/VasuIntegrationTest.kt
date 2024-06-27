// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.checkAndCreateGroupPlacement
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.shared.job.ScheduledJob
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
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
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private lateinit var daycareTemplate: VasuTemplate
    private lateinit var preschoolTemplate: VasuTemplate

    private val mockToday: LocalDate = LocalDate.of(2023, 5, 22)
    private lateinit var clock: MockEvakaClock

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            listOf(testChild_1, testChild_2, testChild_3, testChild_4).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
        }
        clock = MockEvakaClock(HelsinkiDateTime.of(mockToday, LocalTime.of(12, 0)))

        daycareTemplate = let {
            val templateId =
                postVasuTemplate(
                    VasuTemplateController.CreateTemplateRequest(
                        name = "vasu",
                        valid = FiniteDateRange(mockToday.minusMonths(2), mockToday.plusYears(1)),
                        type = CurriculumType.DAYCARE,
                        language = OfficialLanguage.FI
                    )
                )
            putVasuTemplateContent(templateId, getDefaultVasuContent(OfficialLanguage.FI))
            getVasuTemplate(templateId)
        }

        preschoolTemplate = let {
            val templateId =
                postVasuTemplate(
                    VasuTemplateController.CreateTemplateRequest(
                        name = "vasu",
                        valid = FiniteDateRange(mockToday, mockToday.plusYears(1)),
                        type = CurriculumType.PRESCHOOL,
                        language = OfficialLanguage.FI
                    )
                )
            putVasuTemplateContent(templateId, getDefaultLeopsContent(OfficialLanguage.FI))
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

        assertEquals(updatedContent, getVasuDocument(documentId).content)
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
    fun `draft document can be deleted`() {
        val template = getTemplate(CurriculumType.DAYCARE)
        val documentId =
            postVasuDocument(
                testChild_1.id,
                VasuController.CreateDocumentRequest(templateId = template.id)
            )

        deleteVasuDocument(documentId)

        assertThrows<NotFound> { getVasuDocument(documentId) }
    }

    @Test
    fun `published document cannot be deleted`() {
        val template = getTemplate(CurriculumType.DAYCARE)
        val documentId =
            postVasuDocument(
                testChild_1.id,
                VasuController.CreateDocumentRequest(templateId = template.id)
            )
        postVasuDocumentState(documentId, VasuController.ChangeDocumentStateRequest(MOVED_TO_READY))

        assertThrows<Conflict> { deleteVasuDocument(documentId) }
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
                val testDaycareGroup3 =
                    DevDaycareGroup(daycareId = testDaycare.id, name = "third group")
                val placementStart = mockToday.minusMonths(2)
                val placementEnd = placementStart.plusMonths(9)
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = testChild_1.id,
                            unitId = testDaycare.id,
                            startDate = placementStart,
                            endDate = placementEnd
                        )
                    )
                val groupId1 = tx.insert(testDaycareGroup)
                tx.checkAndCreateGroupPlacement(
                    daycarePlacementId = placementId,
                    groupId = groupId1,
                    startDate = placementStart,
                    endDate = placementStart.plusMonths(1)
                )

                val groupId2 = tx.insert(testDaycareGroup2)
                tx.checkAndCreateGroupPlacement(
                    daycarePlacementId = placementId,
                    groupId = groupId2,
                    startDate = placementStart.plusMonths(1).plusDays(1),
                    endDate = placementStart.plusMonths(3),
                )

                // This group placement is not shown in the document because it starts in the future
                val groupId3 = tx.insert(testDaycareGroup3)
                tx.checkAndCreateGroupPlacement(
                    daycarePlacementId = placementId,
                    groupId = groupId3,
                    startDate = placementStart.plusMonths(3).plusDays(1),
                    endDate = placementEnd,
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
            var placementStart = mockToday.minusMonths(1)
            var placementEnd = mockToday.minusDays(1)
            val placementId1 =
                tx.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd
                    )
                )
            val groupId1 = tx.insert(testDaycareGroup)
            tx.checkAndCreateGroupPlacement(
                daycarePlacementId = placementId1,
                groupId = groupId1,
                startDate = placementStart,
                endDate = placementEnd
            )

            placementStart = mockToday
            val placementMiddle = mockToday.plusMonths(1)
            placementEnd = mockToday.plusMonths(2)
            val placementId2 =
                tx.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare2.id,
                        startDate = placementStart,
                        endDate = placementEnd
                    )
                )
            val groupId2 = tx.insert(testDaycareGroup2)
            tx.checkAndCreateGroupPlacement(
                daycarePlacementId = placementId2,
                groupId = groupId2,
                startDate = placementStart,
                endDate = placementMiddle
            )

            // This group placement is not included because it starts in the future
            tx.checkAndCreateGroupPlacement(
                daycarePlacementId = placementId2,
                groupId = groupId2,
                startDate = placementMiddle.plusDays(1),
                endDate = placementEnd
            )
        }

        val basics = getVasuDocument(documentId).basics
        assertEquals(2, basics.placements?.size)
        assertEquals(testDaycare.id, basics.placements?.first()?.unitId)
        assertEquals(testDaycare2.id, basics.placements?.last()?.unitId)
    }

    @Test
    fun `vasu documents with an expired template are closed by a scheduled job`() {
        val draftDocumentId =
            postVasuDocument(
                testChild_1.id,
                VasuController.CreateDocumentRequest(templateId = daycareTemplate.id)
            )
        val readyDocumentId =
            postVasuDocument(
                testChild_2.id,
                VasuController.CreateDocumentRequest(templateId = preschoolTemplate.id)
            )
        postVasuDocumentState(
            readyDocumentId,
            VasuController.ChangeDocumentStateRequest(MOVED_TO_READY)
        )
        val closedDocumentId =
            postVasuDocument(
                testChild_3.id,
                VasuController.CreateDocumentRequest(templateId = daycareTemplate.id)
            )
        postVasuDocumentState(
            closedDocumentId,
            VasuController.ChangeDocumentStateRequest(MOVED_TO_CLOSED)
        )

        // The new template is valid on this date
        val futureClock =
            MockEvakaClock(
                HelsinkiDateTime.of(mockToday.plusYears(1).plusDays(1), LocalTime.of(12, 0))
            )

        val nonExpiredTemplateId =
            postVasuTemplate(
                VasuTemplateController.CreateTemplateRequest(
                    name = "vasu",
                    valid = FiniteDateRange(mockToday.plusYears(1), mockToday.plusYears(2)),
                    type = CurriculumType.DAYCARE,
                    language = OfficialLanguage.FI
                )
            )
        putVasuTemplateContent(nonExpiredTemplateId, getDefaultVasuContent(OfficialLanguage.FI))

        val nonExpiredDocumentId =
            postVasuDocument(
                testChild_4.id,
                VasuController.CreateDocumentRequest(templateId = nonExpiredTemplateId),
                futureClock,
            )

        db.transaction { tx ->
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.RunScheduledJob(ScheduledJob.CloseVasusWithExpiredTemplate.name)),
                runAt = futureClock.now(),
            )
        }
        asyncJobRunner.runPendingJobsSync(futureClock)

        getVasuDocument(draftDocumentId).let { doc ->
            // MOVED_TO_CLOSED is added by the system internal user in the async job
            assertEquals(
                listOf(MOVED_TO_CLOSED to AuthenticatedUser.SystemInternalUser.evakaUserId),
                doc.events.map { it.eventType to it.createdBy }
            )
        }
        getVasuDocument(readyDocumentId).let { doc ->
            // PUBLISHED is added implicitly when MOVED_TO_READY is added
            assertEquals(
                listOf(PUBLISHED, MOVED_TO_READY, MOVED_TO_CLOSED),
                doc.events.map { it.eventType }
            )
        }
        getVasuDocument(closedDocumentId).let { doc ->
            assertEquals(
                listOf(MOVED_TO_CLOSED to adminUser.evakaUserId),
                doc.events.map { it.eventType to it.createdBy }
            )
        }
        getVasuDocument(nonExpiredDocumentId).let { doc ->
            // Template is valid –> not closed
            assertEquals(VasuDocumentState.DRAFT, doc.documentState)
            assertEquals(emptyList(), doc.events)
        }
    }

    @Test
    fun `unit supervisor doesn't see daycare document from duplicate`() {
        val unitSupervisor =
            db.transaction { tx ->
                val unitId =
                    tx.insert(
                        DevDaycare(
                            areaId = testArea.id,
                            enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC)
                        )
                    )
                val employeeId = tx.insert(DevEmployee())
                tx.insertDaycareAclRow(
                    daycareId = unitId,
                    employeeId = employeeId,
                    role = UserRole.UNIT_SUPERVISOR
                )
                tx.insert(
                    DevPlacement(
                        unitId = unitId,
                        childId = testChild_1.id,
                        startDate = clock.today(),
                        endDate = clock.today().plusDays(5)
                    )
                )
                AuthenticatedUser.Employee(employeeId, setOf(UserRole.UNIT_SUPERVISOR))
            }
        val duplicateId =
            db.transaction { tx ->
                tx.insert(DevPerson().copy(duplicateOf = testChild_1.id), DevPersonType.CHILD)
            }
        val documentId =
            vasuController.createDocument(
                dbInstance(),
                adminUser,
                clock,
                duplicateId,
                VasuController.CreateDocumentRequest(templateId = daycareTemplate.id)
            )
        assertThrows<Forbidden> {
            vasuController.getDocument(dbInstance(), unitSupervisor, clock, documentId)
        }
    }

    @Test
    fun `unit supervisor sees latest daycare document from duplicate of`() {
        val unitSupervisor =
            db.transaction { tx ->
                val unitId =
                    tx.insert(
                        DevDaycare(
                            areaId = testArea.id,
                            enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC)
                        )
                    )
                val employeeId = tx.insert(DevEmployee())
                tx.insertDaycareAclRow(
                    daycareId = unitId,
                    employeeId = employeeId,
                    role = UserRole.UNIT_SUPERVISOR
                )
                val childId =
                    tx.insert(DevPerson().copy(duplicateOf = testChild_1.id), DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        unitId = unitId,
                        childId = childId,
                        startDate = clock.today(),
                        endDate = clock.today().plusDays(5)
                    )
                )
                val unitSupervisor =
                    AuthenticatedUser.Employee(employeeId, setOf(UserRole.UNIT_SUPERVISOR))
                unitSupervisor
            }
        val document1Id =
            vasuController.createDocument(
                dbInstance(),
                adminUser,
                clock,
                testChild_1.id,
                VasuController.CreateDocumentRequest(templateId = daycareTemplate.id)
            )
        vasuController.updateDocumentState(
            dbInstance(),
            adminUser,
            clock,
            document1Id,
            VasuController.ChangeDocumentStateRequest(MOVED_TO_CLOSED)
        )
        clock.tick()
        val document2Id =
            vasuController.createDocument(
                dbInstance(),
                adminUser,
                clock,
                testChild_1.id,
                VasuController.CreateDocumentRequest(templateId = daycareTemplate.id)
            )
        assertNotNull(vasuController.getDocument(dbInstance(), unitSupervisor, clock, document2Id))
        assertThrows<Forbidden> {
            vasuController.getDocument(dbInstance(), unitSupervisor, clock, document1Id)
        }
    }

    @Test
    fun `unit supervisor sees preschool document from duplicate`() {
        val unitSupervisor =
            db.transaction { tx ->
                val unitId =
                    tx.insert(
                        DevDaycare(
                            areaId = testArea.id,
                            enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC)
                        )
                    )
                val employeeId = tx.insert(DevEmployee())
                tx.insertDaycareAclRow(
                    daycareId = unitId,
                    employeeId = employeeId,
                    role = UserRole.UNIT_SUPERVISOR
                )
                tx.insert(
                    DevPlacement(
                        unitId = unitId,
                        childId = testChild_1.id,
                        startDate = clock.today(),
                        endDate = clock.today().plusDays(5)
                    )
                )
                AuthenticatedUser.Employee(employeeId, setOf(UserRole.UNIT_SUPERVISOR))
            }
        val duplicateId =
            db.transaction { tx ->
                tx.insert(DevPerson().copy(duplicateOf = testChild_1.id), DevPersonType.CHILD)
            }
        val documentId =
            vasuController.createDocument(
                dbInstance(),
                adminUser,
                clock,
                duplicateId,
                VasuController.CreateDocumentRequest(templateId = preschoolTemplate.id)
            )
        assertNotNull(vasuController.getDocument(dbInstance(), unitSupervisor, clock, documentId))
    }

    @Test
    fun `unit supervisor doesn't see preschool document from duplicate of`() {
        val unitSupervisor =
            db.transaction { tx ->
                val unitId =
                    tx.insert(
                        DevDaycare(
                            areaId = testArea.id,
                            enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC)
                        )
                    )
                val employeeId = tx.insert(DevEmployee())
                tx.insertDaycareAclRow(
                    daycareId = unitId,
                    employeeId = employeeId,
                    role = UserRole.UNIT_SUPERVISOR
                )
                val childId =
                    tx.insert(DevPerson().copy(duplicateOf = testChild_1.id), DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        unitId = unitId,
                        childId = childId,
                        startDate = clock.today(),
                        endDate = clock.today().plusDays(5)
                    )
                )
                AuthenticatedUser.Employee(employeeId, setOf(UserRole.UNIT_SUPERVISOR))
            }
        val documentId =
            vasuController.createDocument(
                dbInstance(),
                adminUser,
                clock,
                testChild_1.id,
                VasuController.CreateDocumentRequest(templateId = preschoolTemplate.id)
            )
        assertThrows<Forbidden> {
            vasuController.getDocument(dbInstance(), unitSupervisor, clock, documentId)
        }
    }

    private fun documentPublishingAndStateTransitions(type: CurriculumType) {
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
        assertNull(db.read { it.getLatestPublishedVasuDocument(mockToday, documentId) })

        clock.tick()
        postVasuDocumentState(documentId, VasuController.ChangeDocumentStateRequest(PUBLISHED))
        assertNotNull(db.read { it.getLatestPublishedVasuDocument(mockToday, documentId) })
        assertEquals(listOf(PUBLISHED), getVasuDocument(documentId).events.map { it.eventType })

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
                                                question.copy(value = mockToday)
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

        clock.tick()
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
                                                question.copy(value = mockToday)
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

        clock.tick()
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
        clock.tick()
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(MOVED_TO_CLOSED)
        )
        postVasuDocumentState(
            documentId,
            VasuController.ChangeDocumentStateRequest(RETURNED_TO_REVIEWED)
        )

        clock.tick()
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
            it.getLatestPublishedVasuDocument(mockToday, documentId).let { doc ->
                assertNotNull(doc)
                assertEquals(contentWithUpdatedEvaluation, doc.content)
            }
        }
    }

    private fun postVasuDocument(
        childId: ChildId,
        request: VasuController.CreateDocumentRequest,
        evakaClock: EvakaClock = clock,
    ): VasuDocumentId {
        return vasuController.createDocument(dbInstance(), adminUser, evakaClock, childId, request)
    }

    private fun getVasuSummaries(childId: ChildId): List<VasuDocumentSummary> {
        return vasuController.getVasuSummariesByChild(dbInstance(), adminUser, clock, childId).map {
            it.data
        }
    }

    private fun getVasuDocument(id: VasuDocumentId): VasuDocument {
        return vasuController.getDocument(dbInstance(), adminUser, clock, id).data
    }

    private fun putVasuDocument(id: VasuDocumentId, request: VasuController.UpdateDocumentRequest) {
        vasuController.putDocument(dbInstance(), adminUser, clock, id, request)
    }

    private fun postVasuDocumentState(
        id: VasuDocumentId,
        request: VasuController.ChangeDocumentStateRequest,
    ) {
        vasuController.updateDocumentState(dbInstance(), adminUser, clock, id, request)
    }

    private fun deleteVasuDocument(id: VasuDocumentId) {
        vasuController.deleteDocument(dbInstance(), adminUser, clock, id)
    }

    private fun postVasuTemplate(
        request: VasuTemplateController.CreateTemplateRequest
    ): VasuTemplateId {
        return vasuTemplateController.postTemplate(dbInstance(), adminUser, clock, request)
    }

    private fun putVasuTemplateContent(id: VasuTemplateId, request: VasuContent) {
        vasuTemplateController.putTemplateContent(dbInstance(), adminUser, clock, id, request)
    }

    private fun getVasuTemplate(id: VasuTemplateId): VasuTemplate {
        return vasuTemplateController.getTemplate(dbInstance(), adminUser, clock, id)
    }

    private fun deleteVasuTemplate(id: VasuTemplateId) {
        vasuTemplateController.deleteTemplate(dbInstance(), adminUser, clock, id)
    }

    private fun givePermissionToShare(
        id: VasuDocumentId,
        user: AuthenticatedUser.Citizen,
    ) {
        vasuControllerCitizen.givePermissionToShare(dbInstance(), user, clock, id)
    }
}
