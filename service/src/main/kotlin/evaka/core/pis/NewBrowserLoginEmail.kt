// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.EmailEnv
import evaka.core.daycare.domain.Language
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import org.springframework.stereotype.Service

@Service
class NewBrowserLoginEmail(
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    init {
        asyncJobRunner.registerHandler(::sendConfirmationCodeEmail)
    }

    fun sendConfirmationCodeEmail(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendNewBrowserLoginEmail,
    ) {
        Email.create(
                dbc,
                job.personId,
                EmailMessageType.TRANSACTIONAL,
                fromAddress = emailEnv.sender(Language.fi),
                content = emailMessageProvider.newBrowserLoginNotification(),
                traceId = "${clock.today()}:${job.personId}",
            )
            ?.also { emailClient.send(it) }
    }
}
