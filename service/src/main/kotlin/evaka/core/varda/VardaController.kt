// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.varda

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.shared.ChildId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/varda")
class VardaController(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val accessControl: AccessControl,
) {
    @PostMapping("/child/reset/{childId}")
    fun markChildForVardaReset(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ) {
        db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.VARDA_OPERATIONS,
                    )
                    asyncJobRunner.plan(
                        it,
                        listOf(AsyncJob.VardaUpdateChild(childId, dryRun = false)),
                        retryCount = 1,
                        runAt = clock.now(),
                    )
                }
            }
            .also { Audit.VardaReportOperations.log(targetId = AuditId(childId)) }
    }
}
