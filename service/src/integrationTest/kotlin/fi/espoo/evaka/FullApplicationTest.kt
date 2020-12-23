// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.config.SharedIntegrationTestConfig
import fi.espoo.evaka.shared.config.defaultObjectMapper
import fi.espoo.evaka.shared.config.getTestDataSource
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTempTokenProvider
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import fi.espoo.evaka.vtjclient.VtjIntegrationTestConfig
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.env.Environment
import java.io.File
import java.time.LocalDate
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [SharedIntegrationTestConfig::class, VtjIntegrationTestConfig::class]
)
abstract class FullApplicationTest {
    @LocalServerPort
    protected var httpPort: Int = 0

    // HTTP client for testing the application

    protected val objectMapper: ObjectMapper = defaultObjectMapper()

    protected lateinit var jdbi: Jdbi

    @Autowired
    protected lateinit var http: FuelManager

    protected lateinit var vardaTokenProvider: VardaTokenProvider
    protected lateinit var vardaClient: VardaClient

    @Autowired
    protected lateinit var env: Environment

    protected lateinit var feeDecisionMinDate: LocalDate
    protected lateinit var vardaOrganizerName: String

    protected lateinit var db: Database.Connection

    protected fun dbInstance(): Database = Database(jdbi)

    private val pngFile = this::class.java.getResource("/attachments-fixtures/espoo-logo.png")

    @BeforeAll
    protected fun beforeAll() {
        assert(httpPort > 0)
        http.basePath = "http://localhost:$httpPort/"
        jdbi = configureJdbi(Jdbi.create(getTestDataSource()))
        db = Database(jdbi).connect()
        db.transaction { it.resetDatabase() }
        feeDecisionMinDate = LocalDate.parse(env.getRequiredProperty("fee_decision_min_date"))
        val vardaBaseUrl = "http://localhost:$httpPort/mock-integration/varda/api"
        vardaTokenProvider = VardaTempTokenProvider(http, env, objectMapper, vardaBaseUrl)
        vardaClient = VardaClient(vardaTokenProvider, http, env, objectMapper, vardaBaseUrl)
        vardaOrganizerName = env.getProperty("fi.espoo.varda.organizer", "Espoo")
    }

    @AfterAll
    protected fun afterAll() {
        db.close()
    }

    fun uploadAttachment(applicationId: UUID, user: AuthenticatedUser): Boolean {
        val (_, res, _) = http.upload("/attachments/enduser/applications/$applicationId", parameters = listOf("type" to "URGENCY"))
            .add(FileDataPart(File(pngFile.toURI()), name = "file"))
            .asUser(user)
            .response()

        return res.isSuccessful
    }
}
