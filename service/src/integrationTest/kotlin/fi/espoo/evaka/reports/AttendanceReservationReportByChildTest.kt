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
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDailyServiceTimes
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
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

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val child = DevPerson()

    @BeforeEach
    fun setup() {
        // val daycare2 = DevDaycare(areaId = area.id)
        db.transaction { tx ->
            tx.insertServiceNeedOptions()
            tx.insert(admin)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(child, DevPersonType.CHILD)
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
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            FiniteDateRange(startDate, endDate).dates().forEach { date ->
                tx.insert(
                    DevReservation(
                        childId = child.id,
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
                    items = weekDays.map { reportItem(child, it) },
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
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child.id,
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
                    items = listOf(reportItem(child, date)),
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
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child.id,
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
                    items = listOf(reportItem(child, date, reservation = null)),
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
                    childId = child.id,
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
        val daycare2 = DevDaycare(areaId = area.id)
        db.transaction { tx ->
            tx.insert(daycare2)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare2.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child.id,
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
        val daycare2 = DevDaycare(areaId = area.id)
        val group = DevDaycareGroup(daycareId = daycare2.id, startDate = date, endDate = date)
        val child2 = DevPerson(lastName = "Doe", firstName = "Jane")
        db.transaction { tx ->
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(daycare2)
            tx.insert(group)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId =
                        tx.insert(
                            DevPlacement(
                                childId = child.id,
                                unitId = daycare2.id,
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
                    childId = child.id,
                    unitId = daycare.id,
                    groupId = null,
                    period = FiniteDateRange(date, date),
                )
            )
            tx.insert(
                DevReservation(
                    childId = child.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child2.id,
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
                                reportItem(child, date, backupCare = true),
                                reportItem(child2, date, backupCare = false),
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
        val children = (1..8).map { i -> DevPerson(lastName = "Doe $i", firstName = "Jane") }
        db.transaction { tx ->
            children.forEach { child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = date,
                        endDate = date,
                    )
                )
                tx.insert(
                    DevReservation(
                        childId = child.id,
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
        val group1 = DevDaycareGroup(daycareId = daycare.id, name = "Testiläiset 1")
        val group2 = DevDaycareGroup(daycareId = daycare.id, name = "Testiläiset 2")
        val child2 = DevPerson()
        val child3 = DevPerson()
        val child4 = DevPerson()
        db.transaction { tx ->
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(child3, DevPersonType.CHILD)
            tx.insert(child4, DevPersonType.CHILD)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId =
                        tx.insert(
                            DevPlacement(
                                childId = child.id,
                                unitId = daycare.id,
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
                    childId = child.id,
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
                                childId = child2.id,
                                unitId = daycare.id,
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
                    childId = child2.id,
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
                                childId = child3.id,
                                unitId = daycare.id,
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
                    childId = child3.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child4.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child4.id,
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
                        listOf(child, child2)
                            .map { reportItem(it, date) }
                            .sortedWith(compareBy({ it.childLastName }, { it.childFirstName })),
                ),
                AttendanceReservationReportByChildGroup(
                    groupId = group2.id,
                    groupName = group2.name,
                    items = listOf(reportItem(child3, date)),
                ),
            ),
            getReport(date, date, listOf(group1.id, group2.id)),
        )
    }

    @Test
    fun `group placement without group ids filter works`() {
        val date = LocalDate.of(2020, 5, 28)
        val group = DevDaycareGroup(daycareId = daycare.id, startDate = date, endDate = date)
        db.transaction { tx ->
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
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
                    childId = child.id,
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
                    items = listOf(reportItem(child, date)),
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
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(12, 0),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child.id,
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
                                child,
                                date,
                                reservation = TimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                            ),
                            reportItem(
                                child,
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
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            FiniteDateRange(startDate, endDate).dates().forEach { date ->
                tx.insert(
                    DevReservation(
                        childId = child.id,
                        date = date,
                        startTime = LocalTime.of(8, 15),
                        endTime = LocalTime.of(15, 48),
                        createdBy = admin.evakaUserId,
                    )
                )
            }
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = LocalDate.of(2022, 10, 27),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = child.id,
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
                            reportItem(child, startDate),
                            reportItem(child, startDate.plusDays(1)),
                            reportItem(child, startDate.plusDays(2)),
                            reportItem(
                                child,
                                startDate.plusDays(3),
                                reservation = null,
                                fullDayAbsence = true,
                            ),
                            reportItem(
                                child,
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
                    childId = child.id,
                    unitId = daycare.id,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // full-day absence
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = startDate,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = startDate,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )

            // not a full-day absence
            tx.insert(
                DevAbsence(
                    childId = child.id,
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
                            reportItem(child, startDate, reservation = null, fullDayAbsence = true),
                            reportItem(child, endDate, reservation = null, fullDayAbsence = false),
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
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // 8-16 every day
            tx.insert(
                DevDailyServiceTimes(
                    childId = child.id,
                    validityPeriod = DateRange(startDate, endDate),
                )
            )

            // Reservation on the second day 9:00 - 15:00
            tx.insert(
                DevReservation(
                    childId = child.id,
                    date = startDate.plusDays(1),
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(15, 0),
                    createdBy = admin.evakaUserId,
                )
            )

            // Absence on the third day
            tx.insert(
                DevAbsence(
                    childId = child.id,
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
                                child,
                                startDate,
                                reservation = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                            ),
                            reportItem(
                                child,
                                startDate.plusDays(1),
                                reservation = TimeRange(LocalTime.of(9, 0), LocalTime.of(15, 0)),
                            ),
                            reportItem(
                                child,
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
            admin.user,
            AttendanceReservationReportController.AttendanceReservationReportByChildBody(
                FiniteDateRange(startDate, endDate),
                daycare.id,
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
            child.dateOfBirth,
            reservation,
            fullDayAbsence,
            backupCare,
        )
}
