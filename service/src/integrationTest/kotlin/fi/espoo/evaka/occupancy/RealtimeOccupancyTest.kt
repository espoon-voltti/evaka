// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.FixtureBuilder
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
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RealtimeOccupancyTest : FullApplicationTest(resetDbBeforeEach = true) {
    val date = LocalDate.now().minusDays(1)
    val groupId = GroupId(UUID.randomUUID())

    @BeforeEach
    internal fun setUp() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insertTestDaycareGroup(DevDaycareGroup(groupId, testDaycare.id))
        }
    }

    @Test
    fun testOccupancyIsCalculatedFromAttendances() {
        db.transaction { tx ->
            FixtureBuilder(tx, date)
                .addChild()
                .withAge(2, 10)
                .saveAnd {
                    addPlacement().toUnit(testDaycare.id).save()
                    addAttendance().inUnit(testDaycare.id).arriving(LocalTime.of(7, 45)).departing(LocalTime.of(16, 30)).save()
                }
                .addChild()
                .withAge(2, 11)
                .saveAnd {
                    addPlacement().toUnit(testDaycare.id).save()
                    addAssistanceNeed().createdBy(EvakaUserId(testDecisionMaker_1.id.raw)).withFactor(2.0).save()
                    addAttendance().inUnit(testDaycare.id).arriving(LocalTime.of(8, 15)).departing(LocalTime.of(16, 30)).save()
                }
                .addChild()
                .withAge(3)
                .saveAnd {
                    addPlacement().toUnit(testDaycare.id).save()
                    addAttendance().inUnit(testDaycare.id).arriving(LocalTime.of(8, 15)).save()
                }
                .addEmployee()
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withGroupAccess(testDaycare.id, groupId)
                .saveAnd {
                    addRealtimeAttendance().inGroup(groupId).arriving(LocalTime.of(7, 0))
                        .departing(LocalTime.of(16, 45)).withCoefficient(occupancyCoefficientSeven).save()
                }

            tx.markExternalStaffArrival(
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
            tx.markExternalStaffArrival(
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

        val result = getRealtimeOccupancy()
        val occupancies = result.occupancySeries
        assertEquals(9, occupancies.size)

        // 7:00, staff 1 arrives
        occupancies.find { it.time == HelsinkiDateTime.Companion.of(date, LocalTime.of(7, 0)) }
            ?.also { assertEquals(0.0, it.childCapacity) }
            ?.also { assertEquals(7.0, it.staffCapacity) }
            ?.also { assertEquals(0.0, it.occupancyRatio) }
            ?: error("data point missing")

        // 7:45, child 1 arrives
        occupancies.find { it.time == HelsinkiDateTime.Companion.of(date, LocalTime.of(7, 45)) }
            ?.also { assertEquals(child1Capacity, it.childCapacity) }
            ?.also { assertEquals(7.0, it.staffCapacity) }
            ?.also { assertEquals(child1Capacity / (7 * 1), it.occupancyRatio) }
            ?: error("data point missing")

        // 8:15, children 2 and 3 arrive
        occupancies.find { it.time == HelsinkiDateTime.Companion.of(date, LocalTime.of(8, 15)) }
            ?.also { assertEquals(child1Capacity + child2Capacity + child3Capacity, it.childCapacity) }
            ?.also { assertEquals(7.0, it.staffCapacity) }
            ?.also { assertEquals((child1Capacity + child2Capacity + child3Capacity) / (7 * 1), it.occupancyRatio) }
            ?: error("data point missing")

        // 10:00, staff 2 arrives
        occupancies.find { it.time == HelsinkiDateTime.Companion.of(date, LocalTime.of(10, 0)) }
            ?.also { assertEquals(child1Capacity + child2Capacity + child3Capacity, it.childCapacity) }
            ?.also { assertEquals(10.5, it.staffCapacity) }
            ?.also { assertEquals((child1Capacity + child2Capacity + child3Capacity) / 10.5, it.occupancyRatio) }
            ?: error("data point missing")

        // 11:00, staff 3 with zero coefficient arrives, does not affect capacities
        occupancies.find { it.time == HelsinkiDateTime.Companion.of(date, LocalTime.of(11, 0)) }
            ?.also { assertEquals(child1Capacity + child2Capacity + child3Capacity, it.childCapacity) }
            ?.also { assertEquals(10.5, it.staffCapacity) }
            ?.also { assertEquals((child1Capacity + child2Capacity + child3Capacity) / 10.5, it.occupancyRatio) }
            ?: error("data point missing")

        // 15:00, staff 3 with zero coefficient departs, does not affect capacities
        occupancies.find { it.time == HelsinkiDateTime.Companion.of(date, LocalTime.of(15, 0)) }
            ?.also { assertEquals(child1Capacity + child2Capacity + child3Capacity, it.childCapacity) }
            ?.also { assertEquals(10.5, it.staffCapacity) }
            ?.also { assertEquals((child1Capacity + child2Capacity + child3Capacity) / 10.5, it.occupancyRatio) }
            ?: error("data point missing")

        // 16:30, children 1 and 2 depart
        occupancies.find { it.time == HelsinkiDateTime.Companion.of(date, LocalTime.of(16, 30)) }
            ?.also { assertEquals(child3Capacity, it.childCapacity) }
            ?.also { assertEquals(10.5, it.staffCapacity) }
            ?.also { assertEquals(child3Capacity / 10.5, it.occupancyRatio) }
            ?: error("data point missing")

        // 16:45, staff 1 departs
        occupancies.find { it.time == HelsinkiDateTime.Companion.of(date, LocalTime.of(16, 45)) }
            ?.also { assertEquals(child3Capacity, it.childCapacity) }
            ?.also { assertEquals(3.5, it.staffCapacity) }
            ?.also { assertEquals(child3Capacity / 3.5, it.occupancyRatio) }
            ?: error("data point missing")

        // 18:00, staff 2 departs, forgets to mark child 3 departed
        occupancies.find { it.time == HelsinkiDateTime.Companion.of(date, LocalTime.of(18, 0)) }
            ?.also { assertEquals(child3Capacity, it.childCapacity) }
            ?.also { assertEquals(0.0, it.staffCapacity) }
            ?.also { assertNull(it.occupancyRatio) }
            ?: error("data point missing")
    }

    private fun getRealtimeOccupancy(): RealtimeOccupancy {
        val timeRange = HelsinkiDateTimeRange(
            HelsinkiDateTime.of(date, LocalTime.of(0, 0)),
            HelsinkiDateTime.of(date, LocalTime.of(23, 59))
        )
        return db.read { tx ->
            RealtimeOccupancy(
                childAttendances = tx.getChildOccupancyAttendances(testDaycare.id, timeRange),
                staffAttendances = tx.getStaffOccupancyAttendances(testDaycare.id, timeRange)
            )
        }
    }
}

// deserializing JSON to RealtimeOccupancy causes weird bugs with computed properties
private data class RealtimeOccupancyResponse(
    val childAttendances: List<ChildOccupancyAttendance>,
    val staffAttendances: List<StaffOccupancyAttendance>,
    val childCapacitySumSeries: List<ChildCapacityPoint>,
    val staffCapacitySumSeries: List<StaffCapacityPoint>,
    val occupancySeries: List<OccupancyPoint>
)
