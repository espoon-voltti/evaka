// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.attachment.AttachmentType
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.config.FuelManagerConfig
import fi.espoo.evaka.shared.config.SharedIntegrationTestConfig
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.config.getTestDataSource
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.vtjclient.VtjIntegrationTestConfig
import io.opentracing.Tracer
import java.io.File
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [SharedIntegrationTestConfig::class, VtjIntegrationTestConfig::class]
)
abstract class FullApplicationTest(private val resetDbBeforeEach: Boolean) {
    @LocalServerPort protected var httpPort: Int = 0

    protected val jsonMapper: JsonMapper =
        defaultJsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    private lateinit var jdbi: Jdbi

    /** HTTP client for testing the application */
    @Autowired private lateinit var fuelManagerConfig: FuelManagerConfig
    protected lateinit var http: FuelManager

    @Autowired protected lateinit var env: Environment

    @Autowired protected lateinit var evakaEnv: EvakaEnv

    @Autowired protected lateinit var tracer: Tracer

    protected lateinit var db: Database.Connection

    protected fun dbInstance(): Database = Database(jdbi, tracer)

    protected val pngFile =
        this::class.java.getResource("/attachments-fixtures/evaka-logo.png") as URL

    @BeforeAll
    fun beforeAll() {
        assert(httpPort > 0)
        http = fuelManagerConfig.noCertCheckFuelManager()
        http.forceMethods = true // use actual PATCH requests
        http.basePath = "http://localhost:$httpPort/"
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
        MockEmailClient.clear()
    }

    @AfterAll
    fun afterAll() {
        db.close()
    }

    fun uploadAttachment(
        applicationId: ApplicationId,
        user: AuthenticatedUser,
        type: AttachmentType = AttachmentType.URGENCY
    ): Boolean {
        val path =
            if (user is AuthenticatedUser.Citizen)
                "/attachments/citizen/applications/$applicationId"
            else "/attachments/applications/$applicationId"
        val (_, res, _) =
            http
                .upload(path, parameters = listOf("type" to type))
                .add(FileDataPart(File(pngFile.toURI()), name = "file"))
                .asUser(user)
                .response()

        return res.isSuccessful
    }
}

fun Request.withMockedTime(time: HelsinkiDateTime) =
    this.header("EvakaMockedTime", time.toZonedDateTime())
