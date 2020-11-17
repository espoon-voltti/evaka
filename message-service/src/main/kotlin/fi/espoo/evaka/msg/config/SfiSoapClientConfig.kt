// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import fi.espoo.evaka.msg.config.SoapCryptoConfig.ClientInterceptors
import fi.espoo.evaka.msg.properties.SfiSoapProperties
import fi.espoo.evaka.msg.sficlient.soap.ObjectFactory
import mu.KotlinLogging
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.FaultAwareWebServiceMessage
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.client.WebServiceFaultException
import org.springframework.ws.client.core.FaultMessageResolver
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.security.support.TrustManagersFactoryBean
import org.springframework.ws.transport.http.HttpsUrlConnectionMessageSender

const val SOAP_PACKAGES: String = "fi.espoo.evaka.msg.sficlient.soap"

@Configuration
@Profile("production", "sfi-dev")
class SfiSoapClientConfig {

    @Bean
    fun marshaller(): Jaxb2Marshaller = Jaxb2Marshaller().apply {
        setPackagesToScan(SOAP_PACKAGES)
    }

    @Bean
    fun wsTemplate(
        marshaller: Jaxb2Marshaller,
        sfiProperties: SfiSoapProperties,
        messageSender: HttpsUrlConnectionMessageSender,
        faultMessageResolver: FaultMessageResolver,
        interceptors: ClientInterceptors
    ) = WebServiceTemplate()
        .apply {
            setMarshaller(marshaller)
            unmarshaller = marshaller
            defaultUri = sfiProperties.address
            // Unlike with X-Road (in pis-service), there are errors that are not logged if the HTTP state
            // is not trusted. So leaving setCheckConnectionForFault() to the default
            setMessageSender(messageSender)
            setFaultMessageResolver(faultMessageResolver)
            if (interceptors.interceptors.isNotEmpty()) {
                setInterceptors(interceptors.interceptors.toTypedArray())
            }
        }

    @Bean
    fun soapFaultResolver(): FaultMessageResolver = SfiFaultMessageResolver()

    @Bean
    fun httpsMessageSender(trustManagersFactoryBean: TrustManagersFactoryBean) = HttpsUrlConnectionMessageSender()
        .apply {
            setTrustManagers(trustManagersFactoryBean.`object`)
            // We skip FQDN matching to cert CN/subject alternative names and just trust the certificate.
            // The trust store must only contain end-entity certificates (no CA certificates)
            // Via API has no public DNS so there is no CN/alt name to verify against.
            //     - VIA API has known IPs which should be set to /etc/hosts and then the NoopVerifier should be removed
            setHostnameVerifier(NoopHostnameVerifier())
        }

    class SfiFaultMessageResolver : FaultMessageResolver {

        private val logger = KotlinLogging.logger { }

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
