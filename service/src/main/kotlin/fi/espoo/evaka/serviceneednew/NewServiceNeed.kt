// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneednew

import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class NewServiceNeed(
    val id: UUID,
    val placementId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    @Nested("option")
    val option: ServiceNeedOptionSummary,
    val shiftCare: Boolean
)

data class ServiceNeedOptionSummary(
    val id: UUID,
    val name: String
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
    val partWeek: Boolean
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
}

fun createNewServiceNeed(
    tx: Database.Transaction,
    placementId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: UUID,
    shiftCare: Boolean
) {
    validateServiceNeed(tx, placementId, startDate, endDate, optionId)
    clearNewServiceNeedsFromPeriod(tx, placementId, FiniteDateRange(startDate, endDate))
    tx.insertNewServiceNeed(placementId, startDate, endDate, optionId, shiftCare)
}

fun updateNewServiceNeed(
    tx: Database.Transaction,
    id: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: UUID,
    shiftCare: Boolean
) {
    val old = tx.getNewServiceNeed(id)
    validateServiceNeed(tx, old.placementId, startDate, endDate, optionId)
    if (startDate.isBefore(old.startDate)) {
        clearNewServiceNeedsFromPeriod(tx, old.placementId, FiniteDateRange(startDate, old.startDate.minusDays(1)))
    }
    if (endDate.isAfter(old.endDate)) {
        clearNewServiceNeedsFromPeriod(tx, old.placementId, FiniteDateRange(old.endDate.plusDays(1), endDate))
    }

    tx.updateNewServiceNeed(id, startDate, endDate, optionId, shiftCare)
}

private fun clearNewServiceNeedsFromPeriod(tx: Database.Transaction, placementId: UUID, period: FiniteDateRange) {
    tx.getOverlappingServiceNeeds(placementId, period.start, period.end).forEach { old ->
        val oldPeriod = FiniteDateRange(old.startDate, old.endDate)
        when {
            period.contains(oldPeriod) -> {
                tx.deleteNewServiceNeed(old.id)
            }
            oldPeriod.contains(period) -> {
                tx.updateNewServiceNeed(
                    id = old.id,
                    startDate = old.startDate,
                    endDate = period.start.minusDays(1),
                    optionId = old.option.id,
                    shiftCare = old.shiftCare
                )
                tx.insertNewServiceNeed(
                    placementId = placementId,
                    startDate = period.end.plusDays(1),
                    endDate = old.endDate,
                    optionId = old.option.id,
                    shiftCare = old.shiftCare
                )
            }
            period.includes(oldPeriod.start) -> {
                tx.updateNewServiceNeed(
                    id = old.id,
                    startDate = period.end.plusDays(1),
                    endDate = old.endDate,
                    optionId = old.option.id,
                    shiftCare = old.shiftCare
                )
            }
            period.includes(oldPeriod.end) -> {
                tx.updateNewServiceNeed(
                    id = old.id,
                    startDate = old.startDate,
                    endDate = period.start.minusDays(1),
                    optionId = old.option.id,
                    shiftCare = old.shiftCare
                )
            }
        }
    }
}
