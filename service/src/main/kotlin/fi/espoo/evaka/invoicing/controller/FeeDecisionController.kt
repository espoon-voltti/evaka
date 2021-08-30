// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.getFeeDecision
import fi.espoo.evaka.invoicing.data.searchFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.service.FeeDecisionService
import fi.espoo.evaka.invoicing.service.FinanceDecisionGenerator
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFeeDecisionApproved
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
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
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class FeeDecisionSortParam {
    HEAD_OF_FAMILY,
    VALIDITY,
    NUMBER,
    CREATED,
    SENT,
    STATUS,
    FINAL_PRICE
}

enum class SortDirection {
    ASC,
    DESC
}

enum class DistinctiveParams {
    UNCONFIRMED_HOURS,
    EXTERNAL_CHILD,
    RETROACTIVE
}

@RestController
@RequestMapping(path = ["/fee-decisions", "/decisions"])
class FeeDecisionController(
    private val service: FeeDecisionService,
    private val generator: FinanceDecisionGenerator,
    private val asyncJobRunner: AsyncJobRunner
) {
    @PostMapping("/search")
    fun search(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: SearchFeeDecisionRequest
    ): ResponseEntity<Paged<FeeDecisionSummary>> {
        Audit.FeeDecisionSearch.log()
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        val maxPageSize = 5000
        if (body.pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        if (body.startDate != null && body.endDate != null && body.endDate < body.startDate)
            throw BadRequest("End date cannot be before start date")
        return db
            .read { tx ->
                tx.searchFeeDecisions(
                    body.page,
                    body.pageSize,
                    body.sortBy ?: FeeDecisionSortParam.STATUS,
                    body.sortDirection ?: SortDirection.DESC,
                    body.status?.split(",")?.mapNotNull { parseEnum<FeeDecisionStatus>(it) } ?: listOf(),
                    body.area?.split(",") ?: listOf(),
                    body.unit?.let { parseUUID(it) },
                    body.distinctions?.split(",")?.mapNotNull { parseEnum<DistinctiveParams>(it) } ?: listOf(),
                    body.searchTerms ?: "",
                    body.startDate?.let { LocalDate.parse(body.startDate, DateTimeFormatter.ISO_DATE) },
                    body.endDate?.let { LocalDate.parse(body.endDate, DateTimeFormatter.ISO_DATE) },
                    body.searchByStartDate,
                    body.financeDecisionHandlerId
                )
            }
            .let { ResponseEntity.ok(it) }
    }

    @PostMapping("/confirm")
    fun confirmDrafts(db: Database.Connection, user: AuthenticatedUser, @RequestBody feeDecisionIds: List<FeeDecisionId>): ResponseEntity<Unit> {
        Audit.FeeDecisionConfirm.log(targetId = feeDecisionIds)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        db.transaction { tx ->
            val confirmedDecisions = service.confirmDrafts(tx, user, feeDecisionIds, Instant.now())
            asyncJobRunner.plan(tx, confirmedDecisions.map { NotifyFeeDecisionApproved(it) })
        }
        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/mark-sent")
    fun setSent(db: Database.Connection, user: AuthenticatedUser, @RequestBody feeDecisionIds: List<FeeDecisionId>): ResponseEntity<Unit> {
        Audit.FeeDecisionMarkSent.log(targetId = feeDecisionIds)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        db.transaction { service.setSent(it, feeDecisionIds) }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/pdf/{uuid}")
    fun getDecisionPdf(db: Database.Connection, user: AuthenticatedUser, @PathVariable uuid: FeeDecisionId): ResponseEntity<ByteArray> {
        Audit.FeeDecisionPdfRead.log(targetId = uuid)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        val headers = HttpHeaders()
        val (filename, pdf) = db.read { service.getFeeDecisionPdf(it, uuid) }
        headers.add("Content-Disposition", "attachment; filename=\"$filename\"")
        headers.add("Content-Type", "application/pdf")
        return ResponseEntity(pdf, headers, HttpStatus.OK)
    }

    @GetMapping("/{uuid}")
    fun getDecision(db: Database.Connection, user: AuthenticatedUser, @PathVariable uuid: FeeDecisionId): ResponseEntity<Wrapper<FeeDecisionDetailed>> {
        Audit.FeeDecisionRead.log(targetId = uuid)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        val res = db.read { it.getFeeDecision(uuid) }
            ?: throw NotFound("No fee decision found with given ID ($uuid)")
        return ResponseEntity.ok(Wrapper(res))
    }

    @GetMapping("/head-of-family/{uuid}")
    fun getHeadOfFamilyFeeDecisions(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable uuid: UUID
    ): ResponseEntity<Wrapper<List<FeeDecision>>> {
        Audit.FeeDecisionHeadOfFamilyRead.log(targetId = uuid)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        val res = db.read {
            it.findFeeDecisionsForHeadOfFamily(
                uuid,
                null,
                null
            )
        }
        return ResponseEntity.ok(Wrapper(res))
    }

    @PostMapping("/head-of-family/{id}/create-retroactive")
    fun generateRetroactiveDecisions(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: CreateRetroactiveFeeDecisionsBody
    ): ResponseEntity<Unit> {
        Audit.FeeDecisionHeadOfFamilyCreateRetroactive.log(targetId = id)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        db.transaction { generator.createRetroactive(it, id, body.from) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/set-type/{uuid}")
    fun setType(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable uuid: FeeDecisionId,
        @RequestBody request: FeeDecisionTypeRequest
    ): ResponseEntity<Unit> {
        Audit.FeeDecisionSetType.log(targetId = uuid)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        db.transaction { service.setType(it, uuid, request.type) }
        return ResponseEntity.noContent().build()
    }
}

data class CreateRetroactiveFeeDecisionsBody(val from: LocalDate)

data class SearchFeeDecisionRequest(
    val page: Int,
    val pageSize: Int,
    val sortBy: FeeDecisionSortParam?,
    val sortDirection: SortDirection?,
    val status: String?,
    val area: String?,
    val unit: String?,
    val distinctions: String?,
    val searchTerms: String?,
    val startDate: String?,
    val endDate: String?,
    val searchByStartDate: Boolean = false,
    val financeDecisionHandlerId: UUID?
)

data class FeeDecisionTypeRequest(
    val type: FeeDecisionType
)
