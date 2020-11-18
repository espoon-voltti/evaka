// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class PlacementService {
    fun createPlacement(
        tx: Database.Transaction,
        type: PlacementType,
        childId: UUID,
        unitId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): Placement {
        tx.clearOldPlacements(childId, startDate, endDate)
        return tx.handle.insertPlacement(type, childId, unitId, startDate, endDate)
    }
}

fun Database.Transaction.updatePlacement(
    id: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    aclAuth: AclAuthorization = AclAuthorization.All
): Placement {
    if (endDate.isBefore(startDate)) throw BadRequest("Inverted time range")

    val old = handle.getPlacement(id) ?: throw NotFound("Placement $id not found")
    if (startDate.isAfter(old.startDate)) handle.clearGroupPlacementsBefore(id, startDate)
    if (endDate.isBefore(old.endDate)) handle.clearGroupPlacementsAfter(id, endDate)
    clearOldPlacements(old.childId, startDate, endDate, id, aclAuth)

    try {
        handle.updatePlacementStartAndEndDate(id, startDate, endDate)
    } catch (e: Exception) {
        throw mapPSQLException(e)
    }

    return old
}

fun Database.Transaction.createGroupPlacement(
    daycarePlacementId: UUID,
    groupId: UUID,
    startDate: LocalDate,
    endDate: LocalDate
): UUID {
    if (endDate.isBefore(startDate))
        throw BadRequest("Must not end before even starting")

    if (handle.getDaycareGroupPlacements(daycarePlacementId, startDate, endDate, groupId).isNotEmpty())
        throw BadRequest("Group placement must not overlap with existing group placement")

    val daycarePlacement = handle.getDaycarePlacement(daycarePlacementId)
        ?: throw NotFound("Placement $daycarePlacementId does not exist")

    if (startDate.isBefore(daycarePlacement.startDate) || endDate.isAfter(daycarePlacement.endDate))
        throw BadRequest("Group placement must be within the daycare placement")

    val group = handle.getDaycareGroup(groupId)
        ?: throw NotFound("Group $groupId does not exist")
    if (group.startDate.isAfter(startDate) || (group.endDate != null && group.endDate.isBefore(endDate)))
        throw BadRequest("Group is not active for the full duration")

    val identicalBefore = handle.getIdenticalPrecedingGroupPlacement(daycarePlacementId, groupId, startDate)
    val identicalAfter = handle.getIdenticalPostcedingGroupPlacement(daycarePlacementId, groupId, endDate)

    try {
        return if (identicalBefore != null && identicalAfter != null) {
            // fills the gap between two existing ones -> merge them
            handle.deleteGroupPlacement(identicalAfter.id!!)
            handle.updateGroupPlacementEndDate(identicalBefore.id!!, identicalAfter.endDate)
            identicalBefore.id
        } else if (identicalBefore != null) {
            // merge with preceding placement
            handle.updateGroupPlacementEndDate(identicalBefore.id!!, endDate)
            identicalBefore.id
        } else if (identicalAfter != null) {
            // merge with postceding placement
            handle.updateGroupPlacementStartDate(identicalAfter.id!!, startDate)
            identicalAfter.id
        } else {
            // no merging needed, create new
            handle.createGroupPlacement(daycarePlacementId, groupId, startDate, endDate).id!!
        }
    } catch (e: Exception) {
        throw mapPSQLException(e)
    }
}

fun Database.Transaction.transferGroup(daycarePlacementId: UUID, groupPlacementId: UUID, groupId: UUID, startDate: LocalDate) {
    val groupPlacement = handle.getDaycareGroupPlacement(groupPlacementId)
        ?: throw NotFound("Group placement not found")

    when {
        startDate.isBefore(groupPlacement.startDate) -> {
            throw BadRequest("Cannot transfer to another group before the original placement even starts")
        }

        startDate.isEqual(groupPlacement.startDate) -> {
            if (groupId == groupPlacement.groupId) {
                return // no changes requested
            }

            handle.deleteGroupPlacement(groupPlacementId)
        }

        startDate.isAfter(groupPlacement.startDate) -> {
            if (startDate.isAfter(groupPlacement.endDate)) {
                throw BadRequest("Cannot transfer to another group after the original placement has already ended.")
            }

            handle.updateGroupPlacementEndDate(groupPlacementId, startDate.minusDays(1))
        }
    }

    createGroupPlacement(daycarePlacementId, groupId, startDate, groupPlacement.endDate)
}

private fun Database.Transaction.clearOldPlacements(childId: UUID, from: LocalDate, to: LocalDate, modifiedId: UUID? = null, aclAuth: AclAuthorization = AclAuthorization.All) {
    if (from.isAfter(to)) throw IllegalArgumentException("inverted range")

    handle.getPlacementsForChildDuring(childId, from, to)
        .filter { placement -> if (modifiedId != null) placement.id != modifiedId else true }
        .forEach { old ->
            if (old.endDate.isBefore(from) || old.startDate.isAfter(to)) {
                throw Error("bug discovered: query returned non-overlapping placement")
            }
            when {
                // old placement is within new placement (or identical)
                !old.startDate.isBefore(from) && !old.endDate.isAfter(to) -> {
                    checkAclAuth(aclAuth, old)
                    val (_, _, _) = handle.cancelPlacement(old.id)
                }

                // old placement encloses new placement
                old.startDate.isBefore(from) && old.endDate.isAfter(to) -> {
                    checkAclAuth(aclAuth, old)
                    splitPlacementWithGap(old, from, to)
                }

                // old placement overlaps with the beginning of new placement
                old.startDate.isBefore(from) && !old.endDate.isAfter(to) -> {
                    checkAclAuth(aclAuth, old)
                    movePlacementEndDateEarlier(old, from.minusDays(1))
                }

                // old placement overlaps with the ending of new placement
                !old.startDate.isBefore(from) && old.endDate.isAfter(to) -> {
                    checkAclAuth(aclAuth, old)
                    movePlacementStartDateLater(old, to.plusDays(1))
                }

                else -> throw Error("bug discovered: some forgotten case?")
            }.exhaust()
        }
}

