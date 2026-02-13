// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.invoicing.calculateMonthlyAmount
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.service.IncomeCoefficientMultiplierProvider
import fi.espoo.evaka.pis.Creator
import fi.espoo.evaka.pis.controllers.FamilyController
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.updateDaycareAcl
import fi.espoo.evaka.shared.domain.RealEvakaClock
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FamilyOverviewTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var familyController: FamilyController
    @Autowired lateinit var coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider

    private val clock = RealEvakaClock()
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val employee = DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN))
    private val financeUser = AuthenticatedUser.Employee(employee.id, setOf(UserRole.FINANCE_ADMIN))
    private val adult1 = DevPerson()
    private val adult2 = DevPerson()
    private val child1 = DevPerson()
    private val child2 = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(area)
            tx.insert(daycare)
            listOf(adult1, adult2).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(child1, child2).forEach { tx.insert(it, DevPersonType.CHILD) }
        }
    }

    @Test
    fun `handles 1 + 1`() {
        createTestFixture1plus1()
        val result = familyController.getFamilyByPerson(dbInstance(), financeUser, clock, adult1.id)

        assertEquals(1, result.children.size)

        assertEquals(adult1.id, result.headOfFamily.personId)

        assertEquals(setOf(child1.id), result.children.map { it.personId }.toSet())
    }

    @Test
    fun `handles 1 + 2`() {
        createTestFixture1plus2()
        val result = familyController.getFamilyByPerson(dbInstance(), financeUser, clock, adult1.id)

        assertEquals(2, result.children.size)

        assertEquals(adult1.id, result.headOfFamily.personId)

        assertEquals(setOf(child1.id, child2.id), result.children.map { it.personId }.toSet())
    }

    @Test
    fun `handles 2 + 2`() {
        createTestFixture2plus2()
        val result = familyController.getFamilyByPerson(dbInstance(), financeUser, clock, adult1.id)

        assertEquals(2, result.children.size)

        assertEquals(adult1.id, result.headOfFamily.personId)

        assertEquals(adult2.id, result.partner!!.personId)

        assertEquals(setOf(child1.id, child2.id), result.children.map { it.personId }.toSet())
    }

    @Test
    fun `doesn't explode if children are missing`() {
        val result = familyController.getFamilyByPerson(dbInstance(), financeUser, clock, adult1.id)

        assertEquals(0, result.children.size)

        assertEquals(adult1.id, result.headOfFamily.personId)
    }

    @Test
    fun `show income total if user is finance admin`() {
        val incomeTotal = addIncome(adult1.id)
        createTestFixture1plus1()
        val result = familyController.getFamilyByPerson(dbInstance(), financeUser, clock, adult1.id)

        assertEquals(adult1.id, result.headOfFamily.personId)
        assertEquals(IncomeEffect.INCOME, result.headOfFamily.income?.effect)
        assertEquals(incomeTotal, result.headOfFamily.income?.total)
    }

    @Test
    fun `hide income total if user is unit supervisor`() {
        val unitSupervisor = initializeUnitSupervisorUserAndRights(child1.id)
        addIncome(adult1.id)
        createTestFixture1plus1()
        val result =
            familyController.getFamilyByPerson(dbInstance(), unitSupervisor, clock, adult1.id)

        assertEquals(adult1.id, result.headOfFamily.personId)
        assertEquals(null, result.headOfFamily.income)
    }

    private fun createTestFixture1plus1() {
        val (from, to) = LocalDate.now().let { listOf(it.minusYears(1), it.plusYears(1)) }
        db.transaction { it.createParentship(child1.id, adult1.id, from, to, Creator.DVV) }
    }

    private fun createTestFixture1plus2() {
        val (from, to) = LocalDate.now().let { listOf(it.minusYears(1), it.plusYears(1)) }
        db.transaction {
            it.createParentship(child1.id, adult1.id, from, to, Creator.DVV)
            it.createParentship(child2.id, adult1.id, from, to, Creator.DVV)
        }
    }

    private fun createTestFixture2plus2() {
        val (from, to) = LocalDate.now().let { listOf(it.minusYears(1), it.plusYears(1)) }
        db.transaction {
            val creator = financeUser.evakaUserId
            it.createParentship(child1.id, adult1.id, from, to, Creator.DVV)
            it.createParentship(child2.id, adult1.id, from, to, Creator.DVV)
            it.createPartnership(
                adult1.id,
                adult2.id,
                from,
                to,
                false,
                Creator.User(creator),
                clock.now(),
            )
        }
    }

    private fun initializeUnitSupervisorUserAndRights(
        childId: ChildId
    ): AuthenticatedUser.Employee {
        val externalId = ExternalId.of("test", "id")
        val unitSupervisor = DevEmployee(externalId = externalId)
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = childId,
                    unitId = daycare.id,
                    startDate = LocalDate.now(),
                    endDate = LocalDate.now().plusYears(1),
                )
            )
            it.insert(unitSupervisor)
            it.updateDaycareAcl(daycare.id, externalId, UserRole.UNIT_SUPERVISOR)
        }
        return AuthenticatedUser.Employee(unitSupervisor.id, setOf())
    }

    private fun addIncome(personId: PersonId): Int {
        val incomeTotal = 500000
        db.transaction {
            it.insert(
                DevIncome(
                    personId = personId,
                    effect = IncomeEffect.INCOME,
                    data =
                        mapOf(
                            "MAIN_INCOME" to
                                IncomeValue(
                                    incomeTotal,
                                    IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS,
                                    1,
                                    calculateMonthlyAmount(
                                        incomeTotal,
                                        coefficientMultiplierProvider.multiplier(
                                            IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
                                        ),
                                    ),
                                )
                        ),
                    modifiedBy = employee.evakaUserId,
                )
            )
        }
        return incomeTotal
    }
}
