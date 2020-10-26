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
import fi.espoo.evaka.shared.async.NotifyIncomeUpdated
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Period
import fi.espoo.evaka.shared.domain.maxEndDate
import org.jdbi.v3.core.Jdbi
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
    private val jdbi: Jdbi,
    private val mapper: ObjectMapper,
    private val asyncJobRunner: AsyncJobRunner
) {
    @GetMapping
    fun getIncome(user: AuthenticatedUser, @RequestParam personId: String?): ResponseEntity<Wrapper<List<Income>>> {
        Audit.PersonIncomeRead.log(targetId = personId)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val parsedId = personId?.let { parseUUID(personId) }
            ?: throw BadRequest("Query parameter personId is mandatory")

        val incomes = jdbi.handle { h -> getIncomesForPerson(h, mapper, parsedId) }
        return ResponseEntity.ok(Wrapper(incomes))
    }

    @PostMapping
    fun createIncome(user: AuthenticatedUser, @RequestBody income: Income): ResponseEntity<UUID> {
        Audit.PersonIncomeCreate.log(targetId = income.personId)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val period = try {
            Period(income.validFrom, income.validTo)
        } catch (e: Exception) {
            with(income) {
                throw BadRequest("Invalid period from $validFrom to $validTo")
            }
        }

        val id = jdbi.transaction { h ->
            val id = UUID.randomUUID()
            val validIncome = income.copy(id = id).let(::validateIncome)
            splitEarlierIncome(h, validIncome.personId, period)
            upsertIncome(h, mapper, validIncome, user.id)
            asyncJobRunner.plan(h, listOf(NotifyIncomeUpdated(validIncome.personId, period.start, period.end)))
            id
        }

        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.ok(id)
    }

    @PutMapping("/{incomeId}")
    fun updateIncome(
        user: AuthenticatedUser,
        @PathVariable incomeId: String,
        @RequestBody income: Income
    ): ResponseEntity<Unit> {
        Audit.PersonIncomeUpdate.log(targetId = incomeId)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)

        jdbi.transaction { h ->
            val existing = getIncome(h, mapper, parseUUID(incomeId))
            val validIncome = income.copy(id = parseUUID(incomeId)).let(::validateIncome)
            upsertIncome(h, mapper, validIncome, user.id)

            val expandedPeriod = existing?.let {
                Period(minOf(it.validFrom, income.validFrom), maxEndDate(it.validTo, income.validTo))
            } ?: Period(income.validFrom, income.validTo)

            asyncJobRunner.plan(
                h,
                listOf(NotifyIncomeUpdated(validIncome.personId, expandedPeriod.start, expandedPeriod.end))
            )
        }

        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{incomeId}")
    fun deleteIncome(user: AuthenticatedUser, @PathVariable incomeId: String): ResponseEntity<Unit> {
        Audit.PersonIncomeDelete.log(targetId = incomeId)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)

        jdbi.transaction { h ->
            val existing = getIncome(h, mapper, parseUUID(incomeId))
                ?: throw BadRequest("Income not found")
            val period = Period(existing.validFrom, existing.validTo)

            deleteIncome(h, parseUUID(incomeId))

            asyncJobRunner.plan(h, listOf(NotifyIncomeUpdated(existing.personId, period.start, period.end)))
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
