package fi.espoo.evaka.ai.model

import fi.espoo.evaka.ai.Parameters
import fi.espoo.evaka.ai.utils.random
import fi.espoo.evaka.ai.utils.randomGene
import kotlin.properties.Delegates

data class Genome(
    val genes: List<Int>
) {
    var cost by Delegates.notNull<Double>()

    init {
        if (genes.any { it < 0 || it > 4 }) throw Error("Invalid gene")
    }

    fun calcCost(units: List<EvakaUnit>, children: List<AppliedChild>) {
        cost = 0.0

        val unitCapacities = units.map { it.id to it.usedCapacity }.toMap().toMutableMap()

        genes.zip(children).forEach { (g, child) ->
            cost += g

            if (g > child.originalPreferredUnits.size - 1)
                cost += 2 * child.originalPreferredUnits.size

            val unitId = child.allPreferredUnits[g].id
            unitCapacities.compute(unitId) { _, capacity -> capacity!! + child.capacity }
        }

        val capacityPercentages = unitCapacities.values.zip(units.map { it.maxCapacity })
            .map { (used, max) -> 100 * used / max }

        cost += capacityPercentages
            .map { it -> (it - 100).coerceAtLeast(0.0) * 15 }
            .sum()
    }

    fun produceDescendant(other: Genome, units: List<EvakaUnit>, children: List<AppliedChild>): Genome {
        return this.genes.zip(other.genes)
            .map { (g1, g2) ->
                if (random.nextDouble() < Parameters.mutationRate) {
                    randomGene()
                } else {
                    if (random.nextDouble() < 0.5) g1 else g2
                }
            }
            .let { Genome(it) }
            .also { it.calcCost(units, children) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Genome

        if (genes != other.genes) return false

        return true
    }

    override fun hashCode(): Int {
        return genes.hashCode()
    }
}
