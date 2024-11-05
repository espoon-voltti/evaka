// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.invoicing.controller.IncomeController
import fi.espoo.evaka.invoicing.data.insertIncome
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeRequest
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.service.IncomeCoefficientMultiplierProvider
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.user.EvakaUser
import fi.espoo.evaka.user.EvakaUserType
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class IncomeIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var mapper: JsonMapper

    @Autowired lateinit var incomeController: IncomeController
    @Autowired lateinit var coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider

    val mockClock = MockEvakaClock(2023, 5, 1, 10, 0)
    val mockUser =
        EvakaUser(
            EvakaUserId(UUID.fromString("00000000-0000-0000-0000-000000000000")),
            "",
            EvakaUserType.EMPLOYEE,
        )

    private fun assertEqualEnough(expected: List<IncomeRequest>, actual: List<Income>) {
        val nullId = IncomeId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        val nullTime = mockClock.now()
        assertEquals(
            expected
                .map {
                    Income(
                        id = nullId,
                        personId = it.personId,
                        effect = it.effect,
                        data = it.data,
                        isEntrepreneur = it.isEntrepreneur,
                        worksAtECHA = it.worksAtECHA,
                        validFrom = it.validFrom,
                        validTo = it.validTo,
                        notes = it.notes,
                        modifiedAt = nullTime,
                        modifiedBy = mockUser,
                        attachments = it.attachments,
                        totalIncome = calculateTotalIncome(it.data, coefficientMultiplierProvider),
                        totalExpenses =
                            calculateTotalExpense(it.data, coefficientMultiplierProvider),
                        total = calculateIncomeTotal(it.data, coefficientMultiplierProvider),
                    )
                }
                .toSet(),
            actual
                .map { it.copy(id = nullId, modifiedAt = nullTime, modifiedBy = mockUser) }
                .toSet(),
        )
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testAdult_1, DevPersonType.ADULT)
        }
    }

    fun testIncomeRequest() =
        IncomeRequest(
            personId = testAdult_1.id,
            effect = IncomeEffect.INCOME,
            data =
                mapOf(
                    "MAIN_INCOME" to
                        IncomeValue(
                            500000,
                            IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS,
                            1,
                            calculateMonthlyAmount(
                                500000,
                                coefficientMultiplierProvider.multiplier(
                                    IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
                                ),
                            ),
                        )
                ),
            validFrom = LocalDate.of(2019, 1, 1),
            validTo = LocalDate.of(2019, 1, 31),
            notes = "",
        )

    private val financeUser =
        AuthenticatedUser.Employee(
            id = testDecisionMaker_1.id,
            roles = setOf(UserRole.FINANCE_ADMIN),
        )
    private val financeUserName = "${testDecisionMaker_1.lastName} ${testDecisionMaker_1.firstName}"
    private val clock = MockEvakaClock(2023, 1, 7, 14, 0)

    @Test
    fun `getPersonIncomes works with no data in DB`() {
        val incomes = getPersonIncomes(testAdult_1.id)
        assertEquals(emptyList(), incomes)
    }

    @Test
    fun `getIncome works with multiple incomes in DB`() {
        val incomeRequests =
            listOf(
                testIncomeRequest()
                    .copy(
                        validFrom = testIncomeRequest().validFrom.plusYears(1),
                        validTo = testIncomeRequest().validTo!!.plusYears(1),
                    ),
                testIncomeRequest(),
            )

        db.transaction { tx ->
            incomeRequests.forEach { tx.insertIncome(clock, mapper, it, financeUser.evakaUserId) }
        }

        val incomes = getPersonIncomes(testAdult_1.id)

        assertEqualEnough(incomeRequests, incomes)
    }

    @Test
    fun `createIncome works with valid income`() {
        createIncome(testIncomeRequest())
        val incomes = getPersonIncomes(testAdult_1.id)
        assertEqualEnough(listOf(testIncomeRequest()), incomes)
    }

    @Test
    fun `createIncome throws with invalid date range`() {
        val income = testIncomeRequest().copy(validTo = testIncomeRequest().validFrom.minusDays(1))
        assertThrows<BadRequest> { createIncome(income) }
    }

    @Test
    fun `createIncome splits earlier indefinite income`() {
        val firstIncome = testIncomeRequest().copy(validTo = null)
        val firstIncomeId =
            db.transaction { tx ->
                tx.insertIncome(clock, mapper, firstIncome, financeUser.evakaUserId)
            }

        val secondIncome = firstIncome.copy(validFrom = firstIncome.validFrom.plusMonths(1))
        createIncome(secondIncome)

        val incomes = getPersonIncomes(testIncomeRequest().personId)

        assertEquals(2, incomes.size)

        val firstIncomeResult = incomes.find { it.id == firstIncomeId }!!
        assertEquals(firstIncome.validFrom, firstIncomeResult.validFrom)
        assertEquals(secondIncome.validFrom.minusDays(1), firstIncomeResult.validTo)

        val secondIncomeResult = incomes.find { it.id != firstIncomeId }!!
        assertEquals(secondIncome.validFrom, secondIncomeResult.validFrom)
        assertEquals(secondIncome.validTo, secondIncomeResult.validTo)
    }

    @Test
    fun `createIncome throws with partly overlapping date range`() {
        db.transaction { tx ->
            tx.insertIncome(clock, mapper, testIncomeRequest(), financeUser.evakaUserId)
        }

        val overlappingIncome =
            testIncomeRequest().let {
                it.copy(validFrom = it.validFrom.plusDays(10), validTo = null)
            }
        assertThrows<Conflict> { createIncome(overlappingIncome) }
    }

    @Test
    fun `createIncome throws with identical date range`() {
        db.transaction { tx ->
            tx.insertIncome(clock, mapper, testIncomeRequest(), financeUser.evakaUserId)
        }
        assertThrows<Conflict> { createIncome(testIncomeRequest()) }
    }

    @Test
    fun `createIncome throws with covering date range`() {
        db.transaction { tx ->
            tx.insertIncome(clock, mapper, testIncomeRequest(), financeUser.evakaUserId)
        }

        val overlappingIncome =
            testIncomeRequest().let {
                it.copy(
                    validFrom = it.validTo!!.minusMonths(1),
                    validTo = it.validTo!!.plusYears(1),
                )
            }
        assertThrows<Conflict> { createIncome(overlappingIncome) }
    }

    @Test
    fun `createIncome removes data if effect is not INCOME`() {
        val income = with(testIncomeRequest()) { this.copy(effect = IncomeEffect.MAX_FEE_ACCEPTED) }
        createIncome(income)

        val incomes = getPersonIncomes(testIncomeRequest().personId)
        assertEquals(1, incomes.size)
        with(incomes.first()) {
            assertEquals(financeUserName, modifiedBy.name)
            assertEquals(0, totalIncome)
            assertEquals(0, totalExpenses)
            assertEquals(0, total)
        }
    }

    @Test
    fun `updateIncome works with valid income`() {
        val incomeId =
            db.transaction { tx ->
                tx.insertIncome(clock, mapper, testIncomeRequest(), financeUser.evakaUserId)
            }

        val updateRequest =
            testIncomeRequest()
                .copy(
                    data =
                        mapOf(
                            "MAIN_INCOME" to
                                IncomeValue(
                                    1000,
                                    IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS,
                                    1,
                                    calculateMonthlyAmount(
                                        1000,
                                        coefficientMultiplierProvider.multiplier(
                                            IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
                                        ),
                                    ),
                                )
                        )
                )

        updateIncome(incomeId, updateRequest)

        val incomes = getPersonIncomes(testIncomeRequest().personId)
        assertEqualEnough(listOf(updateRequest), incomes)
        with(incomes.first()) {
            assertEquals(
                calculateTotalIncome(updateRequest.data, coefficientMultiplierProvider),
                totalIncome,
            )
            assertEquals(
                calculateTotalExpense(updateRequest.data, coefficientMultiplierProvider),
                totalExpenses,
            )
            assertEquals(
                calculateIncomeTotal(updateRequest.data, coefficientMultiplierProvider),
                total,
            )
        }
    }

    @Test
    fun `updateIncome throws with invalid date rage`() {
        val incomeId =
            db.transaction { tx ->
                tx.insertIncome(clock, mapper, testIncomeRequest(), financeUser.evakaUserId)
            }

        val updated = testIncomeRequest().copy(validTo = testIncomeRequest().validFrom.minusDays(1))
        assertThrows<BadRequest> { updateIncome(incomeId, updated) }
    }

    @Test
    fun `updateIncome throws with overlapping date rage`() {
        val incomeId =
            db.transaction { tx ->
                tx.insertIncome(clock, mapper, testIncomeRequest(), financeUser.evakaUserId)
            }

        val anotherIncome =
            with(testIncomeRequest()) {
                this.copy(validFrom = validFrom.plusYears(1), validTo = validTo!!.plusYears(1))
            }
        db.transaction { tx ->
            tx.insertIncome(clock, mapper, anotherIncome, financeUser.evakaUserId)
        }

        val updated = testIncomeRequest().copy(validTo = anotherIncome.validTo)
        assertThrows<Conflict> { updateIncome(incomeId, updated) }
    }

    @Test
    fun `updateIncome removes data if effect is not INCOME`() {
        val incomeId =
            db.transaction { tx ->
                tx.insertIncome(clock, mapper, testIncomeRequest(), financeUser.evakaUserId)
            }

        val updated =
            with(testIncomeRequest()) { this.copy(effect = IncomeEffect.MAX_FEE_ACCEPTED) }
        updateIncome(incomeId, updated)

        val incomes = getPersonIncomes(testAdult_1.id)
        assertEquals(1, incomes.size)
        with(incomes.first()) {
            assertEquals(financeUserName, modifiedBy.name)
            assertEquals(0, totalIncome)
            assertEquals(0, totalExpenses)
            assertEquals(0, total)
        }
    }

    @Test
    fun `deleteIncome works with multiple incomes in DB`() {
        val incomeId =
            db.transaction { tx ->
                tx.insertIncome(clock, mapper, testIncomeRequest(), financeUser.evakaUserId)
            }
        val anotherIncome =
            with(testIncomeRequest()) {
                this.copy(validFrom = validFrom.plusYears(1), validTo = validTo!!.plusYears(1))
            }
        val incomeId2 =
            db.transaction { tx ->
                tx.insertIncome(clock, mapper, anotherIncome, financeUser.evakaUserId)
            }

        deleteIncome(incomeId)

        assertEquals(
            listOf(incomeId2),
            getPersonIncomes(testIncomeRequest().personId).map { it.id },
        )
    }

    private fun getPersonIncomes(personId: PersonId) =
        incomeController.getPersonIncomes(dbInstance(), financeUser, mockClock, personId).map {
            it.data
        }

    private fun createIncome(incomeRequest: IncomeRequest) =
        incomeController.createIncome(dbInstance(), financeUser, mockClock, incomeRequest)

    private fun updateIncome(id: IncomeId, incomeRequest: IncomeRequest) =
        incomeController.updateIncome(dbInstance(), financeUser, mockClock, id, incomeRequest)

    private fun deleteIncome(id: IncomeId) =
        incomeController.deleteIncome(dbInstance(), financeUser, mockClock, id)
}