private fun Database.Transaction.movePlacementStartDateLater(placement: Placement, newStartDate: LocalDate) {
    if (newStartDate.isBefore(placement.startDate)) throw IllegalArgumentException("Use this method only for shortening placement")

    handle.clearGroupPlacementsBefore(placement.id, newStartDate)
    handle.updatePlacementStartDate(placement.id, newStartDate)
}

private fun Database.Transaction.movePlacementEndDateEarlier(placement: Placement, newEndDate: LocalDate) {
    if (newEndDate.isAfter(placement.endDate)) throw IllegalArgumentException("Use this method only for shortening placement")

    handle.clearGroupPlacementsAfter(placement.id, newEndDate)
    handle.updatePlacementEndDate(placement.id, newEndDate)
}

private fun Database.Transaction.splitPlacementWithGap(
    placement: Placement,
    gapStartInclusive: LocalDate,
    gapEndInclusive: LocalDate
) {
    movePlacementEndDateEarlier(placement, gapStartInclusive.minusDays(1))
    handle.insertPlacement(
        type = placement.type,
        childId = placement.childId,
        unitId = placement.unitId,
        startDate = gapEndInclusive.plusDays(1),
        endDate = placement.endDate
    )
}

private fun checkAclAuth(aclAuth: AclAuthorization, placement: Placement) {
    if (!aclAuth.isAuthorized(placement.unitId)) {
        throw Conflict("Not authorized to modify placement (placementId: ${placement.id})")
    }
}

fun Database.Read.getDaycarePlacements(
    daycareId: UUID?,
    childId: UUID?,
    startDate: LocalDate?,
    endDate: LocalDate?
): Set<DaycarePlacementWithGroups> {
    val daycarePlacements = handle.getDaycarePlacements(daycareId, childId, startDate, endDate)
    val minDate = daycarePlacements.map { it.startDate }.min() ?: return emptySet()
    val maxDate = daycarePlacements.map { it.endDate }.max() ?: endDate

    val groupPlacements =
        if (daycareId != null) handle.getDaycareGroupPlacements(daycareId, minDate, maxDate, null) else listOf()

    return daycarePlacements
        .map { daycarePlacement ->
            DaycarePlacementWithGroups(
                id = daycarePlacement.id,
                child = daycarePlacement.child,
                daycare = daycarePlacement.daycare,
                startDate = daycarePlacement.startDate,
                endDate = daycarePlacement.endDate,
                type = daycarePlacement.type,
                missingServiceNeedDays = daycarePlacement.missingServiceNeedDays,
                groupPlacements = groupPlacements.filter { it.daycarePlacementId == daycarePlacement.id }
            )
        }
        .map(::addMissingGroupPlacements)
        .toSet()
}

private fun addMissingGroupPlacements(daycarePlacement: DaycarePlacementWithGroups): DaycarePlacementWithGroups {
    // calculate unique sorted events from start dates and exclusive end dates
    val eventDates = setOf<LocalDate>(
        daycarePlacement.startDate,
        *daycarePlacement.groupPlacements.map { it.startDate }.toTypedArray(),
        *daycarePlacement.groupPlacements.map { it.endDate.plusDays(1) }.toTypedArray(),
        daycarePlacement.endDate.plusDays(1)
    ).sorted()

    val groupPlacements = mutableListOf<DaycareGroupPlacement>()
    for (i in 0..eventDates.size - 2) {
        val startDate = eventDates[i]
        val endDate = eventDates[i + 1].minusDays(1) // convert back to inclusive
        val groupPlacement = daycarePlacement.groupPlacements
            .find { groupPlacement -> startDate.isEqual(groupPlacement.startDate) }
            ?: DaycareGroupPlacement(
                id = null,
                groupId = null,
                daycarePlacementId = daycarePlacement.id,
                startDate = startDate,
                endDate = endDate
            )
        groupPlacements.add(groupPlacement)
    }

    return daycarePlacement.copy(groupPlacements = groupPlacements)
}

data class DaycarePlacement(
    val id: UUID,
    val child: ChildBasics,
    val daycare: DaycareBasics,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: PlacementType
)

data class DaycarePlacementDetails(
    val id: UUID,
    val child: ChildBasics,
    val daycare: DaycareBasics,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: PlacementType,
    val missingServiceNeedDays: Int
)

data class DaycarePlacementWithGroups(
    val id: UUID,
    val child: ChildBasics,
    val daycare: DaycareBasics,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: PlacementType,
    val missingServiceNeedDays: Int,
    val groupPlacements: List<DaycareGroupPlacement>,
    val isRestrictedFromUser: Boolean = false
)

data class DaycareGroupPlacement(
    val id: UUID?,
    val groupId: UUID?,
    val daycarePlacementId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class MissingGroupPlacement(
    val placementId: UUID,
    val placementType: PlacementType?, // null for backup care
    val backup: Boolean,
    val placementPeriod: ClosedPeriod,
    val childId: UUID,
    val firstName: String?,
    val lastName: String?,
    val dateOfBirth: LocalDate,
    val gap: ClosedPeriod
)

data class ChildBasics(
    val id: UUID,
    val socialSecurityNumber: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: LocalDate
)

data class DaycareBasics(
    val id: UUID,
    val name: String,
    val area: String
)
