// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.dev.seed

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import evaka.core.shared.buildHttpClient
import evaka.core.shared.config.defaultJsonMapperBuilder
import evaka.core.shared.db.Database
import evaka.core.shared.db.configureJdbi
import evaka.core.shared.noopTracer
import evaka.core.shared.utils.post
import java.net.URI
import org.jdbi.v3.core.Jdbi

@Suppress("ktlint:evaka:no-println")
fun main() {
    val dbUrl = prop("evaka.database.url", "jdbc:postgresql://localhost:5432/evaka_local")
    val dbUser = prop("evaka.database.username", "postgres")
    val dbPassword = prop("evaka.database.password", "postgres")
    val idpUrl = prop("evaka.integration.vtj.mock_url", "http://localhost:9090")

    val jsonMapper = defaultJsonMapperBuilder().build()
    val dataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = dbUrl
                username = dbUser
                password = dbPassword
                maximumPoolSize = 2
                poolName = "evaka-seed-applications"
            }
        )
    val jdbi = configureJdbi(Jdbi.create(dataSource))

    val families =
        Database(jdbi, noopTracer()).connect { db ->
            db.transaction { tx -> tx.seedApplications() }
        }

    val dataset = buildVtjDataset(families)
    val idp = buildHttpClient(rootUrl = URI(idpUrl), jsonMapper = jsonMapper)
    idp.post<Unit>("idp/users", jsonBody = dataset)

    dataSource.close()
    println(
        "Seeded ${families.size} families and pushed ${dataset.persons.size} people to dummy-idp."
    )
}

private fun prop(name: String, default: String): String =
    System.getProperty(name) ?: System.getenv(name.uppercase().replace('.', '_')) ?: default
