// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.espoo.invoicing.EspooIncomeCoefficientMultiplierProvider
import fi.espoo.evaka.invoicing.calculateIncomeTotal
import fi.espoo.evaka.invoicing.calculateMonthlyAmount
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeRequest
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.service.EspooIncomeTypesProvider
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_1
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IncomeQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val mapper = defaultJsonMapperBuilder().build()
    private val incomeTypesProvider = EspooIncomeTypesProvider()
    private val coefficientMultiplierProvider = EspooIncomeCoefficientMultiplierProvider()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(testAdult_1, DevPersonType.ADULT) }
    }

    private val personId = testAdult_1.id
    private val user = AuthenticatedUser.SystemInternalUser
    private val testIncomeData =
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
        )
    private val testIncome =
        IncomeRequest(
            personId = personId,
            effect = IncomeEffect.INCOME,
            data = testIncomeData,
            isEntrepreneur = true,
            worksAtECHA = true,
            validFrom = LocalDate.of(2019, 1, 1),
            validTo = LocalDate.of(2019, 1, 31),
            notes = "",
            forceNewDecision = false,
        )
    private val clock = MockEvakaClock(2023, 1, 7, 14, 0)

    @Test
    fun `insert valid income`() {
        db.transaction { tx ->
            tx.insertIncome(clock.now(), testIncome, user.evakaUserId)

            val result = tx.createQuery { sql("SELECT id FROM income") }.toList<UUID>()

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `insert adds updatedAt`() {
        db.transaction { tx ->
            tx.insertIncome(clock.now(), testIncome, user.evakaUserId)

            val result = tx.createQuery { sql("SELECT updated_at FROM income") }.toList<Instant>()

            assertNotNull(result.first())
        }
    }

    @Test
    fun `insert income with invalid date range`() {
        db.transaction { tx ->
            val income =
                testIncome.copy(
                    validFrom = LocalDate.of(2000, 1, 1),
                    validTo = LocalDate.of(1900, 1, 1),
                )

            assertThrows<BadRequest> { tx.insertIncome(clock.now(), income, user.evakaUserId) }
        }
    }

    @Test
    fun `insert income with completely overlapping date range`() {
        db.transaction { tx ->
            tx.insertIncome(clock.now(), testIncome, user.evakaUserId)

            assertThrows<Conflict> { tx.insertIncome(clock.now(), testIncome, user.evakaUserId) }
        }
    }

    @Test
    fun `insert income with overlapping date range by one day`() {
        db.transaction { tx ->
            tx.insertIncome(clock.now(), testIncome, user.evakaUserId)

            val overlappingIncome =
                with(testIncome) {
                    this.copy(validFrom = validTo!!, validTo = validTo!!.plusYears(1))
                }

            assertThrows<Conflict> {
                tx.insertIncome(clock.now(), overlappingIncome, user.evakaUserId)
            }
        }
    }

    @Test
    fun `getIncome with no income`() {
        db.transaction { tx ->
            val result =
                tx.getIncome(
                    incomeTypesProvider,
                    coefficientMultiplierProvider,
                    IncomeId(UUID.randomUUID()),
                )

            assertNull(result)
        }
    }

    @Test
    fun `getIncome with single income`() {
        db.transaction { tx ->
            val id = tx.insertIncome(clock.now(), testIncome, user.evakaUserId)

            val result = tx.getIncome(incomeTypesProvider, coefficientMultiplierProvider, id)

            assertNotNull(result)
        }
    }

    @Test
    fun `getIncomesForPerson with single income`() {
        db.transaction { tx ->
            tx.insertIncome(clock.now(), testIncome, user.evakaUserId)

            val result =
                tx.getIncomesForPerson(incomeTypesProvider, coefficientMultiplierProvider, personId)

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getIncomesForPerson with multiple incomes`() {
        db.transaction { tx ->
            tx.insertIncome(clock.now(), testIncome, user.evakaUserId)
            with(testIncome) {
                tx.insertIncome(
                    clock.now(),
                    this.copy(validFrom = validFrom.plusYears(1), validTo = validTo!!.plusYears(1)),
                    user.evakaUserId,
                )
            }
            with(testIncome) {
                tx.insertIncome(
                    clock.now(),
                    this.copy(validFrom = validFrom.plusYears(2), validTo = validTo!!.plusYears(2)),
                    user.evakaUserId,
                )
            }

            val result =
                tx.getIncomesForPerson(incomeTypesProvider, coefficientMultiplierProvider, personId)

            assertEquals(3, result.size)
        }
    }

    @Test
    fun `update valid income`() {
        db.transaction { tx ->
            val incomeId = tx.insertIncome(clock.now(), testIncome, user.evakaUserId)

            val updated =
                testIncome.copy(
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
            tx.updateIncome(clock, incomeId, updated, user.evakaUserId)

            val result =
                tx.getIncomesForPerson(incomeTypesProvider, coefficientMultiplierProvider, personId)

            assertEquals(1, result.size)
            assertEquals(1000, result.first().total)
        }
    }

    @Test
    fun `update with invalid date range`() {
        db.transaction { tx ->
            val incomeId = tx.insertIncome(clock.now(), testIncome, user.evakaUserId)

            val updated = with(testIncome) { this.copy(validTo = validFrom.minusDays(1)) }

            assertThrows<BadRequest> { tx.updateIncome(clock, incomeId, updated, user.evakaUserId) }
        }
    }

    @Test
    fun `update with overlapping date range`() {
        db.transaction { tx ->
            tx.insertIncome(clock.now(), testIncome, user.evakaUserId)

            val anotherIncome =
                with(testIncome) {
                    this.copy(
                        validFrom = validTo!!.plusDays(1),
                        validTo = validTo!!.plusDays(1).plusMonths(1),
                    )
                }
            val incomeId = tx.insertIncome(clock.now(), anotherIncome, user.evakaUserId)

            val updated = anotherIncome.copy(validFrom = testIncome.validFrom)

            assertThrows<Conflict> { tx.updateIncome(clock, incomeId, updated, user.evakaUserId) }
        }
    }

    @Test
    fun `update with multiple incomes only updates one of them`() {
        db.transaction { tx ->
            val secondIncome =
                with(testIncome) {
                    this.copy(validFrom = validFrom.plusYears(1), validTo = validTo!!.plusYears(1))
                }
            val thirdIncome =
                with(testIncome) {
                    this.copy(validFrom = validFrom.plusYears(2), validTo = validTo!!.plusYears(2))
                }

            val incomeId = tx.insertIncome(clock.now(), testIncome, user.evakaUserId)
            tx.insertIncome(clock.now(), secondIncome, user.evakaUserId)
            tx.insertIncome(clock.now(), thirdIncome, user.evakaUserId)

            val newData =
                mapOf(
                    "MAIN_INCOME" to
                        IncomeValue(
                            10000,
                            IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS,
                            1,
                            calculateMonthlyAmount(
                                10000,
                                coefficientMultiplierProvider.multiplier(
                                    IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
                                ),
                            ),
                        )
                )
            val updated = testIncome.copy(data = newData)
            tx.updateIncome(clock, incomeId, updated, user.evakaUserId)

            val result =
                tx.getIncomesForPerson(incomeTypesProvider, coefficientMultiplierProvider, personId)

            assertEquals(
                calculateIncomeTotal(thirdIncome.data, coefficientMultiplierProvider),
                result[0].total,
            )
            assertEquals(
                calculateIncomeTotal(secondIncome.data, coefficientMultiplierProvider),
                result[1].total,
            )
            assertEquals(
                calculateIncomeTotal(newData, coefficientMultiplierProvider),
                result[2].total,
            )
        }
    }

    @Test
    fun `getIncomesFrom with from before both`() {
        db.transaction { tx ->
            tx.insertIncome(clock.now(), testIncome, user.evakaUserId)
            tx.insertIncome(
                clock.now(),
                testIncome.copy(
                    validFrom = testIncome.validTo!!.plusDays(1),
                    validTo = testIncome.validTo!!.plusYears(1),
                ),
                user.evakaUserId,
            )

            val result =
                tx.getIncomesFrom(
                    incomeTypesProvider,
                    coefficientMultiplierProvider,
                    listOf(testIncome.personId),
                    testIncome.validFrom,
                )

            assertEquals(2, result.size)
        }
    }

    @Test
    fun `getIncomesFrom with from before second`() {
        db.transaction { tx ->
            tx.insertIncome(clock.now(), testIncome, user.evakaUserId)
            tx.insertIncome(
                clock.now(),
                testIncome.copy(
                    validFrom = testIncome.validTo!!.plusDays(1),
                    validTo = testIncome.validTo!!.plusYears(1),
                ),
                user.evakaUserId,
            )

            val result =
                tx.getIncomesFrom(
                    incomeTypesProvider,
                    coefficientMultiplierProvider,
                    listOf(testIncome.personId),
                    testIncome.validTo!!.plusDays(1),
                )

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getIncomesFrom with from after both`() {
        db.transaction { tx ->
            tx.insertIncome(clock.now(), testIncome, user.evakaUserId)
            tx.insertIncome(
                clock.now(),
                testIncome.copy(
                    validFrom = testIncome.validTo!!.plusDays(1),
                    validTo = testIncome.validTo!!.plusYears(1),
                ),
                user.evakaUserId,
            )

            val result =
                tx.getIncomesFrom(
                    incomeTypesProvider,
                    coefficientMultiplierProvider,
                    listOf(testIncome.personId),
                    testIncome.validTo!!.plusYears(1).plusDays(1),
                )

            assertEquals(0, result.size)
        }
    }
}
