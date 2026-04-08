// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.daycare.service

import evaka.core.PureJdbiTest
import evaka.core.shared.AreaId
import evaka.core.shared.DaycareId
import evaka.core.shared.GroupId
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StaffAttendanceServiceIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val staffAttendanceService = StaffAttendanceService()

    val areaId: AreaId = AreaId(UUID.randomUUID())
    val daycareId: DaycareId = DaycareId(UUID.randomUUID())
    val groupId: GroupId = GroupId(UUID.randomUUID())
    val groupName = "Testiryhmä"
    val groupStartDate: LocalDate = LocalDate.of(2019, 1, 1)

    val groupId2: GroupId = GroupId(UUID.randomUUID())
    val groupName2 = "Koekaniinit"

    @BeforeEach
    fun insertDaycareGroup() {
        db.transaction { tx ->
            tx.insert(DevCareArea(id = areaId))
            tx.insert(DevDaycare(areaId = areaId, id = daycareId))
            tx.insert(
                DevDaycareGroup(
                    daycareId = daycareId,
                    id = groupId,
                    name = groupName,
                    startDate = groupStartDate,
                )
            )
            tx.insert(
                DevDaycareGroup(
                    daycareId = daycareId,
                    id = groupId2,
                    name = groupName2,
                    startDate = groupStartDate,
                )
            )
        }
    }

    @Test
    fun `get empty attendances`() {
        val result =
            staffAttendanceService.getGroupAttendancesByMonth(
                db,
                groupStartDate.year,
                groupStartDate.monthValue,
                groupId,
            )

        assertEquals(groupId, result.groupId)
        assertEquals(groupName, result.groupName)
        assertEquals(groupStartDate, result.startDate)
        assertEquals(0, result.attendances.size)
    }

    @Test
    fun `create attendance`() {
        val attendanceDate = groupStartDate
        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId, attendanceDate, 1.0),
        )

        val result =
            staffAttendanceService.getGroupAttendancesByMonth(
                db,
                attendanceDate.year,
                attendanceDate.monthValue,
                groupId,
            )

        assertEquals(1, result.attendances.size)
        assertEquals(1.0, result.attendances[attendanceDate]?.count)
    }

    @Test
    fun `modify attendance`() {
        val attendanceDate = groupStartDate
        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId, attendanceDate, 1.0),
        )

        var result =
            staffAttendanceService.getGroupAttendancesByMonth(
                db,
                attendanceDate.year,
                attendanceDate.monthValue,
                groupId,
            )
        assertEquals(1.0, result.attendances[attendanceDate]?.count)

        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId, attendanceDate, 2.5),
        )
        result =
            staffAttendanceService.getGroupAttendancesByMonth(
                db,
                attendanceDate.year,
                attendanceDate.monthValue,
                groupId,
            )
        assertEquals(2.5, result.attendances[attendanceDate]?.count)
    }

    @Test
    fun `create attendance for a date when group is not operating`() {
        val attendanceDate = groupStartDate.minusDays(1)
        assertThrows<BadRequest> {
            staffAttendanceService.upsertStaffAttendance(
                db,
                StaffAttendanceUpdate(groupId, attendanceDate, 1.0),
            )
        }

        val result =
            staffAttendanceService.getGroupAttendancesByMonth(
                db,
                attendanceDate.year,
                attendanceDate.monthValue,
                groupId,
            )
        assertEquals(null, result.attendances[attendanceDate])
    }

    @Test
    fun `get unit attendances`() {
        val firstDay = groupStartDate
        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId, firstDay, 1.0),
        )
        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId2, firstDay, 2.0),
        )

        val secondDay = groupStartDate.plusDays(5)
        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId, secondDay, 6.5),
        )

        val unitResult = staffAttendanceService.getUnitAttendancesForDate(db, daycareId, firstDay)
        assertEquals(firstDay, unitResult.date)
        assertEquals(3.0, unitResult.count)
        assertEquals(2, unitResult.groups.size)
        assertEquals(
            db.read {
                it.createQuery {
                        sql(
                            "SELECT MAX(updated_at) FROM staff_attendance WHERE date = ${bind(firstDay)}"
                        )
                    }
                    .exactlyOne<HelsinkiDateTime>()
            },
            unitResult.updatedAt,
        )

        val groupResult = unitResult.groups.find { it.groupId == groupId }!!
        assertEquals(1.0, groupResult.count)
        val group2Result = unitResult.groups.find { it.groupId == groupId2 }!!
        assertEquals(2.0, group2Result.count)

        val unitResult2 = staffAttendanceService.getUnitAttendancesForDate(db, daycareId, secondDay)
        assertEquals(secondDay, unitResult2.date)
        assertEquals(6.5, unitResult2.count)
    }
}
