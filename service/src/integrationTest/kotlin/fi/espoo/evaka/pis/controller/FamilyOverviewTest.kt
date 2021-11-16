// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.service.FamilyOverview
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestIncome
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.dev.updateDaycareAcl
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class FamilyOverviewTest : FullApplicationTest() {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }
    }

    @AfterEach
    fun afterEach() {
        db.transaction { tx ->
            tx.resetDatabase()
        }
    }

    @Test
    fun `handles 1 + 1`() {
        createTestFixture1plus1()
        val result = fetchAndParseFamilyDetails(testAdult_1.id)

        assertEquals(
            1, result.children.size
        )

        assertEquals(
            testAdult_1.id, result.headOfFamily.personId
        )

        assertEquals(
            setOf(testChild_1.id), result.children.map { it.personId }.toSet()
        )
    }

    @Test
    fun `handles 1 + 2`() {
        createTestFixture1plus2()
        val result = fetchAndParseFamilyDetails(testAdult_1.id)

        assertEquals(
            2, result.children.size
        )

        assertEquals(
            testAdult_1.id, result.headOfFamily.personId
        )

        assertEquals(
            setOf(testChild_1.id, testChild_2.id), result.children.map { it.personId }.toSet()
        )
    }

    @Test
    fun `handles 2 + 2`() {
        createTestFixture2plus2()
        val result = fetchAndParseFamilyDetails(testAdult_1.id)

        assertEquals(
            2, result.children.size
        )

        assertEquals(
            testAdult_1.id, result.headOfFamily.personId
        )

        assertEquals(
            testAdult_2.id, result.partner!!.personId
        )

        assertEquals(
            setOf(testChild_1.id, testChild_2.id), result.children.map { it.personId }.toSet()
        )
    }

    @Test
    fun `doesn't explode if children are missing`() {
        val result = fetchAndParseFamilyDetails(testAdult_1.id)

        assertEquals(
            0, result.children.size
        )

        assertEquals(
            testAdult_1.id, result.headOfFamily.personId
        )
    }

    @Test
    fun `show income total if user is finance admin`() {
        val incomeTotal = addIncome(testAdult_1.id)
        createTestFixture1plus1()
        val result = fetchAndParseFamilyDetails(testAdult_1.id, financeUser)

        assertEquals(testAdult_1.id, result.headOfFamily.personId)
        assertEquals(IncomeEffect.INCOME, result.headOfFamily.income?.effect)
        assertEquals(incomeTotal, result.headOfFamily.income?.total)
    }

    @Test
    fun `hide income total if user is unit supervisor`() {
        val unitSupervisor = initializeUnitSupervisorUserAndRights(testChild_1.id)
        addIncome(testAdult_1.id)
        createTestFixture1plus1()
        val result = fetchAndParseFamilyDetails(testAdult_1.id, unitSupervisor)

        assertEquals(testAdult_1.id, result.headOfFamily.personId)
        assertEquals(null, result.headOfFamily.income)
    }

    private val financeUser = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN))

    private fun fetchAndParseFamilyDetails(personId: UUID, user: AuthenticatedUser = financeUser): FamilyOverview {
        val (_, response, result) = http.get("/family/by-adult/$personId")
            .asUser(user)
            .responseString()

        assertEquals(200, response.statusCode)
        return objectMapper.readValue<FamilyOverview>(result.get())
    }

    private fun createTestFixture1plus1() {
        val (from, to) = LocalDate.now().let {
            listOf(it.minusYears(1), it.plusYears(1))
        }
        db.transaction { it.createParentship(testChild_1.id, testAdult_1.id, from, to) }
    }

    private fun createTestFixture1plus2() {
        val (from, to) = LocalDate.now().let {
            listOf(it.minusYears(1), it.plusYears(1))
        }
        db.transaction {
            it.createParentship(testChild_1.id, testAdult_1.id, from, to)
            it.createParentship(testChild_2.id, testAdult_1.id, from, to)
        }
    }

    private fun createTestFixture2plus2() {
        val (from, to) = LocalDate.now().let {
            listOf(it.minusYears(1), it.plusYears(1))
        }
        db.transaction {
            it.createParentship(testChild_1.id, testAdult_1.id, from, to)
            it.createParentship(testChild_2.id, testAdult_1.id, from, to)
            it.createPartnership(testAdult_1.id, testAdult_2.id, from, to)
        }
    }

    private fun initializeUnitSupervisorUserAndRights(childId: UUID): AuthenticatedUser {
        val externalId = ExternalId.of("test", "id")
        val unitSupervisor = DevEmployee(externalId = externalId)
        db.transaction {
            it.insertTestPlacement(
                childId = childId,
                unitId = testDaycare.id,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusYears(1)
            )
            it.insertTestEmployee(unitSupervisor)
            it.updateDaycareAcl(testDaycare.id, externalId, UserRole.UNIT_SUPERVISOR)
        }
        return AuthenticatedUser.Employee(unitSupervisor.id, setOf())
    }

    private fun addIncome(personId: UUID): Int {
        val incomeTotal = 500000
        db.transaction {
            it.insertTestIncome(
                objectMapper,
                personId,
                effect = IncomeEffect.INCOME,
                data = mapOf("MAIN_INCOME" to IncomeValue(incomeTotal, IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS, 1)),
                updatedBy = testDecisionMaker_1.id
            )
        }
        return incomeTotal
    }
}
