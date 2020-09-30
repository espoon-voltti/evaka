// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg

import fi.espoo.evaka.msg.config.IntegrationTestConfig
import fi.espoo.evaka.msg.config.PostgresContainer
import fi.espoo.evaka.msg.utils.DisableSecurity
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.core.env.MapPropertySource
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@DisableSecurity
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Import(IntegrationTestConfig::class)
@ContextConfiguration(initializers = [PostgresContainerInitializer::class])
abstract class AbstractIntegrationTest {

    @LocalServerPort
    var port: Int = 0
}

class PostgresContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        applicationContext.environment.propertySources.addLast(
            MapPropertySource(
                "PostgresContainerInitializer",
                mapOf("voltti.datasource.url" to PostgresContainer.getInstance().jdbcUrl)
            )
        )
    }
}
