// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.absence.generateAbsencesFromIrregularDailyServiceTimes
import fi.espoo.evaka.daycare.controllers.AdditionalInformation
import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.daycare.getDaycares
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
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
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    featureConfig: FeatureConfig
) {
    private val useFiveYearsOldDaycare = featureConfig.fiveYearsOldDaycareEnabled

    @GetMapping(
        "/placements", // deprecated
        "/employee/placements"
    )
    fun getPlacements(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam daycareId: DaycareId? = null,
        @RequestParam childId: ChildId? = null,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate? = null,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate? = null
    ): PlacementResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    when {
                        daycareId != null ->
                            accessControl.requirePermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Unit.READ_PLACEMENT,
                                daycareId
                            )
                        childId != null ->
                            accessControl.requirePermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Child.READ_PLACEMENT,
                                childId
                            )
                        else -> throw BadRequest("daycareId or childId is required")
                    }

                    val authorizedDaycares =
                        tx.getDaycares(
                                clock,
                                accessControl.requireAuthorizationFilter(
                                    tx,
                                    user,
                                    clock,
                                    Action.Unit.READ
                                )
                            )
                            .asSequence()
                            .map { it.id }
                            .toSet()

                    tx.getDetailedDaycarePlacements(daycareId, childId, from, to)
                        .map { placement ->
                            // TODO: is some info only hidden on frontend?
                            if (!authorizedDaycares.contains(placement.daycare.id)) {
                                placement.copy(isRestrictedFromUser = true)
                            } else {
                                placement
                            }
                        }
                        .toSet()
                        .let { placements ->
                            val placementIds = placements.map { placement -> placement.id }
                            val serviceNeedIds =
                                placements.flatMap { placement ->
                                    placement.serviceNeeds.map { serviceNeed -> serviceNeed.id }
                                }
                            PlacementResponse(
                                placements = placements,
                                permittedPlacementActions =
                                    accessControl.getPermittedActions(
                                        tx,
                                        user,
                                        clock,
                                        placementIds
                                    ),
                                permittedServiceNeedActions =
                                    accessControl.getPermittedActions(
                                        tx,
                                        user,
                                        clock,
                                        serviceNeedIds
                                    ),
                            )
                        }
                }
            }
            .also {
                Audit.PlacementSearch.log(
                    targetId = daycareId?.let(AuditId::invoke) ?: childId?.let(AuditId::invoke),
                    meta =
                        mapOf("startDate" to from, "endDate" to to, "count" to it.placements.size)
                )
            }
    }

    @GetMapping(
        "/placements/plans", // deprecated
        "/employee/placements/plans"
    )
    fun getPlacementPlans(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam daycareId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): List<PlacementPlanDetails> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Unit.READ_PLACEMENT_PLAN,
                        daycareId
                    )

                    it.getPlacementPlans(HelsinkiDateTime.now().toLocalDate(), daycareId, from, to)
                }
            }
            .also {
                Audit.PlacementPlanSearch.log(
                    targetId = AuditId(daycareId),
                    meta = mapOf("startDate" to from, "endDate" to to, "count" to it.size)
                )
            }
    }

    @PostMapping(
        "/placements", // deprecated
        "/employee/placements"
    )
    fun createPlacement(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: PlacementCreateRequestBody
    ) {
        if (body.startDate > body.endDate)
            throw BadRequest("Placement start date cannot be after the end date")
        val now = clock.now()

        val placements =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.CREATE_PLACEMENT,
                        body.unitId
                    )
                    if (tx.getChild(body.childId) == null) {
                        tx.createChild(
                            Child(
                                id = body.childId,
                                additionalInformation = AdditionalInformation()
                            )
                        )
                    }

                    createPlacement(
                            tx,
                            childId = body.childId,
                            unitId = body.unitId,
                            period = FiniteDateRange(body.startDate, body.endDate),
                            type = body.type,
                            useFiveYearsOldDaycare = useFiveYearsOldDaycare,
                            placeGuarantee = body.placeGuarantee
                        )
                        .also {
                            tx.cleanupFutureReservationsAndAbsencesOutsideValidPlacements(
                                body.childId,
                                now.toLocalDate()
                            )
                            generateAbsencesFromIrregularDailyServiceTimes(tx, now, body.childId)
                            asyncJobRunner.plan(
                                tx,
                                listOf(
                                    AsyncJob.GenerateFinanceDecisions.forChild(
                                        body.childId,
                                        DateRange(body.startDate, body.endDate)
                                    )
                                ),
                                runAt = now
                            )
                        }
                }
            }
        Audit.PlacementCreate.log(
            targetId = AuditId(listOf(body.childId, body.unitId)),
            objectId = AuditId(placements.map { it.id })
        )
    }

    @PutMapping(
        "/placements/{placementId}", // deprecated
        "/employee/placements/{placementId}"
    )
    fun updatePlacementById(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable placementId: PlacementId,
        @RequestBody body: PlacementUpdateRequestBody
    ) {
        val now = clock.now()
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Placement.UPDATE,
                    placementId
                )
                val authorizedDaycares =
                    tx.getDaycares(
                            clock,
                            accessControl.requireAuthorizationFilter(
                                tx,
                                user,
                                clock,
                                Action.Unit.READ
                            )
                        )
                        .asSequence()
                        .map { it.id }
                        .toSet()
                val aclAuth = AclAuthorization.Subset(ids = authorizedDaycares)
                val oldPlacement =
                    tx.updatePlacement(
                        placementId,
                        body.startDate,
                        body.endDate,
                        aclAuth,
                        useFiveYearsOldDaycare
                    )

                tx.cleanupFutureReservationsAndAbsencesOutsideValidPlacements(
                    oldPlacement.childId,
                    now.toLocalDate()
                )
                generateAbsencesFromIrregularDailyServiceTimes(tx, now, oldPlacement.childId)
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
                    runAt = now
                )
            }
        }
        Audit.PlacementUpdate.log(targetId = AuditId(placementId))
    }

    @DeleteMapping(
        "/placements/{placementId}", // deprecated
        "/employee/placements/{placementId}"
    )
    fun deletePlacement(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable placementId: PlacementId
    ) {
        val now = clock.now()
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Placement.DELETE,
                        placementId
                    )
                    val placement =
                        tx.getPlacement(placementId)
                            ?: throw NotFound("Placement $placementId not found")
                    tx.cancelPlacement(placementId).also {
                        tx.cleanupFutureReservationsAndAbsencesOutsideValidPlacements(
                            it.childId,
                            now.toLocalDate()
                        )
                        generateAbsencesFromIrregularDailyServiceTimes(tx, now, it.childId)
                        asyncJobRunner.plan(
                            tx,
                            listOf(
                                AsyncJob.GenerateFinanceDecisions.forChild(
                                    it.childId,
                                    DateRange(it.startDate, it.endDate)
                                )
                            ),
                            runAt = now
                        )
                    }
                }
            }
            .also {
                Audit.PlacementCancel.log(
                    targetId = AuditId(placementId),
                    objectId = AuditId(listOf(it.childId, it.unitId))
                )
            }
    }

    @PostMapping(
        "/placements/{placementId}/group-placements", // deprecated
        "/employee/placements/{placementId}/group-placements"
    )
    fun createGroupPlacement(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable placementId: PlacementId,
        @RequestBody body: GroupPlacementRequestBody
    ): GroupPlacementId {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Placement.CREATE_GROUP_PLACEMENT,
                        placementId
                    )
                    tx.checkAndCreateGroupPlacement(
                        daycarePlacementId = placementId,
                        groupId = body.groupId,
                        startDate = body.startDate,
                        endDate = body.endDate
                    )
                }
            }
            .also { groupPlacementId ->
                Audit.DaycareGroupPlacementCreate.log(
                    targetId = AuditId(placementId),
                    objectId = AuditId(groupPlacementId),
                    meta = mapOf("groupId" to body.groupId)
                )
            }
    }

    @DeleteMapping(
        "/group-placements/{groupPlacementId}", // deprecated
        "/employee/group-placements/{groupPlacementId}"
    )
    fun deleteGroupPlacement(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable groupPlacementId: GroupPlacementId
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.GroupPlacement.DELETE,
                    groupPlacementId
                )
                it.deleteGroupPlacement(groupPlacementId)
            }
        }
        Audit.DaycareGroupPlacementDelete.log(targetId = AuditId(groupPlacementId))
    }

    @PostMapping(
        "/group-placements/{groupPlacementId}/transfer", // deprecated
        "/employee/group-placements/{groupPlacementId}/transfer"
    )
    fun transferGroupPlacement(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable groupPlacementId: GroupPlacementId,
        @RequestBody body: GroupTransferRequestBody
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.GroupPlacement.UPDATE,
                    groupPlacementId
                )
                it.transferGroup(groupPlacementId, body.groupId, body.startDate)
            }
        }
        Audit.DaycareGroupPlacementTransfer.log(
            targetId = AuditId(groupPlacementId),
            objectId = AuditId(body.groupId)
        )
    }

    @GetMapping(
        "/placements/child-placement-periods/{adultId}", // deprecated
        "/employee/placements/child-placement-periods/{adultId}"
    )
    fun getChildPlacementPeriods(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable adultId: PersonId
    ): List<FiniteDateRange> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_CHILD_PLACEMENT_PERIODS,
                        adultId
                    )
                    tx.createQuery {
                            sql(
                                """
WITH all_fridge_children AS (
    SELECT child_id, start_date, end_date
    FROM fridge_child WHERE head_of_child = ${bind(adultId)}

    UNION ALL

    SELECT fc.child_id, greatest(fc.start_date, fp2.start_date) AS start_date, least(fc.end_date, coalesce(fp2.end_date, fc.end_date)) AS end_date
    FROM fridge_partner fp1
    JOIN fridge_partner fp2 ON fp2.partnership_id = fp1.partnership_id AND fp2.indx != fp1.indx AND fp1.person_id = ${bind(adultId)}
    JOIN fridge_child fc ON fc.head_of_child = fp2.person_id AND daterange(fc.start_date, fc.end_date, '[]') && daterange(fp2.start_date, fp2.end_date, '[]')
)
SELECT greatest(p.start_date, fc.start_date) AS start, least(p.end_date, fc.end_date) AS end
FROM placement p
JOIN all_fridge_children fc ON fc.child_id = p.child_id AND daterange(p.start_date, p.end_date, '[]') && daterange(fc.start_date, fc.end_date, '[]')
"""
                            )
                        }
                        .toList { FiniteDateRange(column("start"), column("end")) }
                }
            }
            .also {
                Audit.PlacementChildPlacementPeriodsRead.log(
                    targetId = AuditId(adultId),
                    meta = mapOf("count" to it.size)
                )
            }
    }
}

data class PlacementCreateRequestBody(
    val type: PlacementType,
    val childId: ChildId,
    val unitId: DaycareId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val placeGuarantee: Boolean
)

data class PlacementUpdateRequestBody(val startDate: LocalDate, val endDate: LocalDate)

data class GroupPlacementRequestBody(
    val groupId: GroupId,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class GroupTransferRequestBody(val groupId: GroupId, val startDate: LocalDate)
