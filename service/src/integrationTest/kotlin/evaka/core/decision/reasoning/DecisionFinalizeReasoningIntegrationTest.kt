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
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.daycare.domain.Language
import evaka.core.decision.DecisionService
import evaka.core.decision.DecisionType
import evaka.core.decision.DecisionType.DAYCARE
import evaka.core.decision.getDecisionsByApplication
import evaka.core.decision.reasoning.DecisionReasoningCollectionType.DAYCARE as DAYCARE_COLLECTION
import evaka.core.shared.ApplicationId
import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDecisionReasoningGeneric
import evaka.core.shared.dev.DevDecisionReasoningIndividual
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
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DecisionFinalizeReasoningIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 5, 11), LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)
    private val today: LocalDate = now.toLocalDate()
    private val startDate: LocalDate = LocalDate.of(2026, 8, 1)

    @Autowired private lateinit var applicationStateService: ApplicationStateService

    @Autowired private lateinit var decisionService: DecisionService

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(admin) }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun insertGenericReasoning(
        ready: Boolean = true,
        textSv: String = "sv-generic-text",
        textFi: String = "fi-generic-text",
        createdAt: HelsinkiDateTime = now,
    ) = db.transaction { tx ->
        tx.insert(
            DevDecisionReasoningGeneric(
                collectionType = DAYCARE_COLLECTION,
                validFrom = LocalDate.of(2026, 1, 1),
                textFi = textFi,
                textSv = textSv,
                ready = ready,
                createdAt = createdAt,
                modifiedAt = createdAt,
            )
        )
    }

    private fun insertIndividualReasoning(
        titleSv: String = "sv-title",
        textSv: String = "sv-text",
    ) = db.transaction { tx ->
        tx.insert(
            DevDecisionReasoningIndividual(
                collectionType = DAYCARE_COLLECTION,
                titleFi = "fi-title",
                titleSv = titleSv,
                textFi = "fi-text",
                textSv = textSv,
                createdAt = now,
                modifiedAt = now,
            )
        )
    }

    /**
     * Creates an application with a planned decision draft pointing at a unit with the given
     * language. Returns (decisionId, applicationId).
     */
    private fun createPlannedDecision(
        unitLanguage: Language,
        type: DecisionType = DAYCARE,
    ): Pair<DecisionId, ApplicationId> = db.transaction { tx ->
        val guardianId = tx.insert(DevPerson(), DevPersonType.ADULT)
        val childId = tx.insert(DevPerson(dateOfBirth = today.minusYears(3)), DevPersonType.CHILD)
        val areaId = tx.insert(DevCareArea())
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
        decisionId to applicationId
    }

    private fun finalizeViaService(applicationId: ApplicationId) {
        db.transaction { tx ->
            decisionService.finalizeDecisions(
                tx = tx,
                user = admin.user,
                clock = clock,
                applicationId = applicationId,
                sendAsMessage = false,
                skipGuardianApproval = false,
            )
        }
    }

    // ── tests ──────────────────────────────────────────────────────────────────

    @Test
    fun `finalizing a Swedish-unit decision with empty generic textSv throws Conflict`() {
        insertGenericReasoning(textSv = "")
        val (_, applicationId) = createPlannedDecision(Language.sv)

        val exception = assertThrows<Conflict> { finalizeViaService(applicationId) }
        assertEquals(DECISION_REASONING_NOT_FINALIZED, exception.errorCode)
    }

    @Test
    fun `finalizing a Swedish-unit decision with linked individual reasoning with empty textSv throws Conflict`() {
        insertGenericReasoning(textSv = "non-empty swedish generic text")
        val (decisionId, applicationId) = createPlannedDecision(Language.sv)
        val badIndividualId = insertIndividualReasoning(titleSv = "ok-title", textSv = "")

        db.transaction { tx ->
            tx.setDecisionReasoningIndividualSelections(
                decisionId = decisionId,
                reasoningIds = setOf(badIndividualId),
                createdAt = now,
                createdBy = admin.evakaUserId,
            )
        }

        val exception = assertThrows<Conflict> { finalizeViaService(applicationId) }
        assertEquals(DECISION_REASONING_NOT_FINALIZED, exception.errorCode)
    }

    @Test
    fun `finalizing a Swedish-unit decision with all Swedish text present succeeds`() {
        insertGenericReasoning(textSv = "non-empty swedish generic text")
        val (decisionId, applicationId) = createPlannedDecision(Language.sv)
        val goodIndividualId = insertIndividualReasoning(titleSv = "sv-title", textSv = "sv-text")

        db.transaction { tx ->
            tx.setDecisionReasoningIndividualSelections(
                decisionId = decisionId,
                reasoningIds = setOf(goodIndividualId),
                createdAt = now,
                createdBy = admin.evakaUserId,
            )
        }

        assertDoesNotThrow { finalizeViaService(applicationId) }
    }

    @Test
    fun `finalizing a Finnish-unit decision with empty generic textSv succeeds`() {
        insertGenericReasoning(textSv = "")
        val (_, applicationId) = createPlannedDecision(Language.fi)

        assertDoesNotThrow { finalizeViaService(applicationId) }
    }

    @Test
    fun `finalizing with no applicable generic reasoning throws Conflict with error code`() {
        val (_, applicationId) = createPlannedDecision(Language.fi)

        val exception = assertThrows<Conflict> { finalizeViaService(applicationId) }
        assertEquals(DECISION_REASONING_NOT_FINALIZED, exception.errorCode)
    }

    @Test
    fun `finalizing with a not-ready generic reasoning throws Conflict with error code`() {
        insertGenericReasoning(ready = false)
        val (_, applicationId) = createPlannedDecision(Language.fi)

        val exception = assertThrows<Conflict> { finalizeViaService(applicationId) }
        assertEquals(DECISION_REASONING_NOT_FINALIZED, exception.errorCode)
    }

    @Test
    fun `finalizing a decision with an already linked generic reasoning skips validation and keeps the link`() {
        val linkedReasoningId = insertGenericReasoning(ready = true)
        val (decisionId, applicationId) = createPlannedDecision(Language.fi)
        db.transaction { tx -> tx.updateGenericReasoningToDecision(decisionId, linkedReasoningId) }
        // a superseding not-ready reasoning appears afterwards (same validFrom, later createdAt)
        insertGenericReasoning(ready = false, createdAt = now.plusHours(1))

        assertDoesNotThrow { finalizeViaService(applicationId) }

        val storedReasoningId = db.read { tx ->
            tx.createQuery {
                    sql(
                        "SELECT generic_reasoning_id FROM decision WHERE id = ${bind(decisionId)} AND generic_reasoning_id IS NOT NULL"
                    )
                }
                .toList<DecisionGenericReasoningId>()
                .single()
        }
        assertEquals(linkedReasoningId, storedReasoningId)
    }
}
