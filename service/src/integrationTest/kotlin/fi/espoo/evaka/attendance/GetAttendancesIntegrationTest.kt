// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.createMobileDeviceToUnit
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class GetAttendancesIntegrationTest : FullApplicationTest() {
    private val userId = UUID.randomUUID()
    private val mobileUser = AuthenticatedUser(userId, emptySet())
    private val groupId = UUID.randomUUID()
    private val groupName = "Testaajat"
    private val daycarePlacementId = UUID.randomUUID()
    private val placementStart = LocalDate.now().minusDays(30)
    private val placementEnd = LocalDate.now().plusDays(30)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            insertGeneralTestFixtures(tx.handle)
            tx.handle.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            insertTestPlacement(
                h = tx.handle,
                id = daycarePlacementId,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
            insertTestDaycareGroupPlacement(
                h = tx.handle,
                daycarePlacementId = daycarePlacementId,
                groupId = groupId,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.createMobileDeviceToUnit(userId, testDaycare.id)
        }
    }

    @Test
    fun `unit info is correct`() {
        val response = fetchAttendances()
        assertEquals(testDaycare.name, response.unit.name)
        assertEquals(1, response.unit.groups.size)
        assertEquals(groupId, response.unit.groups.first().id)
        assertEquals(groupName, response.unit.groups.first().name)
    }

    @Test
    fun `child is coming`() {
        db.transaction {
            insertTestChildAttendance(
                h = it.handle,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = OffsetDateTime.now().minusDays(1).minusHours(8).toInstant(),
                departed = OffsetDateTime.now().minusDays(1).toInstant()
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.COMING, child.status)
        assertNull(child.attendance)
        assertEquals(0, child.absences.size)
    }

    @Test
    fun `child is present`() {
        val arrived = OffsetDateTime.now().minusHours(3).toInstant()
        db.transaction {
            insertTestChildAttendance(
                h = it.handle,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = arrived,
                departed = null
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.PRESENT, child.status)
        assertNotNull(child.attendance)
        assertEquals(arrived, child.attendance!!.arrived)
        assertNull(child.attendance!!.departed)
        assertEquals(testDaycare.id, child.attendance!!.unitId)
        assertEquals(0, child.absences.size)
    }

    @Test
    fun `child has departed`() {
        val arrived = OffsetDateTime.now().minusHours(3).toInstant()
        val departed = OffsetDateTime.now().minusMinutes(1).toInstant()
        db.transaction {
            insertTestChildAttendance(
                h = it.handle,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = arrived,
                departed = departed
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendance)
        assertEquals(arrived, child.attendance!!.arrived)
        assertEquals(departed, child.attendance!!.departed)
        assertEquals(testDaycare.id, child.attendance!!.unitId)
        assertEquals(0, child.absences.size)
    }

    @Test
    fun `child is absent`() {
        db.transaction {
            insertTestAbsence(
                h = it.handle,
                childId = testChild_1.id,
                careType = CareType.PRESCHOOL,
                date = LocalDate.now(),
                absenceType = AbsenceType.SICKLEAVE
            )
            insertTestAbsence(
                h = it.handle,
                childId = testChild_1.id,
                careType = CareType.PRESCHOOL_DAYCARE,
                date = LocalDate.now(),
                absenceType = AbsenceType.SICKLEAVE
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertNull(child.attendance)
        assertEquals(2, child.absences.size)
        assertEquals(1, child.absences.filter { it.careType == CareType.PRESCHOOL && it.absenceType == AbsenceType.SICKLEAVE }.size)
        assertEquals(1, child.absences.filter { it.careType == CareType.PRESCHOOL_DAYCARE && it.absenceType == AbsenceType.SICKLEAVE }.size)
    }

    private fun fetchAttendances(): AttendanceResponse {
        val (_, res, result) = http.get("/attendances/units/${testDaycare.id}")
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun expectOneChild(): Child {
        val response = fetchAttendances()
        assertEquals(1, response.children.size)
        return response.children.first()
    }
}
