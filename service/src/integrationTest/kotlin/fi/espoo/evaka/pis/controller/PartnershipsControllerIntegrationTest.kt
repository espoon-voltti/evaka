// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.controllers.PartnershipsController
import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.getPartnershipsForPerson
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
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
    private val clock = RealEvakaClock()

    @BeforeEach
    fun init() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insert(DevEmployee(unitSupervisorId))
            it.insert(DevEmployee(serviceWorkerId))
            it.insert(DevEmployee(decisionMakerId))
            it.insert(
                DevParentship(
                    id = ParentshipId(UUID.randomUUID()),
                    headOfChildId = person.id,
                    childId = testChild_1.id,
                    startDate = testChild_1.dateOfBirth,
                    endDate = LocalDate.now()
                )
            )
            it.insertDaycareAclRow(
                daycareId = testDaycare.id,
                employeeId = unitSupervisorId,
                role = UserRole.UNIT_SUPERVISOR
            )
            it.insert(
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

    fun `can create and fetch partnerships`(user: AuthenticatedUser) {
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

    private fun canDeletePartnership(user: AuthenticatedUser) {
        val partnership1 =
            db.transaction { tx ->
                tx.createPartnership(
                        person.id,
                        partner.id,
                        LocalDate.now(),
                        LocalDate.now().plusDays(100),
                        false,
                        null,
                        clock.now()
                    )
                    .also {
                        tx.createPartnership(
                            person.id,
                            partner.id,
                            LocalDate.now().plusDays(200),
                            LocalDate.now().plusDays(300),
                            false,
                            null,
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

    private fun canUpdatePartnershipDuration(user: AuthenticatedUser) {
        val partnership1 =
            db.transaction { tx ->
                tx.createPartnership(
                        person.id,
                        partner.id,
                        LocalDate.now(),
                        LocalDate.now().plusDays(200),
                        false,
                        null,
                        clock.now()
                    )
                    .also {
                        tx.createPartnership(
                            person.id,
                            partner.id,
                            LocalDate.now().plusDays(500),
                            LocalDate.now().plusDays(700),
                            false,
                            null,
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
                        null,
                        clock.now()
                    )
                    .also {
                        tx.createPartnership(
                            person.id,
                            partner.id,
                            LocalDate.now().plusDays(500),
                            LocalDate.now().plusDays(700),
                            false,
                            null,
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
    fun `error is thrown if enduser tries to get partnerships`() {
        val user = AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
        db.transaction { tx ->
            tx.createPartnership(
                person.id,
                partner.id,
                LocalDate.now(),
                LocalDate.now().plusDays(200),
                false,
                null,
                clock.now()
            )
        }

        assertThrows<Forbidden> { controller.getPartnerships(dbInstance(), user, clock, person.id) }
    }

    @Test
    fun `error is thrown if enduser tries to create a partnership`() {
        val user = AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)
        val reqBody =
            PartnershipsController.PartnershipRequest(person.id, partner.id, startDate, endDate)

        assertThrows<Forbidden> { controller.createPartnership(dbInstance(), user, clock, reqBody) }
    }

    @Test
    fun `error is thrown if enduser tries to update partnerships`() {
        val user = AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
        val partnership =
            db.transaction { tx ->
                tx.createPartnership(
                    person.id,
                    partner.id,
                    LocalDate.now(),
                    LocalDate.now().plusDays(200),
                    false,
                    null,
                    clock.now()
                )
            }

        val requestBody =
            PartnershipsController.PartnershipUpdateRequest(
                LocalDate.now(),
                LocalDate.now().plusDays(999)
            )
        assertThrows<Forbidden> {
            controller.updatePartnership(dbInstance(), user, clock, partnership.id, requestBody)
        }
    }

    @Test
    fun `error is thrown if enduser tries to delete a partnership`() {
        val user = AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
        val partnership =
            db.transaction { tx ->
                tx.createPartnership(
                    person.id,
                    partner.id,
                    LocalDate.now(),
                    LocalDate.now().plusDays(200),
                    false,
                    null,
                    clock.now()
                )
            }

        assertThrows<Forbidden> {
            controller.deletePartnership(dbInstance(), user, clock, partnership.id)
        }
    }
}
