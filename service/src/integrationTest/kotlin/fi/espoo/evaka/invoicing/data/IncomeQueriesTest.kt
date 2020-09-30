// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.config.defaultObjectMapper
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class IncomeQueriesTest : PureJdbiTest() {
    private val mapper = defaultObjectMapper()

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
    }

    private val personId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val testIncome = Income(
        id = UUID.randomUUID(),
        personId = personId,
        effect = IncomeEffect.INCOME,
        data = mapOf(IncomeType.MAIN_INCOME to IncomeValue(500000, IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS)),
        isEntrepreneur = true,
        worksAtECHA = true,
        validFrom = LocalDate.of(2019, 1, 1),
        validTo = LocalDate.of(2019, 1, 31),
        notes = ""
    )

    @Test
    fun `insert valid income`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)

            val result = h.createQuery("SELECT id FROM income")
                .mapTo<UUID>()
                .toList()

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `insert adds updatedAt`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)

            val result = h.createQuery("SELECT updated_at FROM income")
                .mapTo<Instant>()
                .toList()

            assertNotNull(result.first())
        }
    }

    @Test
    fun `insert income with invalid date range`() {
        jdbi.handle { h ->
            val income = testIncome.copy(
                validFrom = LocalDate.of(2000, 1, 1),
                validTo = LocalDate.of(1900, 1, 1)
            )

            assertThrows<BadRequest> { upsertIncome(h, mapper, income, userId) }
        }
    }

    @Test
    fun `insert income with completely overlapping date range`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)

            val overlappingIncome = testIncome.copy(id = UUID.randomUUID())

            assertThrows<Conflict> { upsertIncome(h, mapper, overlappingIncome, userId) }
        }
    }

    @Test
    fun `insert income with overlapping date range by one day`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)

            val overlappingIncome = with(testIncome) {
                this.copy(id = UUID.randomUUID(), validFrom = validTo!!, validTo = validTo!!.plusYears(1))
            }

            assertThrows<Conflict> { upsertIncome(h, mapper, overlappingIncome, userId) }
        }
    }

    @Test
    fun `getIncome with no income`() {
        jdbi.handle { h ->
            val result = getIncome(h, mapper, UUID.randomUUID())

            assertNull(result)
        }
    }

    @Test
    fun `getIncome with single income`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)

            val result = getIncome(h, mapper, testIncome.id!!)

            assertNotNull(result)
        }
    }

    @Test
    fun `getIncomesForPerson with single income`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)

            val result = getIncomesForPerson(h, mapper, personId)

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getIncomesForPerson with multiple incomes`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)
            with(testIncome) {
                upsertIncome(
                    h,
                    mapper,
                    this.copy(
                        id = UUID.randomUUID(),
                        validFrom = validFrom.plusYears(1),
                        validTo = validTo!!.plusYears(1)
                    ),
                    userId
                )
            }
            with(testIncome) {
                upsertIncome(
                    h,
                    mapper,
                    this.copy(
                        id = UUID.randomUUID(),
                        validFrom = validFrom.plusYears(2),
                        validTo = validTo!!.plusYears(2)
                    ),
                    userId
                )
            }

            val result = getIncomesForPerson(h, mapper, personId)

            assertEquals(3, result.size)
        }
    }

    @Test
    fun `update valid income`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)

            val updated = testIncome.copy(
                data = mapOf(
                    IncomeType.MAIN_INCOME to IncomeValue(
                        1000,
                        IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
                    )
                )
            )
            upsertIncome(h, mapper, updated, userId)

            val result = getIncomesForPerson(h, mapper, personId)

            assertEquals(1, result.size)
            assertEquals(1000, result.first().total())
        }
    }

    @Test
    fun `update with invalid date range`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)

            val updated = with(testIncome) { this.copy(validTo = validFrom.minusDays(1)) }

            assertThrows<BadRequest> { upsertIncome(h, mapper, updated, userId) }
        }
    }

    @Test
    fun `update with overlapping date range`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)

            val anotherIncome = with(testIncome) {
                this.copy(
                    id = UUID.randomUUID(),
                    validFrom = validTo!!.plusDays(1),
                    validTo = validTo!!.plusDays(1).plusMonths(1)
                )
            }
            upsertIncome(h, mapper, anotherIncome, userId)

            val updated = anotherIncome.copy(validFrom = testIncome.validFrom)

            assertThrows<Conflict> { upsertIncome(h, mapper, updated, userId) }
        }
    }

    @Test
    fun `update with multiple incomes only updates one of them`() {
        jdbi.handle { h ->
            val secondIncome = with(testIncome) {
                this.copy(id = UUID.randomUUID(), validFrom = validFrom.plusYears(1), validTo = validTo!!.plusYears(1))
            }
            val thirdIncome = with(testIncome) {
                this.copy(id = UUID.randomUUID(), validFrom = validFrom.plusYears(2), validTo = validTo!!.plusYears(2))
            }

            upsertIncome(h, mapper, testIncome, userId)
            upsertIncome(h, mapper, secondIncome, userId)
            upsertIncome(h, mapper, thirdIncome, userId)

            val updated = testIncome.copy(
                data = mapOf(
                    IncomeType.MAIN_INCOME to IncomeValue(
                        10000,
                        IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
                    )
                )
            )
            upsertIncome(h, mapper, updated, userId)

            val result = getIncomesForPerson(h, mapper, personId)

            assertEquals(thirdIncome.total(), result[0].total())
            assertEquals(secondIncome.total(), result[1].total())
            assertEquals(updated.total(), result[2].total())
        }
    }

    @Test
    fun `getIncomesFrom with from before both`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)
            upsertIncome(
                h, mapper,
                testIncome.copy(
                    id = UUID.randomUUID(),
                    validFrom = testIncome.validTo!!.plusDays(1),
                    validTo = testIncome.validTo!!.plusYears(1)
                ),
                userId
            )

            val result = getIncomesFrom(
                h,
                mapper,
                listOf(testIncome.personId),
                testIncome.validFrom
            )

            assertEquals(2, result.size)
        }
    }

    @Test
    fun `getIncomesFrom with from before second`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)
            upsertIncome(
                h, mapper,
                testIncome.copy(
                    id = UUID.randomUUID(),
                    validFrom = testIncome.validTo!!.plusDays(1),
                    validTo = testIncome.validTo!!.plusYears(1)
                ),
                userId
            )

            val result = getIncomesFrom(
                h,
                mapper,
                listOf(testIncome.personId),
                testIncome.validTo!!.plusDays(1)
            )

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getIncomesFrom with from after both`() {
        jdbi.handle { h ->
            upsertIncome(h, mapper, testIncome, userId)
            upsertIncome(
                h, mapper,
                testIncome.copy(
                    id = UUID.randomUUID(),
                    validFrom = testIncome.validTo!!.plusDays(1),
                    validTo = testIncome.validTo!!.plusYears(1)
                ),
                userId
            )

            val result = getIncomesFrom(
                h,
                mapper,
                listOf(testIncome.personId),
                testIncome.validTo!!.plusYears(1).plusDays(1)
            )

            assertEquals(0, result.size)
        }
    }
}
