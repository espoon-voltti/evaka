// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service.generator

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.invoicing.controller.getFeeThresholds
import fi.espoo.evaka.invoicing.data.deleteFeeDecisions
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.getFeeAlterationsFrom
import fi.espoo.evaka.invoicing.data.getIncomesFrom
import fi.espoo.evaka.invoicing.data.insertFeeDecisions
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionDifference
import fi.espoo.evaka.invoicing.domain.FeeDecisionPlacement
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.calculateBaseFee
import fi.espoo.evaka.invoicing.domain.calculateFeeBeforeFeeAlterations
import fi.espoo.evaka.invoicing.domain.getECHAIncrease
import fi.espoo.evaka.invoicing.domain.toFeeAlterationsWithEffects
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.pis.determineHeadOfFamily
import fi.espoo.evaka.serviceneed.ServiceNeedOptionFee
import fi.espoo.evaka.serviceneed.getServiceNeedOptionFees
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.*

fun generateAndInsertFeeDecisionsV2(
    tx: Database.Transaction,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    financeMinDate: LocalDate,
    headOfFamilyId: PersonId,
    retroactiveOverride: LocalDate? = null // allows extending beyond normal min date
) {
    val existingDecisions =
        tx.findFeeDecisionsForHeadOfFamily(headOfFamilyId = headOfFamilyId, lockForUpdate = true)

    val activeDecisions =
        existingDecisions.filter { FeeDecisionStatus.effective.contains(it.status) }
    val existingDrafts = existingDecisions.filter { it.status == FeeDecisionStatus.DRAFT }
    val ignoredDrafts = existingDecisions.filter { it.status == FeeDecisionStatus.IGNORED }

    val newDrafts =
        generateFeeDecisionsDrafts(
            tx = tx,
            jsonMapper = jsonMapper,
            incomeTypesProvider = incomeTypesProvider,
            targetAdultId = headOfFamilyId,
            activeDecisions = activeDecisions,
            existingDrafts = existingDrafts,
            ignoredDrafts = ignoredDrafts,
            minDate =
                if (retroactiveOverride != null) minOf(retroactiveOverride, financeMinDate)
                else financeMinDate
        )

    tx.deleteFeeDecisions(existingDrafts.map { it.id })
    tx.insertFeeDecisions(newDrafts)
}

fun generateFeeDecisionsDrafts(
    tx: Database.Read,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    targetAdultId: PersonId,
    activeDecisions: List<FeeDecision>,
    existingDrafts: List<FeeDecision>,
    ignoredDrafts: List<FeeDecision>,
    minDate: LocalDate
): List<FeeDecision> {
    val feeBases =
        getFeeBases(
            tx = tx,
            jsonMapper = jsonMapper,
            incomeTypesProvider = incomeTypesProvider,
            targetAdultId = targetAdultId,
            activeDecisions = activeDecisions,
            minDate = minDate
        )

    val newDrafts = feeBases.map { it.toFeeDecision() }

    return filterAndMergeDrafts(
            newDrafts = newDrafts,
            activeDecisions = activeDecisions,
            ignoredDrafts = ignoredDrafts,
            minDate = minDate
        )
        .map { it.withCreatedDateFromExisting(existingDrafts) }
        .map {
            it.copy(
                difference =
                    it.getDifferencesToPrevious(
                        newDrafts = newDrafts,
                        existingActiveDecisions = activeDecisions,
                        getDifferences = FeeDecisionDifference::getDifference
                    )
            )
        }
}

