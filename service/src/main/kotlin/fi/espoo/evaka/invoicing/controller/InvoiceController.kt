// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.data.deleteDraftInvoices
import fi.espoo.evaka.invoicing.data.getDetailedInvoice
import fi.espoo.evaka.invoicing.data.getHeadOfFamilyInvoices
import fi.espoo.evaka.invoicing.data.paginatedSearch
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.InvoiceSummary
import fi.espoo.evaka.invoicing.service.InvoiceCodes
import fi.espoo.evaka.invoicing.service.InvoiceService
import fi.espoo.evaka.invoicing.service.createAllDraftInvoices
import fi.espoo.evaka.invoicing.service.getInvoiceCodes
import fi.espoo.evaka.invoicing.service.markManuallySent
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.utils.parseEnum
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class InvoiceDistinctiveParams {
    MISSING_ADDRESS
}

enum class InvoiceSortParam {
    HEAD_OF_FAMILY,
    CHILDREN,
    START,
    END,
    SUM,
    STATUS,
    CREATED_AT
}

@RestController
@RequestMapping("/invoices")
class InvoiceController(
    private val service: InvoiceService
) {
    @PostMapping("/search")
    fun searchInvoices(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: SearchInvoicesRequest
    ): ResponseEntity<Paged<InvoiceSummary>> {
        Audit.InvoicesSearch.log()
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)
        val maxPageSize = 5000
        if (body.pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        return db.connect { dbc ->
            dbc
                .read { tx ->
                    tx.paginatedSearch(
                        body.page,
                        body.pageSize,
                        body.sortBy ?: InvoiceSortParam.STATUS,
                        body.sortDirection ?: SortDirection.DESC,
                        body.status?.split(",")?.mapNotNull { parseEnum<InvoiceStatus>(it) } ?: listOf(),
                        body.area?.split(",") ?: listOf(),
                        body.unit?.let { parseUUID(it) },
                        body.distinctions?.split(",")?.mapNotNull { parseEnum<InvoiceDistinctiveParams>(it) } ?: listOf(),
                        body.searchTerms ?: "",
                        body.periodStart?.let { LocalDate.parse(body.periodStart, DateTimeFormatter.ISO_DATE) },
                        body.periodEnd?.let { LocalDate.parse(body.periodEnd, DateTimeFormatter.ISO_DATE) }
                    )
                }
        }
            .let { ResponseEntity.ok(it) }
    }

    @PostMapping("/create-drafts")
    fun createDraftInvoices(db: Database, user: AuthenticatedUser): ResponseEntity<Unit> {
        Audit.InvoicesCreate.log()
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)
        db.connect { dbc -> dbc.transaction { it.createAllDraftInvoices() } }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/delete-drafts")
    fun deleteDraftInvoices(db: Database, user: AuthenticatedUser, @RequestBody invoiceIds: List<UUID>): ResponseEntity<Unit> {
        Audit.InvoicesDeleteDrafts.log(targetId = invoiceIds)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)
        db.connect { dbc -> dbc.transaction { it.deleteDraftInvoices(invoiceIds) } }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/send")
    fun sendInvoices(
        db: Database,
        user: AuthenticatedUser,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false) invoiceDate: LocalDate?,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false) dueDate: LocalDate?,
        @RequestBody invoiceIds: List<UUID>
    ): ResponseEntity<Unit> {
        Audit.InvoicesSend.log(targetId = invoiceIds)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)
        db.connect { dbc ->
            dbc.transaction {
                service.sendInvoices(it, user, invoiceIds, invoiceDate, dueDate)
            }
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/send/by-date")
    fun sendInvoicesByDate(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody payload: InvoicePayload
    ): ResponseEntity<Unit> {
        Audit.InvoicesSendByDate.log()
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)
        db.connect { dbc ->
            dbc.transaction { tx ->
                val invoiceIds = service.getInvoiceIds(tx, payload.from, payload.to, payload.areas)
                service.sendInvoices(tx, user, invoiceIds, payload.invoiceDate, payload.dueDate)
            }
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/mark-sent")
    fun markInvoicesSent(db: Database, user: AuthenticatedUser, @RequestBody invoiceIds: List<UUID>): ResponseEntity<Unit> {
        Audit.InvoicesMarkSent.log(targetId = invoiceIds)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)
        db.connect { dbc -> dbc.transaction { it.markManuallySent(user, invoiceIds) } }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{uuid}")
    fun getInvoice(db: Database, user: AuthenticatedUser, @PathVariable uuid: String): ResponseEntity<Wrapper<InvoiceDetailed>> {
        Audit.InvoicesRead.log(targetId = uuid)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)
        val parsedUuid = parseUUID(uuid)
        val res = db.connect { dbc -> dbc.read { it.getDetailedInvoice(parsedUuid) } }
            ?: throw NotFound("No invoice found with given ID ($uuid)")
        return ResponseEntity.ok(Wrapper(res))
    }

    @GetMapping("/head-of-family/{uuid}")
    fun getHeadOfFamilyInvoices(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable uuid: String
    ): ResponseEntity<Wrapper<List<Invoice>>> {
        Audit.InvoicesRead.log(targetId = uuid)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)
        val parsedUuid = parseUUID(uuid)
        val res = db.connect { dbc -> dbc.read { it.getHeadOfFamilyInvoices(parsedUuid) } }
        return ResponseEntity.ok(Wrapper(res))
    }

    @PutMapping("/{uuid}")
    fun putInvoice(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable uuid: String,
        @RequestBody invoice: Invoice
    ): ResponseEntity<Unit> {
        Audit.InvoicesUpdate.log(targetId = uuid)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)
        val parsedUuid = parseUUID(uuid)
        db.connect { dbc -> dbc.transaction { service.updateInvoice(it, parsedUuid, invoice) } }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/codes")
    fun getInvoiceCodes(db: Database, user: AuthenticatedUser): ResponseEntity<Wrapper<InvoiceCodes>> {
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)
        val codes = db.connect { dbc -> dbc.read { it.getInvoiceCodes() } }
        return ResponseEntity.ok(Wrapper(codes))
    }
}

data class InvoicePayload(val from: LocalDate, val to: LocalDate, val areas: List<String>, val invoiceDate: LocalDate?, val dueDate: LocalDate?)

data class SearchInvoicesRequest(
    val page: Int,
    val pageSize: Int,
    val sortBy: InvoiceSortParam?,
    val sortDirection: SortDirection?,
    val status: String?,
    val area: String?,
    val unit: String?,
    val distinctions: String?,
    val searchTerms: String?,
    val periodStart: String?,
    val periodEnd: String?
)
