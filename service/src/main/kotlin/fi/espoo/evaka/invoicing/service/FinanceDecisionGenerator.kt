// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FinanceDecision
import fi.espoo.evaka.invoicing.domain.FridgeFamily
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.PersonBasic
import fi.espoo.evaka.invoicing.domain.decisionContentsAreEqual
import fi.espoo.evaka.invoicing.domain.getECHAIncrease
import fi.espoo.evaka.pis.determineHeadOfFamily
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPartnersForPerson
import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.Partner
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.orMax
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.stereotype.Component

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

private val logger = KotlinLogging.logger {}

@Component
class FinanceDecisionGenerator(
    private val jsonMapper: JsonMapper,
    private val incomeTypesProvider: IncomeTypesProvider,
    env: EvakaEnv,
    private val featureConfig: FeatureConfig
) {
    private val feeDecisionMinDate = env.feeDecisionMinDate
    final val v1FeeDecisions = env.feeDecisionGeneratorV1Enabled
    final val v2FeeDecisions = env.feeDecisionGeneratorV2Enabled
    final val v2Vouchers = env.voucherValueDecisionGeneratorV2Enabled

    fun scheduleBatchGeneration(tx: Database.Transaction, clock: EvakaClock) {
        if (!v2FeeDecisions) return

        val inserted =
            tx.createUpdate(
                    """
WITH ids AS (
    SELECT head_of_child AS head_of_family_id
    FROM fridge_child

    UNION

    SELECT head_of_family_id
    FROM fee_decision
)
INSERT INTO async_job(type, payload, retry_count, retry_interval)
SELECT 'GenerateFinanceDecisions',
       jsonb_build_object(
               'user', NULL,
               'person', jsonb_build_object(
                       'adultId', head_of_family_id,
                       'skipPropagation', true
                   ),
               'dateRange', jsonb_build_object(
                       'start', :from,
                       'end', NULL
                   )
           ),
       3,
       interval '5 minutes'
FROM ids;
        """
                )
                .bind("from", clock.today().minusMonths(15))
                .execute()

        logger.info { "Scheduled GenerateFinanceDecisions for $inserted people" }
    }

    fun createRetroactiveFeeDecisions(
        tx: Database.Transaction,
        clock: EvakaClock,
        headOfFamily: PersonId,
        from: LocalDate
    ) {
        if (v1FeeDecisions) {
            val families =
                tx.findFamiliesByHeadOfFamily(headOfFamily, from).filter {
                    it.headOfFamily == headOfFamily
                }
            tx.handleFeeDecisionChanges(
                clock,
                jsonMapper,
                incomeTypesProvider,
                from, // intentionally does not care about feeDecisionMinDate
                headOfFamily,
                families
            )
        }

        if (v2FeeDecisions) {
            generateAndInsertFeeDecisionsV2(
                tx = tx,
                clock = clock,
                jsonMapper = jsonMapper,
                incomeTypesProvider = incomeTypesProvider,
                financeMinDate = feeDecisionMinDate,
                headOfFamilyId = headOfFamily,
                retroactiveFrom = from
            )
        }
    }

    fun createRetroactiveValueDecisions(
        tx: Database.Transaction,
        clock: EvakaClock,
        headOfFamily: PersonId,
        from: LocalDate
    ) {
        if (v2Vouchers) {
            tx.getChildrenOfHeadOfFamily(headOfFamily, DateRange(from, null)).forEach { childId ->
                generateAndInsertVoucherValueDecisionsV2(
                    tx = tx,
                    clock = clock,
                    jsonMapper = jsonMapper,
                    incomeTypesProvider = incomeTypesProvider,
                    financeMinDate = feeDecisionMinDate,
                    childId = childId,
                    retroactiveFrom = from
                )
            }
        } else {
            val families =
                tx.findFamiliesByHeadOfFamily(headOfFamily, from).filter {
                    it.headOfFamily == headOfFamily
                }
            families
                .flatMap { family -> family.children.map { it to family } }
                .groupingBy { (child, _) -> child }
                .fold(listOf<FridgeFamily>()) { childFamilies, (_, family) ->
                    childFamilies + family
                }
                .forEach { (child, families) ->
                    tx.handleValueDecisionChanges(
                        featureConfig,
                        jsonMapper,
                        incomeTypesProvider,
                        clock,
                        from,
                        child,
                        families
                    )
                }
        }
    }

    fun generateNewDecisionsForAdult(
        tx: Database.Transaction,
        clock: EvakaClock,
        personId: PersonId,
        from: LocalDate, // only used for v1
        skipPropagation: Boolean = false
    ) {
        if (v2FeeDecisions) {
            val adults =
                if (skipPropagation) setOf(personId)
                else getAllPossiblyAffectedAdultsByAdult(tx, personId)
            adults.forEach { adult ->
                generateAndInsertFeeDecisionsV2(
                    tx = tx,
                    clock = clock,
                    jsonMapper = jsonMapper,
                    incomeTypesProvider = incomeTypesProvider,
                    financeMinDate = feeDecisionMinDate,
                    headOfFamilyId = adult
                )
            }
        }

        if (v2Vouchers) {
            tx.getChildrenOfHeadOfFamily(personId).forEach { childId ->
                generateAndInsertVoucherValueDecisionsV2(
                    tx = tx,
                    clock = clock,
                    jsonMapper = jsonMapper,
                    incomeTypesProvider = incomeTypesProvider,
                    financeMinDate = feeDecisionMinDate,
                    childId = childId
                )
            }
        } else {
            val fromOrMinDate = maxOf(feeDecisionMinDate, from)
            val families = tx.findFamiliesByAdult(personId, fromOrMinDate)
            handleDecisionChangesForFamilies(tx, clock, fromOrMinDate, families)
        }
    }

    fun generateNewDecisionsForChild(
        tx: Database.Transaction,
        clock: EvakaClock,
        childId: ChildId,
        from: LocalDate
    ) {
        if (v2FeeDecisions) {
            getAllPossiblyAffectedAdultsByChild(tx, childId).forEach { adultId ->
                generateAndInsertFeeDecisionsV2(
                    tx = tx,
                    clock = clock,
                    jsonMapper = jsonMapper,
                    incomeTypesProvider = incomeTypesProvider,
                    financeMinDate = feeDecisionMinDate,
                    headOfFamilyId = adultId
                )
            }
        }

        if (v2Vouchers) {
            generateAndInsertVoucherValueDecisionsV2(
                tx = tx,
                clock = clock,
                jsonMapper = jsonMapper,
                incomeTypesProvider = incomeTypesProvider,
                financeMinDate = feeDecisionMinDate,
                childId = childId
            )
        } else {
            val fromOrMinDate = maxOf(feeDecisionMinDate, from)
            val families = tx.findFamiliesByChild(childId, fromOrMinDate)
            handleDecisionChangesForFamilies(tx, clock, fromOrMinDate, families)
        }
    }

    private fun handleDecisionChangesForFamilies(
        tx: Database.Transaction,
        clock: EvakaClock,
        from: LocalDate,
        families: List<FridgeFamily>
    ) {
        if (v1FeeDecisions) {
            families
                .groupBy { it.headOfFamily }
                .forEach { (headOfFamily, families) ->
                    tx.handleFeeDecisionChanges(
                        clock,
                        jsonMapper,
                        incomeTypesProvider,
                        from,
                        headOfFamily,
                        families
                    )
                }
        }

        if (!v2Vouchers) {
            families
                .flatMap { family -> family.children.map { it to family } }
                .groupingBy { (child, _) -> child }
                .fold(listOf<FridgeFamily>()) { childFamilies, (_, family) ->
                    childFamilies + family
                }
                .forEach { (child, families) ->
                    tx.handleValueDecisionChanges(
                        featureConfig,
                        jsonMapper,
                        incomeTypesProvider,
                        clock,
                        from,
                        child,
                        families
                    )
                }
        }
    }
}

