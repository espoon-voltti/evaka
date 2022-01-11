// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.messaging.planFinanceDecisionGeneration
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class GenerateDecisionsBody(
    val starting: String,
    val targetHeads: List<PersonId?>
)

@RestController
@RequestMapping("/fee-decision-generator")
class FeeDecisionGeneratorController(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    @PostMapping("/generate")
    fun generateDecisions(db: Database, user: AuthenticatedUser, @RequestBody data: GenerateDecisionsBody) {
        Audit.FeeDecisionGenerate.log(targetId = data.targetHeads)
        accessControl.requirePermissionFor(user, Action.Global.GENERATE_FEE_DECISIONS)
        db.connect { dbc ->
            generateAllStartingFrom(
                dbc,
                LocalDate.parse(data.starting, DateTimeFormatter.ISO_DATE),
                data.targetHeads.filterNotNull().distinct()
            )
        }
    }

    private fun generateAllStartingFrom(db: Database.Connection, starting: LocalDate, targetHeads: List<PersonId>) {
        db.transaction { planFinanceDecisionGeneration(it, asyncJobRunner, DateRange(starting, null), targetHeads) }
    }
}
