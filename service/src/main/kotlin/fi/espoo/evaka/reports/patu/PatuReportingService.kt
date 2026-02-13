// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports.patu

import fi.espoo.evaka.reports.REPORT_STATEMENT_TIMEOUT
import fi.espoo.evaka.reports.getRawRows
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class PatuReportingService(private val patuIntegrationClient: EspooPatuIntegrationClient?) {

    fun sendPatuReport(dbc: Database.Connection, dateRange: DateRange) {
        if (patuIntegrationClient == null) {
            logger.info { "Patu integration client not configured, skipping sending" }
            return
        }
        val rows =
            dbc.read {
                it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                it.getRawRows(dateRange.start, dateRange.end ?: dateRange.start)
            }
        patuIntegrationClient.send(rows)
    }
}
