// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.application.placementdesktop

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.shared.ApplicationId
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/placement-desktop")
class PlacementDesktopController(private val accessControl: AccessControl) {

    data class PlacementDraftUpdateRequest(val unitId: DaycareId, val startDate: LocalDate? = null)

    data class PlacementDraftUpdateResponse(val startDate: LocalDate)

    @PutMapping("/applications/{applicationId}/placement-draft")
    fun upsertApplicationPlacementDraft(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: PlacementDraftUpdateRequest,
    ): PlacementDraftUpdateResponse {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Application.UPDATE_PLACEMENT_DRAFT,
                        applicationId,
                    )

                    val startDate =
                        tx.upsertApplicationPlacementDraft(
                            applicationId = applicationId,
                            unitId = body.unitId,
                            startDate = body.startDate,
                            now = clock.now(),
                            userId = user.evakaUserId,
                        )

                    PlacementDraftUpdateResponse(startDate)
                }
            }
            .also { Audit.ApplicationPlacementDraftUpdate.log(targetId = AuditId(applicationId)) }
    }

    @DeleteMapping("/applications/{applicationId}/placement-draft")
    fun deleteApplicationPlacementDraft(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @PathVariable applicationId: ApplicationId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Application.UPDATE_PLACEMENT_DRAFT,
                    applicationId,
                )
                tx.deleteApplicationPlacementDraftIfExists(applicationId)
            }
        }
        Audit.ApplicationPlacementDraftDelete.log(targetId = AuditId(applicationId))
    }

    @GetMapping("/daycares/{unitId}")
    fun getPlacementDesktopDaycare(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @PathVariable unitId: DaycareId,
        @RequestParam occupancyStart: LocalDate? = null,
    ): PlacementDesktopDaycare {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_PLACEMENT_DESKTOP_DAYCARES,
                    )
                    val today = clock.today()
                    getPlacementDesktopDaycareWithOccupancies(
                        tx = tx,
                        unitId = unitId,
                        occupancyPeriod =
                            (occupancyStart ?: today).let { FiniteDateRange(it, it.plusMonths(3)) },
                        today = today,
                    )
                }
            }
            .also { Audit.PlacementDesktopDaycaresRead.log(targetId = AuditId(unitId)) }
    }

    @GetMapping("/daycares")
    fun getPlacementDesktopDaycares(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestParam unitIds: Set<DaycareId> = emptySet(),
        @RequestParam occupancyStart: LocalDate? = null,
    ): List<PlacementDesktopDaycare> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_PLACEMENT_DESKTOP_DAYCARES,
                    )
                    val today = clock.today()
                    getPlacementDesktopDaycaresWithOccupancies(
                        tx = tx,
                        unitIds = unitIds,
                        occupancyPeriod =
                            (occupancyStart ?: today).let { FiniteDateRange(it, it.plusMonths(3)) },
                        today = today,
                    )
                }
            }
            .also { Audit.PlacementDesktopDaycaresRead.log(targetId = AuditId(unitIds)) }
    }
}
