// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.database

import evaka.core.shared.db.Database
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ClassPathResource

private val logger = KotlinLogging.logger {}

fun Database.Transaction.ensureOuluDevData() {
    if (createQuery { sql("SELECT count(*) FROM daycare") }.exactlyOne<Int>() == 0) {
        listOf("oulu/dev-data/oulu-dev-data.sql").forEach { path ->
            logger.info { "Running SQL script: $path" }
            ClassPathResource(path).inputStream.use {
                it.bufferedReader().readText().let { content -> execute { sql(content) } }
            }
        }
    }
}

fun Database.Transaction.resetOuluDatabaseForE2ETests() {
    execute { sql("SELECT reset_oulu_database_for_e2e_tests()") }
}
