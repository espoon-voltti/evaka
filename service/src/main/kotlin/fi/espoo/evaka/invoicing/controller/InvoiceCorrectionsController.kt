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
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
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
SELECT id, head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note
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
    val note: String
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
