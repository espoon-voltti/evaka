// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.new

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.varda.VardaUnitClient
import fi.espoo.evaka.varda.VardaUnitRequest
import fi.espoo.evaka.varda.VardaUnitResponse
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import java.net.URI
import java.time.LocalDate
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

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
            "HaeHenkiloRequest(henkilotunnus=${maskHenkilotunnus(henkilotunnus)}, henkilo_oid=$henkilo_oid)"
    }

    fun haeHenkilo(body: HaeHenkiloRequest): HenkiloResponse

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class GetOrCreateHenkiloRequest(
        val etunimet: String,
        val sukunimi: String,
        val henkilotunnus: String?,
        val henkilo_oid: String?
    ) {
        init {
            check(henkilotunnus != null || henkilo_oid != null) {
                "Both params henkilotunnus and henkilo_oid must not be null"
            }
        }

        override fun toString() =
            "GetOrCreateHenkiloRequest(" +
                "etunimet=${maskName(etunimet)}, " +
                "sukunimi=${maskName(sukunimi)}, " +
                "henkilotunnus=${maskHenkilotunnus(henkilotunnus)}, " +
                "henkilo_oid=$henkilo_oid" +
                ")"
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

    // Even though this operation may write to Varda, it has to be in VardaReadClient. We need to
    // have a henkilo to be able to find the corresponding lapsi entries.
    fun getOrCreateHenkilo(body: GetOrCreateHenkiloRequest): HenkiloResponse

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LapsiResponse(
        override val url: URI,
        // Based on Varda's API documentation, lahdejarjestelma is required, but the Varda response
        // doesn't always include it.
        val lahdejarjestelma: String?,
        val vakatoimija_oid: String?,
        val oma_organisaatio_oid: String?,
        val paos_organisaatio_oid: String?,
        val paos_kytkin: Boolean,
    ) : VardaEntity

    fun getLapset(): List<LapsiResponse>

    fun getLapsi(url: URI): LapsiResponse

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class VarhaiskasvatuspaatosResponse(
        override val url: URI,
        // Based on Varda's API documentation, lahdejarjestelma is required, but the Varda response
        // doesn't always include it.
        val lahdejarjestelma: String?,
        override val alkamis_pvm: LocalDate,
        override val paattymis_pvm: LocalDate?,
        val hakemus_pvm: LocalDate,
        val vuorohoito_kytkin: Boolean,
        val tilapainen_vaka_kytkin: Boolean,
        val tuntimaara_viikossa: Double,
        val paivittainen_vaka_kytkin: Boolean,
        val kokopaivainen_vaka_kytkin: Boolean,
        val jarjestamismuoto_koodi: String,
    ) : VardaEntityWithValidity

    fun getVarhaiskasvatuspaatoksetByLapsi(lapsiUrl: URI): List<VarhaiskasvatuspaatosResponse>

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class VarhaiskasvatussuhdeResponse(
        override val url: URI,
        // Based on Varda's API documentation, lahdejarjestelma is required, but the Varda response
        // doesn't always include it.
        val lahdejarjestelma: String?,
        val varhaiskasvatuspaatos: URI,
        val toimipaikka_oid: String,
        override val alkamis_pvm: LocalDate,
        override val paattymis_pvm: LocalDate?,
    ) : VardaEntityWithValidity

    fun getVarhaiskasvatussuhteetByLapsi(lapsiUrl: URI): List<VarhaiskasvatussuhdeResponse>

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MaksutietoResponse(
        override val url: URI,
        // Based on Varda's API documentation, lahdejarjestelma is required, but the Varda response
        // doesn't always include it.
        val lahdejarjestelma: String?,
        val huoltajat: List<Huoltaja>,
        val lapsi: URI,
        override val alkamis_pvm: LocalDate,
        override val paattymis_pvm: LocalDate?,
        val maksun_peruste_koodi: String,
        val palveluseteli_arvo: Double?,
        val asiakasmaksu: Double,
        val perheen_koko: Int?,
    ) : VardaEntityWithValidity

    fun getMaksutiedotByLapsi(lapsiUrl: URI): List<MaksutietoResponse>
}

interface VardaWriteClient {
    @JsonIgnoreProperties(ignoreUnknown = true) data class CreateResponse(val url: URI)

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

    data class SetPaattymisPvmRequest(val paattymis_pvm: LocalDate)

