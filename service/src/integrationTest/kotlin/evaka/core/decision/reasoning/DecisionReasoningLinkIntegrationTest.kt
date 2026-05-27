// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationStateService
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.DaycarePlacementPlan
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.decision.DecisionType.CLUB
import evaka.core.decision.DecisionType.DAYCARE
import evaka.core.decision.DecisionType.DAYCARE_PART_TIME
import evaka.core.decision.DecisionType.PREPARATORY_EDUCATION
import evaka.core.decision.DecisionType.PRESCHOOL
import evaka.core.decision.DecisionType.PRESCHOOL_CLUB
import evaka.core.decision.DecisionType.PRESCHOOL_DAYCARE
import evaka.core.decision.getDecisionsByApplication
import evaka.core.decision.reasoning.DecisionReasoningCollectionType.DAYCARE as DAYCARE_COLLECTION
import evaka.core.decision.reasoning.DecisionReasoningCollectionType.PRESCHOOL as PRESCHOOL_COLLECTION
import evaka.core.shared.ApplicationId
import evaka.core.shared.DecisionGenericReasoningId
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
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.actionrule.AccessControlFilter
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DecisionReasoningLinkIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 5, 11), LocalTime.of(12, 0))
    private val today: LocalDate = now.toLocalDate()

    @Autowired private lateinit var applicationStateService: ApplicationStateService

    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(admin) }
    }

    @Test
    fun `applicableReasoningCollectionType returns the right slot per decision type`() {
        assertEquals(DAYCARE_COLLECTION, DAYCARE.applicableReasoningCollectionType())
        assertEquals(DAYCARE_COLLECTION, DAYCARE_PART_TIME.applicableReasoningCollectionType())
        assertEquals(PRESCHOOL_COLLECTION, PRESCHOOL.applicableReasoningCollectionType())
        assertEquals(DAYCARE_COLLECTION, PRESCHOOL_DAYCARE.applicableReasoningCollectionType())
        assertEquals(
            PRESCHOOL_COLLECTION,
            PREPARATORY_EDUCATION.applicableReasoningCollectionType(),
        )
        assertEquals(DAYCARE_COLLECTION, CLUB.applicableReasoningCollectionType())
        assertEquals(DAYCARE_COLLECTION, PRESCHOOL_CLUB.applicableReasoningCollectionType())
    }

    private fun insertGenericReasoning(
        collectionType: DecisionReasoningCollectionType,
        validFrom: LocalDate,
        ready: Boolean,
        textFi: String = "fi-$validFrom-$collectionType",
        textSv: String = "sv-$validFrom-$collectionType",
    ): DecisionGenericReasoningId = db.transaction { tx ->
        tx.insertGenericReasoning(
            DecisionGenericReasoningRequest(
                collectionType = collectionType,
                validFrom = validFrom,
                textFi = textFi,
                textSv = textSv,
                ready = ready,
            ),
            now,
        )
    }

    @Test
    fun `resolver returns the latest applicable row whose validFrom is on or before startDate`() {
        val older =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val newer =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 4, 1), ready = true)

        val resolved = db.read { tx ->
            tx.resolveApplicableGenericReasoning(DAYCARE, LocalDate.of(2026, 6, 1))
        }

        assertEquals(DAYCARE_COLLECTION, resolved.collectionType)
        assertEquals(newer, resolved.reasoning?.id)
        assertNotEquals(older, resolved.reasoning?.id)
    }

    @Test
    fun `resolver returns rows with ready = false`() {
        val notReady =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = false)

        val resolved = db.read { tx ->
            tx.resolveApplicableGenericReasoning(DAYCARE, LocalDate.of(2026, 6, 1))
        }

        assertEquals(notReady, resolved.reasoning?.id)
        assertEquals(false, resolved.reasoning?.ready)
    }

    @Test
    fun `resolver picks a not-ready newer version over a ready older one`() {
        insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val newerNotReady =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 4, 1), ready = false)

        val resolved = db.read { tx ->
            tx.resolveApplicableGenericReasoning(DAYCARE, LocalDate.of(2026, 5, 19))
        }

        assertEquals(newerNotReady, resolved.reasoning?.id)
        assertEquals(false, resolved.reasoning?.ready)
    }

    @Test
    fun `resolver returns the DAYCARE reasoning for PRESCHOOL_DAYCARE`() {
        val daycareId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        insertGenericReasoning(PRESCHOOL_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)

        val resolved = db.read { tx ->
            tx.resolveApplicableGenericReasoning(PRESCHOOL_DAYCARE, LocalDate.of(2026, 8, 1))
        }

        assertEquals(DAYCARE_COLLECTION, resolved.collectionType)
        assertEquals(daycareId, resolved.reasoning?.id)
    }

    @Test
    fun `resolver returns the DAYCARE reasoning for PRESCHOOL_CLUB`() {
        val daycareId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        insertGenericReasoning(PRESCHOOL_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)

        val resolved = db.read { tx ->
            tx.resolveApplicableGenericReasoning(PRESCHOOL_CLUB, LocalDate.of(2026, 8, 1))
        }

        assertEquals(DAYCARE_COLLECTION, resolved.collectionType)
        assertEquals(daycareId, resolved.reasoning?.id)
    }

    @Test
    fun `resolver skips removed rows`() {
        val removedId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        db.transaction { tx -> tx.removeGenericReasoning(removedId, now) }

        val resolved = db.read { tx ->
            tx.resolveApplicableGenericReasoning(DAYCARE, LocalDate.of(2026, 6, 1))
        }

        assertNull(resolved.reasoning)
    }

    @Test
    fun `insert and read generic reasoning links`() {
        val (decisionId, _) = insertDraftDecisionDirectly(DAYCARE)
        val r1 = insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val r2 = insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 3, 1), ready = true)

        db.transaction { tx ->
            tx.insertDecisionGenericReasoningLinks(
                decisionId = decisionId,
                reasoningIds = listOf(r1, r2),
                createdAt = now,
                createdBy = admin.evakaUserId,
            )
        }

        val linkedIds = db.read { tx -> tx.getDecisionGenericReasoningIds(decisionId) }
        assertEquals(setOf(r1, r2), linkedIds.toSet())
    }

    @Test
    fun `insert generic reasoning links is a no-op when list is empty`() {
        val (decisionId, _) = insertDraftDecisionDirectly(DAYCARE)

        db.transaction { tx ->
            tx.insertDecisionGenericReasoningLinks(
                decisionId = decisionId,
                reasoningIds = emptyList(),
                createdAt = now,
                createdBy = admin.evakaUserId,
            )
        }

        val linkedIds = db.read { tx -> tx.getDecisionGenericReasoningIds(decisionId) }
        assertEquals(emptyList(), linkedIds)
    }

    @Test
    fun `insert and remove individual reasoning link`() {
        val (decisionId, _) = insertDraftDecisionDirectly(DAYCARE)
        val individualId = db.transaction { tx ->
            tx.insertIndividualReasoning(
                DecisionIndividualReasoningRequest(
                    collectionType = DAYCARE_COLLECTION,
                    titleFi = "title-fi",
                    titleSv = "title-sv",
                    textFi = "text-fi",
                    textSv = "text-sv",
                ),
                now,
            )
        }

        db.transaction { tx ->
            tx.insertDecisionIndividualReasoningLink(
                decisionId = decisionId,
                reasoningId = individualId,
                createdAt = now,
                createdBy = admin.evakaUserId,
            )
        }

        assertEquals(
            listOf(individualId),
            db.read { tx -> tx.getDecisionIndividualReasoningIds(decisionId) },
        )

        val removed = db.transaction { tx ->
            tx.deleteDecisionIndividualReasoningLink(decisionId, individualId)
        }
        assertEquals(true, removed)
        assertEquals(
            emptyList(),
            db.read { tx -> tx.getDecisionIndividualReasoningIds(decisionId) },
        )

        val removedAgain = db.transaction { tx ->
            tx.deleteDecisionIndividualReasoningLink(decisionId, individualId)
        }
        assertEquals(false, removedAgain)
    }

    @Test
    fun `cascade deletes join rows when decision is hard-deleted`() {
        val (decisionId, _) = insertDraftDecisionDirectly(DAYCARE)
        val r = insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        db.transaction { tx ->
            tx.insertDecisionGenericReasoningLinks(decisionId, listOf(r), now, admin.evakaUserId)
        }

        db.transaction { tx ->
            tx.execute { sql("DELETE FROM decision WHERE id = ${bind(decisionId)}") }
        }

        val joinRowCount = db.read { tx ->
            tx.createQuery {
                    sql(
                        "SELECT count(*) FROM decision_generic_reasoning WHERE reasoning_id = ${bind(r)}"
                    )
                }
                .exactlyOne<Long>()
        }
        assertEquals(0L, joinRowCount)
    }

    @Test
    fun `sending a decision writes generic reasoning links inside the same transaction`() {
        val genericId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val (decisionId, applicationId) = createPlannedDecisionWithApplication(DAYCARE)

        sendDecisionViaService(applicationId)

        val linked = db.read { tx -> tx.getDecisionGenericReasoningIds(decisionId) }
        assertEquals(listOf(genericId), linked)
    }

    @Test
    fun `sending a decision with no eligible generic writes zero link rows`() {
        val (decisionId, applicationId) = createPlannedDecisionWithApplication(DAYCARE)

        sendDecisionViaService(applicationId)

        val linked = db.read { tx -> tx.getDecisionGenericReasoningIds(decisionId) }
        assertEquals(emptyList(), linked)
    }

    @Test
    fun `sending a PRESCHOOL_DAYCARE decision writes only the DAYCARE link`() {
        val daycareId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        insertGenericReasoning(PRESCHOOL_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val (decisionId, applicationId) = createPlannedDecisionWithApplication(PRESCHOOL_DAYCARE)

        sendDecisionViaService(applicationId)

        val linked = db.read { tx -> tx.getDecisionGenericReasoningIds(decisionId) }
        assertEquals(listOf(daycareId), linked)
    }

    @Test
    fun `sending a decision via the SFI path also writes generic reasoning links`() {
        val genericId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)

        val guardian =
            DevPerson(
                ssn = "010180-1232",
                firstName = "Testi",
                lastName = "Huoltaja",
                streetAddress = "Testikatu 1",
                postalCode = "02770",
                postOffice = "Espoo",
            )
        val child =
            DevPerson(
                ssn = "010617A123U",
                firstName = "Testi",
                lastName = "Lapsi",
                dateOfBirth = today.minusYears(3),
                streetAddress = "Testikatu 1",
                postalCode = "02770",
                postOffice = "Espoo",
            )

        MockPersonDetailsService.addPersons(guardian, child)
        MockPersonDetailsService.addDependants(guardian, child)

        val (decisionId, applicationId) =
            createPlannedDecisionWithApplication(DAYCARE, guardian = guardian, child = child)

        db.transaction { tx ->
            applicationStateService.sendDecisionsWithoutProposal(
                tx = tx,
                user = AuthenticatedUser.Employee(admin.id, setOf(UserRole.ADMIN)),
                clock = MockEvakaClock(now),
                applicationId = applicationId,
            )
        }
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))

        val linked = db.read { tx -> tx.getDecisionGenericReasoningIds(decisionId) }
        assertEquals(listOf(genericId), linked)
    }

    @Test
    fun `generic reasoning links are frozen at finalize, before the decision pdf job runs`() {
        val genericId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val (decisionId, applicationId) = createPlannedDecisionWithApplication(DAYCARE)

        db.transaction { tx ->
            applicationStateService.sendDecisionsWithoutProposal(
                tx = tx,
                user = AuthenticatedUser.Employee(admin.id, setOf(UserRole.ADMIN)),
                clock = MockEvakaClock(now),
                applicationId = applicationId,
            )
        }

        // No async jobs run: the decision pdf has not been generated yet.
        val linked = db.read { tx -> tx.getDecisionGenericReasoningIds(decisionId) }
        assertEquals(listOf(genericId), linked)
    }

    @Test
    fun `freezing generic reasoning links is idempotent across re-runs`() {
        val startDate = LocalDate.of(2026, 8, 1)
        val (decisionId, _) = insertDraftDecisionDirectly(DAYCARE, startDate)
        val reasoning =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)

        val first = db.transaction { tx ->
            tx.freezeGenericReasoningLinks(
                decisionId = decisionId,
                decisionType = DAYCARE,
                startDate = startDate,
                now = now,
                createdBy = admin.evakaUserId,
            )
        }
        val second = db.transaction { tx ->
            tx.freezeGenericReasoningLinks(
                decisionId = decisionId,
                decisionType = DAYCARE,
                startDate = startDate,
                now = now,
                createdBy = admin.evakaUserId,
            )
        }

        assertEquals(listOf(reasoning), first)
        assertEquals(listOf(reasoning), second)
        assertEquals(
            listOf(reasoning),
            db.read { tx -> tx.getDecisionGenericReasoningIds(decisionId) },
        )
    }

    private fun createPlannedDecisionWithApplication(
        type: DecisionType,
        startDate: LocalDate = LocalDate.of(2026, 8, 1),
        guardian: DevPerson = DevPerson(),
        child: DevPerson = DevPerson(dateOfBirth = today.minusYears(3)),
    ): Pair<DecisionId, ApplicationId> = db.transaction { tx ->
        val guardianId = tx.insert(guardian, DevPersonType.ADULT)
        val childId = tx.insert(child, DevPersonType.CHILD)
        val areaId = tx.insert(DevCareArea())
        val unitId = tx.insert(DevDaycare(areaId = areaId))
        val sw = AuthenticatedUser.Employee(admin.id, setOf(UserRole.ADMIN))

        val isPreschool =
            type in setOf(PRESCHOOL, PRESCHOOL_DAYCARE, PREPARATORY_EDUCATION, PRESCHOOL_CLUB)
        val appType = if (isPreschool) ApplicationType.PRESCHOOL else ApplicationType.DAYCARE
        val hasConnectedDaycare = type == PRESCHOOL_DAYCARE

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
                        child = Child(dateOfBirth = child.dateOfBirth),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(unitId)),
                        preferredStartDate = startDate,
                    ),
            )
        applicationStateService.setVerified(
            tx = tx,
            user = sw,
            clock = MockEvakaClock(now),
            applicationId = applicationId,
            confidential = false,
        )
        applicationStateService.createPlacementPlan(
            tx = tx,
            user = sw,
            clock = MockEvakaClock(now),
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

    private fun sendDecisionViaService(applicationId: ApplicationId) {
        db.transaction { tx ->
            applicationStateService.sendDecisionsWithoutProposal(
                tx = tx,
                user = AuthenticatedUser.Employee(admin.id, setOf(UserRole.ADMIN)),
                clock = MockEvakaClock(now),
                applicationId = applicationId,
            )
        }
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))
        db.transaction { tx ->
            applicationStateService.confirmDecisionMailed(
                tx = tx,
                user = AuthenticatedUser.Employee(admin.id, setOf(UserRole.ADMIN)),
                clock = MockEvakaClock(now),
                applicationId = applicationId,
            )
        }
    }

    private fun insertDraftDecisionDirectly(
        type: DecisionType,
        startDate: LocalDate = LocalDate.of(2026, 8, 1),
    ): Pair<DecisionId, ApplicationId> = db.transaction { tx ->
        val guardianId = tx.insert(DevPerson(), DevPersonType.ADULT)
        val childId = tx.insert(DevPerson(dateOfBirth = today.minusYears(3)), DevPersonType.CHILD)
        val areaId = tx.insert(DevCareArea())
        val unitId = tx.insert(DevDaycare(areaId = areaId))
        val applicationId =
            tx.insertTestApplication(
                type = ApplicationType.DAYCARE,
                guardianId = guardianId,
                childId = childId,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        serviceStart = "08:00",
                        serviceEnd = "16:00",
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
INSERT INTO decision (
    created_by, unit_id, application_id, type, start_date, end_date, status, sent_date
) VALUES (
    ${bind(admin.evakaUserId)}, ${bind(unitId)}, ${bind(applicationId)}, ${bind(type)},
    ${bind(startDate)}, ${bind(startDate.plusYears(1))}, ${bind(DecisionStatus.PENDING)}, NULL
)
RETURNING id
"""
                    )
                }
                .executeAndReturnGeneratedKeys()
                .exactlyOne<DecisionId>()
        decisionId to applicationId
    }
}
