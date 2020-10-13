package fi.espoo.evaka.invoicing.domain

import java.util.UUID

interface FinanceDecision<P : FinanceDecisionPart> {
    val id: UUID
    val parts: List<P>
}

interface FinanceDecisionPart

fun <Part : FinanceDecisionPart, Decision : FinanceDecision<Part>, Decisions : Iterable<Decision>> mergeDecisions(
    decisions: Decisions,
    copy: (Decision, List<Part>) -> Decision
): List<Decision> {
    val map = mutableMapOf<UUID, Decision>()
    for (decision in decisions) {
        val id = decision.id
        if (map.containsKey(id)) {
            val existing = map.getValue(id)
            map[id] = copy(existing, existing.parts + decision.parts)
        } else {
            map[id] = decision
        }
    }
    return map.values.toList()
}
