// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.controller

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.ConstList
import evaka.core.EvakaEnv
import evaka.core.caseprocess.CaseProcessMetadataService
import evaka.core.caseprocess.CaseProcessState
import evaka.core.caseprocess.getCaseProcessByVoucherValueDecisionId
import evaka.core.caseprocess.insertCaseProcess
import evaka.core.caseprocess.insertCaseProcessHistoryRow
import evaka.core.document.archival.validateArchivability
import evaka.core.invoicing.data.PagedVoucherValueDecisionSummaries
import evaka.core.invoicing.data.annulVoucherValueDecisions
import evaka.core.invoicing.data.approveValueDecisionDraftsForSending
import evaka.core.invoicing.data.deleteValueDecisions
import evaka.core.invoicing.data.findValueDecisionsForChild
import evaka.core.invoicing.data.getHeadOfFamilyVoucherValueDecisions
import evaka.core.invoicing.data.getValueDecisionsByIds
import evaka.core.invoicing.data.getVoucherValueDecision
import evaka.core.invoicing.data.lockValueDecisions
import evaka.core.invoicing.data.lockValueDecisionsForChild
import evaka.core.invoicing.data.markVoucherValueDecisionsSent
import evaka.core.invoicing.data.searchValueDecisions
import evaka.core.invoicing.data.setVoucherValueDecisionProcessId
import evaka.core.invoicing.data.updateVoucherValueDecisionEndDates
import evaka.core.invoicing.domain.VoucherValueDecisionDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionDifference
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionStatus.ANNULLED
import evaka.core.invoicing.domain.VoucherValueDecisionStatus.DRAFT
import evaka.core.invoicing.domain.VoucherValueDecisionStatus.SENT
import evaka.core.invoicing.domain.VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING
import evaka.core.invoicing.domain.VoucherValueDecisionStatus.WAITING_FOR_SENDING
import evaka.core.invoicing.domain.VoucherValueDecisionSummary
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.invoicing.domain.updateEndDatesOrAnnulConflictingDecisions
import evaka.core.invoicing.service.FinanceDecisionGenerator
import evaka.core.invoicing.service.VoucherValueDecisionService
import evaka.core.invoicing.validateFinanceDecisionHandler
import evaka.core.pis.getPersonById
import evaka.core.shared.ArchiveProcessType
import evaka.core.shared.DaycareId
import evaka.core.shared.EmployeeId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.PersonId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
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
@RequestMapping("/employee/value-decisions")
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
                        pageSize = 200,
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

    data class VoucherValueDecisionResponse(
        val data: VoucherValueDecisionDetailed,
        val permittedActions: Set<Action.VoucherValueDecision>,
    )

    @GetMapping("/{id}")
    fun getVoucherValueDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: VoucherValueDecisionId,
    ): VoucherValueDecisionResponse {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.VoucherValueDecision.READ,
                        id,
                    )
                    val decision =
                        it.getVoucherValueDecision(id)
                            ?: throw NotFound("No voucher value decision found with given ID ($id)")
                    VoucherValueDecisionResponse(
                        data = decision,
                        permittedActions = accessControl.getPermittedActions(it, user, clock, id),
                    )
                }
            }
            .also { Audit.VoucherValueDecisionRead.log(targetId = AuditId(id)) }
    }

    data class VoucherValueDecisionSummaryWithPermittedActions(
        val data: VoucherValueDecisionSummary,
        val permittedActions: Set<Action.VoucherValueDecision>,
    )

    @GetMapping("/head-of-family/{headOfFamilyId}")
    fun getHeadOfFamilyVoucherValueDecisions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable headOfFamilyId: PersonId,
    ): List<VoucherValueDecisionSummaryWithPermittedActions> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.READ_VOUCHER_VALUE_DECISIONS,
                        headOfFamilyId,
                    )
                    val decisions = it.getHeadOfFamilyVoucherValueDecisions(headOfFamilyId)
                    val permittedActions =
                        accessControl.getPermittedActions<
                            VoucherValueDecisionId,
                            Action.VoucherValueDecision,
                        >(
                            it,
                            user,
                            clock,
                            decisions.map(VoucherValueDecisionSummary::id),
                        )
                    decisions.map { decision ->
                        VoucherValueDecisionSummaryWithPermittedActions(
                            decision,
                            permittedActions[decision.id] ?: emptySet(),
                        )
                    }
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
                    metadata = CaseProcessMetadataService(featureConfig),
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
                val now = clock.now()

                tx.markVoucherValueDecisionsSent(ids, now)

                ids.forEach { id ->
                    tx.getCaseProcessByVoucherValueDecisionId(id)?.let { process ->
                        tx.insertCaseProcessHistoryRow(
                            processId = process.id,
                            state = CaseProcessState.COMPLETED,
                            now = now,
                            userId = user.evakaUserId,
                        )
                    }
                }

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

    @PostMapping("/{id}/archive")
    fun planArchiveVoucherValueDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: VoucherValueDecisionId,
        archivalEnabled: Boolean = evakaEnv.archivalEnabled,
    ) {
        if (!archivalEnabled) {
            throw BadRequest("Archival is not enabled")
        }

        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.VoucherValueDecision.ARCHIVE,
                    id,
                )

                val decision =
                    tx.getVoucherValueDecision(id)
                        ?: throw NotFound("Voucher value decision $id not found")
                validateArchivability(decision)

                asyncJobRunner.plan(
                    tx = tx,
                    payloads = listOf(AsyncJob.ArchiveVoucherValueDecision(decision.id, user)),
                    runAt = clock.now(),
                    retryCount = 1,
                )
            }
        }
    }
}

fun sendVoucherValueDecisions(
    tx: Database.Transaction,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    user: AuthenticatedUser.Employee,
    evakaEnv: EvakaEnv,
    metadata: CaseProcessMetadataService,
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

    val process = metadata.getProcessParams(ArchiveProcessType.VOUCHER_VALUE_DECISION, today.year)
    if (process != null) {
        // TODO: Could be heavy. Does this need to be moved into async jobs?
        validDecisions.forEach { decision ->
            val processId = tx.insertCaseProcess(process).id
            tx.insertCaseProcessHistoryRow(
                processId = processId,
                state = CaseProcessState.INITIAL,
                now = decision.created, // retroactive initial state
                userId = AuthenticatedUser.SystemInternalUser.evakaUserId,
            )
            tx.insertCaseProcessHistoryRow(
                processId = processId,
                state = CaseProcessState.DECIDING,
                now = now,
                userId = user.evakaUserId,
            )
            tx.setVoucherValueDecisionProcessId(decision.id, processId)
        }
    }

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
    NO_OPEN_INCOME_STATEMENTS,
}

data class SearchVoucherValueDecisionRequest(
    val page: Int,
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
