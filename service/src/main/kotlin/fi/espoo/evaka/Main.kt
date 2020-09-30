// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication(exclude = [FlywayAutoConfiguration::class])
class Main

fun main(args: Array<String>) {
    val profiles = when (System.getenv("VOLTTI_ENV")) {
        "dev", "test" -> arrayOf("enable_dev_api")
        else -> emptyArray()
    }

    SpringApplicationBuilder()
        .profiles(*profiles)
        .sources(Main::class.java)
        .run(*args)
}
