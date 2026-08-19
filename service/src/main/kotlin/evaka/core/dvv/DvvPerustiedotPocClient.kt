// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import com.fasterxml.jackson.annotation.JsonInclude
import evaka.core.DvvPerustiedotPocEnv
import evaka.core.shared.buildHttpClient
import evaka.core.shared.utils.basicAuthInterceptor
import evaka.core.shared.utils.headerInterceptor
import evaka.core.shared.utils.post
import evaka.core.vtjclient.dto.VtjPerson
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.security.KeyStore
import java.security.SecureRandom
import java.time.LocalDate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper

private val logger = KotlinLogging.logger {}

/**
 * POC client for DVV's REST `POST /perustiedot` ("Henkilöiden perustietojen haku").
 *
 * Kept separate from [DvvModificationsServiceClient] so the POC cannot destabilise the live
 * modifications integration: its own env, own credentials, own http client. The production client
 * is X-Road-shaped (basicAuth + `X-Road-Client` + mTLS); this one targets the DVV sandbox over
 * plain HTTPS with basic auth, which is what the OpenAPI declares
 * (`https://api.hiekkalaatikko.muutostietopalvelu.dvv.fi/api/v1`).
 */
@Profile("enable_dev_api")
@Service
class DvvPerustiedotPocClient(private val jsonMapper: JsonMapper, env: DvvPerustiedotPocEnv?) {
    private val serviceUrl = env?.url

    /** Lazy: the truststore is excluded from the boot jar, so eager init would fail startup. */
    private val httpClient by lazy {
        env?.let {
            buildHttpClient(
                rootUrl = URI(it.url),
                jsonMapper = jsonMapper,
                interceptors =
                    listOf(
                        headerInterceptor("Accept", "application/json"),
                        basicAuthInterceptor(it.userId, it.password.value),
                    ),
                customize = dvvTrustConfiguration(),
            )
        }
    }

    /**
     * DVV allows up to 1000 SSNs per call.
     *
     * @param tietoryhmat which data groups to return; when empty, DVV returns everything the
     *   product's tietosuojalupa permits
     * @return the raw response body, unparsed
     */
    fun getPerustiedot(ssns: List<String>, tietoryhmat: List<String> = emptyList()): String {
        val client =
            httpClient
                ?: error(
                    "DVV perustiedot POC is not configured (evaka.integration.dvv_perustiedot_poc.url)"
                )
        require(ssns.isNotEmpty()) { "At least one SSN is required" }
        require(ssns.size <= MAX_SSNS_PER_REQUEST) {
            "DVV allows at most $MAX_SSNS_PER_REQUEST SSNs per request, got ${ssns.size}"
        }
        logger.info {
            "Fetching perustiedot for ${ssns.size} person(s) from DVV from $serviceUrl/perustiedot"
        }
        return client.post<String>(
            "perustiedot",
            jsonBody =
                DvvPerustiedotRequest(hetulista = ssns, tietoryhmat = tietoryhmat.ifEmpty { null }),
            responseHandler = { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    error("DVV perustiedot request failed with status ${response.code}")
                }
                logger.info { "Fetching perustiedot for ${ssns.size} person(s) succeeded" }
                body
            },
        )
    }

    /** Guardians/dependants come back as name+SSN stubs; hydrating them is a second call. */
    fun getPerustiedotAsVtjPersons(
        ssns: List<String>,
        today: LocalDate,
        tietoryhmat: List<String> = emptyList(),
    ): List<VtjPerson> =
        jsonMapper
            .readValue(getPerustiedot(ssns, tietoryhmat), DvvPerustiedotResponse::class.java)
            .perustiedot
            .map { it.toVtjPerson(today) }

    companion object {
        const val MAX_SSNS_PER_REQUEST = 1000
    }
}

/**
 * DVV's endpoints present certificates issued by DVV's own CA (`DVV Gov. Root CA - G3 RSA`), which
 * ships in neither the OS CA bundle nor the JVM `cacerts`; without it the handshake fails with
 * `SunCertPathBuilderException`. The CA is bundled rather than configured because this POC targets
 * exactly one host — pointing it elsewhere should fail loudly.
 *
 * Only the trust side is set up. The X-Road route additionally needs mutual TLS (see
 * `sslConfiguration` in [DvvModificationsServiceClient]); the sandbox authenticates with basic auth
 * alone. Hostname verification is deliberately left enabled.
 */
private fun dvvTrustConfiguration(): (OkHttpClient.Builder) -> Unit = { builder ->
    val trustStore =
        KeyStore.getInstance("pkcs12").apply {
            val stream =
                checkNotNull(DvvPerustiedotPocClient::class.java.getResourceAsStream(TRUSTSTORE)) {
                    "DVV truststore not found on the classpath at $TRUSTSTORE"
                }
            stream.use { load(it, TRUSTSTORE_PASSWORD.toCharArray()) }
        }
    val trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(trustStore)
    val trustManager =
        trustManagerFactory.trustManagers.filterIsInstance<X509TrustManager>().first()
    val sslContext =
        SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustManager), SecureRandom()) }
    builder.sslSocketFactory(sslContext.socketFactory, trustManager)
}

private const val TRUSTSTORE = "/certs/dvv-truststore.p12"
// Not a secret: the store holds only DVV's public root CA, but PKCS12 requires a password
private const val TRUSTSTORE_PASSWORD = "evaka-dvv-poc"

data class DvvPerustiedotRequest(
    val hetulista: List<String>,
    // omitted entirely when null: DVV then returns every tietoryhmä the tietosuojalupa permits
    @JsonInclude(JsonInclude.Include.NON_NULL) val tietoryhmat: List<String>?,
)
