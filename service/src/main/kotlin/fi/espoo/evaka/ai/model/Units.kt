package fi.espoo.evaka.ai.model

import fi.espoo.evaka.ai.calcDistance
import fi.espoo.evaka.ai.utils.random
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getAllApplicableUnits
import fi.espoo.evaka.occupancy.OccupancyType
import fi.espoo.evaka.occupancy.calculateOccupancyPeriods
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.UUID

data class EvakaUnit(
    val id: UUID,
    val name: String,
    val providerType: ProviderType,
    val language: Language,
    val nearbyUnits: List<NearbyUnit>,
    val maxCapacity: Double,
    val usedCapacity: Double
)

data class NearbyUnit(
    val id: UUID,
    val distance: Double,
    val providerType: ProviderType,
    val language: Language
)

fun getUnitData(tx: Database.Read, date: LocalDate): List<EvakaUnit> {
    val units = tx.getAllApplicableUnits(ApplicationType.PRESCHOOL).filter { it.location != null }
    val distances = mutableMapOf<UUID, List<Pair<UUID, Double>>>()
    units.forEachIndexed { i, u1 ->
        units.forEachIndexed { j, u2 ->
            if (j > i) {
                val distance = calcDistance(u1.location!!, u2.location!!)
                distances.merge(
                    u1.id,
                    listOf(Pair(u2.id, distance))
                ) { oldList, newList -> oldList + newList }
                distances.merge(
                    u2.id,
                    listOf(Pair(u1.id, distance))
                ) { oldList, newList -> oldList + newList }
            }
        }
    }

    return units.mapNotNull { unit ->
        // todo: this should exclude old placements of children with application
        val occupancy = tx.calculateOccupancyPeriods(
            unitId = unit.id,
            period = FiniteDateRange(date, date.plusDays(1)),
            type = OccupancyType.PLANNED
        ).first()

        val maxCapacity = occupancy.caretakers?.times(7) ?: throw Error("unit ${unit.id} missing caretaker info")

        // todo: use real occupancy when there is test data for placements
        val percentage = random.nextDouble() * 0.3 + 0.7

        EvakaUnit(
            id = unit.id,
            name = unit.name,
            providerType = unit.providerType,
            language = unit.language,
            nearbyUnits = distances[unit.id]!!
                .sortedBy { it.second }
                .map { (id, distance) ->
                    units.find { it.id == id }!!.let { otherUnit ->
                        NearbyUnit(
                            id = id,
                            distance = distance,
                            providerType = otherUnit.providerType,
                            language = otherUnit.language
                        )
                    }
                },
            maxCapacity = maxCapacity,
            usedCapacity = percentage * maxCapacity
        )
    }
}
