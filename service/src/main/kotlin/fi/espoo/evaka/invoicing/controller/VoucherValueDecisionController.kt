// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.invoicing.data.PagedVoucherValueDecisionSummaries
import fi.espoo.evaka.invoicing.data.annulVoucherValueDecisions
import fi.espoo.evaka.invoicing.data.approveValueDecisionDraftsForSending
import fi.espoo.evaka.invoicing.data.deleteValueDecisions
import fi.espoo.evaka.invoicing.data.findValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.getHeadOfFamilyVoucherValueDecisions
import fi.espoo.evaka.invoicing.data.getValueDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.data.lockValueDecisions
import fi.espoo.evaka.invoicing.data.lockValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.markVoucherValueDecisionsSent
import fi.espoo.evaka.invoicing.data.searchValueDecisions
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionEndDates
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDifference
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus.ANNULLED
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus.DRAFT
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus.SENT
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus.WAITING_FOR_SENDING
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.invoicing.domain.updateEndDatesOrAnnulConflictingDecisions
import fi.espoo.evaka.invoicing.service.FinanceDecisionGenerator
import fi.espoo.evaka.invoicing.service.VoucherValueDecisionService
import fi.espoo.evaka.invoicing.validateFinanceDecisionHandler
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
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

@RestController
@RequestMapping(
    "/value-decisions", // deprecated
    "/employee/value-decisions",
)
class VoucherValueDecisionController(
    private val valueDecisionService: VoucherValueDecisionService,
    private val generator: FinanceDecisionGenerator,
    private val accessControl: AccessControl,
    private val evakaEnv: EvakaEnv,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val featureConfig: FeatureConfig,
) {
    @PostMapping("/search")
    fun searchVoucherValueDecisions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: SearchVoucherValueDecisionRequest,
    ): PagedVoucherValueDecisionSummaries {
        val maxPageSize = 5000
        if (body.pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.SEARCH_VOUCHER_VALUE_DECISIONS,
                    )
                    tx.searchValueDecisions(
                        clock,
                        featureConfig.postOffice,
                        body.page,
                        body.pageSize,
                        body.sortBy ?: VoucherValueDecisionSortParam.STATUS,
                        body.sortDirection ?: SortDirection.DESC,
                        body.statuses,
                        body.area ?: emptyList(),
                        body.unit,
                        body.searchTerms ?: "",
                        body.startDate,
                        body.endDate,
                        body.searchByStartDate,
                        body.financeDecisionHandlerId,
                        body.difference ?: emptySet(),
                        body.distinctions ?: emptyList(),
                    )
                }
            }
            .also { Audit.VoucherValueDecisionSearch.log(meta = mapOf("total" to it.total)) }
    }

    @GetMapping("/{id}")
    fun getVoucherValueDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: VoucherValueDecisionId,
    ): VoucherValueDecisionDetailed {
        return db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.VoucherValueDecision.READ,
                    id,
                )
                it.getVoucherValueDecision(id)
            }
        }
            ?: throw NotFound("No voucher value decision found with given ID ($id)").also {
                Audit.VoucherValueDecisionRead.log(targetId = AuditId(id))
            }
    }

    @GetMapping("/head-of-family/{headOfFamilyId}")
    fun getHeadOfFamilyVoucherValueDecisions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable headOfFamilyId: PersonId,
    ): List<VoucherValueDecisionSummary> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.READ_VOUCHER_VALUE_DECISIONS,
                        headOfFamilyId,
                    )
                    it.getHeadOfFamilyVoucherValueDecisions(headOfFamilyId)
                }
            }
            .also {
                Audit.VoucherValueDecisionHeadOfFamilyRead.log(targetId = AuditId(headOfFamilyId))
            }
    }

    @PostMapping("/send")
    fun sendVoucherValueDecisionDrafts(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody decisionIds: List<VoucherValueDecisionId>,
        @RequestParam decisionHandlerId: EmployeeId?,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.VoucherValueDecision.UPDATE,
                    decisionIds,
                )
                sendVoucherValueDecisions(
                    tx = it,
                    asyncJobRunner = asyncJobRunner,
                    user = user,
                    evakaEnv = evakaEnv,
                    now = clock.now(),
                    ids = decisionIds,
                    decisionHandlerId = decisionHandlerId,
                    featureConfig.alwaysUseDaycareFinanceDecisionHandler,
                )
            }
        }
        Audit.VoucherValueDecisionSend.log(targetId = AuditId(decisionIds))
    }

    @PostMapping("/mark-sent")
    fun markVoucherValueDecisionSent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody ids: List<VoucherValueDecisionId>,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.VoucherValueDecision.UPDATE,
                    ids,
                )
                val decisions = tx.getValueDecisionsByIds(ids)
                if (decisions.any { it.status != WAITING_FOR_MANUAL_SENDING }) {
                    throw BadRequest("Voucher value decision cannot be marked sent")
                }
                tx.markVoucherValueDecisionsSent(ids, clock.now())
                asyncJobRunner.plan(
                    tx,
                    ids.map { AsyncJob.SendNewVoucherValueDecisionEmail(decisionId = it) },
                    runAt = clock.now(),
                )
            }
        }
        Audit.VoucherValueDecisionMarkSent.log(targetId = AuditId(ids))
    }

    @GetMapping("/pdf/{decisionId}")
    fun getVoucherValueDecisionPdf(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable decisionId: VoucherValueDecisionId,
    ): ResponseEntity<Any> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.VoucherValueDecision.READ,
                        decisionId,
                    )
                    val decision =
                        tx.getVoucherValueDecision(decisionId)
                            ?: error("Cannot find voucher value decision $decisionId")

                    val personIds =
                        listOfNotNull(
                            decision.headOfFamily.id,
                            decision.partner?.id,
                            decision.child.id,
                        )
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

                valueDecisionService.getDecisionPdfResponse(dbc, decisionId)
            }
            .also { Audit.VoucherValueDecisionPdfRead.log(targetId = AuditId(decisionId)) }
    }

    @PostMapping("/ignore")
    fun ignoreVoucherValueDecisionDrafts(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody voucherValueDecisionIds: List<VoucherValueDecisionId>,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.VoucherValueDecision.IGNORE,
                    voucherValueDecisionIds,
                )
                valueDecisionService.ignoreDrafts(tx, voucherValueDecisionIds, clock.today())
            }
        }
        Audit.VoucherValueDecisionIgnore.log(targetId = AuditId(voucherValueDecisionIds))
    }

    @PostMapping("/unignore")
    fun unignoreVoucherValueDecisionDrafts(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody voucherValueDecisionIds: List<VoucherValueDecisionId>,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.VoucherValueDecision.UNIGNORE,
                    voucherValueDecisionIds,
                )
                val headsOfFamilies =
                    valueDecisionService.unignoreDrafts(tx, voucherValueDecisionIds)
                asyncJobRunner.plan(
                    tx,
                    headsOfFamilies.map { personId ->
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            personId,
                            DateRange(clock.today().minusMonths(15), null),
                        )
                    },
                    runAt = clock.now(),
                )
            }
        }
        Audit.VoucherValueDecisionUnignore.log(targetId = AuditId(voucherValueDecisionIds))
    }

    @PostMapping("/set-type/{id}")
    fun setVoucherValueDecisionType(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: VoucherValueDecisionId,
        @RequestBody request: VoucherValueDecisionTypeRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.VoucherValueDecision.UPDATE,
                    id,
                )
                valueDecisionService.setType(it, id, request.type)
            }
        }
        Audit.VoucherValueDecisionSetType.log(targetId = AuditId(id))
    }

    @PostMapping("/head-of-family/{id}/create-retroactive")
    fun generateRetroactiveVoucherValueDecisions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: PersonId,
        @RequestBody body: CreateRetroactiveFeeDecisionsBody,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Person.GENERATE_RETROACTIVE_VOUCHER_VALUE_DECISIONS,
                    id,
                )
                generator.createRetroactiveValueDecisions(it, id, body.from)
            }
        }
        Audit.VoucherValueDecisionHeadOfFamilyCreateRetroactive.log(targetId = AuditId(id))
    }
}