private fun getFeeBases(
    tx: Database.Read,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    targetAdultId: PersonId,
    activeDecisions: List<FeeDecision>,
    minDate: LocalDate
): List<FeeBasis> {
    val familyRelations =
        getFamilyRelations(tx, targetAdultId).filter { it.range.overlaps(DateRange(minDate, null)) }
    val allPartnerIds = familyRelations.mapNotNull { it.partner }.toSet()
    val allChildIds = familyRelations.flatMap { it.children.map { child -> child.id } }.toSet()
    val allPersonIds = listOf(targetAdultId) + allPartnerIds + allChildIds

    val incomesByPerson =
        tx.getIncomesFrom(jsonMapper, incomeTypesProvider, allPersonIds, minDate)
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
    val feeAlterationsByChild =
        tx.getFeeAlterationsFrom(personIds = allChildIds.toList(), from = minDate)
            .groupBy(
                keySelector = { it.personId },
                valueTransform = {
                    FeeAlterationRange(
                        range = DateRange(it.validFrom, it.validTo),
                        feeAlteration = it
                    )
                }
            )
    val allFeeThresholds =
        tx.getFeeThresholds().map { FeeThresholdsRange(it.thresholds.validDuring, it.thresholds) }
    val allServiceNeedOptionFees =
        tx.getServiceNeedOptionFees().map { ServiceNeedOptionFeeRange(it.validity, it) }

    val datesOfChange =
        getDatesOfChange(
            *familyRelations.toTypedArray(),
            *incomesByPerson.flatMap { it.value }.toTypedArray(),
            *placementDetailsByChild.flatMap { it.value }.toTypedArray(),
            *feeAlterationsByChild.flatMap { it.value }.toTypedArray(),
            *allFeeThresholds.toTypedArray(),
            *allServiceNeedOptionFees.toTypedArray()
        ) + activeDecisions.flatMap { listOfNotNull(it.validFrom, it.validTo?.plusDays(1)) }

    return buildDateRanges(datesOfChange).mapNotNull { range ->
        if (!range.overlaps(DateRange(minDate, null))) return@mapNotNull null

        val family = familyRelations.find { it.range.contains(range) }
        val partnerCount = if (family?.partner != null) 1 else 0
        val familySize = 1 + partnerCount + (family?.children?.size ?: 0)

        val feeThresholds =
            allFeeThresholds.find { it.range.contains(range) }?.thresholds
                ?: error(
                    "Missing price or multiple prices for period ${range.start} - ${range.end}, cannot generate fee decision"
                )

        val headOfFamilyIncome =
            incomesByPerson[targetAdultId]?.find { it.range.contains(range) }?.income
        val partnerIncome =
            family?.partner?.let { partner ->
                incomesByPerson[partner]?.find { it.range.contains(range) }?.income
            }
        val parentIncomes =
            if (family?.partner != null) listOf(headOfFamilyIncome, partnerIncome)
            else listOf(headOfFamilyIncome)

        FeeBasis(
            range = range,
            headOfFamilyId = targetAdultId,
            headOfFamilyIncome = headOfFamilyIncome,
            partnerId = family?.partner,
            partnerIncome = partnerIncome,
            children =
                (family?.children ?: emptyList())
                    .sortedWith(
                        compareByDescending<Child> { it.dateOfBirth }
                            .thenByDescending { it.ssn }
                            .thenBy { it.id }
                    )
                    .mapNotNull { child ->
                        placementDetailsByChild[child.id]
                            ?.find { it.range.contains(range) }
                            ?.let { placement -> child to placement }
                    }
                    .mapIndexed childMapping@{ siblingIndex, (child, placement) ->
                        val serviceNeedOptionFee =
                            allServiceNeedOptionFees
                                .find {
                                    it.serviceNeedOptionFee.serviceNeedOptionId ==
                                        placement.serviceNeedOption.id && it.range.contains(range)
                                }
                                ?.serviceNeedOptionFee

                        val income =
                            incomesByPerson[child.id]
                                ?.find {
                                    it.range.contains(range) &&
                                        it.income.effect == IncomeEffect.INCOME
                                }
                                ?.income

                        val feeAlterations =
                            feeAlterationsByChild[child.id]
                                ?.filter { it.range.contains(range) }
                                ?.map { it.feeAlteration }
                                ?: emptyList()

                        val echaAlteration =
                            getECHAIncrease(child.id, range).takeIf {
                                parentIncomes.any { it?.worksAtECHA == true }
                            }

                        ChildFeeBasis(
                            child = child,
                            siblingIndex = siblingIndex,
                            placement = placement,
                            serviceNeedOptionFee = serviceNeedOptionFee,
                            income = income,
                            feeAlterations = feeAlterations + listOfNotNull(echaAlteration)
                        )
                    },
            familySize = familySize,
            feeThresholds = feeThresholds
        )
    }
}

