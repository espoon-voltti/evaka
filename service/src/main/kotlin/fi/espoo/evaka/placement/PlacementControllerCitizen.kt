// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class PlacementControllerCitizen(
    private val accessControl: AccessControl
) {
    @GetMapping("/citizen/children/{childId}/placements")
    fun getPlacements(
        db: Database.Connection,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable childId: UUID,
    ): List<ChildPlacement> {
        Audit.PlacementSearch.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_PLACEMENT, childId)
        return db.read { it.getCitizenChildPlacements(evakaClock.today(), childId) }
    }
}
