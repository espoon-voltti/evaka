// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/varda")
class VardaController(private val vardaService: VardaService) {
    @PostMapping("/run-update-all")
    fun runFullVardaUpdate(db: Database, clock: EvakaClock) {
        db.connect { dbc -> vardaService.startVardaUpdate(dbc, clock) }
    }

    @PostMapping("/mark-child-for-reset/{childId}")
    fun markChildForVardaReset(db: Database, @PathVariable childId: ChildId) {
        db.connect { dbc -> dbc.transaction { it.resetChildResetTimestamp(childId) } }
    }
}
