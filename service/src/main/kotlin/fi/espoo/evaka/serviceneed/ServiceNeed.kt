// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.GenerateFinanceDecisions
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ServiceNeed(
    val id: ServiceNeedId,
    val placementId: PlacementId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    @Nested("option")
    val option: ServiceNeedOptionSummary,
    val shiftCare: Boolean,
    @Nested("confirmed")
    val confirmed: ServiceNeedConfirmation,
    val updated: Instant
)

data class ServiceNeedChildRange(
    val childId: UUID,
    val dateRange: FiniteDateRange
)

data class ServiceNeedOptionSummary(
    val id: ServiceNeedOptionId,
    val name: String,
    val updated: Instant
)

data class ServiceNeedConfirmation(
    val employeeId: UUID,
    val firstName: String,
    val lastName: String,
    val at: HelsinkiDateTime
)

data class ServiceNeedOptionPublicInfo(
    val id: ServiceNeedOptionId,
    val name: String,
    val validPlacementType: PlacementType
)

data class ServiceNeedOption(
    val id: ServiceNeedOptionId,
    val name: String,
    val validPlacementType: PlacementType,
    val defaultOption: Boolean,
    val feeCoefficient: BigDecimal,
    val voucherValueCoefficient: BigDecimal,
    val occupancyCoefficient: BigDecimal,
    val daycareHoursPerWeek: Int,
    val partDay: Boolean,
    val partWeek: Boolean,
    val feeDescriptionFi: String,
    val feeDescriptionSv: String,
    val voucherValueDescriptionFi: String,
    val voucherValueDescriptionSv: String,
    val updated: HelsinkiDateTime = HelsinkiDateTime.now()
)

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
    val sql = """
        SELECT 1
        FROM placement pl
        JOIN service_need_option sno ON sno.valid_placement_type = pl.type
        WHERE pl.id = :placementId AND sno.id = :optionId
    """.trimIndent()
    db.createQuery(sql)
        .bind("placementId", placementId)
        .bind("optionId", optionId)
        .mapTo<Int>()
        .list()
        .let { if (it.isEmpty()) throw BadRequest("Invalid service need type") }

    // language=sql
    val sql2 = """
        SELECT 1
        FROM placement pl
        WHERE pl.id = :placementId AND daterange(pl.start_date, pl.end_date, '[]') @> daterange(:startDate, :endDate, '[]')
    """.trimIndent()
    db.createQuery(sql2)
        .bind("placementId", placementId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .mapTo<Int>()
        .list()
        .let { if (it.isEmpty()) throw BadRequest("Service need must be within placement") }
}

fun createServiceNeed(
    tx: Database.Transaction,
    user: AuthenticatedUser,
    placementId: PlacementId,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: ServiceNeedOptionId,
    shiftCare: Boolean,
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
        confirmedBy = user.id,
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
    shiftCare: Boolean,
    confirmedAt: HelsinkiDateTime
) {
    val old = tx.getServiceNeed(id)
    validateServiceNeed(tx, old.placementId, startDate, endDate, optionId)
    if (startDate.isBefore(old.startDate)) {
        clearServiceNeedsFromPeriod(tx, old.placementId, FiniteDateRange(startDate, old.startDate.minusDays(1)), excluding = id)
    }
    if (endDate.isAfter(old.endDate)) {
        clearServiceNeedsFromPeriod(tx, old.placementId, FiniteDateRange(old.endDate.plusDays(1), endDate), excluding = id)
    }

    tx.updateServiceNeed(
        id = id,
        startDate = startDate,
        endDate = endDate,
        optionId = optionId,
        shiftCare = shiftCare,
        confirmedBy = user.id,
        confirmedAt = confirmedAt
    )
}

fun clearServiceNeedsFromPeriod(tx: Database.Transaction, placementId: PlacementId, periodToClear: FiniteDateRange, excluding: ServiceNeedId? = null) {
    tx.getOverlappingServiceNeeds(placementId, periodToClear.start, periodToClear.end, excluding).forEach { old ->
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
                    confirmedBy = old.confirmed.employeeId,
                    confirmedAt = old.confirmed.at
                )
            }
            periodToClear.includes(oldPeriod.end) -> {
                tx.updateServiceNeed(
                    id = old.id,
                    startDate = old.startDate,
                    endDate = periodToClear.start.minusDays(1),
                    optionId = old.option.id,
                    shiftCare = old.shiftCare,
                    confirmedBy = old.confirmed.employeeId,
                    confirmedAt = old.confirmed.at
                )
            }
            else -> {
                tx.updateServiceNeed(
                    id = old.id,
                    startDate = old.startDate,
                    endDate = periodToClear.start.minusDays(1),
                    optionId = old.option.id,
                    shiftCare = old.shiftCare,
                    confirmedBy = old.confirmed.employeeId,
                    confirmedAt = old.confirmed.at
                )
                tx.insertServiceNeed(
                    placementId = placementId,
                    startDate = periodToClear.end.plusDays(1),
                    endDate = old.endDate,
                    optionId = old.option.id,
                    shiftCare = old.shiftCare,
                    confirmedBy = old.confirmed.employeeId,
                    confirmedAt = old.confirmed.at
                )
            }
        }
    }
}

fun notifyServiceNeedUpdated(tx: Database.Transaction, asyncJobRunner: AsyncJobRunner, childRange: ServiceNeedChildRange) {
    asyncJobRunner.plan(
        tx,
        listOf(GenerateFinanceDecisions.forChild(childRange.childId, childRange.dateRange.asDateRange()))
    )
}
