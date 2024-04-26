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

private fun maskHenkilotunnus(henkilotunnus: String?): String? =
    henkilotunnus?.let { "${it.slice(0..4)}******" }

private fun maskName(name: String): String = name.take(2) + "*".repeat(name.length - 2)

interface VardaReadClient {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class HaeHenkiloRequest(
        val henkilotunnus: String? = null,
        val henkilo_oid: String? = null
    ) {
        init {
            check(henkilotunnus != null || henkilo_oid != null) {
                "Both params henkilotunnus and henkilo_oid must not be null"
            }
        }

        override fun toString(): String =
            "VardaPersonSearchRequest(henkilotunnus=${maskHenkilotunnus(henkilotunnus)}, henkilo_oid=$henkilo_oid)"
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class HenkiloResponse(
        val url: URI,
        /**
         * henkilo_oid is nullable to accommodate dry runs. Varda always returns a non-null value.
         */
        val henkilo_oid: String?,
        val lapsi: List<URI>,
    )

    fun haeHenkilo(body: HaeHenkiloRequest): HenkiloResponse?

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LapsiResponse(
        override val url: URI,
        // Based on Varda's API documentation, lahdejarjestelma is required, but the Varda response
        // sometimes doesn't include it.
        val lahdejarjestelma: String?,
        val vakatoimija_oid: String?,
        val oma_organisaatio_oid: String?,
        val paos_organisaatio_oid: String?,
        val paos_kytkin: Boolean,
    ) : VardaEntity

    fun getLapsi(url: URI): LapsiResponse

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class VarhaiskasvatuspaatosResponse(
        override val url: URI,
        val lahdejarjestelma: String,
        val alkamis_pvm: LocalDate,
        val paattymis_pvm: LocalDate?,
        val hakemus_pvm: LocalDate,
        val vuorohoito_kytkin: Boolean,
        val tilapainen_vaka_kytkin: Boolean,
        val tuntimaara_viikossa: Double,
        val paivittainen_vaka_kytkin: Boolean,
        val kokopaivainen_vaka_kytkin: Boolean,
        val jarjestamismuoto_koodi: String,
    ) : VardaEntity

    fun getVarhaiskasvatuspaatoksetByLapsi(lapsiUrl: URI): List<VarhaiskasvatuspaatosResponse>

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class VarhaiskasvatussuhdeResponse(
        override val url: URI,
        val lahdejarjestelma: String,
        val varhaiskasvatuspaatos: URI,
        val toimipaikka_oid: String,
        val alkamis_pvm: LocalDate,
        val paattymis_pvm: LocalDate?,
    ) : VardaEntity

    fun getVarhaiskasvatussuhteetByLapsi(lapsiUrl: URI): List<VarhaiskasvatussuhdeResponse>

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MaksutietoResponse(
        override val url: URI,
        val lahdejarjestelma: String,
        val huoltajat: List<Huoltaja>,
        val lapsi: URI,
        val alkamis_pvm: LocalDate,
        val paattymis_pvm: LocalDate?,
        val maksun_peruste_koodi: String,
        val palveluseteli_arvo: Double?,
        val asiakasmaksu: Double,
        val perheen_koko: Int?,
    ) : VardaEntity

    fun getMaksutiedotByLapsi(lapsiUrl: URI): List<MaksutietoResponse>
}

interface VardaWriteClient {
    @JsonIgnoreProperties(ignoreUnknown = true) data class CreateResponse(val url: URI)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class CreateHenkiloRequest(
        val etunimet: String,
        val sukunimi: String,
        val kutsumanimi: String,
        val henkilotunnus: String?,
        val henkilo_oid: String?
    ) {
        override fun toString() =
            "CreateHenkiloRequest(" +
                "etunimet=${maskName(etunimet)}, " +
                "sukunimi=${maskName(sukunimi)}, " +
                "kutsumanimi=${maskName(kutsumanimi)}, " +
                "henkilotunnus=${maskHenkilotunnus(henkilotunnus)}, " +
                "henkilo_oid=$henkilo_oid" +
                ")"
    }

    fun createHenkilo(body: CreateHenkiloRequest): VardaReadClient.HenkiloResponse

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class CreateLapsiRequest(
        val lahdejarjestelma: String,
        val henkilo: URI,
        val vakatoimija_oid: String?,
        val oma_organisaatio_oid: String?,
        val paos_organisaatio_oid: String?,
    )

    fun createLapsi(body: CreateLapsiRequest): CreateResponse

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class CreateVarhaiskasvatuspaatosRequest(
        val lapsi: URI,
        val hakemus_pvm: LocalDate,
        val alkamis_pvm: LocalDate,
        val paattymis_pvm: LocalDate?,
        val tuntimaara_viikossa: Double,
        val kokopaivainen_vaka_kytkin: Boolean,
        val tilapainen_vaka_kytkin: Boolean,
        val paivittainen_vaka_kytkin: Boolean,
        val vuorohoito_kytkin: Boolean,
        val jarjestamismuoto_koodi: String,
        val lahdejarjestelma: String
    )

