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
import fi.espoo.evaka.invoicing.data.searchValueDecisions
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
import fi.espoo.evaka.invoicing.domain.updateEndDatesOrAnnulConflictingDecisions
import fi.espoo.evaka.invoicing.service.VoucherValueDecisionService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyVoucherValueDecisionApproved
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.Period
import fi.espoo.evaka.shared.utils.parseEnum
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
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
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/value-decisions")
class VoucherValueDecisionController(
    private val valueDecisionService: VoucherValueDecisionService,
    private val jdbi: Jdbi,
    private val objectMapper: ObjectMapper,
    private val asyncJobRunner: AsyncJobRunner
) {
    @GetMapping("/search")
    fun search(
        user: AuthenticatedUser,
        @RequestParam(required = true) page: Int,
        @RequestParam(required = true) pageSize: Int,
        @RequestParam(required = false) sortBy: VoucherValueDecisionSortParam?,
        @RequestParam(required = false) sortDirection: SortDirection?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) area: String?,
        @RequestParam(required = false) unit: String?,
        @RequestParam(required = false) searchTerms: String?
    ): ResponseEntity<VoucherValueDecisionSearchResult> {
        Audit.VoucherValueDecisionSearch.log()
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val maxPageSize = 5000
        if (pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        val (total, valueDecisions) = jdbi.handle { h ->
            h.searchValueDecisions(
                page,
                pageSize,
                sortBy ?: VoucherValueDecisionSortParam.STATUS,
                sortDirection ?: SortDirection.DESC,
                status?.let { parseEnum<VoucherValueDecisionStatus>(it) }
                    ?: throw BadRequest("Status is a mandatory parameter"),
                area?.split(",") ?: listOf(),
                unit?.let { parseUUID(it) },
                searchTerms ?: ""
            )
        }
        val pages =
            if (total % pageSize == 0) total / pageSize
            else (total / pageSize) + 1
        return ResponseEntity.ok(VoucherValueDecisionSearchResult(valueDecisions, total, pages))
    }

    @GetMapping("/{id}")
    fun getDecision(
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<Wrapper<VoucherValueDecisionDetailed>> {
        Audit.VoucherValueDecisionRead.log(targetId = id)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val res = jdbi.handle { it.getVoucherValueDecision(objectMapper, id) }
            ?: throw NotFound("No voucher value decision found with given ID ($id)")
        return ResponseEntity.ok(Wrapper(res))
    }

    @PostMapping("/send")
    fun sendDrafts(user: AuthenticatedUser, @RequestBody decisionIds: List<UUID>): ResponseEntity<Unit> {
        Audit.VoucherValueDecisionSend.log(targetId = decisionIds)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        jdbi.transaction { sendVoucherValueDecisions(it, user, decisionIds) }
        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/pdf/{id}")
    fun getDecisionPdf(user: AuthenticatedUser, @PathVariable id: UUID): ResponseEntity<ByteArray> {
        Audit.FeeDecisionPdfRead.log(targetId = id)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val (filename, pdf) = jdbi.handle { h -> valueDecisionService.getDecisionPdf(h, id) }
        val headers = HttpHeaders().apply {
            add("Content-Disposition", "attachment; filename=\"$filename\"")
            add("Content-Type", "application/pdf")
        }
        return ResponseEntity(pdf, headers, HttpStatus.OK)
    }

    private fun sendVoucherValueDecisions(h: Handle, user: AuthenticatedUser, ids: List<UUID>) {
        h.lockValueDecisions(ids)
        val decisions = h.getValueDecisionsByIds(objectMapper, ids)
        if (decisions.isEmpty()) return

        if (decisions.any { it.status != VoucherValueDecisionStatus.DRAFT }) {
            throw BadRequest("Some voucher value decisions were not drafts")
        }

        if (decisions.any { it.validFrom > LocalDate.now() }) {
            throw BadRequest("Some of the voucher value decisions are not valid yet")
        }

        val conflicts = decisions
            .flatMap {
                h.lockValueDecisionsForHeadOfFamily(it.headOfFamily.id)
                h.findValueDecisionsForHeadOfFamily(
                    objectMapper,
                    it.headOfFamily.id,
                    Period(it.validFrom, it.validTo),
                    listOf(VoucherValueDecisionStatus.SENT)
                )
            }
            .distinctBy { it.id }
            .filter { !ids.contains(it.id) }

        val updatedConflicts = updateEndDatesOrAnnulConflictingDecisions(decisions, conflicts)
        h.upsertValueDecisions(objectMapper, updatedConflicts)

        val (emptyDecisions, validDecisions) = decisions
            .partition { it.parts.isEmpty() }
        h.deleteValueDecisions(emptyDecisions.map { it.id })

        val validIds = validDecisions.map { it.id }
        h.approveValueDecisionDraftsForSending(validIds, user.id)
        validIds.forEach {
            asyncJobRunner.plan(h, listOf(NotifyVoucherValueDecisionApproved(it)))
        }
    }
}

data class VoucherValueDecisionSearchResult(
    val data: List<VoucherValueDecisionSummary>,
    val total: Int,
    val pages: Int
)

enum class VoucherValueDecisionSortParam {
    HEAD_OF_FAMILY,
    STATUS
}
