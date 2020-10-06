// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.jackson2.Jackson2Config
import org.jdbi.v3.jackson2.Jackson2Plugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

@Configuration
class DatabaseConfig {
    @Bean
    fun jdbi(dataSource: DataSource) = Jdbi.create(dataSource).apply {
        installPlugin(KotlinPlugin())
        installPlugin(PostgresPlugin())
        installPlugin(Jackson2Plugin())
        getConfig(Jackson2Config::class.java).mapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(Jdk8Module())
            .registerModule(ParameterNamesModule())
            .registerModule(KotlinModule())
    }

    @Bean
    fun dataSource(env: Environment): DataSource {
        val dataSourceUsername = env["voltti.datasource.username"]
        val flywayUsername = env["voltti.flyway.username"]
        Flyway.configure()
            .dataSource(env["voltti.datasource.url"], flywayUsername, env["voltti.flyway.password"])
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
                jdbcUrl = env["voltti.datasource.url"]
                username = dataSourceUsername
                password = env["voltti.datasource.password"]
                leakDetectionThreshold = TimeUnit.MINUTES.convert(1, TimeUnit.MILLISECONDS)
            }
        )
    }
}
