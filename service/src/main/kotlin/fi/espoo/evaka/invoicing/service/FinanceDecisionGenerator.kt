// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FinanceDecision
import fi.espoo.evaka.invoicing.domain.FridgeFamily
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.decisionContentsAreEqual
import fi.espoo.evaka.invoicing.domain.getECHAIncrease
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPartnersForPerson
import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.Partner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.minEndDate
import fi.espoo.evaka.shared.domain.orMax
import mu.KotlinLogging
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class FinanceDecisionGenerator(private val objectMapper: ObjectMapper, env: Environment) {
    private val feeDecisionMinDate: LocalDate = LocalDate.parse(env.getRequiredProperty("fee_decision_min_date"))

    fun createRetroactive(tx: Database.Transaction, headOfFamily: UUID, from: LocalDate) {
        val period = DateRange(from, null)
        tx.findFamiliesByHeadOfFamily(headOfFamily, period)
            .filter { it.period.overlaps(period) }
            .forEach {
                // intentionally does not care about feeDecisionMinDate
                tx.handleFeeDecisionChanges(
                    objectMapper,
                    maxOf(period.start, it.period.start),
                    it.headOfFamily,
                    it.partner,
                    it.children
                )
            }
    }

    fun handlePlacement(tx: Database.Transaction, childId: UUID, period: DateRange) {
        logger.debug { "Generating fee decisions from new placement (childId: $childId, period: $period)" }

        val families = tx.findFamiliesByChild(childId, period)
        handleDecisionChangesForFamilies(tx, period, families)
    }

    fun handleServiceNeed(tx: Database.Transaction, childId: UUID, period: DateRange) {
        logger.debug { "Generating fee decisions from changed service need (childId: $childId, period: $period)" }

        val families = tx.findFamiliesByChild(childId, period)
        handleDecisionChangesForFamilies(tx, period, families)
    }

    fun handleFamilyUpdate(tx: Database.Transaction, adultId: UUID, period: DateRange) {
        logger.debug { "Generating fee decisions from changed family (adultId: $adultId, period: $period)" }

        val families = tx.findFamiliesByHeadOfFamily(adultId, period) + tx.findFamiliesByPartner(adultId, period)
        handleDecisionChangesForFamilies(tx, period, families)
    }

    fun handleIncomeChange(tx: Database.Transaction, personId: UUID, period: DateRange) {
        logger.debug { "Generating fee decisions from changed income (personId: $personId, period: $period)" }

        val families = tx.findFamiliesByHeadOfFamily(personId, period) + tx.findFamiliesByPartner(personId, period)
        handleDecisionChangesForFamilies(tx, period, families)
    }

    fun handleFeeAlterationChange(tx: Database.Transaction, childId: UUID, period: DateRange) {
        logger.debug { "Generating fee decisions from changed fee alteration (childId: $childId, period: $period)" }

        val families = tx.findFamiliesByChild(childId, period)
        handleDecisionChangesForFamilies(tx, period, families)
    }

    private fun handleDecisionChangesForFamilies(tx: Database.Transaction, period: DateRange, families: List<FridgeFamily>) {
        families.filter { it.period.overlaps(period) }
            .forEach { family ->
                tx.handleFeeDecisionChanges(
                    objectMapper,
                    maxOf(feeDecisionMinDate, period.start, family.period.start),
                    family.headOfFamily,
                    family.partner,
                    family.children
                )
                family.children.forEach { child ->
                    tx.handleValueDecisionChanges(
                        objectMapper,
                        maxOf(period.start, family.period.start),
                        child,
                        family.headOfFamily,
                        family.partner,
                        family.children
                    )
                }
            }
    }
}

private fun Database.Read.findFamiliesByChild(childId: UUID, period: DateRange): List<FridgeFamily> {
    val parentRelations = getParentships(null, childId, includeConflicts = false, period = period)

    return parentRelations.flatMap {
        val fridgePartners = getPartnersForPerson(it.headOfChildId, includeConflicts = false, period = period)
        val fridgeChildren = getParentships(it.headOfChildId, null, includeConflicts = false, period = period)
        generateFamilyCompositions(
            it.headOfChild.id,
            fridgePartners,
            fridgeChildren,
            DateRange(maxOf(period.start, it.startDate), minEndDate(period.end, it.endDate))
        )
    }
}

