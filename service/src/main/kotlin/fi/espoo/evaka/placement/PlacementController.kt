// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.daycare.controllers.AdditionalInformation
import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PlacementController(
    private val acl: AccessControlList,
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    env: EvakaEnv
) {
    private val useFiveYearsOldDaycare = env.fiveYearsOldDaycareEnabled

    @GetMapping("/placements")
    fun getPlacements(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam(value = "daycareId", required = false) daycareId: DaycareId? = null,
        @RequestParam(value = "childId", required = false) childId: ChildId? = null,
        @RequestParam(value = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate? = null,
        @RequestParam(value = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate? = null
    ): Set<DaycarePlacementWithDetails> {
        Audit.PlacementSearch.log(targetId = daycareId ?: childId)
        when {
            daycareId != null ->
                accessControl.requirePermissionFor(
                    user,
                    clock,
                    Action.Unit.READ_PLACEMENT,
                    daycareId
                )
            childId != null ->
                accessControl.requirePermissionFor(
                    user,
                    clock,
                    Action.Child.READ_PLACEMENT,
                    childId
                )
            else -> throw BadRequest("daycareId or childId is required")
        }

        val auth = acl.getAuthorizedUnits(user)
        val authorizedDaycares = auth.ids ?: emptySet()

        return db.connect { dbc ->
            dbc.read {
                it.getDetailedDaycarePlacements(daycareId, childId, startDate, endDate)
                    .map { placement ->
                        // TODO: is some info only hidden on frontend?
                        if (
                            auth !is AclAuthorization.All &&
                                !authorizedDaycares.contains(placement.daycare.id)
                        )
                            placement.copy(isRestrictedFromUser = true)
                        else placement
                    }
                    .toSet()
            }
        }
    }

    @GetMapping("/placements/plans")
    fun getPlacementPlans(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam(value = "daycareId", required = true) daycareId: DaycareId,
        @RequestParam(value = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate,
        @RequestParam(value = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate
    ): List<PlacementPlanDetails> {
        Audit.PlacementPlanSearch.log(targetId = daycareId)
        accessControl.requirePermissionFor(user, clock, Action.Unit.READ_PLACEMENT_PLAN, daycareId)

        return db.connect { dbc ->
            dbc.read {
                it.getPlacementPlans(
                    HelsinkiDateTime.now().toLocalDate(),
                    daycareId,
                    startDate,
                    endDate
                )
            }
        }
    }

    @PostMapping("/placements")
    fun createPlacement(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: PlacementCreateRequestBody
    ) {
        Audit.PlacementCreate.log(targetId = body.childId, objectId = body.unitId)
        accessControl.requirePermissionFor(user, clock, Action.Unit.CREATE_PLACEMENT, body.unitId)

        if (body.startDate > body.endDate)
            throw BadRequest("Placement start date cannot be after the end date")

        db.connect { dbc ->
            dbc.transaction { tx ->
                if (tx.getChild(body.childId) == null) {
                    tx.createChild(
                        Child(id = body.childId, additionalInformation = AdditionalInformation())
                    )
                }

                createPlacement(
                    tx,
                    childId = body.childId,
                    unitId = body.unitId,
                    period = FiniteDateRange(body.startDate, body.endDate),
                    type = body.type,
                    useFiveYearsOldDaycare = useFiveYearsOldDaycare
                )

                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forChild(
                            body.childId,
                            DateRange(body.startDate, body.endDate)
                        )
                    ),
                    runAt = clock.now()
                )
            }
        }
    }

    @PutMapping("/placements/{placementId}")
    fun updatePlacementById(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("placementId") placementId: PlacementId,
        @RequestBody body: PlacementUpdateRequestBody
    ) {
        Audit.PlacementUpdate.log(targetId = placementId)
        accessControl.requirePermissionFor(user, clock, Action.Placement.UPDATE, placementId)

        val aclAuth = acl.getAuthorizedUnits(user)
        db.connect { dbc ->
            dbc.transaction { tx ->
                val oldPlacement =
                    tx.updatePlacement(
                        placementId,
                        body.startDate,
                        body.endDate,
                        aclAuth,
                        useFiveYearsOldDaycare
                    )
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forChild(
                            oldPlacement.childId,
                            DateRange(
                                minOf(body.startDate, oldPlacement.startDate),
                                maxOf(body.endDate, oldPlacement.endDate)
                            )
                        )
                    ),
                    runAt = clock.now()
                )
            }
        }
    }

    @DeleteMapping("/placements/{placementId}")
    fun deletePlacement(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("placementId") placementId: PlacementId
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Placement.DELETE, placementId)

        db.connect { dbc ->
                dbc.transaction { tx ->
                    tx.cancelPlacement(placementId).also {
                        asyncJobRunner.plan(
                            tx,
                            listOf(
                                AsyncJob.GenerateFinanceDecisions.forChild(
                                    it.childId,
                                    DateRange(it.startDate, it.endDate)
                                )
                            ),
                            runAt = clock.now()
                        )
                    }
                }
            }
            .also {
                Audit.PlacementCancel.log(
                    targetId = placementId,
                    objectId = "${it.childId} ${it.unitId}"
                )
            }
    }

    @PostMapping("/placements/{placementId}/group-placements")
    fun createGroupPlacement(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("placementId") placementId: PlacementId,
        @RequestBody body: GroupPlacementRequestBody
    ): GroupPlacementId {
        Audit.DaycareGroupPlacementCreate.log(targetId = placementId, objectId = body.groupId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Placement.CREATE_GROUP_PLACEMENT,
            placementId
        )

        return db.connect { dbc ->
            dbc.transaction { tx ->
                tx.checkAndCreateGroupPlacement(
                    daycarePlacementId = placementId,
                    groupId = body.groupId,
                    startDate = body.startDate,
                    endDate = body.endDate
                )
            }
        }
    }

    @DeleteMapping("/group-placements/{groupPlacementId}")
    fun deleteGroupPlacement(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("groupPlacementId") groupPlacementId: GroupPlacementId
    ) {
        Audit.DaycareGroupPlacementDelete.log(targetId = groupPlacementId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.GroupPlacement.DELETE,
            groupPlacementId
        )

        db.connect { dbc -> dbc.transaction { it.deleteGroupPlacement(groupPlacementId) } }
    }

    @PostMapping("/group-placements/{groupPlacementId}/transfer")
    fun transferGroupPlacement(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("groupPlacementId") groupPlacementId: GroupPlacementId,
        @RequestBody body: GroupTransferRequestBody
    ) {
        Audit.DaycareGroupPlacementTransfer.log(targetId = groupPlacementId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.GroupPlacement.UPDATE,
            groupPlacementId
        )

        db.connect { dbc ->
            dbc.transaction { it.transferGroup(groupPlacementId, body.groupId, body.startDate) }
        }
    }

    @GetMapping("/placements/child-placement-periods/{adultId}")
    fun getChildPlacementPeriods(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable adultId: PersonId
    ): List<FiniteDateRange> {
        Audit.PlacementChildPlacementPeriodsRead.log(targetId = adultId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Person.READ_CHILD_PLACEMENT_PERIODS,
            adultId
        )

        return db.connect { dbc ->
            dbc.read { tx ->
                tx.createQuery(
                        """
WITH all_fridge_children AS (
    SELECT child_id, start_date, end_date
    FROM fridge_child WHERE head_of_child = :adultId

    UNION ALL

    SELECT fc.child_id, greatest(fc.start_date, fp2.start_date) AS start_date, least(fc.end_date, coalesce(fp2.end_date, fc.end_date)) AS end_date
    FROM fridge_partner fp1
    JOIN fridge_partner fp2 ON fp2.partnership_id = fp1.partnership_id AND fp2.indx != fp1.indx AND fp1.person_id = :adultId
    JOIN fridge_child fc ON fc.head_of_child = fp2.person_id AND daterange(fc.start_date, fc.end_date, '[]') && daterange(fp2.start_date, fp2.end_date, '[]')
)
SELECT greatest(p.start_date, fc.start_date) AS start, least(p.end_date, fc.end_date) AS end
FROM placement p
JOIN all_fridge_children fc ON fc.child_id = p.child_id AND daterange(p.start_date, p.end_date, '[]') && daterange(fc.start_date, fc.end_date, '[]')
"""
                    )
                    .bind("adultId", adultId)
                    .map { rv -> FiniteDateRange(rv.mapColumn("start"), rv.mapColumn("end")) }
                    .toList()
            }
        }
    }
}

data class PlacementCreateRequestBody(
    val type: PlacementType,
    val childId: ChildId,
    val unitId: DaycareId,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class PlacementUpdateRequestBody(val startDate: LocalDate, val endDate: LocalDate)

data class GroupPlacementRequestBody(
    val groupId: GroupId,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class GroupTransferRequestBody(val groupId: GroupId, val startDate: LocalDate)
