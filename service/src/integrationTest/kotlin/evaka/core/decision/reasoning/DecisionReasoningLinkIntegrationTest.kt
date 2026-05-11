// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationType
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
import evaka.core.decision.reasoning.DecisionReasoningCollectionType.DAYCARE as DAYCARE_COLLECTION
import evaka.core.decision.reasoning.DecisionReasoningCollectionType.PRESCHOOL as PRESCHOOL_COLLECTION
import evaka.core.shared.ApplicationId
import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.TestDecision
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.dev.insertTestDecision
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DecisionReasoningLinkIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 5, 11), LocalTime.of(12, 0))
    private val today: LocalDate = now.toLocalDate()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(admin) }
    }

    @Test
    fun `applicableReasoningCollectionTypes returns the right slots per decision type`() {
        assertEquals(setOf(DAYCARE_COLLECTION), DAYCARE.applicableReasoningCollectionTypes())
        assertEquals(
            setOf(DAYCARE_COLLECTION),
            DAYCARE_PART_TIME.applicableReasoningCollectionTypes(),
        )
        assertEquals(setOf(PRESCHOOL_COLLECTION), PRESCHOOL.applicableReasoningCollectionTypes())
        assertEquals(
            setOf(DAYCARE_COLLECTION),
            PRESCHOOL_DAYCARE.applicableReasoningCollectionTypes(),
        )
        assertEquals(
            setOf(PRESCHOOL_COLLECTION),
            PREPARATORY_EDUCATION.applicableReasoningCollectionTypes(),
        )
        assertEquals(setOf(DAYCARE_COLLECTION), CLUB.applicableReasoningCollectionTypes())
        assertEquals(setOf(DAYCARE_COLLECTION), PRESCHOOL_CLUB.applicableReasoningCollectionTypes())
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
    fun `resolver returns the latest ready row whose validFrom is on or before startDate`() {
        val older =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val newer =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 4, 1), ready = true)

        val resolved = db.read { tx ->
            tx.resolveApplicableGenericReasonings(DAYCARE, LocalDate.of(2026, 6, 1))
        }

        assertEquals(1, resolved.size)
        assertEquals(DAYCARE_COLLECTION, resolved[0].collectionType)
        assertEquals(newer, resolved[0].reasoning?.id)
        assertEquals(false, resolved.any { it.reasoning?.id == older })
    }

    @Test
    fun `resolver skips rows with ready = false`() {
        insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = false)

        val resolved = db.read { tx ->
            tx.resolveApplicableGenericReasonings(DAYCARE, LocalDate.of(2026, 6, 1))
        }

        assertEquals(1, resolved.size)
        assertNull(resolved[0].reasoning)
    }

    @Test
    fun `resolver returns only the DAYCARE entry for PRESCHOOL_DAYCARE`() {
        val daycareId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        insertGenericReasoning(PRESCHOOL_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)

        val resolved = db.read { tx ->
            tx.resolveApplicableGenericReasonings(PRESCHOOL_DAYCARE, LocalDate.of(2026, 8, 1))
        }

        assertEquals(1, resolved.size)
        assertEquals(DAYCARE_COLLECTION, resolved[0].collectionType)
        assertEquals(daycareId, resolved[0].reasoning?.id)
    }

    @Test
    fun `resolver returns only the DAYCARE entry for PRESCHOOL_CLUB`() {
        val daycareId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        insertGenericReasoning(PRESCHOOL_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)

        val resolved = db.read { tx ->
            tx.resolveApplicableGenericReasonings(PRESCHOOL_CLUB, LocalDate.of(2026, 8, 1))
        }

        assertEquals(1, resolved.size)
        assertEquals(DAYCARE_COLLECTION, resolved[0].collectionType)
        assertEquals(daycareId, resolved[0].reasoning?.id)
    }

    @Test
    fun `resolver skips removed rows`() {
        val removedId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        db.transaction { tx -> tx.removeGenericReasoning(removedId, now) }

        val resolved = db.read { tx ->
            tx.resolveApplicableGenericReasonings(DAYCARE, LocalDate.of(2026, 6, 1))
        }

        assertEquals(1, resolved.size)
        assertNull(resolved[0].reasoning)
    }

    @Test
    fun `insert and read generic reasoning links`() {
        val (decisionId, _) = createSentDecisionWithApplication(DAYCARE)
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
        val (decisionId, _) = createSentDecisionWithApplication(DAYCARE)

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
        val (decisionId, _) = createSentDecisionWithApplication(DAYCARE)
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
        val (decisionId, _) = createSentDecisionWithApplication(DAYCARE)
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

    /**
     * Creates the minimum rows needed to FK a decision against the database (guardian + child +
     * area + unit + application), then inserts a decision row directly via [insertTestDecision] in
     * the requested status. This bypasses the application state machine — adequate for link-table
     * tests which don't care about state transitions.
     */
    private fun createSentDecisionWithApplication(
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
            tx.insertTestDecision(
                TestDecision(
                    createdBy = admin.evakaUserId,
                    unitId = unitId,
                    applicationId = applicationId,
                    type = type,
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                    status = DecisionStatus.ACCEPTED,
                )
            )
        decisionId to applicationId
    }
}
