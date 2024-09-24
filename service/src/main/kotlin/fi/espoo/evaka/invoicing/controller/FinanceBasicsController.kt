// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.roundToEuros
import fi.espoo.evaka.invoicing.service.generator.ServiceNeedOptionVoucherValueRange
import fi.espoo.evaka.invoicing.service.generator.getVoucherValuesByServiceNeedOption
import fi.espoo.evaka.serviceneed.getServiceNeedOptions
import fi.espoo.evaka.shared.FeeThresholdsId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.ServiceNeedOptionVoucherValueId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.psqlCause
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import org.jdbi.v3.core.JdbiException
import org.jdbi.v3.core.mapper.Nested
import org.postgresql.util.PSQLState
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/finance-basics")
class FinanceBasicsController(private val accessControl: AccessControl) {
    @GetMapping("/fee-thresholds")
    fun getFeeThresholds(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<FeeThresholdsWithId> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_FEE_THRESHOLDS,
                    )
                    tx.getFeeThresholds().sortedByDescending { it.thresholds.validDuring.start }
                }
            }
            .also { Audit.FinanceBasicsFeeThresholdsRead.log(meta = mapOf("count" to it.size)) }
    }

    @PostMapping("/fee-thresholds")
    fun createFeeThresholds(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: FeeThresholds,
    ) {
        validateFeeThresholds(body)
        val id =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.CREATE_FEE_THRESHOLDS,
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
                                ),
                            )
                        }
                    }

                    mapConstraintExceptions { tx.insertNewFeeThresholds(body) }
                }
            }
        Audit.FinanceBasicsFeeThresholdsCreate.log(targetId = AuditId(id))
    }

    @PutMapping("/fee-thresholds/{id}")
    fun updateFeeThresholds(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: FeeThresholdsId,
        @RequestBody thresholds: FeeThresholds,
    ) {
        validateFeeThresholds(thresholds)
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.FeeThresholds.UPDATE, id)

                mapConstraintExceptions { tx.updateFeeThresholds(id, thresholds) }
            }
        }
        Audit.FinanceBasicsFeeThresholdsUpdate.log(targetId = AuditId(id))
    }

    @GetMapping("/voucher-values")
    fun getVoucherValues(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): Map<ServiceNeedOptionId, List<ServiceNeedOptionVoucherValueRangeWithId>> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_VOUCHER_VALUES,
                    )
                    tx.getVoucherValuesByServiceNeedOption()
                }
            }
            .also { Audit.FinanceBasicsVoucherValuesRead.log() }
    }

    @PostMapping("/voucher-values")
    fun createVoucherValue(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: ServiceNeedOptionVoucherValueRange,
    ) {
        val id =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.CREATE_VOUCHER_VALUE,
                    )

                    val serviceNeedOption =
                        tx.getServiceNeedOptions()
                            .filter { it.id == body.serviceNeedOptionId }
                            .firstOrNull()
                    if (serviceNeedOption == null)
                        throw BadRequest("Invalid service need option ID")

                    val currentVoucherValues = tx.getVoucherValuesByServiceNeedOption()

                    currentVoucherValues
                        .getOrDefault(body.serviceNeedOptionId, emptyList())
                        .maxByOrNull { it.voucherValues.range.start }
                        ?.let { latest ->
                            if (!body.range.start.isAfter(latest.voucherValues.range.start))
                                throw BadRequest(
                                    "New voucher value range must start after existing ones"
                                )

                            if (
                                latest.voucherValues.range.end != null &&
                                    body.range.start != latest.voucherValues.range.start.plusDays(1)
                            )
                                throw BadRequest(
                                    "New voucher value can't leave a gap in validities"
                                )

                            if (latest.voucherValues.range.overlaps(body.range)) {
                                tx.updateVoucherValueEndDate(
                                    latest.id,
                                    body.range.start.minusDays(1),
                                )
                            }
                        }

                    tx.insertNewVoucherValue(body)
                }
            }
        Audit.FinanceBasicsVoucherValueCreate.log(targetId = AuditId(id))
    }

    @PutMapping("/voucher-values/{id}")
    fun updateVoucherValue(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ServiceNeedOptionVoucherValueId,
        @RequestBody body: ServiceNeedOptionVoucherValueRange,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.UPDATE_VOUCHER_VALUE,
                )

                val values = tx.getServiceNeedVoucherValuesByVoucherValueRangeId(id)

                if (values.isEmpty()) throw NotFound("Voucher value $id not found")
                if (values[0].id != id) throw BadRequest("Can only update the latest voucher value")

                if (
                    values.size > 1 &&
                        values[1].voucherValues.range.end != null &&
                        body.range.start < values[1].voucherValues.range.end
                )
                    tx.updateVoucherValueEndDate(values[1].id, body.range.start.minusDays(1))

                tx.updateVouchervalue(id, body)

                if (
                    values.size > 1 &&
                        values[1].voucherValues.range.end != null &&
                        body.range.start > values[1].voucherValues.range.end
                )
                    tx.updateVoucherValueEndDate(values[1].id, body.range.start.minusDays(1))
            }
        }

        Audit.FinanceBasicsVoucherValueUpdate.log(targetId = AuditId(id))
    }

    @DeleteMapping("/voucher-values/{id}")
    fun deleteVoucherValue(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ServiceNeedOptionVoucherValueId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.DELETE_VOUCHER_VALUE,
                )

                val values = tx.getServiceNeedVoucherValuesByVoucherValueRangeId(id)

                if (values.isEmpty()) throw NotFound("Voucher value $id not found")
                if (values[0].id != id) throw BadRequest("Can only delete the latest voucher value")

                tx.deleteVoucherValue(id)
                if (values.size > 1) tx.updateVoucherValueEndDate(values[1].id, null)
            }
        }
        Audit.FinanceBasicsVoucherValueDelete.log(targetId = AuditId(id))
    }
}

