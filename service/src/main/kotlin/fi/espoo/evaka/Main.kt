// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication(
    exclude =
        [
            DataSourceAutoConfiguration::class,
            FlywayAutoConfiguration::class,
            SecurityAutoConfiguration::class,
            SecurityFilterAutoConfiguration::class,
            TransactionAutoConfiguration::class,
        ]
)
class Main

fun main(args: Array<String>) {
    val profiles =
        when (System.getenv("VOLTTI_ENV")) {
            "dev",
            "test" -> arrayOf("espoo_evaka", "enable_dev_api")
            else -> arrayOf("espoo_evaka")
        }

    SpringApplicationBuilder().profiles(*profiles).sources(Main::class.java).run(*args)
}
