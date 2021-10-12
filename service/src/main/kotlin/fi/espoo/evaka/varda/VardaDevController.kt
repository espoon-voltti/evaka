// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/varda-dev")
class VardaDevController(
    private val vardaUpdateService: VardaUpdateService,
) {
    @PostMapping("/run-update-all")
    fun runFullVardaUpdate(
        db: Database.Connection
    ): ResponseEntity<Unit> {
        vardaUpdateService.startVardaUpdate(db)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/reset-children")
    fun resetChildren(
        db: Database.Connection,
        @RequestParam(defaultValue = "true") addNewChildren: Boolean,
        @RequestParam(defaultValue = "1000") limit: Int,
    ) {
        vardaUpdateService.planVardaReset(db, addNewChildren = addNewChildren, maxChildren = limit)
    }

    @PostMapping("/reset-by-report/{organizerId}")
    fun resetChildrenByReport(
        db: Database.Connection,
        @PathVariable organizerId: Long,
        @RequestParam(defaultValue = "1000") limit: Int,
    ) {
        vardaUpdateService.resetByVardaChildrenErrorReport(db, organizerId = organizerId, limit = limit)
    }
}
