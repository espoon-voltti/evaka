// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Component

@TestConfiguration
@EnableAsync
@PropertySource("classpath:integration-test-common.properties")
@Import(SfiIntegrationTestConfig::class, TestDataSourceProperties::class)
class IntegrationTestConfig

@Component
@ConfigurationProperties(prefix = "voltti.datasource")
data class TestDataSourceProperties(val url: String = PostgresContainer.getInstance().jdbcUrl)
