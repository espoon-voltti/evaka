// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports.patu

import evaka.core.reports.RawReportRow
import evaka.core.shared.ConfiguredHttpClient
import evaka.core.shared.buildHttpClient
import evaka.core.shared.utils.basicAuthInterceptor
import evaka.core.shared.utils.headerInterceptor
import evaka.core.shared.utils.post
import evaka.instance.espoo.EspooPatuIntegrationEnv
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
            httpClient.post<Unit>("report", jsonBody = patuReport)
        } catch (e: Exception) {
            logger.error(e) { "Sending patu report failed" }
            throw e
        }
    }
}
