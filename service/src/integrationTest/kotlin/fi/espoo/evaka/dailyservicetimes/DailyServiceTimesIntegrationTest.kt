// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dailyservicetimes

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.getAbsencesOfChildByRange
import fi.espoo.evaka.espoo.EspooActionRuleMapping
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.DailyReservationRequest
import fi.espoo.evaka.reservations.ReservationControllerCitizen
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DailyServiceTimeNotificationId
import fi.espoo.evaka.shared.DailyServiceTimesId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDailyServiceTimes
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDaycareFullDay35
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

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val guardian1 = DevPerson()
    private val guardian2 = DevPerson()
    private val child = DevPerson()

    private val today = LocalDate.of(2022, 2, 7) // Monday
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 5, 1))

    private val dailyServiceTimesValidity =
        DateRange(
            // Tuesday
            today.plusDays(92),
            null
        )
    private val tenToNoonRange = TimeRange(LocalTime.of(10, 0), LocalTime.of(12, 0))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(admin)
            listOf(guardian1, guardian2).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(child, DevPersonType.CHILD)

            tx.insertGuardian(guardian1.id, child.id)
            tx.insertGuardian(guardian2.id, child.id)
        }
    }

    @Test
    fun `cannot create, update or delete if validity has started`() {
        assertThrows<BadRequest> {
            createDailyServiceTimes(
                child.id,
                DailyServiceTimesValue.RegularTimes(DateRange(today, null), tenToNoonRange),
            )
        }

        val id = DailyServiceTimesId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insert(
                DevDailyServiceTimes(
                    id = id,
                    childId = child.id,
                    validityPeriod = DateRange(today, null)
                )
            )
        }

        assertThrows<BadRequest> {
            updateDailyServiceTimes(
                id,
                DailyServiceTimesValue.RegularTimes(
                    validityPeriod = DateRange(today.minusDays(1), null),
                    regularTimes = TimeRange(LocalTime.of(19, 0), LocalTime.of(22, 0))
                ),
            )
        }

        assertThrows<BadRequest> { deleteDailyServiceTimes(id) }
    }

    @Test
    fun `can set end time only if it is in the future`() {
        val child2 = DevPerson()

        db.transaction { tx -> tx.insert(child2, DevPersonType.CHILD) }

        val idFuture = DailyServiceTimesId(UUID.randomUUID())
        val past = DailyServiceTimesId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insert(
                DevDailyServiceTimes(
                    id = idFuture,
                    childId = child.id,
                    validityPeriod = DateRange(today, null)
                )
            )
            tx.insert(
                DevDailyServiceTimes(
                    id = past,
                    childId = child2.id,
                    validityPeriod = DateRange(today.minusDays(2), today.minusDays(1))
                )
            )
        }

        // Set to future
        setDailyServiceTimesEndDate(idFuture, today.plusDays(1))

        // Set to past -> not allowed
        assertThrows<BadRequest> { setDailyServiceTimesEndDate(idFuture, today) }

        // Already in the past -> not allowed
        assertThrows<BadRequest> { setDailyServiceTimesEndDate(past, today.plusDays(1)) }
    }

    @Test
    fun `creating daily service times adjusts overlapping entries`() {
        createDailyServiceTimes(
            child.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(today.plusDays(1), today.plusDays(100)),
                regularTimes = tenToNoonRange
            )
        )

        // Add overlapping entry to the start
        createDailyServiceTimes(
            child.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(today.plusDays(1), today.plusDays(10)),
                regularTimes = tenToNoonRange
            )
        )

        // Add overlapping entry in the middle -> not allowed
        assertThrows<Conflict> {
            createDailyServiceTimes(
                child.id,
                DailyServiceTimesValue.RegularTimes(
                    validityPeriod = DateRange(today.plusDays(30), today.plusDays(50)),
                    regularTimes = tenToNoonRange
                ),
            )
        }

        // Add overlapping entry to the end
        createDailyServiceTimes(
            child.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(today.plusDays(90), null),
                regularTimes = tenToNoonRange
            )
        )

        // Add a finite entry to the end when the current is infinite
        createDailyServiceTimes(
            child.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(today.plusDays(100), today.plusDays(120)),
                regularTimes = tenToNoonRange
            )
        )

        run {
            val expectedRanges = listOf(100L to 120L, 90L to 99L, 11L to 89L, 1L to 10L)
            val dailyServiceTimes = getDailyServiceTimes(child.id)
            assertEquals(expectedRanges.size, dailyServiceTimes.size)
            expectedRanges.zip(dailyServiceTimes).forEachIndexed { i, (expected, actual) ->
                val expectedValidity =
                    DateRange(today.plusDays(expected.first), today.plusDays(expected.second))
                assertEquals(
                    expectedValidity,
                    actual.dailyServiceTimes.times.validityPeriod,
                    "Index $i"
                )
            }
        }

        // Add overlapping that covers the whole range -> all others are deleted
        createDailyServiceTimes(
            child.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(today.plusDays(1), null),
                regularTimes = tenToNoonRange
            )
        )

        run {
            val dailyServiceTimes = getDailyServiceTimes(child.id)
            assertEquals(1, dailyServiceTimes.size)
            assertEquals(
                dailyServiceTimes[0].dailyServiceTimes.times.validityPeriod,
                DateRange(today.plusDays(1), null)
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
                    childId = child.id,
                    validityPeriod = DateRange(today.plusDays(1), today.plusDays(10))
                )
            )
            tx.insert(
                DevDailyServiceTimes(
                    childId = child.id,
                    validityPeriod = DateRange(today.plusDays(11), null)
                )
            )
        }

        // Attempt updating the first entry so that it overlaps with the second -> not allowed

        assertThrows<BadRequest> {
            updateDailyServiceTimes(
                id,
                DailyServiceTimesValue.RegularTimes(
                    validityPeriod = DateRange(today.plusDays(1), today.plusDays(11)),
                    regularTimes = tenToNoonRange
                ),
            )
        }

        assertThrows<BadRequest> {
            setDailyServiceTimesEndDate(
                id,
                today.plusDays(11),
            )
        }
    }

    @Test
    fun `adding a new daily service time creates a notification for both guardians`() {
        createDailyServiceTimes(
            child.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = dailyServiceTimesValidity,
                regularTimes = tenToNoonRange
            )
        )

        val guardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        assertEquals(1, guardian1Notifications.size)

        val guardian2Notifications = this.getDailyServiceTimeNotifications(guardian2)
        assertEquals(1, guardian2Notifications.size)
    }

    @Test
    fun `one guardian dismissing their daily service time update notification does not affect other's`() {
        createDailyServiceTimes(
            child.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = dailyServiceTimesValidity,
                regularTimes = tenToNoonRange
            )
        )

        val guardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        this.dismissDailyServiceTimeNotification(guardian1, guardian1Notifications[0])

        val guardian2Notifications = this.getDailyServiceTimeNotifications(guardian2)
        assertEquals(1, guardian2Notifications.size)
    }

    @Test
    fun `updating a daily service time creates a new notification`() {
        createDailyServiceTimes(
            child.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = dailyServiceTimesValidity,
                regularTimes = tenToNoonRange
            )
        )

        val guardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        this.dismissDailyServiceTimeNotification(guardian1, guardian1Notifications[0])

        val times = this.getDailyServiceTimes(child.id)
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
    }

    @Test
    fun `updating a daily service times validity end creates a new notification`() {
        val originalEnd = today.plusDays(10)
        createDailyServiceTimes(
            child.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(today.plusDays(5), originalEnd),
                regularTimes = tenToNoonRange
            )
        )

        val guardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        this.dismissDailyServiceTimeNotification(guardian1, guardian1Notifications[0])

        val times = this.getDailyServiceTimes(child.id)
        assertEquals(1, times.size)
        setDailyServiceTimesEndDate(times[0].dailyServiceTimes.id, originalEnd.plusDays(5))

        val newGuardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        assertEquals(1, newGuardian1Notifications.size)
    }

    @Test
    fun `creating irregular daily service times automatically adds absences`() {
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                operationTimes =
                    // Open on weekdays for regular care
                    listOf(
                        TimeRange(LocalTime.parse("07:00"), LocalTime.parse("17:30")),
                        TimeRange(LocalTime.parse("07:00"), LocalTime.parse("17:30")),
                        TimeRange(LocalTime.parse("07:00"), LocalTime.parse("17:30")),
                        TimeRange(LocalTime.parse("07:00"), LocalTime.parse("17:30")),
                        TimeRange(LocalTime.parse("07:00"), LocalTime.parse("17:30")),
                        null,
                        null
                    ),
                // Also open on Saturdays for shift care
                shiftCareOperationTimes =
                    listOf(
                        TimeRange(LocalTime.parse("07:00"), LocalTime.parse("17:30")),
                        TimeRange(LocalTime.parse("07:00"), LocalTime.parse("17:30")),
                        TimeRange(LocalTime.parse("07:00"), LocalTime.parse("17:30")),
                        TimeRange(LocalTime.parse("07:00"), LocalTime.parse("17:30")),
                        TimeRange(LocalTime.parse("07:00"), LocalTime.parse("17:30")),
                        TimeRange(LocalTime.parse("07:00"), LocalTime.parse("17:30")),
                        null
                    ),
            )

        val placementEnd = dailyServiceTimesValidity.start.plusWeeks(3).minusDays(1)
        db.transaction { tx ->
            tx.insert(snDaycareFullDay35)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(
                    DevPlacement(
                        type = PlacementType.DAYCARE,
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = today,
                        endDate = placementEnd
                    )
                )
                .also { placementId ->
                    // First week: no shift care
                    tx.insert(
                        DevServiceNeed(
                            optionId = snDaycareFullDay35.id,
                            placementId = placementId,
                            startDate = dailyServiceTimesValidity.start,
                            endDate = dailyServiceTimesValidity.start.plusWeeks(1).minusDays(1),
                            confirmedBy = admin.evakaUserId,
                            shiftCare = ShiftCareType.NONE,
                        )
                    )
                    // Second week: shift care
                    tx.insert(
                        DevServiceNeed(
                            optionId = snDaycareFullDay35.id,
                            placementId = placementId,
                            startDate = dailyServiceTimesValidity.start.plusWeeks(1),
                            endDate = dailyServiceTimesValidity.start.plusWeeks(2).minusDays(1),
                            confirmedBy = admin.evakaUserId,
                            shiftCare = ShiftCareType.FULL,
                        )
                    )
                    // Third week: no service need
                }
        }

        val tuesday = dailyServiceTimesValidity.start
        val wednesday = tuesday.plusDays(1)
        val tuesdayBefore = tuesday.minusWeeks(1)
        val wednesdayBefore = wednesday.minusWeeks(1)
        addReservations(listOf(tuesdayBefore, wednesdayBefore, tuesday, wednesday))

        createDailyServiceTimes(
            child.id,
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

        val absenceDates =
            db.transaction { tx ->
                    tx.getAbsencesOfChildByRange(child.id, DateRange(today, placementEnd))
                }
                .map { it.date }
                .sorted()

        assertEquals(
            listOf(
                LocalDate.of(2022, 5, 11), // Wednesday of week 1 (no shift care)
                LocalDate.of(2022, 5, 18), // Wednesday of week 2 (shift care)
                LocalDate.of(2022, 5, 21), // Saturday of week 2 (shift care)
                LocalDate.of(2022, 5, 25), // Wednesday of week 3 (no service need)
            ),
            absenceDates
        )
        assertEquals(listOf(tuesdayBefore, wednesdayBefore, tuesday), getReservationDates())
    }

    private fun createDailyServiceTimes(
        childId: ChildId,
        dailyServiceTime: DailyServiceTimesValue,
    ) {
        dailyServiceTimesController.postDailyServiceTimes(
            dbInstance(),
            admin.user,
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
            admin.user,
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
            admin.user,
            MockEvakaClock(now),
            id,
            DailyServiceTimesController.DailyServiceTimesEndDate(endDate)
        )
    }

    private fun deleteDailyServiceTimes(id: DailyServiceTimesId) {
        dailyServiceTimesController.deleteDailyServiceTimes(
            dbInstance(),
            admin.user,
            MockEvakaClock(now),
            id
        )
    }

    private fun getDailyServiceTimes(
        childId: ChildId
    ): List<DailyServiceTimesController.DailyServiceTimesResponse> {
        return dailyServiceTimesController.getDailyServiceTimes(
            dbInstance(),
            admin.user,
            MockEvakaClock(now),
            childId
        )
    }

    private fun getDailyServiceTimeNotifications(
        user: DevPerson,
    ): List<DailyServiceTimeNotificationId> {
        return dailyServiceTimesCitizenController.getDailyServiceTimeNotifications(
            dbInstance(),
            user.user(CitizenAuthLevel.WEAK),
            MockEvakaClock(now)
        )
    }

    private fun dismissDailyServiceTimeNotification(
        user: DevPerson,
        notificationId: DailyServiceTimeNotificationId
    ) {
        dailyServiceTimesCitizenController.dismissDailyServiceTimeNotification(
            dbInstance(),
            user.user(CitizenAuthLevel.WEAK),
            MockEvakaClock(now),
            listOf(notificationId)
        )
    }

    private fun addReservations(dates: List<LocalDate>) {
        reservationControllerCitizen.postReservations(
            dbInstance(),
            AuthenticatedUser.Citizen(guardian1.id, CitizenAuthLevel.STRONG),
            MockEvakaClock(now),
            dates.map {
                DailyReservationRequest.Reservations(
                    childId = child.id,
                    date = it,
                    reservation = TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0))
                )
            }
        )
    }

    private fun getReservationDates(): List<LocalDate> =
        db.read {
            it.createQuery { sql("SELECT date FROM attendance_reservation ORDER BY date") }
                .toList<LocalDate>()
        }
}
