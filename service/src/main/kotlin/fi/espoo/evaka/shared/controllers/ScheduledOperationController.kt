// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.controllers

import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/scheduled")
class ScheduledOperationController {

    @PostMapping("/dvv/update")
    fun dvvUpdate(db: Database.Connection) {
        // executed by DailyJobRunner instead
    }

    @PostMapping("/koski/update")
    fun koskiUpdate(
        db: Database.Connection,
        @RequestParam(required = false) personIds: String?,
        @RequestParam(required = false) daycareIds: String?
    ): ResponseEntity<Unit> {
        // executed by DailyJobRunner instead
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/varda/update")
    fun vardaUpdate(db: Database.Connection): ResponseEntity<Unit> {
        // executed by DailyJobRunner instead
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/application/clear-old-drafts")
    fun removeOldDraftApplications(db: Database.Connection): ResponseEntity<Unit> {
        // executed by DailyJobRunner instead
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/application/cancel-outdated-transfer-applications")
    fun cancelOutdatedTransferApplications(db: Database.Connection): ResponseEntity<Unit> {
        // executed by DailyJobRunner instead
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/attendance-upkeep")
    fun endOfDayAttendanceUpkeep(db: Database.Connection): ResponseEntity<Unit> {
        // executed by DailyJobRunner instead
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/send-pending-decision-reminder-emails")
    fun sendPendingDecisionReminderEmails(db: Database.Connection): ResponseEntity<Unit> {
        // executed by DailyJobRunner instead
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/freeze-voucher-value-reports")
    fun freezeVoucherValueReports(
        db: Database.Connection,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?
    ): ResponseEntity<Unit> {
        // executed by DailyJobRunner instead
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/end-outdated-voucher-value-decisions")
    fun endOutDatedVoucherValueDecisions(db: Database.Connection): ResponseEntity<Unit> {
        // executed by DailyJobRunner instead
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/clear-old-daycare-daily-notes")
    fun removeOldDaycareDailyNotes(db: Database.Connection): ResponseEntity<Unit> {
        // executed by DailyJobRunner instead
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/remove-old-async-jobs")
    fun removeOldAsyncJobs(db: Database.Connection) {
        // executed by DailyJobRunner instead
    }
}
