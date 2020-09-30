// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [FlywayAutoConfiguration::class])
class MessageBackend

fun main(args: Array<String>) {
    runApplication<MessageBackend>(*args)
}
