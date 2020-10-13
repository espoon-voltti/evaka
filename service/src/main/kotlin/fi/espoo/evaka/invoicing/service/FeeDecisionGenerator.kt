// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.invoicing.data.deleteFeeDecisions
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.getFeeAlterationsFrom
import fi.espoo.evaka.invoicing.data.getIncomesFrom
import fi.espoo.evaka.invoicing.data.getPricing
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionInvariant
import fi.espoo.evaka.invoicing.domain.FeeDecisionPart
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.FridgeFamily
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.PermanentPlacement
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.PlacementType
import fi.espoo.evaka.invoicing.domain.Pricing
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.invoicing.domain.calculateBaseFee
import fi.espoo.evaka.invoicing.domain.calculateFeeBeforeFeeAlterations
import fi.espoo.evaka.invoicing.domain.calculateServiceNeed
import fi.espoo.evaka.invoicing.domain.getECHAIncrease
import fi.espoo.evaka.invoicing.domain.getSiblingDiscountPercent
import fi.espoo.evaka.invoicing.domain.serviceNeedIsNotInvoiced
import fi.espoo.evaka.invoicing.domain.toFeeAlterationsWithEffects
import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.ParentshipService
import fi.espoo.evaka.pis.service.PartnershipService
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.placement.Placement
import fi.espoo.evaka.serviceneed.ServiceNeed
import fi.espoo.evaka.serviceneed.getServiceNeedsByChildDuringPeriod
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.db.withSpringTx
import fi.espoo.evaka.shared.domain.Period
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.orMax
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

