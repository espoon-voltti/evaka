// SPDX-FileCopyrightText: 2017-2022 City of Espoo
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
import fi.espoo.evaka.shared.db.mapRow
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

        db.connect { dbc -> dbc.transaction { createReservations(it, user.evakaUserId, body.validate()) } }
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
        val absence: Absence?,
        val dailyServiceTimes: DailyServiceTimes?,
        val inOtherUnit: Boolean
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
        val dateOfBirth: LocalDate
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
        val absence: Absence?,
        val inOtherUnit: Boolean
    )
}

private fun Database.Read.getUnitOperationalDays(unitId: DaycareId, dateRange: FiniteDateRange) = createQuery(
    """
    SELECT t::date AS date, holiday.date IS NOT NULL AS is_holiday
    FROM generate_series(:start, :end, '1 day') t
    JOIN daycare ON daycare.id = :unitId AND date_part('isodow', t) = ANY(daycare.operation_days)
    LEFT JOIN holiday ON t = holiday.date AND NOT round_the_clock
    """.trimIndent()
)
    .bind("unitId", unitId)
    .bind("start", dateRange.start)
    .bind("end", dateRange.end)
    .mapTo<UnitAttendanceReservations.OperationalDay>()
    .toList()

private fun Database.Read.getAttendanceReservationData(unitId: DaycareId, dateRange: FiniteDateRange): List<UnitAttendanceReservations.QueryRow> {
    val inOtherUnit = "rp.placement_unit_id <> rp.unit_id AND rp.placement_unit_id = :unitId"
    return createQuery(
        """
        SELECT 
            t::date AS date,
            dg.id AS group_id,
            dg.name AS group_name,
            p.id,
            p.first_name,
            p.last_name,
            p.date_of_birth,
            (CASE WHEN ($inOtherUnit)
                THEN '[]'
                ELSE coalesce(res.reservations, '[]') END
            ) AS reservations,
            (CASE WHEN ($inOtherUnit)
                THEN '[]'
                ELSE coalesce(attendances.attendances, '[]') END
            ) AS attendances,
            ab.absence_type,
            $inOtherUnit AS in_other_unit
        FROM generate_series(:start, :end, '1 day') t
        JOIN realized_placement_one(t::date) rp ON true
        JOIN person p ON rp.child_id = p.id
        LEFT JOIN daycare_group dg ON dg.id = (CASE WHEN ($inOtherUnit)
            THEN rp.placement_group_id
            ELSE rp.group_id END
        )
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
        WHERE rp.unit_id = :unitId OR rp.placement_unit_id = :unitId
        """.trimIndent()
    )
        .bind("unitId", unitId)
        .bind("start", dateRange.start)
        .bind("end", dateRange.end)
        .mapTo<UnitAttendanceReservations.QueryRow>()
        .toList()
}

// currently queried separately to be able to use existing mapper
private fun Database.Read.getDailyServiceTimes(childIds: Set<ChildId>) = createQuery(
    """
SELECT
    id,
    child_id,
    type,
    regular_times,
    monday_times,
    tuesday_times,
    wednesday_times,
    thursday_times,
    friday_times,
    saturday_times,
    sunday_times,
    validity_period
FROM daily_service_time
WHERE child_id = ANY(:childIds)
    """.trimIndent()
)
    .bind("childIds", childIds.toTypedArray())
    .map { row -> row.mapColumn<ChildId>("child_id") to toDailyServiceTimes(row.mapRow()).times }
    .toList()
    .groupBy { it.first }
    .mapValues { it.value.map { it.second } }

private fun toChildDayRows(rows: List<UnitAttendanceReservations.QueryRow>, serviceTimes: Map<ChildId, List<DailyServiceTimes>>): List<UnitAttendanceReservations.ChildDailyRecords> {
    return rows
        .groupBy { it.child }
        .map { (child, dailyData) ->
            UnitAttendanceReservations.ChildDailyRecords(
                child = child,
                dailyData = listOfNotNull(
                    dailyData.associateBy(
                        keySelector = { it.date },
                        valueTransform = { day ->
                            UnitAttendanceReservations.ChildRecordOfDay(
                                reservation = day.reservations.getOrNull(0),
                                attendance = day.attendances.getOrNull(0),
                                absence = day.absence,
                                dailyServiceTimes = serviceTimes[child.id]?.find { it.validityPeriod.includes(day.date) },
                                inOtherUnit = day.inOtherUnit
                            )
                        }
                    ),
                    if (dailyData.any { it.reservations.size > 1 || it.attendances.size > 1 }) dailyData.associateBy(
                        keySelector = { it.date },
                        valueTransform = { day ->
                            UnitAttendanceReservations.ChildRecordOfDay(
                                reservation = day.reservations.getOrNull(1),
                                attendance = day.attendances.getOrNull(1),
                                absence = day.absence,
                                dailyServiceTimes = serviceTimes[child.id]?.find { it.validityPeriod.includes(day.date) },
                                inOtherUnit = day.inOtherUnit
                            )
                        }
                    ) else null
                )
            )
        }
}
