// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.config

import fi.espoo.evaka.VtjXroadEnv
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.AutoConfigurations.of
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.security.support.KeyManagersFactoryBean
import org.springframework.ws.soap.security.support.TrustManagersFactoryBean
import org.springframework.ws.transport.http.HttpsUrlConnectionMessageSender

@ExtendWith(MockitoExtension::class)
class XroadSoapClientConfigTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(of(CommonSoapClientTestConfig::class.java))
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withPropertyValues("fi.espoo.voltti.vtj.xroad.keyStore.location=file://fake")

    @Test
    fun `all soap https and client authentication beans should be loaded in production profile in voltti-env production`() {
        contextRunner
            .withPropertyValues("spring.profiles.active=production", "voltti.env=prod")
            .withConfiguration(of(PrivateKeyBeansProvidingTestConfig::class.java))
            .run {
                assertThat(it).hasSingleBean(HttpsUrlConnectionMessageSender::class.java)
                assertThat(it).hasSingleBean(Jaxb2Marshaller::class.java)
                assertThat(it).hasSingleBean(WebServiceTemplate::class.java)
                verify(it.mockKeyBean).`object`
                verify(it.mockTrustBean).`object`
            }
    }

    @Test
    fun `all soap https and client authentication beans should be loaded in voltti-env staging`() {
        contextRunner
            .withPropertyValues("spring.profiles.active=production", "voltti.env=staging")
            .withConfiguration(of(PrivateKeyBeansProvidingTestConfig::class.java))
            .run {
                assertThat(it).hasSingleBean(HttpsUrlConnectionMessageSender::class.java)
                assertThat(it).hasSingleBean(Jaxb2Marshaller::class.java)
                assertThat(it).hasSingleBean(WebServiceTemplate::class.java)
                verify(it.mockKeyBean).`object`
                verify(it.mockTrustBean).`object`
            }
    }

    @Test
    fun `just soap https beans without client authentication beans should be loaded in voltti-env test`() {
        contextRunner
            .withPropertyValues("spring.profiles.active=production", "voltti.env=test")
            .run {
                assertThat(it).hasSingleBean(HttpsUrlConnectionMessageSender::class.java)
                assertThat(it).hasSingleBean(Jaxb2Marshaller::class.java)
                assertThat(it).hasSingleBean(WebServiceTemplate::class.java)
                verify(it.mockTrustBean).`object`
            }
    }

    @Test
    fun `just soap https beans without client authentication beans should be loaded in voltti-env dev`() {
        contextRunner
            .withPropertyValues("spring.profiles.active=production", "voltti.env=dev")
            .run {
                assertThat(it).hasSingleBean(HttpsUrlConnectionMessageSender::class.java)
                assertThat(it).hasSingleBean(Jaxb2Marshaller::class.java)
                assertThat(it).hasSingleBean(WebServiceTemplate::class.java)
                verify(it.mockTrustBean).`object`
            }
    }

    @Test
    fun `no soap beans at all should be loaded in dev profile`() {
        contextRunner.withPropertyValues("spring.profiles.active=dev").run {
            assertThat(it).doesNotHaveBean(HttpsUrlConnectionMessageSender::class.java)
            assertThat(it).doesNotHaveBean(Jaxb2Marshaller::class.java)
            assertThat(it).doesNotHaveBean(WebServiceTemplate::class.java)
        }
    }

    @Test
    fun `no soap beans at all should be loaded in local profile`() {
        contextRunner.withPropertyValues("spring.profiles.active=local", "voltti.env=local").run {
            assertThat(it).doesNotHaveBean(HttpsUrlConnectionMessageSender::class.java)
            assertThat(it).doesNotHaveBean(Jaxb2Marshaller::class.java)
            assertThat(it).doesNotHaveBean(WebServiceTemplate::class.java)
        }
    }

    @Test
    fun `just soap https beans without client authentication beans should be loaded in vtj-dev profile`() {
        contextRunner.withPropertyValues("spring.profiles.active=vtj-dev").run {
            assertThat(it).hasSingleBean(HttpsUrlConnectionMessageSender::class.java)
            assertThat(it).hasSingleBean(Jaxb2Marshaller::class.java)
            assertThat(it).hasSingleBean(WebServiceTemplate::class.java)
            verify(it.mockTrustBean).`object`
        }
    }

    // this is glue to enable verifying the mock interactions while also using the application
    // context runner to load
    // the config. perhaps there's a cleaner way to test configs with mocks?

    private val AssertableApplicationContext.mockKeyBean: KeyManagersFactoryBean
        get() = getBean(PrivateKeyBeansProvidingTestConfig::class.java).keyBean

    private val AssertableApplicationContext.mockTrustBean: TrustManagersFactoryBean
        get() = getBean(CommonSoapClientTestConfig::class.java).trustBean

    @Configuration
    @Import(CommonSoapClientTestConfig::class)
    class PrivateKeyBeansProvidingTestConfig {
        lateinit var keyBean: KeyManagersFactoryBean

        @Bean
        fun keyManagers() =
            mock<KeyManagersFactoryBean>().apply {
                whenever(this.`object`).thenReturn(arrayOf(mock()))
                keyBean = this
            }
    }

    @Configuration
    @Import(XroadSoapClientConfig::class)
    class CommonSoapClientTestConfig {
        lateinit var trustBean: TrustManagersFactoryBean

        @Bean
        fun trustManagers() =
            mock<TrustManagersFactoryBean>().apply {
                lenient().`when`(this.`object`).thenReturn(arrayOf(mock()))
                trustBean = this
            }

        @Bean fun xroadEnv(env: Environment) = VtjXroadEnv.fromEnvironment(env)
    }
}
