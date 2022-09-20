// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports.patu

import fi.espoo.evaka.reports.REPORT_STATEMENT_TIMEOUT
import fi.espoo.evaka.reports.getRawRows
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class PatuAsyncJobProcessor(
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val patuIntegrationClient: PatuIntegrationClient
) {
    init {
        asyncJobRunner.registerHandler { db, _, msg: AsyncJob.SendPatuReport ->
            runSendPatuReport(db, msg)
        }
    }

    fun runSendPatuReport(dbc: Database.Connection, msg: AsyncJob.SendPatuReport) {
        logger.info("Running patu report job ${msg.dateRange}")
        val rows =
            dbc.read {
                it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                it.getRawRows(msg.dateRange.start, msg.dateRange.end ?: msg.dateRange.start)
            }
        patuIntegrationClient.send(rows)
    }
}
