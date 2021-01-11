// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import com.auth0.jwt.algorithms.Algorithm
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component

@TestConfiguration
@Import(SfiIntegrationTestConfig::class, TestDataSourceProperties::class)
class IntegrationTestConfig {
    @Bean
    fun noJwtAlgorithm(): Algorithm = Algorithm.none()
}

@Component
@ConfigurationProperties(prefix = "voltti.datasource")
data class TestDataSourceProperties(val url: String = PostgresContainer.getInstance().jdbcUrl)
