// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.data.deleteIncome
import fi.espoo.evaka.invoicing.data.getIncome
import fi.espoo.evaka.invoicing.data.getIncomesForPerson
import fi.espoo.evaka.invoicing.data.splitEarlierIncome
import fi.espoo.evaka.invoicing.data.upsertIncome
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.maxEndDate
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/incomes")
class IncomeController(
    private val incomeTypesProvider: IncomeTypesProvider,
    private val mapper: ObjectMapper,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val accessControl: AccessControl
) {
    @GetMapping
    fun getIncome(db: Database, user: AuthenticatedUser, @RequestParam personId: PersonId): Wrapper<List<Income>> {
        Audit.PersonIncomeRead.log(targetId = personId)
        accessControl.requirePermissionFor(user, Action.Person.READ_INCOME, personId)

        val incomes = db.connect { dbc -> dbc.read { it.getIncomesForPerson(mapper, incomeTypesProvider, personId.raw) } }
        return Wrapper(incomes)
    }

    @PostMapping
    fun createIncome(db: Database, user: AuthenticatedUser, @RequestBody income: Income): ResponseEntity<IncomeId> {
        Audit.PersonIncomeCreate.log(targetId = income.personId)
        accessControl.requirePermissionFor(user, Action.Person.CREATE_INCOME, PersonId(income.personId))
        val period = try {
            DateRange(income.validFrom, income.validTo)
        } catch (e: Exception) {
            with(income) {
                throw BadRequest("Invalid period from $validFrom to $validTo")
            }
        }

        val id = db.connect { dbc ->
            dbc.transaction { tx ->
                val id = IncomeId(UUID.randomUUID())
                val incomeTypes = incomeTypesProvider.get()
                val validIncome = validateIncome(income.copy(id = id), incomeTypes)
                tx.splitEarlierIncome(validIncome.personId, period)
                tx.upsertIncome(mapper, validIncome, user.id)
                asyncJobRunner.plan(tx, listOf(AsyncJob.GenerateFinanceDecisions.forAdult(validIncome.personId, period)))
                id
            }
        }

        return ResponseEntity.ok(id)
    }

    @PutMapping("/{incomeId}")
    fun updateIncome(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable incomeId: IncomeId,
        @RequestBody income: Income
    ): ResponseEntity<Unit> {
        Audit.PersonIncomeUpdate.log(targetId = incomeId)
        accessControl.requirePermissionFor(user, Action.Income.UPDATE, incomeId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                val existing = tx.getIncome(mapper, incomeTypesProvider, incomeId)
                val incomeTypes = incomeTypesProvider.get()
                val validIncome = validateIncome(income.copy(id = incomeId, applicationId = null), incomeTypes)
                tx.upsertIncome(mapper, validIncome, user.id)

                val expandedPeriod = existing?.let {
                    DateRange(minOf(it.validFrom, income.validFrom), maxEndDate(it.validTo, income.validTo))
                } ?: DateRange(income.validFrom, income.validTo)

                asyncJobRunner.plan(tx, listOf(AsyncJob.GenerateFinanceDecisions.forAdult(validIncome.personId, expandedPeriod)))
            }
        }

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{incomeId}")
    fun deleteIncome(db: Database, user: AuthenticatedUser, @PathVariable incomeId: IncomeId): ResponseEntity<Unit> {
        Audit.PersonIncomeDelete.log(targetId = incomeId)
        accessControl.requirePermissionFor(user, Action.Income.DELETE, incomeId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                val existing = tx.getIncome(mapper, incomeTypesProvider, incomeId)
                    ?: throw BadRequest("Income not found")
                val period = DateRange(existing.validFrom, existing.validTo)

                tx.deleteIncome(incomeId)

                asyncJobRunner.plan(tx, listOf(AsyncJob.GenerateFinanceDecisions.forAdult(existing.personId, period)))
            }
        }

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/types")
    fun getTypes(user: AuthenticatedUser): ResponseEntity<Map<String, IncomeType>> {
        accessControl.requirePermissionFor(user, Action.Global.READ_INCOME_TYPES)
        return incomeTypesProvider.get().let { ResponseEntity.ok(it) }
    }
}

fun validateIncome(income: Income, incomeTypes: Map<String, IncomeType>): Income {
    return if (income.effect == IncomeEffect.INCOME) {
        income.copy(
            data = income.data.mapValues { (type, value) ->
                val incomeType = incomeTypes[type] ?: throw BadRequest("Invalid income type: $type")
                if (incomeType.withCoefficient) value.copy(multiplier = incomeType.multiplier) else value.copy(
                    multiplier = incomeType.multiplier,
                    coefficient = IncomeCoefficient.default()
                )
            }
        )
    } else {
        income.copy(data = mapOf())
    }
}
