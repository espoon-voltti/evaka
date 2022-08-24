// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.occupancy.familyUnitPlacementCoefficient
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class AttendanceReservationReportController(private val accessControl: AccessControl) {

    @GetMapping("/reports/attendance-reservation/{unitId}")
    fun getAttendanceReservationReportByUnit(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
        @PathVariable unitId: DaycareId,
        db: Database,
        user: AuthenticatedUser
    ): List<AttendanceReservationReportRow> {
        Audit.AttendanceReservationReportRead.log(targetId = unitId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_ATTENDANCE_RESERVATION_REPORT, unitId)
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                tx.getAttendanceReservationReport(start, end, unitId)
            }
        }
    }
}

private fun Database.Read.getAttendanceReservationReport(
    start: LocalDate,
    end: LocalDate,
    unitId: DaycareId
): List<AttendanceReservationReportRow> {
    val sql = """
        WITH reservations AS (
          SELECT
            ar.date,
            ar.start_time,
            ar.end_time,
            extract(years FROM age(ar.date, p.date_of_birth)) AS age,
            CASE
              WHEN u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
              WHEN extract(years FROM age(ar.date, p.date_of_birth)) < 3 THEN coalesce(sno.occupancy_coefficient_under_3y, default_sno.occupancy_coefficient_under_3y)
              ELSE coalesce(sno.occupancy_coefficient, default_sno.occupancy_coefficient)
            END AS service_need_factor,
            coalesce(an.capacity_factor, 1) AS assistance_need_factor
          FROM attendance_reservation ar
          JOIN person p ON p.id = ar.child_id
          JOIN placement pl ON pl.child_id = p.id AND ar.date BETWEEN pl.start_date AND pl.end_date
          LEFT JOIN backup_care bc ON bc.child_id = p.id AND ar.date BETWEEN bc.start_date AND bc.end_date
          JOIN daycare u ON u.id = coalesce(bc.unit_id, pl.unit_id)
          LEFT JOIN service_need sn ON sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') @> ar.date
          LEFT JOIN service_need_option sno ON sno.id = sn.option_id
          LEFT JOIN service_need_option default_sno ON default_sno.valid_placement_type = pl.type AND default_sno.default_option
          LEFT JOIN assistance_need an ON an.child_id = p.id AND ar.date BETWEEN an.start_date AND an.end_date
          WHERE u.id = :unitId AND ar.date BETWEEN :start AND :end
        )
        SELECT
          dateTime AT TIME ZONE 'Europe/Helsinki' AS dateTime,
          count(*) FILTER (WHERE r.age < 3) AS child_count_under_3,
          count(*) FILTER (WHERE r.age >= 3) AS child_count_over_3,
          count(*) FILTER (WHERE r.age IS NOT NULL) AS child_count,
          sum(r.service_need_factor * r.assistance_need_factor) AS capacity_factor,
          round(sum(r.service_need_factor * r.assistance_need_factor) / 7, 1) AS staff_count_required
        FROM generate_series(:start, :end + interval '1 day' - interval '1 second', interval '30 minutes') dateTime
        CROSS JOIN daycare u
        LEFT JOIN reservations r ON dateTime BETWEEN
          r.date + date_trunc('hour', r.start_time) + date_part('minute', r.start_time)::int / 30 * interval '30 minutes'
          AND
          r.date + date_trunc('hour', r.end_time + interval '30 minutes' - interval '1 second') + date_part('minute', r.end_time + interval '30 minutes' - interval '1 second')::int / 30 * interval '30 minutes'
        WHERE u.id = :unitId AND extract(isodow FROM dateTime) = ANY(u.operation_days)
        GROUP BY dateTime
        ORDER BY dateTime
    """.trimIndent()
    return createQuery(sql)
        .bind("start", start)
        .bind("end", end)
        .bind("unitId", unitId)
        .mapTo<AttendanceReservationReportRow>()
        .toList()
}

data class AttendanceReservationReportRow(
    val dateTime: HelsinkiDateTime,
    val childCountUnder3: Int,
    val childCountOver3: Int,
    val childCount: Int,
    val capacityFactor: Double,
    val staffCountRequired: Double
)
