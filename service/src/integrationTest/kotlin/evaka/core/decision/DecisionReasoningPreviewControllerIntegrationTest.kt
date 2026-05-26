// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision

import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationStateService
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.DaycarePlacementPlan
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.decision.reasoning.DecisionGenericReasoningRequest
import evaka.core.decision.reasoning.DecisionIndividualReasoningRequest
import evaka.core.decision.reasoning.DecisionReasoningCollectionType
import evaka.core.decision.reasoning.DecisionReasoningCollectionType.DAYCARE as DAYCARE_COLLECTION
import evaka.core.decision.reasoning.insertDecisionIndividualReasoningLink
import evaka.core.decision.reasoning.insertGenericReasoning
import evaka.core.decision.reasoning.insertIndividualReasoning
import evaka.core.decision.reasoning.removeGenericReasoning
import evaka.core.shared.ApplicationId
import evaka.core.shared.DecisionId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DecisionReasoningPreviewControllerIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var decisionController: DecisionController
    @Autowired private lateinit var applicationStateService: ApplicationStateService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 5, 11), LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val serviceWorker = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(serviceWorker)
        }
    }

    @Test
    fun `PENDING decision returns generic reasoning from the live resolver`() {
        val readyId = createReadyGeneric(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1))
        val (decisionId, _) = createPlannedDecision(DecisionType.DAYCARE)

        val response = getPreview(decisionId)

        assertEquals(readyId, response.genericReasoning.reasoning?.id)
        assertEquals(DAYCARE_COLLECTION, response.genericReasoning.collectionType)
    }

    @Test
    fun `sent decision is rejected as not a draft`() {
        createReadyGeneric(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1))
        val (decisionId, applicationId) = createPlannedDecision(DecisionType.DAYCARE)
        sendDecision(applicationId)

        val ex = kotlin.runCatching { getPreview(decisionId) }
        assertIs<BadRequest>(ex.exceptionOrNull())
    }

    @Test
    fun `validUntil is null for the most recent generic reasoning`() {
        createReadyGeneric(DAYCARE_COLLECTION, LocalDate.of(2025, 9, 9))
        val (decisionId, _) = createPlannedDecision(DecisionType.DAYCARE)

        val response = getPreview(decisionId)

        assertEquals(null, response.genericReasoning.validUntil)
    }

    @Test
    fun `validUntil is the next reasoning's valid_from minus one day`() {
        createReadyGeneric(DAYCARE_COLLECTION, LocalDate.of(2025, 9, 9))
        createReadyGeneric(DAYCARE_COLLECTION, LocalDate.of(2026, 9, 9))
        val (decisionId, _) = createPlannedDecision(DecisionType.DAYCARE)

        val response = getPreview(decisionId)

        assertEquals(LocalDate.of(2026, 9, 8), response.genericReasoning.validUntil)
    }

    @Test
    fun `validUntil ignores removed successor reasonings`() {
        createReadyGeneric(DAYCARE_COLLECTION, LocalDate.of(2025, 9, 9))
        val successorId = createReadyGeneric(DAYCARE_COLLECTION, LocalDate.of(2026, 9, 9))
        db.transaction { tx -> tx.removeGenericReasoning(successorId, now) }
        val (decisionId, _) = createPlannedDecision(DecisionType.DAYCARE)

        val response = getPreview(decisionId)

        assertEquals(null, response.genericReasoning.validUntil)
    }

    @Test
    fun `individual reasonings are returned from the join table regardless of status`() {
        val (decisionId, _) = createPlannedDecision(DecisionType.DAYCARE)
        val indId = createIndividual(DAYCARE_COLLECTION)
        db.transaction { tx ->
            tx.insertDecisionIndividualReasoningLink(decisionId, indId, now, admin.evakaUserId)
        }

        val response = getPreview(decisionId)
        assertEquals(listOf(indId), response.individualReasonings.map { it.id })
    }

    @Test
    fun `caller without permission is rejected`() {
        val (decisionId, _) = createPlannedDecision(DecisionType.DAYCARE)
        val unauthorized =
            DevEmployee(roles = setOf(UserRole.MESSAGING)).also {
                db.transaction { tx -> tx.insert(it) }
            }

        val ex = kotlin.runCatching { getPreview(decisionId, user = unauthorized.user) }
        assertEquals(true, ex.isFailure)
        assertEquals("Forbidden", ex.exceptionOrNull()?.javaClass?.simpleName)
    }

    private fun createReadyGeneric(
        collectionType: DecisionReasoningCollectionType,
        validFrom: LocalDate,
    ) = db.transaction { tx ->
        tx.insertGenericReasoning(
            DecisionGenericReasoningRequest(
                collectionType = collectionType,
                validFrom = validFrom,
                textFi = "fi-$validFrom",
                textSv = "sv-$validFrom",
                ready = true,
            ),
            now,
        )
    }

    private fun createIndividual(collectionType: DecisionReasoningCollectionType) =
        db.transaction { tx ->
            tx.insertIndividualReasoning(
                DecisionIndividualReasoningRequest(
                    collectionType = collectionType,
                    titleFi = "title-fi",
                    titleSv = "title-sv",
                    textFi = "text-fi",
                    textSv = "text-sv",
                ),
                now,
            )
        }

    private fun createPlannedDecision(
        type: DecisionType,
        startDate: LocalDate = LocalDate.of(2026, 8, 1),
    ): Pair<DecisionId, ApplicationId> = db.transaction { tx ->
        val guardianId = tx.insert(DevPerson(), DevPersonType.ADULT)
        val childId =
            tx.insert(DevPerson(dateOfBirth = clock.today().minusYears(3)), DevPersonType.CHILD)
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
                        serviceStart = "08:00",
                        serviceEnd = "16:00",
                        child = Child(dateOfBirth = clock.today().minusYears(3)),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(unitId)),
                        preferredStartDate = startDate,
                    ),
            )
        val sw = serviceWorker.user
        applicationStateService.setVerified(tx, sw, clock, applicationId, confidential = false)
        applicationStateService.createPlacementPlan(
            tx,
            sw,
            clock,
            applicationId,
            DaycarePlacementPlan(
                unitId = unitId,
                period = FiniteDateRange(startDate, startDate.plusYears(1)),
            ),
        )
        val decisionId =
            tx.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll)
                .first { it.type == type }
                .id
        decisionId to applicationId
    }

    private fun sendDecision(applicationId: ApplicationId) {
        db.transaction { tx ->
            applicationStateService.sendDecisionsWithoutProposal(
                tx,
                serviceWorker.user,
                clock,
                applicationId,
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)
        db.transaction { tx ->
            applicationStateService.confirmDecisionMailed(
                tx,
                serviceWorker.user,
                clock,
                applicationId,
            )
        }
    }

    private fun getPreview(
        decisionId: DecisionId,
        user: AuthenticatedUser.Employee = serviceWorker.user,
    ) = decisionController.getDraftReasoningPreview(dbInstance(), user, clock, decisionId)
}