@Component
class FeeDecisionGenerator(
    private val personService: PersonService,
    private val parentshipService: ParentshipService,
    private val partnershipService: PartnershipService,
    private val txManager: PlatformTransactionManager,
    private val dataSource: DataSource,
    private val objectMapper: ObjectMapper,
    env: Environment
) {
    private fun <T> toJson(data: T): String = objectMapper.writeValueAsString(data)
    private val feeDecisionMinDate: LocalDate = LocalDate.parse(env.getRequiredProperty("fee_decision_min_date"))

    @Transactional
    fun createRetroactive(headOfFamily: UUID, from: LocalDate) {
        val period = Period(from, null)
        findFamiliesByHeadOfFamily(headOfFamily, period)
            .filter { it.period.overlaps(period) }
            .forEach {
                // intentionally does not care about feeDecisionMinDate
                handleDecisionChanges(maxOf(period.start, it.period.start), it.headOfFamily, it.partner, it.children)
            }
    }

    @Transactional
    fun handlePlacement(childId: UUID, period: Period) {
        logger.debug { "Generating fee decisions from new placement (childId: $childId, period: $period)" }

        val families = findFamiliesByChild(childId, period)
        handleDecisionChangesForFamilies(period, families)
    }

    @Transactional
    fun handleServiceNeed(childId: UUID, period: Period) {
        logger.debug { "Generating fee decisions from changed service need (childId: $childId, period: $period)" }

        val families = findFamiliesByChild(childId, period)
        handleDecisionChangesForFamilies(period, families)
    }

    @Transactional
    fun handleFamilyUpdate(adultId: UUID, period: Period) {
        logger.debug { "Generating fee decisions from changed family (adultId: $adultId, period: $period)" }

        val families = findFamiliesByHeadOfFamily(adultId, period) + findFamiliesByPartner(adultId, period)
        handleDecisionChangesForFamilies(period, families)
    }

    @Transactional
    fun handleIncomeChange(personId: UUID, period: Period) {
        logger.debug { "Generating fee decisions from changed income (personId: $personId, period: $period)" }

        val families = findFamiliesByHeadOfFamily(personId, period) + findFamiliesByPartner(personId, period)
        handleDecisionChangesForFamilies(period, families)
    }

    @Transactional
    fun handleFeeAlterationChange(childId: UUID, period: Period) {
        logger.debug { "Generating fee decisions from changed fee alteration (childId: $childId, period: $period)" }

        val families = findFamiliesByChild(childId, period)
        handleDecisionChangesForFamilies(period, families)
    }

    internal fun handleDecisionChangesForFamilies(period: Period, families: List<FridgeFamily>) {
        families.filter { it.period.overlaps(period) }
            .forEach {
                handleDecisionChanges(maxOf(feeDecisionMinDate, period.start, it.period.start), it.headOfFamily, it.partner, it.children)
            }
    }

    internal fun mapToFamilies(families: List<Pair<Period, FamilyComposition>>): List<FridgeFamily> {
        return families.map { (period, family) ->
            FridgeFamily(
                PersonData.JustId(family.headOfFamily.id),
                family.partner?.let { PersonData.JustId(it.id) },
                family.children.map {
                    PersonData.WithDateOfBirth(
                        id = it.id,
                        dateOfBirth = it.dateOfBirth
                    )
                },
                period
            )
        }
    }

    internal fun findFamiliesByChild(childId: UUID, wholePeriod: Period): List<FridgeFamily> {
        val parentRelations = parentshipService.getParentshipsByChildId(childId)

        val familyCompositions = parentRelations.flatMap {
            val fridgePartners = partnershipService.getPartnersForPerson((it.headOfChildId))
            val fridgeChildren = parentshipService.getParentshipsByHeadOfChildId(it.headOfChildId)
            generateFamilyCompositions(
                it.headOfChild,
                fridgePartners,
                fridgeChildren,
                Period(it.startDate, it.endDate)
            )
        }

        val periodsWhereChildHasNoHeadOfFamily = asDistinctPeriods(familyCompositions.map { it.first }, wholePeriod)
            .map { period -> period to parentRelations.find { Period(it.startDate, it.endDate).contains(period) } }
            .let { mergePeriods(it) }
            .filter { (_, parent) -> parent == null }
            .map { (period, _) -> period }

        if (periodsWhereChildHasNoHeadOfFamily.isNotEmpty()) {
            logger.warn("No family found for $childId during ${periodsWhereChildHasNoHeadOfFamily.joinToString { "${it.start} - ${it.end}" }}")
        }

        return mapToFamilies(familyCompositions)
    }

    internal fun findFamiliesByHeadOfFamily(headOfFamilyId: UUID, period: Period): List<FridgeFamily> {
        val headOfFamily = personService.getPerson(headOfFamilyId)?.let { PersonJSON.from(it) }
            ?: throw Error("Head of family $headOfFamilyId not found")
        val childRelations = parentshipService.getParentshipsByHeadOfChildId(headOfFamilyId)
        val partners = partnershipService.getPartnersForPerson(headOfFamilyId)
        val familyPeriods = generateFamilyCompositions(headOfFamily, partners, childRelations, period)
        return mapToFamilies(familyPeriods)
    }

    internal fun findFamiliesByPartner(personId: UUID, period: Period): List<FridgeFamily> {
        val possibleHeadsOfFamily = partnershipService.getPartnersForPerson(personId)
        return possibleHeadsOfFamily.flatMap { findFamiliesByHeadOfFamily(it.person.id, period) }
    }

    internal fun generateFamilyCompositions(
        headOfFamily: PersonJSON,
        partners: Set<fi.espoo.evaka.pis.service.Partner>,
        parentships: Set<Parentship>,
        wholePeriod: Period
    ): List<Pair<Period, FamilyComposition>> {
        val periodsWhenChildrenAreNotAdults = parentships.map {
            val birthday = it.child.dateOfBirth
            Period(birthday, birthday.plusYears(18))
        }

        val allPeriods = partners.map { Period(it.startDate, it.endDate) } +
            parentships.map { Period(it.startDate, it.endDate) } + periodsWhenChildrenAreNotAdults

        val familyPeriods = asDistinctPeriods(allPeriods, wholePeriod)
            .map { period ->
                val partner = partners.find { Period(it.startDate, it.endDate).contains(period) }?.person
                val children = parentships
                    .filter { Period(it.startDate, it.endDate).contains(period) }
                    // Do not include children that are over 18 years old during the period
                    .filter { it.child.dateOfBirth.plusYears(18) >= period.start }
                    .map { it.child }
                period to FamilyComposition(
                    headOfFamily = headOfFamily,
                    partner = partner,
                    children = children
                )
            }

        return mergePeriods(familyPeriods)
    }

    internal fun handleDecisionChanges(
        from: LocalDate,
        headOfFamily: PersonData.JustId,
        partner: PersonData.JustId?,
        children: List<PersonData.WithDateOfBirth>
    ) {
        logger.debug {
            "Handling decision changes with arguments: ${toJson(
                mapOf(
                    "from" to from,
                    "headOfFamily" to headOfFamily,
                    "partner" to partner,
                    "children" to children
                )
            )}"
        }

        val familySize = 1 + (partner?.let { 1 } ?: 0) + children.size
        val prices = withSpringTx(txManager) { withSpringHandle(dataSource, getPricing(from)) }
        val incomes = withSpringTx(txManager) {
            withSpringHandle(dataSource) { h ->
                getIncomesFrom(h, objectMapper, listOfNotNull(headOfFamily.id, partner?.id), from)
            }
        }
        val feeAlterations = withSpringTx(txManager) {
            withSpringHandle(dataSource) { h ->
                getFeeAlterationsFrom(h, children.map { it.id }, from)
            }
        } + addECHAFeeAlterations(children, incomes)

        val placements = fetchPlacements(from, children)
        val unitsThatAreNotInvoiced =
            withSpringTx(txManager) { withSpringHandle(dataSource, getUnitsThatAreNotInvoiced()) }

        logger.debug {
            "Generating new decision with arguments: ${toJson(
                mapOf(
                    "from" to from,
                    "headOfFamily" to headOfFamily,
                    "partner" to partner,
                    "familySize" to familySize,
                    "placements" to placements,
                    "prices" to prices,
                    "incomes" to incomes,
                    "feeAlterations" to feeAlterations
                )
            )}"
        }
        val newDrafts =
            generateNewDecisions(
                from,
                headOfFamily,
                partner,
                familySize,
                placements,
                prices,
                incomes,
                feeAlterations,
                unitsThatAreNotInvoiced
            )

        logger.debug {
            "Generated new draft decisions: ${toJson(newDrafts)}"
        }

        withSpringTx(txManager) { withSpringHandle(dataSource, lockFeeDecisionsForHeadOfFamily(headOfFamily.id)) }

        val existingDrafts = withSpringTx(txManager) {
            withSpringHandle(dataSource) { h ->
                findFeeDecisionsForHeadOfFamily(h, objectMapper, headOfFamily.id, null, listOf(FeeDecisionStatus.DRAFT))
            }
        }

        logger.debug {
            "Existing drafts: ${toJson(existingDrafts)}"
        }

        val draftsWithUpdatedDates = filterOrUpdateStaleDrafts(existingDrafts, Period(from, null))
            .map { it.copy(id = UUID.randomUUID()) }

        logger.debug {
            "Existing drafts with updated dates: ${toJson(draftsWithUpdatedDates)}"
        }

        val activeDecisions = withSpringTx(txManager) {
            withSpringHandle(dataSource) { h ->
                findFeeDecisionsForHeadOfFamily(
                    h,
                    objectMapper,
                    headOfFamily.id,
                    null,
                    listOf(
                        FeeDecisionStatus.WAITING_FOR_SENDING,
                        FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                        FeeDecisionStatus.SENT
                    )
                )
            }
        }

        val (withUpdatedEndDates, mergedDrafts) = updateDecisionEndDatesAndMergeDrafts(
            activeDecisions,
            newDrafts + draftsWithUpdatedDates
        )

        logger.debug { "Merged new and existing drafts: ${toJson(mergedDrafts)}" }
        logger.debug { "Updated active decisions: ${toJson(withUpdatedEndDates)}" }

        withSpringTx(txManager) {
            withSpringHandle(dataSource) { h ->
                deleteFeeDecisions(h, existingDrafts.map { it.id })
                upsertFeeDecisions(h, objectMapper, mergedDrafts + withUpdatedEndDates)
            }
        }
    }

    private fun fetchPlacements(
        from: LocalDate,
        children: List<PersonData.WithDateOfBirth>
    ): List<Pair<PersonData.WithDateOfBirth, List<Pair<Period, PermanentPlacement>>>> {
        return children.map { child ->
            val placements =
                withSpringTx(txManager) { withSpringHandle(dataSource, getActivePlacements(child.id, from)) }
            if (placements.isEmpty()) return@map child to listOf<Pair<Period, PermanentPlacement>>()

            val serviceNeeds = withSpringHandle(dataSource) { h ->
                getServiceNeedsByChildDuringPeriod(h, child.id, from, null)
            }
            val placementsWithServiceNeeds = addServiceNeedsToPlacements(child.dateOfBirth, placements, serviceNeeds)

            child to placementsWithServiceNeeds
        }
    }
}

