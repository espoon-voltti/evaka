// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.Duration
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class KoskiUpdateService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val env: EvakaEnv,
) {
    fun scheduleKoskiUploads(db: Database.Connection, clock: EvakaClock) {
        if (env.koskiEnabled) {
            db.transaction { tx ->
                tx.setStatementTimeout(Duration.ofMinutes(2))
                val requests = tx.getPendingStudyRights(clock.today())
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
