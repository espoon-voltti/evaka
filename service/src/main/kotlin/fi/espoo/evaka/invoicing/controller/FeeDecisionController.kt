// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.IncludeCodeGen
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
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.controllers.Wrapper
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.utils.parseEnum
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

@IncludeCodeGen
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
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    @PostMapping("/search")
    fun search(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: SearchFeeDecisionRequest
    ): Paged<FeeDecisionSummary> {
        Audit.FeeDecisionSearch.log()
        accessControl.requirePermissionFor(user, Action.Global.SEARCH_FEE_DECISIONS)
        val maxPageSize = 5000
        if (body.pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        if (body.startDate != null && body.endDate != null && body.endDate < body.startDate)
            throw BadRequest("End date cannot be before start date")
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.searchFeeDecisions(
                    body.page,
                    body.pageSize,
                    body.sortBy ?: FeeDecisionSortParam.STATUS,
                    body.sortDirection ?: SortDirection.DESC,
                    body.status?.split(",")?.mapNotNull { parseEnum<FeeDecisionStatus>(it) } ?: listOf(),
                    body.area?.split(",") ?: listOf(),
                    body.unit,
                    body.distinctions?.split(",")?.mapNotNull { parseEnum<DistinctiveParams>(it) } ?: listOf(),
                    body.searchTerms ?: "",
                    body.startDate?.let { LocalDate.parse(body.startDate, DateTimeFormatter.ISO_DATE) },
                    body.endDate?.let { LocalDate.parse(body.endDate, DateTimeFormatter.ISO_DATE) },
                    body.searchByStartDate,
                    body.financeDecisionHandlerId
                )
            }
        }
    }

    @PostMapping("/confirm")
    fun confirmDrafts(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @RequestBody feeDecisionIds: List<FeeDecisionId>
    ) {
        Audit.FeeDecisionConfirm.log(targetId = feeDecisionIds)
        accessControl.requirePermissionFor(user, Action.FeeDecision.UPDATE, feeDecisionIds)
        db.connect { dbc ->
            dbc.transaction { tx ->
                val confirmedDecisions = service.confirmDrafts(
                    tx,
                    user,
                    feeDecisionIds,
                    evakaClock.now()
                )
                asyncJobRunner.plan(tx, confirmedDecisions.map { AsyncJob.NotifyFeeDecisionApproved(it) })
            }
        }
    }

    @PostMapping("/mark-sent")
    fun setSent(db: Database, user: AuthenticatedUser, @RequestBody feeDecisionIds: List<FeeDecisionId>) {
        Audit.FeeDecisionMarkSent.log(targetId = feeDecisionIds)
        accessControl.requirePermissionFor(user, Action.FeeDecision.UPDATE, feeDecisionIds)
        db.connect { dbc -> dbc.transaction { service.setSent(it, feeDecisionIds) } }
    }

    @GetMapping("/pdf/{uuid}")
    fun getDecisionPdf(db: Database, user: AuthenticatedUser, @PathVariable uuid: FeeDecisionId): ResponseEntity<ByteArray> {
        Audit.FeeDecisionPdfRead.log(targetId = uuid)
        accessControl.requirePermissionFor(user, Action.FeeDecision.READ, uuid)

        val (filename, pdf) = db.connect { dbc ->
            dbc.read { tx ->
                val decision = tx.getFeeDecision(uuid) ?: error("Cannot find fee decision $uuid")

                val personIds = listOfNotNull(
                    decision.headOfFamily.id,
                    decision.partner?.id,
                ) + decision.children.map { part -> part.child.id }

                val restrictedDetails = personIds.any { personId ->
                    tx.getPersonById(personId)?.restrictedDetailsEnabled ?: false
                }
                if (restrictedDetails && !user.isAdmin) {
                    throw Forbidden("Päätöksen alaisella henkilöllä on voimassa turvakielto. Osoitetietojen suojaamiseksi vain pääkäyttäjä voi ladata tämän päätöksen.")
                }

                service.getFeeDecisionPdf(tx, uuid)
            }
        }
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"$filename\"")
            .header("Content-Type", "application/pdf")
            .body(pdf)
    }

    @GetMapping("/{uuid}")
    fun getDecision(db: Database, user: AuthenticatedUser, @PathVariable uuid: FeeDecisionId): Wrapper<FeeDecisionDetailed> {
        Audit.FeeDecisionRead.log(targetId = uuid)
        accessControl.requirePermissionFor(user, Action.FeeDecision.READ, uuid)
        val res = db.connect { dbc -> dbc.read { it.getFeeDecision(uuid) } }
            ?: throw NotFound("No fee decision found with given ID ($uuid)")
        return Wrapper(res)
    }

    @GetMapping("/head-of-family/{id}")
    fun getHeadOfFamilyFeeDecisions(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: PersonId
    ): Wrapper<List<FeeDecision>> {
        Audit.FeeDecisionHeadOfFamilyRead.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.Person.READ_FEE_DECISIONS, id)
        return Wrapper(
            db.connect { dbc ->
                dbc.read {
                    it.findFeeDecisionsForHeadOfFamily(id, null, null)
                }
            }
        )
    }

    @PostMapping("/head-of-family/{id}/create-retroactive")
    fun generateRetroactiveDecisions(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: PersonId,
        @RequestBody body: CreateRetroactiveFeeDecisionsBody
    ) {
        Audit.FeeDecisionHeadOfFamilyCreateRetroactive.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.Person.GENERATE_RETROACTIVE_FEE_DECISIONS, id)
        db.connect { dbc -> dbc.transaction { generator.createRetroactiveFeeDecisions(it, id, body.from) } }
    }

    @PostMapping("/set-type/{uuid}")
    fun setType(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable uuid: FeeDecisionId,
        @RequestBody request: FeeDecisionTypeRequest
    ) {
        Audit.FeeDecisionSetType.log(targetId = uuid)
        accessControl.requirePermissionFor(user, Action.FeeDecision.UPDATE, uuid)
        db.connect { dbc -> dbc.transaction { service.setType(it, uuid, request.type) } }
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
    val unit: DaycareId?,
    val distinctions: String?,
    val searchTerms: String?,
    val startDate: String?,
    val endDate: String?,
    val searchByStartDate: Boolean = false,
    val financeDecisionHandlerId: EmployeeId?
)

data class FeeDecisionTypeRequest(
    val type: FeeDecisionType
)
