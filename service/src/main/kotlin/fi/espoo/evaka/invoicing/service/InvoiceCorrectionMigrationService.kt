// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.LocalDate
import org.springframework.stereotype.Service

@Service
class InvoiceCorrectionMigrationService(asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    init {
        asyncJobRunner.registerHandler<AsyncJob.InvoiceCorrectionMigration> { db, clock, msg ->
            db.transaction { tx -> migrateInvoiceCorrection(tx, clock, msg.invoiceCorrectionId) }
        }
    }
}

fun migrateInvoiceCorrection(
    tx: Database.Transaction,
    clock: EvakaClock,
    invoiceCorrectionId: InvoiceCorrectionId,
) {
    data class Correction(val amount: Int, val unitPrice: Int)
    val correction =
        tx.createQuery {
                sql(
                    """
        SELECT amount, unit_price 
        FROM invoice_correction
        WHERE id = ${bind(invoiceCorrectionId)}
    """
                )
            }
            .exactlyOne<Correction>()
    val correctionTotal = correction.amount * correction.unitPrice

    data class InvoiceRow(
        val id: InvoiceRowId,
        val amount: Int,
        val unitPrice: Int,
        val periodStart: LocalDate,
    )
    val invoiceRows =
        tx.createQuery {
                sql(
                    """
        SELECT ir.id, ir.amount, ir.unit_price, i.period_start
        FROM invoice_row ir
        JOIN invoice i ON ir.invoice_id = i.id
        WHERE correction_id = ${bind(invoiceCorrectionId)}
    """
                )
            }
            .toList<InvoiceRow>()

    invoiceRows.forEach { row ->
        tx.execute {
            sql(
                """
            WITH inserted AS (
                INSERT INTO invoice_correction (created, updated, head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note, applied_completely, target_month) 
                SELECT created, updated, head_of_family_id, child_id, unit_id, product, period, ${row.amount}, ${row.unitPrice}, description, note, FALSE, ${bind(row.periodStart.withDayOfMonth(1))}
                FROM invoice_correction
                WHERE id = ${bind(invoiceCorrectionId)}
                RETURNING id
            )
            UPDATE invoice_row ir
            SET correction_id = inserted.id
            WHERE ir.id = ${bind(row.id)}
        """
            )
        }
    }

    val remainingCorrection = correctionTotal - invoiceRows.sumOf { it.unitPrice * it.amount }

    if (remainingCorrection == 0) {
        tx.execute {
            sql(
                """
            DELETE FROM invoice_correction
            WHERE id = ${bind(invoiceCorrectionId)}
        """
            )
        }
    } else {
        val nextTargetMonth = clock.today().plusMonths(1).withDayOfMonth(1)
        val (remainingAmount, remainingUnitPrice) =
            if (remainingCorrection % correction.unitPrice == 0) {
                (remainingCorrection / correction.unitPrice) to correction.unitPrice
            } else {
                1 to remainingCorrection
            }
        tx.execute {
            sql(
                """
            UPDATE invoice_correction
            SET 
                amount = ${remainingAmount}, 
                unit_price = ${bind(remainingUnitPrice)}, 
                target_month = ${bind(nextTargetMonth)}, 
                applied_completely = FALSE
            WHERE id = ${bind(invoiceCorrectionId)}
        """
            )
        }
    }
}