data class ServiceNeedOptionVoucherValueRangeWithId(
    val id: ServiceNeedOptionVoucherValueId,
    @Nested val voucherValues: ServiceNeedOptionVoucherValueRange,
)

data class FeeThresholdsWithId(val id: FeeThresholdsId, @Nested val thresholds: FeeThresholds)

private fun validateFeeThresholds(thresholds: FeeThresholds) {
    val allMaxFeesMatch =
        listOf(
                calculateMaxFeeFromThresholds(
                    thresholds.minIncomeThreshold2,
                    thresholds.maxIncomeThreshold2,
                    thresholds.incomeMultiplier2,
                ),
                calculateMaxFeeFromThresholds(
                    thresholds.minIncomeThreshold3,
                    thresholds.maxIncomeThreshold3,
                    thresholds.incomeMultiplier3,
                ),
                calculateMaxFeeFromThresholds(
                    thresholds.minIncomeThreshold4,
                    thresholds.maxIncomeThreshold4,
                    thresholds.incomeMultiplier4,
                ),
                calculateMaxFeeFromThresholds(
                    thresholds.minIncomeThreshold5,
                    thresholds.maxIncomeThreshold5,
                    thresholds.incomeMultiplier5,
                ),
                calculateMaxFeeFromThresholds(
                    thresholds.minIncomeThreshold6,
                    thresholds.maxIncomeThreshold6,
                    thresholds.incomeMultiplier6,
                ),
            )
            .all { it == thresholds.maxFee }

    if (!allMaxFeesMatch)
        throw BadRequest("Inconsistent max fees from income thresholds", "inconsistent-thresholds")
}

private fun calculateMaxFeeFromThresholds(
    minThreshold: Int,
    maxThreshold: Int,
    multiplier: BigDecimal,
): Int {
    return roundToEuros(BigDecimal(maxThreshold - minThreshold) * multiplier).toInt()
}

