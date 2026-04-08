// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate

data class GroupStaffAttendance(
    val groupId: GroupId,
    val date: LocalDate,
    val count: Double,
    val updatedAt: HelsinkiDateTime,
)

data class UnitStaffAttendance(
    val date: LocalDate,
    val count: Double,
    val updatedAt: HelsinkiDateTime?,
    val groups: List<GroupStaffAttendance>,
)

data class StaffAttendanceForDates(
    val groupId: GroupId,
    val groupName: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val attendances: Map<LocalDate, GroupStaffAttendance>,
)

data class GroupInfo(
    val groupId: GroupId,
    val groupName: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
)

data class StaffAttendanceUpdate(val groupId: GroupId, val date: LocalDate, val count: Double?)

fun Database.Read.getGroupInfo(groupId: GroupId): GroupInfo? =
    createQuery {
            sql(
                """
SELECT dg.id AS group_id, dg.name AS group_name, dg.start_date, dg.end_date
FROM daycare_group AS dg
WHERE id = ${bind(groupId)}
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Read.isValidStaffAttendanceDate(staffAttendance: StaffAttendanceUpdate): Boolean =
    createQuery {
            sql(
                """
SELECT EXISTS (
    SELECT 1
    FROM daycare_group
    WHERE id = ${bind(staffAttendance.groupId)}
    AND daterange(start_date, end_date, '[]') @> ${bind(staffAttendance.date)}
)
"""
            )
        }
        .exactlyOne()

fun Database.Transaction.upsertStaffAttendance(staffAttendance: StaffAttendanceUpdate) {
    execute {
        sql(
            """
INSERT INTO staff_attendance (group_id, date, count)
VALUES (${bind(staffAttendance.groupId)}, ${bind(staffAttendance.date)}, ${bind(staffAttendance.count)})
ON CONFLICT (group_id, date) DO UPDATE SET
    count = ${bind(staffAttendance.count)}
"""
        )
    }
}

fun Database.Read.getStaffAttendanceByRange(
    range: FiniteDateRange,
    groupId: GroupId,
): List<GroupStaffAttendance> =
    createQuery {
            sql(
                """
SELECT group_id, date, count, updated_at
FROM staff_attendance
WHERE group_id = ${bind(groupId)}
AND between_start_and_end(${bind(range)}, date)
"""
            )
        }
        .toList()

fun Database.Read.getUnitStaffAttendanceForDate(
    unitId: DaycareId,
    date: LocalDate,
): UnitStaffAttendance {
    val groupAttendances =
        createQuery {
                sql(
                    """
SELECT sa.group_id, sa.date, sa.count, sa.updated_at
FROM staff_attendance sa
JOIN daycare_group dg on sa.group_id = dg.id
WHERE dg.daycare_id = ${bind(unitId)}
  AND sa.date = ${bind(date)}
"""
                )
            }
            .toList<GroupStaffAttendance>()

    val count = groupAttendances.sumOf { it.count }
    val updatedAt = groupAttendances.maxOfOrNull { it.updatedAt }

    return UnitStaffAttendance(
        date = date,
        count = count,
        groups = groupAttendances,
        updatedAt = updatedAt,
    )
}

fun Database.Transaction.deleteStaffAttendance(groupId: GroupId, date: LocalDate) {
    createUpdate {
            sql(
                """
DELETE FROM staff_attendance
WHERE group_id = ${bind(groupId)}
AND date = ${bind(date)}
"""
            )
        }
        .updateNoneOrOne()
}
