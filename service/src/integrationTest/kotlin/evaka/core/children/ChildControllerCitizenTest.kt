// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.children

import evaka.core.CitizenCalendarEnv
import evaka.core.FullApplicationTest
import evaka.core.absence.AbsenceCategory
import evaka.core.absence.AbsenceType
import evaka.core.pis.service.insertGuardian
import evaka.core.placement.PlacementType
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.dev.DevAbsence
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.noopTracer
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import evaka.instance.espoo.EspooActionRuleMapping
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ChildControllerCitizenTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var childControllerCitizen: ChildControllerCitizen
    @Autowired private lateinit var citizenCalendarEnv: CitizenCalendarEnv

    @Test
    fun `getChildAttendanceSummary placement`() {
        val (guardianId, childId) =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insertGuardian(guardianId = guardianId, childId = childId)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = LocalDate.of(2023, 8, 1),
                        endDate = LocalDate.of(2023, 9, 17),
                    )
                )
                Pair(guardianId, childId)
            }

        assertThat(
                childControllerCitizen.getChildAttendanceSummary(
                    dbInstance(),
                    AuthenticatedUser.Citizen(guardianId, CitizenAuthLevel.WEAK),
                    MockEvakaClock(
                        HelsinkiDateTime.of(LocalDate.of(2023, 9, 11), LocalTime.of(8, 23))
                    ),
                    childId,
                    YearMonth.of(2023, 8),
                )
            )
            .isEqualTo(AttendanceSummary(attendanceDays = 23))
        assertThat(
                childControllerCitizen.getChildAttendanceSummary(
                    dbInstance(),
                    AuthenticatedUser.Citizen(guardianId, CitizenAuthLevel.WEAK),
                    MockEvakaClock(
                        HelsinkiDateTime.of(LocalDate.of(2023, 9, 11), LocalTime.of(8, 23))
                    ),
                    childId,
                    YearMonth.of(2023, 9),
                )
            )
            .isEqualTo(AttendanceSummary(attendanceDays = 11))
        assertThat(
                childControllerCitizen.getChildAttendanceSummary(
                    dbInstance(),
                    AuthenticatedUser.Citizen(guardianId, CitizenAuthLevel.WEAK),
                    MockEvakaClock(
                        HelsinkiDateTime.of(LocalDate.of(2023, 9, 11), LocalTime.of(8, 23))
                    ),
                    childId,
                    YearMonth.of(2023, 10),
                )
            )
            .isEqualTo(AttendanceSummary(attendanceDays = 0))
    }

    @Test
    fun `getChildAttendanceSummary planned absence`() {
        val (guardianId, childId) =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insertGuardian(guardianId = guardianId, childId = childId)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = LocalDate.of(2023, 9, 1),
                        endDate = LocalDate.of(2023, 9, 30),
                    )
                )
                tx.insert(
                    DevAbsence(
                        childId = childId,
                        date = LocalDate.of(2023, 9, 11),
                        absenceType = AbsenceType.PLANNED_ABSENCE,
                        absenceCategory = AbsenceCategory.BILLABLE,
                    )
                )
                Pair(guardianId, childId)
            }

        assertThat(
                childControllerCitizen.getChildAttendanceSummary(
                    dbInstance(),
                    AuthenticatedUser.Citizen(guardianId, CitizenAuthLevel.WEAK),
                    MockEvakaClock(
                        HelsinkiDateTime.of(LocalDate.of(2023, 9, 11), LocalTime.of(8, 23))
                    ),
                    childId,
                    YearMonth.of(2023, 9),
                )
            )
            .isEqualTo(AttendanceSummary(attendanceDays = 20))
    }

    @Test
    fun `getChildAttendanceSummary planned absence outside placement`() {
        val (guardianId, childId) =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insertGuardian(guardianId = guardianId, childId = childId)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = LocalDate.of(2023, 9, 4),
                        endDate = LocalDate.of(2023, 9, 17),
                    )
                )
                tx.insert(
                    DevAbsence(
                        childId = childId,
                        date = LocalDate.of(2023, 9, 18),
                        absenceType = AbsenceType.PLANNED_ABSENCE,
                        absenceCategory = AbsenceCategory.BILLABLE,
                    )
                )
                Pair(guardianId, childId)
            }

        assertThat(
                childControllerCitizen.getChildAttendanceSummary(
                    dbInstance(),
                    AuthenticatedUser.Citizen(guardianId, CitizenAuthLevel.WEAK),
                    MockEvakaClock(
                        HelsinkiDateTime.of(LocalDate.of(2023, 9, 11), LocalTime.of(8, 23))
                    ),
                    childId,
                    YearMonth.of(2023, 9),
                )
            )
            .isEqualTo(AttendanceSummary(attendanceDays = 10))
    }

    @Test
    fun `getChildAttendanceSummary unknown absence`() {
        val (guardianId, childId) =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insertGuardian(guardianId = guardianId, childId = childId)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = LocalDate.of(2023, 9, 1),
                        endDate = LocalDate.of(2023, 9, 30),
                    )
                )
                tx.insert(
                    DevAbsence(
                        childId = childId,
                        date = LocalDate.of(2023, 9, 11),
                        absenceType = AbsenceType.UNKNOWN_ABSENCE,
                        absenceCategory = AbsenceCategory.BILLABLE,
                    )
                )
                Pair(guardianId, childId)
            }

        assertThat(
                childControllerCitizen.getChildAttendanceSummary(
                    dbInstance(),
                    AuthenticatedUser.Citizen(guardianId, CitizenAuthLevel.WEAK),
                    MockEvakaClock(
                        HelsinkiDateTime.of(LocalDate.of(2023, 9, 11), LocalTime.of(8, 23))
                    ),
                    childId,
                    YearMonth.of(2023, 9),
                )
            )
            .isEqualTo(AttendanceSummary(attendanceDays = 21))
    }

    @Test
    fun `getChildAttendanceSummary holiday`() {
        val (guardianId, childId) =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insertGuardian(guardianId = guardianId, childId = childId)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = LocalDate.of(2023, 5, 1),
                        endDate = LocalDate.of(2023, 5, 31),
                    )
                )
                Pair(guardianId, childId)
            }

        // 05/2023: 23 weekdays minus 2 holidays
        assertThat(
                childControllerCitizen.getChildAttendanceSummary(
                    dbInstance(),
                    AuthenticatedUser.Citizen(guardianId, CitizenAuthLevel.WEAK),
                    MockEvakaClock(
                        HelsinkiDateTime.of(LocalDate.of(2023, 5, 11), LocalTime.of(8, 23))
                    ),
                    childId,
                    YearMonth.of(2023, 5),
                )
            )
            .isEqualTo(AttendanceSummary(attendanceDays = 21))
    }

    @Test
    fun `getChildAttendanceSummary forbidden`() {
        val (_, childId) =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insertGuardian(guardianId = guardianId, childId = childId)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = LocalDate.of(2023, 9, 1),
                        endDate = LocalDate.of(2023, 9, 30),
                    )
                )
                Pair(guardianId, childId)
            }

        assertThrows<Forbidden> {
            childControllerCitizen.getChildAttendanceSummary(
                dbInstance(),
                AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.WEAK),
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2023, 9, 11), LocalTime.of(8, 23))),
                childId,
                YearMonth.of(2023, 9),
            )
        }
    }

    @Test
    fun `getChildren contains child info and default permitted actions`() {
        val (guardianId, childId, unitId) =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insertGuardian(guardianId = guardianId, childId = childId)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = LocalDate.of(2023, 8, 1),
                        endDate = LocalDate.of(2023, 9, 17),
                    )
                )
                Triple(guardianId, childId, unitId)
            }
        val childAndPermittedActions =
            ChildAndPermittedActions(
                id = childId,
                firstName = "Test",
                preferredName = "",
                lastName = "Person",
                duplicateOf = null,
                imageId = null,
                group = null,
                unit = null,
                upcomingPlacementType = PlacementType.DAYCARE,
                upcomingPlacementStartDate = LocalDate.of(2023, 8, 1),
                upcomingPlacementIsCalendarOpen = true,
                upcomingPlacementUnit = Unit(id = unitId, name = "Test Daycare"),
                permittedActions =
                    setOf(
                        Action.Citizen.Child.CREATE_ABSENCE,
                        Action.Citizen.Child.CREATE_ABSENCE_APPLICATION,
                        Action.Citizen.Child.READ_ABSENCE_APPLICATIONS,
                        Action.Citizen.Child.CREATE_HOLIDAY_ABSENCE,
                        Action.Citizen.Child.CREATE_RESERVATION,
                        Action.Citizen.Child.READ_SERVICE_NEEDS,
                        Action.Citizen.Child.READ_ATTENDANCE_SUMMARY,
                        Action.Citizen.Child.CREATE_CALENDAR_EVENT_TIME_RESERVATION,
                        Action.Citizen.Child.READ_SERVICE_APPLICATIONS,
                    ),
                serviceApplicationCreationPossible = false,
                absenceApplicationCreationPossible = false,
            )
        assertThat(
                childControllerCitizen.getChildren(
                    dbInstance(),
                    AuthenticatedUser.Citizen(guardianId, CitizenAuthLevel.WEAK),
                    MockEvakaClock(
                        HelsinkiDateTime.of(LocalDate.of(2023, 9, 11), LocalTime.of(8, 23))
                    ),
                )
            )
            .isEqualTo(listOf(childAndPermittedActions))
    }

    @Test
    fun `getChildren permitted actions contains READ_DAILY_SERVICE_TIMES in Espoo`() {
        val guardianId =
            db.transaction { tx ->
                val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insertGuardian(guardianId = guardianId, childId = childId)
                guardianId
            }

        val espooChildControllerCitizen =
            ChildControllerCitizen(
                AccessControl(EspooActionRuleMapping(), noopTracer()),
                citizenCalendarEnv = citizenCalendarEnv,
            )
        val result =
            espooChildControllerCitizen.getChildren(
                dbInstance(),
                AuthenticatedUser.Citizen(guardianId, CitizenAuthLevel.WEAK),
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2023, 9, 11), LocalTime.of(8, 23))),
            )

        assertThat(result.first().permittedActions)
            .contains(Action.Citizen.Child.READ_DAILY_SERVICE_TIMES)
    }
}
