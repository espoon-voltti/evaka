// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.SendApplicationEmail
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger { }

@Component
class SendApplicationReceivedEmailAsyncJobs(
    asyncJobRunner: AsyncJobRunner,
    private val applicationReceivedEmailService: ApplicationReceivedEmailService
) {

    init {
        asyncJobRunner.sendApplicationEmail = ::runSendApplicationEmail
    }

    private fun runSendApplicationEmail(db: Database, msg: SendApplicationEmail) {
        val guardian = db.read { it.handle.getPersonById(msg.guardianId) }
            ?: throw Exception("Didn't find guardian when sending application email (guardianId: ${msg.guardianId})")

        if (!guardian.email.isNullOrBlank()) {
            applicationReceivedEmailService.sendApplicationEmail(guardian.id, guardian.email, msg.language, msg.type, msg.sentWithinPreschoolApplicationPeriod)
        } else {
            logger.warn("Cannot send application received email to guardian ${guardian.id}: missing email")
        }
    }
}
