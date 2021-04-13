// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.vtjclient.properties.XRoadProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Configuration
class FuelManagerConfig {
    @Bean
    @ConditionalOnMissingBean
    fun fuel(env: Environment, xRoadProperties: XRoadProperties): FuelManager {
        return when (env.getProperty("fuel.certificate.check", Boolean::class.java, true)) {
            true -> certCheckFuelManager(xRoadProperties)
            false -> noCertCheckFuelManager()
        }
    }

    fun certCheckFuelManager(xRoadProperties: XRoadProperties): FuelManager = FuelManager().apply {
        var keyManagers = arrayOf<KeyManager>()
        if (xRoadProperties.keyStore.location != null) {
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keystore = KeyStore.getInstance(File(xRoadProperties.keyStore.location), xRoadProperties.keyStore.password?.toCharArray())
            keyManagerFactory.init(keystore, xRoadProperties.keyStore.password?.toCharArray())
            keyManagers = keyManagerFactory.keyManagers
        }

        var trustManagers = arrayOf<TrustManager>()
        if (xRoadProperties.trustStore.location != null) {
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            val trustKeyStore = KeyStore.getInstance(File(xRoadProperties.trustStore.location), xRoadProperties.trustStore.password?.toCharArray())
            trustManagerFactory.init(trustKeyStore)
            trustManagers = trustManagerFactory.trustManagers
        }

        socketFactory = SSLContext.getInstance("SSL").apply {
            init(keyManagers, trustManagers, java.security.SecureRandom())
        }.socketFactory

        hostnameVerifier = HostnameVerifier { _, _ -> true }
    }

    private fun noCertCheckFuelManager() = FuelManager().apply {
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
