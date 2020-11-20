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
import fi.espoo.evaka.shared.utils.zoneId
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
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
        db.transaction { tx ->
            tx.resetDatabase()
            insertGeneralTestFixtures(tx.handle)
            tx.handle.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            updateDaycareAclWithEmployee(tx.handle, testDaycare.id, staffUser.id, UserRole.STAFF)
        }
    }

    @Test
    fun `get child arrival info - happy case`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()
        val info = getArrivalInfoAssertOk(Instant.now())
        assertEquals(false, info.absentFromPreschool)
    }

    @Test
    fun `get child arrival info - already present is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent()
        getArrivalInfoAssertFail(Instant.now(), 409)
    }

    @Test
    fun `post child arrives - happy case`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()

        val arrived = instantMinuteStart()
        val child = markArrivedAssertOkOneChild(arrived)

        assertEquals(AttendanceStatus.PRESENT, child.status)
        assertNotNull(child.attendance)
        assertEquals(arrived, child.attendance!!.arrived)
        assertNull(child.attendance!!.departed)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post child arrives - arriving twice is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent()

        val arrived = instantMinuteStart()
        markArrivedAssertFail(arrived, 409)
    }

    @Test
    fun `post return to coming - happy case when present`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent()

        val child = returnToComingAssertOkOneChild()

        assertEquals(AttendanceStatus.COMING, child.status)
        assertNull(child.attendance)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post return to coming - happy case when absent`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildAbsent(listOf(CareType.PRESCHOOL, CareType.PRESCHOOL_DAYCARE), AbsenceType.UNKNOWN_ABSENCE)

        val child = returnToComingAssertOkOneChild()

        assertEquals(AttendanceStatus.COMING, child.status)
        assertNull(child.attendance)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post return to coming - error when departed`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildDeparted()
        returnToComingAssertFail(409)
    }

    @Test
    fun `get child departure info - happy case`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent()
        val info = getDepartureInfoAssertOk(instantMinuteStart())
        assertTrue(info.absentFrom.isEmpty())
    }

    @Test
    fun `get child departure info - not yet present is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()
        getDepartureInfoAssertFail(instantMinuteStart(), 409)
    }

    @Test
    fun `get child departure info - already departed is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildDeparted()
        getDepartureInfoAssertFail(instantMinuteStart(), 409)
    }

    @Test
    fun `post child departs - happy case`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent()

        val departed = instantMinuteStart()
        val child = markDepartedAssertOkOneChild(departed)

        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendance)
        assertEquals(departed, child.attendance!!.departed)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post child departs - departing twice is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildDeparted()

        val departed = instantMinuteStart()
        markDepartedAssertFail(departed, 409)
    }

    @Test
    fun `post return to present - happy case when departed`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
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
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()
        returnToPresentAssertFail(409)
    }

    @Test
    fun `post return to present - error when present`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent()
        returnToPresentAssertFail(409)
    }

    @Test
    fun `post return to present - error when absent`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildAbsent(listOf(CareType.PRESCHOOL, CareType.PRESCHOOL_DAYCARE), AbsenceType.UNKNOWN_ABSENCE)
        returnToPresentAssertFail(409)
    }

    @Test
    fun `post full day absence - happy case when coming to preschool`() {
        db.transaction { // previous day attendance should have no effect
            insertTestChildAttendance(
                h = it.handle,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = ZonedDateTime.now(zoneId).minusDays(1).minusHours(1).toInstant(),
                departed = ZonedDateTime.now(zoneId).minusDays(1).minusMinutes(1).toInstant()
            )
        }
        givenChildPlacement(PlacementType.PRESCHOOL)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(1, child.absences.size)
        assertEquals(AbsenceType.SICKLEAVE, child.absences.first { it.careType == CareType.PRESCHOOL }.absenceType)
    }

    @Test
    fun `post full day absence - happy case when coming to preschool_daycare`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(2, child.absences.size)
        assertEquals(AbsenceType.SICKLEAVE, child.absences.first { it.careType == CareType.PRESCHOOL }.absenceType)
        assertEquals(AbsenceType.SICKLEAVE, child.absences.first { it.careType == CareType.PRESCHOOL_DAYCARE }.absenceType)
    }

    @Test
    fun `post full day absence - happy case when coming to preparatory`() {
        givenChildPlacement(PlacementType.PREPARATORY)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(1, child.absences.size)
        assertEquals(AbsenceType.SICKLEAVE, child.absences.first { it.careType == CareType.PRESCHOOL }.absenceType)
    }

    @Test
    fun `post full day absence - happy case when coming to prepaatory_daycare`() {
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(2, child.absences.size)
        assertEquals(AbsenceType.SICKLEAVE, child.absences.first { it.careType == CareType.PRESCHOOL }.absenceType)
        assertEquals(AbsenceType.SICKLEAVE, child.absences.first { it.careType == CareType.PRESCHOOL_DAYCARE }.absenceType)
    }

    @Test
    fun `post full day absence - happy case when coming to daycare`() {
        givenChildPlacement(PlacementType.DAYCARE)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(1, child.absences.size)
        assertEquals(AbsenceType.SICKLEAVE, child.absences.first { it.careType == CareType.DAYCARE }.absenceType)
    }

    @Test
    fun `post full day absence - happy case when coming to daycare_part_time`() {
        givenChildPlacement(PlacementType.DAYCARE_PART_TIME)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(1, child.absences.size)
        assertEquals(AbsenceType.SICKLEAVE, child.absences.first { it.careType == CareType.DAYCARE }.absenceType)
    }

    @Test
    fun `post full day absence - happy case when coming to club`() {
        givenChildPlacement(PlacementType.CLUB)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(1, child.absences.size)
        assertEquals(AbsenceType.SICKLEAVE, child.absences.first { it.careType == CareType.CLUB }.absenceType)
    }

    @Test
    fun `post full day absence - error when no placement`() {
        markFullDayAbsenceAssertFail(AbsenceType.SICKLEAVE, 400)
    }

    @Test
    fun `post full day absence - error when present`() {
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildPresent()

        markFullDayAbsenceAssertFail(AbsenceType.SICKLEAVE, 409)
    }

    @Test
    fun `post full day absence - error when departed`() {
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildDeparted()

        markFullDayAbsenceAssertFail(AbsenceType.SICKLEAVE, 409)
    }

    private fun givenChildPlacement(placementType: PlacementType) {
        db.transaction { tx ->
            insertTestPlacement(
                h = tx.handle,
                id = daycarePlacementId,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = placementType
            )
            insertTestDaycareGroupPlacement(
                h = tx.handle,
                daycarePlacementId = daycarePlacementId,
                groupId = groupId,
                startDate = placementStart,
                endDate = placementEnd
            )
        }
    }

    private fun givenChildComing() {
        val child = expectOneChild()
        assertEquals(AttendanceStatus.COMING, child.status)
    }

    private fun givenChildPresent() {
        val arrived = OffsetDateTime.now(zoneId).minusHours(3).toInstant()
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
    }

    private fun givenChildDeparted() {
        val arrived = OffsetDateTime.now(zoneId).minusHours(3).toInstant()
        val departed = OffsetDateTime.now(zoneId).minusHours(1).toInstant()
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
    }

    private fun givenChildAbsent(absentFrom: List<CareType>, absenceType: AbsenceType) {
        absentFrom.forEach { careType ->
            db.transaction {
                insertTestAbsence(h = it.handle, childId = testChild_1.id, absenceType = absenceType, careType = careType, date = LocalDate.now())
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

    private fun getArrivalInfoAssertOk(arrived: Instant): ChildAttendanceController.ArrivalInfoResponse {
        val time = LocalTime.ofInstant(arrived, zoneId).format(DateTimeFormatter.ofPattern("HH:mm"))
        val (_, res, result) = http.get("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival?time=$time")
            .asUser(staffUser)
            .responseObject<ChildAttendanceController.ArrivalInfoResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getArrivalInfoAssertFail(arrived: Instant, status: Int) {
        val time = LocalTime.ofInstant(arrived, zoneId).format(DateTimeFormatter.ofPattern("HH:mm"))
        val (_, res, _) = http.get("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival?time=$time")
            .asUser(staffUser)
            .responseObject<ChildAttendanceController.ArrivalInfoResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun markArrivedAssertOkOneChild(arrived: Instant): Child {
        val time = LocalTime.ofInstant(arrived, zoneId).format(DateTimeFormatter.ofPattern("HH:mm"))
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival")
            .jsonBody("{\"arrived\": \"$time\"}") // test HH:mm deserialization
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun markArrivedAssertFail(arrived: Instant, status: Int) {
        val time = LocalTime.ofInstant(arrived, zoneId)
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.ArrivalRequest(time)))
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

    private fun getDepartureInfoAssertOk(departed: Instant): ChildAttendanceController.DepartureInfoResponse {
        val time = LocalTime.ofInstant(departed, zoneId).format(DateTimeFormatter.ofPattern("HH:mm"))
        val (_, res, result) = http.get("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure?time=$time")
            .asUser(staffUser)
            .responseObject<ChildAttendanceController.DepartureInfoResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getDepartureInfoAssertFail(departed: Instant, status: Int) {
        val time = LocalTime.ofInstant(departed, zoneId).format(DateTimeFormatter.ofPattern("HH:mm"))
        val (_, res, _) = http.get("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure?time=$time")
            .asUser(staffUser)
            .responseObject<ChildAttendanceController.DepartureInfoResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun markDepartedAssertOkOneChild(departed: Instant): Child {
        val time = LocalTime.ofInstant(departed, zoneId)
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.DepartureRequest(time)))
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun markDepartedAssertFail(departed: Instant, status: Int) {
        val time = LocalTime.ofInstant(departed, zoneId)
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.DepartureRequest(time)))
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

    private fun markFullDayAbsenceAssertOkOneChild(absenceType: AbsenceType): Child {
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/full-day-absence")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.FullDayAbsenceRequest(absenceType)))
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun markFullDayAbsenceAssertFail(absenceType: AbsenceType, status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/full-day-absence")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.FullDayAbsenceRequest(absenceType)))
            .asUser(staffUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun expectOneChild(): Child {
        val response = getAttendances()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun instantMinuteStart(): Instant = ZonedDateTime.now(zoneId)
        .withSecond(0)
        .withNano(0)
        .toInstant()
}
