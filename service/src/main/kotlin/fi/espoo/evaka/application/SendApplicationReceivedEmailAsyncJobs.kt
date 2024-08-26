// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class SendApplicationReceivedEmailAsyncJobs(
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val applicationReceivedEmailService: ApplicationReceivedEmailService,
) {

    init {
        asyncJobRunner.registerHandler { db, _, msg: AsyncJob.SendApplicationEmail ->
            runSendApplicationEmail(db, msg)
        }
    }

    private fun runSendApplicationEmail(
        db: Database.Connection,
        msg: AsyncJob.SendApplicationEmail,
    ) {
        applicationReceivedEmailService.sendApplicationEmail(
            db,
            msg.guardianId,
            msg.language,
            msg.type,
            msg.sentWithinPreschoolApplicationPeriod,
        )
    }
}