fun Database.Read.getFeeThresholds(): List<FeeThresholdsWithId> =
    createQuery {
            sql(
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
            )
        }
        .toList<FeeThresholdsWithId>()

fun Database.Transaction.insertNewFeeThresholds(thresholds: FeeThresholds): FeeThresholdsId =
    createUpdate {
            sql(
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
    ${bind(UUID.randomUUID())},
    ${bind(thresholds.validDuring)},
    ${bind(thresholds.minIncomeThreshold2)},
    ${bind(thresholds.minIncomeThreshold3)},
    ${bind(thresholds.minIncomeThreshold4)},
    ${bind(thresholds.minIncomeThreshold5)},
    ${bind(thresholds.minIncomeThreshold6)},
    ${bind(thresholds.incomeMultiplier2)},
    ${bind(thresholds.incomeMultiplier3)},
    ${bind(thresholds.incomeMultiplier4)},
    ${bind(thresholds.incomeMultiplier5)},
    ${bind(thresholds.incomeMultiplier6)},
    ${bind(thresholds.maxIncomeThreshold2)},
    ${bind(thresholds.maxIncomeThreshold3)},
    ${bind(thresholds.maxIncomeThreshold4)},
    ${bind(thresholds.maxIncomeThreshold5)},
    ${bind(thresholds.maxIncomeThreshold6)},
    ${bind(thresholds.incomeThresholdIncrease6Plus)},
    ${bind(thresholds.siblingDiscount2)},
    ${bind(thresholds.siblingDiscount2Plus)},
    ${bind(thresholds.maxFee)},
    ${bind(thresholds.minFee)},
    ${bind(thresholds.temporaryFee)},
    ${bind(thresholds.temporaryFeePartDay)},
    ${bind(thresholds.temporaryFeeSibling)},
    ${bind(thresholds.temporaryFeeSiblingPartDay)}
)
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<FeeThresholdsId>()

fun Database.Transaction.updateFeeThresholdsValidity(id: FeeThresholdsId, newValidity: DateRange) =
    createUpdate {
            sql(
                "UPDATE fee_thresholds SET valid_during = ${bind(newValidity)} WHERE id = ${bind(id)}"
            )
        }
        .execute()

fun Database.Transaction.updateFeeThresholds(id: FeeThresholdsId, feeThresholds: FeeThresholds) =
    createUpdate {
            sql(
                """
UPDATE fee_thresholds
SET
    valid_during = ${bind(feeThresholds.validDuring)},
    min_income_threshold_2 = ${bind(feeThresholds.minIncomeThreshold2)},
    min_income_threshold_3 = ${bind(feeThresholds.minIncomeThreshold3)},
    min_income_threshold_4 = ${bind(feeThresholds.minIncomeThreshold4)},
    min_income_threshold_5 = ${bind(feeThresholds.minIncomeThreshold5)},
    min_income_threshold_6 = ${bind(feeThresholds.minIncomeThreshold6)},
    income_multiplier_2 = ${bind(feeThresholds.incomeMultiplier2)},
    income_multiplier_3 = ${bind(feeThresholds.incomeMultiplier3)},
    income_multiplier_4 = ${bind(feeThresholds.incomeMultiplier4)},
    income_multiplier_5 = ${bind(feeThresholds.incomeMultiplier5)},
    income_multiplier_6 = ${bind(feeThresholds.incomeMultiplier6)},
    max_income_threshold_2 = ${bind(feeThresholds.maxIncomeThreshold2)},
    max_income_threshold_3 = ${bind(feeThresholds.maxIncomeThreshold3)},
    max_income_threshold_4 = ${bind(feeThresholds.maxIncomeThreshold4)},
    max_income_threshold_5 = ${bind(feeThresholds.maxIncomeThreshold5)},
    max_income_threshold_6 = ${bind(feeThresholds.maxIncomeThreshold6)},
    income_threshold_increase_6_plus = ${bind(feeThresholds.incomeThresholdIncrease6Plus)},
    sibling_discount_2 = ${bind(feeThresholds.siblingDiscount2)},
    sibling_discount_2_plus = ${bind(feeThresholds.siblingDiscount2Plus)},
    max_fee = ${bind(feeThresholds.maxFee)},
    min_fee = ${bind(feeThresholds.minFee)},
    temporary_fee = ${bind(feeThresholds.temporaryFee)},
    temporary_fee_part_day = ${bind(feeThresholds.temporaryFeePartDay)},
    temporary_fee_sibling = ${bind(feeThresholds.temporaryFeeSibling)},
    temporary_fee_sibling_part_day = ${bind(feeThresholds.temporaryFeeSiblingPartDay)}
WHERE id = ${bind(id)}
"""
            )
        }
        .execute()

fun Database.Read.getServiceNeedVoucherValuesByVoucherValueRangeId(
    voucherValueId: ServiceNeedOptionVoucherValueId
): List<ServiceNeedOptionVoucherValueRangeWithId> =
    createQuery {
            sql(
                """
SELECT
    id,
    service_need_option_id,
    validity as range,
    base_value,
    coefficient,
    value,
    base_value_under_3y,
    coefficient_under_3y,
    value_under_3y
FROM service_need_option_voucher_value
WHERE service_need_option_id = (
  SELECT service_need_option_id
  FROM service_need_option_voucher_value
  WHERE id = ${bind(voucherValueId)}
)
ORDER by upper(validity) DESC
"""
            )
        }
        .toList<ServiceNeedOptionVoucherValueRangeWithId>()

fun Database.Transaction.insertNewVoucherValue(
    voucherValue: ServiceNeedOptionVoucherValueRange
): ServiceNeedOptionVoucherValueId =
    createUpdate {
            sql(
                """
INSERT INTO service_need_option_voucher_value (
    service_need_option_id,
    validity,
    base_value,
    coefficient,
    value,
    base_value_under_3y,
    coefficient_under_3y,
    value_under_3y

) VALUES (
    ${bind(voucherValue.serviceNeedOptionId)},
    ${bind(voucherValue.range)},
    ${bind(voucherValue.baseValue)},
    ${bind(voucherValue.coefficient)},
    ${bind(voucherValue.value)},
    ${bind(voucherValue.baseValueUnder3y)},
    ${bind(voucherValue.coefficientUnder3y)},
    ${bind(voucherValue.valueUnder3y)}
)
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<ServiceNeedOptionVoucherValueId>()

fun Database.Transaction.updateVouchervalue(
    id: ServiceNeedOptionVoucherValueId,
    voucherValue: ServiceNeedOptionVoucherValueRange,
) {
    execute {
        sql(
            """
                UPDATE service_need_option_voucher_value
                SET
                    validity = ${bind(voucherValue.range)},
                    base_value = ${bind(voucherValue.baseValue)},
                    coefficient = ${bind(voucherValue.coefficient)},
                    value = ${bind(voucherValue.value)},
                    base_value_under_3y = ${bind(voucherValue.baseValueUnder3y)},
                    coefficient_under_3y = ${bind(voucherValue.coefficientUnder3y)},
                    value_under_3y = ${bind(voucherValue.valueUnder3y)}
                WHERE id = ${bind(id)}
            """
                .trimIndent()
        )
    }
}

fun Database.Transaction.deleteVoucherValue(id: ServiceNeedOptionVoucherValueId) {
    createUpdate {
            sql(
                """
                DELETE
                FROM service_need_option_voucher_value
                WHERE id = ${bind(id)}
            """
            )
        }
        .execute()
}

fun Database.Transaction.updateVoucherValueEndDate(
    id: ServiceNeedOptionVoucherValueId,
    endDate: LocalDate?,
) {
    createUpdate {
            sql(
                """
                UPDATE service_need_option_voucher_value
                SET validity = daterange(lower(validity), ${bind(endDate)}, '[]')
                WHERE id = ${bind(id)}
            """
            )
        }
        .execute()
}

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
