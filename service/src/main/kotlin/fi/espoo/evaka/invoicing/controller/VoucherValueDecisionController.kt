// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.data.approveValueDecisionDraftsForSending
import fi.espoo.evaka.invoicing.data.findValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.getHeadOfFamilyVoucherValueDecisions
import fi.espoo.evaka.invoicing.data.getValueDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.data.lockValueDecisions
import fi.espoo.evaka.invoicing.data.lockValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.markVoucherValueDecisionsSent
import fi.espoo.evaka.invoicing.data.searchValueDecisions
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionStatusAndDates
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
import fi.espoo.evaka.invoicing.domain.updateEndDatesOrAnnulConflictingDecisions
import fi.espoo.evaka.invoicing.service.VoucherValueDecisionService
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyVoucherValueDecisionApproved
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.utils.europeHelsinki
import fi.espoo.evaka.shared.utils.parseEnum
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@RestController
@RequestMapping("/value-decisions")
class VoucherValueDecisionController(
    private val valueDecisionService: VoucherValueDecisionService,
    private val asyncJobRunner: AsyncJobRunner
) {
    @PostMapping("/search")
    fun search(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: SearchVoucherValueDecisionRequest
    ): ResponseEntity<Paged<VoucherValueDecisionSummary>> {
        Audit.VoucherValueDecisionSearch.log()
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        val maxPageSize = 5000
        if (body.pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        return db
            .read { tx ->
                tx.searchValueDecisions(
                    body.page,
                    body.pageSize,
                    body.sortBy ?: VoucherValueDecisionSortParam.STATUS,
                    body.sortDirection ?: SortDirection.DESC,
                    body.status?.let { parseEnum<VoucherValueDecisionStatus>(it) }
                        ?: throw BadRequest("Status is a mandatory parameter"),
                    body.area?.split(",") ?: listOf(),
                    body.unit?.let { parseUUID(it) },
                    body.searchTerms ?: "",
                    body.startDate?.let { LocalDate.parse(body.startDate, DateTimeFormatter.ISO_DATE) },
                    body.endDate?.let { LocalDate.parse(body.endDate, DateTimeFormatter.ISO_DATE) },
                    body.searchByStartDate,
                    body.financeDecisionHandlerId
                )
            }
            .let { ResponseEntity.ok(it) }
    }

    @GetMapping("/{id}")
    fun getDecision(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: VoucherValueDecisionId
    ): ResponseEntity<Wrapper<VoucherValueDecisionDetailed>> {
        Audit.VoucherValueDecisionRead.log(targetId = id)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        val res = db.read { it.getVoucherValueDecision(id) }
            ?: throw NotFound("No voucher value decision found with given ID ($id)")
        return ResponseEntity.ok(Wrapper(res))
    }

    @GetMapping("/head-of-family/{headOfFamilyId}")
    fun getHeadOfFamilyDecisions(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable headOfFamilyId: UUID
    ): ResponseEntity<List<VoucherValueDecisionSummary>> {
        Audit.VoucherValueDecisionHeadOfFamilyRead.log(targetId = headOfFamilyId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)
        val res = db.read { it.getHeadOfFamilyVoucherValueDecisions(headOfFamilyId) }
        return ResponseEntity.ok(res)
    }

    @PostMapping("/send")
    fun sendDrafts(db: Database.Connection, user: AuthenticatedUser, @RequestBody decisionIds: List<VoucherValueDecisionId>): ResponseEntity<Unit> {
        Audit.VoucherValueDecisionSend.log(targetId = decisionIds)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        db.transaction {
            sendVoucherValueDecisions(
                tx = it,
                asyncJobRunner = asyncJobRunner,
                user = user,
                now = ZonedDateTime.now(europeHelsinki).toInstant(),
                ids = decisionIds
            )
        }
        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/mark-sent")
    fun markSent(db: Database.Connection, user: AuthenticatedUser, @RequestBody ids: List<VoucherValueDecisionId>): ResponseEntity<Unit> {
        Audit.VoucherValueDecisionMarkSent.log(targetId = ids)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        db.transaction { tx ->
            val decisions = tx.getValueDecisionsByIds(ids)
            if (decisions.any { it.status != VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING })
                throw BadRequest("Voucher value decision cannot be marked sent")
            tx.markVoucherValueDecisionsSent(ids, Instant.now())
        }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/pdf/{id}")
    fun getDecisionPdf(db: Database.Connection, user: AuthenticatedUser, @PathVariable id: VoucherValueDecisionId): ResponseEntity<ByteArray> {
        Audit.FeeDecisionPdfRead.log(targetId = id)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        val (filename, pdf) = db.read { valueDecisionService.getDecisionPdf(it, id) }
        val headers = HttpHeaders().apply {
            add("Content-Disposition", "attachment; filename=\"$filename\"")
            add("Content-Type", "application/pdf")
        }
        return ResponseEntity(pdf, headers, HttpStatus.OK)
    }
}

fun sendVoucherValueDecisions(
    tx: Database.Transaction,
    asyncJobRunner: AsyncJobRunner,
    user: AuthenticatedUser,
    now: Instant,
    ids: List<VoucherValueDecisionId>
) {
    tx.lockValueDecisions(ids)
    val decisions = tx.getValueDecisionsByIds(ids)
    if (decisions.isEmpty()) return

    if (decisions.any { it.status != VoucherValueDecisionStatus.DRAFT }) {
        throw BadRequest("Some voucher value decisions were not drafts")
    }

    if (decisions.any { it.validFrom > LocalDate.now() }) {
        throw BadRequest("Some of the voucher value decisions are not valid yet")
    }

    val conflicts = decisions
        .flatMap {
            tx.lockValueDecisionsForChild(it.child.id)
            tx.findValueDecisionsForChild(
                it.child.id,
                DateRange(it.validFrom, it.validTo),
                listOf(VoucherValueDecisionStatus.SENT)
            )
        }
        .distinctBy { it.id }
        .filter { !ids.contains(it.id) }

    val updatedConflicts = updateEndDatesOrAnnulConflictingDecisions(decisions, conflicts)
    tx.updateVoucherValueDecisionStatusAndDates(updatedConflicts)

    val validIds = decisions.map { it.id }
    tx.approveValueDecisionDraftsForSending(validIds, user.id, now)
    asyncJobRunner.plan(tx, validIds.map { NotifyVoucherValueDecisionApproved(it) })
}

enum class VoucherValueDecisionSortParam {
    HEAD_OF_FAMILY,
    STATUS
}

data class SearchVoucherValueDecisionRequest(
    val page: Int,
    val pageSize: Int,
    val sortBy: VoucherValueDecisionSortParam?,
    val sortDirection: SortDirection?,
    val status: String?,
    val area: String?,
    val unit: String?,
    val searchTerms: String?,
    val financeDecisionHandlerId: UUID?,
    val startDate: String?,
    val endDate: String?,
    val searchByStartDate: Boolean = false
)
