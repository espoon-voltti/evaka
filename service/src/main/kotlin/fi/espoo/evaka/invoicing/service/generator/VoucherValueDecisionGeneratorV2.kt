// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service.generator

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.assistanceneed.getCapacityFactorsByChild
import fi.espoo.evaka.assistanceneed.vouchercoefficient.getAssistanceNeedVoucherCoefficientsForChild
import fi.espoo.evaka.invoicing.controller.getFeeThresholds
import fi.espoo.evaka.invoicing.data.deleteValueDecisions
import fi.espoo.evaka.invoicing.data.findValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.getFeeAlterationsFrom
import fi.espoo.evaka.invoicing.data.getIncomesFrom
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.invoicing.domain.VoucherValue
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDifference
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.invoicing.domain.calculateBaseFee
import fi.espoo.evaka.invoicing.domain.calculateFeeBeforeFeeAlterations
import fi.espoo.evaka.invoicing.domain.firstOfMonthAfterThirdBirthday
import fi.espoo.evaka.invoicing.domain.roundToEuros
import fi.espoo.evaka.invoicing.domain.toFeeAlterationsWithEffects
import fi.espoo.evaka.invoicing.service.AssistanceNeedCoefficient
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.pis.determineHeadOfFamily
import fi.espoo.evaka.serviceneed.getServiceNeedOptionFees
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

fun generateAndInsertVoucherValueDecisionsV2(
    tx: Database.Transaction,
    clock: EvakaClock,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    financeMinDate: LocalDate,
    valueDecisionCapacityFactorEnabled: Boolean,
    childId: ChildId,
    retroactiveOverride: LocalDate? = null // allows extending beyond normal min dates
) {
    val existingDecisions = tx.findValueDecisionsForChild(childId = childId, lockForUpdate = true)

    val activeDecisions =
        existingDecisions.filter { VoucherValueDecisionStatus.effective.contains(it.status) }
    val existingDrafts = existingDecisions.filter { it.status == VoucherValueDecisionStatus.DRAFT }
    val ignoredDrafts = existingDecisions.filter { it.status == VoucherValueDecisionStatus.IGNORED }

    val newDrafts =
        generateVoucherValueDecisionsDrafts(
            tx = tx,
            jsonMapper = jsonMapper,
            incomeTypesProvider = incomeTypesProvider,
            valueDecisionCapacityFactorEnabled = valueDecisionCapacityFactorEnabled,
            targetChildId = childId,
            activeDecisions = activeDecisions,
            existingDrafts = existingDrafts,
            ignoredDrafts = ignoredDrafts,
            softMinDate = getSoftMinDate(clock, retroactiveOverride),
            hardMinDate = getHardMinDate(financeMinDate, retroactiveOverride)
        )

    tx.deleteValueDecisions(existingDrafts.map { it.id })
    tx.upsertValueDecisions(newDrafts)
}

fun generateVoucherValueDecisionsDrafts(
    tx: Database.Read,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    valueDecisionCapacityFactorEnabled: Boolean,
    targetChildId: ChildId,
    activeDecisions: List<VoucherValueDecision>,
    existingDrafts: List<VoucherValueDecision>,
    ignoredDrafts: List<VoucherValueDecision>,
    softMinDate: LocalDate,
    hardMinDate: LocalDate
): List<VoucherValueDecision> {
    val voucherBases =
        getVoucherBases(
            tx = tx,
            jsonMapper = jsonMapper,
            incomeTypesProvider = incomeTypesProvider,
            valueDecisionCapacityFactorEnabled = valueDecisionCapacityFactorEnabled,
            targetChildId = targetChildId,
            activeDecisions = activeDecisions,
            minDate = minOf(softMinDate, hardMinDate)
        )

    val newDrafts = voucherBases.map { it.toVoucherValueDecision() }

    return filterAndMergeDrafts(
            newDrafts = newDrafts,
            activeDecisions = activeDecisions,
            ignoredDrafts = ignoredDrafts,
            softMinDate = softMinDate,
            hardMinDate = hardMinDate
        )
        .map { it.withCreatedDateFromExisting(existingDrafts) }
        .map {
            it.copy(
                difference =
                    it.getDifferencesToPrevious(
                        newDrafts = newDrafts,
                        existingActiveDecisions = activeDecisions,
                        getDifferences = VoucherValueDecisionDifference::getDifference
                    )
            )
        }
}

