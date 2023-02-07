// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.fasterxml.jackson.annotation.JsonFormat
import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.occupancy.familyUnitPlacementCoefficient
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AttendanceReservationReportController(private val accessControl: AccessControl) {

    @GetMapping("/reports/attendance-reservation/{unitId}")
    fun getAttendanceReservationReportByUnit(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
        @PathVariable unitId: DaycareId,
        @RequestParam(required = false) groupIds: List<GroupId>?,
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<AttendanceReservationReportRow> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_ATTENDANCE_RESERVATION_REPORT,
                        unitId
                    )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    tx.getAttendanceReservationReport(
                        start,
                        end,
                        unitId,
                        groupIds?.ifEmpty { null }
                    )
                }
            }
            .also {
                Audit.AttendanceReservationReportRead.log(
                    targetId = unitId,
                    meta =
                        mapOf(
                            "groupIds" to groupIds,
                            "start" to start,
                            "end" to end,
                            "count" to it.size
                        )
                )
            }
    }

    @GetMapping("/reports/attendance-reservation/{unitId}/by-child")
    fun getAttendanceReservationReportByUnitAndChild(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
        @PathVariable unitId: DaycareId,
        @RequestParam(required = false) groupIds: List<GroupId>?,
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser
    ): List<AttendanceReservationReportByChildRow> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_ATTENDANCE_RESERVATION_REPORT,
                        unitId
                    )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    tx.getAttendanceReservationReportByChild(
                        start,
                        end,
                        unitId,
                        groupIds?.ifEmpty { null }
                    )
                }
            }
            .also {
                Audit.AttendanceReservationReportRead.log(
                    targetId = unitId,
                    meta =
                        mapOf(
                            "groupIds" to groupIds,
                            "start" to start,
                            "end" to end,
                            "count" to it.size
                        )
                )
            }
    }
}

private fun Database.Read.getAttendanceReservationReport(
    start: LocalDate,
    end: LocalDate,
    unitId: DaycareId,
    groupIds: List<GroupId>?
): List<AttendanceReservationReportRow> {
    val sql =
        """
        WITH reservations AS (
          SELECT
            CASE WHEN bc.id IS NOT NULL THEN bc.group_id ELSE dgp.daycare_group_id END AS group_id,
            ar.child_id,
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
          LEFT JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = pl.id AND ar.date BETWEEN dgp.start_date AND dgp.end_date
          LEFT JOIN backup_care bc ON bc.child_id = p.id AND ar.date BETWEEN bc.start_date AND bc.end_date
          JOIN daycare u ON u.id = coalesce(bc.unit_id, pl.unit_id)
          LEFT JOIN service_need sn ON sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') @> ar.date
          LEFT JOIN service_need_option sno ON sno.id = sn.option_id
          LEFT JOIN service_need_option default_sno ON default_sno.valid_placement_type = pl.type AND default_sno.default_option
          LEFT JOIN assistance_need an ON an.child_id = p.id AND ar.date BETWEEN an.start_date AND an.end_date
          WHERE u.id = :unitId AND ar.date BETWEEN :start AND :end
            AND (:groupIds::uuid[] IS NULL OR coalesce(bc.group_id, dgp.daycare_group_id) = ANY(:groupIds))
        ), groups AS (
          SELECT dg.id, dg.name, d.operation_days
          FROM daycare_group dg
          JOIN daycare d ON d.id = dg.daycare_id
          WHERE d.id = :unitId
          UNION
          SELECT NULL, NULL, operation_days
          FROM daycare WHERE id = :unitId
        )
        SELECT
          ${if (groupIds != null) "g.id AS group_id, g.name AS group_name" else "NULL AS group_id, NULL as group_name"},
          dateTime AT TIME ZONE 'Europe/Helsinki' AS dateTime,
          count(*) FILTER (WHERE r.age < 3) AS child_count_under_3,
          count(*) FILTER (WHERE r.age >= 3) AS child_count_over_3,
          count(*) FILTER (WHERE r.age IS NOT NULL) AS child_count,
          coalesce(sum(r.service_need_factor * r.assistance_need_factor), 0) AS capacity_factor,
          coalesce(round(sum(r.service_need_factor * r.assistance_need_factor) / 7, 1), 0) AS staff_count_required
        FROM generate_series(:start, :end + interval '1 day' - interval '1 second', interval '30 minutes') dateTime
        CROSS JOIN groups g
        LEFT JOIN reservations r ON
          (r.group_id IS NULL AND g.id IS NULL OR r.group_id = g.id)
          AND
          dateTime BETWEEN
          r.date + r.start_time - interval '30 minutes' + interval '1 second'
          AND
          r.date + r.end_time + interval '30 minutes' - interval '1 second'
        WHERE extract(isodow FROM dateTime) = ANY(g.operation_days)
          AND (:groupIds::uuid[] IS NULL OR g.id = ANY(:groupIds))
          AND NOT EXISTS(SELECT 1 FROM absence WHERE absence.child_id = r.child_id AND absence.date = r.date)  
        GROUP BY 1, 2, 3
        ORDER BY 2, 3
    """
            .trimIndent()
    return createQuery(sql)
        .bind("start", start)
        .bind("end", end)
        .bind("unitId", unitId)
        .bind("groupIds", groupIds)
        .mapTo<AttendanceReservationReportRow>()
        .toList()
}

