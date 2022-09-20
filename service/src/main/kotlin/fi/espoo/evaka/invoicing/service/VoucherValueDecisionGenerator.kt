// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.assistanceneed.getCapacityFactorsByChild
import fi.espoo.evaka.assistanceneed.vouchercoefficient.getAssistanceNeedVoucherCoefficientsForChild
import fi.espoo.evaka.invoicing.data.deleteValueDecisions
import fi.espoo.evaka.invoicing.data.findValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.getFeeAlterationsFrom
import fi.espoo.evaka.invoicing.data.getFeeThresholds
import fi.espoo.evaka.invoicing.data.getIncomesFrom
import fi.espoo.evaka.invoicing.data.lockValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionEndDates
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.FridgeFamily
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.PlacementWithServiceNeed
import fi.espoo.evaka.invoicing.domain.ServiceNeedOptionVoucherValue
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.invoicing.domain.calculateBaseFee
import fi.espoo.evaka.invoicing.domain.calculateFeeBeforeFeeAlterations
import fi.espoo.evaka.invoicing.domain.decisionContentsAreEqual
import fi.espoo.evaka.invoicing.domain.firstOfMonthAfterThirdBirthday
import fi.espoo.evaka.invoicing.domain.getVoucherValues
import fi.espoo.evaka.invoicing.domain.toFeeAlterationsWithEffects
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class AssistanceNeedCoefficient(val validityPeriod: DateRange, val coefficient: BigDecimal)

internal fun Database.Transaction.handleValueDecisionChanges(
    featureConfig: FeatureConfig,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    clock: EvakaClock,
    from: LocalDate,
    child: ChildWithDateOfBirth,
    families: List<FridgeFamily>
) {
    val children = families.flatMap { it.children }.toSet()
    val prices = getFeeThresholds(from)
    val voucherValues = getVoucherValues(from).groupBy { it.serviceNeedOptionId }
    val adults = families.flatMap { listOfNotNull(it.headOfFamily, it.partner) }
    val incomes = getIncomesFrom(jsonMapper, incomeTypesProvider, adults + child.id, from)
    val assistanceNeedCoefficients =
        if (featureConfig.valueDecisionCapacityFactorEnabled)
            getCapacityFactorsByChild(child.id).map {
                AssistanceNeedCoefficient(it.dateRange, it.capacityFactor)
            }
        else
            getAssistanceNeedVoucherCoefficientsForChild(child.id).map {
                AssistanceNeedCoefficient(it.validityPeriod.asDateRange(), it.coefficient)
            }
    val feeAlterations =
        getFeeAlterationsFrom(listOf(child.id), from) + addECHAFeeAlterations(setOf(child), incomes)

    val placements = getPaidPlacements(from, children).toMap()
    val serviceVoucherUnits = getServiceVoucherUnits()

    val newDrafts =
        generateNewValueDecisions(
            from,
            child,
            families,
            placements,
            prices,
            voucherValues,
            incomes,
            assistanceNeedCoefficients,
            feeAlterations,
            serviceVoucherUnits
        )

    lockValueDecisionsForChild(child.id)

    val existingDrafts =
        findValueDecisionsForChild(child.id, null, listOf(VoucherValueDecisionStatus.DRAFT))
    val activeDecisions =
        findValueDecisionsForChild(
            child.id,
            null,
            listOf(
                VoucherValueDecisionStatus.WAITING_FOR_SENDING,
                VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                VoucherValueDecisionStatus.SENT
            )
        )

    val updatedDecisions = updateExistingDecisions(from, newDrafts, existingDrafts, activeDecisions)
    deleteValueDecisions(existingDrafts.map { it.id })
    upsertValueDecisions(updatedDecisions.updatedDrafts)
    updateVoucherValueDecisionEndDates(updatedDecisions.updatedActiveDecisions, clock.now())
}

