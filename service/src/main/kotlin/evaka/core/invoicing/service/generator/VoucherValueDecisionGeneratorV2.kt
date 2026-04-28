// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service.generator

import evaka.core.assistanceneed.getCapacityFactorsByChild
import evaka.core.assistanceneed.vouchercoefficient.getAssistanceNeedVoucherCoefficientsForChild
import evaka.core.invoicing.controller.getFeeThresholds
import evaka.core.invoicing.data.deleteValueDecisions
import evaka.core.invoicing.data.findValueDecisionsForChild
import evaka.core.invoicing.data.getFeeAlterationsFrom
import evaka.core.invoicing.data.getIncomesFrom
import evaka.core.invoicing.data.upsertValueDecisions
import evaka.core.invoicing.domain.ChildWithDateOfBirth
import evaka.core.invoicing.domain.DecisionIncome
import evaka.core.invoicing.domain.FeeAlteration
import evaka.core.invoicing.domain.FeeThresholds
import evaka.core.invoicing.domain.FinanceDecisionType
import evaka.core.invoicing.domain.VoucherValue
import evaka.core.invoicing.domain.VoucherValueDecision
import evaka.core.invoicing.domain.VoucherValueDecisionDifference
import evaka.core.invoicing.domain.VoucherValueDecisionPlacement
import evaka.core.invoicing.domain.VoucherValueDecisionServiceNeed
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.invoicing.domain.calculateBaseFee
import evaka.core.invoicing.domain.calculateFeeBeforeFeeAlterations
import evaka.core.invoicing.domain.firstOfMonthAfterThirdBirthday
import evaka.core.invoicing.domain.roundToEuros
import evaka.core.invoicing.domain.toFeeAlterationsWithEffects
import evaka.core.invoicing.mapIncomeToDecisionIncome
import evaka.core.invoicing.service.IncomeCoefficientMultiplierProvider
import evaka.core.invoicing.service.IncomeTypesProvider
import evaka.core.pis.determineHeadOfFamily
import evaka.core.serviceneed.getServiceNeedOptionFees
import evaka.core.shared.ChildId
import evaka.core.shared.PersonId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

fun generateAndInsertVoucherValueDecisionsV2(
    tx: Database.Transaction,
    incomeTypesProvider: IncomeTypesProvider,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    financeMinDate: LocalDate,
    valueDecisionCapacityFactorEnabled: Boolean,
    childId: ChildId,
    retroactiveOverride: LocalDate? = null, // allows extending beyond normal min date
) {
    val existingDecisions = tx.findValueDecisionsForChild(childId = childId, lockForUpdate = true)

    val activeDecisions = existingDecisions.filter {
        VoucherValueDecisionStatus.effective.contains(it.status)
    }
    val existingDrafts = existingDecisions.filter { it.status == VoucherValueDecisionStatus.DRAFT }
    val ignoredDrafts = existingDecisions.filter { it.status == VoucherValueDecisionStatus.IGNORED }

    val newDrafts =
        generateVoucherValueDecisionsDrafts(
            tx = tx,
            incomeTypesProvider = incomeTypesProvider,
            coefficientMultiplierProvider = coefficientMultiplierProvider,
            valueDecisionCapacityFactorEnabled = valueDecisionCapacityFactorEnabled,
            targetChildId = childId,
            activeDecisions = activeDecisions,
            existingDrafts = existingDrafts,
            ignoredDrafts = ignoredDrafts,
            minDate =
                if (retroactiveOverride != null) minOf(retroactiveOverride, financeMinDate)
                else financeMinDate,
        )

    tx.deleteValueDecisions(existingDrafts.map { it.id })
    tx.upsertValueDecisions(newDrafts)
}

fun generateVoucherValueDecisionsDrafts(
    tx: Database.Read,
    incomeTypesProvider: IncomeTypesProvider,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    valueDecisionCapacityFactorEnabled: Boolean,
    targetChildId: ChildId,
    activeDecisions: List<VoucherValueDecision>,
    existingDrafts: List<VoucherValueDecision>,
    ignoredDrafts: List<VoucherValueDecision>,
    minDate: LocalDate,
): List<VoucherValueDecision> {
    val voucherBases =
        getVoucherBases(
            tx = tx,
            incomeTypesProvider = incomeTypesProvider,
            coefficientMultiplierProvider = coefficientMultiplierProvider,
            valueDecisionCapacityFactorEnabled = valueDecisionCapacityFactorEnabled,
            targetChildId = targetChildId,
            activeDecisions = activeDecisions,
            minDate = minDate,
        )

    val newDrafts = voucherBases.mapNotNull { it.toVoucherValueDecision() }

    return filterAndMergeDrafts(
            newDrafts = newDrafts,
            activeDecisions = activeDecisions,
            ignoredDrafts = ignoredDrafts,
            minDate = minDate,
        )
        .map { it.withMetadataFromExisting(existingDrafts) }
        .map {
            it.copy(
                difference =
                    it.getDifferencesToPrevious(
                        newDrafts = newDrafts,
                        existingActiveDecisions = activeDecisions,
                        getDifferences = VoucherValueDecisionDifference::getDifference,
                    )
            )
        }
}

