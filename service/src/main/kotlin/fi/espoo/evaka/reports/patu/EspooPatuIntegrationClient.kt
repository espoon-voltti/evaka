// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports.patu
import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import fi.espoo.evaka.EspooPatuIntegrationEnv
import fi.espoo.evaka.reports.RawReportRow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class EspooPatuIntegrationClient(private val env: EspooPatuIntegrationEnv, private val jsonMapper: JsonMapper) : PatuIntegrationClient {
    override fun send(patuReport: List<RawReportRow>): PatuIntegrationClient.Result {
        logger.info("Sending patu report of ${patuReport.size} rows")
        val payload = jsonMapper.writeValueAsString(patuReport)
        val (req, res, result) = Fuel.post("${env.url}/report")
            .authentication().basic(env.username, env.password.value)
            .header(Headers.ACCEPT, "application/json")
            .jsonBody(payload)
            .responseString()

        logger.info("Patu: REQ: $req RES: $res")

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
