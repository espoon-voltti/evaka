// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fi.espoo.evaka.msg.config.SoapCryptoConfig.ClientInterceptors
import fi.espoo.evaka.msg.properties.SfiSoapProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor
import org.springframework.ws.soap.security.wss4j2.support.CryptoFactoryBean

class SoapCryptoConfigTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CryptoTestConfig::class.java))

    @Test
    fun `crypto factory bean and security interceptor should be loaded in production profile in voltti-env production`() {
        contextRunner.withPropertyValues("spring.profiles.active=production", "voltti.env=prod")
            .run {
                assertThat(it).hasSingleBean(CryptoFactoryBean::class.java)
                assertThat(it).hasSingleBean(ClientInterceptors::class.java)
                it.assertHasSingleConfiguredInterceptorOfType(Wss4jSecurityInterceptor::class.java)
            }
    }

    @Test
    fun `crypto factory bean and security interceptor should be loaded in voltti-env staging`() {
        contextRunner.withPropertyValues("spring.profiles.active=production", "voltti.env=staging")
            .run {
                assertThat(it).hasSingleBean(CryptoFactoryBean::class.java)
                assertThat(it).hasSingleBean(ClientInterceptors::class.java)
                it.assertHasSingleConfiguredInterceptorOfType(Wss4jSecurityInterceptor::class.java)
            }
    }

    @Test
    fun `crypto factory bean and security interceptor should NOT be loaded in voltti-env test`() {
        contextRunner.withPropertyValues("spring.profiles.active=production", "voltti.env=test")
            .run {
                assertThat(it).doesNotHaveBean(CryptoFactoryBean::class.java)
                assertThat(it).hasSingleBean(ClientInterceptors::class.java)
                it.assertHasEmptyInterceptors()
            }
    }

    @Test
    fun `crypto factory bean and security interceptor should NOT be loaded in voltti-env dev`() {
        contextRunner.withPropertyValues("spring.profiles.active=production", "voltti.env=dev")
            .run {
                assertThat(it).doesNotHaveBean(CryptoFactoryBean::class.java)
                assertThat(it).hasSingleBean(ClientInterceptors::class.java)
                it.assertHasEmptyInterceptors()
            }
    }

    @Test
    fun `nothing SOAP related should be loaded in dev profile`() {
        contextRunner.withPropertyValues("spring.profiles.active=dev")
            .run {
                assertThat(it).doesNotHaveBean(CryptoFactoryBean::class.java)
                assertThat(it).doesNotHaveBean(ClientInterceptors::class.java)
            }
    }

    @Test
    fun `nothing SOAP related should be loaded in local profile`() {
        contextRunner.withPropertyValues("spring.profiles.active=local", "voltti.env=local")
            .run {
                assertThat(it).doesNotHaveBean(CryptoFactoryBean::class.java)
                assertThat(it).doesNotHaveBean(ClientInterceptors::class.java)
            }
    }

    @Test
    fun `crypto factory bean and interceptor with private keys should be loaded in sfi-dev profile`() {
        contextRunner.withPropertyValues("spring.profiles.active=sfi-dev")
            .run {
                assertThat(it).hasSingleBean(CryptoFactoryBean::class.java)
                assertThat(it).hasSingleBean(ClientInterceptors::class.java)
                it.assertHasSingleConfiguredInterceptorOfType(Wss4jSecurityInterceptor::class.java)
            }
    }

    private fun <T> AssertableApplicationContext.assertHasSingleConfiguredInterceptorOfType(clazz: Class<T>) {
        getBean(ClientInterceptors::class.java).let {
            assertThat(it.interceptors).hasSize(1)
            assertThat(it.interceptors[0]).isExactlyInstanceOf(clazz)
        }
    }

    private fun AssertableApplicationContext.assertHasEmptyInterceptors() {
        getBean(ClientInterceptors::class.java).let {
            assertThat(it.interceptors).isEmpty()
        }
    }

    @Configuration
    @Import(SoapCryptoConfig::class)
    class CryptoTestConfig {

        val cryptoFactoryBean = CryptoFactoryBean()

        val interceptor = Wss4jSecurityInterceptor()

        @Bean
        fun soapProperties() = SfiSoapProperties()

        @Bean
        fun helper() = mock<CryptoBuildHelper>().also {
            whenever(it.cryptoFactory(any())).thenReturn(cryptoFactoryBean)
            whenever(it.securityInterceptor(any(), any())).thenReturn(interceptor)
        }
    }
}
