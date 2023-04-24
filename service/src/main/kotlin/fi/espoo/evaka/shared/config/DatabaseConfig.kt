// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fi.espoo.evaka.DatabaseEnv
import fi.espoo.evaka.shared.db.configureJdbi
import java.lang.ref.WeakReference
import java.sql.Connection
import java.sql.ConnectionBuilder
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.sql.DataSource
import kotlin.concurrent.withLock
import org.flywaydb.core.Flyway
import org.flywaydb.core.internal.database.postgresql.PostgreSQLConfigurationExtension
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.Slf4JSqlLogger
import org.postgresql.Driver
import org.postgresql.PGProperty
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class DatabaseConfig {
    @Bean
    fun jdbi(dataSource: DataSource, env: DatabaseEnv) =
        configureJdbi(Jdbi.create(dataSource)).apply {
            if (env.logSql) {
                setSqlLogger(Slf4JSqlLogger(LoggerFactory.getLogger("fi.espoo.evaka.sql")))
            }
        }

    @Profile("production")
    @Bean
    fun dataSource(env: DatabaseEnv): DataSource =
        connectionPool(env, pgDataSource(env)).also { flyway(env, it).migrate() }

    @Profile("local")
    @Bean
    fun devDataSource(env: DatabaseEnv): DataSource = DevDataSource.create(env)
}

private fun pgDataSource(env: DatabaseEnv) =
    PGSimpleDataSource().apply {
        setUrl(env.url)
        user = env.username
        password = env.password.value
        setProperty(
            PGProperty.SOCKET_TIMEOUT,
            TimeUnit.SECONDS.convert(15, TimeUnit.MINUTES).toString()
        )
    }

private fun connectionPool(env: DatabaseEnv, dataSource: DataSource) =
    HikariDataSource(
        HikariConfig().apply {
            connectionInitSql =
                "SET SESSION statement_timeout = '${env.defaultStatementTimeout.toMillis()}ms'"
            maximumPoolSize = env.maximumPoolSize
            leakDetectionThreshold = env.leakDetectionThreshold
            isRegisterMbeans = true
            poolName = "evaka-service"
            this.dataSource = dataSource
        }
    )

class ConnectionControl(env: DatabaseEnv) {
    private val lock = ReentrantLock()
    private val connections: MutableList<WeakReference<Connection>> = mutableListOf()
    private var isSuspended = false
    private val resumed = lock.newCondition()

    inner class TrackedDataSource(private val wrapped: DataSource) : DataSource by wrapped {
        override fun createConnectionBuilder(): ConnectionBuilder =
            throw UnsupportedOperationException()
        override fun getConnection(): Connection = track { wrapped.connection }
        override fun getConnection(username: String?, password: String?): Connection = track {
            wrapped.getConnection(username, password)
        }
    }

    val pool = connectionPool(env, TrackedDataSource(pgDataSource(env)))

    private fun track(f: () -> Connection) =
        lock.withLock {
            while (isSuspended) {
                resumed.await()
            }
            f().also { connection ->
                connections.removeIf { it.get() == null }
                connections.add(WeakReference(connection))
            }
        }

    fun suspend() = lock.withLock { isSuspended = true }

    fun resume() =
        lock.withLock {
            isSuspended = false
            resumed.signalAll()
        }

    fun closeConnections() {
        pool.hikariPoolMXBean.softEvictConnections()
        while (pool.hikariPoolMXBean.totalConnections > 0) {
            pool.hikariPoolMXBean.softEvictConnections()
            TimeUnit.MILLISECONDS.sleep(10)
        }
        lock.withLock { connections.asSequence().mapNotNull { it.get() }.forEach { it.close() } }
    }
}

private fun flyway(env: DatabaseEnv, dataSource: DataSource) =
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
        .placeholders(
            mapOf("application_user" to env.username, "migration_user" to env.flywayUsername)
        )
        .dataSource(dataSource)
        .load()

class DevDataSource
private constructor(
    private val maintenance: DatabaseMaintenance,
    private val connectionControl: ConnectionControl
) : DataSource by connectionControl.pool, AutoCloseable by connectionControl.pool {
    fun resetDatabase() =
        try {
            connectionControl.suspend()
            connectionControl.closeConnections()
            maintenance.recreateAppDatabase()
        } finally {
            connectionControl.resume()
        }

    companion object {
        fun create(env: DatabaseEnv): DevDataSource {
            val maintenance = DatabaseMaintenance(env, "evaka0")
            maintenance.ensureTemplateExists()
            maintenance.runMigrations()
            return DevDataSource(maintenance, ConnectionControl(env))
        }
    }
}

private class DatabaseMaintenance(
    private val env: DatabaseEnv,
    private val templateName: String,
) {
    private val parts = PostgresUrlParts.parse(env.url)
    private val appUsername = env.username
    private val databaseName = parts.databaseName
    private val postgres =
        configureJdbi(
            Jdbi.create(
                HikariDataSource(
                    HikariConfig().apply {
                        this.maximumPoolSize = 1
                        this.minimumIdle = 1
                        this.dataSource = dataSource(parts.copy(databaseName = "postgres"))
                    }
                )
            )
        )
    private val template = dataSource(parts.copy(databaseName = templateName))

    private fun dataSource(parts: PostgresUrlParts) =
        parts.createDataSource(env.flywayUsername, env.flywayPassword.value)

    fun ensureTemplateExists() {
        postgres.open().use {
            if (!it.databaseExists(templateName)) {
                it.execute("CREATE DATABASE $templateName IS_TEMPLATE TRUE")
            }
        }
        Jdbi.create(template).open().use {
            it.execute("GRANT ALL PRIVILEGES ON SCHEMA public TO $appUsername")
        }
    }
    fun runMigrations() {
        val appFlyway = flyway(env, dataSource(parts))
        val templateFlyway = flyway(env, template)
        postgres.open().use {
            if (it.databaseExists(databaseName)) {
                appFlyway.migrate()
                templateFlyway.migrate()
            } else {
                templateFlyway.migrate()
                it.execute("CREATE DATABASE $databaseName WITH TEMPLATE $templateName")
                appFlyway.migrate()
            }
        }
    }

    fun recreateAppDatabase() {
        postgres.open().use {
            it.execute("DROP DATABASE IF EXISTS $databaseName")
            it.execute("CREATE DATABASE $databaseName WITH TEMPLATE $templateName")
        }
    }
}

private data class PostgresUrlParts(val host: String, val port: Int, val databaseName: String) {
    fun createDataSource(username: String, password: String): DataSource =
        PGSimpleDataSource().also {
            it.serverNames = arrayOf(host)
            it.portNumbers = intArrayOf(port)
            it.databaseName = databaseName
            it.user = username
            it.password = password
        }

    companion object {
        fun parse(url: String): PostgresUrlParts =
            Driver.parseURL(url, null)?.let { props ->
                PostgresUrlParts(
                    host = PGProperty.PG_HOST.getOrDefault(props) ?: return@let null,
                    port = PGProperty.PG_PORT.getInt(props),
                    databaseName = PGProperty.PG_DBNAME.getOrDefault(props) ?: return@let null
                )
            }
                ?: error("Failed to parse JDBC url $url")
    }
}

private fun Handle.databaseExists(name: String): Boolean =
    createQuery("SELECT EXISTS (SELECT 1 FROM pg_database WHERE datname = :name)")
        .bind("name", name)
        .mapTo(Boolean::class.java)
        .one()