data class AttendanceReservationReportRow(
    val groupId: GroupId?,
    val groupName: String?,
    val dateTime: HelsinkiDateTime,
    val childCountUnder3: Int,
    val childCountOver3: Int,
    val childCount: Int,
    val capacityFactor: Double,
    val staffCountRequired: Double
)

private fun Database.Read.getAttendanceReservationReportByChild(
    start: LocalDate,
    end: LocalDate,
    unitId: DaycareId,
    groupIds: List<GroupId>?
): List<AttendanceReservationReportByChildRow> {
    val sql =
        """
        WITH
        dates AS (SELECT generate_series::date AS date FROM generate_series(:start, :end, interval '1 day')),
        children AS (
          SELECT
            g.id AS group_id, g.name AS group_name,
            date, p.id, p.last_name, p.first_name, bc.id IS NOT NULL AS is_backup_care
          FROM dates date
          JOIN placement pl ON date BETWEEN pl.start_date AND pl.end_date
          JOIN person p ON p.id = pl.child_id
          LEFT JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = pl.id AND date BETWEEN dgp.start_date AND dgp.end_date
          LEFT JOIN backup_care bc ON bc.child_id = p.id AND date BETWEEN bc.start_date AND bc.end_date
          JOIN daycare u ON u.id = coalesce(bc.unit_id, pl.unit_id)
          LEFT JOIN daycare_group g ON g.daycare_id = u.id AND g.id = coalesce(bc.group_id, dgp.daycare_group_id)
          WHERE u.id = :unitId
            AND (:groupIds::uuid[] IS NULL OR g.id = ANY(:groupIds))
            AND extract(isodow FROM date) = ANY(u.operation_days)
        )
        SELECT
          ${if (groupIds != null) "c.group_id, c.group_name" else "NULL AS group_id, NULL as group_name"},
          c.date,
          c.id AS child_id,
          c.last_name AS child_last_name,
          c.first_name AS child_first_name,
          c.is_backup_care,
          a.id AS absence_id,
          a.absence_type,
          r.id AS reservation_id,
          r.start_time AS reservation_start_time,
          r.end_time AS reservation_end_time
        FROM children c
        LEFT JOIN absence a ON a.child_id = c.id AND a.date = c.date
        LEFT JOIN attendance_reservation r ON r.child_id = c.id AND r.date = c.date
    """
            .trimIndent()
    return createQuery(sql)
        .bind("start", start)
        .bind("end", end)
        .bind("unitId", unitId)
        .bind("groupIds", groupIds)
        .mapTo<AttendanceReservationReportByChildRow>()
        .toList()
}

data class AttendanceReservationReportByChildRow(
    val groupId: GroupId?,
    val groupName: String?,
    val date: LocalDate,
    val childId: ChildId,
    val childLastName: String,
    val childFirstName: String,
    val isBackupCare: Boolean,
    val absenceId: AbsenceId?,
    val absenceType: AbsenceType?,
    val reservationId: AttendanceReservationId?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val reservationStartTime: LocalTime?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val reservationEndTime: LocalTime?
)
