// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.AuditContext
import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationStateService
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.DaycarePlacementPlan
import evaka.core.application.SimpleApplicationAction
import evaka.core.application.fetchApplicationDetails
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.daycare.domain.Language
import evaka.core.decision.DecisionType
import evaka.core.decision.DecisionType.DAYCARE
import evaka.core.decision.getDecisionsByApplication
import evaka.core.decision.getSentDecisionsByApplication
import evaka.core.placement.PlacementPlanConfirmationStatus
import evaka.core.shared.ApplicationId
import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDecisionReasoningGeneric
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired

class PlacementProposalReasoningIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 5, 11), LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)
    private val today: LocalDate = now.toLocalDate()
    private val startDate: LocalDate = LocalDate.of(2026, 8, 1)

    @Autowired private lateinit var applicationStateService: ApplicationStateService

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(admin) }
    }

    private fun insertGenericReasoning(
        ready: Boolean = true,
        textSv: String = "sv-generic-text",
        validFrom: LocalDate = LocalDate.of(2026, 1, 1),
        createdAt: HelsinkiDateTime = now,
        collectionType: DecisionReasoningCollectionType = DecisionReasoningCollectionType.DAYCARE,
    ): DecisionGenericReasoningId = db.transaction { tx ->
        tx.insert(
            DevDecisionReasoningGeneric(
                collectionType = collectionType,
                validFrom = validFrom,
                textFi = "fi-generic-text",
                textSv = textSv,
                ready = ready,
                createdAt = createdAt,
                modifiedAt = createdAt,
            )
        )
    }

    private data class AppFixture(
        val decisionId: DecisionId,
        val applicationId: ApplicationId,
        val unitId: DaycareId,
    )

    private fun createWaitingDecisionApplication(
        unitLanguage: Language = Language.fi,
        type: DecisionType = DAYCARE,
    ): AppFixture = db.transaction { tx ->
        val guardianId = tx.insert(DevPerson(), DevPersonType.ADULT)
        val childId = tx.insert(DevPerson(dateOfBirth = today.minusYears(3)), DevPersonType.CHILD)
        val areaSuffix = UUID.randomUUID()
        val areaId =
            tx.insert(
                DevCareArea(
                    name = "Test Care Area $areaSuffix",
                    shortName = "test_area_$areaSuffix",
                )
            )
        val unitId = tx.insert(DevDaycare(areaId = areaId, language = unitLanguage))

        val isPreschool =
            type in
                setOf(
                    DecisionType.PRESCHOOL,
                    DecisionType.PRESCHOOL_DAYCARE,
                    DecisionType.PREPARATORY_EDUCATION,
                    DecisionType.PRESCHOOL_CLUB,
                )
        val appType = if (isPreschool) ApplicationType.PRESCHOOL else ApplicationType.DAYCARE
        val hasConnectedDaycare = type == DecisionType.PRESCHOOL_DAYCARE

        val applicationId =
            tx.insertTestApplication(
                status = ApplicationStatus.WAITING_PLACEMENT,
                guardianId = guardianId,
                childId = childId,
                type = appType,
                document =
                    DaycareFormV0(
                        type = appType,
                        connectedDaycare =
                            if (appType == ApplicationType.PRESCHOOL) hasConnectedDaycare else null,
                        serviceStart = if (hasConnectedDaycare) "08:00" else null,
                        serviceEnd = if (hasConnectedDaycare) "16:00" else null,
                        child = Child(dateOfBirth = today.minusYears(3)),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(unitId)),
                        preferredStartDate = startDate,
                    ),
            )
        applicationStateService.setVerified(
            tx = tx,
            user = admin.user,
            clock = clock,
            audit = AuditContext(),
            applicationId = applicationId,
            confidential = false,
        )
        applicationStateService.createPlacementPlan(
            tx = tx,
            user = admin.user,
            clock = clock,
            AuditContext(),
            applicationId = applicationId,
            placementPlan =
                DaycarePlacementPlan(
                    unitId = unitId,
                    period = FiniteDateRange(startDate, startDate.plusYears(1)),
                    preschoolDaycarePeriod =
                        if (hasConnectedDaycare) FiniteDateRange(startDate, startDate.plusYears(1))
                        else null,
                ),
        )
        val decisionId =
            tx.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll)
                .first { it.type == type }
                .id
        AppFixture(decisionId, applicationId, unitId)
    }

    private fun sendProposal(applicationId: ApplicationId) {
        db.transaction { tx ->
            applicationStateService.sendPlacementProposal(
                tx,
                admin.user,
                clock,
                AuditContext(),
                applicationId,
            )
        }
    }

    private fun linkedReasoningId(decisionId: DecisionId): DecisionGenericReasoningId? =
        db.read { tx ->
            tx.createQuery {
                    sql(
                        "SELECT generic_reasoning_id FROM decision WHERE id = ${bind(decisionId)} AND generic_reasoning_id IS NOT NULL"
                    )
                }
                .toList<DecisionGenericReasoningId>()
                .singleOrNull()
        }

    private fun applicationStatus(applicationId: ApplicationId): ApplicationStatus = db.read { tx ->
        tx.fetchApplicationDetails(applicationId)!!.status
    }

    @Test
    fun `sendPlacementProposal links the applicable ready reasoning to planned decisions`() {
        val reasoningId = insertGenericReasoning(ready = true)
        val fixture = createWaitingDecisionApplication()

        sendProposal(fixture.applicationId)

        assertEquals(
            ApplicationStatus.WAITING_UNIT_CONFIRMATION,
            applicationStatus(fixture.applicationId),
        )
        assertEquals(reasoningId, linkedReasoningId(fixture.decisionId))
    }

    @Test
    fun `sendPlacementProposal fails with error code when the applicable reasoning is not ready`() {
        insertGenericReasoning(ready = false)
        val fixture = createWaitingDecisionApplication()

        val exception = assertThrows<Conflict> { sendProposal(fixture.applicationId) }
        assertEquals(DECISION_REASONING_NOT_FINALIZED, exception.errorCode)
        assertEquals(ApplicationStatus.WAITING_DECISION, applicationStatus(fixture.applicationId))
        assertNull(linkedReasoningId(fixture.decisionId))
    }

    @Test
    fun `sendPlacementProposal fails with error code when no applicable reasoning exists`() {
        val fixture = createWaitingDecisionApplication()

        val exception = assertThrows<Conflict> { sendProposal(fixture.applicationId) }
        assertEquals(DECISION_REASONING_NOT_FINALIZED, exception.errorCode)
    }

    @Test
    fun `sendPlacementProposal fails with error code for a Swedish decision with blank Swedish text`() {
        insertGenericReasoning(ready = true, textSv = "")
        val fixture = createWaitingDecisionApplication(unitLanguage = Language.sv)

        val exception = assertThrows<Conflict> { sendProposal(fixture.applicationId) }
        assertEquals(DECISION_REASONING_NOT_FINALIZED, exception.errorCode)
    }

    @Test
    fun `an unplanned draft does not block sendPlacementProposal and gets no link`() {
        val fixture = createWaitingDecisionApplication()
        db.transaction { tx ->
            tx.execute {
                sql("UPDATE decision SET planned = false WHERE id = ${bind(fixture.decisionId)}")
            }
        }

        sendProposal(fixture.applicationId)

        assertEquals(
            ApplicationStatus.WAITING_UNIT_CONFIRMATION,
            applicationStatus(fixture.applicationId),
        )
        assertNull(linkedReasoningId(fixture.decisionId))
    }

    @Test
    fun `a reasoning superseded after proposal send does not affect acceptance and the frozen link is kept`() {
        val originalReasoningId = insertGenericReasoning(ready = true)
        val fixture = createWaitingDecisionApplication()
        sendProposal(fixture.applicationId)

        insertGenericReasoning(ready = false, createdAt = now.plusHours(1))

        db.transaction { tx ->
            applicationStateService.respondToPlacementProposal(
                tx,
                admin.user,
                clock,
                fixture.applicationId,
                PlacementPlanConfirmationStatus.ACCEPTED,
            )
            applicationStateService.confirmPlacementProposalChanges(
                tx,
                admin.user,
                clock,
                fixture.unitId,
                rejectReasonTranslations = emptyMap(),
            )
        }

        val sentDecisions = db.read { tx ->
            tx.getSentDecisionsByApplication(fixture.applicationId, AccessControlFilter.PermitAll)
        }
        assertEquals(1, sentDecisions.size)
        assertEquals(originalReasoningId, linkedReasoningId(fixture.decisionId))
    }

    @Test
    fun `withdrawPlacementProposal clears the frozen links`() {
        insertGenericReasoning(ready = true)
        val fixture = createWaitingDecisionApplication()
        sendProposal(fixture.applicationId)

        db.transaction { tx ->
            applicationStateService.withdrawPlacementProposal(
                tx,
                admin.user,
                clock,
                AuditContext(),
                fixture.applicationId,
            )
        }

        assertEquals(ApplicationStatus.WAITING_DECISION, applicationStatus(fixture.applicationId))
        assertNull(linkedReasoningId(fixture.decisionId))
    }

    @Test
    fun `a batch send is rejected atomically when one application has a non-finalized reasoning`() {
        insertGenericReasoning(ready = true, textSv = "")
        val goodFixture = createWaitingDecisionApplication(unitLanguage = Language.fi)
        val badFixture = createWaitingDecisionApplication(unitLanguage = Language.sv)

        val exception =
            assertThrows<Conflict> {
                db.transaction { tx ->
                    applicationStateService.doSimpleAction(
                        tx,
                        admin.user,
                        clock,
                        AuditContext(),
                        SimpleApplicationAction.SEND_PLACEMENT_PROPOSAL,
                        setOf(goodFixture.applicationId, badFixture.applicationId),
                    )
                }
            }
        assertEquals(DECISION_REASONING_NOT_FINALIZED, exception.errorCode)

        assertEquals(
            ApplicationStatus.WAITING_DECISION,
            applicationStatus(goodFixture.applicationId),
        )
        assertEquals(
            ApplicationStatus.WAITING_DECISION,
            applicationStatus(badFixture.applicationId),
        )
        assertNull(linkedReasoningId(goodFixture.decisionId))
        assertNull(linkedReasoningId(badFixture.decisionId))
    }

    @Test
    fun `sendPlacementProposal rolls back generic reasoning links across both collections when only one is finalized`() {
        // getPlannedUnsentDecisions returns decisions in creation order, i.e. the primary
        // PRESCHOOL decision before the connected PRESCHOOL_DAYCARE decision, so freezing this
        // fixture writes the PRESCHOOL link first and only then hits the missing DAYCARE
        // reasoning - exercising rollback of an already-written link, not just an early failure.
        insertGenericReasoning(
            ready = true,
            collectionType = DecisionReasoningCollectionType.PRESCHOOL,
        )
        val fixture = createWaitingDecisionApplication(type = DecisionType.PRESCHOOL_DAYCARE)
        val decisions = db.read { tx ->
            tx.getDecisionsByApplication(fixture.applicationId, AccessControlFilter.PermitAll)
        }
        val preschoolDecisionId = decisions.first { it.type == DecisionType.PRESCHOOL }.id
        val preschoolDaycareDecisionId =
            decisions.first { it.type == DecisionType.PRESCHOOL_DAYCARE }.id

        val exception = assertThrows<Conflict> { sendProposal(fixture.applicationId) }

        assertEquals(DECISION_REASONING_NOT_FINALIZED, exception.errorCode)
        assertEquals(ApplicationStatus.WAITING_DECISION, applicationStatus(fixture.applicationId))
        assertNull(linkedReasoningId(preschoolDecisionId))
        assertNull(linkedReasoningId(preschoolDaycareDecisionId))
    }

    @Test
    fun `sendPlacementProposal does not validate or link when the feature flag is disabled`() {
        whenever(evakaEnv.decisionReasoningEnabled).thenReturn(false)
        val fixture = createWaitingDecisionApplication()

        sendProposal(fixture.applicationId)

        assertEquals(
            ApplicationStatus.WAITING_UNIT_CONFIRMATION,
            applicationStatus(fixture.applicationId),
        )
        assertNull(linkedReasoningId(fixture.decisionId))
    }
}
