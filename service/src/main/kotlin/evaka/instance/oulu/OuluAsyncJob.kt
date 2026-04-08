// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu

import evaka.core.shared.async.AsyncJobPayload
import evaka.core.shared.async.AsyncJobPool
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.instance.oulu.dw.DwExportJob
import evaka.instance.oulu.dw.DwQuery
import evaka.instance.oulu.dw.FabricHistoryQuery
import evaka.instance.oulu.dw.FabricQuery

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
