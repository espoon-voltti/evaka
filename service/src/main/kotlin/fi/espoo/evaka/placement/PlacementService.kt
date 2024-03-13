// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.occupancy.familyUnitPlacementCoefficient
import fi.espoo.evaka.serviceneed.ServiceNeed
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.clearServiceNeedsFromPeriod
import fi.espoo.evaka.serviceneed.getServiceNeedsByChild
import fi.espoo.evaka.serviceneed.getServiceNeedsByUnit
import fi.espoo.evaka.serviceneed.insertServiceNeed
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.user.EvakaUser
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json

data class ApplicationServiceNeed(
    val optionId: ServiceNeedOptionId,
    val shiftCare: Boolean,
    val placementType: PlacementType
)

fun createPlacements(
    tx: Database.Transaction,
    childId: ChildId,
    unitId: DaycareId,
    placementTypePeriods: List<Pair<FiniteDateRange, PlacementType>>,
    serviceNeed: ApplicationServiceNeed? = null,
    cancelPlacementsAfterClub: Boolean? = false,
    placeGuarantee: Boolean
): List<Placement> {
    placementTypePeriods
        .sortedBy { it.first.start }
        .forEach { (period, type) ->
            tx.clearOldPlacements(childId, period.start, period.end)
            if (cancelPlacementsAfterClub == true && type == PlacementType.CLUB) {
                tx.clearFutureNonPreschoolRelatedPlacements(childId, period.end.plusDays(1))
            }
        }

    val firstPlacementTypePeriod = placementTypePeriods.minOfOrNull { it.first.start }
    return placementTypePeriods.map { (period, type) ->
        val placement =
            tx.insertPlacement(
                type,
                childId,
                unitId,
                period.start,
                period.end,
                placeGuarantee && firstPlacementTypePeriod == period.start
            )
        if (serviceNeed?.placementType == type) {
            tx.insertServiceNeed(
                placement.id,
                period.start,
                period.end,
                serviceNeed.optionId,
                ShiftCareType.fromBoolean(serviceNeed.shiftCare),
                confirmedBy = null,
                confirmedAt = null
            )
        }
        placement
    }
}

