// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FeeAlterationQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testChild_1, DevPersonType.CHILD)
        }
    }

    private val personId = testChild_1.id
    private val testFeeAlteration =
        FeeAlteration(
            id = FeeAlterationId(UUID.randomUUID()),
            personId = personId,
            type = FeeAlterationType.DISCOUNT,
            amount = 50,
            isAbsolute = false,
            validFrom = LocalDate.of(2019, 1, 1),
            validTo = LocalDate.of(2019, 1, 31),
            notes = "",
            updatedBy = EvakaUserId(testDecisionMaker_1.id.raw),
        )
    private val clock = RealEvakaClock()

    @Test
    fun `insert valid fee alteration`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, testFeeAlteration)

            val result =
                tx.createQuery { sql("SELECT * FROM fee_alteration") }.toList<FeeAlteration>()

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `insert adds updatedAt`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, testFeeAlteration)

            val result =
                tx.createQuery { sql("SELECT * FROM fee_alteration") }.toList<FeeAlteration>()

            with(result[0]) { assertNotNull(updatedAt) }
        }
    }

    @Test
    fun `insert fee alteration with invalid date range`() {
        db.transaction { tx ->
            val feeAlteration =
                testFeeAlteration.copy(
                    validFrom = LocalDate.of(2000, 1, 1),
                    validTo = LocalDate.of(1900, 1, 1),
                )
            assertThrows<BadRequest> { tx.upsertFeeAlteration(clock, feeAlteration) }
        }
    }

    @Test
    fun `getFeeAlteration with no fee alteration`() {
        db.transaction { tx ->
            val result = tx.getFeeAlteration(FeeAlterationId(UUID.randomUUID()))

            assertNull(result)
        }
    }

    @Test
    fun `getFeeAlteration with single fee alteration`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, testFeeAlteration)

            val result = tx.getFeeAlteration(testFeeAlteration.id!!)

            assertNotNull(result)
        }
    }

    @Test
    fun `getFeeAlterationsForPerson with single fee alteration`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, testFeeAlteration)

            val result = tx.getFeeAlterationsForPerson(personId)

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getFeeAlterationsForPerson with multiple fee alterations`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, testFeeAlteration)
            with(testFeeAlteration) {
                tx.upsertFeeAlteration(
                    clock,
                    this.copy(
                        id = FeeAlterationId(UUID.randomUUID()),
                        validFrom = validFrom.plusYears(1),
                        validTo = validTo!!.plusYears(1),
                    ),
                )
            }
            with(testFeeAlteration) {
                tx.upsertFeeAlteration(
                    clock,
                    this.copy(
                        id = FeeAlterationId(UUID.randomUUID()),
                        validFrom = validFrom.plusYears(2),
                        validTo = validTo!!.plusYears(2),
                    ),
                )
            }

            val result = tx.getFeeAlterationsForPerson(personId)

            assertEquals(3, result.size)
        }
    }

    @Test
    fun `update valid fee alteration`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, testFeeAlteration)

            val updated = testFeeAlteration.copy(amount = 500)
            tx.upsertFeeAlteration(clock, updated)

            val result = tx.getFeeAlterationsForPerson(personId)

            assertEquals(1, result.size)
            assertEquals(500, result.first().amount)
        }
    }

    @Test
    fun `update with invalid date range`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, testFeeAlteration)

            val updated = with(testFeeAlteration) { this.copy(validTo = validFrom.minusDays(1)) }

            assertThrows<BadRequest> { tx.upsertFeeAlteration(clock, updated) }
        }
    }

    @Test
    fun `update with multiple fee alterations only updates one of them`() {
        db.transaction { tx ->
            val secondFeeAlteration =
                with(testFeeAlteration) {
                    this.copy(
                        id = FeeAlterationId(UUID.randomUUID()),
                        validFrom = validFrom.plusYears(1),
                        validTo = validTo!!.plusYears(1),
                    )
                }
            val thirdFeeAlteration =
                with(testFeeAlteration) {
                    this.copy(
                        id = FeeAlterationId(UUID.randomUUID()),
                        validFrom = validFrom.plusYears(2),
                        validTo = validTo!!.plusYears(2),
                    )
                }

            tx.upsertFeeAlteration(clock, testFeeAlteration)
            tx.upsertFeeAlteration(clock, secondFeeAlteration)
            tx.upsertFeeAlteration(clock, thirdFeeAlteration)

            val updated = testFeeAlteration.copy(amount = 500)
            tx.upsertFeeAlteration(clock, updated)

            val result = tx.getFeeAlterationsForPerson(personId)

            assertEquals(thirdFeeAlteration.amount, result[0].amount)
            assertEquals(secondFeeAlteration.amount, result[1].amount)
            assertEquals(updated.amount, result[2].amount)
        }
    }

    @Test
    fun `getFeeAlterationsFrom with from date before both`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, testFeeAlteration)
            tx.upsertFeeAlteration(
                clock,
                testFeeAlteration.copy(
                    id = FeeAlterationId(UUID.randomUUID()),
                    validFrom = testFeeAlteration.validTo!!.plusDays(1),
                    validTo = testFeeAlteration.validTo!!.plusYears(1),
                ),
            )

            val result =
                tx.getFeeAlterationsFrom(
                    listOf(testFeeAlteration.personId),
                    testFeeAlteration.validFrom,
                )

            assertEquals(2, result.size)
        }
    }

    @Test
    fun `getFeeAlterationsFrom with from after second`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, testFeeAlteration)
            tx.upsertFeeAlteration(
                clock,
                testFeeAlteration.copy(
                    id = FeeAlterationId(UUID.randomUUID()),
                    validFrom = testFeeAlteration.validTo!!.plusDays(1),
                    validTo = testFeeAlteration.validTo!!.plusYears(1),
                ),
            )

            val result =
                tx.getFeeAlterationsFrom(
                    listOf(testFeeAlteration.personId),
                    testFeeAlteration.validTo!!.plusDays(1),
                )

            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getFeeAlterationsFrom with from after both`() {
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, testFeeAlteration)
            tx.upsertFeeAlteration(
                clock,
                testFeeAlteration.copy(
                    id = FeeAlterationId(UUID.randomUUID()),
                    validFrom = testFeeAlteration.validTo!!.plusDays(1),
                    validTo = testFeeAlteration.validTo!!.plusYears(1),
                ),
            )

            val result =
                tx.getFeeAlterationsFrom(
                    listOf(testFeeAlteration.personId),
                    testFeeAlteration.validTo!!.plusYears(1).plusDays(1),
                )

            assertEquals(0, result.size)
        }
    }
}
