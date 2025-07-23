// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.shared.HtmlSafe
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class PersonEmailJobs(
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    init {
        asyncJobRunner.registerHandler(::sendConfirmationCodeEmail)
        asyncJobRunner.registerHandler(::sendPasswordChangedEmail)
    }

    private val logger = KotlinLogging.logger {}

    fun sendConfirmationCodeEmail(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendConfirmationCodeEmail,
    ) {
        val (email, confirmationCode) =
            dbc.read {
                it.createQuery {
                        sql(
                            """
SELECT email, verification_code
FROM person_email_verification
WHERE id = ${bind(job.id)}
AND sent_at IS NULL
AND expires_at > ${bind(clock.now())}
"""
                        )
                    }
                    .map { columnPair<String, String>("email", "verification_code") }
                    .exactlyOneOrNull()
            } ?: return
        Email.createForAddress(
                toAddress = email,
                fromAddress = emailEnv.sender(Language.fi),
                emailMessageProvider.confirmationCode(HtmlSafe(confirmationCode)),
                "${clock.today()}:${job.id}",
            )
            ?.also { emailClient.send(it) }
        // try to set sent_at safely so we don't retry the entire job and send the e-mail again on
        // failure
        try {
            dbc.transaction { it.markEmailVerificationSent(job.id, clock.now()) }
        } catch (e: Exception) {
            logger.error(e) { "Error marking email verification sent" }
        }
    }

    fun sendPasswordChangedEmail(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendPasswordChangedEmail,
    ) {
        val (email, verifiedEmail) = dbc.read { it.getPersonEmails(job.personId) }
        val toAddress = verifiedEmail ?: email
        if (toAddress == null) {
            logger.warn {
                "No email found for person ${job.personId} to send password change notification"
            }
            return
        }
        Email.createForAddress(
                toAddress = toAddress,
                fromAddress = emailEnv.sender(Language.fi),
                emailMessageProvider.passwordChanged(),
                "${clock.today()}:${job.personId}",
            )
            ?.also { emailClient.send(it) }
    }
}
