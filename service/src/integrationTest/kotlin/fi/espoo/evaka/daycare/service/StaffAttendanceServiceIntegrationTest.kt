// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.daycare.AbstractIntegrationTest
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.domain.BadRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class StaffAttendanceServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var staffAttendanceService: StaffAttendanceService

    val areaId = UUID.randomUUID()
    val daycareId = UUID.randomUUID()
    val groupId = UUID.randomUUID()
    val groupName = "Testiryhmä"
    val groupStartDate = LocalDate.of(2019, 1, 1)

    @BeforeEach
    protected fun insertDaycareGroup() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertTestCareArea(DevCareArea(id = areaId))
            tx.insertTestDaycare(DevDaycare(areaId = areaId, id = daycareId))
            tx.insertTestDaycareGroup(
                DevDaycareGroup(daycareId = daycareId, id = groupId, name = groupName, startDate = groupStartDate)
            )
        }
    }

    @Test
    fun `get attendances for every day of month`() {
        val result =
            staffAttendanceService.getAttendancesByMonth(db, groupStartDate.year, groupStartDate.monthValue, groupId)

        assertEquals(result.groupId, groupId)
        assertEquals(result.groupName, groupName)
        assertEquals(result.attendances.size, groupStartDate.month.length(false))
    }

    @Test
    fun `attendances are composed correctly`() {
        val attendanceDate = groupStartDate
        val result =
            staffAttendanceService.getAttendancesByMonth(db, attendanceDate.year, attendanceDate.monthValue, groupId)

        assertEquals(result.groupId, groupId)
        assertEquals(result.groupName, groupName)
        assertEquals(result.startDate, groupStartDate)
        assertEquals(result.attendances.size, attendanceDate.month.length(false))

        result.attendances.forEach { attendance ->
            assertEquals(attendance.value?.date, attendance.key)
            assertEquals(attendance.value?.groupId, groupId)
            assertEquals(attendance.value?.count, null)
        }
    }

    @Test
    fun `create attendance`() {
        val attendanceDate = groupStartDate
        staffAttendanceService.upsertStaffAttendance(db, StaffAttendance(groupId, attendanceDate, 1.0, 0.5))

        val result =
            staffAttendanceService.getAttendancesByMonth(db, attendanceDate.year, attendanceDate.monthValue, groupId)

        assertEquals(result.attendances.size, attendanceDate.month.length(false))
        assertEquals(result.attendances[attendanceDate]?.count, 1.0)
        assertEquals(result.attendances[attendanceDate]?.countOther, 0.5)
    }

    @Test
    fun `creating an attendance with null countOther sets countOther to 0`() {
        val attendanceDate = groupStartDate
        staffAttendanceService.upsertStaffAttendance(db, StaffAttendance(groupId, attendanceDate, 1.0, null))

        val result =
            staffAttendanceService.getAttendancesByMonth(db, attendanceDate.year, attendanceDate.monthValue, groupId)

        assertEquals(result.attendances.size, attendanceDate.month.length(false))
        assertEquals(result.attendances[attendanceDate]?.count, 1.0)
        assertEquals(result.attendances[attendanceDate]?.countOther, 0.0)
    }

    @Test
    fun `modify attendance`() {
        val attendanceDate = groupStartDate
        staffAttendanceService.upsertStaffAttendance(db, StaffAttendance(groupId, attendanceDate, 1.0, 0.5))

        var result =
            staffAttendanceService.getAttendancesByMonth(db, attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(result.attendances[attendanceDate]?.count, 1.0)
        assertEquals(result.attendances[attendanceDate]?.countOther, 0.5)

        staffAttendanceService.upsertStaffAttendance(db, StaffAttendance(groupId, attendanceDate, 2.5, 0.0))
        result = staffAttendanceService.getAttendancesByMonth(db, attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(result.attendances[attendanceDate]?.count, 2.5)
        assertEquals(result.attendances[attendanceDate]?.countOther, 0.0)
    }

    @Test
    fun `modifying attendance with null countOther doesn't change countOther`() {
        val attendanceDate = groupStartDate
        staffAttendanceService.upsertStaffAttendance(db, StaffAttendance(groupId, attendanceDate, 1.0, 0.5))

        var result =
            staffAttendanceService.getAttendancesByMonth(db, attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(result.attendances[attendanceDate]?.count, 1.0)
        assertEquals(result.attendances[attendanceDate]?.countOther, 0.5)

        staffAttendanceService.upsertStaffAttendance(db, StaffAttendance(groupId, attendanceDate, 2.5, null))
        result = staffAttendanceService.getAttendancesByMonth(db, attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(result.attendances[attendanceDate]?.count, 2.5)
        assertEquals(result.attendances[attendanceDate]?.countOther, 0.5)
    }

    @Test
    fun `create attendance for a date when group is not operating`() {
        val attendanceDate = groupStartDate.minusDays(1)
        assertThrows<BadRequest> {
            staffAttendanceService.upsertStaffAttendance(db, StaffAttendance(groupId, attendanceDate, 1.0, 0.5))
        }

        val result =
            staffAttendanceService.getAttendancesByMonth(db, attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(result.attendances[attendanceDate]?.count, null)
    }
}
