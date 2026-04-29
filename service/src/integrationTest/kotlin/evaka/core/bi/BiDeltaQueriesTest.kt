// SPDX-FileCopyrightText: 2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

import evaka.core.PureJdbiTest
import evaka.core.absence.AbsenceCategory
import evaka.core.shared.AreaId
import evaka.core.shared.AttendanceReservationId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.GroupId
import evaka.core.shared.StaffAttendanceRealtimeId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevAbsence
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildAttendance
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevReservation
import evaka.core.shared.dev.DevStaffAttendance
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class BiDeltaQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val config =
        BiExportConfig(includePII = true, includeLegacyColumns = true, deltaWindowDays = 7)
    private val now = HelsinkiDateTime.now()
    private val past = now.minusDays((config.deltaWindowDays + 30).toLong())
    private val systemUser = AuthenticatedUser.SystemInternalUser.evakaUserId

    @Test
    fun `getAbsencesDelta includes recent rows and excludes old rows`() {
        val (includedId, excludedId) =
            db.transaction { tx ->
                val childId = tx.insertTestChild()
                val included =
                    tx.insert(
                        DevAbsence(
                            childId = childId,
                            date = LocalDate.of(2024, 1, 1),
                            absenceCategory = AbsenceCategory.BILLABLE,
                            modifiedAt = now,
                        )
                    )
                val excluded =
                    tx.insert(
                        DevAbsence(
                            childId = childId,
                            date = LocalDate.of(2024, 1, 2),
                            absenceCategory = AbsenceCategory.BILLABLE,
                            modifiedAt = past,
                        )
                    )
                included to excluded
            }

        val csv = runDelta(BiQueries.getAbsencesDelta)
        assertTrue(csv.contains(includedId.toString()))
        assertFalse(csv.contains(excludedId.toString()))
    }

    @Test
    fun `getChildAttendanceDelta includes recent rows and excludes old rows`() {
        val (includedRowDate, excludedRowDate, childId, unitId) =
            db.transaction { tx ->
                val unit = tx.insertTestDaycare()
                val child = tx.insertTestChild()
                val included = LocalDate.of(2024, 1, 1)
                val excluded = LocalDate.of(2024, 1, 2)
                tx.insert(
                    DevChildAttendance(
                        childId = child,
                        unitId = unit,
                        date = included,
                        arrived = LocalTime.of(8, 0),
                        departed = LocalTime.of(16, 0),
                        modifiedAt = now,
                    )
                )
                tx.insert(
                    DevChildAttendance(
                        childId = child,
                        unitId = unit,
                        date = excluded,
                        arrived = LocalTime.of(8, 0),
                        departed = LocalTime.of(16, 0),
                        modifiedAt = past,
                    )
                )
                ChildAttendanceFixture(included, excluded, child, unit)
            }

        val csv = runDelta(BiQueries.getChildAttendanceDelta)
        assertTrue(csv.contains(childId.toString()))
        assertTrue(csv.contains(unitId.toString()))
        assertTrue(csv.contains(includedRowDate.toString()))
        assertFalse(csv.contains(excludedRowDate.toString()))
    }

    @Test
    fun `getAttendanceReservationsDelta includes recent rows and excludes old rows`() {
        val (includedId, excludedId) =
            db.transaction { tx ->
                val childId = tx.insertTestChild()
                val included =
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 1, 1),
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(16, 0),
                            createdBy = systemUser,
                        )
                    )
                val excluded = AttendanceReservationId(UUID.randomUUID())
                tx.createUpdate {
                        sql(
                            """
INSERT INTO attendance_reservation (id, child_id, date, start_time, end_time, created_at, created_by, updated_at)
VALUES (${bind(excluded)}, ${bind(childId)}, ${bind(LocalDate.of(2024, 1, 2))}, ${bind(LocalTime.of(8, 0))}, ${bind(LocalTime.of(16, 0))}, ${bind(past)}, ${bind(systemUser)}, ${bind(past)})
"""
                        )
                    }
                    .execute()
                included to excluded
            }

        val csv = runDelta(BiQueries.getAttendanceReservationsDelta)
        assertTrue(csv.contains(includedId.toString()))
        assertFalse(csv.contains(excludedId.toString()))
    }

    @Test
    fun `getStaffAttendanceRealtimeDelta includes recent rows and excludes old rows`() {
        val (includedId, excludedId) =
            db.transaction { tx ->
                val groupId = tx.insertTestGroup()
                val employee = DevEmployee()
                tx.insert(employee)
                val included =
                    tx.insert(
                        DevStaffAttendance(
                            employeeId = employee.id,
                            groupId = groupId,
                            arrived = now.minusHours(2),
                            departed = now.minusHours(1),
                            modifiedAt = now,
                            modifiedBy = systemUser,
                        )
                    )
                val excluded = StaffAttendanceRealtimeId(UUID.randomUUID())
                tx.createUpdate {
                        sql(
                            """
INSERT INTO staff_attendance_realtime (
    id, employee_id, group_id, arrived, departed, occupancy_coefficient, type, departed_automatically,
    arrived_added_at, arrived_added_by, arrived_modified_at, arrived_modified_by,
    departed_added_at, departed_added_by, departed_modified_at, departed_modified_by,
    updated_at
)
VALUES (
    ${bind(excluded)}, ${bind(employee.id)}, ${bind(groupId)},
    ${bind(past)}, ${bind(past.plusHours(1))}, 7.0, 'PRESENT', false,
    ${bind(past)}, ${bind(systemUser)}, ${bind(past)}, ${bind(systemUser)},
    ${bind(past)}, ${bind(systemUser)}, ${bind(past)}, ${bind(systemUser)},
    ${bind(past)}
)
"""
                        )
                    }
                    .execute()
                included to excluded
            }

        val csv = runDelta(BiQueries.getStaffAttendanceRealtimeDelta)
        assertTrue(csv.contains(includedId.toString()))
        assertFalse(csv.contains(excludedId.toString()))
    }

    private data class ChildAttendanceFixture(
        val includedDate: LocalDate,
        val excludedDate: LocalDate,
        val childId: ChildId,
        val unitId: DaycareId,
    )

    private fun runDelta(query: BiQueries.CsvQuery): String = db.read { tx ->
        query(tx, config) { records -> records.joinToString("") }
    }

    private fun Database.Transaction.insertTestArea(): AreaId = insert(DevCareArea())

    private fun Database.Transaction.insertTestDaycare(): DaycareId =
        insert(DevDaycare(areaId = insertTestArea()))

    private fun Database.Transaction.insertTestGroup(): GroupId =
        insert(DevDaycareGroup(daycareId = insertTestDaycare()))

    private fun Database.Transaction.insertTestChild(): ChildId =
        insert(DevPerson(), DevPersonType.CHILD)
}
