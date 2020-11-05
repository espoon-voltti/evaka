// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.noContent
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.daycare.service.ChildService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyPlacementPlanApplied
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles.FINANCE_ADMIN
import fi.espoo.evaka.shared.config.Roles.SERVICE_WORKER
import fi.espoo.evaka.shared.config.Roles.STAFF
import fi.espoo.evaka.shared.config.Roles.UNIT_SUPERVISOR
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.runAfterCommit
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.Jdbi
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
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
import javax.sql.DataSource

@Controller
@RequestMapping("/placements")
class PlacementController(
    private val acl: AccessControlList,
    private val placementService: PlacementService,
    private val placementPlanService: PlacementPlanService,
    private val childService: ChildService,
    private val asyncJobRunner: AsyncJobRunner,
    private val jdbi: Jdbi,
    private val dataSource: DataSource
) {
    @GetMapping
    fun getPlacements(
        user: AuthenticatedUser,
        @RequestParam(value = "daycareId", required = false) daycareId: UUID? = null,
        @RequestParam(value = "childId", required = false) childId: UUID? = null,
        @RequestParam(
            value = "from",
            required = false
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate? = null,
        @RequestParam(
            value = "to",
            required = false
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate? = null
    ): ResponseEntity<Set<DaycarePlacementWithGroups>> {
        Audit.PlacementSearch.log(targetId = daycareId ?: childId)

        val roles = when {
            daycareId != null -> acl.getRolesForUnit(user, daycareId)
            childId != null -> acl.getRolesForChild(user, childId)
            else -> throw BadRequest("daycareId or childId is required")
        }
        roles.requireOneOfRoles(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF)
        val auth = acl.getAuthorizedDaycares(user)
        val authorizedDaycares = auth.ids ?: emptySet()

        return jdbi.handle {
            getDaycarePlacements(it, daycareId, childId, startDate, endDate).map { placement ->
                if (auth !is AclAuthorization.All && !authorizedDaycares.contains(placement.daycare.id))
                    placement.copy(isRestrictedFromUser = true)
                else placement
            }.toSet().let(::ok)
        }
    }

    @GetMapping("/plans")
    fun getPlacementPlans(
        user: AuthenticatedUser,
        @RequestParam(value = "daycareId", required = true) daycareId: UUID,
        @RequestParam(
            value = "from",
            required = false
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<PlacementPlanDetails>> {
        Audit.PlacementPlanSearch.log(targetId = daycareId)
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR)

        return placementPlanService.getPlacementPlansByUnit(daycareId, startDate, endDate).let(::ok)
    }

    @PostMapping
    @Transactional
    fun createPlacement(
        user: AuthenticatedUser,
        @RequestBody body: PlacementCreateRequestBody
    ): ResponseEntity<Placement> {
        Audit.PlacementCreate.log(targetId = body.childId, objectId = body.unitId)
        acl.getRolesForUnit(user, body.unitId)
            .requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR)

        if (body.startDate > body.endDate) throw BadRequest("Placement start date cannot be after the end date")

        childService.initEmptyIfNotExists(body.childId)
        val placement = placementService.createPlacement(
            type = body.type,
            childId = body.childId,
            unitId = body.unitId,
            startDate = body.startDate,
            endDate = body.endDate
        )

        withSpringHandle(dataSource) {
            asyncJobRunner.plan(it, listOf(NotifyPlacementPlanApplied(body.childId, body.startDate, body.endDate)))
        }
        runAfterCommit { asyncJobRunner.scheduleImmediateRun() }

        return ResponseEntity.created(URI.create("/placements/${placement.id}")).body(placement)
    }

    @PutMapping("/{placementId}")
    fun updatePlacementById(
        user: AuthenticatedUser,
        @PathVariable("placementId") placementId: UUID,
        @RequestBody body: PlacementUpdateRequestBody
    ): ResponseEntity<Unit> {
        Audit.PlacementUpdate.log(targetId = placementId)
        acl.getRolesForPlacement(user, placementId)
            .requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR)

        val aclAuth = acl.getAuthorizedDaycares(user)
        jdbi.transaction { h ->
            val oldPlacement = updatePlacement(h, placementId, body.startDate, body.endDate, aclAuth)
            asyncJobRunner.plan(
                h,
                listOf(
                    NotifyPlacementPlanApplied(
                        oldPlacement.childId,
                        minOf(body.startDate, oldPlacement.startDate),
                        maxOf(body.endDate, oldPlacement.endDate)
                    )
                )
            )
        }
        asyncJobRunner.scheduleImmediateRun()

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{placementId}")
    fun deletePlacement(
        user: AuthenticatedUser,
        @PathVariable("placementId") placementId: UUID
    ): ResponseEntity<Unit> {
        Audit.PlacementCancel.log(targetId = placementId)
        acl.getRolesForPlacement(user, placementId)
            .requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR)

        jdbi.transaction { h ->
            val (childId, startDate, endDate) = h.cancelPlacement(placementId)
            asyncJobRunner.plan(h, listOf(NotifyPlacementPlanApplied(childId, startDate, endDate)))
        }
        asyncJobRunner.scheduleImmediateRun()

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{placementId}/group-placements")
    fun createGroupPlacement(
        user: AuthenticatedUser,
        @PathVariable("placementId") placementId: UUID,
        @RequestBody body: GroupPlacementRequestBody
    ): ResponseEntity<UUID> {
        Audit.DaycareGroupPlacementCreate.log(targetId = placementId, objectId = body.groupId)
        acl.getRolesForPlacement(user, placementId)
            .requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR)

        return jdbi.transaction { h ->
            createGroupPlacement(
                h = h,
                daycarePlacementId = placementId,
                groupId = body.groupId,
                startDate = body.startDate,
                endDate = body.endDate
            ).let {
                ResponseEntity.created(URI.create("/placements/$placementId/group-placements/$it")).body(it)
            }
        }
    }

    @DeleteMapping("/{daycarePlacementId}/group-placements/{groupPlacementId}")
    fun deleteGroupPlacement(
        user: AuthenticatedUser,
        @PathVariable("daycarePlacementId") daycarePlacementId: UUID,
        @PathVariable("groupPlacementId") groupPlacementId: UUID
    ): ResponseEntity<Unit> {
        Audit.DaycareGroupPlacementDelete.log(targetId = groupPlacementId)
        acl.getRolesForPlacement(user, daycarePlacementId)
            .requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR)

        return jdbi.transaction {
            deleteGroupPlacement(it, groupPlacementId).let(::noContent)
        }
    }

    @PostMapping("/{daycarePlacementId}/group-placements/{groupPlacementId}/transfer")
    fun transferGroupPlacement(
        user: AuthenticatedUser,
        @PathVariable("daycarePlacementId") daycarePlacementId: UUID,
        @PathVariable("groupPlacementId") groupPlacementId: UUID,
        @RequestBody body: GroupTransferRequestBody
    ): ResponseEntity<Unit> {
        Audit.DaycareGroupPlacementTransfer.log(targetId = groupPlacementId)
        acl.getRolesForPlacement(user, daycarePlacementId)
            .requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR)

        return jdbi.transaction {
            transferGroup(it, daycarePlacementId, groupPlacementId, body.groupId, body.startDate).let(::noContent)
        }
    }
}

data class PlacementCreateRequestBody(
    val type: PlacementType,
    val childId: UUID,
    val unitId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class PlacementUpdateRequestBody(
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class GroupPlacementRequestBody(
    val groupId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class GroupTransferRequestBody(
    val groupId: UUID,
    val startDate: LocalDate
)
