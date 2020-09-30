// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.config.JdbcTestConfiguration
import fi.espoo.evaka.shared.db.withSpringHandle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import javax.sql.DataSource

@JdbcTest
@Import(JdbcTestConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class JdbcIntegrationTest {
    @Autowired
    protected lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @MockBean
    protected lateinit var asyncJobRunnerMock: AsyncJobRunner

    @Autowired
    protected lateinit var dataSource: DataSource

    @BeforeEach
    private fun setUp() {
        withSpringHandle(dataSource) {
            resetDatabase(it)
            insertGeneralTestFixtures(it)
        }
    }

    @AfterEach
    private fun tearDown() {
        try {
            withSpringHandle(dataSource, ::resetDatabase)
        } catch (e: Throwable) {
            // ignore
        }
    }
}