private fun Database.Read.findFamiliesByChild(
    childId: ChildId,
    from: LocalDate
): List<FridgeFamily> {
    val dateRange = DateRange(from, null)
    val parentRelations =
        getParentships(null, childId, includeConflicts = false, period = dateRange)

    return parentRelations.flatMap {
        val fridgePartners =
            mergeFridgePartnerPeriods(
                getPartnersForPerson(it.headOfChildId, includeConflicts = false, period = dateRange)
            )
        val fridgeChildren =
            getParentships(it.headOfChildId, null, includeConflicts = false, period = dateRange)
        val fridgePartnerParentships =
            fridgePartners
                .flatMap { partner -> getParentships(partner.person.id, null, false, dateRange) }
                .distinct()

        generateFamilyCompositions(
            maxOf(dateRange.start, it.startDate),
            it.headOfChild.id,
            fridgePartners,
            fridgeChildren,
            fridgePartnerParentships
        )
    }
}

private fun mergeFridgePartnerPeriods(
    fridgePartners: List<Partner>,
): List<Partner> {
    val partnerPeriods =
        fridgePartners.map { partner -> DateRange(partner.startDate, partner.endDate) to partner }
    val mergedPeriods =
        mergePeriods(
            partnerPeriods,
            equals = { partner1, partner2 -> partner1.person.id == partner2.person.id }
        )
    return mergedPeriods.map { (period, partner) ->
        partner.copy(startDate = period.start, endDate = period.end)
    }
}

