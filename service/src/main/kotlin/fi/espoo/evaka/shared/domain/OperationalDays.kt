// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.data.DateMap
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

// TODO: Remove
data class OperationalDaysDeprecated(
    val fullMonth: List<LocalDate>,
    val generalCase: List<LocalDate>,
    private val specialCases: Map<DaycareId, List<LocalDate>>
) {
    fun forUnit(id: DaycareId): List<LocalDate> = specialCases[id] ?: generalCase
}

// TODO: Remove
fun Database.Read.operationalDays(year: Int, month: Month): OperationalDaysDeprecated {
    val range = FiniteDateRange.ofMonth(year, month)
    return operationalDays(range)
}

// TODO: Remove
private fun LocalDate.isOperationalDate(operationalDays: Set<DayOfWeek>, holidays: Set<LocalDate>) =
    operationalDays.contains(dayOfWeek) &&
        // Units that are operational every day of the week are also operational during holidays
        (operationalDays.size == 7 || !holidays.contains(this))

// TODO: Remove
private fun Database.Read.operationalDays(range: FiniteDateRange): OperationalDaysDeprecated {
    val rangeDates = range.dates()

    // Only includes units that don't have regular monday to friday operational days
    val specialUnitOperationalDays =
        createQuery {
                sql(
                    "SELECT id, operation_days FROM daycare WHERE NOT (operation_days @> '{1,2,3,4,5}' AND operation_days <@ '{1,2,3,4,5}')"
                )
            }
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

    return OperationalDaysDeprecated(rangeDates.toList(), generalCase, specialCases)
}

fun Database.Read.getHolidays(range: FiniteDateRange): Set<LocalDate> =
    createQuery {
            sql("SELECT date FROM holiday WHERE between_start_and_end(${bind(range)}, date)")
        }
        .toSet<LocalDate>()

fun Database.Read.getOperationalDatesForChildren(
    range: FiniteDateRange,
    children: Set<ChildId>
): Map<ChildId, Set<LocalDate>> {
    data class PlacementRange(
        val range: FiniteDateRange,
        val childId: ChildId,
        val unitId: DaycareId
    )
    val placements: Map<ChildId, DateMap<DaycareId>> =
        createQuery {
                sql(
                    """
        SELECT daterange(pl.start_date, pl.end_date, '[]') as range, pl.child_id, pl.unit_id
        FROM placement pl
        WHERE pl.child_id = ANY(${bind(children)}) AND daterange(pl.start_date, pl.end_date, '[]') && ${bind(range)}
    """
                )
            }
            .toList<PlacementRange>()
            .groupBy { it.childId }
            .mapValues { entry -> DateMap.of(entry.value.map { it.range to it.unitId }) }

    val daycareIds =
        placements.values.flatMap { dateMap -> dateMap.entries().map { it.second } }.toSet()

    data class DaycareOperationDays(
        val unitId: DaycareId,
        val operationDays: Set<Int>,
        val shiftCareOperationDays: Set<Int>?,
        val shiftCareOpenOnHolidays: Boolean
    )
    val operationDaysByDaycareId: Map<DaycareId, DaycareOperationDays> =
        createQuery {
                sql(
                    """
        SELECT id AS unit_id, operation_days, shift_care_operation_days, shift_care_open_on_holidays
        FROM daycare 
        WHERE id = ANY(${bind(daycareIds)})
    """
                )
            }
            .toList<DaycareOperationDays>()
            .associateBy { it.unitId }

    val holidays = getHolidays(range)

    data class ShiftCareRange(val childId: ChildId, val range: FiniteDateRange)
    val shiftCareRanges: Map<ChildId, DateSet> =
        createQuery {
                sql(
                    """
        SELECT pl.child_id, daterange(sn.start_date, sn.end_date, '[]') AS range
        FROM placement pl
        JOIN service_need sn ON sn.placement_id = pl.id AND sn.shift_care = ANY('{FULL,INTERMITTENT}'::shift_care_type[])
        WHERE pl.child_id = ANY(${bind(children)}) AND daterange(pl.start_date, pl.end_date, '[]') && ${bind(range)}
    """
                )
            }
            .toList<ShiftCareRange>()
            .groupBy { it.childId }
            .mapValues { entry -> DateSet.of(entry.value.map { it.range }) }

    return children.associate { childId ->
        val operationalDays =
            range.dates().filter { date ->
                val unitId = placements[childId]?.getValue(date) ?: return@filter false
                val hasShiftCare = shiftCareRanges[childId]?.includes(date) ?: false
                val daycareOperationDays =
                    operationDaysByDaycareId[unitId]?.let {
                        it.shiftCareOperationDays?.takeIf { hasShiftCare } ?: it.operationDays
                    } ?: return@filter false
                if (!daycareOperationDays.contains(date.dayOfWeek.value)) {
                    return@filter false
                }
                if (holidays.contains(date)) {
                    return@filter hasShiftCare &&
                        (operationDaysByDaycareId[unitId]?.shiftCareOpenOnHolidays ?: false)
                }
                true
            }

        childId to operationalDays.toSet()
    }
}

fun Database.Read.getOperationalDatesForChild(
    range: FiniteDateRange,
    childId: ChildId
): Set<LocalDate> = getOperationalDatesForChildren(range, setOf(childId))[childId] ?: emptySet()
