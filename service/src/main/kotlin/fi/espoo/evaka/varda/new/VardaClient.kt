// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.new

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import fi.espoo.evaka.shared.utils.responseStringWithRetries
import fi.espoo.evaka.shared.utils.token
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import fi.espoo.voltti.logging.loggers.error
import java.net.URI
import java.time.LocalDate
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class VardaClient(
    private val tokenProvider: VardaTokenProvider,
    private val fuel: FuelManager,
    private val jsonMapper: JsonMapper,
    vardaBaseUrl: URI
) {
    private val baseUrl = vardaBaseUrl.ensureTrailingSlash()

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class VardaPersonSearchRequest(
        val henkilotunnus: String? = null,
        val henkilo_oid: String? = null
    ) {
        init {
            check(henkilotunnus != null || henkilo_oid != null) {
                "Both params ssn and oid must not be null"
            }
        }

        override fun toString(): String {
            val henkilotunnusMasked = henkilotunnus?.let { "${it.slice(0..4)}******" }
            return "VardaPersonSearchRequest(henkilotunnus=$henkilotunnusMasked, henkilo_oid=$henkilo_oid)"
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class HenkiloResponse(
        val url: URI,
        val etunimet: String,
        val sukunimi: String,
        val kutsumanimi: String,
        val henkilo_oid: String,
        val syntyma_pvm: String?,
        val lapsi: List<URI>
    )

    fun haeHenkilo(body: VardaPersonSearchRequest): HenkiloResponse? =
        post(baseUrl.resolve("v1/hae-henkilo/"), body)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class CreateHenkiloRequest(
        val etunimet: String,
        val sukunimi: String,
        val kutsumanimi: String,
        val henkilotunnus: String?,
        val henkilo_oid: String?
    )

    fun createHenkilo(body: CreateHenkiloRequest): HenkiloResponse =
        post(baseUrl.resolve("v1/henkilot/"), body)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class CreateLapsiRequest(
        val lahdejarjestelma: String,
        val henkilo: URI,
        val vakatoimija_oid: String?,
        val oma_organisaatio_oid: String?,
        val paos_organisaatio_oid: String?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LapsiResponse(
        val url: URI,
        val lahdejarjestelma: String,
        val henkilo: URI,
        val henkilo_oid: String,
        val vakatoimija: URI,
        val vakatoimija_oid: String?,
        val oma_organisaatio_oid: String?,
        val paos_organisaatio_oid: String?,
        val paos_kytkin: Boolean,
    )

    fun createLapsi(child: CreateLapsiRequest): LapsiResponse =
        post(baseUrl.resolve("v1/lapset/"), child)

    fun getLapsi(childUrl: URI): LapsiResponse = get(childUrl)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class CreateVarhaiskasvatuspaatosRequest(
        val lapsi: URI,
        val hakemus_pvm: LocalDate,
        val alkamis_pvm: LocalDate,
        val paattymis_pvm: LocalDate?,
        val pikakasittely_kytkin: Boolean,
        val tuntimaara_viikossa: Double,
        val kokopaivainen_vaka_kytkin: Boolean,
        val tilapainen_vaka_kytkin: Boolean,
        val paivittainen_vaka_kytkin: Boolean,
        val vuorohoito_kytkin: Boolean,
        val jarjestamismuoto_koodi: String,
        val lahdejarjestelma: String
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class VarhaiskasvatuspaatosResponse(
        val url: URI,
        val lahdejarjestelma: String,
        val alkamis_pvm: LocalDate,
        val paattymis_pvm: LocalDate?,
        val hakemus_pvm: LocalDate,
        val vuorohoito_kytkin: Boolean,
        val tilapainen_vaka_kytkin: Boolean,
        val pikakasittely_kytkin: Boolean,
        val tuntimaara_viikossa: Double,
        val paivittainen_vaka_kytkin: Boolean,
        val kokopaivainen_vaka_kytkin: Boolean,
        val jarjestamismuoto_koodi: String,
    )

    fun createVarhaiskasvatuspaatos(
        body: CreateVarhaiskasvatuspaatosRequest
    ): VarhaiskasvatuspaatosResponse = post(baseUrl.resolve("v1/varhaiskasvatuspaatokset/"), body)

    fun getVarhaiskasvatuspaatoksetByLapsi(lapsiUrl: URI): List<VarhaiskasvatuspaatosResponse> =
        getAllPages(lapsiUrl.resolve("varhaiskasvatuspaatokset/"))

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class CreateVarhaiskasvatussuhdeRequest(
        val lahdejarjestelma: String,
        val varhaiskasvatuspaatos: URI,
        val toimipaikka_oid: String,
        val alkamis_pvm: LocalDate,
        val paattymis_pvm: LocalDate?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class VarhaiskasvatussuhdeResponse(
        val url: URI,
        val lahdejarjestelma: String,
        val varhaiskasvatuspaatos: URI,
        val toimipaikka_oid: String,
        val alkamis_pvm: LocalDate,
        val paattymis_pvm: LocalDate?,
    )

    fun createVarhaiskasvatussuhde(
        body: CreateVarhaiskasvatussuhdeRequest
    ): VarhaiskasvatussuhdeResponse = post(baseUrl.resolve("v1/varhaiskasvatussuhteet/"), body)

    fun getVarhaiskasvatussuhteetByLapsi(lapsiUrl: URI): List<VarhaiskasvatussuhdeResponse> =
        getAllPages(lapsiUrl.resolve("varhaiskasvatussuhteet/"))

    private inline fun <reified R> get(url: URI): R = request(Method.GET, url)

    private data class PaginatedResponse<T>(
        val count: Int,
        val next: URI?,
        val previous: String?,
        val results: List<T>
    )

    private inline fun <reified T> getAllPages(initialUrl: URI): List<T> {
        val acc = mutableListOf<T>()
        var url: URI? = initialUrl
        while (url != null) {
            val response: PaginatedResponse<T> = get(url)
            acc += response.results
            url = response.next
        }
        return acc.toList()
    }

    private inline fun <T, reified R> post(url: URI, body: T): R = request(Method.POST, url, body)

    fun delete(url: URI) = request<Unit>(Method.DELETE, url)

    private inline fun <reified R> request(method: Method, url: URI, body: Any? = null): R {
        logger.info("requesting $method $url" + if (body == null) "" else " with body $body")

        val (request, _, result) =
            fuel
                .request(method, validateUrl(url))
                .let { if (body != null) it.jsonBody(jsonMapper.writeValueAsString(body)) else it }
                .authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info("successfully requested $method $url")
                if (Unit is R) {
                    Unit
                } else {
                    jsonMapper.readValue(result.get())
                }
            }
            is Result.Failure -> {
                val message = "failed to request $method $url"
                if (null !is R) {
                    vardaError(request, result.error) { err -> "$message: $err" }
                } else {
                    logger.warn { "$message: ${result.error}" }
                    null as R
                }
            }
        }
    }

    private fun validateUrl(url: URI): String {
        val result = url.normalize().toString()
        check(result.startsWith(baseUrl.normalize().toString())) {
            "URL $url does not start with $baseUrl"
        }
        return result
    }

    private data class VardaRequestError(
        val method: String,
        val url: String,
        val body: String,
        val errorMessage: String,
        val errorCode: String?,
        val errorDescription: String?,
        val statusCode: String
    ) {
        fun asMap() =
            mapOf(
                "method" to method,
                "url" to url,
                "body" to body,
                "errorMessage" to errorMessage,
                "errorCode" to errorCode,
                "errorDescription" to errorDescription,
                "statusCode" to statusCode
            )
    }

    private fun parseVardaErrorBody(errorString: String): Pair<List<String>, List<String>> {
        val codes =
            Regex("\"error_code\":\"(\\w+)\"")
                .findAll(errorString)
                .map { it.groupValues[1] }
                .toList()
        val descriptions =
            Regex("\"description\":\"([^\"]+)\"")
                .findAll(errorString)
                .map { it.groupValues[1] }
                .toList()
        return Pair(codes, descriptions)
    }

    private fun parseVardaError(request: Request, error: FuelError): VardaRequestError {
        return try {
            val errorString = error.errorData.decodeToString()
            val (errorCodes, descriptions) = parseVardaErrorBody(errorString)
            VardaRequestError(
                method = request.method.toString(),
                url = request.url.toString(),
                body = request.body.asString("application/json"),
                errorMessage = errorString,
                errorCode = errorCodes.first(),
                errorDescription = descriptions.first(),
                statusCode = error.response.statusCode.toString()
            )
        } catch (e: Exception) {
            VardaRequestError(
                method = request.method.toString(),
                url = request.url.toString(),
                body = request.body.asString("application/json"),
                errorMessage = error.errorData.decodeToString(),
                errorCode = null,
                errorDescription = null,
                statusCode = error.response.statusCode.toString()
            )
        }
    }

    private fun vardaError(
        request: Request,
        error: FuelError,
        message: (meta: VardaRequestError) -> String
    ): Nothing {
        val meta = parseVardaError(request, error)
        logger.error(request, meta.asMap()) {
            "request failed to ${meta.url}, status ${meta.statusCode}, reason ${meta.errorCode}: ${meta.errorDescription}"
        }
        error(message(meta))
    }

    /**
     * Wrapper for Fuel Request.responseString() that handles API token refreshes and retries when
     * throttled.
     *
     * API token refreshes are only attempted once and don't count as a try of the original request.
     *
     * TODO: Make API token usage thread-safe. Now nothing prevents another thread from invalidating
     *   the token about to be used by another thread.
     */
    private fun Request.authenticatedResponseStringWithRetries(
        maxTries: Int = 3
    ): ResponseResultOf<String> =
        tokenProvider.withToken { token, refreshToken ->
            this.authentication()
                .token(token)
                .header(Headers.ACCEPT, "application/json")
                .responseStringWithRetries(maxTries, 300L) { r, remainingTries ->
                    when (r.second.statusCode) {
                        403 ->
                            when {
                                jsonMapper.readTree(r.third.error.errorData).get("errors")?.any {
                                    it.get("error_code").asText() == "PE007"
                                } ?: false -> {
                                    logger.info(
                                        "Varda API token invalid. Refreshing token and retrying original request."
                                    )
                                    val newToken = refreshToken()
                                    // API token refresh should only be attempted once -> don't pass
                                    // an error handler to let
                                    // any subsequent errors fall through.
                                    this.authentication()
                                        .token(newToken)
                                        .responseStringWithRetries(remainingTries, 300L)
                                }
                                else -> r
                            }
                        else -> r
                    }
                }
        }
}
