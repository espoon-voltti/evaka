// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceNeedReport(
    private val acl: AccessControlList,
    private val accessControl: AccessControl
) {
    @GetMapping("/reports/service-need")
    fun getServiceNeedReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): List<ServiceNeedReportRow> {
        Audit.ServiceNeedReportRead.log()
        accessControl.requirePermissionFor(user, clock, Action.Global.READ_SERVICE_NEED_REPORT)
        return db.connect { dbc ->
            dbc.read {
                it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                it.getServiceNeedRows(date, acl.getAuthorizedUnits(user))
            }
        }
    }
}

private fun Database.Read.getServiceNeedRows(
    date: LocalDate,
    authorizedUnits: AclAuthorization
): List<ServiceNeedReportRow> {
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
        
                count(DISTINCT p.id) FILTER ( WHERE sno.part_day = false ) as full_day,
                count(DISTINCT p.id) FILTER ( WHERE sno.part_day = true ) as part_day,
                count(DISTINCT p.id) FILTER ( WHERE sno.part_week = false ) as full_week,
                count(DISTINCT p.id) FILTER ( WHERE sno.part_week = true ) as part_week,
                count(DISTINCT p.id) FILTER ( WHERE sn.shift_care = true ) as shift_care,
                count(DISTINCT p.id) FILTER ( WHERE sn is null ) as missing_service_need,
                count(DISTINCT p.id) as total
        FROM daycare d
        JOIN ages ON true
        JOIN care_area ON d.care_area_id = care_area.id
        LEFT JOIN placement pl ON d.id = pl.unit_id AND daterange(pl.start_date, pl.end_date, '[]') @> :target_date AND pl.type != 'CLUB'::placement_type
        LEFT JOIN person p ON pl.child_id = p.id AND date_part('year', age(:target_date, p.date_of_birth)) = ages.age
        LEFT JOIN service_need sn ON sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') @> :target_date
        LEFT JOIN service_need_option sno ON sno.id = sn.option_id
        ${if (authorizedUnits != AclAuthorization.All) "WHERE d.id = ANY(:units :: uuid[])" else ""}
        GROUP BY care_area_name, ages.age, unit_name, unit_provider_type, unit_type
        ORDER BY care_area_name, unit_name, ages.age
        """.trimIndent(
        )

    return createQuery(sql)
        .bind("target_date", date)
        .bind("units", authorizedUnits.ids)
        .registerColumnMapper(UnitType.JDBI_COLUMN_MAPPER)
        .mapTo<ServiceNeedReportRow>()
        .toList()
}

data class ServiceNeedReportRow(
    val careAreaName: String,
    val unitName: String,
    val unitType: UnitType,
    val unitProviderType: ProviderType,
    val age: Int,
    val fullDay: Int,
    val partDay: Int,
    val fullWeek: Int,
    val partWeek: Int,
    val shiftCare: Int,
    val missingServiceNeed: Int,
    val total: Int
)
