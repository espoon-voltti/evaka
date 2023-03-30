// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dailyservicetimes

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.getAbsencesOfChildByRange
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.DailyReservationRequest
import fi.espoo.evaka.reservations.Reservation
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DailyServiceTimeNotificationId
import fi.espoo.evaka.shared.DailyServiceTimesId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDailyServiceTimes
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDailyServiceTimes
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.withMockedTime
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DailyServiceTimesIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val admin = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))
    private val guardian1 = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.WEAK)
    private val guardian2 = AuthenticatedUser.Citizen(testAdult_2.id, CitizenAuthLevel.WEAK)

    private val groupId = GroupId(UUID.randomUUID())
    private val daycarePlacementId = PlacementId(UUID.randomUUID())
    private val now = HelsinkiDateTime.of(LocalDate.of(2022, 2, 3), LocalTime.of(12, 5, 1))
    private val placementStart = now.toLocalDate().minusDays(30)
    private val placementEnd = now.toLocalDate().plusDays(120)

    private val in100Days = DateRange(now.toLocalDate().plusDays(100), null)
    private val tenToNoonRange = TimeRange(LocalTime.of(10, 0), LocalTime.of(12, 0))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
            tx.insertGuardian(testAdult_2.id, testChild_1.id)
            tx.insertTestDaycareGroup(
                DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = "")
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
        }
    }

    @Test
    fun `cannot create, update or delete if validity has started`() {
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(DateRange(now.toLocalDate(), null), tenToNoonRange),
            400
        )

        val id = DailyServiceTimesId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insertTestDailyServiceTimes(
                DevDailyServiceTimes(
                    id = id,
                    childId = testChild_1.id,
                    validityPeriod = DateRange(now.toLocalDate(), null)
                )
            )
        }

        updateDailyServiceTimes(
            id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(now.toLocalDate().minusDays(1), null),
                regularTimes = TimeRange(LocalTime.of(19, 0), LocalTime.of(22, 0))
            ),
            400
        )

        deleteDailyServiceTimes(id, 400)
    }

    @Test
    fun `can set end time only if it is in the future`() {
        val idFuture = DailyServiceTimesId(UUID.randomUUID())
        val past = DailyServiceTimesId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insertTestDailyServiceTimes(
                DevDailyServiceTimes(
                    id = idFuture,
                    childId = testChild_1.id,
                    validityPeriod = DateRange(now.toLocalDate(), null)
                )
            )
            tx.insertTestDailyServiceTimes(
                DevDailyServiceTimes(
                    id = past,
                    childId = testChild_2.id,
                    validityPeriod = DateRange(now.toLocalDate().minusDays(1), now.toLocalDate())
                )
            )
        }

        // Set to future
        setDailyServiceTimesEndDate(idFuture, now.toLocalDate().plusDays(1), 200)
        // Set to past -> not allowed
        setDailyServiceTimesEndDate(idFuture, now.toLocalDate(), 400)

        // Already in the past -> not allowed
        setDailyServiceTimesEndDate(past, now.toLocalDate().plusDays(1), 400)
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
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod =
                    DateRange(now.toLocalDate().plusDays(30), now.toLocalDate().plusDays(50)),
                regularTimes = tenToNoonRange
            ),
            409
        )

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
            ),
            200
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
            tx.insertTestDailyServiceTimes(
                DevDailyServiceTimes(
                    id = id,
                    childId = testChild_1.id,
                    validityPeriod =
                        DateRange(now.toLocalDate().plusDays(1), now.toLocalDate().plusDays(10))
                )
            )
            tx.insertTestDailyServiceTimes(
                DevDailyServiceTimes(
                    childId = testChild_1.id,
                    validityPeriod = DateRange(now.toLocalDate().plusDays(11), null)
                )
            )
        }

        // Attempt updating the first entry so that it overlaps with the second -> not allowed

        updateDailyServiceTimes(
            id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod =
                    DateRange(now.toLocalDate().plusDays(1), now.toLocalDate().plusDays(11)),
                regularTimes = tenToNoonRange
            ),
            400
        )

        setDailyServiceTimesEndDate(id, now.toLocalDate().plusDays(11), 400)
    }

    @Test
    fun `adding a new daily service time creates a notification for both guardians`() {
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = in100Days,
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
                validityPeriod = in100Days,
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
        this.postReservations(
            listOf(
                DailyReservationRequest(
                    testChild_1.id,
                    now.toLocalDate().plusDays(105),
                    listOf(Reservation.Times(LocalTime.of(10, 0), LocalTime.of(12, 0))),
                    absent = false
                )
            )
        )
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = in100Days,
                regularTimes = tenToNoonRange
            )
        )

        val guardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        assertEquals(1, guardian1Notifications.size)
        assertEquals(true, guardian1Notifications[0].hasDeletedReservations)
    }

    @Test
    fun `updating a daily service time creates a new notification`() {
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = in100Days,
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
                validityPeriod = in100Days,
                regularTimes = TimeRange(LocalTime.of(19, 0), LocalTime.of(22, 0))
            )
        )

        val newGuardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        assertEquals(1, newGuardian1Notifications.size)
        assertEquals(false, newGuardian1Notifications[0].hasDeletedReservations)
    }

    @Test
    fun `updating a daily service times validity end creates a new notification`() {
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = in100Days,
                regularTimes = tenToNoonRange
            )
        )

        val guardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        this.dismissDailyServiceTimeNotification(guardian1, guardian1Notifications[0].id)

        val times = this.getDailyServiceTimes(testChild_1.id)
        assertEquals(1, times.size)
        setDailyServiceTimesEndDate(times[0].dailyServiceTimes.id, LocalDate.now().plusDays(102))

        val newGuardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        assertEquals(1, newGuardian1Notifications.size)
        assertEquals(false, newGuardian1Notifications[0].hasDeletedReservations)
    }

    @Test
    fun `creating irregular daily service times automatically adds absences`() {
        createDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.IrregularTimes(
                validityPeriod = in100Days,
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
        expectedStatus: Int = 200
    ) {
        val (_, res, _) =
            http
                .post("/children/$childId/daily-service-times")
                .jsonBody(jsonMapper.writeValueAsString(dailyServiceTime))
                .asUser(admin)
                .withMockedTime(now)
                .response()

        assertEquals(expectedStatus, res.statusCode)
    }

    private fun updateDailyServiceTimes(
        id: DailyServiceTimesId,
        dailyServiceTime: DailyServiceTimesValue,
        expectedStatus: Int = 200
    ) {
        val (_, res, _) =
            http
                .put("/daily-service-times/$id")
                .jsonBody(jsonMapper.writeValueAsString(dailyServiceTime))
                .asUser(admin)
                .withMockedTime(now)
                .response()

        assertEquals(expectedStatus, res.statusCode)
    }

    private fun setDailyServiceTimesEndDate(
        id: DailyServiceTimesId,
        endDate: LocalDate,
        expectedStatus: Int = 200
    ) {
        val (_, res, _) =
            http
                .put("/daily-service-times/$id/end")
                .jsonBody(
                    jsonMapper.writeValueAsString(
                        DailyServiceTimesController.DailyServiceTimesEndDate(endDate)
                    )
                )
                .asUser(admin)
                .withMockedTime(now)
                .response()

        assertEquals(expectedStatus, res.statusCode)
    }

    private fun deleteDailyServiceTimes(id: DailyServiceTimesId, expectedStatus: Int = 200) {
        val (_, res, _) =
            http.delete("/daily-service-times/$id").asUser(admin).withMockedTime(now).response()

        assertEquals(expectedStatus, res.statusCode)
    }

    private fun getDailyServiceTimes(
        childId: ChildId
    ): List<DailyServiceTimesController.DailyServiceTimesResponse> {
        val (_, res, result) =
            http
                .get("/children/$childId/daily-service-times")
                .asUser(admin)
                .withMockedTime(now)
                .responseObject<List<DailyServiceTimesController.DailyServiceTimesResponse>>(
                    jsonMapper
                )

        assertEquals(200, res.statusCode)

        return result.get()
    }

    private fun getDailyServiceTimeNotifications(
        user: AuthenticatedUser.Citizen
    ): List<DailyServiceTimeNotification> {
        val (_, res, result) =
            http
                .get("/citizen/daily-service-time-notifications")
                .asUser(user)
                .withMockedTime(now)
                .responseObject<List<DailyServiceTimeNotification>>(jsonMapper)

        assertEquals(200, res.statusCode)

        return result.get()
    }

    private fun dismissDailyServiceTimeNotification(
        user: AuthenticatedUser.Citizen,
        notificationId: DailyServiceTimeNotificationId
    ) {
        val (_, res, _) =
            http
                .post("/citizen/daily-service-time-notifications/dismiss")
                .jsonBody(jsonMapper.writeValueAsString(listOf(notificationId)))
                .asUser(user)
                .withMockedTime(now)
                .response()

        assertEquals(200, res.statusCode)
    }

    private fun postReservations(request: List<DailyReservationRequest>) {
        val (_, res, _) =
            http
                .post("/citizen/reservations")
                .jsonBody(jsonMapper.writeValueAsString(request))
                .asUser(AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG))
                .withMockedTime(now)
                .response()

        assertEquals(200, res.statusCode)
    }
}
