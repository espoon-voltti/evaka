// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.absence

import evaka.core.FullApplicationTest
import evaka.core.pis.service.insertGuardian
import evaka.core.reservations.AbsenceRequest
import evaka.core.reservations.ReservationControllerCitizen
import evaka.core.shared.MobileDeviceId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AbsencePushNotificationsTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val clock = MockEvakaClock(2023, 1, 2, 12, 0) // monday
    private val keyPair = WebPushCrypto.generateKeyPair(SecureRandom())

    @Autowired private lateinit var mockEndpoint: MockWebPushEndpoint
    @Autowired private lateinit var reservationControllerCitizen: ReservationControllerCitizen
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(
            areaId = area.id,
            enabledPilotFeatures = setOf(PilotFeature.MESSAGING, PilotFeature.PUSH_NOTIFICATIONS),
        )
    private val group = DevDaycareGroup(daycareId = daycare.id)
    private val device =
        DevMobileDevice(
            unitId = daycare.id,
            pushNotificationCategories = setOf(PushNotificationCategory.NEW_ABSENCE),
        )
    private val citizen = DevPerson()
    private val child = DevPerson()

    @BeforeEach
    fun beforeEach() {
        mockEndpoint.clearData()
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(device)
            tx.upsertPushGroup(clock.now(), device.id, group.id)
            tx.insert(citizen, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(guardianId = citizen.id, childId = child.id)
            val placement =
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusYears(1),
                )
            tx.insert(placement)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement.id,
                    daycareGroupId = group.id,
                    startDate = placement.startDate,
                    endDate = placement.endDate,
                )
            )
        }
    }

    @Test
    fun `a push notification is sent when a citizen adds an absence for today`() {
        val endpoint = URI("http://localhost:$httpPort/public/mock-web-push/subscription/1234")
        upsertSubscription(device.id, endpoint)

        val range = FiniteDateRange(start = clock.today(), end = clock.today().plusDays(1))
        db.transaction { tx ->
            reservationControllerCitizen.postAbsences(
                dbInstance(),
                citizen.user(CitizenAuthLevel.WEAK),
                clock,
                AbsenceRequest(
                    childIds = setOf(child.id),
                    dateRange = range,
                    absenceType = AbsenceType.SICKLEAVE,
                ),
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)

        val request = mockEndpoint.getCapturedRequests("1234").single()
        assertEquals("normal", request.headers["urgency"])
        assertNotNull(request.headers["ttl"]?.toIntOrNull())
        assertTrue(request.headers["authorization"]?.startsWith("vapid") ?: false)
        assertEquals("aes128gcm", request.headers["content-encoding"])
        assertTrue(request.body.isNotEmpty())
    }

    @Test
    fun `a push notification is not sent when a citizen adds an absence for future days`() {
        val endpoint = URI("http://localhost:$httpPort/public/mock-web-push/subscription/1234")
        upsertSubscription(device.id, endpoint)

        val range =
            FiniteDateRange(start = clock.today().plusDays(1), end = clock.today().plusDays(2))
        db.transaction { tx ->
            reservationControllerCitizen.postAbsences(
                dbInstance(),
                citizen.user(CitizenAuthLevel.WEAK),
                clock,
                AbsenceRequest(
                    childIds = setOf(child.id),
                    dateRange = range,
                    absenceType = AbsenceType.SICKLEAVE,
                ),
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(0, mockEndpoint.getCapturedRequests("1234").size)
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
