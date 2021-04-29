// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.utils.helsinkiZone
import fi.espoo.evaka.serviceneednew.migrationSqlCases
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.utils.applyIf
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class InvalidServiceNeedReportController(private val acl: AccessControlList) {
    @GetMapping("/reports/invalid-service-need")
    fun getInvalidServiceNeedReport(
        db: Database,
        user: AuthenticatedUser
    ): ResponseEntity<List<InvalidServiceNeedReportRow>> {
        Audit.InvalidServiceNeedReportRead.log()
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        return db.read { ResponseEntity.ok(it.getInvalidServiceNeedRows(acl.getAuthorizedUnits(user))) }
    }
}

private fun Database.Read.getInvalidServiceNeedRows(
    authorizedUnits: AclAuthorization
): List<InvalidServiceNeedReportRow> {
    // language=sql
    val sql =
        """
WITH data AS (
    SELECT
        p.child_id,
        p.unit_id,
        ch.first_name,
        ch.last_name,
        ($migrationSqlCases) AS option_name,
        greatest(p.start_date, sn.start_date) AS start_date,
        least(p.end_date, sn.end_date) AS end_date
    FROM placement p
    JOIN service_need sn ON p.child_id = sn.child_id AND daterange(p.start_date, p.end_date, '[]') && daterange(sn.start_date, sn.end_date, '[]')
    JOIN person ch ON ch.id = p.child_id
)
SELECT 
    data.*,
    pl.unit_id as current_unit_id,
    u.name as current_unit_name
FROM data
LEFT JOIN placement pl ON pl.child_id = data.child_id AND daterange(pl.start_date, pl.end_date, '[]') @> :today
LEFT JOIN daycare u ON pl.unit_id = u.id
WHERE option_name = 'undefined' ${if (authorizedUnits != AclAuthorization.All) "AND u.id = ANY(:units) AND data.unit_id = ANY(:units)" else ""}
ORDER BY u.name, last_name, first_name, start_date
        """.trimIndent()

    return createQuery(sql)
        .bind("today", LocalDate.now(helsinkiZone))
        .applyIf(authorizedUnits != AclAuthorization.All) {
            bind("units", authorizedUnits.ids?.toTypedArray())
        }
        .mapTo<InvalidServiceNeedReportRow>()
        .list()
}

data class InvalidServiceNeedReportRow(
    val currentUnitId: UUID?,
    val currentUnitName: String?,
    val childId: UUID,
    val firstName: String?,
    val lastName: String?,
    val startDate: LocalDate,
    val endDate: LocalDate
)
