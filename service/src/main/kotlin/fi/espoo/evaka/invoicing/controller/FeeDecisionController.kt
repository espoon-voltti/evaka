// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.invoicing.data.PagedFeeDecisionSummaries
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.getFeeDecision
import fi.espoo.evaka.invoicing.data.searchFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionDifference
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.service.FeeDecisionService
import fi.espoo.evaka.invoicing.service.FinanceDecisionGenerator
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
    PRESCHOOL_CLUB
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
    fun searchFeeDecisions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: SearchFeeDecisionRequest
    ): PagedFeeDecisionSummaries {
        val maxPageSize = 5000
        if (body.pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        if (body.startDate != null && body.endDate != null && body.endDate < body.startDate) {
            throw BadRequest("End date cannot be before start date")
        }
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.SEARCH_FEE_DECISIONS
                    )
                    tx.searchFeeDecisions(
                        clock,
                        featureConfig.postOffice,
                        body.page,
                        body.pageSize,
                        body.sortBy ?: FeeDecisionSortParam.STATUS,
                        body.sortDirection ?: SortDirection.DESC,
                        body.statuses ?: emptyList(),
                        body.area ?: emptyList(),
                        body.unit,
                        body.distinctions ?: emptyList(),
                        body.searchTerms ?: "",
                        body.startDate,
                        body.endDate,
                        body.searchByStartDate,
                        body.financeDecisionHandlerId,
                        body.difference ?: emptySet()
                    )
                }
            }
            .also { Audit.FeeDecisionSearch.log(meta = mapOf("total" to it.total)) }
    }

    @PostMapping("/confirm")
    fun confirmFeeDecisionDrafts(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody feeDecisionIds: List<FeeDecisionId>,
        @RequestParam decisionHandlerId: EmployeeId?
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.FeeDecision.UPDATE,
                    feeDecisionIds
                )
                val confirmedDecisions =
                    service.confirmDrafts(
                        tx,
                        user,
                        feeDecisionIds,
                        clock.now(),
                        decisionHandlerId,
                        featureConfig.alwaysUseDaycareFinanceDecisionHandler
                    )
                asyncJobRunner.plan(
                    tx,
                    confirmedDecisions.map { AsyncJob.NotifyFeeDecisionApproved(it) },
                    runAt = clock.now()
                )
            }
        }
        Audit.FeeDecisionConfirm.log(targetId = feeDecisionIds)
    }

    @PostMapping("/ignore")
    fun ignoreFeeDecisionDrafts(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody feeDecisionIds: List<FeeDecisionId>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.FeeDecision.IGNORE,
                    feeDecisionIds
                )
                service.ignoreDrafts(tx, feeDecisionIds, clock.today())
            }
        }
        Audit.FeeDecisionIgnore.log(targetId = feeDecisionIds)
    }

    @PostMapping("/unignore")
    fun unignoreFeeDecisionDrafts(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody feeDecisionIds: List<FeeDecisionId>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.FeeDecision.UNIGNORE,
                    feeDecisionIds
                )
                val headsOfFamilies = service.unignoreDrafts(tx, feeDecisionIds)
                asyncJobRunner.plan(
                    tx,
                    headsOfFamilies.map { personId ->
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            personId,
                            DateRange(clock.today().minusMonths(15), null)
                        )
                    },
                    runAt = clock.now()
                )
            }
        }
        Audit.FeeDecisionUnignore.log(targetId = feeDecisionIds)
    }

    @PostMapping("/mark-sent")
    fun setFeeDecisionSent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody feeDecisionIds: List<FeeDecisionId>
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.FeeDecision.UPDATE,
                    feeDecisionIds
                )
                service.setSent(it, clock, feeDecisionIds)
            }
        }
        Audit.FeeDecisionMarkSent.log(targetId = feeDecisionIds)
    }

    @GetMapping("/pdf/{decisionId}")
    fun getFeeDecisionPdf(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable decisionId: FeeDecisionId
    ): ResponseEntity<Any> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.FeeDecision.READ,
                        decisionId
                    )

                    val decision =
                        tx.getFeeDecision(decisionId)
                            ?: error("Cannot find fee decision $decisionId")

                    val personIds =
                        listOfNotNull(decision.headOfFamily.id, decision.partner?.id) +
                            decision.children.map { part -> part.child.id }

                    val restrictedDetails =
                        personIds.any { personId ->
                            tx.getPersonById(personId)?.restrictedDetailsEnabled ?: false
                        }
                    if (
                        restrictedDetails && decision.documentContainsContactInfo && !user.isAdmin
                    ) {
                        throw Forbidden(
                            "Päätöksen alaisella henkilöllä on voimassa turvakielto. Osoitetietojen suojaamiseksi vain pääkäyttäjä voi ladata tämän päätöksen."
                        )
                    }
                }
                service.getFeeDecisionPdfResponse(dbc, decisionId)
            }
            .also { Audit.FeeDecisionPdfRead.log(targetId = decisionId) }
    }

    @GetMapping("/{id}")
    fun getFeeDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: FeeDecisionId
    ): FeeDecisionDetailed {
        return db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(it, user, clock, Action.FeeDecision.READ, id)
                it.getFeeDecision(id)
            }
        }
            ?: throw NotFound("No fee decision found with given ID ($id)").also {
                Audit.FeeDecisionRead.log(targetId = id)
            }
    }

    @GetMapping("/head-of-family/{id}")
    fun getHeadOfFamilyFeeDecisions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: PersonId
    ): List<FeeDecision> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.READ_FEE_DECISIONS,
                        id
                    )
                    it.findFeeDecisionsForHeadOfFamily(id, null, null)
                }
            }
            .also {
                Audit.FeeDecisionHeadOfFamilyRead.log(
                    targetId = id,
                    meta = mapOf("count" to it.size)
                )
            }
    }

    @PostMapping("/head-of-family/{id}/create-retroactive")
    fun generateRetroactiveFeeDecisions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: PersonId,
        @RequestBody body: CreateRetroactiveFeeDecisionsBody
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Person.GENERATE_RETROACTIVE_FEE_DECISIONS,
                    id
                )
                generator.createRetroactiveFeeDecisions(it, id, body.from)
            }
        }
        Audit.FeeDecisionHeadOfFamilyCreateRetroactive.log(targetId = id)
    }

    @PostMapping("/set-type/{id}")
    fun setFeeDecisionType(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: FeeDecisionId,
        @RequestBody request: FeeDecisionTypeRequest
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.FeeDecision.UPDATE, id)
                service.setType(it, id, request.type)
            }
        }
        Audit.FeeDecisionSetType.log(targetId = id, meta = mapOf("type" to request.type))
    }
}

data class CreateRetroactiveFeeDecisionsBody(val from: LocalDate)

data class SearchFeeDecisionRequest(
    val page: Int,
    val pageSize: Int,
    val sortBy: FeeDecisionSortParam? = null,
    val sortDirection: SortDirection? = null,
    val statuses: List<FeeDecisionStatus>? = null,
    val area: List<String>? = null,
    val unit: DaycareId? = null,
    val distinctions: List<DistinctiveParams>? = null,
    val searchTerms: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val searchByStartDate: Boolean = false,
    val financeDecisionHandlerId: EmployeeId? = null,
    val difference: Set<FeeDecisionDifference>? = null
)

data class FeeDecisionTypeRequest(val type: FeeDecisionType)
