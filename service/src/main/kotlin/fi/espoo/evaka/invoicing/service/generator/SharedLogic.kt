// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service.generator

import fi.espoo.evaka.invoicing.domain.FinanceDecision
import fi.espoo.evaka.serviceneed.getServiceNeedOptions
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.periodsCanMerge
import java.time.LocalDate

/** New drafts fully before this date are ignored */
fun getSoftMinDate(clock: EvakaClock, retroactiveOverride: LocalDate?): LocalDate {
    val rollingMinDate = clock.today().minusMonths(15)
    return if (retroactiveOverride != null) minOf(retroactiveOverride, rollingMinDate)
    else rollingMinDate
}

/** New drafts starting before this date are set to start on this date */
fun getHardMinDate(financeMinDate: LocalDate, retroactiveOverride: LocalDate?): LocalDate {
    return if (retroactiveOverride != null) minOf(retroactiveOverride, financeMinDate)
    else financeMinDate
}

fun getPlacementDetailsByChild(
    tx: Database.Read,
    childIds: Set<PersonId>
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

            val serviceNeedVoucherValues =
                serviceNeedOptionVoucherValues[serviceNeedOption.id]?.firstOrNull {
                    it.range.contains(range)
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
                serviceNeedVoucherValues = serviceNeedVoucherValues
            )
        }
    }
}

/** Merges adjacent identical drafts and filters out unnecessary drafts */
fun <Decision : FinanceDecision<Decision>> filterAndMergeDrafts(
    newDrafts: List<Decision>,
    activeDecisions: List<Decision>,
    ignoredDrafts: List<Decision>,
    softMinDate: LocalDate,
    hardMinDate: LocalDate
): List<Decision> {
    if (newDrafts.isEmpty()) return emptyList()

    return newDrafts
        .filterNot { draft ->
            existsActiveDuplicateThatWillRemainEffective(draft, activeDecisions, newDrafts)
        }
        .filterNot { draft ->
            // drop empty drafts unless there exists an active decision that overlaps
            draft.isEmpty() && activeDecisions.none { it.validDuring.overlaps(draft.validDuring) }
        }
        .let(::mergeAdjacentIdenticalDrafts)
        .filterNot { draft ->
            existsActiveDuplicateThatWillRemainEffective(draft, activeDecisions, newDrafts)
        }
        .filter { draft -> draft.validDuring.overlaps(DateRange(softMinDate, null)) }
        .mapNotNull { draft ->
            draft.validDuring.intersection(DateRange(hardMinDate, null))?.let {
                draft.withValidity(it)
            }
        }
        .filter { newDraft ->
            !ignoredDrafts.any {
                it.validDuring == newDraft.validDuring && it.contentEquals(newDraft)
            }
        }
}

fun <Decision : FinanceDecision<Decision>> existsActiveDuplicateThatWillRemainEffective(
    draft: Decision,
    activeDecisions: List<Decision>,
    drafts: List<Decision>,
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

fun <Decision : FinanceDecision<Decision>> mergeAdjacentIdenticalDrafts(
    decisions: List<Decision>
): List<Decision> {
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

fun <Decision : FinanceDecision<Decision>, Difference> Decision.getDifferencesToPrevious(
    newDrafts: List<Decision>,
    existingActiveDecisions: List<Decision>,
    getDifferences: (decision1: Decision, decision2: Decision) -> Set<Difference>
): Set<Difference> {
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

/** If there exists identical old draft, copy created date from it */
fun <Decision : FinanceDecision<Decision>> Decision.withCreatedDateFromExisting(
    existingDrafts: List<Decision>
): Decision {
    val duplicateOldDraft =
        existingDrafts.find { oldDraft ->
            contentEquals(oldDraft) && validDuring == oldDraft.validDuring
        }

    return duplicateOldDraft?.let { withCreated(it.created) } ?: this
}
