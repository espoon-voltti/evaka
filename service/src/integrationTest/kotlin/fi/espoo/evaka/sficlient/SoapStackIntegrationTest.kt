// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import fi.espoo.evaka.KeystoreEnv
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.SfiContactOrganizationEnv
import fi.espoo.evaka.SfiContactPersonEnv
import fi.espoo.evaka.SfiEnv
import fi.espoo.evaka.SfiPrintingEnv
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.sficlient.soap.KyselyWS1
import fi.espoo.evaka.sficlient.soap.KyselyWS10
import fi.espoo.evaka.sficlient.soap.KyselyWS2
import fi.espoo.evaka.sficlient.soap.KyselyWS2A
import fi.espoo.evaka.sficlient.soap.TilaKoodiWS
import fi.espoo.evaka.sficlient.soap.VastausWS1
import fi.espoo.evaka.sficlient.soap.VastausWS10
import fi.espoo.evaka.sficlient.soap.VastausWS2
import fi.espoo.evaka.sficlient.soap.VastausWS2A
import fi.espoo.evaka.sficlient.soap.Viranomainen
import fi.espoo.evaka.sficlient.soap.Viranomaispalvelut
import java.io.InputStream
import java.net.ServerSocket
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLHandshakeException
import javax.xml.namespace.QName
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.apache.cxf.configuration.jsse.TLSServerParameters
import org.apache.cxf.endpoint.Server
import org.apache.cxf.jaxws.JaxWsServerFactoryBean
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor
import org.apache.wss4j.common.ConfigurationConstants
import org.apache.wss4j.common.crypto.Merlin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.ws.client.WebServiceFaultException
import org.springframework.ws.client.WebServiceIOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SoapStackIntegrationTest {
    private lateinit var server: MockServer<MockViranomaisPalvelut>

    private val message =
        SfiMessage(
            messageId = "message-id",
            documentId = "document-id",
            documentBucket = "bucket",
            documentKey = "key",
            documentDisplayName = "document",
            ssn = "ssn",
            firstName = "first",
            lastName = "last",
            language = "fi",
            streetAddress = "street",
            postalCode = "01234",
            postOffice = "Espoo",
            countryCode = "FI",
            messageHeader = "header",
            messageContent = "content",
            emailHeader = "email-header",
            emailContent = "email-content"
        )
    private val clientKeystore =
        KeystoreEnv(
            location = ClassPathResource("evaka-integration-test/sficlient/client.p12").uri,
            password = Sensitive("client")
        )
    private val serverKeystore =
        KeystoreEnv(
            location = ClassPathResource("evaka-integration-test/sficlient/server.p12").uri,
            password = Sensitive("localhost")
        )
    private val untrustWorthyKeystore =
        KeystoreEnv(
            location = ClassPathResource("evaka-integration-test/sficlient/untrustworthy.p12").uri,
            password = Sensitive("untrustworthy")
        )
    private val dummyContent = byteArrayOf(0x11, 0x22, 0x33, 0x44)

    private fun dummyGetDocument(bucketName: String, key: String): Document {
        assertEquals(message.documentBucket, bucketName)
        assertEquals(message.documentKey, key)
        return Document("name", dummyContent, "text/plain")
    }

    private fun defaultEnv() =
        SfiEnv(
            address = "https://localhost:${server.port}",
            trustStore = serverKeystore,
            keyStore = null,
            signingKeyAlias = "",
            authorityIdentifier = "authority",
            serviceIdentifier = "service",
            certificateCommonName = "common-name",
            printing =
                SfiPrintingEnv(
                    enabled = true,
                    forcePrintForElectronicUser = false,
                    printingProvider = "provider",
                    billingId = "billing-id",
                    billingPassword = Sensitive("billing-password")
                ),
            contactPerson =
                SfiContactPersonEnv(
                    name = "contact-name",
                    phone = "contact-phone",
                    email = "contact-email"
                ),
            contactOrganization =
                SfiContactOrganizationEnv(
                    name = "contact-organization-name",
                    streetAddress = "contact-organization-street-address",
                    postalCode = "contact-organization-postal-code",
                    postOffice = "contact-organization-post-office"
                )
        )

    @BeforeAll
    fun beforeAll() {
        val loggerCtx = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerCtx.getLogger(Logger.ROOT_LOGGER_NAME).level = Level.INFO
    }

    @BeforeEach
    fun beforeEach() {
        val serverKeys =
            ClassPathResource("evaka-integration-test/sficlient/server.p12").inputStream.use {
                CryptoKeys.load(it, password = "localhost", alias = "localhost")
            }
        val clientKeys =
            ClassPathResource("evaka-integration-test/sficlient/client.p12").inputStream.use {
                CryptoKeys.load(it, password = "client", alias = "client")
            }
        server =
            MockServer.start(
                clazz = Viranomaispalvelut::class,
                service = MockViranomaisPalvelut(),
                serverKeys = serverKeys,
                clientKeys = clientKeys
            )
    }

    @AfterEach
    fun afterEach() {
        server.close()
    }

    @Test
    fun `an incorrectly signed message fails`() {
        val client =
            SfiMessagesSoapClient(
                defaultEnv()
                    .copy(keyStore = untrustWorthyKeystore, signingKeyAlias = "untrustworthy"),
                ::dummyGetDocument
            )
        server.service.implementation.set { viranomainen, _ -> successResponse(viranomainen) }
        val exception = assertThrows<Exception> { client.send(message) }
        val cause = exception.cause as WebServiceFaultException
        assertEquals(
            QName("http://ws.apache.org/wss4j", "SecurityError"),
            cause.webServiceMessage.faultCode
        )
    }

    @Test
    fun `an exception is thrown if the server is not trusted`() {
        val client =
            SfiMessagesSoapClient(
                defaultEnv()
                    .copy(
                        keyStore = clientKeystore,
                        signingKeyAlias = "client",
                        trustStore = untrustWorthyKeystore
                    ),
                ::dummyGetDocument
            )
        server.service.implementation.set { viranomainen, _ -> successResponse(viranomainen) }
        val exception = assertThrows<Exception> { client.send(message) }
        val cause = exception.cause as WebServiceIOException
        assertTrue(cause.contains(SSLHandshakeException::class.java))
    }

    @Test
    fun `a correctly signed message is sent successfully`() {
        val env = defaultEnv().copy(keyStore = clientKeystore, signingKeyAlias = "client")
        val client = SfiMessagesSoapClient(env, ::dummyGetDocument)
        server.service.implementation.set { viranomainen, request ->
            assertEquals(message.messageId, viranomainen.sanomaTunniste)
            assertEquals(env.authorityIdentifier, viranomainen.viranomaisTunnus)
            assertEquals(env.certificateCommonName, viranomainen.sanomaVarmenneNimi)
            assertEquals(env.serviceIdentifier, viranomainen.palveluTunnus)
            assertEquals(env.contactPerson.name, viranomainen.yhteyshenkilo.nimi)
            assertEquals(env.contactPerson.email, viranomainen.yhteyshenkilo.sahkoposti)
            assertEquals(env.contactPerson.phone, viranomainen.yhteyshenkilo.matkapuhelin)

            assertEquals(env.printing.enabled, request.isLahetaTulostukseen)
            assertEquals(env.printing.forcePrintForElectronicUser, request.isPaperi)
            assertEquals(env.printing.printingProvider, request.tulostustoimittaja)

            assertEquals(env.printing.billingId, request.laskutus.tunniste)
            assertEquals(env.printing.billingPassword?.value, request.laskutus.salasana)

            assertEquals(1, request.kohteet.kohde.size)
            val kohde = request.kohteet.kohde[0]

            assertEquals(message.documentId, kohde.viranomaisTunniste)
            assertEquals(message.messageHeader, kohde.nimeke)
            assertEquals(message.messageContent, kohde.kuvausTeksti)

            assertEquals(message.emailHeader, kohde.emailLisatietoOtsikko)
            assertEquals(message.emailContent, kohde.emailLisatietoSisalto)

            assertEquals(1, kohde.asiakas.size)
            val asiakas = kohde.asiakas[0]
            assertEquals(message.ssn, asiakas.asiakasTunnus)
            assertEquals("SSN", asiakas.tunnusTyyppi)

            assertEquals("${message.lastName} ${message.firstName}", asiakas.osoite.nimi)
            assertEquals(message.streetAddress, asiakas.osoite.lahiosoite)
            assertEquals(message.postalCode, asiakas.osoite.postinumero)
            assertEquals(message.postOffice, asiakas.osoite.postitoimipaikka)
            assertEquals(message.countryCode, asiakas.osoite.maa)

            assertEquals(1, kohde.tiedostot.tiedosto.size)
            val tiedosto = kohde.tiedostot.tiedosto[0]
            assertEquals(message.documentDisplayName, tiedosto.tiedostonKuvaus)
            assertTrue(dummyContent.contentEquals(tiedosto.tiedostoSisalto))
            assertEquals("application/pdf", tiedosto.tiedostoMuoto)
            assertEquals(message.documentDisplayName, tiedosto.tiedostoNimi)

            successResponse(viranomainen)
        }
        client.send(message)
    }
}

