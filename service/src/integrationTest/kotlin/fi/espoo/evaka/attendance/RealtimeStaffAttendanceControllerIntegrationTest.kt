// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.insertDaycareGroupAcl
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class RealtimeStaffAttendanceControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var realtimeStaffAttendanceController: RealtimeStaffAttendanceController

    private val now = HelsinkiDateTime.of(LocalDate.of(2023, 2, 1), LocalTime.of(12, 0))
    private val unitSupervisor = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf())
    private val groupId1 = GroupId(UUID.randomUUID())
    private val groupId2 = GroupId(UUID.randomUUID())
    private val supervisor = testDecisionMaker_1
    private val staff = testDecisionMaker_2

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(
                DevDaycareGroup(id = groupId1, daycareId = testDaycare.id, name = "Testiläiset 1")
            )
            tx.insert(
                DevDaycareGroup(id = groupId2, daycareId = testDaycare2.id, name = "Testiläiset 2")
            )

            tx.insertDaycareAclRow(testDaycare.id, supervisor.id, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(testDaycare2.id, supervisor.id, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(testDaycare.id, staff.id, UserRole.STAFF)
            tx.insertDaycareAclRow(testDaycare2.id, staff.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, staff.id, listOf(groupId1))

            tx.upsertOccupancyCoefficient(
                OccupancyCoefficientUpsert(testDaycare.id, staff.id, BigDecimal(7))
            )
            tx.upsertOccupancyCoefficient(
                OccupancyCoefficientUpsert(testDaycare2.id, staff.id, BigDecimal(7))
            )
        }
    }

    @Test
    fun `Attendances can be added to multiple units`() {
        upsertDailyStaffAttendances(
            unitId = testDaycare.id,
            groupId = groupId1,
            arrived = now.minusHours(3),
            departed = now.minusHours(2),
            hasStaffOccupancyEffect = true
        )
        upsertDailyStaffAttendances(
            unitId = testDaycare2.id,
            groupId = groupId2,
            arrived = now.minusHours(1),
            departed = null,
            hasStaffOccupancyEffect = false
        )

        val unit1Attendances = getAttendances(testDaycare.id)
        assertEquals(1, unit1Attendances.staff.size)
        assertEquals(0, unit1Attendances.extraAttendances.size)
        unit1Attendances.staff.first().let { attendance ->
            assertEquals(staff.id, attendance.employeeId)
            assertEquals(listOf(groupId1), attendance.groups)
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
                    attendanceEntry.occupancyCoefficient.stripTrailingZeros()
                )
            }
        }

        val unit2Attendances = getAttendances(testDaycare2.id)
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
                    attendanceEntry.occupancyCoefficient.stripTrailingZeros()
                )
            }
        }
    }

    @Test
    fun attendanceCannotBeInFuture() {
        assertThrows<BadRequest> {
            upsertDailyStaffAttendances(
                testDaycare.id,
                groupId1,
                now.minusHours(3),
                now.plusMinutes(1)
            )
        }
        assertThrows<BadRequest> {
            upsertDailyStaffAttendances(testDaycare.id, groupId1, now.plusMinutes(3), null)
        }
        assertThrows<BadRequest> {
            upsertDailyStaffAttendances(
                testDaycare.id,
                groupId1,
                now.plusHours(22),
                now.plusHours(23)
            )
        }
        upsertDailyStaffAttendances(testDaycare.id, groupId1, now.minusHours(7), now.minusHours(3))
        upsertDailyStaffAttendances(testDaycare.id, groupId1, now.minusHours(2), null)

        assertThrows<BadRequest> {
            upsertDailyExternalAttendances(
                testDaycare.id,
                groupId1,
                now.minusHours(3),
                now.plusMinutes(1)
            )
        }
        assertThrows<BadRequest> {
            upsertDailyExternalAttendances(testDaycare.id, groupId1, now.plusMinutes(3), null)
        }
        assertThrows<BadRequest> {
            upsertDailyExternalAttendances(
                testDaycare.id,
                groupId1,
                now.plusHours(22),
                now.plusHours(23)
            )
        }
        upsertDailyExternalAttendances(
            testDaycare.id,
            groupId1,
            now.minusHours(7),
            now.minusHours(3)
        )
        upsertDailyExternalAttendances(testDaycare.id, groupId1, now.minusHours(2), null)
    }

    @Test
    fun `Attendances without group can be deleted`() {
        realtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances(
            dbInstance(),
            unitSupervisor,
            MockEvakaClock(now),
            RealtimeStaffAttendanceController.StaffAttendanceBody(
                unitId = testDaycare.id,
                date = now.toLocalDate(),
                employeeId = testDecisionMaker_2.id,
                entries =
                    listOf(
                        RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                            id = null,
                            groupId = null,
                            arrived = now.minusHours(3),
                            departed = now.minusHours(2),
                            type = StaffAttendanceType.TRAINING,
                            hasStaffOccupancyEffect = false
                        )
                    )
            )
        )

        getAttendances(testDaycare.id).staff.first().let { attendance ->
            assertEquals(staff.id, attendance.employeeId)
            assertEquals(listOf(groupId1), attendance.groups)
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
                unitId = testDaycare.id,
                date = now.toLocalDate(),
                employeeId = testDecisionMaker_2.id,
                entries = emptyList()
            )
        )
        assertEquals(
            0,
            getAttendances(testDaycare.id)
                .staff
                .get(0)
                .attendances.size
        )
    }

    private fun getAttendances(unitId: DaycareId): StaffAttendanceResponse =
        realtimeStaffAttendanceController.getRealtimeStaffAttendances(
            dbInstance(),
            unitSupervisor,
            MockEvakaClock(now),
            unitId,
            now.toLocalDate(),
            now.toLocalDate()
        )

    private fun upsertDailyStaffAttendances(
        unitId: DaycareId,
        groupId: GroupId,
        arrived: HelsinkiDateTime,
        departed: HelsinkiDateTime?,
        hasStaffOccupancyEffect: Boolean = true
    ) {
        realtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances(
            dbInstance(),
            unitSupervisor,
            MockEvakaClock(now),
            RealtimeStaffAttendanceController.StaffAttendanceBody(
                unitId = unitId,
                date = now.toLocalDate(),
                employeeId = testDecisionMaker_2.id,
                entries =
                    listOf(
                        RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                            id = null,
                            groupId = groupId,
                            arrived = arrived,
                            departed = departed,
                            type = StaffAttendanceType.PRESENT,
                            hasStaffOccupancyEffect = hasStaffOccupancyEffect
                        )
                    )
            )
        )
    }

    private fun upsertDailyExternalAttendances(
        unitId: DaycareId,
        groupId: GroupId,
        arrived: HelsinkiDateTime,
        departed: HelsinkiDateTime?
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
                            departed = departed
                        )
                    )
            )
        )
    }
}
