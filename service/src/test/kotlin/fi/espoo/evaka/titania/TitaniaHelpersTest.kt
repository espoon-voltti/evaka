// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import fi.espoo.evaka.attendance.RawAttendance
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceRealtimeId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TitaniaHelpersTest {
    @Test
    fun `splitOvernight should return attendance is as when departed is null`() {
        val attendance = testAttendance.copy(departed = null)

        Assertions.assertThat(splitOvernight(attendance)).containsExactly(attendance)
    }

    @Test
    fun `splitOvernight should return attendance as is when departed is same date as arrived`() {
        val attendance =
            testAttendance.copy(
                departed = HelsinkiDateTime.of(LocalDate.of(2022, 6, 6), LocalTime.of(16, 27))
            )

        Assertions.assertThat(splitOvernight(attendance)).containsExactly(attendance)
    }

    @Test
    fun `splitOvernight should return multiple attendances when departed is different date as arrived`() {
        val attendance =
            testAttendance.copy(
                departed = HelsinkiDateTime.of(LocalDate.of(2022, 6, 8), LocalTime.of(10, 15))
            )

        Assertions
            .assertThat(splitOvernight(attendance))
            .containsExactly(
                attendance.copy(departed = attendance.arrived.atEndOfDay()),
                attendance.copy(
                    arrived = attendance.arrived.plusDays(1).atStartOfDay(),
                    departed = attendance.arrived.plusDays(1).atEndOfDay()
                ),
                attendance.copy(arrived = attendance.arrived.plusDays(2).atStartOfDay())
            )
    }
}

private val testAttendance =
    RawAttendance(
        id = StaffAttendanceRealtimeId(UUID.randomUUID()),
        groupId = GroupId(UUID.randomUUID()),
        arrived = HelsinkiDateTime.of(LocalDate.of(2022, 6, 6), LocalTime.of(7, 31)),
        departed = null,
        occupancyCoefficient = BigDecimal("1.21"),
        currentOccupancyCoefficient = null,
        type = StaffAttendanceType.PRESENT,
        employeeId = EmployeeId(UUID.randomUUID()),
        firstName = "First",
        lastName = "Last",
        departedAutomatically = false
    )
