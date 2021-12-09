// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.serviceneed.ServiceNeed
import fi.espoo.evaka.serviceneed.clearServiceNeedsFromPeriod
import fi.espoo.evaka.serviceneed.getServiceNeedsByChild
import fi.espoo.evaka.serviceneed.getServiceNeedsByUnit
import fi.espoo.evaka.serviceneed.insertServiceNeed
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.PilotFeature
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json
import java.time.LocalDate
import java.util.UUID

data class ApplicationServiceNeed(
    val optionId: ServiceNeedOptionId,
    val shiftCare: Boolean
)

fun createPlacement(
    tx: Database.Transaction,
    type: PlacementType,
    childId: UUID,
    unitId: DaycareId,
    startDate: LocalDate,
    endDate: LocalDate,
    useFiveYearsOldDaycare: Boolean,
    serviceNeed: ApplicationServiceNeed? = null
): List<Placement> {
    tx.clearOldPlacements(childId, startDate, endDate)

    val placementTypePeriods = if (useFiveYearsOldDaycare) {
        handleFiveYearOldDaycare(tx, childId, type, startDate, endDate)
    } else {
        listOf(FiniteDateRange(startDate, endDate) to type)
    }

    return placementTypePeriods.map { (period, type) ->
        val placement = tx.insertPlacement(type, childId, unitId, period.start, period.end)
        if (serviceNeed != null) {
            tx.insertServiceNeed(
                placement.id, period.start, period.end,
                serviceNeed.optionId, serviceNeed.shiftCare, confirmedBy = null, confirmedAt = null
            )
        }
        placement
    }
}

fun Database.Transaction.updatePlacement(
    id: PlacementId,
    startDate: LocalDate,
    endDate: LocalDate,
    aclAuth: AclAuthorization = AclAuthorization.All,
    useFiveYearsOldDaycare: Boolean
): Placement {
    if (endDate.isBefore(startDate)) throw BadRequest("Inverted time range")

    val old = getPlacement(id) ?: throw NotFound("Placement $id not found")
    if (startDate.isAfter(old.startDate)) {
        clearGroupPlacementsBefore(id, startDate)
        clearServiceNeedsFromPeriod(this, old.id, FiniteDateRange(old.startDate, startDate.minusDays(1)))
    }
    if (endDate.isBefore(old.endDate)) {
        clearGroupPlacementsAfter(id, endDate)
        clearServiceNeedsFromPeriod(this, old.id, FiniteDateRange(endDate.plusDays(1), old.endDate))
    }
    clearOldPlacements(childId = old.childId, from = startDate, to = endDate, excludePlacement = id, aclAuth = aclAuth)

    try {
        val placementTypePeriods = if (useFiveYearsOldDaycare) {
            handleFiveYearOldDaycare(this, old.childId, old.type, startDate, endDate)
        } else {
            listOf(FiniteDateRange(startDate, endDate) to old.type)
        }
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
    daycarePlacementId: PlacementId,
    groupId: GroupId,
    startDate: LocalDate,
    endDate: LocalDate
): GroupPlacementId {
    if (endDate.isBefore(startDate))
        throw BadRequest("Must not end before even starting")

    val daycarePlacement = getDaycarePlacement(daycarePlacementId)
        ?: throw NotFound("Placement $daycarePlacementId does not exist")

    if (startDate.isBefore(daycarePlacement.startDate) || endDate.isAfter(daycarePlacement.endDate))
        throw BadRequest("Group placement must be within the daycare placement")

    val group = getDaycareGroup(groupId)
        ?: throw NotFound("Group $groupId does not exist")
    if (group.daycareId != daycarePlacement.daycare.id)
        throw BadRequest("Group is in wrong unit")
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
            createGroupPlacement(daycarePlacementId, groupId, startDate, endDate)
        }
    } catch (e: Exception) {
        throw mapPSQLException(e)
    }
}

fun Database.Transaction.transferGroup(groupPlacementId: GroupPlacementId, groupId: GroupId, startDate: LocalDate) {
    val groupPlacement = getDaycareGroupPlacement(groupPlacementId)
        ?: throw NotFound("Group placement not found")

    if (getDaycareGroup(groupPlacement.groupId!!)?.daycareId != getDaycareGroup(groupId)?.daycareId)
        throw BadRequest("Cannot transfer to a group in different unit")

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

    createGroupPlacement(groupPlacement.daycarePlacementId, groupId, startDate, groupPlacement.endDate)
}

