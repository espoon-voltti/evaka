// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

fun Handle.insertAttendance(childId: UUID, unitId: UUID, arrived: Instant, departed: Instant? = null): ChildAttendance {
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

fun Handle.getCurrentAttendance(childId: UUID, unitId: UUID): ChildAttendance? {
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
        .list()
        .firstOrNull()
}

fun Handle.updateAttendance(attendanceId: UUID, arrived: Instant, departed: Instant?) {
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

fun Handle.updateCurrentAttendanceEnd(childId: UUID, unitId: UUID, departed: Instant): Boolean {
    // language=sql
    val sql =
        """
        UPDATE child_attendance
        SET departed = :departed
        WHERE child_id = :childId AND unit_id = :unitId AND departed IS NULL
        """.trimIndent()

    return createUpdate(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("departed", departed)
        .execute() > 0
}

fun Handle.fetchUnitInfo(unitId: UUID): UnitInfo {
    data class UnitBasics(
        val id: UUID,
        val name: String
    )
    // language=sql
    val unitSql =
        """
        SELECT u.id, u.name
        FROM daycare u
        WHERE u.id = :unitId
        """.trimIndent()

    val unit = createQuery(unitSql)
        .bind("unitId", unitId)
        .mapTo<UnitBasics>()
        .list().firstOrNull() ?: throw NotFound("Unit $unitId not found")

    // language=sql
    val groupsSql =
        """
        SELECT g.id, g.name
        FROM daycare u
        JOIN daycare_group g on u.id = g.daycare_id AND daterange(g.start_date, g.end_date, '[]') @> :today
        WHERE u.id = :unitId
        """.trimIndent()

    return createQuery(groupsSql)
        .bind("unitId", unitId)
        .bind("today", LocalDate.now(ZoneId.of("Europe/Helsinki")))
        .mapTo<GroupInfo>()
        .list()
        .let { groups ->
            UnitInfo(
                id = unit.id,
                name = unit.name,
                groups = groups
            )
        }
}

fun Handle.fetchChildrenBasics(unitId: UUID): List<ChildBasics> {
    // language=sql
    val sql =
        """
        SELECT 
            ch.id,
            ch.first_name,
            ch.last_name,
            gp.daycare_group_id as group_id,
            p.type as placement_type
        FROM daycare_group_placement gp
        JOIN placement p ON p.id = gp.daycare_placement_id
        JOIN person ch ON ch.id = p.child_id
        WHERE p.unit_id = :unitId AND daterange(gp.start_date, gp.end_date, '[]') @> :today
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bind("today", LocalDate.now(ZoneId.of("Europe/Helsinki")))
        .mapTo<ChildBasics>()
        .list()
}

fun Handle.fetchChildrenAttendances(unitId: UUID): List<ChildAttendance> {
    // language=sql
    val sql =
        """
        SELECT 
            ca.id,
            ca.child_id,
            ca.unit_id,
            ca.arrived,
            ca.departed
        FROM child_attendance ca
        JOIN placement p ON ca.child_id = p.child_id AND daterange(p.start_date, p.end_date, '[]') @> :today
        WHERE p.unit_id = :unitId AND ca.departed IS NULL OR ca.departed::date = :today
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bind("today", LocalDate.now(ZoneId.of("Europe/Helsinki")))
        .mapTo<ChildAttendance>()
        .list()
}

fun Handle.fetchChildrenAbsences(unitId: UUID): List<ChildAbsence> {
    // language=sql
    val sql =
        """
        SELECT 
            ab.id,
            ab.child_id,
            ab.absence_type,
            ab.care_type
        FROM absence ab
        JOIN placement p ON ab.child_id = p.child_id AND daterange(p.start_date, p.end_date, '[]') @> :today
        WHERE p.unit_id = :unitId AND ab.date = :today AND ab.absence_type != 'PRESENCE'
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bind("today", LocalDate.now(ZoneId.of("Europe/Helsinki")))
        .mapTo<ChildAbsence>()
        .list()
}

fun Handle.deleteAttendance(attendanceId: UUID) = createUpdate(
    // language=sql
    """
        DELETE FROM child_attendance
        WHERE id = :attendanceId
    """.trimIndent()
).bind("attendanceId", attendanceId).execute()
