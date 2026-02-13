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
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevMobileDevice
import fi.espoo.evaka.shared.dev.DevPerson
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

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val daycare2 = DevDaycare(areaId = area.id, name = "Test Daycare 2")
    private val child = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))

    private val now = HelsinkiDateTime.of(LocalDate.of(2022, 2, 3), LocalTime.of(12, 5, 1))
    private val mobileUser = AuthenticatedUser.MobileDevice(MobileDeviceId(UUID.randomUUID()))
    private val employee2 = DevEmployee(lastName = "Laite", firstName = "")
    private val mobileDevice2 =
        DevMobileDevice(
            id = MobileDeviceId(employee2.id.raw),
            unitId = daycare2.id,
            name = "Laite ",
        )
    private val groupId = DevDaycareGroup(daycareId = daycare.id, name = "Testaajat")
    private val groupId2 = DevDaycareGroup(daycareId = daycare2.id, name = "Testaajat 2")
    private val daycarePlacement =
        DevPlacement(
            type = PlacementType.PRESCHOOL_DAYCARE,
            childId = child.id,
            unitId = daycare.id,
            startDate = now.toLocalDate().minusDays(30),
            endDate = now.toLocalDate().plusDays(30),
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(groupId)
            tx.insert(groupId2)
            tx.insert(daycarePlacement)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = daycarePlacement.id,
                    daycareGroupId = groupId.id,
                    startDate = daycarePlacement.startDate,
                    endDate = daycarePlacement.endDate,
                )
            )
            tx.createMobileDeviceToUnit(mobileUser.id, daycare.id)
            tx.insert(employee2)
            tx.insert(mobileDevice2)
        }
    }

    @Test
    fun `child is coming`() {
        db.transaction {
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = daycare.id,
                arrived = now.minusDays(1).minusHours(8),
                departed = now.minusDays(1),
            )
        }
        val result = expectOneChild()
        assertFalse(result.backup)

        expectNoChildAttendances()
    }

    @Test
    fun `child is in backup care in another unit`() {
        db.transaction {
            it.insert(
                DevBackupCare(
                    childId = child.id,
                    unitId = daycare2.id,
                    groupId = groupId2.id,
                    period = FiniteDateRange(now.toLocalDate(), now.toLocalDate()),
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
                    childId = child.id,
                    unitId = daycare.id,
                    groupId = groupId.id,
                    period = FiniteDateRange(now.toLocalDate(), now.toLocalDate()),
                )
            )
        }
        val result = expectOneChild()
        expectNoChildAttendances()
        assertTrue(result.backup)
    }

    @Test
    fun `child is present`() {
        val arrived = now.minusHours(3)
        db.transaction {
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = daycare.id,
                arrived = arrived,
                departed = null,
            )
        }
        val result = expectOneChildAttendance()
        assertEquals(AttendanceStatus.PRESENT, result.status)
        assertNotNull(result.attendances)
        assertEquals(
            arrived.withTime(arrived.toLocalTime().withSecond(0).withNano(0)),
            result.attendances[0].arrived,
        )
        assertNull(result.attendances[0].departed)
        assertEquals(0, result.absences.size)
    }

    @Test
    fun `child has departed`() {
        val arrived = now.minusHours(3)
        val departed = now.minusMinutes(1)
        db.transaction {
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = daycare.id,
                arrived = arrived,
                departed = departed,
            )
        }
        val result = expectOneChildAttendance()
        assertEquals(AttendanceStatus.DEPARTED, result.status)
        assertNotNull(result.attendances)
        assertEquals(
            arrived.withTime(arrived.toLocalTime().withSecond(0).withNano(0)),
            result.attendances[0].arrived,
        )
        assertEquals(
            departed.withTime(departed.toLocalTime().withSecond(0).withNano(0)),
            result.attendances[0].departed,
        )
        assertEquals(0, result.absences.size)
    }

    @Test
    fun `child is absent`() {
        db.transaction {
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = now.toLocalDate(),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = now.toLocalDate(),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }
        val result = expectOneChildAttendance()
        assertEquals(AttendanceStatus.ABSENT, result.status)
        assertEquals(emptyList(), result.attendances)
        assertEquals(
            setOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE),
            result.absences.map { it.category }.toSet(),
        )
    }

    @Test
    fun `child has no daily service times`() {
        val result = expectOneChild()
        assertNull(result.dailyServiceTimes)
    }

    @Test
    fun `child has regular daily service times`() {
        val testTimes =
            DailyServiceTimesValue.RegularTimes(
                regularTimes = TimeRange(LocalTime.of(8, 15), LocalTime.of(17, 19)),
                validityPeriod = DateRange(now.toLocalDate().minusDays(10), null),
            )
        db.transaction { tx ->
            tx.createChildDailyServiceTimes(childId = child.id, times = testTimes)
        }
        val result = expectOneChild()
        assertEquals(testTimes, result.dailyServiceTimes)
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
                validityPeriod = DateRange(now.toLocalDate().minusDays(10), null),
            )
        db.transaction { tx ->
            tx.createChildDailyServiceTimes(childId = child.id, times = testTimes)
        }
        val result = expectOneChild()
        assertEquals(testTimes, result.dailyServiceTimes)
    }

    @Test
    fun `yesterday's arrival is present if no departure has been set`() {
        val arrived = now.minusDays(1)
        db.transaction {
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = daycare.id,
                arrived = arrived,
                departed = null,
            )
        }
        val result = expectOneChildAttendance()
        assertEquals(AttendanceStatus.PRESENT, result.status)
        assertNotNull(result.attendances)
        assertEquals(
            arrived.withTime(arrived.toLocalTime().withSecond(0).withNano(0)),
            result.attendances[0].arrived,
        )
        assertNull(result.attendances[0].departed)
        assertEquals(0, result.absences.size)
    }

    @Test
    fun `yesterday's arrival is departed if departure time is within last 30 minutes`() {
        val arrived = now.minusDays(1)
        val departed = now.minusMinutes(25)
        db.transaction {
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = daycare.id,
                arrived = arrived,
                departed = departed,
            )
        }
        val result = expectOneChildAttendance()
        assertEquals(AttendanceStatus.DEPARTED, result.status)
        assertNotNull(result.attendances)
        assertEquals(
            arrived.withTime(arrived.toLocalTime().withSecond(0).withNano(0)),
            result.attendances[0].arrived,
        )
        assertEquals(
            departed.withTime(departed.toLocalTime().withSecond(0).withNano(0)),
            result.attendances[0].departed,
        )
        assertEquals(0, result.absences.size)
    }

    @Test
    fun `yesterday's arrival is coming if departure time is over 30 minutes ago`() {
        val arrived = now.minusDays(1)
        val departed = now.minusMinutes(35)
        db.transaction {
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = daycare.id,
                arrived = arrived,
                departed = departed,
            )
        }

        val result = expectOneChildAttendance()
        assertEquals(AttendanceStatus.COMING, result.status)
        assertEquals(1, result.attendances.size)
    }

    @Test
    fun `yesterday's arrival is not shown if departed yesterday`() {
        val arrived = now.minusDays(1).withTime(LocalTime.of(12, 30))
        val departed = now.minusDays(1).withTime(LocalTime.of(22, 45))
        db.transaction {
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = daycare.id,
                arrived = arrived,
                departed = departed,
            )
        }

        expectNoChildAttendances()
    }

    @Test
    fun `yesterday's presence is presence in backup care but not in placement unit`() {
        val arrived = now.minusDays(1)
        db.transaction {
            it.insert(
                DevBackupCare(
                    childId = child.id,
                    unitId = daycare2.id,
                    groupId = groupId2.id,
                    period = FiniteDateRange(now.minusDays(1).toLocalDate(), now.toLocalDate()),
                )
            )
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = daycare2.id,
                arrived = arrived,
                departed = null,
            )
        }
        val childInBackup = expectOneChildAttendance(daycare2.id, mobileDevice2.user)
        assertEquals(AttendanceStatus.PRESENT, childInBackup.status)
        assertNotNull(childInBackup.attendances)
        assertNull(childInBackup.attendances[0].departed)

        expectNoChildren(daycare.id, mobileUser)
    }

    @Test
    fun `endless presence is visible even if placement ended`() {
        db.transaction {
            it.insert(
                DevBackupCare(
                    childId = child.id,
                    unitId = daycare2.id,
                    groupId = groupId2.id,
                    period =
                        FiniteDateRange(
                            now.minusDays(2).toLocalDate(),
                            now.minusDays(1).toLocalDate(),
                        ),
                )
            )
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = daycare2.id,
                arrived = now.minusDays(2),
                departed = null,
            )
        }
        val childInBackup = expectOneChildAttendance(daycare2.id, mobileDevice2.user)
        assertEquals(AttendanceStatus.PRESENT, childInBackup.status)
        assertNotNull(childInBackup.attendances)
        assertNull(childInBackup.attendances[0].departed)

        expectNoChildren(daycare.id, mobileUser)
    }

    @Test
    fun `reservations for children are shown`() {
        val reservationStart = LocalTime.of(8, 0)
        val reservationEnd = LocalTime.of(16, 0)
        db.transaction {
            it.insert(
                DevReservation(
                    childId = child.id,
                    date = now.toLocalDate(),
                    startTime = reservationStart,
                    endTime = reservationEnd,
                    createdAt = now,
                    createdBy = employee2.evakaUserId,
                )
            )
        }
        val result = expectOneChild()
        assertEquals(
            listOf(
                ReservationResponse.Times(
                    TimeRange(reservationStart, reservationEnd),
                    true,
                    now,
                    employee2.evakaUser,
                )
            ),
            result.reservations,
        )
    }

    @Test
    fun `reservations with no times for children are shown`() {
        db.transaction {
            it.insert(
                DevReservation(
                    childId = child.id,
                    date = now.toLocalDate(),
                    startTime = null,
                    endTime = null,
                    createdAt = now,
                    createdBy = employee2.evakaUserId,
                )
            )
        }
        val result = expectOneChild()
        assertEquals(
            listOf(ReservationResponse.NoTimes(true, now, employee2.evakaUser)),
            result.reservations,
        )
    }

    @Test
    fun `reservations for children in backup care are shown`() {
        val reservationStart = LocalTime.of(8, 0)
        val reservationEnd = LocalTime.of(16, 0)
        db.transaction {
            it.insert(
                DevBackupCare(
                    childId = child.id,
                    unitId = daycare2.id,
                    groupId = groupId2.id,
                    period = FiniteDateRange(now.minusDays(1).toLocalDate(), now.toLocalDate()),
                )
            )
            it.insert(
                DevReservation(
                    childId = child.id,
                    date = now.toLocalDate(),
                    startTime = reservationStart,
                    endTime = reservationEnd,
                    createdAt = now,
                    createdBy = employee2.evakaUserId,
                )
            )
        }
        val childInBackup = expectOneChild(daycare2.id, mobileDevice2.user)
        assertEquals(
            listOf(
                ReservationResponse.Times(
                    TimeRange(reservationStart, reservationEnd),
                    true,
                    now,
                    employee2.evakaUser,
                )
            ),
            childInBackup.reservations,
        )
    }

    private fun getChildren(
        unitId: DaycareId = daycare.id,
        user: AuthenticatedUser.MobileDevice = mobileUser,
    ): List<AttendanceChild> {
        return childAttendanceController.getChildren(
            dbInstance(),
            user,
            MockEvakaClock(now),
            unitId,
        )
    }

    private fun expectOneChild(
        unitId: DaycareId = daycare.id,
        user: AuthenticatedUser.MobileDevice = mobileUser,
    ): AttendanceChild {
        val children = getChildren(unitId, user)
        assertEquals(1, children.size)
        return children[0]
    }

    private fun expectNoChildren(
        unitId: DaycareId = daycare.id,
        user: AuthenticatedUser.MobileDevice = mobileUser,
    ) {
        val children = getChildren(unitId, user)
        assertEquals(0, children.size)
    }

    private fun getAttendanceStatuses(
        unitId: DaycareId = daycare.id,
        user: AuthenticatedUser.MobileDevice = mobileUser,
    ): Map<ChildId, ChildAttendanceController.ChildAttendanceStatusResponse> {
        return childAttendanceController.getAttendanceStatuses(
            dbInstance(),
            user,
            MockEvakaClock(now),
            unitId,
        )
    }

    private fun expectOneChildAttendance(
        unitId: DaycareId = daycare.id,
        user: AuthenticatedUser.MobileDevice = mobileUser,
    ): ChildAttendanceController.ChildAttendanceStatusResponse {
        val response = getAttendanceStatuses(unitId, user)
        assertEquals(1, response.size)
        return response.values.first()
    }

    private fun expectNoChildAttendances(
        unitId: DaycareId = daycare.id,
        user: AuthenticatedUser.MobileDevice = mobileUser,
    ) {
        val response = getAttendanceStatuses(unitId, user)
        assertEquals(0, response.size)
    }
}