fun sendVoucherValueDecisions(
    tx: Database.Transaction,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    user: AuthenticatedUser.Employee,
    evakaEnv: EvakaEnv,
    now: HelsinkiDateTime,
    ids: List<VoucherValueDecisionId>,
    decisionHandlerId: EmployeeId?,
    alwaysUseDaycareFinanceDecisionHandler: Boolean,
) {
    tx.lockValueDecisions(ids)
    val decisions = tx.getValueDecisionsByIds(ids)
    if (decisions.isEmpty()) return

    if (decisions.any { it.status != DRAFT }) {
        throw BadRequest("Some voucher value decisions were not drafts")
    }

    val today = now.toLocalDate()
    val lastPossibleDecisionValidFromDate =
        today.plusDays(evakaEnv.nrOfDaysVoucherValueDecisionCanBeSentInAdvance)
    if (decisions.any { it.validFrom > lastPossibleDecisionValidFromDate }) {
        throw BadRequest(
            "Some of the voucher value decisions are not valid yet",
            "voucherValueDecisions.confirmation.tooFarInFuture",
        )
    }

    decisionHandlerId?.let { validateFinanceDecisionHandler(tx, it) }

    val conflicts =
        decisions
            .flatMap {
                tx.lockValueDecisionsForChild(it.child.id)
                tx.findValueDecisionsForChild(
                    it.child.id,
                    DateRange(it.validFrom, it.validTo),
                    listOf(WAITING_FOR_SENDING, WAITING_FOR_MANUAL_SENDING, SENT),
                )
            }
            .distinctBy { it.id }
            .filter { !ids.contains(it.id) }

    if (conflicts.any { it.status == WAITING_FOR_MANUAL_SENDING }) {
        throw Conflict(
            "Some children have overlapping value decisions waiting for manual sending",
            "WAITING_FOR_MANUAL_SENDING",
        )
    }

    if (conflicts.any { it.status == WAITING_FOR_SENDING })
        error("Some children have overlapping value decisions still waiting for sending")

    val (annulled, updatedDates) =
        updateEndDatesOrAnnulConflictingDecisions(decisions, conflicts).partition {
            it.status == ANNULLED
        }
    tx.annulVoucherValueDecisions(annulled.map { it.id }, now)
    tx.updateVoucherValueDecisionEndDates(updatedDates, now)

    val (emptyDecisions, validDecisions) = decisions.partition { it.isEmpty() }
    tx.deleteValueDecisions(emptyDecisions.map { it.id })
    val validIds = validDecisions.map { it.id }
    tx.approveValueDecisionDraftsForSending(
        validIds,
        user.id,
        now,
        decisionHandlerId,
        alwaysUseDaycareFinanceDecisionHandler,
    )
    asyncJobRunner.plan(
        tx,
        validIds.map { AsyncJob.NotifyVoucherValueDecisionApproved(it) },
        runAt = now,
    )
}