private fun Database.Read.findFamiliesByAdult(
    personId: PersonId,
    from: LocalDate
): List<FridgeFamily> {
    val possibleHeadsOfFamily =
        getPartnersForPerson(personId, includeConflicts = false, period = DateRange(from, null))
            .map { it.person.id }
            .distinct() + personId

    return possibleHeadsOfFamily.flatMap { findFamiliesByHeadOfFamily(it, from) }
}

private fun Database.Read.findFamiliesByHeadOfFamily(
    headOfFamilyId: PersonId,
    from: LocalDate
): List<FridgeFamily> {
    val dateRange = DateRange(from, null)
    val childRelations =
        getParentships(headOfFamilyId, null, includeConflicts = false, period = dateRange)
    val partners =
        getPartnersForPerson(headOfFamilyId, includeConflicts = false, period = dateRange)
    val fridgePartnerParentships =
        partners.flatMap { getParentships(it.person.id, null, false, dateRange) }.distinct()

    return generateFamilyCompositions(
        from,
        headOfFamilyId,
        partners,
        childRelations,
        fridgePartnerParentships
    )
}

private fun generateFamilyCompositions(
    from: LocalDate,
    headOfFamily: PersonId,
    partners: Iterable<Partner>,
    parentships: Iterable<Parentship>,
    fridgePartnerParentships: Iterable<Parentship>
): List<FridgeFamily> {
    val periodsWhenChildrenAreNotAdults =
        parentships.map {
            val birthday = it.child.dateOfBirth
            DateRange(birthday, birthday.plusYears(18))
        }

    val allPeriods =
        partners.map { DateRange(it.startDate, it.endDate) } +
            parentships.map { DateRange(it.startDate, it.endDate) } +
            fridgePartnerParentships.map { DateRange(it.startDate, it.endDate) } +
            periodsWhenChildrenAreNotAdults

    val familyPeriods =
        asDistinctPeriods(allPeriods, DateRange(from, null)).map { period ->
            val partner =
                partners.find { DateRange(it.startDate, it.endDate).contains(period) }?.person
            val children =
                parentships
                    .filter { DateRange(it.startDate, it.endDate).contains(period) }
                    // Do not include children that are over 18 years old during the period
                    .filter { it.child.dateOfBirth.plusYears(18) >= period.start }
                    .map { it.child }
            val fridgePartnerChildren =
                fridgePartnerParentships
                    .filter { it.headOfChild == partner }
                    .filter { DateRange(it.startDate, it.endDate).contains(period) }
                    // Do not include children that are over 18 years old during the period
                    .filter { it.child.dateOfBirth.plusYears(18) >= period.start }
                    .map { it.child }
            period to
                Quadruple(
                    headOfFamily,
                    partner?.id,
                    children.map {
                        PersonBasic(
                            it.id,
                            it.dateOfBirth,
                            it.firstName,
                            it.lastName,
                            it.socialSecurityNumber
                        )
                    },
                    fridgePartnerChildren.map {
                        PersonBasic(
                            it.id,
                            it.dateOfBirth,
                            it.firstName,
                            it.lastName,
                            it.socialSecurityNumber
                        )
                    }
                )
        }

    return mergePeriods(familyPeriods).map { (period, familyData) ->
        val children = familyData.third + familyData.fourth
        val (head, partner) =
            determineHeadOfFamily(
                Pair(familyData.first, familyData.third),
                Pair(familyData.second, familyData.fourth)
            )
        FridgeFamily(headOfFamily = head, partner = partner, children = children, period = period)
    }
}

internal fun <Decision : FinanceDecision<Decision>> mergeAndFilterUnnecessaryDrafts(
    drafts: List<Decision>,
    active: List<Decision>
): List<Decision> {
    if (drafts.isEmpty()) return drafts

    val minDate =
        drafts.map { it.validFrom }.minOrNull()!! // min always exists when list is non-empty
    val maxDate = drafts.map { it.validTo }.maxByOrNull { orMax(it) }

    return asDistinctPeriods(
            (drafts + active).map { DateRange(it.validFrom, it.validTo) },
            DateRange(minDate, maxDate)
        )
        .fold(listOf<Decision>()) { decisions, period ->
            val draft =
                drafts
                    .find { DateRange(it.validFrom, it.validTo).contains(period) }
                    // Because of asDistinctPeriods(), the draft's validity must be same or larger
                    // than the period, i.e. we only ever change draft's validity to be smaller
                    ?.withValidity(DateRange(period.start, period.end))
            val decision = active.find { DateRange(it.validFrom, it.validTo).contains(period) }
            if (
                draft == null ||
                    draftIsUnnecessary(
                        draft,
                        decision,
                        alreadyGeneratedDrafts = decisions.isNotEmpty()
                    )
            ) {
                decisions
            } else {
                decisions + draft
            }
        }
        .let { mergeDecisions(it) }
}

