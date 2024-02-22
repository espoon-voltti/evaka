// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.roundToEuros
import fi.espoo.evaka.shared.FeeThresholdsId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.psqlCause
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.math.BigDecimal
import java.util.UUID
import org.jdbi.v3.core.JdbiException
import org.jdbi.v3.core.mapper.Nested
import org.postgresql.util.PSQLState
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/finance-basics")
class FinanceBasicsController(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    @GetMapping("/fee-thresholds")
    fun getFeeThresholds(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<FeeThresholdsWithId> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_FEE_THRESHOLDS
                    )
                    tx.getFeeThresholds().sortedByDescending { it.thresholds.validDuring.start }
                }
            }
            .also { Audit.FinanceBasicsFeeThresholdsRead.log(meta = mapOf("count" to it.size)) }
    }

    @PostMapping("/fee-thresholds")
    fun createFeeThresholds(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: FeeThresholds
    ) {
        validateFeeThresholds(body)
        val id =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.CREATE_FEE_THRESHOLDS
                    )

                    val latest =
                        tx.getFeeThresholds().maxByOrNull { it.thresholds.validDuring.start }

                    if (latest != null) {
                        if (
                            latest.thresholds.validDuring.end != null &&
                                latest.thresholds.validDuring.overlaps(body.validDuring)
                        ) {
                            throwDateOverlapEx()
                        }

                        if (latest.thresholds.validDuring.end == null) {
                            tx.updateFeeThresholdsValidity(
                                latest.id,
                                latest.thresholds.validDuring.copy(
                                    end = body.validDuring.start.minusDays(1)
                                )
                            )
                        }
                    }

                    val id = mapConstraintExceptions { tx.insertNewFeeThresholds(body) }
                    asyncJobRunner.plan(
                        tx,
                        listOf(AsyncJob.NotifyFeeThresholdsUpdated(body.validDuring)),
                        runAt = clock.now()
                    )
                    id
                }
            }
        Audit.FinanceBasicsFeeThresholdsCreate.log(targetId = id)
    }

    @PutMapping("/fee-thresholds/{id}")
    fun updateFeeThresholds(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: FeeThresholdsId,
        @RequestBody thresholds: FeeThresholds
    ) {
        validateFeeThresholds(thresholds)
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.FeeThresholds.UPDATE, id)

                mapConstraintExceptions { tx.updateFeeThresholds(id, thresholds) }
                asyncJobRunner.plan(
                    tx,
                    listOf(AsyncJob.NotifyFeeThresholdsUpdated(thresholds.validDuring)),
                    runAt = clock.now()
                )
            }
        }
        Audit.FinanceBasicsFeeThresholdsUpdate.log(targetId = id)
    }
}

data class FeeThresholdsWithId(val id: FeeThresholdsId, @Nested val thresholds: FeeThresholds)

private fun validateFeeThresholds(thresholds: FeeThresholds) {
    val allMaxFeesMatch =
        listOf(
                calculateMaxFeeFromThresholds(
                    thresholds.minIncomeThreshold2,
                    thresholds.maxIncomeThreshold2,
                    thresholds.incomeMultiplier2
                ),
                calculateMaxFeeFromThresholds(
                    thresholds.minIncomeThreshold3,
                    thresholds.maxIncomeThreshold3,
                    thresholds.incomeMultiplier3
                ),
                calculateMaxFeeFromThresholds(
                    thresholds.minIncomeThreshold4,
                    thresholds.maxIncomeThreshold4,
                    thresholds.incomeMultiplier4
                ),
                calculateMaxFeeFromThresholds(
                    thresholds.minIncomeThreshold5,
                    thresholds.maxIncomeThreshold5,
                    thresholds.incomeMultiplier5
                ),
                calculateMaxFeeFromThresholds(
                    thresholds.minIncomeThreshold6,
                    thresholds.maxIncomeThreshold6,
                    thresholds.incomeMultiplier6
                )
            )
            .all { it == thresholds.maxFee }

    if (!allMaxFeesMatch)
        throw BadRequest("Inconsistent max fees from income thresholds", "inconsistent-thresholds")
}

private fun calculateMaxFeeFromThresholds(
    minThreshold: Int,
    maxThreshold: Int,
    multiplier: BigDecimal
): Int {
    return roundToEuros(BigDecimal(maxThreshold - minThreshold) * multiplier).toInt()
}

fun Database.Read.getFeeThresholds(): List<FeeThresholdsWithId> =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT
    id,
    valid_during,
    min_income_threshold_2,
    min_income_threshold_3,
    min_income_threshold_4,
    min_income_threshold_5,
    min_income_threshold_6,
    income_multiplier_2,
    income_multiplier_3,
    income_multiplier_4,
    income_multiplier_5,
    income_multiplier_6,
    max_income_threshold_2,
    max_income_threshold_3,
    max_income_threshold_4,
    max_income_threshold_5,
    max_income_threshold_6,
    income_threshold_increase_6_plus,
    sibling_discount_2,
    sibling_discount_2_plus,
    max_fee,
    min_fee,
    temporary_fee,
    temporary_fee_part_day,
    temporary_fee_sibling,
    temporary_fee_sibling_part_day
