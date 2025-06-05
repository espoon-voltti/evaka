// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.system.exitProcess
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger {}

@Configuration
class MigrationRunnerConfig {
    @Bean
    fun migrationRunner(jdbi: Jdbi): CommandLineRunner = CommandLineRunner {
        val runMigrationOnly = System.getenv("RUN_MIGRATION_ONLY")?.toBoolean() ?: false
        if (runMigrationOnly) {
            try {
                jdbi.withHandle<Int, Exception> { handle ->
                    handle.createQuery("SELECT 1").mapTo(Int::class.java).one()
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to run test query" }
                exitProcess(1)
            }

            logger.debug { "RUN_MIGRATION_ONLY=true on. Exit process." }
            exitProcess(0)
        }
    }
}
