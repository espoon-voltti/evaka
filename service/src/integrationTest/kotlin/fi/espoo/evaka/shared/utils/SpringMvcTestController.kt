// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicReference

@RestController
@RequestMapping("/integration-test")
class SpringMvcTestController {
    val lastDbConnection = AtomicReference<Database.Connection?>()

    @GetMapping("/db-connection-pass")
    fun dbConnectionPass(db: Database.Connection) {
        lastDbConnection.set(db)
    }

    @GetMapping("/db-connection-fail")
    fun dbConnectionFail(db: Database.Connection) {
        lastDbConnection.set(db)
        throw RuntimeException("Failed")
    }

    @GetMapping("/require-auth")
    fun requireAuth(user: AuthenticatedUser) {
    }

    @GetMapping("/require-auth-employee")
    fun requireAuth(user: AuthenticatedUser.Employee) {
    }
}
