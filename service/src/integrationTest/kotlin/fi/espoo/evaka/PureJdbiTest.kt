// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.shared.config.getTestDataSource
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.evaka.shared.dev.resetDatabase
import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import javax.sql.DataSource
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class PureJdbiTest(private val resetDbBeforeEach: Boolean) {
    protected lateinit var dataSource: DataSource
    protected lateinit var jdbi: Jdbi
    protected lateinit var db: Database.Connection
    protected val noopTracer: Tracer = NoopTracerFactory.create()

    @BeforeAll
    fun initializeJdbi() {
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
