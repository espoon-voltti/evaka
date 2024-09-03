// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
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
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
    CREATED_AT,
}

@RestController
@RequestMapping(
    "/invoices", // deprecated
    "/employee/invoices",
)
class InvoiceController(
    private val service: InvoiceService,
    private val generator: InvoiceGenerator,
    private val accessControl: AccessControl,
) {
    @PostMapping("/search")
    fun searchInvoices(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: SearchInvoicesRequest,
    ): PagedInvoiceSummaryResponses {
        val maxPageSize = 5000
        if (body.pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.SEARCH_INVOICES,
                    )
                    val paged =
                        tx.paginatedSearch(
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
                    val permittedActions =
                        accessControl.getPermittedActions<InvoiceId, Action.Invoice>(
                            tx,
                            user,
                            clock,
                            paged.data.map { it.id },
                        )
                    PagedInvoiceSummaryResponses(
                        data =
                            paged.data.map {
                                InvoiceSummaryResponse(it, permittedActions[it.id] ?: emptySet())
                            },
                        total = paged.total,
                        pages = paged.pages,
                    )
                }
            }
            .also { Audit.InvoicesSearch.log(meta = mapOf("total" to it.total)) }
    }

    data class InvoiceSummaryResponse(
        val data: InvoiceSummary,
        val permittedActions: Set<Action.Invoice>,
    )

    data class PagedInvoiceSummaryResponses(
        val data: List<InvoiceSummaryResponse>,
        val total: Int,
        val pages: Int,
    )

    @PostMapping("/create-drafts")
    fun createDraftInvoices(db: Database, user: AuthenticatedUser.Employee, clock: EvakaClock) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Global.CREATE_DRAFT_INVOICES,
                )
                val lastMonth =
                    FiniteDateRange.ofMonth(clock.today().withDayOfMonth(1).minusMonths(1))
                generator.createAndStoreAllDraftInvoices(it, lastMonth)
            }
        }
        Audit.InvoicesCreate.log()
    }

    @PostMapping("/delete-drafts")
    fun deleteDraftInvoices(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody invoiceIds: List<InvoiceId>,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Invoice.DELETE,
                    invoiceIds,
                )
                it.deleteDraftInvoices(invoiceIds)
            }
        }
        Audit.InvoicesDeleteDrafts.log(targetId = AuditId(invoiceIds))
    }

    @PostMapping("/send")
    fun sendInvoices(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam invoiceDate: LocalDate?,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam dueDate: LocalDate?,
        @RequestBody invoiceIds: List<InvoiceId>,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.Invoice.SEND, invoiceIds)
                service.sendInvoices(it, user, clock, invoiceIds, invoiceDate, dueDate)
            }
        }
        Audit.InvoicesSend.log(
            targetId = AuditId(invoiceIds),
            meta = mapOf("invoiceDate" to invoiceDate, "dueDate" to dueDate),
        )
    }

    @PostMapping("/send/by-date")
    fun sendInvoicesByDate(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody payload: InvoicePayload,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val invoiceIds = service.getInvoiceIds(tx, payload.from, payload.to, payload.areas)
                accessControl.requirePermissionFor(tx, user, clock, Action.Invoice.SEND, invoiceIds)
                service.sendInvoices(
                    tx,
                    user,
                    clock,
                    invoiceIds,
                    payload.invoiceDate,
                    payload.dueDate,
                )
            }
        }
        Audit.InvoicesSendByDate.log(
            meta =
                mapOf(
                    "from" to payload.from,
                    "to" to payload.to,
                    "areas" to payload.areas,
                    "invoiceDate" to payload.invoiceDate,
                    "dueDate" to payload.dueDate,
                )
        )
    }

    @PostMapping("/mark-sent")
    fun markInvoicesSent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody invoiceIds: List<InvoiceId>,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Invoice.MARK_SENT,
                    invoiceIds,
                )
                it.markManuallySent(user, clock.now(), invoiceIds)
            }
        }
        Audit.InvoicesMarkSent.log(targetId = AuditId(invoiceIds))
    }

    @GetMapping("/{id}")
    fun getInvoice(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: InvoiceId,
    ): InvoiceDetailedResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(tx, user, clock, Action.Invoice.READ, id)
                    val invoice =
                        tx.getDetailedInvoice(id)
                            ?: throw NotFound("No invoice found with given ID ($id)")
                    val permittedActions =
                        accessControl.getPermittedActions<InvoiceId, Action.Invoice>(
                            tx,
                            user,
                            clock,
                            invoice.id,
                        )
                    InvoiceDetailedResponse(invoice, permittedActions)
                }
            }
            .also { Audit.InvoicesRead.log(targetId = AuditId(id)) }
    }

    data class InvoiceDetailedResponse(
        val data: InvoiceDetailed,
        val permittedActions: Set<Action.Invoice>,
    )

    @GetMapping("/head-of-family/{id}")
    fun getHeadOfFamilyInvoices(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: PersonId,
    ): List<Invoice> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.READ_INVOICES,
                        id,
                    )
                    it.getHeadOfFamilyInvoices(id)
                }
            }
            .also {
                Audit.InvoicesRead.log(targetId = AuditId(id), meta = mapOf("count" to it.size))
            }
    }

    @GetMapping("/codes")
    fun getInvoiceCodes(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): InvoiceCodes {
        return db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Global.READ_INVOICE_CODES,
                )
                service.getInvoiceCodes(it)
            }
        }
    }
}

data class InvoicePayload(
    val from: LocalDate,
    val to: LocalDate,
    val areas: List<String>,
    val invoiceDate: LocalDate?,
    val dueDate: LocalDate?,
)

data class SearchInvoicesRequest(
    val page: Int,
    val pageSize: Int,
    val sortBy: InvoiceSortParam? = null,
    val sortDirection: SortDirection? = null,
    val status: List<InvoiceStatus>? = null,
    val area: List<String>? = null,
    val unit: DaycareId? = null,
    val distinctions: List<InvoiceDistinctiveParams>? = null,
    val searchTerms: String? = null,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
)
