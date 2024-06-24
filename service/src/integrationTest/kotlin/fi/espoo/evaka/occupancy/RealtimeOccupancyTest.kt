// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attendance.ExternalStaffArrival
import fi.espoo.evaka.attendance.ExternalStaffDeparture
import fi.espoo.evaka.attendance.markExternalStaffArrival
import fi.espoo.evaka.attendance.markExternalStaffDeparture
import fi.espoo.evaka.attendance.occupancyCoefficientSeven
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevChildAttendance
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.DevStaffAttendance
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import fi.espoo.evaka.snDaycareContractDays10
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RealtimeOccupancyTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val date = LocalDate.of(2022, 5, 23)
    private val groupId = GroupId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insert(DevDaycareGroup(groupId, testDaycare.id))
        }
    }

    @Test
    fun `occupancy is calculated from attendances`() {
        db.transaction { tx ->
            val child1 = DevPerson(dateOfBirth = date.minusYears(2).minusDays(10))
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date
                )
            )
            tx.insert(
                DevChildAttendance(
                    childId = child1.id,
                    unitId = testDaycare.id,
                    date = date,
                    arrived = LocalTime.of(7, 45),
                    departed = LocalTime.of(16, 30)
                )
            )

            val child2 = DevPerson(dateOfBirth = date.minusYears(2).minusDays(11))
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date
                )
            )
            tx.insert(
                DevAssistanceFactor(
                    childId = child2.id,
                    validDuring = date.toFiniteDateRange(),
                    capacityFactor = 2.0
                )
            )
            tx.insert(
                DevChildAttendance(
                    childId = child2.id,
                    unitId = testDaycare.id,
                    date = date,
                    arrived = LocalTime.of(8, 15),
                    departed = LocalTime.of(16, 30)
                )
            )

            val child3 = DevPerson(dateOfBirth = date.minusYears(3))
            tx.insert(child3, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child3.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date
                )
            )
            tx.insert(
                DevChildAttendance(
                    childId = child3.id,
                    unitId = testDaycare.id,
                    date = date,
                    arrived = LocalTime.of(8, 15),
                    departed = null
                )
            )

            val child4 = DevPerson(dateOfBirth = date.minusYears(2).minusMonths(11))
            tx.insert(child4, DevPersonType.CHILD)
            val placement4 =
                DevPlacement(
                    childId = child4.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date
                )
            tx.insert(placement4)
            tx.insert(
                DevServiceNeed(
                    placementId = placement4.id,
                    optionId = snDaycareContractDays10.id,
                    startDate = date,
                    endDate = date,
                    confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw)
                )
            )
            tx.insert(
                DevChildAttendance(
                    childId = child4.id,
                    unitId = testDaycare.id,
                    date = date,
                    arrived = LocalTime.of(8, 30),
                    departed = LocalTime.of(16, 30)
                )
            )

            val employee = DevEmployee()
            tx.insert(
                employee,
                mapOf(testDaycare.id to UserRole.STAFF),
                mapOf(testDaycare.id to listOf(groupId))
            )
            tx.insert(
                DevStaffAttendance(
                    employeeId = employee.id,
                    groupId = groupId,
                    arrived = HelsinkiDateTime.of(date, LocalTime.of(7, 0)),
                    departed = HelsinkiDateTime.of(date, LocalTime.of(16, 45)),
                    occupancyCoefficient = occupancyCoefficientSeven
                )
            )

            tx
                .markExternalStaffArrival(
                    ExternalStaffArrival(
                        "Matti",
                        groupId,
                        HelsinkiDateTime.of(date, LocalTime.of(10, 0)),
                        BigDecimal("3.5")
                    )
                ).let { extAttendanceId ->
                    tx.markExternalStaffDeparture(
                        ExternalStaffDeparture(
                            extAttendanceId,
                            HelsinkiDateTime.of(date, LocalTime.of(18, 0))
                        )
                    )
                }

            // staff with coefficient of zero does not affect occupancy rates
            tx
                .markExternalStaffArrival(
                    ExternalStaffArrival(
                        "Nolla Sijainen",
                        groupId,
                        HelsinkiDateTime.of(date, LocalTime.of(11, 0)),
                        BigDecimal.ZERO
                    )
                ).let { extAttendanceId ->
                    tx.markExternalStaffDeparture(
                        ExternalStaffDeparture(
                            extAttendanceId,
                            HelsinkiDateTime.of(date, LocalTime.of(15, 0))
                        )
                    )
                }
        }

        val child1Capacity = 1.75
        val child2Capacity = 2.0 * 1.75
        val child3Capacity = 1.0
        val child4Capacity = 1.25 // service need option's realizedOccupancyCoefficientUnder3y

        val result = getRealtimeOccupancy()
        val occupancies = result.occupancySeries
        assertEquals(10, occupancies.size)

        // 7:00, staff 1 arrives
        occupancies
            .find { it.time == HelsinkiDateTime.of(date, LocalTime.of(7, 0)) }
            ?.also { assertEquals(0.0, it.childCapacity) }
            ?.also { assertEquals(7.0, it.staffCapacity) }
            ?.also { assertEquals(0.0, it.occupancyRatio) } ?: error("data point missing")

        // 7:45, child 1 arrives
        occupancies
            .find { it.time == HelsinkiDateTime.of(date, LocalTime.of(7, 45)) }
            ?.also { assertEquals(child1Capacity, it.childCapacity) }
            ?.also { assertEquals(7.0, it.staffCapacity) }
            ?.also { assertEquals(child1Capacity / (7 * 1), it.occupancyRatio) }
            ?: error("data point missing")

        // 8:15, children 2 and 3 arrive
        occupancies
            .find { it.time == HelsinkiDateTime.of(date, LocalTime.of(8, 15)) }
            ?.also {
                assertEquals(child1Capacity + child2Capacity + child3Capacity, it.childCapacity)
            }?.also { assertEquals(7.0, it.staffCapacity) }
            ?.also {
                assertEquals(
                    (child1Capacity + child2Capacity + child3Capacity) / (7 * 1),
                    it.occupancyRatio
                )
            } ?: error("data point missing")

        // 8:30, child 4 arrives
        occupancies
            .find { it.time == HelsinkiDateTime.of(date, LocalTime.of(8, 30)) }
            ?.also {
                assertEquals(
                    child1Capacity + child2Capacity + child3Capacity + child4Capacity,
                    it.childCapacity
                )
            }?.also { assertEquals(7.0, it.staffCapacity) }
            ?.also {
                assertEquals(
                    (child1Capacity + child2Capacity + child3Capacity + child4Capacity) / (7 * 1),
                    it.occupancyRatio
                )
            } ?: error("data point missing")

        // 10:00, staff 2 arrives
        occupancies
            .find { it.time == HelsinkiDateTime.of(date, LocalTime.of(10, 0)) }
            ?.also {
                assertEquals(
                    child1Capacity + child2Capacity + child3Capacity + child4Capacity,
                    it.childCapacity
                )
            }?.also { assertEquals(10.5, it.staffCapacity) }
            ?.also {
                assertEquals(
                    (child1Capacity + child2Capacity + child3Capacity + child4Capacity) / 10.5,
                    it.occupancyRatio
                )
            } ?: error("data point missing")

        // 11:00, staff 3 with zero coefficient arrives, does not affect capacities
        occupancies
            .find { it.time == HelsinkiDateTime.of(date, LocalTime.of(11, 0)) }
            ?.also {
                assertEquals(
                    child1Capacity + child2Capacity + child3Capacity + child4Capacity,
                    it.childCapacity
                )
            }?.also { assertEquals(10.5, it.staffCapacity) }
            ?.also {
                assertEquals(
                    (child1Capacity + child2Capacity + child3Capacity + child4Capacity) / 10.5,
                    it.occupancyRatio
                )
            } ?: error("data point missing")

        // 15:00, staff 3 with zero coefficient departs, does not affect capacities
        occupancies
            .find { it.time == HelsinkiDateTime.of(date, LocalTime.of(15, 0)) }
            ?.also {
                assertEquals(
                    child1Capacity + child2Capacity + child3Capacity + child4Capacity,
                    it.childCapacity
                )
            }?.also { assertEquals(10.5, it.staffCapacity) }
            ?.also {
                assertEquals(
                    (child1Capacity + child2Capacity + child3Capacity + child4Capacity) / 10.5,
                    it.occupancyRatio
                )
            } ?: error("data point missing")

        // 16:30, children 1, 2 and 4 depart
        occupancies
            .find { it.time == HelsinkiDateTime.of(date, LocalTime.of(16, 30)) }
            ?.also { assertEquals(child3Capacity, it.childCapacity) }
            ?.also { assertEquals(10.5, it.staffCapacity) }
            ?.also { assertEquals(child3Capacity / 10.5, it.occupancyRatio) }
            ?: error("data point missing")

        // 16:45, staff 1 departs
        occupancies
            .find { it.time == HelsinkiDateTime.of(date, LocalTime.of(16, 45)) }
            ?.also { assertEquals(child3Capacity, it.childCapacity) }
            ?.also { assertEquals(3.5, it.staffCapacity) }
            ?.also { assertEquals(child3Capacity / 3.5, it.occupancyRatio) }
            ?: error("data point missing")

        // 18:00, staff 2 departs, forgets to mark child 3 departed
        occupancies
            .find { it.time == HelsinkiDateTime.of(date, LocalTime.of(18, 0)) }
            ?.also { assertEquals(child3Capacity, it.childCapacity) }
            ?.also { assertEquals(0.0, it.staffCapacity) }
            ?.also { assertNull(it.occupancyRatio) } ?: error("data point missing")
    }

    @Test
    fun `child without placement gets capacity factor 1`() {
        db.transaction { tx ->
            val child = DevPerson(dateOfBirth = date.minusYears(2).minusMonths(10))
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevChildAttendance(
                    childId = child.id,
                    unitId = testDaycare.id,
                    date = date,
                    arrived = LocalTime.of(7, 45),
                    departed = LocalTime.of(16, 30)
                )
            )
        }

        val result = getRealtimeOccupancy()
        val occupancies = result.occupancySeries
        assertEquals(2, occupancies.size)
        assertEquals(1.0, occupancies[0].childCapacity)
    }

    private fun getRealtimeOccupancy(
        timeRange: HelsinkiDateTimeRange =
            HelsinkiDateTimeRange(
                HelsinkiDateTime.of(date, LocalTime.of(0, 0)),
                HelsinkiDateTime.of(date, LocalTime.of(23, 59))
            )
    ): RealtimeOccupancy =
        db.read { tx ->
            RealtimeOccupancy(
                childAttendances = tx.getChildOccupancyAttendances(testDaycare.id, timeRange),
                staffAttendances = tx.getStaffOccupancyAttendances(testDaycare.id, timeRange)
            )
        }

    @Test
    fun `graph data shows no gaps for overnight attendances`() {
        val tomorrow = date.plusDays(1)
        db.transaction { tx ->
            val child1 = DevPerson(dateOfBirth = date.minusYears(2).minusMonths(10))
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date.plusMonths(2)
                )
            )
            listOf(
                DevChildAttendance(
                    childId = child1.id,
                    unitId = testDaycare.id,
                    date = date,
                    arrived = LocalTime.of(20, 45),
                    departed = LocalTime.of(23, 59)
                ),
                DevChildAttendance(
                    childId = child1.id,
                    unitId = testDaycare.id,
                    date = tomorrow,
                    arrived = LocalTime.of(0, 0),
                    departed = LocalTime.of(8, 15)
                )
            ).forEach { tx.insert(it) }

            val child2 = DevPerson(dateOfBirth = date.minusYears(4).minusMonths(3))
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date.plusMonths(2)
                )
            )
            listOf(
                DevChildAttendance(
                    childId = child2.id,
                    unitId = testDaycare.id,
                    date = date,
                    arrived = LocalTime.of(21, 5),
                    departed = LocalTime.of(23, 59)
                ),
                DevChildAttendance(
                    childId = child2.id,
                    unitId = testDaycare.id,
                    date = tomorrow,
                    arrived = LocalTime.of(0, 0),
                    departed = LocalTime.of(8, 50)
                )
            ).forEach { tx.insert(it) }

            val employee = DevEmployee()
            tx.insert(
                employee,
                mapOf(testDaycare.id to UserRole.STAFF),
                mapOf(testDaycare.id to listOf(groupId))
            )
            tx.insert(
                DevStaffAttendance(
                    employeeId = employee.id,
                    groupId = groupId,
                    arrived = HelsinkiDateTime.of(date, LocalTime.of(19, 45)),
                    departed = HelsinkiDateTime.of(tomorrow, LocalTime.of(9, 0)),
                    occupancyCoefficient = occupancyCoefficientSeven
                )
            )
        }

        val result =
            getRealtimeOccupancy(
                HelsinkiDateTimeRange(
                    HelsinkiDateTime.of(date, LocalTime.of(0, 0)),
                    HelsinkiDateTime.of(tomorrow, LocalTime.of(23, 59))
                )
            )
        val occupancies = result.occupancySeries

        assertEquals(
            listOf(
                OccupancyPoint(HelsinkiDateTime.of(date, LocalTime.of(19, 45)), 0.0, 7.0),
                OccupancyPoint(HelsinkiDateTime.of(date, LocalTime.of(20, 45)), 1.75, 7.0),
                OccupancyPoint(HelsinkiDateTime.of(date, LocalTime.of(21, 5)), 2.75, 7.0),
                OccupancyPoint(HelsinkiDateTime.of(tomorrow, LocalTime.of(8, 15)), 1.0, 7.0),
                OccupancyPoint(HelsinkiDateTime.of(tomorrow, LocalTime.of(8, 50)), 0.0, 7.0),
                OccupancyPoint(HelsinkiDateTime.of(tomorrow, LocalTime.of(9, 0)), 0.0, 0.0)
            ),
            occupancies
        )
    }
}
