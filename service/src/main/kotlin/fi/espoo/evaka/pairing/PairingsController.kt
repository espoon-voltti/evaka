// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.upsertMobileDeviceUser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class PairingsController(
    private val acl: AccessControlList,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    /**
     * Unit supervisor calls this endpoint as an authorized desktop user to start a new pairing process.
     *
     * Endpoint returns a Pairing with an id and challengeKey.
     *
     * Pairing status is WAITING_CHALLENGE.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    sealed class PostPairingReq(val id: UUID) {
        data class Unit(val unitId: DaycareId) : PostPairingReq(unitId.raw)
        data class Employee(val employeeId: EmployeeId) : PostPairingReq(employeeId.raw)
    }
    @PostMapping("/pairings")
    fun postPairing(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser.Employee,
        @RequestBody body: PostPairingReq
    ): ResponseEntity<Pairing> {
        Audit.PairingInit.log(targetId = body.id)
        @Suppress("DEPRECATION")
        @Suppress("DEPRECATION")
        when (body) {
            is PostPairingReq.Unit ->
                acl.getRolesForUnit(user, body.unitId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)
            is PostPairingReq.Employee -> {
                user.requireOneOfRoles(UserRole.UNIT_SUPERVISOR)
                if (EmployeeId(user.id) != body.employeeId) throw Forbidden()
            }
        }

        return db.transaction { tx ->
            val pairing = when (body) {
                is PostPairingReq.Unit -> tx.initPairing(unitId = body.unitId)
                is PostPairingReq.Employee -> tx.initPairing(employeeId = body.employeeId)
            }

            asyncJobRunner.plan(
                tx = tx,
                payloads = listOf(AsyncJob.GarbageCollectPairing(pairingId = pairing.id)),
                runAt = pairing.expires.plusDays(1)
            )

            pairing
        }.let { ResponseEntity.ok(it) }
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
        db: Database.DeprecatedConnection,
        @RequestBody body: PostPairingChallengeReq
    ): ResponseEntity<Pairing> {
        Audit.PairingChallenge.log(targetId = body.challengeKey)
        return db
            .transaction { it.challengePairing(body.challengeKey) }
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
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @PathVariable id: PairingId,
        @RequestBody body: PostPairingResponseReq
    ): ResponseEntity<Pairing> {
        Audit.PairingResponse.log(targetId = id)
        val (unitId, employeeId) = db.read { it.fetchPairingReferenceIds(id) }
        try {
            @Suppress("DEPRECATION")
            when {
                unitId != null -> acl.getRolesForPairing(user, id)
                    .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)
                employeeId != null ->
                    if (EmployeeId(user.id) != employeeId) throw Forbidden()
                else -> error("Pairing unitId and employeeId were null")
            }
        } catch (e: Forbidden) {
            throw NotFound("Pairing not found or not authorized")
        }
        db.transaction { it.incrementAttempts(id, body.challengeKey) }

        return db
            .transaction {
                it.respondPairingChallengeCreateDevice(id, body.challengeKey, body.responseKey)
            }
            .let { ResponseEntity.ok(it) }
    }

    /**
     * API Gateway calls this endpoint to authorize a session creation for a mobile device.
     *
     * Endpoint takes in the previously received id, challengeKey and responseKey.
     *
     * Pairing status changes from READY to PAIRED.
     */
    data class PostPairingValidationReq(
        val challengeKey: String,
        val responseKey: String
    )
    @PostMapping("/system/pairings/{id}/validation")
    fun postPairingValidation(
        db: Database.DeprecatedConnection,
        @PathVariable id: PairingId,
        @RequestBody body: PostPairingValidationReq
    ): MobileDeviceIdentity {
        Audit.PairingValidation.log(targetId = id)
        db.transaction { it.incrementAttempts(id, body.challengeKey) }

        return db.transaction { tx ->
            tx.validatePairing(id, body.challengeKey, body.responseKey).also {
                tx.upsertMobileDeviceUser(it.id)
            }
        }
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
        db: Database.DeprecatedConnection,
        @PathVariable id: PairingId
    ): ResponseEntity<PairingStatusRes> {
        Audit.PairingStatusRead.log(targetId = id)
        return db
            .read { it.fetchPairingStatus(id) }
            .let { ResponseEntity.ok(PairingStatusRes(status = it)) }
    }
}
