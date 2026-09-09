// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.placement

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.absence.generateAbsencesFromIrregularDailyServiceTimes
import evaka.core.daycare.controllers.AdditionalInformation
import evaka.core.daycare.controllers.Child
import evaka.core.daycare.createChild
import evaka.core.daycare.getChild
import evaka.core.daycare.getDaycares
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.GroupId
import evaka.core.shared.GroupPlacementId
import evaka.core.shared.PersonId
import evaka.core.shared.PlacementId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AclAuthorization
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PlacementController(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    featureConfig: FeatureConfig,
) {
    private val useFiveYearsOldDaycare = featureConfig.fiveYearsOldDaycareEnabled

    @GetMapping("/employee/children/{childId}/placements")
    fun getChildPlacements(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): PlacementResponse {
        val audit = AuditContext().add(childId)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_PLACEMENT,
                        childId,
                    )

                    val canReadServiceNeeds =
                        accessControl.hasPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Child.READ_SERVICE_NEEDS,
                            childId,
                        )

                    val authorizedDaycares =
                        tx.getDaycares(
                                clock,
                                accessControl.requireAuthorizationFilter(
                                    tx,
                                    user,
                                    clock,
                                    Action.Unit.READ,
                                ),
                            )
                            .asSequence()
                            .map { it.id }
                            .toSet()

                    tx.getDetailedDaycarePlacements(daycareId = null, childId, range = null)
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
                            val serviceNeedIds = placements.flatMap { placement ->
                                placement.serviceNeedDetail?.serviceNeeds.orEmpty().map {
                                    serviceNeed ->
                                    serviceNeed.id
                                }
                            }
                            audit
                                .add(placementIds)
                                .add(serviceNeedIds)
                                .add(placements.map { it.daycare.id })
                                .observeDate(placements.minOfOrNull { it.startDate })
                            val responsePlacements =
                                if (canReadServiceNeeds) placements
                                else placements.map { it.copy(serviceNeedDetail = null) }.toSet()
                            PlacementResponse(
                                placements = responsePlacements,
                                permittedPlacementActions =
                                    accessControl.getPermittedActions(
                                        tx,
                                        user,
                                        clock,
                                        placementIds,
                                    ),
                                permittedServiceNeedActions =
                                    accessControl.getPermittedActions(
                                        tx,
                                        user,
                                        clock,
                                        serviceNeedIds,
                                    ),
                            )
                        }
                }
            }
            .also { audit.log(Audit.PlacementSearch, clock) }
    }

    @PostMapping("/employee/placements")
    fun createPlacement(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: PlacementCreateRequestBody,
    ) {
        if (body.startDate > body.endDate)
            throw BadRequest("Placement start date cannot be after the end date")
        val now = clock.now()
        val audit = AuditContext().add(body.childId).add(body.unitId).observeDate(body.startDate)

        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.CREATE_PLACEMENT,
                        body.unitId,
                    )
                    if (tx.getChild(body.childId) == null) {
                        tx.createChild(
                            Child(
                                id = body.childId,
                                additionalInformation = AdditionalInformation(),
                            ),
                            clock.now(),
                        )
                    }

                    createPlacement(
                            tx,
                            childId = body.childId,
                            unitId = body.unitId,
                            period = FiniteDateRange(body.startDate, body.endDate),
                            type = body.type,
                            useFiveYearsOldDaycare = useFiveYearsOldDaycare,
                            placeGuarantee = body.placeGuarantee,
                            now = now,
                            userId = user.evakaUserId,
                            source = PlacementSource.MANUAL,
                        )
                        .also { placements ->
                            audit.add(placements.map { it.id })
                            tx.deleteFutureReservationsAndAbsencesOutsideValidPlacements(
                                body.childId,
                                now.toLocalDate(),
                            )
                            generateAbsencesFromIrregularDailyServiceTimes(tx, now, body.childId)
                            asyncJobRunner.plan(
                                tx,
                                listOf(
                                    AsyncJob.GenerateFinanceDecisions.forChild(
                                        body.childId,
                                        DateRange(body.startDate, body.endDate),
                                    )
                                ),
                                runAt = now,
                            )
                        }
                }
            }
            .also { audit.log(Audit.PlacementCreate, clock) }
    }

    @PutMapping("/employee/placements/{placementId}")
    fun updatePlacementById(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable placementId: PlacementId,
        @RequestBody body: PlacementUpdateRequestBody,
    ) {
        val now = clock.now()
        val audit =
            AuditContext()
                .add(placementId)
                .observeDate(body.startDate)
                .addMeta("startDate", body.startDate)
                .addMeta("endDate", body.endDate)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Placement.UPDATE,
                        placementId,
                    )
                    val authorizedDaycares =
                        tx.getDaycares(
                                clock,
                                accessControl.requireAuthorizationFilter(
                                    tx,
                                    user,
                                    clock,
                                    Action.Unit.READ,
                                ),
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
                            useFiveYearsOldDaycare,
                            clock.now(),
                            user.evakaUserId,
                            audit,
                        )

                    tx.deleteFutureReservationsAndAbsencesOutsideValidPlacements(
                        oldPlacement.childId,
                        now.toLocalDate(),
                    )
                    generateAbsencesFromIrregularDailyServiceTimes(tx, now, oldPlacement.childId)
                    asyncJobRunner.plan(
                        tx,
                        listOf(
                            AsyncJob.GenerateFinanceDecisions.forChild(
                                oldPlacement.childId,
                                DateRange(
                                    minOf(body.startDate, oldPlacement.startDate),
                                    maxOf(body.endDate, oldPlacement.endDate),
                                ),
                            )
                        ),
                        runAt = now,
                    )
                    oldPlacement
                }
            }
            .also { audit.log(Audit.PlacementUpdate, clock) }
    }

    @DeleteMapping("/employee/placements/{placementId}")
    fun deletePlacement(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable placementId: PlacementId,
    ) {
        val now = clock.now()
        val audit = AuditContext().add(placementId)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Placement.DELETE,
                        placementId,
                    )

                    tx.cancelPlacement(now, user.evakaUserId, placementId).also {
                        audit
                            .add(it.childId)
                            .add(it.unitId)
                            .observeDate(it.startDate)
                            .addMeta("type", it.type)
                            .addMeta("startDate", it.startDate)
                            .addMeta("endDate", it.endDate)
                        tx.deleteFutureReservationsAndAbsencesOutsideValidPlacements(
                            it.childId,
                            now.toLocalDate(),
                        )
                        generateAbsencesFromIrregularDailyServiceTimes(tx, now, it.childId)
                        asyncJobRunner.plan(
                            tx,
                            listOf(
                                AsyncJob.GenerateFinanceDecisions.forChild(
                                    it.childId,
                                    DateRange(it.startDate, it.endDate),
                                )
                            ),
                            runAt = now,
                        )
                    }
                }
            }
            .also { audit.log(Audit.PlacementCancel, clock) }
    }

    @PostMapping("/employee/placements/{placementId}/group-placements")
    fun createGroupPlacement(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable placementId: PlacementId,
        @RequestBody body: GroupPlacementRequestBody,
    ): GroupPlacementId {
        val audit = AuditContext().add(placementId).add(body.groupId).observeDate(body.startDate)
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Placement.CREATE_GROUP_PLACEMENT,
                        placementId,
                    )
                    tx.checkAndCreateGroupPlacement(
                            daycarePlacementId = placementId,
                            groupId = body.groupId,
                            startDate = body.startDate,
                            endDate = body.endDate,
                            audit = audit,
                        )
                        .also { audit.add(it) }
                }
            }
            .also { audit.log(Audit.DaycareGroupPlacementCreate, clock) }
    }

    @DeleteMapping("/employee/group-placements/{groupPlacementId}")
    fun deleteGroupPlacement(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable groupPlacementId: GroupPlacementId,
    ) {
        val audit = AuditContext().add(groupPlacementId)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.GroupPlacement.DELETE,
                        groupPlacementId,
                    )
                    tx.deleteGroupPlacement(groupPlacementId, audit)
                }
            }
            .also { audit.log(Audit.DaycareGroupPlacementDelete, clock) }
    }

    @PostMapping("/employee/group-placements/{groupPlacementId}/transfer")
    fun transferGroupPlacement(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable groupPlacementId: GroupPlacementId,
        @RequestBody body: GroupTransferRequestBody,
    ) {
        val audit =
            AuditContext().add(groupPlacementId).add(body.groupId).observeDate(body.startDate)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.GroupPlacement.UPDATE,
                        groupPlacementId,
                    )
                    tx.transferGroup(groupPlacementId, body.groupId, body.startDate, audit)
                }
            }
            .also { audit.log(Audit.DaycareGroupPlacementTransfer, clock) }
    }

    @GetMapping("/employee/placements/child-placement-periods/{adultId}")
    fun getChildPlacementPeriods(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable adultId: PersonId,
    ): List<FiniteDateRange> {
        val audit = AuditContext().add(adultId)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_CHILD_PLACEMENT_PERIODS,
                        adultId,
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
SELECT fc.child_id, daterange(greatest(p.start_date, fc.start_date), least(p.end_date, fc.end_date), '[]') AS range
FROM placement p
JOIN all_fridge_children fc ON fc.child_id = p.child_id AND daterange(p.start_date, p.end_date, '[]') && daterange(fc.start_date, fc.end_date, '[]')
"""
                            )
                        }
                        .toList<ChildPlacementPeriod>()
                        .also { rows ->
                            audit
                                .add(rows.map { it.childId })
                                .observeDate(rows.minOfOrNull { it.range.start })
                                .addMeta("count", rows.size)
                        }
                        .map { it.range }
                }
            }
            .also { audit.log(Audit.PlacementChildPlacementPeriodsRead, clock) }
    }
}

private data class ChildPlacementPeriod(val childId: ChildId, val range: FiniteDateRange)

data class PlacementCreateRequestBody(
    val type: PlacementType,
    val childId: ChildId,
    val unitId: DaycareId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val placeGuarantee: Boolean,
)

data class PlacementUpdateRequestBody(val startDate: LocalDate, val endDate: LocalDate)

data class GroupPlacementRequestBody(
    val groupId: GroupId,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class GroupTransferRequestBody(val groupId: GroupId, val startDate: LocalDate)
