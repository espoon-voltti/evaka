// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.Month
import org.springframework.stereotype.Service

@Service
class StaffAttendanceService {
    fun getUnitAttendancesForDate(
        db: Database.Connection,
        unitId: DaycareId,
        date: LocalDate
    ): UnitStaffAttendance {
        return db.read { tx -> tx.getUnitStaffAttendanceForDate(unitId, date) }
    }

    fun getGroupAttendancesByMonth(
        db: Database.Connection,
        year: Int,
        month: Int,
        groupId: GroupId
    ): StaffAttendanceForDates {
        val range = FiniteDateRange.ofMonth(year, Month.of(month))

        return db.read { tx ->
            val groupInfo =
                tx.getGroupInfo(groupId)
                    ?: throw BadRequest("Couldn't find group info with id $groupId")

            val attendanceList = tx.getStaffAttendanceByRange(range, groupId)
            val attendanceMap = attendanceList.associateBy { it.date }

            StaffAttendanceForDates(
                groupId,
                groupInfo.groupName,
                groupInfo.startDate,
                groupInfo.endDate,
                attendanceMap
            )
        }
    }

    fun upsertStaffAttendance(db: Database.Connection, staffAttendance: StaffAttendanceUpdate) {
        db.transaction { tx ->
            if (!tx.isValidStaffAttendanceDate(staffAttendance)) {
                throw BadRequest(
                    "Error: Upserting staff count failed. Group is not operating in given date"
                )
            }
            tx.upsertStaffAttendance(staffAttendance)
        }
    }
}

data class GroupStaffAttendance(
    val groupId: GroupId,
    val date: LocalDate,
    val count: Double,
    val countOther: Double,
    val updated: HelsinkiDateTime
)

data class UnitStaffAttendance(
    val date: LocalDate,
    val count: Double,
    val countOther: Double,
    val updated: HelsinkiDateTime?,
    val groups: List<GroupStaffAttendance>
)

data class StaffAttendanceForDates(
    val groupId: GroupId,
    val groupName: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val attendances: Map<LocalDate, GroupStaffAttendance>
)

data class GroupInfo(
    val groupId: GroupId,
    val groupName: String,
    val startDate: LocalDate,
    val endDate: LocalDate?
)

data class StaffAttendanceUpdate(
    val groupId: GroupId,
    val date: LocalDate,
    val count: Double?,
    val countOther: Double?
)

fun Database.Read.getGroupInfo(groupId: GroupId): GroupInfo? {
    // language=SQL
    val sql =
        """
            SELECT dg.id AS group_id, dg.name AS group_name, dg.start_date, dg.end_date
            FROM daycare_group AS dg
            WHERE id = :groupId;
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql).bind("groupId", groupId).exactlyOneOrNull<GroupInfo>()
}

fun Database.Read.isValidStaffAttendanceDate(staffAttendance: StaffAttendanceUpdate): Boolean {
    // language=SQL
    val sql =
        """
        SELECT EXISTS (
            SELECT 1
            FROM daycare_group
            WHERE id = :id
            AND daterange(start_date, end_date, '[]') @> :date
        )
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("id", staffAttendance.groupId)
        .bind("date", staffAttendance.date)
        .exactlyOne<Boolean>()
}

fun Database.Transaction.upsertStaffAttendance(staffAttendance: StaffAttendanceUpdate) {
    // language=SQL
    val sql =
        if (staffAttendance.countOther != null) {
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
            }
            .trimIndent()

    @Suppress("DEPRECATION")
    createUpdate(sql)
        .bind("groupId", staffAttendance.groupId)
        .bind("date", staffAttendance.date)
        .bind("count", staffAttendance.count)
        .bind("count_other", staffAttendance.countOther)
        .execute()
}

fun Database.Read.getStaffAttendanceByRange(
    range: FiniteDateRange,
    groupId: GroupId
): List<GroupStaffAttendance> {
    // language=SQL
    val sql =
        """
        SELECT group_id, date, count, count_other, updated
        FROM staff_attendance
        WHERE group_id = :groupId
        AND between_start_and_end(:range, date)
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("groupId", groupId)
        .bind("range", range)
        .toList<GroupStaffAttendance>()
}

fun Database.Read.getUnitStaffAttendanceForDate(
    unitId: DaycareId,
    date: LocalDate
): UnitStaffAttendance {
    // language=SQL
    val sql =
        """
        SELECT group_id, date, count, count_other, updated
        FROM staff_attendance sa
        JOIN daycare_group dg on sa.group_id = dg.id
        WHERE dg.daycare_id = :unitId
          AND sa.date = :date
        """
            .trimIndent()

    val groupAttendances =
        @Suppress("DEPRECATION")
        createQuery(sql).bind("unitId", unitId).bind("date", date).toList<GroupStaffAttendance>()

    val count = groupAttendances.sumOf { it.count }
    val countOther = groupAttendances.sumOf { it.countOther }
    val updated = groupAttendances.maxOfOrNull { it.updated }

    return UnitStaffAttendance(
        date = date,
        count = count,
        countOther = countOther,
        groups = groupAttendances,
        updated = updated
    )
}
