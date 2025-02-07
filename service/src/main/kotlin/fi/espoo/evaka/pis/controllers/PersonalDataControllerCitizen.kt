//  SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.pis.*
import fi.espoo.evaka.shared.PersonEmailVerificationId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.PasswordConstraints
import fi.espoo.evaka.shared.auth.PasswordService
import fi.espoo.evaka.shared.auth.PasswordSpecification
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.utils.EMAIL_PATTERN
import fi.espoo.evaka.shared.utils.PHONE_PATTERN
import fi.espoo.evaka.user.hasWeakCredentials
import fi.espoo.evaka.user.updateWeakLoginCredentials
import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.SecureRandom
import java.time.Duration
import kotlin.random.asKotlinRandom
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/personal-data")
class PersonalDataControllerCitizen(
    private val accessControl: AccessControl,
    private val passwordService: PasswordService,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val passwordSpecification: PasswordSpecification,
) {
    private val secureRandom = SecureRandom()
    private val logger = KotlinLogging.logger {}

    @PutMapping
    fun updatePersonalData(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: PersonalDataUpdate,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.UPDATE_PERSONAL_DATA,
                    user.id,
                )

                val person = tx.getPersonById(user.id) ?: error("User not found")

                val validationErrors =
                    listOfNotNull(
                        "invalid preferredName"
                            .takeUnless {
                                person.firstName.split(" ").contains(body.preferredName)
                            },
                        "invalid phone".takeUnless { PHONE_PATTERN.matches(body.phone) },
                        "invalid backup phone"
                            .takeUnless {
                                body.backupPhone.isBlank() ||
                                    PHONE_PATTERN.matches(body.backupPhone)
                            },
                        "invalid email"
                            .takeUnless {
                                body.email.isBlank() || EMAIL_PATTERN.matches(body.email)
                            },
                    )

                if (validationErrors.isNotEmpty())
                    throw BadRequest(validationErrors.joinToString(", "))

                tx.updatePersonalDetails(user.id, body)
                if (tx.hasWeakCredentials(user.id)) {
                    sendEmailVerificationCode(tx, clock, user)
                }
            }
        }
        Audit.PersonalDataUpdate.log(targetId = AuditId(user.id))
    }

    @GetMapping("/notification-settings")
    fun getNotificationSettings(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): Set<EmailMessageType> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_NOTIFICATION_SETTINGS,
                        user.id,
                    )
                    tx.getDisabledEmailTypes(user.id)
                }
            }
            .also { Audit.CitizenNotificationSettingsRead.log(targetId = AuditId(user.id)) }
    }

    @PutMapping("/notification-settings")
    fun updateNotificationSettings(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: Set<EmailMessageType>,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.UPDATE_NOTIFICATION_SETTINGS,
                    user.id,
                )
                tx.updateDisabledEmailTypes(user.id, body)
            }
        }
        Audit.PersonalDataUpdate.log(targetId = AuditId(user.id))
    }

    data class UpdateWeakLoginCredentialsRequest(
        val username: String?,
        val password: Sensitive<String>?,
    ) {
        init {
            if (
                password != null && password.value.length !in PasswordConstraints.SUPPORTED_LENGTH
            ) {
                throw BadRequest("Invalid password")
            }
        }
    }

    @PutMapping("/weak-login-credentials")
    fun updateWeakLoginCredentials(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: UpdateWeakLoginCredentialsRequest,
    ) {
        Audit.CitizenCredentialsUpdateAttempt.log(targetId = AuditId(user.id))
        if (body.password != null) {
            if (!passwordSpecification.constraints().isPasswordStructureValid(body.password)) {
                throw BadRequest("Password is not structurally valid", "PASSWORD_FORMAT")
            }
        }
        val password = body.password?.let { passwordService.encode(it) }
        db.connect { dbc ->
            if (body.password != null) {
                if (!passwordSpecification.isPasswordAcceptable(dbc, body.password)) {
                    throw BadRequest("Password is not acceptable", "PASSWORD_UNACCEPTABLE")
                }
            }
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.UPDATE_WEAK_LOGIN_CREDENTIALS,
                    user.id,
                )
                val emails = tx.getPersonEmails(user.id)
                if (body.username != null && emails.verifiedEmail != body.username) {
                    throw BadRequest("Invalid username")
                }
                try {
                    tx.updateWeakLoginCredentials(
                        clock,
                        user.id,
                        body.username?.lowercase(),
                        password,
                    )
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
        Audit.CitizenCredentialsUpdate.log(targetId = AuditId(user.id))
    }

    data class EmailVerificationStatusResponse(
        val email: String?,
        val verifiedEmail: String?,
        val latestVerification: EmailVerification?,
    )

    @GetMapping("/email-verification")
    fun getEmailVerificationStatus(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): EmailVerificationStatusResponse =
        db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_EMAIL_VERIFICATION_STATUS,
                        user.id,
                    )
                    val emails = tx.getPersonEmails(user.id)
                    val verification = tx.getLatestEmailVerification(user.id)
                    EmailVerificationStatusResponse(
                        email = emails.email,
                        verifiedEmail = emails.verifiedEmail,
                        latestVerification = verification?.takeUnless { it.expiresAt < clock.now() },
                    )
                }
            }
            .also { Audit.CitizenEmailVerificationStatusRead.log(targetId = AuditId(user.id)) }

    @PostMapping("/email-verification-code")
    fun sendEmailVerificationCode(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.VERIFY_EMAIL,
                    user.id,
                )
                sendEmailVerificationCode(tx, clock, user)
            }
        }
        Audit.CitizenSendVerificationCode.log(targetId = AuditId(user.id))
    }

    data class EmailVerificationRequest(val id: PersonEmailVerificationId, val code: String)

    @PostMapping("/email-verification")
    fun verifyEmail(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody request: EmailVerificationRequest,
    ) {
        Audit.CitizenVerifyEmailAttempt.log(targetId = AuditId(request.id))
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.VERIFY_EMAIL,
                    user.id,
                )
                tx.verifyAndUpdateEmail(clock.now(), user.id, request.id, request.code)
                try {
                    tx.syncWeakLoginUsername(clock.now(), user.id)
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
        Audit.CitizenVerifyEmail.log(targetId = AuditId(request.id))
    }

    private fun sendEmailVerificationCode(
        tx: Database.Transaction,
        clock: EvakaClock,
        user: AuthenticatedUser.Citizen,
    ) {
        val emails = tx.getPersonEmails(user.id)
        if (emails.email != null && emails.email != emails.verifiedEmail) {
            val verification =
                tx.upsertEmailVerification(
                    clock.now(),
                    user.id,
                    emails.email,
                    NewEmailVerification(
                        verificationCode = generateConfirmationCode(),
                        expiresAt = clock.now().plus(CONFIRMATION_CODE_DURATION),
                    ),
                )
            if (verification.sentAt == null) {
                asyncJobRunner.plan(
                    tx,
                    sequenceOf(AsyncJob.SendConfirmationCodeEmail(id = verification.id)),
                    runAt = clock.now(),
                )
            }
        }
    }

    private fun generateConfirmationCode(): String =
        generateSequence { "0123456789".random(secureRandom.asKotlinRandom()) }
            .take(CONFIRMATION_CODE_LENGTH)
            .joinToString(separator = "")

    @GetMapping("/password-constraints")
    fun getPasswordConstraints(user: AuthenticatedUser.Citizen): PasswordConstraints =
        passwordSpecification.constraints()
}

val CONFIRMATION_CODE_DURATION: Duration = Duration.ofHours(2)
const val CONFIRMATION_CODE_LENGTH = 6
