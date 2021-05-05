// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.serviceneednew.NewServiceNeed
import fi.espoo.evaka.serviceneednew.getServiceNeedsByChild
import fi.espoo.evaka.serviceneednew.getServiceNeedsByUnit
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.FiniteDateRange
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
    ): List<Placement> {
        tx.clearOldPlacements(childId, startDate, endDate)
        val placementTypePeriods = handleFiveYearOldDaycare(tx, childId, type, startDate, endDate)
        return placementTypePeriods.map { (period, type) ->
            tx.insertPlacement(type, childId, unitId, period.start, period.end)
        }
    }
}

fun Database.Transaction.updatePlacement(
    id: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    aclAuth: AclAuthorization = AclAuthorization.All
): Placement {
    if (endDate.isBefore(startDate)) throw BadRequest("Inverted time range")

    val old = getPlacement(id) ?: throw NotFound("Placement $id not found")
    if (startDate.isAfter(old.startDate)) clearGroupPlacementsBefore(id, startDate)
    if (endDate.isBefore(old.endDate)) clearGroupPlacementsAfter(id, endDate)
    clearOldPlacements(old.childId, startDate, endDate, id, aclAuth)

    try {
        val placementTypePeriods = handleFiveYearOldDaycare(this, old.childId, old.type, startDate, endDate)
        val (updatedPeriods, newPeriods) = placementTypePeriods.partition { (period, type) ->
            period.overlaps(FiniteDateRange(old.startDate, old.endDate)) && type == old.type
        }

        updatedPeriods.firstOrNull()?.let { (period, _) ->
            updatePlacementStartAndEndDate(id, period.start, period.end)
        } ?: run {
            checkAclAuth(aclAuth, old)
            cancelPlacement(old.id)
        }

        newPeriods.forEach { (period, type) ->
            insertPlacement(type, old.childId, old.unitId, period.start, period.end)
        }
    } catch (e: Exception) {
        throw mapPSQLException(e)
    }

    return old
}

fun Database.Transaction.checkAndCreateGroupPlacement(
    daycarePlacementId: UUID,
    groupId: UUID,
    startDate: LocalDate,
    endDate: LocalDate
): UUID {
    if (endDate.isBefore(startDate))
        throw BadRequest("Must not end before even starting")

    if (getDaycareGroupPlacements(daycarePlacementId, startDate, endDate, groupId).isNotEmpty())
        throw BadRequest("Group placement must not overlap with existing group placement")

    val daycarePlacement = getDaycarePlacement(daycarePlacementId)
        ?: throw NotFound("Placement $daycarePlacementId does not exist")

    if (startDate.isBefore(daycarePlacement.startDate) || endDate.isAfter(daycarePlacement.endDate))
        throw BadRequest("Group placement must be within the daycare placement")

    val group = getDaycareGroup(groupId)
        ?: throw NotFound("Group $groupId does not exist")
    if (group.startDate.isAfter(startDate) || (group.endDate != null && group.endDate.isBefore(endDate)))
        throw BadRequest("Group is not active for the full duration")

    val identicalBefore = getIdenticalPrecedingGroupPlacement(daycarePlacementId, groupId, startDate)
    val identicalAfter = getIdenticalPostcedingGroupPlacement(daycarePlacementId, groupId, endDate)

    try {
        return if (identicalBefore != null && identicalAfter != null) {
            // fills the gap between two existing ones -> merge them
            deleteGroupPlacement(identicalAfter.id!!)
            updateGroupPlacementEndDate(identicalBefore.id!!, identicalAfter.endDate)
            identicalBefore.id
        } else if (identicalBefore != null) {
            // merge with preceding placement
            updateGroupPlacementEndDate(identicalBefore.id!!, endDate)
            identicalBefore.id
        } else if (identicalAfter != null) {
            // merge with postceding placement
            updateGroupPlacementStartDate(identicalAfter.id!!, startDate)
            identicalAfter.id
        } else {
            // no merging needed, create new
            createGroupPlacement(daycarePlacementId, groupId, startDate, endDate).id!!
        }
    } catch (e: Exception) {
        throw mapPSQLException(e)
    }
}

fun Database.Transaction.transferGroup(daycarePlacementId: UUID, groupPlacementId: UUID, groupId: UUID, startDate: LocalDate) {
    val groupPlacement = getDaycareGroupPlacement(groupPlacementId)
        ?: throw NotFound("Group placement not found")

    when {
        startDate.isBefore(groupPlacement.startDate) -> {
            throw BadRequest("Cannot transfer to another group before the original placement even starts")
        }

        startDate.isEqual(groupPlacement.startDate) -> {
            if (groupId == groupPlacement.groupId) {
                return // no changes requested
            }

            deleteGroupPlacement(groupPlacementId)
        }

        startDate.isAfter(groupPlacement.startDate) -> {
            if (startDate.isAfter(groupPlacement.endDate)) {
                throw BadRequest("Cannot transfer to another group after the original placement has already ended.")
            }

            updateGroupPlacementEndDate(groupPlacementId, startDate.minusDays(1))
        }
    }

    createGroupPlacement(daycarePlacementId, groupId, startDate, groupPlacement.endDate)
}

