// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.attachment.AttachmentParent
import fi.espoo.evaka.attachment.associateOrphanAttachments
import fi.espoo.evaka.invoicing.data.deleteIncome
import fi.espoo.evaka.invoicing.data.getIncome
import fi.espoo.evaka.invoicing.data.getIncomesForPerson
import fi.espoo.evaka.invoicing.data.insertIncome
import fi.espoo.evaka.invoicing.data.splitEarlierIncome
import fi.espoo.evaka.invoicing.data.updateIncome
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeRequest
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.service.IncomeCoefficientMultiplierProvider
import fi.espoo.evaka.invoicing.service.IncomeNotification
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.invoicing.service.getIncomeNotifications
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.maxEndDate
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
@RequestMapping(
    "/incomes", // deprecated
    "/employee/incomes",
)
class IncomeController(
    private val incomeTypesProvider: IncomeTypesProvider,
    private val coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    private val mapper: JsonMapper,
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
                            mapper,
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

                    val incomeTypes = incomeTypesProvider.get()
                    val validIncome = validateIncome(income, incomeTypes)
                    tx.splitEarlierIncome(validIncome.personId, period)
                    val id = tx.insertIncome(clock, mapper, validIncome, user.evakaUserId)
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
                    tx.getIncome(
                        mapper,
                        incomeTypesProvider,
                        coefficientMultiplierProvider,
                        incomeId,
                    )
                val incomeTypes = incomeTypesProvider.get()
                val validIncome = validateIncome(income, incomeTypes)
                tx.updateIncome(clock, mapper, incomeId, validIncome, user.evakaUserId)

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
                    tx.getIncome(
                        mapper,
                        incomeTypesProvider,
                        coefficientMultiplierProvider,
                        incomeId,
                    ) ?: throw BadRequest("Income not found")
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
