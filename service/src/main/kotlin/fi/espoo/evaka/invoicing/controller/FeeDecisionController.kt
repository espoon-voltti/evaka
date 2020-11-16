// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.getFeeDecision
import fi.espoo.evaka.invoicing.data.searchFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.service.DecisionGenerator
import fi.espoo.evaka.invoicing.service.FeeDecisionService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFeeDecisionApproved
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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

data class FeeDecisionSearchResult(
    val data: List<FeeDecisionSummary>,
    val total: Int,
    val pages: Int
)

@RestController
@RequestMapping(path = ["/fee-decisions", "/decisions"])
class FeeDecisionController(
    private val objectMapper: ObjectMapper,
    private val service: FeeDecisionService,
    private val generator: DecisionGenerator,
    private val asyncJobRunner: AsyncJobRunner
) {
    @GetMapping("/search")
    fun search(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam(required = true) page: Int,
        @RequestParam(required = true) pageSize: Int,
        @RequestParam(required = false) sortBy: FeeDecisionSortParam?,
        @RequestParam(required = false) sortDirection: SortDirection?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) area: String?,
        @RequestParam(required = false) unit: String?,
        @RequestParam(required = false) distinctions: String?,
        @RequestParam(required = false) searchTerms: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false) searchByStartDate: Boolean = false
    ): ResponseEntity<FeeDecisionSearchResult> {
        Audit.FeeDecisionSearch.log()
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val maxPageSize = 5000
        if (pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        if (startDate != null && endDate != null && endDate < startDate)
            throw BadRequest("End date cannot be before start date")
        val (total, feeDecisions) = db.read { tx ->
            searchFeeDecisions(
                tx.handle,
                page,
                pageSize,
                sortBy ?: FeeDecisionSortParam.STATUS,
                sortDirection ?: SortDirection.DESC,
                status?.split(",")?.mapNotNull { parseEnum<FeeDecisionStatus>(it) } ?: listOf(),
                area?.split(",") ?: listOf(),
                unit?.let { parseUUID(it) },
                distinctions?.split(",")?.mapNotNull { parseEnum<DistinctiveParams>(it) } ?: listOf(),
                searchTerms ?: "",
                startDate?.let { LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE) },
                endDate?.let { LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE) },
                searchByStartDate
            )
        }
        val pages =
            if (total % pageSize == 0) total / pageSize
            else (total / pageSize) + 1
        return ResponseEntity.ok(FeeDecisionSearchResult(feeDecisions, total, pages))
    }

    @PostMapping("/confirm")
    fun confirmDrafts(db: Database, user: AuthenticatedUser, @RequestBody feeDecisionIds: List<UUID>): ResponseEntity<Unit> {
        Audit.FeeDecisionConfirm.log(targetId = feeDecisionIds)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        db.transaction { tx ->
            val confirmedDecisions = service.confirmDrafts(tx.handle, user, feeDecisionIds)
            asyncJobRunner.plan(tx, confirmedDecisions.map { NotifyFeeDecisionApproved(it) })
        }
        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/mark-sent")
    fun setSent(db: Database, user: AuthenticatedUser, @RequestBody feeDecisionIds: List<UUID>): ResponseEntity<Unit> {
        Audit.FeeDecisionMarkSent.log(targetId = feeDecisionIds)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        db.transaction { service.setSent(it.handle, feeDecisionIds) }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/pdf/{uuid}")
    fun getDecisionPdf(db: Database, user: AuthenticatedUser, @PathVariable uuid: UUID): ResponseEntity<ByteArray> {
        Audit.FeeDecisionPdfRead.log(targetId = uuid)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val headers = HttpHeaders()
        val (filename, pdf) = db.read { service.getFeeDecisionPdf(it.handle, uuid) }
        headers.add("Content-Disposition", "attachment; filename=\"$filename\"")
        headers.add("Content-Type", "application/pdf")
        return ResponseEntity(pdf, headers, HttpStatus.OK)
    }

    @GetMapping("/{uuid}")
    fun getDecision(db: Database, user: AuthenticatedUser, @PathVariable uuid: UUID): ResponseEntity<Wrapper<FeeDecisionDetailed>> {
        Audit.FeeDecisionRead.log(targetId = uuid)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val res = db.read { getFeeDecision(it.handle, objectMapper, uuid) }
            ?: throw NotFound("No fee decision found with given ID ($uuid)")
        return ResponseEntity.ok(Wrapper(res))
    }

    @GetMapping("/head-of-family/{uuid}")
    fun getHeadOfFamilyFeeDecisions(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable uuid: UUID
    ): ResponseEntity<Wrapper<List<FeeDecision>>> {
        Audit.FeeDecisionHeadOfFamilyRead.log(targetId = uuid)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val res = db.read {
            findFeeDecisionsForHeadOfFamily(
                it.handle,
                objectMapper,
                uuid,
                null,
                null
            )
        }
        return ResponseEntity.ok(Wrapper(res))
    }

    @PostMapping("/head-of-family/{id}/create-retroactive")
    fun generateRetroactiveDecisions(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: CreateRetroactiveFeeDecisionsBody
    ): ResponseEntity<Unit> {
        Audit.FeeDecisionHeadOfFamilyCreateRetroactive.log(targetId = id)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        db.transaction { generator.createRetroactive(it.handle, id, body.from) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/set-type/{uuid}")
    fun setType(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable uuid: UUID,
        @RequestBody request: FeeDecisionTypeRequest
    ): ResponseEntity<Unit> {
        Audit.FeeDecisionSetType.log(targetId = uuid)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        db.transaction { service.setType(it.handle, uuid, request.type) }
        return ResponseEntity.noContent().build()
    }
}

data class CreateRetroactiveFeeDecisionsBody(val from: LocalDate)

data class FeeDecisionTypeRequest(
    val type: FeeDecisionType
)
