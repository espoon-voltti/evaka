// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.daycare.service.Caretakers
import fi.espoo.evaka.occupancy.GroupPredicate
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.data.DateMap
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.math.BigDecimal
import java.time.LocalDate

fun Database.Transaction.initCaretakers(groupId: GroupId, startDate: LocalDate, amount: Double) {
    execute {
        sql(
            "INSERT INTO daycare_caretaker (group_id, start_date, end_date, amount) VALUES (${bind(groupId)}, ${bind(startDate)}, NULL, ${bind(amount)})"
        )
    }
}

fun Database.Read.getUnitStats(unitId: DaycareId, range: FiniteDateRange): Caretakers {
    val countsByGroup = getPlannedCaretakersForGroups(GroupPredicate.ByUnit(unitId), range)
    val unitDailyTotals =
        countsByGroup.values.fold(DateMap.empty<BigDecimal>()) { acc, groupDailyCounts ->
            acc.update(groupDailyCounts) { _, old, new -> old + new }
        }
    return Caretakers.fromDailyCounts(unitDailyTotals)
}

fun Database.Read.getGroupStats(
    unitId: DaycareId,
    range: FiniteDateRange,
): Map<GroupId, Caretakers> =
    getPlannedCaretakersForGroups(GroupPredicate.ByUnit(unitId), range).mapValues {
        (_, groupDailyCounts) ->
        Caretakers.fromDailyCounts(groupDailyCounts)
    }

/**
 * Return *date-based* `daycare_caretaker` counts for the given groups and the given date range.
 *
 * The resulting DateMap is minimal and contains data *only* for groups that have entries and *only*
 * for dates within the given date range where the group is active and a caretaker count exists in
 * the database.
 */
fun Database.Read.getPlannedCaretakersForGroups(
    byGroup: GroupPredicate,
    range: FiniteDateRange,
): Map<GroupId, DateMap<BigDecimal>> {
    data class RawCaretakers(
        val groupId: GroupId,
        val range: FiniteDateRange,
        val amount: BigDecimal,
    )
    return createQuery {
            sql(
                """
SELECT
    dg.id AS group_id,
    daterange(dc.start_date, dc.end_date, '[]') * daterange(dg.start_date, dg.end_date, '[]') * ${bind(range)} AS range,
    dc.amount
FROM daycare_group dg
JOIN daycare_caretaker dc ON dg.id = dc.group_id AND daterange(dg.start_date, dg.end_date, '[]') && daterange(dc.start_date, dc.end_date, '[]')
WHERE ${predicate(byGroup.toRawPredicate().forTable("dg"))}
AND daterange(dg.start_date, dg.end_date, '[]') && ${bind(range)}
AND daterange(dc.start_date, dc.end_date, '[]') && ${bind(range)}
"""
            )
        }
        .mapTo<RawCaretakers>()
        .useSequence { rows ->
            rows
                .groupingBy { it.groupId }
                .fold(DateMap.empty()) { countsForGroup, row ->
                    countsForGroup.set(row.range, row.amount)
                }
        }
}
