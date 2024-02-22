// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dailyservicetimes

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.getAbsencesOfChildByRange
import fi.espoo.evaka.espoo.EspooActionRuleMapping
import fi.espoo.evaka.holidayperiod.insertHolidayPeriod
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.DailyReservationRequest
import fi.espoo.evaka.reservations.ReservationControllerCitizen
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DailyServiceTimeNotificationId
import fi.espoo.evaka.shared.DailyServiceTimesId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevDailyServiceTimes
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import io.opentracing.noop.NoopTracerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DailyServiceTimesIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val dailyServiceTimesController =
        DailyServiceTimesController(
            AccessControl(EspooActionRuleMapping(), NoopTracerFactory.create())
        )
    @Autowired
    private lateinit var dailyServiceTimesCitizenController: DailyServiceTimesCitizenController
    @Autowired private lateinit var reservationControllerCitizen: ReservationControllerCitizen

    private val admin = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))
    private val guardian1 = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.WEAK)
    private val guardian2 = AuthenticatedUser.Citizen(testAdult_2.id, CitizenAuthLevel.WEAK)

    private val groupId = GroupId(UUID.randomUUID())
    private val daycarePlacementId = PlacementId(UUID.randomUUID())
    private val now = HelsinkiDateTime.of(LocalDate.of(2022, 2, 3), LocalTime.of(12, 5, 1))
    private val placementStart = now.toLocalDate().minusDays(30)
    private val placementEnd = now.toLocalDate().plusDays(120)

    private val dailyServiceTimesValidity =
        DateRange(
            // Tuesday
            now.toLocalDate().plusDays(97),
            null
        )
    private val tenToNoonRange = TimeRange(LocalTime.of(10, 0), LocalTime.of(12, 0))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
            tx.insertGuardian(testAdult_2.id, testChild_1.id)
            tx.insert(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = ""))
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
        }
    }

    @Test
    fun `cannot create, update or delete if validity has started`() {
        assertThrows<BadRequest> {
            createDailyServiceTimes(
                testChild_1.id,
                DailyServiceTimesValue.RegularTimes(
                    DateRange(now.toLocalDate(), null),
                    tenToNoonRange
                ),
            )
        }

        val id = DailyServiceTimesId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insert(
                DevDailyServiceTimes(
                    id = id,
                    childId = testChild_1.id,
                    validityPeriod = DateRange(now.toLocalDate(), null)
                )
            )
        }

        assertThrows<BadRequest> {
            updateDailyServiceTimes(
                id,
                DailyServiceTimesValue.RegularTimes(
                    validityPeriod = DateRange(now.toLocalDate().minusDays(1), null),
                    regularTimes = TimeRange(LocalTime.of(19, 0), LocalTime.of(22, 0))
                ),
            )
        }

        assertThrows<BadRequest> { deleteDailyServiceTimes(id) }
    }

    @Test
    fun `can set end time only if it is in the future`() {
        val idFuture = DailyServiceTimesId(UUID.randomUUID())
        val past = DailyServiceTimesId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insert(
                DevDailyServiceTimes(
                    id = idFuture,
                    childId = testChild_1.id,
                    validityPeriod = DateRange(now.toLocalDate(), null)
                )
            )
            tx.insert(
                DevDailyServiceTimes(
                    id = past,
                    childId = testChild_2.id,
                    validityPeriod =
                        DateRange(now.toLocalDate().minusDays(2), now.toLocalDate().minusDays(1))
                )
            )
        }

        // Set to future
        setDailyServiceTimesEndDate(idFuture, now.toLocalDate().plusDays(1))

        // Set to past -> not allowed
        assertThrows<BadRequest> { setDailyServiceTimesEndDate(idFuture, now.toLocalDate()) }

        // Already in the past -> not allowed
        assertThrows<BadRequest> {
            setDailyServiceTimesEndDate(past, now.toLocalDate().plusDays(1))
        }
    }

    @Test
    fun `creating daily service times adjusts overlapping entries`() {
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod =
                    DateRange(now.toLocalDate().plusDays(1), now.toLocalDate().plusDays(100)),
                regularTimes = tenToNoonRange
            )
        )

        // Add overlapping entry to the start
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod =
                    DateRange(now.toLocalDate().plusDays(1), now.toLocalDate().plusDays(10)),
                regularTimes = tenToNoonRange
            )
        )

        // Add overlapping entry in the middle -> not allowed
        assertThrows<Conflict> {
            createDailyServiceTimes(
                testChild_1.id,
                DailyServiceTimesValue.RegularTimes(
                    validityPeriod =
                        DateRange(now.toLocalDate().plusDays(30), now.toLocalDate().plusDays(50)),
                    regularTimes = tenToNoonRange
                ),
            )
        }

        // Add overlapping entry to the end
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(now.toLocalDate().plusDays(90), null),
                regularTimes = tenToNoonRange
            )
        )

        // Add a finite entry to the end when the current is infinite
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod =
                    DateRange(now.toLocalDate().plusDays(100), now.toLocalDate().plusDays(120)),
                regularTimes = tenToNoonRange
            )
        )

        run {
            val expectedRanges = listOf(100L to 120L, 90L to 99L, 11L to 89L, 1L to 10L)
            val dailyServiceTimes = getDailyServiceTimes(testChild_1.id)
            assertEquals(expectedRanges.size, dailyServiceTimes.size)
            expectedRanges.zip(dailyServiceTimes).forEachIndexed { i, (expected, actual) ->
                val expectedValidity =
                    DateRange(
                        now.toLocalDate().plusDays(expected.first),
                        now.toLocalDate().plusDays(expected.second)
                    )
                assertEquals(
                    expectedValidity,
                    actual.dailyServiceTimes.times.validityPeriod,
                    "Index $i"
                )
            }
        }

        // Add overlapping that covers the whole range -> all others are deleted
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(now.toLocalDate().plusDays(1), null),
                regularTimes = tenToNoonRange
            )
        )

        run {
            val dailyServiceTimes = getDailyServiceTimes(testChild_1.id)
            assertEquals(1, dailyServiceTimes.size)
            assertEquals(
                dailyServiceTimes[0].dailyServiceTimes.times.validityPeriod,
                DateRange(now.toLocalDate().plusDays(1), null)
            )
        }
    }

    @Test
    fun `disallow updating so that the new validity overlaps with another entry`() {
        val id = DailyServiceTimesId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insert(
                DevDailyServiceTimes(
                    id = id,
                    childId = testChild_1.id,
                    validityPeriod =
                        DateRange(now.toLocalDate().plusDays(1), now.toLocalDate().plusDays(10))
                )
            )
            tx.insert(
                DevDailyServiceTimes(
                    childId = testChild_1.id,
                    validityPeriod = DateRange(now.toLocalDate().plusDays(11), null)
                )
            )
        }

        // Attempt updating the first entry so that it overlaps with the second -> not allowed

        assertThrows<BadRequest> {
            updateDailyServiceTimes(
                id,
                DailyServiceTimesValue.RegularTimes(
                    validityPeriod =
                        DateRange(now.toLocalDate().plusDays(1), now.toLocalDate().plusDays(11)),
                    regularTimes = tenToNoonRange
                ),
            )
        }

        assertThrows<BadRequest> {
            setDailyServiceTimesEndDate(
                id,
                now.toLocalDate().plusDays(11),
            )
        }
    }

    @Test
    fun `adding a new daily service time creates a notification for both guardians`() {
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = dailyServiceTimesValidity,
                regularTimes = tenToNoonRange
            )
        )

        val guardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        assertEquals(1, guardian1Notifications.size)
        assertEquals(false, guardian1Notifications[0].hasDeletedReservations)

        val guardian2Notifications = this.getDailyServiceTimeNotifications(guardian2)
        assertEquals(1, guardian2Notifications.size)
        assertEquals(false, guardian2Notifications[0].hasDeletedReservations)
    }

    @Test
    fun `one guardian dismissing their daily service time update notification does not affect other's`() {
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = dailyServiceTimesValidity,
                regularTimes = tenToNoonRange
            )
        )

        val guardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        this.dismissDailyServiceTimeNotification(guardian1, guardian1Notifications[0].id)

        val guardian2Notifications = this.getDailyServiceTimeNotifications(guardian2)
        assertEquals(1, guardian2Notifications.size)
        assertEquals(false, guardian2Notifications[0].hasDeletedReservations)
    }

    @Test
    fun `adding a new daily service time creates a modal notification when reservations exist during the new period`() {
        db.transaction { tx ->
            tx.insertHolidayPeriod(
                FiniteDateRange(
                    dailyServiceTimesValidity.start.plusDays(7),
                    dailyServiceTimesValidity.start.plusDays(13),
                ),
                reservationDeadline = now.toLocalDate(),
            )
        }
        this.postReservations(
            listOf(
                // Outside the validity period
                DailyReservationRequest.Reservations(
                    testChild_1.id,
                    dailyServiceTimesValidity.start.minusDays(1),
                    TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                ),
                // Inside the validity period
                DailyReservationRequest.Reservations(
                    testChild_1.id,
                    dailyServiceTimesValidity.start,
                    TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                ),
                // Inside the validity period AND inside a holiday period
                DailyReservationRequest.Present(
                    testChild_1.id,
                    dailyServiceTimesValidity.start.plusDays(7),
                )
            )
        )
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = dailyServiceTimesValidity,
                regularTimes = tenToNoonRange
            )
        )

        val guardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        assertEquals(1, guardian1Notifications.size)
        assertEquals(true, guardian1Notifications[0].hasDeletedReservations)

        // Reservations are cleared from the validity period, except if they are inside a holiday
        // period
        assertEquals(
            listOf(
                dailyServiceTimesValidity.start.minusDays(1),
                dailyServiceTimesValidity.start.plusDays(7)
            ),
            getReservationDates(),
        )
    }

    @Test
    fun `updating a daily service time creates a new notification`() {
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = dailyServiceTimesValidity,
                regularTimes = tenToNoonRange
            )
        )

        val guardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        this.dismissDailyServiceTimeNotification(guardian1, guardian1Notifications[0].id)

        val times = this.getDailyServiceTimes(testChild_1.id)
        assertEquals(1, times.size)
        updateDailyServiceTimes(
            times[0].dailyServiceTimes.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = dailyServiceTimesValidity,
                regularTimes = TimeRange(LocalTime.of(19, 0), LocalTime.of(22, 0))
            )
        )

        val newGuardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        assertEquals(1, newGuardian1Notifications.size)
        assertEquals(false, newGuardian1Notifications[0].hasDeletedReservations)
    }

    @Test
    fun `updating a daily service times validity end creates a new notification`() {
        val originalEnd = now.toLocalDate().plusDays(10)
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(now.toLocalDate().plusDays(5), originalEnd),
                regularTimes = tenToNoonRange
            )
        )

        val guardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        this.dismissDailyServiceTimeNotification(guardian1, guardian1Notifications[0].id)

        val times = this.getDailyServiceTimes(testChild_1.id)
        assertEquals(1, times.size)
        setDailyServiceTimesEndDate(times[0].dailyServiceTimes.id, originalEnd.plusDays(5))

        val newGuardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        assertEquals(1, newGuardian1Notifications.size)
        assertEquals(originalEnd.plusDays(1), newGuardian1Notifications[0].dateFrom)
        assertEquals(false, newGuardian1Notifications[0].hasDeletedReservations)
    }

    @Test
    fun `creating irregular daily service times automatically adds absences`() {
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.IrregularTimes(
                validityPeriod = dailyServiceTimesValidity,
                monday = tenToNoonRange,
                tuesday = tenToNoonRange,
                wednesday = null,
                thursday = tenToNoonRange,
                friday = tenToNoonRange,
                saturday = null,
                sunday = null
            )
        )

        val absences =
            db.transaction { tx ->
                tx.getAbsencesOfChildByRange(testChild_1.id, DateRange(now.toLocalDate(), null))
            }
        assert(absences.isNotEmpty())
    }

    private fun createDailyServiceTimes(
        childId: ChildId,
        dailyServiceTime: DailyServiceTimesValue,
    ) {
        dailyServiceTimesController.postDailyServiceTimes(
            dbInstance(),
            admin,
            MockEvakaClock(now),
            childId,
            dailyServiceTime
        )
    }

    private fun updateDailyServiceTimes(
        id: DailyServiceTimesId,
        dailyServiceTime: DailyServiceTimesValue,
    ) {
        dailyServiceTimesController.putDailyServiceTimes(
            dbInstance(),
            admin,
            MockEvakaClock(now),
            id,
            dailyServiceTime
        )
    }

    private fun setDailyServiceTimesEndDate(
        id: DailyServiceTimesId,
        endDate: LocalDate,
    ) {
        dailyServiceTimesController.putDailyServiceTimesEnd(
            dbInstance(),
            admin,
            MockEvakaClock(now),
            id,
            DailyServiceTimesController.DailyServiceTimesEndDate(endDate)
        )
    }

    private fun deleteDailyServiceTimes(id: DailyServiceTimesId) {
        dailyServiceTimesController.deleteDailyServiceTimes(
            dbInstance(),
            admin,
            MockEvakaClock(now),
            id
        )
    }

    private fun getDailyServiceTimes(
        childId: ChildId
    ): List<DailyServiceTimesController.DailyServiceTimesResponse> {
        return dailyServiceTimesController.getDailyServiceTimes(
            dbInstance(),
            admin,
            MockEvakaClock(now),
            childId
        )
    }

    private fun getDailyServiceTimeNotifications(
        user: AuthenticatedUser.Citizen
    ): List<DailyServiceTimeNotification> {
        return dailyServiceTimesCitizenController.getDailyServiceTimeNotifications(
            dbInstance(),
            user,
            MockEvakaClock(now)
        )
    }

    private fun dismissDailyServiceTimeNotification(
        user: AuthenticatedUser.Citizen,
        notificationId: DailyServiceTimeNotificationId
    ) {
        dailyServiceTimesCitizenController.dismissDailyServiceTimeNotification(
            dbInstance(),
            user,
            MockEvakaClock(now),
            listOf(notificationId)
        )
    }

    private fun postReservations(request: List<DailyReservationRequest>) {
        reservationControllerCitizen.postReservations(
            dbInstance(),
            AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
            MockEvakaClock(now),
            request
        )
    }

    private fun getReservationDates(): List<LocalDate> =
        db.read {
            @Suppress("DEPRECATION")
            it.createQuery("SELECT date FROM attendance_reservation ORDER BY date")
                .toList<LocalDate>()
        }
}
