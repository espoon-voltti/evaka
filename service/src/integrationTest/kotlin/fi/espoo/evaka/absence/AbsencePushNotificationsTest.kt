// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.absence

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.reservations.AbsenceRequest
import fi.espoo.evaka.reservations.ReservationControllerCitizen
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AbsencePushNotificationsTest : FullApplicationTest(resetDbBeforeEach = true) {
    private lateinit var clock: MockEvakaClock
    private val keyPair = WebPushCrypto.generateKeyPair(SecureRandom())

    @Autowired private lateinit var mockEndpoint: MockWebPushEndpoint
    @Autowired private lateinit var reservationControllerCitizen: ReservationControllerCitizen
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val area = DevCareArea()
    private lateinit var group: GroupId
    private lateinit var device: MobileDeviceId
    private val citizen = DevPerson()
    private val child = DevPerson()

    @BeforeAll
    override fun beforeAll() {
        super.beforeAll()
        clock = MockEvakaClock(2023, 1, 2, 12, 0) // monday
    }

    @BeforeEach
    fun beforeEach() {
        mockEndpoint.clearData()
        db.transaction { tx ->
            tx.insert(area)
            val unit =
                tx.insert(
                    DevDaycare(
                        areaId = area.id,
                        enabledPilotFeatures =
                            setOf(PilotFeature.MESSAGING, PilotFeature.PUSH_NOTIFICATIONS),
                    )
                )
            group = tx.insert(DevDaycareGroup(daycareId = unit))
            device =
                tx.insert(
                    DevMobileDevice(
                        unitId = unit,
                        pushNotificationCategories = setOf(PushNotificationCategory.NEW_ABSENCE),
                    )
                )
            tx.upsertPushGroup(clock.now(), device, group)
            tx.insert(citizen, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(guardianId = citizen.id, childId = child.id)
            val placement =
                DevPlacement(
                    childId = child.id,
                    unitId = unit,
                    startDate = clock.today(),
                    endDate = clock.today().plusYears(1),
                )
            tx.insert(placement)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement.id,
                    daycareGroupId = group,
                    startDate = placement.startDate,
                    endDate = placement.endDate,
                )
            )
        }
    }

    @Test
    fun `a push notification is sent when a citizen adds an absence for today`() {
        val endpoint = URI("http://localhost:$httpPort/public/mock-web-push/subscription/1234")
        upsertSubscription(device, endpoint)

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
        upsertSubscription(device, endpoint)

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
