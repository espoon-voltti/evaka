// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.vouchercoefficient

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.AssistanceNeedVoucherCoefficientId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AssistanceNeedVoucherCoefficientController(
    private val accessControl: AccessControl
) {
    @PostMapping("/children/{childId}/assistance-need-voucher-coefficients")
    fun createAssistanceNeedVoucherCoefficient(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @RequestBody body: AssistanceNeedVoucherCoefficientRequest
    ): AssistanceNeedVoucherCoefficient {
        Audit.ChildAssistanceNeedVoucherCoefficientCreate.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.CREATE_ASSISTANCE_NEED_VOUCHER_COEFFICIENT, childId)

        return db.connect { dbc ->
            dbc.transaction { tx ->
                adjustExistingCoefficients(tx, childId, body.validityPeriod, null)
                tx.insertAssistanceNeedVoucherCoefficient(childId, body)
            }
        }
    }

    @GetMapping("/children/{childId}/assistance-need-voucher-coefficients")
    fun getAssistanceNeedVoucherCoefficients(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): List<AssistanceNeedVoucherCoefficientResponse> {
        Audit.ChildAssistanceNeedVoucherCoefficientRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_ASSISTANCE_NEED_VOUCHER_COEFFICIENTS, childId)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                tx.getAssistanceNeedVoucherCoefficientsForChild(childId).map {
                    AssistanceNeedVoucherCoefficientResponse(
                        voucherCoefficient = it,
                        permittedActions = accessControl.getPermittedActions(tx, user, it.id)
                    )
                }
            }
        }
    }

    @PutMapping("/assistance-need-voucher-coefficients/{id}")
    fun updateAssistanceNeedVoucherCoefficient(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("id") assistanceNeedVoucherCoefficientId: AssistanceNeedVoucherCoefficientId,
        @RequestBody body: AssistanceNeedVoucherCoefficientRequest
    ): AssistanceNeedVoucherCoefficient {
        Audit.ChildAssistanceNeedVoucherCoefficientUpdate.log(targetId = assistanceNeedVoucherCoefficientId)
        accessControl.requirePermissionFor(user, Action.AssistanceNeedVoucherCoefficient.UPDATE, assistanceNeedVoucherCoefficientId)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                adjustExistingCoefficients(
                    tx,
                    tx.getAssistanceNeedVoucherCoefficientById(assistanceNeedVoucherCoefficientId).childId,
                    body.validityPeriod,
                    assistanceNeedVoucherCoefficientId
                )
                tx.updateAssistanceNeedVoucherCoefficient(
                    id = assistanceNeedVoucherCoefficientId,
                    data = body
                )
            }
        }
    }

    @DeleteMapping("/assistance-need-voucher-coefficients/{id}")
    fun deleteAssistanceNeedVoucherCoefficient(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("id") assistanceNeedVoucherCoefficientId: AssistanceNeedVoucherCoefficientId
    ) {
        Audit.ChildAssistanceNeedVoucherCoefficientDelete.log(targetId = assistanceNeedVoucherCoefficientId)
        accessControl.requirePermissionFor(user, Action.AssistanceNeedVoucherCoefficient.DELETE, assistanceNeedVoucherCoefficientId)
        db.connect { dbc ->
            dbc.transaction { tx ->
                if (!tx.deleteAssistanceNeedVoucherCoefficient(assistanceNeedVoucherCoefficientId)) {
                    throw NotFound(
                        "Assistance need voucher coefficient $assistanceNeedVoucherCoefficientId cannot found or cannot be deleted",
                        "VOUCHER_COEFFICIENT_NOT_FOUND"
                    )
                }
            }
        }
    }

    private fun adjustExistingCoefficients(tx: Database.Transaction, childId: ChildId, range: FiniteDateRange, ignoreCoefficientId: AssistanceNeedVoucherCoefficientId?) {
        val overlappingCoefficients = tx.getOverlappingAssistanceNeedVoucherCoefficientsForChild(
            childId = childId,
            range = range
        ).filterNot { it.id == ignoreCoefficientId }

        if (overlappingCoefficients.isNotEmpty()) {
            overlappingCoefficients.forEach {
                if (range.contains(it.validityPeriod)) {
                    Audit.ChildAssistanceNeedVoucherCoefficientAutoDelete.log(targetId = it.id, objectId = childId)
                    tx.deleteAssistanceNeedVoucherCoefficient(it.id)
                } else {
                    Audit.ChildAssistanceNeedVoucherCoefficientAutoUpdate.log(targetId = it.id, objectId = childId)
                    tx.updateAssistanceNeedVoucherCoefficient(
                        id = it.id,
                        data = AssistanceNeedVoucherCoefficientRequest(
                            coefficient = it.coefficient.toDouble(),
                            validityPeriod =
                            if (range.start <= it.validityPeriod.end && range.start >= it.validityPeriod.start)
                                it.validityPeriod.copy(end = range.start.minusDays(1))
                            else if (range.end >= it.validityPeriod.start && range.end <= it.validityPeriod.end)
                                it.validityPeriod.copy(start = range.end.plusDays(1))
                            else
                                it.validityPeriod
                        )
                    )
                }
            }
        }
    }
}
