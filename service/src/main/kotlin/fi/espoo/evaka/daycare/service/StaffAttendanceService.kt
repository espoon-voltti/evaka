// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.daycare.dao.PGConstants.maxDate
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.util.UUID

@Service
class StaffAttendanceService {
    fun getAttendancesByMonth(db: Database, year: Int, month: Int, groupId: UUID): StaffAttendanceGroup {
        val rangeStart = LocalDate.of(year, month, 1)
        val rangeEnd = rangeStart.with(lastDayOfMonth())

        return db.read { tx ->
            val groupInfo = getGroupInfo(groupId, tx.handle) ?: throw BadRequest("Couldn't find group info with id $groupId")
            val endDate = groupInfo.endDate.let { if (it.isBefore(maxDate)) it else null }

            val attendanceList = getStaffAttendanceByRange(rangeStart, rangeEnd, groupId, tx.handle)
            val attendanceMap = composeAttendanceMap(rangeStart, rangeEnd, groupId, attendanceList)

            StaffAttendanceGroup(groupId, groupInfo.groupName, groupInfo.startDate, endDate, attendanceMap)
        }
    }

    fun upsertStaffAttendance(db: Database, staffAttendance: StaffAttendance) {
        db.transaction { tx ->
            if (!isValidStaffAttendanceDate(staffAttendance, tx.handle)) {
                throw BadRequest("Error: Upserting staff count failed. Group is not operating in given date")
            }
            upsertStaffAttendance(staffAttendance, tx.handle)
        }
    }

    private fun composeAttendanceMap(
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        groupId: UUID,
        attendanceList: List<StaffAttendance>
    ): Map<LocalDate, StaffAttendance?> = generateSequence(rangeStart) { it.plusDays(1) }
        .takeWhile { !it.isAfter(rangeEnd) }
        .map {
            it to (
                attendanceList
                    .firstOrNull { attendance -> attendance.date.isEqual(it) } ?: StaffAttendance(groupId, it, null)
                )
        }
        .toMap()
}

data class StaffAttendanceGroup(
    val groupId: UUID,
    val groupName: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val attendances: Map<LocalDate, StaffAttendance?>
)

data class StaffAttendance(
    var groupId: UUID,
    val date: LocalDate,
    val count: Double?
)

data class GroupInfo(
    val groupId: UUID,
    val groupName: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)

fun getGroupInfo(groupId: UUID, h: Handle): GroupInfo? {
    //language=SQL
    val sql =
        """
            SELECT dg.id AS group_id, dg.name AS group_name, dg.start_date, dg.end_date
            FROM daycare_group AS dg
            WHERE id = :groupId;
        """.trimIndent()

    return h.createQuery(sql)
        .bind("groupId", groupId)
        .mapTo<GroupInfo>()
        .firstOrNull()
}

fun isValidStaffAttendanceDate(staffAttendance: StaffAttendance, h: Handle): Boolean {
    //language=SQL
    val sql =
        """
        SELECT *
        FROM daycare_group
        WHERE id = :id
            AND :date BETWEEN start_date AND end_date
        """.trimIndent()

    return h.createQuery(sql)
        .bind("id", staffAttendance.groupId)
        .bind("date", staffAttendance.date)
        .mapToMap().list().size > 0
}

fun upsertStaffAttendance(staffAttendance: StaffAttendance, h: Handle) {
    //language=SQL
    val sql =
        """
        INSERT INTO staff_attendance (group_id, date, count) 
        VALUES (:groupId, :date, :count)
        ON CONFLICT (group_id, date) DO UPDATE SET count = :count
        """.trimIndent()

    h.createUpdate(sql)
        .bind("groupId", staffAttendance.groupId)
        .bind("date", staffAttendance.date)
        .bind("count", staffAttendance.count)
        .execute()
}

fun getStaffAttendanceByRange(rangeStart: LocalDate, rangeEnd: LocalDate, groupId: UUID, h: Handle): List<StaffAttendance> {
    //language=SQL
    val sql =
        """
        SELECT group_id, date, count
        FROM staff_attendance
        WHERE group_id = :groupId
            AND date BETWEEN :rangeStart AND :rangeEnd;
        """.trimIndent()

    return h.createQuery(sql)
        .bind("groupId", groupId)
        .bind("rangeStart", rangeStart)
        .bind("rangeEnd", rangeEnd)
        .mapTo<StaffAttendance>()
        .list()
}
