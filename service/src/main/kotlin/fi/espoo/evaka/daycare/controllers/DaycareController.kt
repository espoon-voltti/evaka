// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.CaretakerAmount
import fi.espoo.evaka.daycare.Daycare
import fi.espoo.evaka.daycare.DaycareFields
import fi.espoo.evaka.daycare.UnitFeatures
import fi.espoo.evaka.daycare.addUnitFeatures
import fi.espoo.evaka.daycare.createDaycare
import fi.espoo.evaka.daycare.deleteCaretakers
import fi.espoo.evaka.daycare.getCaretakers
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.daycare.getDaycareGroupSummaries
import fi.espoo.evaka.daycare.getDaycareStub
import fi.espoo.evaka.daycare.getDaycares
import fi.espoo.evaka.daycare.getUnitFeatures
import fi.espoo.evaka.daycare.insertCaretakers
import fi.espoo.evaka.daycare.removeUnitFeatures
import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.daycare.service.DaycareService
import fi.espoo.evaka.daycare.updateCaretakers
import fi.espoo.evaka.daycare.updateDaycare
import fi.espoo.evaka.daycare.updateDaycareManager
import fi.espoo.evaka.daycare.updateGroup
import fi.espoo.evaka.shared.DaycareCaretakerId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.PilotFeature
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
import java.time.LocalDate

