// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import kotlin.system.exitProcess

@Suppress("ktlint:evaka:no-println")
fun main(args: Array<String>) {
    val password = args.toList().singleOrNull()?.let(::Sensitive)
    if (password == null) {
        println("Expected exactly one argument: a password")
        println(
            "If you are running via gradle, try passing your password using --args: ./gradlew encodePassword --args=\"mypassword\""
        )
        exitProcess(1)
    }
    val jsonMapper = defaultJsonMapperBuilder().build()
    println("Encoded password as JSON:")
    println(jsonMapper.writeValueAsString(PasswordHashAlgorithm.DEFAULT.encode(password)))
}
