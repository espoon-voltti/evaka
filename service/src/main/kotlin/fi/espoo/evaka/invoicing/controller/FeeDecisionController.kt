// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.ConstList
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
import fi.espoo.evaka.shared.FeatureConfig
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
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

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

@ConstList("feeDecisionDistinctiveParams")
enum class DistinctiveParams {
    UNCONFIRMED_HOURS,
    EXTERNAL_CHILD,
    RETROACTIVE,
    NO_STARTING_PLACEMENTS,
    MAX_FEE_ACCEPTED,
}

@RestController
@RequestMapping(path = ["/fee-decisions", "/decisions"])
class FeeDecisionController(
    private val service: FeeDecisionService,
    private val generator: FinanceDecisionGenerator,
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val featureConfig: FeatureConfig
) {
    @PostMapping("/search")
    fun search(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: SearchFeeDecisionRequest
    ): Paged<FeeDecisionSummary> {
        accessControl.requirePermissionFor(user, clock, Action.Global.SEARCH_FEE_DECISIONS)
        val maxPageSize = 5000
        if (body.pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        if (body.startDate != null && body.endDate != null && body.endDate < body.startDate)
            throw BadRequest("End date cannot be before start date")
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.searchFeeDecisions(
                    clock,
                    body.page,
                    body.pageSize,
                    body.sortBy ?: FeeDecisionSortParam.STATUS,
                    body.sortDirection ?: SortDirection.DESC,
                    body.status ?: emptyList(),
                    body.area ?: emptyList(),
                    body.unit,
                    body.distinctions ?: emptyList(),
                    body.searchTerms ?: "",
                    body.startDate,
                    body.endDate,
                    body.searchByStartDate,
                    body.financeDecisionHandlerId
                )
            }
        }.also {
            Audit.FeeDecisionSearch.log(args = mapOf("total" to it.total))
        }
    }

    @PostMapping("/confirm")
    fun confirmDrafts(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody feeDecisionIds: List<FeeDecisionId>
    ) {
        accessControl.requirePermissionFor(user, clock, Action.FeeDecision.UPDATE, feeDecisionIds)
        db.connect { dbc ->
            dbc.transaction { tx ->
                val confirmedDecisions = service.confirmDrafts(
                    tx,
                    user,
                    feeDecisionIds,
                    clock.now(),
                    featureConfig.alwaysUseDaycareFinanceDecisionHandler
                )
                asyncJobRunner.plan(tx, confirmedDecisions.map { AsyncJob.NotifyFeeDecisionApproved(it) }, runAt = clock.now())
            }
        }
        Audit.FeeDecisionConfirm.log(targetId = feeDecisionIds)
    }

    @PostMapping("/mark-sent")
    fun setSent(db: Database, user: AuthenticatedUser, clock: EvakaClock, @RequestBody feeDecisionIds: List<FeeDecisionId>) {
        accessControl.requirePermissionFor(user, clock, Action.FeeDecision.UPDATE, feeDecisionIds)
        db.connect { dbc -> dbc.transaction { service.setSent(it, clock, feeDecisionIds) } }
        Audit.FeeDecisionMarkSent.log(targetId = feeDecisionIds)
    }

    @GetMapping("/pdf/{decisionId}")
    fun getDecisionPdf(db: Database, user: AuthenticatedUser, clock: EvakaClock, @PathVariable decisionId: FeeDecisionId): ResponseEntity<Any> {
        accessControl.requirePermissionFor(user, clock, Action.FeeDecision.READ, decisionId)

        return db.connect { dbc ->
            dbc.read { tx ->
                val decision = tx.getFeeDecision(decisionId) ?: error("Cannot find fee decision $decisionId")

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
            }
            service.getFeeDecisionPdfResponse(dbc, decisionId)
        }.also {
            Audit.FeeDecisionPdfRead.log(targetId = decisionId)
        }
    }

    @GetMapping("/{uuid}")
    fun getDecision(db: Database, user: AuthenticatedUser, clock: EvakaClock, @PathVariable uuid: FeeDecisionId): Wrapper<FeeDecisionDetailed> {
        accessControl.requirePermissionFor(user, clock, Action.FeeDecision.READ, uuid)
        val res = db.connect { dbc -> dbc.read { it.getFeeDecision(uuid) } }
            ?: throw NotFound("No fee decision found with given ID ($uuid)")
        Audit.FeeDecisionRead.log(targetId = uuid)
        return Wrapper(res)
    }

    @GetMapping("/head-of-family/{id}")
    fun getHeadOfFamilyFeeDecisions(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: PersonId
    ): Wrapper<List<FeeDecision>> {
        accessControl.requirePermissionFor(user, clock, Action.Person.READ_FEE_DECISIONS, id)
        return Wrapper(
            db.connect { dbc ->
                dbc.read {
                    it.findFeeDecisionsForHeadOfFamily(id, null, null)
                }
            }.also {
                Audit.FeeDecisionHeadOfFamilyRead.log(targetId = id, args = mapOf("count" to it.size))
            }
        )
    }

    @PostMapping("/head-of-family/{id}/create-retroactive")
    fun generateRetroactiveDecisions(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: PersonId,
        @RequestBody body: CreateRetroactiveFeeDecisionsBody
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Person.GENERATE_RETROACTIVE_FEE_DECISIONS, id)
        db.connect { dbc -> dbc.transaction { generator.createRetroactiveFeeDecisions(it, id, body.from) } }
        Audit.FeeDecisionHeadOfFamilyCreateRetroactive.log(targetId = id)
    }

    @PostMapping("/set-type/{uuid}")
    fun setType(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable uuid: FeeDecisionId,
        @RequestBody request: FeeDecisionTypeRequest
    ) {
        accessControl.requirePermissionFor(user, clock, Action.FeeDecision.UPDATE, uuid)
        db.connect { dbc -> dbc.transaction { service.setType(it, uuid, request.type) } }
        Audit.FeeDecisionSetType.log(targetId = uuid, args = mapOf("type" to request.type))
    }
}

data class CreateRetroactiveFeeDecisionsBody(val from: LocalDate)

data class SearchFeeDecisionRequest(
    val page: Int,
    val pageSize: Int,
    val sortBy: FeeDecisionSortParam?,
    val sortDirection: SortDirection?,
    val status: List<FeeDecisionStatus>?,
    val area: List<String>?,
    val unit: DaycareId?,
    val distinctions: List<DistinctiveParams>?,
    val searchTerms: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val searchByStartDate: Boolean = false,
    val financeDecisionHandlerId: EmployeeId?,

)

data class FeeDecisionTypeRequest(
    val type: FeeDecisionType
)
