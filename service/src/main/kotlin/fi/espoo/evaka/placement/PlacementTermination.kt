// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.placement.PlacementType.PREPARATORY
import fi.espoo.evaka.placement.PlacementType.PREPARATORY_DAYCARE
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_CLUB
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_DAYCARE
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate

enum class TerminatablePlacementType {
    CLUB,
    PREPARATORY,
    DAYCARE,
    PRESCHOOL,
}

data class TerminatablePlacementGroup(
    val type: TerminatablePlacementType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val unitId: DaycareId,
    val unitName: String,
    val terminatable: Boolean,
    val placements: List<ChildPlacement>,
    /**
     * Relevant for PRESCHOOL/PREPARATORY only. Contains all daycare placements which start after
     * the first PRESCHOOL/PREPARATORY placement.
     */
    val additionalPlacements: List<ChildPlacement>,
)

private fun toTerminatablePlacementType(type: PlacementType): TerminatablePlacementType =
    when (type) {
        PlacementType.CLUB -> TerminatablePlacementType.CLUB
        PlacementType.TEMPORARY_DAYCARE,
        PlacementType.TEMPORARY_DAYCARE_PART_DAY,
        PlacementType.SCHOOL_SHIFT_CARE,
        PlacementType.DAYCARE,
        PlacementType.DAYCARE_PART_TIME,
        PlacementType.DAYCARE_FIVE_YEAR_OLDS,
        PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
        PlacementType.PRESCHOOL_DAYCARE_ONLY,
        PlacementType.PREPARATORY_DAYCARE_ONLY -> TerminatablePlacementType.DAYCARE
        PRESCHOOL,
        PRESCHOOL_DAYCARE,
        PRESCHOOL_CLUB -> TerminatablePlacementType.PRESCHOOL
        PREPARATORY,
        PREPARATORY_DAYCARE -> TerminatablePlacementType.PREPARATORY
    }

fun mapToTerminatablePlacements(
    placements: List<ChildPlacement>,
    today: LocalDate,
): List<TerminatablePlacementGroup> =
    placements
        .groupBy { it.unitName }
        .entries
        .fold(listOf<TerminatablePlacementGroup>()) { acc, (_, childPlacements) ->
            val sorted = childPlacements.sortedBy { it.startDate }
            // all daycare placements after preschool/preparatory are grouped under
            // preschool/preparatory
            val maybePreschoolOrPreparatoryPlacement =
                sorted.find {
                    listOf(
                            PRESCHOOL_DAYCARE,
                            PRESCHOOL,
                            PRESCHOOL_CLUB,
                            PREPARATORY,
                            PREPARATORY_DAYCARE,
                        )
                        .contains(it.type)
                }
            val placementsByType =
                sorted.groupBy {
                    toTerminatablePlacementType(
                        when (it.type) {
                            PlacementType.CLUB,
                            PRESCHOOL,
                            PRESCHOOL_DAYCARE,
                            PRESCHOOL_CLUB,
                            PREPARATORY,
                            PREPARATORY_DAYCARE -> it.type
                            PlacementType.DAYCARE,
                            PlacementType.DAYCARE_PART_TIME,
                            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
                            PlacementType.PRESCHOOL_DAYCARE_ONLY,
                            PlacementType.PREPARATORY_DAYCARE_ONLY,
                            PlacementType.TEMPORARY_DAYCARE,
                            PlacementType.TEMPORARY_DAYCARE_PART_DAY,
                            PlacementType.SCHOOL_SHIFT_CARE ->
                                if (
                                    maybePreschoolOrPreparatoryPlacement
                                        ?.startDate
                                        ?.isBefore(it.startDate) == true
                                ) {
                                    maybePreschoolOrPreparatoryPlacement.type
                                } else {
                                    it.type
                                }
                        }
                    )
                }
            acc +
                placementsByType.map { (type, placements) ->
                    val (placementsOfSameType, additional) =
                        placements.partition { toTerminatablePlacementType(it.type) == type }
                    val startDate = placementsOfSameType.minOf { placement -> placement.startDate }
                    TerminatablePlacementGroup(
                        type = type,
                        placements = placementsOfSameType,
                        additionalPlacements = additional,
                        startDate = startDate,
                        endDate = placementsOfSameType.maxOf { placement -> placement.endDate },
                        unitId = placementsOfSameType[0].unitId,
                        unitName = placementsOfSameType[0].unitName,
                        terminatable =
                            placementsOfSameType[0].terminatable &&
                                startDate.isBefore(today.plusDays(1)),
                    )
                }
        }
        .sortedBy { it.startDate }

