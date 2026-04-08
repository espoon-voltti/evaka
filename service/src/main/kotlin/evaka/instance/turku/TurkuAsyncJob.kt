// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku

import evaka.core.shared.async.AsyncJobPayload
import evaka.core.shared.async.AsyncJobPool
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.instance.turku.dw.DwExportJob
import evaka.instance.turku.dw.DwQuery

sealed interface TurkuAsyncJob : AsyncJobPayload {
    data class SendDWQuery(val query: DwQuery) : TurkuAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    companion object {
        val pool =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(TurkuAsyncJob::class, "turku"),
                AsyncJobPool.Config(concurrency = 1),
                setOf(SendDWQuery::class),
            )
    }
}

class TurkuAsyncJobRegistration(runner: AsyncJobRunner<TurkuAsyncJob>, dwExportJob: DwExportJob) {
    init {
        dwExportJob.let { runner.registerHandler(it::sendDwQuery) }
    }
}
