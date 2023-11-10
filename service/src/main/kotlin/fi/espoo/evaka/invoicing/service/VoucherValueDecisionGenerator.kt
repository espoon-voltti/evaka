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
import fi.espoo.evaka.invoicing.domain.PersonBasic
import fi.espoo.evaka.invoicing.domain.PlacementWithServiceNeed
import fi.espoo.evaka.invoicing.domain.ServiceNeedOptionVoucherValue
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDifference
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.invoicing.domain.calculateBaseFee
import fi.espoo.evaka.invoicing.domain.calculateFeeBeforeFeeAlterations
import fi.espoo.evaka.invoicing.domain.decisionContentsAreEqual
import fi.espoo.evaka.invoicing.domain.firstOfMonthAfterThirdBirthday
import fi.espoo.evaka.invoicing.domain.getVoucherValues
import fi.espoo.evaka.invoicing.domain.roundToEuros
import fi.espoo.evaka.invoicing.domain.toFeeAlterationsWithEffects
import fi.espoo.evaka.invoicing.mapIncomeToDecisionIncome
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.periodsCanMerge
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class AssistanceNeedCoefficient(val validityPeriod: DateRange, val coefficient: BigDecimal)

internal fun Database.Transaction.handleValueDecisionChanges(
    featureConfig: FeatureConfig,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    clock: EvakaClock,
    from: LocalDate,
    child: PersonBasic,
    families: List<FridgeFamily>
) {
    val children = families.flatMap { it.children }.toSet()
    val prices = getFeeThresholds(from)
    val voucherValues = getVoucherValues(from).groupBy { it.serviceNeedOptionId }
    val adults = families.flatMap { listOfNotNull(it.headOfFamily, it.partner) }
    val incomes =
        getIncomesFrom(
            jsonMapper,
            incomeTypesProvider,
            coefficientMultiplierProvider,
            adults + child.id,
            from
        )
    val assistanceNeedCoefficients =
        if (featureConfig.valueDecisionCapacityFactorEnabled) {
            getCapacityFactorsByChild(child.id).map {
                AssistanceNeedCoefficient(it.dateRange, it.capacityFactor)
            }
        } else {
            getAssistanceNeedVoucherCoefficientsForChild(child.id).map {
                AssistanceNeedCoefficient(it.validityPeriod.asDateRange(), it.coefficient)
            }
        }
    val feeAlterations =
        getFeeAlterationsFrom(listOf(child.id), from) +
            addECHAFeeAlterations(setOf(child.id), incomes)

    val placements = getPaidPlacements(from, children).toMap()
    val serviceVoucherUnits = getServiceVoucherUnits()

    lockValueDecisionsForChild(child.id)

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
    val previousDrafts =
        findValueDecisionsForChild(
            child.id,
            from.minusDays(1).let { date -> DateRange(date, date) },
            listOf(VoucherValueDecisionStatus.DRAFT)
        )

    val newDraftsWithoutDifference =
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
            serviceVoucherUnits,
            coefficientMultiplierProvider
        )
    val newDrafts =
        newDraftsWithoutDifference.map { draft ->
            if (draft.isEmpty()) {
                return@map draft
            }
            val draftDifferences =
                (newDraftsWithoutDifference + previousDrafts)
                    .filter { other -> !other.isEmpty() && decisionsCanMerge(other, draft) }
                    .flatMap { other -> VoucherValueDecisionDifference.getDifference(other, draft) }
            if (draftDifferences.isNotEmpty()) {
                return@map draft.copy(difference = draftDifferences.toSet())
            }
            val activeDifferences =
                activeDecisions
                    .filter { active -> decisionsCanMerge(active, draft) }
                    .flatMap { active ->
                        VoucherValueDecisionDifference.getDifference(active, draft)
                    }
            draft.copy(difference = activeDifferences.toSet())
        }

    val existingDrafts =
        findValueDecisionsForChild(child.id, null, listOf(VoucherValueDecisionStatus.DRAFT))

    val updatedDecisions =
        updateExistingDecisions(clock.now(), from, newDrafts, existingDrafts, activeDecisions)
    val updatedDrafts = preserveCreatedDates(updatedDecisions.updatedDrafts, existingDrafts)
    deleteValueDecisions(existingDrafts.map { it.id })
    upsertValueDecisions(updatedDrafts)
    updateVoucherValueDecisionEndDates(updatedDecisions.updatedActiveDecisions, clock.now())
}

private fun preserveCreatedDates(
    updatedDrafts: List<VoucherValueDecision>,
    existingDrafts: List<VoucherValueDecision>
): List<VoucherValueDecision> {
    return updatedDrafts.map { draft ->
        val existingDraft = existingDrafts.find { decisionContentsAreEqual(it, draft) }
        if (existingDraft != null) {
            draft.copy(created = existingDraft.created)
        } else draft
    }
}

