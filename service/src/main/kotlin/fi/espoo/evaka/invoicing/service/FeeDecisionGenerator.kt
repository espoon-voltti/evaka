// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.invoicing.data.deleteFeeDecisions
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.getFeeAlterationsFrom
import fi.espoo.evaka.invoicing.data.getFeeThresholds
import fi.espoo.evaka.invoicing.data.getIncomesFrom
import fi.espoo.evaka.invoicing.data.lockFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.updateFeeDecisionStatusAndDates
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionDifference
import fi.espoo.evaka.invoicing.domain.FeeDecisionPlacement
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.FridgeFamily
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.PersonBasic
import fi.espoo.evaka.invoicing.domain.PlacementWithServiceNeed
import fi.espoo.evaka.invoicing.domain.ServiceNeedValue
import fi.espoo.evaka.invoicing.domain.calculateBaseFee
import fi.espoo.evaka.invoicing.domain.calculateFeeBeforeFeeAlterations
import fi.espoo.evaka.invoicing.domain.decisionContentsAreEqual
import fi.espoo.evaka.invoicing.domain.toFeeAlterationsWithEffects
import fi.espoo.evaka.placement.Placement
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOptionFee
import fi.espoo.evaka.serviceneed.getServiceNeedOptionFees
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.periodsCanMerge
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal fun Database.Transaction.handleFeeDecisionChanges(
    clock: EvakaClock,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    from: LocalDate,
    headOfFamily: PersonId,
    families: List<FridgeFamily>
) {
    val children = families.flatMap { it.children }.toSet()
    val prices = getFeeThresholds(from)
    val fees = getServiceNeedOptionFees(from).groupBy { it.serviceNeedOptionId }
    val partnerIds = families.mapNotNull { it.partner }
    val childIds = children.map { it.id }

    val allIncomes =
        getIncomesFrom(jsonMapper, incomeTypesProvider, partnerIds + headOfFamily + childIds, from)

    val adultIncomes = allIncomes.filter { (partnerIds + headOfFamily).contains(it.personId) }
    val childIncomes =
        childIds.map { childId -> childId to allIncomes.filter { it.personId == childId } }.toMap()

    val feeAlterations =
        getFeeAlterationsFrom(children.map { it.id }, from) +
            addECHAFeeAlterations(children.map { it.id }.toSet(), adultIncomes)

    val placements = getPaidPlacements(from, children).toMap()
    val invoicedUnits = getUnitsThatAreInvoiced()

    lockFeeDecisionsForHeadOfFamily(headOfFamily)

    val activeDecisions =
        findFeeDecisionsForHeadOfFamily(
            headOfFamily,
            null,
            listOf(
                FeeDecisionStatus.WAITING_FOR_SENDING,
                FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                FeeDecisionStatus.SENT
            )
        )
    val previousDrafts =
        findFeeDecisionsForHeadOfFamily(
            headOfFamily,
            from.minusDays(1).let { date -> DateRange(date, date) },
            listOf(FeeDecisionStatus.DRAFT)
        )

    val newDraftsWithoutDifference =
        generateFeeDecisions(
            from,
            headOfFamily,
            families,
            placements,
            prices,
            fees,
            adultIncomes,
            childIncomes,
            feeAlterations,
            invoicedUnits
        )
    val newDrafts =
        newDraftsWithoutDifference.map { draft ->
            if (draft.isEmpty()) {
                return@map draft
            }
            val draftDifferences =
                (newDraftsWithoutDifference + previousDrafts)
                    .filter { other ->
                        !other.isEmpty() && periodsCanMerge(other.validDuring, draft.validDuring)
                    }
                    .flatMap { other -> FeeDecisionDifference.getDifference(other, draft) }
            if (draftDifferences.isNotEmpty()) {
                return@map draft.copy(difference = draftDifferences.toSet())
            }
            val activeDifferences =
                activeDecisions
                    .filter { active -> periodsCanMerge(active.validDuring, draft.validDuring) }
                    .flatMap { active -> FeeDecisionDifference.getDifference(active, draft) }
            draft.copy(difference = activeDifferences.toSet())
        }

    val existingDrafts =
        findFeeDecisionsForHeadOfFamily(headOfFamily, null, listOf(FeeDecisionStatus.DRAFT))
    val partnerDecisions =
        activeDecisions
            .mapNotNull { it.partnerId }
            .flatMap { partnerId ->
                findFeeDecisionsForHeadOfFamily(
                    partnerId,
                    null,
                    listOf(
                        FeeDecisionStatus.WAITING_FOR_SENDING,
                        FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                        FeeDecisionStatus.SENT
                    )
                )
            }
    val combinedActiveDecisions = run {
        activeDecisions
            .flatMap { decision ->
                val pairs =
                    partnerDecisions.filter {
                        decision.headOfFamilyId == it.partnerId &&
                            decision.partnerId == it.headOfFamilyId &&
                            decision.validDuring.overlaps(it.validDuring)
                    }
                asDistinctPeriods(
                        pairs.map { it.validDuring } + decision.validDuring,
                        decision.validDuring
                    )
                    .flatMap { period ->
                        val pair = pairs.find { it.validDuring.overlaps(period) }
                        if (pair != null) {
                            listOf(
                                period to
                                    decision.copy(
                                        validDuring = period,
                                        children =
                                            (decision.children + pair.children)
                                                .sortedBy { it.siblingDiscount }
                                                .sortedByDescending { it.child.dateOfBirth }
                                    ),
                                period to
                                    pair.copy(
                                        validDuring = period,
                                        children =
                                            (decision.children + pair.children)
                                                .sortedBy { it.siblingDiscount }
                                                .sortedByDescending { it.child.dateOfBirth }
                                    ),
                            )
                        } else listOf(period to decision.copy(validDuring = period))
                    }
            }
            .let { mergePeriods(it) }
            .map { it.second }
    }

    val updatedDecisions =
        updateExistingDecisions(
            clock.now(),
            from,
            newDrafts,
            existingDrafts,
            combinedActiveDecisions
        )
    deleteFeeDecisions(existingDrafts.map { it.id })
    val updatedDrafts = preserveCreatedDates(updatedDecisions.updatedDrafts, existingDrafts)
    deleteFeeDecisions(existingDrafts.map { it.id })
    updateFeeDecisionStatusAndDates(updatedDecisions.updatedActiveDecisions)
    upsertFeeDecisions(updatedDrafts)
}

