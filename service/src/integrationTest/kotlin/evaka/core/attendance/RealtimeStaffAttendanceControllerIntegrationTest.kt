// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.attendance

import evaka.core.FullApplicationTest
import evaka.core.shared.DaycareId
import evaka.core.shared.GroupId
import evaka.core.shared.StaffAttendanceExternalId
import evaka.core.shared.StaffAttendanceRealtimeId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.auth.insertDaycareAclRow
import evaka.core.shared.auth.syncDaycareGroupAcl
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevStaffAttendance
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class RealtimeStaffAttendanceControllerIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var realtimeStaffAttendanceController: RealtimeStaffAttendanceController

    private val now = HelsinkiDateTime.of(LocalDate.of(2023, 2, 1), LocalTime.of(12, 0))
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val daycare2 = DevDaycare(areaId = area.id, name = "Test Daycare 2")
    private val supervisor = DevEmployee()
    private val staff = DevEmployee()
    private val unitSupervisor = AuthenticatedUser.Employee(supervisor.id, setOf())
    private val group1 = DevDaycareGroup(daycareId = daycare.id)
    private val group2 = DevDaycareGroup(daycareId = daycare2.id, name = "Group 2")

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(supervisor)
            tx.insert(staff)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(group1)
            tx.insert(group2)

            tx.insertDaycareAclRow(daycare.id, supervisor.id, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(daycare2.id, supervisor.id, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(daycare.id, staff.id, UserRole.STAFF)
            tx.insertDaycareAclRow(daycare2.id, staff.id, UserRole.STAFF)
            tx.syncDaycareGroupAcl(daycare.id, staff.id, listOf(group1.id), now)

            tx.upsertOccupancyCoefficient(
                OccupancyCoefficientUpsert(daycare.id, staff.id, BigDecimal(7)),
            )
            tx.upsertOccupancyCoefficient(
                OccupancyCoefficientUpsert(daycare2.id, staff.id, BigDecimal(7)),
            )
        }
    }

    @Test
    fun `Attendances can be added to multiple units`() {
        upsertDailyStaffAttendances(
            unitId = daycare.id,
            groupId = group1.id,
            arrived = now.minusHours(3),
            departed = now.minusHours(2),
            hasStaffOccupancyEffect = true,
        )
        upsertDailyStaffAttendances(
            unitId = daycare2.id,
            groupId = group2.id,
            arrived = now.minusHours(1),
            departed = null,
            hasStaffOccupancyEffect = false,
        )

        val unit1Attendances = getAttendances(daycare.id)
        assertEquals(1, unit1Attendances.staff.size)
        assertEquals(0, unit1Attendances.extraAttendances.size)
        unit1Attendances.staff.first().let { attendance ->
            assertEquals(staff.id, attendance.employeeId)
            assertEquals(listOf(group1.id), attendance.groups)
            assertEquals(staff.firstName, attendance.firstName)
            assertEquals(staff.lastName, attendance.lastName)
            assertEquals(7.0, attendance.currentOccupancyCoefficient.toDouble())
            assertEquals(1, attendance.attendances.size)
            assertEquals(0, attendance.plannedAttendances.size)
            attendance.attendances.first().let { attendanceEntry ->
                assertEquals(now.minusHours(3), attendanceEntry.arrived)
                assertEquals(now.minusHours(2), attendanceEntry.departed)
                assertEquals(StaffAttendanceType.PRESENT, attendanceEntry.type)
                assertEquals(
                    occupancyCoefficientSeven.stripTrailingZeros(),
                    attendanceEntry.occupancyCoefficient.stripTrailingZeros(),
                )
            }
        }

        val unit2Attendances = getAttendances(daycare2.id)
        assertEquals(1, unit2Attendances.staff.size)
        assertEquals(0, unit2Attendances.extraAttendances.size)
        unit2Attendances.staff.first().let { attendance ->
            assertEquals(staff.id, attendance.employeeId)
            assertEquals(listOf(), attendance.groups) // No group acl to group2
            assertEquals(staff.firstName, attendance.firstName)
            assertEquals(staff.lastName, attendance.lastName)
            assertEquals(7.0, attendance.currentOccupancyCoefficient.toDouble())
            assertEquals(1, attendance.attendances.size)
            assertEquals(0, attendance.plannedAttendances.size)
            attendance.attendances.first().let { attendanceEntry ->
                assertEquals(now.minusHours(1), attendanceEntry.arrived)
                assertEquals(null, attendanceEntry.departed)
                assertEquals(StaffAttendanceType.PRESENT, attendanceEntry.type)
                assertEquals(
                    BigDecimal.ZERO,
                    attendanceEntry.occupancyCoefficient.stripTrailingZeros(),
                )
            }
        }
    }

    @Test
    fun attendanceCannotBeInFuture() {
        assertThrows<BadRequest> {
            upsertDailyStaffAttendances(
                daycare.id,
                group1.id,
                now.minusHours(3),
                now.plusMinutes(1),
            )
        }
        assertThrows<BadRequest> {
            upsertDailyStaffAttendances(daycare.id, group1.id, now.plusMinutes(3), null)
        }
        assertThrows<BadRequest> {
            upsertDailyStaffAttendances(daycare.id, group1.id, now.plusHours(22), now.plusHours(23))
        }
        upsertDailyStaffAttendances(daycare.id, group1.id, now.minusHours(7), now.minusHours(3))
        upsertDailyStaffAttendances(daycare.id, group1.id, now.minusHours(2), null)

        assertThrows<BadRequest> {
            upsertDailyExternalAttendances(
                daycare.id,
                group1.id,
                now.minusHours(3),
                now.plusMinutes(1),
            )
        }
        assertThrows<BadRequest> {
            upsertDailyExternalAttendances(daycare.id, group1.id, now.plusMinutes(3), null)
        }
        assertThrows<BadRequest> {
            upsertDailyExternalAttendances(
                daycare.id,
                group1.id,
                now.plusHours(22),
                now.plusHours(23),
            )
        }
        upsertDailyExternalAttendances(daycare.id, group1.id, now.minusHours(7), now.minusHours(3))
        upsertDailyExternalAttendances(daycare.id, group1.id, now.minusHours(2), null)
    }

    @Test
    fun `Departure must be after arrival`() {
        val exception =
            assertThrows<BadRequest> {
                upsertDailyStaffAttendances(
                    daycare.id,
                    group1.id,
                    now.minusHours(1),
                    now.minusHours(1),
                )
            }
        assertEquals("Departure time must be after arrival time", exception.message)
        assertThrows<BadRequest> {
            upsertDailyStaffAttendances(daycare.id, group1.id, now.minusHours(1), now.minusHours(2))
        }
    }

    @Test
    fun `Attendances in other units are returned`() {
        upsertDailyStaffAttendances(daycare2.id, group2.id, now.minusHours(5), now.minusHours(3))

        val attendance = getAttendances(daycare.id).staff.single()
        assertEquals(listOf(), attendance.attendances)
        assertEquals(
            listOf(
                OtherUnitStaffAttendance(
                    staff.id,
                    daycare2.id,
                    daycare2.name,
                    now.minusHours(5),
                    now.minusHours(3),
                ),
            ),
            attendance.otherUnitAttendances,
        )
        assertEquals(listOf(), getAttendances(daycare2.id).staff.single().otherUnitAttendances)
    }

    @Test
    fun `Attendances in other units are returned only for employees the user can edit`() {
        val otherStaff = DevEmployee()
        db.transaction { tx ->
            tx.insert(otherStaff)
            tx.insertDaycareAclRow(daycare.id, otherStaff.id, UserRole.STAFF)
            tx.insertDaycareAclRow(daycare2.id, otherStaff.id, UserRole.STAFF)
            tx.insert(
                DevStaffAttendance(
                    employeeId = otherStaff.id,
                    groupId = group2.id,
                    arrived = now.minusHours(5),
                    departed = now.minusHours(3),
                    modifiedAt = now,
                    modifiedBy = supervisor.evakaUserId,
                ),
            )
        }
        upsertDailyStaffAttendances(daycare2.id, group2.id, now.minusHours(5), now.minusHours(3))

        val staffUser = AuthenticatedUser.Employee(staff.id, setOf())
        val response =
            realtimeStaffAttendanceController.getRealtimeStaffAttendances(
                dbInstance(),
                staffUser,
                MockEvakaClock(now),
                daycare.id,
                now.toLocalDate(),
                now.toLocalDate(),
            )

        assertEquals(
            1,
            response.staff.single { it.employeeId == staff.id }.otherUnitAttendances.size,
        )
        assertEquals(
            listOf(),
            response.staff.single { it.employeeId == otherStaff.id }.otherUnitAttendances,
        )
        assertEquals(
            1,
            getAttendances(daycare.id)
                .staff
                .single { it.employeeId == otherStaff.id }
                .otherUnitAttendances
                .size,
        )
    }

    @Test
    fun `Attendances in other units are returned only for employees with an acl in the unit`() {
        val formerStaff = DevEmployee()
        db.transaction { tx ->
            tx.insert(formerStaff)
            tx.insertDaycareAclRow(daycare2.id, formerStaff.id, UserRole.STAFF)
            tx.insert(
                DevStaffAttendance(
                    employeeId = formerStaff.id,
                    groupId = group1.id,
                    arrived = now.minusHours(8),
                    departed = now.minusHours(6),
                    modifiedAt = now,
                    modifiedBy = supervisor.evakaUserId,
                ),
            )
            tx.insert(
                DevStaffAttendance(
                    employeeId = formerStaff.id,
                    groupId = group2.id,
                    arrived = now.minusHours(5),
                    departed = now.minusHours(3),
                    modifiedAt = now,
                    modifiedBy = supervisor.evakaUserId,
                ),
            )
        }

        val attendance = getAttendances(daycare.id).staff.single { it.employeeId == formerStaff.id }
        assertEquals(1, attendance.attendances.size)
        assertEquals(listOf(), attendance.otherUnitAttendances)
    }

    @Test
    fun `Attendances in other units are returned until the end of the day following the range`() {
        val tomorrow = now.toLocalDate().plusDays(1)
        val dayAfterTomorrow = now.toLocalDate().plusDays(2)
        db.transaction { tx ->
            listOf(tomorrow, dayAfterTomorrow).forEach { date ->
                tx.insert(
                    DevStaffAttendance(
                        employeeId = staff.id,
                        groupId = group2.id,
                        arrived = HelsinkiDateTime.of(date, LocalTime.of(0, 0)),
                        departed = HelsinkiDateTime.of(date, LocalTime.of(2, 0)),
                        modifiedAt = now,
                        modifiedBy = supervisor.evakaUserId,
                    ),
                )
            }
        }

        assertEquals(
            listOf(HelsinkiDateTime.of(tomorrow, LocalTime.of(0, 0))),
            getAttendances(daycare.id).staff.single().otherUnitAttendances.map { it.arrived },
        )
    }

    @Test
    fun `Attendances can be fetched for at most six weeks`() {
        fun fetch(start: LocalDate, end: LocalDate) =
            realtimeStaffAttendanceController.getRealtimeStaffAttendances(
                dbInstance(),
                unitSupervisor,
                MockEvakaClock(now),
                daycare.id,
                start,
                end,
            )
        val today = now.toLocalDate()

        fetch(today.minusWeeks(6).plusDays(1), today)
        val tooLong = assertThrows<BadRequest> { fetch(today.minusWeeks(6), today) }
        assertEquals("Only 6 weeks of staff attendances may be fetched at once", tooLong.message)
        val reversed = assertThrows<BadRequest> { fetch(today, today.minusDays(1)) }
        assertEquals("Start must be before or equal to the end", reversed.message)
    }

    @Test
    fun `Attendances in other units are not returned for employees whose acl in the unit has ended`() {
        db.transaction { tx ->
            tx.insert(
                DevStaffAttendance(
                    employeeId = staff.id,
                    groupId = group1.id,
                    arrived = now.minusHours(8),
                    departed = now.minusHours(6),
                    modifiedAt = now,
                    modifiedBy = supervisor.evakaUserId,
                ),
            )
            tx.insert(
                DevStaffAttendance(
                    employeeId = staff.id,
                    groupId = group2.id,
                    arrived = now.minusHours(5),
                    departed = now.minusHours(3),
                    modifiedAt = now,
                    modifiedBy = supervisor.evakaUserId,
                ),
            )
        }
        fun otherUnitAttendanceCount(aclEndDate: LocalDate): Int {
            db.transaction { tx ->
                tx.insertDaycareAclRow(daycare.id, staff.id, UserRole.STAFF, aclEndDate)
            }
            return getAttendances(daycare.id)
                .staff
                .single { it.employeeId == staff.id }
                .otherUnitAttendances
                .size
        }

        assertEquals(1, otherUnitAttendanceCount(now.toLocalDate()))
        assertEquals(0, otherUnitAttendanceCount(now.toLocalDate().minusDays(1)))
    }

    @Test
    fun `Overnight attendance cannot overlap with an attendance on the next day in the same unit`() {
        val yesterday = now.toLocalDate().minusDays(1)
        db.transaction { tx ->
            tx.insert(
                DevStaffAttendance(
                    employeeId = staff.id,
                    groupId = group1.id,
                    arrived = HelsinkiDateTime.of(now.toLocalDate(), LocalTime.of(6, 0)),
                    departed = HelsinkiDateTime.of(now.toLocalDate(), LocalTime.of(7, 0)),
                    modifiedAt = now,
                    modifiedBy = supervisor.evakaUserId,
                ),
            )
        }

        val exception =
            assertThrows<BadRequest> {
                upsertStaffAttendancesOnDate(
                    yesterday,
                    listOf(
                        presentEntry(
                            group1.id,
                            HelsinkiDateTime.of(yesterday, LocalTime.of(23, 0)),
                            HelsinkiDateTime.of(now.toLocalDate(), LocalTime.of(6, 30)),
                        ),
                    ),
                )
            }
        assertEquals(OVERLAPPING_STAFF_ATTENDANCE, exception.errorCode)

        upsertStaffAttendancesOnDate(
            yesterday,
            listOf(
                presentEntry(
                    group1.id,
                    HelsinkiDateTime.of(yesterday, LocalTime.of(23, 0)),
                    HelsinkiDateTime.of(now.toLocalDate(), LocalTime.of(6, 0)),
                ),
            ),
        )
    }

    @Test
    fun `Adjacent entries in the same request are allowed`() {
        upsertStaffAttendancesOnDate(
            now.toLocalDate(),
            listOf(
                presentEntry(group1.id, now.minusHours(5), now.minusHours(3)),
                presentEntry(group1.id, now.minusHours(3), null),
            ),
        )
        assertEquals(2, getAttendances(daycare.id).staff.single().attendances.size)
    }

    @Test
    fun `Open entry followed by another entry in the same request is rejected`() {
        val exception =
            assertThrows<BadRequest> {
                upsertStaffAttendancesOnDate(
                    now.toLocalDate(),
                    listOf(
                        presentEntry(group1.id, now.minusHours(5), null),
                        presentEntry(group1.id, now.minusHours(3), now.minusHours(2)),
                    ),
                )
            }
        assertEquals(OVERLAPPING_STAFF_ATTENDANCE, exception.errorCode)
    }

    @Test
    fun `Attendance cannot overlap with an attendance in another unit`() {
        upsertDailyStaffAttendances(daycare2.id, group2.id, now.minusHours(5), now.minusHours(3))

        val exception =
            assertThrows<BadRequest> {
                upsertDailyStaffAttendances(
                    daycare.id,
                    group1.id,
                    now.minusHours(4),
                    now.minusHours(1),
                )
            }
        assertEquals(OVERLAPPING_STAFF_ATTENDANCE, exception.errorCode)

        upsertDailyStaffAttendances(daycare.id, group1.id, now.minusHours(3), now.minusHours(1))
    }

    @Test
    fun `Attendance cannot overlap with an open attendance in another unit`() {
        upsertDailyStaffAttendances(daycare2.id, group2.id, now.minusHours(5), null)

        val exception =
            assertThrows<BadRequest> {
                upsertDailyStaffAttendances(
                    daycare.id,
                    group1.id,
                    now.minusHours(2),
                    now.minusHours(1),
                )
            }
        assertEquals(OVERLAPPING_STAFF_ATTENDANCE, exception.errorCode)
    }

    @Test
    fun `Entries in the same request cannot overlap`() {
        val exception =
            assertThrows<BadRequest> {
                realtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances(
                    dbInstance(),
                    unitSupervisor,
                    MockEvakaClock(now),
                    RealtimeStaffAttendanceController.StaffAttendanceBody(
                        unitId = daycare.id,
                        date = now.toLocalDate(),
                        employeeId = staff.id,
                        entries =
                            listOf(
                                presentEntry(group1.id, now.minusHours(5), now.minusHours(3)),
                                presentEntry(group1.id, now.minusHours(4), now.minusHours(2)),
                            ),
                    ),
                )
            }
        assertEquals(OVERLAPPING_STAFF_ATTENDANCE, exception.errorCode)
    }

    @Test
    fun `Existing attendance can be modified without overlap error`() {
        upsertDailyStaffAttendances(daycare.id, group1.id, now.minusHours(5), now.minusHours(3))
        val existing = getAttendances(daycare.id).staff.single().attendances.single()

        realtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances(
            dbInstance(),
            unitSupervisor,
            MockEvakaClock(now),
            RealtimeStaffAttendanceController.StaffAttendanceBody(
                unitId = daycare.id,
                date = now.toLocalDate(),
                employeeId = staff.id,
                entries =
                    listOf(
                        presentEntry(group1.id, now.minusHours(5), now.minusHours(2))
                            .copy(id = existing.id),
                    ),
            ),
        )

        assertEquals(
            now.minusHours(2),
            getAttendances(daycare.id).staff.single().attendances.single().departed,
        )
    }

    @Test
    fun `Attendances without group can be deleted`() {
        realtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances(
            dbInstance(),
            unitSupervisor,
            MockEvakaClock(now),
            RealtimeStaffAttendanceController.StaffAttendanceBody(
                unitId = daycare.id,
                date = now.toLocalDate(),
                employeeId = staff.id,
                entries =
                    listOf(
                        RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                            id = null,
                            groupId = null,
                            arrived = now.minusHours(3),
                            departed = now.minusHours(2),
                            type = StaffAttendanceType.TRAINING,
                            hasStaffOccupancyEffect = false,
                        ),
                    ),
            ),
        )

        getAttendances(daycare.id).staff.first().let { attendance ->
            assertEquals(staff.id, attendance.employeeId)
            assertEquals(listOf(group1.id), attendance.groups)
            assertEquals(staff.firstName, attendance.firstName)
            assertEquals(staff.lastName, attendance.lastName)
            assertEquals(7.0, attendance.currentOccupancyCoefficient.toDouble())
            assertEquals(1, attendance.attendances.size)
            assertEquals(0, attendance.plannedAttendances.size)
            attendance.attendances.first().let { attendanceEntry ->
                assertEquals(now.minusHours(3), attendanceEntry.arrived)
                assertEquals(now.minusHours(2), attendanceEntry.departed)
                assertEquals(StaffAttendanceType.TRAINING, attendanceEntry.type)
            }
        }

        realtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances(
            dbInstance(),
            unitSupervisor,
            MockEvakaClock(now),
            RealtimeStaffAttendanceController.StaffAttendanceBody(
                unitId = daycare.id,
                date = now.toLocalDate(),
                employeeId = staff.id,
                entries = emptyList(),
            ),
        )
        assertEquals(0, getAttendances(daycare.id).staff[0].attendances.size)
    }

    @Test
    fun `Adjacent attendances can be shifted in both directions`() {
        fun save(vararg rows: Triple<StaffAttendanceRealtimeId?, Int, Int>) =
            realtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances(
                dbInstance(),
                unitSupervisor,
                MockEvakaClock(now),
                RealtimeStaffAttendanceController.StaffAttendanceBody(
                    unitId = daycare.id,
                    date = now.toLocalDate(),
                    employeeId = staff.id,
                    entries =
                        rows.map { (id, arrivalHour, departureHour) ->
                            RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                                id = id,
                                groupId = group1.id,
                                arrived = now.withTime(LocalTime.of(arrivalHour, 0)),
                                departed = now.withTime(LocalTime.of(departureHour, 0)),
                                type = StaffAttendanceType.PRESENT,
                                hasStaffOccupancyEffect = true,
                            )
                        },
                ),
            )

        save(Triple(null, 7, 9), Triple(null, 9, 11))
        val (firstId, secondId) =
            getAttendances(daycare.id)
                .staff
                .first()
                .attendances
                .sortedBy { it.arrived }
                .map { it.id }

        save(Triple(firstId, 8, 10), Triple(secondId, 10, 12))
        save(Triple(firstId, 7, 9), Triple(secondId, 9, 11))

        assertEquals(
            listOf(
                firstId to now.withTime(LocalTime.of(7, 0)),
                secondId to now.withTime(LocalTime.of(9, 0)),
            ),
            getAttendances(daycare.id)
                .staff
                .first()
                .attendances
                .sortedBy { it.arrived }
                .map { it.id to it.arrived },
        )
    }

    @Test
    fun `Staff cannot update other employee's attendances`() {
        // Employee tries to upsert supervisor's attendance which should be forbidden
        assertThrows<Forbidden> {
            realtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances(
                dbInstance(),
                AuthenticatedUser.Employee(staff.id, setOf()),
                MockEvakaClock(now),
                RealtimeStaffAttendanceController.StaffAttendanceBody(
                    unitId = daycare.id,
                    date = now.toLocalDate(),
                    employeeId = supervisor.id,
                    entries =
                        listOf(
                            RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                                id = null,
                                groupId = null,
                                arrived = now.minusHours(3),
                                departed = now.minusHours(2),
                                type = StaffAttendanceType.TRAINING,
                                hasStaffOccupancyEffect = false,
                            ),
                        ),
                ),
            )
        }
    }

    @Test
    fun `Attendance id must belong to the employee`() {
        val supervisorAttendanceId = db.transaction { tx ->
            tx.insert(
                DevStaffAttendance(
                    employeeId = supervisor.id,
                    groupId = group1.id,
                    arrived = now.minusHours(3),
                    departed = now.minusHours(2),
                    modifiedAt = now,
                    modifiedBy = unitSupervisor.evakaUserId,
                ),
            )
        }

        val exception =
            assertThrows<BadRequest> {
                realtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances(
                    dbInstance(),
                    AuthenticatedUser.Employee(staff.id, setOf()),
                    MockEvakaClock(now),
                    RealtimeStaffAttendanceController.StaffAttendanceBody(
                        unitId = daycare.id,
                        date = now.toLocalDate(),
                        employeeId = staff.id,
                        entries =
                            listOf(
                                RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                                    id = supervisorAttendanceId,
                                    groupId = group1.id,
                                    arrived = now.minusHours(1),
                                    departed = null,
                                    type = StaffAttendanceType.PRESENT,
                                    hasStaffOccupancyEffect = true,
                                ),
                            ),
                    ),
                )
            }
        assertEquals("Unknown id was given", exception.message)
    }

    @Test
    fun `Attendance group must be in the unit`() {
        val exception =
            assertThrows<BadRequest> {
                upsertDailyStaffAttendances(
                    daycare.id,
                    group2.id,
                    now.minusHours(3),
                    now.minusHours(2),
                )
            }
        assertEquals("Group is not in unit", exception.message)

        val externalException =
            assertThrows<BadRequest> {
                upsertDailyExternalAttendances(
                    daycare.id,
                    group2.id,
                    now.minusHours(3),
                    now.minusHours(2),
                )
            }
        assertEquals("Group is not in unit", externalException.message)
    }

    @Test
    fun `External attendances of another unit are not touched`() {
        upsertDailyExternalAttendances(daycare2.id, group2.id, now.minusHours(3), now.minusHours(2))
        val otherUnitAttendanceId = getAttendances(daycare2.id).extraAttendances.single().id

        val exception =
            assertThrows<BadRequest> {
                upsertDailyExternalAttendances(
                    daycare.id,
                    group1.id,
                    now.minusHours(1),
                    null,
                    id = otherUnitAttendanceId,
                )
            }
        assertEquals("Unknown id was given", exception.message)

        upsertDailyExternalAttendances(daycare.id, group1.id, now.minusHours(1), null)
        assertEquals(
            listOf(otherUnitAttendanceId),
            getAttendances(daycare2.id).extraAttendances.map { it.id },
        )
    }

    @ParameterizedTest
    @EnumSource(StaffAttendanceType::class)
    fun `Staff attendance type upsert works`(type: StaffAttendanceType) {
        realtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances(
            dbInstance(),
            unitSupervisor,
            MockEvakaClock(now),
            RealtimeStaffAttendanceController.StaffAttendanceBody(
                unitId = daycare.id,
                date = now.toLocalDate(),
                employeeId = staff.id,
                entries =
                    listOf(
                        RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                            id = null,
                            groupId = group1.id.takeIf { type.presentInGroup() },
                            arrived = now.minusHours(3),
                            departed = now.minusHours(2),
                            type = type,
                            hasStaffOccupancyEffect = false,
                        ),
                    ),
            ),
        )
    }

    private fun getAttendances(unitId: DaycareId): StaffAttendanceResponse {
        return realtimeStaffAttendanceController.getRealtimeStaffAttendances(
            dbInstance(),
            unitSupervisor,
            MockEvakaClock(now),
            unitId,
            now.toLocalDate(),
            now.toLocalDate(),
        )
    }

    private fun upsertDailyStaffAttendances(
        unitId: DaycareId,
        groupId: GroupId,
        arrived: HelsinkiDateTime,
        departed: HelsinkiDateTime?,
        hasStaffOccupancyEffect: Boolean = true,
    ) {
        realtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances(
            dbInstance(),
            unitSupervisor,
            MockEvakaClock(now),
            RealtimeStaffAttendanceController.StaffAttendanceBody(
                unitId = unitId,
                date = now.toLocalDate(),
                employeeId = staff.id,
                entries =
                    listOf(
                        RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                            id = null,
                            groupId = groupId,
                            arrived = arrived,
                            departed = departed,
                            type = StaffAttendanceType.PRESENT,
                            hasStaffOccupancyEffect = hasStaffOccupancyEffect,
                        ),
                    ),
            ),
        )
    }

    private fun upsertStaffAttendancesOnDate(
        date: LocalDate,
        entries: List<RealtimeStaffAttendanceController.StaffAttendanceUpsert>,
    ) {
        realtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances(
            dbInstance(),
            unitSupervisor,
            MockEvakaClock(now),
            RealtimeStaffAttendanceController.StaffAttendanceBody(
                unitId = daycare.id,
                date = date,
                employeeId = staff.id,
                entries = entries,
            ),
        )
    }

    private fun presentEntry(
        groupId: GroupId,
        arrived: HelsinkiDateTime,
        departed: HelsinkiDateTime?,
    ) =
        RealtimeStaffAttendanceController.StaffAttendanceUpsert(
            id = null,
            groupId = groupId,
            arrived = arrived,
            departed = departed,
            type = StaffAttendanceType.PRESENT,
            hasStaffOccupancyEffect = true,
        )

    private fun upsertDailyExternalAttendances(
        unitId: DaycareId,
        groupId: GroupId,
        arrived: HelsinkiDateTime,
        departed: HelsinkiDateTime?,
        id: StaffAttendanceExternalId? = null,
    ) {
        realtimeStaffAttendanceController.upsertDailyExternalRealtimeAttendances(
            dbInstance(),
            unitSupervisor,
            MockEvakaClock(now),
            RealtimeStaffAttendanceController.ExternalAttendanceBody(
                unitId = unitId,
                date = now.toLocalDate(),
                name = "test",
                entries =
                    listOf(
                        RealtimeStaffAttendanceController.ExternalAttendanceUpsert(
                            id = id,
                            groupId = groupId,
                            hasStaffOccupancyEffect = false,
                            arrived = arrived,
                            departed = departed,
                        ),
                    ),
            ),
        )
    }
}
