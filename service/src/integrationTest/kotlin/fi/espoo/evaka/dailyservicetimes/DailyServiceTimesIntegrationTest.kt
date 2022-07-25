// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dailyservicetimes

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.DailyReservationRequest
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DailyServiceTimeNotificationId
import fi.espoo.evaka.shared.DailyServiceTimesId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.withMockedTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals

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
    private fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
            tx.insertGuardian(testAdult_2.id, testChild_1.id)
            tx.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = ""))
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
    fun `adding a new daily service time creates a notification for both guardians`() {
        createDailyServiceTime(
            testChild_1.id,
            DailyServiceTimes.RegularTimes(
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
        createDailyServiceTime(
            testChild_1.id,
            DailyServiceTimes.RegularTimes(
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
                    testChild_1.id, now.toLocalDate().plusDays(105),
                    listOf(
                        fi.espoo.evaka.reservations.TimeRange(
                            LocalTime.of(10, 0),
                            LocalTime.of(12, 0)
                        )
                    ),
                    absent = false
                )
            )
        )
        createDailyServiceTime(
            testChild_1.id,
            DailyServiceTimes.RegularTimes(
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
        createDailyServiceTime(
            testChild_1.id,
            DailyServiceTimes.RegularTimes(
                validityPeriod = in100Days,
                regularTimes = tenToNoonRange
            )
        )

        val guardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        this.dismissDailyServiceTimeNotification(guardian1, guardian1Notifications[0].id)

        val times = this.getDailyServiceTimes(testChild_1.id)
        assertEquals(1, times.size)
        updateDailyServiceTime(
            times[0].dailyServiceTimes.id,
            DailyServiceTimes.RegularTimes(
                validityPeriod = in100Days,
                regularTimes = TimeRange(LocalTime.of(19, 0), LocalTime.of(22, 0))
            )
        )

        val newGuardian1Notifications = this.getDailyServiceTimeNotifications(guardian1)
        assertEquals(1, newGuardian1Notifications.size)
        assertEquals(false, newGuardian1Notifications[0].hasDeletedReservations)
    }

    private fun createDailyServiceTime(childId: ChildId, dailyServiceTime: DailyServiceTimes) {
        val (_, res, _) = http.post("/children/$childId/daily-service-times")
            .jsonBody(jsonMapper.writeValueAsString(dailyServiceTime))
            .asUser(admin)
            .withMockedTime(now)
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun updateDailyServiceTime(id: DailyServiceTimesId, dailyServiceTime: DailyServiceTimes) {
        val (_, res, _) = http.put("/daily-service-times/$id")
            .jsonBody(jsonMapper.writeValueAsString(dailyServiceTime))
            .asUser(admin)
            .withMockedTime(now)
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun getDailyServiceTimes(childId: ChildId): List<DailyServiceTimesController.DailyServiceTimesResponse> {
        val (_, res, result) = http.get("/children/$childId/daily-service-times")
            .asUser(admin)
            .withMockedTime(now)
            .responseObject<List<DailyServiceTimesController.DailyServiceTimesResponse>>(jsonMapper)

        assertEquals(200, res.statusCode)

        return result.get()
    }

    private fun getDailyServiceTimeNotifications(user: AuthenticatedUser.Citizen): List<DailyServiceTimeNotification> {
        val (_, res, result) = http.get("/citizen/daily-service-time-notifications")
            .asUser(user)
            .withMockedTime(now)
            .responseObject<List<DailyServiceTimeNotification>>(jsonMapper)

        assertEquals(200, res.statusCode)

        return result.get()
    }

    private fun dismissDailyServiceTimeNotification(user: AuthenticatedUser.Citizen, notificationId: DailyServiceTimeNotificationId) {
        val (_, res, _) = http.post("/citizen/daily-service-time-notifications/dismiss")
            .jsonBody(jsonMapper.writeValueAsString(listOf(notificationId)))
            .asUser(user)
            .withMockedTime(now)
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun postReservations(request: List<DailyReservationRequest>) {
        val (_, res, _) = http.post("/citizen/reservations")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG))
            .withMockedTime(now)
            .response()

        assertEquals(200, res.statusCode)
    }
}
