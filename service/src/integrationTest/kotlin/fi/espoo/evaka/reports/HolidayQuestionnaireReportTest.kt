// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.holidayperiod.QuestionnaireType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareAssistance
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevHolidayQuestionnaire
import fi.espoo.evaka.shared.dev.DevHolidayQuestionnaireAnswer
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevPreschoolAssistance
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.Translatable
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDefaultDaycare
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class HolidayQuestionnaireReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var holidayQuestionnaireReport: HolidayQuestionnaireReport

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adminLoginUser = AuthenticatedUser.Employee(admin.id, admin.roles)

    private val unitSupervisorA = DevEmployee(roles = setOf())

    private final val mockClock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 9, 9), LocalTime.of(12, 15)))
    private final val mockToday = mockClock.today()

    private val holidayQuestionnaire =
        DevHolidayQuestionnaire(
            type = QuestionnaireType.FIXED_PERIOD,
            periodOptions = listOf(FiniteDateRange(mockToday, mockToday.plusWeeks(1))),
            periodOptionLabel = Translatable("", "", ""),
            period = null,
            absenceTypeThreshold = null,
        )

    @Test
    fun `Unit supervisor can see own unit's report results`() {
        val testUnitData = initTestUnitData(mockToday)
        initTestPlacementData(
            monday = mockToday,
            daycare = testUnitData[0],
            altDaycare = testUnitData[2],
        )
        val results =
            holidayQuestionnaireReport.getHolidayQuestionnaireReport(
                dbInstance(),
                mockClock,
                unitSupervisorA.user,
                unitId = testUnitData[0].id,
                groupIds = emptySet(),
                questionnaireId = holidayQuestionnaire.id,
            )

        assertThat(results.isNotEmpty())
    }

    @Test
    fun `Admin can see report results`() {
        val testUnitData = initTestUnitData(mockToday)
        initTestPlacementData(mockToday, testUnitData[0], testUnitData[2])
        val results =
            holidayQuestionnaireReport.getHolidayQuestionnaireReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                unitId = testUnitData[0].id,
                groupIds = emptySet(),
                questionnaireId = holidayQuestionnaire.id,
            )
        assertThat(results.isNotEmpty())
    }

    @Test
    fun `Unit supervisor cannot see another unit's report results`() {
        val testUnitData = initTestUnitData(mockToday)
        initTestPlacementData(mockToday, testUnitData[0], testUnitData[2])
        assertThrows<Forbidden> {
            holidayQuestionnaireReport.getHolidayQuestionnaireReport(
                dbInstance(),
                mockClock,
                unitSupervisorA.user,
                unitId = testUnitData[1].id,
                groupIds = emptySet(),
                questionnaireId = holidayQuestionnaire.id,
            )
        }
    }

    @Test
    fun `Report returns all period days as per unit operation days`() {
        val testUnitData = initTestUnitData(mockToday)
        initTestPlacementData(mockToday, testUnitData[0], testUnitData[2])

        val reportResultsA =
            holidayQuestionnaireReport.getHolidayQuestionnaireReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                unitId = testUnitData[0].id,
                groupIds = emptySet(),
                questionnaireId = holidayQuestionnaire.id,
            )
        val periodDays = holidayQuestionnaire.periodOptions?.get(0)?.dates()?.toList()
        val periodDaysInA = periodDays?.filter { it.dayOfWeek < DayOfWeek.FRIDAY }
        val resultDaysA = reportResultsA.map { it.date }

        assertThat(resultDaysA).containsAll(periodDaysInA)

        val reportResultsB =
            holidayQuestionnaireReport.getHolidayQuestionnaireReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                unitId = testUnitData[1].id,
                groupIds = emptySet(),
                questionnaireId = holidayQuestionnaire.id,
            )

        val resultDaysB = reportResultsB.map { it.date }

        assertThat(resultDaysB).containsAll(periodDays)
    }

    @Test
    fun `Report returns correct attendance data for unit`() {
        val testUnitData = initTestUnitData(mockToday)
        val testData = initTestPlacementData(mockToday, testUnitData[0], testUnitData[2])

        val reportResultsByDate =
            holidayQuestionnaireReport
                .getHolidayQuestionnaireReport(
                    dbInstance(),
                    mockClock,
                    adminLoginUser,
                    unitId = testUnitData[0].id,
                    groupIds = emptySet(),
                    questionnaireId = holidayQuestionnaire.id,
                )
                .associateBy { it.date }

        val expectedMonday =
            HolidayReportRow(
                absentCount = 1,
                requiredStaff = 2,
                presentChildren =
                    testData.slice(1..3).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren =
                    testData.slice(1..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                noResponseChildren =
                    listOf(testData.last()).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                date = mockToday,
                presentOccupancyCoefficient = 7.5,
            )

        val expectedTuesday =
            HolidayReportRow(
                absentCount = 2,
                requiredStaff = 1,
                presentChildren =
                    testData.slice(2..3).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren =
                    listOf(testData[2]).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                noResponseChildren =
                    listOf(testData.last()).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                date = mockToday.plusDays(1),
                presentOccupancyCoefficient = 6.5,
            )

        val expectedWednesday =
            HolidayReportRow(
                absentCount = 3,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[3]).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren =
                    listOf(testData.last()).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                date = mockToday.plusDays(2),
                presentOccupancyCoefficient = 1.0,
            )

        listOf(expectedMonday, expectedTuesday, expectedWednesday).forEachIndexed { index, expected
            ->
            assertReportDay(
                expected,
                reportResultsByDate[mockToday.plusDays(index.toLong())]
                    ?: fail("${index + 1}. date not found"),
            )
        }
    }

    private data class TestDaycare(val id: DaycareId, val groups: Pair<GroupId, GroupId>)

    private fun initTestUnitData(monday: LocalDate): List<TestDaycare> {
        return db.transaction { tx ->
            val areaAId = tx.insert(DevCareArea(name = "Area A", shortName = "Area A"))
            val areaBId = tx.insert(DevCareArea(name = "Area B", shortName = "Area B"))

            val daycareAId =
                tx.insert(
                    DevDaycare(
                        name = "Daycare A",
                        areaId = areaAId,
                        openingDate = monday.minusDays(7),
                        type = setOf(CareType.CENTRE, CareType.PRESCHOOL),
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            val groupAAId =
                tx.insert(
                    DevDaycareGroup(
                        name = "Group AA",
                        startDate = monday.minusDays(7),
                        daycareId = daycareAId,
                    )
                )

            val groupABId =
                tx.insert(
                    DevDaycareGroup(
                        name = "Group AB",
                        startDate = monday.minusDays(7),
                        daycareId = daycareAId,
                    )
                )

            val daycareBId =
                tx.insert(
                    DevDaycare(
                        name = "Daycare B",
                        areaId = areaBId,
                        openingDate = monday.minusDays(7),
                        type = setOf(CareType.CENTRE),
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        shiftCareOperationTimes =
                            List(7) { TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)) },
                        shiftCareOpenOnHolidays = true,
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            val groupBAId =
                tx.insert(
                    DevDaycareGroup(
                        name = "Group BA",
                        startDate = monday.minusDays(7),
                        daycareId = daycareBId,
                    )
                )

            val groupBBId =
                tx.insert(
                    DevDaycareGroup(
                        name = "Group BB",
                        startDate = monday.minusDays(7),
                        daycareId = daycareBId,
                    )
                )

            val daycareCId =
                tx.insert(
                    DevDaycare(
                        name = "Daycare C",
                        areaId = areaAId,
                        openingDate = monday.minusMonths(1),
                        type = setOf(CareType.CENTRE),
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            val groupCAId =
                tx.insert(
                    DevDaycareGroup(
                        name = "Group CA",
                        startDate = monday.minusMonths(1),
                        daycareId = daycareCId,
                    )
                )

            val groupCBId =
                tx.insert(
                    DevDaycareGroup(
                        name = "Group CB",
                        startDate = monday.minusMonths(1),
                        daycareId = daycareCId,
                    )
                )

            listOf(
                TestDaycare(daycareAId, Pair(groupAAId, groupABId)),
                TestDaycare(daycareBId, Pair(groupBAId, groupBBId)),
                TestDaycare(daycareCId, Pair(groupCAId, groupCBId)),
            )
        }
    }

    private fun initTestPlacementData(
        monday: LocalDate,
        daycare: TestDaycare,
        altDaycare: TestDaycare,
    ): List<Pair<DevPerson, List<DevPlacement>>> {
        val tuesday = monday.plusDays(1)
        val wednesday = monday.plusDays(2)
        val thursday = monday.plusDays(3)
        val friday = monday.plusDays(4)
        val saturday = monday.plusDays(5)

        return db.transaction { tx ->
            tx.insertServiceNeedOptions()
            tx.insert(admin)

            tx.insert(unitSupervisorA)

            tx.insertDaycareAclRow(daycare.id, unitSupervisorA.id, UserRole.UNIT_SUPERVISOR)

            val testChildAapo =
                DevPerson(
                    dateOfBirth = monday.minusYears(2),
                    firstName = "Aapo",
                    lastName = "Aarnio",
                )
            tx.insert(testChildAapo, DevPersonType.CHILD)

            val testChildBertil =
                DevPerson(
                    dateOfBirth = monday.minusYears(4),
                    firstName = "Bertil",
                    lastName = "Becker",
                )
            tx.insert(testChildBertil, DevPersonType.CHILD)

            val testChildCecil =
                DevPerson(
                    dateOfBirth = monday.minusYears(4),
                    firstName = "Cecil",
                    lastName = "Cilliacus",
                )
            tx.insert(testChildCecil, DevPersonType.CHILD)

            val testChildDavid =
                DevPerson(
                    dateOfBirth = monday.minusYears(4),
                    firstName = "David",
                    lastName = "Davidson",
                )
            tx.insert(testChildDavid, DevPersonType.CHILD)

            val testChildVille =
                DevPerson(
                    dateOfBirth = monday.minusYears(4),
                    firstName = "Ville",
                    lastName = "Varahoidettava",
                )
            tx.insert(testChildVille, DevPersonType.CHILD)

            val testChildGunnar =
                DevPerson(
                    dateOfBirth = monday.minusYears(4),
                    firstName = "Gunnar",
                    lastName = "Groupless",
                )
            tx.insert(testChildGunnar, DevPersonType.CHILD)

            val defaultPlacementDuration =
                FiniteDateRange(monday.minusMonths(1), monday.plusMonths(5))

            // 5 children
            // A: < 3, no assistance                -> 1.75 coefficient
            // B, V: > 3, assistance without factor -> 1.00 coefficient
            // C: > 3, assistance factor of 5.50    -> 5.50 coefficient
            // D:                                   -> unanswered

            val placementA =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildAapo.id,
                    unitId = daycare.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementA)

            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementA.id,
                    daycareGroupId = daycare.groups.first,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )

            val placementB =
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChildBertil.id,
                    unitId = daycare.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementB)

            tx.insert(
                DevServiceNeed(
                    placementId = placementB.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = wednesday,
                    confirmedBy = admin.evakaUserId,
                    optionId = snDefaultDaycare.id,
                )
            )
            tx.insert(
                DevServiceNeed(
                    placementId = placementB.id,
                    startDate = thursday,
                    endDate = defaultPlacementDuration.end,
                    confirmedBy = admin.evakaUserId,
                    optionId = snDaycareFullDay35.id,
                )
            )

            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementB.id,
                    daycareGroupId = daycare.groups.first,
                    startDate = defaultPlacementDuration.start,
                    endDate = wednesday,
                )
            )

            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementB.id,
                    daycareGroupId = daycare.groups.second,
                    startDate = thursday,
                    endDate = defaultPlacementDuration.end,
                )
            )

            tx.insert(
                DevPreschoolAssistance(
                    childId = testChildBertil.id,
                    validDuring = FiniteDateRange(monday.minusMonths(2), tuesday),
                )
            )

            val placementC1 =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildCecil.id,
                    unitId = daycare.id,
                    startDate = monday.minusMonths(1),
                    endDate = wednesday,
                )
            tx.insert(placementC1)

            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementC1.id,
                    daycareGroupId = daycare.groups.first,
                    startDate = placementC1.startDate,
                    endDate = placementC1.endDate,
                )
            )

            val placementC2 =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildCecil.id,
                    unitId = daycare.id,
                    startDate = thursday,
                    endDate = thursday.plusMonths(1),
                )
            tx.insert(placementC2)

            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementC2.id,
                    daycareGroupId = daycare.groups.second,
                    startDate = placementC2.startDate,
                    endDate = placementC2.endDate,
                )
            )

            tx.insert(
                DevAssistanceFactor(
                    childId = testChildCecil.id,
                    validDuring = FiniteDateRange(monday, wednesday),
                    capacityFactor = 5.50,
                )
            )

            tx.insert(
                DevDaycareAssistance(
                    childId = testChildCecil.id,
                    validDuring = FiniteDateRange(monday.minusMonths(3), wednesday),
                )
            )

            val placementD =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildDavid.id,
                    unitId = daycare.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementD)

            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementD.id,
                    daycareGroupId = daycare.groups.first,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )

            tx.insert(
                DevPlacement(
                    unitId = altDaycare.id,
                    childId = testChildVille.id,
                    startDate = monday,
                    endDate = monday.plusDays(6),
                )
            )
            tx.insert(
                DevBackupCare(
                    period = FiniteDateRange(monday, saturday),
                    childId = testChildVille.id,
                    unitId = daycare.id,
                    groupId = daycare.groups.first,
                )
            )

            tx.insert(holidayQuestionnaire)

            tx.insert(
                DevHolidayQuestionnaireAnswer(
                    modifiedBy = unitSupervisorA.evakaUserId,
                    questionnaireId = holidayQuestionnaire.id,
                    childId = testChildAapo.id,
                    fixedPeriod = FiniteDateRange(monday, wednesday),
                )
            )

            tx.insert(
                DevHolidayQuestionnaireAnswer(
                    modifiedBy = unitSupervisorA.evakaUserId,
                    questionnaireId = holidayQuestionnaire.id,
                    childId = testChildBertil.id,
                    fixedPeriod = FiniteDateRange(tuesday, thursday),
                )
            )

            tx.insert(
                DevHolidayQuestionnaireAnswer(
                    modifiedBy = unitSupervisorA.evakaUserId,
                    questionnaireId = holidayQuestionnaire.id,
                    childId = testChildCecil.id,
                    fixedPeriod = FiniteDateRange(wednesday, friday),
                )
            )

            tx.insert(
                DevHolidayQuestionnaireAnswer(
                    modifiedBy = unitSupervisorA.evakaUserId,
                    questionnaireId = holidayQuestionnaire.id,
                    childId = testChildVille.id,
                    fixedPeriod = FiniteDateRange(thursday, saturday),
                )
            )

            listOf(
                Pair(testChildAapo, listOf(placementA)),
                Pair(testChildBertil, listOf(placementB)),
                Pair(testChildCecil, listOf(placementC1, placementC2)),
                Pair(testChildVille, emptyList()),
                Pair(testChildGunnar, emptyList()),
                Pair(testChildDavid, listOf(placementD)),
            )
        }
    }

    private fun assertReportDay(expected: HolidayReportRow, actual: HolidayReportRow) {
        assertEquals(expected.date, actual.date)
        assertEquals(expected.absentCount, actual.absentCount, "${actual.date}: absentCount")
        assertThat(actual.presentChildren)
            .describedAs("${actual.date}: presentChildren")
            .containsExactlyInAnyOrderElementsOf(expected.presentChildren)

        assertThat(actual.assistanceChildren)
            .describedAs("${actual.date}: assistanceChildren")
            .containsExactlyInAnyOrderElementsOf(expected.assistanceChildren)

        assertEquals(
            expected.presentOccupancyCoefficient,
            actual.presentOccupancyCoefficient,
            "${actual.date}: coefficientSum",
        )
        assertEquals(
            expected.requiredStaff,
            actual.requiredStaff,
            "${actual.date}: staffRequirement",
        )
        assertThat(actual.noResponseChildren)
            .describedAs("${actual.date}: noResponseChildren")
            .containsExactlyInAnyOrderElementsOf(expected.noResponseChildren)
    }
}