data class CryptoKeys(val privateKey: PrivateKey, val publicCertificate: Certificate) {
    fun toKeyStore(alias: String, keyPassword: String = ""): KeyStore =
        KeyStore.getInstance("pkcs12").apply {
            load(null, null)
            setKeyEntry(alias, privateKey, keyPassword.toCharArray(), arrayOf(publicCertificate))
        }

    companion object {
        /**
         * Loads a public+private key pair from a PKCS12 key store input stream.
         *
         * The store password and key password are assumed to be the same.
         */
        fun load(inputStream: InputStream, password: String, alias: String) =
            KeyStore.getInstance("pkcs12")
                .apply { load(inputStream, password.toCharArray()) }
                .let { keyStore ->
                    val publicCertificate = keyStore.getCertificate(alias)
                    val privateKey = keyStore.getKey(alias, password.toCharArray()) as PrivateKey
                    CryptoKeys(privateKey, publicCertificate)
                }
    }
}

private fun findFreePort(): Int =
    ServerSocket().use {
        it.bind(null)
        it.localPort
    }

private const val MOCK_SERVER_CRYPTO: String = "evaka.wss4j.crypto"

class MockServer<T>(val service: T, val port: Int, private val server: Server) : AutoCloseable {
    companion object {
        fun <Service : Any, Impl : Service> start(
            clazz: KClass<Service>,
            service: Impl,
            serverKeys: CryptoKeys,
            clientKeys: CryptoKeys
        ): MockServer<Impl> {
            val port = findFreePort()
            val keyPassword = ""
            val keyStore = serverKeys.toKeyStore(alias = "localhost", keyPassword = keyPassword)
            val server =
                JaxWsServerFactoryBean()
                    .apply {
                        serviceClass = clazz.java
                        address = "https://localhost:$port"
                        serviceBean = service
                    }
                    .also {
                        val tlsParams =
                            TLSServerParameters().apply {
                                keyManagers =
                                    KeyManagerFactory.getInstance(
                                            KeyManagerFactory.getDefaultAlgorithm()
                                        )
                                        .apply { init(keyStore, keyPassword.toCharArray()) }
                                        .keyManagers
                            }
                        JettyHTTPServerEngineFactory()
                            .setTLSServerParametersForPort(port, tlsParams)
                    }
                    .create()

            val wss4j =
                WSS4JInInterceptor(
                    mapOf(
                        ConfigurationConstants.ACTION to
                            "${ConfigurationConstants.SIGNATURE} ${ConfigurationConstants.TIMESTAMP}",
                        ConfigurationConstants.SIG_VER_PROP_REF_ID to MOCK_SERVER_CRYPTO,
                        MOCK_SERVER_CRYPTO to
                            Merlin().apply { trustStore = clientKeys.toKeyStore("client") }
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

private fun successResponse(viranomainen: Viranomainen): VastausWS2A =
    VastausWS2A().apply {
        tilaKoodi =
            TilaKoodiWS().apply {
                tilaKoodi = 202
                tilaKoodiKuvaus = "Kysely on onnistunut"
                sanomaTunniste = viranomainen.sanomaTunniste
            }
    }

class MockViranomaisPalvelut : Viranomaispalvelut {
    val implementation: AtomicReference<(Viranomainen, KyselyWS2A) -> VastausWS2A> =
        AtomicReference()

    override fun lahetaViesti(viranomainen: Viranomainen, kysely: KyselyWS2A): VastausWS2A =
        (implementation.get() ?: throw NotImplementedError()).invoke(viranomainen, kysely)

    override fun haeTilaTieto(viranomainen: Viranomainen, kysely: KyselyWS10): VastausWS10 =
        throw NotImplementedError()

    override fun haeAsiakkaita(viranomainen: Viranomainen, kysely: KyselyWS1): VastausWS1 =
        throw NotImplementedError()

    override fun lisaaKohteita(viranomainen: Viranomainen, kysely: KyselyWS2): VastausWS2 =
        throw NotImplementedError()
}
