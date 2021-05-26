// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.testChild_1
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

    private val personId = testChild_1.id
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
        db.transaction { tx ->
            tx.upsertFeeAlteration(testFeeAlteration)

            val result = tx.createQuery("SELECT * FROM fee_alteration")
                .map(toFeeAlteration)
                .list()

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `insert adds updatedAt`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(testFeeAlteration)

            val result = tx.createQuery("SELECT * FROM fee_alteration")
                .map(toFeeAlteration)
                .list()

            with(result[0]) {
                assertNotNull(updatedAt)
            }
        }
    }

    @Test
    fun `insert fee alteration with invalid date range`() {
        db.transaction { tx ->
            val feeAlteration = testFeeAlteration.copy(
                validFrom = LocalDate.of(2000, 1, 1),
                validTo = LocalDate.of(1900, 1, 1)
            )
            assertThrows<BadRequest> {
                tx.upsertFeeAlteration(feeAlteration)
            }
        }
    }

    @Test
    fun `getFeeAlteration with no fee alteration`() {
        db.transaction { tx ->
            val result = tx.getFeeAlteration(UUID.randomUUID())

            assertNull(result)
        }
    }

    @Test
    fun `getFeeAlteration with single fee alteration`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(testFeeAlteration)

            val result = tx.getFeeAlteration(testFeeAlteration.id!!)

            assertNotNull(result)
        }
    }

    @Test
    fun `getFeeAlterationsForPerson with single fee alteration`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(testFeeAlteration)

            val result = tx.getFeeAlterationsForPerson(personId)

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getFeeAlterationsForPerson with multiple fee alterations`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(testFeeAlteration)
            with(testFeeAlteration) {
                tx.upsertFeeAlteration(
                    this.copy(
                        id = UUID.randomUUID(),
                        validFrom = validFrom.plusYears(1),
                        validTo = validTo!!.plusYears(1)
                    )
                )
            }
            with(testFeeAlteration) {
                tx.upsertFeeAlteration(
                    this.copy(
                        id = UUID.randomUUID(),
                        validFrom = validFrom.plusYears(2),
                        validTo = validTo!!.plusYears(2)
                    )
                )
            }

            val result = tx.getFeeAlterationsForPerson(personId)

            assertEquals(3, result.size)
        }
    }

    @Test
    fun `update valid fee alteration`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(testFeeAlteration)

            val updated = testFeeAlteration.copy(amount = 500)
            tx.upsertFeeAlteration(updated)

            val result = tx.getFeeAlterationsForPerson(personId)

            assertEquals(1, result.size)
            assertEquals(500, result.first().amount)
        }
    }

    @Test
    fun `update with invalid date range`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(testFeeAlteration)

            val updated = with(testFeeAlteration) { this.copy(validTo = validFrom.minusDays(1)) }

            assertThrows<BadRequest> { tx.upsertFeeAlteration(updated) }
        }
    }

    @Test
    fun `update with multiple fee alterations only updates one of them`() {
        db.transaction { tx ->
            val secondFeeAlteration = with(testFeeAlteration) {
                this.copy(id = UUID.randomUUID(), validFrom = validFrom.plusYears(1), validTo = validTo!!.plusYears(1))
            }
            val thirdFeeAlteration = with(testFeeAlteration) {
                this.copy(id = UUID.randomUUID(), validFrom = validFrom.plusYears(2), validTo = validTo!!.plusYears(2))
            }

            tx.upsertFeeAlteration(testFeeAlteration)
            tx.upsertFeeAlteration(secondFeeAlteration)
            tx.upsertFeeAlteration(thirdFeeAlteration)

            val updated = testFeeAlteration.copy(amount = 500)
            tx.upsertFeeAlteration(updated)

            val result = tx.getFeeAlterationsForPerson(personId)

            assertEquals(thirdFeeAlteration.amount, result[0].amount)
            assertEquals(secondFeeAlteration.amount, result[1].amount)
            assertEquals(updated.amount, result[2].amount)
        }
    }

    @Test
    fun `getFeeAlterationsFrom with from date before both`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(testFeeAlteration)
            tx.upsertFeeAlteration(
                testFeeAlteration.copy(
                    id = UUID.randomUUID(),
                    validFrom = testFeeAlteration.validTo!!.plusDays(1),
                    validTo = testFeeAlteration.validTo!!.plusYears(1)
                )
            )

            val result = tx.getFeeAlterationsFrom(
                listOf(testFeeAlteration.personId),
                testFeeAlteration.validFrom
            )

            assertEquals(2, result.size)
        }
    }

    @Test
    fun `getFeeAlterationsFrom with from after second`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(testFeeAlteration)
            tx.upsertFeeAlteration(
                testFeeAlteration.copy(
                    id = UUID.randomUUID(),
                    validFrom = testFeeAlteration.validTo!!.plusDays(1),
                    validTo = testFeeAlteration.validTo!!.plusYears(1)
                )
            )

            val result = tx.getFeeAlterationsFrom(
                listOf(testFeeAlteration.personId),
                testFeeAlteration.validTo!!.plusDays(1)
            )

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getFeeAlterationsFrom with from after both`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(testFeeAlteration)
            tx.upsertFeeAlteration(
                testFeeAlteration.copy(
                    id = UUID.randomUUID(),
                    validFrom = testFeeAlteration.validTo!!.plusDays(1),
                    validTo = testFeeAlteration.validTo!!.plusYears(1)
                )
            )

            val result = tx.getFeeAlterationsFrom(
                listOf(testFeeAlteration.personId),
                testFeeAlteration.validTo!!.plusYears(1).plusDays(1)
            )

            assertEquals(0, result.size)
        }
    }
}
