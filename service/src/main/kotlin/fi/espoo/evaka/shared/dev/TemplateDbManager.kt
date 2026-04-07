// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.dev

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fi.espoo.evaka.shared.config.SwappableDataSource
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.evaka.shared.noopTracer
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.jdbi.v3.core.Jdbi

private val logger = KotlinLogging.logger {}

class TemplateDbManager(
    private val swappableDataSource: SwappableDataSource,
    private val jdbi: Jdbi,
    private val flywayUsername: String,
    private val flywayPassword: String,
    private val jdbcUrl: String,
) {
    private val counter = AtomicInteger(0)
    private val baseJdbcUrl = jdbcUrl.substringBeforeLast('/')
    private val baseDbName = jdbcUrl.substringAfterLast('/')
    private val templateDbName = "${baseDbName}_template"

    private val mgmtJdbi =
        configureJdbi(
            Jdbi.create(
                HikariDataSource(
                    HikariConfig().apply {
                        jdbcUrl = "$baseJdbcUrl/postgres"
                        username = flywayUsername
                        password = flywayPassword
                        maximumPoolSize = 2
                        poolName = "evaka-template-db-manager"
                    }
                )
            )
        )

    // Separate Database instance for management operations
    private fun mgmt() = Database(mgmtJdbi, noopTracer())

    private val currentDbName = AtomicReference(baseDbName)

    private data class PreparedDb(val name: String, val pool: HikariDataSource)

    private var nextDb: CompletableFuture<PreparedDb>? = null

    fun resetToTemplate() {
        ensureInitialized

        // Wait for the pre-created database (blocks if still being created)
        val prepared = nextDb!!.join()

        val oldPool = swappableDataSource.swap(prepared.pool)
        val oldDbName = currentDbName.getAndSet(prepared.name)

        // Clean up old db in the background
        CompletableFuture.runAsync { dropOldDb(oldPool, oldDbName) }

        // Start pre-creating the next database
        nextDb = preCreateNextDb()
    }

    fun resetToOriginal() {
        val oldPool = swappableDataSource.swap(jdbcUrl)
        val oldDbName = currentDbName.getAndSet(baseDbName)
        CompletableFuture.runAsync { dropOldDb(oldPool, oldDbName) }
    }

    // Kotlin's lazy is thread-safe, so this can be called from multiple threads
    private val ensureInitialized by lazy { initialize() }

    private fun initialize() {
        logger.info { "Creating template database '$templateDbName' from '$baseDbName'" }

        mgmt().connect { dbc ->
            // Drop stale databases from previous runs
            val dbNames =
                dbc.read { tx ->
                    tx.createQuery {
                            sql(
                                """
SELECT datname FROM pg_database
WHERE
    datname LIKE ${bind("${baseDbName}_%")} AND
    datname != ${bind(templateDbName)}
"""
                            )
                        }
                        .toList<String>()
                }

            for (name in dbNames) {
                dbc.executeOutsideTransaction {
                    sql("DROP DATABASE IF EXISTS \"$name\" WITH (FORCE)")
                }
            }
            dbc.executeOutsideTransaction {
                sql("DROP DATABASE IF EXISTS \"$templateDbName\" WITH (FORCE)")
            }

            // Close the connection pool so there are no active connections to the source DB
            swappableDataSource.close()
            dbc.executeOutsideTransaction {
                sql("CREATE DATABASE \"$templateDbName\" TEMPLATE \"$baseDbName\"")
            }
            // Reopen the connection pool (swap with same URL creates a fresh pool)
            swappableDataSource.swap(jdbcUrl)

            // Reset the template database to a clean state
            val templatePool =
                HikariDataSource(
                    HikariConfig().apply {
                        jdbcUrl = "$baseJdbcUrl/$templateDbName"
                        username = flywayUsername
                        password = flywayPassword
                        maximumPoolSize = 1
                        poolName = "evaka-template-reset"
                    }
                )
            try {
                val templateJdbi = configureJdbi(Jdbi.create(templatePool))
                Database(templateJdbi, noopTracer()).connect { templateDbc ->
                    templateDbc.transaction { tx -> tx.resetDatabase() }
                }
            } finally {
                templatePool.close()
            }

            logger.info { "Template database '$templateDbName' created successfully" }
        }

        // Pre-create the first database so the first reset is fast
        nextDb = preCreateNextDb()
    }

    private fun preCreateNextDb(): CompletableFuture<PreparedDb> =
        CompletableFuture.supplyAsync {
            val count = counter.incrementAndGet()
            val newDbName = "${baseDbName}_$count"
            mgmt().connect { dbc ->
                dbc.executeOutsideTransaction {
                    sql("CREATE DATABASE \"$newDbName\" TEMPLATE \"$templateDbName\"")
                }
            }

            // Create the pool now so connections are warm by the time reset needs it
            val newJdbcUrl = "$baseJdbcUrl/$newDbName"
            val pool = swappableDataSource.createPool(newJdbcUrl)
            logger.info { "Pre-created database '$newDbName' with warm pool" }
            PreparedDb(newDbName, pool)
        }

    private fun dropOldDb(oldPool: HikariDataSource, oldDbName: String) {
        try {
            oldPool.close()
            if (oldDbName != baseDbName) {
                mgmt().connect { dbc ->
                    dbc.executeOutsideTransaction {
                        sql("DROP DATABASE \"$oldDbName\" WITH (FORCE)")
                    }
                }
                logger.info { "Cleaned up old database '$oldDbName'" }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to clean up old database '$oldDbName'" }
        }
    }
}
