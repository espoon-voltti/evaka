// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.config

import fi.espoo.evaka.VtjXroadEnv
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.ws.soap.security.support.KeyManagersFactoryBean
import org.springframework.ws.soap.security.support.KeyStoreFactoryBean

@ExtendWith(MockitoExtension::class)
class KeyManagerConfigTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KeyManagerTestConfig::class.java))
            .withPropertyValues("fi.espoo.voltti.vtj.xroad.keyStore.location=file://fake")

    @Test
    fun `private key bean factory and key should be loaded in production profile in voltti-env production`() {
        contextRunner
            .withPropertyValues("spring.profiles.active=production", "voltti.env=prod")
            .run {
                assertThat(it).hasSingleBean(KeyManagersFactoryBean::class.java)
                assertThat(it).hasSingleBean(KeyStoreFactoryBean::class.java)
            }
    }

    @Test
    fun `private key bean factory and key should be loaded in voltti-env staging`() {
        contextRunner
            .withPropertyValues("spring.profiles.active=production", "voltti.env=staging")
            .run {
                assertThat(it).hasSingleBean(KeyManagersFactoryBean::class.java)
                assertThat(it).hasSingleBean(KeyStoreFactoryBean::class.java)
            }
    }

    @Test
    fun `private key bean factory and key should NOT be loaded in voltti-env test`() {
        contextRunner
            .withPropertyValues("spring.profiles.active=production", "voltti.env=test")
            .run {
                assertThat(it).doesNotHaveBean(KeyManagersFactoryBean::class.java)
                assertThat(it).doesNotHaveBean(KeyStoreFactoryBean::class.java)
            }
    }

    @Test
    fun `private key bean factory and key should NOT be loaded in voltti-env dev`() {
        contextRunner
            .withPropertyValues("spring.profiles.active=production", "voltti.env=dev")
            .run {
                assertThat(it).doesNotHaveBean(KeyManagersFactoryBean::class.java)
                assertThat(it).doesNotHaveBean(KeyStoreFactoryBean::class.java)
            }
    }

    @Test
    fun `private key bean factory and key should NOT be loaded in dev profile`() {
        contextRunner.withPropertyValues("spring.profiles.active=dev").run {
            assertThat(it).doesNotHaveBean(KeyManagersFactoryBean::class.java)
            assertThat(it).doesNotHaveBean(KeyStoreFactoryBean::class.java)
        }
    }

    @Test
    fun `private key bean factory and key should NOT be loaded in local profile`() {
        contextRunner.withPropertyValues("spring.profiles.active=local", "voltti.env=local").run {
            assertThat(it).doesNotHaveBean(KeyManagersFactoryBean::class.java)
            assertThat(it).doesNotHaveBean(KeyStoreFactoryBean::class.java)
        }
    }

    @Test
    fun `private key bean factory and key should NOT be loaded in vtj-dev profile`() {
        contextRunner.withPropertyValues("spring.profiles.active=vtj-dev").run {
            assertThat(it).doesNotHaveBean(KeyManagersFactoryBean::class.java)
            assertThat(it).doesNotHaveBean(KeyStoreFactoryBean::class.java)
        }
    }

    @Configuration
    @Import(KeyManagerConfig::class)
    class KeyManagerTestConfig {
        @Bean fun xroadEnv(env: Environment) = VtjXroadEnv.fromEnvironment(env)
    }
}
