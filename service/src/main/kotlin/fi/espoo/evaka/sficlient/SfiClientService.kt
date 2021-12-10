// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import fi.espoo.evaka.SfiEnv
import fi.espoo.evaka.sficlient.ISfiClientService.MessageMetadata
import fi.espoo.evaka.sficlient.soap.ArrayOfKohdeWS2A
import fi.espoo.evaka.sficlient.soap.ArrayOfTiedosto
import fi.espoo.evaka.sficlient.soap.Asiakas
import fi.espoo.evaka.sficlient.soap.KohdeWS2A
import fi.espoo.evaka.sficlient.soap.KyselyWS2A
import fi.espoo.evaka.sficlient.soap.LahetaViesti
import fi.espoo.evaka.sficlient.soap.LahetaViestiResponse
import fi.espoo.evaka.sficlient.soap.Osoite
import fi.espoo.evaka.sficlient.soap.Tiedosto
import fi.espoo.evaka.sficlient.soap.Viranomainen
import fi.espoo.evaka.sficlient.soap.Yhteyshenkilo
import fi.espoo.voltti.logging.loggers.info
import mu.KotlinLogging
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.wss4j.common.crypto.Merlin
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.springframework.core.io.UrlResource
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.FaultAwareWebServiceMessage
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.client.WebServiceFaultException
import org.springframework.ws.client.core.FaultMessageResolver
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor
import org.springframework.ws.transport.http.HttpsUrlConnectionMessageSender
import java.security.KeyStore
import java.time.LocalDate
import java.time.ZoneId
import java.util.GregorianCalendar
import java.util.UUID.randomUUID
import javax.net.ssl.TrustManagerFactory
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

const val SOAP_PACKAGES: String = "fi.espoo.evaka.sficlient.soap"

private enum class SignatureKeyIdentifier(val value: String) {
    ISSUER_SERIAL("IssuerSerial"), DIRECT_REFERENCE("DirectReference")
}

private enum class SignatureParts(namespace: String, element: String) {
    SOAP_BODY(namespace = WSConstants.URI_SOAP11_ENV, element = WSConstants.ELEM_BODY),
    TIMESTAMP(namespace = WSConstants.WSU_NS, element = WSConstants.TIMESTAMP_TOKEN_LN),
    BINARY_TOKEN(namespace = WSConstants.WSSE_NS, element = WSConstants.BINARY_TOKEN_LN);

    val part: String = "{}{$namespace}$element"
}

private fun List<SignatureParts>.toPartsExpression(): String = this.map(SignatureParts::part).joinToString(separator = ";")

class SfiClientService(private val sfiEnv: SfiEnv) : ISfiClientService {
    private val wsTemplate = WebServiceTemplate().apply {
        defaultUri = sfiEnv.address

        val jaxb2Marshaller = Jaxb2Marshaller().apply {
            setPackagesToScan(SOAP_PACKAGES)
        }
        marshaller = jaxb2Marshaller
        unmarshaller = jaxb2Marshaller

        // Unlike with X-Road (in pis-service), there are errors that are not logged if the HTTP state
        // is not trusted. So leaving setCheckConnectionForFault() to the default
        setMessageSender(
            HttpsUrlConnectionMessageSender().apply {
                val keyStore = KeyStore.getInstance(sfiEnv.trustStore.type).apply {
                    val location = checkNotNull(sfiEnv.trustStore.location) {
                        "SFI messages API " +
                            "trust store location is not set"
                    }
                    UrlResource(location).inputStream.use { load(it, sfiEnv.trustStore.password.value.toCharArray()) }
                }
                setTrustManagers(
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                        init(keyStore)
                    }.trustManagers
                )

                // We skip FQDN matching to cert CN/subject alternative names and just trust the certificate.
                // The trust store must only contain end-entity certificates (no CA certificates)
                // Via API has no public DNS so there is no CN/alt name to verify against.
                //     - VIA API has known IPs which should be set to /etc/hosts and then the NoopVerifier should be removed
                setHostnameVerifier(NoopHostnameVerifier())
            }
        )

