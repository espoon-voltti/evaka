// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
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
                    startDate = groupStartDate
                )
            )
            tx.insert(
                DevDaycareGroup(
                    daycareId = daycareId,
                    id = groupId2,
                    name = groupName2,
                    startDate = groupStartDate
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
                groupId
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
            StaffAttendanceUpdate(groupId, attendanceDate, 1.0, 0.5)
        )

        val result =
            staffAttendanceService.getGroupAttendancesByMonth(
                db,
                attendanceDate.year,
                attendanceDate.monthValue,
                groupId
            )

        assertEquals(1, result.attendances.size)
        assertEquals(1.0, result.attendances[attendanceDate]?.count)
        assertEquals(0.5, result.attendances[attendanceDate]?.countOther)
    }

    @Test
    fun `creating an attendance with null countOther sets countOther to 0`() {
        val attendanceDate = groupStartDate
        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId, attendanceDate, 1.0, null)
        )

        val result =
            staffAttendanceService.getGroupAttendancesByMonth(
                db,
                attendanceDate.year,
                attendanceDate.monthValue,
                groupId
            )

        assertEquals(1, result.attendances.size)
        assertEquals(1.0, result.attendances[attendanceDate]?.count)
        assertEquals(0.0, result.attendances[attendanceDate]?.countOther)
    }

    @Test
    fun `modify attendance`() {
        val attendanceDate = groupStartDate
        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId, attendanceDate, 1.0, 0.5)
        )

        var result =
            staffAttendanceService.getGroupAttendancesByMonth(
                db,
                attendanceDate.year,
                attendanceDate.monthValue,
                groupId
            )
        assertEquals(1.0, result.attendances[attendanceDate]?.count)
        assertEquals(0.5, result.attendances[attendanceDate]?.countOther)

        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId, attendanceDate, 2.5, 0.0)
        )
        result =
            staffAttendanceService.getGroupAttendancesByMonth(
                db,
                attendanceDate.year,
                attendanceDate.monthValue,
                groupId
            )
        assertEquals(2.5, result.attendances[attendanceDate]?.count)
        assertEquals(0.0, result.attendances[attendanceDate]?.countOther)
    }

    @Test
    fun `modifying attendance with null countOther doesn't change countOther`() {
        val attendanceDate = groupStartDate
        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId, attendanceDate, 1.0, 0.5)
        )

        var result =
            staffAttendanceService.getGroupAttendancesByMonth(
                db,
                attendanceDate.year,
                attendanceDate.monthValue,
                groupId
            )
        assertEquals(1.0, result.attendances[attendanceDate]?.count)
        assertEquals(0.5, result.attendances[attendanceDate]?.countOther)

        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId, attendanceDate, 2.5, null)
        )
        result =
            staffAttendanceService.getGroupAttendancesByMonth(
                db,
                attendanceDate.year,
                attendanceDate.monthValue,
                groupId
            )
        assertEquals(2.5, result.attendances[attendanceDate]?.count)
        assertEquals(0.5, result.attendances[attendanceDate]?.countOther)
    }

    @Test
    fun `create attendance for a date when group is not operating`() {
        val attendanceDate = groupStartDate.minusDays(1)
        assertThrows<BadRequest> {
            staffAttendanceService.upsertStaffAttendance(
                db,
                StaffAttendanceUpdate(groupId, attendanceDate, 1.0, 0.5)
            )
        }

        val result =
            staffAttendanceService.getGroupAttendancesByMonth(
                db,
                attendanceDate.year,
                attendanceDate.monthValue,
                groupId
            )
        assertEquals(null, result.attendances[attendanceDate])
    }

    @Test
    fun `get unit attendances`() {
        val firstDay = groupStartDate
        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId, firstDay, 1.0, 0.5)
        )
        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId2, firstDay, 2.0, 1.5)
        )

        val secondDay = groupStartDate.plusDays(5)
        staffAttendanceService.upsertStaffAttendance(
            db,
            StaffAttendanceUpdate(groupId, secondDay, 6.5, null)
        )

        val unitResult = staffAttendanceService.getUnitAttendancesForDate(db, daycareId, firstDay)
        assertEquals(firstDay, unitResult.date)
        assertEquals(3.0, unitResult.count)
        assertEquals(2.0, unitResult.countOther)
        assertEquals(2, unitResult.groups.size)
        assertEquals(
            db.read {
                @Suppress("DEPRECATION")
                it
                    .createQuery("SELECT MAX(updated) FROM staff_attendance WHERE date = :date")
                    .bind("date", firstDay)
                    .exactlyOne<HelsinkiDateTime>()
            },
            unitResult.updated
        )

        val groupResult = unitResult.groups.find { it.groupId == groupId }!!
        assertEquals(1.0, groupResult.count)
        assertEquals(0.5, groupResult.countOther)
        val group2Result = unitResult.groups.find { it.groupId == groupId2 }!!
        assertEquals(2.0, group2Result.count)
        assertEquals(1.5, group2Result.countOther)

        val unitResult2 = staffAttendanceService.getUnitAttendancesForDate(db, daycareId, secondDay)
        assertEquals(secondDay, unitResult2.date)
        assertEquals(6.5, unitResult2.count)
        assertEquals(0.0, unitResult2.countOther)
    }
}
