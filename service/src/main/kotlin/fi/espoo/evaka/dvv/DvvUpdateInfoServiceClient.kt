package fi.espoo.evaka.dvv

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.result.Result

import mu.KotlinLogging
import org.springframework.core.env.Environment
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

/*
    Integration to DVV Update Info Service (muutostietopalvelu)
    See https://hiekkalaatikko.muutostietopalvelu.cloud.dvv.fi/
 */
class DvvUpdateInfoServiceClient(
        private val objectMapper: ObjectMapper,
        private val env: Environment
) {
    // TODO set these somewhere
    private val serviceUrl = "${env.getRequiredProperty("fi.espoo.dvv-update-info-service.url")}/api/v1"
    private val dvvUserId = env.getRequiredProperty("fi.espoo.dvv-update-info-service.userId")
    private val dvvPassword = env.getRequiredProperty("fi.espoo.dvv-update-info-service.userId")

    // Fetch the first update token of the given date
    fun getFirstUpdateToken(date: LocalDate): DvvUpdateInfoServiceUpdateTokenResponse? {
        logger.info { "Fetching the first update token of ${date} from DVV update info service" }
        val (_, _, result) = Fuel.get("${serviceUrl}/kirjausavain/${date}")
                .header(Headers.ACCEPT, "application/json")
                .header("MUTP-Tunnus", dvvUserId)
                .header("MUTP-Salasana", dvvPassword)
                .responseString()

        return when (result) {
            is Result.Success -> {
                logger.info { "Fetching the first update token of ${date} from DVV update info service succeeded" }
                objectMapper.readValue<DvvUpdateInfoServiceUpdateTokenResponse>(
                        objectMapper.readTree(result.get()).toString()
                )
            }
            is Result.Failure -> {
                logger.error(result.getException()) {
                    "Fetching the first update token of ${date} from DVV update info service failed with body: ${objectMapper.writeValueAsString(date)}." +
                            " message: ${String(result.error.errorData)}"
                }
                null
            }
        }
    }
}

data class DvvUpdateInfoServiceUpdateTokenResponse(
        @JsonProperty("viimeisinKirjausavain")
        var latestUpdateToken: Long
)