    fun setPaattymisPvm(url: URI, body: SetPaattymisPvmRequest)
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

interface VardaEntityWithValidity : VardaEntity {
    val alkamis_pvm: LocalDate
    val paattymis_pvm: LocalDate?
}

class VardaClient(
    private val tokenProvider: VardaTokenProvider,
    private val httpClient: OkHttpClient,
    private val jsonMapper: JsonMapper,
    vardaBaseUrl: URI
) : VardaReadClient, VardaWriteClient, VardaUnitClient {
    private val baseUrl = vardaBaseUrl.ensureTrailingSlash()

    override fun haeHenkilo(
        body: VardaReadClient.HaeHenkiloRequest
    ): VardaReadClient.HenkiloResponse = post(baseUrl.resolve("v1/hae-henkilo/"), body)

    override fun getOrCreateHenkilo(
        body: VardaReadClient.GetOrCreateHenkiloRequest
    ): VardaReadClient.HenkiloResponse = post(baseUrl.resolve("v1/henkilot/"), body)

    override fun createLapsi(
        body: VardaWriteClient.CreateLapsiRequest
    ): VardaWriteClient.CreateResponse = post(baseUrl.resolve("v1/lapset/"), body)

    override fun getLapset(): List<VardaReadClient.LapsiResponse> =
        getAllPages(baseUrl.resolve("v1/lapset/"))

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

    override fun setPaattymisPvm(url: URI, body: VardaWriteClient.SetPaattymisPvmRequest): Unit =
        patch(url, body)

    fun vakajarjestajaUrl(organizerId: String): String =
        baseUrl.resolve("v1/vakajarjestajat/$organizerId/").toString()

    override fun createUnit(unit: VardaUnitRequest): VardaUnitResponse =
        post(baseUrl.resolve("v1/toimipaikat/"), unit)

    override fun updateUnit(id: Long, unit: VardaUnitRequest): VardaUnitResponse =
        put(baseUrl.resolve("v1/toimipaikat/$id/"), unit)

    private inline fun <reified R> get(url: URI): R = request("GET", url)

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

    private inline fun <T, reified R> patch(url: URI, body: T): R = request("PATCH", url, body)

    private inline fun <T, reified R> post(url: URI, body: T): R = request("POST", url, body)

    private inline fun <T, reified R> put(url: URI, body: T): R = request("PUT", url, body)

    override fun <T : VardaEntity> delete(data: T) = request<Unit>("DELETE", data.url)

    private inline fun <reified R> request(method: String, url: URI, body: Any? = null): R {
        logger.info("requesting $method $url" + if (body == null) "" else " with body $body")

        val req =
            Request.Builder()
                .method(
                    method,
                    body?.let {
                        jsonMapper
                            .writeValueAsString(it)
                            .toRequestBody("application/json".toMediaType())
                    }
                )
                .url(validateUrl(url))
                .build()

        return httpClient.executeAuthenticated(req) { response ->
            if (!response.isSuccessful) {
                val message =
                    "request failed $method $url: status=${response.code} body=${response.body?.string()}"
                logger.error { message }
                error(message)
            }

            logger.info { "successfully requested $method $url" }
            if (Unit is R) {
                Unit
            } else {
                jsonMapper.readValue(response.body?.string()!!)
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

    /**
     * Wrapper for Fuel Request.responseString() that handles API token refreshes and retries when
     * throttled.
     *
     * API token refreshes are only attempted once and don't count as a try of the original request.
     *
     * TODO: Make API token usage thread-safe. Now nothing prevents another thread from invalidating
     *   the token about to be used by another thread.
     */
    private fun <T> OkHttpClient.executeAuthenticated(
        request: Request,
        fn: (response: Response) -> T
    ): T =
        tokenProvider.withToken { token, refreshToken ->
            executeWithToken(request, token).use { response ->
                if (response.code == 403) {
                    val errorBody = response.body?.string() ?: ""
                    if (errorBody.contains("PE007")) {
                        logger.info {
                            "Varda API token invalid. Refreshing token and retrying original request."
                        }
                        val newToken = refreshToken()
                        executeWithToken(request, newToken).use { fn(it) }
                    } else {
                        fn(response)
                    }
                } else {
                    fn(response)
                }
            }
        }

    private fun OkHttpClient.executeWithToken(request: Request, token: String): Response =
        this.newCall(request.newBuilder().header("Authorization", "Token $token").build()).execute()
}
