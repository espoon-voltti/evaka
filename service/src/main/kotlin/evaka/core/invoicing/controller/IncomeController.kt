// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.controller

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.attachment.AttachmentParent
import evaka.core.attachment.associateOrphanAttachments
import evaka.core.invoicing.data.deleteIncome
import evaka.core.invoicing.data.endEarlierOverlappingIncome
import evaka.core.invoicing.data.getIncome
import evaka.core.invoicing.data.getIncomesForPerson
import evaka.core.invoicing.data.insertIncome
import evaka.core.invoicing.data.updateIncome
import evaka.core.invoicing.domain.Income
import evaka.core.invoicing.domain.IncomeCoefficient
import evaka.core.invoicing.domain.IncomeEffect
import evaka.core.invoicing.domain.IncomeRequest
import evaka.core.invoicing.domain.IncomeType
import evaka.core.invoicing.service.IncomeCoefficientMultiplierProvider
import evaka.core.invoicing.service.IncomeNotification
import evaka.core.invoicing.service.IncomeTypesProvider
import evaka.core.invoicing.service.getIncomeNotifications
import evaka.core.shared.IncomeId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.maxEndDate
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.math.BigDecimal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/incomes")
class IncomeController(
    private val incomeTypesProvider: IncomeTypesProvider,
    private val coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val accessControl: AccessControl,
) {
    @GetMapping
    fun getPersonIncomes(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam personId: PersonId,
    ): List<IncomeWithPermittedActions> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_INCOME,
                        personId,
                    )

                    val incomes =
                        tx.getIncomesForPerson(
                            incomeTypesProvider,
                            coefficientMultiplierProvider,
                            personId,
                        )
                    val permittedActions =
                        accessControl.getPermittedActions<IncomeId, Action.Income>(
                            tx,
                            user,
                            clock,
                            incomes.map { it.id },
                        )
                    incomes.map {
                        IncomeWithPermittedActions(it, permittedActions[it.id] ?: emptySet())
                    }
                }
            }
            .also { incomes ->
                Audit.PersonIncomeRead.log(
                    targetId = AuditId(personId),
                    meta = mapOf("count" to incomes.size),
                )
            }
    }

    data class IncomeWithPermittedActions(
        val data: Income,
        val permittedActions: Set<Action.Income>,
    )

    @PostMapping
    fun createIncome(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody income: IncomeRequest,
    ): IncomeId {
        val period =
            try {
                DateRange(income.validFrom, income.validTo)
            } catch (e: Exception) {
                with(income) { throw BadRequest("Invalid period from $validFrom to $validTo") }
            }

        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.CREATE_INCOME,
                        income.personId,
                    )

                    val now = clock.now()
                    val incomeTypes = incomeTypesProvider.get()
                    val validIncome = validateIncome(income, incomeTypes)
                    tx.endEarlierOverlappingIncome(
                        now,
                        validIncome.personId,
                        period,
                        user.evakaUserId,
                    )
                    val id = tx.insertIncome(now, validIncome, user.evakaUserId)
                    tx.associateOrphanAttachments(
                        user.evakaUserId,
                        AttachmentParent.Income(id),
                        income.attachments.map { it.id },
                    )
                    asyncJobRunner.plan(
                        tx,
                        listOf(
                            AsyncJob.GenerateFinanceDecisions.forAdult(validIncome.personId, period)
                        ),
                        runAt = clock.now(),
                    )
                    asyncJobRunner.plan(
                        tx,
                        listOf(
                            AsyncJob.GenerateFinanceDecisions.forChild(validIncome.personId, period)
                        ),
                        runAt = clock.now(),
                    )
                    id
                }
            }
            .also { incomeId ->
                Audit.PersonIncomeCreate.log(
                    targetId = AuditId(income.personId),
                    objectId = AuditId(incomeId),
                )
            }
    }

    @PutMapping("/{incomeId}")
    fun updateIncome(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable incomeId: IncomeId,
        @RequestBody income: IncomeRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.Income.UPDATE, incomeId)

                val existing =
                    tx.getIncome(incomeTypesProvider, coefficientMultiplierProvider, incomeId)
                val incomeTypes = incomeTypesProvider.get()
                val validIncome = validateIncome(income, incomeTypes)
                tx.updateIncome(clock, incomeId, validIncome, user.evakaUserId)

                val expandedPeriod =
                    existing?.let {
                        DateRange(
                            minOf(it.validFrom, income.validFrom),
                            maxEndDate(it.validTo, income.validTo),
                        )
                    } ?: DateRange(income.validFrom, income.validTo)

                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            validIncome.personId,
                            expandedPeriod,
                        )
                    ),
                    runAt = clock.now(),
                )
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forChild(
                            validIncome.personId,
                            expandedPeriod,
                        )
                    ),
                    runAt = clock.now(),
                )
            }
        }
        Audit.PersonIncomeUpdate.log(targetId = AuditId(incomeId))
    }

    @DeleteMapping("/{incomeId}")
    fun deleteIncome(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable incomeId: IncomeId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.Income.DELETE, incomeId)

                val existing =
                    tx.getIncome(incomeTypesProvider, coefficientMultiplierProvider, incomeId)
                        ?: throw BadRequest("Income not found")
                val period = DateRange(existing.validFrom, existing.validTo)
                tx.deleteIncome(incomeId)

                asyncJobRunner.plan(
                    tx,
                    listOf(AsyncJob.GenerateFinanceDecisions.forAdult(existing.personId, period)),
                    runAt = clock.now(),
                )
                asyncJobRunner.plan(
                    tx,
                    listOf(AsyncJob.GenerateFinanceDecisions.forChild(existing.personId, period)),
                    runAt = clock.now(),
                )
            }
        }
        Audit.PersonIncomeDelete.log(targetId = AuditId(incomeId))
    }

    data class IncomeOption(
        val value: String,
        val nameFi: String,
        val multiplier: Int,
        val withCoefficient: Boolean,
        val isSubType: Boolean,
    )

    data class IncomeTypeOptions(
        val incomeTypes: List<IncomeOption>,
        val expenseTypes: List<IncomeOption>,
    )

    @GetMapping("/types")
    fun getIncomeTypeOptions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): IncomeTypeOptions {
        db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(it, user, clock, Action.Global.READ_INCOME_TYPES)
            }
        }
        return incomeTypesProvider
            .get()
            .map { (value, type) ->
                IncomeOption(
                    value = value,
                    nameFi = type.nameFi,
                    multiplier = type.multiplier,
                    withCoefficient = type.withCoefficient,
                    isSubType = type.isSubType,
                )
            }
            .partition { it.multiplier > 0 }
            .let { IncomeTypeOptions(incomeTypes = it.first, expenseTypes = it.second) }
    }

    @GetMapping("/multipliers")
    fun getIncomeMultipliers(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): Map<IncomeCoefficient, BigDecimal> {
        db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Global.READ_INCOME_COEFFICIENT_MULTIPLIERS,
                )
            }
        }
        return IncomeCoefficient.entries.associateWith {
            coefficientMultiplierProvider.multiplier(it)
        }
    }

    @GetMapping("/notifications")
    fun getIncomeNotifications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam personId: PersonId,
    ): List<IncomeNotification> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_INCOME_NOTIFICATIONS,
                        personId,
                    )
                    tx.getIncomeNotifications(personId)
                }
            }
            .also { incomeNotifications ->
                Audit.PersonIncomeNotificationRead.log(
                    targetId = AuditId(personId),
                    meta = mapOf("count" to incomeNotifications.size),
                )
            }
    }
}

fun validateIncome(income: IncomeRequest, incomeTypes: Map<String, IncomeType>): IncomeRequest {
    return if (income.effect == IncomeEffect.INCOME) {
        income.copy(
            data =
                income.data.mapValues { (type, value) ->
                    val incomeType =
                        incomeTypes[type] ?: throw BadRequest("Invalid income type: $type")
                    if (incomeType.withCoefficient) {
                        value.copy(multiplier = incomeType.multiplier)
                    } else {
                        value.copy(
                            multiplier = incomeType.multiplier,
                            coefficient = IncomeCoefficient.default(),
                        )
                    }
                }
        )
    } else {
        income.copy(data = mapOf())
    }
}
