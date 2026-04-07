// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu

import fi.espoo.evaka.oulu.dw.DwExportJob
import fi.espoo.evaka.oulu.dw.DwQuery
import fi.espoo.evaka.oulu.dw.FabricHistoryQuery
import fi.espoo.evaka.oulu.dw.FabricQuery
import fi.espoo.evaka.shared.async.AsyncJobPayload
import fi.espoo.evaka.shared.async.AsyncJobPool
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser

sealed interface OuluAsyncJob : AsyncJobPayload {
    data class SendDWQuery(val query: DwQuery) : OuluAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendFabricQuery(val query: FabricQuery) : OuluAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendFabricHistoryQuery(val query: FabricHistoryQuery) : OuluAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    companion object {
        val pool =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(OuluAsyncJob::class, "oulu"),
                AsyncJobPool.Config(concurrency = 1),
                setOf(SendDWQuery::class, SendFabricQuery::class, SendFabricHistoryQuery::class),
            )
    }
}

class OuluAsyncJobRegistration(runner: AsyncJobRunner<OuluAsyncJob>, dwExportJob: DwExportJob) {
    init {
        dwExportJob.let { runner.registerHandler(it::sendDwQuery) }
        dwExportJob.let { runner.registerHandler(it::sendFabricQuery) }
        dwExportJob.let { runner.registerHandler(it::sendFabricHistoryQuery) }
    }
}