fun preserveCreatedDates(
    updatedDrafts: List<FeeDecision>,
    existingDrafts: List<FeeDecision>
): List<FeeDecision> {
    return updatedDrafts.map { updatedDraft ->
        val existingDraft = existingDrafts.find { decisionContentsAreEqual(it, updatedDraft) }
        if (existingDraft != null) {
            updatedDraft.copy(created = existingDraft.created)
        } else updatedDraft
    }
}

private fun generateFeeDecisions(
    from: LocalDate,
    headOfFamily: PersonId,
    families: List<FridgeFamily>,
    allPlacements: Map<PersonBasic, List<Pair<DateRange, PlacementWithServiceNeed>>>,
    prices: List<FeeThresholds>,
    fees: Map<ServiceNeedOptionId, List<ServiceNeedOptionFee>>,
    incomes: List<Income>,
    childIncomes: Map<ChildId, List<Income>>,
    feeAlterations: List<FeeAlteration>,
    invoicedUnits: List<DaycareId>
): List<FeeDecision> {
    val periods =
        families.map { it.period } +
            incomes.map { DateRange(it.validFrom, it.validTo) } +
            childIncomes.values.flatten().map { DateRange(it.validFrom, it.validTo) } +
            prices.map { it.validDuring } +
            fees.flatMap { it.value.map { fee -> fee.validity } } +
            allPlacements.flatMap { (_, placements) -> placements.map { it.first } } +
            feeAlterations.map { DateRange(it.validFrom, it.validTo) }

    return asDistinctPeriods(periods, DateRange(from, null))
        .mapNotNull { period ->
            val family = families.find { it.period.overlaps(period) } ?: return@mapNotNull null

            val price =
                prices.singleOrNull() { it.validDuring.contains(period) }
                    ?: error(
                        "Missing price or multiple prices for period ${period.start} - ${period.end}, cannot generate fee decision"
                    )

            val income =
                incomes
                    .find {
                        headOfFamily == it.personId &&
                            DateRange(it.validFrom, it.validTo).contains(period)
                    }
                    ?.toDecisionIncome()

            val partnerIncome =
                family.partner?.let { partner ->
                    incomes
                        .find {
                            partner == it.personId &&
                                DateRange(it.validFrom, it.validTo).contains(period)
                        }
                        ?.toDecisionIncome()
                }

            val childPeriodIncome =
                childIncomes.mapValues { (_, incomes) ->
                    incomes
                        .find {
                            DateRange(it.validFrom, it.validTo).contains(period) &&
                                it.effect == IncomeEffect.INCOME
                        }
                        ?.toDecisionIncome()
                }

            val validPlacements =
                family.children.mapNotNull { child ->
                    val childPlacements = allPlacements[child] ?: listOf()
                    val validPlacement =
                        childPlacements.find { (dateRange, _) -> dateRange.contains(period) }
                    validPlacement?.let { (_, placement) -> child to placement }
                }

            val children =
                if (validPlacements.isNotEmpty()) {
                    val familyIncomes =
                        family.partner?.let { listOf(income, partnerIncome) } ?: listOf(income)

                    validPlacements
                        .sortedWith(
                            compareByDescending<Pair<PersonBasic, PlacementWithServiceNeed>> {
                                    (child, _) ->
                                    child.dateOfBirth
                                }
                                .thenByDescending { (child, _) -> child.ssn }
                                .thenBy { (child, _) -> child.id }
                        )
                        .mapIndexed { index, (child, placement) ->
                            if (!family.children.contains(child)) {
                                return@mapIndexed null
                            }

                            val placedIntoInvoicedUnit =
                                invoicedUnits.any { unit -> unit == placement.unitId }
                            if (!placedIntoInvoicedUnit) {
                                return@mapIndexed null
                            }

                            val serviceNeedIsFree =
                                placement.serviceNeed.feeCoefficient.compareTo(BigDecimal.ZERO) == 0
                            if (serviceNeedIsFree) {
                                return@mapIndexed null
                            }

                            val serviceNeedOptionFee =
                                fees[placement.serviceNeed.optionId]?.find {
                                    it.validity.contains(period)
                                }
                            val siblingDiscount =
                                serviceNeedOptionFee?.siblingDiscount(index + 1)
                                    ?: price.siblingDiscount(index + 1)
                            val baseFee =
                                serviceNeedOptionFee?.baseFee
                                    ?: calculateBaseFee(
                                        price,
                                        family.getSize(),
                                        familyIncomes +
                                            listOfNotNull(childPeriodIncome.get(child.id))
                                    )
                            val feeBeforeAlterations =
                                calculateFeeBeforeFeeAlterations(
                                    baseFee,
                                    placement.serviceNeed.feeCoefficient,
                                    siblingDiscount,
                                    price.minFee
                                )
                            val relevantFeeAlterations =
                                feeAlterations.filter {
                                    it.personId == child.id &&
                                        DateRange(it.validFrom, it.validTo).contains(period)
                                }
                            val feeAlterationsWithEffects =
                                toFeeAlterationsWithEffects(
                                    feeBeforeAlterations,
                                    relevantFeeAlterations
                                )
                            val finalFee =
                                feeBeforeAlterations + feeAlterationsWithEffects.sumOf { it.effect }

                            FeeDecisionChild(
                                ChildWithDateOfBirth(child.id, child.dateOfBirth),
                                FeeDecisionPlacement(placement.unitId, placement.type),
                                FeeDecisionServiceNeed(
                                    placement.serviceNeed.optionId,
                                    placement.serviceNeed.feeCoefficient,
                                    placement.serviceNeed.contractDaysPerMonth,
                                    placement.serviceNeed.feeDescriptionFi,
                                    placement.serviceNeed.feeDescriptionSv,
                                    placement.missingServiceNeed
                                ),
                                baseFee,
                                siblingDiscount.percent,
                                feeBeforeAlterations,
                                feeAlterationsWithEffects,
                                finalFee,
                                childPeriodIncome.get(child.id)
                            )
                        }
                        .filterNotNull()
                } else {
                    listOf()
                }

            period to
                FeeDecision(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring = period,
                    status = FeeDecisionStatus.DRAFT,
                    decisionType = FeeDecisionType.NORMAL,
                    headOfFamilyId = headOfFamily,
                    partnerId = family.partner,
                    headOfFamilyIncome = income,
                    partnerIncome = partnerIncome,
                    familySize = family.getSize(),
                    feeThresholds = price.getFeeDecisionThresholds(family.getSize()),
                    children = children.sortedBy { it.siblingDiscount },
                    difference = emptySet()
                )
        }
        .let { mergePeriods(it, ::decisionContentsAreEqual) }
        .map { (period, decision) -> decision.withValidity(period) }
}

