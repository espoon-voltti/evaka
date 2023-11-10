// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.FixtureBuilder
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attendance.ExternalStaffArrival
import fi.espoo.evaka.attendance.ExternalStaffDeparture
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.attendance.markExternalStaffArrival
import fi.espoo.evaka.attendance.markExternalStaffDeparture
import fi.espoo.evaka.attendance.occupancyCoefficientSeven
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevDaycareGroup
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
            FixtureBuilder(tx, date)
                .addChild()
                .withAge(2, 10)
                .saveAnd {
                    addPlacement().toUnit(testDaycare.id).save()
                    addAttendance()
                        .inUnit(testDaycare.id)
                        .arriving(LocalTime.of(7, 45))
                        .departing(LocalTime.of(16, 30))
                        .save()
                }
                .addChild()
                .withAge(2, 11)
                .saveAnd {
                    addPlacement().toUnit(testDaycare.id).save()
                    tx.insert(
                        DevAssistanceFactor(
                            childId = childId,
                            validDuring = date.toFiniteDateRange(),
                            capacityFactor = 2.0,
                        )
                    )
                    addAttendance()
                        .inUnit(testDaycare.id)
                        .arriving(LocalTime.of(8, 15))
                        .departing(LocalTime.of(16, 30))
                        .save()
                }
                .addChild()
                .withAge(3)
                .saveAnd {
                    addPlacement().toUnit(testDaycare.id).save()
                    addAttendance().inUnit(testDaycare.id).arriving(LocalTime.of(8, 15)).save()
                }
                .addChild()
                .withAge(2, 11)
                .saveAnd {
                    addPlacement().toUnit(testDaycare.id).saveAnd {
                        addServiceNeed()
                            .withOption(snDaycareContractDays10)
                            .createdBy(EvakaUserId(testDecisionMaker_1.id.raw))
                            .save()
                    }
                    addAttendance()
                        .inUnit(testDaycare.id)
                        .arriving(LocalTime.of(8, 30))
                        .departing(LocalTime.of(16, 30))
                        .save()
                }
                .addEmployee()
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withGroupAccess(testDaycare.id, groupId)
                .saveAnd {
                    addRealtimeAttendance()
                        .inGroup(groupId)
                        .arriving(LocalTime.of(7, 0))
                        .departing(LocalTime.of(16, 45))
                        .withCoefficient(occupancyCoefficientSeven)
                        .withType(StaffAttendanceType.PRESENT)
                        .save()
                }

            tx.markExternalStaffArrival(
                    ExternalStaffArrival(
                        "Matti",
                        groupId,
                        HelsinkiDateTime.of(date, LocalTime.of(10, 0)),
                        BigDecimal("3.5")
                    )
                )
                .let { extAttendanceId ->
                    tx.markExternalStaffDeparture(
                        ExternalStaffDeparture(
                            extAttendanceId,
                            HelsinkiDateTime.of(date, LocalTime.of(18, 0))
                        )
                    )
                }

            // staff with coefficient of zero does not affect occupancy rates
            tx.markExternalStaffArrival(
                    ExternalStaffArrival(
                        "Nolla Sijainen",
                        groupId,
                        HelsinkiDateTime.of(date, LocalTime.of(11, 0)),
                        BigDecimal.ZERO
                    )
                )
                .let { extAttendanceId ->
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
            }
            ?.also { assertEquals(7.0, it.staffCapacity) }
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
            }
            ?.also { assertEquals(7.0, it.staffCapacity) }
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
            }
            ?.also { assertEquals(10.5, it.staffCapacity) }
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
            }
            ?.also { assertEquals(10.5, it.staffCapacity) }
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
            }
            ?.also { assertEquals(10.5, it.staffCapacity) }
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
            FixtureBuilder(tx, date).addChild().withAge(2, 10).saveAnd {
                // addPlacement().toUnit(testDaycare.id).save()
                addAttendance()
                    .inUnit(testDaycare.id)
                    .arriving(LocalTime.of(7, 45))
                    .departing(LocalTime.of(16, 30))
                    .save()
            }
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
    ): RealtimeOccupancy {
        return db.read { tx ->
            RealtimeOccupancy(
                childAttendances = tx.getChildOccupancyAttendances(testDaycare.id, timeRange),
                staffAttendances = tx.getStaffOccupancyAttendances(testDaycare.id, timeRange)
            )
        }
    }

    @Test
    fun `graph data shows no gaps for overnight attendances`() {
        val tomorrow = date.plusDays(1)
        db.transaction { tx ->
            FixtureBuilder(tx, date)
                .addChild()
                .withAge(2, 10)
                .saveAnd {
                    addPlacement()
                        .toUnit(testDaycare.id)
                        .fromDay(date)
                        .toDay(date.plusMonths(2))
                        .save()
                    addAttendance()
                        .inUnit(testDaycare.id)
                        .arriving(LocalTime.of(20, 45))
                        .departing(LocalTime.of(23, 59))
                        .save()
                    addAttendance()
                        .inUnit(testDaycare.id)
                        .arriving(tomorrow, LocalTime.of(0, 0))
                        .departing(tomorrow, LocalTime.of(8, 15))
                        .save()
                }
                .addChild()
                .withAge(4, 3)
                .saveAnd {
                    addPlacement()
                        .toUnit(testDaycare.id)
                        .fromDay(date)
                        .toDay(date.plusMonths(2))
                        .save()
                    addAttendance()
                        .inUnit(testDaycare.id)
                        .arriving(LocalTime.of(21, 5))
                        .departing(LocalTime.of(23, 59))
                        .save()
                    addAttendance()
                        .inUnit(testDaycare.id)
                        .arriving(tomorrow, LocalTime.of(0, 0))
                        .departing(tomorrow, LocalTime.of(8, 50))
                        .save()
                }
                .addEmployee()
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withGroupAccess(testDaycare.id, groupId)
                .saveAnd {
                    addRealtimeAttendance()
                        .inGroup(groupId)
                        .arriving(LocalTime.of(19, 45))
                        .departing(tomorrow, LocalTime.of(9, 0))
                        .withCoefficient(occupancyCoefficientSeven)
                        .withType(StaffAttendanceType.PRESENT)
                        .save()
                }
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
