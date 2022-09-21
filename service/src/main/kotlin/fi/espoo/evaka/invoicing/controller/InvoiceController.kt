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
import fi.espoo.evaka.invoicing.service.InvoiceGenerator
import fi.espoo.evaka.invoicing.service.InvoiceService
import fi.espoo.evaka.invoicing.service.markManuallySent
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.controllers.Wrapper
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

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
    private val service: InvoiceService,
    private val generator: InvoiceGenerator,
    private val accessControl: AccessControl
) {
    @PostMapping("/search")
    fun searchInvoices(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: SearchInvoicesRequest
    ): Paged<InvoiceSummaryResponse> {
        Audit.InvoicesSearch.log()
        accessControl.requirePermissionFor(user, clock, Action.Global.SEARCH_INVOICES)
        val maxPageSize = 5000
        if (body.pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        return db.connect { dbc ->
            dbc
                .read { tx ->
                    val paged = tx.paginatedSearch(
                        body.page,
                        body.pageSize,
                        body.sortBy ?: InvoiceSortParam.STATUS,
                        body.sortDirection ?: SortDirection.DESC,
                        body.status ?: emptyList(),
                        body.area ?: emptyList(),
                        body.unit,
                        body.distinctions ?: emptyList(),
                        body.searchTerms ?: "",
                        body.periodStart,
                        body.periodEnd,
                    )
                    val permittedActions = accessControl.getPermittedActions<InvoiceId, Action.Invoice>(
                        tx,
                        user,
                        clock,
                        paged.data.map { it.id }
                    )
                    Paged(
                        data = paged.data.map { InvoiceSummaryResponse(it, permittedActions[it.id] ?: emptySet()) },
                        total = paged.total,
                        pages = paged.pages,
                    )
                }
        }
    }

    data class InvoiceSummaryResponse(val data: InvoiceSummary, val permittedActions: Set<Action.Invoice>)

    @PostMapping("/create-drafts")
    fun createDraftInvoices(db: Database, user: AuthenticatedUser, clock: EvakaClock) {
        Audit.InvoicesCreate.log()
        accessControl.requirePermissionFor(user, clock, Action.Global.CREATE_DRAFT_INVOICES)
        db.connect { dbc -> dbc.transaction { generator.createAndStoreAllDraftInvoices(it) } }
    }

    @PostMapping("/delete-drafts")
    fun deleteDraftInvoices(db: Database, user: AuthenticatedUser, clock: EvakaClock, @RequestBody invoiceIds: List<InvoiceId>) {
        Audit.InvoicesDeleteDrafts.log(targetId = invoiceIds)
        accessControl.requirePermissionFor(user, clock, Action.Invoice.DELETE, invoiceIds)
        db.connect { dbc -> dbc.transaction { it.deleteDraftInvoices(invoiceIds) } }
    }

    @PostMapping("/send")
    fun sendInvoices(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false) invoiceDate: LocalDate?,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false) dueDate: LocalDate?,
        @RequestBody invoiceIds: List<InvoiceId>
    ) {
        Audit.InvoicesSend.log(targetId = invoiceIds)
        accessControl.requirePermissionFor(user, clock, Action.Invoice.SEND, invoiceIds)
        db.connect { dbc ->
            dbc.transaction {
                service.sendInvoices(it, user, clock, invoiceIds, invoiceDate, dueDate)
            }
        }
    }

    @PostMapping("/send/by-date")
    fun sendInvoicesByDate(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody payload: InvoicePayload
    ) {
        Audit.InvoicesSendByDate.log()
        db.connect { dbc ->
            dbc.transaction { tx ->
                val invoiceIds = service.getInvoiceIds(tx, payload.from, payload.to, payload.areas)
                accessControl.requirePermissionFor(user, clock, Action.Invoice.SEND, invoiceIds)
                service.sendInvoices(tx, user, clock, invoiceIds, payload.invoiceDate, payload.dueDate)
            }
        }
    }

    @PostMapping("/mark-sent")
    fun markInvoicesSent(db: Database, user: AuthenticatedUser, clock: EvakaClock, @RequestBody invoiceIds: List<InvoiceId>) {
        Audit.InvoicesMarkSent.log(targetId = invoiceIds)
        accessControl.requirePermissionFor(user, clock, Action.Invoice.UPDATE, invoiceIds)
        db.connect { dbc -> dbc.transaction { it.markManuallySent(user, clock.now(), invoiceIds) } }
    }

    @GetMapping("/{id}")
    fun getInvoice(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: InvoiceId
    ): Wrapper<InvoiceDetailedResponse> {
        Audit.InvoicesRead.log(targetId = id)
        accessControl.requirePermissionFor(user, clock, Action.Invoice.READ, id)
        val res = db.connect { dbc ->
            dbc.read { tx ->
                val invoice = tx.getDetailedInvoice(id) ?: throw NotFound("No invoice found with given ID ($id)")
                val permittedActions =
                    accessControl.getPermittedActions<InvoiceId, Action.Invoice>(tx, user, clock, invoice.id)
                InvoiceDetailedResponse(invoice, permittedActions)
            }
        }
        return Wrapper(res)
    }

    data class InvoiceDetailedResponse(
        val data: InvoiceDetailed,
        val permittedActions: Set<Action.Invoice>,
    )

    @GetMapping("/head-of-family/{uuid}")
    fun getHeadOfFamilyInvoices(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable uuid: PersonId
    ): Wrapper<List<Invoice>> {
        Audit.InvoicesRead.log(targetId = uuid)
        accessControl.requirePermissionFor(user, clock, Action.Person.READ_INVOICES, uuid)
        return Wrapper(db.connect { dbc -> dbc.read { it.getHeadOfFamilyInvoices(uuid) } })
    }

    @PutMapping("/{id}")
    fun putInvoice(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: InvoiceId,
        @RequestBody invoice: Invoice
    ) {
        Audit.InvoicesUpdate.log(targetId = id)
        accessControl.requirePermissionFor(user, clock, Action.Invoice.UPDATE, id)
        db.connect { dbc -> dbc.transaction { service.updateInvoice(it, id, invoice) } }
    }

    @GetMapping("/codes")
    fun getInvoiceCodes(db: Database, user: AuthenticatedUser, clock: EvakaClock): Wrapper<InvoiceCodes> {
        accessControl.requirePermissionFor(user, clock, Action.Global.READ_INVOICE_CODES)
        return Wrapper(db.connect { dbc -> dbc.read { service.getInvoiceCodes(it) } })
    }
}

data class InvoicePayload(val from: LocalDate, val to: LocalDate, val areas: List<String>, val invoiceDate: LocalDate?, val dueDate: LocalDate?)

data class SearchInvoicesRequest(
    val page: Int,
    val pageSize: Int,
    val sortBy: InvoiceSortParam?,
    val sortDirection: SortDirection?,
    val status: List<InvoiceStatus>?,
    val area: List<String>?,
    val unit: DaycareId?,
    val distinctions: List<InvoiceDistinctiveParams>?,
    val searchTerms: String?,
    val periodStart: LocalDate?,
    val periodEnd: LocalDate?
)