internal fun Database.Read.getPaidPlacements(
    from: LocalDate,
    children: Set<PersonBasic>
): List<Pair<PersonBasic, List<Pair<DateRange, PlacementWithServiceNeed>>>> {
    return children.map { child ->
        val placements = getActivePaidPlacements(child.id, from)
        if (placements.isEmpty())
            return@map child to listOf<Pair<DateRange, PlacementWithServiceNeed>>()

        val serviceNeeds =
            createQuery(
                    """
SELECT
    daterange(sn.start_date, sn.end_date, '[]') AS range, 
    sno.id AS option_id, 
    sno.fee_coefficient, 
    sno.contract_days_per_month,
    sno.fee_description_fi, 
    sno.fee_description_sv, 
    sno.voucher_value_description_fi, 
    sno.voucher_value_description_sv
FROM service_need sn
JOIN service_need_option sno ON sn.option_id = sno.id
WHERE sn.placement_id = ANY(:placementIds)
"""
                )
                .bind("placementIds", placements.map { it.id })
                .toList { column<DateRange>("range") to row<ServiceNeedValue>() }

        val defaultServiceNeeds =
            createQuery(
                    // language=sql
                    """
SELECT 
    valid_placement_type,
    id AS option_id,
    fee_coefficient,
    fee_description_fi,
    fee_description_sv,
    voucher_value_description_fi,
    voucher_value_description_sv
FROM service_need_option WHERE default_option
"""
                )
                .toMap { column<PlacementType>("valid_placement_type") to row<ServiceNeedValue>() }

        val placementsWithServiceNeedOptions = run {
            placements.flatMap { placement ->
                val placementPeriod = DateRange(placement.startDate, placement.endDate)
                asDistinctPeriods(serviceNeeds.map { it.first }, placementPeriod)
                    .map { period ->
                        val serviceNeed = serviceNeeds.find { it.first.contains(period) }
                        period to
                            PlacementWithServiceNeed(
                                unitId = placement.unitId,
                                type = placement.type,
                                serviceNeed =
                                    serviceNeed?.second
                                        ?: defaultServiceNeeds[placement.type]
                                        ?: error(
                                            "No default service need found for type ${placement.type}"
                                        ),
                                missingServiceNeed = serviceNeed == null
                            )
                    }
                    .let { mergePeriods(it) }
            }
        }

        child to placementsWithServiceNeedOptions
    }
}

private val excludedPlacementTypes =
    arrayOf(
        fi.espoo.evaka.placement.PlacementType.CLUB,
        fi.espoo.evaka.placement.PlacementType.TEMPORARY_DAYCARE,
        fi.espoo.evaka.placement.PlacementType.TEMPORARY_DAYCARE_PART_DAY,
        fi.espoo.evaka.placement.PlacementType.SCHOOL_SHIFT_CARE
    )

/**
 * Leaves out club and temporary placements since they shouldn't have an effect on fee or value
 * decisions
 */
private fun Database.Read.getActivePaidPlacements(
    childId: ChildId,
    from: LocalDate
): List<Placement> {
    return createQuery(
            """
SELECT
    id,
    type,
    child_id,
    unit_id,
    start_date,
    end_date,
    place_guarantee
FROM placement
WHERE child_id = :childId AND end_date >= :from AND NOT type = ANY(:excludedTypes::placement_type[])
        """
                .trimIndent()
        )
        .bind("childId", childId)
        .bind("from", from)
        .bind("excludedTypes", excludedPlacementTypes)
        .toList<Placement>()
}