private fun Database.Transaction.clearOldPlacements(childId: UUID, from: LocalDate, to: LocalDate, excludePlacement: PlacementId? = null, aclAuth: AclAuthorization = AclAuthorization.All) {
    if (from.isAfter(to)) throw IllegalArgumentException("inverted range")

    getPlacementsForChildDuring(childId, from, to)
        .filter { placement -> if (excludePlacement != null) placement.id != excludePlacement else true }
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
    clearServiceNeedsFromPeriod(this, placement.id, FiniteDateRange(placement.startDate, newStartDate.minusDays(1)))
    updatePlacementStartDate(placement.id, newStartDate)
}

private fun Database.Transaction.movePlacementEndDateEarlier(placement: Placement, newEndDate: LocalDate) {
    if (newEndDate.isAfter(placement.endDate)) throw IllegalArgumentException("Use this method only for shortening placement")

    clearGroupPlacementsAfter(placement.id, newEndDate)
    clearServiceNeedsFromPeriod(this, placement.id, FiniteDateRange(newEndDate.plusDays(1), placement.endDate))
    updatePlacementEndDate(placement.id, newEndDate)
}

private fun Database.Transaction.splitPlacementWithGap(
    placement: Placement,
    gapStartInclusive: LocalDate,
    gapEndInclusive: LocalDate
) {
    movePlacementEndDateEarlier(placement, newEndDate = gapStartInclusive.minusDays(1))

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
    daycareId: DaycareId?,
    childId: UUID?,
    startDate: LocalDate?,
    endDate: LocalDate?
): Set<DaycarePlacementWithDetails> {
    val daycarePlacements = getDaycarePlacements(daycareId, childId, startDate, endDate)
    val minDate = daycarePlacements.map { it.startDate }.minOrNull() ?: return emptySet()
    val maxDate = daycarePlacements.map { it.endDate }.maxOrNull() ?: endDate

    val groupPlacements =
        when {
            daycareId != null -> getGroupPlacementsAtDaycare(daycareId, DateRange(minDate, maxDate))
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
                groupPlacements = groupPlacements.filter { it.daycarePlacementId == daycarePlacement.id },
                serviceNeeds = serviceNeeds.filter { it.placementId == daycarePlacement.id },
                terminatedBy = daycarePlacement.terminatedBy,
                terminationRequestedDate = daycarePlacement.terminationRequestedDate
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
                groupName = null,
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

fun getMissingGroupPlacements(
    tx: Database.Read,
    unitId: DaycareId
): List<MissingGroupPlacement> {
    data class GroupPlacementGap(
        val backup: Boolean,
        val placementId: PlacementId,
        val placementType: PlacementType?,
        val placementRange: FiniteDateRange,
        val childId: UUID,
        @Json
        val serviceNeeds: Set<ServiceNeed>,
        val gapRange: FiniteDateRange
    )

    val evakaLaunch = LocalDate.of(2020, 3, 1)

    data class PlacementResult(
        val id: PlacementId,
        val type: PlacementType,
        val range: FiniteDateRange,
        val childId: UUID,
        @Json
        val serviceNeeds: Set<ServiceNeed>,
        val groupPlacementRanges: List<FiniteDateRange>
    )
    val placements = tx.createQuery(
        """
        SELECT
            FALSE AS backup,
            pl.id,
            pl.type,
            daterange(greatest(pl.start_date, :launch), pl.end_date, '[]') AS range,
            pl.child_id,
            coalesce(jsonb_agg(jsonb_build_object(
                'id', sn.id,
                'placementId', sn.placement_id,
                'startDate', sn.start_date,
                'endDate', sn.end_date,
                'option', jsonb_build_object(
                    'id', sno.id,
                    'nameFi', sno.name_fi,
                    'nameSv', sno.name_sv,
                    'nameEn', sno.name_en,
                    'updated', date_part('epoch', sno.updated)
                ),
                'shiftCare', sn.shift_care,
                'confirmed', jsonb_build_object(
                    'userId', u.id,
                    'name', u.name,
                    'at', date_part('epoch', sn.confirmed_at)
                ),
                'updated', date_part('epoch', sn.updated)
            )) FILTER (WHERE sn.id IS NOT NULL), '[]') AS service_needs,
            coalesce(array_agg(daterange(greatest(dgp.start_date, :launch), dgp.end_date, '[]')) FILTER (WHERE dgp is NOT NULL), '{}') AS group_placement_ranges
        FROM placement pl
        LEFT JOIN daycare_group_placement dgp on pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') && daterange(:launch, NULL)
        LEFT JOIN service_need sn ON sn.placement_id = pl.id
        LEFT JOIN service_need_option sno ON sno.id = sn.option_id
        LEFT JOIN evaka_user u ON u.id = sn.confirmed_by
        WHERE pl.unit_id = :unitId AND daterange(pl.start_date, pl.end_date, '[]') && daterange(:launch, NULL)
        GROUP BY pl.id, pl.type, pl.start_date, pl.end_date, pl.child_id
        --exclude simple cases where there is only one group placement and it matches the placement range
        HAVING count(dgp) != 1
            OR greatest(min(dgp.start_date), :launch) != greatest(pl.start_date, :launch)
            OR greatest(max(dgp.end_date), :launch) != greatest(pl.end_date, :launch);
    """
    ).bind("unitId", unitId).bind("launch", evakaLaunch).mapTo<PlacementResult>().list()

    val missingGroupPlacements = placements.flatMap { placement ->
        placement.range.complement(placement.groupPlacementRanges).map { gap ->
            GroupPlacementGap(
                backup = false,
                placementId = placement.id,
                placementType = placement.type,
                placementRange = placement.range,
                childId = placement.childId,
                serviceNeeds = placement.serviceNeeds,
                gapRange = gap
            )
        }
    }

    val missingBackupCareGroups = tx.createQuery(
        """
        SELECT 
            TRUE AS backup,
            id AS placement_id,
            NULL AS placement_type,
            daterange(start_date, end_date, '[]') AS placement_range,
            child_id,
            '[]' AS service_needs,
            daterange(start_date, end_date, '[]') AS gap_range
        FROM backup_care bc
        WHERE bc.unit_id = :unitId AND bc.group_id IS NULL 
            AND daterange(bc.start_date, bc.end_date, '[]') && daterange(:launch, NULL)
    """
    ).bind("unitId", unitId).bind("launch", evakaLaunch).mapTo<GroupPlacementGap>().list()

    val gaps = missingGroupPlacements + missingBackupCareGroups

    val children = tx.createQuery(
        """
        SELECT id, first_name, last_name, date_of_birth, social_security_number
        FROM person WHERE id = ANY(:childIds)
    """
    )
        .bind("childIds", gaps.map { it.childId }.toSet().toTypedArray())
        .mapTo<ChildBasics>()
        .list()
        .associateBy { it.id }

    return gaps.map { gap ->
        val child = children[gap.childId]!!
        MissingGroupPlacement(
            placementId = gap.placementId,
            placementType = gap.placementType,
            backup = gap.backup,
            placementPeriod = gap.placementRange,
            childId = child.id,
            firstName = child.firstName,
            lastName = child.lastName,
            dateOfBirth = child.dateOfBirth,
            serviceNeeds = gap.serviceNeeds,
            gap = gap.gapRange
        )
    }
}

data class DaycarePlacement(
    val id: PlacementId,
    val child: ChildBasics,
    val daycare: DaycareBasics,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: PlacementType
)

data class DaycarePlacementDetails(
    val id: PlacementId,
    val child: ChildBasics,
    val daycare: DaycareBasics,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: PlacementType,
    val missingServiceNeedDays: Int,
    val terminationRequestedDate: LocalDate?,
    @Nested
    val terminatedBy: TerminatedBy?
)

data class DaycarePlacementWithDetails(
    val id: PlacementId,
    val child: ChildBasics,
    val daycare: DaycareBasics,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: PlacementType,
    val missingServiceNeedDays: Int,
    val groupPlacements: List<DaycareGroupPlacement>,
    val serviceNeeds: List<ServiceNeed>,
    val isRestrictedFromUser: Boolean = false,
    val terminationRequestedDate: LocalDate?,
    @Nested
    val terminatedBy: TerminatedBy?
)

data class DaycareGroupPlacement(
    val id: GroupPlacementId?,
    val groupId: GroupId?,
    val groupName: String?,
    val daycarePlacementId: PlacementId,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class MissingGroupPlacement(
    val placementId: PlacementId,
    val placementType: PlacementType?, // null for backup care
    val backup: Boolean,
    val placementPeriod: FiniteDateRange,
    val childId: UUID,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val serviceNeeds: Set<ServiceNeed>,
    val gap: FiniteDateRange
)

data class ChildBasics(
    val id: UUID,
    val socialSecurityNumber: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: LocalDate
)

data class DaycareBasics(
    val id: DaycareId,
    val name: String,
    val area: String,
    val providerType: ProviderType,
    val enabledPilotFeatures: List<PilotFeature>
)