internal fun getPlacementType(placement: Placement, isFiveYearOld: Boolean): PlacementType =
    when (placement.type) {
        fi.espoo.evaka.placement.PlacementType.CLUB -> PlacementType.CLUB
        fi.espoo.evaka.placement.PlacementType.DAYCARE,
        fi.espoo.evaka.placement.PlacementType.DAYCARE_PART_TIME ->
            if (isFiveYearOld) PlacementType.FIVE_YEARS_OLD_DAYCARE
            else PlacementType.DAYCARE
        fi.espoo.evaka.placement.PlacementType.PRESCHOOL -> PlacementType.PRESCHOOL
        fi.espoo.evaka.placement.PlacementType.PRESCHOOL_DAYCARE -> PlacementType.PRESCHOOL_WITH_DAYCARE
        fi.espoo.evaka.placement.PlacementType.PREPARATORY -> PlacementType.PREPARATORY
        fi.espoo.evaka.placement.PlacementType.PREPARATORY_DAYCARE -> PlacementType.PREPARATORY_WITH_DAYCARE
    }

internal fun addECHAFeeAlterations(
    children: List<PersonData.WithDateOfBirth>,
    incomes: List<Income>
): List<FeeAlteration> {
    return incomes.filter { it.worksAtECHA }.flatMap { income ->
        children.map { child -> getECHAIncrease(child.id, Period(income.validFrom, income.validTo)) }
    }
}