private fun getVoucherBases(
    tx: Database.Read,
    incomeTypesProvider: IncomeTypesProvider,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    valueDecisionCapacityFactorEnabled: Boolean,
    targetChildId: ChildId,
    activeDecisions: List<VoucherValueDecision>,
    minDate: LocalDate,
): List<VoucherBasis> {
    val child = tx.getChild(targetChildId)
    val familyRelations =
        getChildFamilyRelations(tx, targetChildId).filter {
            it.range.overlaps(DateRange(minDate, null))
        }
    val allHeadOfChildIds = familyRelations.mapNotNull { it.headOfChild }.toSet()
    val allPartnerIds = familyRelations.mapNotNull { it.partner }.toSet()
    val allChildIds =
        familyRelations.flatMap { it.childrenInFamily.map { child -> child.id } }.toSet()

    val incomesByPerson =
        tx.getIncomesFrom(
                incomeTypesProvider = incomeTypesProvider,
                coefficientMultiplierProvider = coefficientMultiplierProvider,
                personIds = (allHeadOfChildIds + allPartnerIds + targetChildId).toList(),
                from = minDate,
            )
            .groupBy(
                keySelector = { it.personId },
                valueTransform = {
                    IncomeRange(
                        range = DateRange(it.validFrom, it.validTo),
                        income = mapIncomeToDecisionIncome(it, coefficientMultiplierProvider),
                    )
                },
            )

    val placementDetailsByChild = getPlacementDetailsByChild(tx, allChildIds)

    val feeAlterationRanges =
        tx.getFeeAlterationsFrom(personIds = listOf(targetChildId), from = minDate).map {
            FeeAlterationRange(range = DateRange(it.validFrom, it.validTo), feeAlteration = it)
        }

    val allFeeThresholds =
        tx.getFeeThresholds().map { FeeThresholdsRange(it.thresholds.validDuring, it.thresholds) }
    val allServiceNeedOptionFees =
        tx.getServiceNeedOptionFees().map { ServiceNeedOptionFeeRange(it.validity, it) }

    val startOf3YoCoefficient = firstOfMonthAfterThirdBirthday(child.dateOfBirth)

    val assistanceNeedCoefficients =
        if (valueDecisionCapacityFactorEnabled) {
            tx.getCapacityFactorsByChild(child.id).map {
                AssistanceNeedRange(it.dateRange, it.capacityFactor)
            }
        } else {
            tx.getAssistanceNeedVoucherCoefficientsForChild(child.id).map {
                AssistanceNeedRange(it.validityPeriod.asDateRange(), it.coefficient)
            }
        }

    val datesOfChange =
        getDatesOfChange(
            *familyRelations.toTypedArray(),
            *incomesByPerson.flatMap { it.value }.toTypedArray(),
            *placementDetailsByChild.flatMap { it.value }.toTypedArray(),
            *feeAlterationRanges.toTypedArray(),
            *allFeeThresholds.toTypedArray(),
            *allServiceNeedOptionFees.toTypedArray(),
            *assistanceNeedCoefficients.toTypedArray(),
        ) +
            startOf3YoCoefficient +
            activeDecisions.flatMap { listOfNotNull(it.validFrom, it.validTo.plusDays(1)) }

    return buildDateRanges(datesOfChange).mapNotNull { range ->
        if (!range.overlaps(DateRange(minDate, null))) return@mapNotNull null

        val family = familyRelations.find { it.range.contains(range) } ?: return@mapNotNull null
        val headOfChild = family.headOfChild ?: return@mapNotNull null
        val partnerCount = if (family.partner != null) 1 else 0
        val familySize = 1 + partnerCount + family.childrenInFamily.size

        val feeThresholds =
            allFeeThresholds.find { it.range.contains(range) }?.thresholds
                ?: error(
                    "Missing prices for period ${range.start} - ${range.end}, cannot generate fee decision"
                )

        val headOfFamilyIncome =
            incomesByPerson[headOfChild]?.find { it.range.contains(range) }?.income
        val partnerIncome =
            family.partner?.let { partner ->
                incomesByPerson[partner]?.find { it.range.contains(range) }?.income
            }
        val childIncome =
            incomesByPerson[targetChildId]?.firstOrNull { it.range.contains(range) }?.income

        val placement =
            placementDetailsByChild[targetChildId]?.firstOrNull { it.range.contains(range) }

        val feeAlterations =
            feeAlterationRanges.filter { it.range.contains(range) }.map { it.feeAlteration }

        val assistanceNeedCoefficient =
            assistanceNeedCoefficients.firstOrNull { it.range.contains(range) }?.coefficient
                ?: BigDecimal.ONE

        val siblingIndex =
            family.childrenInFamily
                .sortedWith(
                    compareByDescending<Child> { it.dateOfBirth }
                        .thenByDescending { it.ssn }
                        .thenBy { it.id }
                )
                .filter { child ->
                    placementDetailsByChild[child.id]?.any { it.range.contains(range) } ?: false
                }
                .indexOfFirst { it.id == targetChildId }
                .takeIf { it >= 0 } ?: 0

        VoucherBasis(
            range = range,
            headOfFamilyId = family.headOfChild,
            headOfFamilyIncome = headOfFamilyIncome,
            partnerId = family.partner,
            partnerIncome = partnerIncome,
            child = child,
            placement = placement,
            assistanceNeedCoefficient = assistanceNeedCoefficient,
            useUnder3YoCoefficient = range.end?.isBefore(startOf3YoCoefficient) ?: false,
            childIncome = childIncome,
            feeAlterations = feeAlterations,
            siblingIndex = siblingIndex,
            familySize = familySize,
            feeThresholds = feeThresholds,
        )
    }
}

