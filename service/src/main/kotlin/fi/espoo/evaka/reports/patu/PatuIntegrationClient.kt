// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports.patu

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.reports.RawReportRow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

interface PatuIntegrationClient {
    data class Result(
        val succeeded: Boolean
    )

    fun send(patuReport: List<RawReportRow>): Result

    class MockPatuClient(
        private val jsonMapper: JsonMapper
    ) : PatuIntegrationClient {
        override fun send(patuReport: List<RawReportRow>): Result {
            logger.info(
                "Mock patu client got report rows ${jsonMapper.writeValueAsString(patuReport)}"
            )
            return Result(true)
        }
    }
}
