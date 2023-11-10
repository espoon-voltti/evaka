// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.IncomeController
import fi.espoo.evaka.invoicing.data.getIncomesForPerson
import fi.espoo.evaka.invoicing.data.upsertIncome
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.service.IncomeCoefficientMultiplierProvider
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.controllers.Wrapper
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class IncomeIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var mapper: JsonMapper

    @Autowired lateinit var incomeTypesProvider: IncomeTypesProvider
    @Autowired lateinit var coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider

    private fun assertEqualEnough(expected: List<Income>, actual: List<Income>) {
        val nullId = IncomeId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        assertEquals(
            expected.map { it.copy(id = nullId, updatedAt = null) }.toSet(),
            actual.map { it.copy(id = nullId, updatedAt = null) }.toSet()
        )
    }

    private fun deserializeResult(json: String) =
        jsonMapper
            .readValue<Wrapper<List<IncomeController.IncomeWithPermittedActions>>>(json)
            .data
            .map { it.data }

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    private fun testIncome(): Income {
        val data = mapOf(
            "MAIN_INCOME" to
                    IncomeValue(
                        500000,
                        IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS,
                        1,
                        calculateMonthlyAmount(500000, coefficientMultiplierProvider.multiplier(IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS))
                    )
        )
        return Income(
            id = IncomeId(UUID.randomUUID()),
            personId = testAdult_1.id,
            effect = IncomeEffect.INCOME,
            data = data,
            validFrom = LocalDate.of(2019, 1, 1),
            validTo = LocalDate.of(2019, 1, 31),
            notes = "",
            totalIncome = calculateTotalIncome(data, coefficientMultiplierProvider),
            totalExpenses = calculateTotalExpense(data, coefficientMultiplierProvider),
            total = calculateIncomeTotal(data, coefficientMultiplierProvider)

        )
    }

    private val financeUser =
        AuthenticatedUser.Employee(
            id = testDecisionMaker_1.id,
            roles = setOf(UserRole.FINANCE_ADMIN)
        )
    private val financeUserName = "${testDecisionMaker_1.lastName} ${testDecisionMaker_1.firstName}"
    private val clock = RealEvakaClock()

    @Test
    fun `getIncome works with no data in DB`() {
        val (_, response, result) =
            http.get("/incomes?personId=${testAdult_1.id}").asUser(financeUser).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(listOf(), deserializeResult(result.get()))
    }

    @Test
    fun `getIncome works with single income in DB`() {
        val testIncome = testIncome()
        db.transaction { tx -> tx.upsertIncome(clock, mapper, testIncome, financeUser.evakaUserId) }

        val (_, response, result) =
            http.get("/incomes?personId=${testAdult_1.id}").asUser(financeUser).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(
            listOf(testIncome.copy(updatedBy = financeUserName)),
            deserializeResult(result.get())
        )
    }

    @Test
    fun `getIncome works with multiple incomes in DB`() {
        val testIncome = testIncome()
        val incomes =
            listOf(
                testIncome.copy(
                    id = IncomeId(UUID.randomUUID()),
                    validFrom = testIncome.validFrom.plusYears(1),
                    validTo = testIncome.validTo!!.plusYears(1)
                ),
                testIncome
            )
        db.transaction { tx ->
            incomes.forEach { tx.upsertIncome(clock, mapper, it, financeUser.evakaUserId) }
        }

        val (_, response, result) =
            http.get("/incomes?personId=${testAdult_1.id}").asUser(financeUser).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(
            incomes.map { it.copy(updatedBy = financeUserName) },
            deserializeResult(result.get())
        )
    }

    @Test
    fun `createIncome works with valid income`() {
        val testIncome = testIncome()
        val (_, postResponse, _) =
            http
                .post("/incomes?personId=${testAdult_1.id}")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(testIncome))
                .responseString()
        assertEquals(200, postResponse.statusCode)

        val (_, response, result) =
            http.get("/incomes?personId=${testAdult_1.id}").asUser(financeUser).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(
            listOf(testIncome.copy(updatedBy = financeUserName)),
            deserializeResult(result.get())
        )
    }

    @Test
    fun `createIncome throws with invalid date range`() {
        val income = testIncome.copy(validTo = testIncome.validFrom.minusDays(1))
        val (_, postResponse, _) =
            http
                .post("/incomes?personId=${testAdult_1.id}")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(income))
                .responseString()
        assertEquals(400, postResponse.statusCode)
    }

    @Test
    fun `createIncome splits earlier indefinite income`() {
        val firstIncome = testIncome().copy(validTo = null)
        db.transaction { tx ->
            tx.upsertIncome(clock, mapper, firstIncome, financeUser.evakaUserId)
        }

        val secondIncome = firstIncome.copy(validFrom = firstIncome.validFrom.plusMonths(1))
        val (_, postResponse, _) =
            http
                .post("/incomes?personId=${testAdult_1.id}")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(secondIncome))
                .responseString()
        assertEquals(200, postResponse.statusCode)

        val result =
            db.transaction { tx ->
                tx.getIncomesForPerson(mapper, incomeTypesProvider, coefficientMultiplierProvider, testAdult_1.id)
            }

        assertEquals(2, result.size)

        val firstIncomeResult = result.find { it.id == firstIncome.id }!!
        assertEquals(firstIncome.validFrom, firstIncomeResult.validFrom)
        assertEquals(secondIncome.validFrom.minusDays(1), firstIncomeResult.validTo)

        val secondIncomeResult = result.find { it.id != firstIncome.id }!!
        assertEquals(secondIncome.validFrom, secondIncomeResult.validFrom)
        assertEquals(secondIncome.validTo, secondIncomeResult.validTo)
    }

    @Test
    fun `createIncome throws with partly overlapping date range`() {
        val testIncome = testIncome()
        db.transaction { tx -> tx.upsertIncome(clock, mapper, testIncome, financeUser.evakaUserId) }

        val overlappingIncome =
            testIncome.let { it.copy(validFrom = it.validFrom.plusDays(10), validTo = null) }

        val (_, postResponse, _) =
            http
                .post("/incomes?personId=${testAdult_1.id}")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(overlappingIncome))
                .responseString()
        assertEquals(409, postResponse.statusCode)
    }

    @Test
    fun `createIncome throws with identical date range`() {
        val testIncome = testIncome()
        db.transaction { tx -> tx.upsertIncome(clock, mapper, testIncome, financeUser.evakaUserId) }

        val (_, postResponse, _) =
            http
                .post("/incomes?personId=${testAdult_1.id}")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(testIncome))
                .responseString()
        assertEquals(409, postResponse.statusCode)
    }

    @Test
    fun `createIncome throws with covering date range`() {
        val testIncome = testIncome()
        db.transaction { tx -> tx.upsertIncome(clock, mapper, testIncome, financeUser.evakaUserId) }

        val overlappingIncome =
            testIncome.let {
                it.copy(
                    validFrom = it.validTo!!.minusMonths(1),
                    validTo = it.validTo!!.plusYears(1)
                )
            }
        val (_, postResponse, _) =
            http
                .post("/incomes?personId=${testAdult_1.id}")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(overlappingIncome))
                .responseString()
        assertEquals(409, postResponse.statusCode)
    }

    @Test
    fun `createIncome removes data if effect is not INCOME`() {
        val income = with(testIncome()) { this.copy(effect = IncomeEffect.MAX_FEE_ACCEPTED) }

        val (_, postResponse, _) =
            http
                .post("/incomes?personId=${testAdult_1.id}")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(income))
                .responseString()
        assertEquals(200, postResponse.statusCode)

        val (_, response, result) =
            http.get("/incomes?personId=${testAdult_1.id}").asUser(financeUser).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(
            listOf(income.copy(data = emptyMap(), updatedBy = financeUserName, totalIncome = 0, totalExpenses = 0, total = 0)),
            deserializeResult(result.get())
        )
    }

    @Test
    fun `updateIncome works with valid income`() {
        val testIncome = testIncome()
        db.transaction { tx -> tx.upsertIncome(clock, mapper, testIncome, financeUser.evakaUserId) }

        val newIncomeData = mapOf(
            "MAIN_INCOME" to
                    IncomeValue(
                        1000,
                        IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS,
                        1,
                        calculateMonthlyAmount(1000, coefficientMultiplierProvider.multiplier(IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS))
                    )
        )
        val updated =
            testIncome.copy(
                data = newIncomeData,
                totalIncome = calculateTotalIncome(newIncomeData, coefficientMultiplierProvider),
                totalExpenses = calculateTotalExpense(newIncomeData, coefficientMultiplierProvider),
                total = calculateIncomeTotal(newIncomeData, coefficientMultiplierProvider)
            )
        val (_, putResponse, _) =
            http
                .put("/incomes/${updated.id}?personId=${testAdult_1.id}")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(updated))
                .responseString()
        assertEquals(200, putResponse.statusCode)

        val (_, response, result) =
            http.get("/incomes?personId=${testAdult_1.id}").asUser(financeUser).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(
            listOf(updated.copy(updatedBy = financeUserName)),
            deserializeResult(result.get())
        )
    }

    @Test
    fun `updateIncome throws with invalid date rage`() {
        val testIncome = testIncome()
        db.transaction { tx -> tx.upsertIncome(clock, mapper, testIncome, financeUser.evakaUserId) }

        val updated = testIncome.copy(validTo = testIncome.validFrom.minusDays(1))
        val (_, putResponse, _) =
            http
                .put("/incomes/${updated.id}?personId=${testAdult_1.id}")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(updated))
                .responseString()
        assertEquals(400, putResponse.statusCode)
    }

    @Test
    fun `updateIncome throws with overlapping date rage`() {
        val testIncome = testIncome()
        db.transaction { tx -> tx.upsertIncome(clock, mapper, testIncome, financeUser.evakaUserId) }

        val anotherIncome =
            with(testIncome) {
                this.copy(
                    id = IncomeId(UUID.randomUUID()),
                    validFrom = validFrom.plusYears(1),
                    validTo = validTo!!.plusYears(1)
                )
            }
        db.transaction { tx ->
            tx.upsertIncome(clock, mapper, anotherIncome, financeUser.evakaUserId)
        }

        val updated = testIncome.copy(validTo = anotherIncome.validTo)
        val (_, putResponse, _) =
            http
                .put("/incomes/${updated.id}?personId=${testAdult_1.id}")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(updated))
                .responseString()
        assertEquals(409, putResponse.statusCode)
    }

    @Test
    fun `updateIncome removes data if effect is not INCOME`() {
        val testIncome = testIncome()
        db.transaction { tx -> tx.upsertIncome(clock, mapper, testIncome, financeUser.evakaUserId) }

        val updated = with(testIncome) { this.copy(effect = IncomeEffect.MAX_FEE_ACCEPTED) }
        val (_, putResponse, _) =
            http
                .put("/incomes/${updated.id}?personId=${testAdult_1.id}")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(updated))
                .responseString()
        assertEquals(200, putResponse.statusCode)

        val (_, response, result) =
            http.get("/incomes?personId=${testAdult_1.id}").asUser(financeUser).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(
            listOf(updated.copy(data = emptyMap(), updatedBy = financeUserName, totalIncome = 0, totalExpenses = 0, total = 0)),
            deserializeResult(result.get())
        )
    }

    @Test
    fun `updateIncome nullify application_id`() {
        val testIncome = testIncome()
        db.transaction { tx ->
            val application = tx.insertApplication()
            tx.upsertIncome(
                clock,
                mapper,
                testIncome.copy(applicationId = application.id),
                financeUser.evakaUserId
            )
        }

        val (_, responseBeforeUpdate, resultBeforeUpdate) =
            http.get("/incomes?personId=${testAdult_1.id}").asUser(financeUser).responseString()
        assertEquals(200, responseBeforeUpdate.statusCode)

        val beforeUpdate = deserializeResult(resultBeforeUpdate.get()).first()
        assertNotNull(beforeUpdate.applicationId)

        val updated = with(testIncome) { this.copy(effect = IncomeEffect.MAX_FEE_ACCEPTED) }
        val (_, putResponse, _) =
            http
                .put("/incomes/${updated.id}?personId=${testAdult_1.id}")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(updated))
                .responseString()
        assertEquals(200, putResponse.statusCode)

        val (_, responseAfterUpdate, resultAfterUpdate) =
            http.get("/incomes?personId=${testAdult_1.id}").asUser(financeUser).responseString()
        assertEquals(200, responseAfterUpdate.statusCode)

        val afterUpdate = deserializeResult(resultAfterUpdate.get()).first()
        assertNull(afterUpdate.applicationId)
    }

    @Test
    fun `deleteIncome works with multiple incomes in DB`() {
        val testIncome = testIncome()
        val anotherIncome =
            with(testIncome) {
                this.copy(
                    id = IncomeId(UUID.randomUUID()),
                    validFrom = validFrom.plusYears(1),
                    validTo = validTo!!.plusYears(1)
                )
            }
        db.transaction { tx ->
            tx.upsertIncome(clock, mapper, testIncome, financeUser.evakaUserId)
            tx.upsertIncome(clock, mapper, anotherIncome, financeUser.evakaUserId)
        }

        val resultBeforeDelete =
            db.transaction { tx ->
                tx.getIncomesForPerson(mapper, incomeTypesProvider, coefficientMultiplierProvider, testIncome.personId)
            }

        assertEquals(2, resultBeforeDelete.size)

        val (_, deleteResponse, _) =
            http.delete("/incomes/${testIncome.id}").asUser(financeUser).responseString()

        assertEquals(200, deleteResponse.statusCode)

        val resultAfterDelete =
            db.transaction { tx ->
                tx.getIncomesForPerson(mapper, incomeTypesProvider, coefficientMultiplierProvider, testIncome.personId)
            }

        assertEquals(1, resultAfterDelete.size)
    }
}