private fun Database.Read.findFamiliesByHeadOfFamily(headOfFamilyId: UUID, period: DateRange): List<FridgeFamily> {
    val childRelations = getParentships(headOfFamilyId, null, includeConflicts = false, period = period)
    val partners = getPartnersForPerson(headOfFamilyId, includeConflicts = false, period = period)
    return generateFamilyCompositions(headOfFamilyId, partners, childRelations, period)
}

private fun Database.Read.findFamiliesByPartner(personId: UUID, period: DateRange): List<FridgeFamily> {
    val possibleHeadsOfFamily = getPartnersForPerson(personId, includeConflicts = false, period = period)
    return possibleHeadsOfFamily.flatMap { findFamiliesByHeadOfFamily(it.person.id, period) }
}

private fun generateFamilyCompositions(
    headOfFamily: UUID,
    partners: Iterable<Partner>,
    parentships: Iterable<Parentship>,
    wholePeriod: DateRange
): List<FridgeFamily> {
    val periodsWhenChildrenAreNotAdults = parentships.map {
        val birthday = it.child.dateOfBirth
        DateRange(birthday, birthday.plusYears(18))
    }

    val allPeriods = partners.map { DateRange(it.startDate, it.endDate) } +
        parentships.map { DateRange(it.startDate, it.endDate) } + periodsWhenChildrenAreNotAdults

    val familyPeriods = asDistinctPeriods(allPeriods, wholePeriod)
        .map { period ->
            val partner = partners.find { DateRange(it.startDate, it.endDate).contains(period) }?.person
            val children = parentships
                .filter { DateRange(it.startDate, it.endDate).contains(period) }
                // Do not include children that are over 18 years old during the period
                .filter { it.child.dateOfBirth.plusYears(18) >= period.start }
                .map { it.child }
            period to Triple(
                PersonData.JustId(headOfFamily),
                partner?.let { PersonData.JustId(it.id) },
                children.map { PersonData.WithDateOfBirth(it.id, it.dateOfBirth) }
            )
        }

    return mergePeriods(familyPeriods).map { (period, familyData) ->
        FridgeFamily(
            headOfFamily = familyData.first,
            partner = familyData.second,
            children = familyData.third,
            period = period
        )
    }
}

internal fun <Decision : FinanceDecision<Decision>> mergeAndFilterUnnecessaryDrafts(
    drafts: List<Decision>,
    active: List<Decision>
): List<Decision> {
    if (drafts.isEmpty()) return drafts

    val minDate = drafts.map { it.validFrom }.minOrNull()!! // min always exists when list is non-empty
    val maxDate = drafts.map { it.validTo }.maxByOrNull { orMax(it) }

    return asDistinctPeriods((drafts + active).map { DateRange(it.validFrom, it.validTo) }, DateRange(minDate, maxDate))
        .fold(listOf<Decision>()) { decisions, period ->
            val keptDraft = drafts.find { DateRange(it.validFrom, it.validTo).contains(period) }?.let { draft ->
                val decision = active.find { DateRange(it.validFrom, it.validTo).contains(period) }
                if (draftIsUnnecessary(draft, decision, alreadyGeneratedDrafts = decisions.isNotEmpty())) {
                    null
                } else {
                    draft.withValidity(DateRange(period.start, period.end))
                }
            }
            if (keptDraft != null) decisions + keptDraft else decisions
        }
        .let { mergeDecisions(it) }
}

/*
 * a draft is unnecessary when:
 *   - the draft is "empty" and there is no existing sent decision that should be overridden
 *   - the draft is practically identical to an existing sent decision and no drafts have been generated before this draft
 */
internal fun <Decision : FinanceDecision<Decision>> draftIsUnnecessary(
    draft: Decision,
    sent: Decision?,
    alreadyGeneratedDrafts: Boolean
): Boolean {
    return (draft.isEmpty() && sent == null) ||
        (!alreadyGeneratedDrafts && sent != null && draft.contentEquals(sent))
}

