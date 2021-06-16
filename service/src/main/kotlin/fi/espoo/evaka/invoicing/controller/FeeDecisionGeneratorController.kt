// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.messaging.planFinanceDecisionGeneration
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class GenerateDecisionsBody(
    val starting: String,
    val targetHeads: List<UUID?>
)

@RestController
@RequestMapping("/fee-decision-generator")
class FeeDecisionGeneratorController(private val asyncJobRunner: AsyncJobRunner) {
    @PostMapping("/generate")
    fun generateDecisions(db: Database.Connection, user: AuthenticatedUser, @RequestBody data: GenerateDecisionsBody): ResponseEntity<Unit> {
        Audit.FeeDecisionGenerate.log(targetId = data.targetHeads)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        generateAllStartingFrom(
            db,
            LocalDate.parse(data.starting, DateTimeFormatter.ISO_DATE),
            data.targetHeads.filterNotNull().distinct()
        )
        return ResponseEntity.noContent().build()
    }

    private fun generateAllStartingFrom(db: Database.Connection, starting: LocalDate, targetHeads: List<UUID>) {
        db.transaction { planFinanceDecisionGeneration(it, asyncJobRunner, DateRange(starting, null), targetHeads) }
        asyncJobRunner.scheduleImmediateRun()
    }
}
