// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.FullApplicationTest
import org.junit.jupiter.api.BeforeAll
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class DvvModificationsServiceIntegrationTestBase : FullApplicationTest() {
    protected lateinit var dvvModificationsServiceClient: DvvModificationsServiceClient
    protected lateinit var dvvModificationsService: DvvModificationsService

    @BeforeAll
    protected fun initDvvModificationService() {
        assert(httpPort > 0)
        val mockDvvBaseUrl = "http://localhost:$httpPort/mock-integration/dvv/api"
        dvvModificationsServiceClient = DvvModificationsServiceClient(objectMapper, noCertCheckFuelManager(), env, mockDvvBaseUrl)
        dvvModificationsService = DvvModificationsService(jdbi, dvvModificationsServiceClient)
    }

    fun noCertCheckFuelManager() = FuelManager().apply {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate>? = null
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        })

        socketFactory = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, java.security.SecureRandom())
        }.socketFactory

        hostnameVerifier = HostnameVerifier { _, _ -> true }
    }
}
