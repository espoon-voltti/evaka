// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.DvvModificationsEnv
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.VtjXroadEnv
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired

class DvvModificationsServiceIntegrationTestBase(resetDbBeforeEach: Boolean) :
    FullApplicationTest(resetDbBeforeEach = resetDbBeforeEach) {

    @Autowired protected lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    protected lateinit var dvvModificationsServiceClient: DvvModificationsServiceClient
    protected lateinit var dvvModificationsService: DvvModificationsService
    protected lateinit var requestCustomizerMock: DvvModificationRequestCustomizer

    @BeforeEach
    fun initDvvModificationService() {
        assert(httpPort > 0)
        val mockDvvBaseUrl = "http://localhost:$httpPort/mock-integration/dvv/api/v1"
        requestCustomizerMock = mock()
        dvvModificationsServiceClient =
            DvvModificationsServiceClient(
                jsonMapper,
                listOf(requestCustomizerMock),
                EvakaEnv.fromEnvironment(env).copy(httpClientCertificateCheck = false),
                VtjXroadEnv.fromEnvironment(env).copy(keyStore = null, trustStore = null),
                DvvModificationsEnv.fromEnvironment(env).copy(url = mockDvvBaseUrl)
            )
        dvvModificationsService =
            DvvModificationsService(dvvModificationsServiceClient, asyncJobRunner)
    }

    fun noCertCheckFuelManager() =
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
}