private fun getVoucherBases(
    tx: Database.Read,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    valueDecisionCapacityFactorEnabled: Boolean,
    targetChildId: ChildId,
    activeDecisions: List<VoucherValueDecision>,
    minDate: LocalDate
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
                mapper = jsonMapper,
                incomeTypesProvider = incomeTypesProvider,
                personIds = (allHeadOfChildIds + allPartnerIds + targetChildId).toList(),
                from = minDate
            )
            .groupBy(
                keySelector = { it.personId },
                valueTransform = {
                    IncomeRange(
                        range = DateRange(it.validFrom, it.validTo),
                        income = it.toDecisionIncome()
                    )
                }
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
        (if (valueDecisionCapacityFactorEnabled) {
                tx.getCapacityFactorsByChild(child.id).map {
                    AssistanceNeedCoefficient(it.dateRange, it.capacityFactor)
                }
            } else {
                tx.getAssistanceNeedVoucherCoefficientsForChild(child.id).map {
                    AssistanceNeedCoefficient(it.validityPeriod.asDateRange(), it.coefficient)
                }
            })
            .map { AssistanceNeedRange(it.validityPeriod, it.coefficient) }

    val datesOfChange =
        getDatesOfChange(
            *familyRelations.toTypedArray(),
            *incomesByPerson.flatMap { it.value }.toTypedArray(),
            *placementDetailsByChild.flatMap { it.value }.toTypedArray(),
            *feeAlterationRanges.toTypedArray(),
            *allFeeThresholds.toTypedArray(),
            *allServiceNeedOptionFees.toTypedArray(),
            *assistanceNeedCoefficients.toTypedArray()
        ) +
            startOf3YoCoefficient +
            activeDecisions.flatMap { listOfNotNull(it.validFrom, it.validTo?.plusDays(1)) }

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
            feeThresholds = feeThresholds
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
    val feeThresholds: FeeThresholds
) : WithRange {
    fun toVoucherValueDecision(): VoucherValueDecision {
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
                child = child
            )
        }

        val voucherValues =
            placement.serviceNeedVoucherValues?.let {
                if (useUnder3YoCoefficient) {
                    VoucherValue(
                        baseValue = it.baseValueUnder3y,
                        coefficient = it.coefficientUnder3y,
                        value = it.valueUnder3y
                    )
                } else {
                    VoucherValue(
                        baseValue = it.baseValue,
                        coefficient = it.coefficient,
                        value = it.value
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
                feeThresholds.minFee
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
                    type = placement.placementType
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
                    missing = !placement.hasServiceNeed
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
            difference = emptySet()
        )
    }
}

private data class ChildFamilyRelations(
    override val finiteRange: FiniteDateRange,
    val headOfChild: PersonId?,
    val partner: PersonId?,
    val childrenInFamily: List<Child>
) : WithFiniteRange

private fun getChildFamilyRelations(
    tx: Database.Read,
    targetChildId: ChildId
): List<ChildFamilyRelations> {
    val headOfChildRelations = tx.getHeadOfChildRelations(targetChildId)
    val headOfChildIds = headOfChildRelations.map { it.headOfChild }.toSet()
    val partnerRelations =
        headOfChildIds.associateWith { headOfChildId -> tx.getPartnerRelations(headOfChildId) }
    val partnerIds = partnerRelations.values.flatMap { it.map { p -> p.partnerId } }.toSet()
    val adultIds = headOfChildIds + partnerIds
    val childRelationsByParent = tx.getChildRelations(adultIds)

    val ranges =
        buildFiniteDateRanges(
            *headOfChildRelations.toTypedArray(),
            *partnerRelations.flatMap { it.value }.toTypedArray(),
            *childRelationsByParent.flatMap { it.value }.toTypedArray()
        )

    return ranges.map { range ->
        val headOfChild =
            headOfChildRelations.firstOrNull { it.range.contains(range) }?.headOfChild
                ?: return@map ChildFamilyRelations(
                    finiteRange = range,
                    headOfChild = null,
                    partner = null,
                    childrenInFamily = emptyList()
                )

        val partner =
            partnerRelations[headOfChild]?.firstOrNull { it.range.contains(range) }?.partnerId

        val headOfChildChildren =
            childRelationsByParent[headOfChild]
                ?.filter { it.range.contains(range) }
                ?.map { it.child }
                ?: emptyList()

        val partnersChildren =
            childRelationsByParent
                .takeIf { partner != null }
                ?.get(partner)
                ?.filter { it.range.contains(range) }
                ?.map { it.child }
                ?: emptyList()

        val isHeadOfFamily =
            determineHeadOfFamily(headOfChild to headOfChildChildren, partner to partnersChildren)
                .first == headOfChild

        val children =
            if (mergeFamilies) {
                if (isHeadOfFamily) headOfChildChildren + partnersChildren else emptyList()
            } else {
                headOfChildChildren
            }

        ChildFamilyRelations(
            finiteRange = range,
            headOfChild = headOfChild,
            partner = partner,
            childrenInFamily = children
        )
    }
}

private data class HeadOfChildRelation(
    override val finiteRange: FiniteDateRange,
    val headOfChild: PersonId
) : WithFiniteRange

private fun Database.Read.getHeadOfChildRelations(childId: ChildId): List<HeadOfChildRelation> {
    return createQuery<Any> {
            sql(
                """
            SELECT head_of_child, daterange(start_date, end_date, '[]') as finite_range
            FROM fridge_child
            WHERE child_id = ${bind(childId)} AND NOT conflict
        """
            )
        }
        .mapTo<HeadOfChildRelation>()
        .toList()
}

private fun Database.Read.getChild(childId: ChildId): ChildWithDateOfBirth {
    return createQuery<Any> {
            sql(
                """
            SELECT id, date_of_birth
            FROM person
            WHERE id = ${bind(childId)}
        """
            )
        }
        .mapTo<ChildWithDateOfBirth>()
        .exactlyOne()
}

private data class AssistanceNeedRange(override val range: DateRange, val coefficient: BigDecimal) :
    WithRange
