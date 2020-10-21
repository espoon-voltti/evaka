package fi.espoo.evaka.shared.config

import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.vtjclient.properties.XRoadProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Configuration
class DvvModificationServiceConfig {

    @Bean
    fun dvvModificationsServiceClientFuel(env: Environment, xRoadProperties: XRoadProperties): FuelManager {
        return when (env.getProperty("fi.espoo.integration.dvv-modifications-service.enabled", Boolean::class.java, false)) {
            true -> productionFuel(xRoadProperties)
            false -> noCertCheckFuelManager()
        }
    }

    private fun productionFuel(xRoadProperties: XRoadProperties): FuelManager = FuelManager().apply {
        keystore = KeyStore.getInstance(
            File(xRoadProperties.keyStore.location),
            xRoadProperties.keyStore.password?.toCharArray()
        )

        val trustKeyStore = KeyStore.getInstance(
            File(xRoadProperties.trustStore.location),
            xRoadProperties.trustStore.password?.toCharArray()
        )

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keystore, xRoadProperties.keyStore.password?.toCharArray())

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())

        trustManagerFactory.init(trustKeyStore)

        socketFactory = SSLContext.getInstance("SSL").apply {
            init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, java.security.SecureRandom())
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
