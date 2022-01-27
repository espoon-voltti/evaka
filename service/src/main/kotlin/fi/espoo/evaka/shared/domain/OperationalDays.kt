// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import org.jdbi.v3.core.kotlin.mapTo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

fun operationalDates(
    dates: Sequence<LocalDate>,
    operationalDays: Set<Int>,
    holidays: Set<LocalDate>
): Sequence<LocalDate> = dates
    .filter { operationalDays.contains(it.dayOfWeek.value) }
    // Units that are operational every day of the week are also operational during holidays
    .filter { operationalDays.size == 7 || !holidays.contains(it) }

data class OperationalDays(
    val fullMonth: List<LocalDate>,
    val generalCase: List<LocalDate>,
    private val specialCases: Map<DaycareId, List<LocalDate>>
) {
    fun forUnit(id: DaycareId): List<LocalDate> = specialCases[id] ?: generalCase
}

fun Database.Read.operationalDays(year: Int, month: Month): OperationalDays {
    val range = FiniteDateRange.ofMonth(year, month)
    val firstDayOfMonth = LocalDate.of(year, month, 1)
    val daysOfMonth = generateSequence(firstDayOfMonth) { it.plusDays(1) }
        .takeWhile { date -> date.month == month }

    // Only includes units that don't have regular monday to friday operational days
    val specialUnitOperationalDays = createQuery("SELECT id, operation_days FROM daycare WHERE NOT (operation_days @> '{1,2,3,4,5}' AND operation_days <@ '{1,2,3,4,5}')")
        .map { row -> row.mapColumn<DaycareId>("id") to row.mapColumn<Array<Int>>("operation_days").map { it }.toSet() }
        .toList()

    val holidays = getHolidays(range)

    val generalCase = daysOfMonth
        .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
        .filterNot { holidays.contains(it) }
        .toList()

    val specialCases = specialUnitOperationalDays.associate { (unitId, operationalDays) ->
        unitId to operationalDates(daysOfMonth, operationalDays, holidays).toList()
    }

    return OperationalDays(daysOfMonth.toList(), generalCase, specialCases)
}

fun Database.Read.getHolidays(range: FiniteDateRange): Set<LocalDate> = createQuery(
    "SELECT date FROM holiday WHERE between_start_and_end(:range, date)"
)
    .bind("range", range)
    .mapTo<LocalDate>()
    .toSet()
