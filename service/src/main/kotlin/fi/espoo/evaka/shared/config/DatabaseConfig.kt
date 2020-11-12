// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fi.espoo.evaka.shared.db.DatabaseSpringSupport
import fi.espoo.evaka.shared.db.configureJdbi
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.core.env.getProperty
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

@Configuration
@Import(DatabaseSpringSupport::class)
class DatabaseConfig {
    @Bean
    fun jdbi(dataSource: DataSource) = configureJdbi(Jdbi.create(dataSource))

    @Bean
    fun dataSource(env: Environment): DataSource {
        val dataSourceUrl = env.getRequiredProperty("spring.datasource.url")
        val dataSourceUsername = env.getRequiredProperty("spring.datasource.username")
        val flywayUsername = env.getRequiredProperty("flyway.username")
        Flyway.configure()
            .dataSource(dataSourceUrl, flywayUsername, env.getRequiredProperty("flyway.password"))
            .placeholders(
                mapOf(
                    "application_user" to dataSourceUsername,
                    "migration_user" to flywayUsername
                )
            )
            .load()
            .run {
                migrate()
            }
        return HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = dataSourceUrl
                username = dataSourceUsername
                password = env.getRequiredProperty("spring.datasource.password")
                maximumPoolSize = 20
                leakDetectionThreshold = env.getProperty<Long>("spring.datasource.hikari.leak-detection-threshold") ?: 0
                addDataSourceProperty("socketTimeout", TimeUnit.SECONDS.convert(15, TimeUnit.MINUTES).toInt())
            }
        )
    }
}
