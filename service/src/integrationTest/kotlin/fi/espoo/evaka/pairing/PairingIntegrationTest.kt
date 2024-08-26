// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.pis.SystemController
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.updateDaycareAclWithEmployee
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PairingIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var systemController: SystemController
    @Autowired private lateinit var mobileDevicesController: MobileDevicesController
    @Autowired private lateinit var pairingsController: PairingsController

    private val clock = MockEvakaClock(2023, 1, 1, 12, 0)

    private lateinit var user: AuthenticatedUser.Employee
    private lateinit var testUnit: DaycareId

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            val area = tx.insert(DevCareArea())
            testUnit = tx.insert(DevDaycare(areaId = area))
            val employee = tx.insert(DevEmployee())
            user = AuthenticatedUser.Employee(employee, emptySet())
            tx.updateDaycareAclWithEmployee(testUnit, user.id, UserRole.UNIT_SUPERVISOR)
        }
    }

    @Test
    fun `mobile device life-cycle happy flow`() {
        // no devices initially
        val listRes1 = getMobileDevices()
        assertEquals(0, listRes1.size)

        // web: init pairing
        val res1 = postPairing(testUnit)
        val id = res1.id
        val challengeKey = res1.challengeKey
        assertEquals(testUnit, res1.unitId)
        assertEquals(12, challengeKey.length)
        assertNull(res1.responseKey)
        assertNull(res1.mobileDeviceId)
        assertEquals(PairingStatus.WAITING_CHALLENGE, res1.status)

        // status polling
        assertEquals(PairingStatus.WAITING_CHALLENGE, getPairingStatus(id))

        // mobile: challenge
        val res2 = postPairingChallenge(challengeKey)
        val responseKey = res2.responseKey
        assertEquals(id, res2.id)
        assertEquals(testUnit, res2.unitId)
        assertEquals(challengeKey, res2.challengeKey)
        assertEquals(12, responseKey!!.length)
        assertNull(res1.mobileDeviceId)
        assertEquals(PairingStatus.WAITING_RESPONSE, res2.status)

        // status polling
        assertEquals(PairingStatus.WAITING_RESPONSE, getPairingStatus(id))

        // web: response to challenge
        val res3 = postPairingResponse(id, challengeKey, responseKey)
        assertEquals(id, res3.id)
        assertEquals(testUnit, res3.unitId)
        assertEquals(challengeKey, res3.challengeKey)
        assertEquals(responseKey, res3.responseKey)
        val deviceId = res3.mobileDeviceId!!
        assertEquals(PairingStatus.READY, res3.status)

        // web: device has been created with default name
        val listRes2 = getMobileDevices()
        assertEquals(1, listRes2.size)
        assertEquals(deviceId, listRes2.first().id)
        assertEquals("Nimeämätön laite", listRes2.first().name)

        // web: renaming device
        putMobileDeviceName(deviceId, "Hippiäiset")
        val listRes3 = getMobileDevices()
        assertEquals(1, listRes3.size)
        assertEquals(deviceId, listRes3.first().id)
        assertEquals("Hippiäiset", listRes3.first().name)

        // status polling
        assertEquals(PairingStatus.READY, getPairingStatus(id))

        // mobile > apigw: validating pairing to create session
        val res4 = postPairingValidation(id, challengeKey, responseKey)
        assertEquals(deviceId, res4.id)

        // status polling
        assertEquals(PairingStatus.PAIRED, getPairingStatus(id))

        // mobile > apigw: fetching device to recreate session
        val res5 = authenticateMobileDevice(deviceId)
        assertEquals(deviceId, res5.id)

        // web: removing device
        deleteMobileDevice(deviceId)
        val listRes4 = getMobileDevices()
        assertEquals(0, listRes4.size)

        // mobile > apigw: fetching device to recreate session should fail
        assertThrows<NotFound> { authenticateMobileDevice(deviceId) }
    }

    @Test
    fun `postPairingChallenge - happy case`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.WAITING_CHALLENGE,
                challengeKey = "foo",
                responseKey = null,
                expires = clock.now().plusMinutes(1),
            )
        givenPairing(pairing)
        postPairingChallenge(pairing.challengeKey)
    }

    @Test
    fun `postPairingChallenge - pairing missing returns 404`() {
        assertThrows<NotFound> { postPairingChallenge("foo") }
    }

    @Test
    fun `postPairingChallenge - pairing expired returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.WAITING_CHALLENGE,
                challengeKey = "foo",
                responseKey = null,
                expires = clock.now().minusMinutes(1),
            )
        givenPairing(pairing)
        assertThrows<NotFound> { postPairingChallenge(pairing.challengeKey) }
    }

    @Test
    fun `postPairingChallenge - pairing in wrong status returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.WAITING_RESPONSE,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
            )
        givenPairing(pairing)
        assertThrows<NotFound> { postPairingChallenge(pairing.challengeKey) }
    }

    @Test
    fun `postPairingResponse - happy case`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.WAITING_RESPONSE,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
            )
        givenPairing(pairing)
        postPairingResponse(pairing.id, pairing.challengeKey, pairing.responseKey!!)
    }

    @Test
    fun `postPairingResponse - wrong id returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.WAITING_RESPONSE,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
            )
        givenPairing(pairing)
        assertThrows<NotFound> {
            postPairingResponse(
                PairingId(UUID.randomUUID()),
                pairing.challengeKey,
                pairing.responseKey!!,
            )
        }
    }

    @Test
    fun `postPairingResponse - wrong challengeKey returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.WAITING_RESPONSE,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
            )
        givenPairing(pairing)
        assertThrows<NotFound> { postPairingResponse(pairing.id, "wrong", pairing.responseKey!!) }
    }

    @Test
    fun `postPairingResponse - wrong responseKey returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.WAITING_RESPONSE,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
            )
        givenPairing(pairing)
        assertThrows<NotFound> { postPairingResponse(pairing.id, pairing.challengeKey, "wrong") }
    }

    @Test
    fun `postPairingResponse - wrong status returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.READY,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
            )
        givenPairing(pairing)
        assertThrows<NotFound> {
            postPairingResponse(pairing.id, pairing.challengeKey, pairing.responseKey!!)
        }
    }

    @Test
    fun `postPairingResponse - attempts exceeded returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.WAITING_RESPONSE,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
            )
        givenPairing(pairing, attempts = 101)
        assertThrows<NotFound> {
            postPairingResponse(pairing.id, pairing.challengeKey, pairing.responseKey!!)
        }
    }

    @Test
    fun `postPairingResponse - pairing expired returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.WAITING_RESPONSE,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().minusMinutes(1),
            )
        givenPairing(pairing)
        assertThrows<NotFound> {
            postPairingResponse(pairing.id, pairing.challengeKey, pairing.responseKey!!)
        }
    }

    @Test
    fun `postPairingValidation - happy case`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.READY,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
                mobileDeviceId = MobileDeviceId(UUID.randomUUID()),
            )
        givenPairing(pairing)
        postPairingValidation(pairing.id, pairing.challengeKey, pairing.responseKey!!)
    }

    @Test
    fun `postPairingValidation - wrong id returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.READY,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
                mobileDeviceId = MobileDeviceId(UUID.randomUUID()),
            )
        givenPairing(pairing)
        assertThrows<NotFound> {
            postPairingValidation(
                PairingId(UUID.randomUUID()),
                pairing.challengeKey,
                pairing.responseKey!!,
            )
        }
    }

    @Test
    fun `postPairingValidation - wrong challengeKey returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.READY,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
                mobileDeviceId = MobileDeviceId(UUID.randomUUID()),
            )
        givenPairing(pairing)
        assertThrows<NotFound> { postPairingValidation(pairing.id, "wrong", pairing.responseKey!!) }
    }

    @Test
    fun `postPairingValidation - wrong responseKey returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.READY,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
                mobileDeviceId = MobileDeviceId(UUID.randomUUID()),
            )
        givenPairing(pairing)
        assertThrows<NotFound> { postPairingValidation(pairing.id, pairing.challengeKey, "wrong") }
    }

    @Test
    fun `postPairingValidation - wrong status returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.PAIRED,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
                mobileDeviceId = MobileDeviceId(UUID.randomUUID()),
            )
        givenPairing(pairing)
        assertThrows<NotFound> {
            postPairingValidation(pairing.id, pairing.challengeKey, pairing.responseKey!!)
        }
    }

    @Test
    fun `postPairingValidation - attempts exceeded returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.READY,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().plusMinutes(1),
                mobileDeviceId = MobileDeviceId(UUID.randomUUID()),
            )
        givenPairing(pairing, attempts = 101)
        assertThrows<NotFound> {
            postPairingValidation(pairing.id, pairing.challengeKey, pairing.responseKey!!)
        }
    }

    @Test
    fun `postPairingValidation - pairing expired returns 404`() {
        val pairing =
            Pairing(
                id = PairingId(UUID.randomUUID()),
                unitId = testUnit,
                status = PairingStatus.READY,
                challengeKey = "foo",
                responseKey = "bar",
                expires = clock.now().minusMinutes(1),
                mobileDeviceId = MobileDeviceId(UUID.randomUUID()),
            )
        givenPairing(pairing)
        assertThrows<NotFound> {
            postPairingValidation(pairing.id, pairing.challengeKey, pairing.responseKey!!)
        }
    }

    @Test
    fun `generatePairingKey - values are unique, chars are easy to distinguish and no uppercase chars`() {
        val count = 100
        val values = (1..count).map { generatePairingKey() }.toSet()
        assertEquals(count, values.size)
        assertTrue(values.all { key -> key.length == 12 })

        val concatenated = values.joinToString(separator = "")
        val badChars = listOf('i', 'l', 'I', '1', 'o', 'O', '0', ' ')
        badChars.forEach { c -> assertFalse(concatenated.contains(c, ignoreCase = false)) }
        concatenated.forEach { c -> assertFalse(c.isUpperCase()) }
    }

    private fun givenPairing(pairing: Pairing, attempts: Int = 0) {
        db.transaction { tx ->
            if (pairing.mobileDeviceId != null) {
                tx.execute {
                    sql(
                        "INSERT INTO employee (id, first_name, last_name, active) VALUES (${bind(pairing.mobileDeviceId)}, '', '', TRUE)"
                    )
                }
                tx.execute {
                    sql(
                        "INSERT INTO mobile_device (id, unit_id, name) VALUES (${bind(pairing.mobileDeviceId)}, ${bind(pairing.unitId)}, 'Laite')"
                    )
                }
            }
            tx.execute {
                sql(
                    """
            INSERT INTO pairing (id, unit_id, expires, status, challenge_key, response_key, attempts, mobile_device_id) 
            VALUES (${bind(pairing.id)}, ${bind(pairing.unitId)}, ${bind(pairing.expires)}, ${bind(pairing.status)}, ${bind(pairing.challengeKey)}, ${bind(pairing.responseKey)}, ${bind(attempts)}, ${bind(pairing.mobileDeviceId)});
            """
                )
            }
        }
    }

    private fun postPairing(unitId: DaycareId): Pairing =
        pairingsController.postPairing(
            dbInstance(),
            user,
            clock,
            PairingsController.PostPairingReq.Unit(unitId = unitId),
        )

    private fun postPairingChallenge(challengeKey: String): Pairing =
        pairingsController.postPairingChallenge(
            dbInstance(),
            clock,
            PairingsController.PostPairingChallengeReq(challengeKey = challengeKey),
        )

    private fun postPairingResponse(
        id: PairingId,
        challengeKey: String,
        responseKey: String,
    ): Pairing =
        pairingsController.postPairingResponse(
            dbInstance(),
            user,
            clock,
            id,
            PairingsController.PostPairingResponseReq(
                challengeKey = challengeKey,
                responseKey = responseKey,
            ),
        )

    private fun postPairingValidation(
        id: PairingId,
        challengeKey: String,
        responseKey: String,
    ): MobileDeviceIdentity =
        pairingsController.postPairingValidation(
            dbInstance(),
            AuthenticatedUser.SystemInternalUser,
            clock,
            id,
            PairingsController.PostPairingValidationReq(
                challengeKey = challengeKey,
                responseKey = responseKey,
            ),
        )

    private fun getPairingStatus(id: PairingId): PairingStatus =
        pairingsController.getPairingStatus(dbInstance(), clock, id).status

    private fun getMobileDevices(): List<MobileDevice> =
        mobileDevicesController.getMobileDevices(dbInstance(), user, clock, testUnit)

    private fun putMobileDeviceName(id: MobileDeviceId, name: String) =
        mobileDevicesController.putMobileDeviceName(
            dbInstance(),
            user,
            clock,
            id,
            MobileDevicesController.RenameRequest(name),
        )

    private fun deleteMobileDevice(id: MobileDeviceId) =
        mobileDevicesController.deleteMobileDevice(dbInstance(), user, clock, id)

    private fun authenticateMobileDevice(id: MobileDeviceId): MobileDeviceDetails =
        systemController.authenticateMobileDevice(
            dbInstance(),
            AuthenticatedUser.SystemInternalUser,
            clock,
            id,
            SystemController.MobileDeviceTracking(userAgent = "007"),
        )
}
