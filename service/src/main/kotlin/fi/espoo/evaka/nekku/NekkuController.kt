// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId.Companion.invoke
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class NekkuController(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {

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
    fun getNekkuMealTypes(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<NekkuMealType> {
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
                    listOf(NekkuMealType(null, "Seka")) +
                        NekkuProductMealType.entries.map { NekkuMealType(it, it.description) }
                }
            }
            .also { Audit.NekkuMealTypesRead.log() }
    }

    @GetMapping("/employee/nekku/special-diets")
    fun getNekkuSpecialDiets(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<NekkuSpecialDietWithoutFields> {
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
                    tx.getNekkuSpecialDiets()
                }
            }
            .also { Audit.NekkuSpecialDietsRead.log() }
    }

    @GetMapping("/employee/nekku/special-diet-fields")
    fun getNekkuSpecialDietFields(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<NekkuSpecialDietsFieldWithoutOptions> {
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
                    tx.getNekkuSpecialDietFields()
                }
            }
            .also { Audit.NekkuSpecialDietFieldsRead.log() }
    }

    @GetMapping("/employee/nekku/special-diet-options")
    fun getNekkuSpecialDietOptions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<NekkuSpecialDietOptionWithFieldId> {
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
                    tx.getNekkuSpecialDietOptions()
                }
            }
            .also { Audit.NekkuSpecialDietFieldOptionsRead.log() }
    }

    @PostMapping("/employee/nekku/manual-order")
    fun nekkuManualOrder(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam groupId: GroupId,
        @RequestParam date: LocalDate,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Group.NEKKU_MANUAL_ORDER,
                        groupId,
                    )
                    planNekkuManualOrderJob(tx, asyncJobRunner, clock.now(), groupId, date)
                }
            }
            .also {
                Audit.NekkuManualOrder.log(
                    targetId = fi.espoo.evaka.AuditId(groupId),
                    meta = mapOf("date" to date),
                )
            }
    }
}
