// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.invoicing.data.deleteFeeDecisions2
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily2
import fi.espoo.evaka.invoicing.data.getFeeAlterationsFrom
import fi.espoo.evaka.invoicing.data.getIncomesFrom
import fi.espoo.evaka.invoicing.data.getPricing
import fi.espoo.evaka.invoicing.data.lockFeeDecisionsForHeadOfFamily2
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions2
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision2
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionPlacement
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.PlacementWithServiceNeed
import fi.espoo.evaka.invoicing.domain.Pricing
import fi.espoo.evaka.invoicing.domain.ServiceNeedValue
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.invoicing.domain.calculateBaseFee
import fi.espoo.evaka.invoicing.domain.calculateFeeBeforeFeeAlterations
import fi.espoo.evaka.invoicing.domain.decisionContentsAreEqual
import fi.espoo.evaka.invoicing.domain.getSiblingDiscountPercent
import fi.espoo.evaka.invoicing.domain.toFeeAlterationsWithEffects
import fi.espoo.evaka.placement.Placement
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import org.jdbi.v3.core.kotlin.mapTo
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal fun Database.Transaction.handleFeeDecisionChanges2(
    objectMapper: ObjectMapper,
    from: LocalDate,
    headOfFamily: PersonData.JustId,
    partner: PersonData.JustId?,
    children: List<PersonData.WithDateOfBirth>
) {
    val familySize = 1 + (partner?.let { 1 } ?: 0) + children.size
    val prices = getPricing(from)
    val incomes = getIncomesFrom(objectMapper, listOfNotNull(headOfFamily.id, partner?.id), from)
    val feeAlterations =
        getFeeAlterationsFrom(children.map { it.id }, from) + addECHAFeeAlterations(children, incomes)

    val placements = getPaidPlacements2(from, children)
    val invoicedUnits = getUnitsThatAreInvoiced()

    val newDrafts =
        generateNewFeeDecisions2(
            from,
            headOfFamily,
            partner,
            familySize,
            placements,
            prices,
            incomes,
            feeAlterations,
            invoicedUnits
        )

    lockFeeDecisionsForHeadOfFamily2(headOfFamily.id)

    val existingDrafts =
        findFeeDecisionsForHeadOfFamily2(headOfFamily.id, null, listOf(FeeDecisionStatus.DRAFT))
    val activeDecisions = findFeeDecisionsForHeadOfFamily2(
        headOfFamily.id,
        null,
        listOf(
            FeeDecisionStatus.WAITING_FOR_SENDING,
            FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
            FeeDecisionStatus.SENT
        )
    )

    val updatedDecisions = updateExistingDecisions(from, newDrafts, existingDrafts, activeDecisions)
    deleteFeeDecisions2(existingDrafts.map { it.id })
    upsertFeeDecisions2(updatedDecisions)
}

private fun generateNewFeeDecisions2(
    from: LocalDate,
    headOfFamily: PersonData.JustId,
    partner: PersonData.JustId?,
    familySize: Int,
    allPlacements: List<Pair<PersonData.WithDateOfBirth, List<Pair<DateRange, PlacementWithServiceNeed>>>>,
    prices: List<Pair<DateRange, Pricing>>,
    incomes: List<Income>,
    feeAlterations: List<FeeAlteration>,
    invoicedUnits: List<UUID>
): List<FeeDecision2> {
    val periods = incomes.map { DateRange(it.validFrom, it.validTo) } +
        prices.map { it.first } +
        allPlacements.flatMap { (_, placements) ->
            placements.map { it.first }
        } +
        feeAlterations.map { DateRange(it.validFrom, it.validTo) }

    return asDistinctPeriods(periods, DateRange(from, null))
        .map { period ->
            val price = prices.find { it.first.contains(period) }?.second
                ?: error("Missing price for period ${period.start} - ${period.end}, cannot generate fee decision")

            val income = incomes
                .find { headOfFamily.id == it.personId && DateRange(it.validFrom, it.validTo).contains(period) }
                ?.toDecisionIncome()

            val partnerIncome =
                partner?.let {
                    incomes
                        .find {
                            partner.id == it.personId && DateRange(
                                it.validFrom,
                                it.validTo
                            ).contains(period)
                        }
                        ?.toDecisionIncome()
                }

            val validPlacements = allPlacements
                .map { (child, placements) -> child to placements.find { it.first.contains(period) }?.second }
                .filter { (_, placement) -> placement != null }
                .map { (child, placement) -> child to placement!! }

            val children = if (validPlacements.isNotEmpty()) {
                val familyIncomes = partner?.let { listOf(income, partnerIncome) } ?: listOf(income)

                val baseFee = calculateBaseFee(price, familySize, familyIncomes)
                validPlacements
                    .sortedByDescending { (child, _) -> child.dateOfBirth }
                    .mapIndexed { index, (child, placement) ->
                        val placedIntoInvoicedUnit = invoicedUnits.any { unit -> unit == placement.unitId }
                        if (!placedIntoInvoicedUnit) {
                            return@mapIndexed null
                        }

                        val serviceNeedIsFree = placement.serviceNeed.feeCoefficient.compareTo(BigDecimal.ZERO) == 0
                        if (serviceNeedIsFree) {
                            return@mapIndexed null
                        }

                        val siblingDiscount = getSiblingDiscountPercent(index + 1)
                        val feeBeforeAlterations = calculateFeeBeforeFeeAlterations(baseFee, placement.serviceNeed.feeCoefficient, siblingDiscount)
                        val relevantFeeAlterations = feeAlterations.filter {
                            it.personId == child.id && DateRange(it.validFrom, it.validTo).contains(period)
                        }
                        val feeAlterationsWithEffects =
                            toFeeAlterationsWithEffects(feeBeforeAlterations, relevantFeeAlterations)
                        val finalFee = feeBeforeAlterations + feeAlterationsWithEffects.sumBy { it.effect }

                        FeeDecisionChild(
                            child,
                            FeeDecisionPlacement(UnitData.JustId(placement.unitId), placement.type),
                            FeeDecisionServiceNeed(
                                placement.serviceNeed.feeCoefficient,
                                placement.serviceNeed.feeDescriptionFi,
                                placement.serviceNeed.feeDescriptionSv
                            ),
                            baseFee,
                            siblingDiscount,
                            feeBeforeAlterations,
                            feeAlterationsWithEffects,
                            finalFee
                        )
                    }.filterNotNull()
            } else {
                listOf()
            }

            period to FeeDecision2(
                id = UUID.randomUUID(),
                validDuring = period,
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamily = headOfFamily,
                partner = partner,
                headOfFamilyIncome = income,
                partnerIncome = partnerIncome,
                familySize = familySize,
                pricing = price,
                children = children.sortedBy { it.siblingDiscount }
            )
        }
        .let { mergePeriods(it, ::decisionContentsAreEqual) }
        .map { (period, decision) -> decision.withValidity(period) }
}

