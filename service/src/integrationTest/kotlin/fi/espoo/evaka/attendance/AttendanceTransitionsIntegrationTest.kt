// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.createMobileDeviceToUnit
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.europeHelsinki
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttendanceTransitionsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val mobileUser = AuthenticatedUser.MobileDevice(MobileDeviceId(UUID.randomUUID()))
    private val groupId = GroupId(UUID.randomUUID())
    private val groupName = "Testaajat"
    private val daycarePlacementId = PlacementId(UUID.randomUUID())
    private val placementStart = LocalDate.now().minusDays(30)
    private val placementEnd = LocalDate.now().plusDays(30)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            tx.createMobileDeviceToUnit(mobileUser.id, testDaycare.id)
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
        assertEquals(arrived, child.attendance?.arrived?.toLocalTime()?.withSecond(0)?.withNano(0))
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
        givenChildAbsent(AbsenceType.UNKNOWN_ABSENCE, AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE)

        val child = returnToComingAssertOkOneChild()

        assertEquals(AttendanceStatus.COMING, child.status)
        assertNull(child.attendance)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `get child departure info - preschool daycare placement and present from preschool start`() {
        val arrived = LocalTime.of(9, 0)
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk()
        assertEquals(
            listOf(
                AbsenceThreshold(AbsenceCategory.NONBILLABLE, LocalTime.of(10, 0)),
                AbsenceThreshold(AbsenceCategory.BILLABLE, LocalTime.of(13, 15))
            ),
            info
        )
    }

    @Test
    fun `get child departure info - preschool daycare placement and present hour before preschool start`() {
        val arrived = LocalTime.of(8, 0)
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk()
        assertEquals(
            listOf(AbsenceThreshold(AbsenceCategory.NONBILLABLE, LocalTime.of(10, 0))),
            info
        )
    }

    @Test
    fun `get child departure info - preschool daycare placement and present from 1230`() {
        val arrived = LocalTime.of(12, 30)
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk()
        assertEquals(
            listOf(
                AbsenceThreshold(AbsenceCategory.NONBILLABLE, LocalTime.of(23, 59)),
                AbsenceThreshold(AbsenceCategory.BILLABLE, LocalTime.of(13, 15))
            ),
            info
        )
    }

    @Test
    fun `get child departure info - preschool daycare placement and present from 0850`() {
        val arrived = LocalTime.of(8, 50)
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk()
        assertEquals(
            listOf(
                AbsenceThreshold(AbsenceCategory.NONBILLABLE, LocalTime.of(10, 0)),
                AbsenceThreshold(AbsenceCategory.BILLABLE, LocalTime.of(13, 15))
            ),
            info
        )
    }

    @Test
    fun `get child departure info - preparatory daycare placement and present from 8`() {
        val arrived = LocalTime.of(8, 0)
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk()
        assertEquals(
            listOf(AbsenceThreshold(AbsenceCategory.NONBILLABLE, LocalTime.of(10, 0))),
            info
        )
    }

    @Test
    fun `get child departure info - preparatory daycare placement and present from 9`() {
        val arrived = LocalTime.of(9, 0)
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk()
        assertEquals(
            listOf(
                AbsenceThreshold(AbsenceCategory.NONBILLABLE, LocalTime.of(10, 0)),
                AbsenceThreshold(AbsenceCategory.BILLABLE, LocalTime.of(14, 15))
            ),
            info
        )
    }

    @Test
    fun `get child departure info - preparatory daycare placement and present from 1330`() {
        val arrived = LocalTime.of(13, 30)
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk()
        assertEquals(
            listOf(
                AbsenceThreshold(AbsenceCategory.NONBILLABLE, LocalTime.of(23, 59)),
                AbsenceThreshold(AbsenceCategory.BILLABLE, LocalTime.of(14, 15))
            ),
            info
        )
    }

    @Test
    fun `get child departure info - preparatory daycare placement and present from 0850`() {
        val arrived = LocalTime.of(8, 50)
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildPresent(arrived)

        val info = getDepartureInfoAssertOk()
        assertEquals(
            listOf(
                AbsenceThreshold(AbsenceCategory.NONBILLABLE, LocalTime.of(10, 0)),
                AbsenceThreshold(AbsenceCategory.BILLABLE, LocalTime.of(14, 15))
            ),
            info
        )
    }

    @Test
    fun `get child departure info - not yet present is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()
        getDepartureInfoAssertFail(409)
    }

    @Test
    fun `get child departure info - already departed is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildDeparted()
        getDepartureInfoAssertFail(409)
    }

    @Test
    fun `post child departs - happy case`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(LocalTime.of(8, 0))

        val departed = LocalTime.of(16, 0)
        val child = markDepartedAssertOkOneChild(departed, absenceType = null)

        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendance)
        assertEquals(departed, child.attendance?.departed?.toLocalTime()?.withSecond(0)?.withNano(0))
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
        assertEquals(departed, child.attendance?.departed?.toLocalTime()?.withSecond(0)?.withNano(0))
        assertContentEquals(listOf(AbsenceCategory.BILLABLE), child.absences.map { it.category })
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
        assertEquals(departed, child.attendance?.departed?.toLocalTime()?.withSecond(0)?.withNano(0))
        assertContentEquals(listOf(AbsenceCategory.NONBILLABLE), child.absences.map { it.category })
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
        assertEquals(departed, child.attendance?.departed?.toLocalTime()?.withSecond(0)?.withNano(0))
        assertContentEquals(listOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post child departs - multi day attendance that ends at midnight`() {
        givenChildPlacement(PlacementType.DAYCARE)
        givenChildPresent(LocalTime.of(8, 50), LocalDate.now().minusDays(1))

        val departed = LocalTime.of(0, 0)
        val child = markDepartedAssertOkOneChild(departed, null)

        assertEquals(AttendanceStatus.COMING, child.status)
        assertNull(child.attendance)
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
    fun `post full day absence - happy case when coming to preschool`() {
        // previous day attendance should have no effect
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = HelsinkiDateTime.now().minusDays(1).minusHours(1),
                departed = HelsinkiDateTime.now().minusDays(1).minusMinutes(1)
            )
        }
        givenChildPlacement(PlacementType.PRESCHOOL)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertContentEquals(listOf(AbsenceCategory.NONBILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post full day absence - happy case when coming to preschool_daycare`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertContentEquals(listOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post full day absence - happy case when coming to preparatory`() {
        givenChildPlacement(PlacementType.PREPARATORY)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertContentEquals(listOf(AbsenceCategory.NONBILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post full day absence - happy case when coming to preparatory_daycare`() {
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertContentEquals(listOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post full day absence - happy case when coming to daycare`() {
        givenChildPlacement(PlacementType.DAYCARE)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertContentEquals(listOf(AbsenceCategory.BILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post full day absence - happy case when coming to daycare_part_time`() {
        givenChildPlacement(PlacementType.DAYCARE_PART_TIME)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertContentEquals(listOf(AbsenceCategory.BILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post full day absence - happy case when coming to club`() {
        givenChildPlacement(PlacementType.CLUB)
        givenChildComing()

        val child = markFullDayAbsenceAssertOkOneChild(AbsenceType.SICKLEAVE)

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertContentEquals(listOf(AbsenceCategory.NONBILLABLE), child.absences.map { it.category })
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

    private fun givenChildPresent(arrived: LocalTime = roundedTimeNow().minusHours(1), date: LocalDate = LocalDate.now()) {
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = HelsinkiDateTime.of(date, arrived),
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
                arrived = HelsinkiDateTime.now().withTime(arrived),
                departed = HelsinkiDateTime.now().withTime(departed)
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.DEPARTED, child.status)
    }

    private fun givenChildAbsent(absenceType: AbsenceType, vararg categories: AbsenceCategory) {
        categories.forEach { category ->
            db.transaction {
                it.insertTestAbsence(childId = testChild_1.id, absenceType = absenceType, category = category, date = LocalDate.now())
            }
        }
    }

    private fun getAttendances(): AttendanceResponse {
        val (_, res, result) = http.get("/attendances/units/${testDaycare.id}")
            .asUser(mobileUser)
            .responseObject<AttendanceResponse>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun markArrivedAssertOkOneChild(arrived: LocalTime): Child {
        val time = arrived.format(DateTimeFormatter.ofPattern("HH:mm"))
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival")
            .jsonBody("{\"arrived\": \"$time\"}") // test HH:mm deserialization
            .asUser(mobileUser)
            .response()

        assertEquals(200, res.statusCode)
        val response = getAttendances()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun markArrivedAssertFail(arrived: LocalTime, status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/arrival")
            .jsonBody(jsonMapper.writeValueAsString(ChildAttendanceController.ArrivalRequest(arrived)))
            .asUser(mobileUser)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun returnToComingAssertOkOneChild(): Child {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/return-to-coming")
            .asUser(mobileUser)
            .response()

        assertEquals(200, res.statusCode)
        val response = getAttendances()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun returnToComingAssertFail(status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/return-to-coming")
            .asUser(mobileUser)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun getDepartureInfoAssertOk(): List<AbsenceThreshold> {
        val (_, res, result) = http.get("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure")
            .asUser(mobileUser)
            .responseObject<List<AbsenceThreshold>>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getDepartureInfoAssertFail(status: Int) {
        val (_, res, _) = http.get("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure")
            .asUser(mobileUser)
            .responseObject<AbsenceThreshold>(jsonMapper)

        assertEquals(status, res.statusCode)
    }

    private fun markDepartedAssertOkOneChild(departed: LocalTime, absenceType: AbsenceType?): Child {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure")
            .jsonBody(jsonMapper.writeValueAsString(ChildAttendanceController.DepartureRequest(departed, absenceType)))
            .asUser(mobileUser)
            .response()

        assertEquals(200, res.statusCode)
        val response = getAttendances()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun markDepartedAssertFail(departed: LocalTime, absenceType: AbsenceType?, status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/departure")
            .jsonBody(jsonMapper.writeValueAsString(ChildAttendanceController.DepartureRequest(departed, absenceType)))
            .asUser(mobileUser)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun returnToPresentAssertOkOneChild(): Child {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/return-to-present")
            .asUser(mobileUser)
            .response()

        assertEquals(200, res.statusCode)
        val response = getAttendances()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun returnToPresentAssertFail(status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/return-to-present")
            .asUser(mobileUser)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun markFullDayAbsenceAssertOkOneChild(absenceType: AbsenceType): Child {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/full-day-absence")
            .jsonBody(jsonMapper.writeValueAsString(ChildAttendanceController.FullDayAbsenceRequest(absenceType)))
            .asUser(mobileUser)
            .response()

        assertEquals(200, res.statusCode)
        val response = getAttendances()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun markFullDayAbsenceAssertFail(absenceType: AbsenceType, status: Int) {
        val (_, res, _) = http.post("/attendances/units/${testDaycare.id}/children/${testChild_1.id}/full-day-absence")
            .jsonBody(jsonMapper.writeValueAsString(ChildAttendanceController.FullDayAbsenceRequest(absenceType)))
            .asUser(mobileUser)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun expectOneChild(): Child {
        val response = getAttendances()
        assertEquals(1, response.children.size)
        return response.children.first()
    }

    private fun roundedTimeNow() = LocalTime.now(europeHelsinki).withSecond(0).withNano(0)
}
