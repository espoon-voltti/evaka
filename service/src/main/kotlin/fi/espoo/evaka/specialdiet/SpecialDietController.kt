// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.specialdiet

import fi.espoo.evaka.Audit
import fi.espoo.evaka.jamix.JamixSpecialDiet
import fi.espoo.evaka.jamix.cleanupJamixDietList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/diets")
class SpecialDietController(private val accessControl: AccessControl) {

    /**
     * There is no UI for this endpoint. The point of this endpoint is to digest a list of diets in
     * the format Jamix API returns them from its GET /diets endpoint.
     */
    @PutMapping
    fun putDiets(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody specialDietList: List<JamixSpecialDiet>
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.UPDATE_SPECIAL_DIET_LIST
                    )
                    val cleanedDietList = cleanupJamixDietList(specialDietList)
                    tx.resetSpecialDietsNotContainedWithin(cleanedDietList)
                    tx.setSpecialDiets(cleanedDietList)
                }
            }
            .also { Audit.SpecialDietsUpdate.log() }
    }

    @GetMapping
    fun getDiets(
        db: Database,
        authenticatedUser: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): List<SpecialDiet> {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        authenticatedUser,
                        clock,
                        Action.Global.READ_SPECIAL_DIET_LIST
                    )
                    tx.getSpecialDiets()
                }
            }
            .also { Audit.SpecialDietsRead.log() }
    }
}
