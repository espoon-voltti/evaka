// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.jamix

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class JamixController(
    private val jamixService: JamixService,
    private val accessControl: AccessControl,
) {
    @PutMapping("/employee/jamix/send-orders")
    fun sendJamixOrders(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ) {
        db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.Global.SEND_JAMIX_ORDERS)
            }
            jamixService.planOrdersForUnitAndDate(dbc, clock, unitId, date)
        }
        Audit.SendJamixOrders.log(targetId = AuditId.invoke(unitId), meta = mapOf("date" to date))
    }
}
