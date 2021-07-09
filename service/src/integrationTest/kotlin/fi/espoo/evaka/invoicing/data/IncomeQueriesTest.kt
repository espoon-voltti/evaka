// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.config.defaultObjectMapper
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.testAdult_1
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IncomeQueriesTest : PureJdbiTest() {
    private val mapper = defaultObjectMapper()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }
    }

    @AfterEach
    fun afterEach() {
        db.transaction { tx ->
            tx.resetDatabase()
        }
    }

    private val personId = testAdult_1.id
    private val userId = UUID.randomUUID()
    private val testIncome = Income(
        id = IncomeId(UUID.randomUUID()),
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
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)

            val result = tx.createQuery("SELECT id FROM income")
                .mapTo<UUID>()
                .toList()

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `insert adds updatedAt`() {
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)

            val result = tx.createQuery("SELECT updated_at FROM income")
                .mapTo<Instant>()
                .toList()

            assertNotNull(result.first())
        }
    }

    @Test
    fun `insert income with invalid date range`() {
        db.transaction { tx ->
            val income = testIncome.copy(
                validFrom = LocalDate.of(2000, 1, 1),
                validTo = LocalDate.of(1900, 1, 1)
            )

            assertThrows<BadRequest> { tx.upsertIncome(mapper, income, userId) }
        }
    }

    @Test
    fun `insert income with completely overlapping date range`() {
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)

            val overlappingIncome = testIncome.copy(id = IncomeId(UUID.randomUUID()))

            assertThrows<Conflict> { tx.upsertIncome(mapper, overlappingIncome, userId) }
        }
    }

    @Test
    fun `insert income with overlapping date range by one day`() {
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)

            val overlappingIncome = with(testIncome) {
                this.copy(id = IncomeId(UUID.randomUUID()), validFrom = validTo!!, validTo = validTo!!.plusYears(1))
            }

            assertThrows<Conflict> { tx.upsertIncome(mapper, overlappingIncome, userId) }
        }
    }

    @Test
    fun `getIncome with no income`() {
        db.transaction { tx ->
            val result = tx.getIncome(mapper, IncomeId(UUID.randomUUID()))

            assertNull(result)
        }
    }

    @Test
    fun `getIncome with single income`() {
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)

            val result = tx.getIncome(mapper, testIncome.id!!)

            assertNotNull(result)
        }
    }

    @Test
    fun `getIncomesForPerson with single income`() {
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)

            val result = tx.getIncomesForPerson(mapper, personId)

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getIncomesForPerson with multiple incomes`() {
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)
            with(testIncome) {
                tx.upsertIncome(
                    mapper,
                    this.copy(
                        id = IncomeId(UUID.randomUUID()),
                        validFrom = validFrom.plusYears(1),
                        validTo = validTo!!.plusYears(1)
                    ),
                    userId
                )
            }
            with(testIncome) {
                tx.upsertIncome(
                    mapper,
                    this.copy(
                        id = IncomeId(UUID.randomUUID()),
                        validFrom = validFrom.plusYears(2),
                        validTo = validTo!!.plusYears(2)
                    ),
                    userId
                )
            }

            val result = tx.getIncomesForPerson(mapper, personId)

            assertEquals(3, result.size)
        }
    }

    @Test
    fun `update valid income`() {
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)

            val updated = testIncome.copy(
                data = mapOf(
                    IncomeType.MAIN_INCOME to IncomeValue(
                        1000,
                        IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
                    )
                )
            )
            tx.upsertIncome(mapper, updated, userId)

            val result = tx.getIncomesForPerson(mapper, personId)

            assertEquals(1, result.size)
            assertEquals(1000, result.first().total())
        }
    }

    @Test
    fun `update with invalid date range`() {
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)

            val updated = with(testIncome) { this.copy(validTo = validFrom.minusDays(1)) }

            assertThrows<BadRequest> { tx.upsertIncome(mapper, updated, userId) }
        }
    }

    @Test
    fun `update with overlapping date range`() {
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)

            val anotherIncome = with(testIncome) {
                this.copy(
                    id = IncomeId(UUID.randomUUID()),
                    validFrom = validTo!!.plusDays(1),
                    validTo = validTo!!.plusDays(1).plusMonths(1)
                )
            }
            tx.upsertIncome(mapper, anotherIncome, userId)

            val updated = anotherIncome.copy(validFrom = testIncome.validFrom)

            assertThrows<Conflict> { tx.upsertIncome(mapper, updated, userId) }
        }
    }

    @Test
    fun `update with multiple incomes only updates one of them`() {
        db.transaction { tx ->
            val secondIncome = with(testIncome) {
                this.copy(id = IncomeId(UUID.randomUUID()), validFrom = validFrom.plusYears(1), validTo = validTo!!.plusYears(1))
            }
            val thirdIncome = with(testIncome) {
                this.copy(id = IncomeId(UUID.randomUUID()), validFrom = validFrom.plusYears(2), validTo = validTo!!.plusYears(2))
            }

            tx.upsertIncome(mapper, testIncome, userId)
            tx.upsertIncome(mapper, secondIncome, userId)
            tx.upsertIncome(mapper, thirdIncome, userId)

            val updated = testIncome.copy(
                data = mapOf(
                    IncomeType.MAIN_INCOME to IncomeValue(
                        10000,
                        IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
                    )
                )
            )
            tx.upsertIncome(mapper, updated, userId)

            val result = tx.getIncomesForPerson(mapper, personId)

            assertEquals(thirdIncome.total(), result[0].total())
            assertEquals(secondIncome.total(), result[1].total())
            assertEquals(updated.total(), result[2].total())
        }
    }

    @Test
    fun `getIncomesFrom with from before both`() {
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)
            tx.upsertIncome(
                mapper,
                testIncome.copy(
                    id = IncomeId(UUID.randomUUID()),
                    validFrom = testIncome.validTo!!.plusDays(1),
                    validTo = testIncome.validTo!!.plusYears(1)
                ),
                userId
            )

            val result = tx.getIncomesFrom(
                mapper,
                listOf(testIncome.personId),
                testIncome.validFrom
            )

            assertEquals(2, result.size)
        }
    }

    @Test
    fun `getIncomesFrom with from before second`() {
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)
            tx.upsertIncome(
                mapper,
                testIncome.copy(
                    id = IncomeId(UUID.randomUUID()),
                    validFrom = testIncome.validTo!!.plusDays(1),
                    validTo = testIncome.validTo!!.plusYears(1)
                ),
                userId
            )

            val result = tx.getIncomesFrom(
                mapper,
                listOf(testIncome.personId),
                testIncome.validTo!!.plusDays(1)
            )

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getIncomesFrom with from after both`() {
        db.transaction { tx ->
            tx.upsertIncome(mapper, testIncome, userId)
            tx.upsertIncome(
                mapper,
                testIncome.copy(
                    id = IncomeId(UUID.randomUUID()),
                    validFrom = testIncome.validTo!!.plusDays(1),
                    validTo = testIncome.validTo!!.plusYears(1)
                ),
                userId
            )

            val result = tx.getIncomesFrom(
                mapper,
                listOf(testIncome.personId),
                testIncome.validTo!!.plusYears(1).plusDays(1)
            )

            assertEquals(0, result.size)
        }
    }
}
