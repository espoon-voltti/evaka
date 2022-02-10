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
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json
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
        db: Database,
        user: AuthenticatedUser,
        @RequestParam unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): UnitAttendanceReservations {
        Audit.UnitAttendanceReservationsRead.log(targetId = unitId, objectId = from)
        ac.requirePermissionFor(user, Action.Unit.READ_ATTENDANCE_RESERVATIONS, unitId)
        if (to < from || from.plusMonths(1) < to) throw BadRequest("Invalid query dates")
        val dateRange = FiniteDateRange(from, to)
        return db.connect { dbc ->
            dbc.read { tx ->
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
                                    children = toChildDayRows(rows, serviceTimes)
                                )
                            },
                            ungrouped = ungroupedRows?.let { toChildDayRows(it, serviceTimes) }
                                ?: emptyList()
                        )
                    }
            }
        }
    }

    @PostMapping
    fun postReservations(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: List<DailyReservationRequest>
    ) {
        val distinctChildIds = body.map { it.childId }.toSet()
        Audit.AttendanceReservationEmployeeCreate.log(targetId = distinctChildIds.joinToString())
        distinctChildIds.forEach { childId ->
            ac.requirePermissionFor(user, Action.Child.CREATE_ATTENDANCE_RESERVATION, childId)
        }

        db.connect { dbc -> dbc.transaction { createReservationsAsEmployee(it, user.id, body.validate()) } }
    }
}

