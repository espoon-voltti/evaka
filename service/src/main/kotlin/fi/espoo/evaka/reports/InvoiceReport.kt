// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.invoicing.data.searchInvoices
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.addressUsable
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.domain.Period
import org.jdbi.v3.core.Jdbi
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

internal fun getMonthPeriod(date: LocalDate): Period {
    val from = date.with(TemporalAdjusters.firstDayOfMonth())
    val to = date.with(TemporalAdjusters.lastDayOfMonth())
    return Period(from, to)
}

@RestController
class InvoiceReportController(private val jdbi: Jdbi) {
    @GetMapping("/reports/invoices")
    fun getInvoiceReport(
        user: AuthenticatedUser,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<InvoiceReport> {
        Audit.InvoicesReportRead.log()
        user.requireOneOfRoles(Roles.FINANCE_ADMIN, Roles.DIRECTOR, Roles.ADMIN)
        return getInvoiceReportWithRows(
            getMonthPeriod(date)
        ).let(::ok)
    }

    fun getInvoiceReportWithRows(period: Period): InvoiceReport {
        val rows = jdbi.handle { h ->
            val invoices = searchInvoices(
                h,
                statuses = listOf(InvoiceStatus.SENT),
                sentAt = period
            )

            invoices.groupBy { it.agreementType }
                .map { (agreementType, invoices) ->
                    InvoiceReportRow(
                        areaCode = agreementType,
                        amountOfInvoices = invoices.size,
                        totalSumCents = invoices.sumBy { it.totalPrice() },
                        amountWithoutSSN = invoices.count { it.headOfFamily.ssn.isNullOrBlank() },
                        amountWithoutAddress = invoices.count { withoutAddress(it) },
                        amountWithZeroPrice = invoices.count { it.totalPrice() == 0 }
                    )
                }
                .sortedBy { it.areaCode }
        }

        return InvoiceReport(reportRows = rows)
    }
}

fun withoutAddress(invoice: InvoiceDetailed): Boolean = !addressUsable(
    invoice.headOfFamily.streetAddress,
    invoice.headOfFamily.postalCode,
    invoice.headOfFamily.postOffice
) && !addressUsable(
    invoice.headOfFamily.invoicingStreetAddress,
    invoice.headOfFamily.invoicingPostalCode,
    invoice.headOfFamily.invoicingPostOffice
)

data class InvoiceReportRow(
    val areaCode: Int,
    val amountOfInvoices: Int,
    val totalSumCents: Int,
    val amountWithoutSSN: Int,
    val amountWithoutAddress: Int,
    val amountWithZeroPrice: Int
)

data class InvoiceReport(
    val reportRows: List<InvoiceReportRow>
) {
    val totalAmountOfInvoices: Int = reportRows.sumBy { it.amountOfInvoices }
    val totalSumCents: Int = reportRows.sumBy { it.totalSumCents }
    val totalAmountWithoutSSN: Int = reportRows.sumBy { it.amountWithoutSSN }
    val totalAmountWithoutAddress: Int = reportRows.sumBy { it.amountWithoutAddress }
    val totalAmountWithZeroPrice: Int = reportRows.sumBy { it.amountWithZeroPrice }
}