internal fun addServiceNeedsToPlacements(
    dateOfBirth: LocalDate,
    placements: List<Placement>,
    serviceNeeds: List<ServiceNeed>
): List<Pair<Period, PermanentPlacement>> {
    val yearTheChildTurns5 = dateOfBirth.year + 5
    val fiveYearOldTerm = Period(LocalDate.of(yearTheChildTurns5, 8, 1), LocalDate.of(yearTheChildTurns5 + 1, 7, 31))

    return placements.flatMap { placement ->
        val placementPeriod = Period(placement.startDate, placement.endDate)
        asDistinctPeriods(serviceNeeds.map { Period(it.startDate, it.endDate) } + fiveYearOldTerm, placementPeriod)
            .mapNotNull { period ->
                val serviceNeed = serviceNeeds.find { (Period(it.startDate, it.endDate)).contains(period) }
                // Do not include temporary placements as they should be invoiced separately
                if (serviceNeed?.temporary == true) null
                else {
                    val isFiveYearOld = fiveYearOldTerm.contains(period)
                    val placementType = getPlacementType(placement, isFiveYearOld)
                    period to PermanentPlacement(
                        unit = placement.unitId,
                        type = placementType,
                        serviceNeed = calculateServiceNeed(placementType, serviceNeed?.hoursPerWeek)
                    )
                }
            }
            .let { mergePeriods(it) }
    }
}

