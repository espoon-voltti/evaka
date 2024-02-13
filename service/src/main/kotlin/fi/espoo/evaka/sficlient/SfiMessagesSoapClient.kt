// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import fi.espoo.evaka.SfiEnv
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.sficlient.soap.ArrayOfKohdeWS2A
import fi.espoo.evaka.sficlient.soap.ArrayOfTiedosto
import fi.espoo.evaka.sficlient.soap.Asiakas
import fi.espoo.evaka.sficlient.soap.KohdeWS2A
import fi.espoo.evaka.sficlient.soap.KyselyWS2A
import fi.espoo.evaka.sficlient.soap.LahetaViesti
import fi.espoo.evaka.sficlient.soap.LahetaViestiResponse
import fi.espoo.evaka.sficlient.soap.ObjectFactory
import fi.espoo.evaka.sficlient.soap.Osoite
import fi.espoo.evaka.sficlient.soap.Tiedosto
import fi.espoo.evaka.sficlient.soap.Viranomainen
import fi.espoo.evaka.sficlient.soap.Yhteyshenkilo
import fi.espoo.evaka.shared.domain.europeHelsinki
import fi.espoo.voltti.logging.loggers.error
import fi.espoo.voltti.logging.loggers.info
import java.time.LocalDate
import java.time.ZoneId
import java.util.GregorianCalendar
import javax.net.ssl.TrustManagerFactory
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar
import mu.KotlinLogging
import org.apache.commons.text.StringEscapeUtils
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.wss4j.common.crypto.Merlin
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.FaultAwareWebServiceMessage
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.client.WebServiceFaultException
import org.springframework.ws.client.core.FaultMessageResolver
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.SoapMessage
import org.springframework.ws.soap.client.SoapFaultClientException
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor
import org.springframework.ws.transport.http.HttpsUrlConnectionMessageSender
import org.springframework.xml.transform.StringResult
import org.springframework.xml.transform.TransformerFactoryUtils

private const val MESSAGE_API_VERSION = "1.1"

