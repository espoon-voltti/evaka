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
import java.time.OffsetDateTime
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
    fun `get child arrival info - happy case`() {
        givenChildComing()
        val info = getArrivalInfoAssertOk(Instant.now())
        assertEquals(false, info.absentFromPreschool)
    }

    @Test
    fun `get child arrival info - already present is error`() {
        givenChildPresent()
        getArrivalInfoAssertFail(Instant.now(), 409)
    }

    @Test
    fun `post child arrives - happy case`() {
        givenChildComing()

        val arrived = Instant.now()
        val child = markArrivedAssertOkOneChild(arrived)

        assertEquals(AttendanceStatus.PRESENT, child.status)
        assertNotNull(child.attendance)
        assertEquals(arrived, child.attendance!!.arrived)
        assertNull(child.attendance!!.departed)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post child arrives - arriving twice is error`() {
        givenChildPresent()

        val arrived = Instant.now()
        markArrivedAssertFail(arrived, 409)
    }

    @Test
    fun `post return to coming - happy case when present`() {
        givenChildPresent()

        val child = returnToComingAssertOkOneChild()

        assertEquals(AttendanceStatus.COMING, child.status)
        assertNull(child.attendance)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post return to coming - happy case when absent`() {
        givenChildAbsent(listOf(CareType.PRESCHOOL, CareType.PRESCHOOL_DAYCARE), AbsenceType.UNKNOWN_ABSENCE)

        val child = returnToComingAssertOkOneChild()

        assertEquals(AttendanceStatus.COMING, child.status)
        assertNull(child.attendance)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post return to coming - error when departed`() {
        givenChildDeparted()
        returnToComingAssertFail(409)
    }

    @Test
    fun `get child departure info - happy case`() {
        givenChildPresent()
        val info = getDepartureInfoAssertOk(Instant.now())
        assertTrue(info.absentFrom.isEmpty())
    }

    @Test
    fun `get child departure info - not yet present is error`() {
        givenChildComing()
        getDepartureInfoAssertFail(Instant.now(), 409)
    }

    @Test
    fun `get child departure info - already departed is error`() {
        givenChildDeparted()
        getDepartureInfoAssertFail(Instant.now(), 409)
    }

    @Test
    fun `post child departs - happy case`() {
        givenChildPresent()

        val departed = Instant.now()
        val child = markDepartedAssertOkOneChild(departed)

        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendance)
        assertEquals(departed, child.attendance!!.departed)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post child departs - departing twice is error`() {
        givenChildDeparted()

        val departed = Instant.now()
        markDepartedAssertFail(departed, 409)
    }

    @Test
    fun `post return to present - happy case when departed`() {
        givenChildDeparted()

        val child = returnToPresentAssertOkOneChild()

        assertEquals(AttendanceStatus.PRESENT, child.status)
        assertNotNull(child.attendance)
        assertNotNull(child.attendance!!.arrived)
        assertNull(child.attendance!!.departed)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post return to present - error when coming`() {
        givenChildComing()
        returnToPresentAssertFail(409)
    }

    @Test
    fun `post return to present - error when present`() {
        givenChildPresent()
        returnToPresentAssertFail(409)
    }

    @Test
    fun `post return to present - error when absent`() {
        givenChildAbsent(listOf(CareType.PRESCHOOL, CareType.PRESCHOOL_DAYCARE), AbsenceType.UNKNOWN_ABSENCE)
        returnToPresentAssertFail(409)
    }

    private fun givenChildComing() {
        val childBefore = expectOneChild()
        assertEquals(AttendanceStatus.COMING, childBefore.status)
    }

    private fun givenChildPresent() {
        val arrived = OffsetDateTime.now().minusHours(3).toInstant()
        jdbi.handle {
            insertTestChildAttendance(
                h = it,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = arrived,
                departed = null
            )
        }
        val childBefore = expectOneChild()
        assertEquals(AttendanceStatus.PRESENT, childBefore.status)
    }

    private fun givenChildDeparted() {
        val arrived = OffsetDateTime.now().minusHours(3).toInstant()
        val departed = OffsetDateTime.now().minusHours(1).toInstant()
        jdbi.handle {
            insertTestChildAttendance(
                h = it,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = arrived,
                departed = departed
            )
        }
        val childBefore = expectOneChild()
        assertEquals(AttendanceStatus.DEPARTED, childBefore.status)
    }

    private fun givenChildAbsent(absentFrom: List<CareType>, absenceType: AbsenceType) {
        absentFrom.forEach { careType ->
            jdbi.handle {
                insertTestAbsence(h = it, childId = testChild_1.id, absenceType = absenceType, careType = careType, date = LocalDate.now())
            }
        }
    }

    private fun getAttendances(): AttendanceResponse {
        val (_, res, result) = http.get("/attendances/units/${testDaycare.id}")
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getArrivalInfoAssertOk(time: Instant): ChildAttendanceController.ArrivalInfoResponse {
        val (_, res, result) = http.get("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival?time=$time")
            .asUser(staffUser)
            .responseObject<ChildAttendanceController.ArrivalInfoResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getArrivalInfoAssertFail(time: Instant, status: Int) {
        val (_, res, _) = http.get("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival?time=$time")
            .asUser(staffUser)
            .responseObject<ChildAttendanceController.ArrivalInfoResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun markArrivedAssertOkOneChild(arrived: Instant): Child {
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.ArrivalRequest(arrived)))
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun markArrivedAssertFail(arrived: Instant, status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.ArrivalRequest(arrived)))
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun returnToComingAssertOkOneChild(): Child {
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/return-to-coming")
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun returnToComingAssertFail(status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/return-to-coming")
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun getDepartureInfoAssertOk(time: Instant): ChildAttendanceController.DepartureInfoResponse {
        val (_, res, result) = http.get("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure?time=$time")
            .asUser(staffUser)
            .responseObject<ChildAttendanceController.DepartureInfoResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getDepartureInfoAssertFail(time: Instant, status: Int) {
        val (_, res, _) = http.get("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure?time=$time")
            .asUser(staffUser)
            .responseObject<ChildAttendanceController.DepartureInfoResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun markDepartedAssertOkOneChild(departed: Instant): Child {
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.DepartureRequest(departed)))
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun markDepartedAssertFail(departed: Instant, status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.DepartureRequest(departed)))
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun returnToPresentAssertOkOneChild(): Child {
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/return-to-present")
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun returnToPresentAssertFail(status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/return-to-present")
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun expectOneChild(): Child {
        val response = getAttendances()
        assertEquals(1, response.children.size)
        return response.children.first()
    }
}
