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
class EmailVerificationJobs(
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    init {
        asyncJobRunner.registerHandler(::sendEmailVerificationEmail)
    }

    private val logger = KotlinLogging.logger {}

    fun sendEmailVerificationEmail(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendEmailVerificationCodeEmail,
    ) {
        val (email, verificationCode) =
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
                emailMessageProvider.emailVerification(HtmlSafe(verificationCode)),
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
}
