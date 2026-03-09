// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.noopTracer
import fi.espoo.evaka.turku.database.resetTurkuDatabaseForE2ETests
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.EnableWireMock
import software.amazon.awssdk.services.s3.S3Client

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [IntegrationTestConfiguration::class])
@ActiveProfiles("turku_evaka")
@EnableWireMock
abstract class AbstractIntegrationTest(private val resetDbBeforeEach: Boolean = true) {
    @Autowired private lateinit var jdbi: Jdbi

    protected lateinit var db: Database.Connection

    @Autowired protected lateinit var s3Client: S3Client

    @Autowired protected lateinit var properties: TurkuProperties

    @BeforeAll
    protected fun initializeJdbi() {
        db = Database(jdbi, noopTracer()).connectWithManualLifecycle()
        db.transaction {
            ClassPathResource("turku/dev-data/reset-turku-database-for-e2e-tests.sql")
                .inputStream
                .use { stream -> it.execute { sql(stream.bufferedReader().readText()) } }
            if (!resetDbBeforeEach) {
                it.resetTurkuDatabaseForE2ETests()
            }
        }
    }

    @BeforeEach
    fun setup() {
        if (resetDbBeforeEach) {
            db.transaction { it.resetTurkuDatabaseForE2ETests() }
        }
    }

    @AfterAll
    protected fun afterAll() {
        db.close()
    }
}
