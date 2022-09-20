// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.dailyservicetimes.createChildDailyServiceTimes
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.createMobileDeviceToUnit
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestBackUpCare
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestReservation
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.withMockedTime
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetAttendancesIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val mobileUser = AuthenticatedUser.MobileDevice(MobileDeviceId(UUID.randomUUID()))
    private val mobileUser2 = AuthenticatedUser.MobileDevice(MobileDeviceId(UUID.randomUUID()))
    private val groupId = GroupId(UUID.randomUUID())
    private val groupId2 = GroupId(UUID.randomUUID())
    private val groupName = "Testaajat"
    private val daycarePlacementId = PlacementId(UUID.randomUUID())
    private val now = HelsinkiDateTime.of(LocalDate.of(2022, 2, 3), LocalTime.of(12, 5, 1))
    private val placementStart = now.toLocalDate().minusDays(30)
    private val placementEnd = now.toLocalDate().plusDays(30)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertTestDaycareGroup(
                DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName)
            )
            tx.insertTestDaycareGroup(
                DevDaycareGroup(id = groupId2, daycareId = testDaycare2.id, name = groupName)
            )
            tx.insertTestPlacement(
                id = daycarePlacementId,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = daycarePlacementId,
                groupId = groupId,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.createMobileDeviceToUnit(mobileUser.id, testDaycare.id)
            tx.createMobileDeviceToUnit(mobileUser2.id, testDaycare2.id)
        }
    }

    // TODO move elsewhere too
    @Test
    fun `unit info is correct`() {
        val response = fetchUnitInfo()
        assertEquals(testDaycare.name, response.name)
        assertEquals(1, response.groups.size)
        assertEquals(groupId, response.groups.first().id)
        assertEquals(groupName, response.groups.first().name)
        assertEquals("Supervisor", response.staff.first().lastName)
    }

    @Test
    fun `child is coming`() {
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = now.minusDays(1).minusHours(8),
                departed = now.minusDays(1)
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.COMING, child.status)
        assertNull(child.attendance)
        assertEquals(0, child.absences.size)
        assertFalse(child.backup)
    }

    @Test
    fun `child is in backup care in another unit`() {
        db.transaction {
            it.insertTestBackUpCare(
                childId = testChild_1.id,
                unitId = testDaycare2.id,
                groupId = groupId2,
                startDate = now.toLocalDate(),
                endDate = now.toLocalDate(),
            )
        }
        val response = fetchAttendances()
        assertEquals(0, response.children.size)
    }

    @Test
    fun `child is in backup care in same another unit`() {
        db.transaction {
            it.insertTestBackUpCare(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                groupId = groupId,
                startDate = now.toLocalDate(),
                endDate = now.toLocalDate(),
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.COMING, child.status)
        assertNull(child.attendance)
        assertEquals(0, child.absences.size)
        assertTrue(child.backup)
    }

    @Test
    fun `child is present`() {
        val arrived = now.minusHours(3)
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = arrived,
                departed = null
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.PRESENT, child.status)
        assertNotNull(child.attendance)
        assertEquals(
            arrived.withTime(arrived.toLocalTime().withSecond(0).withNano(0)),
            child.attendance!!.arrived
        )
        assertNull(child.attendance!!.departed)
        assertEquals(0, child.absences.size)
    }

    @Test
    fun `child has departed`() {
        val arrived = now.minusHours(3)
        val departed = now.minusMinutes(1)
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = arrived,
                departed = departed
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendance)
        assertEquals(
            arrived.withTime(arrived.toLocalTime().withSecond(0).withNano(0)),
            child.attendance!!.arrived
        )
        assertEquals(
            departed.withTime(departed.toLocalTime().withSecond(0).withNano(0)),
            child.attendance!!.departed
        )
        assertEquals(0, child.absences.size)
    }

    @Test
    fun `child is absent`() {
        db.transaction {
            it.insertTestAbsence(
                childId = testChild_1.id,
                category = AbsenceCategory.NONBILLABLE,
                date = now.toLocalDate(),
                absenceType = AbsenceType.SICKLEAVE
            )
            it.insertTestAbsence(
                childId = testChild_1.id,
                category = AbsenceCategory.BILLABLE,
                date = now.toLocalDate(),
                absenceType = AbsenceType.SICKLEAVE
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertNull(child.attendance)
        assertEquals(
            setOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE),
            child.absences.map { it.category }.toSet()
        )
    }

    @Test
    fun `child has no daily service times`() {
        val child = expectOneChild()
        assertNull(child.dailyServiceTimes)
    }

    @Test
    fun `child has regular daily service times`() {
        val testTimes =
            DailyServiceTimes.RegularTimes(
                regularTimes = TimeRange(LocalTime.of(8, 15), LocalTime.of(17, 19)),
                validityPeriod = DateRange(now.toLocalDate().minusDays(10), null)
            )
        db.transaction { tx ->
            tx.createChildDailyServiceTimes(childId = testChild_1.id, times = testTimes)
        }
        val child = expectOneChild()
        assertEquals(testTimes, child.dailyServiceTimes)
    }

    @Test
    fun `child has irregular daily service times`() {
        val testTimes =
            DailyServiceTimes.IrregularTimes(
                monday = TimeRange(LocalTime.of(8, 15), LocalTime.of(17, 19)),
                tuesday = TimeRange(LocalTime.of(8, 16), LocalTime.of(17, 19)),
                wednesday = TimeRange(LocalTime.of(8, 17), LocalTime.of(17, 19)),
                thursday = null,
                friday = TimeRange(LocalTime.of(8, 19), LocalTime.of(17, 19)),
                saturday = null,
                sunday = TimeRange(LocalTime.of(10, 0), LocalTime.of(17, 30)),
                validityPeriod = DateRange(now.toLocalDate().minusDays(10), null)
            )
        db.transaction { tx ->
            tx.createChildDailyServiceTimes(childId = testChild_1.id, times = testTimes)
        }
        val child = expectOneChild()
        assertEquals(testTimes, child.dailyServiceTimes)
    }

    @Test
    fun `yesterday's arrival is present if no departure has been set`() {
        val arrived = now.minusDays(1)
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = arrived,
                departed = null
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.PRESENT, child.status)
        assertNotNull(child.attendance)
        assertEquals(
            arrived.withTime(arrived.toLocalTime().withSecond(0).withNano(0)),
            child.attendance!!.arrived
        )
        assertNull(child.attendance!!.departed)
        assertEquals(0, child.absences.size)
    }

    @Test
    fun `yesterday's arrival is departed if departure time is within last 30 minutes`() {
        val arrived = now.minusDays(1)
        val departed = now.minusMinutes(25)
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = arrived,
                departed = departed
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendance)
        assertEquals(
            arrived.withTime(arrived.toLocalTime().withSecond(0).withNano(0)),
            child.attendance!!.arrived
        )
        assertEquals(
            departed.withTime(departed.toLocalTime().withSecond(0).withNano(0)),
            child.attendance!!.departed
        )
        assertEquals(0, child.absences.size)
    }

    @Test
    fun `yesterday's arrival is coming if departure time is over 30 minutes ago`() {
        val arrived = now.minusDays(1)
        val departed = now.minusMinutes(35)
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = arrived,
                departed = departed
            )
        }
        val child = expectOneChild()
        assertEquals(AttendanceStatus.COMING, child.status)
        assertNull(child.attendance)
        assertEquals(0, child.absences.size)
    }

    @Test
    fun `yesterday's presence is presence in backup care but not in placement unit`() {
        val arrived = now.minusDays(1)
        val backupUnitId = testDaycare2.id
        db.transaction {
            it.insertTestBackUpCare(
                childId = testChild_1.id,
                unitId = backupUnitId,
                groupId = groupId2,
                startDate = now.minusDays(1).toLocalDate(),
                endDate = now.toLocalDate()
            )
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = backupUnitId,
                arrived = arrived,
                departed = null
            )
        }
        val childInBackup = expectOneChild(backupUnitId, mobileUser2)
        assertEquals(AttendanceStatus.PRESENT, childInBackup.status)
        assertNotNull(childInBackup.attendance)
        assertNull(childInBackup.attendance?.departed)

        val childrenInPlacementUnit = fetchAttendances()
        assertEquals(0, childrenInPlacementUnit.children.size)
    }

    @Test
    fun `endless presence is visible even if placement ended`() {
        val backupUnitId = testDaycare2.id
        db.transaction {
            it.insertTestBackUpCare(
                childId = testChild_1.id,
                unitId = backupUnitId,
                groupId = groupId2,
                startDate = now.minusDays(2).toLocalDate(),
                endDate = now.minusDays(1).toLocalDate()
            )
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = backupUnitId,
                arrived = now.minusDays(2),
                departed = null
            )
        }
        val childInBackup = expectOneChild(backupUnitId, mobileUser2)
        assertEquals(AttendanceStatus.PRESENT, childInBackup.status)
        assertNotNull(childInBackup.attendance)
        assertNull(childInBackup.attendance?.departed)

        val childrenInPlacementUnit = fetchAttendances()
        assertEquals(0, childrenInPlacementUnit.children.size)
    }

    @Test
    fun `reservations for children are shown`() {
        val reservationStart = HelsinkiDateTime.of(now.toLocalDate(), LocalTime.of(8, 0))
        val reservationEnd = HelsinkiDateTime.of(now.toLocalDate(), LocalTime.of(16, 0))
        db.transaction {
            it.insertTestReservation(
                DevReservation(
                    childId = testChild_1.id,
                    date = reservationStart.toLocalDate(),
                    startTime = reservationStart.toLocalTime(),
                    endTime = reservationEnd.toLocalTime(),
                    createdBy = mobileUser2.evakaUserId
                )
            )
        }
        val child = expectOneChild()
        assertEquals(
            listOf(AttendanceReservation(reservationStart, reservationEnd)),
            child.reservations
        )
    }

    @Test
    fun `reservations for children in backup care are shown`() {
        val backupUnitId = testDaycare2.id
        val reservationStart = HelsinkiDateTime.of(now.toLocalDate(), LocalTime.of(8, 0))
        val reservationEnd = HelsinkiDateTime.of(now.toLocalDate(), LocalTime.of(16, 0))
        db.transaction {
            it.insertTestBackUpCare(
                childId = testChild_1.id,
                unitId = backupUnitId,
                groupId = groupId2,
                startDate = now.minusDays(1).toLocalDate(),
                endDate = now.toLocalDate()
            )
            it.insertTestReservation(
                DevReservation(
                    childId = testChild_1.id,
                    date = reservationStart.toLocalDate(),
                    startTime = reservationStart.toLocalTime(),
                    endTime = reservationEnd.toLocalTime(),
                    createdBy = mobileUser2.evakaUserId
                )
            )
        }
        val childInBackup = expectOneChild(backupUnitId, mobileUser2)
        assertEquals(
            listOf(AttendanceReservation(reservationStart, reservationEnd)),
            childInBackup.reservations
        )
    }

    private fun fetchUnitInfo(): UnitInfo {
        val (_, res, result) =
            http
                .get("/mobile/units/${testDaycare.id}")
                .asUser(mobileUser)
                .withMockedTime(now)
                .responseObject<UnitInfo>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun fetchAttendances(
        unitId: DaycareId = testDaycare.id,
        user: AuthenticatedUser = mobileUser
    ): AttendanceResponse {
        val (_, res, result) =
            http
                .get("/attendances/units/$unitId")
                .asUser(user)
                .withMockedTime(now)
                .responseObject<AttendanceResponse>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun expectOneChild(
        unitId: DaycareId = testDaycare.id,
        user: AuthenticatedUser = mobileUser
    ): Child {
        val response = fetchAttendances(unitId, user)
        assertEquals(1, response.children.size)
        return response.children.first()
    }
}
