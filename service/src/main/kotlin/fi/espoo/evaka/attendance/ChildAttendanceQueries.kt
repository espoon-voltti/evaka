// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.daycare.service.Absence
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.utils.zoneId
import org.jdbi.v3.core.kotlin.mapTo
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun Database.Transaction.insertAttendance(childId: UUID, unitId: UUID, arrived: Instant, departed: Instant? = null): ChildAttendance {
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

fun Database.Transaction.insertAbsence(user: AuthenticatedUser, childId: UUID, date: LocalDate, careType: CareType, absenceType: AbsenceType): Absence {
    // language=sql
    val sql =
        """
        INSERT INTO absence (child_id, date, care_type, absence_type, modified_by)
        VALUES (:childId, :date, :careType, :absenceType, :userId)
        RETURNING *
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("date", date)
        .bind("careType", careType)
        .bind("absenceType", absenceType)
        .bind("userId", user.id)
        .mapTo<Absence>()
        .first()
}

fun Database.Read.getChildCurrentDayAttendance(childId: UUID, unitId: UUID): ChildAttendance? {
    // language=sql
    val sql =
        """
        SELECT id, child_id, unit_id, arrived, departed
        FROM child_attendance
        WHERE child_id = :childId AND unit_id = :unitId AND tstzrange(:todayStart, :todayEnd, '[)') @> arrived
        ORDER BY arrived DESC
        LIMIT 1
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("todayStart", LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant())
        .bind("todayEnd", LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant())
        .mapTo<ChildAttendance>()
        .list()
        .firstOrNull()
}

fun Database.Read.fetchUnitInfo(unitId: UUID): UnitInfo {
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
        .bind("today", LocalDate.now(zoneId))
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

fun Database.Read.fetchChildrenBasics(unitId: UUID): List<ChildBasics> {
    // language=sql
    val sql =
        """
        SELECT 
            ch.id,
            ch.first_name,
            ch.last_name,
            ch.date_of_birth,
            gp.daycare_group_id as group_id,
            p.type as placement_type,
            false AS backup
        FROM daycare_group_placement gp
        JOIN placement p ON p.id = gp.daycare_placement_id
        JOIN person ch ON ch.id = p.child_id
        WHERE p.unit_id = :unitId AND daterange(gp.start_date, gp.end_date, '[]') @> :today AND NOT EXISTS (
            SELECT 1 FROM backup_care bc WHERE bc.child_id = p.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> :today
        )
        
        UNION ALL 
        
        SELECT 
            ch.id,
            ch.first_name,
            ch.last_name,
            ch.date_of_birth,
            bc.group_id,
            p.type as placement_type,
            true AS backup
        FROM backup_care bc
        JOIN placement p ON p.child_id = bc.child_id AND daterange(p.start_date, p.end_date, '[]') @> :today
        JOIN person ch ON ch.id = p.child_id
        WHERE bc.unit_id = :unitId AND bc.group_id IS NOT NULL AND daterange(bc.start_date, bc.end_date, '[]') @> :today
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bind("today", LocalDate.now(zoneId))
        .mapTo<ChildBasics>()
        .list()
}

// language=sql
private val placedChildrenSql =
    """
    SELECT p.child_id
    FROM placement p
    WHERE p.unit_id = :unitId AND daterange(p.start_date, p.end_date, '[]') @> :today AND NOT EXISTS (
        SELECT 1 FROM backup_care bc WHERE bc.child_id = p.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> :today
    )
    
    UNION ALL
    
    SELECT bc.child_id
    FROM backup_care bc
    JOIN placement p ON p.child_id = bc.child_id AND daterange(p.start_date, p.end_date, '[]') @> :today
    WHERE bc.unit_id = :unitId AND daterange(bc.start_date, bc.end_date, '[]') @> :today    
    """.trimIndent()

fun Database.Read.fetchChildrenAttendances(unitId: UUID): List<ChildAttendance> {
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
        WHERE (ca.departed IS NULL OR ca.departed::date = :today) AND ca.child_id IN ($placedChildrenSql)
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bind("today", LocalDate.now(zoneId))
        .mapTo<ChildAttendance>()
        .list()
}

fun Database.Read.fetchChildrenAbsences(unitId: UUID): List<ChildAbsence> {
    // language=sql
    val sql =
        """
        SELECT 
            ab.id,
            ab.child_id,
            ab.care_type
        FROM absence ab
        WHERE ab.date = :today AND ab.absence_type != 'PRESENCE' AND ab.child_id IN ($placedChildrenSql)
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bind("today", LocalDate.now(zoneId))
        .mapTo<ChildAbsence>()
        .list()
}

fun Database.Transaction.updateAttendance(attendanceId: UUID, arrived: Instant, departed: Instant?) {
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

fun Database.Transaction.updateAttendanceEnd(attendanceId: UUID, departed: Instant?) {
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

fun Database.Transaction.deleteAttendance(id: UUID) {
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

fun Database.Transaction.deleteCurrentDayAbsences(childId: UUID) {
    // language=sql
    val sql =
        """
        DELETE FROM absence
        WHERE child_id = :childId AND date = :today
        """.trimIndent()

    createUpdate(sql)
        .bind("childId", childId)
        .bind("today", LocalDate.now(zoneId))
        .execute()
}
