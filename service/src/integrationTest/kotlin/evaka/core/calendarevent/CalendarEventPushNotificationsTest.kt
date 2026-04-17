// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.calendarevent

import evaka.core.FullApplicationTest
import evaka.core.pis.service.insertGuardian
import evaka.core.shared.MobileDeviceId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCalendarEvent
import evaka.core.shared.dev.DevCalendarEventAttendee
import evaka.core.shared.dev.DevCalendarEventTime
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevMobileDevice
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.PilotFeature
import evaka.core.webpush.MockWebPushEndpoint
import evaka.core.webpush.PushNotificationCategory
import evaka.core.webpush.WebPushCrypto
import evaka.core.webpush.WebPushSubscription
import evaka.core.webpush.upsertPushGroup
import evaka.core.webpush.upsertPushSubscription
import java.net.URI
import java.security.SecureRandom
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CalendarEventPushNotificationsTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val clock = MockEvakaClock(2023, 1, 2, 12, 0) // monday
    private val keyPair = WebPushCrypto.generateKeyPair(SecureRandom())

    @Autowired private lateinit var mockEndpoint: MockWebPushEndpoint
    @Autowired private lateinit var calendarEventController: CalendarEventController
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(
            areaId = area.id,
            enabledPilotFeatures = setOf(PilotFeature.MESSAGING, PilotFeature.PUSH_NOTIFICATIONS),
        )
    private val group = DevDaycareGroup(daycareId = daycare.id)
    private val employee = DevEmployee()
    private val device =
        DevMobileDevice(
            unitId = daycare.id,
            pushNotificationCategories = setOf(PushNotificationCategory.CALENDAR_EVENT_RESERVATION),
        )
    private val citizen = DevPerson()
    private val child = DevPerson()
    private val placement =
        DevPlacement(
            childId = child.id,
            unitId = daycare.id,
            startDate = clock.today(),
            endDate = clock.today().plusYears(1),
        )
    private val groupPlacement =
        DevDaycareGroupPlacement(
            daycarePlacementId = placement.id,
            daycareGroupId = group.id,
            startDate = placement.startDate,
            endDate = placement.endDate,
        )
    private val calendarEvent =
        DevCalendarEvent(
            title = "Vasukeskustelu",
            description = "test",
            period = FiniteDateRange(clock.today().plusDays(1), clock.today().plusDays(9)),
            modifiedAt = clock.now(),
            modifiedBy = employee.evakaUserId,
            eventType = CalendarEventType.DISCUSSION_SURVEY,
        )
    private val calendarEventAttendee =
        DevCalendarEventAttendee(
            calendarEventId = calendarEvent.id,
            unitId = daycare.id,
            groupId = group.id,
        )
    private val calendarEventTime =
        DevCalendarEventTime(
            calendarEventId = calendarEvent.id,
            date = clock.today().plusDays(3),
            start = LocalTime.of(9, 0),
            end = LocalTime.of(10, 0),
            childId = null,
            modifiedAt = clock.now(),
            modifiedBy = employee.evakaUserId,
        )

    @BeforeEach
    fun beforeEach() {
        mockEndpoint.clearData()
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee, unitRoles = mapOf(daycare.id to UserRole.STAFF))
            tx.insert(device)
            tx.upsertPushGroup(clock.now(), device.id, group.id)
            tx.insert(citizen, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(guardianId = citizen.id, childId = child.id)
            tx.insert(placement)
            tx.insert(groupPlacement)
            tx.insert(calendarEvent)
            tx.insert(calendarEventAttendee)
            tx.insert(calendarEventTime)
        }
    }

    @Test
    fun `a push notification is sent when a citizen reserves a discussion time`() {
        val endpoint = URI("http://localhost:$httpPort/public/mock-web-push/subscription/1234")
        upsertSubscription(device.id, endpoint)

        calendarEventController.addCalendarEventTimeReservation(
            dbInstance(),
            citizen.user(CitizenAuthLevel.WEAK),
            clock,
            CalendarEventTimeCitizenReservationForm(
                calendarEventTimeId = calendarEventTime.id,
                childId = child.id,
            ),
        )
        asyncJobRunner.runPendingJobsSync(clock)
        assertNotificationSent()

        mockEndpoint.clearData()
        assertEquals(0, mockEndpoint.getCapturedRequests("1234").size)
        calendarEventController.deleteCalendarEventTimeReservation(
            dbInstance(),
            citizen.user(CitizenAuthLevel.WEAK),
            clock,
            calendarEventTimeId = calendarEventTime.id,
            childId = child.id,
        )
        asyncJobRunner.runPendingJobsSync(clock)
        assertNotificationSent()
    }

    private fun assertNotificationSent() {
        val request = mockEndpoint.getCapturedRequests("1234").single()
        assertEquals("normal", request.headers["urgency"])
        assertNotNull(request.headers["ttl"]?.toIntOrNull())
        assertTrue(request.headers["authorization"]?.startsWith("vapid") ?: false)
        assertEquals("aes128gcm", request.headers["content-encoding"])
        assertTrue(request.body.isNotEmpty())
    }

    private fun upsertSubscription(device: MobileDeviceId, endpoint: URI) =
        db.transaction { tx ->
            tx.upsertPushSubscription(
                device,
                WebPushSubscription(
                    endpoint = endpoint,
                    expires = null,
                    ecdhKey = WebPushCrypto.encode(keyPair.publicKey).toList(),
                    authSecret = listOf(0x00, 0x11, 0x22, 0x33),
                ),
            )
        }
}
