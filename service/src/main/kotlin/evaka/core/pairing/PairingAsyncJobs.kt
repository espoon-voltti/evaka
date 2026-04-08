// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pairing

import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import org.springframework.stereotype.Component

@Component
class PairingAsyncJobs(asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    init {
        asyncJobRunner.registerHandler { db, _, msg: AsyncJob.GarbageCollectPairing ->
            runGarbageCollectPairing(db, msg)
        }
    }

    private fun runGarbageCollectPairing(
        db: Database.Connection,
        msg: AsyncJob.GarbageCollectPairing,
    ) {
        db.transaction {
            it.createUpdate { sql("DELETE FROM pairing WHERE id = ${bind(msg.pairingId)}") }
                .execute()
        }
    }
}
