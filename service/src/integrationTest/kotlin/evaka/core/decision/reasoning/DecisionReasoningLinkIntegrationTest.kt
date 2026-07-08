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
import evaka.core.decision.DecisionDraftUpdate
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
import evaka.core.decision.updateDecisionDrafts
import evaka.core.shared.ApplicationId
import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionId
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.actionrule.AccessControlFilter
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DecisionReasoningLinkIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 5, 11), LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)
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
            resolveApplicableGenericReasoning(tx, DAYCARE, LocalDate.of(2026, 6, 1))
        }

        assertEquals(DAYCARE_COLLECTION, resolved?.collectionType)
        assertEquals(newer, resolved?.id)
        assertNotEquals(older, resolved?.id)
    }

    @Test
    fun `resolver returns rows with ready = false`() {
        val notReady =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = false)

        val resolved = db.read { tx ->
            resolveApplicableGenericReasoning(tx, DAYCARE, LocalDate.of(2026, 6, 1))
        }

        assertEquals(notReady, resolved?.id)
        assertEquals(false, resolved?.ready)
    }

    @Test
    fun `resolver picks a not-ready newer version over a ready older one`() {
        insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val newerNotReady =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 4, 1), ready = false)

        val resolved = db.read { tx ->
            resolveApplicableGenericReasoning(tx, DAYCARE, LocalDate.of(2026, 5, 19))
        }

        assertEquals(newerNotReady, resolved?.id)
        assertEquals(false, resolved?.ready)
    }

    @Test
    fun `resolver returns the DAYCARE reasoning for PRESCHOOL_DAYCARE`() {
        val daycareId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        insertGenericReasoning(PRESCHOOL_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)

        val resolved = db.read { tx ->
            resolveApplicableGenericReasoning(tx, PRESCHOOL_DAYCARE, LocalDate.of(2026, 8, 1))
        }

        assertEquals(DAYCARE_COLLECTION, resolved?.collectionType)
        assertEquals(daycareId, resolved?.id)
    }

    @Test
    fun `resolver returns the DAYCARE reasoning for PRESCHOOL_CLUB`() {
        val daycareId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        insertGenericReasoning(PRESCHOOL_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)

        val resolved = db.read { tx ->
            resolveApplicableGenericReasoning(tx, PRESCHOOL_CLUB, LocalDate.of(2026, 8, 1))
        }

        assertEquals(DAYCARE_COLLECTION, resolved?.collectionType)
        assertEquals(daycareId, resolved?.id)
    }

    @Test
    fun `resolver skips removed rows`() {
        val removedId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        db.transaction { tx -> tx.removeGenericReasoning(removedId, now) }

        val resolved = db.read { tx ->
            resolveApplicableGenericReasoning(tx, DAYCARE, LocalDate.of(2026, 6, 1))
        }

        assertNull(resolved)
    }

    @Test
    fun `resolver sets validUntil to the day before the next applicable validFrom`() {
        insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 9, 1), ready = true)

        val resolved = db.read { tx ->
            resolveApplicableGenericReasoning(tx, DAYCARE, LocalDate.of(2026, 6, 1))
        }

        assertEquals(LocalDate.of(2026, 8, 31), resolved?.endDate)
    }

    @Test
    fun `resolver ignores removed successors when computing validUntil`() {
        insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val successor =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 9, 1), ready = true)
        db.transaction { tx -> tx.removeGenericReasoning(successor, now) }

        val resolved = db.read { tx ->
            resolveApplicableGenericReasoning(tx, DAYCARE, LocalDate.of(2026, 6, 1))
        }

        assertEquals(null, resolved?.endDate)
    }

    private fun insertIndividualReasoning(key: String): DecisionIndividualReasoningId =
        db.transaction { tx ->
            tx.insertIndividualReasoning(
                DecisionIndividualReasoningRequest(
                    collectionType = DAYCARE_COLLECTION,
                    titleFi = "title-fi-$key",
                    titleSv = "title-sv-$key",
                    textFi = "text-fi-$key",
                    textSv = "text-sv-$key",
                ),
                now,
            )
        }

    @Test
    fun `setting individual reasoning selections replaces the previous set`() {
        val (decisionId, _) = insertDraftDecisionDirectly(DAYCARE)
        val r1 = insertIndividualReasoning("r1")
        val r2 = insertIndividualReasoning("r2")
        val r3 = insertIndividualReasoning("r3")

        db.transaction { tx ->
            tx.setDecisionReasoningIndividualSelections(
                decisionId = decisionId,
                reasoningIds = setOf(r1, r2),
                createdAt = now,
                createdBy = admin.evakaUserId,
            )
        }
        assertEquals(
            setOf(r1, r2),
            db.read { tx -> tx.getDecisionIndividualReasoningIds(decisionId) }.toSet(),
        )

        db.transaction { tx ->
            tx.setDecisionReasoningIndividualSelections(
                decisionId = decisionId,
                reasoningIds = setOf(r2, r3),
                createdAt = now,
                createdBy = admin.evakaUserId,
            )
        }
        assertEquals(
            setOf(r2, r3),
            db.read { tx -> tx.getDecisionIndividualReasoningIds(decisionId) }.toSet(),
        )

        db.transaction { tx ->
            tx.setDecisionReasoningIndividualSelections(
                decisionId = decisionId,
                reasoningIds = emptySet(),
                createdAt = now,
                createdBy = admin.evakaUserId,
            )
        }
        assertEquals(
            emptyList(),
            db.read { tx -> tx.getDecisionIndividualReasoningIds(decisionId) },
        )
    }

    @Test
    fun `sending a decision sets the generic reasoning link inside the same transaction`() {
        val genericId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val (decisionId, applicationId) = createPlannedDecisionWithApplication(DAYCARE)

        sendDecisionViaService(applicationId)

        assertEquals(genericId, getDecisionGenericReasoningId(decisionId))
    }

    @Test
    fun `sending a decision with no eligible generic reasoning throws`() {
        val (_, applicationId) = createPlannedDecisionWithApplication(DAYCARE)

        assertThrows<Conflict> { sendDecisionViaService(applicationId) }
    }

    @Test
    fun `sending a PRESCHOOL_DAYCARE decision sets the DAYCARE link`() {
        val daycareId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        insertGenericReasoning(PRESCHOOL_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val (decisionId, applicationId) = createPlannedDecisionWithApplication(PRESCHOOL_DAYCARE)

        sendDecisionViaService(applicationId)

        assertEquals(daycareId, getDecisionGenericReasoningId(decisionId))
    }

    @Test
    fun `sending a decision via the SFI path also sets the generic reasoning link`() {
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
                user = admin.user,
                clock = clock,
                applicationId = applicationId,
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(genericId, getDecisionGenericReasoningId(decisionId))
    }

    @Test
    fun `generic reasoning link is set at finalize, before the decision pdf job runs`() {
        val genericId =
            insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val (decisionId, applicationId) = createPlannedDecisionWithApplication(DAYCARE)

        db.transaction { tx ->
            applicationStateService.sendDecisionsWithoutProposal(
                tx = tx,
                user = admin.user,
                clock = clock,
                applicationId = applicationId,
            )
        }

        // No async jobs run: the decision pdf has not been generated yet.
        assertEquals(genericId, getDecisionGenericReasoningId(decisionId))
    }

    @Test
    fun `setting the generic reasoning link a second time throws because the column is set-once`() {
        val (decisionId, _) = insertDraftDecisionDirectly(DAYCARE)
        val r1 = insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val r2 = insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 2, 1), ready = true)

        db.transaction { tx -> tx.updateGenericReasoningToDecision(decisionId, r1) }
        assertEquals(r1, getDecisionGenericReasoningId(decisionId))

        assertThrows<NotFound> {
            db.transaction { tx -> tx.updateGenericReasoningToDecision(decisionId, r2) }
        }
        assertEquals(r1, getDecisionGenericReasoningId(decisionId))
    }

    @Test
    fun `getDecisionPdfReasoningSource returns the linked generic text and individual selections`() {
        val (decisionId, _) = insertDraftDecisionDirectly(DAYCARE)
        val genericId =
            insertGenericReasoning(
                DAYCARE_COLLECTION,
                LocalDate.of(2026, 1, 1),
                ready = true,
                textFi = "Yleinen perustelu",
                textSv = "Generisk motivering",
            )
        val individualId = insertIndividualReasoning("r1")
        db.transaction { tx ->
            tx.updateGenericReasoningToDecision(decisionId, genericId)
            tx.setDecisionReasoningIndividualSelections(
                decisionId = decisionId,
                reasoningIds = setOf(individualId),
                createdAt = now,
                createdBy = admin.evakaUserId,
            )
        }

        val source = db.read { tx -> tx.getDecisionPdfReasoningSource(decisionId) }

        assertEquals("Yleinen perustelu", source.generic?.textFi)
        assertEquals("Generisk motivering", source.generic?.textSv)
        assertEquals(listOf("title-fi-r1"), source.individual.map { it.titleFi })
        assertEquals(listOf("text-fi-r1"), source.individual.map { it.textFi })
    }

    @Test
    fun `getDecisionPdfReasoningSource returns null generic and empty individual when nothing is linked`() {
        val (decisionId, _) = insertDraftDecisionDirectly(DAYCARE)

        val source = db.read { tx -> tx.getDecisionPdfReasoningSource(decisionId) }

        assertNull(source.generic)
        assertEquals(emptyList(), source.individual)
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
            user = admin.user,
            clock = clock,
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

    private fun sendDecisionViaService(applicationId: ApplicationId) {
        db.transaction { tx ->
            applicationStateService.sendDecisionsWithoutProposal(
                tx = tx,
                user = admin.user,
                clock = clock,
                applicationId = applicationId,
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)
        db.transaction { tx ->
            applicationStateService.confirmDecisionMailed(
                tx = tx,
                user = admin.user,
                clock = clock,
                applicationId = applicationId,
            )
        }
    }

    private fun getDecisionGenericReasoningId(decisionId: DecisionId): DecisionGenericReasoningId? =
        db.read { tx ->
            tx.createQuery {
                    sql("SELECT generic_reasoning_id FROM decision WHERE id = ${bind(decisionId)}")
                }
                .exactlyOne<DecisionGenericReasoningId?>()
        }

    /** Builds a minimal DecisionDraftUpdate for an existing decision row. */
    private fun draftUpdate(
        decisionId: DecisionId,
        unitId: DaycareId,
        startDate: LocalDate = LocalDate.of(2026, 8, 1),
        individualReasoningIds: Set<DecisionIndividualReasoningId> = emptySet(),
    ) =
        DecisionDraftUpdate(
            id = decisionId,
            unitId = unitId,
            startDate = startDate,
            endDate = startDate.plusYears(1),
            planned = true,
            individualReasoningIds = individualReasoningIds,
        )

    @Test
    fun `updateDecisionDrafts persists individual reasoning ids`() {
        val (decisionId, applicationId) = insertDraftDecisionDirectly(DAYCARE)
        val r1 = insertIndividualReasoning("r1")
        val r2 = insertIndividualReasoning("r2")
        val unitId = db.read { tx ->
            tx.createQuery { sql("SELECT unit_id FROM decision WHERE id = ${bind(decisionId)}") }
                .exactlyOne<DaycareId>()
        }

        db.transaction { tx ->
            updateDecisionDrafts(
                tx,
                applicationId,
                listOf(draftUpdate(decisionId, unitId, individualReasoningIds = setOf(r1, r2))),
                now,
                admin.evakaUserId,
                decisionReasoningEnabled = true,
            )
        }

        assertEquals(
            setOf(r1, r2),
            db.read { tx -> tx.getDecisionIndividualReasoningIds(decisionId) }.toSet(),
        )
    }

    @Test
    fun `updateDecisionDrafts with empty set clears selections`() {
        val (decisionId, applicationId) = insertDraftDecisionDirectly(DAYCARE)
        val r1 = insertIndividualReasoning("r1")
        val unitId = db.read { tx ->
            tx.createQuery { sql("SELECT unit_id FROM decision WHERE id = ${bind(decisionId)}") }
                .exactlyOne<DaycareId>()
        }

        db.transaction { tx ->
            updateDecisionDrafts(
                tx,
                applicationId,
                listOf(draftUpdate(decisionId, unitId, individualReasoningIds = setOf(r1))),
                now,
                admin.evakaUserId,
                decisionReasoningEnabled = true,
            )
        }
        assertEquals(listOf(r1), db.read { tx -> tx.getDecisionIndividualReasoningIds(decisionId) })

        db.transaction { tx ->
            updateDecisionDrafts(
                tx,
                applicationId,
                listOf(draftUpdate(decisionId, unitId, individualReasoningIds = emptySet())),
                now,
                admin.evakaUserId,
                decisionReasoningEnabled = true,
            )
        }
        assertEquals(
            emptyList(),
            db.read { tx -> tx.getDecisionIndividualReasoningIds(decisionId) },
        )
    }

    @Test
    fun `updateDecisionDrafts rejects a removed reasoning`() {
        val (decisionId, applicationId) = insertDraftDecisionDirectly(DAYCARE)
        val r1 = insertIndividualReasoning("removed")
        db.transaction { tx -> tx.removeIndividualReasoning(r1, now) }
        val unitId = db.read { tx ->
            tx.createQuery { sql("SELECT unit_id FROM decision WHERE id = ${bind(decisionId)}") }
                .exactlyOne<DaycareId>()
        }

        assertThrows<BadRequest> {
            db.transaction { tx ->
                updateDecisionDrafts(
                    tx,
                    applicationId,
                    listOf(draftUpdate(decisionId, unitId, individualReasoningIds = setOf(r1))),
                    now,
                    admin.evakaUserId,
                    decisionReasoningEnabled = true,
                )
            }
        }
    }

    @Test
    fun `updateDecisionDrafts rejects a reasoning whose collectionType does not match the decision type`() {
        val (decisionId, applicationId) = insertDraftDecisionDirectly(DAYCARE)
        // PRESCHOOL_COLLECTION reasoning cannot be used on a DAYCARE decision
        val preschoolReasoning = db.transaction { tx ->
            tx.insertIndividualReasoning(
                DecisionIndividualReasoningRequest(
                    collectionType = PRESCHOOL_COLLECTION,
                    titleFi = "wrong-type-fi",
                    titleSv = "wrong-type-sv",
                    textFi = "wrong-fi",
                    textSv = "wrong-sv",
                ),
                now,
            )
        }
        val unitId = db.read { tx ->
            tx.createQuery { sql("SELECT unit_id FROM decision WHERE id = ${bind(decisionId)}") }
                .exactlyOne<DaycareId>()
        }

        assertThrows<BadRequest> {
            db.transaction { tx ->
                updateDecisionDrafts(
                    tx,
                    applicationId,
                    listOf(
                        draftUpdate(
                            decisionId,
                            unitId,
                            individualReasoningIds = setOf(preschoolReasoning),
                        )
                    ),
                    now,
                    admin.evakaUserId,
                    decisionReasoningEnabled = true,
                )
            }
        }
    }

    @Test
    fun `updateDecisionDrafts is rejected after the decision is sent`() {
        insertGenericReasoning(DAYCARE_COLLECTION, LocalDate.of(2026, 1, 1), ready = true)
        val (decisionId, applicationId) = createPlannedDecisionWithApplication(DAYCARE)
        sendDecisionViaService(applicationId)
        val r1 = insertIndividualReasoning("r1")
        val unitId = unitIdOf(decisionId)

        assertThrows<NotFound> {
            db.transaction { tx ->
                updateDecisionDrafts(
                    tx,
                    applicationId,
                    listOf(draftUpdate(decisionId, unitId, individualReasoningIds = setOf(r1))),
                    now,
                    admin.evakaUserId,
                    decisionReasoningEnabled = true,
                )
            }
        }
    }

    @Test
    fun `updateDecisionDrafts is rejected when the reasoning id does not exist`() {
        val (decisionId, applicationId) = insertDraftDecisionDirectly(DAYCARE)
        val unitId = unitIdOf(decisionId)
        val bogusReasoningId = DecisionIndividualReasoningId(java.util.UUID.randomUUID())

        assertThrows<NotFound> {
            db.transaction { tx ->
                updateDecisionDrafts(
                    tx,
                    applicationId,
                    listOf(
                        draftUpdate(
                            decisionId,
                            unitId,
                            individualReasoningIds = setOf(bogusReasoningId),
                        )
                    ),
                    now,
                    admin.evakaUserId,
                    decisionReasoningEnabled = true,
                )
            }
        }
    }

    @Test
    fun `updateDecisionDrafts is rejected when the decision does not exist`() {
        val (decisionId, applicationId) = insertDraftDecisionDirectly(DAYCARE)
        val unitId = unitIdOf(decisionId)
        val bogusDecisionId = DecisionId(java.util.UUID.randomUUID())

        assertThrows<NotFound> {
            db.transaction { tx ->
                updateDecisionDrafts(
                    tx,
                    applicationId,
                    listOf(draftUpdate(bogusDecisionId, unitId)),
                    now,
                    admin.evakaUserId,
                    decisionReasoningEnabled = true,
                )
            }
        }
    }

    private fun unitIdOf(decisionId: DecisionId): DaycareId = db.read { tx ->
        tx.createQuery { sql("SELECT unit_id FROM decision WHERE id = ${bind(decisionId)}") }
            .exactlyOne<DaycareId>()
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
