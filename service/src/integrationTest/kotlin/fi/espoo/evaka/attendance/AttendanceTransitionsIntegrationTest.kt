// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.updateDaycareAclWithEmployee
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AttendanceTransitionsIntegrationTest : FullApplicationTest() {
    private val staffUser = AuthenticatedUser(testDecisionMaker_1.id, emptySet())
    private val groupId = UUID.randomUUID()
    private val groupName = "Testaajat"
    private val daycarePlacementId = UUID.randomUUID()
    private val placementStart = LocalDate.now().minusDays(30)
    private val placementEnd = LocalDate.now().plusDays(30)

    @BeforeEach
    fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
            h.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            insertTestPlacement(
                h = h,
                id = daycarePlacementId,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
            insertTestDaycareGroupPlacement(
                h = h,
                daycarePlacementId = daycarePlacementId,
                groupId = groupId,
                startDate = placementStart,
                endDate = placementEnd
            )
            updateDaycareAclWithEmployee(h, testDaycare.id, staffUser.id, UserRole.STAFF)
        }
    }

    @Test
    fun `child arrives - happy case`() {
        givenChildComing()
        val arrival = Instant.now()
        val child = markArrived(arrival)
        expectOneChild()
        assertEquals(AttendanceStatus.PRESENT, child.status)
        assertNotNull(child.attendance)
        assertNotNull(child.attendance!!.arrived)
        assertNull(child.attendance!!.departed)
        assertTrue(child.absences.isEmpty())
    }

    private fun givenChildComing() {
        val childBefore = expectOneChild()
        assertEquals(AttendanceStatus.COMING, childBefore.status)
    }

    private fun fetchAttendances(): AttendanceResponse {
        val (_, res, result) = http.get("/attendances/units/${testDaycare.id}")
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun markArrived(arrived: Instant): Child {
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.ArrivalRequest(arrived)))
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun expectOneChild(): Child {
        val response = fetchAttendances()
        assertEquals(1, response.children.size)
        return response.children.first()
    }
}
