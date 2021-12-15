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
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

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
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @RequestParam(value = "daycareId", required = false) daycareId: DaycareId? = null,
        @RequestParam(value = "childId", required = false) childId: UUID? = null,
        @RequestParam(
            value = "from",
            required = false
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate? = null,
        @RequestParam(
            value = "to",
            required = false
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate? = null
    ): Set<DaycarePlacementWithDetails> {
        Audit.PlacementSearch.log(targetId = daycareId ?: childId)
        when {
            daycareId != null -> accessControl.requirePermissionFor(user, Action.Unit.READ_PLACEMENT, daycareId)
            childId != null -> accessControl.requirePermissionFor(user, Action.Child.READ_PLACEMENT, childId)
            else -> throw BadRequest("daycareId or childId is required")
        }

        val auth = acl.getAuthorizedDaycares(user)
        val authorizedDaycares = auth.ids ?: emptySet()

        return db.read {
            it.getDetailedDaycarePlacements(daycareId, childId, startDate, endDate).map { placement ->
                // TODO: is some info only hidden on frontend?
                if (auth !is AclAuthorization.All && !authorizedDaycares.contains(placement.daycare.id))
                    placement.copy(isRestrictedFromUser = true)
                else placement
            }.toSet()
        }
    }

    @GetMapping("/placements/plans")
    fun getPlacementPlans(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @RequestParam(value = "daycareId", required = true) daycareId: DaycareId,
        @RequestParam(
            value = "from",
            required = false
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): List<PlacementPlanDetails> {
        Audit.PlacementPlanSearch.log(targetId = daycareId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_PLACEMENT_PLAN, daycareId)

        return db.read { it.getPlacementPlans(daycareId, startDate, endDate) }
    }

    @PostMapping("/placements")
    fun createPlacement(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @RequestBody body: PlacementCreateRequestBody
    ) {
        Audit.PlacementCreate.log(targetId = body.childId, objectId = body.unitId)
        accessControl.requirePermissionFor(user, Action.Unit.CREATE_PLACEMENT, body.unitId)

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
                listOf(AsyncJob.GenerateFinanceDecisions.forChild(body.childId, DateRange(body.startDate, body.endDate)))
            )
        }
    }

    @PutMapping("/placements/{placementId}")
    fun updatePlacementById(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @PathVariable("placementId") placementId: PlacementId,
        @RequestBody body: PlacementUpdateRequestBody
    ) {
        Audit.PlacementUpdate.log(targetId = placementId)
        accessControl.requirePermissionFor(user, Action.Placement.UPDATE, placementId)

        val aclAuth = acl.getAuthorizedDaycares(user)
        db.transaction { tx ->
            val oldPlacement = tx.updatePlacement(placementId, body.startDate, body.endDate, aclAuth, useFiveYearsOldDaycare)
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
                )
            )
        }
    }

    @DeleteMapping("/placements/{placementId}")
    fun deletePlacement(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @PathVariable("placementId") placementId: PlacementId
    ) {
        Audit.PlacementCancel.log(targetId = placementId)
        accessControl.requirePermissionFor(user, Action.Placement.DELETE, placementId)

        db.transaction { tx ->
            val (childId, startDate, endDate) = tx.cancelPlacement(placementId)
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.GenerateFinanceDecisions.forChild(childId, DateRange(startDate, endDate)))
            )
        }
    }

    @PostMapping("/placements/{placementId}/group-placements")
    fun createGroupPlacement(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @PathVariable("placementId") placementId: PlacementId,
        @RequestBody body: GroupPlacementRequestBody
    ): GroupPlacementId {
        Audit.DaycareGroupPlacementCreate.log(targetId = placementId, objectId = body.groupId)
        accessControl.requirePermissionFor(user, Action.Placement.CREATE_GROUP_PLACEMENT, placementId)

        return db.transaction { tx ->
            tx.checkAndCreateGroupPlacement(
                daycarePlacementId = placementId,
                groupId = body.groupId,
                startDate = body.startDate,
                endDate = body.endDate
            )
        }
    }

    @DeleteMapping("/group-placements/{groupPlacementId}")
    fun deleteGroupPlacement(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @PathVariable("groupPlacementId") groupPlacementId: GroupPlacementId
    ) {
        Audit.DaycareGroupPlacementDelete.log(targetId = groupPlacementId)
        accessControl.requirePermissionFor(user, Action.GroupPlacement.DELETE, groupPlacementId)

        db.transaction { it.deleteGroupPlacement(groupPlacementId) }
    }

    @PostMapping("/group-placements/{groupPlacementId}/transfer")
    fun transferGroupPlacement(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @PathVariable("groupPlacementId") groupPlacementId: GroupPlacementId,
        @RequestBody body: GroupTransferRequestBody
    ) {
        Audit.DaycareGroupPlacementTransfer.log(targetId = groupPlacementId)
        accessControl.requirePermissionFor(user, Action.GroupPlacement.UPDATE, groupPlacementId)

        db.transaction {
            it.transferGroup(groupPlacementId, body.groupId, body.startDate)
        }
    }
}

data class PlacementCreateRequestBody(
    val type: PlacementType,
    val childId: UUID,
    val unitId: DaycareId,
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
