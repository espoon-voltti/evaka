// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.preschoolTerm2023
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.createMobileDeviceToUnit
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class AttendanceTransitionsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var childAttendanceController: ChildAttendanceController

    private val mobileUser = AuthenticatedUser.MobileDevice(MobileDeviceId(UUID.randomUUID()))
    private val groupId = GroupId(UUID.randomUUID())
    private val groupName = "Testaajat"
    private val daycarePlacementId = PlacementId(UUID.randomUUID())
    private val today = LocalDate.of(2024, 1, 17)
    private val placementStart = today.minusDays(30)
    private val placementEnd = today.plusDays(30)

    private val mockClock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(17, 19, 30)))
    private val roundedNow = mockClock.now().toLocalTime().withSecond(0).withNano(0)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(preschoolTerm2023)
            tx.insert(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            tx.createMobileDeviceToUnit(mobileUser.id, testDaycare.id)
        }
    }

    @Test
    fun `post child arrives - happy case`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()

        val arrived = LocalTime.of(9, 15)
        markArrived(arrived)
        val child = expectOneAttendanceStatus()

        assertEquals(AttendanceStatus.PRESENT, child.status)
        assertNotNull(child.attendances)
        assertEquals(arrived, child.attendances[0].arrived.toLocalTime().withSecond(0).withNano(0))
        assertNull(child.attendances[0].departed)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post child arrives - arriving twice is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(arrived = LocalTime.of(9, 0))

        val arrived = LocalTime.of(9, 15)
        assertThrows<Conflict> { markArrived(arrived) }
    }

    @Test
    fun `post return to coming - happy case when present`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent()

        returnToComing()
        expectNoAttendanceStatuses()
    }

    @Test
    fun `post return to coming - happy case when absent`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildAbsent(
            AbsenceType.UNKNOWN_ABSENCE,
            AbsenceCategory.BILLABLE,
            AbsenceCategory.NONBILLABLE,
        )

        returnToComing()
        expectNoAttendanceStatuses()
    }

    @Test
    fun `get expected absences - preschool daycare placement and present from preschool start`() {
        val arrived = LocalTime.of(9, 0)
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(arrived)

        assertEquals(
            setOf(AbsenceCategory.NONBILLABLE, AbsenceCategory.BILLABLE),
            getExpectedAbsencesOnDeparture(LocalTime.of(9, 30), testChild_1.id),
        )
        assertEquals(
            setOf(AbsenceCategory.BILLABLE),
            getExpectedAbsencesOnDeparture(LocalTime.of(12, 45), testChild_1.id),
        )
        assertEquals(setOf(), getExpectedAbsencesOnDeparture(LocalTime.of(13, 20), testChild_1.id))
    }

    @Test
    fun `get expected absences - preschool daycare placement and present from preschool end`() {
        val arrived = LocalTime.of(13, 0)
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(arrived)

        assertEquals(
            setOf(AbsenceCategory.NONBILLABLE),
            getExpectedAbsencesOnDeparture(LocalTime.of(17, 20), testChild_1.id),
        )
    }

    @Test
    fun `get expected absences - preschool daycare placement and present hour before preschool start`() {
        val arrived = LocalTime.of(8, 0)
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(arrived)

        assertEquals(
            setOf(AbsenceCategory.NONBILLABLE),
            getExpectedAbsencesOnDeparture(LocalTime.of(9, 45), testChild_1.id),
        )
        assertEquals(setOf(), getExpectedAbsencesOnDeparture(LocalTime.of(13, 0), testChild_1.id))
    }

    @Test
    fun `get expected absences - not yet present`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()
        assertThrows<BadRequest> {
            getExpectedAbsencesOnDeparture(LocalTime.of(13, 30), testChild_1.id)
        }
    }

    @Test
    fun `get expected absences - already departed`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildDeparted(arrived = LocalTime.of(9, 0), departed = LocalTime.of(13, 0))
        assertThrows<BadRequest> {
            getExpectedAbsencesOnDeparture(LocalTime.of(13, 30), testChild_1.id)
        }
    }

    @Test
    fun `post child departs - happy case`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(LocalTime.of(8, 0))

        val departed = LocalTime.of(16, 0)
        markDeparted(
            departed,
            testChild_1.id,
            absenceTypeNonbillable = null,
            absenceTypeBillable = null,
        )
        val child = expectOneAttendanceStatus()

        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendances)
        assertEquals(
            departed,
            child.attendances[0].departed?.toLocalTime()?.withSecond(0)?.withNano(0),
        )
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post child departs - absent from preschool_daycare`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(LocalTime.of(9, 0))

        val departed = LocalTime.of(13, 0)
        val absenceType = AbsenceType.OTHER_ABSENCE
        markDeparted(
            departed,
            testChild_1.id,
            absenceTypeNonbillable = null,
            absenceTypeBillable = absenceType,
        )
        val child = expectOneAttendanceStatus()

        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendances)
        assertEquals(
            departed,
            child.attendances[0].departed?.toLocalTime()?.withSecond(0)?.withNano(0),
        )
        assertContentEquals(listOf(AbsenceCategory.BILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post child departs - absent from preschool`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(LocalTime.of(12, 45))

        val departed = LocalTime.of(18, 0)
        val absenceType = AbsenceType.UNKNOWN_ABSENCE
        markDeparted(
            departed,
            testChild_1.id,
            absenceTypeNonbillable = absenceType,
            absenceTypeBillable = null,
        )
        val child = expectOneAttendanceStatus()

        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendances)
        assertEquals(
            departed,
            child.attendances[0].departed?.toLocalTime()?.withSecond(0)?.withNano(0),
        )
        assertContentEquals(listOf(AbsenceCategory.NONBILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post child departs - absent from preschool and preschool_daycare`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildPresent(LocalTime.of(8, 50))

        val departed = LocalTime.of(9, 30)
        val absenceType = AbsenceType.SICKLEAVE
        markDeparted(departed, testChild_1.id, absenceType, absenceType)

        val child = expectOneAttendanceStatus()
        assertEquals(AttendanceStatus.DEPARTED, child.status)
        assertNotNull(child.attendances)
        assertEquals(
            departed,
            child.attendances[0].departed?.toLocalTime()?.withSecond(0)?.withNano(0),
        )
        assertEquals(
            setOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE),
            child.absences.map { it.category }.toSet(),
        )
    }

    @Test
    fun `post child departs - multi day attendance - departed less than 30 min ago`() {
        givenChildPlacement(PlacementType.DAYCARE)
        givenChildPresent(LocalTime.of(20, 50), mockClock.today().minusDays(1))

        val departed = mockClock.now().toLocalTime().minusMinutes(20).withSecond(0).withNano(0)
        markDeparted(departed, testChild_1.id, null, null)

        val child = expectOneAttendanceStatus()
        assertEquals(AttendanceStatus.DEPARTED, child.status)
    }

    @Test
    fun `post child departs - multi day attendance - departed more than 30 min ago`() {
        givenChildPlacement(PlacementType.DAYCARE)
        givenChildPresent(LocalTime.of(20, 50), mockClock.today().minusDays(1))

        val departed = mockClock.now().toLocalTime().withSecond(0).withNano(0).minusMinutes(40)
        markDeparted(departed, testChild_1.id, null, null)

        val child = expectOneAttendanceStatus()
        assertEquals(AttendanceStatus.COMING, child.status)
    }

    @Test
    fun `post child departs - departing twice is error`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildDeparted(arrived = LocalTime.of(9, 0), departed = LocalTime.of(14, 0))

        assertThrows<BadRequest> { markDeparted(LocalTime.of(15, 0), testChild_1.id, null, null) }
    }

    @Test
    fun `post return to present - happy case when departed`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildDeparted()

        returnToPresent()
        val child = expectOneAttendanceStatus()

        assertEquals(AttendanceStatus.PRESENT, child.status)
        assertNotNull(child.attendances)
        assertNotNull(child.attendances[0].arrived)
        assertNull(child.attendances[0].departed)
        assertTrue(child.absences.isEmpty())
    }

    @Test
    fun `post full day absence - happy case when coming to preschool`() {
        // previous day attendance should have no effect
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = mockClock.now().minusDays(1).minusHours(1),
                departed = mockClock.now().minusDays(1).minusMinutes(1),
            )
        }
        givenChildPlacement(PlacementType.PRESCHOOL)
        givenChildComing()

        markFullDayAbsence(AbsenceType.SICKLEAVE)
        val child = expectOneAttendanceStatus()

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertContentEquals(listOf(AbsenceCategory.NONBILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post full day absence - happy case when coming to preschool_daycare`() {
        givenChildPlacement(PlacementType.PRESCHOOL_DAYCARE)
        givenChildComing()

        markFullDayAbsence(AbsenceType.SICKLEAVE)
        val child = expectOneAttendanceStatus()

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(
            setOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE),
            child.absences.map { it.category }.toSet(),
        )
    }

    @Test
    fun `post full day absence - happy case when coming to preparatory`() {
        givenChildPlacement(PlacementType.PREPARATORY)
        givenChildComing()

        markFullDayAbsence(AbsenceType.SICKLEAVE)
        val child = expectOneAttendanceStatus()

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertContentEquals(listOf(AbsenceCategory.NONBILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post full day absence - happy case when coming to preparatory_daycare`() {
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildComing()

        markFullDayAbsence(AbsenceType.SICKLEAVE)
        val child = expectOneAttendanceStatus()

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertEquals(
            setOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE),
            child.absences.map { it.category }.toSet(),
        )
    }

    @Test
    fun `post full day absence - happy case when coming to daycare`() {
        givenChildPlacement(PlacementType.DAYCARE)
        givenChildComing()

        markFullDayAbsence(AbsenceType.SICKLEAVE)
        val child = expectOneAttendanceStatus()

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertContentEquals(listOf(AbsenceCategory.BILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post full day absence - happy case when coming to daycare_part_time`() {
        givenChildPlacement(PlacementType.DAYCARE_PART_TIME)
        givenChildComing()

        markFullDayAbsence(AbsenceType.SICKLEAVE)
        val child = expectOneAttendanceStatus()

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertContentEquals(listOf(AbsenceCategory.BILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post full day absence - happy case when coming to club`() {
        givenChildPlacement(PlacementType.CLUB)
        givenChildComing()

        markFullDayAbsence(AbsenceType.SICKLEAVE)
        val child = expectOneAttendanceStatus()

        assertEquals(AttendanceStatus.ABSENT, child.status)
        assertContentEquals(listOf(AbsenceCategory.NONBILLABLE), child.absences.map { it.category })
    }

    @Test
    fun `post full day absence - error when no placement`() {
        assertThrows<BadRequest> { markFullDayAbsence(AbsenceType.SICKLEAVE) }
    }

    @Test
    fun `post full day absence - error when present`() {
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildPresent()

        assertThrows<Conflict> { markFullDayAbsence(AbsenceType.SICKLEAVE) }
    }

    @Test
    fun `post full day absence - error when departed`() {
        givenChildPlacement(PlacementType.PREPARATORY_DAYCARE)
        givenChildDeparted()

        assertThrows<Conflict> { markFullDayAbsence(AbsenceType.SICKLEAVE) }
    }

    private fun givenChildPlacement(placementType: PlacementType) {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    id = daycarePlacementId,
                    type = placementType,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = daycarePlacementId,
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }
    }

    private fun givenChildComing() {
        val attendance = getAttendanceStatuses()
        if (attendance.isNotEmpty()) {
            assertEquals(1, attendance.size)
            assertEquals(AttendanceStatus.COMING, attendance.values.first().status)
        }
    }

    private fun givenChildPresent(
        arrived: LocalTime = roundedNow.minusHours(1),
        date: LocalDate = mockClock.today(),
    ) {
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = HelsinkiDateTime.of(date, arrived),
                departed = null,
            )
        }
        val child = expectOneAttendanceStatus()
        assertEquals(AttendanceStatus.PRESENT, child.status)
    }

    private fun givenChildDeparted(
        arrived: LocalTime = roundedNow.minusHours(1),
        departed: LocalTime = roundedNow.minusMinutes(10),
    ) {
        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = mockClock.now().withTime(arrived),
                departed = mockClock.now().withTime(departed),
            )
        }
        val child = expectOneAttendanceStatus()
        assertEquals(AttendanceStatus.DEPARTED, child.status)
    }

    private fun givenChildAbsent(absenceType: AbsenceType, vararg categories: AbsenceCategory) {
        categories.forEach { category ->
            db.transaction {
                it.insert(
                    DevAbsence(
                        childId = testChild_1.id,
                        date = LocalDate.now(),
                        absenceType = absenceType,
                        absenceCategory = category,
                    )
                )
            }
        }
    }

    private fun getAttendanceStatuses() =
        childAttendanceController.getAttendanceStatuses(
            dbInstance(),
            mobileUser,
            mockClock,
            testDaycare.id,
        )

    private fun expectOneAttendanceStatus():
        ChildAttendanceController.ChildAttendanceStatusResponse {
        val attendances = getAttendanceStatuses()
        assertEquals(1, attendances.size)
        return attendances.values.first()
    }

    private fun expectNoAttendanceStatuses() {
        val attendances = getAttendanceStatuses()
        assertEquals(0, attendances.size)
    }

    private fun markArrived(arrived: LocalTime) {
        childAttendanceController.postArrivals(
            dbInstance(),
            mobileUser,
            mockClock,
            testDaycare.id,
            ChildAttendanceController.ArrivalsRequest(
                children = setOf(testChild_1.id),
                arrived = arrived,
            ),
        )
    }

    private fun returnToComing() {
        childAttendanceController.returnToComing(
            dbInstance(),
            mobileUser,
            mockClock,
            testDaycare.id,
            testChild_1.id,
        )
    }

    private fun getExpectedAbsencesOnDeparture(
        departed: LocalTime,
        childId: ChildId,
    ): Set<AbsenceCategory>? {
        return childAttendanceController
            .getExpectedAbsencesOnDepartures(
                dbInstance(),
                mobileUser,
                mockClock,
                testDaycare.id,
                ChildAttendanceController.ExpectedAbsencesOnDeparturesRequest(
                    setOf(childId),
                    departed,
                ),
            )
            .categoriesByChild[childId]
    }

    private fun markDeparted(
        departed: LocalTime,
        childId: ChildId,
        absenceTypeNonbillable: AbsenceType?,
        absenceTypeBillable: AbsenceType?,
    ) {
        childAttendanceController.postDepartures(
            dbInstance(),
            mobileUser,
            mockClock,
            testDaycare.id,
            ChildAttendanceController.DeparturesRequest(
                departed = departed,
                departures =
                    listOf(
                        ChildAttendanceController.ChildDeparture(
                            childId = childId,
                            absenceTypeNonbillable = absenceTypeNonbillable,
                            absenceTypeBillable = absenceTypeBillable,
                        )
                    ),
            ),
        )
    }

    private fun returnToPresent() {
        childAttendanceController.returnToPresent(
            dbInstance(),
            mobileUser,
            mockClock,
            testDaycare.id,
            testChild_1.id,
        )
    }

    private fun markFullDayAbsence(absenceType: AbsenceType) {
        childAttendanceController.postFullDayAbsence(
            dbInstance(),
            mobileUser,
            mockClock,
            testDaycare.id,
            testChild_1.id,
            ChildAttendanceController.FullDayAbsenceRequest(absenceType),
        )
    }
}
