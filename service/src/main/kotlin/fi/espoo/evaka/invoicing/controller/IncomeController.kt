// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.Audit
import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.attachment.associateIncomeAttachments
import fi.espoo.evaka.attachment.deleteAttachment
import fi.espoo.evaka.invoicing.data.deleteIncome
import fi.espoo.evaka.invoicing.data.getIncome
import fi.espoo.evaka.invoicing.data.getIncomesForPerson
import fi.espoo.evaka.invoicing.data.splitEarlierIncome
import fi.espoo.evaka.invoicing.data.upsertIncome
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.service.IncomeNotification
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.invoicing.service.getIncomeNotifications
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.controllers.Wrapper
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.util.UUID
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
@RequestMapping("/incomes")
class IncomeController(
    private val documentClient: DocumentService,
    private val incomeTypesProvider: IncomeTypesProvider,
    private val mapper: JsonMapper,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val accessControl: AccessControl,
    bucketEnv: BucketEnv
) {
    private val filesBucket = bucketEnv.attachments

    @GetMapping
    fun getIncome(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam personId: PersonId
    ): Wrapper<List<IncomeWithPermittedActions>> {
        val incomes =
            db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_INCOME,
                        personId
                    )

                    val incomes = tx.getIncomesForPerson(mapper, incomeTypesProvider, personId)
                    val permittedActions =
                        accessControl.getPermittedActions<IncomeId, Action.Income>(
                            tx,
                            user,
                            clock,
                            incomes.mapNotNull { it.id }
                        )
                    incomes.map {
                        IncomeWithPermittedActions(it, permittedActions[it.id] ?: emptySet())
                    }
                }
            }
        Audit.PersonIncomeRead.log(targetId = personId, meta = mapOf("count" to incomes.size))
        return Wrapper(incomes)
    }

    @ExcludeCodeGen
    data class IncomeWithPermittedActions(
        val data: Income,
        val permittedActions: Set<Action.Income>
    )

    @PostMapping
    fun createIncome(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody income: Income
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
                        income.personId
                    )

                    val id = IncomeId(UUID.randomUUID())
                    val incomeTypes = incomeTypesProvider.get()
                    val validIncome = validateIncome(income.copy(id = id), incomeTypes)
                    tx.splitEarlierIncome(validIncome.personId, period)
                    tx.upsertIncome(clock, mapper, validIncome, user.evakaUserId)
                    tx.associateIncomeAttachments(
                        user.evakaUserId,
                        id,
                        income.attachments.map { it.id }
                    )
                    asyncJobRunner.plan(
                        tx,
                        listOf(AsyncJob.GenerateFinanceDecisions.forAdult(validIncome.personId)),
                        runAt = clock.now()
                    )
                    asyncJobRunner.plan(
                        tx,
                        listOf(AsyncJob.GenerateFinanceDecisions.forChild(validIncome.personId)),
                        runAt = clock.now()
                    )
                    id
                }
            }
            .also { incomeId ->
                Audit.PersonIncomeCreate.log(targetId = income.personId, objectId = incomeId)
            }
    }

    @PutMapping("/{incomeId}")
    fun updateIncome(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable incomeId: IncomeId,
        @RequestBody income: Income
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.Income.UPDATE, incomeId)

                val incomeTypes = incomeTypesProvider.get()
                val validIncome =
                    validateIncome(income.copy(id = incomeId, applicationId = null), incomeTypes)
                tx.upsertIncome(clock, mapper, validIncome, user.evakaUserId)

                asyncJobRunner.plan(
                    tx,
                    listOf(AsyncJob.GenerateFinanceDecisions.forAdult(validIncome.personId)),
                    runAt = clock.now()
                )
                asyncJobRunner.plan(
                    tx,
                    listOf(AsyncJob.GenerateFinanceDecisions.forChild(validIncome.personId)),
                    runAt = clock.now()
                )
            }
        }
        Audit.PersonIncomeUpdate.log(targetId = incomeId)
    }

    @DeleteMapping("/{incomeId}")
    fun deleteIncome(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable incomeId: IncomeId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.Income.DELETE, incomeId)

                val existing =
                    tx.getIncome(mapper, incomeTypesProvider, incomeId)
                        ?: throw BadRequest("Income not found")

                existing.attachments.map {
                    tx.deleteAttachment(it.id)
                    documentClient.delete(filesBucket, "${it.id}")
                }
                tx.deleteIncome(incomeId)

                asyncJobRunner.plan(
                    tx,
                    listOf(AsyncJob.GenerateFinanceDecisions.forAdult(existing.personId)),
                    runAt = clock.now()
                )
                asyncJobRunner.plan(
                    tx,
                    listOf(AsyncJob.GenerateFinanceDecisions.forChild(existing.personId)),
                    runAt = clock.now()
                )
            }
        }
        Audit.PersonIncomeDelete.log(targetId = incomeId)
    }

    @GetMapping("/types")
    fun getTypes(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): Map<String, IncomeType> {
        db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(it, user, clock, Action.Global.READ_INCOME_TYPES)
            }
        }
        return incomeTypesProvider.get()
    }

    @GetMapping("/notifications")
    fun getIncomeNotifications(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam personId: PersonId
    ): Wrapper<List<IncomeNotification>> {
        val incomeNotifications =
            db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_INCOME_NOTIFICATIONS,
                        personId
                    )
                    tx.getIncomeNotifications(personId)
                }
            }
        Audit.PersonIncomeNotificationRead.log(
            targetId = personId,
            meta = mapOf("count" to incomeNotifications.size)
        )
        return Wrapper(incomeNotifications)
    }
}

fun validateIncome(income: Income, incomeTypes: Map<String, IncomeType>): Income {
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
                            coefficient = IncomeCoefficient.default()
                        )
                    }
                }
        )
    } else {
        income.copy(data = mapOf())
    }
}