@ExcludeCodeGen
data class UnitAttendanceReservations(
    val unit: String,
    val operationalDays: List<OperationalDay>,
    val groups: List<GroupAttendanceReservations>,
    val ungrouped: List<ChildDailyRecords>
) {
    data class OperationalDay(
        val date: LocalDate,
        val isHoliday: Boolean
    )

    data class GroupAttendanceReservations(
        val group: ReservationGroup,
        val children: List<ChildDailyRecords>
    )

    data class ReservationGroup(
        @PropagateNull
        val id: GroupId,
        val name: String
    )

    data class ChildDailyRecords(
        val child: Child,
        val dailyData: List<Map<LocalDate, ChildRecordOfDay>>
    )

    data class ChildRecordOfDay(
        val reservation: ReservationTimes?,
        val attendance: AttendanceTimes?,
        val absence: Absence?
    )

    data class ReservationTimes(
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
        val id: ChildId,
        val firstName: String,
        val lastName: String,
        val dateOfBirth: LocalDate,
        val dailyServiceTimes: DailyServiceTimes?
    )

    data class QueryRow(
        val date: LocalDate,
        @Nested("group")
        val group: ReservationGroup?,
        @Nested
        val child: Child,
        @Json
        val reservations: List<ReservationTimes>,
        @Json
        val attendances: List<AttendanceTimes>,
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
        dg.id AS group_id,
        dg.name AS group_name,
        p.id,
        p.first_name,
        p.last_name,
        p.date_of_birth,
        coalesce(res.reservations, '[]') AS reservations,
        coalesce(attendances.attendances, '[]') AS attendances,
        ab.absence_type
    FROM generate_series(:start, :end, '1 day') t
    JOIN placement pl ON daterange(pl.start_date, pl.end_date, '[]') @> t::date
    JOIN person p ON p.id = pl.child_id
    LEFT JOIN backup_care bc ON t::date BETWEEN bc.start_date AND bc.end_date AND p.id = bc.child_id
    LEFT JOIN daycare_group_placement dgp on dgp.daycare_placement_id = pl.id AND daterange(dgp.start_date, dgp.end_date, '[]') @> t::date
    LEFT JOIN daycare_group dg ON dg.id = coalesce(bc.group_id, dgp.daycare_group_id)
    LEFT JOIN LATERAL (
        SELECT
            jsonb_agg(
                jsonb_build_object(
                    'startTime', to_char(att.start_time, 'HH24:MI'),
                    'endTime', to_char(att.end_time, 'HH24:MI')
                ) ORDER BY att.start_time ASC
            ) AS attendances
        FROM child_attendance att WHERE att.child_id = p.id AND att.date = t::date
    ) attendances ON true
    LEFT JOIN LATERAL (
        SELECT
            jsonb_agg(
                jsonb_build_object(
                    'startTime', to_char(ar.start_time, 'HH24:MI'),
                    'endTime', to_char(ar.end_time, 'HH24:MI')
                ) ORDER BY ar.start_time ASC
            ) AS reservations
        FROM attendance_reservation ar WHERE ar.child_id = p.id AND ar.date = t::date
    ) res ON true
    LEFT JOIN LATERAL (
        SELECT absence_type
        FROM absence
        WHERE absence.date = t::date AND absence.child_id = p.id
        LIMIT 1
    ) ab ON true
    WHERE coalesce(bc.unit_id, pl.unit_id) = :unitId
    """.trimIndent()
)
    .bind("unitId", unitId)
    .bind("start", dateRange.start)
    .bind("end", dateRange.end)
    .mapTo<UnitAttendanceReservations.QueryRow>()
    .toList()

// currently queried separately to be able to use existing mapper
private fun Database.Read.getDailyServiceTimes(childIds: Set<ChildId>) = createQuery(
    """
SELECT
    child_id,
    type,
    regular_start,
    regular_end,
    monday_start,
    monday_end,
    tuesday_start,
    tuesday_end,
    wednesday_start,
    wednesday_end,
    thursday_start,
    thursday_end,
    friday_start,
    friday_end,
    saturday_start,
    saturday_end,
    sunday_start,
    sunday_end
FROM daily_service_time
WHERE child_id = ANY(:childIds)
    """.trimIndent()
)
    .bind("childIds", childIds.toTypedArray())
    .map { row -> toDailyServiceTimes(row)?.let { row.mapColumn<ChildId>("child_id") to it } }
    .filterNotNull()
    .toMap()

private fun toChildDayRows(rows: List<UnitAttendanceReservations.QueryRow>, serviceTimes: Map<ChildId, DailyServiceTimes>): List<UnitAttendanceReservations.ChildDailyRecords> {
    return rows
        .groupBy { it.child }
        .map { (child, dailyData) ->
            UnitAttendanceReservations.ChildDailyRecords(
                child = child.copy(dailyServiceTimes = serviceTimes.get(child.id)),
                dailyData = listOfNotNull(
                    dailyData.associateBy(
                        keySelector = { it.date },
                        valueTransform = {
                            UnitAttendanceReservations.ChildRecordOfDay(
                                reservation = it.reservations.getOrNull(0),
                                attendance = it.attendances.getOrNull(0),
                                absence = it.absence
                            )
                        }
                    ),
                    if (dailyData.any { it.reservations.size > 1 || it.attendances.size > 1 }) dailyData.associateBy(
                        keySelector = { it.date },
                        valueTransform = {
                            UnitAttendanceReservations.ChildRecordOfDay(
                                reservation = it.reservations.getOrNull(1),
                                attendance = it.attendances.getOrNull(1),
                                absence = it.absence
                            )
                        }
                    ) else null
                )
            )
        }
}

fun createReservationsAsEmployee(tx: Database.Transaction, userId: UUID, reservations: List<DailyReservationRequest>) {
    tx.clearOldAbsences(reservations.filter { it.reservations != null }.map { it.childId to it.date })
    tx.clearOldReservations(reservations.map { it.childId to it.date })
    tx.insertValidReservations(userId, reservations)
}

private fun Database.Transaction.insertValidReservations(userId: UUID, requests: List<DailyReservationRequest>) {
    val batch = prepareBatch(
        """
        INSERT INTO attendance_reservation (child_id, date, start_time, end_time, created_by)
        SELECT :childId, :date, :start, :end, :userId
        FROM placement pl
        LEFT JOIN backup_care bc ON daterange(bc.start_date, bc.end_date, '[]') @> :date AND bc.child_id = :childId
        JOIN daycare d ON d.id = coalesce(bc.unit_id, pl.unit_id)
        WHERE 
            pl.child_id = :childId AND 
            daterange(pl.start_date, pl.end_date, '[]') @> :date AND 
            extract(isodow FROM :date) = ANY(d.operation_days) AND
            (d.round_the_clock OR NOT EXISTS(SELECT 1 FROM holiday h WHERE h.date = :date)) AND
            NOT EXISTS(SELECT 1 FROM absence ab WHERE ab.child_id = :childId AND ab.date = :date)
        ON CONFLICT DO NOTHING;
        """.trimIndent()
    )

    requests.forEach { request ->
        request.reservations?.forEach { res ->
            batch
                .bind("userId", userId)
                .bind("childId", request.childId)
                .bind("date", request.date)
                .bind("start", res.startTime)
                .bind("end", res.endTime)
                .bind("date", request.date)
                .add()
        }
    }

    batch.execute()
}
