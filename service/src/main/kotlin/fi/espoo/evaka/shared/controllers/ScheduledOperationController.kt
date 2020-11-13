// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.removeOldDrafts
import fi.espoo.evaka.dvv.DvvModificationsBatchRefreshService
import fi.espoo.evaka.koski.KoskiUpdateService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.varda.VardaUpdateService
import org.jdbi.v3.core.Jdbi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/scheduled")
class ScheduledOperationController(
    private val jdbi: Jdbi,
    private val vardaUpdateService: VardaUpdateService,
    private val koskiUpdateService: KoskiUpdateService,
    private val dvvModificationsBatchRefreshService: DvvModificationsBatchRefreshService
) {

    @PostMapping("/dvv/update")
    fun dvvUpdate(user: AuthenticatedUser): ResponseEntity<Int> {
        Audit.VtjBatchSchedule.log()
        return ResponseEntity.ok(dvvModificationsBatchRefreshService.scheduleBatch())
    }

    @PostMapping("/koski/update")
    fun koskiUpdate(
        @RequestParam(required = false) personIds: String?,
        @RequestParam(required = false) daycareIds: String?
    ): ResponseEntity<Unit> {
        koskiUpdateService.update(personIds, daycareIds)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/varda/update")
    fun vardaUpdate(): ResponseEntity<Unit> {
        vardaUpdateService.updateAll()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/varda/units/update")
    fun vardaUpdateUnits(): ResponseEntity<Unit> {
        vardaUpdateService.updateUnits()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/application/clear-old-drafts")
    fun removeOldDraftApplications(): ResponseEntity<Unit> {
        Audit.ApplicationsDeleteDrafts.log()
        jdbi.transaction { removeOldDrafts(it) }
        return ResponseEntity.noContent().build()
    }
}
