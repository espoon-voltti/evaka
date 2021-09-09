// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.Audit
import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.dailyservicetimes.toDailyServiceTimes
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

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
        Audit.UnitAttendanceReservationsRead.log(targetId = unitId, objectId = from)
        ac.requirePermissionFor(user, Action.Unit.READ_ATTENDANCE_RESERVATIONS, unitId)
        if (to < from || from.plusMonths(1) < to) throw BadRequest("Invalid query dates")
        val dateRange = FiniteDateRange(from, to)
        return db.read { tx ->
            val unitName = tx.getDaycare(unitId)?.name ?: throw NotFound("Unit $unitId not found")
            val operationalDays = tx.getUnitOperationalDays(unitId, dateRange)
            tx
                .getAttendanceReservationData(unitId, dateRange)
                .groupBy { it.group }
                .let { groupedReservations ->
                    val ungroupedRows = groupedReservations[null]

                    val childIds = groupedReservations.values.flatten().map { it.child.id }.toSet()
                    val serviceTimes = tx.getDailyServiceTimes(childIds)

                    UnitAttendanceReservations(
                        unit = unitName,
                        operationalDays = operationalDays,
                        groups = groupedReservations.entries.mapNotNull { (group, rows) ->
                            if (group == null) null
                            else UnitAttendanceReservations.GroupAttendanceReservations(
                                group = group,
                                children = mapChildReservations(rows, serviceTimes)
                            )
                        },
                        ungrouped = ungroupedRows?.let { mapChildReservations(it, serviceTimes) } ?: emptyList()
                    )
                }
        }
    }

    @PostMapping
    fun postReservations(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: List<DailyReservationRequest>
    ) {
        val distinctChildIds = body.map { it.childId }.toSet()
        Audit.AttendanceReservationEmployeeCreate.log(targetId = distinctChildIds.joinToString())
        distinctChildIds.forEach { childId ->
            ac.requirePermissionFor(user, Action.Child.CREATE_ATTENDANCE_RESERVATION, childId)
        }

        db.transaction { createReservationsAsEmployee(it, user.id, body) }
    }
}

