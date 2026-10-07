// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.serviceneed

import evaka.core.FullApplicationTest
import evaka.core.daycare.domain.ProviderType
import evaka.core.insertServiceNeedOptions
import evaka.core.invoicing.controller.insertNewVoucherValue
import evaka.core.invoicing.service.generator.ServiceNeedOptionVoucherValueRange
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
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.RealEvakaClock
import evaka.core.snDaycareFullDay25to35
import evaka.core.snDaycareFullDay35
import evaka.core.snDefaultDaycare
import evaka.core.snPreschoolDaycare45
import java.math.BigDecimal
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
    private val voucherUnit =
        DevDaycare(areaId = area.id, providerType = ProviderType.PRIVATE_SERVICE_VOUCHER)
    private val supervisor = DevEmployee()
    private val admin = DevEmployee()
    private val child = DevPerson()

    private val unitSupervisor =
        AuthenticatedUser.Employee(supervisor.id, setOf(UserRole.UNIT_SUPERVISOR))
    private val adminUser = AuthenticatedUser.Employee(admin.id, setOf(UserRole.ADMIN))

    // Finance decisions are generated from 2021-10-07 onwards: five years before the clock's date,
    // which is later than the integration test fee decision min date
    private val financeClock = MockEvakaClock(2026, 10, 7, 12, 0)
    private val voucherValuesStart = LocalDate.of(2024, 8, 1)
    private val optionWithLateVoucherValues =
        snDaycareFullDay35.copy(
            id = ServiceNeedOptionId(UUID.randomUUID()),
            nameFi = "Voucher value from 2024-08-01",
            nameSv = "Voucher value from 2024-08-01",
            nameEn = "Voucher value from 2024-08-01",
        )

    lateinit var placementId: PlacementId

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(voucherUnit)
            tx.insert(
                supervisor,
                mapOf(
                    daycare.id to UserRole.UNIT_SUPERVISOR,
                    voucherUnit.id to UserRole.UNIT_SUPERVISOR,
                ),
            )
            tx.insert(child, DevPersonType.CHILD)
            tx.insertServiceNeedOptions()
            tx.insertServiceNeedOption(optionWithLateVoucherValues)
            tx.insertNewVoucherValue(
                ServiceNeedOptionVoucherValueRange(
                    serviceNeedOptionId = optionWithLateVoucherValues.id,
                    range = DateRange(voucherValuesStart, null),
                    baseValue = 94900,
                    coefficient = BigDecimal("0.80"),
                    value = 75920,
                    baseValueUnder3y = 147095,
                    coefficientUnder3y = BigDecimal("0.80"),
                    valueUnder3y = 117676,
                )
            )
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

    @Test
    fun `post service need in voucher unit with option missing voucher value`() {
        val placementId =
            givenPlacement(voucherUnit, LocalDate.of(2023, 9, 11), LocalDate.of(2024, 7, 31))

        val exception =
            assertThrows<BadRequest> {
                postServiceNeedAtFinanceClock(
                    placementId,
                    LocalDate.of(2023, 9, 11),
                    LocalDate.of(2024, 7, 31),
                    optionWithLateVoucherValues.id,
                )
            }
        assertEquals("VOUCHER_VALUE_MISSING", exception.errorCode)
    }

    @Test
    fun `post service need in voucher unit with voucher value covering only part of the period`() {
        val placementId =
            givenPlacement(voucherUnit, LocalDate.of(2024, 5, 1), LocalDate.of(2024, 12, 31))

        val exception =
            assertThrows<BadRequest> {
                postServiceNeedAtFinanceClock(
                    placementId,
                    LocalDate.of(2024, 5, 1),
                    LocalDate.of(2024, 12, 31),
                    optionWithLateVoucherValues.id,
                )
            }
        assertEquals("VOUCHER_VALUE_MISSING", exception.errorCode)
    }

    @Test
    fun `cannot update service need in voucher unit to option missing voucher value`() {
        val placementId =
            givenPlacement(voucherUnit, LocalDate.of(2023, 9, 11), LocalDate.of(2024, 7, 31))
        val serviceNeed =
            DevServiceNeed(
                placementId = placementId,
                startDate = LocalDate.of(2023, 9, 11),
                endDate = LocalDate.of(2024, 7, 31),
                optionId = snDaycareFullDay35.id,
                confirmedBy = unitSupervisor.evakaUserId,
            )
        db.transaction { tx -> tx.insert(serviceNeed) }

        val exception =
            assertThrows<BadRequest> {
                serviceNeedController.putServiceNeed(
                    dbInstance(),
                    unitSupervisor,
                    financeClock,
                    serviceNeed.id,
                    ServiceNeedController.ServiceNeedUpdateRequest(
                        startDate = serviceNeed.startDate,
                        endDate = serviceNeed.endDate,
                        optionId = optionWithLateVoucherValues.id,
                        shiftCare = ShiftCareType.NONE,
                        partWeek = false,
                    ),
                )
            }
        assertEquals("VOUCHER_VALUE_MISSING", exception.errorCode)
    }

    @Test
    fun `post service need in voucher unit with voucher value for the whole period`() {
        val placementId =
            givenPlacement(voucherUnit, voucherValuesStart, LocalDate.of(2024, 12, 31))

        postServiceNeedAtFinanceClock(
            placementId,
            voucherValuesStart,
            LocalDate.of(2024, 12, 31),
            optionWithLateVoucherValues.id,
        )
    }

    @Test
    fun `post service need in voucher unit without voucher value before finance decisions are generated`() {
        val placementId =
            givenPlacement(voucherUnit, LocalDate.of(2020, 8, 1), LocalDate.of(2021, 7, 31))

        postServiceNeedAtFinanceClock(
            placementId,
            LocalDate.of(2020, 8, 1),
            LocalDate.of(2021, 7, 31),
            optionWithLateVoucherValues.id,
        )
    }

    @Test
    fun `post service need in municipal unit with option missing voucher value`() {
        val placementId =
            givenPlacement(daycare, LocalDate.of(2023, 9, 11), LocalDate.of(2024, 7, 31))

        postServiceNeedAtFinanceClock(
            placementId,
            LocalDate.of(2023, 9, 11),
            LocalDate.of(2024, 7, 31),
            optionWithLateVoucherValues.id,
        )
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
        val pastServiceNeedId = db.transaction { tx ->
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
        val currentServiceNeedId = db.transaction { tx ->
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
        val futureServiceNeedId = db.transaction { tx ->
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

    private fun givenPlacement(unit: DevDaycare, start: LocalDate, end: LocalDate): PlacementId =
        db.transaction { tx ->
            tx.insert(
                DevPlacement(childId = child.id, unitId = unit.id, startDate = start, endDate = end)
            )
        }

    private fun postServiceNeedAtFinanceClock(
        placementId: PlacementId,
        start: LocalDate,
        end: LocalDate,
        optionId: ServiceNeedOptionId,
    ) =
        serviceNeedController.postServiceNeed(
            dbInstance(),
            unitSupervisor,
            financeClock,
            ServiceNeedController.ServiceNeedCreateRequest(
                placementId = placementId,
                startDate = start,
                endDate = end,
                optionId = optionId,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
            ),
        )

    private fun getServiceNeeds(childId: ChildId, placementId: PlacementId): List<ServiceNeed> =
        placementController
            .getChildPlacements(dbInstance(), unitSupervisor, clock, childId = childId)
            .placements
            .first { it.id == placementId }
            .serviceNeedDetail
            ?.serviceNeeds
            .orEmpty()
}
