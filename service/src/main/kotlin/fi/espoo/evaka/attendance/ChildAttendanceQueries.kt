// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.dailyservicetimes.toDailyServiceTimes
import fi.espoo.evaka.daycare.service.Absence
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AttendanceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapJsonColumn
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

fun Database.Transaction.insertAttendance(
    childId: ChildId,
    unitId: DaycareId,
    arrived: HelsinkiDateTime,
    departed: HelsinkiDateTime? = null
): ChildAttendance {
    // language=sql
    val sql =
        """
        INSERT INTO child_attendance (child_id, unit_id, arrived, departed)
        VALUES (:childId, :unitId, :arrived, :departed)
        RETURNING *
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("arrived", arrived)
        .bind("departed", departed)
        .mapTo<ChildAttendance>()
        .first()
}

fun Database.Transaction.insertAbsence(
    user: AuthenticatedUser,
    childId: ChildId,
    date: LocalDate,
    category: AbsenceCategory,
    absenceType: AbsenceType
): Absence {
    // language=sql
    val sql =
        """
        INSERT INTO absence (child_id, date, category, absence_type, modified_by)
        VALUES (:childId, :date, :category, :absenceType, :userId)
        RETURNING *
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("date", date)
        .bind("category", category)
        .bind("absenceType", absenceType)
        .bind("userId", user.id)
        .mapTo<Absence>()
        .first()
}

fun Database.Read.getChildAttendance(childId: ChildId, unitId: DaycareId, now: HelsinkiDateTime): ChildAttendance? {
    // language=sql
    val sql =
        """
        SELECT id, child_id, unit_id, arrived, departed
        FROM child_attendance
        WHERE child_id = :childId AND unit_id = :unitId
        AND (arrived::date = :date OR tstzrange(arrived, departed) @> :departedThreshold)
        ORDER BY arrived DESC
        LIMIT 1
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("date", now.toLocalDate())
        .bind("departedThreshold", now.minusMinutes(30))
        .mapTo<ChildAttendance>()
        .firstOrNull()
}

fun Database.Read.getChildOngoingAttendance(childId: ChildId, unitId: DaycareId): ChildAttendance? {
    // language=sql
    val sql =
        """
        SELECT id, child_id, unit_id, arrived, departed
        FROM child_attendance
        WHERE child_id = :childId AND unit_id = :unitId AND departed IS NULL
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .mapTo<ChildAttendance>()
        .firstOrNull()
}