class SfiMessagesSoapClient(
    private val sfiEnv: SfiEnv,
    private val getDocument: (bucketName: String, key: String) -> Document
) : SfiMessagesClient {
    private val wsTemplate =
        WebServiceTemplate().apply {
            defaultUri = sfiEnv.address

            val jaxb2Marshaller =
                Jaxb2Marshaller().apply { setPackagesToScan(ObjectFactory::class.java.packageName) }
            marshaller = jaxb2Marshaller
            unmarshaller = jaxb2Marshaller

            // Unlike with X-Road (in pis-service), there are errors that are not logged if the HTTP
            // state
            // is not trusted. So leaving setCheckConnectionForFault() to the default
            setMessageSender(
                HttpsUrlConnectionMessageSender().apply {
                    setTrustManagers(
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                            .apply { init(sfiEnv.trustStore.load()) }
                            .trustManagers
                    )

                    // We skip FQDN matching to cert CN/subject alternative names and just trust the
                    // certificate.
                    // The trust store must only contain end-entity certificates (no CA
                    // certificates)
                    // Via API has no public DNS so there is no CN/alt name to verify against.
                    //     - VIA API has known IPs which should be set to /etc/hosts and then the
                    // NoopVerifier should be removed
                    setHostnameVerifier(NoopHostnameVerifier())
                }
            )

            faultMessageResolver = SfiFaultMessageResolver()

            interceptors =
                arrayOf(
                    Wss4jSecurityInterceptor().apply {
                        if (sfiEnv.keyStore != null) {
                            setSecurementActions(
                                "${WSHandlerConstants.SIGNATURE} ${WSHandlerConstants.TIMESTAMP}"
                            )
                            // The security token reference in the example
                            // https://esuomi.fi/palveluntarjoajille/viestit/tekninen-aineisto/
                            // is a BinarySecurityToken instead of the default IssuerSerial
                            // http://docs.oasis-open.org/wss-m/wss/v1.1.1/os/wss-x509TokenProfile-v1.1.1-os.html#_Toc118727693
                            setSecurementSignatureKeyIdentifier(
                                SignatureKeyIdentifier.DIRECT_REFERENCE.value
                            )
                            // sign body (the default) and the timestamp
                            setSecurementSignatureParts(
                                listOf(SignatureParts.SOAP_BODY, SignatureParts.TIMESTAMP)
                                    .toPartsExpression()
                            )

                            setSecurementUsername(sfiEnv.signingKeyAlias)
                            setSecurementPassword(sfiEnv.keyStore.password?.value)
                            setSecurementSignatureCrypto(
                                Merlin().apply {
                                    this.keyStore =
                                        sfiEnv.keyStore.load().also {
                                            check(it.containsAlias(sfiEnv.signingKeyAlias)) {
                                                "Suomi.fi Messages API key store is configured but doesn't contain a key with alias \"${sfiEnv.signingKeyAlias}\""
                                            }
                                        }
                                }
                            )
                        } else {
                            setSecurementActions(WSHandlerConstants.TIMESTAMP)
                        }

                        setSecurementMustUnderstand(false)
                        // the above example sets TTL at 60s
                        setSecurementTimeToLive(500)
                    }
                )
        }

    private val logger = KotlinLogging.logger {}

    override fun send(msg: SfiMessage) {
        val pdfBytes = getDocument(msg.documentBucket, msg.documentKey).bytes

        logger.info(
            mapOf("meta" to mapOf("caseId" to msg.documentId, "messageId" to msg.messageId))
        ) {
            "Sending SFI message about ${msg.documentId} with messageId: ${msg.messageId}"
        }

        val request =
            LahetaViesti().apply {
                viranomainen =
                    Viranomainen().apply {
                        sanomaTunniste = msg.messageId
                        sanomaVarmenneNimi = sfiEnv.certificateCommonName
                        viranomaisTunnus = sfiEnv.authorityIdentifier
                        palveluTunnus = sfiEnv.serviceIdentifier
                        sanomaVersio = MESSAGE_API_VERSION
                        yhteyshenkilo =
                            Yhteyshenkilo().apply {
                                nimi = sfiEnv.contactPerson.name
                                matkapuhelin = sfiEnv.contactPerson.phone
                                sahkoposti = sfiEnv.contactPerson.email
                            }
                        osoite =
                            Osoite().apply {
                                nimi = sfiEnv.contactOrganization.name
                                lahiosoite = sfiEnv.contactOrganization.streetAddress
                                postinumero = sfiEnv.contactOrganization.postalCode
                                postitoimipaikka = sfiEnv.contactOrganization.postOffice
                                maa = "FI"
                            }
                    }
                kysely =
                    KyselyWS2A().apply {
                        isLahetaTulostukseen = sfiEnv.printing.enabled
                        tulostustoimittaja = sfiEnv.printing.printingProvider
                        isPaperi = sfiEnv.printing.forcePrintForElectronicUser
                        laskutus =
                            KyselyWS2A.Laskutus().apply {
                                tunniste = sfiEnv.printing.billingId
                                salasana =
                                    sfiEnv.printing.billingPassword?.value?.takeIf {
                                        it.isNotBlank()
                                    }
                            }
                        kohteet =
                            ArrayOfKohdeWS2A().apply {
                                kohde +=
                                    KohdeWS2A().apply {
                                        isLisaaOsoitesivu = true
                                        viranomaisTunniste = msg.documentId
                                        nimeke = msg.messageHeader
                                        kuvausTeksti =
                                            StringEscapeUtils.unescapeJava(msg.messageContent)
                                        lahetysPvm = LocalDate.now(europeHelsinki).toXmlGregorian()
                                        lahettajaNimi = "eVaka"
                                        tiedostot =
                                            ArrayOfTiedosto().apply {
                                                tiedosto +=
                                                    Tiedosto().apply {
                                                        tiedostonKuvaus =
                                                            stripFileExtension(
                                                                msg.documentDisplayName
                                                            )
                                                        tiedostoMuoto = "application/pdf"
                                                        tiedostoNimi = msg.documentDisplayName
                                                        tiedostoSisalto = pdfBytes
                                                    }
                                            }
                                        asiakas +=
                                            Asiakas().apply {
                                                asiakasTunnus = msg.ssn
                                                tunnusTyyppi = "SSN"
                                                osoite =
                                                    Osoite().apply {
                                                        nimi = "${msg.lastName} ${msg.firstName}"
                                                        lahiosoite = msg.streetAddress
                                                        postinumero = msg.postalCode
                                                        postitoimipaikka = msg.postOffice
                                                        maa = msg.countryCode
                                                    }
                                            }
                                        if (msg.emailHeader != null && msg.emailContent != null) {
                                            emailLisatietoOtsikko = msg.emailHeader
                                            emailLisatietoSisalto = msg.emailContent
                                        }
                                    }
                            }
                    }
            }

        val soapResponse: LahetaViestiResponse =
            try {
                wsTemplate.marshalSendAndReceiveAsType(request)
            } catch (e: Exception) {
                throw Exception(
                    "Error while sending SFI request about ${msg.documentId} with messageId: ${msg.messageId}",
                    e
                )
            }

        val response = SfiResponse.from(soapResponse)
        if (response.code == SfiResponseCode.Success) {
            logger.info(
                mapOf(
                    "meta" to
                        mapOf(
                            "caseId" to msg.documentId,
                            "messageId" to msg.messageId,
                            "response" to response.text
                        )
                )
            ) {
                "Successfully sent SFI message about ${msg.documentId} with messageId: ${msg.messageId} response: ${response.text}"
            }
        } else if (
            response.code == SfiResponseCode.ValidationError &&
                response.text.startsWith(
                    "Asian tietosisällössä virheitä. Viranomaistunnisteella löytyy jo asia, joka on tallennettu asiakkaan tilille Viestit-palveluun"
                )
        ) {
            logger.info {
                "SFI message delivery failed with ${response.code}: ${response.text}. Skipping duplicate message"
            }
        } else if (response.code == SfiResponseCode.AccountNotFoundError) {
            logger.info {
                "SFI message delivery failed with ${response.code}: ${response.text}. " +
                    "This is to be expected when the recipient does not have an SFI account."
            }
        } else {
            throw RuntimeException(
                "SFI message delivery failed with code ${response.code}: ${response.text}"
            )
        }
    }

    private inline fun <reified T> WebServiceTemplate.marshalSendAndReceiveAsType(request: Any): T =
        marshalSendAndReceive(request).let {
            it as? T
                ?: throw IllegalStateException("Unexpected SFI response type : ${it.javaClass}")
        }
}

