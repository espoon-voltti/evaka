// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.shared.domain.Period
import java.time.LocalDate
import java.util.UUID

interface FinanceDecision<Part : FinanceDecisionPart, Decision : FinanceDecision<Part, Decision>> {
    val id: UUID
    val parts: List<Part>
    val validFrom: LocalDate
    val validTo: LocalDate?
    val headOfFamily: PersonData.JustId

    fun withRandomId(): Decision
    fun withValidity(period: Period): Decision
    fun contentEquals(decision: Decision): Boolean
    fun isAnnulled(): Boolean
    fun annul(): Decision
}

fun <Part : FinanceDecisionPart, Decision : FinanceDecision<Part, Decision>> decisionContentsAreEqual(
    decision1: Decision,
    decision2: Decision
): Boolean = decision1.contentEquals(decision2)

interface FinanceDecisionPart

interface MergeableDecision<Part : FinanceDecisionPart, Decision : MergeableDecision<Part, Decision>> {
    val id: UUID
    val parts: List<Part>

    fun withParts(parts: List<Part>): Decision
}

fun <Part : FinanceDecisionPart, Decision : MergeableDecision<Part, Decision>, Decisions : Iterable<Decision>> Decisions.merge(): List<Decision> {
    val map = mutableMapOf<UUID, Decision>()
    for (decision in this) {
        val id = decision.id
        if (map.containsKey(id)) {
            val existing = map.getValue(id)
            map[id] = existing.withParts(existing.parts + decision.parts)
        } else {
            map[id] = decision
        }
    }
    return map.values.toList()
}

fun <Part : FinanceDecisionPart, Decision : FinanceDecision<Part, Decision>> updateEndDatesOrAnnulConflictingDecisions(
    newDecisions: List<Decision>,
    conflicting: List<Decision>
): List<Decision> {
    val fixedConflicts = newDecisions
        .sortedBy { it.validFrom }
        .fold(conflicting) { conflicts, newDecision ->
            val updatedConflicts = conflicts
                .filter { conflict ->
                    conflict.headOfFamily.id == newDecision.headOfFamily.id && Period(
                        conflict.validFrom,
                        conflict.validTo
                    ).overlaps(Period(newDecision.validFrom, newDecision.validTo))
                }
                .map { conflict ->
                    if (newDecision.validFrom <= conflict.validFrom) {
                        conflict.annul()
                    } else {
                        conflict.withValidity(Period(conflict.validFrom, newDecision.validFrom.minusDays(1)))
                    }
                }

            conflicts.map { conflict -> updatedConflicts.find { it.id == conflict.id } ?: conflict }
        }

    val (annulledConflicts, nonAnnulledConflicts) = fixedConflicts.partition { it.isAnnulled() }
    val originalConflictsAnnulled = conflicting
        .filter { conflict -> annulledConflicts.any { it.id == conflict.id } }
        .map { it.annul() }

    return nonAnnulledConflicts + originalConflictsAnnulled
}