FROM fee_thresholds
        """
                .trimIndent()
        )
        .toList<FeeThresholdsWithId>()

fun Database.Transaction.insertNewFeeThresholds(thresholds: FeeThresholds): FeeThresholdsId =
    @Suppress("DEPRECATION")
    createUpdate(
            """
INSERT INTO fee_thresholds (
    id,
    valid_during,
    min_income_threshold_2,
    min_income_threshold_3,
    min_income_threshold_4,
    min_income_threshold_5,
    min_income_threshold_6,
    income_multiplier_2,
    income_multiplier_3,
    income_multiplier_4,
    income_multiplier_5,
    income_multiplier_6,
    max_income_threshold_2,
    max_income_threshold_3,
    max_income_threshold_4,
    max_income_threshold_5,
    max_income_threshold_6,
    income_threshold_increase_6_plus,
    sibling_discount_2,
    sibling_discount_2_plus,
    max_fee,
    min_fee,
    temporary_fee,
    temporary_fee_part_day,
    temporary_fee_sibling,
    temporary_fee_sibling_part_day
) VALUES (
    :id,
    :validDuring,
    :minIncomeThreshold2,
    :minIncomeThreshold3,
    :minIncomeThreshold4,
    :minIncomeThreshold5,
    :minIncomeThreshold6,
    :incomeMultiplier2,
    :incomeMultiplier3,
    :incomeMultiplier4,
    :incomeMultiplier5,
    :incomeMultiplier6,
    :maxIncomeThreshold2,
    :maxIncomeThreshold3,
    :maxIncomeThreshold4,
    :maxIncomeThreshold5,
    :maxIncomeThreshold6,
    :incomeThresholdIncrease6Plus,
    :siblingDiscount2,
    :siblingDiscount2Plus,
    :maxFee,
    :minFee,
    :temporaryFee,
    :temporaryFeePartDay,
    :temporaryFeeSibling,
    :temporaryFeeSiblingPartDay
)
RETURNING id
"""
        )
        .bindKotlin(thresholds)
        .bind("id", UUID.randomUUID())
        .executeAndReturnGeneratedKeys()
        .exactlyOne<FeeThresholdsId>()

fun Database.Transaction.updateFeeThresholdsValidity(id: FeeThresholdsId, newValidity: DateRange) =
    @Suppress("DEPRECATION")
    createUpdate("UPDATE fee_thresholds SET valid_during = :validDuring WHERE id = :id")
        .bind("id", id)
        .bind("validDuring", newValidity)
        .execute()

fun Database.Transaction.updateFeeThresholds(id: FeeThresholdsId, feeThresholds: FeeThresholds) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
UPDATE fee_thresholds
SET
    valid_during = :validDuring,
    min_income_threshold_2 = :minIncomeThreshold2,
    min_income_threshold_3 = :minIncomeThreshold3,
    min_income_threshold_4 = :minIncomeThreshold4,
    min_income_threshold_5 = :minIncomeThreshold5,
    min_income_threshold_6 = :minIncomeThreshold6,
    income_multiplier_2 = :incomeMultiplier2,
    income_multiplier_3 = :incomeMultiplier3,
    income_multiplier_4 = :incomeMultiplier4,
    income_multiplier_5 = :incomeMultiplier5,
    income_multiplier_6 = :incomeMultiplier6,
    max_income_threshold_2 = :maxIncomeThreshold2,
    max_income_threshold_3 = :maxIncomeThreshold3,
    max_income_threshold_4 = :maxIncomeThreshold4,
    max_income_threshold_5 = :maxIncomeThreshold5,
    max_income_threshold_6 = :maxIncomeThreshold6,
    income_threshold_increase_6_plus = :incomeThresholdIncrease6Plus,
    sibling_discount_2 = :siblingDiscount2,
    sibling_discount_2_plus = :siblingDiscount2Plus,
    max_fee = :maxFee,
    min_fee = :minFee,
    temporary_fee = :temporaryFee,
    temporary_fee_part_day = :temporaryFeePartDay,
    temporary_fee_sibling = :temporaryFeeSibling,
    temporary_fee_sibling_part_day = :temporaryFeeSiblingPartDay
WHERE id = :id
"""
        )
        .bindKotlin(feeThresholds)
        .bind("id", id)
        .execute()

fun <T> mapConstraintExceptions(fn: () -> T): T {
    return try {
        fn()
    } catch (e: JdbiException) {
        when (e.psqlCause()?.sqlState) {
            PSQLState.EXCLUSION_VIOLATION.state -> throwDateOverlapEx(cause = e)
            else -> throw e
        }
    }
}

fun throwDateOverlapEx(cause: Throwable? = null): Nothing =
    throw BadRequest("Fee thresholds over lap with existing fee thresholds", "date-overlap", cause)
