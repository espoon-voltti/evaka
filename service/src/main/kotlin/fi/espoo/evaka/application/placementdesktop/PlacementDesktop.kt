// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.placementdesktop

import fi.espoo.evaka.occupancy.OccupancyType
import fi.espoo.evaka.occupancy.calculateDailyUnitOccupancyValues
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.user.EvakaUser
import java.time.LocalDate
import org.jdbi.v3.json.Json

data class PlacementDesktopDaycare(
    val id: DaycareId,
    val name: String,
    val serviceWorkerNote: String,
    @Json val placementDrafts: List<PlacementDraft>,
    val maxOccupancyConfirmed: Double? = null,
    val maxOccupancyPlanned: Double? = null,
    val maxOccupancyDraft: Double? = null,
)

data class PlacementDraft(
    val applicationId: ApplicationId,
    val unitId: DaycareId,
    val startDate: LocalDate,
    val childId: ChildId,
    val childName: String,
    val modifiedAt: HelsinkiDateTime,
    val modifiedBy: EvakaUser,
    val serviceWorkerNote: String,
)

fun getPlacementDesktopDaycaresWithOccupancies(
    tx: Database.Read,
    unitIds: Set<DaycareId>,
    occupancyPeriod: FiniteDateRange,
    today: LocalDate,
): List<PlacementDesktopDaycare> {
    val daycares = tx.getPlacementDesktopDaycaresWithoutOccupancies(unitIds)
    val maxOccupancies =
        listOf(OccupancyType.DRAFT, OccupancyType.PLANNED, OccupancyType.CONFIRMED).associateWith {
            type ->
            tx.calculateDailyUnitOccupancyValues(
                    today = today,
                    queryPeriod = occupancyPeriod,
                    type = type,
                    unitFilter = AccessControlFilter.PermitAll,
                    unitIds = unitIds,
                )
                .associate {
                    it.key.unitId to
                        it.occupancies
                            .filter { occupancyPeriod.includes(it.key) }
                            .values
                            .mapNotNull { it.percentage }
                            .maxOrNull()
                }
        }
    return daycares.map { daycare ->
        daycare.copy(
            maxOccupancyDraft = maxOccupancies[OccupancyType.DRAFT]?.get(daycare.id),
            maxOccupancyPlanned = maxOccupancies[OccupancyType.PLANNED]?.get(daycare.id),
            maxOccupancyConfirmed = maxOccupancies[OccupancyType.CONFIRMED]?.get(daycare.id),
        )
    }
}

fun getPlacementDesktopDaycareWithOccupancies(
    tx: Database.Read,
    unitId: DaycareId,
    occupancyPeriod: FiniteDateRange,
    today: LocalDate,
): PlacementDesktopDaycare =
    getPlacementDesktopDaycaresWithOccupancies(tx, setOf(unitId), occupancyPeriod, today)
        .firstOrNull() ?: throw NotFound("Daycare $unitId not found")
