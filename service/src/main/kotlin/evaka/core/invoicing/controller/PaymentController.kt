// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.controller

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.invoicing.data.PagedPayments
import evaka.core.invoicing.data.deleteDraftPayments
import evaka.core.invoicing.data.searchPayments
import evaka.core.invoicing.domain.PaymentStatus
import evaka.core.invoicing.domain.createPaymentDrafts
import evaka.core.invoicing.service.PaymentService
import evaka.core.shared.DaycareId
import evaka.core.shared.PaymentId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PaymentController(
    private val accessControl: AccessControl,
    private val paymentService: PaymentService,
) {
    @PostMapping("/employee/payments/search")
    fun searchPayments(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody params: SearchPaymentsRequest,
    ): PagedPayments {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.SEARCH_PAYMENTS,
                    )
                    tx.searchPayments(params)
                }
            }
            .also { Audit.PaymentsSearch.log(meta = mapOf("total" to it.total)) }
    }

    @PostMapping("/employee/payments/create-drafts")
    fun createPaymentDrafts(db: Database, user: AuthenticatedUser.Employee, clock: EvakaClock) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.CREATE_DRAFT_PAYMENTS,
                )
                createPaymentDrafts(tx)
            }
        }
        Audit.PaymentsCreate.log()
    }

    data class SendPaymentsRequest(
        val paymentDate: LocalDate,
        val dueDate: LocalDate,
        val paymentIds: List<PaymentId>,
    )

    @PostMapping("/employee/payments/delete-drafts")
    fun deleteDraftPayments(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody paymentIds: List<PaymentId>,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Payment.DELETE,
                    paymentIds,
                )
                it.deleteDraftPayments(paymentIds)
            }
        }
        Audit.PaymentsDeleteDrafts.log(targetId = AuditId(paymentIds))
    }

    @PostMapping("/employee/payments/send")
    fun sendPayments(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: SendPaymentsRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Payment.SEND,
                    body.paymentIds,
                )
                paymentService.sendPayments(
                    tx,
                    clock.now(),
                    user,
                    body.paymentIds,
                    body.paymentDate,
                    body.dueDate,
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
        @RequestBody paymentIds: List<PaymentId>,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Payment.CONFIRM,
                    paymentIds,
                )
                paymentService.confirmPayments(it, paymentIds)
            }
        }
        Audit.PaymentsConfirmDrafts.log(targetId = AuditId(paymentIds))
    }

    @PostMapping("/employee/payments/revert-to-draft")
    fun revertPaymentsToDrafts(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody paymentIds: List<PaymentId>,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Payment.REVERT_TO_DRAFT,
                    paymentIds,
                )
                paymentService.revertPaymentsToDrafts(it, paymentIds)
            }
        }
        Audit.PaymentsRevertToDrafts.log(targetId = AuditId(paymentIds))
    }
}

data class SearchPaymentsRequest(
    val page: Int,
    val sortBy: PaymentSortParam,
    val sortDirection: SortDirection,
    val searchTerms: String,
    val area: List<String>,
    val unit: DaycareId?,
    val distinctions: List<PaymentDistinctiveParams>,
    val status: PaymentStatus,
    val paymentDateStart: LocalDate?,
    val paymentDateEnd: LocalDate?,
)

enum class PaymentSortParam {
    UNIT,
    PERIOD,
    CREATED,
    NUMBER,
    AMOUNT,
}

enum class PaymentDistinctiveParams {
    MISSING_PAYMENT_DETAILS
}
