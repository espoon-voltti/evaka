// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.updateDaycareAclWithEmployee
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PairingIntegrationTest : FullApplicationTest() {
    private val user = AuthenticatedUser.Employee(testDecisionMaker_1.id, emptySet())
    private val testUnitId = testDaycare.id

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
            tx.updateDaycareAclWithEmployee(testUnitId, user.id, UserRole.UNIT_SUPERVISOR)
        }
    }

    @Test
    fun `mobile device life-cycle happy flow`() {
        // no devices initially
        val listRes1 = getMobileDevicesAssertOk()
        assertEquals(0, listRes1.size)

        // web: init pairing
        val res1 = postPairingAssertOk(testUnitId)
        val id = res1.id
        val challengeKey = res1.challengeKey
        assertEquals(testUnitId, res1.unitId)
        assertEquals(10, challengeKey.length)
        assertNull(res1.responseKey)
        assertNull(res1.mobileDeviceId)
        assertEquals(PairingStatus.WAITING_CHALLENGE, res1.status)

        // status polling
        assertEquals(PairingStatus.WAITING_CHALLENGE, getPairingStatusAssertOk(id, authenticated = true))

        // mobile: challenge
        val res2 = postPairingChallengeAssertOk(challengeKey)
        val responseKey = res2.responseKey
        assertEquals(id, res2.id)
        assertEquals(testUnitId, res2.unitId)
        assertEquals(challengeKey, res2.challengeKey)
        assertEquals(10, responseKey!!.length)
        assertNull(res1.mobileDeviceId)
        assertEquals(PairingStatus.WAITING_RESPONSE, res2.status)

        // status polling
        assertEquals(PairingStatus.WAITING_RESPONSE, getPairingStatusAssertOk(id, authenticated = false))
        assertEquals(PairingStatus.WAITING_RESPONSE, getPairingStatusAssertOk(id, authenticated = true))

        // web: response to challenge
        val res3 = postPairingResponseAssertOk(id, challengeKey, responseKey)
        assertEquals(id, res3.id)
        assertEquals(testUnitId, res3.unitId)
        assertEquals(challengeKey, res3.challengeKey)
        assertEquals(responseKey, res3.responseKey)
        val deviceId = res3.mobileDeviceId!!
        assertEquals(PairingStatus.READY, res3.status)

        // web: device has been created with default name
        val listRes2 = getMobileDevicesAssertOk()
        assertEquals(1, listRes2.size)
        assertEquals(deviceId, listRes2.first().id)
        assertEquals("Nimeämätön laite", listRes2.first().name)

        // web: renaming device
        putMobileDeviceNameAssertOk(deviceId, "Hippiäiset")
        val listRes3 = getMobileDevicesAssertOk()
        assertEquals(1, listRes3.size)
        assertEquals(deviceId, listRes3.first().id)
        assertEquals("Hippiäiset", listRes3.first().name)

        // status polling
        assertEquals(PairingStatus.READY, getPairingStatusAssertOk(id, authenticated = false))
        assertEquals(PairingStatus.READY, getPairingStatusAssertOk(id, authenticated = true))

        // mobile > apigw: validating pairing to create session
        val res4 = postPairingValidationAssertOk(id, challengeKey, responseKey)
        assertEquals(deviceId, res4.id)

        // status polling
        assertEquals(PairingStatus.PAIRED, getPairingStatusAssertOk(id, authenticated = false))

        // mobile > apigw: fetching device to recreate session
        val res5 = getMobileDeviceAssertOk(deviceId)
        assertEquals(deviceId, res5.id)

        // web: removing device
        deleteMobileDeviceAssertOk(deviceId)
        val listRes4 = getMobileDevicesAssertOk()
        assertEquals(0, listRes4.size)

        // mobile > apigw: fetching device to recreate session should fail
        getMobileDeviceAssertFail(deviceId, 404)
    }

    @Test
    fun `postPairingChallenge - happy case`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.WAITING_CHALLENGE,
            challengeKey = "foo",
            responseKey = null,
            expires = HelsinkiDateTime.now().plusMinutes(1)
        )
        givenPairing(pairing)
        postPairingChallengeAssertOk(pairing.challengeKey)
    }

    @Test
    fun `postPairingChallenge - pairing missing returns 404`() {
        postPairingChallengeAssertFail("foo", 404)
    }

    @Test
    fun `postPairingChallenge - pairing expired returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.WAITING_CHALLENGE,
            challengeKey = "foo",
            responseKey = null,
            expires = HelsinkiDateTime.now().minusMinutes(1)
        )
        givenPairing(pairing)
        postPairingChallengeAssertFail(pairing.challengeKey, 404)
    }

    @Test
    fun `postPairingChallenge - pairing in wrong status returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.WAITING_RESPONSE,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1)
        )
        givenPairing(pairing)
        postPairingChallengeAssertFail(pairing.challengeKey, 404)
    }

    @Test
    fun `postPairingResponse - happy case`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.WAITING_RESPONSE,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1)
        )
        givenPairing(pairing)
        postPairingResponseAssertOk(pairing.id, pairing.challengeKey, pairing.responseKey!!)
    }

    @Test
    fun `postPairingResponse - wrong id returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.WAITING_RESPONSE,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1)
        )
        givenPairing(pairing)
        postPairingResponseAssertFail(UUID.randomUUID(), pairing.challengeKey, pairing.responseKey!!, 404)
    }

    @Test
    fun `postPairingResponse - wrong challengeKey returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.WAITING_RESPONSE,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1)
        )
        givenPairing(pairing)
        postPairingResponseAssertFail(pairing.id, "wrong", pairing.responseKey!!, 404)
    }

    @Test
    fun `postPairingResponse - wrong responseKey returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.WAITING_RESPONSE,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1)
        )
        givenPairing(pairing)
        postPairingResponseAssertFail(pairing.id, pairing.challengeKey, "wrong", 404)
    }

    @Test
    fun `postPairingResponse - wrong status returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.READY,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1)
        )
        givenPairing(pairing)
        postPairingResponseAssertFail(pairing.id, pairing.challengeKey, pairing.responseKey!!, 404)
    }

    @Test
    fun `postPairingResponse - attempts exceeded returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.WAITING_RESPONSE,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1)
        )
        givenPairing(pairing, attempts = 101)
        postPairingResponseAssertFail(pairing.id, pairing.challengeKey, pairing.responseKey!!, 404)
    }

    @Test
    fun `postPairingResponse - pairing expired returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.WAITING_RESPONSE,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().minusMinutes(1)
        )
        givenPairing(pairing)
        postPairingResponseAssertFail(pairing.id, pairing.challengeKey, pairing.responseKey!!, 404)
    }

    @Test
    fun `postPairingValidation - happy case`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.READY,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1),
            mobileDeviceId = MobileDeviceId(UUID.randomUUID())
        )
        givenPairing(pairing)
        postPairingValidationAssertOk(pairing.id, pairing.challengeKey, pairing.responseKey!!)
    }

    @Test
    fun `postPairingValidation - wrong id returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.READY,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1),
            mobileDeviceId = MobileDeviceId(UUID.randomUUID())
        )
        givenPairing(pairing)
        postPairingValidationAssertFail(UUID.randomUUID(), pairing.challengeKey, pairing.responseKey!!, 404)
    }

    @Test
    fun `postPairingValidation - wrong challengeKey returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.READY,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1),
            mobileDeviceId = MobileDeviceId(UUID.randomUUID())
        )
        givenPairing(pairing)
        postPairingValidationAssertFail(pairing.id, "wrong", pairing.responseKey!!, 404)
    }

    @Test
    fun `postPairingValidation - wrong responseKey returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.READY,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1),
            mobileDeviceId = MobileDeviceId(UUID.randomUUID())
        )
        givenPairing(pairing)
        postPairingValidationAssertFail(pairing.id, pairing.challengeKey, "wrong", 404)
    }

    @Test
    fun `postPairingValidation - wrong status returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.PAIRED,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1),
            mobileDeviceId = MobileDeviceId(UUID.randomUUID())
        )
        givenPairing(pairing)
        postPairingValidationAssertFail(pairing.id, pairing.challengeKey, pairing.responseKey!!, 404)
    }

    @Test
    fun `postPairingValidation - attempts exceeded returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.READY,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().plusMinutes(1),
            mobileDeviceId = MobileDeviceId(UUID.randomUUID())
        )
        givenPairing(pairing, attempts = 101)
        postPairingValidationAssertFail(pairing.id, pairing.challengeKey, pairing.responseKey!!, 404)
    }

    @Test
    fun `postPairingValidation - pairing expired returns 404`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.READY,
            challengeKey = "foo",
            responseKey = "bar",
            expires = HelsinkiDateTime.now().minusMinutes(1),
            mobileDeviceId = MobileDeviceId(UUID.randomUUID())
        )
        givenPairing(pairing)
        postPairingValidationAssertFail(pairing.id, pairing.challengeKey, pairing.responseKey!!, 404)
    }

    @Test
    fun `generatePairingKey - values are unique and chars are easy to distinguish`() {
        val count = 100
        val values = (1..count).map { generatePairingKey() }.toSet()
        assertEquals(count, values.size)
        assertTrue(values.all { key -> key.length == 10 })

        val concatenated = values.joinToString(separator = "")
        val badChars = listOf('i', 'l', 'I', '1', 'o', 'O', '0', ' ')
        badChars.forEach { c -> assertFalse(concatenated.contains(c, ignoreCase = false)) }
    }

    private fun givenPairing(pairing: Pairing, attempts: Int = 0) {
        // language=sql
        val sql =
            """
            INSERT INTO pairing (id, unit_id, expires, status, challenge_key, response_key, attempts, mobile_device_id) 
            VALUES (:id, :unitId, :expires, :status, :challengeKey, :responseKey, :attempts, :mobileDeviceId);
            """.trimIndent()
        db.transaction { tx ->
            if (pairing.mobileDeviceId != null) {
                tx.createUpdate("INSERT INTO employee (id, first_name, last_name) VALUES (:id, '', '')")
                    .bind("id", pairing.mobileDeviceId)
                    .execute()
                tx.createUpdate("INSERT INTO mobile_device (id, unit_id, name) VALUES (:id, :unitId, 'Laite')")
                    .bind("id", pairing.mobileDeviceId)
                    .bind("unitId", pairing.unitId)
                    .execute()
            }
            tx.createUpdate(sql)
                .bind("id", pairing.id)
                .bind("unitId", pairing.unitId)
                .bind("expires", pairing.expires)
                .bind("status", pairing.status)
                .bind("challengeKey", pairing.challengeKey)
                .bind("responseKey", pairing.responseKey)
                .bind("attempts", attempts)
                .bind("mobileDeviceId", pairing.mobileDeviceId)
                .execute()
        }
    }

    private fun postPairingAssertOk(unitId: DaycareId): Pairing {
        val (_, res, result) = http.post("/pairings")
            .jsonBody(
                objectMapper.writeValueAsString(
                    PairingsController.PostPairingReq(
                        unitId = unitId
                    )
                )
            )
            .asUser(user)
            .responseObject<Pairing>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun postPairingChallengeAssertOk(challengeKey: String): Pairing {
        val (_, res, result) = http.post("/public/pairings/challenge")
            .jsonBody(
                objectMapper.writeValueAsString(
                    PairingsController.PostPairingChallengeReq(
                        challengeKey = challengeKey
                    )
                )
            )
            .responseObject<Pairing>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun postPairingChallengeAssertFail(challengeKey: String, status: Int) {
        val (_, res, _) = http.post("/public/pairings/challenge")
            .jsonBody(
                objectMapper.writeValueAsString(
                    PairingsController.PostPairingChallengeReq(
                        challengeKey = challengeKey
                    )
                )
            )
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun postPairingResponseAssertOk(id: UUID, challengeKey: String, responseKey: String): Pairing {
        val (_, res, result) = http.post("/pairings/$id/response")
            .jsonBody(
                objectMapper.writeValueAsString(
                    PairingsController.PostPairingResponseReq(
                        challengeKey = challengeKey,
                        responseKey = responseKey
                    )
                )
            )
            .asUser(user)
            .responseObject<Pairing>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun postPairingResponseAssertFail(id: UUID, challengeKey: String, responseKey: String, status: Int) {
        val (_, res, _) = http.post("/pairings/$id/response")
            .jsonBody(
                objectMapper.writeValueAsString(
                    PairingsController.PostPairingResponseReq(
                        challengeKey = challengeKey,
                        responseKey = responseKey
                    )
                )
            )
            .asUser(user)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun postPairingValidationAssertOk(id: UUID, challengeKey: String, responseKey: String): MobileDeviceIdentity {
        val (_, res, result) = http.post("/system/pairings/$id/validation")
            .jsonBody(
                objectMapper.writeValueAsString(
                    PairingsController.PostPairingResponseReq(
                        challengeKey = challengeKey,
                        responseKey = responseKey
                    )
                )
            )
            .asUser(AuthenticatedUser.SystemInternalUser)
            .responseObject<MobileDeviceIdentity>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun postPairingValidationAssertFail(id: UUID, challengeKey: String, responseKey: String, status: Int) {
        val (_, res, _) = http.post("/system/pairings/$id/validation")
            .jsonBody(
                objectMapper.writeValueAsString(
                    PairingsController.PostPairingResponseReq(
                        challengeKey = challengeKey,
                        responseKey = responseKey
                    )
                )
            )
            .asUser(AuthenticatedUser.SystemInternalUser)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun getPairingStatusAssertOk(id: UUID, authenticated: Boolean): PairingStatus {
        val (_, res, result) = http.get("/public/pairings/$id/status")
            .let { if (authenticated) it.asUser(user) else it }
            .responseObject<PairingsController.PairingStatusRes>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get().status
    }

    private fun getMobileDevicesAssertOk(): List<MobileDevice> {
        val (_, res, result) = http.get("/mobile-devices?unitId=$testUnitId")
            .asUser(user)
            .responseObject<List<MobileDevice>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getMobileDeviceAssertOk(id: MobileDeviceId): MobileDevice {
        val (_, res, result) = http.get("/system/mobile-devices/$id")
            .asUser(AuthenticatedUser.SystemInternalUser)
            .responseObject<MobileDevice>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getMobileDeviceAssertFail(id: MobileDeviceId, status: Int) {
        val (_, res, _) = http.get("/system/mobile-devices/$id")
            .asUser(AuthenticatedUser.SystemInternalUser)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun putMobileDeviceNameAssertOk(id: MobileDeviceId, name: String) {
        val (_, res, _) = http.put("/mobile-devices/$id/name")
            .jsonBody(
                objectMapper.writeValueAsString(
                    MobileDevicesController.RenameRequest(
                        name = name
                    )
                )
            )
            .asUser(user)
            .response()

        assertEquals(204, res.statusCode)
    }

    private fun deleteMobileDeviceAssertOk(id: MobileDeviceId) {
        val (_, res, _) = http.delete("/mobile-devices/$id")
            .asUser(user)
            .response()
        assertEquals(204, res.statusCode)
    }
}
