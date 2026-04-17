// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.daycare.controllers

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.absence.application.AbsenceApplicationStatus
import evaka.core.absence.application.selectAbsenceApplications
import evaka.core.application.getActiveTransferApplicationsFromUnit
import evaka.core.backupcare.UnitBackupCare
import evaka.core.backupcare.getBackupCaresForDaycare
import evaka.core.daycare.CaretakerAmount
import evaka.core.daycare.Daycare
import evaka.core.daycare.DaycareFields
import evaka.core.daycare.UnitFeatures
import evaka.core.daycare.UnitOperationPeriod
import evaka.core.daycare.addUnitFeatures
import evaka.core.daycare.createDaycare
import evaka.core.daycare.deleteCaretakers
import evaka.core.daycare.getCaretakers
import evaka.core.daycare.getDaycare
import evaka.core.daycare.getDaycareGroup
import evaka.core.daycare.getDaycareGroupSummaries
import evaka.core.daycare.getDaycareGroups
import evaka.core.daycare.getDaycareStub
import evaka.core.daycare.getDaycares
import evaka.core.daycare.getGroupStats
import evaka.core.daycare.getLastPlacementDate
import evaka.core.daycare.getOphUnitOIDs
import evaka.core.daycare.getUnitFeatures
import evaka.core.daycare.getUnitOperationPeriods
import evaka.core.daycare.insertCaretakers
import evaka.core.daycare.removeUnitFeatures
import evaka.core.daycare.service.Caretakers
import evaka.core.daycare.service.DaycareGroup
import evaka.core.daycare.service.DaycareService
import evaka.core.daycare.updateCaretakers
import evaka.core.daycare.updateDaycare
import evaka.core.daycare.updateGroup
import evaka.core.daycare.updateUnitClosingDate
import evaka.core.daycare.validateUnitClosingDate
import evaka.core.occupancy.OccupancyPeriod
import evaka.core.occupancy.OccupancyPeriodGroupLevel
import evaka.core.occupancy.OccupancyResponse
import evaka.core.occupancy.OccupancyType
import evaka.core.occupancy.calculateOccupancyPeriodsGroupLevel
import evaka.core.placement.DaycarePlacementWithDetails
import evaka.core.placement.MissingBackupGroupPlacement
import evaka.core.placement.MissingGroupPlacement
import evaka.core.placement.TerminatedPlacement
import evaka.core.placement.UnitChildrenCapacityFactors
import evaka.core.placement.getDetailedDaycarePlacements
import evaka.core.placement.getMissingGroupPlacements
import evaka.core.placement.getTerminatedPlacements
import evaka.core.placement.getUnitChildrenCapacities
import evaka.core.placement.getWaitingUnitConfirmationApplicationsCount
import evaka.core.serviceneed.application.getUndecidedServiceApplicationsByUnit
import evaka.core.shared.BackupCareId
import evaka.core.shared.DaycareCaretakerId
import evaka.core.shared.DaycareId
import evaka.core.shared.GroupId
import evaka.core.shared.GroupPlacementId
import evaka.core.shared.PlacementId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import evaka.core.shared.security.PilotFeature
import evaka.core.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/daycares")
class DaycareController(
    private val daycareService: DaycareService,
    private val accessControl: AccessControl,
) {
    @GetMapping
    fun getDaycares(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam includeClosed: Boolean = true,
    ): List<Daycare> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    val filter =
                        accessControl.requireAuthorizationFilter(tx, user, clock, Action.Unit.READ)
                    tx.getDaycares(clock, filter, includeClosed)
                }
            }
            .also { Audit.UnitSearch.log(meta = mapOf("count" to it.size)) }
    }

    @GetMapping("/features")
    fun getUnitFeatures(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<UnitFeatures> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_UNIT_FEATURES,
                    )
                    it.getUnitFeatures(clock.today())
                }
            }
            .also { Audit.UnitFeaturesRead.log(meta = mapOf("count" to it.size)) }
    }

    @PutMapping("/unit-features")
    fun updateUnitFeatures(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody request: UpdateFeaturesRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.UPDATE_FEATURES,
                    request.unitIds,
                )

                if (request.enable) {
                    it.addUnitFeatures(request.unitIds, request.features)
                } else {
                    it.removeUnitFeatures(request.unitIds, request.features)
                }
            }
        }
        Audit.UnitFeaturesUpdate.log(targetId = AuditId(request.unitIds))
    }

    @GetMapping("/{daycareId}")
    fun getDaycare(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
    ): DaycareResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(tx, user, clock, Action.Unit.READ, daycareId)
                    val daycare =
                        tx.getDaycare(daycareId) ?: throw NotFound("daycare $daycareId not found")
                    val lastPlacementDate = tx.getLastPlacementDate(daycareId)
                    val groups = tx.getDaycareGroupSummaries(daycareId)
                    val permittedActions =
                        accessControl.getPermittedActions<GroupId, Action.Group>(
                            tx,
                            user,
                            clock,
                            groups.map { it.id },
                        )
                    val reservedOphUnitOIDs =
                        tx.getOphUnitOIDs()
                            .filter { it.key != daycareId && it.value.trim().isNotEmpty() }
                            .values
                            .toSet()
                    DaycareResponse(
                        daycare,
                        groups.map {
                            DaycareGroupResponse(
                                id = it.id,
                                name = it.name,
                                endDate = it.endDate,
                                permittedActions = permittedActions[it.id]!!,
                            )
                        },
                        lastPlacementDate,
                        accessControl.getPermittedActions(tx, user, clock, daycareId),
                        reservedOphUnitOIDs,
                    )
                }
            }
            .also { Audit.UnitRead.log(targetId = AuditId(daycareId)) }
    }

    data class ServiceWorkerNote(val note: String)

    @GetMapping("/{daycareId}/service-worker-note")
    fun getUnitServiceWorkerNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
    ): ServiceWorkerNote {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_SERVICE_WORKER_NOTE,
                        daycareId,
                    )
                    tx.createQuery {
                            sql(
                                """
                    SELECT service_worker_note
                    FROM daycare
                    WHERE id = ${bind(daycareId)}
                """
                            )
                        }
                        .exactlyOneOrNull<String>()
                        ?.let { ServiceWorkerNote(it) }
                        ?: throw NotFound("daycare $daycareId not found")
                }
            }
            .also { Audit.UnitServiceWorkerNoteRead.log(targetId = AuditId(daycareId)) }
    }

    @PutMapping("/{daycareId}/service-worker-note")
    fun setUnitServiceWorkerNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @RequestBody body: ServiceWorkerNote,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.SET_SERVICE_WORKER_NOTE,
                        daycareId,
                    )
                    tx.execute {
                            sql(
                                """
                    UPDATE daycare
                    SET service_worker_note = ${bind(body.note)}
                    WHERE id = ${bind(daycareId)}
                """
                            )
                        }
                        .also { if (it == 0) throw NotFound("daycare $daycareId not found") }
                }
            }
            .also { Audit.UnitServiceWorkerNoteSet.log(targetId = AuditId(daycareId)) }
    }

    @GetMapping("/{daycareId}/groups")
    fun getGroups(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate? = null,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate? = null,
        @RequestParam includeClosed: Boolean = true,
    ): List<DaycareGroup> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Unit.READ_GROUPS,
                        daycareId,
                    )
                    daycareService.getDaycareGroups(it, daycareId, from, to, includeClosed)
                }
            }
            .also {
                Audit.UnitGroupsSearch.log(
                    targetId = AuditId(daycareId),
                    meta = mapOf("from" to from, "to" to to, "count" to it.size),
                )
            }
    }

    @PostMapping("/{daycareId}/groups")
    fun createGroup(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @RequestBody body: CreateGroupRequest,
    ): DaycareGroup {
        return db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Unit.CREATE_GROUP,
                        daycareId,
                    )
                    daycareService.createGroup(
                        it,
                        daycareId,
                        body.name,
                        body.startDate,
                        body.initialCaretakers,
                        body.aromiCustomerId,
                    )
                }
            }
            .also { group ->
                Audit.UnitGroupsCreate.log(
                    targetId = AuditId(daycareId),
                    objectId = AuditId(group.id),
                )
            }
    }

    data class GroupUpdateRequest(
        val name: String,
        val startDate: LocalDate,
        val endDate: LocalDate?,
        val jamixCustomerNumber: Int?,
        val aromiCustomerId: String?,
        val nekkuCustomerNumber: String?,
    )

    @PutMapping("/{daycareId}/groups/{groupId}")
    fun updateGroup(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
        @RequestBody body: GroupUpdateRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.Group.UPDATE, groupId)
                it.updateGroup(
                    groupId,
                    body.name,
                    body.startDate,
                    body.endDate,
                    body.jamixCustomerNumber,
                    body.aromiCustomerId,
                    body.nekkuCustomerNumber,
                )
            }
        }
        Audit.UnitGroupsUpdate.log(targetId = AuditId(groupId))
    }

    @DeleteMapping("/{daycareId}/groups/{groupId}")
    fun deleteGroup(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.Group.DELETE, groupId)
                daycareService.deleteGroup(it, groupId)
            }
        }
        Audit.UnitGroupsDelete.log(targetId = AuditId(groupId))
    }

    @GetMapping("/{daycareId}/groups/{groupId}/caretakers")
    fun getCaretakers(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
    ): CaretakersResponse {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Group.READ_CARETAKERS,
                        groupId,
                    )
                    CaretakersResponse(
                        caretakers = getCaretakers(it, groupId),
                        unitName = it.getDaycareStub(daycareId)?.name ?: "",
                        groupName = it.getDaycareGroup(groupId)?.name ?: "",
                    )
                }
            }
            .also {
                Audit.UnitGroupsCaretakersRead.log(
                    targetId = AuditId(groupId),
                    meta = mapOf("count" to it.caretakers.size),
                )
            }
    }

    @PostMapping("/{daycareId}/groups/{groupId}/caretakers")
    fun createCaretakers(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
        @RequestBody body: CaretakerRequest,
    ) {
        val daycareCaretakerId =
            db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Group.CREATE_CARETAKERS,
                        groupId,
                    )
                    insertCaretakers(
                        it,
                        groupId = groupId,
                        startDate = body.startDate,
                        endDate = body.endDate,
                        amount = body.amount,
                    )
                }
            }
        Audit.UnitGroupsCaretakersCreate.log(
            targetId = AuditId(groupId),
            objectId = AuditId(daycareCaretakerId),
        )
    }

    @PutMapping("/{daycareId}/groups/{groupId}/caretakers/{id}")
    fun updateCaretakers(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
        @PathVariable id: DaycareCaretakerId,
        @RequestBody body: CaretakerRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Group.UPDATE_CARETAKERS,
                    groupId,
                )
                updateCaretakers(
                    it,
                    groupId = groupId,
                    id = id,
                    startDate = body.startDate,
                    endDate = body.endDate,
                    amount = body.amount,
                )
            }
        }
        Audit.UnitGroupsCaretakersUpdate.log(targetId = AuditId(id))
    }

    @DeleteMapping("/{daycareId}/groups/{groupId}/caretakers/{id}")
    fun removeCaretakers(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
        @PathVariable id: DaycareCaretakerId,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Group.DELETE_CARETAKERS,
                    groupId,
                )
                deleteCaretakers(it, groupId = groupId, id = id)
            }
        }
        Audit.UnitGroupsCaretakersDelete.log(targetId = AuditId(id))
    }

    @PutMapping("/{daycareId}")
    fun updateDaycare(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @RequestBody fields: DaycareFields,
    ) {
        fields.validate()
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.UPDATE,
                        daycareId,
                    )
                    fields.closingDate?.also { validateUnitClosingDate(tx, daycareId, it) }
                    tx.updateDaycare(daycareId, fields)
                }
            }
            .also { Audit.UnitUpdate.log(targetId = AuditId(daycareId)) }
    }

    @PutMapping("/{unitId}/closing-date")
    fun updateUnitClosingDate(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @RequestParam closingDate: LocalDate,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(tx, user, clock, Action.Unit.UPDATE, unitId)
                    validateUnitClosingDate(tx, unitId, closingDate)
                    tx.updateUnitClosingDate(unitId, closingDate)
                }
            }
            .also { Audit.UnitUpdate.log(targetId = AuditId(unitId)) }
    }

    @PostMapping
    fun createDaycare(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody fields: DaycareFields,
    ): CreateDaycareResponse {
        fields.validate()
        return CreateDaycareResponse(
            db.connect { dbc ->
                    dbc.transaction {
                        accessControl.requirePermissionFor(
                            it,
                            user,
                            clock,
                            Action.Global.CREATE_UNIT,
                        )
                        val id = it.createDaycare(fields.areaId, fields.name)
                        it.updateDaycare(id, fields)
                        id
                    }
                }
                .also { unitId -> Audit.UnitCreate.log(targetId = AuditId(unitId)) }
        )
    }

    @GetMapping("/{unitId}/group-details")
    fun getUnitGroupDetails(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): UnitGroupDetails {
        val terminatedPlacementsViewWeeks = 2L
        val period = FiniteDateRange(from, to)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_GROUP_DETAILS,
                        unitId,
                    )
                    val groups = tx.getDaycareGroups(unitId, from, to)
                    val placements = tx.getDetailedDaycarePlacements(unitId, null, period).toList()
                    val backupCares = tx.getBackupCaresForDaycare(unitId, period)
                    val (missingGroupPlacements, missingBackupPlacements) =
                        if (
                            accessControl.hasPermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Unit.READ_MISSING_GROUP_PLACEMENTS,
                                unitId,
                            )
                        ) {
                            getMissingGroupPlacements(tx, unitId, clock.today().plusMonths(7))
                        } else {
                            Pair(emptyList(), emptyList())
                        }
                    val recentlyTerminatedPlacements =
                        if (
                            accessControl.hasPermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Unit.READ_TERMINATED_PLACEMENTS,
                                unitId,
                            )
                        ) {
                            val terminatedPlacements =
                                tx.getTerminatedPlacements(
                                    clock.today(),
                                    unitId,
                                    clock.today().minusWeeks(terminatedPlacementsViewWeeks),
                                    clock.today(),
                                )
                            val transferApplications =
                                tx.getActiveTransferApplicationsFromUnit(unitId, clock.today())

                            terminatedPlacements + transferApplications
                        } else {
                            emptyList()
                        }
                    val caretakers = tx.getGroupStats(unitId, FiniteDateRange(from, to))
                    val backupCareIds =
                        backupCares.map { it.id }.toSet() +
                            missingBackupPlacements.map { it.backupCareId }.toSet()
                    val placementIds =
                        placements.map { it.id }.toSet() +
                            missingGroupPlacements.map { it.placementId }.toSet()

                    val childIds =
                        placements.map { it.child.id }.toSet() +
                            backupCares.map { it.child.id }.toSet()

                    val capacities =
                        if (
                            accessControl.hasPermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Unit.READ_CHILD_CAPACITY_FACTORS,
                                unitId,
                            )
                        ) {
                            tx.getUnitChildrenCapacities(childIds, from)
                        } else {
                            listOf()
                        }

                    val groupOccupancies =
                        if (
                            accessControl.hasPermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Unit.READ_OCCUPANCIES,
                                unitId,
                            )
                        ) {
                            getGroupOccupancies(
                                tx,
                                clock.today(),
                                unitId,
                                period,
                                AccessControlFilter.PermitAll,
                            )
                        } else null

                    UnitGroupDetails(
                        groups = groups,
                        placements = placements,
                        backupCares = backupCares,
                        missingGroupPlacements = missingGroupPlacements,
                        missingBackupGroupPlacements = missingBackupPlacements,
                        recentlyTerminatedPlacements = recentlyTerminatedPlacements,
                        caretakers = caretakers,
                        unitChildrenCapacityFactors = capacities,
                        groupOccupancies = groupOccupancies,
                        permittedBackupCareActions =
                            accessControl.getPermittedActions(tx, user, clock, backupCareIds),
                        permittedPlacementActions =
                            accessControl.getPermittedActions(tx, user, clock, placementIds),
                        permittedGroupPlacementActions =
                            accessControl.getPermittedActions(
                                tx,
                                user,
                                clock,
                                placements.flatMap { placement ->
                                    placement.groupPlacements.mapNotNull { groupPlacement ->
                                        groupPlacement.id
                                    }
                                },
                            ),
                    )
                }
            }
            .also { Audit.UnitView.log(targetId = AuditId(unitId)) }
    }

    @GetMapping("/{daycareId}/notifications")
    fun getUnitNotifications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
    ): UnitNotifications {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    val daycareApplications =
                        if (
                            accessControl.hasPermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Unit.READ_APPLICATIONS_AND_PLACEMENT_PLANS,
                                daycareId,
                            )
                        )
                            tx.getWaitingUnitConfirmationApplicationsCount(daycareId)
                        else 0

                    val absenceApplications =
                        if (
                            accessControl.hasPermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Unit.READ_ABSENCE_APPLICATIONS,
                                daycareId,
                            )
                        )
                            tx.selectAbsenceApplications(
                                    unitId = daycareId,
                                    status = AbsenceApplicationStatus.WAITING_DECISION,
                                )
                                .size
                        else 0

                    val serviceApplications =
                        if (
                            accessControl.hasPermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Unit.READ_SERVICE_APPLICATIONS,
                                daycareId,
                            )
                        )
                            tx.getUndecidedServiceApplicationsByUnit(daycareId).size
                        else 0

                    UnitNotifications(
                        applications =
                            daycareApplications + absenceApplications + serviceApplications,
                        groups =
                            if (
                                accessControl.hasPermissionFor(
                                    tx,
                                    user,
                                    clock,
                                    Action.Unit.READ_MISSING_GROUP_PLACEMENTS,
                                    daycareId,
                                )
                            ) {
                                val (group, backup) =
                                    getMissingGroupPlacements(
                                        tx,
                                        daycareId,
                                        clock.today().plusMonths(7),
                                    )
                                group.size + backup.size
                            } else {
                                0
                            },
                    )
                }
            }
            .also { Audit.UnitCounters.log(targetId = AuditId(daycareId)) }
    }

    @GetMapping("/employee/unitOperationPeriods")
    fun getUnitOperationPeriods(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam unitIds: List<DaycareId>?,
    ): Map<DaycareId, UnitOperationPeriod> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requireAuthorizationFilter(tx, user, clock, Action.Unit.READ)
                    tx.getUnitOperationPeriods(unitIds)
                }
            }
            .also { response ->
                Audit.UnitOperationPeriodsRead.log(targetId = AuditId(response.keys))
            }
    }

    data class CreateDaycareResponse(val id: DaycareId)

    data class CreateGroupRequest(
        val name: String,
        val startDate: LocalDate,
        val initialCaretakers: Double,
        val aromiCustomerId: String?,
    )

    data class CaretakerRequest(
        val startDate: LocalDate,
        val endDate: LocalDate?,
        val amount: Double,
    )

    data class CaretakersResponse(
        val unitName: String,
        val groupName: String,
        val caretakers: List<CaretakerAmount>,
    )

    data class DaycareGroupResponse(
        val id: GroupId,
        val name: String,
        val endDate: LocalDate?,
        val permittedActions: Set<Action.Group>,
    )

    data class DaycareResponse(
        val daycare: Daycare,
        val groups: List<DaycareGroupResponse>,
        val lastPlacementDate: LocalDate?,
        val permittedActions: Set<Action.Unit>,
        val reservedOphUnitOIDs: Set<String>,
    )

    data class UpdateFeaturesRequest(
        val unitIds: List<DaycareId>,
        val features: List<PilotFeature>,
        val enable: Boolean,
    )

    data class UnitNotifications(val applications: Int, val groups: Int)
}