internal fun filterOrUpdateStaleDrafts(drafts: List<FeeDecision>, period: Period): List<FeeDecision> {
    val (overlappingDrafts, nonOverlappingDrafts) = drafts.partition {
        Period(
            it.validFrom,
            it.validTo
        ).overlaps(period)
    }

    val updatedOverlappingDrafts = when (period.end) {
        null -> overlappingDrafts.flatMap {
            when {
                it.validFrom < period.start -> listOf(it.copy(validTo = period.start.minusDays(1)))
                else -> emptyList()
            }
        }
        else -> overlappingDrafts.flatMap {
            when {
                it.validFrom < period.start && orMax(it.validTo) > orMax(period.end) -> listOf(
                    it.copy(validFrom = it.validFrom, validTo = period.start.minusDays(1)),
                    it.copy(validFrom = period.end.plusDays(1), validTo = it.validTo)
                )
                it.validFrom < period.start && orMax(it.validTo) <= orMax(period.end) -> listOf(
                    it.copy(
                        validFrom = it.validFrom,
                        validTo = period.start.minusDays(1)
                    )
                )
                it.validFrom >= period.start && orMax(it.validTo) > orMax(period.end) -> listOf(
                    it.copy(
                        validFrom = period.end.plusDays(1),
                        validTo = it.validTo
                    )
                )
                else -> emptyList()
            }
        }
    }

    return nonOverlappingDrafts + updatedOverlappingDrafts
}

internal fun mergeAndFilterUnnecessaryDrafts(
    drafts: List<FeeDecision>,
    active: List<FeeDecision>
): List<FeeDecision> {
    if (drafts.isEmpty()) return drafts

    val minDate = drafts.map { it.validFrom }.min()!! // min always exists when list is non-empty
    val maxDate = drafts.map { it.validTo }.maxBy { orMax(it) }

    return asDistinctPeriods((drafts + active).map { Period(it.validFrom, it.validTo) }, Period(minDate, maxDate))
        .fold(listOf<FeeDecision>()) { decisions, period ->
            val keptDraft = drafts.find { Period(it.validFrom, it.validTo).contains(period) }?.let { draft ->
                val decision = active.find { Period(it.validFrom, it.validTo).contains(period) }
                if (draftIsUnnecessary(draft, decision, alreadyGeneratedDrafts = decisions.isNotEmpty())) {
                    null
                } else {
                    draft.copy(validFrom = period.start, validTo = period.end)
                }
            }
            if (keptDraft != null) decisions + keptDraft else decisions
        }
        .let { mergeFeeDecisions(it) }
}

internal fun decisionsAreEqualEnough(decision1: FeeDecision, decision2: FeeDecision): Boolean {
    return FeeDecisionInvariant(
        decision1.headOfFamily,
        decision1.partner,
        decision1.headOfFamilyIncome,
        decision1.partnerIncome,
        decision1.familySize,
        decision1.pricing,
        decision1.parts.toSet()
    ) == FeeDecisionInvariant(
        decision2.headOfFamily,
        decision2.partner,
        decision2.headOfFamilyIncome,
        decision2.partnerIncome,
        decision2.familySize,
        decision2.pricing,
        decision2.parts.toSet()
    )
}

/*
 * a draft is unnecessary when:
 *   - the draft is "empty" and there is no existing sent decision that should be overridden
 *   - the draft is practically identical to an existing sent decision and no drafts have been generated before this draft
 */
internal fun draftIsUnnecessary(draft: FeeDecision, sent: FeeDecision?, alreadyGeneratedDrafts: Boolean): Boolean {
    return (draft.parts.isEmpty() && sent == null) ||
        (!alreadyGeneratedDrafts && sent != null && decisionsAreEqualEnough(draft, sent))
}

