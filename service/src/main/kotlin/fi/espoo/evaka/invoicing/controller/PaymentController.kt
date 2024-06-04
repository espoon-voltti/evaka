// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.invoicing.data.PagedPayments
import fi.espoo.evaka.invoicing.data.deleteDraftPayments
import fi.espoo.evaka.invoicing.data.searchPayments
import fi.espoo.evaka.invoicing.domain.PaymentStatus
import fi.espoo.evaka.invoicing.domain.createPaymentDrafts
import fi.espoo.evaka.invoicing.service.PaymentService
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PaymentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PaymentController(
    private val accessControl: AccessControl,
    private val paymentService: PaymentService
) {
    @PostMapping(path = ["/payments/search", "/employee/payments/search"])
    fun searchPayments(
        db: Database,
        user: AuthenticatedUser.Employee,
        @RequestBody params: SearchPaymentsRequest
    ): PagedPayments {
        return db.connect { dbc -> dbc.read { tx -> tx.searchPayments(params) } }
    }

    @PostMapping(path = ["/payments/create-drafts", "/employee/payments/create-drafts"])
    fun createPaymentDrafts(db: Database, user: AuthenticatedUser.Employee, clock: EvakaClock) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.CREATE_DRAFT_PAYMENTS
                )
                createPaymentDrafts(tx)
            }
        }
        Audit.PaymentsCreate.log()
    }

    data class SendPaymentsRequest(
        val paymentDate: LocalDate,
        val dueDate: LocalDate,
        val paymentIds: List<PaymentId>
    )

    @PostMapping(path = ["/payments/delete-drafts", "/employee/payments/delete-drafts"])
    fun deleteDraftPayments(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody paymentIds: List<PaymentId>
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Payment.DELETE,
                    paymentIds
                )
                it.deleteDraftPayments(paymentIds)
            }
        }
        Audit.PaymentsDeleteDrafts.log(targetId = AuditId(paymentIds))
    }

    @PostMapping(path = ["/payments/send", "/employee/payments/send"])
    fun sendPayments(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: SendPaymentsRequest
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Payment.SEND,
                    body.paymentIds
                )
                paymentService.sendPayments(
                    tx,
                    clock.now(),
                    user,
                    body.paymentIds,
                    body.paymentDate,
                    body.dueDate
                )
            }
        }
        Audit.PaymentsSend.log(targetId = AuditId(body.paymentIds))
    }

    @PostMapping("/employee/payments/confirm")
    fun confirmDraftPayments(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody paymentIds: List<PaymentId>
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Payment.CONFIRM,
                    paymentIds
                )
                paymentService.confirmPayments(it, paymentIds)
            }
        }
        Audit.PaymentsConfirmDrafts.log(targetId = paymentIds)
    }

    @PostMapping("/employee/payments/revert-to-draft")
    fun revertPaymentsToDrafts(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody paymentIds: List<PaymentId>
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Payment.REVERT_TO_DRAFT,
                    paymentIds
                )
                paymentService.revertPaymentsToDrafts(it, paymentIds)
            }
        }
        Audit.PaymentsRevertToDrafts.log(targetId = paymentIds)
    }
}

data class SearchPaymentsRequest(
    val page: Int,
    val pageSize: Int,
    val sortBy: PaymentSortParam,
    val sortDirection: SortDirection,
    val searchTerms: String,
    val area: List<String>,
    val unit: DaycareId?,
    val distinctions: List<PaymentDistinctiveParams>,
    val status: PaymentStatus,
    val paymentDateStart: LocalDate?,
    val paymentDateEnd: LocalDate?
)

enum class PaymentSortParam {
    UNIT,
    PERIOD,
    CREATED,
    NUMBER,
    AMOUNT
}

enum class PaymentDistinctiveParams {
    MISSING_PAYMENT_DETAILS
}
