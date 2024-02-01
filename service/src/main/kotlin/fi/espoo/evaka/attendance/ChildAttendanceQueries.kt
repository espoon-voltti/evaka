// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.dailyservicetimes.DailyServiceTimeRow
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.dailyservicetimes.toDailyServiceTimes
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.ChildAttendanceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime
import org.jdbi.v3.core.mapper.Nested

fun Database.Transaction.insertAttendance(
    childId: ChildId,
    unitId: DaycareId,
    date: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime? = null
): ChildAttendanceId {
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
        .exactlyOne<ChildAttendanceId>()
}

fun Database.Read.getChildAttendanceId(
    childId: ChildId,
    unitId: DaycareId,
    now: HelsinkiDateTime
): ChildAttendanceId? {
    return createQuery<Any> {
            sql(
                """
        SELECT id
        FROM child_attendance
        WHERE child_id = :childId AND unit_id = :unitId
        AND (end_time IS NULL OR (date = :date AND (start_time != '00:00'::time OR (start_time = '00:00'::time AND :departedThreshold < end_time))))
        ORDER BY date, start_time DESC
        LIMIT 1
"""
            )
        }
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("date", now.toLocalDate())
        .bind("departedThreshold", now.toLocalTime().minusMinutes(30))
        .exactlyOneOrNull()
}

fun Database.Read.getCompletedChildAttendanceTimes(
    childId: ChildId,
    unitId: DaycareId,
    date: LocalDate
): List<TimeRange> {
    return createQuery<Any> {
            sql(
                """
        SELECT (start_time, end_time)::timerange
        FROM child_attendance
        WHERE child_id = ${bind(childId)} AND unit_id = ${bind(unitId)} AND date = ${bind(date)} AND end_time IS NOT NULL 
    """
            )
        }
        .toList()
}

data class OngoingAttendance(
    val id: ChildAttendanceId,
    val date: LocalDate,
    val startTime: LocalTime
) {
    fun toTimeRange(departed: HelsinkiDateTime) =
        TimeRange(
            start = if (date.isBefore(departed.toLocalDate())) LocalTime.of(0, 0) else startTime,
            end = departed.toLocalTime()
        )
}

fun Database.Read.getChildOngoingAttendance(
    childId: ChildId,
    unitId: DaycareId
): OngoingAttendance? =
    createQuery(
            "SELECT id, date, start_time FROM child_attendance WHERE child_id = :childId AND unit_id = :unitId AND end_time IS NULL"
        )
        .bind("childId", childId)
        .bind("unitId", unitId)
        .exactlyOneOrNull<OngoingAttendance>()

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

    return createQuery(sql).bind("unitId", unitId).bind("date", now.toLocalDate()).toList {
        row<ChildBasicsRow>().toChildBasics()
    }
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
        .toList<UnitChildAttendancesRow>()
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
    jsonb_agg(jsonb_build_object('category', a.category, 'type', a.absence_type)) AS absences
FROM absence a
WHERE a.child_id = ANY (SELECT child_id FROM placed_children) AND a.date = :date
GROUP BY a.child_id
"""
        )
        .bind("unitId", unitId)
        .bind("date", date)
        .toMap { column<ChildId>("child_id") to jsonColumn<List<ChildAbsence>>("absences") }
}

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
        .toMap { columnPair("child_id", "placement_type") }
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
        .toList<LocalDate>()
}

fun Database.Transaction.unsetAttendanceEndTime(attendanceId: ChildAttendanceId) {
    createUpdate("UPDATE child_attendance SET end_time = NULL WHERE id = :id")
        .bind("id", attendanceId)
        .execute()
}

fun Database.Transaction.updateAttendanceEnd(attendanceId: ChildAttendanceId, endTime: LocalTime) {
    createUpdate("UPDATE child_attendance SET end_time = :endTime WHERE id = :id")
        .bind("id", attendanceId)
        .bind("endTime", endTime)
        .execute()
}

fun Database.Transaction.deleteAttendance(id: ChildAttendanceId) {
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
        .toList<AbsenceId>()
}

fun Database.Transaction.deleteAttendancesByDate(
    childId: ChildId,
    date: LocalDate
): List<ChildAttendanceId> =
    createUpdate(
            "DELETE FROM child_attendance WHERE child_id = :childId AND date = :date RETURNING id"
        )
        .bind("childId", childId)
        .bind("date", date)
        .executeAndReturnGeneratedKeys()
        .toList<ChildAttendanceId>()

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
        .toList<AbsenceId>()
}

fun Database.Read.childrenHaveAttendanceInRange(
    childIds: Set<PersonId>,
    range: FiniteDateRange
): Boolean {
    return createQuery<Any> {
            sql(
                """
            SELECT EXISTS(SELECT FROM child_attendance WHERE child_id = any(${bind(childIds)}) AND ${bind(range)} @> date)
            """
            )
        }
        .exactlyOne()
}

fun Database.Read.getChildAttendances(
    where: Predicate<DatabaseTable.ChildAttendance>
): List<ChildAttendanceRow> =
    createQuery<Any> {
            sql(
                """
SELECT child_id, unit_id, date, start_time, end_time
FROM child_attendance ca
WHERE ${predicate(where.forTable("ca"))}
"""
            )
        }
        .toList()

fun Database.Read.getChildAttendancesCitizen(
    today: LocalDate,
    guardianId: PersonId,
    range: FiniteDateRange
): List<ChildAttendanceRow> =
    getChildAttendances(
        Predicate {
            where(
                """
between_start_and_end(${bind(range)}, $it.date) AND
$it.child_id IN (
    SELECT child_id FROM guardian WHERE guardian_id = ${bind(guardianId)}
    UNION ALL
    SELECT child_id FROM foster_parent WHERE parent_id = ${bind(guardianId)} AND valid_during @> ${bind(today)}
)
"""
            )
        }
    )
