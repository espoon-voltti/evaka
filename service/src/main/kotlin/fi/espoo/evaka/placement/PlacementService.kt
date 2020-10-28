// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.daycare.dao.mapPSQLException
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.Handle
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.Error
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@Service
@Transactional(readOnly = true)
class PlacementService(
    private val dataSource: DataSource
) {
    // todo: maybe use jdbi at some point
    @Transactional
    fun createPlacement(
        type: PlacementType,
        childId: UUID,
        unitId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): Placement {
        return withSpringHandle(dataSource) { h ->
            clearOldPlacements(h, childId, startDate, endDate)
            h.insertPlacement(type, childId, unitId, startDate, endDate)
        }
    }

    // todo: maybe use jdbi at some point
    @Transactional
    fun getPlacementDraftPlacements(childId: UUID): List<PlacementDraftPlacement> {
        return withSpringHandle(dataSource) { h ->
            h.getPlacementDraftPlacements(childId)
        }
    }
}

// todo: use handle
fun updatePlacement(
    h: Handle,
    id: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    aclAuth: AclAuthorization = AclAuthorization.All
): Placement {
    if (endDate.isBefore(startDate)) throw BadRequest("Inverted time range")

    val old = h.getPlacement(id) ?: throw NotFound("Placement $id not found")
    if (startDate.isAfter(old.startDate)) h.clearGroupPlacementsBefore(id, startDate)
    if (endDate.isBefore(old.endDate)) h.clearGroupPlacementsAfter(id, endDate)
    clearOldPlacements(h, old.childId, startDate, endDate, id, aclAuth)

    try {
        h.updatePlacementStartAndEndDate(id, startDate, endDate)
    } catch (e: DataIntegrityViolationException) {
        throw mapPSQLException(e)
    }

    return old
}

fun createGroupPlacement(
    h: Handle,
    daycarePlacementId: UUID,
    groupId: UUID,
    startDate: LocalDate,
    endDate: LocalDate
): UUID {
    if (endDate.isBefore(startDate))
        throw BadRequest("Must not end before even starting")

    if (h.getDaycareGroupPlacements(daycarePlacementId, startDate, endDate, groupId).isNotEmpty())
        throw BadRequest("Group placement must not overlap with existing group placement")

    val daycarePlacement = h.getDaycarePlacement(daycarePlacementId)
        ?: throw NotFound("Placement $daycarePlacementId does not exist")

    if (startDate.isBefore(daycarePlacement.startDate) || endDate.isAfter(daycarePlacement.endDate))
        throw BadRequest("Group placement must be within the daycare placement")

    val group = h.getDaycareGroup(groupId)
        ?: throw NotFound("Group $groupId does not exist")
    if (group.startDate.isAfter(startDate) || (group.endDate != null && group.endDate.isBefore(endDate)))
        throw BadRequest("Group is not active for the full duration")

    val identicalBefore = h.getIdenticalPrecedingGroupPlacement(daycarePlacementId, groupId, startDate)
    val identicalAfter = h.getIdenticalPostcedingGroupPlacement(daycarePlacementId, groupId, endDate)

    try {
        return if (identicalBefore != null && identicalAfter != null) {
            // fills the gap between two existing ones -> merge them
            h.deleteGroupPlacement(identicalAfter.id!!)
            h.updateGroupPlacementEndDate(identicalBefore.id!!, identicalAfter.endDate)
            identicalBefore.id
        } else if (identicalBefore != null) {
            // merge with preceding placement
            h.updateGroupPlacementEndDate(identicalBefore.id!!, endDate)
            identicalBefore.id
        } else if (identicalAfter != null) {
            // merge with postceding placement
            h.updateGroupPlacementStartDate(identicalAfter.id!!, startDate)
            identicalAfter.id
        } else {
            // no merging needed, create new
            h.createGroupPlacement(daycarePlacementId, groupId, startDate, endDate).id!!
        }
    } catch (e: DataIntegrityViolationException) {
        throw mapPSQLException(e)
    }
}

