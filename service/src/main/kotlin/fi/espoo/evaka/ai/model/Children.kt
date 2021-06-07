package fi.espoo.evaka.ai.model

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo
import java.lang.Error
import java.time.LocalDate
import java.util.UUID

data class AppliedChild(
    val id: UUID,
    val originalPreferredUnits: List<EvakaUnit>,
    val allPreferredUnits: List<EvakaUnit>,
    val capacity: Double
)

fun getChildren(tx: Database.Read, date: LocalDate, units: List<EvakaUnit>): List<AppliedChild> {
    data class ChildData(
        val id: UUID,
        val preferredUnits: List<UUID>,
        val connectedDaycare: Boolean
    )

    // language=sql
    val sql = """
        SELECT a.id, a.preferredunits, a.connecteddaycare
        FROM application_view a
        WHERE 
            status = 'WAITING_PLACEMENT'::application_status_type AND
            type = 'PRESCHOOL' AND
            :period @> preferredstartdate
    """.trimIndent()

    return tx.createQuery(sql)
        .bind("period", FiniteDateRange(date.minusDays(15), date.plusDays(15)))
        .mapTo<ChildData>()
        .list()
        .map { child ->
            val originalPreferredUnits = child.preferredUnits.mapNotNull { id -> units.find { it.id == id } }

            val allPreferredUnits = mutableListOf(*originalPreferredUnits.toTypedArray())
            while (allPreferredUnits.size < 5) {
                val extraPreference = originalPreferredUnits.first().nearbyUnits
                    .firstOrNull { unit ->
                        allPreferredUnits.none { it.id == unit.id } &&
                            originalPreferredUnits.any { it.language == unit.language } &&
                            unit.providerType != ProviderType.PRIVATE_SERVICE_VOUCHER &&
                            unit.providerType != ProviderType.PRIVATE
                    }
                    ?.let { (id) -> units.find { it.id == id }!! }
                    ?: throw Error("Not enough suitable alternative preferences found for child ${child.id}")

                allPreferredUnits.add(extraPreference)
            }

            AppliedChild(
                id = child.id,
                originalPreferredUnits = originalPreferredUnits,
                allPreferredUnits = allPreferredUnits,
                capacity = if (child.connectedDaycare) 1.0 else 0.5
            )
        }
}
