// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.invoicing.domain.SiblingDiscount
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.math.BigDecimal
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull

@ConstList("shiftCareType")
enum class ShiftCareType : DatabaseEnum {
    NONE,
    INTERMITTENT,
    FULL;

    override val sqlType: String = "shift_care_type"

    companion object {
        fun fromBoolean(value: Boolean): ShiftCareType {
            return when (value) {
                true -> FULL
                false -> NONE
            }
        }
    }
}

data class ServiceNeed(
    val id: ServiceNeedId,
    val placementId: PlacementId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    @Nested("option") val option: ServiceNeedOptionSummary,
    val shiftCare: ShiftCareType,
    @Nested("confirmed")
    @JsonDeserialize(using = ServiceNeedConfirmationDeserializer::class)
    val confirmed: ServiceNeedConfirmation?,
    val updated: HelsinkiDateTime
)

data class ServiceNeedSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    @Nested("option") val option: ServiceNeedOptionPublicInfo?,
    val contractDaysPerMonth: Int?,
    val unitName: String
)

data class ServiceNeedChildRange(val childId: ChildId, val dateRange: FiniteDateRange)

data class ServiceNeedOptionSummary(
    val id: ServiceNeedOptionId,
    val nameFi: String,
    val nameSv: String,
    val nameEn: String,
    val updated: HelsinkiDateTime
)

data class ServiceNeedConfirmation(
    @PropagateNull val userId: EvakaUserId,
    val name: String,
    val at: HelsinkiDateTime?
)

data class ServiceNeedOptionPublicInfo(
    val id: ServiceNeedOptionId,
    val nameFi: String,
    val nameSv: String,
    val nameEn: String,
    val validPlacementType: PlacementType
) {
    companion object {
        fun of(option: ServiceNeedOption) =
            ServiceNeedOptionPublicInfo(
                option.id,
                option.nameFi,
                option.nameSv,
                option.nameEn,
                option.validPlacementType
            )
    }
}

data class ServiceNeedOption(
    val id: ServiceNeedOptionId,
    val nameFi: String,
    val nameSv: String,
    val nameEn: String,
    val validPlacementType: PlacementType,
    val defaultOption: Boolean,
    val feeCoefficient: BigDecimal,
    val occupancyCoefficient: BigDecimal,
    val occupancyCoefficientUnder3y: BigDecimal,
    val realizedOccupancyCoefficient: BigDecimal,
    val realizedOccupancyCoefficientUnder3y: BigDecimal,
    val daycareHoursPerWeek: Int, // Used only for Varda
    val contractDaysPerMonth: Int?,
    val daycareHoursPerMonth: Int?,
    val partDay: Boolean,
    val partWeek: Boolean,
    val feeDescriptionFi: String,
    val feeDescriptionSv: String,
    val voucherValueDescriptionFi: String,
    val voucherValueDescriptionSv: String,
    val active: Boolean,
    val updated: HelsinkiDateTime = HelsinkiDateTime.now()
) {
    fun daycareMinutesPerMonth(): Long? = daycareHoursPerMonth?.let { it * 60L }
}

data class ServiceNeedOptionFee(
    val serviceNeedOptionId: ServiceNeedOptionId,
    val validity: DateRange,
    val baseFee: Int,
    val siblingDiscount2: BigDecimal,
    val siblingFee2: Int,
    val siblingDiscount2Plus: BigDecimal,
    val siblingFee2Plus: Int
) {
    fun siblingDiscount(siblingOrdinal: Int): SiblingDiscount {
        val multiplier =
            when (siblingOrdinal) {
                1 -> BigDecimal(1)
                2 -> BigDecimal(1) - siblingDiscount2
                else -> BigDecimal(1) - siblingDiscount2Plus
            }
        val percent = ((BigDecimal(1) - multiplier) * BigDecimal(100)).toInt()
        val fee =
            when (siblingOrdinal) {
                1 -> null
                2 -> siblingFee2
                else -> siblingFee2Plus
            }
        return SiblingDiscount(multiplier, percent, fee)
    }
}

