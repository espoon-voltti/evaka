// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.occupancy.getStaffOccupancyAttendances
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevStaffAttendance
import fi.espoo.evaka.shared.dev.DevStaffAttendancePlan
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testRoundTheClockDaycare
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RealtimeStaffAttendanceQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private lateinit var employee1: DevEmployee
    private lateinit var employee2: DevEmployee
    private lateinit var employee3: DevEmployee
    private lateinit var employee4: DevEmployee
    private val group1 = DevDaycareGroup(daycareId = testDaycare.id, name = "Koirat")
    private val group2 = DevDaycareGroup(daycareId = testDaycare.id, name = "Kissat")
    private val group3 = DevDaycareGroup(daycareId = testDaycare.id, name = "Tyhjät")
    private val roundTheClockGroup =
        DevDaycareGroup(daycareId = testRoundTheClockDaycare.id, name = "Kookoskalmarit")

    private val today = LocalDate.of(2023, 10, 27)
    private val systemUser = AuthenticatedUser.SystemInternalUser

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testRoundTheClockDaycare)
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(group3)
            tx.insert(roundTheClockGroup)
            employee1 = DevEmployee(firstName = "One", lastName = "in group 1")
            tx.insert(
                employee1,
                mapOf(testDaycare.id to UserRole.STAFF),
                mapOf(testDaycare.id to listOf(group1.id)),
            )
            employee2 = DevEmployee(firstName = "Two", lastName = "in group 2")
            tx.insert(
                employee2,
                mapOf(testDaycare.id to UserRole.STAFF),
                mapOf(testDaycare.id to listOf(group2.id)),
            )
            employee3 = DevEmployee(firstName = "Three", lastName = "in group 1")
            tx.insert(
                employee3,
                mapOf(testDaycare.id to UserRole.SPECIAL_EDUCATION_TEACHER),
                mapOf(testDaycare.id to listOf(group1.id)),
            )
            employee4 = DevEmployee(firstName = "Four", lastName = "in group 2")
            tx.insert(
                employee4,
                mapOf(testDaycare.id to UserRole.STAFF),
                mapOf(testDaycare.id to listOf(group2.id)),
            )
        }
    }

    @Test
    fun realtimeAttendanceQueries() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        db.transaction { tx ->
            tx.markStaffArrival(
                    employee1.id,
                    group1.id,
                    now.minusDays(1),
                    BigDecimal(7.0),
                    now,
                    employee1.evakaUserId,
                )
                .let {
                    tx.markStaffDeparture(
                        it,
                        now.minusDays(1).plusHours(8),
                        now,
                        employee1.evakaUserId,
                    )
                }
            tx.markStaffArrival(
                    employee1.id,
                    group1.id,
                    now,
                    BigDecimal(0),
                    now,
                    employee1.evakaUserId,
                )
                .let { tx.markStaffDeparture(it, now.plusHours(2), now, employee1.evakaUserId) }
            tx.markStaffArrival(
                    employee3.id,
                    group1.id,
                    now.plusHours(1),
                    BigDecimal("0.0"),
                    now,
                    employee3.evakaUserId,
                )
                .let { tx.markStaffDeparture(it, now.plusHours(7), now, employee3.evakaUserId) }
            tx.markStaffArrival(
                    employee2.id,
                    group2.id,
                    now.plusHours(2),
                    BigDecimal(3.5),
                    now,
                    employee2.evakaUserId,
                )
                .let { tx.markStaffDeparture(it, now.plusHours(8), now, employee2.evakaUserId) }
            // Exactly same values as above
            tx.markStaffArrival(
                    employee4.id,
                    group2.id,
                    now.plusHours(2),
                    BigDecimal(3.5),
                    now,
                    employee4.evakaUserId,
                )
                .let { tx.markStaffDeparture(it, now.plusHours(8), now, employee4.evakaUserId) }
        }

        db.read {
            val attendances =
                it.getStaffAttendances(testDaycare.id, now.toLocalDate().toFiniteDateRange(), now)
            assertEquals(4, attendances.size)
            assertEquals(
                listOf("One", "Three", "Four", "Two"),
                attendances.map { a -> a.firstName },
            )
            assertEquals(
                listOf(group1.id, group1.id, group2.id, group2.id),
                attendances.flatMap { a -> a.groupIds },
            )

            val occupancyAttendances =
                it.getStaffOccupancyAttendances(
                    testDaycare.id,
                    HelsinkiDateTimeRange(now.atStartOfDay(), now.atEndOfDay()),
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
                        occupancyCoefficientSeven,
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
                    occupancyCoefficientSeven,
                )
            )
        }
        val externalAttendances = db.read { it.getExternalStaffAttendances(testDaycare.id) }

        assertEquals(1, externalAttendances.size)
        assertEquals("Foo Present", externalAttendances[0].name)
        assertEquals(group1.id, externalAttendances[0].groupId)
        assertEquals(now.minusDays(1), externalAttendances[0].arrived)
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds departures for yesterday's arrivals without a plan`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        db.transaction { tx ->
            val arrivalTime = now.minusDays(1).minusMinutes(1)
            tx.markStaffArrival(
                employee1.id,
                group1.id,
                arrivalTime,
                BigDecimal(7.0),
                now,
                employee1.evakaUserId,
            )
            tx.markStaffArrival(
                employee2.id,
                group1.id,
                now,
                BigDecimal(0),
                now,
                employee1.evakaUserId,
            )

            tx.addMissingStaffAttendanceDepartures(now, systemUser.evakaUserId)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(2, staffAttendances.size)
            assertEquals(
                arrivalTime.plusHours(12),
                staffAttendances.first { it.employeeId == employee1.id }.departed,
            )
            assertEquals(null, staffAttendances.first { it.employeeId == employee2.id }.departed)
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds departure for today's TRAINING arrival with a planned departure in the past`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(17, 0))
        val arrival = now.atStartOfDay().plusHours(8)
        val plannedDeparture = arrival.plusHours(8)
        db.transaction { tx ->
            tx.upsertStaffAttendance(
                attendanceId = null,
                employeeId = employee1.id,
                groupId = group1.id,
                arrivalTime = arrival,
                departureTime = null,
                occupancyCoefficient = BigDecimal(7.0),
                type = StaffAttendanceType.TRAINING,
                modifiedAt = now,
                modifiedBy = employee1.evakaUserId,
            )

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1.id,
                    startTime = arrival,
                    endTime = plannedDeparture,
                )
            )

            tx.addMissingStaffAttendanceDepartures(now, systemUser.evakaUserId)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(
                plannedDeparture,
                staffAttendances.first { it.employeeId == employee1.id }.departed,
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
            tx.upsertStaffAttendance(
                attendanceId = null,
                employeeId = employee1.id,
                groupId = group1.id,
                arrivalTime = arrival,
                departureTime = null,
                occupancyCoefficient = BigDecimal(7.0),
                type = StaffAttendanceType.JUSTIFIED_CHANGE,
                modifiedAt = startOfToday,
                modifiedBy = employee1.evakaUserId,
            )

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1.id,
                    startTime = arrival,
                    endTime = plannedDeparture,
                )
            )

            tx.addMissingStaffAttendanceDepartures(startOfToday, systemUser.evakaUserId)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(
                expectedAddedDepartureTime,
                staffAttendances.first { it.employeeId == employee1.id }.departed,
            )
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds a departure after 12h for overnight attendance when type is  PRESENT`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(17, 0))
        val arrival = now.atStartOfDay().minusHours(4)
        val plannedDeparture = arrival.plusHours(8)
        db.transaction { tx ->
            tx.upsertStaffAttendance(
                attendanceId = null,
                employeeId = employee1.id,
                groupId = group1.id,
                arrivalTime = arrival,
                departureTime = null,
                occupancyCoefficient = BigDecimal(7.0),
                type = StaffAttendanceType.PRESENT,
                modifiedAt = now,
                modifiedBy = employee1.evakaUserId,
            )

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1.id,
                    startTime = arrival,
                    endTime = plannedDeparture,
                )
            )

            tx.addMissingStaffAttendanceDepartures(now, systemUser.evakaUserId)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(
                arrival.plusHours(12),
                staffAttendances.first { it.employeeId == employee1.id }.departed,
            )
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds a departure at planned time for overnight attendance when type is OTHER_WORK`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(17, 0))
        val arrival = now.atStartOfDay().minusHours(4)
        val plannedDeparture = arrival.plusHours(8)
        db.transaction { tx ->
            tx.upsertStaffAttendance(
                attendanceId = null,
                employeeId = employee1.id,
                groupId = group1.id,
                arrivalTime = arrival,
                departureTime = null,
                occupancyCoefficient = BigDecimal(7.0),
                type = StaffAttendanceType.OTHER_WORK,
                modifiedAt = now,
                modifiedBy = employee1.evakaUserId,
            )

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1.id,
                    startTime = arrival,
                    endTime = plannedDeparture,
                )
            )

            tx.addMissingStaffAttendanceDepartures(now, systemUser.evakaUserId)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(
                plannedDeparture,
                staffAttendances.first { it.employeeId == employee1.id }.departed,
            )
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture won't add a departure for today's arrival with a planned departure in the future`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(17, 0))
        val arrival = now.atStartOfDay().plusHours(8)
        val plannedDeparture = arrival.plusHours(10)
        db.transaction { tx ->
            tx.markStaffArrival(
                employee1.id,
                group1.id,
                arrival,
                BigDecimal(7.0),
                now,
                employee1.evakaUserId,
            )

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1.id,
                    startTime = arrival,
                    endTime = plannedDeparture,
                )
            )

            tx.addMissingStaffAttendanceDepartures(now, systemUser.evakaUserId)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(null, staffAttendances.first { it.employeeId == employee1.id }.departed)
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture won't add a departure when attendance is to a round the clock unit and attendance is less than 12h`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        db.transaction { tx ->
            tx.markStaffArrival(
                employee1.id,
                roundTheClockGroup.id,
                now.minusHours(11),
                BigDecimal(7.0),
                now,
                employee1.evakaUserId,
            )

            tx.addMissingStaffAttendanceDepartures(now, systemUser.evakaUserId)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(null, staffAttendances.first { it.employeeId == employee1.id }.departed)
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds a departure when attendance is to a round the clock unit and attendance is more than 12h`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val arrival = now.minusHours(12 + 1)

        db.transaction { tx ->
            tx.markStaffArrival(
                employee1.id,
                roundTheClockGroup.id,
                arrival,
                BigDecimal(7.0),
                now,
                employee1.evakaUserId,
            )

            tx.addMissingStaffAttendanceDepartures(now, systemUser.evakaUserId)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(
                arrival.plusHours(12),
                staffAttendances.first { it.employeeId == employee1.id }.departed,
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
            tx.markStaffArrival(
                employee1.id,
                group1.id,
                plannedDeparture,
                BigDecimal(7.0),
                now,
                employee1.evakaUserId,
            )

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1.id,
                    startTime = plannedArrival,
                    endTime = plannedDeparture,
                )
            )

            tx.addMissingStaffAttendanceDepartures(now, systemUser.evakaUserId)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(null, staffAttendances.first { it.employeeId == employee1.id }.departed)
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture won't add a departure when attendance is to a normal unit and attendance is less than 12h`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        db.transaction { tx ->
            tx.markStaffArrival(
                employee1.id,
                group1.id,
                now.minusHours(11),
                BigDecimal(7.0),
                now,
                employee1.evakaUserId,
            )

            tx.addMissingStaffAttendanceDepartures(now, systemUser.evakaUserId)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(null, staffAttendances.first { it.employeeId == employee1.id }.departed)
        }
    }

    @Test
    fun `addMissingStaffAttendanceDeparture adds a departure when attendance is to a normal unit and attendance is more than 12h`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val arrival = now.minusHours(12 + 1)

        db.transaction { tx ->
            tx.markStaffArrival(
                employee1.id,
                group1.id,
                arrival,
                BigDecimal(7.0),
                now,
                employee1.evakaUserId,
            )

            tx.addMissingStaffAttendanceDepartures(now, systemUser.evakaUserId)

            val staffAttendances = tx.getRealtimeStaffAttendances()
            assertEquals(1, staffAttendances.size)
            assertEquals(
                arrival.plusHours(12),
                staffAttendances.first { it.employeeId == employee1.id }.departed,
            )
            assertTrue(staffAttendances.first().departedAutomatically)
        }
    }

    @Test
    fun `upsertStaffAttendance sets arrival added on insert`() {
        val (unitId, groupId, user) = initUpsertStaffAttendanceTestData()
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 12, 20), LocalTime.of(8, 0)))
        val arrivalTime = clock.now()
        val arrivalAddedAt = arrivalTime.plusMinutes(2)

        val attendance =
            db.transaction { tx ->
                tx.upsertStaffAttendance(
                    attendanceId = null,
                    employeeId = user.id,
                    groupId = groupId,
                    arrivalTime = arrivalTime,
                    departureTime = null,
                    occupancyCoefficient = BigDecimal.ZERO,
                    type = StaffAttendanceType.PRESENT,
                    departedAutomatically = false,
                    modifiedAt = arrivalAddedAt,
                    modifiedBy = user.evakaUserId,
                )
            }

        assertThat(
                db.read { tx ->
                    tx.getStaffAttendancesForDateRange(
                        unitId,
                        clock.today().let { FiniteDateRange(it, it) },
                    )
                }
            )
            .extracting(
                { it.id },
                { it.arrivedAddedAt },
                { it.arrivedModifiedAt },
                { it.departedAddedAt },
                { it.departedModifiedAt },
            )
            .containsExactly(Tuple(attendance!!.new.id, arrivalAddedAt, arrivalAddedAt, null, null))
    }

    @Test
    fun `upsertStaffAttendance sets departure added on insert`() {
        val (unitId, groupId, user) = initUpsertStaffAttendanceTestData()
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 12, 20), LocalTime.of(8, 0)))
        val arrivalTime = clock.now()
        val departureTime = arrivalTime.plusHours(7)
        val addedAt = departureTime.plusMinutes(2)

        val attendance =
            db.transaction { tx ->
                tx.upsertStaffAttendance(
                    attendanceId = null,
                    employeeId = user.id,
                    groupId = groupId,
                    arrivalTime = arrivalTime,
                    departureTime = departureTime,
                    occupancyCoefficient = BigDecimal.ZERO,
                    type = StaffAttendanceType.PRESENT,
                    departedAutomatically = false,
                    modifiedAt = addedAt,
                    modifiedBy = user.evakaUserId,
                )
            }

        assertThat(
                db.read { tx ->
                    tx.getStaffAttendancesForDateRange(
                        unitId,
                        clock.today().let { FiniteDateRange(it, it) },
                    )
                }
            )
            .extracting(
                { it.id },
                { it.arrivedAddedAt },
                { it.arrivedModifiedAt },
                { it.departedAddedAt },
                { it.departedModifiedAt },
            )
            .containsExactly(Tuple(attendance!!.new.id, addedAt, addedAt, addedAt, addedAt))
    }

    @Test
    fun `upsertStaffAttendance sets arrival modified on update`() {
        val (unitId, groupId, user) = initUpsertStaffAttendanceTestData()
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 12, 20), LocalTime.of(8, 0)))
        val arrivalTime = clock.now()
        val arrivalAddedAt = arrivalTime.plusMinutes(2)
        val attendanceId =
            db.transaction { tx ->
                tx.insert(
                    DevStaffAttendance(
                        employeeId = user.id,
                        groupId = groupId,
                        arrived = arrivalTime,
                        departed = null,
                        modifiedAt = arrivalAddedAt,
                        modifiedBy = user.evakaUserId,
                    )
                )
            }

        val arrivalModifiedAt = arrivalTime.plusHours(1)
        db.transaction { tx ->
            tx.upsertStaffAttendance(
                attendanceId = attendanceId,
                employeeId = user.id,
                groupId = groupId,
                arrivalTime = arrivalModifiedAt,
                departureTime = null,
                occupancyCoefficient = BigDecimal.ZERO,
                type = StaffAttendanceType.PRESENT,
                departedAutomatically = false,
                modifiedAt = arrivalModifiedAt,
                modifiedBy = user.evakaUserId,
            )
        }

        assertThat(
                db.read { tx ->
                    tx.getStaffAttendancesForDateRange(
                        unitId,
                        clock.today().let { FiniteDateRange(it, it) },
                    )
                }
            )
            .extracting(
                { it.id },
                { it.arrivedAddedAt },
                { it.arrivedModifiedAt },
                { it.departedAddedAt },
                { it.departedModifiedAt },
            )
            .containsExactly(Tuple(attendanceId, arrivalAddedAt, arrivalModifiedAt, null, null))
    }

    @Test
    fun `upsertStaffAttendance sets departure modified on update`() {
        val (unitId, groupId, user) = initUpsertStaffAttendanceTestData()
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 12, 20), LocalTime.of(8, 0)))
        val arrivalTime = clock.now()
        val arrivalAddedAt = arrivalTime.plusMinutes(2)
        val attendanceId =
            db.transaction { tx ->
                tx.insert(
                    DevStaffAttendance(
                        employeeId = user.id,
                        groupId = groupId,
                        arrived = arrivalTime,
                        departed = null,
                        modifiedAt = arrivalAddedAt,
                        modifiedBy = user.evakaUserId,
                    )
                )
            }

        val departureTime = arrivalTime.plusHours(7)
        val departureAddedAt = departureTime.plusMinutes(2)
        db.transaction { tx ->
            tx.upsertStaffAttendance(
                attendanceId = attendanceId,
                employeeId = user.id,
                groupId = groupId,
                arrivalTime = arrivalTime,
                departureTime = departureTime,
                occupancyCoefficient = BigDecimal.ZERO,
                type = StaffAttendanceType.PRESENT,
                departedAutomatically = false,
                modifiedAt = departureAddedAt,
                modifiedBy = user.evakaUserId,
            )
        }

        assertThat(
                db.read { tx ->
                    tx.getStaffAttendancesForDateRange(
                        unitId,
                        clock.today().let { FiniteDateRange(it, it) },
                    )
                }
            )
            .extracting(
                { it.id },
                { it.arrivedAddedAt },
                { it.arrivedModifiedAt },
                { it.departedAddedAt },
                { it.departedModifiedAt },
            )
            .containsExactly(
                Tuple(
                    attendanceId,
                    arrivalAddedAt,
                    arrivalAddedAt,
                    departureAddedAt,
                    departureAddedAt,
                )
            )
    }

    @Test
    fun `upsertStaffAttendance sets departure modified on remove`() {
        val (unitId, groupId, user) = initUpsertStaffAttendanceTestData()
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 12, 20), LocalTime.of(8, 0)))
        val arrival = clock.now()
        val departure = arrival.plusHours(7)
        val addedAt = arrival.plusMinutes(2)
        val attendanceId =
            db.transaction { tx ->
                tx.insert(
                    DevStaffAttendance(
                        employeeId = user.id,
                        groupId = groupId,
                        arrived = arrival,
                        departed = departure,
                        modifiedAt = addedAt,
                        modifiedBy = user.evakaUserId,
                    )
                )
            }

        val departureModifiedAt = departure.plusMinutes(2)
        db.transaction { tx ->
            tx.upsertStaffAttendance(
                attendanceId = attendanceId,
                employeeId = user.id,
                groupId = groupId,
                arrivalTime = arrival,
                departureTime = null,
                occupancyCoefficient = BigDecimal.ZERO,
                type = StaffAttendanceType.PRESENT,
                departedAutomatically = false,
                modifiedAt = departureModifiedAt,
                modifiedBy = user.evakaUserId,
            )
        }

        assertThat(
                db.read { tx ->
                    tx.getStaffAttendancesForDateRange(
                        unitId,
                        clock.today().let { FiniteDateRange(it, it) },
                    )
                }
            )
            .extracting(
                { it.id },
                { it.arrivedAddedAt },
                { it.arrivedModifiedAt },
                { it.departedAddedAt },
                { it.departedModifiedAt },
            )
            .containsExactly(Tuple(attendanceId, addedAt, addedAt, addedAt, departureModifiedAt))
    }

    @Test
    fun `upsertStaffAttendance doesn't set added for old data`() {
        val (unitId, groupId, user) = initUpsertStaffAttendanceTestData()
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 12, 20), LocalTime.of(8, 0)))
        val arrival = clock.now()
        val attendanceId =
            db.transaction { tx ->
                tx.insert(
                    DevStaffAttendance(
                        employeeId = user.id,
                        groupId = groupId,
                        arrived = arrival,
                        departed = null,
                        modifiedAt = null,
                        modifiedBy = null,
                    )
                )
            }

        val departure = arrival.plusHours(7)
        val departureModifiedAt = departure.plusMinutes(2)
        db.transaction { tx ->
            tx.upsertStaffAttendance(
                attendanceId = attendanceId,
                employeeId = user.id,
                groupId = groupId,
                arrivalTime = arrival,
                departureTime = departure,
                occupancyCoefficient = BigDecimal.ZERO,
                type = StaffAttendanceType.PRESENT,
                departedAutomatically = false,
                modifiedAt = departureModifiedAt,
                modifiedBy = user.evakaUserId,
            )
        }

        assertThat(
                db.read { tx ->
                    tx.getStaffAttendancesForDateRange(
                        unitId,
                        clock.today().let { FiniteDateRange(it, it) },
                    )
                }
            )
            .extracting(
                { it.id },
                { it.arrivedAddedAt },
                { it.arrivedModifiedAt },
                { it.departedAddedAt },
                { it.departedModifiedAt },
            )
            .containsExactly(Tuple(attendanceId, null, null, null, departureModifiedAt))
    }

    @Test
    fun `upsertStaffAttendance update works with unmodified data`() {
        val (unitId, groupId, user) = initUpsertStaffAttendanceTestData()
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 12, 20), LocalTime.of(8, 0)))
        val arrivalTime = clock.now()
        val arrivalAddedAt = arrivalTime.plusMinutes(2)
        val attendance =
            DevStaffAttendance(
                employeeId = user.id,
                groupId = groupId,
                arrived = arrivalTime,
                departed = null,
                modifiedAt = arrivalAddedAt,
                modifiedBy = user.evakaUserId,
            )
        val attendanceId = db.transaction { tx -> tx.insert(attendance) }

        db.transaction { tx ->
            tx.upsertStaffAttendance(
                attendanceId = attendanceId,
                employeeId = attendance.employeeId,
                groupId = attendance.groupId,
                arrivalTime = attendance.arrived,
                departureTime = attendance.departed,
                occupancyCoefficient = attendance.occupancyCoefficient,
                type = attendance.type,
                departedAutomatically = attendance.departedAutomatically,
                modifiedAt = arrivalAddedAt,
                modifiedBy = user.evakaUserId,
            )
        }

        assertThat(
                db.read { tx ->
                    tx.getStaffAttendancesForDateRange(
                        unitId,
                        clock.today().let { FiniteDateRange(it, it) },
                    )
                }
            )
            .extracting(
                { it.id },
                { it.arrivedAddedAt },
                { it.arrivedModifiedAt },
                { it.departedAddedAt },
                { it.departedModifiedAt },
            )
            .containsExactly(Tuple(attendanceId, arrivalAddedAt, arrivalAddedAt, null, null))
    }

    private fun initUpsertStaffAttendanceTestData():
        Triple<DaycareId, GroupId, AuthenticatedUser.Employee> {
        val (unitId, groupId) =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea().copy(shortName = "better test area"))
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val groupId = tx.insert(DevDaycareGroup(daycareId = unitId))
                unitId to groupId
            }
        val user =
            db.transaction { tx ->
                val employeeId =
                    tx.insert(DevEmployee(), unitRoles = mapOf(unitId to UserRole.STAFF))
                AuthenticatedUser.Employee(employeeId, roles = emptySet())
            }
        return Triple(unitId, groupId, user)
    }
}
