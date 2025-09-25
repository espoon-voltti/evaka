// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.placementdesktop

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycaresById
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
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
    data class TrialPlacementUpdateRequest(val trialUnitId: DaycareId?)

    @PutMapping("/applications/{applicationId}/trial-unit")
    fun updateApplicationTrialPlacement(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: TrialPlacementUpdateRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Application.UPDATE_TRIAL_PLACEMENT,
                    applicationId,
                )
                tx.updateApplicationTrialPlacement(
                    applicationId,
                    body.trialUnitId,
                    clock.now(),
                    user,
                )
            }
        }
        Audit.ApplicationTrialPlacementUnitUpdate.log(targetId = AuditId(applicationId))
    }

    data class PlacementDesktopDaycare(val id: DaycareId, val name: String, val foo: Int?)

    @GetMapping("/daycares/{daycareId}")
    fun getPlacementDesktopDaycare(
        db: Database,
        user: AuthenticatedUser.Employee,
        @PathVariable daycareId: DaycareId,
    ): PlacementDesktopDaycare {
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.getDaycare(daycareId)?.let {
                    PlacementDesktopDaycare(it.id, it.name, (Math.random() * 100.0).toInt())
                } ?: throw NotFound()
            }
        }
    }

    @GetMapping("/daycares")
    fun getPlacementDesktopDaycares(
        db: Database,
        user: AuthenticatedUser.Employee,
        @RequestParam daycareIds: Set<DaycareId> = emptySet(),
    ): List<PlacementDesktopDaycare> {
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.getDaycaresById(daycareIds).values.map {
                    PlacementDesktopDaycare(it.id, it.name, (Math.random() * 100.0).toInt())
                }
            }
        }
    }
}
