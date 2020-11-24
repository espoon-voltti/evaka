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
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    private fun getPairingStatusAssertOk(id: UUID, authenticated: Boolean): PairingStatus {
        val (_, res, result) = http.get("/public/pairings/$id/status")
            .let { if (authenticated) it.asUser(user) else it }
            .responseObject<PairingsController.PairingStatusRes>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get().status
    }
}
