// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.database

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.noopTracer
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.legacyMockVtjDataset
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
@Profile("local")
class DevDataInitializer(jdbi: Jdbi) {
    init {
        Database(jdbi, noopTracer()).connect { db ->
            db.transaction { tx ->
                tx.runTurkuDevScript("turku/dev-data/reset-turku-database-for-e2e-tests.sql")
                tx.ensureTurkuDevData()
            }
        }
        MockPersonDetailsService.add(legacyMockVtjDataset())
    }
}

private fun Database.Transaction.runTurkuDevScript(path: String) {
    logger.info { "Running SQL script: $path" }
    ClassPathResource(path).inputStream.use {
        it.bufferedReader().readText().let { content -> execute { sql(content) } }
    }
}
