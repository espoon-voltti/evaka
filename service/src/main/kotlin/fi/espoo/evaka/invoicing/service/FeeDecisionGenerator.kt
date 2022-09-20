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
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionPlacement
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.FridgeFamily
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.PlacementWithServiceNeed
import fi.espoo.evaka.invoicing.domain.ServiceNeedValue
import fi.espoo.evaka.invoicing.domain.calculateBaseFee
import fi.espoo.evaka.invoicing.domain.calculateFeeBeforeFeeAlterations
import fi.espoo.evaka.invoicing.domain.decisionContentsAreEqual
import fi.espoo.evaka.invoicing.domain.toFeeAlterationsWithEffects
import fi.espoo.evaka.placement.Placement
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapRow
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal fun Database.Transaction.handleFeeDecisionChanges(
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    from: LocalDate,
    headOfFamily: PersonId,
    families: List<FridgeFamily>
) {
    val children = families.flatMap { it.children }.toSet()
    val prices = getFeeThresholds(from)
    val partnerIds = families.mapNotNull { it.partner }
    val childIds = children.map { it.id }

    val allIncomes =
        getIncomesFrom(jsonMapper, incomeTypesProvider, partnerIds + headOfFamily + childIds, from)

    val adultIncomes = allIncomes.filter { (partnerIds + headOfFamily).contains(it.personId) }
    val childIncomes =
        childIds.map { childId -> childId to allIncomes.filter { it.personId == childId } }.toMap()

    val feeAlterations =
        getFeeAlterationsFrom(children.map { it.id }, from) +
            addECHAFeeAlterations(children, adultIncomes)

    val placements = getPaidPlacements(from, children).toMap()
    val invoicedUnits = getUnitsThatAreInvoiced()

    val newDrafts =
        generateFeeDecisions(
            from,
            headOfFamily,
            families,
            placements,
            prices,
            adultIncomes,
            childIncomes,
            feeAlterations,
            invoicedUnits
        )

    lockFeeDecisionsForHeadOfFamily(headOfFamily)

    val existingDrafts =
        findFeeDecisionsForHeadOfFamily(headOfFamily, null, listOf(FeeDecisionStatus.DRAFT))
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
                    .map { period ->
                        val pair = pairs.find { it.validDuring.overlaps(period) }
                        period to
                            if (pair != null) {
                                decision.copy(
                                    validDuring = period,
                                    children =
                                        (decision.children + pair.children)
                                            .sortedBy { it.siblingDiscount }
                                            .sortedByDescending { it.child.dateOfBirth }
                                )
                            } else decision.copy(validDuring = period)
                    }
            }
            .let { mergePeriods(it) }
            .map { it.second }
    }

    val updatedDecisions =
        updateExistingDecisions(from, newDrafts, existingDrafts, combinedActiveDecisions)
    deleteFeeDecisions(existingDrafts.map { it.id })
    upsertFeeDecisions(updatedDecisions.updatedDrafts + updatedDecisions.updatedActiveDecisions)
}

private fun generateFeeDecisions(
    from: LocalDate,
    headOfFamily: PersonId,
    families: List<FridgeFamily>,
    allPlacements: Map<ChildWithDateOfBirth, List<Pair<DateRange, PlacementWithServiceNeed>>>,
    prices: List<FeeThresholds>,
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
            allPlacements.flatMap { (_, placements) -> placements.map { it.first } } +
            feeAlterations.map { DateRange(it.validFrom, it.validTo) }

    return asDistinctPeriods(periods, DateRange(from, null))
        .mapNotNull { period ->
            val family = families.find { it.period.overlaps(period) } ?: return@mapNotNull null

            val price =
                prices.find { it.validDuring.contains(period) }
                    ?: error(
                        "Missing price for period ${period.start} - ${period.end}, cannot generate fee decision"
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
                        .sortedByDescending { (child, _) -> child.dateOfBirth }
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

                            val siblingDiscountMultiplier =
                                price.siblingDiscountMultiplier(index + 1)
                            val baseFee =
                                calculateBaseFee(
                                    price,
                                    family.getSize(),
                                    familyIncomes + listOfNotNull(childPeriodIncome.get(child.id))
                                )
                            val feeBeforeAlterations =
                                calculateFeeBeforeFeeAlterations(
                                    baseFee,
                                    placement.serviceNeed.feeCoefficient,
                                    siblingDiscountMultiplier,
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
                                child,
                                FeeDecisionPlacement(placement.unitId, placement.type),
                                FeeDecisionServiceNeed(
                                    placement.serviceNeed.feeCoefficient,
                                    placement.serviceNeed.contractDaysPerMonth,
                                    placement.serviceNeed.feeDescriptionFi,
                                    placement.serviceNeed.feeDescriptionSv,
                                    placement.missingServiceNeed
                                ),
                                baseFee,
                                price.siblingDiscountPercent(index + 1),
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
                    children = children.sortedBy { it.siblingDiscount }
                )
        }
        .let { mergePeriods(it, ::decisionContentsAreEqual) }
        .map { (period, decision) -> decision.withValidity(period) }
}

private fun Database.Read.getUnitsThatAreInvoiced(): List<DaycareId> {
    return createQuery("SELECT id FROM daycare WHERE invoiced_by_municipality")
        .mapTo<DaycareId>()
        .toList()
}

internal fun Database.Read.getPaidPlacements(
    from: LocalDate,
    children: Set<ChildWithDateOfBirth>
): List<Pair<ChildWithDateOfBirth, List<Pair<DateRange, PlacementWithServiceNeed>>>> {
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
                .map { row -> row.mapColumn<DateRange>("range") to row.mapRow<ServiceNeedValue>() }
                .toList()

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
                .map { row ->
                    row.mapColumn<PlacementType>("valid_placement_type") to
                        row.mapRow<ServiceNeedValue>()
                }
                .toMap()

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
                                serviceNeed = serviceNeed?.second
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
    end_date
FROM placement
WHERE child_id = :childId AND end_date >= :from AND NOT type = ANY(:excludedTypes::placement_type[])
        """.trimIndent(
            )
        )
        .bind("childId", childId)
        .bind("from", from)
        .bind("excludedTypes", excludedPlacementTypes)
        .mapTo<Placement>()
        .toList()
}