internal fun Database.Read.getPaidPlacements2(
    from: LocalDate,
    children: List<PersonData.WithDateOfBirth>
): List<Pair<PersonData.WithDateOfBirth, List<Pair<DateRange, PlacementWithServiceNeed>>>> {
    return children.map { child ->
        val placements = getActivePaidPlacements(child.id, from)
        if (placements.isEmpty()) return@map child to listOf<Pair<DateRange, PlacementWithServiceNeed>>()

        val serviceNeeds = createQuery(
            // language=sql
            """
SELECT daterange(sn.start_date, sn.end_date, '[]') AS range, sno.id, sno.fee_coefficient, sno.voucher_value_coefficient, sno.fee_description_fi, sno.fee_description_sv, sno.voucher_value_description_fi, sno.voucher_value_description_sv
FROM new_service_need sn
JOIN service_need_option sno ON sn.option_id = sno.id
WHERE sn.placement_id = ANY(:placementIds)
"""
        )
            .bind("placementIds", placements.map { it.id }.toTypedArray())
            .map { row ->
                row.mapColumn<DateRange>("range") to row.getRow(ServiceNeedValue::class.java)
            }
            .toList()

        val defaultServiceNeeds = createQuery(
            // language=sql
            """
SELECT valid_placement_type, id, fee_coefficient, voucher_value_coefficient, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv
FROM service_need_option WHERE default_option
"""
        )
            .map { row ->
                row.getColumn("valid_placement_type", fi.espoo.evaka.placement.PlacementType::class.java) to row.getRow(
                    ServiceNeedValue::class.java
                )
            }
            .toMap()

        val placementsWithServiceNeeds = run {
            placements.flatMap { placement ->
                val placementPeriod = DateRange(placement.startDate, placement.endDate)
                asDistinctPeriods(serviceNeeds.map { it.first }, placementPeriod)
                    .map { period ->
                        val serviceNeed = serviceNeeds.find { it.first.contains(period) }
                        period to PlacementWithServiceNeed(
                            unitId = placement.unitId,
                            type = placement.type,
                            serviceNeed = serviceNeed?.second ?: defaultServiceNeeds[placement.type]
                                ?: error("No default service need found for type ${placement.type}")
                        )
                    }
                    .let { mergePeriods(it) }
            }
        }

        child to placementsWithServiceNeeds
    }
}

private val excludedPlacementTypes = arrayOf(
    fi.espoo.evaka.placement.PlacementType.CLUB,
    fi.espoo.evaka.placement.PlacementType.TEMPORARY_DAYCARE,
    fi.espoo.evaka.placement.PlacementType.TEMPORARY_DAYCARE_PART_DAY
)
/**
 * Leaves out club and temporary placements since they shouldn't have an effect on fee or value decisions
 */
private fun Database.Read.getActivePaidPlacements(childId: UUID, from: LocalDate): List<Placement> {
    // language=sql
    val sql =
        "SELECT * FROM placement WHERE child_id = :childId AND end_date >= :from AND NOT type = ANY(:excludedTypes::placement_type[])"

    return createQuery(sql)
        .bind("childId", childId)
        .bind("from", from)
        .bind("excludedTypes", excludedPlacementTypes)
        .mapTo<Placement>()
        .toList()
}
