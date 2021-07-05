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
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.createMobileDeviceToUnit
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.utils.europeHelsinki
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttendanceTransitionsIntegrationTest : FullApplicationTest() {
    private val userId = UUID.randomUUID()
    private val mobileUser = AuthenticatedUser.MobileDevice(userId)
    private val groupId = UUID.randomUUID()
    private val groupName = "Testaajat"
    private val daycarePlacementId = UUID.randomUUID()
    private val placementStart = LocalDate.now().minusDays(30)
    private val placementEnd = LocalDate.now().plusDays(30)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
            tx.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            tx.createMobileDeviceToUnit(userId, testDaycare.id)
        }
    }

    @Test
    fun `post child arrives - happy case`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()

        val arrived = roundedTimeNow()
        val child = markArrivedAssertOkOneChild(arrived)

        assertEquals(AttendanceStatus.PRESENT, child.status)
        assertNotNull(child.attendance)
        assertEquals(arrived, LocalTime.ofInstant(child.attendance!!.arrived, europeHelsinki))
        assertNull(child.attendance!!.departed)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post child arrives - arriving twice is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent()

        val arrived = roundedTimeNow()
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
    fun `get child departure info - preschool_daycare placement and present from 8 to 14 means no absence`() {
        val arrived = LocalTime.of(8, 0)
        val departed = LocalTime.of(14, 0)
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk(departed)
        assertTrue(info.absentFrom.isEmpty())
    }

    @Test
    fun `get child departure info - preschool_daycare placement and present from 9 to 13 means absence from preschool_daycare`() {
        val arrived = LocalTime.of(9, 0)
        val departed = LocalTime.of(13, 0)
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk(departed)
        assertEquals(setOf(CareType.PRESCHOOL_DAYCARE), info.absentFrom)
    }

    @Test
    fun `get child departure info - preschool_daycare placement and present from 1230 to 17 means absence from preschool`() {
        val arrived = LocalTime.of(12, 30)
        val departed = LocalTime.of(17, 0)
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk(departed)
        assertEquals(setOf(CareType.PRESCHOOL), info.absentFrom)
    }

    @Test
    fun `get child departure info - preschool_daycare placement and present from 0850 to 0930 means absence from preschool and preschool_daycare`() {
        val arrived = LocalTime.of(8, 50)
        val departed = LocalTime.of(9, 30)
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk(departed)
        assertEquals(setOf(CareType.PRESCHOOL, CareType.PRESCHOOL_DAYCARE), info.absentFrom)
    }

    @Test
    fun `get child departure info - preparatory_daycare placement and present from 8 to 15 means no absence`() {
        val arrived = LocalTime.of(8, 0)
        val departed = LocalTime.of(15, 0)
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk(departed)
        assertTrue(info.absentFrom.isEmpty())
    }

    @Test
    fun `get child departure info - preparatory_daycare placement and present from 9 to 14 means absence from preschool_daycare`() {
        val arrived = LocalTime.of(9, 0)
        val departed = LocalTime.of(14, 0)
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk(departed)
        assertEquals(setOf(CareType.PRESCHOOL_DAYCARE), info.absentFrom)
    }

    @Test
    fun `get child departure info - preparatory_daycare placement and present from 1330 to 17 means absence from preschool`() {
        val arrived = LocalTime.of(13, 30)
        val departed = LocalTime.of(17, 0)
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk(departed)
        assertEquals(setOf(CareType.PRESCHOOL), info.absentFrom)
    }

    @Test
    fun `get child departure info - preparatory_daycare placement and present from 0850 to 0930 means absence from preschool and preschool_daycare`() {
        val arrived = LocalTime.of(8, 50)
        val departed = LocalTime.of(9, 30)
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk(departed)
        assertEquals(setOf(CareType.PRESCHOOL, CareType.PRESCHOOL_DAYCARE), info.absentFrom)
    }

    @Test
    fun `get child departure info - not yet present is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()
        getDepartureInfoAssertFail(roundedTimeNow(), 409)
    }

    @Test
    fun `get child departure info - already departed is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildDeparted()
        getDepartureInfoAssertFail(roundedTimeNow(), 409)
    }

    @Test
    fun `post child departs - happy case`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(LocalTime.of(8, 0))

        val departed = LocalTime.of(16, 0)
        val child = markDepartedAssertOkOneChild(departed, absenceType = null)

        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendance)
        assertEquals(departed, LocalTime.ofInstant(child.attendance!!.departed, europeHelsinki))
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post child departs - absent from preschool_daycare`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(LocalTime.of(9, 0))

        val departed = LocalTime.of(13, 0)
        val absenceType = AbsenceType.OTHER_ABSENCE
        val child = markDepartedAssertOkOneChild(departed, absenceType)

        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendance)
        assertEquals(departed, LocalTime.ofInstant(child.attendance!!.departed, europeHelsinki))
        assertEquals(1, child.absences.size)
        assertEquals(CareType.PRESCHOOL_DAYCARE, child.absences.first().careType)
    }

    @Test
    fun `post child departs - absent from preschool`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(LocalTime.of(12, 45))

        val departed = LocalTime.of(18, 0)
        val absenceType = AbsenceType.UNKNOWN_ABSENCE
        val child = markDepartedAssertOkOneChild(departed, absenceType)

        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendance)
        assertEquals(departed, LocalTime.ofInstant(child.attendance!!.departed, europeHelsinki))
        assertEquals(1, child.absences.size)
        assertEquals(CareType.PRESCHOOL, child.absences.first().careType)
    }

    @Test
    fun `post child departs - absent from preschool and preschool_daycare`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(LocalTime.of(8, 50))

        val departed = LocalTime.of(9, 30)
        val absenceType = AbsenceType.SICKLEAVE
        val child = markDepartedAssertOkOneChild(departed, absenceType)

        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendance)
        assertEquals(departed, LocalTime.ofInstant(child.attendance!!.departed, europeHelsinki))
        assertEquals(2, child.absences.size)
        assertTrue(child.absences.any { it.careType == CareType.PRESCHOOL })
        assertTrue(child.absences.any { it.careType == CareType.PRESCHOOL_DAYCARE })
    }

    @Test
    fun `post child departs - departing twice is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildDeparted()

        markDepartedAssertFail(roundedTimeNow(), null, 409)
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
        // previous day attendance should have no effect
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = ZonedDateTime.now(europeHelsinki).minusDays(1).minusHours(1).toInstant(),
                departed = ZonedDateTime.now(europeHelsinki).minusDays(1).minusMinutes(1).toInstant()
            )
        }
        givenChildPlacement(PlacementType.PRESCHOOL)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(1, child.absences.size)
        assertTrue(child.absences.any { it.careType == CareType.PRESCHOOL })
    }

    @Test
    fun `post full day absence - happy case when coming to preschool_daycare`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(2, child.absences.size)
        assertTrue(child.absences.any { it.careType == CareType.PRESCHOOL })
        assertTrue(child.absences.any { it.careType == CareType.PRESCHOOL_DAYCARE })
    }

    @Test
    fun `post full day absence - happy case when coming to preparatory`() {
        givenChildPlacement(PlacementType.PREPARATORY)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(1, child.absences.size)
        assertTrue(child.absences.any { it.careType == CareType.PRESCHOOL })
    }

    @Test
    fun `post full day absence - happy case when coming to prepaatory_daycare`() {
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(2, child.absences.size)
        assertTrue(child.absences.any { it.careType == CareType.PRESCHOOL })
        assertTrue(child.absences.any { it.careType == CareType.PRESCHOOL_DAYCARE })
    }

    @Test
    fun `post full day absence - happy case when coming to daycare`() {
        givenChildPlacement(PlacementType.DAYCARE)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(1, child.absences.size)
        assertTrue(child.absences.any { it.careType == CareType.DAYCARE })
    }

    @Test
    fun `post full day absence - happy case when coming to daycare_part_time`() {
        givenChildPlacement(PlacementType.DAYCARE_PART_TIME)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(1, child.absences.size)
        assertTrue(child.absences.any { it.careType == CareType.DAYCARE })
    }

    @Test
    fun `post full day absence - happy case when coming to club`() {
        givenChildPlacement(PlacementType.CLUB)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(1, child.absences.size)
        assertTrue(child.absences.any { it.careType == CareType.CLUB })
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
            tx.insertTestPlacement(
                id = daycarePlacementId,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = placementType
            )
            tx.insertTestDaycareGroupPlacement(
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

    private fun givenChildPresent(arrived: LocalTime = roundedTimeNow().minusHours(1)) {
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = ZonedDateTime.of(LocalDate.now(europeHelsinki).atTime(arrived), europeHelsinki).toInstant(),
                departed = null
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.PRESENT, child.status)
    }

    private fun givenChildDeparted(
        arrived: LocalTime = roundedTimeNow().minusHours(1),
        departed: LocalTime = roundedTimeNow().minusMinutes(10)
    ) {
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = ZonedDateTime.of(LocalDate.now(europeHelsinki).atTime(arrived), europeHelsinki).toInstant(),
                departed = ZonedDateTime.of(LocalDate.now(europeHelsinki).atTime(departed), europeHelsinki).toInstant()
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.DEPARTED, child.status)
    }

    private fun givenChildAbsent(absentFrom: List<CareType>, absenceType: AbsenceType) {
        absentFrom.forEach { careType ->
            db.transaction {
                it.insertTestAbsence(childId = testChild_1.id, absenceType = absenceType, careType = careType, date = LocalDate.now())
            }
        }
    }

    private fun getAttendances(): AttendanceResponse {
        val (_, res, result) = http.get("/attendances/units/${testDaycare.id}")
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun markArrivedAssertOkOneChild(arrived: LocalTime): Child {
        val time = arrived.format(DateTimeFormatter.ofPattern("HH:mm"))
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival")
            .jsonBody("{\"arrived\": \"$time\"}") // test HH:mm deserialization
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun markArrivedAssertFail(arrived: LocalTime, status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.ArrivalRequest(arrived)))
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun returnToComingAssertOkOneChild(): Child {
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/return-to-coming")
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun returnToComingAssertFail(status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/return-to-coming")
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun getDepartureInfoAssertOk(departed: LocalTime): ChildAttendanceController.DepartureInfoResponse {
        val time = departed.format(DateTimeFormatter.ofPattern("HH:mm"))
        val (_, res, result) = http.get("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure?time=$time")
            .asUser(mobileUser)
            .responseObject<ChildAttendanceController.DepartureInfoResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getDepartureInfoAssertFail(departed: LocalTime, status: Int) {
        val time = departed.format(DateTimeFormatter.ofPattern("HH:mm"))
        val (_, res, _) = http.get("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure?time=$time")
            .asUser(mobileUser)
            .responseObject<ChildAttendanceController.DepartureInfoResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun markDepartedAssertOkOneChild(departed: LocalTime, absenceType: AbsenceType?): Child {
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.DepartureRequest(departed, absenceType)))
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun markDepartedAssertFail(departed: LocalTime, absenceType: AbsenceType?, status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.DepartureRequest(departed, absenceType)))
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun returnToPresentAssertOkOneChild(): Child {
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/return-to-present")
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun returnToPresentAssertFail(status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/return-to-present")
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun markFullDayAbsenceAssertOkOneChild(absenceType: AbsenceType): Child {
        val (_, res, result) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/full-day-absence")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.FullDayAbsenceRequest(absenceType)))
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        val response = result.get()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun markFullDayAbsenceAssertFail(absenceType: AbsenceType, status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/full-day-absence")
            .jsonBody(objectMapper.writeValueAsString(ChildAttendanceController.FullDayAbsenceRequest(absenceType)))
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(objectMapper)

        assertEquals(status, res.statusCode)
    }

    private fun expectOneChild(): Child {
        val response = getAttendances()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun roundedTimeNow() = LocalTime.now(europeHelsinki).withSecond(0).withNano(0)
}
