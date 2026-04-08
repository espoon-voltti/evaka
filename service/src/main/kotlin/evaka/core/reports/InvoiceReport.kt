// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.Audit
import evaka.core.invoicing.data.searchInvoices
import evaka.core.invoicing.domain.InvoiceDetailed
import evaka.core.invoicing.domain.InvoiceStatus
import evaka.core.invoicing.domain.addressUsable
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.YearMonth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class InvoiceReportController(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/invoices")
    fun getInvoiceReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam yearMonth: YearMonth,
    ): InvoiceReport {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_INVOICE_REPORT,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getInvoiceReportWithRows(FiniteDateRange.ofMonth(yearMonth))
                }
            }
            .also {
                Audit.InvoicesReportRead.log(
                    meta = mapOf("yearMonth" to yearMonth, "count" to it.reportRows.size)
                )
            }
    }
}

private fun Database.Read.getInvoiceReportWithRows(period: FiniteDateRange): InvoiceReport {
    val invoices = searchInvoices(InvoiceStatus.SENT, period = period)

    val rows =
        invoices
            .groupBy { it.agreementType }
            .map { (agreementType, invoices) ->
                InvoiceReportRow(
                    areaCode = agreementType,
                    amountOfInvoices = invoices.size,
                    totalSumCents = invoices.sumOf { it.totalPrice },
                    amountWithoutSSN = invoices.count { it.headOfFamily.ssn.isNullOrBlank() },
                    amountWithoutAddress = invoices.count { withoutAddress(it) },
                    amountWithZeroPrice = invoices.count { it.totalPrice == 0 },
                )
            }
            .sortedBy { it.areaCode }

    return InvoiceReport(reportRows = rows)
}

private fun withoutAddress(invoice: InvoiceDetailed): Boolean =
    !addressUsable(
        invoice.headOfFamily.streetAddress,
        invoice.headOfFamily.postalCode,
        invoice.headOfFamily.postOffice,
    ) &&
        !addressUsable(
            invoice.headOfFamily.invoicingStreetAddress,
            invoice.headOfFamily.invoicingPostalCode,
            invoice.headOfFamily.invoicingPostOffice,
        )

data class InvoiceReportRow(
    val areaCode: Int?,
    val amountOfInvoices: Int,
    val totalSumCents: Int,
    val amountWithoutSSN: Int,
    val amountWithoutAddress: Int,
    val amountWithZeroPrice: Int,
)

data class InvoiceReport(val reportRows: List<InvoiceReportRow>) {
    val totalAmountOfInvoices: Int = reportRows.sumOf { it.amountOfInvoices }
    val totalSumCents: Int = reportRows.sumOf { it.totalSumCents }
    val totalAmountWithoutSSN: Int = reportRows.sumOf { it.amountWithoutSSN }
    val totalAmountWithoutAddress: Int = reportRows.sumOf { it.amountWithoutAddress }
    val totalAmountWithZeroPrice: Int = reportRows.sumOf { it.amountWithZeroPrice }
}
