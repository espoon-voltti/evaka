// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

val MAX_NUMBER_OF_DAYS = 14

@RestController
class PresenceReportController {
    @GetMapping("/reports/presences")
    fun getPresenceReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): ResponseEntity<List<PresenceReportRow>> {
        Audit.PresenceReportRead.log()
        user.requireOneOfRoles(Roles.DIRECTOR, Roles.ADMIN)
        if (to.isBefore(from)) throw BadRequest("Inverted time range")
        if (to.isAfter(from.plusDays(MAX_NUMBER_OF_DAYS.toLong()))) throw BadRequest("Period is too long. Use maximum of $MAX_NUMBER_OF_DAYS days")

        return db.read { it.getPresenceRows(from, to) }.let(::ok)
    }
}

private fun Database.Read.getPresenceRows(from: LocalDate, to: LocalDate): List<PresenceReportRow> {
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
            dg.name,
            NOT EXISTS (SELECT 1 FROM absence a WHERE t = a.date AND a.child_id = p.id AND a.absence_type <> 'PRESENCE') AS present
        FROM days
        LEFT JOIN daycare_group_placement dgp ON daterange(dgp.start_date, dgp.end_date, '[]') @> t::date
        LEFT JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
        LEFT JOIN daycare ON dg.daycare_id = daycare.id
        LEFT JOIN placement pl ON dgp.daycare_placement_id = pl.id AND pl.type != 'CLUB'::placement_type
        LEFT JOIN person p ON pl.child_id = p.id
        LEFT JOIN holiday h ON t = h.date
        WHERE dw = ANY(daycare.operation_days) AND
          h.date IS NULL AND
          (daycare.provider_type = 'MUNICIPAL' OR daycare.id IS NULL);
        """.trimIndent()
    return createQuery(sql)
        .bind("from", from)
        .bind("to", to)
        .map { rs, _ ->
            PresenceReportRow(
                date = rs.getDate("date").toLocalDate(),
                socialSecurityNumber = rs.getString("social_security_number"),
                daycareId = rs.getString("daycare_id")?.let { UUID.fromString(it) },
                daycareGroupName = rs.getString("name"),
                present = rs.getBoolean("present").takeIf { rs.getString("daycare_id") != null }
            )
        }
        .toList()
}

data class PresenceReportRow(
    val date: LocalDate,
    val socialSecurityNumber: String?,
    val daycareId: UUID?,
    val daycareGroupName: String?,
    val present: Boolean?
)
