// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import fi.espoo.evaka.KeystoreEnv
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.VtjXroadClientEnv
import fi.espoo.evaka.VtjXroadEnv
import fi.espoo.evaka.VtjXroadServiceEnv
import java.io.FileOutputStream
import java.math.BigInteger
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSession
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okhttp3.OkHttpClient
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DvvModificationsServiceClientSslTest {
    @TempDir lateinit var tempDir: Path

    private fun createXroadEnv(
        httpClientCertificateCheck: Boolean,
        keyStore: KeystoreEnv? = null,
        trustStore: KeystoreEnv? = null,
    ) =
        VtjXroadEnv(
            trustStore = trustStore,
            keyStore = keyStore,
            address = "http://localhost",
            httpClientCertificateCheck = httpClientCertificateCheck,
            client =
                VtjXroadClientEnv(
                    instance = "",
                    memberClass = "",
                    memberCode = "",
                    subsystemCode = "",
                ),
            service =
                VtjXroadServiceEnv(
                    instance = "",
                    memberClass = "",
                    memberCode = "",
                    subsystemCode = "",
                    serviceCode = "",
                    serviceVersion = null,
                ),
            protocolVersion = "4.0",
        )

    private fun generateSelfSignedCert(): X509Certificate {
        val keyPair =
            KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val now = Date()
        val certBuilder =
            JcaX509v3CertificateBuilder(
                X500Name("CN=test"),
                BigInteger.ONE,
                now,
                Date(now.time + 86400000),
                X500Name("CN=test"),
                keyPair.public,
            )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        return JcaX509CertificateConverter().getCertificate(certBuilder.build(signer))
    }

    private fun generateKeyStoreFile(password: String): KeystoreEnv {
        val keyPair =
            KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val now = Date()
        val certBuilder =
            JcaX509v3CertificateBuilder(
                X500Name("CN=test"),
                BigInteger.ONE,
                now,
                Date(now.time + 86400000),
                X500Name("CN=test"),
                keyPair.public,
            )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val cert = JcaX509CertificateConverter().getCertificate(certBuilder.build(signer))

        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, password.toCharArray())
        keyStore.setKeyEntry("key", keyPair.private, password.toCharArray(), arrayOf(cert))

        val file = tempDir.resolve("keystore-${System.nanoTime()}.p12")
        FileOutputStream(file.toFile()).use { keyStore.store(it, password.toCharArray()) }
        return KeystoreEnv(location = file.toUri(), type = "pkcs12", password = Sensitive(password))
    }

    private fun generateTrustStoreFile(password: String): KeystoreEnv {
        val cert = generateSelfSignedCert()

        val trustStore = KeyStore.getInstance("PKCS12")
        trustStore.load(null, password.toCharArray())
        trustStore.setCertificateEntry("ca", cert)

        val file = tempDir.resolve("truststore-${System.nanoTime()}.p12")
        FileOutputStream(file.toFile()).use { trustStore.store(it, password.toCharArray()) }
        return KeystoreEnv(location = file.toUri(), type = "pkcs12", password = Sensitive(password))
    }

    private fun mockSslSession(): SSLSession =
        mock<SSLSession>().also {
            whenever(it.peerCertificates)
                .thenThrow(SSLPeerUnverifiedException("no peer certificates"))
        }

    @Test
    fun `sslConfiguration does nothing when certificate check is enabled but stores are null`() {
        val env = createXroadEnv(httpClientCertificateCheck = true)
        val customize = sslConfiguration(env)

        val builder = OkHttpClient.Builder()
        customize(builder)
        val client = builder.build()

        // The default hostname verifier rejects arbitrary hostnames
        assertFalse(client.hostnameVerifier.verify("any.host", mockSslSession()))
    }

    @Test
    fun `sslConfiguration configures trust-all-certs when certificate check is disabled`() {
        val env = createXroadEnv(httpClientCertificateCheck = false)
        val customize = sslConfiguration(env)

        val builder = OkHttpClient.Builder()
        customize(builder)
        val client = builder.build()

        assertTrue(client.hostnameVerifier.verify("any.host", mockSslSession()))

        // Trust-all-certs accepts any certificate without validation
        val trustManager = client.x509TrustManager!!
        assertEquals(0, trustManager.acceptedIssuers.size)
        val untrustedCert = generateSelfSignedCert()
        trustManager.checkServerTrusted(arrayOf(untrustedCert), "RSA")
    }

    @Test
    fun `sslConfiguration configures certificate check when enabled with stores present`() {
        val keyStoreEnv = generateKeyStoreFile("testpass")
        val trustStoreEnv = generateTrustStoreFile("trustpass")
        val env =
            createXroadEnv(
                httpClientCertificateCheck = true,
                keyStore = keyStoreEnv,
                trustStore = trustStoreEnv,
            )
        val customize = sslConfiguration(env)

        val builder = OkHttpClient.Builder()
        customize(builder)
        val client = builder.build()

        assertTrue(client.hostnameVerifier.verify("any.host", mockSslSession()))

        // Certificate check only trusts certificates from the provided trust store
        val trustManager = client.x509TrustManager!!
        assertContains(1..Int.MAX_VALUE, trustManager.acceptedIssuers.size)
        val untrustedCert = generateSelfSignedCert()
        assertThrows<CertificateException> {
            trustManager.checkServerTrusted(arrayOf(untrustedCert), "RSA")
        }
    }
}
