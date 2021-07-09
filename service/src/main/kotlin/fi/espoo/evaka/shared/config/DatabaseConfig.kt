// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fi.espoo.evaka.DatabaseEnv
import fi.espoo.evaka.shared.db.configureJdbi
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

@Configuration
class DatabaseConfig {
    @Bean
    fun jdbi(dataSource: DataSource) = configureJdbi(Jdbi.create(dataSource))

    @Bean
    fun dataSource(env: DatabaseEnv): DataSource {
        Flyway.configure()
            .dataSource(env.url, env.flywayUsername, env.flywayPassword.value)
            .placeholders(
                mapOf(
                    "application_user" to env.username,
                    "migration_user" to env.flywayUsername
                )
            )
            .load()
            .run {
                migrate()
            }
        return HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = env.url
                username = env.username
                password = env.password.value
                maximumPoolSize = env.maximumPoolSize
                leakDetectionThreshold = env.leakDetectionThreshold
                addDataSourceProperty("socketTimeout", TimeUnit.SECONDS.convert(15, TimeUnit.MINUTES).toInt())
            }
        )
    }
}