// todo: use handle
fun transferGroup(h: Handle, daycarePlacementId: UUID, groupPlacementId: UUID, groupId: UUID, startDate: LocalDate) {
    val groupPlacement = h.getDaycareGroupPlacement(groupPlacementId)
        ?: throw NotFound("Group placement not found")

    when {
        startDate.isBefore(groupPlacement.startDate) -> {
            throw BadRequest("Cannot transfer to another group before the original placement even starts")
        }

        startDate.isEqual(groupPlacement.startDate) -> {
            if (groupId == groupPlacement.groupId) {
                return // no changes requested
            }

            h.deleteGroupPlacement(groupPlacementId)
        }

        startDate.isAfter(groupPlacement.startDate) -> {
            if (startDate.isAfter(groupPlacement.endDate)) {
                throw BadRequest("Cannot transfer to another group after the original placement has already ended.")
            }

            h.updateGroupPlacementEndDate(groupPlacementId, startDate.minusDays(1))
        }
    }

    createGroupPlacement(h, daycarePlacementId, groupId, startDate, groupPlacement.endDate)
}

// todo: use handle
fun deleteGroupPlacement(h: Handle, groupPlacementId: UUID) {
    val success = h.deleteGroupPlacement(groupPlacementId)
    if (!success) throw NotFound("Group placement not found")
}

@Suppress("IMPLICIT_CAST_TO_ANY")
private fun clearOldPlacements(h: Handle, childId: UUID, from: LocalDate, to: LocalDate, modifiedId: UUID? = null, aclAuth: AclAuthorization = AclAuthorization.All) {
    if (from.isAfter(to)) throw IllegalArgumentException("inverted range")

    h.getPlacementsForChildDuring(childId, from, to)
        .filter { placement -> if (modifiedId != null) placement.id != modifiedId else true }
        .forEach { old ->
            if (old.endDate.isBefore(from) || old.startDate.isAfter(to)) {
                throw Error("bug discovered: query returned non-overlapping placement")
            }
            when {
                // old placement is within new placement (or identical)
                !old.startDate.isBefore(from) && !old.endDate.isAfter(to) -> {
                    checkAclAuth(aclAuth, old)
                    h.cancelPlacement(old.id)
                }

                // old placement encloses new placement
                old.startDate.isBefore(from) && old.endDate.isAfter(to) -> {
                    checkAclAuth(aclAuth, old)
                    splitPlacementWithGap(h, old, from, to)
                }

                // old placement overlaps with the beginning of new placement
                old.startDate.isBefore(from) && !old.endDate.isAfter(to) -> {
                    checkAclAuth(aclAuth, old)
                    movePlacementEndDateEarlier(h, old, from.minusDays(1))
                }

                // old placement overlaps with the ending of new placement
                !old.startDate.isBefore(from) && old.endDate.isAfter(to) -> {
                    checkAclAuth(aclAuth, old)
                    movePlacementStartDateLater(h, old, to.plusDays(1))
                }

                else -> throw Error("bug discovered: some forgotten case?")
            }.exhaust()
        }
}

private fun movePlacementStartDateLater(h: Handle, placement: Placement, newStartDate: LocalDate) {
    if (newStartDate.isBefore(placement.startDate)) throw IllegalArgumentException("Use this method only for shortening placement")

    h.clearGroupPlacementsBefore(placement.id, newStartDate)
    h.updatePlacementStartDate(placement.id, newStartDate)
}

private fun movePlacementEndDateEarlier(h: Handle, placement: Placement, newEndDate: LocalDate) {
    if (newEndDate.isAfter(placement.endDate)) throw IllegalArgumentException("Use this method only for shortening placement")

    h.clearGroupPlacementsAfter(placement.id, newEndDate)
    h.updatePlacementEndDate(placement.id, newEndDate)
}

private fun splitPlacementWithGap(
    h: Handle,
    placement: Placement,
    gapStartInclusive: LocalDate,
    gapEndInclusive: LocalDate
) {
    movePlacementEndDateEarlier(h, placement, gapStartInclusive.minusDays(1))
    h.insertPlacement(
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

fun getDaycarePlacements(
    h: Handle,
    daycareId: UUID?,
    childId: UUID?,
    startDate: LocalDate?,
    endDate: LocalDate?
): Set<DaycarePlacementWithGroups> {
    val daycarePlacements = h.getDaycarePlacements(daycareId, childId, startDate, endDate)
    val minDate = daycarePlacements.map { it.startDate }.min() ?: return emptySet()
    val maxDate = daycarePlacements.map { it.endDate }.max() ?: endDate

    val groupPlacements =
        if (daycareId != null) h.getDaycareGroupPlacements(daycareId, minDate, maxDate, null) else listOf()

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
