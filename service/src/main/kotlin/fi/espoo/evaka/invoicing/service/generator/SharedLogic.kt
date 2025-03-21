// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service.generator

import fi.espoo.evaka.invoicing.domain.FinanceDecision
import fi.espoo.evaka.serviceneed.getServiceNeedOptions
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.periodsCanMerge
import java.time.LocalDate

fun getPlacementDetailsByChild(
    tx: Database.Read,
    childIds: Set<PersonId>,
): Map<PersonId, List<PlacementDetails>> {
    if (childIds.isEmpty()) return emptyMap()

    val placements = tx.getPlacementRangesByChild(childIds)
    val serviceNeeds = tx.getServiceNeedRangesByChild(childIds)
    val serviceNeedOptions = tx.getServiceNeedOptions()
    val serviceNeedOptionVoucherValues = tx.getVoucherValuesByServiceNeedOption()

    val dateRanges =
        buildFiniteDateRanges(
            *placements.flatMap { it.value }.toTypedArray(),
            *serviceNeeds.flatMap { it.value }.toTypedArray(),
            *serviceNeedOptionVoucherValues
                .flatMap { it.value.map { it.voucherValues } }
                .toTypedArray(),
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
                    } ?: throw Error("Missing default service need option for ${placement.type}")
                }

            val serviceNeedVoucherValues =
                serviceNeedOptionVoucherValues[serviceNeedOption.id]?.firstOrNull {
                    it.voucherValues.range.contains(range)
                }

            PlacementDetails(
                childId = childId,
                finiteRange = range,
                placementType = placement.type,
                unitId = placement.unitId,
                providerType = placement.unitProviderType,
                invoicedUnit = placement.invoicedUnit,
                hasServiceNeed = serviceNeed != null,
                serviceNeedOption = serviceNeedOption,
                serviceNeedVoucherValues = serviceNeedVoucherValues?.voucherValues,
            )
        }
    }
}

/** Merges adjacent identical drafts and filters out unnecessary drafts */
fun <Decision : FinanceDecision<Decision>> filterAndMergeDrafts(
    newDrafts: List<Decision>,
    activeDecisions: List<Decision>,
    ignoredDrafts: List<Decision>,
    minDate: LocalDate,
    nrOfDaysDecisionCanBeSentInAdvance: Long,
): List<Decision> {
    if (newDrafts.isEmpty()) return emptyList()

    return newDrafts
        .filterNot { draft ->
            existsActiveDuplicateThatWillRemainEffective(
                draft,
                activeDecisions,
                newDrafts,
                nrOfDaysDecisionCanBeSentInAdvance,
            )
        }
        .filterNot { draft ->
            // drop empty drafts unless there exists an active decision that overlaps
            draft.isEmpty() && activeDecisions.none { it.validDuring.overlaps(draft.validDuring) }
        }
        .let { mergeAdjacentIdenticalDrafts(it, nrOfDaysDecisionCanBeSentInAdvance) }
        .filterNot { draft ->
            existsActiveDuplicateThatWillRemainEffective(
                draft,
                activeDecisions,
                newDrafts,
                nrOfDaysDecisionCanBeSentInAdvance,
            )
        }
        .mapNotNull { draft ->
            draft.validDuring.intersection(DateRange(minDate, null))?.let { draft.withValidity(it) }
        }
        .filter { newDraft ->
            !ignoredDrafts.any {
                it.validDuring == newDraft.validDuring &&
                    it.contentEquals(newDraft, nrOfDaysDecisionCanBeSentInAdvance)
            }
        }
}

fun <Decision : FinanceDecision<Decision>> existsActiveDuplicateThatWillRemainEffective(
    draft: Decision,
    activeDecisions: List<Decision>,
    drafts: List<Decision>,
    nrOfDaysDecisionCanBeSentInAdvance: Long,
): Boolean {
    val activeDuplicate =
        activeDecisions.find {
            it.validDuring.contains(draft.validDuring) &&
                it.contentEquals(draft, nrOfDaysDecisionCanBeSentInAdvance)
        } ?: return false

    val nonIdenticalDraftsOverlappingActive =
        drafts.filter {
            it.validDuring.overlaps(activeDuplicate.validDuring) &&
                !it.contentEquals(activeDuplicate, nrOfDaysDecisionCanBeSentInAdvance)
        }

    val activeValidUntil =
        nonIdenticalDraftsOverlappingActive.minOfOrNull { it.validFrom.minusDays(1) }
            ?: activeDuplicate.validTo

    if (activeValidUntil < activeDuplicate.validFrom) {
        return false // active decision will be annulled
    }

    val newActiveRange = DateRange(activeDuplicate.validFrom, activeValidUntil)

    return newActiveRange.contains(draft.validDuring)
}

fun <Decision : FinanceDecision<Decision>> mergeAdjacentIdenticalDrafts(
    decisions: List<Decision>,
    nrOfDaysDecisionCanBeSentInAdvance: Long,
): List<Decision> {
    return decisions
        .sortedBy { it.validFrom }
        .fold(emptyList()) { acc, next ->
            val prev = acc.lastOrNull()
            if (
                prev != null &&
                    periodsCanMerge(prev.validDuring, next.validDuring) &&
                    prev.contentEquals(next, nrOfDaysDecisionCanBeSentInAdvance)
            ) {
                acc.dropLast(1) + prev.withValidity(FiniteDateRange(prev.validFrom, next.validTo))
            } else {
                acc + next
            }
        }
}

fun <Decision : FinanceDecision<Decision>, Difference> Decision.getDifferencesToPrevious(
    newDrafts: List<Decision>,
    existingActiveDecisions: List<Decision>,
    getDifferences:
        (decision1: Decision, decision2: Decision, nrOfDaysDecisionCanBeSentInAdvance: Long) -> Set<
                Difference
            >,
    nrOfDaysDecisionCanBeSentInAdvance: Long,
): Set<Difference> {
    if (this.isEmpty()) {
        return emptySet()
    }

    val draftDifferences =
        newDrafts
            .filter { other ->
                !other.isEmpty() && periodsCanMerge(other.validDuring, this.validDuring)
            }
            .flatMap { other -> getDifferences(other, this, nrOfDaysDecisionCanBeSentInAdvance) }

    if (draftDifferences.isNotEmpty()) {
        return draftDifferences.toSet()
    }

    val activeDifferences =
        existingActiveDecisions
            .filter { active -> periodsCanMerge(active.validDuring, this.validDuring) }
            .flatMap { active -> getDifferences(active, this, nrOfDaysDecisionCanBeSentInAdvance) }

    return activeDifferences.toSet()
}

/** If there exists identical old draft, copy id and created date from it */
fun <Decision : FinanceDecision<Decision>> Decision.withMetadataFromExisting(
    existingDrafts: List<Decision>,
    nrOfDaysDecisionCanBeSentInAdvance: Long,
): Decision {
    val duplicateOldDraft =
        existingDrafts.find { oldDraft ->
            contentEquals(oldDraft, nrOfDaysDecisionCanBeSentInAdvance) &&
                validDuring == oldDraft.validDuring
        }

    return duplicateOldDraft?.let { this.withId(it.id.raw).withCreated(it.created) } ?: this
}
