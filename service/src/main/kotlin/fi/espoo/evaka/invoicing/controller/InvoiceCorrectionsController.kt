// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.psqlCause
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.JdbiException
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
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
        @PathVariable personId: PersonId
    ): List<InvoiceCorrection> {
        Audit.InvoiceCorrectionsRead.log(targetId = personId)
        accessControl.requirePermissionFor(user, Action.Person.READ_INVOICE_CORRECTIONS, personId)
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.createQuery(
                    """
SELECT id, head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note,
    EXISTS (SELECT 1 FROM invoice i JOIN invoice_row r ON i.status != 'DRAFT'::invoice_status AND r.correction_id = invoice_correction.id) AS partially_invoiced
FROM invoice_correction WHERE head_of_family_id = :personId
"""
                )
                    .bind("personId", personId)
                    .mapTo<InvoiceCorrection>()
                    .toList()
            }
        }
    }

    @PostMapping
    fun createInvoiceCorrection(
        db: Database,
        user: AuthenticatedUser.Employee,
        @RequestBody body: NewInvoiceCorrection
    ) {
        Audit.InvoiceCorrectionsCreate.log(targetId = body.headOfFamilyId, objectId = body.childId)
        accessControl.requirePermissionFor(user, Action.Person.CREATE_INVOICE_CORRECTION, body.headOfFamilyId)
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.createUpdate(
                    """
INSERT INTO invoice_correction (head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note)
VALUES (:headOfFamilyId, :childId, :unitId, :product, :period, :amount, :unitPrice, :description, :note)
"""
                )
                    .bindKotlin(body)
                    .execute()
            }
        }
    }

    @DeleteMapping("/{id}")
    fun deleteInvoiceCorrection(
        db: Database,
        user: AuthenticatedUser.Employee,
        @PathVariable id: InvoiceCorrectionId
    ) {
        Audit.InvoiceCorrectionsDelete.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.InvoiceCorrection.DELETE, id)
        db.connect { dbc ->
            dbc.transaction { tx ->
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
                        PSQLState.FOREIGN_KEY_VIOLATION.state -> throw BadRequest("Cannot delete an already invoiced correction")
                        else -> throw e
                    }
                }
            }
        }
    }

    data class NoteUpdateBody(val note: String)
    @PostMapping("/{id}/note")
    fun updateNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        @PathVariable id: InvoiceCorrectionId,
        @RequestBody body: NoteUpdateBody
    ) {
        Audit.InvoiceCorrectionsNoteUpdate.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.InvoiceCorrection.UPDATE_NOTE, id)
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.createUpdate("UPDATE invoice_correction SET note = :note WHERE id = :id")
                    .bind("id", id,)
                    .bind("note", body.note)
                    .execute()
            }
        }
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
    val partiallyInvoiced: Boolean
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
