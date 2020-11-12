// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.dvv.DvvModificationsBatchRefreshService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/scheduled")
class ScheduledOperationController(
    val dvvModificationsBatchRefreshService: DvvModificationsBatchRefreshService
) {

    @PostMapping("/batch-refresh/dvv")
    fun batchRefreshDvv(user: AuthenticatedUser): ResponseEntity<Int> {
        Audit.VtjBatchSchedule.log()
        return ResponseEntity.ok(dvvModificationsBatchRefreshService.scheduleBatch())
    }
}
