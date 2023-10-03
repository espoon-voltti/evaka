// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.daycare.domain.ProviderType
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
import fi.espoo.evaka.invoicing.domain.FinanceDecision
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.VoucherValue
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.invoicing.domain.calculateBaseFee
import fi.espoo.evaka.invoicing.domain.calculateFeeBeforeFeeAlterations
import fi.espoo.evaka.invoicing.domain.firstOfMonthAfterThirdBirthday
import fi.espoo.evaka.invoicing.domain.getECHAIncrease
import fi.espoo.evaka.invoicing.domain.roundToEuros
import fi.espoo.evaka.invoicing.domain.toFeeAlterationsWithEffects
import fi.espoo.evaka.pis.HasDateOfBirth
import fi.espoo.evaka.pis.determineHeadOfFamily
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.serviceneed.ServiceNeedOptionFee
import fi.espoo.evaka.serviceneed.getServiceNeedOptionFees
import fi.espoo.evaka.serviceneed.getServiceNeedOptions
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.periodsCanMerge
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import org.jdbi.v3.core.mapper.Nested

const val mergeFamilies = true

fun generateAndInsertFinanceDecisionsV2(
    tx: Database.Transaction,
    clock: EvakaClock,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    financeMinDate: LocalDate,
    headOfFamilyId: PersonId,
    retroactiveFrom: LocalDate? = null // ignores other min dates
) {
    tx.lockFeeDecisionsForHeadOfFamily(headOfFamilyId)
    // todo: lock vv decisions

    val rollingMinDate = retroactiveFrom ?: clock.today().minusMonths(15)
    val hardMinDate = retroactiveFrom ?: financeMinDate

    val feeBases =
        getFeeBases(
            tx = tx,
            jsonMapper = jsonMapper,
            incomeTypesProvider = incomeTypesProvider,
            targetAdultId = headOfFamilyId,
            minDate = maxOf(rollingMinDate, hardMinDate)
        )

    generateAndInsertFeeDecisions(
        tx = tx,
        rangeOverlapFilter = DateRange(rollingMinDate, null),
        hardRangeLimit = DateRange(hardMinDate, null),
        headOfFamilyId = headOfFamilyId,
        feeBases = feeBases
    )

    // todo
    /*    generateAndInsertVoucherValueDecisions(
        tx = tx,
        minDate = minDate,
        headOfFamilyId = headOfFamilyId,
        useV2Tables = useV2Tables,
        feeBases = feeBases
    )*/
}