fun validateServiceNeed(
    db: Database.Read,
    placementId: PlacementId,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: ServiceNeedOptionId
) {
    if (endDate.isBefore(startDate)) {
        throw BadRequest("Start date cannot be before end date.")
    }

    // language=sql
    val sql =
        """
        SELECT 1
        FROM placement pl
        JOIN service_need_option sno ON sno.valid_placement_type = pl.type
        WHERE pl.id = :placementId AND sno.id = :optionId
    """
            .trimIndent()
    @Suppress("DEPRECATION")
    db.createQuery(sql)
        .bind("placementId", placementId)
        .bind("optionId", optionId)
        .toList<Int>()
        .let { if (it.isEmpty()) throw BadRequest("Invalid service need type") }

    // language=sql
    val sql2 =
        """
        SELECT 1
        FROM placement pl
        WHERE pl.id = :placementId AND daterange(pl.start_date, pl.end_date, '[]') @> daterange(:startDate, :endDate, '[]')
    """
            .trimIndent()
    @Suppress("DEPRECATION")
    db.createQuery(sql2)
        .bind("placementId", placementId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .toList<Int>()
        .let { if (it.isEmpty()) throw BadRequest("Service need must be within placement") }
}

fun createServiceNeed(
    tx: Database.Transaction,
    user: AuthenticatedUser,
    placementId: PlacementId,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: ServiceNeedOptionId,
    shiftCare: ShiftCareType,
    confirmedAt: HelsinkiDateTime
): ServiceNeedId {
    validateServiceNeed(tx, placementId, startDate, endDate, optionId)
    clearServiceNeedsFromPeriod(tx, placementId, FiniteDateRange(startDate, endDate))
    return tx.insertServiceNeed(
        placementId = placementId,
        startDate = startDate,
        endDate = endDate,
        optionId = optionId,
        shiftCare = shiftCare,
        confirmedBy = user.evakaUserId,
        confirmedAt = confirmedAt
    )
}

fun updateServiceNeed(
    tx: Database.Transaction,
    user: AuthenticatedUser,
    id: ServiceNeedId,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: ServiceNeedOptionId,
    shiftCare: ShiftCareType,
    confirmedAt: HelsinkiDateTime
) {
    val old = tx.getServiceNeed(id)
    validateServiceNeed(tx, old.placementId, startDate, endDate, optionId)
    if (startDate.isBefore(old.startDate)) {
        clearServiceNeedsFromPeriod(
            tx,
            old.placementId,
            FiniteDateRange(startDate, old.startDate.minusDays(1)),
            excluding = id
        )
    }
    if (endDate.isAfter(old.endDate)) {
        clearServiceNeedsFromPeriod(
            tx,
            old.placementId,
            FiniteDateRange(old.endDate.plusDays(1), endDate),
            excluding = id
        )
    }

    tx.updateServiceNeed(
        id = id,
        startDate = startDate,
        endDate = endDate,
        optionId = optionId,
        shiftCare = shiftCare,
        confirmedBy = user.evakaUserId,
        confirmedAt = confirmedAt
    )
}

fun clearServiceNeedsFromPeriod(
    tx: Database.Transaction,
    placementId: PlacementId,
    periodToClear: FiniteDateRange,
    excluding: ServiceNeedId? = null
) {
    tx.getOverlappingServiceNeeds(placementId, periodToClear.start, periodToClear.end, excluding)
        .forEach { old ->
            val oldPeriod = FiniteDateRange(old.startDate, old.endDate)
            when {
                periodToClear.contains(oldPeriod) -> {
                    tx.deleteServiceNeed(old.id)
                }
                periodToClear.includes(oldPeriod.start) -> {
                    tx.updateServiceNeed(
                        id = old.id,
                        startDate = periodToClear.end.plusDays(1),
                        endDate = old.endDate,
                        optionId = old.option.id,
                        shiftCare = old.shiftCare,
                        confirmedBy = old.confirmed?.userId,
                        confirmedAt = old.confirmed?.at
                    )
                }
                periodToClear.includes(oldPeriod.end) -> {
                    tx.updateServiceNeed(
                        id = old.id,
                        startDate = old.startDate,
                        endDate = periodToClear.start.minusDays(1),
                        optionId = old.option.id,
                        shiftCare = old.shiftCare,
                        confirmedBy = old.confirmed?.userId,
                        confirmedAt = old.confirmed?.at
                    )
                }
                else -> {
                    tx.updateServiceNeed(
                        id = old.id,
                        startDate = old.startDate,
                        endDate = periodToClear.start.minusDays(1),
                        optionId = old.option.id,
                        shiftCare = old.shiftCare,
                        confirmedBy = old.confirmed?.userId,
                        confirmedAt = old.confirmed?.at
                    )
                    tx.insertServiceNeed(
                        placementId = placementId,
                        startDate = periodToClear.end.plusDays(1),
                        endDate = old.endDate,
                        optionId = old.option.id,
                        shiftCare = old.shiftCare,
                        confirmedBy = old.confirmed?.userId,
                        confirmedAt = old.confirmed?.at
                    )
                }
            }
        }
}

fun notifyServiceNeedUpdated(
    tx: Database.Transaction,
    clock: EvakaClock,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    childRange: ServiceNeedChildRange
) {
    asyncJobRunner.plan(
        tx,
        listOf(
            AsyncJob.GenerateFinanceDecisions.forChild(
                childRange.childId,
                childRange.dateRange.asDateRange()
            )
        ),
        runAt = clock.now()
    )
}
