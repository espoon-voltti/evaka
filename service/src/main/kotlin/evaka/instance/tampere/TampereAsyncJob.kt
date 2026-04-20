// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere

import evaka.core.bi.BiExportJob
import evaka.core.bi.BiTable
import evaka.core.shared.async.AsyncJobPayload
import evaka.core.shared.async.AsyncJobPool
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser

sealed interface TampereAsyncJob : AsyncJobPayload {
    data class SendBiTable(val table: BiTable) : TampereAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    companion object {
        val pool =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(TampereAsyncJob::class, "tampere"),
                AsyncJobPool.Config(concurrency = 1),
                setOf(SendBiTable::class),
            )
    }
}

class TampereAsyncJobRegistration(
    runner: AsyncJobRunner<TampereAsyncJob>,
    biExportJob: BiExportJob,
) {
    init {
        runner.registerHandler { db, clock, msg: TampereAsyncJob.SendBiTable ->
            biExportJob.sendBiTable(db, clock, msg.table.fileName, msg.table.query)
        }
    }
}