@RestController
@RequestMapping("/daycares")
class DaycareController(
    private val daycareService: DaycareService,
    private val accessControl: AccessControl
) {
    @GetMapping
    fun getDaycares(db: Database, user: AuthenticatedUser, clock: EvakaClock): List<Daycare> {
        return db.connect { dbc ->
            dbc.read { tx ->
                val filter = accessControl.requireAuthorizationFilter(tx, user, clock, Action.Unit.READ)
                tx.getDaycares(AclAuthorization.from(filter))
            }
        }.also {
            Audit.UnitSearch.log()
        }
    }

    @GetMapping("/features")
    fun getFeatures(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<UnitFeatures> {
        accessControl.requirePermissionFor(user, clock, Action.Global.READ_UNIT_FEATURES)
        return db.connect { dbc -> dbc.read { it.getUnitFeatures() } }.also {
            Audit.UnitFeaturesRead.log()
        }
    }

    @PostMapping("/unit-features")
    fun postUnitFeatures(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody request: UpdateFeaturesRequest
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Unit.UPDATE_FEATURES, request.unitIds)

        db.connect { dbc ->
            dbc.transaction {
                if (request.enable) {
                    it.addUnitFeatures(request.unitIds, request.features)
                } else {
                    it.removeUnitFeatures(request.unitIds, request.features)
                }
            }
        }
        Audit.UnitFeaturesUpdate.log(targetId = request.unitIds)
    }

    @GetMapping("/{daycareId}")
    fun getDaycare(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId
    ): DaycareResponse {
        accessControl.requirePermissionFor(user, clock, Action.Unit.READ, daycareId)
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.getDaycare(daycareId)?.let { daycare ->
                    val groups = tx.getDaycareGroupSummaries(daycareId)
                    val permittedActions = accessControl.getPermittedActions<GroupId, Action.Group>(tx, user, clock, groups.map { it.id })
                    DaycareResponse(
                        daycare,
                        groups.map {
                            DaycareGroupResponse(
                                id = it.id,
                                name = it.name,
                                endDate = it.endDate,
                                permittedActions = permittedActions[it.id]!!
                            )
                        },
                        accessControl.getPermittedActions(tx, user, clock, daycareId)
                    )
                }
            } ?: throw NotFound("daycare $daycareId not found")
        }.also {
            Audit.UnitRead.log(targetId = daycareId)
        }
    }

    @GetMapping("/{daycareId}/groups")
    fun getGroups(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate? = null,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate? = null
    ): List<DaycareGroup> {
        accessControl.requirePermissionFor(user, clock, Action.Unit.READ_GROUPS, daycareId)
        return db.connect { dbc -> dbc.read { daycareService.getDaycareGroups(it, daycareId, from, to) } }.also {
            Audit.UnitGroupsSearch.log(targetId = daycareId)
        }
    }

    @PostMapping("/{daycareId}/groups")
    fun createGroup(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @RequestBody body: CreateGroupRequest
    ): DaycareGroup {
        accessControl.requirePermissionFor(user, clock, Action.Unit.CREATE_GROUP, daycareId)

        return db.connect { dbc -> dbc.transaction { daycareService.createGroup(it, daycareId, body.name, body.startDate, body.initialCaretakers) } }.also {
            Audit.UnitGroupsCreate.log(targetId = daycareId)
        }
    }

    data class GroupUpdateRequest(
        val name: String,
        val startDate: LocalDate,
        val endDate: LocalDate?
    )
    @PutMapping("/{daycareId}/groups/{groupId}")
    fun updateGroup(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
        @RequestBody body: GroupUpdateRequest
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Group.UPDATE, groupId)

        db.connect { dbc -> dbc.transaction { it.updateGroup(groupId, body.name, body.startDate, body.endDate) } }
        Audit.UnitGroupsUpdate.log(targetId = groupId)
    }

    @DeleteMapping("/{daycareId}/groups/{groupId}")
    fun deleteGroup(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Group.DELETE, groupId)

        db.connect { dbc -> dbc.transaction { daycareService.deleteGroup(it, groupId) } }
        Audit.UnitGroupsDelete.log(targetId = groupId)
    }

    @GetMapping("/{daycareId}/groups/{groupId}/caretakers")
    fun getCaretakers(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId
    ): CaretakersResponse {
        accessControl.requirePermissionFor(user, clock, Action.Group.READ_CARETAKERS, groupId)

        return db.connect { dbc ->
            dbc.read {
                CaretakersResponse(
                    caretakers = getCaretakers(it, groupId),
                    unitName = it.getDaycareStub(daycareId)?.name ?: "",
                    groupName = it.getDaycareGroup(groupId)?.name ?: ""
                )
            }
        }.also {
            Audit.UnitGroupsCaretakersRead.log(targetId = groupId)
        }
    }

    @PostMapping("/{daycareId}/groups/{groupId}/caretakers")
    fun createCaretakers(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
        @RequestBody body: CaretakerRequest
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Group.CREATE_CARETAKERS, groupId)

        db.connect { dbc ->
            dbc.transaction {
                insertCaretakers(
                    it,
                    groupId = groupId,
                    startDate = body.startDate,
                    endDate = body.endDate,
                    amount = body.amount
                )
            }
        }
        Audit.UnitGroupsCaretakersCreate.log(targetId = groupId)
    }

    @PutMapping("/{daycareId}/groups/{groupId}/caretakers/{id}")
    fun updateCaretakers(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
        @PathVariable id: DaycareCaretakerId,
        @RequestBody body: CaretakerRequest
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Group.UPDATE_CARETAKERS, groupId)

        db.connect { dbc ->
            dbc.transaction {
                updateCaretakers(
                    it,
                    groupId = groupId,
                    id = id,
                    startDate = body.startDate,
                    endDate = body.endDate,
                    amount = body.amount
                )
            }
        }
        Audit.UnitGroupsCaretakersUpdate.log(targetId = id)
    }

    @DeleteMapping("/{daycareId}/groups/{groupId}/caretakers/{id}")
    fun removeCaretakers(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
        @PathVariable id: DaycareCaretakerId
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Group.DELETE_CARETAKERS, groupId)

        db.connect { dbc ->
            dbc.transaction {
                deleteCaretakers(it, groupId = groupId, id = id)
            }
        }
        Audit.UnitGroupsCaretakersDelete.log(targetId = id)
    }

    @PutMapping("/{daycareId}")
    fun updateDaycare(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @RequestBody fields: DaycareFields
    ): Daycare {
        accessControl.requirePermissionFor(user, clock, Action.Unit.UPDATE, daycareId)
        fields.validate()
        return db.connect { dbc ->
            dbc.transaction {
                it.updateDaycareManager(daycareId, fields.unitManager)
                it.updateDaycare(daycareId, fields)
                it.getDaycare(daycareId)!!
            }
        }.also {
            Audit.UnitUpdate.log(targetId = daycareId)
        }
    }

    @PostMapping
    fun createDaycare(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody fields: DaycareFields
    ): CreateDaycareResponse {
        accessControl.requirePermissionFor(user, clock, Action.Global.CREATE_UNIT)
        fields.validate()
        return CreateDaycareResponse(
            db.connect { dbc ->
                dbc.transaction {
                    val id = it.createDaycare(fields.areaId, fields.name)
                    it.updateDaycareManager(id, fields.unitManager)
                    it.updateDaycare(id, fields)
                    id
                }
            }.also {
                Audit.UnitCreate.log()
            }
        )
    }

    data class CreateDaycareResponse(val id: DaycareId)

    data class CreateGroupRequest(
        val name: String,
        val startDate: LocalDate,
        val initialCaretakers: Double
    )

    data class CaretakerRequest(
        val startDate: LocalDate,
        val endDate: LocalDate?,
        val amount: Double
    )

    data class CaretakersResponse(
        val unitName: String,
        val groupName: String,
        val caretakers: List<CaretakerAmount>
    )

    data class DaycareGroupResponse(
        val id: GroupId,
        val name: String,
        val endDate: LocalDate?,
        val permittedActions: Set<Action.Group>
    )

    data class DaycareResponse(val daycare: Daycare, val groups: List<DaycareGroupResponse>, val permittedActions: Set<Action.Unit>)

    data class UpdateFeaturesRequest(val unitIds: List<DaycareId>, val features: List<PilotFeature>, val enable: Boolean)
}
