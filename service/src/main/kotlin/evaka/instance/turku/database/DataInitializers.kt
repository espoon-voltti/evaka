// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.database

import evaka.core.shared.db.Database
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ClassPathResource

private val logger = KotlinLogging.logger {}

fun Database.Transaction.ensureTurkuDevData() {
    if (createQuery { sql("SELECT count(*) FROM daycare") }.mapTo<Int>().exactlyOne() == 0) {
        listOf("turku/dev-data/turku-dev-data.sql").forEach { path ->
            logger.info { "Running SQL script: $path" }
            ClassPathResource(path).inputStream.use {
                it.bufferedReader().readText().let { content -> execute { sql(content) } }
            }
        }
    }
}
