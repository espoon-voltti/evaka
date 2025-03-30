package fi.espoo.evaka.placementdesktop

import fi.espoo.evaka.application.mapRequestedPlacementType
import fi.espoo.evaka.occupancy.OccupancyType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reports.calculateUnitOccupancyReport
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/placement-desktop")
class PlacementDesktopController {
    @GetMapping("/daycares")
    fun getPlacementDaycares(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam occupancyDate: LocalDate,
    ): List<PlacementDaycare> {
        return db.connect { dbc ->
            dbc.read { tx ->
                val daycares =
                    tx.createQuery {
                            sql(
                                """
                SELECT 
                    d.id,
                    d.name,
                    ca.name AS care_area_name,
                    d.service_worker_note,
                    d.capacity,
                    0 as child_occupancy_confirmed, -- filled in later
                    0 as child_occupancy_planned -- filled in later
                FROM daycare d 
                JOIN care_area ca ON d.care_area_id = ca.id
            """
                            )
                        }
                        .mapTo<PlacementDaycare>()
                        .toList()

                val confirmedOccupancies =
                    tx.calculateUnitOccupancyReport(
                            today = clock.today(),
                            areaId = null,
                            providerType = null,
                            unitTypes = null,
                            queryPeriod = occupancyDate.toFiniteDateRange(),
                            type = OccupancyType.CONFIRMED,
                            unitFilter = AccessControlFilter.PermitAll,
                        )
                        .associateBy(
                            keySelector = { it.unitId },
                            valueTransform = { it.occupancies[occupancyDate] },
                        )

                val plannedOccupancies =
                    tx.calculateUnitOccupancyReport(
                            today = clock.today(),
                            areaId = null,
                            providerType = null,
                            unitTypes = null,
                            queryPeriod = occupancyDate.toFiniteDateRange(),
                            type = OccupancyType.PLANNED,
                            unitFilter = AccessControlFilter.PermitAll,
                        )
                        .associateBy(
                            keySelector = { it.unitId },
                            valueTransform = { it.occupancies[occupancyDate] },
                        )

                daycares.map {
                    it.copy(
                        childOccupancyConfirmed = confirmedOccupancies[it.id]?.sum ?: 0.0,
                        childOccupancyPlanned = plannedOccupancies[it.id]?.sum ?: 0.0,
                    )
                }
            }
        }
    }

    @GetMapping("/applications")
    fun getPlacementApplications(
        db: Database,
        user: AuthenticatedUser.Employee,
    ): List<PlacementApplication> {
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.createQuery {
                        sql(
                            """
                SELECT 
                    a.id,
                    ch.first_name,
                    ch.last_name,
                    ch.date_of_birth,
                    pu.preferredUnits,
                    a.document ->> 'serviceStart' AS service_start,
                    a.document ->> 'serviceEnd' AS service_end,
                    CASE 
                        WHEN a.document -> 'careDetails' ->> 'assistanceNeeded' IS NOT NULL 
                        THEN a.document -> 'careDetails' ->> 'assistanceDescription'
                    END AS assistance_need,
                    pp.unit_id AS planned_placement_unit,
                    a.trial_placement_unit,
                    a.document
                FROM application a
                JOIN person ch ON a.child_id = ch.id
                LEFT JOIN LATERAL ( 
                    SELECT COALESCE(array_agg(e::UUID) FILTER (WHERE e IS NOT NULL), '{}'::UUID[]) AS preferredUnits
                    FROM jsonb_array_elements_text(a.document -> 'apply' -> 'preferredUnits') e
                ) pu ON true
                LEFT JOIN placement_plan pp ON pp.application_id = a.id
                WHERE a.status IN ('WAITING_PLACEMENT', 'WAITING_DECISION')
            """
                        )
                    }
                    .toList {
                        PlacementApplication(
                            id = column("id"),
                            firstName = column("first_name"),
                            lastName = column("last_name"),
                            dateOfBirth = column("date_of_birth"),
                            placementType = mapRequestedPlacementType("document"),
                            preferredUnits = column("preferredUnits"),
                            serviceStart = column("service_start"),
                            serviceEnd = column("service_end"),
                            assistanceNeed = column("assistance_need"),
                            plannedPlacementUnit = column("planned_placement_unit"),
                            trialPlacementUnit = column("trial_placement_unit"),
                        )
                    }
            }
        }
    }

    data class TrialPlacementUnitRequest(val trialPlacementUnit: DaycareId?)

    @PutMapping("/applications/{applicationId}/trial-placement-unit")
    fun setTrialPlacementUnit(
        db: Database,
        user: AuthenticatedUser.Employee,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: TrialPlacementUnitRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.createUpdate {
                        sql(
                            """
                UPDATE application 
                SET trial_placement_unit = ${bind(body.trialPlacementUnit)}
                WHERE id = ${bind(applicationId)}
            """
                        )
                    }
                    .updateExactlyOne()
            }
        }
    }
}

data class PlacementDaycare(
    val id: DaycareId,
    val name: String,
    val careAreaName: String,
    val serviceWorkerNote: String?,
    val capacity: Int,
    val childOccupancyConfirmed: Double,
    val childOccupancyPlanned: Double,
)

data class PlacementApplication(
    val id: ApplicationId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val placementType: PlacementType,
    val preferredUnits: List<DaycareId>,
    val serviceStart: String,
    val serviceEnd: String,
    val assistanceNeed: String?,
    val plannedPlacementUnit: DaycareId?,
    val trialPlacementUnit: DaycareId?,
) {
    val primaryPreference: DaycareId
        get() = preferredUnits.first()

    val otherPreferences: List<DaycareId>
        get() = preferredUnits.drop(1)
}
