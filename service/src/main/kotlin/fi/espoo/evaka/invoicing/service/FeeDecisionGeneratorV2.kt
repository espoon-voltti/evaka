// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.invoicing.controller.getFeeThresholds
import fi.espoo.evaka.invoicing.data.deleteFeeDecisions
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.getFeeAlterationsFrom
import fi.espoo.evaka.invoicing.data.getIncomesFrom
import fi.espoo.evaka.invoicing.data.insertFeeDecisions
import fi.espoo.evaka.invoicing.data.lockFeeDecisionsForHeadOfFamily
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
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.calculateBaseFee
import fi.espoo.evaka.invoicing.domain.calculateFeeBeforeFeeAlterations
import fi.espoo.evaka.invoicing.domain.getECHAIncrease
import fi.espoo.evaka.invoicing.domain.toFeeAlterationsWithEffects
import fi.espoo.evaka.pis.HasDateOfBirth
import fi.espoo.evaka.pis.determineHeadOfFamily
import fi.espoo.evaka.serviceneed.ServiceNeedOptionFee
import fi.espoo.evaka.serviceneed.getServiceNeedOptionFees
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.*
import org.jdbi.v3.core.mapper.Nested

private const val mergeFamilies = true

fun generateAndInsertFeeDecisionsV2(
    tx: Database.Transaction,
    clock: EvakaClock,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    financeMinDate: LocalDate,
    headOfFamilyId: PersonId,
    retroactiveFrom: LocalDate? = null // ignores other min dates
) {
    tx.lockFeeDecisionsForHeadOfFamily(headOfFamilyId)

    val rollingMinDate = retroactiveFrom ?: clock.today().minusMonths(15)
    val hardMinDate = retroactiveFrom ?: financeMinDate

    val newDrafts =
        generateFeeDecisionsDrafts(
                tx = tx,
                jsonMapper = jsonMapper,
                incomeTypesProvider = incomeTypesProvider,
                targetAdultId = headOfFamilyId,
                minDate = maxOf(rollingMinDate, hardMinDate)
            )
            .filter { draft -> draft.validDuring.overlaps(DateRange(rollingMinDate, null)) }
            .mapNotNull { draft ->
                draft.validDuring.intersection(DateRange(hardMinDate, null))?.let {
                    draft.copy(validDuring = it)
                }
            }

    val existingDraftDecisions =
        tx.findFeeDecisionsForHeadOfFamily(
            headOfFamilyId,
            period = null,
            status = listOf(FeeDecisionStatus.DRAFT)
        )

    val ignoredDrafts =
        tx.findFeeDecisionsForHeadOfFamily(
            headOfFamilyId,
            period = null,
            status = listOf(FeeDecisionStatus.IGNORED)
        )

    tx.deleteFeeDecisions(existingDraftDecisions.map { it.id })

    // insert while preserving created dates
    newDrafts
        .filter { newDraft ->
            !ignoredDrafts.any {
                it.validDuring == newDraft.validDuring && it.contentEquals(newDraft)
            }
        }
        .map { newDraft ->
            val duplicateOldDraft =
                existingDraftDecisions.find { oldDraft ->
                    newDraft.contentEquals(oldDraft) && newDraft.validDuring == oldDraft.validDuring
                }
            if (duplicateOldDraft != null) {
                newDraft.copy(created = duplicateOldDraft.created)
            } else {
                newDraft
            }
        }
        .let { tx.insertFeeDecisions(it) }
}

