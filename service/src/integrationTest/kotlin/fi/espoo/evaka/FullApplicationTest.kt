// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.attachment.AttachmentType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.config.SharedIntegrationTestConfig
import fi.espoo.evaka.shared.config.defaultObjectMapper
import fi.espoo.evaka.shared.config.getTestDataSource
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
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
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.env.Environment
import java.io.File
import java.net.URL
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

    protected val objectMapper: ObjectMapper = defaultObjectMapper()

    protected lateinit var jdbi: Jdbi

    @Autowired
    protected lateinit var http: FuelManager

    protected lateinit var vardaTokenProvider: VardaTokenProvider
    protected lateinit var vardaClient: VardaClient

    @Autowired
    protected lateinit var env: Environment
    @SpyBean
    protected lateinit var evakaEnv: EvakaEnv
    @Autowired
    protected lateinit var vardaEnv: VardaEnv
    @Autowired
    protected lateinit var ophEnv: OphEnv

    protected lateinit var feeDecisionMinDate: LocalDate
    protected lateinit var vardaOrganizerName: String
    protected lateinit var municipalOrganizerOid: String
    protected lateinit var ophMunicipalityCode: String
    protected lateinit var ophMunicipalOrganizerIdUrl: String

    protected lateinit var db: Database.Connection

    protected fun dbInstance(): Database = Database(jdbi)

    protected val pngFile = this::class.java.getResource("/attachments-fixtures/evaka-logo.png") as URL

    @BeforeAll
    protected fun beforeAll() {
        assert(httpPort > 0)
        http.basePath = "http://localhost:$httpPort/"
        jdbi = configureJdbi(Jdbi.create(getTestDataSource()))
        db = Database(jdbi).connectWithManualLifecycle()
        db.transaction { it.resetDatabase() }
        feeDecisionMinDate = evakaEnv.feeDecisionMinDate
        municipalOrganizerOid = ophEnv.organizerOid
        ophMunicipalityCode = ophEnv.municipalityCode
        ophMunicipalOrganizerIdUrl = "${vardaEnv.url}/v1/vakajarjestajat/${ophEnv.organizerId}/"
        val vardaBaseUrl = "http://localhost:$httpPort/mock-integration/varda/api"
        val vardaEnv = VardaEnv.fromEnvironment(env).copy(url = vardaBaseUrl)
        vardaTokenProvider = VardaTempTokenProvider(http, objectMapper, vardaEnv)
        vardaClient = VardaClient(vardaTokenProvider, http, objectMapper, vardaEnv)
    }

    @AfterAll
    protected fun afterAll() {
        db.close()
    }

    fun uploadAttachment(applicationId: ApplicationId, user: AuthenticatedUser, type: AttachmentType = AttachmentType.URGENCY): Boolean {
        val path = if (user.isEndUser) "/attachments/citizen/applications/$applicationId" else "/attachments/applications/$applicationId"
        val (_, res, _) = http.upload(path, parameters = listOf("type" to type))
            .add(FileDataPart(File(pngFile.toURI()), name = "file"))
            .asUser(user)
            .response()

        return res.isSuccessful
    }
}

fun Request.withMockedTime(time: HelsinkiDateTime) = this.header("EvakaMockedTime", time.toZonedDateTime())