data class VoucherBasis(
    override val range: DateRange,
    val headOfFamilyId: PersonId,
    val headOfFamilyIncome: DecisionIncome?,
    val partnerId: PersonId?,
    val partnerIncome: DecisionIncome?,
    val child: ChildWithDateOfBirth,
    val placement: PlacementDetails?,
    val assistanceNeedCoefficient: BigDecimal,
    val useUnder3YoCoefficient: Boolean,
    val childIncome: DecisionIncome?,
    val feeAlterations: List<FeeAlteration>,
    val siblingIndex: Int,
    val familySize: Int,
    val feeThresholds: FeeThresholds,
) : WithRange {
    fun toVoucherValueDecision(): VoucherValueDecision? {
        if (range.end == null) return null
        if (
            placement == null ||
                placement.financeDecisionType != FinanceDecisionType.VOUCHER_VALUE_DECISION
        ) {
            return VoucherValueDecision.empty(
                validFrom = range.start,
                validTo = range.end,
                headOfFamilyId = headOfFamilyId,
                partnerId = partnerId,
                headOfFamilyIncome = headOfFamilyIncome,
                partnerIncome = partnerIncome,
                childIncome = childIncome,
                familySize = familySize,
                feeThresholds = feeThresholds.getFeeDecisionThresholds(familySize),
                child = child,
            )
        }

        val voucherValues =
            placement.serviceNeedVoucherValues?.let {
                if (useUnder3YoCoefficient) {
                    VoucherValue(
                        baseValue = it.baseValueUnder3y,
                        coefficient = it.coefficientUnder3y,
                        value = it.valueUnder3y,
                    )
                } else {
                    VoucherValue(
                        baseValue = it.baseValue,
                        coefficient = it.coefficient,
                        value = it.value,
                    )
                }
            }
                ?: error(
                    "Voucher value coefficient missing for service need ${placement.serviceNeedOption.id} during $range"
                )

        val parentIncomes =
            if (partnerId != null) listOf(headOfFamilyIncome, partnerIncome)
            else listOf(headOfFamilyIncome)

        val baseFee =
            calculateBaseFee(feeThresholds, familySize, parentIncomes + listOfNotNull(childIncome))

        val siblingDiscount = feeThresholds.siblingDiscount(siblingOrdinal = siblingIndex + 1)

        val feeBeforeAlterations =
            calculateFeeBeforeFeeAlterations(
                baseFee,
                placement.serviceNeedOption.feeCoefficient,
                siblingDiscount,
                feeThresholds.minFee,
            )

        val feeAlterationsWithEffects =
            toFeeAlterationsWithEffects(feeBeforeAlterations, feeAlterations)

        val finalFee = feeBeforeAlterations + feeAlterationsWithEffects.sumOf { it.effect }

        return VoucherValueDecision(
            id = VoucherValueDecisionId(UUID.randomUUID()),
            validFrom = range.start,
            validTo = range.end,
            headOfFamilyId = headOfFamilyId,
            status = VoucherValueDecisionStatus.DRAFT,
            decisionType = VoucherValueDecisionType.NORMAL,
            partnerId = partnerId,
            headOfFamilyIncome = headOfFamilyIncome,
            partnerIncome = partnerIncome,
            childIncome = childIncome,
            familySize = familySize,
            feeThresholds = feeThresholds.getFeeDecisionThresholds(familySize),
            child = child,
            placement =
                VoucherValueDecisionPlacement(
                    unitId = placement.unitId,
                    type = placement.placementType,
                ),
            serviceNeed =
                VoucherValueDecisionServiceNeed(
                    feeCoefficient = placement.serviceNeedOption.feeCoefficient,
                    voucherValueCoefficient = voucherValues.coefficient,
                    feeDescriptionFi = placement.serviceNeedOption.feeDescriptionFi,
                    feeDescriptionSv = placement.serviceNeedOption.feeDescriptionSv,
                    voucherValueDescriptionFi =
                        placement.serviceNeedOption.voucherValueDescriptionFi,
                    voucherValueDescriptionSv =
                        placement.serviceNeedOption.voucherValueDescriptionSv,
                    missing = !placement.hasServiceNeed,
                ),
            baseCoPayment = baseFee,
            siblingDiscount = siblingDiscount.percent,
            coPayment = feeBeforeAlterations,
            feeAlterations = feeAlterationsWithEffects,
            finalCoPayment = finalFee,
            baseValue = voucherValues.baseValue,
            assistanceNeedCoefficient = assistanceNeedCoefficient,
            voucherValue =
                roundToEuros(BigDecimal(voucherValues.value) * assistanceNeedCoefficient).toInt(),
            difference = emptySet(),
        )
    }
}