private fun generateNewValueDecisions(
    from: LocalDate,
    voucherChild: PersonBasic,
    families: List<FridgeFamily>,
    allPlacements: Map<PersonBasic, List<Pair<DateRange, PlacementWithServiceNeed>>>,
    prices: List<FeeThresholds>,
    voucherValues: Map<ServiceNeedOptionId, List<ServiceNeedOptionVoucherValue>>,
    incomes: List<Income>,
    assistanceNeedCoefficients: List<AssistanceNeedCoefficient>,
    feeAlterations: List<FeeAlteration>,
    serviceVoucherUnits: List<DaycareId>,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider
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
                prices.singleOrNull() { it.validDuring.contains(period) }
                    ?: error(
                        "Missing price or multiple prices for period ${period.start} - ${period.end}, cannot generate voucher value decision"
                    )

            val income =
                incomes
                    .find {
                        family.headOfFamily == it.personId &&
                            DateRange(it.validFrom, it.validTo).contains(period)
                    }
                    ?.let { inc -> mapIncomeToDecisionIncome(inc, coefficientMultiplierProvider) }

            val childIncome =
                incomes
                    .find {
                        voucherChild.id == it.personId &&
                            DateRange(it.validFrom, it.validTo).contains(period) &&
                            it.effect == IncomeEffect.INCOME
                    }
                    ?.let { inc -> mapIncomeToDecisionIncome(inc, coefficientMultiplierProvider) }

            val partnerIncome =
                family.partner?.let { partner ->
                    incomes
                        .find {
                            partner == it.personId &&
                                DateRange(it.validFrom, it.validTo).contains(period)
                        }
                        ?.let { inc ->
                            mapIncomeToDecisionIncome(inc, coefficientMultiplierProvider)
                        }
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
                    ) {
                        null
                    } else {
                        val voucherValue =
                            voucherValues[placement.serviceNeed.optionId]?.find {
                                it.validity.contains(period)
                            }
                                ?: error(
                                    "Cannot generate voucher value decision: Missing voucher value for service need option ${placement.serviceNeed.optionId}, period ${period.start} - ${period.end}"
                                )
                        getVoucherValues(period, voucherChild.dateOfBirth, voucherValue)
                    }

                if (voucherValue == null || voucherValue.value == 0 || placement == null) {
                    return@let period to
                        VoucherValueDecision.empty(
                            headOfFamilyId = family.headOfFamily,
                            partnerId = family.partner,
                            headOfFamilyIncome = income,
                            partnerIncome = partnerIncome,
                            childIncome = childIncome,
                            familySize = family.getSize(),
                            feeThresholds = price.getFeeDecisionThresholds(family.getSize()),
                            validFrom = period.start,
                            validTo = period.end,
                            child = ChildWithDateOfBirth(voucherChild.id, voucherChild.dateOfBirth)
                        )
                }

                val siblingIndex =
                    validPlacements
                        .sortedWith(
                            compareByDescending<Pair<PersonBasic, PlacementWithServiceNeed>> {
                                    (child, _) ->
                                    child.dateOfBirth
                                }
                                .thenByDescending { (child, _) -> child.ssn }
                                .thenBy { (child, _) -> child.id }
                        )
                        .indexOfFirst { (child, _) -> child == voucherChild }

                val siblingDiscount = price.siblingDiscount(siblingIndex + 1)

                val coPaymentBeforeAlterations =
                    calculateFeeBeforeFeeAlterations(
                        baseCoPayment,
                        placement.serviceNeed.feeCoefficient,
                        siblingDiscount,
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
                        child = ChildWithDateOfBirth(voucherChild.id, voucherChild.dateOfBirth),
                        placement = VoucherValueDecisionPlacement(placement.unitId, placement.type),
                        serviceNeed =
                            VoucherValueDecisionServiceNeed(
                                placement.serviceNeed.feeCoefficient,
                                voucherValue.coefficient,
                                placement.serviceNeed.feeDescriptionFi,
                                placement.serviceNeed.feeDescriptionSv,
                                placement.serviceNeed.voucherValueDescriptionFi,
                                placement.serviceNeed.voucherValueDescriptionSv,
                                placement.missingServiceNeed
                            ),
                        baseCoPayment = baseCoPayment,
                        siblingDiscount = siblingDiscount.percent,
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
                            roundToEuros(BigDecimal(voucherValue.value) * assistanceNeedCoefficient)
                                .toInt(),
                        difference = emptySet()
                    )
            }
        }
        .let { mergePeriods(it, ::decisionContentsAreEqual) }
        .map { (period, decision) -> decision.withValidity(period) }
}

private fun decisionsCanMerge(d1: VoucherValueDecision, d2: VoucherValueDecision): Boolean =
    periodsCanMerge(DateRange(d1.validFrom, d1.validTo), DateRange(d2.validFrom, d2.validTo))

private fun Database.Read.getServiceVoucherUnits(): List<DaycareId> {
    return createQuery(
            "SELECT id FROM daycare WHERE provider_type = 'PRIVATE_SERVICE_VOUCHER' AND NOT invoiced_by_municipality"
        )
        .toList<DaycareId>()
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
        """
                .trimIndent()
        )
        .bind("from", from)
        .toList<ServiceNeedOptionVoucherValue>()
}
