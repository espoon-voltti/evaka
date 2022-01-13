// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.upsertMobileDeviceUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PairingsController(
    private val accessControl: AccessControl,
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
    sealed class PostPairingReq(val id: Id<*>) {
        data class Unit(val unitId: DaycareId) : PostPairingReq(unitId)
        data class Employee(val employeeId: EmployeeId) : PostPairingReq(employeeId)
    }
    @PostMapping("/pairings")
    fun postPairing(
        db: Database,
        user: AuthenticatedUser.Employee,
        @RequestBody body: PostPairingReq
    ): Pairing {
        Audit.PairingInit.log(targetId = body.id)
        @Suppress("DEPRECATION")
        when (body) {
            is PostPairingReq.Unit ->
                accessControl.requirePermissionFor(user, Action.Unit.CREATE_MOBILE_DEVICE_PAIRING, body.unitId)
            is PostPairingReq.Employee -> {
                accessControl.requirePermissionFor(user, Action.Global.CREATE_PERSONAL_MOBILE_DEVICE_PAIRING)
                if (EmployeeId(user.id) != body.employeeId) throw Forbidden()
            }
        }

        return db.connect { dbc ->
            dbc.transaction { tx ->
                when (body) {
                    is PostPairingReq.Unit -> tx.initPairing(unitId = body.unitId)
                    is PostPairingReq.Employee -> tx.initPairing(employeeId = body.employeeId)
                }.also {
                    asyncJobRunner.plan(
                        tx = tx,
                        payloads = listOf(AsyncJob.GarbageCollectPairing(pairingId = it.id)),
                        runAt = it.expires.plusDays(1)
                    )
                }
            }
        }
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
        db: Database,
        @RequestBody body: PostPairingChallengeReq
    ): Pairing {
        Audit.PairingChallenge.log(targetId = body.challengeKey)
        return db.connect { dbc -> dbc.transaction { it.challengePairing(body.challengeKey) } }
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
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: PairingId,
        @RequestBody body: PostPairingResponseReq
    ): Pairing {
        Audit.PairingResponse.log(targetId = id)
        return db.connect { dbc ->
            val (unitId, employeeId) = dbc.read { it.fetchPairingReferenceIds(id) }
            try {
                when {
                    unitId != null -> accessControl.requirePermissionFor(user, Action.Pairing.POST_RESPONSE, id)
                    employeeId != null ->
                        if (EmployeeId(user.id) != employeeId) throw Forbidden()
                    else -> error("Pairing unitId and employeeId were null")
                }
            } catch (e: Forbidden) {
                throw NotFound("Pairing not found or not authorized")
            }
            dbc.transaction { it.incrementAttempts(id, body.challengeKey) }

            dbc.transaction {
                it.respondPairingChallengeCreateDevice(id, body.challengeKey, body.responseKey)
            }
        }
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
        db: Database,
        @PathVariable id: PairingId,
        @RequestBody body: PostPairingValidationReq
    ): MobileDeviceIdentity {
        Audit.PairingValidation.log(targetId = id)
        return db.connect { dbc ->
            dbc.transaction { it.incrementAttempts(id, body.challengeKey) }
            dbc.transaction { tx ->
                tx.validatePairing(id, body.challengeKey, body.responseKey).also {
                    tx.upsertMobileDeviceUser(it.id)
                }
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
        db: Database,
        @PathVariable id: PairingId
    ): PairingStatusRes {
        Audit.PairingStatusRead.log(targetId = id)
        return PairingStatusRes(db.connect { dbc -> dbc.read { it.fetchPairingStatus(id) } })
    }
}
