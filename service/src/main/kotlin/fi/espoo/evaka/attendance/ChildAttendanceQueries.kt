// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import java.time.OffsetDateTime
import java.util.UUID

fun Handle.createAttendance(childId: UUID, arrived: OffsetDateTime, departed: OffsetDateTime? = null): ChildAttendance {
    // language=sql
    val sql =
        """
        INSERT INTO child_attendance (child_id, arrived, departed)
        VALUES (:childId, :arrived, :departed)
        RETURNING *
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("arrived", arrived)
        .bind("departed", departed)
        .mapTo<ChildAttendance>()
        .first()
}

fun Handle.updateCurrentAttendanceEnd(childId: UUID, departed: OffsetDateTime) {
    // language=sql
    val sql =
        """
        UPDATE child_attendance
        SET departed = :departed
        WHERE child_id = :childId AND departed IS NULL
        """.trimIndent()

    createUpdate(sql)
        .bind("childId", childId)
        .bind("departed", departed)
        .execute()
}

fun Handle.getChildrenInGroup(groupId: UUID): List<ChildInGroup> {
    // language=sql
    val sql =
        """
        SELECT 
            ch.id AS child_id,
            ch.first_name,
            ch.last_name,
            gp.daycare_group_id,
            ( CASE 
                WHEN EXISTS (
                    SELECT 1 FROM child_attendance ca
                    WHERE ca.child_id = ch.id AND ca.departed IS NULL
                ) THEN 'PRESENT'
                WHEN EXISTS (
                    SELECT 1 FROM child_attendance ca
                    WHERE ca.child_id = ch.id 
                        AND ca.arrived < now()
                        AND ca.departed >= make_timestamptz(
                            extract(year from now())::int, 
                            extract(month from now())::int, 
                            extract(day from now())::int, 
                            0, 0, 0.0
                        )
                ) THEN 'DEPARTED'
                WHEN EXISTS (
                    SELECT 1 FROM absence ab
                    WHERE ab.child_id = ch.id AND ab.date = now()::date AND ab.absence_type != 'PRESENCE'
                ) THEN 'ABSENT'
                ELSE 'COMING'
            END ) AS status,
            ca.arrived,
            ca.departed,
            ca.id AS child_attendance_id
        FROM daycare_group_placement gp
        JOIN placement p ON p.id = gp.daycare_placement_id
        JOIN person ch ON ch.id = p.child_id
        LEFT JOIN (
            SELECT child_id, arrived, departed, id
            FROM child_attendance ca 
            WHERE ca.arrived > current_date) ca 
        ON p.child_id = ca.child_id
        WHERE gp.daycare_group_id = :groupId AND daterange(gp.start_date, gp.end_date, '[]') @> current_date
        """.trimIndent()

    return createQuery(sql)
        .bind("groupId", groupId)
        .mapTo<ChildInGroup>()
        .list()
}

fun Handle.getDaycareAttendances(daycareId: UUID): List<ChildInGroup> {
    // language=sql
    val sql =
        """
        SELECT 
            ch.id AS child_id,
            ch.first_name,
            ch.last_name,
            gp.daycare_group_id,
            ( CASE 
                WHEN EXISTS (
                    SELECT 1 FROM child_attendance ca
                    WHERE ca.child_id = ch.id AND ca.departed IS NULL
                ) THEN 'PRESENT'
                WHEN EXISTS (
                    SELECT 1 FROM child_attendance ca
                    WHERE ca.child_id = ch.id 
                        AND ca.arrived < now()
                        AND ca.departed >= make_timestamptz(
                            extract(year from now())::int, 
                            extract(month from now())::int, 
                            extract(day from now())::int, 
                            0, 0, 0.0
                        )
                ) THEN 'DEPARTED'
                WHEN EXISTS (
                    SELECT 1 FROM absence ab
                    WHERE ab.child_id = ch.id AND ab.date = now()::date AND ab.absence_type != 'PRESENCE'
                ) THEN 'ABSENT'
                ELSE 'COMING'
            END ) AS status,
            ca.arrived,
            ca.departed,
            ca.id AS child_attendance_id
        FROM daycare_group_placement gp
        JOIN placement p ON p.id = gp.daycare_placement_id
        JOIN person ch ON ch.id = p.child_id
        LEFT JOIN (
            SELECT child_id, arrived, departed, id
            FROM child_attendance ca 
            WHERE ca.arrived > current_date) ca 
        ON p.child_id = ca.child_id
        WHERE p.unit_id = :daycareId AND daterange(gp.start_date, gp.end_date, '[]') @> current_date
        """.trimIndent()

    return createQuery(sql)
        .bind("daycareId", daycareId)
        .mapTo<ChildInGroup>()
        .list()
}

fun Handle.deleteAttendance(attendanceId: UUID) = createUpdate(
    // language=sql
    """
        DELETE FROM child_attendance
        WHERE id = :attendanceId
    """.trimIndent()
).bind("attendanceId", attendanceId).execute()
