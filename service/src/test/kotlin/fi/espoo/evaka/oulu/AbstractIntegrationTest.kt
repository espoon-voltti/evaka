// SPDX-FileCopyrightText: 2024 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu

import fi.espoo.evaka.oulu.database.resetOuluDatabaseForE2ETests
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.runDevScript
import fi.espoo.evaka.shared.noopTracer
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.wiremock.spring.EnableWireMock
import software.amazon.awssdk.services.s3.S3Client

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [IntegrationTestConfiguration::class])
@EnableWireMock
abstract class AbstractIntegrationTest(private val resetDbBeforeEach: Boolean = true) {
    @Autowired private lateinit var jdbi: Jdbi

    protected lateinit var db: Database.Connection

    @Autowired protected lateinit var s3Client: S3Client

    @Autowired protected lateinit var properties: EvakaOuluProperties

    @BeforeAll
    protected fun initializeJdbi() {
        db = Database(jdbi, noopTracer()).connectWithManualLifecycle()
        db.transaction {
            it.runDevScript("reset-oulu-database-for-e2e-tests.sql")
            if (!resetDbBeforeEach) {
                it.resetOuluDatabaseForE2ETests()
            }
        }
    }

    @BeforeEach
    fun setup() {
        if (resetDbBeforeEach) {
            db.transaction { it.resetOuluDatabaseForE2ETests() }
        }
    }

    @AfterAll
    protected fun afterAll() {
        db.close()
    }
}
