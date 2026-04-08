// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.koski

import evaka.core.KoskiEnv
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class KoskiUpdateService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val koskiEnv: KoskiEnv?,
) {
    fun scheduleKoskiUploads(db: Database.Connection, clock: EvakaClock) {
        if (koskiEnv != null) {
            db.transaction { tx ->
                tx.setStatementTimeout(Duration.ofMinutes(2))
                val requests = tx.getPendingStudyRights(clock.today(), koskiEnv.syncRangeStart)
                logger.info { "Scheduling ${requests.size} Koski upload requests" }
                asyncJobRunner.plan(
                    tx,
                    requests.map { AsyncJob.UploadToKoski(it) },
                    retryCount = 1,
                    runAt = clock.now(),
                )
            }
        }
    }
}
