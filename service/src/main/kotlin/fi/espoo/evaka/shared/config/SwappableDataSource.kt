// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.PrintWriter
import java.sql.Connection
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger
import javax.sql.DataSource

class SwappableDataSource(initial: HikariDataSource, baseConfig: HikariConfig) : DataSource {
    private val poolCounter = AtomicInteger(1)
    private val current = AtomicReference(initial)

    // Store config independently so swap() works even after close()
    private val storedUsername = baseConfig.username
    private val storedPassword = baseConfig.password
    private val storedConnectionInitSql = baseConfig.connectionInitSql
    private val storedMaximumPoolSize = baseConfig.maximumPoolSize
    private val storedLeakDetectionThreshold = baseConfig.leakDetectionThreshold
    private val storedIsRegisterMbeans = baseConfig.isRegisterMbeans
    private val storedDataSourceProperties =
        Properties().apply { putAll(baseConfig.dataSourceProperties) }

    fun createPool(newJdbcUrl: String): HikariDataSource {
        val counter = poolCounter.getAndIncrement()
        return HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = newJdbcUrl
                username = storedUsername
                password = storedPassword
                connectionInitSql = storedConnectionInitSql
                maximumPoolSize = storedMaximumPoolSize
                leakDetectionThreshold = storedLeakDetectionThreshold
                isRegisterMbeans = storedIsRegisterMbeans
                poolName = "evaka-service-$counter"
                storedDataSourceProperties.forEach { (key, value) ->
                    addDataSourceProperty(key as String, value)
                }
            }
        )
    }

    fun swap(newPool: HikariDataSource): HikariDataSource = current.getAndSet(newPool)

    fun swap(newJdbcUrl: String): HikariDataSource = swap(createPool(newJdbcUrl))

    fun close() {
        current.get().close()
    }

    override fun getConnection(): Connection = current.get().connection

    override fun getConnection(username: String?, password: String?): Connection =
        current.get().getConnection(username, password)

    override fun getLogWriter(): PrintWriter? = current.get().logWriter

    override fun setLogWriter(out: PrintWriter?) {
        current.get().logWriter = out
    }

    override fun setLoginTimeout(seconds: Int) {
        current.get().loginTimeout = seconds
    }

    override fun getLoginTimeout(): Int = current.get().loginTimeout

    override fun getParentLogger(): Logger = current.get().parentLogger

    override fun <T : Any?> unwrap(iface: Class<T>?): T = current.get().unwrap(iface)

    override fun isWrapperFor(iface: Class<*>?): Boolean = current.get().isWrapperFor(iface)
}
