// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import mu.KotlinLogging
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

/*
    Integration to DVV modifications service (muutostietopalvelu)
    See https://hiekkalaatikko.muutostietopalvelu.cloud.dvv.fi/
 */
@Service
class DvvModificationsServiceClient(
    private val objectMapper: ObjectMapper,
    private val fuel: FuelManager,
    private val env: Environment,
    private val serviceUrl: String = "${env.getRequiredProperty("fi.espoo.integration.dvv-modifications-service.url")}"
) {
    private val dvvUserId = env.getRequiredProperty("fi.espoo.integration.dvv-modifications-service.userId")
    private val dvvPassword = env.getRequiredProperty("fi.espoo.integration.dvv-modifications-service.password")
    private val dvvXroadClientId: String = env.getRequiredProperty("fi.espoo.integration.dvv-modifications-service.xRoadClientId")
    private val dvvProductCode: String = env.getRequiredProperty("fi.espoo.integration.dvv-modifications-service.productCode")

    // Fetch the first modification token of the given date
    fun getFirstModificationToken(date: LocalDate): DvvModificationServiceModificationTokenResponse? {
        logger.info { "Fetching the first modification token of $date from DVV modification service from $serviceUrl/api/v1/kirjausavain/$date" }
        val (_, _, result) = fuel.get("$serviceUrl/api/v1/kirjausavain/$date")
            .header(Headers.ACCEPT, "application/json")
            .header("MUTP-Tunnus", dvvUserId)
            .header("MUTP-Salasana", dvvPassword)
            .header("X-Road-Client", dvvXroadClientId)
            .responseString()

        return when (result) {
            is Result.Success -> {
                logger.info { "Fetching the first modification token of $date from DVV modification service succeeded" }
                objectMapper.readValue<DvvModificationServiceModificationTokenResponse>(
                    objectMapper.readTree(result.get()).toString()
                )
            }
            is Result.Failure -> {
                logger.error(result.getException()) {
                    "Fetching the first modification of $date from DVV modification service failed, message: ${String(result.error.errorData)}"
                }
                null
            }
        }
    }

    fun getModifications(updateToken: String, ssns: List<String>): DvvModificationsResponse? {
        logger.info { "Fetching modifications with token $updateToken from DVV modifications service from $serviceUrl/api/v1/muutokset" }
        val (_, _, result) = fuel.post("$serviceUrl/api/v1/muutokset")
            .header(Headers.ACCEPT, "application/json")
            .header("MUTP-Tunnus", dvvUserId)
            .header("MUTP-Salasana", dvvPassword)
            .header("X-Road-Client", dvvXroadClientId)
            .jsonBody(
                """{
                    "viimeisinKirjausavain": $updateToken,
                    "tuotekoodi": "$dvvProductCode",
                    "hetulista": [${ssns.map { "\"$it\"" }.joinToString()}]
                }
                """.trimIndent()
            )
            .responseString()

        return when (result) {
            is Result.Success -> {
                logger.info { "Fetching modifications with token $updateToken from DVV modifications service succeeded" }
                objectMapper.readValue<DvvModificationsResponse>(
                    objectMapper.readTree(result.get()).toString()
                )
            }
            is Result.Failure -> {
                logger.error(result.getException()) {
                    "Fetching modifications with token $updateToken from DVV modifications service failed, message: ${String(result.error.errorData)}"
                }
                null
            }
        }
    }
}

data class DvvModificationServiceModificationTokenResponse(
    @JsonProperty("viimeisinKirjausavain")
    var latestModificationToken: Long
)
