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
import evaka.core.application.SearchApplicationRequest
import evaka.core.application.persistence.club.Adult as ClubAdult
import evaka.core.application.persistence.club.Apply as ClubApply
import evaka.core.application.persistence.club.Child as ClubChild
import evaka.core.application.persistence.club.ClubFormV0
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.clubTerm2025
import evaka.core.daycare.CareType
import evaka.core.decision.DecisionDraftUpdate
import evaka.core.decision.DecisionType
import evaka.core.decision.getDecisionsByApplication
import evaka.core.decision.reasoning.DecisionReasoningCollectionType.CLUB as CLUB_COLLECTION
import evaka.core.shared.ApplicationId
import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
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
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired

class DecisionReasoningExemptTypesIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 5, 11), LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)
    private val today: LocalDate = now.toLocalDate()
    private val startDate: LocalDate = LocalDate.of(2026, 8, 1)
    private val clubStartDate: LocalDate = LocalDate.of(2026, 5, 18)

    @Autowired private lateinit var applicationStateService: ApplicationStateService
    @Autowired private lateinit var applicationControllerV2: ApplicationControllerV2
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(admin) }
        whenever(featureConfig.decisionsWithoutReasonings).thenReturn(setOf(DecisionType.CLUB))
    }

    private fun insertClubGenericReasoning(ready: Boolean = true) = db.transaction { tx ->
        tx.insert(
            DevDecisionReasoningGeneric(
                collectionType = CLUB_COLLECTION,
                validFrom = LocalDate.of(2026, 1, 1),
                textFi = "fi-generic-text",
                textSv = "sv-generic-text",
                ready = ready,
                createdAt = now,
                modifiedAt = now,
            )
        )
    }

    private fun insertClubIndividualReasoning() = db.transaction { tx ->
        tx.insert(
            DevDecisionReasoningIndividual(
                collectionType = CLUB_COLLECTION,
                language = OfficialLanguage.FI,
                title = "fi-title",
                text = "fi-text",
                createdAt = now,
                modifiedAt = now,
            )
        )
    }

    /**
     * Creates a club application with a planned CLUB decision draft via the placement plan flow.
     */
    private fun createPlannedClubDecision(): Pair<DecisionId, ApplicationId> =
        db.transaction { tx ->
            tx.insert(clubTerm2025)
            val guardianId = tx.insert(DevPerson(), DevPersonType.ADULT)
            val childId =
                tx.insert(DevPerson(dateOfBirth = today.minusYears(3)), DevPersonType.CHILD)
            val areaId = tx.insert(DevCareArea())
            val unitId =
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        type = setOf(CareType.CENTRE, CareType.CLUB),
                        clubApplyPeriod = DateRange(LocalDate.of(2020, 1, 1), null),
                    )
                )

            val applicationId =
                tx.insertTestApplication(
                    status = ApplicationStatus.WAITING_PLACEMENT,
                    guardianId = guardianId,
                    childId = childId,
                    type = ApplicationType.CLUB,
                    document =
                        ClubFormV0(
                            child = ClubChild(dateOfBirth = today.minusYears(3)),
                            guardian = ClubAdult(),
                            apply = ClubApply(preferredUnits = listOf(unitId)),
                            preferredStartDate = clubStartDate,
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
                audit = AuditContext(),
                applicationId = applicationId,
                placementPlan =
                    DaycarePlacementPlan(
                        unitId = unitId,
                        period = FiniteDateRange(clubStartDate, LocalDate.of(2026, 5, 29)),
                    ),
            )
            val decisionId =
                tx.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll)
                    .first { it.type == DecisionType.CLUB }
                    .id
            decisionId to applicationId
        }

    private fun createPlannedDaycareDecision(): ApplicationId = db.transaction { tx ->
        val guardianId = tx.insert(DevPerson(), DevPersonType.ADULT)
        val childId = tx.insert(DevPerson(dateOfBirth = today.minusYears(3)), DevPersonType.CHILD)
        val areaId = tx.insert(DevCareArea())
        val unitId = tx.insert(DevDaycare(areaId = areaId))

        val applicationId =
            tx.insertTestApplication(
                status = ApplicationStatus.WAITING_PLACEMENT,
                guardianId = guardianId,
                childId = childId,
                type = ApplicationType.DAYCARE,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
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
            audit = AuditContext(),
            applicationId = applicationId,
            placementPlan =
                DaycarePlacementPlan(
                    unitId = unitId,
                    period = FiniteDateRange(startDate, startDate.plusYears(1)),
                ),
        )
        applicationId
    }

    /**
     * Inserts an application in WAITING_DECISION status with a bare planned decision draft row,
     * bypassing the placement plan flow.
     */
    private fun insertPlannedDraftDecision(
        type: DecisionType
    ): Triple<DecisionId, ApplicationId, DaycareId> = db.transaction { tx ->
        val guardianId = tx.insert(DevPerson(), DevPersonType.ADULT)
        val childId = tx.insert(DevPerson(dateOfBirth = today.minusYears(3)), DevPersonType.CHILD)
        val shortName = "test_area_${type.name.lowercase()}"
        val areaId = tx.insert(DevCareArea(name = "Test Area $type", shortName = shortName))
        val unitId = tx.insert(DevDaycare(areaId = areaId))

        val appType =
            if (type == DecisionType.CLUB) ApplicationType.CLUB else ApplicationType.PRESCHOOL
        val applicationId =
            tx.insertTestApplication(
                status = ApplicationStatus.WAITING_DECISION,
                guardianId = guardianId,
                childId = childId,
                type = appType,
                confidential = false,
                document =
                    if (appType == ApplicationType.CLUB)
                        ClubFormV0(
                            child = ClubChild(dateOfBirth = today.minusYears(3)),
                            guardian = ClubAdult(),
                            apply = ClubApply(preferredUnits = listOf(unitId)),
                            preferredStartDate = startDate,
                        )
                    else
                        DaycareFormV0(
                            type = appType,
                            child = Child(dateOfBirth = today.minusYears(3)),
                            guardian = Adult(),
                            apply = Apply(preferredUnits = listOf(unitId)),
                            preferredStartDate = startDate,
                        ),
            )
        val decisionId =
            tx.createUpdate {
                    sql(
                        """
INSERT INTO decision (created_by, unit_id, application_id, type, start_date, end_date, planned)
VALUES (${bind(admin.evakaUserId)}, ${bind(unitId)}, ${bind(applicationId)}, ${bind(type)}, ${bind(startDate)}, ${bind(startDate.plusYears(1))}, true)
RETURNING id
"""
                    )
                }
                .executeAndReturnGeneratedKeys()
                .exactlyOne<DecisionId>()
        Triple(decisionId, applicationId, unitId)
    }

    private fun sendDecisions(applicationId: ApplicationId) {
        db.transaction { tx ->
            applicationStateService.sendDecisionsWithoutProposal(
                tx = tx,
                user = admin.user,
                clock = clock,
                audit = AuditContext(),
                applicationId = applicationId,
            )
        }
    }

    @Test
    fun `sending an exempt club decision succeeds without any generic reasoning and the pdf is generated`() {
        val (decisionId, applicationId) = createPlannedClubDecision()

        assertDoesNotThrow { sendDecisions(applicationId) }
        asyncJobRunner.runPendingJobsSync(clock)

        data class DecisionRow(
            val genericReasoningId: DecisionGenericReasoningId?,
            val documentKey: String?,
        )
        val row = db.read { tx ->
            tx.createQuery {
                    sql(
                        "SELECT generic_reasoning_id, document_key FROM decision WHERE id = ${bind(decisionId)}"
                    )
                }
                .exactlyOne<DecisionRow>()
        }
        assertNull(row.genericReasoningId)
        assertNotNull(row.documentKey)
    }

    @Test
    fun `sending a non-exempt decision still requires a generic reasoning`() {
        val applicationId = createPlannedDaycareDecision()

        val exception = assertThrows<Conflict> { sendDecisions(applicationId) }
        assertEquals(DECISION_REASONING_NOT_FINALIZED, exception.errorCode)
    }

    @Test
    fun `stats query excludes exempt decision types but keeps other types of the same collection`() {
        val (_, clubApplicationId, _) = insertPlannedDraftDecision(DecisionType.CLUB)
        val (_, preschoolClubApplicationId, _) =
            insertPlannedDraftDecision(DecisionType.PRESCHOOL_CLUB)

        val stats = db.read {
            it.getApplicationDecisionReasoningStats(
                setOf(clubApplicationId, preschoolClubApplicationId),
                setOf(DecisionType.CLUB),
            )
        }

        assertNull(stats[clubApplicationId])
        assertEquals(
            DecisionReasoningStats(individualReasoningCount = 0, reasoningWarningCount = 1),
            stats[preschoolClubApplicationId],
        )
    }

    @Test
    fun `search endpoint reports no reasoning warnings for an exempt club application`() {
        val (_, applicationId) = createPlannedClubDecision()

        val summary =
            applicationControllerV2
                .getApplicationSummaries(
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
                .data
                .single { it.id == applicationId }

        assertEquals(0, summary.individualReasoningCount)
        assertEquals(0, summary.reasoningWarningCount)
    }

    @Test
    fun `getDecisionDrafts returns no generic reasoning for an exempt decision type`() {
        insertClubGenericReasoning(ready = true)
        val (decisionId, applicationId) = createPlannedClubDecision()

        val draftGroup =
            applicationControllerV2.getDecisionDrafts(
                dbInstance(),
                admin.user,
                clock,
                applicationId,
            )

        val draft = draftGroup.decisions.single { it.id == decisionId }
        assertNull(draft.genericReasoning)
    }

    @Test
    fun `updateDecisionDrafts rejects individual reasoning selections for an exempt decision type`() {
        val (decisionId, applicationId, unitId) = insertPlannedDraftDecision(DecisionType.CLUB)
        val individualReasoningId = insertClubIndividualReasoning()

        assertThrows<BadRequest> {
            applicationControllerV2.updateDecisionDrafts(
                dbInstance(),
                admin.user,
                clock,
                applicationId,
                listOf(
                    DecisionDraftUpdate(
                        id = decisionId,
                        unitId = unitId,
                        startDate = startDate,
                        endDate = startDate.plusYears(1),
                        planned = true,
                        individualReasoningIds = setOf(individualReasoningId),
                    )
                ),
            )
        }

        assertDoesNotThrow {
            applicationControllerV2.updateDecisionDrafts(
                dbInstance(),
                admin.user,
                clock,
                applicationId,
                listOf(
                    DecisionDraftUpdate(
                        id = decisionId,
                        unitId = unitId,
                        startDate = startDate,
                        endDate = startDate.plusYears(1),
                        planned = true,
                        individualReasoningIds = emptySet(),
                    )
                ),
            )
        }
    }
}
