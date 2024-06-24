// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.config

import fi.espoo.evaka.VtjXroadEnv
import fi.espoo.evaka.vtjclient.mapper.toClientHeader
import fi.espoo.evaka.vtjclient.mapper.toServiceHeader
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.VTJQuery
import fi.espoo.evaka.vtjclient.soap.ObjectFactory
import fi.espoo.voltti.logging.MdcKey
import jakarta.xml.bind.helpers.DefaultValidationEventHandler
import mu.KotlinLogging
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.FaultAwareWebServiceMessage
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.client.WebServiceFaultException
import org.springframework.ws.client.core.FaultMessageResolver
import org.springframework.ws.client.core.WebServiceMessageCallback
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.SoapMessage
import org.springframework.ws.soap.security.support.KeyManagersFactoryBean
import org.springframework.ws.soap.security.support.TrustManagersFactoryBean
import org.springframework.ws.transport.WebServiceMessageSender
import org.springframework.ws.transport.http.HttpsUrlConnectionMessageSender

const val SOAP_PACKAGES: String = "fi.espoo.evaka.vtjclient.soap"

@Configuration
@Profile("production", "vtj-dev", "integration-test")
class XroadSoapClientConfig {
    @Bean
    @Profile("!vtj-dev")
    fun marshaller(): Jaxb2Marshaller = Jaxb2Marshaller().apply { setPackagesToScan(SOAP_PACKAGES) }

    @Bean
    @Profile("vtj-dev")
    fun requestLoggingMarshaller(): Jaxb2Marshaller =
        Jaxb2Marshaller().apply {
            setPackagesToScan(SOAP_PACKAGES)
            setValidationEventHandler(DefaultValidationEventHandler())
        }

    @Bean
    fun wsTemplate(
        marshaller: Jaxb2Marshaller,
        xRoadEnv: VtjXroadEnv,
        messageSender: WebServiceMessageSender,
        faultMessageResolver: FaultMessageResolver
    ) = WebServiceTemplate().apply {
        setMarshaller(marshaller)
        unmarshaller = marshaller
        defaultUri = xRoadEnv.address
        // don't rely on HTTP status to indicate fault (will not work), check the message
        setCheckConnectionForFault(false)
        setMessageSender(messageSender)
        setFaultMessageResolver(faultMessageResolver)
    }

    @Bean fun soapFaultResolver(): FaultMessageResolver = XroadFaultMessageResolver()

    class XroadFaultMessageResolver : FaultMessageResolver {
        private val logger = KotlinLogging.logger {}

        override fun resolveFault(message: WebServiceMessage) {
            when (message) {
                is FaultAwareWebServiceMessage -> {
                    logger.error(
                        "Fault while doing X-Road request: ${message.faultCode}. Reason: ${message.faultReason}",
                        message
                    )
                    throw WebServiceFaultException(message)
                }
                else -> {
                    logger.error("Unknown error while doing X-Road request: \"$message\".", message)
                    throw WebServiceFaultException("Message has unknown fault: $message")
                }
            }
        }
    }

    @Bean fun vtjObjectFactory() = ObjectFactory()

    @Bean
    @ConditionalOnExpression("'\${voltti.env}' != 'prod' && '\${voltti.env}' != 'staging'")
    @ConditionalOnMissingBean(WebServiceMessageSender::class)
    fun httpsMessageSender(trustManagersFactoryBean: TrustManagersFactoryBean) =
        HttpsUrlConnectionMessageSender().apply {
            setTrustManagers(trustManagersFactoryBean.`object`)
            // We skip FQDN matching to cert CN/subject alternative names and just trust the
            // certificate.
            // The trust store must only contain end-entity certificates (no CA certificates)
            // TODO: Either keep using single certs or fix the certs and host names for security
            // servers
            setHostnameVerifier(NoopHostnameVerifier())
        }

    @Bean
    @ConditionalOnExpression("'\${voltti.env}' == 'prod' || '\${voltti.env}' == 'staging'")
    @ConditionalOnMissingBean(WebServiceMessageSender::class)
    fun httpsClientAuthMessageSender(
        trustManagersFactoryBean: TrustManagersFactoryBean,
        keyManagersFactoryBean: KeyManagersFactoryBean
    ) = HttpsUrlConnectionMessageSender().apply {
        setTrustManagers(trustManagersFactoryBean.`object`)
        setKeyManagers(keyManagersFactoryBean.`object`)
        // We skip FQDN matching to cert CN/subject alternative names and just trust the
        // certificate.
        // The trust store must only contain end-entity certificates (no CA certificates)
        // TODO: Either keep using single certs or fix the certs and host names for security
        // servers
        setHostnameVerifier(NoopHostnameVerifier())
    }

    @Bean
    fun vtjSoapHeaderCallback(
        xRoadEnv: VtjXroadEnv,
        factory: ObjectFactory,
        marshaller: Jaxb2Marshaller
    ): SoapRequestAdapter =
        object : SoapRequestAdapter {
            override fun createCallback(query: VTJQuery) =
                WebServiceMessageCallback {
                    val marsh = marshaller.jaxbContext.createMarshaller()
                    val headerResult = (it as SoapMessage).soapHeader.result

                    marsh.apply {
                        marshal(factory.createId(MdcKey.TRACE_ID.get() ?: ""), headerResult)
                        marshal(factory.createIssue(""), headerResult)
                        marshal(factory.createUserId(query.requestingUserId.toString()), headerResult)
                        marshal(factory.createProtocolVersion(xRoadEnv.protocolVersion), headerResult)
                        marshal(xRoadEnv.client.toClientHeader(), headerResult)
                        marshal(xRoadEnv.service.toServiceHeader(), headerResult)
                    }
                }
        }
}

interface SoapRequestAdapter {
    fun createCallback(query: VTJQuery): WebServiceMessageCallback
}
