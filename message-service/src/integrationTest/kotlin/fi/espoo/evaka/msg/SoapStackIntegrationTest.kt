// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg

import fi.espoo.evaka.msg.config.CryptoBuildHelper
import fi.espoo.evaka.msg.config.SfiErrorResponseHandlerConfig
import fi.espoo.evaka.msg.config.SfiSoapClientConfig
import fi.espoo.evaka.msg.config.SoapCryptoConfig
import fi.espoo.evaka.msg.config.TrustManagerConfig
import fi.espoo.evaka.msg.mapper.SfiMapper
import fi.espoo.evaka.msg.properties.SfiMessageProperties
import fi.espoo.evaka.msg.properties.SfiPrintingProperties
import fi.espoo.evaka.msg.properties.SfiSoapProperties
import fi.espoo.evaka.msg.service.sfi.ISfiClientService
import fi.espoo.evaka.msg.service.sfi.SfiAccountDetailsService
import fi.espoo.evaka.msg.service.sfi.SfiClientService
import fi.espoo.evaka.msg.sficlient.soap.KyselyWS1
import fi.espoo.evaka.msg.sficlient.soap.KyselyWS10
import fi.espoo.evaka.msg.sficlient.soap.KyselyWS2
import fi.espoo.evaka.msg.sficlient.soap.KyselyWS2A
import fi.espoo.evaka.msg.sficlient.soap.TilaKoodiWS
import fi.espoo.evaka.msg.sficlient.soap.VastausWS1
import fi.espoo.evaka.msg.sficlient.soap.VastausWS10
import fi.espoo.evaka.msg.sficlient.soap.VastausWS2
import fi.espoo.evaka.msg.sficlient.soap.VastausWS2A
import fi.espoo.evaka.msg.sficlient.soap.Viranomainen
import fi.espoo.evaka.msg.sficlient.soap.Viranomaispalvelut
import org.apache.cxf.configuration.jsse.TLSServerParameters
import org.apache.cxf.endpoint.Server
import org.apache.cxf.jaxws.JaxWsServerFactoryBean
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor
import org.apache.wss4j.common.ConfigurationConstants
import org.apache.wss4j.common.crypto.Merlin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.ws.client.WebServiceFaultException
import org.springframework.ws.client.WebServiceIOException
import java.io.InputStream
import java.net.ServerSocket
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLHandshakeException
import javax.xml.namespace.QName
import kotlin.reflect.KClass

class SoapStackIntegrationTest {
    private lateinit var server: MockServer<MockViranomaisPalvelut>
    private lateinit var contextRunner: ApplicationContextRunner

    private val message = ISfiClientService.MessageMetadata(
        message = ISfiClientService.MessageDetails(
            messageId = null,
            uniqueCaseIdentifier = "",
            header = "",
            content = "",
            sendDate = LocalDate.of(2021, 1, 1),
            senderName = "",
            recipient = ISfiClientService.Recipient(
                ssn = "ssn",
                name = "name",
                email = "email",
                phone = "phone",
                address = ISfiClientService.RecipientAddress(
                    streetAddress = "street",
                    postalCode = "01234",
                    postOffice = "Espoo",
                )
            ),
            pdfDetails = ISfiClientService.PdfDetails(
                fileDescription = "description",
                fileName = "test.pdf",
                content = ByteArray(0)
            ),
            emailNotification = null
        )
    )

    private fun defaultSoapProperties() = SfiSoapProperties().apply {
        address = "https://localhost:${server.port}"
        trustStore.location = ClassPathResource("server.p12").uri
        trustStore.password = "localhost"
    }

    @BeforeEach
    fun beforeEach() {
        val serverKeys = ClassPathResource("server.p12").inputStream.use { CryptoKeys.load(it, password = "localhost", alias = "localhost") }
        val clientKeys = ClassPathResource("client.p12").inputStream.use { CryptoKeys.load(it, password = "client", alias = "client") }
        server = MockServer.start(clazz = Viranomaispalvelut::class, service = MockViranomaisPalvelut(), serverKeys = serverKeys, clientKeys = clientKeys)
        contextRunner = ApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(
                CryptoBuildHelper::class.java,
                SfiAccountDetailsService::class.java,
                SfiClientService::class.java,
                SfiErrorResponseHandlerConfig::class.java,
                SfiMessageProperties::class.java,
                SfiPrintingProperties::class.java,
                SfiSoapClientConfig::class.java,
                SfiMapper::class.java,
                SoapCryptoConfig::class.java,
                TrustManagerConfig::class.java,
            )
        ).withPropertyValues("spring.profiles.active=production", "voltti.env=prod")
    }

    @AfterEach
    fun afterEach() {
        server.close()
    }

    @Test
    fun `an incorrectly signed message fails`() {
        contextRunner.withBean(
            SfiSoapProperties::class.java,
            {
                defaultSoapProperties().apply {
                    keyStore.location = ClassPathResource("untrustworthy.p12").uri
                    keyStore.password = "untrustworthy"
                    keyStore.signingKeyAlias = "untrustworthy"
                }
            }
        ).run { ctx ->
            server.service.implementation.set { viranomainen, _ -> successResponse(viranomainen) }
            val client = ctx.getBean(SfiClientService::class.java)
            val exception = assertThrows<Exception> { client.sendMessage(message) }
            val cause = exception.cause as WebServiceFaultException
            assertEquals(QName("http://ws.apache.org/wss4j", "SecurityError"), cause.webServiceMessage.faultCode)
        }
    }

    @Test
    fun `an exception is thrown if the server is not trusted`() {
        contextRunner.withBean(
            SfiSoapProperties::class.java,
            {
                defaultSoapProperties().apply {
                    keyStore.location = ClassPathResource("client.p12").uri
                    keyStore.password = "client"
                    keyStore.signingKeyAlias = "client"
                    trustStore.location = ClassPathResource("untrustworthy.p12").uri
                    trustStore.password = "untrustworthy"
                }
            }
        ).run { ctx ->
            server.service.implementation.set { viranomainen, _ -> successResponse(viranomainen) }
            val client = ctx.getBean(SfiClientService::class.java)
            val exception = assertThrows<Exception> { client.sendMessage(message) }
            val cause = exception.cause as WebServiceIOException
            assertTrue(cause.contains(SSLHandshakeException::class.java))
        }
    }

    @Test
    fun `a correctly signed message is sent successfully`() {
        contextRunner.withBean(
            SfiSoapProperties::class.java,
            {
                defaultSoapProperties().apply {
                    keyStore.location = ClassPathResource("client.p12").uri
                    keyStore.password = "client"
                    keyStore.signingKeyAlias = "client"
                }
            }
        ).run { ctx ->
            server.service.implementation.set { viranomainen, request ->
                assertEquals(1, request.kohteet.kohde.size)
                val kohde = request.kohteet.kohde[0]
                assertEquals(1, kohde.asiakas.size)
                val asiakas = kohde.asiakas[0]
                assertEquals(message.message.recipient.ssn, asiakas.asiakasTunnus)
                assertEquals("SSN", asiakas.tunnusTyyppi)

                successResponse(viranomainen)
            }
            val client = ctx.getBean(SfiClientService::class.java)
            val response = client.sendMessage(message)
            assertEquals(202, response.code)
            assertTrue(response.isOkResponse())
        }
    }
}

