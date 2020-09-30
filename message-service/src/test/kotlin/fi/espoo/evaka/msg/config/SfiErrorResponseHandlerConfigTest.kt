// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import fi.espoo.evaka.msg.service.sfi.DefaultSfiErrorResponseHandler
import fi.espoo.evaka.msg.service.sfi.IgnoreSpecificErrorsHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class SfiErrorResponseHandlerConfigTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SfiErrorResponseHandlerConfig::class.java))

    @Test
    fun `account error ignoring handler should be loaded in production profile in voltti-env production`() {
        contextRunner.withPropertyValues("spring.profiles.active=production", "voltti.env=prod")
            .run {
                assertThat(it).doesNotHaveBean(DefaultSfiErrorResponseHandler::class.java)
                assertThat(it).hasSingleBean(IgnoreSpecificErrorsHandler::class.java)
            }
    }

    @Test
    fun `account error ignoring handler should be loaded in voltti-env staging`() {
        contextRunner.withPropertyValues("spring.profiles.active=production", "voltti.env=staging")
            .run {
                assertThat(it).doesNotHaveBean(DefaultSfiErrorResponseHandler::class.java)
                assertThat(it).hasSingleBean(IgnoreSpecificErrorsHandler::class.java)
            }
    }

    @Test
    fun `account error ignoring handler should be loaded in voltti-env test`() {
        contextRunner.withPropertyValues("spring.profiles.active=production", "voltti.env=test")
            .run {
                assertThat(it).doesNotHaveBean(DefaultSfiErrorResponseHandler::class.java)
                assertThat(it).hasSingleBean(IgnoreSpecificErrorsHandler::class.java)
            }
    }

    @Test
    fun `account error ignoring handler should be loaded in voltti-env dev`() {
        contextRunner.withPropertyValues("spring.profiles.active=production", "voltti.env=dev")
            .run {
                assertThat(it).doesNotHaveBean(DefaultSfiErrorResponseHandler::class.java)
                assertThat(it).hasSingleBean(IgnoreSpecificErrorsHandler::class.java)
            }
    }

    @Test
    fun `nothing SFI error handling related should be loaded in dev profile`() {
        contextRunner.withPropertyValues("spring.profiles.active=dev")
            .run {
                assertThat(it).doesNotHaveBean(DefaultSfiErrorResponseHandler::class.java)
                assertThat(it).doesNotHaveBean(IgnoreSpecificErrorsHandler::class.java)
            }
    }

    @Test
    fun `nothing SFI error handling related should be loaded in local profile`() {
        contextRunner.withPropertyValues("spring.profiles.active=local", "voltti.env=local")
            .run {
                assertThat(it).doesNotHaveBean(DefaultSfiErrorResponseHandler::class.java)
                assertThat(it).doesNotHaveBean(IgnoreSpecificErrorsHandler::class.java)
            }
    }

    @Test
    fun `default handler should be loaded in sfi-dev profile`() {
        contextRunner.withPropertyValues("spring.profiles.active=sfi-dev")
            .run {
                assertThat(it).hasSingleBean(DefaultSfiErrorResponseHandler::class.java)
                assertThat(it).doesNotHaveBean(IgnoreSpecificErrorsHandler::class.java)
            }
    }
}
