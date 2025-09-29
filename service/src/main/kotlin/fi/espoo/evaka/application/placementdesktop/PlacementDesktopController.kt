// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.placementdesktop

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
    data class PlacementDraftUpdateRequest(val unitId: DaycareId?)

    @PutMapping("/applications/{applicationId}/placement-draft")
    fun updateApplicationPlacementDraft(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: PlacementDraftUpdateRequest,
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
                tx.updateApplicationPlacementDraft(
                    applicationId = applicationId,
                    unitId = body.unitId,
                    now = clock.now(),
                    userId = user.evakaUserId,
                )
            }
        }
        Audit.ApplicationPlacementDraftUpdate.log(targetId = AuditId(applicationId))
    }

    @GetMapping("/daycares/{unitId}")
    fun getPlacementDesktopDaycare(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @PathVariable unitId: DaycareId,
    ): PlacementDesktopDaycare {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_PLACEMENT_DESKTOP_DAYCARES,
                    )
                    tx.getPlacementDesktopDaycare(unitId)
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
    ): List<PlacementDesktopDaycare> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_PLACEMENT_DESKTOP_DAYCARES,
                    )
                    tx.getPlacementDesktopDaycares(unitIds)
                }
            }
            .also { Audit.PlacementDesktopDaycaresRead.log(targetId = AuditId(unitIds)) }
    }
}
