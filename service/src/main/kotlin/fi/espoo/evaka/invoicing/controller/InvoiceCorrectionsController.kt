// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.psqlCause
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
@RequestMapping("/invoice-corrections")
class InvoiceCorrectionsController(private val accessControl: AccessControl) {
    @GetMapping("/{personId}")
    fun getPersonInvoiceCorrections(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable personId: PersonId
    ): List<InvoiceCorrectionWithPermittedActions> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_INVOICE_CORRECTIONS,
                        personId
                    )
                    val invoiceCorrections =
                        tx.createQuery(
                                """
SELECT c.id, c.head_of_family_id, c.child_id, c.unit_id, c.product, c.period, c.amount, c.unit_price, c.description, c.note,
    i.id AS invoice_id,
    i.status AS invoice_status
FROM invoice_correction c
LEFT JOIN LATERAL (
    SELECT i.id, i.status
    FROM invoice_row r
    JOIN invoice i ON r.invoice_id = i.id
    WHERE c.id = r.correction_id
    ORDER BY CASE WHEN i.status = 'DRAFT'::invoice_status THEN 2 ELSE 1 END ASC, i.period_start DESC
    LIMIT 1
) i ON true
WHERE c.head_of_family_id = :personId AND NOT applied_completely
"""
                            )
                            .bind("personId", personId)
                            .toList<InvoiceCorrection>()
                    val permittedActions =
                        accessControl.getPermittedActions<
                            InvoiceCorrectionId,
                            Action.InvoiceCorrection
                        >(
                            tx,
                            user,
                            clock,
                            invoiceCorrections.map { it.id }
                        )
                    invoiceCorrections.map {
                        InvoiceCorrectionWithPermittedActions(
                            it,
                            permittedActions[it.id] ?: emptySet()
                        )
                    }
                }
            }
            .also {
                Audit.InvoiceCorrectionsRead.log(
                    targetId = personId,
                    meta = mapOf("count" to it.size)
                )
            }
    }

    data class InvoiceCorrectionWithPermittedActions(
        val data: InvoiceCorrection,
        val permittedActions: Set<Action.InvoiceCorrection>
    )

    @PostMapping
    fun createInvoiceCorrection(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: NewInvoiceCorrection
    ) {
        val invoiceCorrectionId =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.CREATE_INVOICE_CORRECTION,
                        body.headOfFamilyId
                    )
                    tx.createUpdate(
                            """
INSERT INTO invoice_correction (head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note)
VALUES (:headOfFamilyId, :childId, :unitId, :product, :period, :amount, :unitPrice, :description, :note)
RETURNING id
"""
                        )
                        .bindKotlin(body)
                        .executeAndReturnGeneratedKeys()
                        .exactlyOne<InvoiceCorrectionId>()
                }
            }
        Audit.InvoiceCorrectionsCreate.log(
            targetId = listOf(body.headOfFamilyId, body.childId),
            objectId = invoiceCorrectionId
        )
    }

    @DeleteMapping("/{id}")
    fun deleteInvoiceCorrection(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: InvoiceCorrectionId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.InvoiceCorrection.DELETE,
                    id
                )
                try {
                    tx.createUpdate(
                            """
WITH deleted_invoice_row AS (
    DELETE FROM invoice_row r USING invoice i WHERE r.correction_id = :id AND r.invoice_id = i.id AND i.status = 'DRAFT'
)
DELETE FROM invoice_correction WHERE id = :id RETURNING id
"""
                        )
                        .bind("id", id)
                        .execute()
                } catch (e: JdbiException) {
                    when (e.psqlCause()?.sqlState) {
                        PSQLState.FOREIGN_KEY_VIOLATION.state ->
                            throw BadRequest(
                                "Cannot delete an already invoiced correction",
                                cause = e
                            )
                        else -> throw e
                    }
                }
            }
        }
        Audit.InvoiceCorrectionsDelete.log(targetId = id)
    }

    data class NoteUpdateBody(val note: String)

    @PostMapping("/{id}/note")
    fun updateNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: InvoiceCorrectionId,
        @RequestBody body: NoteUpdateBody
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.InvoiceCorrection.UPDATE_NOTE,
                    id
                )
                tx.createUpdate("UPDATE invoice_correction SET note = :note WHERE id = :id")
                    .bind("id", id)
                    .bind("note", body.note)
                    .execute()
            }
        }
        Audit.InvoiceCorrectionsNoteUpdate.log(targetId = id)
    }
}

data class InvoiceCorrection(
    val id: InvoiceCorrectionId,
    val headOfFamilyId: PersonId,
    val childId: ChildId,
    val unitId: DaycareId,
    val product: ProductKey,
    val period: FiniteDateRange,
    val amount: Int,
    val unitPrice: Int,
    val description: String,
    val note: String,
    val invoiceId: InvoiceId?,
    val invoiceStatus: InvoiceStatus?
)

data class NewInvoiceCorrection(
    val headOfFamilyId: PersonId,
    val childId: ChildId,
    val unitId: DaycareId,
    val product: ProductKey,
    val period: FiniteDateRange,
    val amount: Int,
    val unitPrice: Int,
    val description: String,
    val note: String
)
