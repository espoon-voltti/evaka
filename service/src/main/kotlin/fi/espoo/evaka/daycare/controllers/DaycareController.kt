// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.daycare.CaretakerAmount
import fi.espoo.evaka.daycare.Daycare
import fi.espoo.evaka.daycare.DaycareFields
import fi.espoo.evaka.daycare.UnitFeatures
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
import fi.espoo.evaka.daycare.service.DaycareCapacityStats
import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.daycare.service.DaycareService
import fi.espoo.evaka.daycare.setUnitFeatures
import fi.espoo.evaka.daycare.updateCaretakers
import fi.espoo.evaka.daycare.updateDaycare
import fi.espoo.evaka.daycare.updateDaycareManager
import fi.espoo.evaka.daycare.updateGroup
import fi.espoo.evaka.shared.DaycareCaretakerId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
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
    private val acl: AccessControlList,
    private val accessControl: AccessControl
) {
    @GetMapping
    fun getDaycares(db: Database, user: AuthenticatedUser): List<Daycare> {
        Audit.UnitSearch.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_UNITS)
        return db.connect { dbc -> dbc.read { it.getDaycares(acl.getAuthorizedDaycares(user)) } }
    }

    @GetMapping("/features")
    fun getFeatures(
        db: Database,
        user: AuthenticatedUser
    ): List<UnitFeatures> {
        Audit.UnitFeaturesRead.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_UNIT_FEATURES)
        return db.connect { dbc -> dbc.read { it.getUnitFeatures() } }
    }

    @PutMapping("/{daycareId}/features")
    fun putFeatures(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @RequestBody features: Set<PilotFeature>
    ) {
        Audit.UnitFeaturesUpdate.log(targetId = daycareId)
        accessControl.requirePermissionFor(user, Action.Unit.UPDATE_FEATURES, daycareId)
        db.connect { dbc -> dbc.transaction { it.setUnitFeatures(daycareId, features) } }
    }

    @GetMapping("/{daycareId}")
    fun getDaycare(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId
    ): DaycareResponse {
        Audit.UnitRead.log(targetId = daycareId)
        accessControl.requirePermissionFor(user, Action.Unit.READ, daycareId)
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.getDaycare(daycareId)?.let { daycare ->
                    val groups = tx.getDaycareGroupSummaries(daycareId)
                    val permittedActions = accessControl.getPermittedGroupActions(user, groups.map { it.id })
                    DaycareResponse(
                        daycare,
                        groups.map {
                            DaycareGroupResponse(id = it.id, name = it.name, permittedActions = permittedActions[it.id]!!)
                        },
                        accessControl.getPermittedUnitActions(user, listOf(daycareId)).values.first()
                    )
                }
            }
        } ?: throw NotFound("daycare $daycareId not found")
    }

    @GetMapping("/{daycareId}/groups")
    fun getGroups(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate? = null,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate? = null
    ): List<DaycareGroup> {
        Audit.UnitGroupsSearch.log(targetId = daycareId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_GROUPS, daycareId)
        return db.connect { dbc -> dbc.read { daycareService.getDaycareGroups(it, daycareId, from, to) } }
    }

    @PostMapping("/{daycareId}/groups")
    fun createGroup(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @RequestBody body: CreateGroupRequest
    ): DaycareGroup {
        Audit.UnitGroupsCreate.log(targetId = daycareId)
        accessControl.requirePermissionFor(user, Action.Unit.CREATE_GROUP, daycareId)

        return db.connect { dbc -> dbc.transaction { daycareService.createGroup(it, daycareId, body.name, body.startDate, body.initialCaretakers) } }
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
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
        @RequestBody body: GroupUpdateRequest
    ) {
        Audit.UnitGroupsUpdate.log(targetId = groupId)
        accessControl.requirePermissionFor(user, Action.Group.UPDATE, groupId)

        db.connect { dbc -> dbc.transaction { it.updateGroup(groupId, body.name, body.startDate, body.endDate) } }
    }

    @DeleteMapping("/{daycareId}/groups/{groupId}")
    fun deleteGroup(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId
    ) {
        Audit.UnitGroupsDelete.log(targetId = groupId)
        accessControl.requirePermissionFor(user, Action.Group.DELETE, groupId)

        db.connect { dbc -> dbc.transaction { daycareService.deleteGroup(it, groupId) } }
    }

    @GetMapping("/{daycareId}/groups/{groupId}/caretakers")
    fun getCaretakers(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId
    ): CaretakersResponse {
        Audit.UnitGroupsCaretakersRead.log(targetId = groupId)
        accessControl.requirePermissionFor(user, Action.Group.READ_CARETAKERS, groupId)

        return db.connect { dbc ->
            dbc.read {
                CaretakersResponse(
                    caretakers = getCaretakers(it, groupId),
                    unitName = it.getDaycareStub(daycareId)?.name ?: "",
                    groupName = it.getDaycareGroup(groupId)?.name ?: ""
                )
            }
        }
    }

    @PostMapping("/{daycareId}/groups/{groupId}/caretakers")
    fun createCaretakers(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
        @RequestBody body: CaretakerRequest
    ) {
        Audit.UnitGroupsCaretakersCreate.log(targetId = groupId)
        accessControl.requirePermissionFor(user, Action.Group.CREATE_CARETAKERS, groupId)

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
    }

    @PutMapping("/{daycareId}/groups/{groupId}/caretakers/{id}")
    fun updateCaretakers(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
        @PathVariable id: DaycareCaretakerId,
        @RequestBody body: CaretakerRequest
    ) {
        Audit.UnitGroupsCaretakersUpdate.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.Group.UPDATE_CARETAKERS, groupId)

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
    }

    @DeleteMapping("/{daycareId}/groups/{groupId}/caretakers/{id}")
    fun removeCaretakers(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @PathVariable groupId: GroupId,
        @PathVariable id: DaycareCaretakerId
    ) {
        Audit.UnitGroupsCaretakersDelete.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.Group.DELETE_CARETAKERS, groupId)

        db.connect { dbc ->
            dbc.transaction {
                deleteCaretakers(
                    it,
                    groupId = groupId,
                    id = id
                )
            }
        }
    }

    @GetMapping("/{daycareId}/stats")
    fun getStats(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): DaycareCapacityStats {
        Audit.UnitStatisticsCreate.log(targetId = daycareId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_CAPACITY_STATS, daycareId)
        return db.connect { dbc -> dbc.read { daycareService.getDaycareCapacityStats(it, daycareId, from, to) } }
    }

    @PutMapping("/{daycareId}")
    fun updateDaycare(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @RequestBody fields: DaycareFields
    ): Daycare {
        Audit.UnitUpdate.log(targetId = daycareId)
        accessControl.requirePermissionFor(user, Action.Unit.UPDATE, daycareId)
        fields.validate()
        return db.connect { dbc ->
            dbc.transaction {
                it.updateDaycareManager(daycareId, fields.unitManager)
                it.updateDaycare(daycareId, fields)
                it.getDaycare(daycareId)!!
            }
        }
    }

    @PostMapping
    fun createDaycare(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody fields: DaycareFields
    ): CreateDaycareResponse {
        Audit.UnitCreate.log()
        accessControl.requirePermissionFor(user, Action.Global.CREATE_UNIT)
        fields.validate()
        return CreateDaycareResponse(
            db.connect { dbc ->
                dbc.transaction {
                    val id = it.createDaycare(fields.areaId, fields.name)
                    it.updateDaycareManager(id, fields.unitManager)
                    it.updateDaycare(id, fields)
                    id
                }
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

    @ExcludeCodeGen
    data class DaycareGroupResponse(val id: GroupId, val name: String, val permittedActions: Set<Action.Group>)

    @ExcludeCodeGen
    data class DaycareResponse(val daycare: Daycare, val groups: List<DaycareGroupResponse>, val permittedActions: Set<Action.Unit>)
}
