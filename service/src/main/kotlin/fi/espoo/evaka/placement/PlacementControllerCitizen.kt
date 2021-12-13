// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

data class PlacementTerminationRequestBody(
    val placementIds: List<PlacementId>,
    val terminationDate: LocalDate,
)

data class PlacementTerminationConstraint(
    val placementId: PlacementId,
    val requiresTerminationOf: PlacementId
)

data class ChildPlacementResponse(
    val placements: List<ChildPlacement>,
    val terminationConstraints: List<PlacementTerminationConstraint>
)

private fun placementsOverlap(p1: ChildPlacement, p2: ChildPlacement): Boolean =
    FiniteDateRange(p1.placementStartDate, p1.placementEndDate)
        .overlaps(FiniteDateRange(p2.placementStartDate, p2.placementEndDate))

@RestController
class PlacementControllerCitizen(
    private val accessControl: AccessControl
) {
    @GetMapping("/citizen/children/{childId}/placements")
    fun getPlacements(
        db: Database.Connection,
        user: AuthenticatedUser.Citizen,
        evakaClock: EvakaClock,
        @PathVariable childId: UUID,
    ): ChildPlacementResponse {
        Audit.PlacementSearch.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_PLACEMENT, childId)
        val placements = db.read { it.getCitizenChildPlacements(evakaClock.today(), childId) }
        val constraints = placements.flatMap { p ->
            if (p.placementType == PlacementType.PRESCHOOL) {
                placements.find { p2 -> p2.placementType == PlacementType.PRESCHOOL_DAYCARE && placementsOverlap(p, p2) }
                    ?.let { listOf(PlacementTerminationConstraint(p.placementId, it.placementId)) }
                    ?: listOf()
            } else {
                listOf()
            }
        }
        return ChildPlacementResponse(placements = placements, terminationConstraints = constraints)
    }

    @PostMapping("/citizen/placements/terminate")
    fun postPlacementTermination(
        db: Database.Connection,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: PlacementTerminationRequestBody
    ) {
        Audit.PlacementTerminate.log(targetId = body.placementIds.joinToString())

        // TODO list support for accessControl
        body.placementIds.forEach { accessControl.requirePermissionFor(user, Action.Placement.TERMINATE, it) }

        // TODO validate dates and PRESCHOOL+PRESCHOOL_DAYCARE combo
        db.transaction { tx ->
            body.placementIds.forEach { tx.terminatePlacementFrom(clock.today(), it, body.terminationDate, user.id) }
        }
    }
}
