// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import com.fasterxml.jackson.annotation.JsonProperty
import evaka.core.DvvModificationsEnv
import evaka.core.KeystoreEnv
import evaka.core.VtjXroadEnv
import evaka.core.shared.buildHttpClient
import evaka.core.shared.utils.basicAuthInterceptor
import evaka.core.shared.utils.get
import evaka.core.shared.utils.headerInterceptor
import evaka.core.shared.utils.post
import evaka.core.shared.utils.trustAllCerts
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.security.SecureRandom
import java.time.LocalDate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper

private val logger = KotlinLogging.logger {}

/*
   Integration to DVV modifications service (muutostietopalvelu)
   See https://hiekkalaatikko.muutostietopalvelu.cloud.dvv.fi/
*/
@Service
class DvvModificationsServiceClient(
    private val jsonMapper: JsonMapper,
    private val customizers: List<DvvModificationRequestCustomizer>,
    xroadEnv: VtjXroadEnv,
    env: DvvModificationsEnv,
) {
    private val serviceUrl = env.url
    private val httpClient =
        buildHttpClient(
            rootUrl = URI(serviceUrl),
            jsonMapper = jsonMapper,
            interceptors =
                buildList {
                    add(headerInterceptor("Accept", "application/json"))
                    add(basicAuthInterceptor(env.userId, env.password.value))
                    add(headerInterceptor("X-Road-Client", env.xroadClientId))
                    if (customizers.isNotEmpty()) {
                        add(
                            okhttp3.Interceptor { chain ->
                                val builder = chain.request().newBuilder()
                                customizers.forEach { it.customize(builder) }
                                chain.proceed(builder.build())
                            }
                        )
                    }
                },
            customize = sslConfiguration(xroadEnv),
        )

    fun getFirstModificationToken(
        date: LocalDate
    ): DvvModificationServiceModificationTokenResponse? {
        logger.info {
            "Fetching the first modification token of $date from DVV modification service from $serviceUrl/kirjausavain/$date"
        }
        return try {
            val response =
                httpClient.get<DvvModificationServiceModificationTokenResponse>(
                    "kirjausavain/$date"
                )
            logger.info {
                "Fetching the first modification token of $date from DVV modification service succeeded"
            }
            response
        } catch (e: Exception) {
            logger.error(e) {
                "Fetching the first modification of $date from DVV modification service failed"
            }
            null
        }
    }

    fun getModifications(updateToken: String, ssns: List<String>): DvvModificationsResponse {
        logger.info {
            "Fetching modifications with token $updateToken from DVV modifications service from $serviceUrl/muutokset"
        }
        return try {
            val response =
                httpClient.post<DvvModificationsResponse>(
                    "muutokset",
                    jsonBody =
                        DvvModificationsRequest(
                            viimeisinKirjausavain =
                                updateToken.toLongOrNull()
                                    ?: error("DVV update token is not a valid long: $updateToken"),
                            hetulista = ssns,
                        ),
                )
            logger.info {
                "Fetching modifications with token $updateToken from DVV modifications service succeeded"
            }
            response
        } catch (e: Exception) {
            logger.error(e) {
                "Fetching modifications with token $updateToken from DVV modifications service failed"
            }
            throw e
        }
    }
}

internal fun sslConfiguration(xroadEnv: VtjXroadEnv): (OkHttpClient.Builder) -> Unit = { builder ->
    if (
        xroadEnv.httpClientCertificateCheck &&
            xroadEnv.keyStore != null &&
            xroadEnv.trustStore != null
    ) {
        configureCertificateCheck(builder, xroadEnv.keyStore, xroadEnv.trustStore)
    } else if (!xroadEnv.httpClientCertificateCheck) {
        trustAllCerts(builder)
    }
}

private fun configureCertificateCheck(
    builder: OkHttpClient.Builder,
    keyStore: KeystoreEnv,
    trustStore: KeystoreEnv,
) {
    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    keyManagerFactory.init(keyStore.load(), keyStore.password?.value?.toCharArray())

    val trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(trustStore.load())

    val sslContext =
        SSLContext.getInstance("TLS").apply {
            init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, SecureRandom())
        }

    val trustManager =
        trustManagerFactory.trustManagers.filterIsInstance<X509TrustManager>().first()
    builder.sslSocketFactory(sslContext.socketFactory, trustManager)
    builder.hostnameVerifier { _, _ -> true }
}

data class DvvModificationServiceModificationTokenResponse(
    @JsonProperty("viimeisinKirjausavain") var latestModificationToken: Long
)

/** Callback interface that can be implemented by beans wishing to customize the HTTP requests. */
fun interface DvvModificationRequestCustomizer {
    fun customize(builder: Request.Builder)
}
