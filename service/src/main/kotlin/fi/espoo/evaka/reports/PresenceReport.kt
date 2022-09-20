// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val MAX_NUMBER_OF_DAYS = 14

@RestController
class PresenceReportController(private val accessControl: AccessControl) {
    @GetMapping("/reports/presences")
    fun getPresenceReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): List<PresenceReportRow> {
        Audit.PresenceReportRead.log()
        accessControl.requirePermissionFor(user, clock, Action.Global.READ_PRESENCE_REPORT)
        if (to.isBefore(from)) throw BadRequest("Inverted time range")
        if (to.isAfter(from.plusDays(MAX_NUMBER_OF_DAYS.toLong())))
            throw BadRequest("Period is too long. Use maximum of $MAX_NUMBER_OF_DAYS days")

        return db.connect { dbc ->
            dbc.read {
                it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                it.getPresenceRows(from, to)
            }
        }
    }
}

fun Database.Read.getPresenceRows(from: LocalDate, to: LocalDate): List<PresenceReportRow> {
    // language=sql
    val sql =
        """
        WITH days AS
        (
            SELECT t, extract(DOW FROM t) dw
            FROM generate_series(:from, :to, '1 day') t
        )
        SELECT
            t::date AS date,
            daycare.id AS daycare_id,
            p.social_security_number,
            dg.name AS daycareGroupName,
            NOT (a.categories @> absence_categories(pl.type) AND a.categories <@ absence_categories(pl.type)) AS present
        FROM days
        LEFT JOIN daycare_group_placement dgp ON daterange(dgp.start_date, dgp.end_date, '[]') @> t::date
        LEFT JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
        LEFT JOIN daycare ON dg.daycare_id = daycare.id
        LEFT JOIN placement pl ON dgp.daycare_placement_id = pl.id AND pl.type != 'CLUB'::placement_type
        LEFT JOIN person p ON pl.child_id = p.id
        LEFT JOIN LATERAL (SELECT coalesce(array_agg(category), '{}') AS categories FROM absence a WHERE p.id = a.child_id AND a.date = t::date) a ON true
        LEFT JOIN holiday h ON t = h.date
        WHERE dw = ANY(daycare.operation_days) AND
          (h.date IS NULL OR daycare.operation_days @> ARRAY[1, 2, 3, 4, 5, 6, 7]) AND
          (daycare.provider_type = 'MUNICIPAL' OR daycare.id IS NULL);
        """.trimIndent(
        )
    return createQuery(sql).bind("from", from).bind("to", to).mapTo<PresenceReportRow>().toList()
}

data class PresenceReportRow(
    val date: LocalDate,
    val socialSecurityNumber: String?,
    val daycareId: DaycareId?,
    val daycareGroupName: String?,
    val present: Boolean?
)
