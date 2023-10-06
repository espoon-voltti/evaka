// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.DateRange
import java.time.LocalDate

interface FinanceDecision<Decision : FinanceDecision<Decision>> {
    val id: Id<*>
    val validFrom: LocalDate
    val validTo: LocalDate?
    val headOfFamilyId: PersonId
    val validDuring: DateRange

    fun withRandomId(): Decision

    fun withValidity(period: DateRange): Decision

    fun contentEquals(decision: Decision): Boolean

    fun overlapsWith(other: Decision): Boolean

    fun isAnnulled(): Boolean

    fun isEmpty(): Boolean

    fun annul(): Decision
}

fun <Decision : FinanceDecision<Decision>> decisionContentsAreEqual(
    decision1: Decision,
    decision2: Decision
): Boolean = decision1.contentEquals(decision2)

fun <Decision : FinanceDecision<Decision>> updateEndDatesOrAnnulConflictingDecisions(
    newDecisions: List<Decision>,
    conflicting: List<Decision>
): List<Decision> {
    val fixedConflicts =
        newDecisions
            .sortedBy { it.validFrom }
            .fold(conflicting) { conflicts, newDecision ->
                val updatedConflicts =
                    conflicts
                        .filter { conflict -> conflict.overlapsWith(newDecision) }
                        .map { conflict ->
                            if (newDecision.validFrom <= conflict.validFrom) {
                                conflict.annul()
                            } else {
                                conflict.withValidity(
                                    DateRange(
                                        conflict.validFrom,
                                        newDecision.validFrom.minusDays(1)
                                    )
                                )
                            }
                        }

                conflicts.map { conflict ->
                    updatedConflicts.find { it.id == conflict.id } ?: conflict
                }
            }

    val (annulledConflicts, nonAnnulledConflicts) = fixedConflicts.partition { it.isAnnulled() }
    val originalConflictsAnnulled =
        conflicting
            .filter { conflict -> annulledConflicts.any { it.id == conflict.id } }
            .map { it.annul() }

    return nonAnnulledConflicts + originalConflictsAnnulled
}

enum class FinanceDecisionType {
    FEE_DECISION,
    VOUCHER_VALUE_DECISION
}
