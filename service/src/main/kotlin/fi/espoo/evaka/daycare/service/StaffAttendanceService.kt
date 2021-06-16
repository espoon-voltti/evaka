// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.PGConstants.maxDate
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.util.UUID

@Service
class StaffAttendanceService {
    fun getAttendancesByMonth(db: Database.Connection, year: Int, month: Int, groupId: UUID): StaffAttendanceGroup {
        val rangeStart = LocalDate.of(year, month, 1)
        val rangeEnd = rangeStart.with(lastDayOfMonth())

        return db.read { tx ->
            val groupInfo = tx.getGroupInfo(groupId) ?: throw BadRequest("Couldn't find group info with id $groupId")
            val endDate = groupInfo.endDate.let { if (it.isBefore(maxDate)) it else null }

            val attendanceList = tx.getStaffAttendanceByRange(rangeStart, rangeEnd, groupId)
            val attendanceMap = composeAttendanceMap(rangeStart, rangeEnd, groupId, attendanceList)

            StaffAttendanceGroup(groupId, groupInfo.groupName, groupInfo.startDate, endDate, attendanceMap)
        }
    }

    fun upsertStaffAttendance(db: Database.Connection, staffAttendance: StaffAttendance) {
        db.transaction { tx ->
            if (!tx.isValidStaffAttendanceDate(staffAttendance)) {
                throw BadRequest("Error: Upserting staff count failed. Group is not operating in given date")
            }
            tx.upsertStaffAttendance(staffAttendance)
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
                    .firstOrNull { attendance -> attendance.date.isEqual(it) } ?: StaffAttendance(groupId, it, null, null)
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
    val groupId: UUID,
    val date: LocalDate,
    val count: Double?,
    val countOther: Double?
)

data class GroupInfo(
    val groupId: UUID,
    val groupName: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)

fun Database.Read.getGroupInfo(groupId: UUID): GroupInfo? {
    //language=SQL
    val sql =
        """
            SELECT dg.id AS group_id, dg.name AS group_name, dg.start_date, dg.end_date
            FROM daycare_group AS dg
            WHERE id = :groupId;
        """.trimIndent()

    return createQuery(sql)
        .bind("groupId", groupId)
        .mapTo<GroupInfo>()
        .firstOrNull()
}

fun Database.Read.isValidStaffAttendanceDate(staffAttendance: StaffAttendance): Boolean {
    //language=SQL
    val sql =
        """
        SELECT *
        FROM daycare_group
        WHERE id = :id
            AND :date BETWEEN start_date AND end_date
        """.trimIndent()

    return createQuery(sql)
        .bind("id", staffAttendance.groupId)
        .bind("date", staffAttendance.date)
        .mapToMap().list().size > 0
}

fun Database.Transaction.upsertStaffAttendance(staffAttendance: StaffAttendance) {
    //language=SQL
    val sql = if (staffAttendance.countOther != null) {
        """
        INSERT INTO staff_attendance (group_id, date, count, count_other)
        VALUES (:groupId, :date, :count, :count_other)
        ON CONFLICT (group_id, date) DO UPDATE SET
            count = :count,
            count_other = :count_other
        """
    } else {
        """
        INSERT INTO staff_attendance (group_id, date, count, count_other)
        VALUES (:groupId, :date, :count, DEFAULT)
        ON CONFLICT (group_id, date) DO UPDATE SET
            count = :count
        """
    }.trimIndent()

    createUpdate(sql)
        .bind("groupId", staffAttendance.groupId)
        .bind("date", staffAttendance.date)
        .bind("count", staffAttendance.count)
        .bind("count_other", staffAttendance.countOther)
        .execute()
}

fun Database.Read.getStaffAttendanceByRange(rangeStart: LocalDate, rangeEnd: LocalDate, groupId: UUID): List<StaffAttendance> {
    //language=SQL
    val sql =
        """
        SELECT group_id, date, count, count_other
        FROM staff_attendance
        WHERE group_id = :groupId
            AND date BETWEEN :rangeStart AND :rangeEnd;
        """.trimIndent()

    return createQuery(sql)
        .bind("groupId", groupId)
        .bind("rangeStart", rangeStart)
        .bind("rangeEnd", rangeEnd)
        .mapTo<StaffAttendance>()
        .list()
}
