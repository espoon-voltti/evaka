// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.data.approveValueDecisionDraftsForSending
import fi.espoo.evaka.invoicing.data.deleteValueDecisions
import fi.espoo.evaka.invoicing.data.findValueDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.getValueDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.data.lockValueDecisions
import fi.espoo.evaka.invoicing.data.lockValueDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.markVoucherValueDecisionsSent
import fi.espoo.evaka.invoicing.data.searchValueDecisions
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionStatusAndDates
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
import fi.espoo.evaka.invoicing.domain.updateEndDatesOrAnnulConflictingDecisions
import fi.espoo.evaka.invoicing.service.VoucherValueDecisionService
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyVoucherValueDecisionApproved
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.utils.parseEnum
import fi.espoo.evaka.shared.utils.zoneId
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@RestController
@RequestMapping("/value-decisions")
class VoucherValueDecisionController(
    private val valueDecisionService: VoucherValueDecisionService,
    private val objectMapper: ObjectMapper,
    private val asyncJobRunner: AsyncJobRunner
) {
    @GetMapping("/search")
    fun search(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam(required = true) page: Int,
        @RequestParam(required = true) pageSize: Int,
        @RequestParam(required = false) sortBy: VoucherValueDecisionSortParam?,
        @RequestParam(required = false) sortDirection: SortDirection?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) area: String?,
        @RequestParam(required = false) unit: String?,
        @RequestParam(required = false) searchTerms: String?,
        @RequestParam(required = false) financeDecisionHandlerId: UUID?
    ): ResponseEntity<Paged<VoucherValueDecisionSummary>> {
        Audit.VoucherValueDecisionSearch.log()
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        val maxPageSize = 5000
        if (pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        return db
            .read { tx ->
                tx.searchValueDecisions(
                    page,
                    pageSize,
                    sortBy ?: VoucherValueDecisionSortParam.STATUS,
                    sortDirection ?: SortDirection.DESC,
                    status?.let { parseEnum<VoucherValueDecisionStatus>(it) }
                        ?: throw BadRequest("Status is a mandatory parameter"),
                    area?.split(",") ?: listOf(),
                    unit?.let { parseUUID(it) },
                    searchTerms ?: "",
                    financeDecisionHandlerId
                )
            }
            .let { ResponseEntity.ok(it) }
    }

    @GetMapping("/{id}")
    fun getDecision(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<Wrapper<VoucherValueDecisionDetailed>> {
        Audit.VoucherValueDecisionRead.log(targetId = id)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        val res = db.read { it.handle.getVoucherValueDecision(objectMapper, id) }
            ?: throw NotFound("No voucher value decision found with given ID ($id)")
        return ResponseEntity.ok(Wrapper(res))
    }

    @PostMapping("/send")
    fun sendDrafts(db: Database.Connection, user: AuthenticatedUser, @RequestBody decisionIds: List<UUID>): ResponseEntity<Unit> {
        Audit.VoucherValueDecisionSend.log(targetId = decisionIds)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        db.transaction {
            sendVoucherValueDecisions(
                tx = it,
                objectMapper = objectMapper,
                asyncJobRunner = asyncJobRunner,
                user = user,
                now = ZonedDateTime.now(zoneId).toInstant(),
                ids = decisionIds
            )
        }
        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/mark-sent")
    fun markSent(db: Database.Connection, user: AuthenticatedUser, @RequestBody ids: List<UUID>): ResponseEntity<Unit> {
        Audit.VoucherValueDecisionMarkSent.log(targetId = ids)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        db.transaction { tx ->
            val decisions = tx.handle.getValueDecisionsByIds(objectMapper, ids)
            if (decisions.any { it.status != VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING })
                throw BadRequest("Voucher value decision cannot be marked sent")
            tx.markVoucherValueDecisionsSent(ids, Instant.now())
        }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/pdf/{id}")
    fun getDecisionPdf(db: Database.Connection, user: AuthenticatedUser, @PathVariable id: UUID): ResponseEntity<ByteArray> {
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
    objectMapper: ObjectMapper,
    asyncJobRunner: AsyncJobRunner,
    user: AuthenticatedUser,
    now: Instant,
    ids: List<UUID>
) {
    tx.handle.lockValueDecisions(ids)
    val decisions = tx.handle.getValueDecisionsByIds(objectMapper, ids)
    if (decisions.isEmpty()) return

    if (decisions.any { it.status != VoucherValueDecisionStatus.DRAFT }) {
        throw BadRequest("Some voucher value decisions were not drafts")
    }

    if (decisions.any { it.validFrom > LocalDate.now() }) {
        throw BadRequest("Some of the voucher value decisions are not valid yet")
    }

    val conflicts = decisions
        .flatMap {
            tx.handle.lockValueDecisionsForHeadOfFamily(it.headOfFamily.id)
            tx.handle.findValueDecisionsForHeadOfFamily(
                objectMapper,
                it.headOfFamily.id,
                DateRange(it.validFrom, it.validTo),
                listOf(VoucherValueDecisionStatus.SENT)
            )
        }
        .distinctBy { it.id }
        .filter { !ids.contains(it.id) }

    val updatedConflicts = updateEndDatesOrAnnulConflictingDecisions(decisions, conflicts)
    tx.updateVoucherValueDecisionStatusAndDates(updatedConflicts)

    val (emptyDecisions, validDecisions) = decisions
        .partition { it.parts.isEmpty() }
    tx.handle.deleteValueDecisions(emptyDecisions.map { it.id })

    val validIds = validDecisions.map { it.id }
    tx.handle.approveValueDecisionDraftsForSending(validIds, user.id, now)
    asyncJobRunner.plan(tx, validIds.map { NotifyVoucherValueDecisionApproved(it) })
}

enum class VoucherValueDecisionSortParam {
    HEAD_OF_FAMILY,
    STATUS
}
