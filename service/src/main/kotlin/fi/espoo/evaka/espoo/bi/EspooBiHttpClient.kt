// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

import fi.espoo.evaka.EspooBiEnv
import fi.espoo.evaka.shared.TimeoutConfig
import fi.espoo.evaka.shared.buildHttpClient
import fi.espoo.evaka.shared.utils.basicAuthInterceptor
import fi.espoo.evaka.shared.utils.executePutRequest
import fi.espoo.evaka.shared.utils.streamRequestBody
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

    fun sendBiCsvFile(fileName: String, stream: EspooBiJob.CsvInputStream) {
        logger.info { "Sending BI CSV file $fileName" }

        val body = streamRequestBody("text/csv".toMediaType(), stream)

        try {
            httpClient.executePutRequest("report", body, mapOf("filename" to fileName))
        } catch (e: Exception) {
            logger.error(e, mapOf("errorMessage" to e.message)) {
                "Failed to send BI CSV file $fileName (${stream.totalBytes} bytes sent before failure)"
            }
            throw e
        }
        logger.info { "Sent BI CSV file $fileName successfully (${stream.totalBytes} bytes)" }
    }
}