enum class VoucherValueDecisionSortParam {
    HEAD_OF_FAMILY,
    CHILD,
    VALIDITY,
    VOUCHER_VALUE,
    FINAL_CO_PAYMENT,
    NUMBER,
    CREATED,
    SENT,
    STATUS,
}

@ConstList("voucherValueDecisionDistinctiveParams")
enum class VoucherValueDecisionDistinctiveParams {
    UNCONFIRMED_HOURS,
    EXTERNAL_CHILD,
    RETROACTIVE,
    NO_STARTING_PLACEMENTS,
    MAX_FEE_ACCEPTED,
}

data class SearchVoucherValueDecisionRequest(
    val page: Int,
    val pageSize: Int,
    val sortBy: VoucherValueDecisionSortParam?,
    val sortDirection: SortDirection?,
    val statuses: List<VoucherValueDecisionStatus>,
    val area: List<String>?,
    val unit: DaycareId?,
    val distinctions: List<VoucherValueDecisionDistinctiveParams>?,
    val searchTerms: String?,
    val financeDecisionHandlerId: EmployeeId?,
    val difference: Set<VoucherValueDecisionDifference>?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val searchByStartDate: Boolean = false,
)

data class VoucherValueDecisionTypeRequest(val type: VoucherValueDecisionType)
