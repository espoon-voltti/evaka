// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.jamix

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
