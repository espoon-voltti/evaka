// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
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

@RestController
@RequestMapping("/attendance-reservations")
class AttendanceReservationController(private val ac: AccessControl) {
    @GetMapping
    fun getAttendanceReservations(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate
    ): UnitAttendanceReservations {
        Audit.UnitAttendanceReservations.log(targetId = unitId, objectId = from)
        ac.requirePermissionFor(user, Action.Unit.READ_ATTENDANCE_RESERVATIONS, unitId)
        val dateRange = FiniteDateRange(from, from.plusDays(6))
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
                                children = mapChildReservations(rows)
                            )
                        },
                        ungrouped = mapChildReservations(ungroupedRows)
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
            res.start_time,
            res.end_time,
            res.start_date,
            group_placement.daycare_group_id AS group_id
        FROM person child
        JOIN placement ON child.id = placement.child_id AND daterange(placement.start_date, placement.end_date, '[]') && :dateRange
        LEFT JOIN attendance_reservation res ON child.id = res.child_id AND res.start_date BETWEEN placement.start_date AND placement.end_date
        LEFT JOIN daycare_group_placement group_placement ON placement.id = group_placement.daycare_placement_id
            AND daterange(placement.start_date, placement.end_date, '[]') && daterange(group_placement.start_date, group_placement.end_date, '[]')
    )
    SELECT
        unit_group.unit_name AS unit,
        unit_group.group_name AS group,
        group_res.id,
        group_res.first_name,
        group_res.last_name,
        group_res.date_of_birth,
        group_res.start_date AS date,
        to_char(group_res.start_time::time, 'HH24:MI') AS start_time,
        to_char(group_res.end_time::time, 'HH24:MI') AS end_time
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
        val reservations: Map<LocalDate, Reservation>
    )

    data class Reservation(
        val startTime: String,
        val endTime: String
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
        @Nested
        val reservation: QueryRowReservation?
    )

    data class QueryRowReservation(
        @PropagateNull
        val date: LocalDate,
        val startTime: String,
        val endTime: String
    )

    data class OperationalDay(
        val date: LocalDate,
        val isHoliday: Boolean
    )
}

private fun mapChildReservations(
    data: List<UnitAttendanceReservations.QueryRow>
): List<UnitAttendanceReservations.ChildReservations> = data
    .mapNotNull { row -> row.child?.let { it to row.reservation } }
    .groupBy { (child, _) -> child }
    .map { (child, reservations) ->
        UnitAttendanceReservations.ChildReservations(
            child = child,
            reservations = reservations
                .mapNotNull { (_, reservation) -> reservation }
                .associate { it.date to UnitAttendanceReservations.Reservation(it.startTime, it.endTime) }
        )
    }
