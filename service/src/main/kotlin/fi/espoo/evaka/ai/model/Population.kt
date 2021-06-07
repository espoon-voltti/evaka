package fi.espoo.evaka.ai.model

import fi.espoo.evaka.ai.Parameters
import fi.espoo.evaka.ai.utils.pickRandomWithExpDistribution
import fi.espoo.evaka.ai.utils.randomGene
import java.util.UUID

class Population(
    val units: List<EvakaUnit>,
    val children: List<AppliedChild>
) {
    var generation = 0

    var members: List<Genome> = (0 until Parameters.populationSize).map {
        Genome(
            genes = children.indices.map { randomGene() }
        )
    }.toMutableList()

    init {
        members.forEach { it.calcCost(units, children) }
    }

    fun advance() {
        generation++

        val descendants = (0 until Parameters.pairsToMate).map {
            Pair(
                members.pickRandomWithExpDistribution(2.0),
                members.pickRandomWithExpDistribution(2.0)
            )
        }.flatMap { (g1, g2) ->
            (0 until Parameters.descendantsPerPair).map { g1.produceDescendant(g2, units, children) }
        }

        members = (members + descendants)
            .toSet()
            .sortedBy { it.cost }
            .slice(0 until Parameters.populationSize)
    }

    fun getMinimumCost() = members.minOf { it.cost }

    fun getBest() = members.minByOrNull { it.cost }!!.let { genome ->
        val resultUnits = units.map { unit ->
            val resultChildren = children
                .zip(genome.genes)
                .mapNotNull { (child, gene) ->
                    if (child.allPreferredUnits[gene].id == unit.id) child else null
                }
                .map { child ->
                    ResultChild(
                        id = child.id,
                        capacity = child.capacity,
                        firstPreference = child.originalPreferredUnits.first().name,
                        secondPreference = child.originalPreferredUnits.getOrNull(1)?.name,
                        thirdPreference = child.originalPreferredUnits.getOrNull(2)?.name,
                        distanceToFirstPreference = child.originalPreferredUnits.first().let { first ->
                            if (first.id == unit.id) 0.0 else first.nearbyUnits.find { (id) -> unit.id == id }!!.distance
                        },
                        originalPreferenceRank = child.originalPreferredUnits.indexOfFirst { it.id == unit.id }.takeIf { it >= 0 },
                        preferenceRank = child.allPreferredUnits.indexOfFirst { it.id == unit.id }
                    )
                }

            val usedCapacityAfter = unit.usedCapacity + resultChildren.sumOf({ it.capacity })

            ResultUnit(
                children = resultChildren,
                maxCapacity = unit.maxCapacity,
                usedCapacityBefore = unit.usedCapacity,
                usedCapacityAfter = usedCapacityAfter,
                capacityPercentage = 100 * usedCapacityAfter / unit.maxCapacity
            )
        }

        Result(resultUnits)
    }
}

data class Result(
    val units: List<ResultUnit>
) {
    val maxCapacityPercentage: Double
        get() = this.units.maxByOrNull { it.capacityPercentage }!!.capacityPercentage

    val minCapacityPercentage: Double
        get() = this.units.minByOrNull { it.capacityPercentage }!!.capacityPercentage

    val childrenInFirstPreferencePercentage: Double
        get() = this.units.flatMap { it.children }.let { 100.0 * it.filter { it.originalPreferenceRank == 0 }.size / it.size }

    val childrenInOneOfPreferencesPercentage: Double
        get() = this.units.flatMap { it.children }.let { 100.0 * it.filter { it.originalPreferenceRank != null }.size / it.size }

    val childrenInOneOfPreferencesWhen3GivenPercentage: Double
        get() = this.units.flatMap { it.children.filter { it.thirdPreference != null } }
            .let { 100.0 * it.filter { it.originalPreferenceRank != null }.size / it.size }
}

data class ResultUnit(
    val children: List<ResultChild>,
    val maxCapacity: Double,
    val usedCapacityBefore: Double,
    val usedCapacityAfter: Double,
    val capacityPercentage: Double
)

data class ResultChild(
    val id: UUID,
    val capacity: Double,
    val firstPreference: String,
    val secondPreference: String?,
    val thirdPreference: String?,
    val distanceToFirstPreference: Double,
    val originalPreferenceRank: Int?,
    val preferenceRank: Int
)
