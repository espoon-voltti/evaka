// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration

@SpringBootApplication(
    exclude = [DataSourceAutoConfiguration::class, TransactionAutoConfiguration::class]
)
class Main

fun main(args: Array<String>) {
    val municipality = System.getenv("EVAKA_MUNICIPALITY") ?: "espoo"
    val municipalityProfile = "${municipality}_evaka"

    val profiles =
        when (System.getenv("VOLTTI_ENV")) {
            "dev",
            "test" -> arrayOf(municipalityProfile, "enable_dev_api")

            else -> arrayOf(municipalityProfile)
        }

    SpringApplicationBuilder().profiles(*profiles).sources(Main::class.java).run(*args)
}
