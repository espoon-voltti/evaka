// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.placement.PlacementType.PREPARATORY_DAYCARE
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_DAYCARE
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class PlacementControllerCitizen(
    private val accessControl: AccessControl
) {

    enum class TerminatablePlacementType {
        CLUB, PREPARATORY, DAYCARE, PRESCHOOL
    }

    data class TerminatablePlacementGroup(
        val type: TerminatablePlacementType,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val unitId: DaycareId,
        val unitName: String,
        val placements: List<ChildPlacement>
    )

    private fun mapToTerminatablePlacements(placements: List<ChildPlacement>): List<TerminatablePlacementGroup> = placements
        .groupBy { it.unitName }
        .entries
        .fold(listOf()) { acc, (_, placements) ->
            val placementsByType = placements.groupBy {
                when (it.type) {
                    PlacementType.CLUB -> TerminatablePlacementType.CLUB
                    PlacementType.TEMPORARY_DAYCARE, // TODO is this correct?
                    PlacementType.TEMPORARY_DAYCARE_PART_DAY, // TODO is this correct?
                    PlacementType.SCHOOL_SHIFT_CARE, // TODO is this correct?
                    PlacementType.DAYCARE,
                    PlacementType.DAYCARE_PART_TIME,
                    PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                    PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> TerminatablePlacementType.DAYCARE
                    PlacementType.PRESCHOOL,
                    PRESCHOOL_DAYCARE -> TerminatablePlacementType.PRESCHOOL
                    PlacementType.PREPARATORY,
                    PREPARATORY_DAYCARE -> TerminatablePlacementType.PREPARATORY
                }
            }
            acc + placementsByType.map {
                TerminatablePlacementGroup(
                    type = it.key,
                    placements = it.value,
                    startDate = it.value.minOf { placement -> placement.startDate },
                    endDate = it.value.maxOf { placement -> placement.endDate },
                    unitId = it.value[0].unitId,
                    unitName = it.value[0].unitName,
                )
            }
        }

    data class ChildPlacementResponse(
        val placements: List<TerminatablePlacementGroup>,
    )

    @GetMapping("/citizen/children/{childId}/placements")
    fun getPlacements(
        db: Database.Connection,
        user: AuthenticatedUser.Citizen,
        evakaClock: EvakaClock,
        @PathVariable childId: UUID,
    ): ChildPlacementResponse {
        Audit.PlacementSearch.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_PLACEMENT, childId)

        return ChildPlacementResponse(
            placements = mapToTerminatablePlacements(
                db.read {
                    it.getCitizenChildPlacements(
                        evakaClock.today(),
                        childId
                    )
                }
            )
        )
    }

    data class PlacementTerminationRequestBody(
        val type: TerminatablePlacementType,
        val unitId: DaycareId,
        val terminationDate: LocalDate,
        val terminateDaycareOnly: Boolean?,
    )

    @PostMapping("/citizen/children/{childId}/placements/terminate")
    fun postPlacementTermination(
        db: Database.Connection,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: UUID,
        @RequestBody body: PlacementTerminationRequestBody
    ) {
        Audit.PlacementTerminate.log(body.unitId, body.type)

        val placements = db.read { it.getCitizenChildPlacements(clock.today(), childId) }.also {
            // TODO list support for accessControl
            it
                .map { p -> p.id }
                .forEach { id -> accessControl.requirePermissionFor(user, Action.Placement.TERMINATE, id) }
        }
        val terminatable = mapToTerminatablePlacements(placements)
            .find { it.unitId == body.unitId && it.type == body.type }
            ?: throw NotFound("Matching placement type not found")

        db.transaction { tx ->
            // TODO validate termination
            terminatable.placements.forEach {
                if (body.terminateDaycareOnly == true) {
                    if (listOf(PRESCHOOL_DAYCARE, PREPARATORY_DAYCARE).contains(it.type)) {
                        // TODO change placement type to PRESCHOOL / PREPARATORY and remove service needs, split placement if current placement
                    }
                } else if (it.startDate.isAfter(body.terminationDate)) { // TODO check off by one
                    tx.cancelPlacement(it.id)
                } else if (body.terminationDate.isAfter(it.startDate) &&
                    // TODO check off by one
                    // TODO what is a valid termination date?
                    body.terminationDate.isBefore(it.endDate)
                ) {

                    tx.terminatePlacementFrom(clock.today(), it.id, body.terminationDate, user.id)
                }
            }
        }
    }
}
