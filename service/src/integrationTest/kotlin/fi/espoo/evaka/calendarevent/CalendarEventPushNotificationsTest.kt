// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCalendarEvent
import fi.espoo.evaka.shared.dev.DevCalendarEventAttendee
import fi.espoo.evaka.shared.dev.DevCalendarEventTime
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevMobileDevice
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.webpush.MockWebPushEndpoint
import fi.espoo.evaka.webpush.PushNotificationCategory
import fi.espoo.evaka.webpush.WebPushCrypto
import fi.espoo.evaka.webpush.WebPushSubscription
import fi.espoo.evaka.webpush.upsertPushGroup
import fi.espoo.evaka.webpush.upsertPushSubscription
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
