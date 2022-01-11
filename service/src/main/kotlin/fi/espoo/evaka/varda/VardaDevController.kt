// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/varda-dev")
class VardaDevController(
    private val vardaUpdateService: VardaUpdateService,
    private val vardaResetService: VardaResetService
) {
    @PostMapping("/run-update-all")
    fun runFullVardaUpdate(
        db: Database
    ) {
        db.connect { dbc -> vardaUpdateService.startVardaUpdate(dbc) }
    }

    @PostMapping("/reset-children")
    fun resetChildren(
        db: Database,
        @RequestParam(defaultValue = "true") addNewChildren: Boolean,
        @RequestParam(defaultValue = "1000") limit: Int,
    ) {
        db.connect { dbc -> vardaResetService.planVardaReset(dbc, addNewChildren = addNewChildren, maxChildren = limit) }
    }

    @PostMapping("/reset-by-report/{organizerId}")
    fun resetChildrenByReport(
        db: Database,
        @PathVariable organizerId: Long,
        @RequestParam(defaultValue = "1000") limit: Int,
    ) {
        db.connect { dbc -> vardaResetService.planResetByVardaChildrenErrorReport(dbc, organizerId = organizerId, limit = limit) }
    }

    @PostMapping("/mark-child-for-reset/{childId}")
    fun markChildForVardaReset(
        db: Database,
        @PathVariable childId: ChildId,
    ) {
        db.connect { dbc -> dbc.transaction { it.resetChildResetTimestamp(childId) } }
    }
}
