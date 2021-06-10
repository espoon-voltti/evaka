// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/varda-dev")
class VardaDevController(
    private val vardaUpdateService: VardaUpdateService
) {
    @DeleteMapping("/child/{vardaChildId}")
    fun deleteChild(
        db: Database.Connection,
        @PathVariable vardaChildId: Long
    ): ResponseEntity<Unit> {
        if (listOf("dev", "test", "staging").contains(System.getenv("VOLTTI_ENV"))) {
            vardaUpdateService.deleteFeeDataByChild(vardaChildId, db)
            vardaUpdateService.deletePlacementsByChild(vardaChildId, db)
            vardaUpdateService.deleteDecisionsByChild(vardaChildId, db)
            vardaUpdateService.deleteChild(vardaChildId, db)
            return ResponseEntity.ok().build()
        }
        return ResponseEntity.notFound().build()
    }
}
