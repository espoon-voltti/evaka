// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.daycare.service.Stats
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import java.time.LocalDate
import java.util.UUID

fun Database.Transaction.initCaretakers(groupId: UUID, startDate: LocalDate, amount: Double) {
    // language=SQL
    val sql = "INSERT INTO daycare_caretaker (group_id, start_date, amount) VALUES (:groupId, :startDate, :amount)"

    createUpdate(sql)
        .bind("groupId", groupId)
        .bind("startDate", startDate)
        .bind("amount", amount)
        .execute()
}

fun Database.Read.getUnitStats(unitId: UUID, startDate: LocalDate, endDate: LocalDate): Stats {
    if (startDate.isBefore(endDate.minusYears(5))) {
        throw BadRequest("Too long time range")
    }

    // language=SQL
    val sql =
        """
        with dailyTotals as (
            select t, sum(ct.amount) as total
            from generate_series(:startDate::date, :endDate::date, '1 day') t
            left outer join daycare_caretaker ct on daterange(ct.start_date, ct.end_date, '[]') @> t::date
            left outer join daycare_group dg on ct.group_id = dg.id
            where dg.daycare_id = :unitId
            group by t
        )
        select
            coalesce(min(total), 0.0) as minimum,
            coalesce(max(total), 0.0) as maximum
        from dailyTotals;
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .mapTo<Stats>()
        .first()
}

fun Database.Read.getGroupStats(unitId: UUID, startDate: LocalDate, endDate: LocalDate): Map<UUID, Stats> {
    // language=SQL
    val sql =
        """
        select
            dg.id as group_id,
            min(coalesce(dc.amount, 0)) as minimum,
            max(coalesce(dc.amount, 0)) as maximum
        from daycare_group dg
        left outer join daycare_caretaker dc on dg.id = dc.group_id
        where dg.daycare_id = :unitId
        and daterange(:startDate, :endDate, '[]') && daterange(dc.start_date, dc.end_date, '[]')
        and daterange(:startDate, :endDate, '[]') && daterange(dg.start_date, dg.end_date, '[]')
        group by dg.id;
        """.trimIndent()

    return createQuery(sql)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("unitId", unitId)
        .mapTo<GroupStats>()
        .associateByTo(mutableMapOf(), GroupStats::groupId, GroupStats::stats)
        .toMap()
}

private data class GroupStats(
    val groupId: UUID,
    @Nested val stats: Stats
)
