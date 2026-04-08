// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core

import evaka.core.shared.config.getTestDataSource
import evaka.core.shared.db.Database
import evaka.core.shared.db.configureJdbi
import evaka.core.shared.dev.resetDatabase
import evaka.core.shared.noopTracer
import io.opentelemetry.api.trace.Tracer
import javax.sql.DataSource
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class PureJdbiTest(private val resetDbBeforeEach: Boolean) {
    protected lateinit var dataSource: DataSource
    protected lateinit var jdbi: Jdbi
    protected lateinit var db: Database.Connection
    protected val noopTracer: Tracer = noopTracer()

    protected fun dbInstance(): Database = Database(jdbi, noopTracer)

    @BeforeAll
    open fun beforeAll() {
        (LoggerFactory.getLogger("evaka.core") as ch.qos.logback.classic.Logger).level =
            ch.qos.logback.classic.Level.DEBUG
        dataSource = getTestDataSource()
        jdbi = configureJdbi(Jdbi.create(dataSource))
        db = Database(jdbi, noopTracer).connectWithManualLifecycle()
        if (!resetDbBeforeEach) {
            db.transaction { it.resetDatabase() }
        }
    }

    @AfterAll
    fun closeJdbi() {
        db.close()
    }

    @BeforeEach
    fun resetBeforeTest() {
        if (resetDbBeforeEach) {
            db.transaction { it.resetDatabase() }
        }
    }
}
