// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision

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
import evaka.core.decision.reasoning.DecisionReasoningCollectionType
import evaka.core.placement.PlacementPlanConfirmationStatus
import evaka.core.shared.ApplicationId
import evaka.core.shared.DaycareId
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
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired

class DecisionDecidedByIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val planCreator =
        DevEmployee(firstName = "Plan", lastName = "Creator", roles = setOf(UserRole.ADMIN))
    private val sender =
        DevEmployee(firstName = "Decision", lastName = "Sender", roles = setOf(UserRole.ADMIN))
    private val otherSender =
        DevEmployee(firstName = "Other", lastName = "Sender", roles = setOf(UserRole.ADMIN))
    private val proposalAcceptor =
        DevEmployee(firstName = "Proposal", lastName = "Acceptor", roles = setOf(UserRole.ADMIN))

    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 5, 11), LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)
    private val today: LocalDate = now.toLocalDate()
    private val startDate: LocalDate = LocalDate.of(2026, 8, 1)

    @Autowired private lateinit var applicationStateService: ApplicationStateService

    private lateinit var unitId: DaycareId
    private lateinit var applicationId: ApplicationId
    private lateinit var decisionId: DecisionId

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(planCreator)
            tx.insert(sender)
            tx.insert(otherSender)
            tx.insert(proposalAcceptor)
            tx.insert(
                DevDecisionReasoningGeneric(
                    collectionType = DecisionReasoningCollectionType.DAYCARE,
                    validFrom = LocalDate.of(2026, 1, 1),
                    textFi = "fi-generic-text",
                    textSv = "sv-generic-text",
                    ready = true,
                    createdAt = now,
                    modifiedAt = now,
                )
            )
            val areaId = tx.insert(DevCareArea())
            unitId = tx.insert(DevDaycare(areaId = areaId))
            val guardianId = tx.insert(DevPerson(), DevPersonType.ADULT)
            val childId =
                tx.insert(DevPerson(dateOfBirth = today.minusYears(3)), DevPersonType.CHILD)
            applicationId =
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
                user = planCreator.user,
                clock = clock,
                audit = AuditContext(),
                applicationId = applicationId,
                confidential = false,
            )
            applicationStateService.createPlacementPlan(
                tx = tx,
                user = planCreator.user,
                clock = clock,
                audit = AuditContext(),
                applicationId = applicationId,
                placementPlan =
                    DaycarePlacementPlan(
                        unitId = unitId,
                        period = FiniteDateRange(startDate, startDate.plusYears(1)),
                    ),
            )
            decisionId =
                tx.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll)
                    .single()
                    .id
        }
    }

    @Test
    fun `an unsent decision falls back to the creator of the placement plan`() {
        assertEquals(nameOf(planCreator), decidedByName())
    }

    @Test
    fun `sendDecisionsWithoutProposal sets the sender as the decider`() {
        sendDecisionsWithoutProposal(sender)

        assertEquals(nameOf(sender), decidedByName())
    }

    @Test
    fun `the sender of a placement proposal stays the decider after the unit accepts it`() {
        sendPlacementProposal(sender)
        db.transaction { tx ->
            applicationStateService.respondToPlacementProposal(
                tx,
                proposalAcceptor.user,
                clock,
                AuditContext(),
                applicationId,
                PlacementPlanConfirmationStatus.ACCEPTED,
            )
            applicationStateService.confirmPlacementProposalChanges(
                tx,
                proposalAcceptor.user,
                clock,
                AuditContext(),
                unitId,
                rejectReasonTranslations = emptyMap(),
            )
        }

        val decision = db.read { tx -> tx.getDecision(decisionId)!! }
        assertNotNull(decision.sentDate)
        assertEquals(nameOf(sender), decision.decidedByName)
    }

    @Test
    fun `withdrawing a placement proposal clears the decider and a resend sets the new sender`() {
        sendPlacementProposal(sender)
        db.transaction { tx ->
            applicationStateService.withdrawPlacementProposal(
                tx,
                sender.user,
                clock,
                AuditContext(),
                applicationId,
            )
        }
        assertEquals(nameOf(planCreator), decidedByName())

        sendPlacementProposal(otherSender)
        assertEquals(nameOf(otherSender), decidedByName())
    }

    @Test
    fun `the decider is set when decision reasonings are disabled`() {
        whenever(evakaEnv.decisionReasoningEnabled).thenReturn(false)

        sendDecisionsWithoutProposal(sender)

        assertEquals(nameOf(sender), decidedByName())
    }

    private fun sendDecisionsWithoutProposal(employee: DevEmployee) {
        db.transaction { tx ->
            applicationStateService.sendDecisionsWithoutProposal(
                tx,
                employee.user,
                clock,
                AuditContext(),
                applicationId,
            )
        }
    }

    private fun sendPlacementProposal(employee: DevEmployee) {
        db.transaction { tx ->
            applicationStateService.sendPlacementProposal(
                tx,
                employee.user,
                clock,
                AuditContext(),
                applicationId,
            )
        }
    }

    private fun decidedByName(): String = db.read { tx ->
        tx.getDecision(decisionId)!!.decidedByName
    }

    private fun nameOf(employee: DevEmployee): String = db.read { tx ->
        tx.createQuery {
                sql("SELECT name FROM evaka_user WHERE employee_id = ${bind(employee.id)}")
            }
            .exactlyOne()
    }
}
