// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.espoo.EspooActionRuleMapping
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevHoliday
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import io.opentracing.noop.NoopTracerFactory
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
                        startDate = LocalDate.of(2023, 9, 1),
                        endDate = LocalDate.of(2023, 9, 30),
                    )
                )
                tx.insert(DevHoliday(date = LocalDate.of(2023, 9, 11), description = "holiday"))
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
                hasPedagogicalDocuments = false,
                hasCurriculums = false,
                permittedActions =
                    setOf(
                        Action.Citizen.Child.CREATE_ABSENCE,
                        Action.Citizen.Child.CREATE_HOLIDAY_ABSENCE,
                        Action.Citizen.Child.CREATE_RESERVATION,
                        Action.Citizen.Child.READ_SERVICE_NEEDS,
                        Action.Citizen.Child.READ_ATTENDANCE_SUMMARY,
                        Action.Citizen.Child.CREATE_CALENDAR_EVENT_TIME_RESERVATION,
                        Action.Citizen.Child.DELETE_CALENDAR_EVENT_TIME_RESERVATION,
                    ),
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
                AccessControl(EspooActionRuleMapping(), NoopTracerFactory.create())
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
