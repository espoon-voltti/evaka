// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class SextetReportController(private val accessControl: AccessControl) {
    @GetMapping("/reports/sextet")
    fun getApplicationsReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam year: Int,
        @RequestParam placementType: PlacementType
    ): List<SextetReportRow> {
        Audit.SextetReportRead.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_SEXTET_REPORT)

        return db.connect { dbc ->
            dbc.read {
                it.sextetReport(
                    LocalDate.of(year, 1, 1),
                    LocalDate.of(year, 12, 31),
                    placementType,
                )
            }
        }
    }
}

fun Database.Read.sextetReport(from: LocalDate, to: LocalDate, placementType: PlacementType): List<SextetReportRow> {
    return createQuery(
        """
WITH operational_days AS (
    SELECT daycare.id AS unit_id, date
    FROM generate_series(:from, :to, '1 day'::interval) date
    JOIN daycare ON extract(isodow from date) = ANY(daycare.operation_days)
    WHERE
       date <> ALL (SELECT date FROM holiday)
       OR daycare.operation_days = '{1,2,3,4,5,6,7}'::int[]
)
SELECT d.id as unit_id, d.name as unit_name, p.type as placement_type, count(p.id) AS attendance_days
FROM operational_days od
JOIN placement p ON p.unit_id = od.unit_id AND od.date BETWEEN p.start_date AND p.end_date
JOIN daycare d ON p.unit_id = d.id
WHERE NOT EXISTS (
    SELECT 1
    FROM absence
    WHERE child_id = p.child_id AND date = od.date
    HAVING count(category) >= cardinality(absence_categories(p.type))
)
AND p.type = :placementType
GROUP BY d.id, d.name, p.type
ORDER BY d.name, p.type
    """
    )
        .bind("from", from)
        .bind("to", to)
        .bind("placementType", placementType)
        .mapTo<SextetReportRow>()
        .list()
}

data class SextetReportRow(
    val unitId: DaycareId,
    val unitName: String,
    val placementType: PlacementType,
    val attendanceDays: Int
)
