// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.placement.PlacementType.CLUB
import fi.espoo.evaka.placement.PlacementType.PREPARATORY
import fi.espoo.evaka.placement.PlacementType.PREPARATORY_DAYCARE
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL
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
        val placements: List<ChildPlacement>,
        val additionalPlacements: List<ChildPlacement>
    )

    private fun toTerminatablePlacementType(type: PlacementType): TerminatablePlacementType =
        when (type) {
            CLUB -> TerminatablePlacementType.CLUB
            PlacementType.TEMPORARY_DAYCARE,
            PlacementType.TEMPORARY_DAYCARE_PART_DAY,
            PlacementType.SCHOOL_SHIFT_CARE,
            PlacementType.DAYCARE,
            PlacementType.DAYCARE_PART_TIME,
            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> TerminatablePlacementType.DAYCARE
            PRESCHOOL,
            PRESCHOOL_DAYCARE -> TerminatablePlacementType.PRESCHOOL
            PREPARATORY,
            PREPARATORY_DAYCARE -> TerminatablePlacementType.PREPARATORY
        }

    private fun mapToTerminatablePlacements(placements: List<ChildPlacement>): List<TerminatablePlacementGroup> = placements
        .groupBy { it.unitName }
        .entries
        .fold(listOf<TerminatablePlacementGroup>()) { acc, (_, childPlacements) ->
            val sorted = childPlacements.sortedBy { it.startDate }
            // all daycare placements after preschool/preparatory are grouped under preschool/preparatory
            val maybePreschoolOrPreparatoryPlacement =
                sorted.find { listOf(PRESCHOOL, PRESCHOOL_DAYCARE, PREPARATORY, PREPARATORY_DAYCARE).contains(it.type) }
            val placementsByType = sorted.groupBy {
                toTerminatablePlacementType(
                    when (it.type) {
                        CLUB,
                        PRESCHOOL,
                        PRESCHOOL_DAYCARE,
                        PREPARATORY,
                        PREPARATORY_DAYCARE -> it.type
                        PlacementType.DAYCARE,
                        PlacementType.DAYCARE_PART_TIME,
                        PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                        PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
                        PlacementType.TEMPORARY_DAYCARE,
                        PlacementType.TEMPORARY_DAYCARE_PART_DAY,
                        PlacementType.SCHOOL_SHIFT_CARE -> if (maybePreschoolOrPreparatoryPlacement?.startDate?.isBefore(it.startDate) == true) {
                            maybePreschoolOrPreparatoryPlacement.type
                        } else {
                            it.type
                        }
                    }
                )
            }
            acc + placementsByType.map { (type, placements) ->
                val (placementsOfSameType, additional) = placements.partition { toTerminatablePlacementType(it.type) == type }
                TerminatablePlacementGroup(
                    type = type,
                    placements = placementsOfSameType,
                    additionalPlacements = additional,
                    startDate = placementsOfSameType.minOf { placement -> placement.startDate },
                    endDate = placementsOfSameType.maxOf { placement -> placement.endDate },
                    unitId = placementsOfSameType[0].unitId,
                    unitName = placementsOfSameType[0].unitName,
                )
            }
        }
        .sortedBy { it.startDate }

    data class ChildPlacementResponse(
        val placements: List<TerminatablePlacementGroup>,
    )

    @GetMapping("/citizen/children/{childId}/placements")
    fun getPlacements(
        db: Database.DeprecatedConnection,
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
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: UUID,
        @RequestBody body: PlacementTerminationRequestBody
    ) {
        Audit.PlacementTerminate.log(body.unitId, body.type)

        val terminatablePlacements = db.read { it.getCitizenChildPlacements(clock.today(), childId) }
            .also {
                // TODO list support for accessControl
                it.map { p -> p.id }.forEach { id -> accessControl.requirePermissionFor(user, Action.Placement.TERMINATE, id) }
            }
            .let { mapToTerminatablePlacements(it) }
            .filter { it.unitId == body.unitId && it.type == body.type }
            .flatMap { it.placements + it.additionalPlacements }
            .filter { it.startsAfter(body.terminationDate) || it.containsExclusively(body.terminationDate) }
            .also { if (it.isEmpty()) throw NotFound("Matching placement type not found") }

        db.transaction { tx ->
            terminatablePlacements
                .sortedByDescending { it.startDate }
                .forEach { placement ->
                    if (placement.startsAfter(body.terminationDate)) {
                        tx.cancelPlacement(placement.id)
                    } else if (placement.containsExclusively(body.terminationDate)) {
                        if (body.terminateDaycareOnly == true && (placement.type == PRESCHOOL_DAYCARE || placement.type == PREPARATORY_DAYCARE)) {
                            tx.terminatePlacementFrom(clock.today(), placement.id, body.terminationDate, null)
                            // create new placement without daycare for the remaining period
                            // with placement type to PRESCHOOL / PREPARATORY
                            val typeForRemainingPeriod = if (placement.type === PRESCHOOL_DAYCARE) PRESCHOOL else PREPARATORY
                            tx.insertPlacement(
                                type = typeForRemainingPeriod,
                                childId = childId,
                                unitId = body.unitId,
                                startDate = body.terminationDate.plusDays(1),
                                endDate = placement.endDate
                            )
                        } else {
                            tx.terminatePlacementFrom(clock.today(), placement.id, body.terminationDate, user.id)
                        }
                    }
                }
        }
    }
}

private fun ChildPlacement.startsAfter(date: LocalDate): Boolean = this.startDate.isAfter(date)
private fun ChildPlacement.containsExclusively(date: LocalDate): Boolean = date.isAfter(this.startDate) && date.isBefore(this.endDate)
