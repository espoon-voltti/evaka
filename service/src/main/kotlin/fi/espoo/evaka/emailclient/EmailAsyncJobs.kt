// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.SendApplicationEmail
import fi.espoo.evaka.shared.db.handle
import org.jdbi.v3.core.Jdbi
import org.springframework.stereotype.Component

@Component
class EmailAsyncJobs(
    asyncJobRunner: AsyncJobRunner,
    private val emailClient: IEmailClient,
    private val jdbi: Jdbi
) {

    init {
        asyncJobRunner.sendApplicationEmail = ::runSendApplicationEmail
    }

    private fun runSendApplicationEmail(msg: SendApplicationEmail) {
        val guardian = jdbi.handle { it.getPersonById(msg.guardianId) }
            ?: throw Exception("Didn't find guardian when sending application email (guardianId: ${msg.guardianId})")
        emailClient.sendApplicationEmail(guardian.id, guardian.email, msg.language)
    }
}
