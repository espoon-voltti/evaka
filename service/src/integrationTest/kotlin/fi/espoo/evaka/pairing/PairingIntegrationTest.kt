// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.updateDaycareAclWithEmployee
import fi.espoo.evaka.shared.utils.zoneId
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class PairingIntegrationTest : FullApplicationTest() {
    private val user = AuthenticatedUser(testDecisionMaker_1.id, emptySet())
    private val apigw = AuthenticatedUser(UUID.randomUUID(), emptySet())
    private val testUnitId = testDaycare.id

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            insertGeneralTestFixtures(tx.handle)
            updateDaycareAclWithEmployee(tx.handle, testUnitId, user.id, UserRole.UNIT_SUPERVISOR)
        }
    }

    @Test
    fun `full pairing flow happy case`() {
        val res1 = postPairingAssertOk(testUnitId)
        val id = res1.id
        val challengeKey = res1.challengeKey
        assertEquals(testUnitId, res1.unitId)
        assertEquals(10, challengeKey.length)
        assertNull(res1.responseKey)
        assertEquals(PairingStatus.WAITING_CHALLENGE, res1.status)

        assertEquals(PairingStatus.WAITING_CHALLENGE, getPairingStatusAssertOk(id, authenticated = true))

        val res2 = postPairingChallengeAssertOk(challengeKey)
        val responseKey = res2.responseKey
        assertEquals(id, res2.id)
        assertEquals(testUnitId, res2.unitId)
        assertEquals(challengeKey, res2.challengeKey)
        assertEquals(10, responseKey!!.length)
        assertEquals(PairingStatus.WAITING_RESPONSE, res2.status)

        assertEquals(PairingStatus.WAITING_RESPONSE, getPairingStatusAssertOk(id, authenticated = false))
        assertEquals(PairingStatus.WAITING_RESPONSE, getPairingStatusAssertOk(id, authenticated = true))

        val res3 = postPairingResponseAssertOk(id, challengeKey, responseKey)
        assertEquals(id, res3.id)
        assertEquals(testUnitId, res3.unitId)
        assertEquals(challengeKey, res3.challengeKey)
        assertEquals(responseKey, res3.responseKey)
        assertEquals(PairingStatus.READY, res3.status)

        assertEquals(PairingStatus.READY, getPairingStatusAssertOk(id, authenticated = false))
        assertEquals(PairingStatus.READY, getPairingStatusAssertOk(id, authenticated = true))

        postPairingValidationAssertOk(id, challengeKey, responseKey)

        assertEquals(PairingStatus.PAIRED, getPairingStatusAssertOk(id, authenticated = false))
        assertEquals(PairingStatus.PAIRED, getPairingStatusAssertOk(id, authenticated = true))
    }

    @Test
    fun `postPairingChallenge - happy case`() {
        val pairing = Pairing(
            id = UUID.randomUUID(),
            unitId = testUnitId,
            status = PairingStatus.WAITING_CHALLENGE,
            challengeKey = "foo",
            responseKey = null,
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).minusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
        )
        givenPairing(pairing)
        (1..11).forEach { _ ->
            postPairingResponseAssertFail(pairing.id, pairing.challengeKey, "wrong", 404)
        }
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
            expires = ZonedDateTime.now(zoneId).minusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
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
            expires = ZonedDateTime.now(zoneId).plusMinutes(1).toInstant()
        )
        givenPairing(pairing)
        (1..11).forEach { _ ->
            postPairingValidationAssertFail(pairing.id, pairing.challengeKey, "wrong", 404)
        }
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
            expires = ZonedDateTime.now(zoneId).minusMinutes(1).toInstant()
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
            INSERT INTO pairing (id, unit_id, expires, status, challenge_key, response_key, attempts) 
            VALUES (:id, :unitId, :expires, :status, :challengeKey, :responseKey, :attempts);
            """.trimIndent()
        db.transaction { tx ->
            tx.createUpdate(sql)
                .bind("id", pairing.id)
                .bind("unitId", pairing.unitId)
                .bind("expires", pairing.expires)
                .bind("status", pairing.status)
                .bind("challengeKey", pairing.challengeKey)
                .bind("responseKey", pairing.responseKey)
                .bind("attempts", attempts)
                .execute()
        }
    }

    private fun postPairingAssertOk(unitId: UUID): Pairing {
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

    private fun postPairingValidationAssertOk(id: UUID, challengeKey: String, responseKey: String) {
        val (_, res, _) = http.post("/apigw/pairings/$id/validation")
            .jsonBody(
                objectMapper.writeValueAsString(
                    PairingsController.PostPairingResponseReq(
                        challengeKey = challengeKey,
                        responseKey = responseKey
                    )
                )
            )
            .asUser(apigw)
            .response()

        assertEquals(204, res.statusCode)
    }

    private fun postPairingValidationAssertFail(id: UUID, challengeKey: String, responseKey: String, status: Int) {
        val (_, res, _) = http.post("/apigw/pairings/$id/validation")
            .jsonBody(
                objectMapper.writeValueAsString(
                    PairingsController.PostPairingResponseReq(
                        challengeKey = challengeKey,
                        responseKey = responseKey
                    )
                )
            )
            .asUser(apigw)
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
}
