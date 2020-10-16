package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.shared.domain.Period
import java.time.LocalDate
import java.util.UUID

interface FinanceDecision<Part : FinanceDecisionPart, Decision : FinanceDecision<Part, Decision>> {
    val id: UUID
    val parts: List<Part>
    val validFrom: LocalDate
    val validTo: LocalDate?

    fun withRandomId(): Decision
    fun withValidity(period: Period): Decision
    fun contentEquals(decision: Decision): Boolean
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
