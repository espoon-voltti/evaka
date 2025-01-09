// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.time.Instant
import org.springframework.stereotype.Component

data class PasswordBlacklistSource(val name: String, val updatedAt: HelsinkiDateTime)

@Component
class PasswordBlacklist(private val passwordSpecification: PasswordSpecification) {
    private val logger = KotlinLogging.logger {}

    fun importBlacklists(dbc: Database.Connection, directory: Path): Int {
        logger.info { "Importing password blacklists from $directory" }
        val files = directory.toFile().listFiles { _, name -> name.endsWith(".txt") } ?: return 0

        var total = 0
        for (file in files) {
            val source =
                PasswordBlacklistSource(
                    file.name,
                    HelsinkiDateTime.from(Instant.ofEpochMilli(file.lastModified())),
                )
            total +=
                dbc.transaction { tx ->
                    importPasswords(
                        tx,
                        source,
                        file.bufferedReader(Charsets.UTF_8).lineSequence().map { it.trim() },
                    )
                }
        }

        logger.info { "Imported $total blacklisted passwords" }
        return total
    }

    fun importPasswords(
        tx: Database.Transaction,
        source: PasswordBlacklistSource,
        passwords: Sequence<String>,
    ): Int =
        if (tx.isBlacklistSourceUpToDate(source)) {
            logger.info { "Skipping up-to-date blacklist $source" }
            0
        } else {
            logger.info { "Importing passwords from $source" }
            var total = 0
            for (chunk in
                passwords
                    .filter {
                        passwordSpecification.constraints().isPasswordStructureValid(Sensitive(it))
                    }
                    .chunked(100_000)) {
                tx.upsertPasswordBlacklist(source, chunk.asSequence())
                total += chunk.size
            }
            total
        }
}
