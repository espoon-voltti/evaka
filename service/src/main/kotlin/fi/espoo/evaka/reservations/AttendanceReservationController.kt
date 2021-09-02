// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/attendance-reservations")
class AttendanceReservationController(private val ac: AccessControl) {
    @GetMapping
    fun getAttendanceReservations(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): UnitAttendanceReservations {
        Audit.UnitAttendanceReservations.log(targetId = unitId, objectId = from)
        ac.requirePermissionFor(user, Action.Unit.READ_ATTENDANCE_RESERVATIONS, unitId)
        if (to < from || from.plusMonths(1) < to) throw BadRequest("Invalid query dates")
        val dateRange = FiniteDateRange(from, to)
        return db.read { tx ->
            val operationalDays = tx.getUnitOperationalDays(unitId, dateRange)
            tx
                .getUnitAttendanceReservations(unitId, dateRange)
                .groupBy { it.group }
                .let { groupedReservations ->
                    val ungroupedRows = groupedReservations[null]
                        ?: error("Unit attendances query should always include ungrouped")

                    UnitAttendanceReservations(
                        unit = ungroupedRows.first().unit,
                        operationalDays = operationalDays,
                        groups = groupedReservations.entries.mapNotNull { (group, rows) ->
                            if (group == null) null
                            else UnitAttendanceReservations.GroupAttendanceReservations(
                                group = group,
                                children = mapChildReservationsAndAttendances(rows)
                            )
                        },
                        ungrouped = mapChildReservationsAndAttendances(ungroupedRows)
                    )
                }
        }
    }
}

private fun Database.Read.getUnitOperationalDays(unitId: DaycareId, dateRange: FiniteDateRange) = createQuery(
    """
    SELECT t::date AS date, holiday.date IS NOT NULL AS is_holiday
    FROM generate_series(:start, :end, '1 day') t
    JOIN daycare ON daycare.id = :unitId AND date_part('isodow', t) = ANY(daycare.operation_days)
    LEFT JOIN holiday ON t = holiday.date
    """.trimIndent()
)
    .bind("unitId", unitId)
    .bind("start", dateRange.start)
    .bind("end", dateRange.end)
    .mapTo<UnitAttendanceReservations.OperationalDay>()
    .toList()

private fun Database.Read.getUnitAttendanceReservations(unitId: DaycareId, dateRange: FiniteDateRange) = createQuery(
    """
    WITH unit_group AS (
        SELECT
            daycare.id AS unit_id,
            daycare.name AS unit_name,
            daycare_group.id AS group_id,
            daycare_group.name AS group_name
        FROM daycare JOIN daycare_group ON daycare.id = daycare_group.daycare_id
        WHERE daycare.id = :unitId
        UNION ALL
        SELECT
            daycare.id AS unit_id,
            daycare.name AS unit_name,
            NULL AS group_id,
            NULL AS group_name
        FROM daycare
        WHERE daycare.id = :unitId
    ), group_res AS (
        SELECT
            child.id,
            child.first_name,
            child.last_name,
            child.date_of_birth,
            res.start_time as reservation_start,
            res.end_time as reservation_end,
            res.start_date as reservation_date,
            att.arrived as attendance_start,
            att.departed as attendance_end,
            abs.date as absence_date,
            abs.absence_type,
            group_placement.daycare_group_id AS group_id
        FROM person child
        JOIN placement ON child.id = placement.child_id AND daterange(placement.start_date, placement.end_date, '[]') && :dateRange
        LEFT JOIN daycare_group_placement group_placement ON placement.id = group_placement.daycare_placement_id
            AND daterange(placement.start_date, placement.end_date, '[]') && daterange(group_placement.start_date, group_placement.end_date, '[]')
        -- todo: should these joins be also limited by :dateRange?
        -- todo: I guess these join conditions now result in tons of unnecessary rows, needs optimizing/rethinking
        LEFT JOIN attendance_reservation res ON child.id = res.child_id AND res.start_date BETWEEN placement.start_date AND placement.end_date
        LEFT JOIN child_attendance att ON child.id = att.child_id AND att.arrived BETWEEN placement.start_date AND placement.end_date
        LEFT JOIN absence abs ON child.id = abs.child_id AND abs.date BETWEEN placement.start_date AND placement.end_date
    )
    SELECT
        unit_group.unit_name AS unit,
        unit_group.group_name AS group,
        group_res.id,
        group_res.first_name,
        group_res.last_name,
        group_res.date_of_birth,
        group_res.reservation_date,
        to_char((group_res.reservation_start AT TIME ZONE 'Europe/Helsinki')::time, 'HH24:MI') AS reservation_start_time,
        to_char((group_res.reservation_end AT TIME ZONE 'Europe/Helsinki')::time, 'HH24:MI') AS reservation_end_time,
        group_res.attendance_start,
        group_res.attendance_end,
        group_res.absence_date,
        group_res.absence_type
    FROM unit_group
    LEFT JOIN group_res ON unit_group.group_id = group_res.group_id OR (unit_group.group_id IS NULL AND group_res.group_id IS NULL)
    """.trimIndent()
)
    .bind("unitId", unitId)
    .bind("dateRange", dateRange)
    .mapTo<UnitAttendanceReservations.QueryRow>()
    .toList()