private data class ChildFamilyRelations(
    override val finiteRange: FiniteDateRange,
    val headOfChild: PersonId?,
    val partner: PersonId?,
    val childrenInFamily: List<Child>,
) : WithFiniteRange

private fun getChildFamilyRelations(
    tx: Database.Read,
    targetChildId: ChildId,
): List<ChildFamilyRelations> {
    val headOfChildRelations = tx.getHeadOfChildRelations(targetChildId)
    val headOfChildIds = headOfChildRelations.map { it.headOfChild }.toSet()
    val partnerRelations = headOfChildIds.associateWith { headOfChildId ->
        tx.getPartnerRelations(headOfChildId)
    }
    val partnerIds = partnerRelations.values.flatMap { it.map { p -> p.partnerId } }.toSet()
    val adultIds = headOfChildIds + partnerIds
    val childRelationsByParent = tx.getChildRelations(adultIds)

    val ranges =
        buildFiniteDateRanges(
            *headOfChildRelations.toTypedArray(),
            *partnerRelations.flatMap { it.value }.toTypedArray(),
            *childRelationsByParent.flatMap { it.value }.toTypedArray(),
        )

    return ranges.map { range ->
        val headOfChild =
            headOfChildRelations.firstOrNull { it.range.contains(range) }?.headOfChild
                ?: return@map ChildFamilyRelations(
                    finiteRange = range,
                    headOfChild = null,
                    partner = null,
                    childrenInFamily = emptyList(),
                )

        val partner =
            partnerRelations[headOfChild]?.firstOrNull { it.range.contains(range) }?.partnerId

        val headOfChildChildren =
            childRelationsByParent[headOfChild]
                ?.filter { it.range.contains(range) }
                ?.map { it.child } ?: emptyList()

        val partnersChildren =
            childRelationsByParent
                .takeIf { partner != null }
                ?.get(partner)
                ?.filter { it.range.contains(range) }
                ?.map { it.child } ?: emptyList()

        val (realHeadOfFamily, realPartner) =
            determineHeadOfFamily(headOfChild to headOfChildChildren, partner to partnersChildren)

        val children = headOfChildChildren + partnersChildren

        ChildFamilyRelations(
            finiteRange = range,
            headOfChild = realHeadOfFamily,
            partner = realPartner,
            childrenInFamily = children,
        )
    }
}

private data class HeadOfChildRelation(
    override val finiteRange: FiniteDateRange,
    val headOfChild: PersonId,
) : WithFiniteRange

private fun Database.Read.getHeadOfChildRelations(childId: ChildId): List<HeadOfChildRelation> {
    return createQuery {
            sql(
                """
            SELECT head_of_child, daterange(start_date, end_date, '[]') as finite_range
            FROM fridge_child
            WHERE child_id = ${bind(childId)} AND NOT conflict
        """
            )
        }
        .toList<HeadOfChildRelation>()
}

private fun Database.Read.getChild(childId: ChildId): ChildWithDateOfBirth {
    return createQuery {
            sql(
                """
            SELECT id, date_of_birth
            FROM person
            WHERE id = ${bind(childId)}
        """
            )
        }
        .exactlyOne<ChildWithDateOfBirth>()
}

private data class AssistanceNeedRange(override val range: DateRange, val coefficient: BigDecimal) :
    WithRange
