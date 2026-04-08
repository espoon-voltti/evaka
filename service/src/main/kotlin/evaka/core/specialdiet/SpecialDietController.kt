// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.specialdiet

import evaka.core.Audit
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/employee/diets")
class SpecialDietController(private val accessControl: AccessControl) {
    @GetMapping
    fun getDiets(
        db: Database,
        authenticatedUser: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<SpecialDiet> {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        authenticatedUser,
                        clock,
                        Action.Global.READ_SPECIAL_DIET_LIST,
                    )
                    tx.getSpecialDiets()
                }
            }
            .also { Audit.SpecialDietsRead.log() }
    }
}
