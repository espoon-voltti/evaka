// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

class DevDataInitializerTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DevDataInitializerTestConfig::class.java))

    @Test
    fun `dev data initializer should NOT be loaded without profiles`() {
        contextRunner
            .run { assertThat(it).doesNotHaveBean(DevDataInitializer::class.java) }
    }

    @Test
    fun `dev data initializer should NOT be loaded with espoo_evaka profile`() {
        contextRunner
            .withPropertyValues("spring.profiles.active=espoo_evaka")
            .run { assertThat(it).doesNotHaveBean(DevDataInitializer::class.java) }
    }

    @Test
    fun `dev data initializer should NOT be loaded with local profile`() {
        contextRunner
            .withPropertyValues("spring.profiles.active=local")
            .run { assertThat(it).doesNotHaveBean(DevDataInitializer::class.java) }
    }

    @Test
    fun `dev data initializer should be loaded with espoo_evaka and local profile`() {
        contextRunner
            .withPropertyValues("spring.profiles.active=espoo_evaka,local")
            .run { assertThat(it).hasSingleBean(DevDataInitializer::class.java) }
    }
}

@Configuration
@Import(DevDataInitializer::class)
class DevDataInitializerTestConfig {
    @Bean
    fun jdbi() = mock<Jdbi>(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
}
