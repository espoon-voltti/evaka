// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

fun LocalDate.isOperationalDate(operationalDays: Set<DayOfWeek>, holidays: Set<LocalDate>) =
    operationalDays.contains(dayOfWeek) &&
        // Units that are operational every day of the week are also operational during holidays
        (operationalDays.size == 7 || !holidays.contains(this))

data class OperationalDays(
    val fullMonth: List<LocalDate>,
    val generalCase: List<LocalDate>,
    private val specialCases: Map<DaycareId, List<LocalDate>>
) {
    fun forUnit(id: DaycareId): List<LocalDate> = specialCases[id] ?: generalCase
}

fun Database.Read.operationalDays(year: Int, month: Month): OperationalDays {
    val range = FiniteDateRange.ofMonth(year, month)
    return operationalDays(range)
}

fun Database.Read.operationalDays(range: FiniteDateRange): OperationalDays {
    val rangeDates = range.dates()

    // Only includes units that don't have regular monday to friday operational days
    val specialUnitOperationalDays =
        createQuery(
                "SELECT id, operation_days FROM daycare WHERE NOT (operation_days @> '{1,2,3,4,5}' AND operation_days <@ '{1,2,3,4,5}')"
            )
            .toList {
                column<DaycareId>("id") to
                    column<Set<Int>>("operation_days").map { DayOfWeek.of(it) }.toSet()
            }

    val holidays = getHolidays(range)

    val generalCase =
        rangeDates
            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
            .filterNot { holidays.contains(it) }
            .toList()

    val specialCases =
        specialUnitOperationalDays.associate { (unitId, operationalDays) ->
            unitId to rangeDates.filter { it.isOperationalDate(operationalDays, holidays) }.toList()
        }

    return OperationalDays(rangeDates.toList(), generalCase, specialCases)
}

fun Database.Read.getHolidays(range: FiniteDateRange): Set<LocalDate> =
    createQuery("SELECT date FROM holiday WHERE between_start_and_end(:range, date)")
        .bind("range", range)
        .toSet<LocalDate>()
