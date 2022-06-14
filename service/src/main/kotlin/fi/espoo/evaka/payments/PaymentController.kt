// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.payments

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/payments")
class PaymentController(
    private val accessControl: AccessControl
) {
    @PostMapping("/create-drafts")
    fun createDrafts(db: Database, user: AuthenticatedUser, clock: EvakaClock) {
        Audit.PaymentsCreate.log()
        accessControl.requirePermissionFor(user, Action.Global.CREATE_DRAFT_PAYMENTS)
        db.connect { dbc -> dbc.transaction { tx -> createPaymentDrafts(tx) } }
    }
}
