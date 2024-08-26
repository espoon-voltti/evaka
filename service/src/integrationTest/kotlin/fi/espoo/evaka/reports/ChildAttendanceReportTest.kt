// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChildAttendance
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeInterval
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ChildAttendanceReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var childAttendanceReportController: ChildAttendanceReportController

    private val today = LocalDate.of(2024, 3, 1)
    private val range = FiniteDateRange(today.minusDays(2), today.plusDays(1))
    private val now = HelsinkiDateTime.of(today, LocalTime.of(15, 0))

    private final val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private final val area = DevCareArea()
    private final val daycare = DevDaycare(areaId = area.id)
    private final val child = DevPerson()
    private final val placement =
        DevPlacement(
            childId = child.id,
            unitId = daycare.id,
            startDate = range.start,
            endDate = range.end,
        )

    @Test
    fun `returns correct data`() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)

            tx.insert(
                DevChildAttendance(
                    childId = child.id,
                    unitId = daycare.id,
                    date = today.minusDays(1),
                    arrived = LocalTime.of(9, 0),
                    departed = LocalTime.of(14, 0),
                )
            )
            tx.insert(
                DevChildAttendance(
                    childId = child.id,
                    unitId = daycare.id,
                    date = today.minusDays(1),
                    arrived = LocalTime.of(15, 0),
                    departed = LocalTime.of(18, 0),
                )
            )
            tx.insert(
                DevChildAttendance(
                    childId = child.id,
                    unitId = daycare.id,
                    date = today,
                    arrived = LocalTime.of(9, 0),
                    departed = null,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child.id,
                    date = today.minusDays(1),
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(18, 0),
                    createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child.id,
                    date = today,
                    startTime = null,
                    endTime = null,
                    createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = today.minusDays(2),
                    absenceType = AbsenceType.PLANNED_ABSENCE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = today.minusDays(2),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )
        }

        val response =
            childAttendanceReportController.getChildAttendanceReport(
                dbInstance(),
                admin.user,
                MockEvakaClock(now),
                child.id,
                range.start,
                range.end,
            )

        assertEquals(
            listOf(
                ChildAttendanceReportRow(
                    date = today.minusDays(2),
                    reservations = emptyList(),
                    attendances = emptyList(),
                    billableAbsence = AbsenceType.PLANNED_ABSENCE,
                    nonbillableAbsence = AbsenceType.SICKLEAVE,
                ),
                ChildAttendanceReportRow(
                    date = today.minusDays(1),
                    reservations = listOf(TimeRange(LocalTime.of(9, 0), LocalTime.of(18, 0))),
                    attendances =
                        listOf(
                            TimeInterval(LocalTime.of(9, 0), LocalTime.of(14, 0)),
                            TimeInterval(LocalTime.of(15, 0), LocalTime.of(18, 0)),
                        ),
                    billableAbsence = null,
                    nonbillableAbsence = null,
                ),
                ChildAttendanceReportRow(
                    date = today,
                    reservations = emptyList(),
                    attendances = listOf(TimeInterval(LocalTime.of(9, 0), null)),
                    billableAbsence = null,
                    nonbillableAbsence = null,
                ),
                ChildAttendanceReportRow(
                    date = today.plusDays(1),
                    reservations = emptyList(),
                    attendances = emptyList(),
                    billableAbsence = null,
                    nonbillableAbsence = null,
                ),
            ),
            response,
        )
    }
}
