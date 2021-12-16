// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.SuomiFiAsyncJob
import org.springframework.stereotype.Service

@Service
class SfiAsyncJobs(
    private val sfiClient: SfiMessagesClient,
    asyncJobRunner: AsyncJobRunner<SuomiFiAsyncJob>
) {
    init {
        asyncJobRunner.registerHandler { _, payload: SuomiFiAsyncJob.SendMessage ->
            sendMessagePDF(payload.message)
        }
    }

    fun sendMessagePDF(msg: SfiMessage) {
        sfiClient.send(msg)
    }
}
