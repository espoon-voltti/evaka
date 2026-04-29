// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu

import evaka.core.bi.BiExportJob
import evaka.core.bi.BiTable
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

    data class SendBiTable(val table: BiTable) : OuluAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    companion object {
        val pool =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(OuluAsyncJob::class, "oulu"),
                AsyncJobPool.Config(concurrency = 1),
                setOf(SendDWQuery::class, SendBiTable::class),
            )
    }
}

class OuluAsyncJobRegistration(
    runner: AsyncJobRunner<OuluAsyncJob>,
    dwExportJob: DwExportJob,
    biExportJob: BiExportJob,
) {
    init {
        runner.registerHandler(dwExportJob::sendDwQuery)
        runner.registerHandler { db, clock, msg: OuluAsyncJob.SendBiTable ->
            biExportJob.sendBiTable(db, clock, msg.table)
        }
    }
}