private fun Database.Transaction.clearOldPlacements(childId: UUID, from: LocalDate, to: LocalDate, modifiedId: UUID? = null, aclAuth: AclAuthorization = AclAuthorization.All) {
    if (from.isAfter(to)) throw IllegalArgumentException("inverted range")

    getPlacementsForChildDuring(childId, from, to)
        .filter { placement -> if (modifiedId != null) placement.id != modifiedId else true }
        .forEach { old ->
            if (old.endDate.isBefore(from) || old.startDate.isAfter(to)) {
                throw Error("bug discovered: query returned non-overlapping placement")
            }
            when {
                // old placement is within new placement (or identical)
                !old.startDate.isBefore(from) && !old.endDate.isAfter(to) -> {
                    checkAclAuth(aclAuth, old)
                    val (_, _, _) = cancelPlacement(old.id)
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

    clearGroupPlacementsBefore(placement.id, newStartDate)
    updatePlacementStartDate(placement.id, newStartDate)
}

private fun Database.Transaction.movePlacementEndDateEarlier(placement: Placement, newEndDate: LocalDate) {
    if (newEndDate.isAfter(placement.endDate)) throw IllegalArgumentException("Use this method only for shortening placement")

    clearGroupPlacementsAfter(placement.id, newEndDate)
    updatePlacementEndDate(placement.id, newEndDate)
}

private fun Database.Transaction.splitPlacementWithGap(
    placement: Placement,
    gapStartInclusive: LocalDate,
    gapEndInclusive: LocalDate
) {
    movePlacementEndDateEarlier(placement, gapStartInclusive.minusDays(1))
    insertPlacement(
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

fun Database.Read.getDetailedDaycarePlacements(
    daycareId: UUID?,
    childId: UUID?,
    startDate: LocalDate?,
    endDate: LocalDate?
): Set<DaycarePlacementWithDetails> {
    val daycarePlacements = getDaycarePlacements(daycareId, childId, startDate, endDate)
    val minDate = daycarePlacements.map { it.startDate }.minOrNull() ?: return emptySet()
    val maxDate = daycarePlacements.map { it.endDate }.maxOrNull() ?: endDate

    val groupPlacements =
        when {
            daycareId != null -> getDaycareGroupPlacements(daycareId, minDate, maxDate, null)
            childId != null -> getChildGroupPlacements(childId)
            else -> listOf()
        }

    val serviceNeeds =
        when {
            daycareId != null -> getServiceNeedsByUnit(daycareId, minDate, maxDate)
            childId != null -> getServiceNeedsByChild(childId)
            else -> listOf()
        }

    return daycarePlacements
        .map { daycarePlacement ->
            DaycarePlacementWithDetails(
                id = daycarePlacement.id,
                child = daycarePlacement.child,
                daycare = daycarePlacement.daycare,
                startDate = daycarePlacement.startDate,
                endDate = daycarePlacement.endDate,
                type = daycarePlacement.type,
                missingServiceNeedDays = daycarePlacement.missingServiceNeedDays,
                missingNewServiceNeedDays = daycarePlacement.missingNewServiceNeedDays,
                groupPlacements = groupPlacements.filter { it.daycarePlacementId == daycarePlacement.id },
                serviceNeeds = serviceNeeds.filter { it.placementId == daycarePlacement.id },
            )
        }
        .map(::addMissingGroupPlacements)
        .toSet()
}

private fun addMissingGroupPlacements(daycarePlacement: DaycarePlacementWithDetails): DaycarePlacementWithDetails {
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

private fun handleFiveYearOldDaycare(tx: Database.Transaction, childId: UUID, type: PlacementType, startDate: LocalDate, endDate: LocalDate): List<Pair<FiniteDateRange, PlacementType>> {
    val (normalPlacementType, fiveYearOldPlacementType) = when (type) {
        PlacementType.DAYCARE, PlacementType.DAYCARE_FIVE_YEAR_OLDS ->
            PlacementType.DAYCARE to PlacementType.DAYCARE_FIVE_YEAR_OLDS
        PlacementType.DAYCARE_PART_TIME, PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS ->
            PlacementType.DAYCARE_PART_TIME to PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS
        else -> return listOf(FiniteDateRange(startDate, endDate) to type)
    }

    val child = tx.getPersonById(childId) ?: error("Child not found ($childId)")
    val fiveYearOldTermStart = LocalDate.of(child.dateOfBirth.plusYears(5).year, 8, 1)
    val fiveYearOldTermEnd = fiveYearOldTermStart.plusYears(1).minusDays(1)

    return listOfNotNull(
        if (startDate < fiveYearOldTermStart)
            FiniteDateRange(
                startDate,
                minOf(fiveYearOldTermStart.minusDays(1), endDate)
            ) to normalPlacementType
        else null,
        if (fiveYearOldTermStart <= endDate && startDate <= fiveYearOldTermEnd)
            FiniteDateRange(
                maxOf(fiveYearOldTermStart, startDate),
                minOf(fiveYearOldTermEnd, endDate)
            ) to fiveYearOldPlacementType
        else null,
        if (fiveYearOldTermEnd < endDate)
            FiniteDateRange(
                maxOf(fiveYearOldTermEnd.plusDays(1), startDate),
                endDate
            ) to normalPlacementType
        else null
    )
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
    val missingServiceNeedDays: Int,
    val missingNewServiceNeedDays: Int
)

data class DaycarePlacementWithDetails(
    val id: UUID,
    val child: ChildBasics,
    val daycare: DaycareBasics,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: PlacementType,
    val missingServiceNeedDays: Int,
    val missingNewServiceNeedDays: Int,
    val groupPlacements: List<DaycareGroupPlacement>,
    val serviceNeeds: List<NewServiceNeed>,
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
    val placementPeriod: FiniteDateRange,
    val childId: UUID,
    val firstName: String?,
    val lastName: String?,
    val dateOfBirth: LocalDate,
    val gap: FiniteDateRange
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
    val area: String,
    val providerType: ProviderType
)
