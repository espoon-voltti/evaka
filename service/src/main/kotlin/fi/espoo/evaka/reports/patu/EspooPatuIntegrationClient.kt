// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports.patu

import fi.espoo.evaka.EspooPatuIntegrationEnv
import fi.espoo.evaka.reports.RawReportRow
import fi.espoo.evaka.shared.ConfiguredHttpClient
import fi.espoo.evaka.shared.buildHttpClient
import fi.espoo.evaka.shared.utils.basicAuthInterceptor
import fi.espoo.evaka.shared.utils.executePostJsonRequest
import fi.espoo.evaka.shared.utils.headerInterceptor
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import tools.jackson.databind.json.JsonMapper

private val logger = KotlinLogging.logger {}

class EspooPatuIntegrationClient(env: EspooPatuIntegrationEnv, jsonMapper: JsonMapper) {
    private val httpClient: ConfiguredHttpClient =
        buildHttpClient(
            rootUrl = URI(env.url),
            jsonMapper = jsonMapper,
            interceptors =
                listOf(
                    basicAuthInterceptor(env.username, env.password.value),
                    headerInterceptor("Accept", "application/json"),
                ),
        )

    fun send(patuReport: List<RawReportRow>) {
        logger.info { "Sending patu report of ${patuReport.size} rows" }
        try {
            httpClient.executePostJsonRequest("report", patuReport)
        } catch (e: Exception) {
            logger.error(e) { "Sending patu report failed" }
            throw e
        }
    }
}
