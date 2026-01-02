// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration

@SpringBootApplication(
    exclude = [DataSourceAutoConfiguration::class, TransactionAutoConfiguration::class]
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
