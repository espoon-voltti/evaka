// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.controller

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.invoicing.service.InvoiceCorrection
import evaka.core.invoicing.service.InvoiceCorrectionInsert
import evaka.core.invoicing.service.getInvoiceCorrectionsForHeadOfFamily
import evaka.core.invoicing.service.insertInvoiceCorrection
import evaka.core.shared.InvoiceCorrectionId
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.db.psqlCause
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.jdbi.v3.core.JdbiException
import org.postgresql.util.PSQLState
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/invoice-corrections")
class InvoiceCorrectionsController(private val accessControl: AccessControl) {
    @GetMapping("/{personId}")
    fun getPersonInvoiceCorrections(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable personId: PersonId,
    ): List<InvoiceCorrectionWithPermittedActions> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_INVOICE_CORRECTIONS,
                        personId,
                    )
                    val invoiceCorrections = tx.getInvoiceCorrectionsForHeadOfFamily(personId)
                    val permittedActions =
                        accessControl.getPermittedActions<
                            InvoiceCorrectionId,
                            Action.InvoiceCorrection,
                        >(
                            tx,
                            user,
                            clock,
                            invoiceCorrections.map { it.id },
                        )
                    invoiceCorrections.map {
                        InvoiceCorrectionWithPermittedActions(
                            it,
                            permittedActions[it.id] ?: emptySet(),
                        )
                    }
                }
            }
            .also {
                Audit.InvoiceCorrectionsRead.log(
                    targetId = AuditId(personId),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    data class InvoiceCorrectionWithPermittedActions(
        val data: InvoiceCorrection,
        val permittedActions: Set<Action.InvoiceCorrection>,
    )

    @PostMapping
    fun createInvoiceCorrection(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: InvoiceCorrectionInsert,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.CREATE_INVOICE_CORRECTION,
                        body.headOfFamilyId,
                    )
                    tx.insertInvoiceCorrection(body, user.evakaUserId, clock.now())
                }
            }
            .also { invoiceCorrectionId ->
                Audit.InvoiceCorrectionsCreate.log(
                    targetId = AuditId(listOf(body.headOfFamilyId, body.childId)),
                    objectId = AuditId(invoiceCorrectionId),
                )
            }
    }

    @DeleteMapping("/{id}")
    fun deleteInvoiceCorrection(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: InvoiceCorrectionId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.InvoiceCorrection.DELETE,
                    id,
                )
                try {
                    tx.execute {
                        sql(
                            """
WITH deleted_invoice_row AS (
    DELETE FROM invoice_row r USING invoice i WHERE r.correction_id = ${bind(id)} AND r.invoice_id = i.id AND i.status = 'DRAFT'
)
DELETE FROM invoice_correction
WHERE id = ${bind(id)}
"""
                        )
                    }
                } catch (e: JdbiException) {
                    when (e.psqlCause()?.sqlState) {
                        PSQLState.FOREIGN_KEY_VIOLATION.state -> {
                            throw BadRequest(
                                "Cannot delete an already invoiced correction",
                                cause = e,
                            )
                        }

                        else -> {
                            throw e
                        }
                    }
                }
            }
        }
        Audit.InvoiceCorrectionsDelete.log(targetId = AuditId(id))
    }

    data class NoteUpdateBody(val note: String)

    @PostMapping("/{id}/note")
    fun updateInvoiceCorrectionNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: InvoiceCorrectionId,
        @RequestBody body: NoteUpdateBody,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.InvoiceCorrection.UPDATE_NOTE,
                    id,
                )
                tx.createUpdate {
                        sql(
                            "UPDATE invoice_correction SET note = ${bind(body.note)} WHERE id = ${bind(id)}"
                        )
                    }
                    .execute()
            }
        }
        Audit.InvoiceCorrectionsNoteUpdate.log(targetId = AuditId(id))
    }
}