private fun ChildPlacement.startsAfter(date: LocalDate): Boolean = this.startDate.isAfter(date)

fun cancelOrTerminatePlacement(
    tx: Database.Transaction,
    placement: ChildPlacement,
    terminationDate: LocalDate,
    terminatedBy: AuthenticatedUser.Citizen,
    now: HelsinkiDateTime,
) {
    if (placement.startsAfter(terminationDate)) {
        tx.cancelPlacement(now, terminatedBy.evakaUserId, placement.id)
    } else {
        tx.terminatePlacementFrom(placement.id, terminationDate, terminatedBy.evakaUserId, now)
    }
}

fun terminateBilledDaycare(
    tx: Database.Transaction,
    user: AuthenticatedUser.Citizen,
    terminatablePlacementGroup: TerminatablePlacementGroup,
    terminationDate: LocalDate,
    childId: ChildId,
    unitId: DaycareId,
    now: HelsinkiDateTime,
) {
    // additional placements after termination date are always canceled
    terminatablePlacementGroup.additionalPlacements
        .filter { it.endDate.isAfter(terminationDate) }
        .forEach { cancelOrTerminatePlacement(tx, it, terminationDate, user, now) }

    val preschoolOrPreparatoryWithDaycare =
        terminatablePlacementGroup.placements.filter {
            (it.type == PRESCHOOL_DAYCARE || it.type == PREPARATORY_DAYCARE) &&
                it.endDate.isAfter(terminationDate)
        }

    // placements which have already been updated on the earlier iteration
    val placementsToSkip = mutableListOf<ChildPlacement>()
    for (placement in preschoolOrPreparatoryWithDaycare) {
        if (placementsToSkip.contains(placement)) {
            continue
        }
        val newPlacementType =
            when (placement.type) {
                PRESCHOOL_DAYCARE -> PRESCHOOL
                PRESCHOOL_CLUB -> PRESCHOOL
                PREPARATORY_DAYCARE -> PREPARATORY
                else ->
                    throw IllegalStateException(
                        "Should not be handling any other placement types here"
                    )
            }
        // It is possible that we already have an adjacent placement of the same type, e.g. when the
        // billed DAYCARE part
        // of a PRESCHOOL_DAYCARE placement has been terminated before. Detect those and prevent
        // duplicates.
        val adjacentPlacement =
            terminatablePlacementGroup.placements
                .find {
                    it.type == newPlacementType && it.startDate == placement.endDate.plusDays(1)
                }
                ?.also { placementsToSkip.add(it) }

        // If placement is in the future
        //  - has an adjacent placement => cancel placement & adjust adjacent start
        //  - no adjacent => change PRESCHOOL_DAYCARE to PRESCHOOL
        // If placement is ongoing
        // - has an adjacent placement => change the end date to termination date & adjust adjacent
        //   start
        // - no adjacent => change the end date and create adjacent

        if (placement.startDate.isAfter(terminationDate)) {
            if (adjacentPlacement != null) {
                // given an adjacent placement, do not create a duplicate PRESCHOOL or PREPARATORY
                // placement, just update the dates
                tx.cancelPlacement(now, user.evakaUserId, placement.id)
                tx.updatePlacementStartDate(
                    adjacentPlacement.id,
                    placement.startDate,
                    now,
                    user.evakaUserId,
                )
            } else {
                // convert PRESCHOOL_DAYCARE and PREPARATORY_DAYCARE placement to PRESCHOOL and
                // PREPARATORY
                // and remove their service needs
                tx.deleteServiceNeedsFromPlacement(placement.id)
                tx.updatePlacementType(
                    placementId = placement.id,
                    type = newPlacementType,
                    now,
                    user.evakaUserId,
                )
            }
        } else {
            tx.movePlacementEndDateEarlier(
                placement.id,
                placement.endDate,
                terminationDate,
                now,
                user.evakaUserId,
            )
            tx.updatePlacementTermination(placement.id, now.toLocalDate(), user.evakaUserId)
            if (adjacentPlacement != null) {
                tx.updatePlacementStartDate(
                    adjacentPlacement.id,
                    terminationDate.plusDays(1),
                    now,
                    user.evakaUserId,
                )
            } else {
                // create new placement without daycare for the remaining period
                // with placement type to PRESCHOOL / PREPARATORY
                tx.insertPlacement(
                    type = newPlacementType,
                    childId = childId,
                    unitId = unitId,
                    startDate = terminationDate.plusDays(1),
                    endDate = placement.endDate,
                    placeGuarantee = false,
                )
            }
        }
    }
}