internal fun <Decision : FinanceDecision<Decision>> mergeDecisions(
    decisions: List<Decision>
): List<Decision> {
    return decisions
        .map { DateRange(it.validFrom, it.validTo) to it }
        .let { mergePeriods(it, ::decisionContentsAreEqual) }
        .map { (period, decision) -> decision.withValidity(period) }
        .map { it.withRandomId() }
}

internal fun <Decision : FinanceDecision<Decision>> updateExistingDecisions(
    from: LocalDate,
    newDrafts: List<Decision>,
    existingDrafts: List<Decision>,
    activeDecisions: List<Decision>
): List<Decision> {
    val draftsWithUpdatedDates = filterOrUpdateStaleDrafts(existingDrafts, DateRange(from, null))
        .map { it.withRandomId() }

    val (withUpdatedEndDates, mergedDrafts) = updateDecisionEndDatesAndMergeDrafts(
        activeDecisions,
        newDrafts + draftsWithUpdatedDates
    )

    return mergedDrafts + withUpdatedEndDates
}

internal fun <Decision : FinanceDecision<Decision>> filterOrUpdateStaleDrafts(
    drafts: List<Decision>,
    period: DateRange
): List<Decision> {
    val (overlappingDrafts, nonOverlappingDrafts) = drafts.partition {
        DateRange(it.validFrom, it.validTo).overlaps(period)
    }

    val updatedOverlappingDrafts = when (period.end) {
        null -> overlappingDrafts.flatMap {
            when {
                it.validFrom < period.start -> listOf(it.withValidity(DateRange(it.validFrom, period.start.minusDays(1))))
                else -> emptyList()
            }
        }
        else -> overlappingDrafts.flatMap {
            when {
                it.validFrom < period.start && orMax(it.validTo) > orMax(period.end) -> listOf(
                    it.withValidity(DateRange(it.validFrom, period.start.minusDays(1))),
                    it.withValidity(DateRange(period.end.plusDays(1), it.validTo))
                )
                it.validFrom < period.start && orMax(it.validTo) <= orMax(period.end) -> listOf(
                    it.withValidity(DateRange(it.validFrom, period.start.minusDays(1)))
                )
                it.validFrom >= period.start && orMax(it.validTo) > orMax(period.end) -> listOf(
                    it.withValidity(DateRange(period.end.plusDays(1), it.validTo))
                )
                else -> emptyList()
            }
        }
    }

    return nonOverlappingDrafts + updatedOverlappingDrafts
}

internal fun <Decision : FinanceDecision<Decision>> updateDecisionEndDatesAndMergeDrafts(
    actives: List<Decision>,
    drafts: List<Decision>
): Pair<List<Decision>, List<Decision>> {
    val mergedDrafts = mergeDecisions(drafts)

    /*
     * Immediately update the validity end dates for active decisions if a new draft has the same contents and they
     * both are valid to the future
     */
    val (updatedActives, keptDrafts) = actives.fold(
        Pair(listOf<Decision>(), mergedDrafts)
    ) { (updatedActives, keptDrafts), decision ->
        val firstOverlappingSimilarDraft = keptDrafts.filter { draft ->
            decision.validFrom == draft.validFrom
        }.firstOrNull { draft -> decision.contentEquals(draft) }

        firstOverlappingSimilarDraft?.let { similarDraft ->
            val now = LocalDate.now()
            if (orMax(decision.validTo) >= now && orMax(similarDraft.validTo) >= now) {
                Pair(
                    updatedActives + decision.withValidity(DateRange(decision.validFrom, similarDraft.validTo)),
                    keptDrafts.filterNot { it.id == similarDraft.id }
                )
            } else null
        } ?: Pair(updatedActives, keptDrafts)
    }

    val allUpdatedActives = actives.map { decision -> updatedActives.find { it.id == decision.id } ?: decision }
    val filteredDrafts = mergeAndFilterUnnecessaryDrafts(keptDrafts, allUpdatedActives)

    return Pair(updatedActives, filteredDrafts)
}

internal fun addECHAFeeAlterations(
    children: List<PersonData.WithDateOfBirth>,
    incomes: List<Income>
): List<FeeAlteration> {
    return incomes.filter { it.worksAtECHA }.flatMap { income ->
        children.map { child -> getECHAIncrease(child.id, DateRange(income.validFrom, income.validTo)) }
    }
}
