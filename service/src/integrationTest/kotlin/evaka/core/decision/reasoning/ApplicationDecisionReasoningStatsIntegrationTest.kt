// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.AuditContext
import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationControllerV2
import evaka.core.application.ApplicationStateService
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationStatusOption
import evaka.core.application.ApplicationType
import evaka.core.application.ApplicationTypeToggle
import evaka.core.application.DaycarePlacementPlan
import evaka.core.application.PagedApplicationSummaries
import evaka.core.application.SearchApplicationRequest
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.daycare.domain.Language
import evaka.core.decision.DecisionType
import evaka.core.decision.DecisionType.DAYCARE
import evaka.core.decision.getDecisionsByApplication
import evaka.core.decision.reasoning.DecisionReasoningCollectionType.DAYCARE as DAYCARE_COLLECTION
import evaka.core.shared.ApplicationId
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
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ApplicationDecisionReasoningStatsIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 5, 11), LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)
    private val today: LocalDate = now.toLocalDate()
    private val startDate: LocalDate = LocalDate.of(2026, 8, 1)

    @Autowired private lateinit var applicationStateService: ApplicationStateService
    @Autowired private lateinit var applicationControllerV2: ApplicationControllerV2

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(admin) }
    }

    private fun insertGenericReasoning(
        ready: Boolean,
        collectionType: DecisionReasoningCollectionType = DAYCARE_COLLECTION,
    ) = db.transaction { tx ->
        tx.insert(
            DevDecisionReasoningGeneric(
                collectionType = collectionType,
                validFrom = LocalDate.of(2026, 1, 1),
                textFi = "fi-generic-text",
                textSv = "sv-generic-text",
                ready = ready,
                createdAt = now,
                modifiedAt = now,
            )
        )
    }

    private fun insertIndividualReasoning() = db.transaction { tx ->
        tx.insert(
            DevDecisionReasoningIndividual(
                collectionType = DAYCARE_COLLECTION,
                titleFi = "fi-title",
                titleSv = "sv-title",
                textFi = "fi-text",
                textSv = "sv-text",
                createdAt = now,
                modifiedAt = now,
            )
        )
    }

    private fun createPlannedDecision(
        type: DecisionType = DAYCARE
    ): Pair<DecisionId, ApplicationId> = db.transaction { tx ->
        val guardianId = tx.insert(DevPerson(), DevPersonType.ADULT)
        val childId = tx.insert(DevPerson(dateOfBirth = today.minusYears(3)), DevPersonType.CHILD)
        val areaId = tx.insert(DevCareArea())
        val unitId = tx.insert(DevDaycare(areaId = areaId, language = Language.fi))

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

    @Test
    fun `counts individual reasonings and reports no warning when generic is ready`() {
        insertGenericReasoning(ready = true)
        val (decisionId, applicationId) = createPlannedDecision()
        val r1 = insertIndividualReasoning()
        val r2 = insertIndividualReasoning()
        db.transaction { tx ->
            tx.setDecisionReasoningIndividualSelections(
                decisionId = decisionId,
                reasoningIds = setOf(r1, r2),
                createdAt = now,
                createdBy = admin.evakaUserId,
            )
        }

        val stats = db.read { it.getApplicationDecisionReasoningStats(setOf(applicationId)) }

        assertEquals(
            DecisionReasoningStats(individualReasoningCount = 2, reasoningWarningCount = 0),
            stats[applicationId],
        )
    }

    @Test
    fun `reports a warning when applicable generic reasoning is missing`() {
        val (_, applicationId) = createPlannedDecision()

        val stats = db.read { it.getApplicationDecisionReasoningStats(setOf(applicationId)) }

        assertEquals(
            DecisionReasoningStats(individualReasoningCount = 0, reasoningWarningCount = 1),
            stats[applicationId],
        )
    }

    @Test
    fun `reports a warning when applicable generic reasoning is not ready`() {
        insertGenericReasoning(ready = false)
        val (_, applicationId) = createPlannedDecision()

        val stats = db.read { it.getApplicationDecisionReasoningStats(setOf(applicationId)) }

        assertEquals(
            DecisionReasoningStats(individualReasoningCount = 0, reasoningWarningCount = 1),
            stats[applicationId],
        )
    }

    @Test
    fun `reports a warning when a newer not-ready generic supersedes an older ready one`() {
        db.transaction { tx ->
            tx.insert(
                DevDecisionReasoningGeneric(
                    collectionType = DAYCARE_COLLECTION,
                    validFrom = LocalDate.of(2026, 1, 1),
                    textFi = "fi-generic-text",
                    textSv = "sv-generic-text",
                    ready = true,
                    createdAt = HelsinkiDateTime.of(LocalDate.of(2026, 5, 10), LocalTime.of(12, 0)),
                    modifiedAt = HelsinkiDateTime.of(LocalDate.of(2026, 5, 10), LocalTime.of(12, 0)),
                )
            )
        }
        db.transaction { tx ->
            tx.insert(
                DevDecisionReasoningGeneric(
                    collectionType = DAYCARE_COLLECTION,
                    validFrom = LocalDate.of(2026, 1, 1),
                    textFi = "fi-generic-text",
                    textSv = "sv-generic-text",
                    ready = false,
                    createdAt = now,
                    modifiedAt = now,
                )
            )
        }
        val (_, applicationId) = createPlannedDecision()

        val stats = db.read { it.getApplicationDecisionReasoningStats(setOf(applicationId)) }

        assertEquals(1, stats[applicationId]?.reasoningWarningCount)
    }

    @Test
    fun `the most recent generic reasoning valid at the decision start date governs, not the oldest`() {
        db.transaction { tx ->
            tx.insert(
                DevDecisionReasoningGeneric(
                    collectionType = DAYCARE_COLLECTION,
                    validFrom = LocalDate.of(2025, 1, 1),
                    textFi = "fi-generic-text",
                    textSv = "sv-generic-text",
                    ready = true,
                    createdAt = HelsinkiDateTime.of(LocalDate.of(2025, 1, 1), LocalTime.of(12, 0)),
                    modifiedAt = HelsinkiDateTime.of(LocalDate.of(2025, 1, 1), LocalTime.of(12, 0)),
                )
            )
        }
        db.transaction { tx ->
            tx.insert(
                DevDecisionReasoningGeneric(
                    collectionType = DAYCARE_COLLECTION,
                    validFrom = LocalDate.of(2026, 6, 1),
                    textFi = "fi-generic-text",
                    textSv = "sv-generic-text",
                    ready = false,
                    createdAt = HelsinkiDateTime.of(LocalDate.of(2026, 6, 1), LocalTime.of(12, 0)),
                    modifiedAt = HelsinkiDateTime.of(LocalDate.of(2026, 6, 1), LocalTime.of(12, 0)),
                )
            )
        }
        val (_, applicationId) = createPlannedDecision()

        val stats = db.read { it.getApplicationDecisionReasoningStats(setOf(applicationId)) }

        assertEquals(1, stats[applicationId]?.reasoningWarningCount)
    }

    @Test
    fun `counts one warning per draft decision for a preschool application with two decisions`() {
        val (_, applicationId) = createPlannedDecision(type = DecisionType.PRESCHOOL_DAYCARE)

        val stats = db.read { it.getApplicationDecisionReasoningStats(setOf(applicationId)) }

        assertEquals(2, stats[applicationId]?.reasoningWarningCount)
    }

    private fun searchWaitingDecision(): PagedApplicationSummaries =
        applicationControllerV2.getApplicationSummaries(
            dbInstance(),
            admin.user,
            clock,
            SearchApplicationRequest(
                page = null,
                sortBy = null,
                sortDir = null,
                areas = null,
                units = null,
                basis = null,
                type = ApplicationTypeToggle.ALL,
                preschoolType = null,
                statuses = listOf(ApplicationStatusOption.WAITING_DECISION),
                dateType = null,
                distinctions = null,
                periodStart = null,
                periodEnd = null,
                searchTerms = null,
                transferApplications = null,
                voucherApplications = null,
            ),
        )

    @Test
    fun `search endpoint populates reasoning counts when the feature is enabled`() {
        insertGenericReasoning(ready = false)
        val (decisionId, applicationId) = createPlannedDecision()
        val r1 = insertIndividualReasoning()
        db.transaction { tx ->
            tx.setDecisionReasoningIndividualSelections(
                decisionId = decisionId,
                reasoningIds = setOf(r1),
                createdAt = now,
                createdBy = admin.evakaUserId,
            )
        }

        val summary = searchWaitingDecision().data.single { it.id == applicationId }

        assertEquals(1, summary.individualReasoningCount)
        assertEquals(1, summary.reasoningWarningCount)
    }

    @Test
    fun `unplanned drafts are excluded from the stats`() {
        val (decisionId, applicationId) = createPlannedDecision()
        db.transaction { tx ->
            tx.execute { sql("UPDATE decision SET planned = false WHERE id = ${bind(decisionId)}") }
        }

        val stats = db.read { it.getApplicationDecisionReasoningStats(setOf(applicationId)) }

        assertNull(stats[applicationId])
    }
}
