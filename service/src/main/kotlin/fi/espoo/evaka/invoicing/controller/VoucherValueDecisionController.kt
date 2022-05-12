// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.invoicing.data.annulVoucherValueDecisions
import fi.espoo.evaka.invoicing.data.approveValueDecisionDraftsForSending
import fi.espoo.evaka.invoicing.data.findValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.getHeadOfFamilyVoucherValueDecisions
import fi.espoo.evaka.invoicing.data.getValueDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.data.lockValueDecisions
import fi.espoo.evaka.invoicing.data.lockValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.markVoucherValueDecisionsSent
import fi.espoo.evaka.invoicing.data.searchValueDecisions
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionEndDatesIfNeeded
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
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
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.controllers.Wrapper
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

@RestController
@RequestMapping("/value-decisions")
class VoucherValueDecisionController(
    private val valueDecisionService: VoucherValueDecisionService,
    private val generator: FinanceDecisionGenerator,
    private val accessControl: AccessControl,
    private val evakaEnv: EvakaEnv,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val featureConfig: FeatureConfig
) {
    @PostMapping("/search")
    fun search(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: SearchVoucherValueDecisionRequest
    ): Paged<VoucherValueDecisionSummary> {
        Audit.VoucherValueDecisionSearch.log()
        accessControl.requirePermissionFor(user, Action.Global.SEARCH_VOUCHER_VALUE_DECISIONS)
        val maxPageSize = 5000
        if (body.pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        return db.connect { dbc ->
            dbc
                .read { tx ->
                    tx.searchValueDecisions(
                        body.page,
                        body.pageSize,
                        body.sortBy ?: VoucherValueDecisionSortParam.STATUS,
                        body.sortDirection ?: SortDirection.DESC,
                        body.status?.let { parseEnum<VoucherValueDecisionStatus>(it) }
                            ?: throw BadRequest("Status is a mandatory parameter"),
                        body.area?.split(",") ?: listOf(),
                        body.unit,
                        body.searchTerms ?: "",
                        body.startDate?.let { LocalDate.parse(body.startDate, DateTimeFormatter.ISO_DATE) },
                        body.endDate?.let { LocalDate.parse(body.endDate, DateTimeFormatter.ISO_DATE) },
                        body.searchByStartDate,
                        body.financeDecisionHandlerId
                    )
                }
        }
    }

    @GetMapping("/{id}")
    fun getDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: VoucherValueDecisionId
    ): Wrapper<VoucherValueDecisionDetailed> {
        Audit.VoucherValueDecisionRead.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.VoucherValueDecision.READ, id)
        val res = db.connect { dbc -> dbc.read { it.getVoucherValueDecision(id) } }
            ?: throw NotFound("No voucher value decision found with given ID ($id)")
        return Wrapper(res)
    }

    @GetMapping("/head-of-family/{headOfFamilyId}")
    fun getHeadOfFamilyDecisions(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable headOfFamilyId: PersonId
    ): List<VoucherValueDecisionSummary> {
        Audit.VoucherValueDecisionHeadOfFamilyRead.log(targetId = headOfFamilyId)
        accessControl.requirePermissionFor(user, Action.Person.READ_VOUCHER_VALUE_DECISIONS, headOfFamilyId)
        return db.connect { dbc -> dbc.read { it.getHeadOfFamilyVoucherValueDecisions(headOfFamilyId) } }
    }

    @PostMapping("/send")
    fun sendDrafts(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @RequestBody decisionIds: List<VoucherValueDecisionId>
    ) {
        Audit.VoucherValueDecisionSend.log(targetId = decisionIds)
        accessControl.requirePermissionFor(user, Action.VoucherValueDecision.UPDATE, decisionIds)
        db.connect { dbc ->
            dbc.transaction {
                sendVoucherValueDecisions(
                    tx = it,
                    asyncJobRunner = asyncJobRunner,
                    user = user,
                    evakaEnv = evakaEnv,
                    now = evakaClock.now(),
                    ids = decisionIds,
                    featureConfig.alwaysUseDaycareFinanceDecisionHandler
                )
            }
        }
    }

    @PostMapping("/mark-sent")
    fun markSent(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @RequestBody ids: List<VoucherValueDecisionId>
    ) {
        Audit.VoucherValueDecisionMarkSent.log(targetId = ids)
        accessControl.requirePermissionFor(user, Action.VoucherValueDecision.UPDATE, ids)
        db.connect { dbc ->
            dbc.transaction { tx ->
                val decisions = tx.getValueDecisionsByIds(ids)
                if (decisions.any { it.status != WAITING_FOR_MANUAL_SENDING })
                    throw BadRequest("Voucher value decision cannot be marked sent")
                tx.markVoucherValueDecisionsSent(ids, evakaClock.now())
            }
        }
    }

    @GetMapping("/pdf/{id}")
    fun getDecisionPdf(db: Database, user: AuthenticatedUser, @PathVariable id: VoucherValueDecisionId): ResponseEntity<ByteArray> {
        Audit.FeeDecisionPdfRead.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.VoucherValueDecision.READ, id)

        val (filename, pdf) = db.connect { dbc ->
            dbc.read { tx ->
                val decision = tx.getVoucherValueDecision(id) ?: error("Cannot find voucher value decision $id")

                val personIds = listOfNotNull(
                    decision.headOfFamily.id,
                    decision.partner?.id,
                    decision.child.id
                )
                val restrictedDetails = personIds.any { personId ->
                    tx.getPersonById(personId)?.restrictedDetailsEnabled ?: false
                }
                if (restrictedDetails && !user.isAdmin) {
                    throw Forbidden("Päätöksen alaisella henkilöllä on voimassa turvakielto. Osoitetietojen suojaamiseksi vain pääkäyttäjä voi ladata tämän päätöksen.")
                }

                valueDecisionService.getDecisionPdf(tx, id)
            }
        }

        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"$filename\"")
            .header("Content-Type", "application/pdf")
            .body(pdf)
    }

    @PostMapping("/set-type/{uuid}")
    fun setType(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable uuid: VoucherValueDecisionId,
        @RequestBody request: VoucherValueDecisionTypeRequest
    ) {
        Audit.VoucherValueDecisionSetType.log(targetId = uuid)
        accessControl.requirePermissionFor(user, Action.VoucherValueDecision.UPDATE, uuid)
        db.connect { dbc -> dbc.transaction { valueDecisionService.setType(it, uuid, request.type) } }
    }

    @PostMapping("/head-of-family/{id}/create-retroactive")
    fun generateRetroactiveDecisions(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: PersonId,
        @RequestBody body: CreateRetroactiveFeeDecisionsBody
    ) {
        Audit.VoucherValueDecisionHeadOfFamilyCreateRetroactive.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.Person.GENERATE_RETROACTIVE_VOUCHER_VALUE_DECISIONS, id)
        db.connect { dbc -> dbc.transaction { generator.createRetroactiveValueDecisions(it, id, body.from) } }
    }
}

