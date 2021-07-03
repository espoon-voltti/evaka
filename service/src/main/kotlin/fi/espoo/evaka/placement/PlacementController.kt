// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.AdditionalInformation
import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.daycare.controllers.utils.noContent
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.GenerateFinanceDecisions
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.core.env.Environment
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
@RequestMapping("/placements")
class PlacementController(
    private val acl: AccessControlList,
    private val asyncJobRunner: AsyncJobRunner,
    env: Environment
) {
    private val useFiveYearsOldDaycare = env.getProperty("fi.espoo.evaka.five_years_old_daycare.enabled", Boolean::class.java, true)

    @GetMapping
    fun getPlacements(
        db: Database.Connection,
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
    ): ResponseEntity<Set<DaycarePlacementWithDetails>> {
        Audit.PlacementSearch.log(targetId = daycareId ?: childId)

        val roles = when {
            daycareId != null -> acl.getRolesForUnit(user, daycareId)
            childId != null -> acl.getRolesForChild(user, childId)
            else -> throw BadRequest("daycareId or childId is required")
        }
        roles.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)
        val auth = acl.getAuthorizedDaycares(user)
        val authorizedDaycares = auth.ids ?: emptySet()

        return db.read {
            it.getDetailedDaycarePlacements(daycareId, childId, startDate, endDate).map { placement ->
                if (auth !is AclAuthorization.All && !authorizedDaycares.contains(placement.daycare.id))
                    placement.copy(isRestrictedFromUser = true)
                else placement
            }.toSet().let(::ok)
        }
    }

    @GetMapping("/plans")
    fun getPlacementPlans(
        db: Database.Connection,
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
            .requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR)

        return db.read { it.getPlacementPlans(daycareId, startDate, endDate) }.let(::ok)
    }

    @PostMapping
    fun createPlacement(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: PlacementCreateRequestBody
    ): ResponseEntity<Unit> {
        Audit.PlacementCreate.log(targetId = body.childId, objectId = body.unitId)
        acl.getRolesForUnit(user, body.unitId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)

        if (body.startDate > body.endDate) throw BadRequest("Placement start date cannot be after the end date")

        db.transaction { tx ->
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
                type = body.type,
                childId = body.childId,
                unitId = body.unitId,
                startDate = body.startDate,
                endDate = body.endDate,
                useFiveYearsOldDaycare = useFiveYearsOldDaycare
            )
            asyncJobRunner.plan(
                tx,
                listOf(GenerateFinanceDecisions.forChild(body.childId, DateRange(body.startDate, body.endDate)))
            )
        }

        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{placementId}")
    fun updatePlacementById(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("placementId") placementId: PlacementId,
        @RequestBody body: PlacementUpdateRequestBody
    ): ResponseEntity<Unit> {
        Audit.PlacementUpdate.log(targetId = placementId)
        acl.getRolesForPlacement(user, placementId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)

        val aclAuth = acl.getAuthorizedDaycares(user)
        db.transaction { tx ->
            val oldPlacement = tx.updatePlacement(placementId, body.startDate, body.endDate, aclAuth, useFiveYearsOldDaycare)
            asyncJobRunner.plan(
                tx,
                listOf(
                    GenerateFinanceDecisions.forChild(
                        oldPlacement.childId,
                        DateRange(
                            minOf(body.startDate, oldPlacement.startDate),
                            maxOf(body.endDate, oldPlacement.endDate)
                        )
                    )
                )
            )
        }

        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{placementId}")
    fun deletePlacement(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("placementId") placementId: PlacementId
    ): ResponseEntity<Unit> {
        Audit.PlacementCancel.log(targetId = placementId)
        acl.getRolesForPlacement(user, placementId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)

        db.transaction { tx ->
            val (childId, startDate, endDate) = tx.cancelPlacement(placementId)
            asyncJobRunner.plan(
                tx,
                listOf(GenerateFinanceDecisions.forChild(childId, DateRange(startDate, endDate)))
            )
        }

        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{placementId}/group-placements")
    fun createGroupPlacement(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("placementId") placementId: PlacementId,
        @RequestBody body: GroupPlacementRequestBody
    ): ResponseEntity<UUID> {
        Audit.DaycareGroupPlacementCreate.log(targetId = placementId, objectId = body.groupId)
        acl.getRolesForPlacement(user, placementId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        return db.transaction { tx ->
            tx.checkAndCreateGroupPlacement(
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
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycarePlacementId") daycarePlacementId: PlacementId,
        @PathVariable("groupPlacementId") groupPlacementId: UUID
    ): ResponseEntity<Unit> {
        Audit.DaycareGroupPlacementDelete.log(targetId = groupPlacementId)
        acl.getRolesForPlacement(user, daycarePlacementId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        val success = db.transaction { it.deleteGroupPlacement(groupPlacementId) }
        if (!success) throw NotFound("Group placement not found")
        return noContent()
    }

    @PostMapping("/{daycarePlacementId}/group-placements/{groupPlacementId}/transfer")
    fun transferGroupPlacement(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycarePlacementId") daycarePlacementId: PlacementId,
        @PathVariable("groupPlacementId") groupPlacementId: UUID,
        @RequestBody body: GroupTransferRequestBody
    ): ResponseEntity<Unit> {
        Audit.DaycareGroupPlacementTransfer.log(targetId = groupPlacementId)
        acl.getRolesForPlacement(user, daycarePlacementId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        return db.transaction {
            it.transferGroup(daycarePlacementId, groupPlacementId, body.groupId, body.startDate).let(::noContent)
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
    val groupId: GroupId,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class GroupTransferRequestBody(
    val groupId: GroupId,
    val startDate: LocalDate
)
