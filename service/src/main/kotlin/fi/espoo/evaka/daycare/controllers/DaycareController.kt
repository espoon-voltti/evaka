// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.absence.application.AbsenceApplicationStatus
import fi.espoo.evaka.absence.application.selectAbsenceApplications
import fi.espoo.evaka.application.getActiveTransferApplicationsFromUnit
import fi.espoo.evaka.backupcare.UnitBackupCare
import fi.espoo.evaka.backupcare.getBackupCaresForDaycare
import fi.espoo.evaka.daycare.CaretakerAmount
import fi.espoo.evaka.daycare.Daycare
import fi.espoo.evaka.daycare.DaycareFields
import fi.espoo.evaka.daycare.UnitFeatures
import fi.espoo.evaka.daycare.UnitOperationPeriod
import fi.espoo.evaka.daycare.addUnitFeatures
import fi.espoo.evaka.daycare.createDaycare
import fi.espoo.evaka.daycare.deleteCaretakers
import fi.espoo.evaka.daycare.getCaretakers
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.daycare.getDaycareGroupSummaries
import fi.espoo.evaka.daycare.getDaycareGroups
import fi.espoo.evaka.daycare.getDaycareStub
import fi.espoo.evaka.daycare.getDaycares
import fi.espoo.evaka.daycare.getGroupStats
import fi.espoo.evaka.daycare.getLastPlacementDate
import fi.espoo.evaka.daycare.getUnitFeatures
import fi.espoo.evaka.daycare.getUnitOperationPeriods
import fi.espoo.evaka.daycare.insertCaretakers
import fi.espoo.evaka.daycare.removeUnitFeatures
import fi.espoo.evaka.daycare.service.Caretakers
import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.daycare.service.DaycareService
import fi.espoo.evaka.daycare.updateCaretakers
import fi.espoo.evaka.daycare.updateDaycare
import fi.espoo.evaka.daycare.updateGroup
import fi.espoo.evaka.daycare.updateUnitClosingDate
import fi.espoo.evaka.daycare.validateUnitClosingDate
import fi.espoo.evaka.occupancy.OccupancyPeriod
import fi.espoo.evaka.occupancy.OccupancyPeriodGroupLevel
import fi.espoo.evaka.occupancy.OccupancyResponse
import fi.espoo.evaka.occupancy.OccupancyType
import fi.espoo.evaka.occupancy.calculateOccupancyPeriodsGroupLevel
import fi.espoo.evaka.placement.DaycarePlacementWithDetails
import fi.espoo.evaka.placement.MissingBackupGroupPlacement
import fi.espoo.evaka.placement.MissingGroupPlacement
import fi.espoo.evaka.placement.TerminatedPlacement
import fi.espoo.evaka.placement.UnitChildrenCapacityFactors
import fi.espoo.evaka.placement.getDetailedDaycarePlacements
import fi.espoo.evaka.placement.getMissingGroupPlacements
import fi.espoo.evaka.placement.getTerminatedPlacements
import fi.espoo.evaka.placement.getUnitChildrenCapacities
import fi.espoo.evaka.placement.getWaitingUnitConfirmationApplicationsCount
import fi.espoo.evaka.serviceneed.application.getUndecidedServiceApplicationsByUnit
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.DaycareCaretakerId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
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
                    daycareService.getDaycareGroups(it, daycareId, from, to)
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
