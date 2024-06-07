// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.requests.DefaultBody
import fi.espoo.evaka.EspooBiEnv
import fi.espoo.voltti.logging.loggers.error
import java.time.Duration
import mu.KotlinLogging

class EspooBiHttpClient(private val env: EspooBiEnv) : EspooBiClient {
    private val fuel = FuelManager()
    private val logger = KotlinLogging.logger {}
    private val readTimeout = Duration.ofMinutes(5)

    override fun sendBiCsvFile(fileName: String, stream: EspooBiJob.CsvInputStream) {
        logger.info("Sending BI CSV file $fileName")
        val (_, _, result) =
            fuel
                .put("${env.url}/report", listOf("filename" to fileName))
                .header("Content-type", "text/csv")
                .timeoutRead(readTimeout.toMillis().toInt())
                .authentication()
                .basic(env.username, env.password.value)
                .body(DefaultBody({ stream }))
                .responseString()
        result.fold(
            { logger.info("Sent BI CSV file $fileName successfully (${stream.totalBytes} bytes)") },
            { error ->
                val meta = mapOf("errorMessage" to error.errorData.decodeToString())
                logger.error(error, meta) {
                    "Failed to send BI CSV file $fileName (${stream.totalBytes} bytes sent before failure)"
                }
                throw error
            }
        )
    }
}
