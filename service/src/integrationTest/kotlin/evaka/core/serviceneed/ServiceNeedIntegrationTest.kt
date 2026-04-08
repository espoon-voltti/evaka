// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.serviceneed

import evaka.core.FullApplicationTest
import evaka.core.insertServiceNeedOptions
import evaka.core.placement.PlacementController
import evaka.core.shared.ChildId
import evaka.core.shared.PlacementId
import evaka.core.shared.ServiceNeedId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevServiceNeed
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertServiceNeedOption
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.RealEvakaClock
import evaka.core.snDaycareFullDay25to35
import evaka.core.snDaycareFullDay35
import evaka.core.snDefaultDaycare
import evaka.core.snPreschoolDaycare45
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ServiceNeedIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var placementController: PlacementController
    @Autowired lateinit var serviceNeedController: ServiceNeedController

    private val clock = RealEvakaClock()
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val supervisor = DevEmployee()
    private val admin = DevEmployee()
    private val child = DevPerson()

    private val unitSupervisor =
        AuthenticatedUser.Employee(supervisor.id, setOf(UserRole.UNIT_SUPERVISOR))
    private val adminUser = AuthenticatedUser.Employee(admin.id, setOf(UserRole.ADMIN))

    lateinit var placementId: PlacementId

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(supervisor, mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(child, DevPersonType.CHILD)
            tx.insertServiceNeedOptions()
            placementId =
                tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = testDate(1),
                        endDate = testDate(30),
                    )
                )
        }
    }

    @Test
    fun `post first service need`() {
        serviceNeedController.postServiceNeed(
            dbInstance(),
            unitSupervisor,
            clock,
            ServiceNeedController.ServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(1),
                endDate = testDate(30),
                optionId = snDefaultDaycare.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
            ),
        )

        getServiceNeeds(child.id, placementId).let { res ->
            assertEquals(1, res.size)
            res.first().let { sn ->
                assertEquals(testDate(1), sn.startDate)
                assertEquals(testDate(30), sn.endDate)
                assertEquals(placementId, sn.placementId)
                assertEquals(snDefaultDaycare.id, sn.option.id)
                assertEquals(snDefaultDaycare.nameFi, sn.option.nameFi)
            }
        }
    }

    @Test
    fun `post service need with inverted range`() {
        assertThrows<BadRequest> {
            serviceNeedController.postServiceNeed(
                dbInstance(),
                unitSupervisor,
                clock,
                ServiceNeedController.ServiceNeedCreateRequest(
                    placementId = placementId,
                    startDate = testDate(5),
                    endDate = testDate(3),
                    optionId = snDefaultDaycare.id,
                    shiftCare = ShiftCareType.NONE,
                    partWeek = false,
                ),
            )
        }
    }

    @Test
    fun `post service need with range outside placement`() {
        assertThrows<BadRequest> {
            serviceNeedController.postServiceNeed(
                dbInstance(),
                unitSupervisor,
                clock,
                ServiceNeedController.ServiceNeedCreateRequest(
                    placementId = placementId,
                    startDate = testDate(1),
                    endDate = testDate(31),
                    optionId = snDefaultDaycare.id,
                    shiftCare = ShiftCareType.NONE,
                    partWeek = false,
                ),
            )
        }
    }

    @Test
    fun `post service need with range not contained by option validity`() {
        db.transaction {
            it.createUpdate {
                    sql(
                        """
                UPDATE service_need_option SET valid_from = ${bind(testDate(2))}
                WHERE id = ${bind(snDefaultDaycare.id)}
            """
                    )
                }
                .execute()
        }
        assertThrows<BadRequest> {
            serviceNeedController.postServiceNeed(
                dbInstance(),
                unitSupervisor,
                clock,
                ServiceNeedController.ServiceNeedCreateRequest(
                    placementId = placementId,
                    startDate = testDate(1),
                    endDate = testDate(30),
                    optionId = snDefaultDaycare.id,
                    shiftCare = ShiftCareType.NONE,
                    partWeek = false,
                ),
            )
        }
    }

    @Test
    fun `post service need with invalid option`() {
        assertThrows<BadRequest> {
            serviceNeedController.postServiceNeed(
                dbInstance(),
                unitSupervisor,
                clock,
                ServiceNeedController.ServiceNeedCreateRequest(
                    placementId = placementId,
                    startDate = testDate(1),
                    endDate = testDate(31),
                    optionId = snPreschoolDaycare45.id,
                    shiftCare = ShiftCareType.NONE,
                    partWeek = false,
                ),
            )
        }
    }

    @Test
    fun `post service need, no overlap`() {
        givenServiceNeed(1, 15, placementId)

        serviceNeedController.postServiceNeed(
            dbInstance(),
            unitSupervisor,
            clock,
            ServiceNeedController.ServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(16),
                endDate = testDate(30),
                optionId = snDaycareFullDay35.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
            ),
        )

        getServiceNeeds(child.id, placementId).let { serviceNeeds ->
            assertEquals(2, serviceNeeds.size)
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(15) }
            )
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(16) && it.endDate == testDate(30) }
            )
        }
    }

    @Test
    fun `post service need, fully encloses previous`() {
        givenServiceNeed(10, 20, placementId)

        serviceNeedController.postServiceNeed(
            dbInstance(),
            unitSupervisor,
            clock,
            ServiceNeedController.ServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(1),
                endDate = testDate(30),
                optionId = snDaycareFullDay35.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
            ),
        )

        getServiceNeeds(child.id, placementId).let { res ->
            assertEquals(1, res.size)
            res.first().let { sn ->
                assertEquals(testDate(1), sn.startDate)
                assertEquals(testDate(30), sn.endDate)
                assertEquals(placementId, sn.placementId)
                assertEquals(snDaycareFullDay35.id, sn.option.id)
                assertEquals(snDaycareFullDay35.nameFi, sn.option.nameFi)
            }
        }
    }

    @Test
    fun `post service need, starts during existing`() {
        givenServiceNeed(1, 30, placementId)

        serviceNeedController.postServiceNeed(
            dbInstance(),
            unitSupervisor,
            clock,
            ServiceNeedController.ServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(16),
                endDate = testDate(30),
                optionId = snDaycareFullDay35.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
            ),
        )

        getServiceNeeds(child.id, placementId).let { serviceNeeds ->
            assertEquals(2, serviceNeeds.size)
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(15) }
            )
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(16) && it.endDate == testDate(30) }
            )
        }
    }

    @Test
    fun `post service need, ends during existing`() {
        givenServiceNeed(10, 30, placementId)

        serviceNeedController.postServiceNeed(
            dbInstance(),
            unitSupervisor,
            clock,
            ServiceNeedController.ServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(1),
                endDate = testDate(20),
                optionId = snDaycareFullDay35.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
            ),
        )

        getServiceNeeds(child.id, placementId).let { serviceNeeds ->
            assertEquals(2, serviceNeeds.size)
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(20) }
            )
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(21) && it.endDate == testDate(30) }
            )
        }
    }

    @Test
    fun `post service need, is inside existing`() {
        givenServiceNeed(1, 30, placementId)

        serviceNeedController.postServiceNeed(
            dbInstance(),
            unitSupervisor,
            clock,
            ServiceNeedController.ServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(10),
                endDate = testDate(20),
                optionId = snDaycareFullDay35.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
            ),
        )

        getServiceNeeds(child.id, placementId).let { serviceNeeds ->
            assertEquals(3, serviceNeeds.size)
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(9) }
            )
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(10) && it.endDate == testDate(20) }
            )
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(21) && it.endDate == testDate(30) }
            )
        }
    }

    @Test
    fun `post service need, spans multiple`() {
        givenServiceNeed(1, 9, placementId)
        givenServiceNeed(10, 19, placementId)
        givenServiceNeed(20, 30, placementId)

        serviceNeedController.postServiceNeed(
            dbInstance(),
            unitSupervisor,
            clock,
            ServiceNeedController.ServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(5),
                endDate = testDate(25),
                optionId = snDaycareFullDay35.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
            ),
        )

        getServiceNeeds(child.id, placementId).let { serviceNeeds ->
            assertEquals(3, serviceNeeds.size)
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(4) }
            )
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(5) && it.endDate == testDate(25) }
            )
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(26) && it.endDate == testDate(30) }
            )
        }
    }

    @Test
    fun `delete service need`() {
        givenServiceNeed(1, 9, placementId)
        val idToDelete = givenServiceNeed(10, 19, placementId)
        givenServiceNeed(20, 30, placementId)

        serviceNeedController.deleteServiceNeed(dbInstance(), adminUser, clock, idToDelete)

        getServiceNeeds(child.id, placementId).let { serviceNeeds ->
            assertEquals(2, serviceNeeds.size)
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(9) }
            )
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(20) && it.endDate == testDate(30) }
            )
        }
    }

    @Test
    fun `update service need`() {
        givenServiceNeed(1, 9, placementId)
        val idToUpdate = givenServiceNeed(10, 19, placementId)
        givenServiceNeed(20, 30, placementId)

        serviceNeedController.putServiceNeed(
            dbInstance(),
            unitSupervisor,
            clock,
            idToUpdate,
            ServiceNeedController.ServiceNeedUpdateRequest(
                startDate = testDate(5),
                endDate = testDate(25),
                optionId = snDaycareFullDay25to35.id,
                shiftCare = ShiftCareType.FULL,
                partWeek = false,
            ),
        )

        getServiceNeeds(child.id, placementId).let { serviceNeeds ->
            assertEquals(3, serviceNeeds.size)
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(4) }
            )
            assertTrue(
                serviceNeeds.any {
                    it.startDate == testDate(5) &&
                        it.endDate == testDate(25) &&
                        it.shiftCare == ShiftCareType.FULL &&
                        it.partWeek == false &&
                        it.option.id == snDaycareFullDay25to35.id
                }
            )
            assertTrue(
                serviceNeeds.any { it.startDate == testDate(26) && it.endDate == testDate(30) }
            )
        }
    }

    @Test
    fun `cannot update service need with non-matching partWeek`() {
        val idToUpdate = givenServiceNeed(1, 30, placementId)

        assertThrows<BadRequest> {
            serviceNeedController.putServiceNeed(
                dbInstance(),
                unitSupervisor,
                clock,
                idToUpdate,
                ServiceNeedController.ServiceNeedUpdateRequest(
                    startDate = testDate(1),
                    endDate = testDate(30),
                    optionId = snDaycareFullDay25to35.id,
                    shiftCare = ShiftCareType.FULL,
                    partWeek = true,
                ),
            )
        }
    }

    @Test
    fun `update service need without partWeek default`() {
        val optionId = ServiceNeedOptionId(UUID.randomUUID())
        db.transaction {
            it.insertServiceNeedOption(snDaycareFullDay35.copy(id = optionId, partWeek = null))
        }
        val idToUpdate = givenServiceNeed(1, 30, placementId, optionId)
        getServiceNeeds(child.id, placementId).let { serviceNeeds ->
            assertEquals(false, serviceNeeds.first().partWeek)
        }

        serviceNeedController.putServiceNeed(
            dbInstance(),
            unitSupervisor,
            clock,
            idToUpdate,
            ServiceNeedController.ServiceNeedUpdateRequest(
                startDate = testDate(1),
                endDate = testDate(30),
                optionId = optionId,
                shiftCare = ShiftCareType.FULL,
                partWeek = true,
            ),
        )
        getServiceNeeds(child.id, placementId).let { serviceNeeds ->
            assertEquals(true, serviceNeeds.first().partWeek)
        }
    }

    private fun testDate(day: Int) = LocalDate.now().plusDays(day.toLong())

    private fun givenServiceNeed(
        start: Int,
        end: Int,
        placementId: PlacementId,
        optionId: ServiceNeedOptionId = snDefaultDaycare.id,
    ): ServiceNeedId {
        return db.transaction { tx ->
            val period = FiniteDateRange(testDate(start), testDate(end))
            tx.insert(
                DevServiceNeed(
                    placementId = placementId,
                    startDate = period.start,
                    endDate = period.end,
                    optionId = optionId,
                    confirmedBy = unitSupervisor.evakaUserId,
                    confirmedAt = HelsinkiDateTime.now(),
                )
            )
        }
    }

    @Test
    fun `get child future service needs returns only future service needs`() {
        val today = LocalDate.of(2024, 1, 15)
        val pastServiceNeedId =
            db.transaction { tx ->
                tx.insert(
                    DevServiceNeed(
                        placementId = placementId,
                        startDate = LocalDate.of(2024, 1, 1),
                        endDate = LocalDate.of(2024, 1, 10),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.NONE,
                        partWeek = false,
                        confirmedBy = unitSupervisor.evakaUserId,
                        confirmedAt = HelsinkiDateTime.now(),
                    )
                )
            }
        val currentServiceNeedId =
            db.transaction { tx ->
                tx.insert(
                    DevServiceNeed(
                        placementId = placementId,
                        startDate = LocalDate.of(2024, 1, 11),
                        endDate = LocalDate.of(2024, 1, 20),
                        optionId = snDaycareFullDay35.id,
                        shiftCare = ShiftCareType.FULL,
                        partWeek = false,
                        confirmedBy = unitSupervisor.evakaUserId,
                        confirmedAt = HelsinkiDateTime.now(),
                    )
                )
            }
        val futureServiceNeedId =
            db.transaction { tx ->
                tx.insert(
                    DevServiceNeed(
                        placementId = placementId,
                        startDate = LocalDate.of(2024, 1, 21),
                        endDate = LocalDate.of(2024, 1, 30),
                        optionId = snDaycareFullDay25to35.id,
                        shiftCare = ShiftCareType.NONE,
                        partWeek = true,
                        confirmedBy = unitSupervisor.evakaUserId,
                        confirmedAt = HelsinkiDateTime.now(),
                    )
                )
            }

        val result =
            serviceNeedController.getChildServiceNeeds(
                dbInstance(),
                unitSupervisor,
                MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.MIDNIGHT)),
                child.id,
                today,
            )

        assertEquals(2, result.size)
        assertTrue(result.any { it.validDuring.start == LocalDate.of(2024, 1, 11) })
        assertTrue(result.any { it.validDuring.start == LocalDate.of(2024, 1, 21) })
        assertTrue(result.none { it.validDuring.start == LocalDate.of(2024, 1, 1) })
    }

    @Test
    fun `get child future service needs with multiple future periods`() {
        val today = LocalDate.of(2024, 1, 15)
        db.transaction { tx ->
            tx.insert(
                DevServiceNeed(
                    placementId = placementId,
                    startDate = LocalDate.of(2024, 1, 20),
                    endDate = LocalDate.of(2024, 2, 10),
                    optionId = snDefaultDaycare.id,
                    shiftCare = ShiftCareType.NONE,
                    partWeek = false,
                    confirmedBy = unitSupervisor.evakaUserId,
                    confirmedAt = HelsinkiDateTime.now(),
                )
            )
            tx.insert(
                DevServiceNeed(
                    placementId = placementId,
                    startDate = LocalDate.of(2024, 2, 11),
                    endDate = LocalDate.of(2024, 3, 15),
                    optionId = snDaycareFullDay35.id,
                    shiftCare = ShiftCareType.FULL,
                    partWeek = false,
                    confirmedBy = unitSupervisor.evakaUserId,
                    confirmedAt = HelsinkiDateTime.now(),
                )
            )
        }

        val result =
            serviceNeedController.getChildServiceNeeds(
                dbInstance(),
                unitSupervisor,
                MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.MIDNIGHT)),
                child.id,
                today,
            )

        assertEquals(2, result.size)
        assertEquals(ShiftCareType.NONE, result[0].shiftCare)
        assertEquals(ShiftCareType.FULL, result[1].shiftCare)
    }

    private fun getServiceNeeds(childId: ChildId, placementId: PlacementId): List<ServiceNeed> =
        placementController
            .getChildPlacements(dbInstance(), unitSupervisor, clock, childId = childId)
            .placements
            .first { it.id == placementId }
            .serviceNeeds
}
