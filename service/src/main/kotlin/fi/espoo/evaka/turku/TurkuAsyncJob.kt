// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku

import fi.espoo.evaka.shared.async.AsyncJobPayload
import fi.espoo.evaka.shared.async.AsyncJobPool
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.turku.dw.DwExportJob
import fi.espoo.evaka.turku.dw.DwQuery

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
