// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
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
            val unitName = tx.getDaycare(unitId)?.name ?: throw NotFound("Unit $unitId not found")
            val operationalDays = tx.getUnitOperationalDays(unitId, dateRange)
            tx
                .getAttendanceReservationData(unitId, dateRange)
                .let { it }
                .groupBy { it.group }
                .let { groupedReservations ->
                    val ungroupedRows = groupedReservations[null]

                    UnitAttendanceReservations(
                        unit = unitName,
                        operationalDays = operationalDays,
                        groups = groupedReservations.entries.mapNotNull { (group, rows) ->
                            if (group == null) null
                            else UnitAttendanceReservations.GroupAttendanceReservations(
                                group = group,
                                children = mapRowsToChildReservations(rows)
                            )
                        },
                        ungrouped = ungroupedRows?.let { mapRowsToChildReservations(it) } ?: emptyList()
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

private fun Database.Read.getAttendanceReservationData(unitId: DaycareId, dateRange: FiniteDateRange) = createQuery(
    """
    SELECT 
        t::date AS date,
        dg.name AS "group",
        p.id,
        p.first_name,
        p.last_name,
        p.date_of_birth,
        to_char((res.start_time AT TIME ZONE 'Europe/Helsinki')::time, 'HH24:MI') AS reservation_start_time,
        to_char((res.end_time AT TIME ZONE 'Europe/Helsinki')::time, 'HH24:MI') AS reservation_end_time,
        att.arrived AS attendance_start,
        att.departed AS attendance_end,
        ab.absence_type
    FROM generate_series(:start, :end, '1 day') t
    JOIN placement pl ON pl.unit_id = :unitId AND daterange(pl.start_date, pl.end_date, '[]') @> t::date
    JOIN person p ON p.id = pl.child_id
    LEFT JOIN daycare_group_placement dgp on dgp.daycare_placement_id = pl.id AND daterange(dgp.start_date, dgp.end_date, '[]') @> t::date
    LEFT JOIN daycare_group dg ON dg.id = dgp.daycare_group_id
    LEFT JOIN attendance_reservation res ON res.child_id = p.id AND res.start_date = t::date
    LEFT JOIN child_attendance att ON att.child_id = p.id AND (att.arrived AT TIME ZONE 'Europe/Helsinki')::date = t::date
    LEFT JOIN absence ab ON ab.child_id = p.id AND ab.date = t::date
    """.trimIndent()
)
    .bind("unitId", unitId)
    .bind("start", dateRange.start)
    .bind("end", dateRange.end)
    .mapTo<UnitAttendanceReservations.QueryRow>()
    .toList()

private fun mapRowsToChildReservations(rows: List<UnitAttendanceReservations.QueryRow>): List<UnitAttendanceReservations.ChildReservations> {
    return rows
        .groupBy { it.child }
        .map { (child, rowsByChild) ->
            UnitAttendanceReservations.ChildReservations(
                child = child,
                dailyData = rowsByChild
                    .associateBy { it.date }
                    .map { (date, rowByDateAndChild) ->
                        UnitAttendanceReservations.DailyChildData(
                            date = date,
                            reservation = rowByDateAndChild.reservation?.let {
                                UnitAttendanceReservations.ReservationTimes(
                                    startTime = it.startTime,
                                    endTime = it.endTime
                                )
                            },
                            attendance = rowByDateAndChild.attendance?.let {
                                UnitAttendanceReservations.AttendanceTimes(
                                    startTime = it.start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                                    endTime = it.end?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")),
                                )
                            },
                            absence = rowByDateAndChild.absence?.let {
                                UnitAttendanceReservations.Absence(
                                    type = it.type
                                )
                            }
                        )
                    }
                    .sortedBy { it.date }
            )
        }
        .sortedBy { "${it.child.firstName} ${it.child.lastName}" }
}

data class UnitAttendanceReservations(
    val unit: String,
    val operationalDays: List<OperationalDay>,
    val groups: List<GroupAttendanceReservations>,
    val ungrouped: List<ChildReservations>
) {
    data class OperationalDay(
        val date: LocalDate,
        val isHoliday: Boolean
    )

    data class GroupAttendanceReservations(
        val group: String,
        val children: List<ChildReservations>
    )

    data class ChildReservations(
        val child: Child,
        val dailyData: List<DailyChildData>
    )

    data class DailyChildData(
        val date: LocalDate,
        val reservation: ReservationTimes?,
        val attendance: AttendanceTimes?,
        val absence: Absence?
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
        val id: PersonId,
        val firstName: String,
        val lastName: String,
        val dateOfBirth: LocalDate
    )

    data class QueryRow(
        val date: LocalDate,
        val group: String?,
        @Nested
        val child: Child,
        @Nested("reservation")
        val reservation: QueryRowReservation?,
        @Nested("attendance")
        val attendance: QueryRowAttendance?,
        @Nested("absence")
        val absence: QueryRowAbsence?
    )

    data class QueryRowReservation(
        @PropagateNull
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
        val type: AbsenceType
    )
}