fun createPlacement(
    tx: Database.Transaction,
    childId: ChildId,
    unitId: DaycareId,
    period: FiniteDateRange,
    type: PlacementType,
    serviceNeed: ApplicationServiceNeed? = null,
    useFiveYearsOldDaycare: Boolean = false,
    placeGuarantee: Boolean
): List<Placement> {
    val placementTypePeriods =
        if (useFiveYearsOldDaycare) {
            resolveFiveYearOldPlacementPeriods(tx, childId, listOf(period to type))
        } else {
            listOf(period to type)
        }
    return createPlacements(
        tx,
        childId,
        unitId,
        placementTypePeriods,
        serviceNeed,
        placeGuarantee = placeGuarantee
    )
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
        clearServiceNeedsFromPeriod(
            this,
            old.id,
            FiniteDateRange(old.startDate, startDate.minusDays(1))
        )
    }
    if (endDate.isBefore(old.endDate)) {
        clearGroupPlacementsAfter(id, endDate)
        clearServiceNeedsFromPeriod(this, old.id, FiniteDateRange(endDate.plusDays(1), old.endDate))
    }
    clearOldPlacements(
        childId = old.childId,
        from = startDate,
        to = endDate,
        excludePlacement = id,
        aclAuth = aclAuth
    )

    try {
        val placementTypePeriods =
            if (useFiveYearsOldDaycare) {
                resolveFiveYearOldPlacementPeriods(
                    this,
                    old.childId,
                    listOf(FiniteDateRange(startDate, endDate) to old.type)
                )
            } else {
                listOf(FiniteDateRange(startDate, endDate) to old.type)
            }
        val (updatedPeriods, newPeriods) =
            placementTypePeriods.partition { (period, type) ->
                period.overlaps(FiniteDateRange(old.startDate, old.endDate)) && type == old.type
            }

        updatedPeriods.firstOrNull()?.let { (period, _) ->
            updatePlacementStartAndEndDate(id, period.start, period.end)
        }
            ?: run {
                checkAclAuth(aclAuth, old)
                cancelPlacement(old.id)
            }

        newPeriods.forEach { (period, type) ->
            insertPlacement(
                type,
                old.childId,
                old.unitId,
                period.start,
                period.end,
                old.placeGuarantee
            )
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
    if (endDate.isBefore(startDate)) {
        throw BadRequest("Must not end before even starting")
    }

    val daycarePlacement =
        getDaycarePlacement(daycarePlacementId)
            ?: throw NotFound("Placement $daycarePlacementId does not exist")

    if (
        startDate.isBefore(daycarePlacement.startDate) || endDate.isAfter(daycarePlacement.endDate)
    ) {
        throw BadRequest("Group placement must be within the daycare placement")
    }

    val group = getDaycareGroup(groupId) ?: throw NotFound("Group $groupId does not exist")
    if (group.daycareId != daycarePlacement.daycare.id) {
        throw BadRequest("Group is in wrong unit")
    }
    if (
        group.startDate.isAfter(startDate) ||
            (group.endDate != null && group.endDate.isBefore(endDate))
    ) {
        throw BadRequest("Group is not active for the full duration")
    }

    val identicalBefore =
        getIdenticalPrecedingGroupPlacement(daycarePlacementId, groupId, startDate)
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

fun Database.Transaction.transferGroup(
    groupPlacementId: GroupPlacementId,
    groupId: GroupId,
    startDate: LocalDate
) {
    val groupPlacement =
        getDaycareGroupPlacement(groupPlacementId) ?: throw NotFound("Group placement not found")

    if (
        getDaycareGroup(groupPlacement.groupId!!)?.daycareId != getDaycareGroup(groupId)?.daycareId
    ) {
        throw BadRequest("Cannot transfer to a group in different unit")
    }

    val placement = getPlacement(groupPlacement.daycarePlacementId)

    when {
        startDate.isBefore(groupPlacement.startDate) -> {
            throw BadRequest(
                "Cannot transfer to another group before the original placement even starts"
            )
        }
        startDate.isEqual(groupPlacement.startDate) -> {
            if (groupId == groupPlacement.groupId) {
                return // no changes requested
            }

            deleteGroupPlacement(groupPlacementId)
            if (placement != null) {
                clearCalendarEventAttendees(
                    placement.childId,
                    placement.unitId,
                    FiniteDateRange(startDate, LocalDate.MAX)
                )
            }
        }
        startDate.isAfter(groupPlacement.startDate) -> {
            if (startDate.isAfter(groupPlacement.endDate)) {
                throw BadRequest(
                    "Cannot transfer to another group after the original placement has already ended."
                )
            }

            updateGroupPlacementEndDate(groupPlacementId, startDate.minusDays(1))
            if (placement != null) {
                clearCalendarEventAttendees(
                    placement.childId,
                    placement.unitId,
                    FiniteDateRange(startDate, LocalDate.MAX)
                )
            }
        }
    }

    createGroupPlacement(
        groupPlacement.daycarePlacementId,
        groupId,
        startDate,
        groupPlacement.endDate
    )
}

private fun Database.Transaction.clearOldPlacements(
    childId: ChildId,
    from: LocalDate,
    to: LocalDate,
    excludePlacement: PlacementId? = null,
    aclAuth: AclAuthorization = AclAuthorization.All
) {
    if (from.isAfter(to)) throw IllegalArgumentException("inverted range")

    getPlacementsForChildDuring(childId, from, to)
        .filter { placement ->
            if (excludePlacement != null) placement.id != excludePlacement else true
        }
        .forEach { old ->
            if (old.endDate.isBefore(from) || old.startDate.isAfter(to)) {
                throw Error("bug discovered: query returned non-overlapping placement")
            }
            when {
                // old placement is within new placement (or identical)
                !old.startDate.isBefore(from) && !old.endDate.isAfter(to) -> {
                    checkAclAuth(aclAuth, old)
                    cancelPlacement(old.id).let {}
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

private fun Database.Transaction.clearFutureNonPreschoolRelatedPlacements(
    childId: ChildId,
    startingFrom: LocalDate,
) {
    val schoolYears = generateSchoolYearDateRanges(startingFrom)
    val futurePlacements = getPlacementsForChildDuring(childId, startingFrom, null)
    val futureSchoolYearsWithPreschoolPlacements =
        futurePlacements
            .filter {
                listOf(
                        PlacementType.PREPARATORY,
                        PlacementType.PREPARATORY_DAYCARE,
                        PlacementType.PRESCHOOL,
                        PlacementType.PRESCHOOL_DAYCARE,
                        PlacementType.PRESCHOOL_CLUB
                    )
                    .contains(it.type)
            }
            .flatMap { placement ->
                schoolYears.filter {
                    it.overlaps(FiniteDateRange(placement.startDate, placement.endDate))
                }
            }
    futurePlacements.forEach { placement ->
        val dateRange = FiniteDateRange(placement.startDate, placement.endDate)
        if (futureSchoolYearsWithPreschoolPlacements.none { it.overlaps(dateRange) }) {
            cancelPlacement(placement.id)
        }
    }
}

fun generateSchoolYearDateRanges(startingFrom: LocalDate) =
    (0..9)
        .map { startingFrom.plusYears(it.toLong()) }
        .map {
            FiniteDateRange(
                it.withMonth(8).withDayOfMonth(1),
                it.plusYears(1).withMonth(7).withDayOfMonth(31)
            )
        }

fun Database.Transaction.movePlacementStartDateLater(
    placement: Placement,
    newStartDate: LocalDate
) {
    if (newStartDate.isBefore(placement.startDate))
        throw IllegalArgumentException("Use this method only for shortening placement")

    clearGroupPlacementsBefore(placement.id, newStartDate)
    clearServiceNeedsFromPeriod(
        this,
        placement.id,
        FiniteDateRange(placement.startDate, newStartDate.minusDays(1))
    )
    updatePlacementStartDate(placement.id, newStartDate)
}

fun Database.Transaction.movePlacementEndDateEarlier(placement: Placement, newEndDate: LocalDate) {
    if (newEndDate.isAfter(placement.endDate))
        throw IllegalArgumentException("Use this method only for shortening placement")

    clearGroupPlacementsAfter(placement.id, newEndDate)
    clearServiceNeedsFromPeriod(
        this,
        placement.id,
        FiniteDateRange(newEndDate.plusDays(1), placement.endDate)
    )
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
        endDate = placement.endDate,
        placeGuarantee = placement.placeGuarantee
    )
}

private fun checkAclAuth(aclAuth: AclAuthorization, placement: Placement) {
    if (!aclAuth.isAuthorized(placement.unitId)) {
        throw Conflict("Not authorized to modify placement (placementId: ${placement.id})")
    }
}

data class UnitChildrenCapacityFactors(
    val childId: PersonId,
    val assistanceNeedFactor: Double,
    val serviceNeedFactor: Double
)

fun Database.Read.getUnitChildrenCapacities(
    unitId: DaycareId,
    date: LocalDate
): List<UnitChildrenCapacityFactors> {
    return createQuery {
            sql(
                """
SELECT
    ch.id child_id,
    MAX(COALESCE(an.capacity_factor, 1)) assistance_need_factor,
    MAX(CASE
        WHEN u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
        WHEN extract(YEARS FROM age(${bind(date)}, ch.date_of_birth)) < 3 THEN coalesce(sno.occupancy_coefficient_under_3y, default_sno.occupancy_coefficient_under_3y)
        ELSE coalesce(sno.occupancy_coefficient, default_sno.occupancy_coefficient, 1)
    END) AS service_need_factor
FROM realized_placement_one(${bind(date)}) pl
JOIN daycare u ON u.id = pl.unit_id
JOIN person ch ON ch.id = pl.child_id
LEFT JOIN service_need sn on sn.placement_id = pl.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(date)}
LEFT JOIN service_need_option sno on sn.option_id = sno.id
LEFT JOIN service_need_option default_sno on pl.placement_type = default_sno.valid_placement_type AND default_sno.default_option
LEFT JOIN assistance_factor an ON an.child_id = ch.id AND an.valid_during @> ${bind(date)}
WHERE pl.unit_id = ${bind(unitId)}
GROUP BY ch.id
"""
            )
        }
        .toList<UnitChildrenCapacityFactors>()
}

fun Database.Read.getDetailedDaycarePlacements(
    daycareId: DaycareId?,
    childId: ChildId?,
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
                groupPlacements =
                    groupPlacements.filter { it.daycarePlacementId == daycarePlacement.id },
                serviceNeeds = serviceNeeds.filter { it.placementId == daycarePlacement.id },
                terminatedBy = daycarePlacement.terminatedBy,
                terminationRequestedDate = daycarePlacement.terminationRequestedDate,
                updated = daycarePlacement.updated,
                placeGuarantee = daycarePlacement.placeGuarantee
            )
        }
        .map(::addMissingGroupPlacements)
        .toSet()
}

private fun addMissingGroupPlacements(
    daycarePlacement: DaycarePlacementWithDetails
): DaycarePlacementWithDetails {
    // calculate unique sorted events from start dates and exclusive end dates
    val eventDates =
        setOf<LocalDate>(
                daycarePlacement.startDate,
                *daycarePlacement.groupPlacements.map { it.startDate }.toTypedArray(),
                *daycarePlacement.groupPlacements.map { it.endDate.plusDays(1) }.toTypedArray(),
                daycarePlacement.endDate.plusDays(1)
            )
            .sorted()

    val groupPlacements = mutableListOf<DaycareGroupPlacement>()
    for (i in 0..eventDates.size - 2) {
        val startDate = eventDates[i]
        val endDate = eventDates[i + 1].minusDays(1) // convert back to inclusive
        val groupPlacement =
            daycarePlacement.groupPlacements.find { groupPlacement ->
                startDate.isEqual(groupPlacement.startDate)
            }
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

fun getMissingGroupPlacements(tx: Database.Read, unitId: DaycareId): List<MissingGroupPlacement> {
    val evakaLaunch = LocalDate.of(2020, 3, 1)

    val missingGroupPlacements =
        tx.createQuery {
                sql(
                    """
WITH missing_group_placement AS (
    SELECT p.id, p.type, daterange(p.start_date, p.end_date, '[]') AS placement_period, p.child_id,
        multirange(daterange(greatest(p.start_date, ${bind(evakaLaunch)}), p.end_date, '[]')) - coalesce(dgp.ranges, '{}'::datemultirange) AS ranges
    FROM placement p
    LEFT JOIN LATERAL (
        SELECT range_agg(daterange(dgp.start_date, dgp.end_date, '[]')) AS ranges
        FROM daycare_group_placement dgp
        WHERE p.id = dgp.daycare_placement_id
    ) dgp ON true
    WHERE p.unit_id = ${bind(unitId)} AND daterange(p.start_date, p.end_date, '[]') && daterange(${bind(evakaLaunch)}, NULL)
    AND NOT isempty(multirange(daterange(greatest(p.start_date, ${bind(evakaLaunch)}), p.end_date, '[]')) - coalesce(dgp.ranges, '{}'::datemultirange))
)
SELECT
    FALSE AS backup,
    p.id AS placement_id,
    p.type AS placement_type,
    p.placement_period,
    unnest(p.ranges) AS gap,
    p.child_id,
    c.first_name,
    c.last_name,
    c.date_of_birth,
    '[]' AS from_units,
    sn.service_needs
FROM missing_group_placement p
JOIN person c ON p.child_id = c.id
JOIN LATERAL (
    SELECT coalesce(jsonb_agg(jsonb_build_object(
        'startDate', sn.start_date,
        'endDate', sn.end_date,
        'nameFi', sno.name_fi
    )), '[]'::jsonb) AS service_needs
    FROM service_need sn
    JOIN service_need_option sno ON sn.option_id = sno.id
    WHERE p.id = sn.placement_id
) sn ON true
"""
                )
            }
            .toList<MissingGroupPlacement>()

    val missingBackupCareGroups =
        tx.createQuery {
                sql(
                    """
SELECT
    TRUE AS backup,
    bc.id AS placement_id,
    NULL AS placement_type,
    daterange(bc.start_date, bc.end_date, '[]') AS placement_period,
    '[]' AS service_needs,
    daterange(bc.start_date, bc.end_date, '[]') AS gap,
    bc.child_id,
    c.first_name,
    c.last_name,
    c.date_of_birth,
    p.units AS from_units
FROM backup_care bc
JOIN person c ON bc.child_id = c.id
JOIN LATERAL (
  SELECT coalesce(jsonb_agg(DISTINCT u.name), '[]'::jsonb) AS units
  FROM placement p
  JOIN daycare u ON u.id = p.unit_id
  WHERE p.child_id = bc.child_id
    AND daterange(p.start_date, p.end_date, '[]') && daterange(bc.start_date, bc.end_date, '[]')
) p ON TRUE
WHERE bc.unit_id = ${bind(unitId)} AND bc.group_id IS NULL
    AND daterange(bc.start_date, bc.end_date, '[]') && daterange(${bind(evakaLaunch)}, NULL)
"""
                )
            }
            .toList<MissingGroupPlacement>()

    return missingGroupPlacements + missingBackupCareGroups
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
    @Nested("child") val child: ChildBasics,
    @Nested("daycare") val daycare: DaycareBasics,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: PlacementType,
    val missingServiceNeedDays: Int,
    val terminationRequestedDate: LocalDate?,
    val updated: HelsinkiDateTime?,
    @Nested("terminated_by") val terminatedBy: EvakaUser?,
    val placeGuarantee: Boolean
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
    val terminatedBy: EvakaUser?,
    val updated: HelsinkiDateTime?,
    val placeGuarantee: Boolean
)

data class PlacementResponse(
    val placements: Set<DaycarePlacementWithDetails>,
    val permittedPlacementActions: Map<PlacementId, Set<Action.Placement>>,
    val permittedServiceNeedActions: Map<ServiceNeedId, Set<Action.ServiceNeed>>,
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
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    @Json val fromUnits: List<String>, // for backup care
    @Json val serviceNeeds: List<MissingGroupPlacementServiceNeed>,
    val gap: FiniteDateRange
)

data class MissingGroupPlacementServiceNeed(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val nameFi: String
)

data class ChildBasics(
    val id: ChildId,
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
    val enabledPilotFeatures: List<PilotFeature>,
    val language: Language
)

data class ChildDaycareGroupPlacement(
    val childId: ChildId,
    val groupId: GroupId,
    val groupName: String
)
