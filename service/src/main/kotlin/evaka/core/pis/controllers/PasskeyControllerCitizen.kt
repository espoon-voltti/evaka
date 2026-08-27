// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.controllers

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.pis.getCitizenUserDetails
import evaka.core.pis.splitFirstNames
import evaka.core.shared.CitizenPasskeyId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import evaka.core.shared.utils.assertNotNull
import evaka.core.user.CitizenPasskey
import evaka.core.user.NewPasskey
import evaka.core.user.PasskeyService
import evaka.core.user.UserAgentParser
import evaka.core.user.consumePasskeyRegistration
import evaka.core.user.countCitizenPasskeys
import evaka.core.user.deleteCitizenPasskey
import evaka.core.user.getCitizenPasskeyCredentialIds
import evaka.core.user.getCitizenPasskeys
import evaka.core.user.insertCitizenPasskey
import evaka.core.user.updateCitizenPasskeyName
import evaka.core.user.upsertCitizenUserForPasskey
import evaka.core.user.upsertPasskeyRegistration
import java.time.Duration
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

const val MAX_PASSKEYS_PER_CITIZEN = 10

private val REGISTRATION_CHALLENGE_TTL: Duration = Duration.ofMinutes(5)

@RestController
@RequestMapping("/citizen/passkeys")
class PasskeyControllerCitizen(
    private val accessControl: AccessControl,
    private val passkeyService: PasskeyService,
    private val userAgentParser: UserAgentParser,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    @GetMapping
    fun getPasskeys(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<CitizenPasskey> =
        db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_PASSKEYS,
                        user.id,
                    )
                    tx.getCitizenPasskeys(user.id)
                }
            }
            .also { Audit.CitizenPasskeysRead.log(targetId = AuditId(user.id)) }

    data class PasskeyRegistrationOptions(
        /** Argument for the browser's navigator.credentials.create() */
        val credentialsCreate: String
    )

    @PostMapping("/register")
    fun startPasskeyRegistration(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): PasskeyRegistrationOptions {
        Audit.CitizenPasskeyRegisterAttempt.log(targetId = AuditId(user.id))
        return db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.ADD_PASSKEY,
                    user.id,
                )
                if (tx.countCitizenPasskeys(user.id) >= MAX_PASSKEYS_PER_CITIZEN) {
                    throw Conflict("Too many passkeys", "PASSKEY_LIMIT")
                }
                val details = tx.getCitizenUserDetails(user.id) ?: throw NotFound()
                val firstName =
                    details.preferredName.ifBlank {
                        splitFirstNames(details.firstName).firstOrNull() ?: ""
                    }
                val started =
                    passkeyService.startRegistration(
                        person = user.id,
                        accountName = "$firstName ${details.lastName}",
                        existingCredentialIds = tx.getCitizenPasskeyCredentialIds(user.id),
                    )
                tx.upsertPasskeyRegistration(
                    user.id,
                    started.options,
                    clock.now().plus(REGISTRATION_CHALLENGE_TTL),
                )
                PasskeyRegistrationOptions(started.credentialsCreate)
            }
        }
    }

    data class FinishPasskeyRegistrationRequest(
        val name: String,
        /** JSON serialization of the PublicKeyCredential returned by the browser */
        val credential: String,
    ) {
        init {
            if (name.isBlank() || name.length > 100) throw BadRequest("Invalid passkey name")
        }
    }

    @PostMapping("/register/finish")
    fun finishPasskeyRegistration(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestHeader(HttpHeaders.USER_AGENT, required = false) userAgent: String?,
        @RequestBody body: FinishPasskeyRegistrationRequest,
    ): CitizenPasskey =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.ADD_PASSKEY,
                        user.id,
                    )
                    val optionsJson =
                        tx.consumePasskeyRegistration(user.id, clock.now())
                            ?: throw NotFound("No pending passkey registration")
                    if (tx.countCitizenPasskeys(user.id) >= MAX_PASSKEYS_PER_CITIZEN) {
                        throw Conflict("Too many passkeys", "PASSKEY_LIMIT")
                    }
                    val finished =
                        passkeyService.finishRegistration(
                            tx,
                            person = user.id,
                            optionsJson = optionsJson,
                            credentialJson = body.credential,
                        )
                    tx.upsertCitizenUserForPasskey(user.id)
                    val passkey =
                        tx.insertCitizenPasskey(
                            user.id,
                            NewPasskey(
                                credentialId = finished.credentialId,
                                publicKey = finished.publicKey,
                                signatureCounter = finished.signatureCounter,
                                aaguid = finished.aaguid,
                                transports = finished.transports,
                                name = body.name.trim(),
                                client = userAgentParser.parse(userAgent),
                            ),
                        )
                    asyncJobRunner.plan(
                        tx,
                        sequenceOf(AsyncJob.SendPasskeyAddedEmail(user.id)),
                        runAt = clock.now(),
                    )
                    passkey
                }
            }
            .also {
                Audit.CitizenPasskeyRegister.log(
                    targetId = AuditId(user.id),
                    objectId = AuditId(it.id),
                )
            }

    data class UpdatePasskeyNameRequest(val name: String) {
        init {
            if (name.isBlank() || name.length > 100) throw BadRequest("Invalid passkey name")
        }
    }

    @PutMapping("/{id}/name")
    fun updatePasskeyName(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: CitizenPasskeyId,
        @RequestBody body: UpdatePasskeyNameRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.UPDATE_PASSKEY_NAME,
                    user.id,
                )
                tx.updateCitizenPasskeyName(user.id, id, body.name.trim())
                    ?: throw NotFound("Passkey not found")
            }
        }
        Audit.CitizenPasskeyUpdate.log(targetId = AuditId(user.id), objectId = AuditId(id))
    }

    @DeleteMapping("/{id}")
    fun deletePasskey(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: CitizenPasskeyId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.DELETE_PASSKEY,
                    user.id,
                )
                tx.deleteCitizenPasskey(user.id, id).assertNotNull(msg = "Passkey not found")
                asyncJobRunner.plan(
                    tx,
                    sequenceOf(AsyncJob.SendPasskeyRemovedEmail(user.id)),
                    runAt = clock.now(),
                )
            }
        }
        Audit.CitizenPasskeyDelete.log(targetId = AuditId(user.id), objectId = AuditId(id))
    }
}
