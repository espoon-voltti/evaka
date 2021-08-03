// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.VtjXroadEnv
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
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
    fun fuel(env: EvakaEnv, vtjXroadEnv: VtjXroadEnv, resourceLoader: ResourceLoader): FuelManager {
        return when (env.httpClientCertificateCheck) {
            true -> certCheckFuelManager(vtjXroadEnv, resourceLoader)
            false -> noCertCheckFuelManager()
        }
    }

    fun certCheckFuelManager(xroadEnv: VtjXroadEnv, resourceLoader: ResourceLoader): FuelManager = FuelManager().apply {
        var keyManagers = arrayOf<KeyManager>()
        if (xroadEnv.keyStore.location != null) {
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keystore = KeyStore.getInstance(resourceLoader.getResource(xroadEnv.keyStore.location).file, xroadEnv.keyStore.password.value.toCharArray())
            keyManagerFactory.init(keystore, xroadEnv.keyStore.password.value.toCharArray())
            keyManagers = keyManagerFactory.keyManagers
        }

        var trustManagers = arrayOf<TrustManager>()
        if (xroadEnv.trustStore.location != null) {
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            val trustKeyStore = KeyStore.getInstance(resourceLoader.getResource(xroadEnv.trustStore.location).file, xroadEnv.trustStore.password.value.toCharArray())
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
