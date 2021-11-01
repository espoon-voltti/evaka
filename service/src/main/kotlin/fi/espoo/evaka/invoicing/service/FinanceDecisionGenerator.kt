// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.EvakaEnv
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
import fi.espoo.evaka.pis.service.getChildGuardians
import fi.espoo.evaka.shared.FeatureFlags
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.orMax
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Component
class FinanceDecisionGenerator(
    private val objectMapper: ObjectMapper,
    private val incomeTypesProvider: IncomeTypesProvider,
    env: EvakaEnv,
    featureFlags: FeatureFlags
) {
    private val feeDecisionMinDate = env.feeDecisionMinDate
    private val valueDecisionCapacityFactorEnabled = featureFlags.valueDecisionCapacityFactorEnabled

    fun createRetroactive(tx: Database.Transaction, headOfFamily: UUID, from: LocalDate) {
        val families = tx.findFamiliesByHeadOfFamily(headOfFamily, from)
        tx.handleFeeDecisionChanges(
            objectMapper,
            incomeTypesProvider,
            from, // intentionally does not care about feeDecisionMinDate
            PersonData.JustId(headOfFamily),
            families
        )
    }

    fun generateNewDecisionsForAdult(tx: Database.Transaction, personId: UUID, from: LocalDate) {
        val fromOrMinDate = maxOf(feeDecisionMinDate, from)
        val families = tx.findFamiliesByAdult(personId, fromOrMinDate)
        handleDecisionChangesForFamilies(tx, fromOrMinDate, families)
    }

    fun generateNewDecisionsForChild(tx: Database.Transaction, personId: UUID, from: LocalDate) {
        val fromOrMinDate = maxOf(feeDecisionMinDate, from)
        val families = tx.findFamiliesByChild(personId, fromOrMinDate)
        handleDecisionChangesForFamilies(tx, fromOrMinDate, families)
    }

    private fun handleDecisionChangesForFamilies(
        tx: Database.Transaction,
        from: LocalDate,
        families: List<FridgeFamily>
    ) {
        families
            .groupBy { it.headOfFamily }
            .forEach { (headOfFamily, families) ->
                tx.handleFeeDecisionChanges(
                    objectMapper,
                    incomeTypesProvider,
                    from,
                    headOfFamily,
                    families
                )
            }

        families
            .flatMap { family -> family.children.map { it to family } }
            .groupingBy { (child, _) -> child }
            .fold(listOf<FridgeFamily>()) { childFamilies, (_, family) ->
                childFamilies + family
            }
            .forEach { (child, families) ->
                tx.handleValueDecisionChanges(
                    valueDecisionCapacityFactorEnabled,
                    objectMapper,
                    incomeTypesProvider,
                    from,
                    child,
                    families
                )
            }
    }
}

private fun Database.Read.findFamiliesByChild(childId: UUID, from: LocalDate): List<FridgeFamily> {
    val dateRange = DateRange(from, null)
    val parentRelations = getParentships(null, childId, includeConflicts = false, period = dateRange)

    return parentRelations.flatMap {
        val fridgePartners = getPartnersForPerson(it.headOfChildId, includeConflicts = false, period = dateRange)
        val fridgeChildren = getParentships(it.headOfChildId, null, includeConflicts = false, period = dateRange)
        val fridgePartnerParentships =
            fridgePartners.flatMap { partner -> getParentships(partner.person.id, null, false, dateRange) }
        val fridgePartnerParentshipsWithGuardianship =
            filterFridgePartnerParentshipsByGuardianship(it.headOfChildId, fridgePartnerParentships)

        generateFamilyCompositions(
            maxOf(dateRange.start, it.startDate),
            it.headOfChild.id,
            fridgePartners,
            fridgeChildren,
            fridgePartnerParentships,
            fridgePartnerParentshipsWithGuardianship
        )
    }
}

private fun Database.Read.findFamiliesByAdult(personId: UUID, from: LocalDate): List<FridgeFamily> {
    val possibleHeadsOfFamily = getPartnersForPerson(personId, includeConflicts = false, period = DateRange(from, null))
        .map { it.person.id }
        .distinct() + personId

    return possibleHeadsOfFamily.flatMap { findFamiliesByHeadOfFamily(it, from) }
}

private fun Database.Read.findFamiliesByHeadOfFamily(headOfFamilyId: UUID, from: LocalDate): List<FridgeFamily> {
    val dateRange = DateRange(from, null)
    val childRelations = getParentships(headOfFamilyId, null, includeConflicts = false, period = dateRange)
    val partners = getPartnersForPerson(headOfFamilyId, includeConflicts = false, period = dateRange)
    val fridgePartnerParentships = partners.flatMap { getParentships(it.person.id, null, false, dateRange) }
    val fridgePartnerParentshipsWithGuardianship =
        filterFridgePartnerParentshipsByGuardianship(headOfFamilyId, fridgePartnerParentships)

    return generateFamilyCompositions(
        from,
        headOfFamilyId,
        partners,
        childRelations,
        fridgePartnerParentships,
        fridgePartnerParentshipsWithGuardianship
    )
}