internal fun mergeFeeDecisions(decisions: List<FeeDecision>): List<FeeDecision> {
    return decisions
        .map {
            Period(it.validFrom, it.validTo) to FeeDecisionInvariant(
                it.headOfFamily,
                it.partner,
                it.headOfFamilyIncome,
                it.partnerIncome,
                it.familySize,
                it.pricing,
                it.parts.toSet()
            )
        }
        .let { mergePeriods(it) }
        .map { (period, data) ->
            FeeDecision(
                id = UUID.randomUUID(),
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamily = data.headOfFamily,
                partner = data.partner,
                headOfFamilyIncome = data.headOfFamilyIncome,
                partnerIncome = data.partnerIncome,
                familySize = data.familySize,
                pricing = data.pricing,
                parts = data.parts.toList().sortedBy { it.siblingDiscount },
                validFrom = period.start,
                validTo = period.end
            )
        }
}

internal fun generateNewDecisions(
    from: LocalDate,
    headOfFamily: PersonData.JustId,
    partner: PersonData.JustId?,
    familySize: Int,
    allPlacements: List<Pair<PersonData.WithDateOfBirth, List<Pair<Period, PermanentPlacement>>>>,
    prices: List<Pair<Period, Pricing>>,
    incomes: List<Income>,
    feeAlterations: List<FeeAlteration>,
    unitsThatAreNotInvoiced: List<UnitData.InvoicedByMunicipality>
): List<FeeDecision> {
    val periods = incomes.map { Period(it.validFrom, it.validTo) } +
        prices.map { it.first } +
        allPlacements.flatMap { (_, placements) ->
            placements.map { it.first }
        } +
        feeAlterations.map { Period(it.validFrom, it.validTo) }

    val decisionPeriods = asDistinctPeriods(periods, Period(from, null)).map { period ->
        val price = prices.find { it.first.contains(period) }?.second
            ?: error("Missing price for period ${period.start} - ${period.end}, cannot generate fee decision")

        val income = incomes
            .find { headOfFamily.id == it.personId && Period(it.validFrom, it.validTo).contains(period) }
            ?.toDecisionIncome()

        val partnerIncome =
            partner?.let {
                incomes
                    .find {
                        partner.id == it.personId && Period(
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

        val parts = if (validPlacements.isNotEmpty()) {
            val familyIncomes = partner?.let { listOf(income, partnerIncome) } ?: listOf(income)

            val baseFee = calculateBaseFee(price, familySize, familyIncomes)
            validPlacements
                .sortedByDescending { (child, _) -> child.dateOfBirth }
                .mapIndexed { index, (child, placement) ->
                    val unitIsNotInvoiced = unitsThatAreNotInvoiced.any { unit -> unit.id == placement.unit }
                    if (unitIsNotInvoiced) {
                        return@mapIndexed null
                    }

                    val serviceNeedIsFree = serviceNeedIsNotInvoiced(placement.serviceNeed)
                    if (serviceNeedIsFree) {
                        return@mapIndexed null
                    }

                    val siblingDiscount = getSiblingDiscountPercent(index + 1)
                    val feeBeforeAlterations =
                        calculateFeeBeforeFeeAlterations(baseFee, placement, siblingDiscount)
                    val relevantFeeAlterations = feeAlterations.filter {
                        it.personId == child.id && Period(it.validFrom, it.validTo).contains(period)
                    }

                    FeeDecisionPart(
                        child,
                        placement,
                        baseFee,
                        siblingDiscount,
                        feeBeforeAlterations,
                        toFeeAlterationsWithEffects(feeBeforeAlterations, relevantFeeAlterations)
                    )
                }.filterNotNull()
        } else {
            listOf()
        }

        period to FeeDecisionInvariant(
            headOfFamily = headOfFamily,
            partner = partner,
            headOfFamilyIncome = income,
            partnerIncome = partnerIncome,
            familySize = familySize,
            pricing = price,
            parts = parts.toSet()
        )
    }.let { mergePeriods(it) }

    return decisionPeriods.map { (period, decisionData) ->
        FeeDecision(
            id = UUID.randomUUID(),
            status = FeeDecisionStatus.DRAFT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamily = decisionData.headOfFamily,
            partner = decisionData.partner,
            headOfFamilyIncome = decisionData.headOfFamilyIncome,
            partnerIncome = decisionData.partnerIncome,
            familySize = decisionData.familySize,
            pricing = decisionData.pricing,
            parts = decisionData.parts.toList().sortedBy { it.siblingDiscount },
            validFrom = period.start,
            validTo = period.end
        )
    }
}

internal fun updateDecisionEndDatesAndMergeDrafts(
    actives: List<FeeDecision>,
    drafts: List<FeeDecision>
): Pair<List<FeeDecision>, List<FeeDecision>> {
    val mergedDrafts = mergeFeeDecisions(drafts)

    /*
     * Immediately update the validity end dates for active decisions if a new draft has the same contents and they
     * both are valid to the future
     */
    val (updatedActives, keptDrafts) = actives.fold(
        Pair(
            listOf<FeeDecision>(),
            mergedDrafts
        )
    ) { (updatedActives, keptDrafts), decision ->
        val firstOverlappingSimilarDraft = keptDrafts.filter { draft ->
            decision.validFrom == draft.validFrom
        }.firstOrNull { draft ->
            decisionsAreEqualEnough(decision, draft)
        }

        firstOverlappingSimilarDraft?.let { similarDraft ->
            val now = LocalDate.now()
            if (orMax(decision.validTo) >= now && orMax(similarDraft.validTo) >= now) {
                Pair(
                    updatedActives + decision.copy(validTo = similarDraft.validTo),
                    keptDrafts.filterNot { it.id == similarDraft.id }
                )
            } else null
        } ?: Pair(updatedActives, keptDrafts)
    }

    val allUpdatedActives = actives.map { decision -> updatedActives.find { it.id == decision.id } ?: decision }
    val filteredDrafts = mergeAndFilterUnnecessaryDrafts(keptDrafts, allUpdatedActives)

    return Pair(updatedActives, filteredDrafts)
}

data class FamilyComposition(
    val headOfFamily: PersonJSON,
    val partner: PersonJSON?,
    val children: List<PersonJSON>
)

fun getUnitsThatAreNotInvoiced(): (Handle) -> List<UnitData.InvoicedByMunicipality> = { h ->
    val sql =
        """
        SELECT
            daycare.id,
            daycare.invoiced_by_municipality
        FROM daycare
        WHERE daycare.invoiced_by_municipality = false
    """

    h.createQuery(sql)
        .map { rs: ResultSet, _ ->
            UnitData.InvoicedByMunicipality(
                id = rs.getUUID("id"),
                invoicedByMunicipality = rs.getBoolean("invoiced_by_municipality")
            )
        }
        .list()
}

/**
 * Leaves out club placements since they shouldn't have an effect on fee decisions
 */
fun getActivePlacements(childId: UUID, from: LocalDate): (Handle) -> List<Placement> = { h ->
    // language=sql
    val sql = "SELECT * FROM placement WHERE child_id = :childId AND end_date >= :from AND NOT type = 'CLUB'::placement_type"

    h.createQuery(sql)
        .bind("childId", childId)
        .bind("from", from)
        .map { rs, _ ->
            Placement(
                id = rs.getUUID("id"),
                type = rs.getEnum("type"),
                childId = rs.getUUID("child_id"),
                unitId = rs.getUUID("unit_id"),
                startDate = rs.getDate("start_date").toLocalDate(),
                endDate = rs.getDate("end_date").toLocalDate()
            )
        }
        .list()
}

fun lockFeeDecisionsForHeadOfFamily(headOfFamily: UUID): (Handle) -> Unit = { h ->
    h.createUpdate("SELECT id FROM fee_decision WHERE head_of_family = :headOfFamily FOR UPDATE")
        .bind("headOfFamily", headOfFamily)
        .execute()
}