/*
 * a draft is unnecessary when:
 *   - the draft is "empty" and there exists no sent decision that should be overridden,
 *   - or the draft is "bit-by-bit identical" to an existing sent decision,
 *   - or the draft is "content-wise" identical to an existing sent decision and no drafts have been generated before this draft
 */
internal fun <Decision : FinanceDecision<Decision>> draftIsUnnecessary(
    draft: Decision,
    sent: Decision?,
    alreadyGeneratedDrafts: Boolean
): Boolean {
    return (draft.isEmpty() && sent == null) ||
        (sent != null &&
            draft.contentEquals(sent) &&
            draft.validFrom == sent.validFrom &&
            draft.validTo == draft.validTo) ||
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

data class UpdatedExistingDecisions<Decision : FinanceDecision<Decision>>(
    val updatedDrafts: List<Decision>,
    val updatedActiveDecisions: List<Decision>
)

internal fun <Decision : FinanceDecision<Decision>> updateExistingDecisions(
    now: HelsinkiDateTime,
    from: LocalDate,
    newDrafts: List<Decision>,
    existingDrafts: List<Decision>,
    activeDecisions: List<Decision>
): UpdatedExistingDecisions<Decision> {
    val draftsWithUpdatedDates =
        filterOrUpdateStaleDrafts(existingDrafts, DateRange(from, null)).map { it.withRandomId() }

    val (withUpdatedEndDates, mergedDrafts) =
        updateDecisionEndDatesAndMergeDrafts(
            now,
            activeDecisions,
            newDrafts + draftsWithUpdatedDates
        )

    return UpdatedExistingDecisions(
        updatedDrafts = mergedDrafts,
        updatedActiveDecisions = withUpdatedEndDates
    )
}

internal fun <Decision : FinanceDecision<Decision>> filterOrUpdateStaleDrafts(
    drafts: List<Decision>,
    period: DateRange
): List<Decision> {
    val (overlappingDrafts, nonOverlappingDrafts) =
        drafts.partition { DateRange(it.validFrom, it.validTo).overlaps(period) }

    val updatedOverlappingDrafts =
        when (period.end) {
            null ->
                overlappingDrafts.flatMap {
                    when {
                        it.validFrom < period.start ->
                            listOf(
                                it.withValidity(DateRange(it.validFrom, period.start.minusDays(1)))
                            )
                        else -> emptyList()
                    }
                }
            else ->
                overlappingDrafts.flatMap {
                    when {
                        it.validFrom < period.start && orMax(it.validTo) > orMax(period.end) ->
                            listOf(
                                it.withValidity(DateRange(it.validFrom, period.start.minusDays(1))),
                                it.withValidity(DateRange(period.end.plusDays(1), it.validTo))
                            )
                        it.validFrom < period.start && orMax(it.validTo) <= orMax(period.end) ->
                            listOf(
                                it.withValidity(DateRange(it.validFrom, period.start.minusDays(1)))
                            )
                        it.validFrom >= period.start && orMax(it.validTo) > orMax(period.end) ->
                            listOf(it.withValidity(DateRange(period.end.plusDays(1), it.validTo)))
                        else -> emptyList()
                    }
                }
        }

    return nonOverlappingDrafts + updatedOverlappingDrafts
}

internal fun <Decision : FinanceDecision<Decision>> updateDecisionEndDatesAndMergeDrafts(
    now: HelsinkiDateTime,
    actives: List<Decision>,
    drafts: List<Decision>
): Pair<List<Decision>, List<Decision>> {
    val mergedDrafts = mergeDecisions(drafts)

    /*
     * Immediately update the validity end dates for active decisions if a new draft has the same contents and they
     * both are valid to the future
     */
    val (updatedActiveDecisions, unnecessaryDrafts) =
        actives
            .mapNotNull { decision ->
                val firstOverlappingSimilarDraft =
                    mergedDrafts
                        .filter { draft -> decision.validFrom == draft.validFrom }
                        .firstOrNull { draft -> decision.contentEquals(draft) }

                firstOverlappingSimilarDraft?.let { similarDraft ->
                    if (
                        orMax(decision.validTo) >= now.toLocalDate() &&
                            orMax(similarDraft.validTo) >= now.toLocalDate()
                    ) {
                        decision.withValidity(
                            DateRange(decision.validFrom, similarDraft.validTo)
                        ) to similarDraft.id
                    } else {
                        null
                    }
                }
            }
            .unzip()

    val allActiveDecisions =
        actives.map { decision -> updatedActiveDecisions.find { it.id == decision.id } ?: decision }
    val keptDrafts = mergedDrafts.filterNot { draft -> unnecessaryDrafts.contains(draft.id) }
    val filteredDrafts = mergeAndFilterUnnecessaryDrafts(keptDrafts, allActiveDecisions)

    return Pair(updatedActiveDecisions, filteredDrafts)
}

internal fun addECHAFeeAlterations(
    children: Set<ChildId>,
    incomes: List<Income>
): List<FeeAlteration> {
    return incomes
        .filter { it.worksAtECHA }
        .flatMap { income ->
            children.map { childId ->
                getECHAIncrease(childId, DateRange(income.validFrom, income.validTo))
            }
        }
}

internal fun getAllPossiblyAffectedAdultsByAdult(
    tx: Database.Read,
    adultId: PersonId
): Set<PersonId> {
    val children =
        tx.getParentships(headOfChildId = adultId, childId = null).map { it.childId }.toSet() +
            tx.getChildrenFromFeeDecisions(adultId)

    val partners =
        tx.getPartnersForPerson(adultId, false).map { it.person.id } +
            tx.getPartnersFromFeeDecisions(adultId) +
            children.flatMap { child ->
                tx.getParentships(headOfChildId = null, childId = child).map { it.headOfChildId }
            }

    return (partners + adultId).toSet()
}

internal fun getAllPossiblyAffectedAdultsByChild(
    tx: Database.Read,
    childId: PersonId
): Set<PersonId> {
    val heads = tx.getParentships(headOfChildId = null, childId = childId).map { it.headOfChildId }
    val partners =
        heads.flatMap { head -> tx.getPartnersForPerson(head, false) }.map { it.person.id }
    val feeDecisionParents = tx.getParentsFromFeeDecisions(childId)
    return (heads + partners + feeDecisionParents).toSet()
}

private fun Database.Read.getPartnersFromFeeDecisions(personId: PersonId) =
    createQuery<Any> {
            sql(
                """
        SELECT partner_id FROM fee_decision WHERE head_of_family_id = ${bind(personId)} AND status NOT IN ('DRAFT', 'IGNORED') AND partner_id IS NOT NULL 
        UNION ALL 
        SELECT head_of_family_id FROM fee_decision WHERE partner_id = ${bind(personId)} AND status NOT IN ('DRAFT', 'IGNORED')
        """
            )
        }
        .toSet<PersonId>()

private fun Database.Read.getChildrenFromFeeDecisions(personId: PersonId) =
    createQuery<Any> {
            sql(
                """
        SELECT fdc.child_id 
        FROM fee_decision fd
        JOIN fee_decision_child fdc ON fd.id = fdc.fee_decision_id
        WHERE (fd.head_of_family_id = ${bind(personId)} OR fd.partner_id = ${bind(personId)}) AND fd.status NOT IN ('DRAFT', 'IGNORED')
        """
            )
        }
        .toSet<PersonId>()

private data class FeeDecisionParents(val headOfFamilyId: PersonId, val partnerId: PersonId?)

private fun Database.Read.getParentsFromFeeDecisions(personId: PersonId) =
    createQuery<Any> {
            sql(
                """
        SELECT fd.head_of_family_id, fd.partner_id
        FROM fee_decision fd
        JOIN fee_decision_child fdc ON fd.id = fdc.fee_decision_id
        WHERE fdc.child_id = ${bind(personId)} AND fd.status NOT IN ('DRAFT', 'IGNORED')
        """
            )
        }
        .mapTo<FeeDecisionParents>()
        .flatMap { listOfNotNull(it.headOfFamilyId, it.partnerId) }
        .toSet()

private fun Database.Read.getChildrenOfHeadOfFamily(personId: PersonId, range: DateRange? = null) =
    createQuery<Any> {
            sql(
                """
        SELECT child_id
        FROM fridge_child
        WHERE head_of_child = ${bind(personId)} AND NOT conflict 
        ${if (range != null) "AND daterange(start_date, end_date, '[]') && ${bind(range)}" else ""}
        """
            )
        }
        .mapTo<ChildId>()
        .toSet()
