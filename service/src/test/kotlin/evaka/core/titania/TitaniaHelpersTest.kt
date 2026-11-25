// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.titania

import evaka.core.attendance.RawAttendance
import evaka.core.attendance.StaffAttendanceType
import evaka.core.shared.EmployeeId
import evaka.core.shared.GroupId
import evaka.core.shared.StaffAttendanceRealtimeId
import evaka.core.shared.domain.DateTimeRange
import evaka.core.shared.domain.HelsinkiDateTime
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TitaniaHelpersTest {

    @Test
    fun `splitOvernight should return attendance is as when departed is null`() {
        val attendance = testAttendance.copy(period = DateTimeRange(testArrived, null))

        Assertions.assertThat(splitOvernight(attendance)).containsExactly(attendance)
    }

    @Test
    fun `splitOvernight should return attendance as is when departed is same date as arrived`() {
        val attendance =
            testAttendance.copy(
                period =
                    DateTimeRange(
                        testArrived,
                        HelsinkiDateTime.of(LocalDate.of(2022, 6, 6), LocalTime.of(16, 27)),
                    ),
            )

        Assertions.assertThat(splitOvernight(attendance)).containsExactly(attendance)
    }

    @Test
    fun `splitOvernight should return multiple attendances when departed is different date as arrived`() {
        val departed = HelsinkiDateTime.of(LocalDate.of(2022, 6, 8), LocalTime.of(10, 15))
        val attendance = testAttendance.copy(period = DateTimeRange(testArrived, departed))

        Assertions.assertThat(splitOvernight(attendance))
            .containsExactly(
                attendance.copy(period = DateTimeRange(testArrived, testArrived.atEndOfDay())),
                attendance.copy(
                    period =
                        DateTimeRange(
                            testArrived.plusDays(1).atStartOfDay(),
                            testArrived.plusDays(1).atEndOfDay(),
                        ),
                ),
                attendance.copy(
                    period = DateTimeRange(testArrived.plusDays(2).atStartOfDay(), departed),
                ),
            )
    }
}

private val testArrived = HelsinkiDateTime.of(LocalDate.of(2022, 6, 6), LocalTime.of(7, 31))

private val testAttendance =
    RawAttendance(
        id = StaffAttendanceRealtimeId(UUID.randomUUID()),
        groupId = GroupId(UUID.randomUUID()),
        period = DateTimeRange(testArrived, null),
        occupancyCoefficient = BigDecimal("1.21"),
        currentOccupancyCoefficient = null,
        type = StaffAttendanceType.PRESENT,
        employeeId = EmployeeId(UUID.randomUUID()),
        firstName = "First",
        lastName = "Last",
        departedAutomatically = false,
        arrivedAddedAt = null,
        arrivedAddedBy = null,
        arrivedModifiedAt = null,
        arrivedModifiedBy = null,
        departedAddedAt = null,
        departedAddedBy = null,
        departedModifiedAt = null,
        departedModifiedBy = null,
    )