fun generateFeeDecisionsDrafts(
    tx: Database.Read,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    targetAdultId: PersonId,
    minDate: LocalDate
): List<FeeDecision> {
    val existingActiveDecisions =
        tx.findFeeDecisionsForHeadOfFamily(
            targetAdultId,
            period = null,
            status =
                listOf(
                    FeeDecisionStatus.SENT,
                    FeeDecisionStatus.WAITING_FOR_SENDING,
                    FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING
                )
        )
    val feeBases =
        getFeeBases(
            tx,
            jsonMapper,
            incomeTypesProvider,
            targetAdultId,
            existingActiveDecisions,
            minDate
        )
    val newDrafts = feeBases.map { it.toFeeDecision() }

    if (newDrafts.isEmpty()) return emptyList()

    val filteredAndMergedDrafts =
        newDrafts
            .filterNot { draft ->
                existsActiveDuplicateThatWillRemainEffective(
                    draft,
                    existingActiveDecisions,
                    newDrafts
                )
            }
            .filterNot { draft ->
                // drop drafts without children unless there exists an active decision that overlaps
                draft.children.isEmpty() &&
                    existingActiveDecisions.none { it.validDuring.overlaps(draft.validDuring) }
            }
            .let(::mergeAdjacentIdenticalDrafts)
            .filterNot { draft ->
                existsActiveDuplicateThatWillRemainEffective(
                    draft,
                    existingActiveDecisions,
                    newDrafts
                )
            }

    val newDraftsWithDifferences =
        filteredAndMergedDrafts.map {
            it.copy(
                difference =
                    it.getDifferencesToPrevious(
                        newDrafts = newDrafts,
                        existingActiveDecisions = existingActiveDecisions,
                        getDifferences = FeeDecisionDifference::getDifference
                    )
            )
        }

    return newDraftsWithDifferences
}

private fun getFeeBases(
    tx: Database.Read,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    targetAdultId: PersonId,
    existingActiveDecisions: List<FeeDecision>,
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

    val placementDetailsByChild = getPlacementDetails(tx, allChildIds)

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
        ) + existingActiveDecisions.flatMap { listOfNotNull(it.validFrom, it.validTo?.plusDays(1)) }

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
                            ?.takeIf { it.affectsSiblingDiscount() }
                            ?.let { placement -> child to placement }
                    }
                    .mapIndexedNotNull childMapping@{ siblingIndex, (child, placement) ->
                        if (!placement.displayOnFeeDecision()) return@childMapping null

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
        if (!placement.displayOnFeeDecision()) return null

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

private data class PartnerRelation(val partnerId: PersonId, override val range: DateRange) :
    WithRange

private fun Database.Read.getPartnerRelations(id: PersonId): List<PartnerRelation> {
    return createQuery(
            """
            SELECT 
                fp2.person_id as partnerId,
                daterange(fp2.start_date, fp2.end_date, '[]') as range
            FROM fridge_partner fp1
            JOIN fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id AND fp1.indx <> fp2.indx
            WHERE fp1.person_id = :id AND NOT fp1.conflict AND NOT fp2.conflict
        """
        )
        .bind("id", id)
        .toList<PartnerRelation>()
}

data class Child(val id: PersonId, override val dateOfBirth: LocalDate, val ssn: String?) :
    HasDateOfBirth

private data class ChildRelation(
    val headOfChild: PersonId,
    override val finiteRange: FiniteDateRange,
    @Nested("child") val child: Child
) : WithFiniteRange

private fun Database.Read.getChildRelations(
    parentIds: Set<PersonId>
): Map<PersonId, List<ChildRelation>> {
    if (parentIds.isEmpty()) return emptyMap()

    return createQuery(
            """
            SELECT 
                fc.head_of_child, 
                daterange(fc.start_date, fc.end_date, '[]') as finite_range,
                p.id as child_id,
                p.date_of_birth as child_date_of_birth,
                p.social_security_number as child_ssn
            FROM fridge_child fc
            JOIN person p on fc.child_id = p.id
            WHERE head_of_child = ANY(:ids) AND NOT conflict
        """
        )
        .bind("ids", parentIds.toTypedArray())
        .mapTo<ChildRelation>()
        .mapNotNull {
            val under18 =
                FiniteDateRange(
                    it.child.dateOfBirth,
                    it.child.dateOfBirth.plusYears(18).minusDays(1)
                )
            it.range.intersection(under18)?.let { range -> it.copy(finiteRange = range) }
        }
        .groupBy { it.headOfChild }
}
