// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionDraft
import fi.espoo.evaka.decision.DecisionDraftService
import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.fetchDecisionDrafts
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.data.getIncomesForPerson
import fi.espoo.evaka.invoicing.data.upsertIncome
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.placement.Placement
import fi.espoo.evaka.placement.PlacementPlan
import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementPlanRejectReason
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementPlan
import fi.espoo.evaka.placement.getPlacementsForChild
import fi.espoo.evaka.preschoolTerm2020
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.message.MockEvakaMessageClient
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testChild_7
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import org.jdbi.v3.core.Handle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationStateServiceIntegrationTests : FullApplicationTest() {
    @Autowired
    private lateinit var service: ApplicationStateService

    @Autowired
    private lateinit var decisionService: DecisionService

    @Autowired
    private lateinit var decisionDraftService: DecisionDraftService

    @Autowired
    private lateinit var asyncJobRunner: AsyncJobRunner

    @Autowired
    lateinit var mapper: ObjectMapper

    private val serviceWorker = AuthenticatedUser(testDecisionMaker_1.id, setOf(Roles.SERVICE_WORKER))

    private val applicationId = UUID.randomUUID()
    val address = Address(
        street = "Street 1",
        postalCode = "00200",
        postOffice = "Espoo"
    )
    val mainPeriod = preschoolTerm2020
    val connectedPeriod = ClosedPeriod(preschoolTerm2020.start.minusDays(15), preschoolTerm2020.end.plusDays(15))

    @BeforeEach
    private fun beforeEach() {
        MockEvakaMessageClient.clearMessages()
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
        }
    }

    @Test
    fun `sendApplication - preschool has due date same as sent date`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.PRESCHOOL)

        // when
        service.sendApplication(h, serviceWorker, applicationId)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.SENT, application.status)
        assertEquals(LocalDate.now(), application.sentDate)
        assertEquals(LocalDate.now(), application.dueDate)
    }

    @Test
    fun `sendApplication - daycare has due date after 4 months if not urgent`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.DAYCARE, urgent = false)

        // when
        service.sendApplication(h, serviceWorker, applicationId)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(LocalDate.now().plusMonths(4), application.dueDate)
    }

    @Test
    fun `sendApplication - daycare has due date after 2 weeks if urgent`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.DAYCARE, urgent = true)

        // when
        service.sendApplication(h, serviceWorker, applicationId)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(LocalDate.now().plusWeeks(2), application.dueDate)
    }

    @Test
    fun `sendApplication - daycare has not due date if a transfer application`() = jdbi.handle { h ->
        // given
        val draft = insertDraftApplication(h, appliedType = PlacementType.DAYCARE, urgent = false)
        h.insertTestPlacement(
            DevPlacement(
                childId = draft.childId,
                unitId = testDaycare2.id,
                startDate = draft.form.preferences.preferredStartDate!!,
                endDate = draft.form.preferences.preferredStartDate!!.plusYears(1)
            )
        )

        // when
        service.sendApplication(h, serviceWorker, applicationId)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(true, application.transferApplication)
        assertEquals(null, application.dueDate)
    }

    @Test
    fun `moveToWaitingPlacement without otherInfo - status is changed and checkedByAdmin defaults true`() =
        jdbi.handle { h ->
            // given
            insertDraftApplication(h, hasAdditionalInfo = false)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(true, application.checkedByAdmin)
        }

    @Test
    fun `moveToWaitingPlacement with otherInfo - status is changed and checkedByAdmin defaults false`() =
        jdbi.handle { h ->
            // given
            insertDraftApplication(h, hasAdditionalInfo = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(false, application.checkedByAdmin)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - new income has been created`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
        service.sendApplication(h, serviceWorker, applicationId)

        // when
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(true, application.form.maxFeeAccepted)
        val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
        assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
        assertEquals(1, incomes.size)
        assertEquals(IncomeEffect.MAX_FEE_ACCEPTED, incomes.first().effect)
    }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - existing overlapping indefinite income will be handled`() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val earlierIndefinite = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().minusDays(10),
                validTo = null
            )
            upsertIncome(h, mapper, earlierIndefinite, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(2, incomes.size)
            assertEquals(IncomeEffect.MAX_FEE_ACCEPTED, incomes.first().effect)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes[1].effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - existing overlapping income will be handled by not adding a new income for user`() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val earlierIncome = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().minusDays(10),
                validTo = LocalDate.now().plusMonths(5)
            )
            upsertIncome(h, mapper, earlierIncome, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(1, incomes.size)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - later indefinite income will be handled by not adding a new income`() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val laterIndefiniteIncome = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().plusMonths(5),
                validTo = null
            )
            upsertIncome(h, mapper, laterIndefiniteIncome, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(1, incomes.size)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - earlier income does not affect creating a new income`() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val earlierIncome = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().minusMonths(7),
                validTo = LocalDate.now().minusMonths(5)
            )
            upsertIncome(h, mapper, earlierIncome, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(2, incomes.size)
            assertEquals(IncomeEffect.MAX_FEE_ACCEPTED, incomes.first().effect)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes[1].effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - later income will be handled by not adding a new income`() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val laterIncome = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().plusMonths(5),
                validTo = LocalDate.now().plusMonths(6)
            )
            upsertIncome(h, mapper, laterIncome, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(1, incomes.size)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - if application does not have a preferred start date income will not be created`() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val earlierIndefinite = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().minusDays(10),
                validTo = null
            )
            upsertIncome(h, mapper, earlierIndefinite, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true, preferredStartDate = null)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(1, incomes.size)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - no new income will be created if there exists indefinite income for the same day `() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val sameDayIncomeIndefinite = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().plusMonths(4),
                validTo = null
            )
            upsertIncome(h, mapper, sameDayIncomeIndefinite, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(1, incomes.size)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - no new income will be created if there exists income for the same day `() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val sameDayIncome = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().plusMonths(4),
                validTo = LocalDate.now().plusMonths(5)
            )
            upsertIncome(h, mapper, sameDayIncome, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(1, incomes.size)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - new income will be created if there exists indefinite income for the same day - 1 `() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val dayBeforeIncomeIndefinite = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().plusMonths(4).minusDays(1),
                validTo = null
            )
            upsertIncome(h, mapper, dayBeforeIncomeIndefinite, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(2, incomes.size)
            assertEquals(IncomeEffect.MAX_FEE_ACCEPTED, incomes.first().effect)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes[1].effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - no new income will be created if there exists indefinite income for the same day + 1 `() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val nextDayIncomeIndefinite = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().plusMonths(4).plusDays(1),
                validTo = null
            )
            upsertIncome(h, mapper, nextDayIncomeIndefinite, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(1, incomes.size)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - no new income will be created if there exists income for the same day - 1 `() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val IncomeDayBefore = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().plusMonths(4).minusDays(1),
                validTo = LocalDate.now().plusMonths(5)
            )
            upsertIncome(h, mapper, IncomeDayBefore, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(1, incomes.size)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - no new income will be created if there exists income that ends on the same day`() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val earlierIncomeEndingOnSameDay = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().minusMonths(4),
                validTo = LocalDate.now().plusMonths(4)
            )
            upsertIncome(h, mapper, earlierIncomeEndingOnSameDay, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(1, incomes.size)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - no new income will be created if there exists income that ends on the same day + 1`() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val earlierIncomeEndingOnNextDay = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().minusMonths(4),
                validTo = LocalDate.now().plusMonths(4).plusDays(1)
            )
            upsertIncome(h, mapper, earlierIncomeEndingOnNextDay, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(1, incomes.size)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes.first().effect)
        }

    @Test
    fun `moveToWaitingPlacement with maxFeeAccepted - no new income will be created if there exists income that ends on the same day - 1`() =
        jdbi.handle { h ->
            // given
            val financeUser = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(Roles.FINANCE_ADMIN))
            val earlierIncomeEndingOnDayBefore = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.NOT_AVAILABLE,
                notes = "Income not available",
                personId = testAdult_5.id,
                validFrom = LocalDate.now().minusMonths(4),
                validTo = LocalDate.now().plusMonths(4).minusDays(1)
            )
            upsertIncome(h, mapper, earlierIncomeEndingOnDayBefore, financeUser.id)
            insertDraftApplication(h, guardian = testAdult_5, maxFeeAccepted = true)
            service.sendApplication(h, serviceWorker, applicationId)

            // when
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(true, application.form.maxFeeAccepted)
            val incomes = getIncomesForPerson(h, mapper, testAdult_5.id)
            assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
            assertEquals(2, incomes.size)
            assertEquals(IncomeEffect.MAX_FEE_ACCEPTED, incomes.first().effect)
            assertEquals(IncomeEffect.NOT_AVAILABLE, incomes[1].effect)
        }

    @Test
    fun `moveToWaitingPlacement - guardian contact details are updated`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.DAYCARE)
        service.sendApplication(h, serviceWorker, applicationId)

        // when
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)

        // then
        val guardian = h.getPersonById(testAdult_5.id)!!
        assertEquals("abc@espoo.fi", guardian.email)
        assertEquals("0501234567", guardian.phone)
    }

    @Test
    fun `moveToWaitingPlacement - child is upserted with diet and allergies`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.DAYCARE, hasAdditionalInfo = true)
        service.sendApplication(h, serviceWorker, applicationId)

        // when
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)

        // then
        val childDetails = h.getChild(testChild_6.id)!!.additionalInformation
        assertEquals("diet", childDetails.diet)
        assertEquals("allergies", childDetails.allergies)
    }

    @Test
    fun `setVerified and setUnverified - changes checkedByAdmin`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, hasAdditionalInfo = true)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)

        // when
        service.setVerified(h, serviceWorker, applicationId)

        // then
        var application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
        assertEquals(true, application.checkedByAdmin)

        // when
        service.setUnverified(h, serviceWorker, applicationId)

        // then
        application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)
        assertEquals(false, application.checkedByAdmin)
    }

    @Test
    fun `cancelApplication from SENT - status is changed`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h)
        service.sendApplication(h, serviceWorker, applicationId)

        // when
        service.cancelApplication(h, serviceWorker, applicationId)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.CANCELLED, application.status)
    }

    @Test
    fun `cancelApplication from WAITING_PLACEMENT - status is changed`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)

        // when
        service.cancelApplication(h, serviceWorker, applicationId)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.CANCELLED, application.status)
    }

    @Test
    fun `returnToSent - status is changed`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)

        // when
        service.returnToSent(h, serviceWorker, applicationId)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.SENT, application.status)
    }

    @Test
    fun `createPlacementPlan - daycare`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.DAYCARE)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)

        // when
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = mainPeriod
            )
        )

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

        val placementPlan = getPlacementPlan(h, applicationId)!!
        assertEquals(
            PlacementPlan(
                id = placementPlan.id,
                unitId = testDaycare.id,
                applicationId = applicationId,
                type = PlacementType.DAYCARE,
                period = mainPeriod,
                preschoolDaycarePeriod = null
            ),
            placementPlan
        )

        val decisionDrafts = fetchDecisionDrafts(h, applicationId)
        assertEquals(1, decisionDrafts.size)
        assertEquals(
            DecisionDraft(
                id = decisionDrafts.first().id,
                type = DecisionType.DAYCARE,
                startDate = mainPeriod.start,
                endDate = mainPeriod.end,
                unitId = testDaycare.id,
                planned = true
            ),
            decisionDrafts.first()
        )
    }

    @Test
    fun `createPlacementPlan - daycare part-time`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.DAYCARE_PART_TIME)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)

        // when
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = mainPeriod
            )
        )

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

        val placementPlan = getPlacementPlan(h, applicationId)!!
        assertEquals(
            PlacementPlan(
                id = placementPlan.id,
                unitId = testDaycare.id,
                applicationId = applicationId,
                type = PlacementType.DAYCARE_PART_TIME,
                period = mainPeriod,
                preschoolDaycarePeriod = null
            ),
            placementPlan
        )

        val decisionDrafts = fetchDecisionDrafts(h, applicationId)
        assertEquals(1, decisionDrafts.size)
        assertEquals(
            DecisionDraft(
                id = decisionDrafts.first().id,
                type = DecisionType.DAYCARE_PART_TIME,
                startDate = mainPeriod.start,
                endDate = mainPeriod.end,
                unitId = testDaycare.id,
                planned = true
            ),
            decisionDrafts.first()
        )
    }

    @Test
    fun `createPlacementPlan - preschool`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.PRESCHOOL)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)

        // when
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = mainPeriod
            )
        )

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

        val placementPlan = getPlacementPlan(h, applicationId)!!
        assertEquals(
            PlacementPlan(
                id = placementPlan.id,
                unitId = testDaycare.id,
                applicationId = applicationId,
                type = PlacementType.PRESCHOOL,
                period = mainPeriod,
                preschoolDaycarePeriod = null
            ),
            placementPlan
        )

        val decisionDrafts = fetchDecisionDrafts(h, applicationId)
        assertEquals(2, decisionDrafts.size)

        decisionDrafts.find { it.type == DecisionType.PRESCHOOL }!!.let {
            assertEquals(
                DecisionDraft(
                    id = it.id,
                    type = DecisionType.PRESCHOOL,
                    startDate = mainPeriod.start,
                    endDate = mainPeriod.end,
                    unitId = testDaycare.id,
                    planned = true
                ),
                it
            )
        }
        decisionDrafts.find { it.type == DecisionType.PRESCHOOL_DAYCARE }!!.let {
            assertEquals(
                DecisionDraft(
                    id = it.id,
                    type = DecisionType.PRESCHOOL_DAYCARE,
                    startDate = mainPeriod.start,
                    endDate = mainPeriod.end,
                    unitId = testDaycare.id,
                    planned = false
                ),
                it
            )
        }
    }

    @Test
    fun `createPlacementPlan - preschool with daycare`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.PRESCHOOL_DAYCARE)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)

        // when
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = mainPeriod,
                preschoolDaycarePeriod = connectedPeriod
            )
        )

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

        val placementPlan = getPlacementPlan(h, applicationId)!!
        assertEquals(
            PlacementPlan(
                id = placementPlan.id,
                unitId = testDaycare.id,
                applicationId = applicationId,
                type = PlacementType.PRESCHOOL_DAYCARE,
                period = mainPeriod,
                preschoolDaycarePeriod = connectedPeriod
            ),
            placementPlan
        )

        val decisionDrafts = fetchDecisionDrafts(h, applicationId)
        assertEquals(2, decisionDrafts.size)

        decisionDrafts.find { it.type == DecisionType.PRESCHOOL }!!.let {
            assertEquals(
                DecisionDraft(
                    id = it.id,
                    type = DecisionType.PRESCHOOL,
                    startDate = mainPeriod.start,
                    endDate = mainPeriod.end,
                    unitId = testDaycare.id,
                    planned = true
                ),
                it
            )
        }
        decisionDrafts.find { it.type == DecisionType.PRESCHOOL_DAYCARE }!!.let {
            assertEquals(
                DecisionDraft(
                    id = it.id,
                    type = DecisionType.PRESCHOOL_DAYCARE,
                    startDate = connectedPeriod.start,
                    endDate = connectedPeriod.end,
                    unitId = testDaycare.id,
                    planned = true
                ),
                it
            )
        }
    }

    @Test
    fun `cancelPlacementPlan - removes placement plan and decision drafts and changes status`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.PRESCHOOL_DAYCARE)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = mainPeriod,
                preschoolDaycarePeriod = connectedPeriod
            )
        )

        // when
        service.cancelPlacementPlan(h, serviceWorker, applicationId)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.WAITING_PLACEMENT, application.status)

        val placementPlan = getPlacementPlan(h, applicationId)
        assertEquals(null, placementPlan)

        val decisionDrafts = fetchDecisionDrafts(h, applicationId)
        assertEquals(0, decisionDrafts.size)
    }

    @Test
    fun `confirmPlacementWithoutDecision - applies placement plan, removes decision drafts and changes status`() =
        jdbi.handle { h ->
            // given
            val child = testChild_6
            insertDraftApplication(h, appliedType = PlacementType.PRESCHOOL_DAYCARE, child = child)
            service.sendApplication(h, serviceWorker, applicationId)
            service.moveToWaitingPlacement(h, serviceWorker, applicationId)
            service.createPlacementPlan(
                h,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = testDaycare.id,
                    period = mainPeriod,
                    preschoolDaycarePeriod = connectedPeriod
                )
            )

            // when
            service.confirmPlacementWithoutDecision(h, serviceWorker, applicationId)

            // then
            val application = fetchApplicationDetails(h, applicationId)!!
            assertEquals(ApplicationStatus.ACTIVE, application.status)

            val placementPlan = getPlacementPlan(h, applicationId)
            assertEquals(null, placementPlan)

            val placements = h.getPlacementsForChild(child.id)
            assertEquals(2, placements.size)
            placements
                .find { it.type === PlacementType.PRESCHOOL_DAYCARE }!!
                .let {
                    assertEquals(
                        Placement(
                            id = it.id,
                            type = PlacementType.PRESCHOOL_DAYCARE,
                            childId = child.id,
                            unitId = testDaycare.id,
                            startDate = connectedPeriod.start,
                            endDate = mainPeriod.end
                        ),
                        it
                    )
                }
            placements
                .find { it.type === PlacementType.DAYCARE }!!
                .let {
                    assertEquals(
                        Placement(
                            id = it.id,
                            type = PlacementType.DAYCARE,
                            childId = child.id,
                            unitId = testDaycare.id,
                            startDate = mainPeriod.end.plusDays(1),
                            endDate = connectedPeriod.end
                        ),
                        it
                    )
                }

            val decisionDrafts = fetchDecisionDrafts(h, applicationId)
            assertEquals(0, decisionDrafts.size)

            val decisions = getDecisionsByApplication(h, applicationId, AclAuthorization.All)
            assertEquals(0, decisions.size)
        }

    @Test
    fun `sendDecisionsWithoutProposal - applier is the only guardian`() = jdbi.handle { h ->
        sendDecisionsWithoutProposalTest(
            h,
            child = testChild_2,
            applier = testAdult_1,
            secondDecisionTo = null,
            manualMailing = false
        )
    }

    @Test
    fun `sendDecisionsWithoutProposal - applier is guardian and other guardian exists in same address`() =
        jdbi.handle { h ->
            sendDecisionsWithoutProposalTest(
                h,
                child = testChild_1,
                applier = testAdult_1,
                secondDecisionTo = null,
                manualMailing = false
            )
        }

    @Test
    fun `sendDecisionsWithoutProposal - applier is guardian and other guardian exists in different address`() =
        jdbi.handle { h ->
            sendDecisionsWithoutProposalTest(
                h,
                child = testChild_6,
                applier = testAdult_5,
                secondDecisionTo = testAdult_6,
                manualMailing = false
            )
        }

    @Test
    fun `sendDecisionsWithoutProposal - child has no ssn`() = jdbi.handle { h ->
        sendDecisionsWithoutProposalTest(
            h,
            child = testChild_7,
            applier = testAdult_5,
            secondDecisionTo = null,
            manualMailing = true
        )
    }

    @Test
    fun `sendDecisionsWithoutProposal - applier has no ssn, child has no guardian`() = jdbi.handle { h ->
        sendDecisionsWithoutProposalTest(
            h,
            child = testChild_7,
            applier = testAdult_4,
            secondDecisionTo = null,
            manualMailing = true
        )
    }

    @Test
    fun `sendDecisionsWithoutProposal - applier has no ssn, child has another guardian`() = jdbi.handle { h ->
        sendDecisionsWithoutProposalTest(
            h,
            child = testChild_2,
            applier = testAdult_4,
            secondDecisionTo = testChild_1,
            manualMailing = true
        )
    }

    private fun sendDecisionsWithoutProposalTest(
        h: Handle,
        child: PersonData.Detailed,
        applier: PersonData.Detailed,
        secondDecisionTo: PersonData.Detailed?,
        manualMailing: Boolean
    ) {
        // given
        insertDraftApplication(h, appliedType = PlacementType.PRESCHOOL, guardian = applier, child = child)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(unitId = testDaycare.id, period = mainPeriod)
        )

        // when
        service.sendDecisionsWithoutProposal(h, serviceWorker, applicationId)
        asyncJobRunner.runPendingJobsSync()
        asyncJobRunner.runPendingJobsSync()

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        if (manualMailing) {
            assertEquals(ApplicationStatus.WAITING_MAILING, application.status)
        } else {
            assertEquals(ApplicationStatus.WAITING_CONFIRMATION, application.status)
        }

        val decisionsByApplication = getDecisionsByApplication(h, applicationId, AclAuthorization.All)
        assertEquals(1, decisionsByApplication.size)
        val decision = decisionsByApplication.first()
        assertNotNull(decision.sentDate)
        assertNotNull(decision.documentUri)

        if (secondDecisionTo == null) {
            assertNull(decision.otherGuardianDocumentUri)
        } else {
            assertNotNull(decision.otherGuardianDocumentUri)
        }

        val messages = MockEvakaMessageClient.getMessages()
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
    }

    @Test
    fun `sendPlacementProposal - updates status`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.PRESCHOOL_DAYCARE)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = mainPeriod,
                preschoolDaycarePeriod = connectedPeriod
            )
        )

        // when
        service.sendPlacementProposal(h, serviceWorker, applicationId)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.WAITING_UNIT_CONFIRMATION, application.status)

        val decisions = getDecisionsByApplication(h, applicationId, AclAuthorization.All)
        assertEquals(0, decisions.size)
    }

    @Test
    fun `withdrawPlacementProposal - updates status`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.PRESCHOOL_DAYCARE)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = mainPeriod,
                preschoolDaycarePeriod = connectedPeriod
            )
        )
        service.sendPlacementProposal(h, serviceWorker, applicationId)

        // when
        service.withdrawPlacementProposal(h, serviceWorker, applicationId)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.WAITING_DECISION, application.status)

        val decisions = getDecisionsByApplication(h, applicationId, AclAuthorization.All)
        assertEquals(0, decisions.size)
    }

    @Test
    fun `acceptPlacementProposal - sends decisions and updates status`() = jdbi.handle { h ->
        // given
        insertDraftApplication(
            h, appliedType = PlacementType.PRESCHOOL_DAYCARE,
            child = testChild_2,
            guardian = testAdult_1
        )
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = mainPeriod,
                preschoolDaycarePeriod = connectedPeriod
            )
        )
        service.sendPlacementProposal(h, serviceWorker, applicationId)

        // when
        service.respondToPlacementProposal(h, serviceWorker, applicationId, PlacementPlanConfirmationStatus.ACCEPTED)
        service.acceptPlacementProposal(h, serviceWorker, testDaycare.id)
        asyncJobRunner.runPendingJobsSync()
        asyncJobRunner.runPendingJobsSync()

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.WAITING_CONFIRMATION, application.status)

        val decisionsByApplication = getDecisionsByApplication(h, applicationId, AclAuthorization.All)
        assertEquals(2, decisionsByApplication.size)
        decisionsByApplication.forEach { decision ->
            assertNotNull(decision.sentDate)
            assertNotNull(decision.documentUri)
            assertNull(decision.otherGuardianDocumentUri)
        }
        val messages = MockEvakaMessageClient.getMessages()
        assertEquals(2, messages.size)
        assertEquals(2, messages.filter { it.ssn == testAdult_1.ssn }.size)
    }

    @Test
    fun `acceptPlacementProposal - if no decisions are marked for sending go straight to active`() = jdbi.handle { h ->
        // given
        insertDraftApplication(
            h, appliedType = PlacementType.PRESCHOOL_DAYCARE,
            child = testChild_2,
            guardian = testAdult_1
        )
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = mainPeriod,
                preschoolDaycarePeriod = connectedPeriod
            )
        )
        fetchDecisionDrafts(h, applicationId).map { draft ->
            DecisionDraftService.DecisionDraftUpdate(
                id = draft.id,
                unitId = draft.unitId,
                startDate = draft.startDate,
                endDate = draft.endDate,
                planned = false
            )
        }.let { updates -> decisionDraftService.updateDecisionDrafts(h, applicationId, updates) }
        service.sendPlacementProposal(h, serviceWorker, applicationId)

        // when
        service.respondToPlacementProposal(h, serviceWorker, applicationId, PlacementPlanConfirmationStatus.ACCEPTED)
        service.acceptPlacementProposal(h, serviceWorker, testDaycare.id)
        asyncJobRunner.runPendingJobsSync()
        asyncJobRunner.runPendingJobsSync()

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.ACTIVE, application.status)

        val decisionDrafts = fetchDecisionDrafts(h, applicationId)
        assertEquals(0, decisionDrafts.size)

        val decisionsByApplication = getDecisionsByApplication(h, applicationId, AclAuthorization.All)
        assertEquals(0, decisionsByApplication.size)

        val messages = MockEvakaMessageClient.getMessages()
        assertEquals(0, messages.size)

        val placementPlan = getPlacementPlan(h, applicationId)
        assertNull(placementPlan)

        val placements = h.getPlacementsForChild(testChild_2.id)
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

    @Test
    fun `acceptPlacementProposal - throws if some application is pending response`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.PRESCHOOL, child = testChild_2, guardian = testAdult_1)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = mainPeriod
            )
        )
        service.sendPlacementProposal(h, serviceWorker, applicationId)

        // when / then
        assertThrows<BadRequest> { service.acceptPlacementProposal(h, serviceWorker, testDaycare.id) }
    }

    @Test
    fun `acceptPlacementProposal - throws if some application has been rejected`() = jdbi.handle { h ->
        // given
        insertDraftApplication(h, appliedType = PlacementType.PRESCHOOL, child = testChild_2, guardian = testAdult_1)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = mainPeriod
            )
        )
        service.sendPlacementProposal(h, serviceWorker, applicationId)

        // when / then
        service.respondToPlacementProposal(
            h,
            serviceWorker,
            applicationId,
            PlacementPlanConfirmationStatus.REJECTED,
            PlacementPlanRejectReason.REASON_1
        )
        assertThrows<BadRequest> { service.acceptPlacementProposal(h, serviceWorker, testDaycare.id) }
    }

    @Test
    fun `reject preschool decision rejects preschool daycare decision too`() = jdbi.handle { h ->
        // given
        workflowForPreschoolDaycareDecisions(h)

        // when
        service.rejectDecision(h, serviceWorker, applicationId, getDecision(h, DecisionType.PRESCHOOL).id)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.REJECTED, application.status)

        with(getDecision(h, DecisionType.PRESCHOOL)) {
            assertEquals(DecisionStatus.REJECTED, status)
        }
        with(getDecision(h, DecisionType.PRESCHOOL_DAYCARE)) {
            assertEquals(DecisionStatus.REJECTED, status)
        }

        val placementPlan = getPlacementPlan(h, applicationId)
        assertNull(placementPlan)

        val placements = h.getPlacementsForChild(testChild_6.id)
        assertEquals(0, placements.size)
    }

    @Test
    fun `accept preschool and reject preschool daycare`() = jdbi.handle { h ->
        // given
        workflowForPreschoolDaycareDecisions(h)

        // when
        service.acceptDecision(
            h,
            serviceWorker,
            applicationId,
            getDecision(h, DecisionType.PRESCHOOL).id,
            mainPeriod.start
        )
        service.rejectDecision(h, serviceWorker, applicationId, getDecision(h, DecisionType.PRESCHOOL_DAYCARE).id)

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.ACTIVE, application.status)

        with(getDecision(h, DecisionType.PRESCHOOL)) {
            assertEquals(DecisionStatus.ACCEPTED, status)
        }
        with(getDecision(h, DecisionType.PRESCHOOL_DAYCARE)) {
            assertEquals(DecisionStatus.REJECTED, status)
        }

        val placementPlan = getPlacementPlan(h, applicationId)
        assertNull(placementPlan)

        val placements = h.getPlacementsForChild(testChild_6.id)
        assertEquals(1, placements.size)
        with(placements.first()) {
            assertEquals(mainPeriod.start, startDate)
            assertEquals(mainPeriod.end, endDate)
            assertEquals(PlacementType.PRESCHOOL, type)
        }
    }

    @Test
    fun `enduser can accept and reject own decisions`() = jdbi.handle { h ->
        // given
        workflowForPreschoolDaycareDecisions(h)
        val user = AuthenticatedUser(testAdult_5.id, setOf(UserRole.END_USER))

        // when
        service.acceptDecision(
            h,
            user,
            applicationId,
            getDecision(h, DecisionType.PRESCHOOL).id,
            mainPeriod.start,
            isEnduser = true
        )
        service.rejectDecision(
            h,
            user,
            applicationId,
            getDecision(h, DecisionType.PRESCHOOL_DAYCARE).id,
            isEnduser = true
        )

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.ACTIVE, application.status)
    }

    @Test
    fun `enduser can not accept decision of someone else`() = jdbi.handle { h ->
        // given
        workflowForPreschoolDaycareDecisions(h)
        val user = AuthenticatedUser(testAdult_1.id, setOf(UserRole.END_USER))

        // when
        assertThrows<Forbidden> {
            service.acceptDecision(
                h,
                user,
                applicationId,
                getDecision(h, DecisionType.PRESCHOOL).id,
                mainPeriod.start,
                isEnduser = true
            )
        }
    }

    @Test
    fun `enduser can not reject decision of someone else`() = jdbi.handle { h ->
        // given
        workflowForPreschoolDaycareDecisions(h)
        val user = AuthenticatedUser(testAdult_1.id, setOf(UserRole.END_USER))

        // when
        assertThrows<Forbidden> {
            service.rejectDecision(h, user, applicationId, getDecision(h, DecisionType.PRESCHOOL).id, isEnduser = true)
        }
    }

    @Test
    fun `accept preschool and accept preschool daycare`() = jdbi.handle { h ->
        // given
        workflowForPreschoolDaycareDecisions(h)

        // when
        service.acceptDecision(
            h,
            serviceWorker,
            applicationId,
            getDecision(h, DecisionType.PRESCHOOL).id,
            mainPeriod.start
        )
        service.acceptDecision(
            h,
            serviceWorker,
            applicationId,
            getDecision(h, DecisionType.PRESCHOOL_DAYCARE).id,
            connectedPeriod.start
        )

        // then
        val application = fetchApplicationDetails(h, applicationId)!!
        assertEquals(ApplicationStatus.ACTIVE, application.status)

        with(getDecision(h, DecisionType.PRESCHOOL)) {
            assertEquals(DecisionStatus.ACCEPTED, status)
        }
        with(getDecision(h, DecisionType.PRESCHOOL_DAYCARE)) {
            assertEquals(DecisionStatus.ACCEPTED, status)
        }

        val placementPlan = getPlacementPlan(h, applicationId)
        assertNull(placementPlan)

        val placements = h.getPlacementsForChild(testChild_6.id)
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

    @Test
    fun `accept preschool daycare first throws`() = jdbi.handle { h ->
        // given
        workflowForPreschoolDaycareDecisions(h)

        // when / then
        assertThrows<BadRequest> {
            service.acceptDecision(
                h,
                serviceWorker,
                applicationId,
                getDecision(h, DecisionType.PRESCHOOL_DAYCARE).id,
                mainPeriod.start
            )
        }
    }

    @Test
    fun `accepting already accepted decision throws`() = jdbi.handle { h ->
        // given
        workflowForPreschoolDaycareDecisions(h)
        service.acceptDecision(
            h,
            serviceWorker,
            applicationId,
            getDecision(h, DecisionType.PRESCHOOL).id,
            mainPeriod.start
        )

        // when / then
        assertThrows<BadRequest> {
            service.acceptDecision(
                h,
                serviceWorker,
                applicationId,
                getDecision(h, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
    }

    @Test
    fun `accepting already rejected decision throws`() = jdbi.handle { h ->
        // given
        workflowForPreschoolDaycareDecisions(h)
        service.rejectDecision(h, serviceWorker, applicationId, getDecision(h, DecisionType.PRESCHOOL).id)

        // when / then
        assertThrows<BadRequest> {
            service.acceptDecision(
                h,
                serviceWorker,
                applicationId,
                getDecision(h, DecisionType.PRESCHOOL).id,
                mainPeriod.start
            )
        }
    }

    @Test
    fun `rejecting already accepted decision throws`() = jdbi.handle { h ->
        // given
        workflowForPreschoolDaycareDecisions(h)
        service.acceptDecision(
            h,
            serviceWorker,
            applicationId,
            getDecision(h, DecisionType.PRESCHOOL).id,
            mainPeriod.start
        )

        // when / then
        assertThrows<BadRequest> {
            service.rejectDecision(h, serviceWorker, applicationId, getDecision(h, DecisionType.PRESCHOOL).id)
        }
    }

    @Test
    fun `rejecting already rejected decision throws`() = jdbi.handle { h ->
        // given
        workflowForPreschoolDaycareDecisions(h)
        service.rejectDecision(h, serviceWorker, applicationId, getDecision(h, DecisionType.PRESCHOOL).id)

        // when / then
        assertThrows<BadRequest> {
            service.rejectDecision(h, serviceWorker, applicationId, getDecision(h, DecisionType.PRESCHOOL).id)
        }
    }

    private fun getDecision(h: Handle, type: DecisionType): Decision =
        getDecisionsByApplication(h, applicationId, AclAuthorization.All).first { it.type == type }

    private fun workflowForPreschoolDaycareDecisions(h: Handle) {
        insertDraftApplication(h, appliedType = PlacementType.PRESCHOOL_DAYCARE)
        service.sendApplication(h, serviceWorker, applicationId)
        service.moveToWaitingPlacement(h, serviceWorker, applicationId)
        service.createPlacementPlan(
            h,
            serviceWorker,
            applicationId,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = mainPeriod,
                preschoolDaycarePeriod = connectedPeriod
            )
        )
        service.sendDecisionsWithoutProposal(h, serviceWorker, applicationId)
    }

    private fun insertDraftApplication(
        h: Handle,
        guardian: PersonData.Detailed = testAdult_5,
        child: PersonData.Detailed = testChild_6,
        appliedType: PlacementType = PlacementType.PRESCHOOL_DAYCARE,
        urgent: Boolean = false,
        hasAdditionalInfo: Boolean = false,
        maxFeeAccepted: Boolean = false,
        preferredStartDate: LocalDate? = LocalDate.now().plusMonths(4)
    ): ApplicationDetails {
        insertTestApplication(
            h = h,
            id = applicationId,
            sentDate = null,
            dueDate = null,
            status = ApplicationStatus.CREATED,
            guardianId = guardian.id,
            childId = child.id
        )
        val application = ApplicationDetails(
            id = applicationId,
            type = when (appliedType) {
                PlacementType.PRESCHOOL, PlacementType.PRESCHOOL_DAYCARE, PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE -> ApplicationType.PRESCHOOL
                PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME -> ApplicationType.DAYCARE
                PlacementType.CLUB -> ApplicationType.CLUB
            },
            form = ApplicationForm(
                child = ChildDetails(
                    person = PersonBasics(
                        firstName = child.firstName,
                        lastName = child.lastName,
                        socialSecurityNumber = child.ssn
                    ),
                    dateOfBirth = child.dateOfBirth,
                    address = address,
                    futureAddress = null,
                    nationality = "fi",
                    language = "fi",
                    allergies = if (hasAdditionalInfo) "allergies" else "",
                    diet = if (hasAdditionalInfo) "diet" else "",
                    assistanceNeeded = false,
                    assistanceDescription = ""
                ),
                guardian = Guardian(
                    person = PersonBasics(
                        firstName = guardian.firstName,
                        lastName = guardian.lastName,
                        socialSecurityNumber = guardian.ssn
                    ),
                    address = address,
                    futureAddress = null,
                    phoneNumber = "0501234567",
                    email = "abc@espoo.fi"
                ),
                preferences = Preferences(
                    preferredUnits = listOf(PreferredUnit(testDaycare.id, testDaycare.name)),
                    preferredStartDate = preferredStartDate,
                    serviceNeed = if (appliedType in listOf(
                        PlacementType.PRESCHOOL,
                        PlacementType.PREPARATORY
                    )
                    ) null else ServiceNeed(
                        startTime = "09:00",
                        endTime = "15:00",
                        shiftCare = false,
                        partTime = appliedType == PlacementType.DAYCARE_PART_TIME
                    ),
                    siblingBasis = null,
                    preparatory = appliedType in listOf(PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE),
                    urgent = urgent
                ),
                secondGuardian = null,
                otherPartner = null,
                otherChildren = emptyList(),
                otherInfo = if (hasAdditionalInfo) "foobar" else "",
                maxFeeAccepted = maxFeeAccepted,
                clubDetails = null
            ),
            status = ApplicationStatus.CREATED,
            origin = ApplicationOrigin.PAPER,
            childId = child.id,
            childRestricted = false,
            guardianId = guardian.id,
            guardianRestricted = false,
            createdDate = OffsetDateTime.now(),
            modifiedDate = OffsetDateTime.now(),
            sentDate = null,
            dueDate = null,
            transferApplication = false,
            additionalDaycareApplication = false,
            otherGuardianId = null,
            checkedByAdmin = false,
            hideFromGuardian = false
        )
        insertTestApplicationForm(
            h = h,
            applicationId = applicationId,
            document = DaycareFormV0.fromApplication2(application)
        )
        return application
    }
}
