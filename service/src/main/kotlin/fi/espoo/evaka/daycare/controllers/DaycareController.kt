// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.Daycare
import fi.espoo.evaka.daycare.DaycareFields
import fi.espoo.evaka.daycare.DaycareGroupSummary
import fi.espoo.evaka.daycare.controllers.utils.created
import fi.espoo.evaka.daycare.controllers.utils.noContent
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.daycare.createDaycare
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.daycare.getDaycareGroupSummaries
import fi.espoo.evaka.daycare.getDaycareStub
import fi.espoo.evaka.daycare.getDaycares
import fi.espoo.evaka.daycare.service.CaretakerAmount
import fi.espoo.evaka.daycare.service.CaretakerService
import fi.espoo.evaka.daycare.service.DaycareCapacityStats
import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.daycare.service.DaycareService
import fi.espoo.evaka.daycare.updateDaycare
import fi.espoo.evaka.daycare.updateDaycareManager
import fi.espoo.evaka.daycare.updateGroup
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Controller
@RequestMapping("/daycares")
class DaycareController(
    private val daycareService: DaycareService,
    private val caretakerService: CaretakerService,
    private val acl: AccessControlList
) {
    @GetMapping
    fun getDaycares(db: Database.Connection, user: AuthenticatedUser): ResponseEntity<List<Daycare>> {
        Audit.UnitSearch.log()
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)
        return ResponseEntity.ok(db.read { it.getDaycares(acl.getAuthorizedDaycares(user)) })
    }

    @GetMapping("/{daycareId}")
    fun getDaycare(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID
    ): ResponseEntity<DaycareResponse> {
        Audit.UnitRead.log(targetId = daycareId)
        val currentUserRoles = acl.getRolesForUnit(user, daycareId)
        currentUserRoles.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)
        return db.read { tx ->
            tx.getDaycare(daycareId)?.let { daycare ->
                val groups = tx.getDaycareGroupSummaries(daycareId)
                ResponseEntity.ok(DaycareResponse(daycare, groups, currentUserRoles.roles))
            }
        } ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/{daycareId}/groups")
    fun getGroups(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID,
        @RequestParam(
            value = "from",
            required = false
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate? = null,
        @RequestParam(
            value = "to",
            required = false
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate? = null
    ): ResponseEntity<List<DaycareGroup>> {
        Audit.UnitGroupsSearch.log(targetId = daycareId)
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)

        return db.read { daycareService.getDaycareGroups(it, daycareId, startDate, endDate) }.let(::ok)
    }

    @PostMapping("/{daycareId}/groups")
    fun createGroup(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID,
        @RequestBody body: CreateGroupRequest
    ): ResponseEntity<DaycareGroup> {
        Audit.UnitGroupsCreate.log(targetId = daycareId)
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        return db.transaction { daycareService.createGroup(it, daycareId, body.name, body.startDate, body.initialCaretakers) }
            .let { created(it, URI.create("/$daycareId/groups/${it.id}")) }
    }

    data class GroupUpdateRequest(
        val name: String,
        val startDate: LocalDate,
        val endDate: LocalDate?
    )
    @PutMapping("/{daycareId}/groups/{groupId}")
    fun updateGroup(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID,
        @PathVariable("groupId") groupId: GroupId,
        @RequestBody body: GroupUpdateRequest
    ): ResponseEntity<Unit> {
        Audit.UnitGroupsUpdate.log(targetId = groupId)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction { it.updateGroup(groupId, body.name, body.startDate, body.endDate) }

        return noContent()
    }

    @DeleteMapping("/{daycareId}/groups/{groupId}")
    fun deleteGroup(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID,
        @PathVariable("groupId") groupId: GroupId
    ): ResponseEntity<Unit> {
        Audit.UnitGroupsDelete.log(targetId = groupId)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)

        db.transaction { daycareService.deleteGroup(it, daycareId, groupId) }
        return noContent()
    }

    @GetMapping("/{daycareId}/groups/{groupId}/caretakers")
    fun getCaretakers(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID,
        @PathVariable("groupId") groupId: GroupId
    ): ResponseEntity<CaretakersResponse> {
        Audit.UnitGroupsCaretakersRead.log(targetId = groupId)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)

        return db.read {
            val daycareStub = it.getDaycareStub(daycareId)
            ok(
                CaretakersResponse(
                    caretakers = caretakerService.getCaretakers(it, groupId),
                    unitName = daycareStub?.name ?: "",
                    groupName = it.getDaycareGroup(groupId)?.name ?: ""
                )
            )
        }
    }

    @PostMapping("/{daycareId}/groups/{groupId}/caretakers")
    fun createCaretakers(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID,
        @PathVariable("groupId") groupId: GroupId,
        @RequestBody body: CaretakerRequest
    ): ResponseEntity<Unit> {
        Audit.UnitGroupsCaretakersCreate.log(targetId = groupId)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction {
            caretakerService.insert(
                it,
                groupId = groupId,
                startDate = body.startDate,
                endDate = body.endDate,
                amount = body.amount
            )
        }
        return noContent()
    }

    @PutMapping("/{daycareId}/groups/{groupId}/caretakers/{id}")
    fun updateCaretakers(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID,
        @PathVariable("groupId") groupId: GroupId,
        @PathVariable("id") id: UUID,
        @RequestBody body: CaretakerRequest
    ): ResponseEntity<Unit> {
        Audit.UnitGroupsCaretakersUpdate.log(targetId = id)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction {
            caretakerService.update(
                it,
                groupId = groupId,
                id = id,
                startDate = body.startDate,
                endDate = body.endDate,
                amount = body.amount
            )
        }
        return noContent()
    }

    @DeleteMapping("/{daycareId}/groups/{groupId}/caretakers/{id}")
    fun removeCaretakers(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID,
        @PathVariable("groupId") groupId: GroupId,
        @PathVariable("id") id: UUID
    ): ResponseEntity<Unit> {
        Audit.UnitGroupsCaretakersDelete.log(targetId = id)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction {
            caretakerService.delete(
                it,
                groupId = groupId,
                id = id
            )
        }
        return noContent()
    }

    @GetMapping("/{daycareId}/stats")
    fun getStats(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID,
        @RequestParam(
            value = "from",
            required = true
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam(value = "to", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<DaycareCapacityStats> {
        Audit.UnitStatisticsCreate.log(targetId = daycareId)
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)

        return db.read { daycareService.getDaycareCapacityStats(it, daycareId, startDate, endDate) }.let(::ok)
    }

    @PutMapping("/{daycareId}")
    fun updateDaycare(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID,
        @RequestBody fields: DaycareFields
    ): ResponseEntity<Daycare> {
        Audit.UnitUpdate.log(targetId = daycareId)
        user.requireOneOfRoles(UserRole.ADMIN)
        fields.validate()
        return ResponseEntity.ok(
            db.transaction {
                it.updateDaycareManager(daycareId, fields.unitManager)
                it.updateDaycare(daycareId, fields)
                it.getDaycare(daycareId)!!
            }
        )
    }

    @PutMapping("")
    fun createDaycare(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody fields: DaycareFields
    ): ResponseEntity<CreateDaycareResponse> {
        Audit.UnitCreate.log()
        user.requireOneOfRoles(UserRole.ADMIN)
        fields.validate()
        return ResponseEntity.ok(
            CreateDaycareResponse(
                db.transaction {
                    val id = it.createDaycare(fields.areaId, fields.name)
                    it.updateDaycareManager(id, fields.unitManager)
                    it.updateDaycare(id, fields)
                    id
                }
            )
        )
    }

    data class CreateDaycareResponse(val id: UUID)

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
}

data class DaycareResponse(val daycare: Daycare, val groups: List<DaycareGroupSummary>, val currentUserRoles: Set<UserRole>)