private fun Database.Read.filterFridgePartnerParentshipsByGuardianship(
    headOfFamily: UUID,
    fridgePartnerParentships: Iterable<Parentship>
): List<Parentship> = fridgePartnerParentships.filter {
    getChildGuardians(it.child.id).contains(headOfFamily)
}

private fun generateFamilyCompositions(
    from: LocalDate,
    headOfFamily: UUID,
    partners: Iterable<Partner>,
    parentships: Iterable<Parentship>,
    fridgePartnerParentships: Iterable<Parentship>,
    fridgePartnerParentshipsWithGuardianship: Iterable<Parentship>
): List<FridgeFamily> {
    val periodsWhenChildrenAreNotAdults = parentships.map {
        val birthday = it.child.dateOfBirth
        DateRange(birthday, birthday.plusYears(18))
    }

    val allPeriods = partners.map { DateRange(it.startDate, it.endDate) } +
        parentships.map { DateRange(it.startDate, it.endDate) } +
        fridgePartnerParentships.map { DateRange(it.startDate, it.endDate) } +
        periodsWhenChildrenAreNotAdults

    val familyPeriods = asDistinctPeriods(allPeriods, DateRange(from, null))
        .map { period ->
            val partner = partners.find { DateRange(it.startDate, it.endDate).contains(period) }?.person
            val children = parentships
                .filter { DateRange(it.startDate, it.endDate).contains(period) }
                // Do not include children that are over 18 years old during the period
                .filter { it.child.dateOfBirth.plusYears(18) >= period.start }
                .map { it.child }
            val fridgePartnerChildren = fridgePartnerParentships
                .filter { it.headOfChild == partner }
                .filter { DateRange(it.startDate, it.endDate).contains(period) }
                // Do not include children that are over 18 years old during the period
                .filter { it.child.dateOfBirth.plusYears(18) >= period.start }
                .map { it.child }
            period to Quadruple(
                PersonData.JustId(headOfFamily),
                partner?.let { PersonData.JustId(it.id) },
                children.map { PersonData.WithDateOfBirth(it.id, it.dateOfBirth) },
                fridgePartnerChildren.map { PersonData.WithDateOfBirth(it.id, it.dateOfBirth) }
            )
        }

    return mergePeriods(familyPeriods).map { (period, familyData) ->
        val (children, fridgeChildren) = if (familyData.second?.id != null) {
            getChildrenAndFridgeChildren(headOfFamily, familyData.second.id, familyData.third, familyData.fourth, fridgePartnerParentshipsWithGuardianship)
        } else {
            Pair(familyData.third, listOf())
        }

        FridgeFamily(
            headOfFamily = familyData.first,
            partner = familyData.second,
            children = children,
            period = period,
            fridgeSiblings = fridgeChildren,
        )
    }
}

fun getChildrenAndFridgeChildren(
    headOfFamily: UUID,
    fridgePartner: UUID,
    children: List<PersonData.WithDateOfBirth>,
    fridgePartnerChildren: List<PersonData.WithDateOfBirth>,
    fridgePartnerParentshipsWithGuardianship: Iterable<Parentship>
):
    Pair<List<PersonData.WithDateOfBirth>, List<PersonData.WithDateOfBirth>> =
    if (fridgePartnerChildren.map { it.id }.toSet() == fridgePartnerParentshipsWithGuardianship.map { it.child.id }.toSet()) {
        if (shouldReceiveFeeDecision(headOfFamily, fridgePartner, children, fridgePartnerChildren)) {
            Pair(children + fridgePartnerChildren, listOf())
        } else {
            Pair(listOf(), children + fridgePartnerChildren)
        }
    } else {
        Pair(children, fridgePartnerChildren)
    }

/*
 *  In case headOfFamily is a guardian of all fridgePartner's fridge children,
 *  only one of these two should receive fee decision. This function decides
 *  which one it is.
 */
fun shouldReceiveFeeDecision(
    headOfFamily: UUID,
    fridgePartner: UUID,
    children: List<PersonData.WithDateOfBirth>,
    fridgePartnerChildren: List<PersonData.WithDateOfBirth>
):
    Boolean =
    if (children.size != fridgePartnerChildren.size) {
        children.size > fridgePartnerChildren.size
    } else {
        val date1 = children.map { it.dateOfBirth }.minOrNull() ?: LocalDate.MAX
        val date2 = fridgePartnerChildren.map { it.dateOfBirth }.minOrNull() ?: LocalDate.MAX
        if (date1 != date2) {
            date1 < date2
        } else {
            headOfFamily > fridgePartner
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
