// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevDailyServiceTimes
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.isWeekend
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testChild_7
import fi.espoo.evaka.testChild_8
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.unitSupervisorOfTestDaycare
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class AttendanceReservationReportByChildTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var attendanceReservationReportController:
        AttendanceReservationReportController

    private val admin =
        AuthenticatedUser.Employee(unitSupervisorOfTestDaycare.id, setOf(UserRole.ADMIN))

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insertServiceNeedOptions()

            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(
                unitSupervisorOfTestDaycare,
                mapOf(testDaycare.id to UserRole.UNIT_SUPERVISOR),
            )
            listOf(
                    testChild_1,
                    testChild_2,
                    testChild_3,
                    testChild_4,
                    testChild_5,
                    testChild_6,
                    testChild_7,
                    testChild_8,
                )
                .forEach { tx.insert(it, DevPersonType.CHILD) }
        }
    }

    @Test
    fun `returns only unit's operation days`() {
        val startDate = LocalDate.of(2022, 9, 1) // Thu
        val endDate = LocalDate.of(2022, 9, 6) // Tue
        val weekDays =
            FiniteDateRange(startDate, endDate).dates().filter { !it.isWeekend() }.toList()
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            FiniteDateRange(startDate, endDate).dates().forEach { date ->
                tx.insert(
                    DevReservation(
                        childId = testChild_1.id,
                        date = date,
                        startTime = LocalTime.of(8, 15),
                        endTime = LocalTime.of(15, 48),
                        createdBy = admin.evakaUserId,
                    )
                )
            }
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = null,
                    groupName = null,
                    items = weekDays.map { reportItem(testChild_1, it) },
                )
            ),
            getReport(startDate, endDate),
        )
    }

    @Test
    fun `end date is inclusive`() {
        val date = LocalDate.of(2022, 9, 2)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = null,
                    groupName = null,
                    items = listOf(reportItem(testChild_1, date)),
                )
            ),
            getReport(date, date),
        )
    }

    @Test
    fun `reservation with no times is ignored`() {
        val date = LocalDate.of(2022, 9, 2)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = null,
                    endTime = null,
                    createdBy = admin.evakaUserId,
                )
            )
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = null,
                    groupName = null,
                    items = listOf(reportItem(testChild_1, date, reservation = null)),
                )
            ),
            getReport(date, date),
        )
    }

    @Test
    fun `child without placement is not included`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = null,
                    groupName = null,
                    items = emptyList(),
                )
            ),
            getReport(date, date),
        )
    }

    @Test
    fun `child with placement to a different unit is not included`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare2.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = null,
                    groupName = null,
                    items = emptyList(),
                )
            ),
            getReport(date, date),
        )
    }

    @Test
    fun `backup care is supported`() {
        val date = LocalDate.of(2020, 5, 28)
        val group = DevDaycareGroup(daycareId = testDaycare2.id, startDate = date, endDate = date)
        db.transaction { tx ->
            tx.insert(group)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId =
                        tx.insert(
                            DevPlacement(
                                childId = testChild_1.id,
                                unitId = testDaycare2.id,
                                startDate = date,
                                endDate = date,
                            )
                        ),
                    daycareGroupId = group.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    groupId = null,
                    period = FiniteDateRange(date, date),
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevPlacement(
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_2.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = null,
                    groupName = null,
                    items =
                        listOf(
                                reportItem(testChild_1, date, backupCare = true),
                                reportItem(testChild_2, date, backupCare = false),
                            )
                            .sortedWith(compareBy({ it.childLastName }, { it.childFirstName })),
                )
            ),
            getReport(date, date),
        )
    }

    @Test
    fun `multiple children is supported`() {
        val date = LocalDate.of(2020, 5, 28)
        val children =
            listOf(
                testChild_1,
                testChild_2,
                testChild_3,
                testChild_4,
                testChild_5,
                testChild_6,
                testChild_7,
                testChild_8,
            )
        db.transaction { tx ->
            children.forEach { testChild ->
                tx.insert(
                    DevPlacement(
                        childId = testChild.id,
                        unitId = testDaycare.id,
                        startDate = date,
                        endDate = date,
                    )
                )
                tx.insert(
                    DevReservation(
                        childId = testChild.id,
                        date = date,
                        startTime = LocalTime.of(8, 15),
                        endTime = LocalTime.of(15, 48),
                        createdBy = admin.evakaUserId,
                    )
                )
            }
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = null,
                    groupName = null,
                    items =
                        children
                            .map { reportItem(it, date) }
                            .sortedWith(compareBy({ it.childLastName }, { it.childFirstName })),
                )
            ),
            getReport(date, date),
        )
    }

    @Test
    fun `group ids filter works`() {
        val date = LocalDate.of(2020, 5, 28)
        val group1 = DevDaycareGroup(daycareId = testDaycare.id, name = "Testiläiset 1")
        val group2 = DevDaycareGroup(daycareId = testDaycare.id, name = "Testiläiset 2")
        db.transaction { tx ->
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId =
                        tx.insert(
                            DevPlacement(
                                childId = testChild_1.id,
                                unitId = testDaycare.id,
                                startDate = date,
                                endDate = date,
                            )
                        ),
                    daycareGroupId = group1.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId =
                        tx.insert(
                            DevPlacement(
                                childId = testChild_2.id,
                                unitId = testDaycare.id,
                                startDate = date,
                                endDate = date,
                            )
                        ),
                    daycareGroupId = group1.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_2.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId =
                        tx.insert(
                            DevPlacement(
                                childId = testChild_3.id,
                                unitId = testDaycare.id,
                                startDate = date,
                                endDate = date,
                            )
                        ),
                    daycareGroupId = group2.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_3.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevPlacement(
                    childId = testChild_4.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_4.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = group1.id,
                    groupName = group1.name,
                    items =
                        listOf(testChild_1, testChild_2)
                            .map { reportItem(it, date) }
                            .sortedWith(compareBy({ it.childLastName }, { it.childFirstName })),
                ),
                AttendanceReservationReportByChildGroup(
                    groupId = group2.id,
                    groupName = group2.name,
                    items = listOf(reportItem(testChild_3, date)),
                ),
            ),
            getReport(date, date, listOf(group1.id, group2.id)),
        )
    }

    @Test
    fun `group placement without group ids filter works`() {
        val date = LocalDate.of(2020, 5, 28)
        val group = DevDaycareGroup(daycareId = testDaycare.id, startDate = date, endDate = date)
        db.transaction { tx ->
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = date,
                        endDate = date,
                    )
                )
            tx.insert(group)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = group.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = null,
                    groupName = null,
                    items = listOf(reportItem(testChild_1, date)),
                )
            ),
            getReport(date, date),
        )
    }

    @Test
    fun `multiple reservations are supported`() {
        val date = LocalDate.of(2022, 9, 2)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(12, 0),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(19, 0),
                    endTime = LocalTime.of(23, 59),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = null,
                    groupName = null,
                    items =
                        listOf(
                            reportItem(
                                testChild_1,
                                date,
                                reservation = TimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                            ),
                            reportItem(
                                testChild_1,
                                date,
                                reservation = TimeRange(LocalTime.of(19, 0), LocalTime.of(23, 59)),
                            ),
                        ),
                )
            ),
            getReport(date, date),
        )
    }

    @Test
    fun `absences are supported`() {
        val startDate = LocalDate.of(2022, 10, 24)
        val endDate = LocalDate.of(2022, 10, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            FiniteDateRange(startDate, endDate).dates().forEach { date ->
                tx.insert(
                    DevReservation(
                        childId = testChild_1.id,
                        date = date,
                        startTime = LocalTime.of(8, 15),
                        endTime = LocalTime.of(15, 48),
                        createdBy = admin.evakaUserId,
                    )
                )
            }
            tx.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = LocalDate.of(2022, 10, 27),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = LocalDate.of(2022, 10, 28),
                    absenceType = AbsenceType.PARENTLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = null,
                    groupName = null,
                    items =
                        listOf(
                            reportItem(testChild_1, startDate),
                            reportItem(testChild_1, startDate.plusDays(1)),
                            reportItem(testChild_1, startDate.plusDays(2)),
                            reportItem(
                                testChild_1,
                                startDate.plusDays(3),
                                reservation = null,
                                fullDayAbsence = true,
                            ),
                            reportItem(
                                testChild_1,
                                startDate.plusDays(4),
                                reservation = null,
                                fullDayAbsence = true,
                            ),
                        ),
                )
            ),
            getReport(startDate, endDate),
        )
    }

    @Test
    fun `preschool daycare requires 2 absences for full-day absence`() {
        val startDate = LocalDate.of(2022, 10, 24)
        val endDate = LocalDate.of(2022, 10, 25)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // full-day absence
            tx.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = startDate,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = startDate,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )

            // not a full-day absence
            tx.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = endDate,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = null,
                    groupName = null,
                    items =
                        listOf(
                            reportItem(testChild_1, startDate, reservation = null, fullDayAbsence = true),
                            reportItem(testChild_1, endDate, reservation = null, fullDayAbsence = false),
                        ),
                )
            ),
            getReport(startDate, endDate),
        )
    }

    @Test
    fun `daily service times are returned if exists and there is no reservation for the day`() {
        val startDate = LocalDate.of(2022, 10, 24)
        val endDate = LocalDate.of(2022, 10, 26)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // 8-16 every day
            tx.insert(
                DevDailyServiceTimes(
                    childId = testChild_1.id,
                    validityPeriod = DateRange(startDate, endDate),
                )
            )

            // Reservation on the second day 9:00 - 15:00
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = startDate.plusDays(1),
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(15, 0),
                    createdBy = admin.evakaUserId,
                )
            )

            // Absence on the third day
            tx.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = endDate,
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        assertEquals(
            listOf(
                AttendanceReservationReportByChildGroup(
                    groupId = null,
                    groupName = null,
                    items =
                        listOf(
                            reportItem(
                                testChild_1,
                                startDate,
                                reservation = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                            ),
                            reportItem(
                                testChild_1,
                                startDate.plusDays(1),
                                reservation = TimeRange(LocalTime.of(9, 0), LocalTime.of(15, 0)),
                            ),
                            reportItem(
                                testChild_1,
                                startDate.plusDays(2),
                                reservation = null,
                                fullDayAbsence = true,
                            ),
                        ),
                )
            ),
            getReport(startDate, endDate),
        )
    }

    private fun getReport(
        startDate: LocalDate,
        endDate: LocalDate,
        groupIds: List<GroupId> = emptyList(),
    ) =
        attendanceReservationReportController.getAttendanceReservationReportByChild(
            dbInstance(),
            RealEvakaClock(),
            admin,
            AttendanceReservationReportController.AttendanceReservationReportByChildBody(
                FiniteDateRange(startDate, endDate),
                testDaycare.id,
                groupIds,
            ),
        )

    private fun reportItem(
        child: DevPerson,
        date: LocalDate,
        reservation: TimeRange? = TimeRange(LocalTime.of(8, 15), LocalTime.of(15, 48)),
        fullDayAbsence: Boolean = false,
        backupCare: Boolean = false,
    ) =
        AttendanceReservationReportByChildItem(
            date,
            child.id,
            child.lastName,
            child.firstName,
            reservation,
            fullDayAbsence,
            backupCare,
        )
}
