// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.bi

import evaka.core.bi.CsvInputStream
import evaka.core.shared.TimeoutConfig
import evaka.core.shared.buildHttpClient
import evaka.core.shared.utils.basicAuthInterceptor
import evaka.core.shared.utils.put
import evaka.core.shared.utils.streamRequestBody
import evaka.instance.espoo.EspooBiEnv
import fi.espoo.voltti.logging.loggers.error
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.time.Duration
import okhttp3.MediaType.Companion.toMediaType

class EspooBiHttpClient(env: EspooBiEnv) {
    private val logger = KotlinLogging.logger {}

    private val httpClient =
        buildHttpClient(
            rootUrl = URI(env.url),
            interceptors = listOf(basicAuthInterceptor(env.username, env.password.value)),
            timeouts = TimeoutConfig(readTimeout = Duration.ofMinutes(5)),
        )

    fun sendBiCsvFile(fileName: String, stream: CsvInputStream) {
        logger.info { "Sending BI CSV file $fileName" }

        val body = streamRequestBody("text/csv".toMediaType(), stream)

        try {
            httpClient.put<Unit>("report", body = body, queryParams = mapOf("filename" to fileName))
        } catch (e: Exception) {
            logger.error(e, mapOf("errorMessage" to e.message)) {
                "Failed to send BI CSV file $fileName (${stream.totalBytes} bytes sent before failure)"
            }
            throw e
        }
        logger.info { "Sent BI CSV file $fileName successfully (${stream.totalBytes} bytes)" }
    }
}
