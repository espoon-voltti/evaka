// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.dailyservicetimes.createChildDailyServiceTimes
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.ReservationResponse
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.createMobileDeviceToUnit
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
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
import org.springframework.beans.factory.annotation.Autowired

class GetAttendancesIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var childAttendanceController: ChildAttendanceController

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
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            tx.insert(DevDaycareGroup(id = groupId2, daycareId = testDaycare2.id, name = groupName))
            tx.insert(
                DevPlacement(
                    id = daycarePlacementId,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = daycarePlacementId,
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            )
            tx.createMobileDeviceToUnit(mobileUser.id, testDaycare.id)
            tx.createMobileDeviceToUnit(mobileUser2.id, testDaycare2.id)
        }
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
        assertFalse(child.backup)

        expectNoChildAttendances()
    }

    @Test
    fun `child is in backup care in another unit`() {
        db.transaction {
            it.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = testDaycare2.id,
                    groupId = groupId2,
                    period = FiniteDateRange(now.toLocalDate(), now.toLocalDate())
                )
            )
        }
        expectNoChildren()
        expectNoChildAttendances()
    }

    @Test
    fun `child is in backup care in same unit`() {
        db.transaction {
            it.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    groupId = groupId,
                    period = FiniteDateRange(now.toLocalDate(), now.toLocalDate())
                )
            )
        }
        val child = expectOneChild()
        expectNoChildAttendances()
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
        val child = expectOneChildAttendance()
        assertEquals(AttendanceStatus.PRESENT, child.status)
        assertNotNull(child.attendances)
        assertEquals(
            arrived.withTime(arrived.toLocalTime().withSecond(0).withNano(0)),
            child.attendances[0].arrived
        )
        assertNull(child.attendances[0].departed)
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
        val child = expectOneChildAttendance()
        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendances)
        assertEquals(
            arrived.withTime(arrived.toLocalTime().withSecond(0).withNano(0)),
            child.attendances[0].arrived
        )
        assertEquals(
            departed.withTime(departed.toLocalTime().withSecond(0).withNano(0)),
            child.attendances[0].departed
        )
        assertEquals(0, child.absences.size)
    }

    @Test
    fun `child is absent`() {
        db.transaction {
            it.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = now.toLocalDate(),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.NONBILLABLE
                )
            )
            it.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = now.toLocalDate(),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE
                )
            )
        }
        val child = expectOneChildAttendance()
        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(emptyList(), child.attendances)
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
            DailyServiceTimesValue.RegularTimes(
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
            DailyServiceTimesValue.IrregularTimes(
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
        val child = expectOneChildAttendance()
        assertEquals(AttendanceStatus.PRESENT, child.status)
        assertNotNull(child.attendances)
        assertEquals(
            arrived.withTime(arrived.toLocalTime().withSecond(0).withNano(0)),
            child.attendances[0].arrived
        )
        assertNull(child.attendances[0].departed)
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
        val child = expectOneChildAttendance()
        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendances)
        assertEquals(
            arrived.withTime(arrived.toLocalTime().withSecond(0).withNano(0)),
            child.attendances[0].arrived
        )
        assertEquals(
            departed.withTime(departed.toLocalTime().withSecond(0).withNano(0)),
            child.attendances[0].departed
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

        val child = expectOneChildAttendance()
        assertEquals(AttendanceStatus.COMING, child.status)
        assertEquals(1, child.attendances.size)
    }

    @Test
    fun `yesterday's arrival is not shown if departed yesterday`() {
        val arrived = now.minusDays(1).withTime(LocalTime.of(12, 30))
        val departed = now.minusDays(1).withTime(LocalTime.of(22, 45))
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = arrived,
                departed = departed
            )
        }

        expectNoChildAttendances()
    }

    @Test
    fun `yesterday's presence is presence in backup care but not in placement unit`() {
        val arrived = now.minusDays(1)
        val backupUnitId = testDaycare2.id
        db.transaction {
            it.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = backupUnitId,
                    groupId = groupId2,
                    period = FiniteDateRange(now.minusDays(1).toLocalDate(), now.toLocalDate())
                )
            )
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = backupUnitId,
                arrived = arrived,
                departed = null
            )
        }
        val childInBackup = expectOneChildAttendance(backupUnitId, mobileUser2)
        assertEquals(AttendanceStatus.PRESENT, childInBackup.status)
        assertNotNull(childInBackup.attendances)
        assertNull(childInBackup.attendances[0].departed)

        // No children in placement unit
        expectNoChildren(testDaycare.id, mobileUser)
    }

    @Test
    fun `endless presence is visible even if placement ended`() {
        val backupUnitId = testDaycare2.id
        db.transaction {
            it.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = backupUnitId,
                    groupId = groupId2,
                    period =
                        FiniteDateRange(
                            now.minusDays(2).toLocalDate(),
                            now.minusDays(1).toLocalDate()
                        )
                )
            )
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = backupUnitId,
                arrived = now.minusDays(2),
                departed = null
            )
        }
        val childInBackup = expectOneChildAttendance(backupUnitId, mobileUser2)
        assertEquals(AttendanceStatus.PRESENT, childInBackup.status)
        assertNotNull(childInBackup.attendances)
        assertNull(childInBackup.attendances[0].departed)

        // No children in placement unit
        expectNoChildren(testDaycare.id, mobileUser)
    }

    @Test
    fun `reservations for children are shown`() {
        val reservationStart = LocalTime.of(8, 0)
        val reservationEnd = LocalTime.of(16, 0)
        db.transaction {
            it.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = now.toLocalDate(),
                    startTime = reservationStart,
                    endTime = reservationEnd,
                    createdBy = mobileUser2.evakaUserId
                )
            )
        }
        val child = expectOneChild()
        assertEquals(
            listOf(ReservationResponse.Times(TimeRange(reservationStart, reservationEnd), true)),
            child.reservations
        )
    }

    @Test
    fun `reservations with no times for children are shown`() {
        db.transaction {
            it.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = now.toLocalDate(),
                    startTime = null,
                    endTime = null,
                    createdBy = mobileUser2.evakaUserId
                )
            )
        }
        val child = expectOneChild()
        assertEquals(listOf(ReservationResponse.NoTimes(true)), child.reservations)
    }

    @Test
    fun `reservations for children in backup care are shown`() {
        val backupUnitId = testDaycare2.id
        val reservationStart = LocalTime.of(8, 0)
        val reservationEnd = LocalTime.of(16, 0)
        db.transaction {
            it.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = backupUnitId,
                    groupId = groupId2,
                    period = FiniteDateRange(now.minusDays(1).toLocalDate(), now.toLocalDate())
                )
            )
            it.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = now.toLocalDate(),
                    startTime = reservationStart,
                    endTime = reservationEnd,
                    createdBy = mobileUser2.evakaUserId
                )
            )
        }
        val childInBackup = expectOneChild(backupUnitId, mobileUser2)
        assertEquals(
            listOf(ReservationResponse.Times(TimeRange(reservationStart, reservationEnd), true)),
            childInBackup.reservations
        )
    }

    private fun getChildren(
        unitId: DaycareId = testDaycare.id,
        user: AuthenticatedUser = mobileUser
    ): List<AttendanceChild> {
        return childAttendanceController.getChildren(
            dbInstance(),
            user,
            MockEvakaClock(now),
            unitId
        )
    }

    private fun expectOneChild(
        unitId: DaycareId = testDaycare.id,
        user: AuthenticatedUser = mobileUser
    ): AttendanceChild {
        val children = getChildren(unitId, user)
        assertEquals(1, children.size)
        return children[0]
    }

    private fun expectNoChildren(
        unitId: DaycareId = testDaycare.id,
        user: AuthenticatedUser = mobileUser
    ) {
        val children = getChildren(unitId, user)
        assertEquals(0, children.size)
    }

    private fun getAttendanceStatuses(
        unitId: DaycareId = testDaycare.id,
        user: AuthenticatedUser = mobileUser
    ): Map<ChildId, ChildAttendanceController.ChildAttendanceStatusResponse> {
        return childAttendanceController.getAttendanceStatuses(
            dbInstance(),
            user,
            MockEvakaClock(now),
            unitId
        )
    }

    private fun expectOneChildAttendance(
        unitId: DaycareId = testDaycare.id,
        user: AuthenticatedUser = mobileUser
    ): ChildAttendanceController.ChildAttendanceStatusResponse {
        val response = getAttendanceStatuses(unitId, user)
        assertEquals(1, response.size)
        return response.values.first()
    }

    private fun expectNoChildAttendances(
        unitId: DaycareId = testDaycare.id,
        user: AuthenticatedUser = mobileUser
    ) {
        val response = getAttendanceStatuses(unitId, user)
        assertEquals(0, response.size)
    }
}