fun sendVoucherValueDecisions(
    tx: Database.Transaction,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    user: AuthenticatedUser,
    evakaEnv: EvakaEnv,
    now: HelsinkiDateTime,
    ids: List<VoucherValueDecisionId>,
    alwaysUseDaycareFinanceDecisionHandler: Boolean
) {
    tx.lockValueDecisions(ids)
    val decisions = tx.getValueDecisionsByIds(ids)
    if (decisions.isEmpty()) return

    if (decisions.any { it.status != DRAFT }) {
        throw BadRequest("Some voucher value decisions were not drafts")
    }

    val today = now.toLocalDate()
    val lastPossibleDecisionValidFromDate = today.plusDays(evakaEnv.nrOfDaysVoucherValueDecisionCanBeSentInAdvance)
    if (decisions.any { it.validFrom > lastPossibleDecisionValidFromDate }) {
        throw BadRequest(
            "Some of the voucher value decisions are not valid yet",
            "voucherValueDecisions.confirmation.tooFarInFuture"
        )
    }

    val conflicts = decisions
        .flatMap {
            tx.lockValueDecisionsForChild(it.child.id)
            tx.findValueDecisionsForChild(
                it.child.id,
                DateRange(it.validFrom, it.validTo),
                listOf(WAITING_FOR_SENDING, WAITING_FOR_MANUAL_SENDING, SENT)
            )
        }
        .distinctBy { it.id }
        .filter { !ids.contains(it.id) }

    if (conflicts.any { it.status == WAITING_FOR_MANUAL_SENDING }) throw Conflict(
        "Some children have overlapping value decisions waiting for manual sending",
        "WAITING_FOR_MANUAL_SENDING"
    )

    if (conflicts.any { it.status == WAITING_FOR_SENDING }) error("Some children have overlapping value decisions still waiting for sending")

    val (annulled, updatedDates) = updateEndDatesOrAnnulConflictingDecisions(decisions, conflicts)
        .partition { it.status == ANNULLED }
    tx.annulVoucherValueDecisions(annulled.map { it.id }, now)
    tx.updateVoucherValueDecisionEndDatesIfNeeded(updatedDates, now)

    val validIds = decisions.map { it.id }
    tx.approveValueDecisionDraftsForSending(validIds, EmployeeId(user.id), now, alwaysUseDaycareFinanceDecisionHandler)
    asyncJobRunner.plan(tx, validIds.map { AsyncJob.NotifyVoucherValueDecisionApproved(it) })
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
    val unit: DaycareId?,
    val searchTerms: String?,
    val financeDecisionHandlerId: EmployeeId?,
    val startDate: String?,
    val endDate: String?,
    val searchByStartDate: Boolean = false
)

data class VoucherValueDecisionTypeRequest(
    val type: VoucherValueDecisionType
)
