// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import fi.espoo.evaka.msg.properties.SfiSoapProperties
import fi.espoo.evaka.msg.sficlient.soap.ObjectFactory
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.wss4j.common.crypto.Merlin
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
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
import javax.net.ssl.TrustManagerFactory

const val SOAP_PACKAGES: String = "fi.espoo.evaka.msg.sficlient.soap"

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

@Configuration
@Profile("production", "sfi-dev")
class SfiSoapClientConfig {
    @Bean
    fun wsTemplate(
        env: Environment,
        sfiProperties: SfiSoapProperties,
    ) = WebServiceTemplate()
        .apply {
            defaultUri = sfiProperties.address

            val jaxb2Marshaller = Jaxb2Marshaller().apply {
                setPackagesToScan(SOAP_PACKAGES)
            }
            marshaller = jaxb2Marshaller
            unmarshaller = jaxb2Marshaller

            // Unlike with X-Road (in pis-service), there are errors that are not logged if the HTTP state
            // is not trusted. So leaving setCheckConnectionForFault() to the default
            setMessageSender(
                HttpsUrlConnectionMessageSender().apply {
                    val keyStore = KeyStore.getInstance(sfiProperties.trustStore.type).apply {
                        val location = checkNotNull(sfiProperties.trustStore.location) {
                            "SFI messages API " +
                                "trust store location is not set"
                        }
                        UrlResource(location).inputStream.use { load(it, sfiProperties.trustStore.password?.toCharArray()) }
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

            val wsSecurityEnabled = sfiProperties.wsSecurityEnabled ?: when (env.getProperty("voltti.env")) {
                "prod" -> true
                "staging" -> true
                else -> false
            }

            if (wsSecurityEnabled) {
                interceptors = arrayOf(
                    Wss4jSecurityInterceptor().apply {
                        setSecurementActions("${WSHandlerConstants.SIGNATURE} ${WSHandlerConstants.TIMESTAMP}")
                        setSecurementUsername(sfiProperties.keyStore.signingKeyAlias)
                        setSecurementMustUnderstand(false)

                        // The security token reference in the example https://esuomi.fi/palveluntarjoajille/viestit/tekninen-aineisto/
                        // is a BinarySecurityToken instead of the default IssuerSerial
                        // http://docs.oasis-open.org/wss-m/wss/v1.1.1/os/wss-x509TokenProfile-v1.1.1-os.html#_Toc118727693
                        setSecurementSignatureKeyIdentifier(SignatureKeyIdentifier.DIRECT_REFERENCE.value)

                        // the above example sets TTL at 60s
                        setSecurementTimeToLive(500)
                        // sign body (the default) and the timestamp
                        setSecurementSignatureParts(listOf(SignatureParts.SOAP_BODY, SignatureParts.TIMESTAMP).toPartsExpression())

                        setSecurementPassword(sfiProperties.keyStore.password)
                        setSecurementSignatureCrypto(
                            Merlin().apply {
                                keyStore = KeyStore.getInstance(sfiProperties.keyStore.type).apply {
                                    val location = checkNotNull(sfiProperties.keyStore.location) {
                                        "SFI client authentication key store location is not set"
                                    }
                                    UrlResource(location).inputStream.use { load(it, sfiProperties.keyStore.password?.toCharArray()) }
                                }
                            }
                        )
                    }
                )
            }
        }

    class SfiFaultMessageResolver : FaultMessageResolver {

        override fun resolveFault(message: WebServiceMessage) {
            when (message) {
                is FaultAwareWebServiceMessage -> throw WebServiceFaultException(message)
                else -> throw WebServiceFaultException("Message has unknown fault: $message")
            }
        }
    }

    @Bean
    fun sfiObjectFactory() = ObjectFactory()
}
