// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import fi.espoo.evaka.msg.config.SignatureKeyIdentifier.DIRECT_REFERENCE
import fi.espoo.evaka.msg.config.SignatureParts.SOAP_BODY
import fi.espoo.evaka.msg.config.SignatureParts.TIMESTAMP
import fi.espoo.evaka.msg.config.SoapCryptoConfig.ClientInterceptors.Companion.empty
import fi.espoo.evaka.msg.config.SoapCryptoConfig.ClientInterceptors.Companion.single
import fi.espoo.evaka.msg.properties.SfiSoapProperties
import org.apache.wss4j.dom.WSConstants.BINARY_TOKEN_LN
import org.apache.wss4j.dom.WSConstants.ELEM_BODY
import org.apache.wss4j.dom.WSConstants.TIMESTAMP_TOKEN_LN
import org.apache.wss4j.dom.WSConstants.URI_SOAP11_ENV
import org.apache.wss4j.dom.WSConstants.WSSE_NS
import org.apache.wss4j.dom.WSConstants.WSU_NS
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Component
import org.springframework.ws.client.support.interceptor.ClientInterceptor
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor
import org.springframework.ws.soap.security.wss4j2.support.CryptoFactoryBean

enum class SignatureKeyIdentifier(val value: String) {
    ISSUER_SERIAL("IssuerSerial"), DIRECT_REFERENCE("DirectReference")
}

enum class SignatureParts(namespace: String, element: String) {
    SOAP_BODY(namespace = URI_SOAP11_ENV, element = ELEM_BODY),
    TIMESTAMP(namespace = WSU_NS, element = TIMESTAMP_TOKEN_LN),
    BINARY_TOKEN(namespace = WSSE_NS, element = BINARY_TOKEN_LN);

    val part: String = "{}{$namespace}$element"
}

fun List<SignatureParts>.toPartsExpression(): String = this.map(SignatureParts::part).joinToString(separator = ";")

@Configuration
@Profile("production", "sfi-dev")
class SoapCryptoConfig(private val helper: CryptoBuildHelper) {

    data class ClientInterceptors(val interceptors: List<ClientInterceptor>) {
        companion object {
            fun single(interceptor: ClientInterceptor) = ClientInterceptors(listOf(interceptor))
            fun empty() = ClientInterceptors(emptyList())
        }
    }

    @Bean
    @Profile("production")
    @ConditionalOnExpression("'\${voltti.env}' == 'prod' || '\${voltti.env}' == 'staging'")
    fun authInterceptors(cryptoFactory: CryptoFactoryBean, properties: SfiSoapProperties): ClientInterceptors =
        helper.securityInterceptor(cryptoFactory, properties).let(::single)

    @Bean
    @Profile("sfi-dev")
    // If you want to test unauthenticated messages locally,
    // switch sfi-dev to use an empty interceptor list without dependencies
    fun devAuthInterceptors(cryptoFactory: CryptoFactoryBean, properties: SfiSoapProperties): ClientInterceptors =
        helper.securityInterceptor(cryptoFactory, properties).let(::single)

    @Bean
    @Profile("production")
    @ConditionalOnExpression("'\${voltti.env}' != 'prod' && '\${voltti.env}' != 'staging'")
    fun noAuthInterceptors(): ClientInterceptors = empty()

    @Bean
    @Profile("production")
    @ConditionalOnExpression("'\${voltti.env}' == 'prod' || '\${voltti.env}' == 'staging'")
    fun prodCryptoFactory(properties: SfiSoapProperties) = helper.cryptoFactory(properties)

    @Bean
    @Profile("sfi-dev")
    // If you want to test unauthenticated messages locally,
    // switch sfi-dev to use an empty interceptor list
    fun devCryptoFactory(properties: SfiSoapProperties) = helper.cryptoFactory(properties)
}

// This class exists to make testing the configurations easier, as the beans try to load keystores and such
@Component
@Profile("production", "sfi-dev")
class CryptoBuildHelper {
    fun cryptoFactory(properties: SfiSoapProperties): CryptoFactoryBean = CryptoFactoryBean().apply {
        val keyStore = checkNotNull(properties.keyStore.location) {
            "SFI client authentication key store location is not set"
        }
        setKeyStoreLocation(UrlResource(keyStore))
        setKeyStorePassword(properties.keyStore.password)
        setKeyStoreType(properties.keyStore.type)
    }

    fun securityInterceptor(cryptoFactory: CryptoFactoryBean, properties: SfiSoapProperties): Wss4jSecurityInterceptor =
        Wss4jSecurityInterceptor().apply {
            setSecurementActions("${WSHandlerConstants.SIGNATURE} ${WSHandlerConstants.TIMESTAMP}")
            setSecurementUsername(properties.keyStore.signingKeyAlias)
            setSecurementMustUnderstand(false)

            // The security token reference in the example https://esuomi.fi/palveluntarjoajille/viestit/tekninen-aineisto/
            // is a BinarySecurityToken instead of the default IssuerSerial
            // http://docs.oasis-open.org/wss-m/wss/v1.1.1/os/wss-x509TokenProfile-v1.1.1-os.html#_Toc118727693
            setSecurementSignatureKeyIdentifier(DIRECT_REFERENCE.value)

            // the above example sets TTL at 60s
            setSecurementTimeToLive(500)
            // sign body (the default) and the timestamp
            setSecurementSignatureParts(listOf(SOAP_BODY, TIMESTAMP).toPartsExpression())

            setSecurementPassword(properties.keyStore.password)
            setSecurementSignatureCrypto(cryptoFactory.`object`)
        }
}
