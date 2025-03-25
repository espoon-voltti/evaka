// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class NekkuController(private val accessControl: AccessControl) {

    @GetMapping("/employee/nekku/unit-numbers")
    fun getNekkuUnitNumbers(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<NekkuUnitNumber> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    if (
                        !accessControl.isPermittedForSomeTarget(
                            tx,
                            user,
                            clock,
                            Action.Unit.READ_GROUPS,
                        )
                    )
                        throw Forbidden()
                    tx.getNekkuUnitNumbers()
                }
            }
            .also { Audit.NekkuUnitsRead.log() }
    }

    @GetMapping("/employee/nekku/meal-types")
    fun getNekkuMealTypes(user: AuthenticatedUser.Employee): List<NekkuMealType> {
        return listOf(NekkuMealType(null, "Seka")) +
            NekkuProductMealType.entries.map {
                NekkuMealType(it, it.description.replaceFirstChar { it.uppercaseChar() })
            }
    }
}
