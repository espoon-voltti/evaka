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

sealed interface OuluAsyncJob : AsyncJobPayload {
    data class SendDWQuery(val query: DwQuery) : OuluAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    companion object {
        val pool =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(OuluAsyncJob::class, "oulu"),
                AsyncJobPool.Config(concurrency = 1),
                setOf(SendDWQuery::class),
            )
    }
}

class OuluAsyncJobRegistration(runner: AsyncJobRunner<OuluAsyncJob>, dwExportJob: DwExportJob) {
    init {
        runner.registerHandler(dwExportJob::sendDwQuery)
    }
}
