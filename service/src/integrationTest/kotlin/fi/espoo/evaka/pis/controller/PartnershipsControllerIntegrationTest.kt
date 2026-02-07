// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.pis.Creator
import fi.espoo.evaka.pis.controllers.PartnershipsController
import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.getPartnership
import fi.espoo.evaka.pis.getPartnershipsForPerson
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.RealEvakaClock
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

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val person = DevPerson()
    private val partner = DevPerson()
    private val child = DevPerson(dateOfBirth = LocalDate.of(2020, 1, 1))
    private val financeAdmin = DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN))
    private val unitSupervisor = DevEmployee()
    private val serviceWorker = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))
    private val partnershipCreator = financeAdmin.evakaUserId
    private val clock = RealEvakaClock()

    @BeforeEach
    fun init() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            listOf(person, partner).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(financeAdmin)
            tx.insert(unitSupervisor)
            tx.insert(serviceWorker)
            tx.insert(
                DevParentship(
                    headOfChildId = person.id,
                    childId = child.id,
                    startDate = child.dateOfBirth,
                    endDate = LocalDate.now(),
                )
            )
            tx.insertDaycareAclRow(
                daycareId = daycare.id,
                employeeId = unitSupervisor.id,
                role = UserRole.UNIT_SUPERVISOR,
            )
            tx.insert(
                DevPlacement(childId = child.id, unitId = daycare.id, endDate = LocalDate.now())
            )
        }
    }

    @Test
    fun `service worker can create and fetch partnerships`() {
        `can create and fetch partnerships`(serviceWorker.user)
    }

    @Test
    fun `unit supervisor can create and fetch partnerships`() {
        `can create and fetch partnerships`(
            AuthenticatedUser.Employee(unitSupervisor.id, setOf(UserRole.UNIT_SUPERVISOR))
        )
    }

    @Test
    fun `finance admin can create and fetch partnerships`() {
        `can create and fetch partnerships`(financeAdmin.user)
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
                setOf(UserRole.SERVICE_WORKER),
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
                        clock.now(),
                    )
                    .also {
                        tx.createPartnership(
                            person.id,
                            partner.id,
                            LocalDate.now().plusDays(200),
                            LocalDate.now().plusDays(300),
                            false,
                            Creator.User(partnershipCreator),
                            clock.now(),
                        )
                        assertEquals(2, tx.getPartnershipsForPerson(person.id).size)
                    }
            }

        controller.deletePartnership(dbInstance(), user, clock, partnership1.id)
        db.read { r -> assertEquals(1, r.getPartnershipsForPerson(person.id).size) }
    }

    @Test
    fun `service worker can update partnerships`() {
        canUpdatePartnershipDuration(serviceWorker.user)
    }

    @Test
    fun `unit supervisor can update partnerships`() {
        canUpdatePartnershipDuration(
            AuthenticatedUser.Employee(unitSupervisor.id, setOf(UserRole.UNIT_SUPERVISOR))
        )
    }

    @Test
    fun `finance admin can update partnerships`() {
        canUpdatePartnershipDuration(financeAdmin.user)
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
                        clock.now(),
                    )
                    .also {
                        tx.createPartnership(
                            person.id,
                            partner.id,
                            LocalDate.now().plusDays(500),
                            LocalDate.now().plusDays(700),
                            false,
                            Creator.User(partnershipCreator),
                            clock.now(),
                        )
                    }
            }

        val newStartDate = LocalDate.now().plusDays(100)
        val newEndDate = LocalDate.now().plusDays(300)
        val requestBody = PartnershipsController.PartnershipUpdateRequest(newStartDate, newEndDate)
        controller.updatePartnership(dbInstance(), user, clock, partnership1.id, requestBody)

        // partnership1 should have new dates
        val fetched1 = db.read { it.getPartnership(partnership1.id) }!!
        assertEquals(newStartDate, fetched1.startDate)
        assertEquals(newEndDate, fetched1.endDate)
    }

    @Test
    fun `can updating partnership duration to overlap throws conflict`() {
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER),
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
                        clock.now(),
                    )
                    .also {
                        tx.createPartnership(
                            person.id,
                            partner.id,
                            LocalDate.now().plusDays(500),
                            LocalDate.now().plusDays(700),
                            false,
                            Creator.User(partnershipCreator),
                            clock.now(),
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
                    clock.now(),
                )
            }

        controller.retryPartnership(dbInstance(), financeAdmin.user, clock, partnership1.id)

        val getResponse =
            controller.getPartnerships(dbInstance(), financeAdmin.user, clock, person.id)
        assertEquals(1, getResponse.size)
        with(getResponse.first().data) { assertFalse(this.conflict) }
    }
}
