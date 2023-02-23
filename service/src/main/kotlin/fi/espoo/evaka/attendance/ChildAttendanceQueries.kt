// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.dailyservicetimes.DailyServiceTimeRow
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.dailyservicetimes.toDailyServiceTimes
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json

fun Database.Transaction.insertAttendance(
    childId: ChildId,
    unitId: DaycareId,
    date: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime? = null
): AttendanceId {
    // language=sql
    val sql =
        """
        INSERT INTO child_attendance (child_id, unit_id, date, start_time, end_time)
        VALUES (:childId, :unitId, :date, :startTime, :endTime)
        RETURNING id
        """
            .trimIndent()

    return createUpdate(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("date", date)
        .bind("startTime", startTime.withSecond(0).withNano(0))
        .bind("endTime", endTime?.withSecond(0)?.withNano(0))
        .executeAndReturnGeneratedKeys()
        .mapTo<AttendanceId>()
        .single()
}

fun Database.Read.getChildAttendance(
    childId: ChildId,
    unitId: DaycareId,
    now: HelsinkiDateTime
): ChildAttendance? {
    // language=sql
    val sql =
        """
        SELECT id, child_id, unit_id, (date + start_time) AT TIME ZONE 'Europe/Helsinki' AS arrived, (date + end_time) AT TIME ZONE 'Europe/Helsinki' AS departed
        FROM child_attendance
        WHERE child_id = :childId AND unit_id = :unitId
        AND (end_time IS NULL OR (date = :date AND (start_time != '00:00'::time OR (start_time = '00:00'::time AND :departedThreshold < end_time))))
        ORDER BY date, start_time DESC
        LIMIT 1
        """
            .trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("date", now.toLocalDate())
        .bind("departedThreshold", now.toLocalTime().minusMinutes(30))
        .mapTo<ChildAttendance>()
        .firstOrNull()
}

data class OngoingAttendance(val id: AttendanceId, val date: LocalDate, val startTime: LocalTime)

fun Database.Read.getChildOngoingAttendance(
    childId: ChildId,
    unitId: DaycareId
): OngoingAttendance? =
    createQuery(
            "SELECT id, date, start_time FROM child_attendance WHERE child_id = :childId AND unit_id = :unitId AND end_time IS NULL"
        )
        .bind("childId", childId)
        .bind("unitId", unitId)
        .mapTo<OngoingAttendance>()
        .firstOrNull()

data class ChildBasics(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val dateOfBirth: LocalDate,
    val dailyServiceTimes: DailyServiceTimes?,
    val placementType: PlacementType,
    val groupId: GroupId?,
    val backup: Boolean,
    val imageUrl: String?
)

data class ChildBasicsRow(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val dateOfBirth: LocalDate,
    @Nested("dst") val dailyServiceTimes: DailyServiceTimeRow?,
    val placementType: PlacementType,
    val groupId: GroupId?,
    val backup: Boolean,
    val imageId: String?
) {
    fun toChildBasics() =
        ChildBasics(
            id,
            firstName,
            lastName,
            preferredName,
            dateOfBirth,
            dailyServiceTimes = dailyServiceTimes?.let { toDailyServiceTimes(it) },
            placementType,
            groupId,
            backup,
            imageUrl = imageId?.let { id -> "/api/internal/child-images/$id" }
        )
}

fun Database.Read.fetchChildrenBasics(unitId: DaycareId, now: HelsinkiDateTime): List<ChildBasics> {
    // language=sql
    val sql =
        """
        WITH child_group_placement AS (
            -- Placed to unit
            SELECT
                gp.daycare_group_id as group_id,
                p.child_id,
                p.type as placement_type,
                false AS backup
            FROM daycare_group_placement gp
            JOIN placement p ON p.id = gp.daycare_placement_id
            WHERE
                p.unit_id = :unitId AND 
                daterange(p.start_date, p.end_date, '[]') * daterange(gp.start_date, gp.end_date, '[]') @> :date AND
                NOT EXISTS (
                    SELECT 1
                    FROM backup_care bc
                    WHERE
                        bc.child_id = p.child_id AND
                        daterange(bc.start_date, bc.end_date, '[]') @> :date
                ) AND
                NOT EXISTS (
                    SELECT 1 FROM child_attendance ca
                    WHERE ca.child_id = p.child_id AND ca.end_time IS NULL
                )

            UNION ALL

            -- Placed to unit as backup
            SELECT
                bc.group_id,
                p.child_id,
                p.type as placement_type,
                true AS backup
            FROM backup_care bc
            JOIN placement p ON (
                p.child_id = bc.child_id AND
                daterange(p.start_date, p.end_date, '[]') @> :date
            )
            WHERE
                bc.unit_id = :unitId AND
                bc.group_id IS NOT NULL AND
                daterange(bc.start_date, bc.end_date, '[]') @> :date AND
                NOT EXISTS (
                    SELECT 1 FROM child_attendance ca
                    WHERE ca.child_id = p.child_id AND ca.end_time IS NULL
                )

            UNION ALL

            -- Attendance in the unit
            SELECT
                (CASE WHEN bc.id IS NOT NULL THEN bc.group_id ELSE gp.daycare_group_id END) as group_id,
                ca.child_id,
                p.type as placement_type,
                bc.id IS NOT NULL as backup
            FROM child_attendance ca
            JOIN placement p
                ON ca.child_id = p.child_id
                AND daterange(p.start_date, p.end_date, '[]') @> ca.date
            LEFT JOIN daycare_group_placement gp
                ON gp.daycare_placement_id = p.id
                AND daterange(gp.start_date, gp.end_date, '[]') @> ca.date
            LEFT JOIN backup_care bc
                ON ca.child_id = bc.child_id
                AND daterange(bc.start_date, bc.end_date, '[]') @> ca.date
                AND bc.unit_id = ca.unit_id
            WHERE ca.end_time IS NULL AND ca.unit_id = :unitId
        )
        SELECT
            pe.id,
            pe.first_name,
            pe.last_name,
            pe.preferred_name,
            pe.date_of_birth,
            dst.id AS dst_id,
            dst.child_id AS dst_child_id,
            dst.type AS dst_type,
            dst.regular_times AS dst_regular_times,
            dst.monday_times AS dst_monday_times,
            dst.tuesday_times AS dst_tuesday_times,
            dst.wednesday_times AS dst_wednesday_times,
            dst.thursday_times AS dst_thursday_times,
            dst.friday_times AS dst_friday_times,
            dst.saturday_times AS dst_saturday_times,
            dst.sunday_times AS dst_sunday_times,
            dst.validity_period AS dst_validity_period,
            cimg.id AS image_id,
            c.group_id,
            c.placement_type,
            c.backup
        FROM child_group_placement c
        JOIN person pe ON pe.id = c.child_id
        LEFT JOIN daily_service_time dst ON dst.child_id = c.child_id AND dst.validity_period @> :date
        LEFT JOIN child_images cimg ON pe.id = cimg.child_id
        """
            .trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bind("date", now.toLocalDate())
        .mapTo<ChildBasicsRow>()
        .map { it.toChildBasics() }
        .list()
}

private data class UnitChildAttendancesRow(
    val childId: ChildId,
    val arrived: HelsinkiDateTime,
    var departed: HelsinkiDateTime?
)

fun Database.Read.getUnitChildAttendances(
    unitId: DaycareId,
    now: HelsinkiDateTime,
): Map<ChildId, List<AttendanceTimes>> {
    return createQuery(
            """
SELECT
    child_id,
    (ca.date + ca.start_time) AT TIME ZONE 'Europe/Helsinki' AS arrived,
    (ca.date + ca.end_time) AT TIME ZONE 'Europe/Helsinki' AS departed
FROM child_attendance ca
WHERE
    ca.unit_id = :unitId AND (
        ca.end_time IS NULL OR
        (ca.date = :today AND ca.start_time != '00:00'::time) OR
        -- An overnight attendance is included for 30 minutes after child has departed
        (ca.date = :today AND ca.start_time = '00:00'::time AND :departedThreshold < ca.end_time) OR
        (ca.date = :today - 1 AND ca.end_time = '23:59'::time)
    )
"""
        )
        .bind("unitId", unitId)
        .bind("today", now.toLocalDate())
        .bind("departedThreshold", now.toLocalTime().minusMinutes(30))
        .mapTo<UnitChildAttendancesRow>()
        .groupBy { it.childId }
        .mapValues {
            it.value
                .sortedByDescending { att -> att.arrived }
                .fold(listOf<AttendanceTimes>()) { result, attendance ->
                    // Merge overnight attendances as one
                    val departed = attendance.departed
                    if (
                        result.isNotEmpty() &&
                            departed != null &&
                            departed.hour == 23 &&
                            departed.minute == 59 &&
                            result.isNotEmpty()
                    ) {
                        result.dropLast(1) + result.last().copy(arrived = attendance.arrived)
                    } else {
                        result + AttendanceTimes(attendance.arrived, departed)
                    }
                }
                .filterNot { att ->
                    // Remove yesterday's stray attendance that wasn't merged because the child
                    // departed > 30 minutes ago
                    att.departed != null && att.departed.toLocalDate() != now.toLocalDate()
                }
        }
        .filter { it.value.isNotEmpty() }
}

private data class UnitChildAbsencesRow(
    val childId: ChildId,
    @Json val absences: List<ChildAbsence>
)

fun Database.Read.getUnitChildAbsences(
    unitId: DaycareId,
    date: LocalDate,
): Map<ChildId, List<ChildAbsence>> {
    return createQuery(
            """
WITH placed_children AS (
    SELECT child_id FROM placement WHERE unit_id = :unitId AND :date BETWEEN start_date AND end_date
    UNION
    SELECT child_id FROM backup_care WHERE unit_id = :unitId AND :date BETWEEN start_date AND end_date
)
SELECT 
    a.child_id, 
    jsonb_agg(jsonb_build_object('category', a.category)) AS absences
FROM absence a
WHERE a.child_id = ANY (SELECT child_id FROM placed_children) AND a.date = :date
GROUP BY a.child_id
"""
        )
        .bind("unitId", unitId)
        .bind("date", date)
        .mapTo<UnitChildAbsencesRow>()
        .associateBy { it.childId }
        .mapValues { it.value.absences }
}

private data class ChildPlacementTypeRow(val childId: ChildId, val placementType: PlacementType)

fun Database.Read.getChildPlacementTypes(
    childIds: Set<ChildId>,
    today: LocalDate
): Map<ChildId, PlacementType> {
    return createQuery(
            """
SELECT child_id, type AS placement_type
FROM placement p
WHERE p.child_id = ANY(:childIds) AND :today BETWEEN p.start_date AND p.end_date
"""
        )
        .bind("childIds", childIds)
        .bind("today", today)
        .mapTo<ChildPlacementTypeRow>()
        .associateBy { it.childId }
        .mapValues { it.value.placementType }
}

fun Database.Read.getChildAttendanceStartDatesByRange(
    childId: ChildId,
    period: DateRange
): List<LocalDate> {
    return createQuery(
            """
        SELECT date
        FROM child_attendance
        WHERE between_start_and_end(:period, date)
        AND child_id = :childId
        AND start_time != '00:00'::time  -- filter out overnight stays
    """
        )
        .bind("period", period)
        .bind("childId", childId)
        .mapTo<LocalDate>()
        .list()
}

fun Database.Transaction.unsetAttendanceEndTime(attendanceId: AttendanceId) {
    createUpdate("UPDATE child_attendance SET end_time = NULL WHERE id = :id")
        .bind("id", attendanceId)
        .execute()
}

fun Database.Transaction.updateAttendanceEnd(attendanceId: AttendanceId, endTime: LocalTime) {
    createUpdate("UPDATE child_attendance SET end_time = :endTime WHERE id = :id")
        .bind("id", attendanceId)
        .bind("endTime", endTime)
        .execute()
}

fun Database.Transaction.deleteAttendance(id: AttendanceId) {
    // language=sql
    val sql =
        """
        DELETE FROM child_attendance
        WHERE id = :id
        """
            .trimIndent()

    createUpdate(sql).bind("id", id).execute()
}

fun Database.Transaction.deleteAbsencesByDate(childId: ChildId, date: LocalDate): List<AbsenceId> {
    // language=sql
    val sql =
        """
        DELETE FROM absence
        WHERE child_id = :childId AND date = :date
        RETURNING id
        """
            .trimIndent()

    return createUpdate(sql)
        .bind("childId", childId)
        .bind("date", date)
        .executeAndReturnGeneratedKeys()
        .mapTo<AbsenceId>()
        .toList()
}

fun Database.Transaction.deleteAttendancesByDate(
    childId: ChildId,
    date: LocalDate
): List<AttendanceId> =
    createUpdate(
            "DELETE FROM child_attendance WHERE child_id = :childId AND date = :date RETURNING id"
        )
        .bind("childId", childId)
        .bind("date", date)
        .executeAndReturnGeneratedKeys()
        .mapTo<AttendanceId>()
        .toList()

fun Database.Transaction.deleteAbsencesByFiniteDateRange(
    childId: ChildId,
    dateRange: FiniteDateRange
): List<AbsenceId> {
    // language=sql
    val sql =
        """
        DELETE FROM absence
        WHERE child_id = :childId AND between_start_and_end(:dateRange, date)
        RETURNING id
        """
            .trimIndent()

    return createUpdate(sql)
        .bind("childId", childId)
        .bind("dateRange", dateRange)
        .executeAndReturnGeneratedKeys()
        .mapTo<AbsenceId>()
        .toList()
}

fun Database.Read.fetchAttendanceReservations(
    unitId: DaycareId,
    now: HelsinkiDateTime
): Map<ChildId, List<AttendanceReservation>> =
    createQuery(
            """
    WITH placed_children AS (
        SELECT child_id FROM placement WHERE unit_id = :unitId AND :date BETWEEN start_date AND end_date
        UNION
        SELECT child_id FROM backup_care WHERE unit_id = :unitId AND :date BETWEEN start_date AND end_date
    )
    SELECT
        child.id AS child_id,
        coalesce(real_start.date, res.date) AS start_date,
        coalesce(real_start.start_time, res.start_time) AS start_time,
        coalesce(real_end.date, res.date) AS end_date,
        coalesce(real_end.end_time, res.end_time) AS end_time
    FROM placed_children pc
    JOIN attendance_reservation res ON res.child_id = pc.child_id AND res.date = :date
    JOIN person child ON res.child_id = child.id
    LEFT JOIN attendance_reservation real_start ON res.start_time = '00:00'::time AND res.child_id = real_start.child_id AND real_start.end_time = '23:59'::time AND res.date = real_start.date + 1
    LEFT JOIN attendance_reservation real_end ON res.end_time = '23:59'::time AND res.child_id = real_end.child_id AND real_end.start_time = '00:00'::time AND res.date = real_end.date - 1
    """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("date", now.toLocalDate())
        .bind("time", now.toLocalTime())
        .map { ctx ->
            ctx.mapColumn<ChildId>("child_id") to
                AttendanceReservation(
                    HelsinkiDateTime.of(ctx.mapColumn("start_date"), ctx.mapColumn("start_time")),
                    HelsinkiDateTime.of(ctx.mapColumn("end_date"), ctx.mapColumn("end_time"))
                )
        }
        .groupBy { (childId, _) -> childId }
        .mapValues { it.value.map { (_, reservation) -> reservation } }

fun Database.Read.getChildAttendanceReservationStartDatesByRange(
    childId: ChildId,
    period: DateRange
): List<LocalDate> {
    return createQuery(
            """
        SELECT date
        FROM attendance_reservation
        WHERE between_start_and_end(:period, date)
        AND child_id = :childId
        AND start_time != '00:00'::time  -- filter out overnight reservations
        """
        )
        .bind("period", period)
        .bind("childId", childId)
        .mapTo<LocalDate>()
        .list()
}
