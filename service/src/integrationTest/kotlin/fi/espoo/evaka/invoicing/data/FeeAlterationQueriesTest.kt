// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

class FeeAlterationQueriesTest : PureJdbiTest() {
    @BeforeEach
    fun beforeEach() {
        jdbi.handle(::insertGeneralTestFixtures)
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
    }

    private val personId = UUID.randomUUID()
    private val testFeeAlteration = FeeAlteration(
        id = UUID.randomUUID(),
        personId = personId,
        type = FeeAlteration.Type.DISCOUNT,
        amount = 50,
        isAbsolute = false,
        validFrom = LocalDate.of(2019, 1, 1),
        validTo = LocalDate.of(2019, 1, 31),
        notes = "",
        updatedBy = testDecisionMaker_1.id
    )

    @Test
    fun `insert valid fee alteration`() {
        jdbi.handle { h ->
            upsertFeeAlteration(h, testFeeAlteration)

            val result = h.createQuery("SELECT * FROM fee_alteration")
                .map(toFeeAlteration)
                .list()

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `insert adds updatedAt`() {
        jdbi.handle { h ->
            upsertFeeAlteration(h, testFeeAlteration)

            val result = h.createQuery("SELECT * FROM fee_alteration")
                .map(toFeeAlteration)
                .list()

            with(result[0]) {
                assertNotNull(updatedAt)
            }
        }
    }

    @Test
    fun `insert fee alteration with invalid date range`() {
        jdbi.handle { h ->
            val feeAlteration = testFeeAlteration.copy(
                validFrom = LocalDate.of(2000, 1, 1),
                validTo = LocalDate.of(1900, 1, 1)
            )
            assertThrows<BadRequest> {
                upsertFeeAlteration(h, feeAlteration)
            }
        }
    }

    @Test
    fun `getFeeAlteration with no fee alteration`() {
        jdbi.handle { h ->
            val result = getFeeAlteration(h, UUID.randomUUID())

            assertNull(result)
        }
    }

    @Test
    fun `getFeeAlteration with single fee alteration`() {
        jdbi.handle { h ->
            upsertFeeAlteration(h, testFeeAlteration)

            val result = getFeeAlteration(h, testFeeAlteration.id!!)

            assertNotNull(result)
        }
    }

    @Test
    fun `getFeeAlterationsForPerson with single fee alteration`() {
        jdbi.handle { h ->
            upsertFeeAlteration(h, testFeeAlteration)

            val result = getFeeAlterationsForPerson(h, personId)

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getFeeAlterationsForPerson with multiple fee alterations`() {
        jdbi.handle { h ->
            upsertFeeAlteration(h, testFeeAlteration)
            with(testFeeAlteration) {
                upsertFeeAlteration(
                    h,
                    this.copy(
                        id = UUID.randomUUID(),
                        validFrom = validFrom.plusYears(1),
                        validTo = validTo!!.plusYears(1)
                    )
                )
            }
            with(testFeeAlteration) {
                upsertFeeAlteration(
                    h,
                    this.copy(
                        id = UUID.randomUUID(),
                        validFrom = validFrom.plusYears(2),
                        validTo = validTo!!.plusYears(2)
                    )
                )
            }

            val result = getFeeAlterationsForPerson(h, personId)

            assertEquals(3, result.size)
        }
    }

    @Test
    fun `update valid fee alteration`() {
        jdbi.handle { h ->
            upsertFeeAlteration(h, testFeeAlteration)

            val updated = testFeeAlteration.copy(amount = 500)
            upsertFeeAlteration(h, updated)

            val result = getFeeAlterationsForPerson(h, personId)

            assertEquals(1, result.size)
            assertEquals(500, result.first().amount)
        }
    }

    @Test
    fun `update with invalid date range`() {
        jdbi.handle { h ->
            upsertFeeAlteration(h, testFeeAlteration)

            val updated = with(testFeeAlteration) { this.copy(validTo = validFrom.minusDays(1)) }

            assertThrows<BadRequest> { upsertFeeAlteration(h, updated) }
        }
    }

    @Test
    fun `update with multiple fee alterations only updates one of them`() {
        jdbi.handle { h ->
            val secondFeeAlteration = with(testFeeAlteration) {
                this.copy(id = UUID.randomUUID(), validFrom = validFrom.plusYears(1), validTo = validTo!!.plusYears(1))
            }
            val thirdFeeAlteration = with(testFeeAlteration) {
                this.copy(id = UUID.randomUUID(), validFrom = validFrom.plusYears(2), validTo = validTo!!.plusYears(2))
            }

            upsertFeeAlteration(h, testFeeAlteration)
            upsertFeeAlteration(h, secondFeeAlteration)
            upsertFeeAlteration(h, thirdFeeAlteration)

            val updated = testFeeAlteration.copy(amount = 500)
            upsertFeeAlteration(h, updated)

            val result = getFeeAlterationsForPerson(h, personId)

            assertEquals(thirdFeeAlteration.amount, result[0].amount)
            assertEquals(secondFeeAlteration.amount, result[1].amount)
            assertEquals(updated.amount, result[2].amount)
        }
    }

    @Test
    fun `getFeeAlterationsFrom with from date before both`() {
        jdbi.handle { h ->
            upsertFeeAlteration(h, testFeeAlteration)
            upsertFeeAlteration(
                h,
                testFeeAlteration.copy(
                    id = UUID.randomUUID(),
                    validFrom = testFeeAlteration.validTo!!.plusDays(1),
                    validTo = testFeeAlteration.validTo!!.plusYears(1)
                )
            )

            val result = getFeeAlterationsFrom(
                h,
                listOf(testFeeAlteration.personId),
                testFeeAlteration.validFrom
            )

            assertEquals(2, result.size)
        }
    }

    @Test
    fun `getFeeAlterationsFrom with from after second`() {
        jdbi.handle { h ->
            upsertFeeAlteration(h, testFeeAlteration)
            upsertFeeAlteration(
                h,
                testFeeAlteration.copy(
                    id = UUID.randomUUID(),
                    validFrom = testFeeAlteration.validTo!!.plusDays(1),
                    validTo = testFeeAlteration.validTo!!.plusYears(1)
                )
            )

            val result = getFeeAlterationsFrom(
                h,
                listOf(testFeeAlteration.personId),
                testFeeAlteration.validTo!!.plusDays(1)
            )

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getFeeAlterationsFrom with from after both`() {
        jdbi.handle { h ->
            upsertFeeAlteration(h, testFeeAlteration)
            upsertFeeAlteration(
                h,
                testFeeAlteration.copy(
                    id = UUID.randomUUID(),
                    validFrom = testFeeAlteration.validTo!!.plusDays(1),
                    validTo = testFeeAlteration.validTo!!.plusYears(1)
                )
            )

            val result = getFeeAlterationsFrom(
                h,
                listOf(testFeeAlteration.personId),
                testFeeAlteration.validTo!!.plusYears(1).plusDays(1)
            )

            assertEquals(0, result.size)
        }
    }
}
