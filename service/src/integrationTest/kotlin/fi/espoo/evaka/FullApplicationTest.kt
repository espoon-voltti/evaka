// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.config.SharedIntegrationTestConfig
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.config.getTestDataSource
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.vtjclient.VtjIntegrationTestConfig
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import io.opentelemetry.api.trace.Tracer
import java.net.URL
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.env.Environment
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [SharedIntegrationTestConfig::class, VtjIntegrationTestConfig::class],
)
abstract class FullApplicationTest(private val resetDbBeforeEach: Boolean) {
    @LocalServerPort protected var httpPort: Int = 0

    protected val jsonMapper: JsonMapper =
        defaultJsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    private lateinit var jdbi: Jdbi

    @Autowired protected lateinit var env: Environment

    @MockitoSpyBean protected lateinit var evakaEnv: EvakaEnv

    @Autowired protected lateinit var tracer: Tracer

    @MockitoSpyBean protected lateinit var featureConfig: FeatureConfig

    protected lateinit var db: Database.Connection

    protected fun dbInstance(): Database = Database(jdbi, tracer)

    protected val pngFile =
        this::class.java.getResource("/attachments-fixtures/evaka-logo.png") as URL

    @BeforeAll
    fun beforeAll() {
        assert(httpPort > 0)
        jdbi = configureJdbi(Jdbi.create(getTestDataSource()))
        db = Database(jdbi, tracer).connectWithManualLifecycle()
        if (!resetDbBeforeEach) {
            db.transaction { it.resetDatabase() }
        }
    }

    @BeforeEach
    fun resetBeforeTest() {
        if (resetDbBeforeEach) {
            db.transaction { it.resetDatabase() }
        }
        MockPersonDetailsService.reset()
        MockEmailClient.clear()
    }

    @AfterAll
    fun afterAll() {
        db.close()
    }
}
