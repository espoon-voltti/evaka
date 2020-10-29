// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles.ADMIN
import fi.espoo.evaka.shared.config.Roles.DIRECTOR
import fi.espoo.evaka.shared.config.Roles.SERVICE_WORKER
import fi.espoo.evaka.shared.config.Roles.UNIT_SUPERVISOR
import fi.espoo.evaka.shared.db.handle
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
class ServiceNeedReport(
    private val acl: AccessControlList,
    private val jdbi: Jdbi
) {
    @GetMapping("/reports/service-need")
    fun getChildAgeLanguageReport(
        user: AuthenticatedUser,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<ServiceNeedReportRow>> {
        Audit.ServiceNeedReportRead.log()
        user.requireOneOfRoles(SERVICE_WORKER, ADMIN, DIRECTOR, UNIT_SUPERVISOR)
        val authorizedUnits = acl.getAuthorizedUnits(user)
        return jdbi.handle { h ->
            getServiceNeedRows(h, date, authorizedUnits.ids?.toList()).let(::ok)
        }
    }
}

fun getServiceNeedRows(h: Handle, date: LocalDate, units: List<UUID>? = null): List<ServiceNeedReportRow> {
    // language=sql
    val sql =
        """
        WITH ages AS (SELECT age FROM generate_series(0, 8) as age)
        SELECT
                care_area.name as care_area_name,
                d.name as unit_name,
                d.provider_type as unit_provider_type,
                d.type as unit_type,
                ages.age,
        
                count(DISTINCT p.id) FILTER ( WHERE sn.part_day = false ) as full_day,
                count(DISTINCT p.id) FILTER ( WHERE sn.part_day = true ) as part_day,
                count(DISTINCT p.id) FILTER ( WHERE sn.part_week = false ) as full_week,
                count(DISTINCT p.id) FILTER ( WHERE sn.part_week = true ) as part_week,
                count(DISTINCT p.id) FILTER ( WHERE sn.shift_care = true ) as shift_care,
                count(DISTINCT p.id) FILTER ( WHERE sn is null ) as missing_service_need,
                count(DISTINCT p.id) as total
        FROM daycare d
        JOIN ages ON true
        JOIN care_area ON d.care_area_id = care_area.id
        LEFT JOIN placement pl ON d.id = pl.unit_id AND daterange(pl.start_date, pl.end_date, '[]') @> :target_date AND pl.type != 'CLUB'::placement_type
        LEFT JOIN person p ON pl.child_id = p.id AND date_part('year', age(p.date_of_birth)) = ages.age
        LEFT JOIN service_need sn ON sn.child_id = p.id AND daterange(sn.start_date, sn.end_date, '[]') @> :target_date
        ${if (units != null) "WHERE d.id = ANY(:units :: uuid[])" else ""}
        GROUP BY care_area_name, ages.age, unit_name, unit_provider_type, unit_type
        ORDER BY care_area_name, unit_name, ages.age
        """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    return h.createQuery(sql)
        .bind("target_date", date)
        .bind("units", units?.toTypedArray())
        .map { rs, _ ->
            ServiceNeedReportRow(
                careAreaName = rs.getString("care_area_name"),
                unitName = rs.getString("unit_name"),
                unitType = (rs.getArray("unit_type").array as Array<out Any>).map { it.toString() }.toSet().let(::getPrimaryUnitType),
                unitProviderType = rs.getString("unit_provider_type"),
                age = rs.getInt("age"),
                fullDay = rs.getInt("full_day"),
                partDay = rs.getInt("part_day"),
                fullWeek = rs.getInt("full_week"),
                partWeek = rs.getInt("part_week"),
                shiftCare = rs.getInt("shift_care"),
                missingServiceNeed = rs.getInt("missing_service_need"),
                total = rs.getInt("total")
            )
        }
        .toList()
}

data class ServiceNeedReportRow(
    val careAreaName: String,
    val unitName: String,
    val unitType: UnitType?,
    val unitProviderType: String,
    val age: Int,
    val fullDay: Int,
    val partDay: Int,
    val fullWeek: Int,
    val partWeek: Int,
    val shiftCare: Int,
    val missingServiceNeed: Int,
    val total: Int
)
