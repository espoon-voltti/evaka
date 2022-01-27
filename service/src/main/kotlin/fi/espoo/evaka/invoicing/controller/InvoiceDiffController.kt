// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.service.InvoiceGeneratorDiff
import fi.espoo.evaka.invoicing.service.InvoiceGeneratorDiffer
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

@RestController
@RequestMapping("/invoice-diff")
class InvoiceDiffController(
    private val generatorDiffer: InvoiceGeneratorDiffer,
    private val accessControl: AccessControl
) {
    // For generating a diff between draft invoices generated with current InvoiceGenerator
    // vs generated with new experimental NewInvoiceGenerator
    @PostMapping("/debug-diff")
    fun createDraftInvoicesDebugDiff(db: Database, user: AuthenticatedUser, @RequestBody body: InvoiceDebugDiffRequest): InvoiceGeneratorDiff {
        Audit.InvoicesDebugDiff.log()
        accessControl.requirePermissionFor(user, Action.Global.CREATE_DRAFT_INVOICES_DEBUG_DIFF)
        return db.connect { dbc -> dbc.transaction { generatorDiffer.createInvoiceGeneratorDiff(it, DateRange(body.startDate, body.endDate)) } }
    }

    data class InvoiceDebugDiffRequest(
        val startDate: LocalDate,
        val endDate: LocalDate
    )
}
