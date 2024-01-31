// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/payments")
class PaymentController(
    private val accessControl: AccessControl,
    private val paymentService: PaymentService
) {
    @PostMapping("/search")
    fun searchPayments(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody params: SearchPaymentsRequest
    ): PagedPayments {
        return db.connect { dbc -> dbc.read { tx -> tx.searchPayments(params) } }
    }

    @PostMapping("/create-drafts")
    fun createDrafts(db: Database, user: AuthenticatedUser, clock: EvakaClock) {
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

    @PostMapping("/delete-drafts")
    fun deleteDraftPayments(
        db: Database,
        user: AuthenticatedUser,
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
        Audit.PaymentsDeleteDrafts.log(targetId = paymentIds)
    }

    @PostMapping("/send")
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
        Audit.PaymentsSend.log(targetId = body.paymentIds)
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
