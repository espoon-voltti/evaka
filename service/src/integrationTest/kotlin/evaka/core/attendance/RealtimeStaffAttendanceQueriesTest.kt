// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.attendance

import evaka.core.PureJdbiTest
import evaka.core.occupancy.getStaffOccupancyAttendances
import evaka.core.shared.DaycareId
import evaka.core.shared.GroupId
import evaka.core.shared.StaffAttendanceExternalId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevStaffAttendance
import evaka.core.shared.dev.DevStaffAttendancePlan
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.HelsinkiDateTimeRange
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.TimeRange
import evaka.core.shared.domain.toFiniteDateRange
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
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val roundTheClockDaycare =
        DevDaycare(
            areaId = area.id,
            name = "Round the Clock Daycare",
            shiftCareOperationTimes =
                List(7) { TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)) },
            shiftCareOpenOnHolidays = true,
        )
    private val employee1 = DevEmployee(firstName = "One", lastName = "in group 1")
    private val employee2 = DevEmployee(firstName = "Two", lastName = "in group 2")
    private val employee3 = DevEmployee(firstName = "Three", lastName = "in group 1")
    private val employee4 = DevEmployee(firstName = "Four", lastName = "in group 2")
    private val group1 = DevDaycareGroup(daycareId = daycare.id, name = "Koirat")
    private val group2 = DevDaycareGroup(daycareId = daycare.id, name = "Kissat")
    private val group3 = DevDaycareGroup(daycareId = daycare.id, name = "Tyhjät")
    private val roundTheClockGroup =
        DevDaycareGroup(daycareId = roundTheClockDaycare.id, name = "Kookoskalmarit")

    private val today = LocalDate.of(2023, 10, 27)
    private val systemUser = AuthenticatedUser.SystemInternalUser

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(roundTheClockDaycare)
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(group3)
            tx.insert(roundTheClockGroup)
            tx.insert(
                employee1,
                mapOf(daycare.id to UserRole.STAFF),
                mapOf(daycare.id to listOf(group1.id)),
            )
            tx.insert(
                employee2,
                mapOf(daycare.id to UserRole.STAFF),
                mapOf(daycare.id to listOf(group2.id)),
            )
            tx.insert(
                employee3,
                mapOf(daycare.id to UserRole.SPECIAL_EDUCATION_TEACHER),
                mapOf(daycare.id to listOf(group1.id)),
            )
            tx.insert(
                employee4,
                mapOf(daycare.id to UserRole.STAFF),
                mapOf(daycare.id to listOf(group2.id)),
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
                it.getStaffAttendances(daycare.id, now.toLocalDate().toFiniteDateRange(), now)
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
                    daycare.id,
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
                        now.minusDays(1),
                        employee1.evakaUserId,
                    ),
                )
                .let {
                    tx.markExternalStaffDeparture(
                        ExternalStaffDeparture(
                            it,
                            now.minusDays(1).plusHours(8),
                            now.minusDays(1).plusHours(8),
                            employee1.evakaUserId,
                        ),
                    )
                }
            tx.markExternalStaffArrival(
                ExternalStaffArrival(
                    "Foo Present",
                    group1.id,
                    now.minusDays(1),
                    occupancyCoefficientSeven,
                    now.minusDays(1),
                    employee1.evakaUserId,
                ),
            )
        }
        val externalAttendances = db.read { it.getExternalStaffAttendances(daycare.id) }

        assertEquals(1, externalAttendances.size)
        assertEquals("Foo Present", externalAttendances[0].name)
        assertEquals(group1.id, externalAttendances[0].groupId)
        assertEquals(now.minusDays(1), externalAttendances[0].arrived)
    }

    @Test
    fun `upsertExternalStaffAttendance updates the group`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val attendanceId = db.transaction { tx ->
            tx.markExternalStaffArrival(
                ExternalStaffArrival(
                    "Foo Present",
                    group1.id,
                    now,
                    occupancyCoefficientSeven,
                    now,
                    employee1.evakaUserId,
                ),
            )
        }

        db.transaction { tx ->
            tx.upsertExternalStaffAttendance(
                attendanceId,
                "Foo Present",
                group2.id,
                now,
                null,
                occupancyCoefficientSeven,
                modifiedAt = now,
                modifiedBy = employee1.evakaUserId,
            )
        }

        assertEquals(
            listOf(group2.id),
            db.read { it.getExternalStaffAttendances(daycare.id) }.map { it.groupId },
        )
    }

    @Test
    fun `markExternalStaffArrival and markExternalStaffDeparture set added and modified`() {
        val arrival = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val arrivalMarkedAt = arrival.plusMinutes(1)
        val departure = arrival.plusHours(7)
        val departureMarkedAt = departure.plusMinutes(2)
        db.transaction { tx ->
            val attendanceId =
                tx.markExternalStaffArrival(
                    ExternalStaffArrival(
                        "Foo",
                        group1.id,
                        arrival,
                        occupancyCoefficientSeven,
                        arrivalMarkedAt,
                        employee1.evakaUserId,
                    ),
                )
            tx.markExternalStaffDeparture(
                ExternalStaffDeparture(
                    attendanceId,
                    departure,
                    departureMarkedAt,
                    employee2.evakaUserId,
                ),
            )
        }

        assertThat(getExternalAttendances())
            .extracting(
                { it.arrivedAddedAt },
                { it.arrivedAddedBy?.id },
                { it.arrivedModifiedAt },
                { it.arrivedModifiedBy?.id },
                { it.departedAddedAt },
                { it.departedAddedBy?.id },
                { it.departedModifiedAt },
                { it.departedModifiedBy?.id },
            )
            .containsExactly(
                Tuple(
                    arrivalMarkedAt,
                    employee1.evakaUserId,
                    arrivalMarkedAt,
                    employee1.evakaUserId,
                    departureMarkedAt,
                    employee2.evakaUserId,
                    departureMarkedAt,
                    employee2.evakaUserId,
                ),
            )
    }

    @Test
    fun `upsertExternalStaffAttendance sets arrival modified on update`() {
        val arrival = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val departure = arrival.plusHours(7)
        val modifiedAt = departure.plusHours(1)
        val attendanceId = db.transaction { tx ->
            tx.upsertExternalStaffAttendance(
                null,
                "Foo",
                group1.id,
                arrival,
                departure,
                occupancyCoefficientSeven,
                modifiedAt = departure,
                modifiedBy = employee1.evakaUserId,
            )
        }

        db.transaction { tx ->
            tx.upsertExternalStaffAttendance(
                attendanceId,
                "Foo",
                group1.id,
                arrival.minusMinutes(30),
                departure,
                occupancyCoefficientSeven,
                modifiedAt = modifiedAt,
                modifiedBy = employee2.evakaUserId,
            )
        }

        assertThat(getExternalAttendances())
            .extracting(
                { it.arrivedAddedAt },
                { it.arrivedModifiedAt },
                { it.arrivedModifiedBy?.id },
                { it.departedAddedAt },
                { it.departedModifiedAt },
            )
            .containsExactly(
                Tuple(departure, modifiedAt, employee2.evakaUserId, departure, departure),
            )
    }

    @Test
    fun `upsertExternalStaffAttendance update works with unmodified data`() {
        val arrival = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val departure = arrival.plusHours(7)
        val save = { id: StaffAttendanceExternalId?, modifiedAt: HelsinkiDateTime ->
            db.transaction { tx ->
                tx.upsertExternalStaffAttendance(
                    id,
                    "Foo",
                    group1.id,
                    arrival,
                    departure,
                    occupancyCoefficientSeven,
                    modifiedAt = modifiedAt,
                    modifiedBy = employee1.evakaUserId,
                )
            }
        }
        val attendanceId = save(null, departure)

        save(attendanceId, departure.plusHours(1))

        assertThat(getExternalAttendances())
            .extracting({ it.arrivedModifiedAt }, { it.departedModifiedAt })
            .containsExactly(Tuple(departure, departure))
    }

    @Test
    fun `upsertExternalStaffAttendance keeps an automatic departure as added when a person corrects it`() {
        val arrival = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val automaticDepartureAt = arrival.plusHours(12).plusMinutes(55)
        val correctedAt = automaticDepartureAt.plusMinutes(30)
        val attendanceId = db.transaction { tx ->
            tx.markExternalStaffArrival(
                ExternalStaffArrival(
                    "Foo",
                    group1.id,
                    arrival,
                    occupancyCoefficientSeven,
                    arrival,
                    employee1.evakaUserId,
                ),
            )
        }
        db.transaction { tx ->
            tx.addMissingStaffAttendanceDepartures(automaticDepartureAt, systemUser.evakaUserId)
        }

        db.transaction { tx ->
            tx.upsertExternalStaffAttendance(
                attendanceId,
                "Foo",
                group1.id,
                arrival,
                arrival.plusHours(8),
                occupancyCoefficientSeven,
                modifiedAt = correctedAt,
                modifiedBy = employee1.evakaUserId,
            )
        }

        assertThat(getExternalAttendances())
            .extracting(
                { it.arrivedModifiedAt },
                { it.departedAddedAt },
                { it.departedAddedBy?.id },
                { it.departedModifiedAt },
                { it.departedModifiedBy?.id },
            )
            .containsExactly(
                Tuple(
                    arrival,
                    automaticDepartureAt,
                    systemUser.evakaUserId,
                    correctedAt,
                    employee1.evakaUserId,
                ),
            )
    }

    @Test
    fun `upsertExternalStaffAttendance and markExternalStaffDeparture add a departure anew after it was removed`() {
        val arrival = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val departure = arrival.plusHours(7)
        val removedAt = departure.plusMinutes(5)
        val reEnteredAt = removedAt.plusMinutes(5)
        val (unitViewId, mobileId) =
            db.transaction { tx ->
                listOf("Unit view", "Mobile").map { name ->
                    tx.upsertExternalStaffAttendance(
                            null,
                            name,
                            group1.id,
                            arrival,
                            departure,
                            occupancyCoefficientSeven,
                            modifiedAt = arrival,
                            modifiedBy = employee1.evakaUserId,
                        )!!
                        .also { id ->
                            tx.upsertExternalStaffAttendance(
                                id,
                                name,
                                group1.id,
                                arrival,
                                null,
                                occupancyCoefficientSeven,
                                modifiedAt = removedAt,
                                modifiedBy = employee1.evakaUserId,
                            )
                        }
                }
            }

        db.transaction { tx ->
            tx.upsertExternalStaffAttendance(
                unitViewId,
                "Unit view",
                group1.id,
                arrival,
                departure,
                occupancyCoefficientSeven,
                modifiedAt = reEnteredAt,
                modifiedBy = employee2.evakaUserId,
            )
            tx.markExternalStaffDeparture(
                ExternalStaffDeparture(mobileId, departure, reEnteredAt, employee2.evakaUserId),
            )
        }

        assertThat(getExternalAttendances())
            .extracting({ it.name }, { it.departedAddedAt }, { it.departedAddedBy?.id })
            .containsExactlyInAnyOrder(
                Tuple("Unit view", reEnteredAt, employee2.evakaUserId),
                Tuple("Mobile", reEnteredAt, employee2.evakaUserId),
            )
    }

    @Test
    fun `on old external staff data, only a first departure sets added`() {
        val arrival = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val departure = arrival.plusHours(7)
        val (mobileId, unitViewId, unitViewChangedId) =
            db.transaction { tx ->
                listOf(
                        "Mobile" to null,
                        "Unit view" to null,
                        "Unit view changed" to arrival.plusHours(6),
                    )
                    .map { (name, departed) ->
                        tx.createUpdate {
                                sql(
                                    """
INSERT INTO staff_attendance_external (name, group_id, arrived, departed, occupancy_coefficient)
VALUES (${bind(name)}, ${bind(group1.id)}, ${bind(arrival)}, ${bind(departed)}, ${bind(occupancyCoefficientSeven)})
RETURNING id
""",
                                )
                            }
                            .executeAndReturnGeneratedKeys()
                            .exactlyOne<StaffAttendanceExternalId>()
                    }
            }

        db.transaction { tx ->
            tx.markExternalStaffDeparture(
                ExternalStaffDeparture(mobileId, departure, departure, employee1.evakaUserId),
            )
            listOf(unitViewId to "Unit view", unitViewChangedId to "Unit view changed").forEach {
                (id, name) ->
                tx.upsertExternalStaffAttendance(
                    id,
                    name,
                    group1.id,
                    arrival,
                    departure,
                    occupancyCoefficientSeven,
                    modifiedAt = departure,
                    modifiedBy = employee1.evakaUserId,
                )
            }
        }

        assertThat(getExternalAttendances())
            .extracting({ it.name }, { it.departedAddedAt }, { it.departedModifiedAt })
            .containsExactlyInAnyOrder(
                Tuple("Mobile", departure, departure),
                Tuple("Unit view", departure, departure),
                Tuple("Unit view changed", null, departure),
            )
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
    fun `addMissingStaffAttendanceDeparture does not keep the added marking of a removed departure`() {
        val now = HelsinkiDateTime.of(today, LocalTime.of(17, 0))
        val trainingArrival = now.atStartOfDay().plusHours(8)
        val plannedDeparture = trainingArrival.plusHours(8)
        val presentArrival = now.minusDays(1).minusMinutes(1)
        db.transaction { tx ->
            listOf(
                    Triple(employee1.id, trainingArrival, StaffAttendanceType.TRAINING),
                    Triple(employee2.id, presentArrival, StaffAttendanceType.PRESENT),
                )
                .forEach { (employeeId, arrival, type) ->
                    val id =
                        tx.upsertStaffAttendance(
                                attendanceId = null,
                                employeeId = employeeId,
                                groupId = group1.id,
                                arrivalTime = arrival,
                                departureTime = arrival.plusHours(1),
                                occupancyCoefficient = BigDecimal(7.0),
                                type = type,
                                modifiedAt = arrival,
                                modifiedBy = employee1.evakaUserId,
                            )!!
                            .new
                            .id
                    tx.upsertStaffAttendance(
                        attendanceId = id,
                        employeeId = employeeId,
                        groupId = group1.id,
                        arrivalTime = arrival,
                        departureTime = null,
                        occupancyCoefficient = BigDecimal(7.0),
                        type = type,
                        modifiedAt = arrival.plusHours(2),
                        modifiedBy = employee1.evakaUserId,
                    )
                }
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1.id,
                    startTime = trainingArrival,
                    endTime = plannedDeparture,
                ),
            )
            val externalId =
                tx.upsertExternalStaffAttendance(
                    null,
                    "Foo",
                    group1.id,
                    presentArrival,
                    presentArrival.plusHours(1),
                    occupancyCoefficientSeven,
                    modifiedAt = presentArrival,
                    modifiedBy = employee1.evakaUserId,
                )
            tx.upsertExternalStaffAttendance(
                externalId,
                "Foo",
                group1.id,
                presentArrival,
                null,
                occupancyCoefficientSeven,
                modifiedAt = presentArrival.plusHours(2),
                modifiedBy = employee1.evakaUserId,
            )

            tx.addMissingStaffAttendanceDepartures(now, systemUser.evakaUserId)
        }

        assertThat(
                db.read { tx ->
                    tx.getStaffAttendancesForDateRange(
                        daycare.id,
                        FiniteDateRange(today.minusDays(1), today),
                    )
                },
            )
            .extracting({ it.departed }, { it.departedAddedAt })
            .containsExactlyInAnyOrder(
                Tuple(plannedDeparture, null),
                Tuple(presentArrival.plusHours(12), null),
            )
        assertThat(getExternalAttendances())
            .extracting({ it.departed }, { it.departedAddedAt })
            .containsExactly(Tuple(presentArrival.plusHours(12), null))
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
                ),
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
                ),
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
                ),
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
    fun `addMissingStaffAttendanceDeparture adds a departure at planned time for overnight attendance when type is SICKNESS`() {
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
                type = StaffAttendanceType.SICKNESS,
                modifiedAt = now,
                modifiedBy = employee1.evakaUserId,
            )

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1.id,
                    startTime = arrival,
                    endTime = plannedDeparture,
                ),
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
    fun `addMissingStaffAttendanceDeparture adds a departure at planned time for overnight attendance when type is CHILD_SICKNESS`() {
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
                type = StaffAttendanceType.CHILD_SICKNESS,
                modifiedAt = now,
                modifiedBy = employee1.evakaUserId,
            )

            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee1.id,
                    startTime = arrival,
                    endTime = plannedDeparture,
                ),
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
                ),
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
    fun `addMissingStaffAttendanceDeparture adds a departure 12h after arrival for overnight attendance when arrival is 1 minute before plan start`() {
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
                    startTime = arrival.plusMinutes(1),
                    endTime = plannedDeparture,
                ),
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
                ),
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
                ),
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

        val attendance = db.transaction { tx ->
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
                },
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

        val attendance = db.transaction { tx ->
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
                },
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
        val attendanceId = db.transaction { tx ->
            tx.insert(
                DevStaffAttendance(
                    employeeId = user.id,
                    groupId = groupId,
                    arrived = arrivalTime,
                    departed = null,
                    modifiedAt = arrivalAddedAt,
                    modifiedBy = user.evakaUserId,
                ),
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
                },
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
        val attendanceId = db.transaction { tx ->
            tx.insert(
                DevStaffAttendance(
                    employeeId = user.id,
                    groupId = groupId,
                    arrived = arrivalTime,
                    departed = null,
                    modifiedAt = arrivalAddedAt,
                    modifiedBy = user.evakaUserId,
                ),
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
                },
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
                ),
            )
    }

    @Test
    fun `upsertStaffAttendance sets departure modified on remove and adds a departure entered again anew`() {
        val (unitId, groupId, user) = initUpsertStaffAttendanceTestData()
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 12, 20), LocalTime.of(8, 0)))
        val arrival = clock.now()
        val departure = arrival.plusHours(7)
        val addedAt = arrival.plusMinutes(2)
        val attendanceId = db.transaction { tx ->
            tx.insert(
                DevStaffAttendance(
                    employeeId = user.id,
                    groupId = groupId,
                    arrived = arrival,
                    departed = departure,
                    modifiedAt = addedAt,
                    modifiedBy = user.evakaUserId,
                ),
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
                },
            )
            .extracting(
                { it.id },
                { it.arrivedAddedAt },
                { it.arrivedModifiedAt },
                { it.departedAddedAt },
                { it.departedModifiedAt },
            )
            .containsExactly(Tuple(attendanceId, addedAt, addedAt, addedAt, departureModifiedAt))

        val reEnteredAt = departureModifiedAt.plusMinutes(5)
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
                modifiedAt = reEnteredAt,
                modifiedBy = user.evakaUserId,
            )
        }

        assertThat(
                db.read { tx ->
                    tx.getStaffAttendancesForDateRange(
                        unitId,
                        clock.today().let { FiniteDateRange(it, it) },
                    )
                },
            )
            .extracting({ it.departedAddedAt }, { it.departedModifiedAt })
            .containsExactly(Tuple(reEnteredAt, reEnteredAt))
    }

    @Test
    fun `upsertStaffAttendance doesn't set added for old data`() {
        val (unitId, groupId, user) = initUpsertStaffAttendanceTestData()
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 12, 20), LocalTime.of(8, 0)))
        val arrival = clock.now()
        val attendanceId = db.transaction { tx ->
            tx.insert(
                DevStaffAttendance(
                    employeeId = user.id,
                    groupId = groupId,
                    arrived = arrival,
                    departed = null,
                    modifiedAt = null,
                    modifiedBy = null,
                ),
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
                },
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
                },
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

    @Test
    fun `upsertStaffAttendance keeps an automatic departure as added when a person corrects it`() {
        val (unitId, groupId, user) = initUpsertStaffAttendanceTestData()
        val arrivalTime = HelsinkiDateTime.of(LocalDate.of(2024, 12, 20), LocalTime.of(8, 0))
        val automaticDepartureAt = arrivalTime.plusHours(12).plusMinutes(55)
        val correctedAt = automaticDepartureAt.plusMinutes(30)
        val attendanceId = db.transaction { tx ->
            tx.upsertStaffAttendance(
                    attendanceId = null,
                    employeeId = user.id,
                    groupId = groupId,
                    arrivalTime = arrivalTime,
                    departureTime = null,
                    occupancyCoefficient = BigDecimal.ZERO,
                    type = StaffAttendanceType.PRESENT,
                    modifiedAt = arrivalTime,
                    modifiedBy = user.evakaUserId,
                )!!
                .new
                .id!!
        }
        db.transaction { tx ->
            tx.addMissingStaffAttendanceDepartures(automaticDepartureAt, systemUser.evakaUserId)
        }

        db.transaction { tx ->
            tx.upsertStaffAttendance(
                attendanceId = attendanceId,
                employeeId = user.id,
                groupId = groupId,
                arrivalTime = arrivalTime,
                departureTime = arrivalTime.plusHours(8),
                occupancyCoefficient = BigDecimal.ZERO,
                type = StaffAttendanceType.PRESENT,
                modifiedAt = correctedAt,
                modifiedBy = user.evakaUserId,
            )
        }

        assertThat(
                db.read { tx ->
                    tx.getStaffAttendancesForDateRange(
                        unitId,
                        FiniteDateRange(arrivalTime.toLocalDate(), arrivalTime.toLocalDate()),
                    )
                },
            )
            .extracting(
                { it.departedAddedAt },
                { it.departedAddedBy?.id },
                { it.departedModifiedAt },
                { it.departedModifiedBy?.id },
            )
            .containsExactly(
                Tuple(
                    automaticDepartureAt,
                    systemUser.evakaUserId,
                    correctedAt,
                    user.evakaUserId,
                ),
            )
    }

    private fun getExternalAttendances() = db.read { tx ->
        tx.getExternalStaffAttendancesByDateRange(daycare.id, FiniteDateRange(today, today))
    }

    private fun initUpsertStaffAttendanceTestData():
        Triple<DaycareId, GroupId, AuthenticatedUser.Employee> {
        val (unitId, groupId) =
            db.transaction { tx ->
                val areaId =
                    tx.insert(
                        DevCareArea(name = "Upsert Test Area", shortName = "upsert_test_area"),
                    )
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val groupId = tx.insert(DevDaycareGroup(daycareId = unitId))
                unitId to groupId
            }
        val user = db.transaction { tx ->
            val employeeId = tx.insert(DevEmployee(), unitRoles = mapOf(unitId to UserRole.STAFF))
            AuthenticatedUser.Employee(employeeId, roles = emptySet())
        }
        return Triple(unitId, groupId, user)
    }
}