data class UnitGroupDetails(
    val groups: List<DaycareGroup>,
    val placements: List<DaycarePlacementWithDetails>,
    val backupCares: List<UnitBackupCare>,
    val missingGroupPlacements: List<MissingGroupPlacement>,
    val missingBackupGroupPlacements: List<MissingBackupGroupPlacement>,
    val recentlyTerminatedPlacements: List<TerminatedPlacement>,
    val caretakers: Map<GroupId, Caretakers>,
    val unitChildrenCapacityFactors: List<UnitChildrenCapacityFactors>,
    val groupOccupancies: GroupOccupancies?,
    val permittedBackupCareActions: Map<BackupCareId, Set<Action.BackupCare>>,
    val permittedPlacementActions: Map<PlacementId, Set<Action.Placement>>,
    val permittedGroupPlacementActions: Map<GroupPlacementId, Set<Action.GroupPlacement>>,
)

data class GroupOccupancies(
    val confirmed: Map<GroupId, OccupancyResponse>,
    val realized: Map<GroupId, OccupancyResponse>,
)

private fun getGroupOccupancies(
    tx: Database.Read,
    today: LocalDate,
    unitId: DaycareId,
    period: FiniteDateRange,
    unitFilter: AccessControlFilter<DaycareId>,
): GroupOccupancies {
    return GroupOccupancies(
        confirmed =
            getGroupOccupancyResponses(
                tx.calculateOccupancyPeriodsGroupLevel(
                    today,
                    unitId,
                    period,
                    OccupancyType.CONFIRMED,
                    unitFilter,
                )
            ),
        realized =
            getGroupOccupancyResponses(
                tx.calculateOccupancyPeriodsGroupLevel(
                    today,
                    unitId,
                    period,
                    OccupancyType.REALIZED,
                    unitFilter,
                )
            ),
    )
}

private fun getGroupOccupancyResponses(
    occupancies: List<OccupancyPeriodGroupLevel>
): Map<GroupId, OccupancyResponse> {
    return occupancies
        .groupBy { it.groupId }
        .mapValues { (_, value) ->
            val occupancyPeriods =
                value.map {
                    OccupancyPeriod(
                        period = it.period,
                        sum = it.sum,
                        headcount = it.headcount,
                        caretakers = it.caretakers,
                        percentage = it.percentage,
                    )
                }

            OccupancyResponse(
                occupancies = occupancyPeriods,
                max =
                    occupancyPeriods
                        .filter { it.percentage != null }
                        .maxByOrNull { it.percentage!! },
                min =
                    occupancyPeriods
                        .filter { it.percentage != null }
                        .minByOrNull { it.percentage!! },
            )
        }
}
