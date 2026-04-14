// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.mobile

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.CitizenPushSubscriptionDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CitizenPushSubscriptionControllerIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var controller: CitizenPushSubscriptionController

    private lateinit var clock: MockEvakaClock
    private val citizen = DevPerson()

    @BeforeEach
    fun setup() {
        clock = MockEvakaClock(2026, 4, 14, 12, 0)
        db.transaction { tx -> tx.insert(citizen, DevPersonType.ADULT) }
    }

    private fun user() = AuthenticatedUser.CitizenMobile(citizen.id, CitizenAuthLevel.WEAK)

    @Test
    fun `upsert creates a new subscription`() {
        val deviceId = CitizenPushSubscriptionDeviceId(UUID.randomUUID())
        controller.upsertSubscription(
            dbInstance(),
            user(),
            clock,
            CitizenPushSubscriptionController.UpsertBody(deviceId, "ExponentPushToken[AAA]"),
        )
        val row = db.read { tx -> tx.getCitizenPushSubscription(citizen.id, deviceId) }
        assertEquals("ExponentPushToken[AAA]", row?.expoPushToken)
    }

    @Test
    fun `upsert updates existing token for the same device`() {
        val deviceId = CitizenPushSubscriptionDeviceId(UUID.randomUUID())
        controller.upsertSubscription(
            dbInstance(),
            user(),
            clock,
            CitizenPushSubscriptionController.UpsertBody(deviceId, "ExponentPushToken[OLD]"),
        )
        controller.upsertSubscription(
            dbInstance(),
            user(),
            clock,
            CitizenPushSubscriptionController.UpsertBody(deviceId, "ExponentPushToken[NEW]"),
        )
        val row = db.read { tx -> tx.getCitizenPushSubscription(citizen.id, deviceId) }
        assertEquals("ExponentPushToken[NEW]", row?.expoPushToken)
    }

    @Test
    fun `delete removes the subscription`() {
        val deviceId = CitizenPushSubscriptionDeviceId(UUID.randomUUID())
        controller.upsertSubscription(
            dbInstance(),
            user(),
            clock,
            CitizenPushSubscriptionController.UpsertBody(deviceId, "ExponentPushToken[AAA]"),
        )
        controller.deleteSubscription(dbInstance(), user(), clock, deviceId)
        val row = db.read { tx -> tx.getCitizenPushSubscription(citizen.id, deviceId) }
        assertNull(row)
    }
}