@ExcludeCodeGen
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
        val dailyData: Map<LocalDate, DailyChildData>
    )

    data class DailyChildData(
        val reservation: ReservationTimes?,
        val attendance: AttendanceTimes?,
        val absence: Absence?
    )

    data class ReservationTimes(
        @PropagateNull
        val startTime: String,
        val endTime: String
    )

    data class AttendanceTimes(
        @PropagateNull
        val startTime: String,
        val endTime: String?
    )

    data class Absence(
        @PropagateNull
        val type: AbsenceType
    )

    data class Child(
        val id: UUID,
        val firstName: String,
        val lastName: String,
        val dateOfBirth: LocalDate,
        val dailyServiceTimes: DailyServiceTimes?
    )

    data class QueryRow(
        val date: LocalDate,
        val group: String?,
        @Nested
        val child: Child,
        @Nested("reservation")
        val reservation: ReservationTimes?,
        @Nested("attendance")
        val attendance: AttendanceTimes?,
        @Nested("absence")
        val absence: Absence?
    )
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
        to_char((att.arrived AT TIME ZONE 'Europe/Helsinki')::time, 'HH24:MI') AS attendance_start_time,
        to_char((att.departed AT TIME ZONE 'Europe/Helsinki')::time, 'HH24:MI') AS attendance_end_time,
        ab.absence_type
    FROM generate_series(:start, :end, '1 day') t
    JOIN placement pl ON pl.unit_id = :unitId AND daterange(pl.start_date, pl.end_date, '[]') @> t::date
    JOIN person p ON p.id = pl.child_id
    LEFT JOIN daycare_group_placement dgp on dgp.daycare_placement_id = pl.id AND daterange(dgp.start_date, dgp.end_date, '[]') @> t::date
    LEFT JOIN daycare_group dg ON dg.id = dgp.daycare_group_id
    LEFT JOIN attendance_reservation res ON res.child_id = p.id AND res.start_date = t::date
    LEFT JOIN child_attendance att ON att.child_id = p.id AND (att.arrived AT TIME ZONE 'Europe/Helsinki')::date = t::date
    LEFT JOIN LATERAL (
        SELECT absence_type
        FROM absence
        WHERE absence.date = t::date AND absence.child_id = p.id
        LIMIT 1
    ) ab ON true
    """.trimIndent()
)
    .bind("unitId", unitId)
    .bind("start", dateRange.start)
    .bind("end", dateRange.end)
    .mapTo<UnitAttendanceReservations.QueryRow>()
    .toList()

// currently queried separately to be able to use existing mapper
private fun Database.Read.getDailyServiceTimes(childIds: Set<UUID>) = createQuery(
    """
    SELECT *
    FROM daily_service_time
    WHERE child_id = ANY(:childIds)
    """.trimIndent()
)
    .bind("childIds", childIds.toTypedArray())
    .map { row -> toDailyServiceTimes(row)?.let { row.mapColumn<UUID>("child_id") to it } }
    .filterNotNull()
    .toMap()

private fun mapChildReservations(rows: List<UnitAttendanceReservations.QueryRow>, serviceTimes: Map<UUID, DailyServiceTimes>): List<UnitAttendanceReservations.ChildReservations> {
    return rows
        .groupBy { it.child }
        .map { (child, rowsByChild) ->
            UnitAttendanceReservations.ChildReservations(
                child = child.copy(dailyServiceTimes = serviceTimes.get(child.id)),
                dailyData = rowsByChild.associateBy(
                    keySelector = { it.date },
                    valueTransform = {
                        UnitAttendanceReservations.DailyChildData(
                            reservation = it.reservation,
                            attendance = it.attendance,
                            absence = it.absence
                        )
                    }
                )
            )
        }
        .sortedBy { "${it.child.firstName} ${it.child.lastName}" }
}

fun createReservationsAsEmployee(tx: Database.Transaction, userId: UUID, reservations: List<DailyReservationRequest>) {
    tx.clearOldAbsences(reservations.filter { it.reservation != null }.map { it.childId to it.date })
    tx.clearOldReservations(reservations.map { it.childId to it.date })
    tx.insertValidReservations(userId, reservations)
}

private fun Database.Transaction.insertValidReservations(userId: UUID, reservations: List<DailyReservationRequest>) {
    val batch = prepareBatch(
        """
        INSERT INTO attendance_reservation (child_id, start_time, end_time, created_by_guardian_id, created_by_employee_id)
        SELECT :childId, :start, :end, NULL, :userId
        FROM placement pl 
        JOIN daycare d ON d.id = pl.unit_id
        WHERE 
            pl.child_id = :childId AND 
            daterange(pl.start_date, pl.end_date, '[]') @> :date AND 
            extract(DOW FROM :date) = ANY(d.operation_days) AND 
            NOT EXISTS(SELECT 1 FROM holiday h WHERE h.date = :date) AND
            NOT EXISTS(SELECT 1 FROM absence ab WHERE ab.child_id = :childId AND ab.date = :date)
        ON CONFLICT DO NOTHING;
        """.trimIndent()
    )

    reservations.forEach { res ->
        if (res.reservation != null) {
            val start = HelsinkiDateTime.of(
                date = res.date,
                time = res.reservation.startTime
            )
            val end = HelsinkiDateTime.of(
                date = if (res.reservation.endTime.isAfter(res.reservation.startTime)) res.date else res.date.plusDays(1),
                time = res.reservation.endTime
            )
            batch
                .bind("userId", userId)
                .bind("childId", res.childId)
                .bind("start", start)
                .bind("end", end)
                .bind("date", res.date)
                .add()
        }
    }

    batch.execute()
}
