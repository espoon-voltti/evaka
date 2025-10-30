// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.notes.getApplicationNotes
import fi.espoo.evaka.application.notes.getServiceWorkerApplicationNote
import fi.espoo.evaka.application.notes.updateServiceWorkerApplicationNote
import fi.espoo.evaka.caseprocess.CaseProcessState
import fi.espoo.evaka.caseprocess.ProcessMetadataController
import fi.espoo.evaka.caseprocess.getCaseProcessByApplicationId
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionDraft
import fi.espoo.evaka.decision.DecisionDraftUpdate
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.fetchDecisionDrafts
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.decision.getSentDecisionsByApplication
import fi.espoo.evaka.decision.updateDecisionDrafts
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementPlan
import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementPlanRejectReason
import fi.espoo.evaka.placement.PlacementSource
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementPlan
import fi.espoo.evaka.placement.getPlacementsForChild
import fi.espoo.evaka.preschoolTerm2020
import fi.espoo.evaka.preschoolTerm2021
import fi.espoo.evaka.serviceneed.getServiceNeedsByChild
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.config.testFeatureConfig
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFridgePartner
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.snPreschoolClub45
import fi.espoo.evaka.snPreschoolDaycare45
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testChild_7
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.legacyMockVtjDataset
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import kotlin.enums.enumEntries
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ApplicationStateServiceIntegrationTests : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var service: ApplicationStateService
    @Autowired private lateinit var metadataController: ProcessMetadataController

    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val serviceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    private val applicationId = ApplicationId(UUID.randomUUID())
    private val mainPeriod = FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4))
    private val connectedPeriod =
        FiniteDateRange(mainPeriod.start.minusDays(12), mainPeriod.end.plusDays(15))

    private val today: LocalDate = LocalDate.of(2020, 2, 16)
    private val now: HelsinkiDateTime = HelsinkiDateTime.of(today, LocalTime.of(12, 0, 0))
    private val clock = MockEvakaClock(now)

    @BeforeEach
    fun beforeEach() {
        MockSfiMessagesClient.reset()
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            listOf(testAdult_1, testAdult_2, testAdult_3, testAdult_4, testAdult_5, testAdult_6)
                .forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(testChild_1, testChild_2, testChild_6, testChild_7).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insert(preschoolTerm2020)
        }
        MockPersonDetailsService.add(legacyMockVtjDataset())
    }

    @Test
    fun `initialize daycare application form with null service need option`() {
        val applicationId =
            db.transaction { tx ->
                service.createApplication(
                    tx,
                    AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
                    now,
                    type = ApplicationType.DAYCARE,
                    child = tx.getPersonById(testChild_1.id)!!,
                    guardian = tx.getPersonById(testAdult_1.id)!!,
                    origin = ApplicationOrigin.PAPER,
                )
            }

        db.read {
            val application = it.fetchApplicationDetails(applicationId)!!
            assertNull(application.form.preferences.serviceNeed?.serviceNeedOption)
        }
    }

    @Test
    fun `preschool application initialization uses correct term's start as default preferred start date`() {
        db.transaction { tx ->
            tx.insert(preschoolTerm2021)
            val applicationDate = preschoolTerm2021.applicationPeriod.start.minusWeeks(1)
            val applicationId =
                service.createApplication(
                    tx,
                    AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
                    now = HelsinkiDateTime.of(applicationDate, LocalTime.of(12, 0)),
                    type = ApplicationType.PRESCHOOL,
                    child = tx.getPersonById(testChild_1.id)!!,
                    guardian = tx.getPersonById(testAdult_1.id)!!,
                    origin = ApplicationOrigin.PAPER,
                )

            val result = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(null, result.form.preferences.preferredStartDate)
        }

        db.transaction { tx ->
            val applicationDate = preschoolTerm2021.applicationPeriod.start
            val applicationId =
                service.createApplication(
                    tx,
                    AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
                    now = HelsinkiDateTime.of(applicationDate, LocalTime.of(12, 0)),
                    type = ApplicationType.PRESCHOOL,
                    child = tx.getPersonById(testChild_1.id)!!,
                    guardian = tx.getPersonById(testAdult_1.id)!!,
                    origin = ApplicationOrigin.PAPER,
                )

            val result = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(
                preschoolTerm2021.extendedTerm.start,
                result.form.preferences.preferredStartDate,
            )
        }
    }

    @Test
    fun `sendApplication - preschool has due date same as sent date`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 13),
            )
        }

        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }

        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.SENT, application.status)
            assertEquals(today, application.sentDate)
            assertEquals(today, application.dueDate)
        }
    }

    @Test
    fun `relative due date TRUE - non-urgent application's due date is calculated as equaling preferred start if it's more than 4 months away`() {
        val preferredStartDate = LocalDate.of(2020, 8, 1)
        val sentDate = LocalDate.of(2020, 2, 1)
        val dueDate =
            service.calculateDueDate(
                applicationType = ApplicationType.DAYCARE,
                sentDate = sentDate,
                preferredStartDate = preferredStartDate,
                isUrgent = false,
                isTransferApplication = false,
                attachments = emptyList(),
                config = testFeatureConfig.copy(preferredStartRelativeApplicationDueDate = true),
            )

        assertEquals(preferredStartDate, dueDate)
    }

    @Test
    fun `relative due date FALSE - non-urgent application's due date is calculated as 4 months after sent date`() {
        val preferredStartDate = LocalDate.of(2020, 8, 1)
        val sentDate = LocalDate.of(2020, 2, 1)
        val defaultDueDate = sentDate.plusMonths(4)

        val dueDate =
            service.calculateDueDate(
                applicationType = ApplicationType.DAYCARE,
                sentDate = sentDate,
                preferredStartDate = preferredStartDate,
                isUrgent = false,
                isTransferApplication = false,
                attachments = emptyList(),
                config = testFeatureConfig.copy(preferredStartRelativeApplicationDueDate = false),
            )

        assertEquals(defaultDueDate, dueDate)
    }

    @Test
    fun `sendApplication - daycare has due date after 4 months if not urgent`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.DAYCARE,
                urgent = false,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
        }
        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(today.plusMonths(4), application.dueDate)
        }
    }

    private fun assertDueDate(applicationId: ApplicationId, expected: LocalDate?) {
        db.read {
            val application = it.fetchApplicationDetails(applicationId)!!
            if (expected != null) {
                assertEquals(expected, application.dueDate)
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
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
        }
        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }
        // then
        assertDueDate(applicationId, null) // missing attachment

        // when
        assertTrue(
            uploadAttachment(
                applicationId,
                AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
            )
        )
        db.transaction { tx ->
            tx.execute {
                sql(
                    "UPDATE attachment SET received_at = ${bind(now.minusWeeks(1))} WHERE application_id = ${bind(applicationId)}"
                )
            }
        }
        assertTrue(
            uploadAttachment(
                applicationId,
                AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
            )
        )
        // then
        assertDueDate(
            applicationId,
            now.plusWeeks(2).toLocalDate(),
        ) // end date >= earliest attachment.receivedAt
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
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
        }
        // when
        assertTrue(
            uploadAttachment(
                applicationId,
                AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
                ApplicationAttachmentType.EXTENDED_CARE,
            )
        )
        assertTrue(
            uploadAttachment(
                applicationId,
                AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
                ApplicationAttachmentType.URGENCY,
            )
        )

        // then
        assertDueDate(applicationId, null) // application not sent

        // when
        db.transaction { tx ->
            tx.execute {
                sql(
                    "UPDATE attachment SET received_at = ${bind(now.plusWeeks(1))} WHERE type = ${bind(ApplicationAttachmentType.EXTENDED_CARE)} AND application_id = ${bind(applicationId)}"
                )
            }
            tx.execute {
                sql(
                    "UPDATE attachment SET received_at = ${bind(now.plusDays(3))} WHERE type = ${bind(ApplicationAttachmentType.URGENCY)} AND application_id = ${bind(applicationId)}"
                )
            }
        }
        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }
        // then
        assertDueDate(
            applicationId,
            now.plusDays(14 + 3).toLocalDate(),
        ) // attachments received after application sent
    }

    @Test
    fun `sendApplication - daycare has not due date if a transfer application`() {
        db.transaction { tx ->
            // given
            val draft =
                tx.insertApplication(
                    appliedType = PlacementType.DAYCARE,
                    urgent = false,
                    applicationId = applicationId,
                    preferredStartDate = LocalDate.of(2020, 8, 1),
                )
            tx.insert(
                DevPlacement(
                    childId = draft.childId,
                    unitId = testDaycare2.id,
                    startDate = draft.form.preferences.preferredStartDate!!,
                    endDate = draft.form.preferences.preferredStartDate!!.plusYears(1),
                )
            )
        }
        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.transferApplication)
            assertEquals(null, application.dueDate)
        }
    }

    @Test
    fun `sendApplication - daycare application is marked as transfer application when child is in 5yo daycare`() {
        db.transaction { tx ->
            // given
            val draft =
                tx.insertApplication(
                    appliedType = PlacementType.DAYCARE,
                    urgent = false,
                    applicationId = applicationId,
                    preferredStartDate = LocalDate.of(2020, 8, 1),
                )
            tx.insert(
                DevPlacement(
                    childId = draft.childId,
                    unitId = testDaycare2.id,
                    startDate = draft.form.preferences.preferredStartDate!!,
                    endDate = draft.form.preferences.preferredStartDate!!.plusYears(1),
                    type = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                )
            )
        }
        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(true, application.transferApplication)
        }
    }

    @Test
    fun `sendApplication - preschool placement already exists -- preschool+connected application is marked as additionalDaycareApplication`() {
        db.transaction { tx ->
            // given
            val draft =
                tx.insertApplication(
                    appliedType = PlacementType.PRESCHOOL_DAYCARE,
                    applicationId = applicationId,
                    preferredStartDate = LocalDate.of(2020, 8, 13),
                )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = draft.childId,
                    unitId = testDaycare2.id,
                    startDate = draft.form.preferences.preferredStartDate!!,
                    endDate = draft.form.preferences.preferredStartDate!!.plusYears(1),
                )
            )
        }

        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }

        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.SENT, application.status)
            assertEquals(true, application.additionalDaycareApplication)
        }
    }

    @Test
    fun `sendApplication - preschool placement already exists -- preparatory+connected application is not marked as additionalDaycareApplication`() {
        db.transaction { tx ->
            // given
            val draft =
                tx.insertApplication(
                    appliedType = PlacementType.PREPARATORY_DAYCARE,
                    applicationId = applicationId,
                    preferredStartDate = LocalDate.of(2020, 8, 13),
                )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = draft.childId,
                    unitId = testDaycare2.id,
                    startDate = draft.form.preferences.preferredStartDate!!,
                    endDate = draft.form.preferences.preferredStartDate!!.plusYears(1),
                )
            )
        }

        db.transaction { tx ->
            // when
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }

        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.SENT, application.status)
            assertEquals(false, application.additionalDaycareApplication)
        }
    }

    @Test
    fun `moveToWaitingPlacement without otherInfo - status is changed and checkedByAdmin defaults true`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
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
                otherInfo = "something",
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
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
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
        }
        db.read { tx ->
            // then
            val guardian = tx.getPersonById(testAdult_5.id)!!
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
                guardianEmail = "",
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
        }
        db.read { tx ->
            // then
            val guardian = tx.getPersonById(testAdult_6.id)!!
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
                diet = "vegaani",
                allergies = "pähkinät",
                applicationId = applicationId,
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
        }
        db.read { tx ->
            // then
            val childDetails = tx.getChild(testChild_6.id)!!.additionalInformation
            assertEquals("vegaani", childDetails.diet)
            assertEquals("pähkinät", childDetails.allergies)
        }
    }

    @Test
    fun `setVerified - changes checkedByAdmin`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                diet = "vegaani",
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.setVerified(tx, serviceWorker, clock, applicationId, confidential = null)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(true, application.checkedByAdmin)
        }
    }

    @Test
    fun `cancelApplication from SENT - status is changed`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.cancelApplication(tx, serviceWorker, clock, applicationId, null)
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
            tx.insertApplication(
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.cancelApplication(tx, serviceWorker, clock, applicationId, null)
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
            tx.insertApplication(
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.returnToSent(tx, serviceWorker, clock, applicationId)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.SENT, application.status)
        }
    }

    @Test
    fun `returnToSent after cancelling - status is changed and process history is reverted`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.cancelApplication(tx, serviceWorker, clock, applicationId, null)

            val process = tx.getCaseProcessByApplicationId(applicationId)
            assertNotNull(process)
            assertTrue(process.history.any { it.state == CaseProcessState.COMPLETED })
        }

        db.transaction { tx ->
            // when
            service.returnToSent(tx, serviceWorker, clock, applicationId)
        }

        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.SENT, application.status)
            val process = tx.getCaseProcessByApplicationId(applicationId)
            assertNotNull(process)
            assertFalse(process.history.any { it.state == CaseProcessState.COMPLETED })
        }
    }

    @Test
    fun `createPlacementPlan - daycare`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.DAYCARE,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(unitId = testDaycare.id, period = mainPeriod),
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

            val placementPlan = tx.getPlacementPlan(applicationId)!!
            assertEquals(
                PlacementPlan(
                    id = placementPlan.id,
                    unitId = testDaycare.id,
                    applicationId = applicationId,
                    type = PlacementType.DAYCARE,
                    period = mainPeriod,
                    preschoolDaycarePeriod = null,
                ),
                placementPlan,
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
                    planned = true,
                ),
                decisionDrafts.first(),
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
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(unitId = testDaycare.id, period = mainPeriod),
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

            val placementPlan = tx.getPlacementPlan(applicationId)!!
            assertEquals(
                PlacementPlan(
                    id = placementPlan.id,
                    unitId = testDaycare.id,
                    applicationId = applicationId,
                    type = PlacementType.DAYCARE_PART_TIME,
                    period = mainPeriod,
                    preschoolDaycarePeriod = null,
                ),
                placementPlan,
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
                    planned = true,
                ),
                decisionDrafts.first(),
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
                preferredStartDate = LocalDate.of(2020, 8, 13),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(unitId = testDaycare.id, period = mainPeriod),
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

            val placementPlan = tx.getPlacementPlan(applicationId)!!
            assertEquals(
                PlacementPlan(
                    id = placementPlan.id,
                    unitId = testDaycare.id,
                    applicationId = applicationId,
                    type = PlacementType.PRESCHOOL,
                    period = mainPeriod,
                    preschoolDaycarePeriod = null,
                ),
                placementPlan,
            )

            val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
            assertEquals(2, decisionDrafts.size)

            decisionDrafts
                .find { it.type == DecisionType.PRESCHOOL }!!
                .let {
                    assertEquals(
                        DecisionDraft(
                            id = it.id,
                            type = DecisionType.PRESCHOOL,
                            startDate = mainPeriod.start,
                            endDate = mainPeriod.end,
                            unitId = testDaycare.id,
                            planned = true,
                        ),
                        it,
                    )
                }
            decisionDrafts
                .find { it.type == DecisionType.PRESCHOOL_DAYCARE }!!
                .let {
                    assertEquals(
                        DecisionDraft(
                            id = it.id,
                            type = DecisionType.PRESCHOOL_DAYCARE,
                            startDate = mainPeriod.start,
                            endDate = mainPeriod.end,
                            unitId = testDaycare.id,
                            planned = false,
                        ),
                        it,
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
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod,
                ),
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

            val placementPlan = tx.getPlacementPlan(applicationId)!!
            assertEquals(
                PlacementPlan(
                    id = placementPlan.id,
                    unitId = testDaycare.id,
                    applicationId = applicationId,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod,
                ),
                placementPlan,
            )

            val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
            assertEquals(2, decisionDrafts.size)

            decisionDrafts
                .find { it.type == DecisionType.PRESCHOOL }!!
                .let {
                    assertEquals(
                        DecisionDraft(
                            id = it.id,
                            type = DecisionType.PRESCHOOL,
                            startDate = mainPeriod.start,
                            endDate = mainPeriod.end,
                            unitId = testDaycare.id,
                            planned = true,
                        ),
                        it,
                    )
                }
            decisionDrafts
                .find { it.type == DecisionType.PRESCHOOL_DAYCARE }!!
                .let {
                    assertEquals(
                        DecisionDraft(
                            id = it.id,
                            type = DecisionType.PRESCHOOL_DAYCARE,
                            startDate = connectedPeriod.start,
                            endDate = connectedPeriod.end,
                            unitId = testDaycare.id,
                            planned = true,
                        ),
                        it,
                    )
                }
        }
    }

    @Test
    fun `createPlacementPlan - preschool with club`() {
        db.transaction { tx ->
            // given
            val serviceNeedOption =
                ServiceNeedOption(
                    id = snPreschoolClub45.id,
                    nameFi = snPreschoolClub45.nameFi,
                    nameSv = snPreschoolClub45.nameSv,
                    nameEn = snPreschoolClub45.nameEn,
                    validPlacementType = PlacementType.PRESCHOOL_CLUB,
                )
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL_CLUB,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
                serviceNeedOption = serviceNeedOption,
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod,
                ),
            )
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

            val placementPlan = tx.getPlacementPlan(applicationId)!!
            assertEquals(
                PlacementPlan(
                    id = placementPlan.id,
                    unitId = testDaycare.id,
                    applicationId = applicationId,
                    type = PlacementType.PRESCHOOL_CLUB,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod,
                ),
                placementPlan,
            )

            val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
            assertEquals(2, decisionDrafts.size)

            decisionDrafts
                .find { it.type == DecisionType.PRESCHOOL }!!
                .let {
                    assertEquals(
                        DecisionDraft(
                            id = it.id,
                            type = DecisionType.PRESCHOOL,
                            startDate = mainPeriod.start,
                            endDate = mainPeriod.end,
                            unitId = testDaycare.id,
                            planned = true,
                        ),
                        it,
                    )
                }
            decisionDrafts
                .find { it.type == DecisionType.PRESCHOOL_CLUB }!!
                .let {
                    assertEquals(
                        DecisionDraft(
                            id = it.id,
                            type = DecisionType.PRESCHOOL_CLUB,
                            startDate = connectedPeriod.start,
                            endDate = connectedPeriod.end,
                            unitId = testDaycare.id,
                            planned = true,
                        ),
                        it,
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
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod,
                ),
            )
        }
        db.transaction { tx ->
            // when
            service.cancelPlacementPlan(tx, serviceWorker, clock, applicationId)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)

            val placementPlan = tx.getPlacementPlan(applicationId)
            assertEquals(null, placementPlan)

            val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
            assertEquals(0, decisionDrafts.size)
        }
    }

    @Test
    fun `sendDecisionsWithoutProposal - applier is the only guardian`() =
        sendDecisionsWithoutProposalTest(
            child = testChild_2,
            applier = testAdult_1,
            applierIsGuardian = true,
            secondDecisionTo = null,
            manualMailing = false,
        )

    @Test
    fun `sendDecisionsWithoutProposal - applier is guardian and other guardian exists in same address`() =
        sendDecisionsWithoutProposalTest(
            child = testChild_1,
            applier = testAdult_1,
            applierIsGuardian = true,
            secondDecisionTo = null,
            manualMailing = false,
        )

    @Test
    fun `sendDecisionsWithoutProposal - applier is guardian and other guardian exists in different address`() =
        sendDecisionsWithoutProposalTest(
            child = testChild_6,
            applier = testAdult_5,
            applierIsGuardian = true,
            secondDecisionTo = testAdult_6,
            manualMailing = false,
        )

    @Test
    fun `sendDecisionsWithoutProposal - child has no ssn`() =
        sendDecisionsWithoutProposalTest(
            child = testChild_7,
            applier = testAdult_5,
            applierIsGuardian = false,
            secondDecisionTo = null,
            manualMailing = true,
        )

    @Test
    fun `sendDecisionsWithoutProposal - applier has no ssn, child has no guardian`() =
        sendDecisionsWithoutProposalTest(
            child = testChild_7,
            applier = testAdult_4,
            applierIsGuardian = false,
            secondDecisionTo = null,
            manualMailing = true,
        )

    @Test
    fun `sendDecisionsWithoutProposal - applier has no ssn, child has another guardian`() =
        sendDecisionsWithoutProposalTest(
            child = testChild_2,
            applier = testAdult_4,
            applierIsGuardian = false,
            secondDecisionTo = testChild_1,
            manualMailing = true,
        )

    private fun sendDecisionsWithoutProposalTest(
        child: DevPerson,
        applier: DevPerson,
        applierIsGuardian: Boolean,
        secondDecisionTo: DevPerson?,
        manualMailing: Boolean,
    ) {
        // given
        db.transaction { tx ->
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL,
                guardian = applier,
                child = child,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 13),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(unitId = testDaycare.id, period = mainPeriod),
            )
        }

        // when
        db.transaction { tx ->
            service.sendDecisionsWithoutProposal(tx, serviceWorker, clock, applicationId)
        }
        asyncJobRunner.runPendingJobsSync(clock)

        // then
        db.read {
            val application = it.fetchApplicationDetails(applicationId)!!
            if (manualMailing) {
                assertEquals(ApplicationStatus.WAITING_MAILING, application.status)
            } else {
                assertEquals(ApplicationStatus.WAITING_CONFIRMATION, application.status)
            }

            val decisionsByApplication =
                it.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll)
            assertEquals(1, decisionsByApplication.size)
            val decision = decisionsByApplication.first()
            if (!manualMailing) {
                assertNotNull(decision.sentDate)
            }
            assertNotNull(decision.documentKey)
        }

        val messages = MockSfiMessagesClient.getMessages()
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

        if (manualMailing) {
            assertThat(MockEmailClient.emails)
                .extracting({ it.toAddress }, { it.content.subject })
                .containsExactly(
                    Tuple(
                        "abc@espoo.fi",
                        "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
                    )
                )
        } else {
            assertThat(MockEmailClient.emails)
                .extracting({ it.toAddress }, { it.content.subject })
                .containsExactly(
                    Tuple(
                        "abc@espoo.fi",
                        "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
                    ),
                    Tuple(
                        "abc@espoo.fi",
                        "Uusi päätös eVakassa / Nytt beslut i eVaka / New decision in eVaka",
                    ),
                )
        }

        MockEmailClient.clear()

        if (manualMailing) {
            db.transaction { tx ->
                service.confirmDecisionMailed(tx, serviceWorker, clock, applicationId)
            }
            asyncJobRunner.runPendingJobsSync(clock)

            db.read {
                val application = it.fetchApplicationDetails(applicationId)!!
                assertEquals(ApplicationStatus.WAITING_CONFIRMATION, application.status)
            }

            if (applierIsGuardian) {
                assertThat(MockEmailClient.emails)
                    .extracting({ it.toAddress }, { it.content.subject })
                    .containsExactly(
                        Tuple(
                            "abc@espoo.fi",
                            "Uusi päätös eVakassa / Nytt beslut i eVaka / New decision in eVaka",
                        )
                    )
            } else {
                assertThat(MockEmailClient.emails)
                    .extracting({ it.toAddress }, { it.content.subject })
                    .isEmpty()
            }
        }
    }

    @Test
    fun `sendPlacementProposal - updates status`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL_DAYCARE,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod,
                ),
            )
            // when
            service.sendPlacementProposal(tx, serviceWorker, clock, applicationId)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_UNIT_CONFIRMATION, application.status)

            val decisions =
                tx.getSentDecisionsByApplication(applicationId, AccessControlFilter.PermitAll)
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
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod,
                ),
            )
            service.sendPlacementProposal(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.withdrawPlacementProposal(tx, serviceWorker, clock, applicationId)
        }
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

            val decisions =
                tx.getSentDecisionsByApplication(applicationId, AccessControlFilter.PermitAll)
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
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod,
                ),
            )
            service.sendPlacementProposal(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.respondToPlacementProposal(
                tx,
                serviceWorker,
                clock,
                applicationId,
                PlacementPlanConfirmationStatus.ACCEPTED,
            )
            service.confirmPlacementProposalChanges(
                tx,
                serviceWorker,
                clock,
                testDaycare.id,
                rejectReasonTranslations =
                    enumEntries<PlacementPlanRejectReason>().associateBy({ it }, { it.name }),
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_CONFIRMATION, application.status)

            val notes = tx.getApplicationNotes(applicationId)
            assertEquals(emptyList(), notes)

            val decisionsByApplication =
                tx.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll)
            assertEquals(2, decisionsByApplication.size)
            decisionsByApplication.forEach { decision ->
                assertNotNull(decision.sentDate)
                assertNotNull(decision.documentKey)
            }
            val messages = MockSfiMessagesClient.getMessages()
            assertEquals(2, messages.size)
            assertEquals(2, messages.filter { it.ssn == testAdult_1.ssn }.size)
            assertThat(MockEmailClient.emails)
                .extracting({ it.toAddress }, { it.content.subject })
                .containsExactly(
                    Tuple(
                        "abc@espoo.fi",
                        "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
                    ),
                    Tuple(
                        "abc@espoo.fi",
                        "Uusi päätös eVakassa / Nytt beslut i eVaka / New decision in eVaka",
                    ),
                )
        }
    }

    @Test
    fun `acceptPlacementProposal - if no decisions are marked for sending do nothing`() {
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL_DAYCARE,
                child = testChild_2,
                guardian = testAdult_1,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod,
                ),
            )
            tx.fetchDecisionDrafts(applicationId)
                .map { draft ->
                    DecisionDraftUpdate(
                        id = draft.id,
                        unitId = draft.unitId,
                        startDate = draft.startDate,
                        endDate = draft.endDate,
                        planned = false,
                    )
                }
                .let { updates -> updateDecisionDrafts(tx, applicationId, updates) }
            service.sendPlacementProposal(tx, serviceWorker, clock, applicationId)
        }
        db.transaction { tx ->
            // when
            service.respondToPlacementProposal(
                tx,
                serviceWorker,
                clock,
                applicationId,
                PlacementPlanConfirmationStatus.ACCEPTED,
            )
            service.confirmPlacementProposalChanges(
                tx,
                serviceWorker,
                clock,
                testDaycare.id,
                rejectReasonTranslations =
                    enumEntries<PlacementPlanRejectReason>().associateBy({ it }, { it.name }),
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_UNIT_CONFIRMATION, application.status)

            val notes = tx.getApplicationNotes(applicationId)
            assertEquals(emptyList(), notes)

            val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
            assertEquals(2, decisionDrafts.size)

            val decisionsByApplication =
                tx.getSentDecisionsByApplication(applicationId, AccessControlFilter.PermitAll)
            assertEquals(0, decisionsByApplication.size)

            val messages = MockSfiMessagesClient.getMessages()
            assertEquals(0, messages.size)

            assertThat(MockEmailClient.emails)
                .extracting({ it.toAddress }, { it.content.subject })
                .containsExactly(
                    Tuple(
                        "abc@espoo.fi",
                        "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
                    )
                )

            val placementPlan = tx.getPlacementPlan(applicationId)
            assertNotNull(placementPlan)

            val placements = tx.getPlacementsForChild(testChild_2.id)
            assertEquals(0, placements.size)

            assertEquals(1, tx.getParentships(testAdult_1.id, testChild_2.id).size)
            assertEquals(0, tx.getParentships(testAdult_2.id, testChild_2.id).size)
        }
    }

    @Test
    fun `confirmPlacementProposalChanges - reject reason is copied to service worker's application notes`() {
        val rejectReason = "päiväkoti täynnä"
        val previousNoteContent = "Aiempi muistilapun sisältö."
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL_DAYCARE,
                child = testChild_2,
                guardian = testAdult_1,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod,
                ),
            )
            service.sendPlacementProposal(tx, serviceWorker, clock, applicationId)
            tx.updateServiceWorkerApplicationNote(applicationId, previousNoteContent)
        }
        db.transaction { tx ->
            // when
            service.respondToPlacementProposal(
                tx,
                serviceWorker,
                clock,
                applicationId,
                PlacementPlanConfirmationStatus.REJECTED_NOT_CONFIRMED,
                PlacementPlanRejectReason.REASON_1,
            )
            service.confirmPlacementProposalChanges(
                tx,
                serviceWorker,
                clock,
                testDaycare.id,
                rejectReasonTranslations = mapOf(PlacementPlanRejectReason.REASON_1 to rejectReason),
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertNull(tx.getPlacementPlan(applicationId))

            val dateTimeString =
                clock
                    .now()
                    .toZonedDateTime()
                    .format(
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                            .withLocale(Locale.of("fi", "FI"))
                    )
            val expectedNote =
                "$previousNoteContent\nSijoitusehdotus hylätty (${testDaycare.name}) - $rejectReason - ${testDecisionMaker_1.firstName} ${testDecisionMaker_1.lastName} $dateTimeString"
            val note = tx.getServiceWorkerApplicationNote(applicationId)
            assertEquals(expectedNote, note)

            val decisionsByApplication =
                tx.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll)
            assertEquals(0, decisionsByApplication.size)
            val messages = MockSfiMessagesClient.getMessages()
            assertEquals(0, messages.size)
            assertThat(MockEmailClient.emails)
                .extracting({ it.toAddress }, { it.content.subject })
                .containsExactly(
                    Tuple(
                        "abc@espoo.fi",
                        "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
                    )
                )
        }
    }

    @Test
    fun `confirmPlacementProposalChanges - reject other reason is copied to service worker's application notes`() {
        val rejectReason = "päiväkoti täynnä"
        val previousNoteContent = "Aiempi muistilapun sisältö."
        db.transaction { tx ->
            // given
            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL_DAYCARE,
                child = testChild_2,
                guardian = testAdult_1,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod,
                ),
            )
            service.sendPlacementProposal(tx, serviceWorker, clock, applicationId)
            tx.updateServiceWorkerApplicationNote(applicationId, previousNoteContent)
        }
        db.transaction { tx ->
            // when
            service.respondToPlacementProposal(
                tx,
                serviceWorker,
                clock,
                applicationId,
                PlacementPlanConfirmationStatus.REJECTED_NOT_CONFIRMED,
                PlacementPlanRejectReason.OTHER,
                rejectReason,
            )
            service.confirmPlacementProposalChanges(
                tx,
                serviceWorker,
                clock,
                testDaycare.id,
                rejectReasonTranslations = mapOf(PlacementPlanRejectReason.OTHER to "Muu syy"),
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)
        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertNull(tx.getPlacementPlan(applicationId))

            val dateTimeString =
                clock
                    .now()
                    .toZonedDateTime()
                    .format(
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                            .withLocale(Locale.of("fi", "FI"))
                    )

            val expectedNote =
                "$previousNoteContent\nSijoitusehdotus hylätty (${testDaycare.name}) - Muu syy: $rejectReason - ${testDecisionMaker_1.firstName} ${testDecisionMaker_1.lastName} $dateTimeString"
            val note = tx.getServiceWorkerApplicationNote(applicationId)
            assertEquals(expectedNote, note)

            val decisionsByApplication =
                tx.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll)
            assertEquals(0, decisionsByApplication.size)
            val messages = MockSfiMessagesClient.getMessages()
            assertEquals(0, messages.size)
            assertThat(MockEmailClient.emails)
                .extracting({ it.toAddress }, { it.content.subject })
                .containsExactly(
                    Tuple(
                        "abc@espoo.fi",
                        "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
                    )
                )
        }
    }

    @Test
    fun `confirmPlacementProposalChanges - preschool decision is not sent to guardian if skip is enabled`() {
        workflowForPreschoolDecisions(
            config = testFeatureConfig.copy(skipGuardianPreschoolDecisionApproval = true)
        )

        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.ACTIVE, application.status)

            with(getDecision(tx, DecisionType.PRESCHOOL)) {
                assertEquals(DecisionStatus.ACCEPTED, status)
            }
        }
    }

    @Test
    fun `confirmPlacementProposalChanges - preparatory decision is not sent to guardian if skip is enabled`() {
        workflowForPreschoolDecisions(
            config = testFeatureConfig.copy(skipGuardianPreschoolDecisionApproval = true)
        )

        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.ACTIVE, application.status)

            with(getDecision(tx, DecisionType.PRESCHOOL)) {
                assertEquals(DecisionStatus.ACCEPTED, status)
            }
        }
    }

    @Test
    fun `confirmPlacementProposalChanges - preschool daycare decision is sent to guardian even if skip is enabled`() {
        workflowForPreschoolDaycareDecisions(
            config = testFeatureConfig.copy(skipGuardianPreschoolDecisionApproval = true)
        )

        db.read { tx ->
            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.WAITING_CONFIRMATION, application.status)

            with(getDecision(tx, DecisionType.PRESCHOOL)) {
                assertEquals(DecisionStatus.ACCEPTED, status)
            }
            with(getDecision(tx, DecisionType.PRESCHOOL_DAYCARE)) {
                assertEquals(DecisionStatus.PENDING, status)
            }
        }
    }

    @Test
    fun `acceptPlacementProposal - if partner already has a fridge family, child is added to that`() {
        val partnershipId = PartnershipId(UUID.randomUUID())
        val parentshipId = ParentshipId(UUID.randomUUID())
        val startDate = clock.today()

        db.transaction { tx ->
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 1,
                    otherIndx = 2,
                    personId = testAdult_1.id,
                    startDate = startDate,
                    createdAt = clock.now(),
                )
            )
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 2,
                    otherIndx = 1,
                    personId = testAdult_2.id,
                    startDate = startDate,
                    createdAt = clock.now(),
                )
            )

            tx.insertGuardian(testAdult_2.id, testChild_1.id)

            tx.insert(
                DevParentship(
                    parentshipId,
                    testChild_1.id,
                    testAdult_2.id,
                    startDate,
                    startDate.plusMonths(12),
                )
            )

            tx.insertApplication(
                appliedType = PlacementType.PRESCHOOL_DAYCARE,
                child = testChild_2,
                guardian = testAdult_1,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
        }

        asyncJobRunner.runPendingJobsSync(clock)

        db.read { tx ->
            assertEquals(1, tx.getParentships(testAdult_2.id, testChild_2.id).size)
            assertEquals(0, tx.getParentships(testAdult_1.id, testChild_2.id).size)
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
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
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

            val placementPlan = tx.getPlacementPlan(applicationId)
            assertNull(placementPlan)

            val placements = tx.getPlacementsForChild(testChild_6.id)
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
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start,
            )
            service.rejectDecision(
                tx,
                serviceWorker,
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL_DAYCARE).id,
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

            val placementPlan = tx.getPlacementPlan(applicationId)
            assertNull(placementPlan)

            val placements = tx.getPlacementsForChild(testChild_6.id)
            assertEquals(1, placements.size)
            with(placements.first()) {
                assertEquals(mainPeriod.start, startDate)
                assertEquals(mainPeriod.end, endDate)
                assertEquals(PlacementType.PRESCHOOL, type)
            }
        }
    }

    @Test
    fun `enduser can accept and reject own decisions`() {
        // given
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            // when
            val user = AuthenticatedUser.Citizen(testAdult_5.id, CitizenAuthLevel.STRONG)
            service.acceptDecision(
                tx,
                user,
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start,
            )
            service.rejectDecision(
                tx,
                user,
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL_DAYCARE).id,
            )

            // then
            val application = tx.fetchApplicationDetails(applicationId)!!
            assertEquals(ApplicationStatus.ACTIVE, application.status)

            // application is recorded as the placement's source
            val placements = tx.getPlacementsForChild(testChild_6.id)
            placements.single().also { placement ->
                assertEquals(placement.source, PlacementSource.APPLICATION)
                assertEquals(placement.sourceApplicationId, applicationId)
            }
        }
    }

    @Test
    fun `enduser can not accept decision of someone else`() {
        // given
        workflowForPreschoolDaycareDecisions()

        db.transaction { tx ->
            val user = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)
            // when
            assertThrows<Forbidden> {
                service.acceptDecision(
                    tx,
                    user,
                    clock,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL).id,
                    mainPeriod.start,
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
            val user = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)
            assertThrows<Forbidden> {
                service.rejectDecision(
                    tx,
                    user,
                    clock,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL).id,
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
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start,
            )
            service.acceptDecision(
                tx,
                serviceWorker,
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL_DAYCARE).id,
                connectedPeriod.start,
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

            val placementPlan = tx.getPlacementPlan(applicationId)
            assertNull(placementPlan)

            val placements = tx.getPlacementsForChild(testChild_6.id)
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
    fun `daycare with unknown service need option`() {
        // given
        val serviceNeedOption =
            ServiceNeedOption(
                ServiceNeedOptionId(UUID.randomUUID()),
                "unknown service need option",
                "unknown service need option",
                "unknown service need option",
                null,
            )
        workflowForPreschoolDaycareDecisions(serviceNeedOption = serviceNeedOption)

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start,
            )
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(
                serviceNeedOption,
                application.form.preferences.serviceNeed?.serviceNeedOption,
            )
            val serviceNeeds = it.getServiceNeedsByChild(application.childId)
            assertThat(serviceNeeds).isEmpty()
        }
    }

    @Test
    fun `daycare with known service need option`() {
        db.transaction { tx -> tx.insert(snPreschoolDaycare45) }

        // given
        val serviceNeedOption =
            ServiceNeedOption(
                snPreschoolDaycare45.id,
                snPreschoolDaycare45.nameFi,
                snPreschoolDaycare45.nameSv,
                snPreschoolDaycare45.nameEn,
                validPlacementType = null,
            )
        workflowForPreschoolDaycareDecisions(serviceNeedOption = serviceNeedOption)

        db.transaction { tx ->
            // when
            service.acceptDecision(
                tx,
                serviceWorker,
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start,
            )
            service.acceptDecision(
                tx,
                serviceWorker,
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL_DAYCARE).id,
                LocalDate.of(2020, 8, 1),
            )
        }
        db.read {
            // then
            val application = it.fetchApplicationDetails(applicationId)!!
            assertEquals(
                serviceNeedOption,
                application.form.preferences.serviceNeed?.serviceNeedOption,
            )
            val serviceNeeds = it.getServiceNeedsByChild(application.childId)
            assertEquals(listOf(serviceNeedOption.id), serviceNeeds.map { sn -> sn.option.id })
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
                    clock,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL_DAYCARE).id,
                    mainPeriod.start,
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
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start,
            )
        }
        db.transaction { tx ->
            // when / then
            assertThrows<BadRequest> {
                service.acceptDecision(
                    tx,
                    serviceWorker,
                    clock,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL).id,
                    mainPeriod.start,
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
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
            )
        }
        db.transaction { tx ->
            // when / then
            assertThrows<BadRequest> {
                service.acceptDecision(
                    tx,
                    serviceWorker,
                    clock,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL).id,
                    mainPeriod.start,
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
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
                mainPeriod.start,
            )
        }
        db.transaction { tx ->
            // when / then
            assertThrows<BadRequest> {
                service.rejectDecision(
                    tx,
                    serviceWorker,
                    clock,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL).id,
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
                clock,
                applicationId,
                getDecision(tx, DecisionType.PRESCHOOL).id,
            )
        }
        db.transaction { tx ->
            // when / then
            assertThrows<BadRequest> {
                service.rejectDecision(
                    tx,
                    serviceWorker,
                    clock,
                    applicationId,
                    getDecision(tx, DecisionType.PRESCHOOL).id,
                )
            }
        }
    }

    @Test
    fun `metadata is collected`() {
        val guardian = DevPerson()
        val child = DevPerson()
        val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
        val testApplicationId =
            db.transaction { tx ->
                tx.insert(guardian, DevPersonType.ADULT)
                tx.insert(child, DevPersonType.RAW_ROW)
                tx.insert(admin)
                service.createApplication(
                    tx = tx,
                    user = guardian.user(CitizenAuthLevel.STRONG),
                    now = clock.now(),
                    origin = ApplicationOrigin.ELECTRONIC,
                    type = ApplicationType.DAYCARE,
                    guardian = tx.getPersonById(guardian.id)!!,
                    child = tx.getPersonById(child.id)!!,
                )
            }
        val decisionId =
            db.transaction { tx ->
                service.updateOwnApplicationContentsCitizen(
                    tx = tx,
                    user = guardian.user(CitizenAuthLevel.STRONG),
                    now = clock.now(),
                    applicationId = testApplicationId,
                    update =
                        CitizenApplicationUpdate(
                            form =
                                ApplicationFormUpdate(
                                    child =
                                        ChildDetailsUpdate(
                                            futureAddress = null,
                                            allergies = "",
                                            diet = "",
                                            assistanceNeeded = false,
                                            assistanceDescription = "",
                                        ),
                                    guardian =
                                        GuardianUpdate(
                                            futureAddress = null,
                                            phoneNumber = "",
                                            email = null,
                                        ),
                                    secondGuardian = null,
                                    otherPartner = null,
                                    otherChildren = emptyList(),
                                    preferences =
                                        Preferences(
                                            preferredUnits =
                                                listOf(
                                                    PreferredUnit(testDaycare.id, testDaycare.name)
                                                ),
                                            preferredStartDate = clock.today().plusMonths(5),
                                            connectedDaycarePreferredStartDate = null,
                                            serviceNeed =
                                                ServiceNeed(
                                                    startTime = "09:00",
                                                    endTime = "17:00",
                                                    shiftCare = false,
                                                    partTime = false,
                                                    serviceNeedOption = null,
                                                ),
                                            siblingBasis = null,
                                            preparatory = false,
                                            urgent = false,
                                        ),
                                    maxFeeAccepted = false,
                                    otherInfo = "",
                                    clubDetails = null,
                                ),
                            allowOtherGuardianAccess = true,
                        ),
                )
                service.sendApplication(
                    tx,
                    guardian.user(CitizenAuthLevel.STRONG),
                    clock,
                    testApplicationId,
                )
                service.moveToWaitingPlacement(tx, serviceWorker, clock, testApplicationId)
                service.createPlacementPlan(
                    tx,
                    serviceWorker,
                    clock,
                    testApplicationId,
                    DaycarePlacementPlan(
                        unitId = testDaycare.id,
                        period =
                            FiniteDateRange(
                                clock.today().plusMonths(5),
                                clock.today().plusYears(3),
                            ),
                        preschoolDaycarePeriod = null,
                    ),
                )
                service.sendDecisionsWithoutProposal(tx, serviceWorker, clock, testApplicationId)
                service.confirmDecisionMailed(tx, serviceWorker, clock, testApplicationId)
                val decisionId =
                    tx.createQuery {
                            sql(
                                "SELECT id FROM decision WHERE application_id = ${bind(testApplicationId)}"
                            )
                        }
                        .exactlyOne<DecisionId>()
                service.acceptDecision(
                    tx = tx,
                    user = guardian.user(CitizenAuthLevel.STRONG),
                    clock = clock,
                    applicationId = testApplicationId,
                    decisionId = decisionId,
                    requestedStartDate = clock.today().plusMonths(5),
                )
                decisionId
            }
        asyncJobRunner.runPendingJobsSync(clock)

        val metadata =
            metadataController
                .getApplicationMetadata(dbInstance(), admin.user, clock, testApplicationId)
                .data
        assertNotNull(metadata)
        assertEquals("1/123.123.a/2020", metadata.process.caseIdentifier)
        assertEquals(120, metadata.process.archiveDurationMonths)
        assertEquals(4, metadata.process.history.size)
        assertEquals(CaseProcessState.INITIAL, metadata.process.history[0].state)
        assertEquals(guardian.evakaUserId(), metadata.process.history[0].enteredBy.id)
        assertEquals(CaseProcessState.PREPARATION, metadata.process.history[1].state)
        assertEquals(serviceWorker.evakaUserId, metadata.process.history[1].enteredBy.id)
        assertEquals(CaseProcessState.DECIDING, metadata.process.history[2].state)
        assertEquals(serviceWorker.evakaUserId, metadata.process.history[2].enteredBy.id)
        assertEquals(CaseProcessState.COMPLETED, metadata.process.history[3].state)
        assertEquals(guardian.evakaUserId(), metadata.process.history[3].enteredBy.id)
        assertEquals("Varhaiskasvatus- ja palvelusetelihakemus", metadata.primaryDocument.name)
        assertEquals(guardian.evakaUserId(), metadata.primaryDocument.createdBy?.id)
        assertEquals(
            HelsinkiDateTime.of(clock.today().atStartOfDay()),
            metadata.primaryDocument.createdAt,
        )
        assertNull(metadata.primaryDocument.downloadPath)
        assertEquals(1, metadata.secondaryDocuments.size)
        metadata.secondaryDocuments[0].also { doc ->
            assertEquals("Päätös varhaiskasvatuksesta", doc.name)
            assertEquals(serviceWorker.evakaUserId, doc.createdBy?.id)
            assertEquals("/employee/decisions/$decisionId/download", doc.downloadPath)
        }
    }

    private fun getDecision(r: Database.Read, type: DecisionType): Decision =
        r.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll).first {
            it.type == type
        }

    private fun workflowForPreschoolDecisions(
        preferredStartDate: LocalDate? = LocalDate.of(2020, 8, 1),
        serviceNeedOption: ServiceNeedOption? = null,
        config: FeatureConfig = testFeatureConfig,
    ) = workflow(PlacementType.PRESCHOOL, preferredStartDate, serviceNeedOption, config)

    private fun workflowForPreparatoryDecisions(
        preferredStartDate: LocalDate? = LocalDate.of(2020, 8, 1),
        serviceNeedOption: ServiceNeedOption? = null,
        config: FeatureConfig = testFeatureConfig,
    ) = workflow(PlacementType.PREPARATORY, preferredStartDate, serviceNeedOption, config)

    private fun workflowForPreschoolDaycareDecisions(
        preferredStartDate: LocalDate? = LocalDate.of(2020, 8, 1),
        serviceNeedOption: ServiceNeedOption? = null,
        config: FeatureConfig = testFeatureConfig,
    ) = workflow(PlacementType.PRESCHOOL_DAYCARE, preferredStartDate, serviceNeedOption, config)

    private fun workflow(
        appliedType: PlacementType,
        preferredStartDate: LocalDate? = LocalDate.of(2020, 8, 1),
        serviceNeedOption: ServiceNeedOption? = null,
        config: FeatureConfig = testFeatureConfig,
    ) {
        db.transaction { tx ->
            tx.insertApplication(
                guardian = testAdult_5,
                maxFeeAccepted = true,
                appliedType = appliedType,
                applicationId = applicationId,
                preferredStartDate = preferredStartDate,
                serviceNeedOption = serviceNeedOption,
            )
            service.sendApplication(tx, serviceWorker, clock, applicationId)
            service.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
            service.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod,
                ),
            )
            service.sendDecisionsWithoutProposal(tx, serviceWorker, clock, applicationId, config)
        }
    }
}
