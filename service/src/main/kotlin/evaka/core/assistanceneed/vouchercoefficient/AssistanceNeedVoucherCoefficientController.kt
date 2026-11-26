// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.assistanceneed.vouchercoefficient

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.shared.AssistanceNeedVoucherCoefficientId
import evaka.core.shared.ChildId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.data.DateSet
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AssistanceNeedVoucherCoefficientController(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    @PostMapping("/employee/children/{childId}/assistance-need-voucher-coefficients")
    fun createAssistanceNeedVoucherCoefficient(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: AssistanceNeedVoucherCoefficientRequest,
    ): AssistanceNeedVoucherCoefficient {
        val audit = AuditContext().add(childId).observeDate(body.validityPeriod.start)
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_ASSISTANCE_NEED_VOUCHER_COEFFICIENT,
                        childId,
                    )
                    adjustExistingCoefficients(
                        tx = tx,
                        user = user,
                        now = clock.now(),
                        childId = childId,
                        range = body.validityPeriod,
                        ignoreCoefficientId = null,
                        audit = audit,
                    )
                    tx.insertAssistanceNeedVoucherCoefficient(user, clock.now(), childId, body)
                        .also {
                            audit.add(it.id)
                            asyncJobRunner.plan(
                                tx,
                                listOf(
                                    AsyncJob.GenerateFinanceDecisions.forChild(
                                        childId,
                                        body.validityPeriod.asDateRange(),
                                    ),
                                ),
                                runAt = clock.now(),
                            )
                        }
                }
            }
            .also { audit.log(Audit.ChildAssistanceNeedVoucherCoefficientCreate, clock) }
    }

    @GetMapping("/employee/children/{childId}/assistance-need-voucher-coefficients")
    fun getAssistanceNeedVoucherCoefficients(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): List<AssistanceNeedVoucherCoefficientResponse> {
        val audit = AuditContext().add(childId)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_ASSISTANCE_NEED_VOUCHER_COEFFICIENTS,
                        childId,
                    )
                    tx.getAssistanceNeedVoucherCoefficientsForChild(childId)
                        .also { coefficients -> audit.add(coefficients.map { it.id }) }
                        .map {
                            AssistanceNeedVoucherCoefficientResponse(
                                voucherCoefficient = it,
                                permittedActions =
                                    accessControl.getPermittedActions(tx, user, clock, it.id),
                            )
                        }
                }
            }
            .also { audit.log(Audit.ChildAssistanceNeedVoucherCoefficientRead, clock) }
    }

    @PutMapping("/employee/assistance-need-voucher-coefficients/{id}")
    fun updateAssistanceNeedVoucherCoefficient(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedVoucherCoefficientId,
        @RequestBody body: AssistanceNeedVoucherCoefficientRequest,
    ): AssistanceNeedVoucherCoefficient {
        val audit = AuditContext().add(id).observeDate(body.validityPeriod.start)
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedVoucherCoefficient.UPDATE,
                        id,
                    )
                    val existing =
                        tx.getAssistanceNeedVoucherCoefficientById(id).also {
                            audit.add(it.childId).observeDate(it.validityPeriod.start)
                        }
                    adjustExistingCoefficients(
                        tx = tx,
                        user = user,
                        now = clock.now(),
                        childId = existing.childId,
                        range = body.validityPeriod,
                        ignoreCoefficientId = id,
                        audit = audit,
                    )

                    val combinedRange =
                        DateSet.of(existing.validityPeriod, body.validityPeriod)
                            .spanningRange()!!
                            .asDateRange()
                    tx.updateAssistanceNeedVoucherCoefficient(
                            user = user,
                            now = clock.now(),
                            id = id,
                            data = body,
                        )
                        .also {
                            asyncJobRunner.plan(
                                tx,
                                listOf(
                                    AsyncJob.GenerateFinanceDecisions.forChild(
                                        existing.childId,
                                        combinedRange,
                                    ),
                                ),
                                runAt = clock.now(),
                            )
                        }
                }
            }
            .also { audit.log(Audit.ChildAssistanceNeedVoucherCoefficientUpdate, clock) }
    }

    @DeleteMapping("/employee/assistance-need-voucher-coefficients/{id}")
    fun deleteAssistanceNeedVoucherCoefficient(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedVoucherCoefficientId,
    ) {
        val audit = AuditContext().add(id)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedVoucherCoefficient.DELETE,
                        id,
                    )
                    val existing =
                        tx.deleteAssistanceNeedVoucherCoefficient(id)
                            ?: throw NotFound(
                                "Assistance need voucher coefficient $id not found or cannot be deleted",
                                "VOUCHER_COEFFICIENT_NOT_FOUND",
                            )
                    audit
                        .add(existing.childId)
                        .observeDate(existing.validityPeriod.start)
                        .addMeta("startDate", existing.validityPeriod.start)
                        .addMeta("endDate", existing.validityPeriod.end)
                    asyncJobRunner.plan(
                        tx,
                        listOf(
                            AsyncJob.GenerateFinanceDecisions.forChild(
                                existing.childId,
                                existing.validityPeriod.asDateRange(),
                            ),
                        ),
                        runAt = clock.now(),
                    )
                }
            }
            .also { audit.log(Audit.ChildAssistanceNeedVoucherCoefficientDelete, clock) }
    }

    private fun adjustExistingCoefficients(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        now: HelsinkiDateTime,
        childId: ChildId,
        range: FiniteDateRange,
        ignoreCoefficientId: AssistanceNeedVoucherCoefficientId?,
        audit: AuditContext,
    ) {
        val overlappingCoefficients =
            tx.getOverlappingAssistanceNeedVoucherCoefficientsForChild(
                    childId = childId,
                    range = range,
                )
                .filterNot { it.id == ignoreCoefficientId }
                .also { coefficients -> audit.add(coefficients.map { it.id }) }

        if (overlappingCoefficients.isNotEmpty()) {
            overlappingCoefficients.forEach {
                if (range.contains(it.validityPeriod)) {
                    tx.deleteAssistanceNeedVoucherCoefficient(it.id)
                } else {
                    tx.updateAssistanceNeedVoucherCoefficient(
                        user = user,
                        now = now,
                        id = it.id,
                        data =
                            AssistanceNeedVoucherCoefficientRequest(
                                coefficient = it.coefficient.toDouble(),
                                validityPeriod =
                                    if (
                                        range.start <= it.validityPeriod.end &&
                                            range.start >= it.validityPeriod.start
                                    ) {
                                        it.validityPeriod.copy(end = range.start.minusDays(1))
                                    } else if (
                                        range.end >= it.validityPeriod.start &&
                                            range.end <= it.validityPeriod.end
                                    ) {
                                        it.validityPeriod.copy(start = range.end.plusDays(1))
                                    } else {
                                        it.validityPeriod
                                    },
                            ),
                    )
                }
            }
        }
    }
}