private fun generateNewValueDecisions(
    from: LocalDate,
    voucherChild: ChildWithDateOfBirth,
    families: List<FridgeFamily>,
    allPlacements: Map<ChildWithDateOfBirth, List<Pair<DateRange, PlacementWithServiceNeed>>>,
    prices: List<FeeThresholds>,
    voucherValues: Map<ServiceNeedOptionId, List<ServiceNeedOptionVoucherValue>>,
    incomes: List<Income>,
    assistanceNeedCoefficients: List<AssistanceNeedCoefficient>,
    feeAlterations: List<FeeAlteration>,
    serviceVoucherUnits: List<DaycareId>
): List<VoucherValueDecision> {
    val periods =
        families.map { it.period } +
            incomes.map { DateRange(it.validFrom, it.validTo) } +
            assistanceNeedCoefficients.map { it.validityPeriod } +
            prices.map { it.validDuring } +
            voucherValues.flatMap { it.value.map { voucherValues -> voucherValues.validity } } +
            allPlacements.flatMap { (child, placements) ->
                placements.map { it.first } +
                    listOf(
                        DateRange(
                            child.dateOfBirth,
                            firstOfMonthAfterThirdBirthday(child.dateOfBirth).minusDays(1)
                        ),
                        DateRange(firstOfMonthAfterThirdBirthday(child.dateOfBirth), null)
                    )
            } +
            feeAlterations.map { DateRange(it.validFrom, it.validTo) }

    return asDistinctPeriods(periods, DateRange(from, null))
        .mapNotNull { period ->
            val family = families.find { it.period.overlaps(period) } ?: return@mapNotNull null

            val price =
                prices.find { it.validDuring.contains(period) }
                    ?: error(
                        "Missing price for period ${period.start} - ${period.end}, cannot generate voucher value decision"
                    )

            val income =
                incomes
                    .find {
                        family.headOfFamily == it.personId &&
                            DateRange(it.validFrom, it.validTo).contains(period)
                    }
                    ?.toDecisionIncome()

            val childIncome =
                incomes
                    .find {
                        voucherChild.id == it.personId &&
                            DateRange(it.validFrom, it.validTo).contains(period) &&
                            it.effect == IncomeEffect.INCOME
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

            val assistanceNeedCoefficient =
                assistanceNeedCoefficients.find { it.validityPeriod.contains(period) }?.coefficient
                    ?: BigDecimal("1.00")

            val validPlacements =
                family.children.mapNotNull { child ->
                    val childPlacements = allPlacements[child] ?: listOf()
                    val validPlacement =
                        childPlacements.find { (dateRange, _) -> dateRange.contains(period) }
                    validPlacement?.let { (_, placement) -> child to placement }
                }

            val voucherChildPlacement =
                validPlacements.find { (child, _) -> child == voucherChild }?.second

            voucherChildPlacement.let { placement ->
                val familyIncomes =
                    family.partner?.let { listOf(income, partnerIncome) } ?: listOf(income)
                val baseCoPayment =
                    calculateBaseFee(
                        price,
                        family.getSize(),
                        familyIncomes + listOfNotNull(childIncome)
                    )
                val voucherValue =
                    if (
                        placement == null ||
                            serviceVoucherUnits.none { unit -> unit == placement.unitId }
                    )
                        null
                    else {
                        val voucherValue =
                            voucherValues[placement.serviceNeed.optionId]?.find {
                                it.validity.contains(period)
                            }
                                ?: error(
                                    "Cannot generate voucher value decision: Missing voucher value for service need option ${placement.serviceNeed.optionId}, period ${period.start} - ${period.end}"
                                )
                        getVoucherValues(period, voucherChild.dateOfBirth, voucherValue)
                    }

                if (voucherValue == null || voucherValue.value == 0 || placement == null)
                    return@let period to
                        VoucherValueDecision.empty(
                            status = VoucherValueDecisionStatus.DRAFT,
                            decisionType = VoucherValueDecisionType.NORMAL,
                            headOfFamilyId = family.headOfFamily,
                            partnerId = family.partner,
                            headOfFamilyIncome = income,
                            partnerIncome = partnerIncome,
                            childIncome = childIncome,
                            familySize = family.getSize(),
                            feeThresholds = price.getFeeDecisionThresholds(family.getSize()),
                            validFrom = period.start,
                            validTo = period.end,
                            child = voucherChild,
                            baseCoPayment = baseCoPayment,
                            siblingDiscount = price.siblingDiscountPercent(1),
                            baseValue = 0,
                        )

                val siblingIndex =
                    validPlacements
                        .sortedByDescending { (child, _) -> child.dateOfBirth }
                        .indexOfFirst { (child, _) -> child == voucherChild }

                val siblingDiscountMultiplier = price.siblingDiscountMultiplier(siblingIndex + 1)
                val coPaymentBeforeAlterations =
                    calculateFeeBeforeFeeAlterations(
                        baseCoPayment,
                        placement.serviceNeed.feeCoefficient,
                        siblingDiscountMultiplier,
                        price.minFee
                    )
                val relevantFeeAlterations =
                    feeAlterations.filter { DateRange(it.validFrom, it.validTo).contains(period) }
                val feeAlterationsWithEffects =
                    toFeeAlterationsWithEffects(coPaymentBeforeAlterations, relevantFeeAlterations)
                val finalCoPayment =
                    coPaymentBeforeAlterations + feeAlterationsWithEffects.sumOf { it.effect }

                period to
                    VoucherValueDecision(
                        id = VoucherValueDecisionId(UUID.randomUUID()),
                        status = VoucherValueDecisionStatus.DRAFT,
                        decisionType = VoucherValueDecisionType.NORMAL,
                        headOfFamilyId = family.headOfFamily,
                        partnerId = family.partner,
                        headOfFamilyIncome = income,
                        partnerIncome = partnerIncome,
                        childIncome = childIncome,
                        familySize = family.getSize(),
                        feeThresholds = price.getFeeDecisionThresholds(family.getSize()),
                        validFrom = period.start,
                        validTo = period.end,
                        child = voucherChild,
                        placement = VoucherValueDecisionPlacement(placement.unitId, placement.type),
                        serviceNeed =
                            VoucherValueDecisionServiceNeed(
                                placement.serviceNeed.feeCoefficient,
                                voucherValue.coefficient,
                                placement.serviceNeed.feeDescriptionFi,
                                placement.serviceNeed.feeDescriptionSv,
                                placement.serviceNeed.voucherValueDescriptionFi,
                                placement.serviceNeed.voucherValueDescriptionSv
                            ),
                        baseCoPayment = baseCoPayment,
                        siblingDiscount = price.siblingDiscountPercent(siblingIndex + 1),
                        coPayment = coPaymentBeforeAlterations,
                        feeAlterations =
                            toFeeAlterationsWithEffects(
                                coPaymentBeforeAlterations,
                                relevantFeeAlterations
                            ),
                        finalCoPayment = finalCoPayment,
                        baseValue = voucherValue.baseValue,
                        assistanceNeedCoefficient = assistanceNeedCoefficient,
                        voucherValue =
                            (BigDecimal(voucherValue.value) * assistanceNeedCoefficient).toInt()
                    )
            }
        }
        .let { mergePeriods(it, ::decisionContentsAreEqual) }
        .map { (period, decision) -> decision.withValidity(period) }
}

private fun Database.Read.getServiceVoucherUnits(): List<DaycareId> {
    return createQuery(
            "SELECT id FROM daycare WHERE provider_type = 'PRIVATE_SERVICE_VOUCHER' AND NOT invoiced_by_municipality"
        )
        .mapTo<DaycareId>()
        .toList()
}

private fun Database.Read.getVoucherValues(from: LocalDate): List<ServiceNeedOptionVoucherValue> {
    return createQuery(
            """
SELECT
    id,
    service_need_option_id,
    validity,
    base_value,
    coefficient,
    value,
    base_value_under_3y,
    coefficient_under_3y,
    value_under_3y
FROM service_need_option_voucher_value
WHERE validity && daterange(:from, null, '[]')
        """.trimIndent(
            )
        )
        .bind("from", from)
        .mapTo<ServiceNeedOptionVoucherValue>()
        .toList()
}