        faultMessageResolver = SfiFaultMessageResolver()

        if (sfiEnv.wsSecurityEnabled) {
            interceptors = arrayOf(
                Wss4jSecurityInterceptor().apply {
                    setSecurementActions("${WSHandlerConstants.SIGNATURE} ${WSHandlerConstants.TIMESTAMP}")
                    setSecurementUsername(sfiEnv.signingKeyAlias)
                    setSecurementMustUnderstand(false)

                    // The security token reference in the example https://esuomi.fi/palveluntarjoajille/viestit/tekninen-aineisto/
                    // is a BinarySecurityToken instead of the default IssuerSerial
                    // http://docs.oasis-open.org/wss-m/wss/v1.1.1/os/wss-x509TokenProfile-v1.1.1-os.html#_Toc118727693
                    setSecurementSignatureKeyIdentifier(SignatureKeyIdentifier.DIRECT_REFERENCE.value)

                    // the above example sets TTL at 60s
                    setSecurementTimeToLive(500)
                    // sign body (the default) and the timestamp
                    setSecurementSignatureParts(listOf(SignatureParts.SOAP_BODY, SignatureParts.TIMESTAMP).toPartsExpression())

                    setSecurementPassword(sfiEnv.keyStore.password.value)
                    setSecurementSignatureCrypto(
                        Merlin().apply {
                            keyStore = KeyStore.getInstance(sfiEnv.keyStore.type).apply {
                                val location = checkNotNull(sfiEnv.keyStore.location) {
                                    "SFI client authentication key store location is not set"
                                }
                                UrlResource(location).inputStream.use { load(it, sfiEnv.keyStore.password.value.toCharArray()) }
                            }
                        }
                    )
                }
            )
        }
    }

    private val logger = KotlinLogging.logger {}

    override fun sendMessage(metadata: MessageMetadata): SfiResponse {
        val uniqueSfiMessageId = metadata.message.messageId ?: randomUUID().toString()
        val caseId = metadata.message.uniqueCaseIdentifier

        logger.info(
            mapOf(
                "meta" to mapOf(
                    "caseId" to caseId,
                    "messageId" to uniqueSfiMessageId
                )
            )
        ) { "Sending SFI message about $caseId with messageId: $uniqueSfiMessageId" }

        val request = LahetaViesti()
            .apply {
                viranomainen = getAuthorityDetails(uniqueSfiMessageId)
                kysely = createQueryWithPrintingDetails()
                kysely.kohteet = metadata.message.toMessageTargets()
            }

        val soapResponse: LahetaViestiResponse = try {
            wsTemplate.marshalSendAndReceiveAsType(request)
        } catch (e: Exception) {
            throw Exception("Error while sending SFI request about $caseId with messageId: $uniqueSfiMessageId", e)
        }

        return soapResponse.let(SfiResponse.Mapper::from)
            .let {
                if (it.isOkResponse()) {
                    logger.info(
                        mapOf(
                            "meta" to mapOf(
                                "caseId" to caseId,
                                "messageId" to uniqueSfiMessageId,
                                "response" to it.text
                            )
                        )
                    ) { "Successfully sent SFI message about $caseId with messageId: $uniqueSfiMessageId response: ${it.text}" }
                } else {
                    if (it.code == ERROR_VALIDATION && it.text == "Asian tietosisällössä virheitä. Viranomaistunnisteella löytyy jo asia, joka on tallennettu asiakkaan tilille Viestit-palveluun") {
                        logger.info {
                            "SFI message delivery failed with ${it.code}: ${it.text}. Skipping duplicate message"
                        }
                    } else if (it.code == ERROR_SFI_ACCOUNT_NOT_FOUND) {
                        logger.info {
                            "SFI message delivery failed with ${it.code}: ${it.text}. " +
                                "This is to be expected when the recipient does not have an SFI account."
                        }
                    } else {
                        throw SfiErrorResponseHandler.SFiMessageDeliveryException("SFI message delivery failed with code ${it.code}: ${it.text}")
                    }
                }
                it
            }
    }

    private inline fun <reified T> WebServiceTemplate.marshalSendAndReceiveAsType(request: Any): T =
        marshalSendAndReceive(request)
            .let {
                it as? T ?: throw IllegalStateException("Unexpected SFI response type : ${it.javaClass}")
            }

    private fun getAuthorityDetails(uniqueSfiMessageId: String): Viranomainen = Viranomainen().apply {
        // Should add trace parsing or create new trace, currently, there is none
        sanomaTunniste = uniqueSfiMessageId
        sanomaVarmenneNimi = sfiEnv.message.certificateCommonName
        viranomaisTunnus = sfiEnv.message.authorityIdentifier
        palveluTunnus = sfiEnv.message.serviceIdentifier
        sanomaVersio = sfiEnv.message.messageApiVersion
        yhteyshenkilo = createContactPerson()
    }

    private fun createQueryWithPrintingDetails(): KyselyWS2A = KyselyWS2A().apply {
        isLahetaTulostukseen = sfiEnv.printing.enabled
        tulostustoimittaja = sfiEnv.printing.printingProvider
        isPaperi = sfiEnv.printing.forcePrintForElectronicUser
        laskutus = createBillingDetails()
    }

    private fun createBillingDetails() = KyselyWS2A.Laskutus().apply {
        tunniste = sfiEnv.printing.billingId
        salasana = when {
            sfiEnv.printing.billingPassword.isNullOrEmpty() -> null
            else -> sfiEnv.printing.billingPassword
        }
    }

    private fun createContactPerson() = Yhteyshenkilo().apply {
        nimi = sfiEnv.printing.contactPersonName
        matkapuhelin = sfiEnv.printing.contactPersonPhone
        sahkoposti = sfiEnv.printing.contactPersonEmail
    }
}

