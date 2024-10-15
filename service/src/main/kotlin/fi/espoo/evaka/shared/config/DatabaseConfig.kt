// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fi.espoo.evaka.DatabaseEnv
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension
import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

val logger = mu.KotlinLogging.logger {}

@Configuration
class DatabaseConfig {
    @Bean
    fun jdbi(dataSource: DataSource, env: DatabaseEnv) =
        configureJdbi(Jdbi.create(dataSource)).apply {
            if (env.logSql) {
                setSqlLogger(Database.sqlLogger)
            }
        }

    @Bean
    fun dataSource(env: DatabaseEnv): DataSource {
        Flyway.configure()
            .apply {
                pluginRegister
                    .getPlugin(PostgreSQLConfigurationExtension::class.java)
                    .isTransactionalLock = false
            }
            .ignoreMigrationPatterns(
                if (env.flywayIgnoreFutureMigrations) {
                    "*:future"
                } else {
                    ""
                }
            )
            .validateMigrationNaming(true)
            .dataSource(env.url, env.flywayUsername, env.flywayPassword.value)
            .locations(*env.flywayLocations.toTypedArray())
            .placeholders(
                mapOf("application_user" to env.username, "migration_user" to env.flywayUsername)
            )
            .load()
            .run { migrate() }

        logger.info { "Sleeping for 30 minutes to emulate a long database migration..." }
        Thread.sleep(Duration.ofMinutes(30))

        return HikariDataSource(
            HikariConfig().apply {
                connectionInitSql =
                    "SET SESSION statement_timeout = '${env.defaultStatementTimeout.toMillis()}ms'"
                jdbcUrl = env.url
                username = env.username
                password = env.password.value
                maximumPoolSize = env.maximumPoolSize
                leakDetectionThreshold = env.leakDetectionThreshold
                isRegisterMbeans = true
                poolName = "evaka-service"
                addDataSourceProperty(
                    "socketTimeout",
                    TimeUnit.SECONDS.convert(15, TimeUnit.MINUTES).toInt(),
                )
            }
        )
    }
}
