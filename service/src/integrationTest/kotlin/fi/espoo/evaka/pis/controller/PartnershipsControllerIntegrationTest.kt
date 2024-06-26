// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.Creator
import fi.espoo.evaka.pis.controllers.PartnershipsController
import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.getPartnershipsForPerson
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PartnershipsControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var controller: PartnershipsController

    private val person = testAdult_1
    private val partner = testAdult_2

    private val unitSupervisorId = EmployeeId(UUID.randomUUID())
    private val serviceWorkerId = EmployeeId(UUID.randomUUID())
    private val decisionMakerId = EmployeeId(UUID.randomUUID())
    private val testDecisionMakerEmployee =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN))
    private val partnershipCreator = testDecisionMakerEmployee.evakaUserId
    private val clock = RealEvakaClock()

    @BeforeEach
    fun init() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testDaycare)
            listOf(testAdult_1, testAdult_2).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(DevEmployee(unitSupervisorId))
            tx.insert(DevEmployee(serviceWorkerId))
            tx.insert(DevEmployee(decisionMakerId))
            tx.insert(
                DevParentship(
                    id = ParentshipId(UUID.randomUUID()),
                    headOfChildId = person.id,
                    childId = testChild_1.id,
                    startDate = testChild_1.dateOfBirth,
                    endDate = LocalDate.now()
                )
            )
            tx.insertDaycareAclRow(
                daycareId = testDaycare.id,
                employeeId = unitSupervisorId,
                role = UserRole.UNIT_SUPERVISOR
            )
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    endDate = LocalDate.now()
                )
            )
        }
    }

    @Test
    fun `service worker can create and fetch partnerships`() {
        `can create and fetch partnerships`(
            AuthenticatedUser.Employee(serviceWorkerId, setOf(UserRole.SERVICE_WORKER))
        )
    }

    @Test
    fun `unit supervisor can create and fetch partnerships`() {
        `can create and fetch partnerships`(
            AuthenticatedUser.Employee(unitSupervisorId, setOf(UserRole.UNIT_SUPERVISOR))
        )
    }

    @Test
    fun `finance admin can create and fetch partnerships`() {
        `can create and fetch partnerships`(
            AuthenticatedUser.Employee(decisionMakerId, setOf(UserRole.FINANCE_ADMIN))
        )
    }

    fun `can create and fetch partnerships`(user: AuthenticatedUser.Employee) {
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)
        val reqBody =
            PartnershipsController.PartnershipRequest(person.id, partner.id, startDate, endDate)
        controller.createPartnership(dbInstance(), user, clock, reqBody)

        val getResponse = controller.getPartnerships(dbInstance(), user, clock, person.id)
        assertEquals(1, getResponse.size)
        with(getResponse.first().data) {
            assertNotNull(this.id)
            assertEquals(startDate, this.startDate)
            assertEquals(endDate, this.endDate)
            assertEquals(setOf(person.id, partner.id), this.partners.map { it.id }.toSet())
        }
    }

    @Test
    fun `service worker can delete partnerships`() {
        canDeletePartnership(
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        )
    }

    @Test
    fun `finance admin can delete partnerships`() {
        canDeletePartnership(
            AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.FINANCE_ADMIN))
        )
    }

    private fun canDeletePartnership(user: AuthenticatedUser.Employee) {
        val partnership1 =
            db.transaction { tx ->
                tx.createPartnership(
                        person.id,
                        partner.id,
                        LocalDate.now(),
                        LocalDate.now().plusDays(100),
                        false,
                        Creator.User(partnershipCreator),
                        clock.now()
                    )
                    .also {
                        tx.createPartnership(
                            person.id,
                            partner.id,
                            LocalDate.now().plusDays(200),
                            LocalDate.now().plusDays(300),
                            false,
                            Creator.User(partnershipCreator),
                            clock.now()
                        )
                        assertEquals(2, tx.getPartnershipsForPerson(person.id).size)
                    }
            }

        controller.deletePartnership(dbInstance(), user, clock, partnership1.id)
        db.read { r -> assertEquals(1, r.getPartnershipsForPerson(person.id).size) }
    }

    @Test
    fun `service worker can update partnerships`() {
        canUpdatePartnershipDuration(
            AuthenticatedUser.Employee(serviceWorkerId, setOf(UserRole.SERVICE_WORKER))
        )
    }

    @Test
    fun `unit supervisor can update partnerships`() {
        canUpdatePartnershipDuration(
            AuthenticatedUser.Employee(unitSupervisorId, setOf(UserRole.UNIT_SUPERVISOR))
        )
    }

    @Test
    fun `finance admin can update partnerships`() {
        canUpdatePartnershipDuration(
            AuthenticatedUser.Employee(decisionMakerId, setOf(UserRole.FINANCE_ADMIN))
        )
    }

    private fun canUpdatePartnershipDuration(user: AuthenticatedUser.Employee) {
        val partnership1 =
            db.transaction { tx ->
                tx.createPartnership(
                        person.id,
                        partner.id,
                        LocalDate.now(),
                        LocalDate.now().plusDays(200),
                        false,
                        Creator.User(partnershipCreator),
                        clock.now()
                    )
                    .also {
                        tx.createPartnership(
                            person.id,
                            partner.id,
                            LocalDate.now().plusDays(500),
                            LocalDate.now().plusDays(700),
                            false,
                            Creator.User(partnershipCreator),
                            clock.now()
                        )
                    }
            }

        val newStartDate = LocalDate.now().plusDays(100)
        val newEndDate = LocalDate.now().plusDays(300)
        val requestBody = PartnershipsController.PartnershipUpdateRequest(newStartDate, newEndDate)
        controller.updatePartnership(dbInstance(), user, clock, partnership1.id, requestBody)

        // partnership1 should have new dates
        val fetched1 = controller.getPartnership(dbInstance(), user, clock, partnership1.id)
        assertEquals(newStartDate, fetched1.startDate)
        assertEquals(newEndDate, fetched1.endDate)
    }

    @Test
    fun `can updating partnership duration to overlap throws conflict`() {
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        val partnership1 =
            db.transaction { tx ->
                tx.createPartnership(
                        person.id,
                        partner.id,
                        LocalDate.now(),
                        LocalDate.now().plusDays(200),
                        false,
                        Creator.User(partnershipCreator),
                        clock.now()
                    )
                    .also {
                        tx.createPartnership(
                            person.id,
                            partner.id,
                            LocalDate.now().plusDays(500),
                            LocalDate.now().plusDays(700),
                            false,
                            Creator.User(partnershipCreator),
                            clock.now()
                        )
                    }
            }

        val newStartDate = LocalDate.now().plusDays(100)
        val newEndDate = LocalDate.now().plusDays(600)
        val requestBody = PartnershipsController.PartnershipUpdateRequest(newStartDate, newEndDate)
        assertThrows<Conflict> {
            controller.updatePartnership(dbInstance(), user, clock, partnership1.id, requestBody)
        }
    }

    @Test
    fun `clearing conflict flag works`() {
        val partnership1 =
            db.transaction { tx ->
                tx.createPartnership(
                    person.id,
                    partner.id,
                    LocalDate.now(),
                    LocalDate.now().plusDays(200),
                    true,
                    Creator.User(partnershipCreator),
                    clock.now()
                )
            }

        controller.retryPartnership(dbInstance(), testDecisionMakerEmployee, clock, partnership1.id)

        val getResponse =
            controller.getPartnerships(dbInstance(), testDecisionMakerEmployee, clock, person.id)
        assertEquals(1, getResponse.size)
        with(getResponse.first().data) { assertFalse(this.conflict) }
    }
}