data class FeeBasis(
    override val range: DateRange,
    val headOfFamilyId: PersonId,
    val headOfFamilyIncome: DecisionIncome?,
    val partnerId: PersonId?,
    val partnerIncome: DecisionIncome?,
    val children: List<ChildFeeBasis>,
    val familySize: Int,
    val feeThresholds: FeeThresholds
) : WithRange {
    fun toFeeDecision(): FeeDecision {
        return FeeDecision(
            id = FeeDecisionId(UUID.randomUUID()),
            validDuring = range,
            status = FeeDecisionStatus.DRAFT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = headOfFamilyId,
            partnerId = partnerId,
            headOfFamilyIncome = headOfFamilyIncome,
            partnerIncome = partnerIncome,
            familySize = familySize,
            feeThresholds = feeThresholds.getFeeDecisionThresholds(familySize),
            children =
                children.mapNotNull { childFeeBasis ->
                    childFeeBasis.toFeeDecisionChild(
                        feeThresholds = feeThresholds,
                        familySize = familySize,
                        parentIncomes =
                            if (partnerId != null) listOf(headOfFamilyIncome, partnerIncome)
                            else listOf(headOfFamilyIncome)
                    )
                },
            difference = emptySet()
        )
    }
}

data class ChildFeeBasis(
    val child: Child,
    val siblingIndex: Int,
    val placement: PlacementDetails,
    val serviceNeedOptionFee: ServiceNeedOptionFee?,
    val income: DecisionIncome?,
    val feeAlterations: List<FeeAlteration>
) {
    fun toFeeDecisionChild(
        feeThresholds: FeeThresholds,
        familySize: Int,
        parentIncomes: List<DecisionIncome?>
    ): FeeDecisionChild? {
        if (placement.financeDecisionType != FinanceDecisionType.FEE_DECISION) {
            return null
        }

        val siblingDiscount =
            serviceNeedOptionFee?.siblingDiscount(siblingOrdinal = siblingIndex + 1)
                ?: feeThresholds.siblingDiscount(siblingOrdinal = siblingIndex + 1)

        val baseFee =
            serviceNeedOptionFee?.baseFee
                ?: calculateBaseFee(
                    feeThresholds,
                    familySize,
                    parentIncomes + listOfNotNull(income)
                )

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

        return FeeDecisionChild(
            ChildWithDateOfBirth(child.id, child.dateOfBirth),
            FeeDecisionPlacement(placement.unitId, placement.placementType),
            FeeDecisionServiceNeed(
                optionId = placement.serviceNeedOption.id,
                feeCoefficient = placement.serviceNeedOption.feeCoefficient,
                contractDaysPerMonth = placement.serviceNeedOption.contractDaysPerMonth,
                descriptionFi = placement.serviceNeedOption.feeDescriptionFi,
                descriptionSv = placement.serviceNeedOption.feeDescriptionSv,
                missing = !placement.hasServiceNeed
            ),
            baseFee,
            siblingDiscount.percent,
            feeBeforeAlterations,
            feeAlterationsWithEffects,
            finalFee,
            income
        )
    }
}

private data class FamilyRelations(
    override val finiteRange: FiniteDateRange,
    val partner: PersonId?,
    val children: List<Child>
) : WithFiniteRange

private fun getFamilyRelations(tx: Database.Read, targetAdultId: PersonId): List<FamilyRelations> {
    val partnerRelations = tx.getPartnerRelations(targetAdultId)
    val adultIds = (partnerRelations.map { it.partnerId } + targetAdultId).toSet()
    val childRelationsByParent = tx.getChildRelations(adultIds)

    val ranges =
        buildFiniteDateRanges(
            *partnerRelations.toTypedArray(),
            *childRelationsByParent.flatMap { it.value }.toTypedArray()
        )

    return ranges.map { range ->
        val partner = partnerRelations.firstOrNull { it.range.contains(range) }?.partnerId
        val ownChildren =
            childRelationsByParent
                .filter { it.key == targetAdultId }
                .flatMap { it.value }
                .filter { it.range.contains(range) }
                .map { it.child }
        val partnersChildren =
            childRelationsByParent
                .takeIf { partner != null }
                ?.filter { it.key == partner }
                ?.flatMap { it.value }
                ?.filter { it.range.contains(range) }
                ?.map { it.child }
                ?: emptyList()
        val isHeadOfFamily =
            determineHeadOfFamily(targetAdultId to ownChildren, partner to partnersChildren)
                .first == targetAdultId

        val children =
            if (mergeFamilies) {
                if (isHeadOfFamily) ownChildren + partnersChildren else emptyList()
            } else {
                ownChildren
            }
        FamilyRelations(finiteRange = range, partner = partner, children = children)
    }
}
