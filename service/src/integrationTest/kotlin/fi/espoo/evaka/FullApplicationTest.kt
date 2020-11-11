// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.shared.config.SharedIntegrationTestConfig
import fi.espoo.evaka.shared.config.defaultObjectMapper
import fi.espoo.evaka.shared.config.getTestDataSource
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTempTokenProvider
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import fi.espoo.evaka.vtjclient.VtjIntegrationTestConfig
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.env.Environment
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [SharedIntegrationTestConfig::class, VtjIntegrationTestConfig::class]
)
abstract class FullApplicationTest {
    @LocalServerPort
    protected var httpPort: Int = 0

    // HTTP client for testing the application
    protected val http: FuelManager = FuelManager()

    protected val objectMapper: ObjectMapper = defaultObjectMapper()

    protected lateinit var jdbi: Jdbi
    protected lateinit var db: Database

    protected lateinit var vardaTokenProvider: VardaTokenProvider
    protected lateinit var vardaClient: VardaClient

    @Autowired
    protected lateinit var env: Environment

    protected lateinit var feeDecisionMinDate: LocalDate
    protected lateinit var vardaOrganizerName: String

    @BeforeAll
    protected fun beforeAll() {
        assert(httpPort > 0)
        http.basePath = "http://localhost:$httpPort/"
        jdbi = configureJdbi(Jdbi.create(getTestDataSource()))
        db = Database(jdbi)
        jdbi.handle(::resetDatabase)
        feeDecisionMinDate = LocalDate.parse(env.getRequiredProperty("fee_decision_min_date"))
        val vardaBaseUrl = "http://localhost:$httpPort/mock-integration/varda/api"
        vardaTokenProvider = VardaTempTokenProvider(env, objectMapper, vardaBaseUrl)
        vardaClient = VardaClient(vardaTokenProvider, env, objectMapper, vardaBaseUrl)
        vardaOrganizerName = env.getProperty("fi.espoo.varda.organizer", "Espoo")
    }
}
