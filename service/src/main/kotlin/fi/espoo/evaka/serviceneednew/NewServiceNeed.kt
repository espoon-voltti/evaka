// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneednew

import fi.espoo.evaka.placement.PlacementType
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

data class NewServiceNeed(
    val id: UUID,
    val placementId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    @Nested("option")
    val option: ServiceNeedOptionSummary,
    val shiftCare: Boolean,
    @Nested("confirmed")
    val confirmed: ServiceNeedConfirmation,
    val updated: Instant
)

data class NewServiceNeedChildRange(
    val childId: UUID,
    val dateRange: FiniteDateRange
)

data class ServiceNeedOptionSummary(
    val id: UUID,
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
    val id: UUID,
    val name: String,
    val validPlacementType: PlacementType
)

data class ServiceNeedOption(
    val id: UUID,
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
    placementId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: UUID
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

fun createNewServiceNeed(
    tx: Database.Transaction,
    user: AuthenticatedUser,
    placementId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: UUID,
    shiftCare: Boolean,
    confirmedAt: HelsinkiDateTime
): UUID {
    validateServiceNeed(tx, placementId, startDate, endDate, optionId)
    clearNewServiceNeedsFromPeriod(tx, placementId, FiniteDateRange(startDate, endDate))
    return tx.insertNewServiceNeed(
        placementId = placementId,
        startDate = startDate,
        endDate = endDate,
        optionId = optionId,
        shiftCare = shiftCare,
        confirmedBy = user.id,
        confirmedAt = confirmedAt
    )
}

fun updateNewServiceNeed(
    tx: Database.Transaction,
    user: AuthenticatedUser,
    id: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: UUID,
    shiftCare: Boolean,
    confirmedAt: HelsinkiDateTime
) {
    val old = tx.getNewServiceNeed(id)
    validateServiceNeed(tx, old.placementId, startDate, endDate, optionId)
    if (startDate.isBefore(old.startDate)) {
        clearNewServiceNeedsFromPeriod(tx, old.placementId, FiniteDateRange(startDate, old.startDate.minusDays(1)), excluding = id)
    }
    if (endDate.isAfter(old.endDate)) {
        clearNewServiceNeedsFromPeriod(tx, old.placementId, FiniteDateRange(old.endDate.plusDays(1), endDate), excluding = id)
    }

    tx.updateNewServiceNeed(
        id = id,
        startDate = startDate,
        endDate = endDate,
        optionId = optionId,
        shiftCare = shiftCare,
        confirmedBy = user.id,
        confirmedAt = confirmedAt
    )
}

fun clearNewServiceNeedsFromPeriod(tx: Database.Transaction, placementId: UUID, periodToClear: FiniteDateRange, excluding: UUID? = null) {
    tx.getOverlappingServiceNeeds(placementId, periodToClear.start, periodToClear.end, excluding).forEach { old ->
        val oldPeriod = FiniteDateRange(old.startDate, old.endDate)
        when {
            periodToClear.contains(oldPeriod) -> {
                tx.deleteNewServiceNeed(old.id)
            }
            periodToClear.includes(oldPeriod.start) -> {
                tx.updateNewServiceNeed(
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
                tx.updateNewServiceNeed(
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
                tx.updateNewServiceNeed(
                    id = old.id,
                    startDate = old.startDate,
                    endDate = periodToClear.start.minusDays(1),
                    optionId = old.option.id,
                    shiftCare = old.shiftCare,
                    confirmedBy = old.confirmed.employeeId,
                    confirmedAt = old.confirmed.at
                )
                tx.insertNewServiceNeed(
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

fun notifyServiceNeedUpdated(tx: Database.Transaction, asyncJobRunner: AsyncJobRunner, childRange: NewServiceNeedChildRange) {
    asyncJobRunner.plan(
        tx,
        listOf(GenerateFinanceDecisions.forChild(childRange.childId, childRange.dateRange.asDateRange()))
    )
}
