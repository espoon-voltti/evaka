// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.syncDaycareGroupAcl
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
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
                OccupancyCoefficientUpsert(daycare.id, staff.id, BigDecimal(7))
            )
            tx.upsertOccupancyCoefficient(
                OccupancyCoefficientUpsert(daycare2.id, staff.id, BigDecimal(7))
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
                        )
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
                            )
                        ),
                ),
            )
        }
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
                        )
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
                        )
                    ),
            ),
        )
    }

    private fun upsertDailyExternalAttendances(
        unitId: DaycareId,
        groupId: GroupId,
        arrived: HelsinkiDateTime,
        departed: HelsinkiDateTime?,
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
                            id = null,
                            groupId = groupId,
                            hasStaffOccupancyEffect = false,
                            arrived = arrived,
                            departed = departed,
                        )
                    ),
            ),
        )
    }
}
