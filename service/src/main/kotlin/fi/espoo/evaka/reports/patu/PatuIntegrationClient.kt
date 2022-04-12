// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports.patu

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import fi.espoo.evaka.EspooPatuIntegrationEnv
import fi.espoo.evaka.reports.RawReportRow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

interface PatuIntegrationClient {
    data class Result(
        val succeeded: Boolean
    )

    fun send(patuReport: List<RawReportRow>): Result

    class MockPatuClient(private val jsonMapper: JsonMapper) : PatuIntegrationClient {
        override fun send(patuReport: List<RawReportRow>): Result {
            logger.info("Mock patu client got report rows ${jsonMapper.writeValueAsString(patuReport)}")
            return Result(true)
        }
    }
}

class EspooPatuIntegrationClient(private val env: EspooPatuIntegrationEnv, private val jsonMapper: JsonMapper) : PatuIntegrationClient {
    override fun send(patuReport: List<RawReportRow>): PatuIntegrationClient.Result {
        logger.debug("Sending patu report of ${patuReport.size} rows to integration")
        val payload = jsonMapper.writeValueAsString(patuReport)
        val (_, _, result) = Fuel.post("${env.url}/report")
            .authentication().basic(env.username, env.password.value)
            .jsonBody(payload)
            .responseString()

        return when (result) {
            is Result.Success -> {
                PatuIntegrationClient.Result(true)
            }
            is Result.Failure -> {
                logger.error(result.getException()) {
                    "Sending patu report failed, message: ${String(result.error.errorData)}"
                }
                PatuIntegrationClient.Result(false)
            }
        }
    }
}