data class UnitAttendanceReservations(
    val unit: String,
    val operationalDays: List<OperationalDay>,
    val groups: List<GroupAttendanceReservations>,
    val ungrouped: List<ChildReservations>
) {
    data class GroupAttendanceReservations(
        val group: String,
        val children: List<ChildReservations>
    )

    data class ChildReservations(
        val child: Child,
        val reservations: Map<LocalDate, ReservationTimes>,
        val attendances: Map<LocalDate, AttendanceTimes>,
        val absences: Map<LocalDate, Absence>
    )

    data class ReservationTimes(
        val startTime: String,
        val endTime: String
    )

    data class AttendanceTimes(
        val startTime: String,
        val endTime: String?
    )

    data class Absence(
        val type: AbsenceType
    )

    data class Child(
        @PropagateNull
        val id: PersonId,
        val firstName: String,
        val lastName: String,
        val dateOfBirth: LocalDate
    )

    data class QueryRow(
        val unit: String,
        val group: String?,
        @Nested
        val child: Child?,
        @Nested("reservation")
        val reservation: QueryRowReservation?,
        @Nested("attendance")
        val attendance: QueryRowAttendance?,
        @Nested("absence")
        val absence: QueryRowAbsence?
    )

    data class QueryRowReservation(
        @PropagateNull
        val date: LocalDate,
        val startTime: String,
        val endTime: String
    )

    data class QueryRowAttendance(
        @PropagateNull
        val start: HelsinkiDateTime,
        val end: HelsinkiDateTime?
    )

    data class QueryRowAbsence(
        @PropagateNull
        val date: LocalDate,
        val type: AbsenceType
    )

    data class OperationalDay(
        val date: LocalDate,
        val isHoliday: Boolean
    )
}

private fun mapChildReservationsAndAttendances(
    data: List<UnitAttendanceReservations.QueryRow>
): List<UnitAttendanceReservations.ChildReservations> = data
    .mapNotNull { row -> row.child?.let { it to ChildData(row.reservation, row.attendance, row.absence) } }
    .groupBy { (child, _) -> child }
    .map { (child, childDataList) ->
        UnitAttendanceReservations.ChildReservations(
            child = child,
            reservations = childDataList
                .mapNotNull { it.second.reservation }
                .associate { it.date to UnitAttendanceReservations.ReservationTimes(it.startTime, it.endTime) },
            attendances = childDataList
                .mapNotNull { it.second.attendance }
                .associate {
                    it.start.toLocalDate() to UnitAttendanceReservations.AttendanceTimes(
                        it.start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        it.end?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm"))
                    )
                },
            absences = childDataList
                .mapNotNull { it.second.absence }
                .associate { it.date to UnitAttendanceReservations.Absence(it.type) }
        )
    }

private data class ChildData(
    val reservation: UnitAttendanceReservations.QueryRowReservation?,
    val attendance: UnitAttendanceReservations.QueryRowAttendance?,
    val absence: UnitAttendanceReservations.QueryRowAbsence?
)