private enum class SignatureKeyIdentifier(val value: String) {
    ISSUER_SERIAL("IssuerSerial"),
    DIRECT_REFERENCE("DirectReference")
}

private enum class SignatureParts(namespace: String, element: String) {
    SOAP_BODY(namespace = WSConstants.URI_SOAP11_ENV, element = WSConstants.ELEM_BODY),
    TIMESTAMP(namespace = WSConstants.WSU_NS, element = WSConstants.TIMESTAMP_TOKEN_LN),
    BINARY_TOKEN(namespace = WSConstants.WSSE_NS, element = WSConstants.BINARY_TOKEN_LN);

    val part: String = "{}{$namespace}$element"
}

private fun List<SignatureParts>.toPartsExpression(): String =
    this.map(SignatureParts::part).joinToString(separator = ";")

private class SfiFaultMessageResolver : FaultMessageResolver {
    private val logger = KotlinLogging.logger {}

    override fun resolveFault(message: WebServiceMessage) {
        when (message) {
            is SoapMessage -> {
                try {
                    val meta = mapOf("envelope" to message.transformToString())
                    logger.error(meta) {
                        "Encountered SOAP fault: ${message.faultCode}. Reason: ${message.faultReason}"
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to transform SOAP fault message to string" }
                }
                throw SoapFaultClientException(message)
            }
            is FaultAwareWebServiceMessage -> throw WebServiceFaultException(message)
            else -> throw WebServiceFaultException("Message has unknown fault: $message")
        }
    }
}

private fun SoapMessage.transformToString(): String {
    val transformer = TransformerFactoryUtils.newInstance().newTransformer()
    val result = StringResult()
    transformer.transform(this.envelope.source, result)
    return result.toString()
}

private fun LocalDate.toXmlGregorian(): XMLGregorianCalendar =
    GregorianCalendar.from(atStartOfDay(ZoneId.of("Europe/Helsinki"))).let {
        DatatypeFactory.newInstance().newXMLGregorianCalendar(it)
    }

private fun stripFileExtension(fileName: String) = fileName.substringBefore(".")

private sealed class SfiResponseCode {
    object Success : SfiResponseCode()

    object AccountNotFoundError : SfiResponseCode()

    object ValidationError : SfiResponseCode()

    data class Other(val code: Int) : SfiResponseCode()

    companion object {
        // https://palveluhallinta.suomi.fi/fi/tuki/artikkelit/5c69b9e445a7231c486dbfe6
        // Taulukko 31. LahetaViestiResponse-elementti.
        fun fromCode(code: Int) =
            when (code) {
                202 -> Success
                461 -> AccountNotFoundError
                525 -> ValidationError
                else -> Other(code)
            }
    }
}

private data class SfiResponse(val code: SfiResponseCode, val text: String) {
    companion object {
        fun from(soapResponse: LahetaViestiResponse) =
            soapResponse.lahetaViestiResult.tilaKoodi.let {
                SfiResponse(
                    code = SfiResponseCode.fromCode(it.tilaKoodi),
                    text = it.tilaKoodiKuvaus
                )
            }
    }
}