    fun createVarhaiskasvatuspaatos(body: CreateVarhaiskasvatuspaatosRequest): CreateResponse

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class CreateVarhaiskasvatussuhdeRequest(
        val lahdejarjestelma: String,
        val varhaiskasvatuspaatos: URI,
        val toimipaikka_oid: String,
        val alkamis_pvm: LocalDate,
        val paattymis_pvm: LocalDate?,
    )

    fun createVarhaiskasvatussuhde(body: CreateVarhaiskasvatussuhdeRequest): CreateResponse

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class CreateMaksutietoRequest(
        val lahdejarjestelma: String,
        val huoltajat: List<Huoltaja>,
        val lapsi: URI,
        val alkamis_pvm: LocalDate,
        val paattymis_pvm: LocalDate?,
        val maksun_peruste_koodi: String,
        val palveluseteli_arvo: Double?,
        val asiakasmaksu: Double,
        val perheen_koko: Int?,
    )

    fun createMaksutieto(body: CreateMaksutietoRequest): CreateResponse

    fun <T : VardaEntity> delete(data: T)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Huoltaja(
    val henkilotunnus: String?,
    val henkilo_oid: String?,
    val etunimet: String,
    val sukunimi: String
) {
    init {
        check(henkilotunnus != null || henkilo_oid != null) {
            "Both params henkilotunnus and henkilo_oid must not be null"
        }
    }

    override fun toString(): String =
        "Huoltaja(" +
            "henkilotunnus=${maskHenkilotunnus(henkilotunnus)}, " +
            "henkilo_oid=$henkilo_oid, " +
            "etunimet=${maskName(etunimet)}, " +
            "sukunimi=${maskName(sukunimi)}" +
            ")"
}

interface VardaEntity {
    val url: URI
}

class VardaClient(
    private val tokenProvider: VardaTokenProvider,
    private val fuel: FuelManager,
    private val jsonMapper: JsonMapper,
    vardaBaseUrl: URI
) : VardaReadClient, VardaWriteClient {
    private val baseUrl = vardaBaseUrl.ensureTrailingSlash()

    override fun haeHenkilo(
        body: VardaReadClient.HaeHenkiloRequest
    ): VardaReadClient.HenkiloResponse? = post(baseUrl.resolve("v1/hae-henkilo/"), body)

    override fun createHenkilo(
        body: VardaWriteClient.CreateHenkiloRequest
    ): VardaReadClient.HenkiloResponse = post(baseUrl.resolve("v1/henkilot/"), body)

    override fun createLapsi(
        body: VardaWriteClient.CreateLapsiRequest
    ): VardaWriteClient.CreateResponse = post(baseUrl.resolve("v1/lapset/"), body)

    override fun getLapsi(url: URI): VardaReadClient.LapsiResponse = get(url)

    override fun createVarhaiskasvatuspaatos(
        body: VardaWriteClient.CreateVarhaiskasvatuspaatosRequest
    ): VardaWriteClient.CreateResponse = post(baseUrl.resolve("v1/varhaiskasvatuspaatokset/"), body)

    override fun getVarhaiskasvatuspaatoksetByLapsi(
        lapsiUrl: URI
    ): List<VardaReadClient.VarhaiskasvatuspaatosResponse> =
        getAllPages(lapsiUrl.resolve("varhaiskasvatuspaatokset/"))

    override fun createVarhaiskasvatussuhde(
        body: VardaWriteClient.CreateVarhaiskasvatussuhdeRequest
    ): VardaWriteClient.CreateResponse = post(baseUrl.resolve("v1/varhaiskasvatussuhteet/"), body)

    override fun getVarhaiskasvatussuhteetByLapsi(
        lapsiUrl: URI
    ): List<VardaReadClient.VarhaiskasvatussuhdeResponse> =
        getAllPages(lapsiUrl.resolve("varhaiskasvatussuhteet/"))

    override fun createMaksutieto(
        body: VardaWriteClient.CreateMaksutietoRequest
    ): VardaWriteClient.CreateResponse = post(baseUrl.resolve("v1/maksutiedot/"), body)

    override fun getMaksutiedotByLapsi(lapsiUrl: URI): List<VardaReadClient.MaksutietoResponse> =
        getAllPages(lapsiUrl.resolve("maksutiedot/"))

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

    override fun <T : VardaEntity> delete(data: T) = request<Unit>(Method.DELETE, data.url)

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
                if (null !is R || result.error.response.statusCode != 404) {
                    vardaError(request, result.error) { err ->
                        "failed to request $method $url: ${err.toString().trim()}"
                    }
                } else {
                    logger.info("successfully requested $method $url: not found")
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
            "request failed ${meta.method} ${meta.url}, status ${meta.statusCode}, reason ${meta.errorCode}: ${meta.errorDescription}"
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