private fun generateAndInsertFeeDecisions(
    tx: Database.Transaction,
    headOfFamilyId: PersonId,
    feeBases: List<FeeBasis>,
    rangeOverlapFilter: DateRange,
    hardRangeLimit: DateRange
) {
    val existingActiveFeeDecisions =
        tx.findFeeDecisionsForHeadOfFamily(
            headOfFamilyId,
            period = null,
            status =
                listOf(
                    FeeDecisionStatus.SENT,
                    FeeDecisionStatus.WAITING_FOR_SENDING,
                    FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING
                )
        )

    val newDrafts =
        generateDecisionDrafts(feeBases, existingActiveFeeDecisions) { feeBasis ->
                listOf(feeBasis.toFeeDecision())
            }
            .filter { draft -> draft.validDuring.overlaps(rangeOverlapFilter) }
            .mapNotNull { draft ->
                draft.validDuring.intersection(hardRangeLimit)?.let { limitedRange ->
                    draft.copy(validDuring = limitedRange)
                }
            }
            .let { newDraftsWithoutDifferences ->
                newDraftsWithoutDifferences.map { draft ->
                    draft.copy(
                        difference =
                            draft.getDifferencesWithPrevious(
                                newDrafts = newDraftsWithoutDifferences,
                                existingActiveDecisions = existingActiveFeeDecisions,
                                FeeDecisionDifference::getDifference
                            )
                    )
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

fun <T : FinanceDecision<T>> generateDecisionDrafts(
    feeBases: List<FeeBasis>,
    existingActiveDecisions: List<T>,
    mapper: (FeeBasis) -> List<T>
): List<T> {
    val datesOfChange =
        getDatesOfChange(*feeBases.toTypedArray()) +
            existingActiveDecisions.flatMap { listOfNotNull(it.validFrom, it.validTo?.plusDays(1)) }

    val newDrafts =
        buildDateRanges(datesOfChange).flatMap { range ->
            feeBases
                .firstOrNull { it.range.contains(range) }
                ?.let(mapper)
                ?.map { it.withValidity(range) }
                ?: emptyList()
        }

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
                // drop empty drafts unless there exists an active decision that overlaps
                draft.isEmpty() &&
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

    return filteredAndMergedDrafts
}

private fun getFeeBases(
    tx: Database.Read,
    jsonMapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    targetAdultId: PersonId,
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
        )

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
                            ?.takeIf { it.placementType.providesSiblingDiscount() }
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

    fun toVoucherValueDecisions(): List<VoucherValueDecision> {
        val parentIncomes =
            if (partnerId != null) listOf(headOfFamilyIncome, partnerIncome)
            else listOf(headOfFamilyIncome)

        return children.mapNotNull { child ->
            val placement = child.placement
            if (!placement.displayOnVoucherValueDecision()) return@mapNotNull null

            val voucherValue =
                placement.serviceNeedOptionVoucherValue?.let {
                    getVoucherValues(range, child.child.dateOfBirth, it)
                }
                    ?: error(
                        "Cannot generate voucher value decision: Missing voucher value for service need option ${placement.serviceNeedOption.id} during $range"
                    )

            val siblingDiscount =
                child.serviceNeedOptionFee?.siblingDiscount(siblingOrdinal = child.siblingIndex + 1)
                    ?: feeThresholds.siblingDiscount(siblingOrdinal = child.siblingIndex + 1)

            val baseFee =
                child.serviceNeedOptionFee?.baseFee
                    ?: calculateBaseFee(
                        feeThresholds,
                        familySize,
                        parentIncomes + listOfNotNull(child.income)
                    )

            val feeBeforeAlterations =
                calculateFeeBeforeFeeAlterations(
                    baseFee,
                    placement.serviceNeedOption.feeCoefficient,
                    siblingDiscount,
                    feeThresholds.minFee
                )

            val feeAlterationsWithEffects =
                toFeeAlterationsWithEffects(feeBeforeAlterations, child.feeAlterations)

            val finalFee = feeBeforeAlterations + feeAlterationsWithEffects.sumOf { it.effect }

            val assistanceNeedCoefficient = BigDecimal.ZERO // TODO

            VoucherValueDecision(
                id = VoucherValueDecisionId(UUID.randomUUID()),
                validFrom = range.start,
                validTo = range.end,
                headOfFamilyId = headOfFamilyId,
                status = VoucherValueDecisionStatus.DRAFT,
                decisionType = VoucherValueDecisionType.NORMAL,
                partnerId = partnerId,
                headOfFamilyIncome = headOfFamilyIncome,
                partnerIncome = partnerIncome,
                childIncome = child.income,
                familySize = familySize,
                feeThresholds = feeThresholds.getFeeDecisionThresholds(familySize),
                child = ChildWithDateOfBirth(child.child.id, child.child.dateOfBirth),
                placement =
                    VoucherValueDecisionPlacement(
                        unitId = placement.unitId,
                        type = placement.placementType
                    ),
                serviceNeed =
                    VoucherValueDecisionServiceNeed(
                        feeCoefficient = placement.serviceNeedOption.feeCoefficient,
                        voucherValueCoefficient = voucherValue.coefficient,
                        feeDescriptionFi = placement.serviceNeedOption.feeDescriptionFi,
                        feeDescriptionSv = placement.serviceNeedOption.feeDescriptionSv,
                        voucherValueDescriptionFi =
                            placement.serviceNeedOption.voucherValueDescriptionFi,
                        voucherValueDescriptionSv =
                            placement.serviceNeedOption.voucherValueDescriptionSv,
                        missing = !placement.hasServiceNeed
                    ),
                baseCoPayment = baseFee,
                siblingDiscount = feeThresholds.siblingDiscount(child.siblingIndex).percent,
                coPayment = feeBeforeAlterations,
                feeAlterations = feeAlterationsWithEffects,
                finalCoPayment = finalFee,
                baseValue = voucherValue.baseValue,
                assistanceNeedCoefficient = assistanceNeedCoefficient,
                voucherValue =
                    roundToEuros(BigDecimal(voucherValue.value) * assistanceNeedCoefficient)
                        .toInt(),
                difference = emptySet()
            )
        }
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
        .mapTo<PartnerRelation>()
        .list()
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

data class PlacementDetails(
    val childId: PersonId,
    override val finiteRange: FiniteDateRange,
    val placementType: PlacementType,
    val unitId: DaycareId,
    val invoicedUnit: Boolean,
    val providerType: ProviderType,
    val hasServiceNeed: Boolean,
    val serviceNeedOption: ServiceNeedOption,
    val serviceNeedOptionVoucherValue: ServiceNeedOptionVoucherValue?
) : WithFiniteRange {
    fun displayOnFeeDecision(): Boolean {
        return displayOnFinanceDecision() &&
            invoicedUnit &&
            providerType != ProviderType.PRIVATE_SERVICE_VOUCHER
    }

    fun displayOnVoucherValueDecision(): Boolean {
        return displayOnFinanceDecision() && providerType == ProviderType.PRIVATE_SERVICE_VOUCHER
    }

    private fun displayOnFinanceDecision(): Boolean {
        return placementType.isInvoiced() &&
            !PlacementType.temporary.contains(placementType) &&
            serviceNeedOption.feeCoefficient != BigDecimal.ZERO
    }
}

private data class Placement(
    val childId: PersonId,
    override val finiteRange: FiniteDateRange,
    val type: PlacementType,
    val unitId: DaycareId,
    val invoicedUnit: Boolean,
    val providerType: ProviderType
) : WithFiniteRange

private data class ServiceNeed(
    val childId: PersonId,
    override val finiteRange: FiniteDateRange,
    val optionId: ServiceNeedOptionId
) : WithFiniteRange

// TODO: duplicated from domain package, remove that after v1 generation is removed
data class ServiceNeedOptionVoucherValue(
    val serviceNeedOptionId: ServiceNeedOptionId,
    override val range: DateRange,
    val baseValue: Int,
    val coefficient: BigDecimal,
    val value: Int,
    val baseValueUnder3y: Int,
    val coefficientUnder3y: BigDecimal,
    val valueUnder3y: Int
) : WithRange

private fun getPlacementDetails(
    tx: Database.Read,
    childIds: Set<PersonId>
): Map<PersonId, List<PlacementDetails>> {
    if (childIds.isEmpty()) return emptyMap()

    val serviceNeedOptions = tx.getServiceNeedOptions()

    val serviceNeedOptionVoucherValues =
        tx.createQuery(
                """
        SELECT
            service_need_option_id, validity as range,
            base_value, coefficient, value,
            base_value_under_3y, coefficient_under_3y, value_under_3y
        FROM service_need_option_voucher_value
    """
            )
            .mapTo<ServiceNeedOptionVoucherValue>()
            .groupBy { it.serviceNeedOptionId }

    val placements =
        tx.createQuery(
                """
        SELECT 
            child_id, 
            daterange(pl.start_date, pl.end_date, '[]') as finite_range, 
            pl.type,
            pl.unit_id,
            d.invoiced_by_municipality as invoiced_unit,
            d.provider_type
        FROM placement pl
        JOIN daycare d ON pl.unit_id = d.id
        WHERE child_id = ANY(:ids)
    """
            )
            .bind("ids", childIds.toTypedArray())
            .mapTo<Placement>()
            .groupBy { it.childId }

    val serviceNeeds =
        tx.createQuery(
                """
        SELECT child_id, daterange(sn.start_date, sn.end_date, '[]') as finite_range, sn.option_id
        FROM service_need sn
        JOIN placement p ON sn.placement_id = p.id
        WHERE child_id = ANY(:ids)
    """
            )
            .bind("ids", childIds.toTypedArray())
            .mapTo<ServiceNeed>()
            .groupBy { it.childId }

    val dateRanges =
        buildFiniteDateRanges(
            *placements.flatMap { it.value }.toTypedArray(),
            *serviceNeeds.flatMap { it.value }.toTypedArray(),
            *serviceNeedOptionVoucherValues.flatMap { it.value }.toTypedArray()
        )

    return childIds.associateWith { childId ->
        dateRanges.mapNotNull { range ->
            val placement =
                placements[childId]?.firstOrNull { it.range.contains(range) }
                    ?: return@mapNotNull null
            val serviceNeed = serviceNeeds[childId]?.firstOrNull { it.range.contains(range) }
            val serviceNeedOption =
                if (serviceNeed != null) {
                    serviceNeedOptions.find { it.id == serviceNeed.optionId }
                        ?: throw Error("Missing service need option ${serviceNeed.optionId}")
                } else {
                    serviceNeedOptions.find {
                        it.defaultOption && it.validPlacementType == placement.type
                    }
                        ?: throw Error("Missing default service need option for ${placement.type}")
                }
            val serviceNeedOptionVoucherValue =
                serviceNeedOptionVoucherValues[serviceNeedOption.id]?.firstOrNull {
                    it.range.contains(range)
                }

            PlacementDetails(
                childId = childId,
                finiteRange = range,
                placementType = placement.type,
                unitId = placement.unitId,
                invoicedUnit = placement.invoicedUnit,
                providerType = placement.providerType,
                hasServiceNeed = serviceNeed != null,
                serviceNeedOption = serviceNeedOption,
                serviceNeedOptionVoucherValue = serviceNeedOptionVoucherValue
            )
        }
    }
}

private data class IncomeRange(override val range: DateRange, val income: DecisionIncome) :
    WithRange

private data class FeeThresholdsRange(
    override val range: DateRange,
    val thresholds: FeeThresholds
) : WithRange

private data class ServiceNeedOptionFeeRange(
    override val range: DateRange,
    val serviceNeedOptionFee: ServiceNeedOptionFee
) : WithRange

private data class FeeAlterationRange(
    override val range: DateRange,
    val feeAlteration: FeeAlteration
) : WithRange

fun <T : FinanceDecision<T>> mergeAdjacentIdenticalDrafts(decisions: List<T>): List<T> {
    return decisions
        .sortedBy { it.validFrom }
        .fold(emptyList()) { acc, next ->
            val prev = acc.lastOrNull()
            if (
                prev != null &&
                    periodsCanMerge(prev.validDuring, next.validDuring) &&
                    prev.contentEquals(next)
            ) {
                acc.dropLast(1) + prev.withValidity(DateRange(prev.validFrom, next.validTo))
            } else {
                acc + next
            }
        }
}

fun <T : FinanceDecision<T>> existsActiveDuplicateThatWillRemainEffective(
    draft: T,
    activeDecisions: List<T>,
    drafts: List<T>,
): Boolean {
    val activeDuplicate =
        activeDecisions.find {
            it.validDuring.contains(draft.validDuring) && it.contentEquals(draft)
        }
            ?: return false

    val nonIdenticalDraftsOverlappingActive =
        drafts.filter {
            it.validDuring.overlaps(activeDuplicate.validDuring) &&
                !it.contentEquals(activeDuplicate)
        }

    val activeValidUntil =
        nonIdenticalDraftsOverlappingActive.minOfOrNull { it.validFrom.minusDays(1) }
            ?: activeDuplicate.validTo

    if (activeValidUntil != null && activeValidUntil < activeDuplicate.validFrom) {
        return false // active decision will be annulled
    }

    val newActiveRange = DateRange(activeDuplicate.validFrom, activeValidUntil)

    return newActiveRange.contains(draft.validDuring)
}

private fun <Decision : FinanceDecision<Decision>, Diff> Decision.getDifferencesWithPrevious(
    newDrafts: List<Decision>,
    existingActiveDecisions: List<Decision>,
    getDifferences: (d1: Decision, d2: Decision) -> Set<Diff>
): Set<Diff> {
    if (this.isEmpty()) {
        return emptySet()
    }

    val draftDifferences =
        newDrafts
            .filter { other ->
                !other.isEmpty() && periodsCanMerge(other.validDuring, this.validDuring)
            }
            .flatMap { other -> getDifferences(other, this) }

    if (draftDifferences.isNotEmpty()) {
        return draftDifferences.toSet()
    }

    val activeDifferences =
        existingActiveDecisions
            .filter { active -> periodsCanMerge(active.validDuring, this.validDuring) }
            .flatMap { active -> getDifferences(active, this) }

    return activeDifferences.toSet()
}

// TODO: duplicated from domain package, remove that after v1 generation is removed
private fun getVoucherValues(
    period: DateRange,
    dateOfBirth: LocalDate,
    voucherValues: ServiceNeedOptionVoucherValue
): VoucherValue {
    val thirdBirthdayPeriodStart = firstOfMonthAfterThirdBirthday(dateOfBirth)
    val periodStartInMiddleOfTargetPeriod =
        period.includes(thirdBirthdayPeriodStart) &&
            thirdBirthdayPeriodStart != period.start &&
            thirdBirthdayPeriodStart != period.end

    check(!periodStartInMiddleOfTargetPeriod) {
        "Third birthday period start ($thirdBirthdayPeriodStart) is in the middle of the period ($period), cannot calculate an unambiguous age coefficient"
    }

    return when {
        period.start < thirdBirthdayPeriodStart ->
            VoucherValue(
                voucherValues.baseValueUnder3y,
                voucherValues.coefficientUnder3y,
                voucherValues.valueUnder3y
            )
        else ->
            VoucherValue(voucherValues.baseValue, voucherValues.coefficient, voucherValues.value)
    }
}
