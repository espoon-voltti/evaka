// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole.ADMIN
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_ADMIN
import fi.espoo.evaka.shared.auth.UserRole.SERVICE_WORKER
import fi.espoo.evaka.shared.auth.UserRole.UNIT_SUPERVISOR
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class MissingServiceNeedReportController(
    private val jdbi: Jdbi,
    private val acl: AccessControlList
) {
    @GetMapping("/reports/missing-service-need")
    fun getMissingServiceNeedReport(
        user: AuthenticatedUser,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): ResponseEntity<List<MissingServiceNeedReportRow>> {
        Audit.MissingServiceNeedReportRead.log()
        user.requireOneOfRoles(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR)
        if (to != null && to.isBefore(from)) throw BadRequest("Invalid time range")

        val authorizedUnits = acl.getAuthorizedUnits(user)
        return jdbi.handle { h -> ResponseEntity.ok(getMissingServiceNeedRows(h, from, to, authorizedUnits.ids)) }
    }
}

fun getMissingServiceNeedRows(
    h: Handle,
    from: LocalDate,
    to: LocalDate?,
    units: Collection<UUID>? = null
): List<MissingServiceNeedReportRow> {
    // language=sql
    val sql =
        """
        SELECT 
            ca.name AS care_area_name, daycare.name AS unit_name, unit_id,
            child_id, first_name, last_name, unit_id, sum(days_without_sn) AS days_without_sn 
        FROM (
          SELECT DISTINCT
            child_id,
            unit_id,
            days - days_with_sn AS days_without_sn
          FROM (
            SELECT
              pl.child_id,
              pl.unit_id,
              days_in_range(pl.period) AS days,
              coalesce(sum(days_in_range(pl.period * sn.period)) OVER w, 0) AS days_with_sn
            FROM (
              SELECT id, child_id, unit_id, daterange(start_date, end_date, '[]') * daterange(:from, :to, '[]') AS period
              FROM placement
              WHERE placement.type != 'CLUB'::placement_type
            ) AS pl
            LEFT JOIN (
              SELECT child_id, daterange(start_date, end_date, '[]') * daterange(:from, :to, '[]') AS period
              FROM service_need
            ) AS sn
            ON pl.child_id = sn.child_id
            AND pl.period && sn.period
            WINDOW w AS (PARTITION BY (pl.id))
          ) AS stats
          WHERE days - days_with_sn > 0
        ) results
        JOIN person ON person.id = child_id
        JOIN daycare ON daycare.id = unit_id AND 'CLUB'::care_types != ANY(daycare.type)
        JOIN care_area ca ON ca.id = daycare.care_area_id
        ${if (units != null) "WHERE daycare.id = ANY(:units)" else ""}
        GROUP BY ca.name, daycare.name, unit_id, child_id, first_name, last_name, unit_id
        ORDER BY ca.name, daycare.name, last_name, first_name
        """.trimIndent()
    return h.createQuery(sql)
        .bind("units", units?.toTypedArray())
        .bind("from", from)
        .bind("to", to)
        .map { rs, _ ->
            MissingServiceNeedReportRow(
                careAreaName = rs.getString("care_area_name"),
                unitId = rs.getUUID("unit_id"),
                unitName = rs.getString("unit_name"),
                childId = rs.getUUID("child_id"),
                firstName = rs.getString("first_name"),
                lastName = rs.getString("last_name"),
                daysWithoutServiceNeed = rs.getInt("days_without_sn")
            )
        }.toList()
}

data class MissingServiceNeedReportRow(
    val careAreaName: String,
    val unitId: UUID,
    val unitName: String,
    val childId: UUID,
    val firstName: String?,
    val lastName: String?,
    val daysWithoutServiceNeed: Int
)
