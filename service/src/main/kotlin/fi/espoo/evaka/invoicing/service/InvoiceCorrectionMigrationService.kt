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
import java.time.YearMonth
import org.springframework.stereotype.Service

@Service
class InvoiceCorrectionMigrationService(asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    init {
        asyncJobRunner.registerHandler<AsyncJob.InvoiceCorrectionMigration> { db, clock, msg ->
            db.transaction { tx -> migrateInvoiceCorrection(tx, clock, msg.invoiceCorrectionId) }
        }
    }
}

fun migrateInvoiceCorrections(tx: Database.Transaction, nextMonth: YearMonth) {
    tx.execute {
        sql(
            """
        WITH applied_corrections AS (
            SELECT ic.id AS original_correction_id, ext.uuid_generate_v1mc() AS new_correction_id, ir.id AS invoice_row_id, ic.created, ic.head_of_family_id, ic.child_id, ic.unit_id, ic.product, ic.period, ir.amount, ir.unit_price, ic.description, ic.note, i.period_start AS target_month
            FROM invoice_correction ic 
            JOIN invoice_row ir ON ir.correction_id = ic.id
            JOIN invoice i ON ir.invoice_id = i.id
        ), inserted_corrections AS (
            INSERT INTO invoice_correction (id, created, head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note, applied_completely, target_month) 
            SELECT ac.new_correction_id, ac.created, ac.head_of_family_id, ac.child_id, ac.unit_id, ac.product, ac.period, ac.amount, ac.unit_price, ac.description, ac.note, FALSE, ac.target_month
            FROM applied_corrections ac
        ), updated_invoice_rows AS (
            UPDATE invoice_row ir
            SET correction_id = ac.new_correction_id
            FROM applied_corrections ac
            WHERE ir.id = ac.invoice_row_id
            RETURNING ir.*
        ), remaining_corrections AS (
            SELECT ic.id, ic.amount * ic.unit_price - SUM(coalesce(ir.amount, 0) * coalesce(ir.unit_price, 0)) AS remaining_correction
            FROM invoice_correction ic
            LEFT JOIN applied_corrections ac ON ac.original_correction_id = ic.id
            LEFT JOIN updated_invoice_rows ir ON ir.correction_id = ac.new_correction_id
            WHERE ic.target_month IS NULL
            GROUP BY ic.id
        )
        UPDATE invoice_correction ic
        SET 
            amount = CASE WHEN rc.remaining_correction % ic.unit_price = 0 THEN rc.remaining_correction / ic.unit_price ELSE ic.amount END,
            unit_price = CASE WHEN rc.remaining_correction % ic.unit_price = 0 THEN ic.unit_price ELSE rc.remaining_correction / ic.amount END,
            target_month = make_date(
                EXTRACT(YEAR FROM ${bind(nextMonth.atDay(1))})::int, 
                EXTRACT(MONTH FROM ${bind(nextMonth.atDay(1))})::int, 
                1
            ),
            applied_completely = FALSE
        FROM remaining_corrections rc
        WHERE ic.id = rc.id AND rc.remaining_correction != 0;
    """
        )
    }

    tx.execute {
        sql(
            """
        DELETE FROM invoice_correction
        WHERE target_month IS NULL;
    """
        )
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
        WHERE id = ${bind(invoiceCorrectionId)} AND target_month IS NULL 
    """
                )
            }
            .exactlyOneOrNull<Correction>() ?: return // already migrated or not found

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
            FROM inserted
            WHERE ir.id = ${bind(row.id)}
        """
            )
        }
    }

    val remainingCorrection =
        (correction.amount * correction.unitPrice) - invoiceRows.sumOf { it.unitPrice * it.amount }

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
                amount = ${bind(remainingAmount)},
                unit_price = ${bind(remainingUnitPrice)}, 
                target_month = ${bind(nextTargetMonth)}, 
                applied_completely = FALSE
            WHERE id = ${bind(invoiceCorrectionId)}
        """
            )
        }
    }
}
