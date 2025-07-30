// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
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
