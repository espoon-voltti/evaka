// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.varda.new.getVardaUpdateChildIds
import fi.espoo.evaka.varda.old.VardaResetService
import fi.espoo.evaka.varda.old.VardaUpdateService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    "/varda", // deprecated
    "/employee/varda"
)
class VardaController(
    private val vardaService: VardaUpdateService,
    private val vardaResetService: VardaResetService,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val accessControl: AccessControl,
    private val vardaEnv: VardaEnv
) {
    @PostMapping("/start-update")
    fun runFullVardaUpdate(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ) {
        db
            .connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.VARDA_OPERATIONS
                    )
                }
                vardaService.updateUnits(dbc, clock)
                vardaService.planVardaChildrenUpdate(dbc, clock)
            }.also { Audit.VardaReportOperations.log() }
    }

    @PostMapping("/start-reset")
    fun runFullVardaReset(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ) {
        db
            .connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.VARDA_OPERATIONS
                    )
                }
                vardaResetService.planVardaReset(
                    dbc,
                    clock,
                    addNewChildren = !vardaEnv.newIntegrationEnabled
                )
            }.also { Audit.VardaReportOperations.log() }
    }

    @PostMapping("/child/reset/{childId}")
    fun markChildForVardaReset(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ) {
        db
            .connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.VARDA_OPERATIONS
                    )
                    if (it.getVardaUpdateChildIds().contains(childId)) {
                        // New integration
                        asyncJobRunner.plan(
                            it,
                            listOf(AsyncJob.VardaUpdateChild(childId, dryRun = false)),
                            retryCount = 1,
                            runAt = clock.now()
                        )
                    } else {
                        // Old integration
                        it.resetChildResetTimestamp(childId)
                        asyncJobRunner.plan(
                            it,
                            listOf(AsyncJob.ResetVardaChildOld(childId)),
                            retryCount = 1,
                            runAt = clock.now()
                        )
                    }
                }
            }.also { Audit.VardaReportOperations.log(targetId = AuditId(childId)) }
    }
}
