// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.daycare.AbstractIntegrationTest
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class StaffAttendanceServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var db: Jdbi

    @Autowired
    lateinit var staffAttendanceService: StaffAttendanceService

    val areaId = UUID.randomUUID()
    val daycareId = UUID.randomUUID()
    val groupId = UUID.randomUUID()
    val groupName = "Testiryhmä"
    val groupStartDate = LocalDate.of(2019, 1, 1)

    @BeforeEach
    private fun beforeEach() {
        insertDaycareGroup()
    }

    @AfterEach
    private fun afterEach() {
        cleanTestData()
    }

    @Test
    fun `get attendances for every day of month`() {
        val result =
            staffAttendanceService.getAttendancesByMonth(groupStartDate.year, groupStartDate.monthValue, groupId)

        assertEquals(result.groupId, groupId)
        assertEquals(result.groupName, groupName)
        assertEquals(result.attendances.size, groupStartDate.month.length(false))
    }

    @Test
    fun `attendances are composed correctly`() {
        val attendanceDate = groupStartDate
        val result =
            staffAttendanceService.getAttendancesByMonth(attendanceDate.year, attendanceDate.monthValue, groupId)

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
        staffAttendanceService.upsertStaffAttendance(StaffAttendance(groupId, attendanceDate, 1.0))

        val result =
            staffAttendanceService.getAttendancesByMonth(attendanceDate.year, attendanceDate.monthValue, groupId)

        assertEquals(result.attendances.size, attendanceDate.month.length(false))
        assertEquals(result.attendances[attendanceDate]?.count, 1.0)
    }

    @Test
    fun `modify attendance`() {
        val attendanceDate = groupStartDate
        staffAttendanceService.upsertStaffAttendance(StaffAttendance(groupId, attendanceDate, 1.0))

        var result =
            staffAttendanceService.getAttendancesByMonth(attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(result.attendances[attendanceDate]?.count, 1.0)

        staffAttendanceService.upsertStaffAttendance(StaffAttendance(groupId, attendanceDate, 2.5))
        result = staffAttendanceService.getAttendancesByMonth(attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(result.attendances[attendanceDate]?.count, 2.5)
    }

    @Test
    fun `create attendance for a date when group is not operating`() {
        val attendanceDate = groupStartDate.minusDays(1)
        assertThrows<BadRequest> {
            staffAttendanceService.upsertStaffAttendance(StaffAttendance(groupId, attendanceDate, 1.0))
        }

        val result =
            staffAttendanceService.getAttendancesByMonth(attendanceDate.year, attendanceDate.monthValue, groupId)
        assertEquals(result.attendances[attendanceDate]?.count, null)
    }

    private fun insertDaycareGroup() {
        jdbi.transaction {
            it.insertTestCareArea(DevCareArea(id = areaId))
            it.insertTestDaycare(DevDaycare(areaId = areaId, id = daycareId))
            it.insertTestDaycareGroup(
                DevDaycareGroup(daycareId = daycareId, id = groupId, name = groupName, startDate = groupStartDate)
            )
        }
    }

    private fun cleanTestData() {
        jdbi.handle {
            it.createUpdate(
                //language=SQL
                """
                DELETE FROM staff_attendance;
                DELETE FROM daycare_group where daycare_id = :daycareId;
                DELETE FROM daycare WHERE care_area_id = :areaId;
                DELETE FROM care_area WHERE id = :areaId;
                """.trimIndent()
            )
                .bind("areaId", areaId)
                .bind("daycareId", daycareId)
                .execute()
        }
    }
}
