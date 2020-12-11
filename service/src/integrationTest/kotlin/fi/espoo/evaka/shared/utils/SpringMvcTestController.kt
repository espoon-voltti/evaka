// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicReference

@RestController
@RequestMapping("/integration-test")
class SpringMvcTestController {
    val lastDbConnection = AtomicReference<Database.Connection?>()

    @GetMapping("/db-connection-pass")
    fun dbConnectionPass(db: Database.Connection): ResponseEntity<Unit> {
        lastDbConnection.set(db)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/db-connection-fail")
    fun dbConnectionFail(db: Database.Connection): ResponseEntity<Unit> {
        lastDbConnection.set(db)
        throw RuntimeException("Failed")
    }
}
