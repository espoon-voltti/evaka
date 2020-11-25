package fi.espoo.evaka.pairing

import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class PairingsController(
    private val acl: AccessControlList
) {
    /**
     * Unit supervisor calls this endpoint as an authorized desktop user to start a new pairing process.
     *
     * Endpoint returns a Pairing with an id and challengeKey.
     *
     * Pairing status is WAITING_CHALLENGE.
     */
    data class PostPairingReq(
        val unitId: UUID
    )
    @PostMapping("/pairings")
    fun postPairing(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: PostPairingReq
    ): ResponseEntity<Pairing> {
        acl.getRolesForUnit(user, body.unitId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        return db
            .transaction { tx -> initPairing(tx, body.unitId) }
            .let { ResponseEntity.ok(it) }
    }

    /**
     * Unit supervisor calls this endpoint as an unauthenticated mobile user to start a pairing challenge phase.
     *
     * Endpoint takes in the challengeKey previously shown on desktop and returns a Pairing with an id,
     * challengeKey and responseKey.
     *
     * Pairing status changes from WAITING_CHALLENGE to WAITING_RESPONSE.
     */
    data class PostPairingChallengeReq(
        val challengeKey: String
    )
    @PostMapping("/public/pairings/challenge")
    fun postPairingChallenge(
        db: Database.Connection,
        @RequestBody body: PostPairingChallengeReq
    ): ResponseEntity<Pairing> {
        return db
            .transaction { tx -> challengePairing(tx, body.challengeKey) }
            .let { ResponseEntity.ok(it) }
    }

    /**
     * Unit supervisor calls this endpoint as an authorized desktop user to respond to challenge.
     *
     * Endpoint takes in the previously received id and challengeKey, as well as the responseKey shown on mobile.
     *
     * Pairing status changes from WAITING_RESPONSE to READY.
     */
    data class PostPairingResponseReq(
        val challengeKey: String,
        val responseKey: String
    )
    @PostMapping("/pairings/{id}/response")
    fun postPairingResponse(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: PostPairingResponseReq
    ): ResponseEntity<Pairing> {
        try {
            acl.getRolesForPairing(user, id).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)
        } catch (e: Forbidden) {
            throw NotFound("Pairing not found or not authorized")
        }
        db.transaction { tx -> incrementAttempts(tx, id, body.challengeKey) }

        return db
            .transaction { tx -> respondPairingChallenge(tx, id, body.challengeKey, body.responseKey) }
            .let { ResponseEntity.ok(it) }
    }

    /**
     * API Gateway calls this endpoint to authorize a session creation for a mobile device.
     *
     * Endpoint takes in the previously received id, challengeKey and responseKey.
     *
     * Pairing status changes from WAITING_RESPONSE to READY.
     */
    data class PostPairingValidationReq(
        val challengeKey: String,
        val responseKey: String
    )
    @PostMapping("/apigw/pairings/{id}/validation")
    fun postPairingValidation(
        db: Database.Connection,
        @PathVariable id: UUID,
        @RequestBody body: PostPairingValidationReq
    ): ResponseEntity<Unit> {
        db.transaction { tx -> incrementAttempts(tx, id, body.challengeKey) }

        db.transaction { tx ->
            validatePairing(tx, id, body.challengeKey, body.responseKey)

            // todo: insert into mobile_device and employee ?
        }

        return ResponseEntity.noContent().build()
    }

    /**
     * Unit supervisor calls this endpoint in a polling fashion either as an authorized desktop user or
     * as an unauthenticated mobile user in order to monitor the progress of the async pairing process.
     *
     * Endpoint takes in the previously received pairing id and returns the pairing status.
     */
    data class PairingStatusRes(
        val status: PairingStatus
    )
    @GetMapping("/public/pairings/{id}/status")
    fun getPairingStatus(
        db: Database.Connection,
        @PathVariable id: UUID
    ): ResponseEntity<PairingStatusRes> {
        return db
            .transaction { tx -> fetchPairingStatus(tx, id) }
            .let { ResponseEntity.ok(PairingStatusRes(status = it)) }
    }
}
