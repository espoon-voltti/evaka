// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.aromi

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AromiController(
    private val accessControl: AccessControl,
    private val aromiService: AromiService,
) {
    @GetMapping("/employee/aromi", produces = ["text/csv"])
    fun getMealOrders(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam start: LocalDate,
        @RequestParam end: LocalDate,
    ): ByteArray {
        return db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.Global.READ_AROMI_ORDERS)
            }
            aromiService.getMealOrdersCsv(dbc, FiniteDateRange(start, end))
        }
    }
}
