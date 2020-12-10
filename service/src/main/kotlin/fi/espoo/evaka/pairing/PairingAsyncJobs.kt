// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.GarbageCollectPairing
import fi.espoo.evaka.shared.db.Database
import org.springframework.stereotype.Component

@Component
class PairingAsyncJobs(
    asyncJobRunner: AsyncJobRunner
) {
    init {
        asyncJobRunner.garbageCollectPairing = ::runGarbageCollectPairing
    }

    private fun runGarbageCollectPairing(db: Database, msg: GarbageCollectPairing) {
        db.transaction {
            it.createUpdate("DELETE FROM pairing WHERE id = :id")
                .bind("id", msg.pairingId)
                .execute()
        }
    }
}
