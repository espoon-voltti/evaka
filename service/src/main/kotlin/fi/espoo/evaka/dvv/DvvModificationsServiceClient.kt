// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import fi.espoo.evaka.DvvModificationsEnv
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.KeystoreEnv
import fi.espoo.evaka.VtjXroadEnv
import java.security.cert.X509Certificate
import java.time.LocalDate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/*
   Integration to DVV modifications service (muutostietopalvelu)
   See https://hiekkalaatikko.muutostietopalvelu.cloud.dvv.fi/
*/
@Service
class DvvModificationsServiceClient(
    private val jsonMapper: JsonMapper,
    private val customizers: List<DvvModificationRequestCustomizer>,
    evakaEnv: EvakaEnv,
    xroadEnv: VtjXroadEnv,
    env: DvvModificationsEnv
) {
    private val serviceUrl: String = env.url
    private val dvvUserId = env.userId
    private val dvvPassword = env.password
    private val dvvXroadClientId: String = env.xroadClientId

    private val fuel =
        if (
            evakaEnv.httpClientCertificateCheck &&
                xroadEnv.keyStore != null &&
                xroadEnv.trustStore != null
        ) {
            certCheckFuelManager(keyStore = xroadEnv.keyStore, trustStore = xroadEnv.trustStore)
        } else if (!evakaEnv.httpClientCertificateCheck) {
            noCertCheckFuelManager()
        } else {
            FuelManager()
        }

    private fun certCheckFuelManager(keyStore: KeystoreEnv, trustStore: KeystoreEnv): FuelManager =
        FuelManager().apply {
            val keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keystore = keyStore.load()
            keyManagerFactory.init(keystore, keyStore.password?.value?.toCharArray())

            val trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            val trustKeyStore = trustStore.load()
            trustManagerFactory.init(trustKeyStore)

            socketFactory =
                SSLContext.getInstance("SSL")
                    .apply {
                        init(
                            keyManagerFactory.keyManagers,
                            trustManagerFactory.trustManagers,
                            java.security.SecureRandom()
                        )
                    }
                    .socketFactory

            hostnameVerifier = HostnameVerifier { _, _ -> true }
        }

    private fun noCertCheckFuelManager() =
        FuelManager().apply {
            val trustAllCerts =
                arrayOf<TrustManager>(
                    object : X509TrustManager {
                        override fun getAcceptedIssuers(): Array<X509Certificate>? = null

                        override fun checkClientTrusted(
                            chain: Array<X509Certificate>,
                            authType: String
                        ) = Unit

                        override fun checkServerTrusted(
                            chain: Array<X509Certificate>,
                            authType: String
                        ) = Unit
                    }
                )

            socketFactory =
                SSLContext.getInstance("SSL")
                    .apply { init(null, trustAllCerts, java.security.SecureRandom()) }
                    .socketFactory

            hostnameVerifier = HostnameVerifier { _, _ -> true }
        }

    // Fetch the first modification token of the given date
    fun getFirstModificationToken(
        date: LocalDate
    ): DvvModificationServiceModificationTokenResponse? {
        logger.info {
            "Fetching the first modification token of $date from DVV modification service from $serviceUrl/kirjausavain/$date"
        }
        val (_, _, result) =
            fuel
                .get("$serviceUrl/kirjausavain/$date")
                .header(Headers.ACCEPT, "application/json")
                .header("MUTP-Tunnus", dvvUserId)
                .header("MUTP-Salasana", dvvPassword.value)
                .header("X-Road-Client", dvvXroadClientId)
                .apply { customizers.forEach { it.customize(this) } }
                .responseString()

        return when (result) {
            is Result.Success -> {
                logger.info {
                    "Fetching the first modification token of $date from DVV modification service succeeded"
                }
                jsonMapper.readValue<DvvModificationServiceModificationTokenResponse>(
                    jsonMapper.readTree(result.get()).toString()
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

    fun getModifications(updateToken: String, ssns: List<String>): DvvModificationsResponse {
        logger.info {
            "Fetching modifications with token $updateToken from DVV modifications service from $serviceUrl/muutokset"
        }
        val (_, _, result) =
            fuel
                .post("$serviceUrl/muutokset")
                .header(Headers.ACCEPT, "application/json")
                .header("MUTP-Tunnus", dvvUserId)
                .header("MUTP-Salasana", dvvPassword.value)
                .header("X-Road-Client", dvvXroadClientId)
                .jsonBody(
                    """{
                    "viimeisinKirjausavain": $updateToken,
                    "hetulista": [${ssns.map { "\"$it\"" }.joinToString()}]
                }
                """
                        .trimIndent()
                )
                .apply { customizers.forEach { it.customize(this) } }
                .responseString()

        return when (result) {
            is Result.Success -> {
                logger.info {
                    "Fetching modifications with token $updateToken from DVV modifications service succeeded"
                }
                jsonMapper.readValue<DvvModificationsResponse>(
                    jsonMapper.readTree(result.get()).toString()
                )
            }
            is Result.Failure -> {
                logger.error(result.getException()) {
                    "Fetching modifications with token $updateToken from DVV modifications service failed, message: ${String(result.error.errorData)}"
                }
                throw result.getException()
            }
        }
    }
}

data class DvvModificationServiceModificationTokenResponse(
    @JsonProperty("viimeisinKirjausavain") var latestModificationToken: Long
)

/** Callback interface that can be implemented by beans wishing to customize the HTTP requests. */
fun interface DvvModificationRequestCustomizer {
    fun customize(request: Request)
}
