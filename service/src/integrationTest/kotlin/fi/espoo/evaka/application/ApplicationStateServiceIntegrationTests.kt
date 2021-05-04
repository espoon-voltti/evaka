// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionDraft
import fi.espoo.evaka.decision.DecisionDraftService
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.fetchDecisionDrafts
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.data.getIncomesForPerson
import fi.espoo.evaka.invoicing.data.upsertIncome
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.placement.PlacementPlan
import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementPlanRejectReason
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementPlan
import fi.espoo.evaka.placement.getPlacementsForChild
import fi.espoo.evaka.preschoolTerm2020
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.message.MockEvakaMessageClient
import fi.espoo.evaka.shared.utils.europeHelsinki
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testChild_7
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.UUID

class ApplicationStateServiceIntegrationTests : FullApplicationTest() {
    @Autowired
    private lateinit var service: ApplicationStateService

    @Autowired
    private lateinit var decisionDraftService: DecisionDraftService

    @Autowired
    private lateinit var asyncJobRunner: AsyncJobRunner

    @Autowired
    lateinit var mapper: ObjectMapper

    private val serviceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    private val applicationId = UUID.randomUUID()
    val address = Address(
        street = "Street 1",
        postalCode = "00200",
        postOffice = "Espoo"
    )
    val mainPeriod = preschoolTerm2020
    val connectedPeriod = FiniteDateRange(preschoolTerm2020.start.minusDays(12), preschoolTerm2020.end.plusDays(15))