data class ChildBasics(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String?,
    val dateOfBirth: LocalDate,
    val dailyServiceTimes: DailyServiceTimes?,
    val placementType: PlacementType,
    val groupId: GroupId,
    val backup: Boolean,
    val attendance: AttendanceTimes?,
    val absences: List<ChildAbsence>,
    val imageUrl: String?
)
fun Database.Read.fetchChildrenBasics(unitId: DaycareId, instant: HelsinkiDateTime): List<ChildBasics> {
    // language=sql
    val sql =
        """
        WITH child_group_placement AS (
            SELECT
                gp.daycare_group_id as group_id,
                p.child_id,
                p.type as placement_type,
                false AS backup
            FROM daycare_group_placement gp
            JOIN placement p ON p.id = gp.daycare_placement_id
            WHERE
                p.unit_id = :unitId AND daterange(gp.start_date, gp.end_date, '[]') @> :date AND
                NOT EXISTS (
                    SELECT 1
                    FROM backup_care bc
                    WHERE
                        bc.child_id = p.child_id AND
                        daterange(bc.start_date, bc.end_date, '[]') @> :date
                ) AND
                NOT EXISTS (
                    SELECT 1 FROM child_attendance ca
                    WHERE ca.child_id = p.child_id AND ca.departed IS NULL
                )

            UNION ALL

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
                    WHERE ca.child_id = p.child_id AND ca.departed IS NULL
                )

            UNION ALL

            SELECT
                coalesce(bc.group_id, gp.daycare_group_id) as group_id,
                ca.child_id,
                p.type as placement_type,
                bc.group_id IS NOT NULL as backup
            FROM child_attendance ca
            JOIN placement p
                ON ca.child_id = p.child_id
                AND daterange(p.start_date, p.end_date, '[]') @> (ca.arrived at time zone 'Europe/Helsinki')::date
            JOIN daycare_group_placement gp
                ON gp.daycare_placement_id = p.id
                AND daterange(gp.start_date, gp.end_date, '[]') @> (ca.arrived at time zone 'Europe/Helsinki')::date
            LEFT JOIN backup_care bc
                ON ca.child_id = bc.child_id
                AND daterange(bc.start_date, bc.end_date, '[]') @> (ca.arrived at time zone 'Europe/Helsinki')::date
            WHERE ca.departed IS NULL AND ca.unit_id = :unitId
        )
        SELECT
            pe.id,
            pe.first_name,
            pe.last_name,
            pe.preferred_name,
            pe.date_of_birth,
            dst.child_id,
            dst.type,
            dst.regular_start,
            dst.regular_end,
            dst.monday_start,
            dst.monday_end,
            dst.tuesday_start,
            dst.tuesday_end,
            dst.wednesday_start,
            dst.wednesday_end,
            dst.thursday_start,
            dst.thursday_end,
            dst.friday_start,
            dst.friday_end,
            dst.saturday_start,
            dst.saturday_end,
            dst.sunday_start,
            dst.sunday_end,
            cimg.id AS image_id,
            c.group_id,
            c.placement_type,
            c.backup,
            ca.attendance,
            coalesce(a.absences, '[]'::jsonb) AS absences
        FROM child_group_placement c
        JOIN person pe ON pe.id = c.child_id
        LEFT JOIN daily_service_time dst ON dst.child_id = c.child_id
        LEFT JOIN child_images cimg ON pe.id = cimg.child_id
        LEFT JOIN LATERAL (
            SELECT jsonb_build_object('arrived', ca.arrived, 'departed', ca.departed) AS attendance
            FROM child_attendance ca WHERE ca.child_id = pe.id
            AND ((ca.arrived at time zone 'Europe/Helsinki')::date = :date OR tstzrange(arrived, departed) @> :departedThreshold)
            ORDER BY ca.arrived DESC LIMIT 1
        ) ca ON true
        LEFT JOIN LATERAL (
            SELECT jsonb_agg(jsonb_build_object('category', a.category)) AS absences
            FROM absence a WHERE a.child_id = pe.id AND a.date = :date
            GROUP BY a.child_id
        ) a ON true
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bind("date", instant.toLocalDate())
        .bind("departedThreshold", instant.minusMinutes(30))
        .map { row ->
            ChildBasics(
                id = row.mapColumn("id"),
                firstName = row.mapColumn("first_name"),
                lastName = row.mapColumn("last_name"),
                preferredName = row.mapColumn("preferred_name"),
                dateOfBirth = row.mapColumn("date_of_birth"),
                placementType = row.mapColumn("placement_type"),
                dailyServiceTimes = toDailyServiceTimes(row),
                groupId = row.mapColumn("group_id"),
                backup = row.mapColumn("backup"),
                attendance = row.mapJsonColumn("attendance"),
                absences = row.mapJsonColumn("absences"),
                imageUrl = row.mapColumn<String?>("image_id")?.let { id -> "/api/internal/child-images/$id" }
            )
        }
        .list()
}

fun Database.Transaction.updateAttendance(
    attendanceId: AttendanceId,
    arrived: HelsinkiDateTime,
    departed: HelsinkiDateTime?
) {
    // language=sql
    val sql =
        """
        UPDATE child_attendance
        SET arrived = :arrived, departed = :departed
        WHERE id = :id
        """.trimIndent()

    createUpdate(sql)
        .bind("id", attendanceId)
        .bind("arrived", arrived)
        .bind("departed", departed)
        .execute()
}

fun Database.Transaction.updateAttendanceEnd(attendanceId: AttendanceId, departed: HelsinkiDateTime?) {
    // language=sql
    val sql =
        """
        UPDATE child_attendance
        SET departed = :departed
        WHERE id = :id
        """.trimIndent()

    createUpdate(sql)
        .bind("id", attendanceId)
        .bind("departed", departed)
        .execute()
}

fun Database.Transaction.deleteAttendance(id: AttendanceId) {
    // language=sql
    val sql =
        """
        DELETE FROM child_attendance
        WHERE id = :id
        """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .execute()
}

fun Database.Transaction.deleteAbsencesByDate(childId: ChildId, date: LocalDate) {
    // language=sql
    val sql =
        """
        DELETE FROM absence
        WHERE child_id = :childId AND date = :date
        """.trimIndent()

    createUpdate(sql)
        .bind("childId", childId)
        .bind("date", date)
        .execute()
}

fun Database.Transaction.deleteAbsencesByFiniteDateRange(childId: ChildId, dateRange: FiniteDateRange) {
    // language=sql
    val sql =
        """
        DELETE FROM absence
        WHERE child_id = :childId AND between_start_and_end(:dateRange, date)
        """.trimIndent()

    createUpdate(sql)
        .bind("childId", childId)
        .bind("dateRange", dateRange)
        .execute()
}

fun Database.Read.fetchAttendanceReservations(
    unitId: DaycareId,
    date: LocalDate
): Map<ChildId, List<AttendanceReservation>> = createQuery(
    """
    SELECT
        child.id AS child_id,
        to_char((res.start_time AT TIME ZONE 'Europe/Helsinki')::time, 'HH24:MI') AS start_time,
        to_char((res.end_time AT TIME ZONE 'Europe/Helsinki')::time, 'HH24:MI') AS end_time
    FROM attendance_reservation res
    JOIN person child ON res.child_id = child.id
    JOIN placement ON child.id = placement.child_id AND res.start_date BETWEEN placement.start_date AND placement.end_date
    WHERE res.start_date = :date AND placement.unit_id = :unitId
    """.trimIndent()
)
    .bind("unitId", unitId)
    .bind("date", date)
    .map { ctx ->
        ctx.mapColumn<ChildId>("child_id") to AttendanceReservation(
            ctx.mapColumn("start_time"),
            ctx.mapColumn("end_time")
        )
    }
    .groupBy { (childId, _) -> childId }
    .mapValues { it.value.map { (_, reservation) -> reservation } }
