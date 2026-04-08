// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports.patu

import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class PatuAsyncJobProcessor(
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val patuReportingService: PatuReportingService,
) {
    init {
        asyncJobRunner.registerHandler { db, _, msg: AsyncJob.SendPatuReport ->
            runSendPatuReport(db, msg)
        }
    }

    private fun runSendPatuReport(dbc: Database.Connection, msg: AsyncJob.SendPatuReport) {
        logger.info { "Running patu report job ${msg.dateRange}" }
        patuReportingService.sendPatuReport(dbc, msg.dateRange)
    }
}
