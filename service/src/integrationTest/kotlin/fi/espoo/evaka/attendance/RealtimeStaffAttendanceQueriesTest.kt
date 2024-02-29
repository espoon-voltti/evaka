// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.occupancy.getStaffOccupancyAttendances
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevStaffAttendancePlan
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testRoundTheClockDaycare
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RealtimeStaffAttendanceQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val employee1Id: EmployeeId = EmployeeId(UUID.randomUUID())
    private val employee2Id: EmployeeId = EmployeeId(UUID.randomUUID())
    private val employee3Id: EmployeeId = EmployeeId(UUID.randomUUID())
    private val employee4Id: EmployeeId = EmployeeId(UUID.randomUUID())
    private val group1 = DevDaycareGroup(daycareId = testDaycare.id, name = "Koirat")
    private val group2 = DevDaycareGroup(daycareId = testDaycare.id, name = "Kissat")
    private val group3 = DevDaycareGroup(daycareId = testDaycare.id, name = "Tyhjät")
    private val roundTheClockGroup =
        DevDaycareGroup(daycareId = testRoundTheClockDaycare.id, name = "Kookoskalmarit")

    private val today = LocalDate.of(2023, 10, 27)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(group3)
            tx.insert(roundTheClockGroup)
            tx.insert(
                DevEmployee(id = employee1Id, firstName = "One", lastName = "in group 1"),
                mapOf(testDaycare.id to UserRole.STAFF),
                mapOf(testDaycare.id to listOf(group1.id))
            )
            tx.insert(
                DevEmployee(id = employee2Id, firstName = "Two", lastName = "in group 2"),
                mapOf(testDaycare.id to UserRole.STAFF),
                mapOf(testDaycare.id to listOf(group2.id))
            )
            tx.insert(
                DevEmployee(id = employee3Id, firstName = "Three", lastName = "in group 1"),
                mapOf(testDaycare.id to UserRole.SPECIAL_EDUCATION_TEACHER),
                mapOf(testDaycare.id to listOf(group1.id))
            )
            tx.insert(
                DevEmployee(id = employee4Id, firstName = "Four", lastName = "in group 2"),
                mapOf(testDaycare.id to UserRole.STAFF),
                mapOf(testDaycare.id to listOf(group2.id))
            )
        }
    }

    @Test
    fun realtimeAttendanceQueries() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        db.transaction { tx ->
            tx.markStaffArrival(employee1Id, group1.id, now.minusDays(1), BigDecimal(7.0)).let {
                tx.markStaffDeparture(it, now.minusDays(1).plusHours(8))
            }
            tx.markStaffArrival(employee1Id, group1.id, now, BigDecimal(0)).let {
                tx.markStaffDeparture(it, now.plusHours(2))
            }
            tx.markStaffArrival(employee3Id, group1.id, now.plusHours(1), BigDecimal("0.0")).let {
                tx.markStaffDeparture(it, now.plusHours(7))
            }
            tx.markStaffArrival(employee2Id, group2.id, now.plusHours(2), BigDecimal(3.5)).let {
                tx.markStaffDeparture(it, now.plusHours(8))
            }
            // Exactly same values as above
            tx.markStaffArrival(employee4Id, group2.id, now.plusHours(2), BigDecimal(3.5)).let {
                tx.markStaffDeparture(it, now.plusHours(8))
            }
        }

        db.read {
            val attendances = it.getStaffAttendances(testDaycare.id, now)
            assertEquals(4, attendances.size)
            assertEquals(
                listOf("One", "Three", "Four", "Two"),
                attendances.map { a -> a.firstName }
            )
            assertEquals(
                listOf(group1.id, group1.id, group2.id, group2.id),
                attendances.flatMap { a -> a.groupIds }
            )

            val occupancyAttendances =
                it.getStaffOccupancyAttendances(
                    testDaycare.id,
                    HelsinkiDateTimeRange(now.atStartOfDay(), now.atEndOfDay())
                )
            assertEquals(listOf(0.0, 0.0, 3.5, 3.5), occupancyAttendances.map { a -> a.capacity })
        }
    }

    @Test
    fun externalAttendanceQueries() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        db.transaction { tx ->
            tx.markExternalStaffArrival(
                    ExternalStaffArrival(
                        "Foo Absent",
                        group1.id,
                        now.minusDays(1),
                        occupancyCoefficientSeven
                    )
                )
                .let {
                    tx.markExternalStaffDeparture(
                        ExternalStaffDeparture(it, now.minusDays(1).plusHours(8))
                    )
                }
            tx.markExternalStaffArrival(
                ExternalStaffArrival(
                    "Foo Present",
                    group1.id,
                    now.minusDays(1),
                    occupancyCoefficientSeven
                )
            )
        }
        val externalAttendances = db.read { it.getExternalStaffAttendances(testDaycare.id, now) }

        assertEquals(1, externalAttendances.size)
        assertEquals("Foo Present", externalAttendances[0].name)
        assertEquals(group1.id, externalAttendances[0].groupId)
        assertEquals(now.minusDays(1), externalAttendances[0].arrived)
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds departures for yesterday's arrivals without a plan`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        db.transaction { tx ->
            tx.markStaffArrival(
                employee1Id,
                group1.id,
                now.minusDays(1).minusMinutes(1),
                BigDecimal(7.0)
            )
            tx.markStaffArrival(employee2Id, group1.id, now, BigDecimal(0))

            tx.addMissingStaffAttendanceDepartures(now)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(2, staffAttendances.size)
            assertEquals(
                now.minusDays(1).withTime(LocalTime.of(20, 0)),
                staffAttendances.first { it.employeeId == employee1Id }.departed
            )
            assertEquals(null, staffAttendances.first { it.employeeId == employee2Id }.departed)
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds departures for yesterday's arrivals without a plan also when arrival is after the cutoff at 8 PM`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val arrival = HelsinkiDateTime.of(today.minusDays(1), LocalTime.of(20, 30))
        db.transaction { tx ->
            tx.markStaffArrival(employee1Id, group1.id, arrival, BigDecimal(7.0))
            tx.markStaffArrival(employee2Id, group1.id, now, BigDecimal(0))

            tx.addMissingStaffAttendanceDepartures(now)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(2, staffAttendances.size)
            assertEquals(
                now.withTime(LocalTime.of(0, 0)),
                staffAttendances.first { it.employeeId == employee1Id }.departed
            )
            assertEquals(null, staffAttendances.first { it.employeeId == employee2Id }.departed)
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds departure for today's arrival with a planned departure in the past`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(17, 0))
        val arrival = now.atStartOfDay().plusHours(8)
        val plannedDeparture = arrival.plusHours(8)
        db.transaction { tx ->
            tx.markStaffArrival(employee1Id, group1.id, arrival, BigDecimal(7.0))

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1Id,
                    startTime = arrival,
                    endTime = plannedDeparture,
                )
            )

            tx.addMissingStaffAttendanceDepartures(now)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(
                plannedDeparture,
                staffAttendances.first { it.employeeId == employee1Id }.departed
            )
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds departure at 2000 for JUSTIFIED_CHANGE arrival with a planned departure in the past`() {
        val startOfToday = HelsinkiDateTime.of(today, LocalTime.of(0, 0))
        val arrival = startOfToday.minusDays(1).plusHours(8) // Yesterday 0800
        val plannedDeparture = arrival.plusHours(8)
        val expectedAddedDepartureTime = startOfToday.minusDays(1).plusHours(20)

        db.transaction { tx ->
            val id = tx.markStaffArrival(employee1Id, group1.id, arrival, BigDecimal(7.0))

            @Suppress("DEPRECATION")
            tx.createUpdate(
                    "UPDATE staff_attendance_realtime a SET type = 'JUSTIFIED_CHANGE' WHERE id = :id"
                )
                .bind("id", id)
                .execute()

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1Id,
                    startTime = arrival,
                    endTime = plannedDeparture,
                )
            )

            tx.addMissingStaffAttendanceDepartures(startOfToday)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(
                expectedAddedDepartureTime,
                staffAttendances.first { it.employeeId == employee1Id }.departed
            )
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds a departure for overnight planned attendance`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(17, 0))
        val arrival = now.atStartOfDay().minusHours(4)
        val plannedDeparture = arrival.plusHours(8)
        db.transaction { tx ->
            tx.markStaffArrival(employee1Id, group1.id, arrival, BigDecimal(7.0))

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1Id,
                    startTime = arrival,
                    endTime = plannedDeparture,
                )
            )

            tx.addMissingStaffAttendanceDepartures(now)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(
                plannedDeparture,
                staffAttendances.first { it.employeeId == employee1Id }.departed
            )
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture won't add a departure for today's arrival with a planned departure in the future`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(17, 0))
        val arrival = now.atStartOfDay().plusHours(8)
        val plannedDeparture = arrival.plusHours(10)
        db.transaction { tx ->
            tx.markStaffArrival(employee1Id, group1.id, arrival, BigDecimal(7.0))

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1Id,
                    startTime = arrival,
                    endTime = plannedDeparture,
                )
            )

            tx.addMissingStaffAttendanceDepartures(now)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(null, staffAttendances.first { it.employeeId == employee1Id }.departed)
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture won't add a departure when attendance is to a round the clock unit and attendance is less than 12h`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        db.transaction { tx ->
            tx.markStaffArrival(
                employee1Id,
                roundTheClockGroup.id,
                now.minusHours(11),
                BigDecimal(7.0)
            )

            tx.addMissingStaffAttendanceDepartures(now)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(null, staffAttendances.first { it.employeeId == employee1Id }.departed)
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds a departure when attendance is to a round the clock unit and attendance is more than 12h`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val arrival = now.minusHours(12 + 1)

        db.transaction { tx ->
            tx.markStaffArrival(employee1Id, roundTheClockGroup.id, arrival, BigDecimal(7.0))

            tx.addMissingStaffAttendanceDepartures(now)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(
                arrival.plusHours(12),
                staffAttendances.first { it.employeeId == employee1Id }.departed
            )
            assertTrue(staffAttendances.first().departedAutomatically)
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture won't add a departure from a plan when arrival is plan's departure`() {
        val now = HelsinkiDateTime.of(LocalDate.of(2023, 3, 11), LocalTime.of(0, 0))
        val plannedArrival = now.atStartOfDay().minusHours(16)
        val plannedDeparture = plannedArrival.plusHours(8)
        db.transaction { tx ->
            tx.markStaffArrival(employee1Id, group1.id, plannedDeparture, BigDecimal(7.0))

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1Id,
                    startTime = plannedArrival,
                    endTime = plannedDeparture,
                )
            )

            tx.addMissingStaffAttendanceDepartures(now)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(
                now.minusDays(1).withTime(LocalTime.of(20, 0)),
                staffAttendances.first { it.employeeId == employee1Id }.departed
            )
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture won't add a departure when attendance is to a normal unit and attendance is less than 12h`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        db.transaction { tx ->
            tx.markStaffArrival(
                employee1Id,
                group1.id,
                now.minusHours(11),
                BigDecimal(7.0)
            )

            tx.addMissingStaffAttendanceDepartures(now)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(null, staffAttendances.first { it.employeeId == employee1Id }.departed)
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds a departure when attendance is to a normal unit and attendance is more than 12h`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val arrival = now.minusHours(12 + 1)

        db.transaction { tx ->
            tx.markStaffArrival(employee1Id, group1.id, arrival, BigDecimal(7.0))

            tx.addMissingStaffAttendanceDepartures(now)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(
                arrival.plusHours(12),
                staffAttendances.first { it.employeeId == employee1Id }.departed
            )
            assertTrue(staffAttendances.first().departedAutomatically)
        }
    }
}
