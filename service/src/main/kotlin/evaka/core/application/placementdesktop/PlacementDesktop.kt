// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.application.placementdesktop

import evaka.core.occupancy.OccupancyResponse
import evaka.core.occupancy.OccupancyType
import evaka.core.occupancy.calculateDailyUnitOccupancyValues
import evaka.core.occupancy.getOccupancyResponse
import evaka.core.occupancy.reduceDailyOccupancyValues
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.actionrule.AccessControlFilter
import evaka.core.user.EvakaUser
import java.time.LocalDate
import org.jdbi.v3.json.Json

data class PlacementDesktopDaycare(
    val id: DaycareId,
    val name: String,
    val serviceWorkerNote: String,
    @Json val placementDrafts: List<PlacementDraft>,
    val occupancyConfirmed: OccupancyResponse?,
    val occupancyPlanned: OccupancyResponse?,
    val occupancyDraft: OccupancyResponse?,
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
    val occupancyResponses =
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
                            .let { reduceDailyOccupancyValues(it) }
                            .let { getOccupancyResponse(it) }
                }
        }
    return daycares.map { daycare ->
        daycare.copy(
            occupancyDraft = occupancyResponses[OccupancyType.DRAFT]?.get(daycare.id),
            occupancyPlanned = occupancyResponses[OccupancyType.PLANNED]?.get(daycare.id),
            occupancyConfirmed = occupancyResponses[OccupancyType.CONFIRMED]?.get(daycare.id),
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