data class CryptoKeys(val privateKey: PrivateKey, val publicCertificate: Certificate) {
    fun toKeyStore(alias: String, keyPassword: String = ""): KeyStore = KeyStore.getInstance("pkcs12").apply {
        load(null, null)
        setKeyEntry(alias, privateKey, keyPassword.toCharArray(), arrayOf(publicCertificate))
    }

    companion object {
        /**
         * Loads a public+private key pair from a PKCS12 key store input stream.
         *
         * The store password and key password are assumed to be the same.
         */
        fun load(inputStream: InputStream, password: String, alias: String) = KeyStore.getInstance("pkcs12").apply {
            load(inputStream, password.toCharArray())
        }.let { keyStore ->
            val publicCertificate = keyStore.getCertificate(alias)
            val privateKey = keyStore.getKey(alias, password.toCharArray()) as PrivateKey
            CryptoKeys(privateKey, publicCertificate)
        }
    }
}

private fun findFreePort(): Int = ServerSocket().use {
    it.bind(null)
    it.localPort
}

private const val MOCK_SERVER_CRYPTO: String = "evaka.wss4j.crypto"
class MockServer<T>(val service: T, val port: Int, private val server: Server) : AutoCloseable {
    companion object {
        fun <Service : Any, Impl : Service> start(clazz: KClass<Service>, service: Impl, serverKeys: CryptoKeys, clientKeys: CryptoKeys): MockServer<Impl> {
            val port = findFreePort()
            val keyPassword = ""
            val keyStore = serverKeys.toKeyStore(alias = "localhost", keyPassword = keyPassword)
            val server = JaxWsServerFactoryBean().apply {
                serviceClass = clazz.java
                address = "https://localhost:$port"
                serviceBean = service
            }.also {
                val tlsParams = TLSServerParameters().apply {
                    keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
                        init(keyStore, keyPassword.toCharArray())
                    }.keyManagers
                }
                JettyHTTPServerEngineFactory().setTLSServerParametersForPort(port, tlsParams)
            }.create()

            val wss4j = WSS4JInInterceptor(
                mapOf(
                    ConfigurationConstants.ACTION to "${ConfigurationConstants.SIGNATURE} ${ConfigurationConstants.TIMESTAMP}",
                    ConfigurationConstants.SIG_VER_PROP_REF_ID to MOCK_SERVER_CRYPTO,
                    MOCK_SERVER_CRYPTO to Merlin().apply {
                        trustStore = clientKeys.toKeyStore("client")
                    }
                )
            )

            server.endpoint.inInterceptors += wss4j
            return MockServer(service, port, server)
        }
    }

    override fun close() {
        server.destroy()
        JettyHTTPServerEngineFactory.destroyForPort(port)
    }
}

private fun successResponse(viranomainen: Viranomainen): VastausWS2A = VastausWS2A().apply {
    tilaKoodi = TilaKoodiWS().apply {
        tilaKoodi = 202
        tilaKoodiKuvaus = "Kysely on onnistunut"
        sanomaTunniste = viranomainen.sanomaTunniste
    }
}

class MockViranomaisPalvelut : Viranomaispalvelut {
    val implementation: AtomicReference<(Viranomainen, KyselyWS2A) -> VastausWS2A> = AtomicReference()

    override fun lahetaViesti(viranomainen: Viranomainen, kysely: KyselyWS2A): VastausWS2A = (implementation.get() ?: throw NotImplementedError()).invoke(viranomainen, kysely)
    override fun haeTilaTieto(viranomainen: Viranomainen, kysely: KyselyWS10): VastausWS10 = throw NotImplementedError()
    override fun haeAsiakkaita(viranomainen: Viranomainen, kysely: KyselyWS1): VastausWS1 = throw NotImplementedError()
    override fun lisaaKohteita(viranomainen: Viranomainen, kysely: KyselyWS2): VastausWS2 = throw NotImplementedError()
}
