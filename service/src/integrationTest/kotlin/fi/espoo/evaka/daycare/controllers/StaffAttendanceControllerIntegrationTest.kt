// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.StaffAttendanceForDates
import fi.espoo.evaka.daycare.StaffAttendanceUpdate
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.RealEvakaClock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class StaffAttendanceControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: StaffAttendanceController

    val areaId: AreaId = AreaId(UUID.randomUUID())
    val daycareId: DaycareId = DaycareId(UUID.randomUUID())
    val groupId: GroupId = GroupId(UUID.randomUUID())
    val groupName = "Testiryhmä"
    val groupStartDate: LocalDate = LocalDate.of(2019, 1, 1)

    val groupId2: GroupId = GroupId(UUID.randomUUID())
    val groupName2 = "Koekaniinit"

    private val supervisor = DevEmployee()

    @BeforeEach
    fun setup() {
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
            tx.insert(supervisor, mapOf(daycareId to UserRole.UNIT_SUPERVISOR))
        }
    }

    private fun getAttendances(year: Int, month: Int, groupId: GroupId): StaffAttendanceForDates {
        return controller.getStaffAttendancesByGroup(
            dbInstance(),
            supervisor.user,
            RealEvakaClock(),
            year,
            month,
            groupId,
        )
    }

    private fun upsert(staffAttendance: StaffAttendanceUpdate) {
        controller.upsertStaffAttendance(
            dbInstance(),
            supervisor.user,
            RealEvakaClock(),
            staffAttendance,
            staffAttendance.groupId,
        )
    }

    @Test
    fun `get empty attendances`() {
        val result = getAttendances(groupStartDate.year, groupStartDate.monthValue, groupId)

        assertEquals(groupId, result.groupId)
        assertEquals(groupName, result.groupName)
        assertEquals(groupStartDate, result.startDate)
        assertEquals(0, result.attendances.size)
    }

    @Test
    fun `create attendance`() {
        val attendanceDate = groupStartDate
        upsert(StaffAttendanceUpdate(groupId, attendanceDate, 1.0))

        val result = getAttendances(attendanceDate.year, attendanceDate.monthValue, groupId)

        assertEquals(1, result.attendances.size)
        assertEquals(1.0, result.attendances[attendanceDate]?.count)
    }

    @Test
    fun `modify attendance`() {
        val attendanceDate = groupStartDate
        upsert(StaffAttendanceUpdate(groupId, attendanceDate, 1.0))

        var result = getAttendances(attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(1.0, result.attendances[attendanceDate]?.count)

        upsert(StaffAttendanceUpdate(groupId, attendanceDate, 2.5))
        result = getAttendances(attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(2.5, result.attendances[attendanceDate]?.count)
    }

    @Test
    fun `create attendance for a date when group is not operating`() {
        val attendanceDate = groupStartDate.minusDays(1)
        assertThrows<BadRequest> { upsert(StaffAttendanceUpdate(groupId, attendanceDate, 1.0)) }

        val result = getAttendances(attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(null, result.attendances[attendanceDate])
    }

    @Test
    fun `clear attendance`() {
        val attendanceDate = groupStartDate
        upsert(StaffAttendanceUpdate(groupId, attendanceDate, 1.0))

        var result = getAttendances(attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(1, result.attendances.size)

        upsert(StaffAttendanceUpdate(groupId, attendanceDate, null))
        result = getAttendances(attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(0, result.attendances.size)
    }
}
