// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
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
        msg: AsyncJob.GarbageCollectPairing
    ) {
        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate("DELETE FROM pairing WHERE id = :id")
                .bind("id", msg.pairingId)
                .execute()
        }
    }
}
