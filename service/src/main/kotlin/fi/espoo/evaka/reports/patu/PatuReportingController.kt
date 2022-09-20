// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports.patu

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/patu-report")
class PatuReportingController(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val accessControl: AccessControl
) {

    @PostMapping
    fun sendPatuReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ) {
        Audit.PatuReportSend.log()
        accessControl.requirePermissionFor(user, clock, Action.Global.SEND_PATU_REPORT)
        db.connect { dbc ->
            dbc.transaction { tx ->
                val range = DateRange(from, to)
                logger.info("Scheduling patu report $range")
                asyncJobRunner.plan(
                    tx,
                    payloads = listOf(AsyncJob.SendPatuReport(range)),
                    runAt = HelsinkiDateTime.now(),
                    retryCount = 1
                )
            }
        }
    }
}
