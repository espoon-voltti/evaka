// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.pairing.MobileDeviceIdentity
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevMobileDevice
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestMobileDevice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class SystemIdentityControllerTest : FullApplicationTest() {
    private lateinit var areaId: UUID
    private lateinit var unitId: UUID

    @BeforeEach
    protected fun beforeEach() {
        db.transaction { it.resetDatabase() }
        areaId = db.transaction { it.handle.insertTestCareArea(DevCareArea()) }
        unitId = db.transaction { it.handle.insertTestDaycare(DevDaycare(areaId = areaId)) }
    }

    @Test
    fun `mobile identity endpoint can find a device by its token`() {
        val token = UUID.randomUUID()
        val deviceId = db.transaction { it.insertTestDevice(longTermToken = token) }

        val (_, res, result) = http.get("/system/mobile-identity/$token").asUser(AuthenticatedUser.machineUser)
            .responseObject<MobileDeviceIdentity>()
        assertTrue(res.isSuccessful)
        assertEquals(MobileDeviceIdentity(id = deviceId, longTermToken = token), result.get())
    }

    @Test
    fun `mobile identity endpoint doesn't return deleted mobile devices`() {
        val token = UUID.randomUUID()
        db.transaction { it.insertTestDevice(longTermToken = token, deleted = true) }

        val (_, res, _) = http.get("/system/mobile-identity/$token").asUser(AuthenticatedUser.machineUser)
            .response()
        assertEquals(404, res.statusCode)
    }

    private fun Database.Transaction.insertTestDevice(longTermToken: UUID? = null, deleted: Boolean = false): UUID {
        val id = handle.insertTestEmployee(DevEmployee())
        insertTestMobileDevice(
            DevMobileDevice(
                id = id,
                unitId = unitId,
                longTermToken = longTermToken,
                deleted = deleted
            )
        )
        return id
    }
}
