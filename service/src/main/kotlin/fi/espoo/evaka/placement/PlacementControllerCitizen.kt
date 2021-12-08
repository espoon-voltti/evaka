// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController("/citizen/placements")
class PlacementControllerCitizen(
    private val accessControl: AccessControl
) {
    @GetMapping()
    fun getPlacements(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam(value = "childId") childId: UUID,
    ): List<Placement> {
        Audit.PlacementSearch.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_PLACEMENT, childId)
        return db.read { it.getPlacementsForChild(childId) }
    }
}
