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
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.GenerateFinanceDecisions
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.maxEndDate
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
    private val mapper: ObjectMapper,
    private val asyncJobRunner: AsyncJobRunner
) {
    @GetMapping
    fun getIncome(db: Database.Connection, user: AuthenticatedUser, @RequestParam personId: String?): ResponseEntity<Wrapper<List<Income>>> {
        Audit.PersonIncomeRead.log(targetId = personId)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        val parsedId = personId?.let { parseUUID(personId) }
            ?: throw BadRequest("Query parameter personId is mandatory")

        val incomes = db.read { it.getIncomesForPerson(mapper, parsedId) }
        return ResponseEntity.ok(Wrapper(incomes))
    }

    @PostMapping
    fun createIncome(db: Database.Connection, user: AuthenticatedUser, @RequestBody income: Income): ResponseEntity<UUID> {
        Audit.PersonIncomeCreate.log(targetId = income.personId)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        val period = try {
            DateRange(income.validFrom, income.validTo)
        } catch (e: Exception) {
            with(income) {
                throw BadRequest("Invalid period from $validFrom to $validTo")
            }
        }

        val id = db.transaction { tx ->
            val id = UUID.randomUUID()
            val validIncome = income.copy(id = id).let(::validateIncome)
            tx.splitEarlierIncome(validIncome.personId, period)
            tx.upsertIncome(mapper, validIncome, user.id)
            asyncJobRunner.plan(tx, listOf(GenerateFinanceDecisions.forAdult(validIncome.personId, period)))
            id
        }

        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.ok(id)
    }

    @PutMapping("/{incomeId}")
    fun updateIncome(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable incomeId: String,
        @RequestBody income: Income
    ): ResponseEntity<Unit> {
        Audit.PersonIncomeUpdate.log(targetId = incomeId)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)

        db.transaction { tx ->
            val existing = tx.getIncome(mapper, parseUUID(incomeId))
            val validIncome = income.copy(id = parseUUID(incomeId)).let(::validateIncome)
            tx.upsertIncome(mapper, validIncome, user.id)

            val expandedPeriod = existing?.let {
                DateRange(minOf(it.validFrom, income.validFrom), maxEndDate(it.validTo, income.validTo))
            } ?: DateRange(income.validFrom, income.validTo)

            asyncJobRunner.plan(tx, listOf(GenerateFinanceDecisions.forAdult(validIncome.personId, expandedPeriod)))
        }

        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{incomeId}")
    fun deleteIncome(db: Database.Connection, user: AuthenticatedUser, @PathVariable incomeId: String): ResponseEntity<Unit> {
        Audit.PersonIncomeDelete.log(targetId = incomeId)
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)

        db.transaction { tx ->
            val existing = tx.getIncome(mapper, parseUUID(incomeId))
                ?: throw BadRequest("Income not found")
            val period = DateRange(existing.validFrom, existing.validTo)

            tx.deleteIncome(parseUUID(incomeId))

            asyncJobRunner.plan(tx, listOf(GenerateFinanceDecisions.forAdult(existing.personId, period)))
        }

        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }
}

fun validateIncome(income: Income): Income {
    return if (income.effect == IncomeEffect.INCOME) {
        income.copy(
            data = income.data.mapValues { (type, value) ->
                when (type) {
                    IncomeType.MAIN_INCOME, IncomeType.SECONDARY_INCOME, IncomeType.OTHER_INCOME -> value
                    else -> value.copy(coefficient = IncomeCoefficient.default())
                }
            }
        )
    } else {
        income.copy(data = mapOf())
    }
}