    @BeforeEach
    private fun beforeEach() {
        MockEvakaMessageClient.clearMessages()
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `sendApplication - preschool has due date same as sent date`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 13)
            )
        }

        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, applicationId)
        }

        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.SENT, application.status)
            assertEquals(LocalDate.now(), application.sentDate)
            assertEquals(LocalDate.now(), application.dueDate)
        }
    }

    @Test
    fun `sendApplication - daycare has due date after 4 months if not urgent`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.DAYCARE,
                urgent = false,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
        }
        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, applicationId)
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(LocalDate.now().plusMonths(4), application.dueDate)
        }
    }

    private fun assertDueDate(applicationId: UUID, expected: Instant?) {
        db.read {
            val application = it.fetchApplicationDetails(applicationId)!!
            if (expected != null) {
                assertEquals(LocalDate.ofInstant(expected, europeHelsinki), application.dueDate)
            } else {
                assertNull(application.dueDate)
            }
        }
    }

    @Test
    fun `sendApplication - daycare has due date after 2 weeks if urgent and has attachments`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                guardian = testAdult_1,
                appliedType = PlacementType.DAYCARE,
                urgent = true,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
        }
        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, applicationId)
        }
        // then
        assertDueDate(applicationId, null) // missing attachment

        // when
        assertTrue(uploadAttachment(applicationId, AuthenticatedUser.Citizen(testAdult_1.id)))
        db.transaction { tx ->
            tx.createUpdate("UPDATE attachment SET received_at = :receivedAt WHERE application_id = :applicationId")
                .bind("applicationId", applicationId)
                .bind("receivedAt", Instant.now().minus(Period.ofWeeks(1)))
                .execute()
        }
        assertTrue(uploadAttachment(applicationId, AuthenticatedUser.Citizen(testAdult_1.id)))
        // then
        assertDueDate(applicationId, Instant.now().plus(Period.ofWeeks(2))) // end date >= earliest attachment.receivedAt
    }

    @Test
    fun `sendApplication - urgent daycare application gets due date from first received attachment`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                guardian = testAdult_1,
                appliedType = PlacementType.DAYCARE,
                urgent = true,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
        }
        // when
        assertTrue(uploadAttachment(applicationId, AuthenticatedUser.Citizen(testAdult_1.id), AttachmentType.EXTENDED_CARE))
        assertTrue(uploadAttachment(applicationId, AuthenticatedUser.Citizen(testAdult_1.id), AttachmentType.URGENCY))

        // then
        assertDueDate(applicationId, null) // application not sent

        // when
        db.transaction { tx ->
            tx.createUpdate("UPDATE attachment SET received_at = :receivedAt WHERE type = :type AND application_id = :applicationId")
                .bind("type", AttachmentType.EXTENDED_CARE)
                .bind("applicationId", applicationId)
                .bind("receivedAt", Instant.now().plus(Period.ofWeeks(1)))
                .execute()
            tx.createUpdate("UPDATE attachment SET received_at = :receivedAt WHERE type = :type AND application_id = :applicationId")
                .bind("type", AttachmentType.URGENCY)
                .bind("applicationId", applicationId)
                .bind("receivedAt", Instant.now().plus(Period.ofDays(3)))
                .execute()
        }
        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, applicationId)
        }
        // then
        assertDueDate(applicationId, Instant.now().plus(Period.ofDays(14 + 3))) // attachments received after application sent
    }

    @Test
    fun `sendApplication - daycare has not due date if a transfer application`() {
        db.transaction { tx ->
            // given
            val draft = tx.insertApplication(
                appliedType = PlacementType.DAYCARE,
                urgent = false,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            tx.insertTestPlacement(
                DevPlacement(
                    childId = draft.childId,
                    unitId = testDaycare2.id,
                    startDate = draft.form.preferences.preferredStartDate!!,
                    endDate = draft.form.preferences.preferredStartDate!!.plusYears(1)
                )
            )
        }
        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, applicationId)
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.transferApplication)
            assertEquals(null, application.dueDate)
        }
    }

    @Test
    fun `moveToWaitingPlacement without otherInfo - status is changed and checkedByAdmin defaults true`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                hasAdditionalInfo = false,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(true, application.checkedByAdmin)
        }
    }

    @Test
    fun `moveToWaitingPlacement with otherInfo - status is changed and checkedByAdmin defaults false`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                hasAdditionalInfo = true,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(false, application.checkedByAdmin)
        }
    }

    @Test
    fun `moveToWaitingPlacement - guardian contact details are updated`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.DAYCARE,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
        }
        db.read { tx ->
            // then
            val guardian = tx.handle.getPersonById(testAdult_5.id)!!
            assertEquals("abc@espoo.fi", guardian.email)
            assertEquals("0501234567", guardian.phone)
        }
    }

    @Test
    fun `moveToWaitingPlacement - empty application guardian email does not wipe out person email`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                guardian = testAdult_6,
                appliedType = PlacementType.DAYCARE,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
                guardianEmail = ""
            )
            service.sendApplication(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
        }
        db.read { tx ->
            // then
            val guardian = tx.handle.getPersonById(testAdult_6.id)!!
            assertEquals(testAdult_6.email, guardian.email)
            assertEquals("0501234567", guardian.phone)
        }
    }

    @Test
    fun `moveToWaitingPlacement - child is upserted with diet and allergies`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.DAYCARE,
                hasAdditionalInfo = true,
                applicationId = applicationId
            )
            service.sendApplication(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
        }
        db.read { tx ->
            // then
            val childDetails = tx.handle.getChild(testChild_6.id)!!.additionalInformation
            assertEquals("diet", childDetails.diet)
            assertEquals("allergies", childDetails.allergies)
        }
    }

    @Test
    fun `setVerified and setUnverified - changes checkedByAdmin`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                hasAdditionalInfo = true,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.setVerified(tx, serviceWorker, applicationId)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(true, application.checkedByAdmin)
        }
        db.transaction { tx ->
            // when
            service.setUnverified(tx, serviceWorker, applicationId)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(false, application.checkedByAdmin)
        }
    }

    @Test
    fun `cancelApplication from SENT - status is changed`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(applicationId = applicationId, preferredStartDate = LocalDate.of(2020, 8, 1))
            service.sendApplication(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.cancelApplication(tx, serviceWorker, applicationId)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.CANCELLED, application.status)
        }
    }

    @Test
    fun `cancelApplication from WAITING_PLACEMENT - status is changed`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(applicationId = applicationId, preferredStartDate = LocalDate.of(2020, 8, 1))
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.cancelApplication(tx, serviceWorker, applicationId)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.CANCELLED, application.status)
        }
    }

    @Test
    fun `returnToSent - status is changed`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(applicationId = applicationId, preferredStartDate = LocalDate.of(2020, 8, 1))
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.returnToSent(tx, serviceWorker, applicationId)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.SENT, application.status)
        }
    }

    @Test
    fun `createPlacementPlan - daycare`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.DAYCARE,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod
                )
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

            val placementPlan = getPlacementPlan(tx.handle, applicationId)!!
            assertEquals(
                PlacementPlan(
                    id = placementPlan.id,
                    unitId = testDaycare.id,
                    applicationId = applicationId,
                    type = PlacementType.DAYCARE,
                    period = mainPeriod,
                    preschoolDaycarePeriod = null
                ),
                placementPlan
            )

            val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
            assertEquals(1, decisionDrafts.size)
            assertEquals(
                DecisionDraft(
                    id = decisionDrafts.first().id,
                    type = DecisionType.DAYCARE,
                    startDate = mainPeriod.start,
                    endDate = mainPeriod.end,
                    unitId = testDaycare.id,
                    planned = true
                ),
                decisionDrafts.first()
            )
        }
    }

    @Test
    fun `createPlacementPlan - daycare part-time`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.DAYCARE_PART_TIME,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod
                )
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

            val placementPlan = getPlacementPlan(tx.handle, applicationId)!!
            assertEquals(
                PlacementPlan(
                    id = placementPlan.id,
                    unitId = testDaycare.id,
                    applicationId = applicationId,
                    type = PlacementType.DAYCARE_PART_TIME,
                    period = mainPeriod,
                    preschoolDaycarePeriod = null
                ),
                placementPlan
            )

            val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
            assertEquals(1, decisionDrafts.size)
            assertEquals(
                DecisionDraft(
                    id = decisionDrafts.first().id,
                    type = DecisionType.DAYCARE_PART_TIME,
                    startDate = mainPeriod.start,
                    endDate = mainPeriod.end,
                    unitId = testDaycare.id,
                    planned = true
                ),
                decisionDrafts.first()
            )
        }
    }

    @Test
    fun `createPlacementPlan - preschool`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 13)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod
                )
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

            val placementPlan = getPlacementPlan(tx.handle, applicationId)!!
            assertEquals(
                PlacementPlan(
                    id = placementPlan.id,
                    unitId = testDaycare.id,
                    applicationId = applicationId,
                    type = PlacementType.PRESCHOOL,
                    period = mainPeriod,
                    preschoolDaycarePeriod = null
                ),
                placementPlan
            )

            val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
            assertEquals(2, decisionDrafts.size)

            decisionDrafts.find { it.type == DecisionType.PRESCHOOL }!!.let {
                assertEquals(
                    DecisionDraft(
                        id = it.id,
                        type = DecisionType.PRESCHOOL,
                        startDate = mainPeriod.start,
                        endDate = mainPeriod.end,
                        unitId = testDaycare.id,
                        planned = true
                    ),
                    it
                )
            }
            decisionDrafts.find { it.type == DecisionType.PRESCHOOL_DAYCARE }!!.let {
                assertEquals(
                    DecisionDraft(
                        id = it.id,
                        type = DecisionType.PRESCHOOL_DAYCARE,
                        startDate = mainPeriod.start,
                        endDate = mainPeriod.end,
                        unitId = testDaycare.id,
                        planned = false
                    ),
                    it
                )
            }
        }
    }

    @Test
    fun `createPlacementPlan - preschool with daycare`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL_DAYCARE,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod
                )
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

            val placementPlan = getPlacementPlan(tx.handle, applicationId)!!
            assertEquals(
                PlacementPlan(
                    id = placementPlan.id,
                    unitId = testDaycare.id,
                    applicationId = applicationId,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod
                ),
                placementPlan
            )

            val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
            assertEquals(2, decisionDrafts.size)

            decisionDrafts.find { it.type == DecisionType.PRESCHOOL }!!.let {
                assertEquals(
                    DecisionDraft(
                        id = it.id,
                        type = DecisionType.PRESCHOOL,
                        startDate = mainPeriod.start,
                        endDate = mainPeriod.end,
                        unitId = testDaycare.id,
                        planned = true
                    ),
                    it
                )
            }
            decisionDrafts.find { it.type == DecisionType.PRESCHOOL_DAYCARE }!!.let {
                assertEquals(
                    DecisionDraft(
                        id = it.id,
                        type = DecisionType.PRESCHOOL_DAYCARE,
                        startDate = connectedPeriod.start,
                        endDate = connectedPeriod.end,
                        unitId = testDaycare.id,
                        planned = true
                    ),
                    it
                )
            }
        }
    }

    @Test
    fun `cancelPlacementPlan - removes placement plan and decision drafts and changes status`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL_DAYCARE,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod
                )
            )
        }
        db.transaction { tx ->
            // when
            service.cancelPlacementPlan(tx, serviceWorker, applicationId)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)

            val placementPlan = getPlacementPlan(tx.handle, applicationId)
            assertEquals(null, placementPlan)

            val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
            assertEquals(0, decisionDrafts.size)
        }
    }

    @Test
    fun `sendDecisionsWithoutProposal - applier is the only guardian`() = sendDecisionsWithoutProposalTest(
        child = testChild_2,
        applier = testAdult_1,
        secondDecisionTo = null,
        manualMailing = false
    )

    @Test
    fun `sendDecisionsWithoutProposal - applier is guardian and other guardian exists in same address`() =
        sendDecisionsWithoutProposalTest(
            child = testChild_1,
            applier = testAdult_1,
            secondDecisionTo = null,
            manualMailing = false
        )

    @Test
    fun `sendDecisionsWithoutProposal - applier is guardian and other guardian exists in different address`() =
        sendDecisionsWithoutProposalTest(
            child = testChild_6,
            applier = testAdult_5,
            secondDecisionTo = testAdult_6,
            manualMailing = false
        )

    @Test
    fun `sendDecisionsWithoutProposal - child has no ssn`() = sendDecisionsWithoutProposalTest(
        child = testChild_7,
        applier = testAdult_5,
        secondDecisionTo = null,
        manualMailing = true
    )

    @Test
    fun `sendDecisionsWithoutProposal - applier has no ssn, child has no guardian`() = sendDecisionsWithoutProposalTest(
        child = testChild_7,
        applier = testAdult_4,
        secondDecisionTo = null,
        manualMailing = true
    )

    @Test
    fun `sendDecisionsWithoutProposal - applier has no ssn, child has another guardian`() =
        sendDecisionsWithoutProposalTest(
            child = testChild_2,
            applier = testAdult_4,
            secondDecisionTo = testChild_1,
            manualMailing = true
        )

    private fun sendDecisionsWithoutProposalTest(
        child: PersonData.Detailed,
        applier: PersonData.Detailed,
        secondDecisionTo: PersonData.Detailed?,
        manualMailing: Boolean
    ) {
        // given
        db.transaction { tx ->
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL,
                guardian = applier,
                child = child,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 13)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(unitId = testDaycare.id, period = mainPeriod)
            )
        }

        // when
        db.transaction { tx ->
            service.sendDecisionsWithoutProposal(tx, serviceWorker, applicationId)
        }
        asyncJobRunner.runPendingJobsSync()
        asyncJobRunner.runPendingJobsSync()

        // then
        db.read {
            val application = it.fetchApplicationDetails(applicationId)!!
            if (manualMailing) {
                assertEquals(ApplicationStatus.WAITING_MAILING, application.status)
            } else {
                assertEquals(ApplicationStatus.WAITING_CONFIRMATION, application.status)
            }

            val decisionsByApplication = it.getDecisionsByApplication(applicationId, AclAuthorization.All)
            assertEquals(1, decisionsByApplication.size)
            val decision = decisionsByApplication.first()
            assertNotNull(decision.sentDate)
            assertNotNull(decision.documentUri)

            if (secondDecisionTo == null) {
                assertNull(decision.otherGuardianDocumentUri)
            } else {
                assertNotNull(decision.otherGuardianDocumentUri)
            }
        }

        val messages = MockEvakaMessageClient.getMessages()
        if (manualMailing) {
            assertEquals(0, messages.size)
        } else {
            if (secondDecisionTo == null) {
                assertEquals(1, messages.size)
            } else {
                assertEquals(2, messages.size)
                assertEquals(1, messages.filter { it.ssn == secondDecisionTo.ssn }.size)
            }
            assertEquals(1, messages.filter { it.ssn == applier.ssn }.size)
        }
    }

    @Test
    fun `sendPlacementProposal - updates status`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL_DAYCARE,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod
                )
            )
            // when
            service.sendPlacementProposal(tx, serviceWorker, applicationId)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_UNIT_CONFIRMATION, application.status)

            val decisions = tx.getDecisionsByApplication(applicationId, AclAuthorization.All)
            assertEquals(0, decisions.size)
        }
    }

    @Test
    fun `withdrawPlacementProposal - updates status`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL_DAYCARE,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod
                )
            )
            service.sendPlacementProposal(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.withdrawPlacementProposal(tx, serviceWorker, applicationId)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

            val decisions = tx.getDecisionsByApplication(applicationId, AclAuthorization.All)
            assertEquals(0, decisions.size)
        }
    }

    @Test
    fun `acceptPlacementProposal - sends decisions and updates status`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL_DAYCARE,
                child = testChild_2,
                guardian = testAdult_1,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod
                )
            )
            service.sendPlacementProposal(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.respondToPlacementProposal(
                tx,
                serviceWorker,
                applicationId,
                PlacementPlanConfirmationStatus.ACCEPTED
            )
            service.acceptPlacementProposal(tx, serviceWorker, testDaycare.id)
        }
        asyncJobRunner.runPendingJobsSync()
        asyncJobRunner.runPendingJobsSync()
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_CONFIRMATION, application.status)

            val decisionsByApplication = tx.getDecisionsByApplication(applicationId, AclAuthorization.All)
            assertEquals(2, decisionsByApplication.size)
            decisionsByApplication.forEach { decision ->
                assertNotNull(decision.sentDate)
                assertNotNull(decision.documentUri)
                assertNull(decision.otherGuardianDocumentUri)
            }
            val messages = MockEvakaMessageClient.getMessages()
            assertEquals(2, messages.size)
            assertEquals(2, messages.filter { it.ssn == testAdult_1.ssn }.size)
        }
    }

    @Test
    fun `acceptPlacementProposal - if no decisions are marked for sending do nothing`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL_DAYCARE,
                child = testChild_2,
                guardian = testAdult_1, applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod
                )
            )
            tx.fetchDecisionDrafts(applicationId).map { draft ->
                DecisionDraftService.DecisionDraftUpdate(
                    id = draft.id,
                    unitId = draft.unitId,
                    startDate = draft.startDate,
                    endDate = draft.endDate,
                    planned = false
                )
            }.let { updates -> decisionDraftService.updateDecisionDrafts(tx, applicationId, updates) }
            service.sendPlacementProposal(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when
            service.respondToPlacementProposal(
                tx,
                serviceWorker,
                applicationId,
                PlacementPlanConfirmationStatus.ACCEPTED
            )
            service.acceptPlacementProposal(tx, serviceWorker, testDaycare.id)
        }
        asyncJobRunner.runPendingJobsSync()
        asyncJobRunner.runPendingJobsSync()
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_UNIT_CONFIRMATION, application.status)

            val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
            assertEquals(2, decisionDrafts.size)

            val decisionsByApplication = tx.getDecisionsByApplication(applicationId, AclAuthorization.All)
            assertEquals(0, decisionsByApplication.size)

            val messages = MockEvakaMessageClient.getMessages()
            assertEquals(0, messages.size)

            val placementPlan = getPlacementPlan(tx.handle, applicationId)
            assertNotNull(placementPlan)

            val placements = tx.handle.getPlacementsForChild(testChild_2.id)
            assertEquals(0, placements.size)
        }
    }

    @Test
    fun `acceptPlacementProposal - throws if some application is pending response`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL,
                child = testChild_2,
                guardian = testAdult_1, applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 13)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod
                )
            )
            service.sendPlacementProposal(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when / then
            assertThrows<BadRequest> { service.acceptPlacementProposal(tx, serviceWorker, testDaycare.id) }
        }
    }

    @Test
    fun `acceptPlacementProposal - throws if some application has been rejected`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL,
                child = testChild_2,
                guardian = testAdult_1, applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 13)
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod
                )
            )
            service.sendPlacementProposal(tx, serviceWorker, applicationId)
        }
        db.transaction { tx ->
            // when / then
            service.respondToPlacementProposal(
                tx,
                serviceWorker,
                applicationId,
                PlacementPlanConfirmationStatus.REJECTED,
                PlacementPlanRejectReason.REASON_1
            )
            assertThrows<BadRequest> { service.acceptPlacementProposal(tx, serviceWorker, testDaycare.id) }
        }
    }

    @Test
    fun `reject preschool decision rejects preschool daycare decision too`() {
        // given
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.rejectDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.REJECTED, application.status)

            with(getDecision(tx, DecisionType.PRESCHOOL)) {
                assertEquals(DecisionStatus.REJECTED, status)
            }
            with(getDecision(tx, DecisionType.PRESCHOOL_DAYCARE)) {
                assertEquals(DecisionStatus.REJECTED, status)
            }

            val placementPlan = getPlacementPlan(tx.handle, applicationId)
            assertNull(placementPlan)

            val placements = tx.handle.getPlacementsForChild(testChild_6.id)
            assertEquals(0, placements.size)
        }
    }

    @Test
    fun `accept preschool and reject preschool daycare`() {
        // given
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
            service.rejectDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL_DAYCARE).id
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.ACTIVE, application.status)

            with(getDecision(tx, DecisionType.PRESCHOOL)) {
                assertEquals(DecisionStatus.ACCEPTED, status)
            }
            with(getDecision(tx, DecisionType.PRESCHOOL_DAYCARE)) {
                assertEquals(DecisionStatus.REJECTED, status)
            }

            val placementPlan = getPlacementPlan(tx.handle, applicationId)
            assertNull(placementPlan)

            val placements = tx.handle.getPlacementsForChild(testChild_6.id)
            assertEquals(1, placements.size)
            with(placements.first()) {
                assertEquals(mainPeriod.start, startDate)
                assertEquals(mainPeriod.end, endDate)
                assertEquals(PlacementType.PRESCHOOL, type)
            }
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - new income has been created`() {
        // given
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = it.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(1, incomes.size)
            assertEquals(applicationId, incomes.first().applicationId)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - existing overlapping indefinite income will be handled`() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val earlierIndefinite = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start.minusDays(10),
                validTo = null
            )
            tx.upsertIncome(mapper, earlierIndefinite, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = it.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(2, incomes.size)
            assertEquals(applicationId, incomes.first().applicationId)
            assertEquals(IncomeEffect.MAX_FEE_ACCEPTED, incomes.first().effect)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes[1].effect)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - existing overlapping income will be handled by not adding a new income for user`() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val earlierIncome = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start.minusDays(10),
                validTo = mainPeriod.start.plusMonths(5)
            )
            tx.upsertIncome(mapper, earlierIncome, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = it.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(1, incomes.size)
            assertNull(incomes.first().applicationId)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - later indefinite income will be handled by not adding a new income`() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val laterIndefiniteIncome = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start.plusMonths(5),
                validTo = null
            )
            tx.upsertIncome(mapper, laterIndefiniteIncome, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = it.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(1, incomes.size)
            assertNull(incomes.first().applicationId)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - earlier income does not affect creating a new income`() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val earlierIncome = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start.minusMonths(7),
                validTo = mainPeriod.start.minusMonths(5)
            )
            tx.upsertIncome(mapper, earlierIncome, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = it.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(2, incomes.size)
            val incomeByApplication = incomes.first()
            assertEquals(applicationId, incomeByApplication.applicationId)
            assertEquals(IncomeEffect.MAX_FEE_ACCEPTED, incomeByApplication.effect)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes[1].effect)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - later income will be handled by not adding a new income`() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val laterIncome = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start.plusMonths(5),
                validTo = mainPeriod.start.plusMonths(6)
            )
            tx.upsertIncome(mapper, laterIncome, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = it.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(1, incomes.size)
            assertNull(incomes.first().applicationId)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - if application does not have a preferred start date income will still be created`() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val earlierIndefinite = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start.minusDays(10),
                validTo = null
            )
            tx.upsertIncome(mapper, earlierIndefinite, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions(preferredStartDate = null)

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = it.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(2, incomes.size)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - no new income will be created if there exists indefinite income for the same day `() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val sameDayIncomeIndefinite = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start,
                validTo = null
            )
            tx.upsertIncome(mapper, sameDayIncomeIndefinite, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = tx.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(1, incomes.size)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - no new income will be created if there exists income for the same day `() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val sameDayIncome = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start,
                validTo = mainPeriod.start.plusMonths(5)
            )
            tx.upsertIncome(mapper, sameDayIncome, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = tx.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(1, incomes.size)
            assertNull(incomes.first().applicationId)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - new income will be created if there exists indefinite income for the same day - 1 `() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val dayBeforeIncomeIndefinite = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start.minusDays(1),
                validTo = null
            )
            tx.upsertIncome(mapper, dayBeforeIncomeIndefinite, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = tx.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(2, incomes.size)
            assertEquals(application.id, incomes.first().applicationId)
            assertEquals(IncomeEffect.MAX_FEE_ACCEPTED, incomes.first().effect)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes[1].effect)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - no new income will be created if there exists indefinite income for the same day + 1 `() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val nextDayIncomeIndefinite = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start.plusDays(1),
                validTo = null
            )
            tx.upsertIncome(mapper, nextDayIncomeIndefinite, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = tx.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(1, incomes.size)
            assertNull(incomes.first().applicationId)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - no new income will be created if there exists income for the same day - 1`() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val incomeDayBefore = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start.minusDays(1),
                validTo = mainPeriod.start.plusMonths(5)
            )
            tx.upsertIncome(mapper, incomeDayBefore, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = tx.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(1, incomes.size)
            assertNull(incomes.first().applicationId)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - no new income will be created if there exists income that ends on the same day`() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val earlierIncomeEndingOnSameDay = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start.minusMonths(2),
                validTo = mainPeriod.start
            )
            tx.upsertIncome(mapper, earlierIncomeEndingOnSameDay, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = tx.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(1, incomes.size)
            assertNull(incomes.first().applicationId)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - no new income will be created if there exists income that ends on the same day + 1`() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val earlierIncomeEndingOnNextDay = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start.minusMonths(2),
                validTo = mainPeriod.start.plusDays(1)
            )
            tx.upsertIncome(mapper, earlierIncomeEndingOnNextDay, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = tx.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(1, incomes.size)
            assertNull(incomes.first().applicationId)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }
    }

    @Test
    fun `accept preschool application with maxFeeAccepted - new income will be created if there exists income that ends on the same day - 1`() {
        db.transaction { tx ->
            // given
            val financeUser = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.FINANCE_ADMIN))
            val earlierIncomeEndingOnDayBefore = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = mainPeriod.start.minusMonths(2),
                validTo = mainPeriod.start.minusDays(1)
            )
            tx.upsertIncome(mapper, earlierIncomeEndingOnDayBefore, financeUser.id)
        }
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = tx.getIncomesForPerson(mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.ACTIVE, application.status)
            assertEquals(2, incomes.size)
            assertEquals(IncomeEffect.MAX_FEE_ACCEPTED, incomes.first().effect)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes[1].effect)
        }
    }

    @Test
    fun `enduser can accept and reject own decisions`() {
        // given
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            val user = AuthenticatedUser.Citizen(testAdult_5.id)
            service.acceptDecision(
                tx,
                user,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start,
                isEnduser = true
            )
            service.rejectDecision(
                tx,
                user,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL_DAYCARE).id,
                isEnduser = true
            )

            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.ACTIVE, application.status)
        }
    }

    @Test
    fun `enduser can not accept decision of someone else`() {
        // given
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            val user = AuthenticatedUser.Citizen(testAdult_1.id)
            // when
            assertThrows<Forbidden> {
                service.acceptDecision(
                    tx,
                    user,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL).id,
                    mainPeriod.start,
                    isEnduser = true
                )
            }
        }
    }

    @Test
    fun `enduser can not reject decision of someone else`() {
        // given
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            val user = AuthenticatedUser.Citizen(testAdult_1.id)
            assertThrows<Forbidden> {
                service.rejectDecision(
                    tx,
                    user,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL).id,
                    isEnduser = true
                )
            }
        }
    }

    @Test
    fun `accept preschool and accept preschool daycare`() {
        // given
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL_DAYCARE).id,
                connectedPeriod.start
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.ACTIVE, application.status)

            with(getDecision(tx, DecisionType.PRESCHOOL)) {
                assertEquals(DecisionStatus.ACCEPTED, status)
            }
            with(getDecision(tx, DecisionType.PRESCHOOL_DAYCARE)) {
                assertEquals(DecisionStatus.ACCEPTED, status)
            }

            val placementPlan = getPlacementPlan(tx.handle, applicationId)
            assertNull(placementPlan)

            val placements = tx.handle.getPlacementsForChild(testChild_6.id)
            assertEquals(2, placements.size)
            with(placements.first { it.type == PlacementType.PRESCHOOL_DAYCARE }) {
                assertEquals(connectedPeriod.start, startDate)
                assertEquals(mainPeriod.end, endDate)
            }
            with(placements.first { it.type == PlacementType.DAYCARE }) {
                assertEquals(mainPeriod.end.plusDays(1), startDate)
                assertEquals(connectedPeriod.end, endDate)
            }
        }
    }

    @Test
    fun `accept preschool daycare first throws`() {
        // given
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when / then
            assertThrows<BadRequest> {
                service.acceptDecision(
                    tx,
                    serviceWorker,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL_DAYCARE).id,
                    mainPeriod.start
                )
            }
        }
    }

    @Test
    fun `accepting already accepted decision throws`() {
        // given
        workflowForPreschoolDaycareDecisions()
        db.transaction { tx ->
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.transaction { tx ->
            // when / then
            assertThrows<BadRequest> {
                service.acceptDecision(
                    tx,
                    serviceWorker,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL).id,
                    mainPeriod.start
                )
            }
        }
    }

    @Test
    fun `accepting already rejected decision throws`() {
        // given
        workflowForPreschoolDaycareDecisions()
        db.transaction { tx ->
            service.rejectDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id
            )
        }
        db.transaction { tx ->
            // when / then
            assertThrows<BadRequest> {
                service.acceptDecision(
                    tx,
                    serviceWorker,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL).id,
                    mainPeriod.start
                )
            }
        }
    }

    @Test
    fun `rejecting already accepted decision throws`() {
        // given
        workflowForPreschoolDaycareDecisions()
        db.transaction { tx ->
            service.acceptDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
        db.transaction { tx ->
            // when / then
            assertThrows<BadRequest> {
                service.rejectDecision(
                    tx,
                    serviceWorker,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL).id
                )
            }
        }
    }

    @Test
    fun `rejecting already rejected decision throws`() {
        // given
        workflowForPreschoolDaycareDecisions()
        db.transaction { tx ->
            service.rejectDecision(
                tx,
                serviceWorker,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id
            )
        }
        db.transaction { tx ->
            // when / then
            assertThrows<BadRequest> {
                service.rejectDecision(
                    tx,
                    serviceWorker,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL).id
                )
            }
        }
    }

    private fun getDecision(r: Database.Read, type: DecisionType): Decision =
        r.getDecisionsByApplication(applicationId, AclAuthorization.All).first { it.type == type }

    private fun workflowForPreschoolDaycareDecisions(preferredStartDate: LocalDate? = LocalDate.of(2020, 8, 1)) {
        db.transaction { tx ->
            tx.insertApplication(
                guardian = testAdult_5,
                maxFeeAccepted = true,
                appliedType = PlacementType.PRESCHOOL_DAYCARE,
                applicationId = applicationId,
                preferredStartDate = preferredStartDate
            )
            service.sendApplication(tx, serviceWorker, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod
                )
            )
            service.sendDecisionsWithoutProposal(tx, serviceWorker, applicationId)
        }
    }
}