private class SfiFaultMessageResolver : FaultMessageResolver {
    override fun resolveFault(message: WebServiceMessage) {
        when (message) {
            is FaultAwareWebServiceMessage -> throw WebServiceFaultException(message)
            else -> throw WebServiceFaultException("Message has unknown fault: $message")
        }
    }
}

private fun LocalDate.toXmlGregorian(): XMLGregorianCalendar =
    GregorianCalendar.from(atStartOfDay(ZoneId.of("Europe/Helsinki")))
        .let {
            DatatypeFactory.newInstance().newXMLGregorianCalendar(it)
        }

private fun ISfiClientService.MessageDetails.toMessageTargets() = ArrayOfKohdeWS2A().apply {
    KohdeWS2A().also {
        it.viranomaisTunniste = uniqueCaseIdentifier
        it.nimeke = header
        it.kuvausTeksti = content
        it.lahetysPvm = sendDate.toXmlGregorian()
        it.lahettajaNimi = senderName
        it.tiedostot = pdfDetails.toAttachments()
        it.asiakas.add(recipient.toAsiakas())
        emailNotification?.let { email ->
            it.emailLisatietoOtsikko = email.header
            it.emailLisatietoSisalto = email.message
        }
    }.let(kohde::add)
}

private fun ISfiClientService.PdfDetails.toAttachments() = ArrayOfTiedosto().apply {
    Tiedosto()
        .also {
            it.tiedostonKuvaus = fileDescription
            it.tiedostoMuoto = fileType
            it.tiedostoNimi = fileName
            it.tiedostoSisalto = content
        }.let(tiedosto::add)
}

private fun ISfiClientService.Recipient.toAsiakas() =
    Asiakas().also {
        it.asiakasTunnus = ssn
        it.tunnusTyyppi = "SSN"
        it.osoite = Osoite()
            .also { o ->
                o.nimi = name
                o.lahiosoite = address.streetAddress
                o.postinumero = address.postalCode
                o.postitoimipaikka = address.postOffice
                o.maa = "FI"
            }
    }
