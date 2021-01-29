// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.removeOldDrafts
import fi.espoo.evaka.attachment.AttachmentsController
import fi.espoo.evaka.dvv.DvvModificationsBatchRefreshService
import fi.espoo.evaka.invoicing.controller.parseUUID
import fi.espoo.evaka.koski.KoskiSearchParams
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.ScheduleKoskiUploads
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.varda.VardaUpdateService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/scheduled")
class ScheduledOperationController(
    private val vardaUpdateService: VardaUpdateService,
    private val dvvModificationsBatchRefreshService: DvvModificationsBatchRefreshService,
    private val attachmentsController: AttachmentsController,
    private val asyncJobRunner: AsyncJobRunner
) {

    @PostMapping("/dvv/update")
    fun dvvUpdate(db: Database.Connection): ResponseEntity<Int> {
        Audit.VtjBatchSchedule.log()
        return ResponseEntity.ok(dvvModificationsBatchRefreshService.scheduleBatch(db))
    }

    @PostMapping("/koski/update")
    fun koskiUpdate(
        db: Database.Connection,
        @RequestParam(required = false) personIds: String?,
        @RequestParam(required = false) daycareIds: String?
    ): ResponseEntity<Unit> {
        val params = KoskiSearchParams(
            personIds = personIds?.split(",")?.map(::parseUUID) ?: listOf(),
            daycareIds = daycareIds?.split(",")?.map(::parseUUID) ?: listOf()
        )
        db.transaction { asyncJobRunner.plan(it, listOf(ScheduleKoskiUploads(params))) }
        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/varda/update")
    fun vardaUpdate(db: Database.Connection): ResponseEntity<Unit> {
        vardaUpdateService.scheduleVardaUpdate(db)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/application/clear-old-drafts")
    fun removeOldDraftApplications(db: Database.Connection): ResponseEntity<Unit> {
        Audit.ApplicationsDeleteDrafts.log()
        db.transaction { removeOldDrafts(it, attachmentsController::deleteAttachment) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/attendance-upkeep")
    fun endOfDayAttendanceUpkeep(db: Database.Connection): ResponseEntity<Unit> {
        db.transaction {
            it.createUpdate(
                // language=SQL
                """
                    UPDATE child_attendance ca
                    SET departed = ((arrived AT TIME ZONE 'Europe/Helsinki')::date + time '23:59') AT TIME ZONE 'Europe/Helsinki'
                    WHERE departed IS NULL
                """.trimIndent()
            ).execute()
        }
        return ResponseEntity.noContent().build()
    }
}
