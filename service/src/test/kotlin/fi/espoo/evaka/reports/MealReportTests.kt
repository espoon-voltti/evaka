// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.reports

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.ChildServiceNeedInfo
import fi.espoo.evaka.daycare.DaycareInfo
import fi.espoo.evaka.daycare.DaycareMealtimes
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.mealintegration.DefaultMealTypeMapper
import fi.espoo.evaka.mealintegration.MealType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.ChildData
import fi.espoo.evaka.reservations.ReservationResponse
import fi.espoo.evaka.reservations.UnitAttendanceReservations
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PreschoolTermId
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.specialdiet.SpecialDiet
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MealReportTests {
    @Test
    fun `mealReportData should return no meals for absent child`() {
        val testDate = LocalDate.of(2023, 4, 15)
        val childInfo =
            listOf(
                MealReportChildInfo(
                    placementType = PlacementType.DAYCARE,
                    firstName = "John",
                    lastName = "Doe",
                    reservations = listOf(), // No reservations
                    absences = setOf(AbsenceCategory.BILLABLE),
                    dietInfo = null,
                    mealTextureInfo = null,
                    dailyPreschoolTime = null,
                    dailyPreparatoryTime = null,
                    mealTimes =
                        DaycareMealtimes(
                            breakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                            lunch = TimeRange(LocalTime.of(11, 0), LocalTime.of(11, 20)),
                            snack = TimeRange(LocalTime.of(14, 0), LocalTime.of(14, 20)),
                            supper = null,
                            eveningSnack = null,
                        ),
                )
            )
        val preschoolTerms = emptyList<PreschoolTerm>()

        val report =
            mealReportData(
                children = childInfo,
                date = testDate,
                preschoolTerms = preschoolTerms,
                DefaultMealTypeMapper,
            )

        assertTrue(report.isEmpty(), "Expected no meals for absent child")
    }

    @Test
    fun `mealReportData should default to breakfast, lunch, and snack when no reservations`() {
        val testDate = LocalDate.of(2023, 5, 10)
        val childInfo =
            listOf(
                MealReportChildInfo(
                    placementType = PlacementType.DAYCARE,
                    firstName = "Jane",
                    lastName = "Smith",
                    reservations = emptyList(), // No reservations
                    absences = null, // No absences
                    dietInfo = null,
                    mealTextureInfo = null,
                    dailyPreschoolTime = null,
                    dailyPreparatoryTime = null,
                    mealTimes =
                        DaycareMealtimes(
                            breakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                            lunch = TimeRange(LocalTime.of(12, 0), LocalTime.of(12, 20)),
                            snack = TimeRange(LocalTime.of(15, 30), LocalTime.of(15, 50)),
                            supper = null,
                            eveningSnack = null,
                        ),
                )
            )
        val preschoolTerms = emptyList<PreschoolTerm>()

        val report =
            mealReportData(
                children = childInfo,
                date = testDate,
                preschoolTerms = preschoolTerms,
                DefaultMealTypeMapper,
            )

        val expectedMeals = setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.SNACK)
        val actualMeals = report.map { it.mealType }.toSet()

        assertEquals(
            expectedMeals,
            actualMeals,
            "Expected default meals (breakfast, lunch, snack) when there are no reservations",
        )
    }

    @Test
    fun `mealReportData should default to breakfast, preschool lunch, and snack when no reservations for PRESCHOOL_DAYCARE`() {
        val testDate = LocalDate.of(2023, 5, 10)
        val childInfo =
            listOf(
                MealReportChildInfo(
                    placementType = PlacementType.PRESCHOOL_DAYCARE,
                    firstName = "Jane",
                    lastName = "Smith",
                    reservations = emptyList(), // No reservations
                    absences = null, // No absences
                    dietInfo = null,
                    mealTextureInfo = null,
                    dailyPreschoolTime = null,
                    dailyPreparatoryTime = null,
                    mealTimes =
                        DaycareMealtimes(
                            breakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                            lunch = TimeRange(LocalTime.of(12, 0), LocalTime.of(12, 20)),
                            snack = TimeRange(LocalTime.of(15, 30), LocalTime.of(15, 50)),
                            supper = null,
                            eveningSnack = null,
                        ),
                )
            )
        val preschoolTerms = emptyList<PreschoolTerm>()

        val report =
            mealReportData(
                children = childInfo,
                date = testDate,
                preschoolTerms = preschoolTerms,
                DefaultMealTypeMapper,
            )

        val expectedMeals = setOf(MealType.BREAKFAST, MealType.LUNCH_PRESCHOOL, MealType.SNACK)
        val actualMeals = report.map { it.mealType }.toSet()

        assertEquals(
            expectedMeals,
            actualMeals,
            "Expected default meals (breakfast, lunch, snack) when there are no reservations",
        )
    }

    @Test
    fun `mealReportData should provide meals during preschool times for preschool type placements`() {
        val testDate = LocalDate.of(2023, 5, 10)
        val childInfo =
            listOf(
                MealReportChildInfo(
                    placementType = PlacementType.PRESCHOOL, // Preschool type placement
                    firstName = "Alice",
                    lastName = "Johnson",
                    reservations = listOf(), // No specific reservations
                    absences = null, // No absences
                    dietInfo = null,
                    mealTextureInfo = null,
                    dailyPreschoolTime = TimeRange(LocalTime.of(10, 0), LocalTime.of(14, 30)),
                    dailyPreparatoryTime = null,
                    mealTimes =
                        DaycareMealtimes(
                            breakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                            lunch = TimeRange(LocalTime.of(11, 0), LocalTime.of(11, 20)),
                            snack = TimeRange(LocalTime.of(14, 0), LocalTime.of(14, 20)),
                            supper = null,
                            eveningSnack = null,
                        ),
                )
            )
        val preschoolTerms =
            listOf(
                PreschoolTerm(
                    finnishPreschool =
                        FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                    id = PreschoolTermId(UUID.randomUUID()),
                    extendedTerm =
                        FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                    swedishPreschool =
                        FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                    termBreaks = DateSet.empty(),
                    applicationPeriod =
                        FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                )
            )

        val report =
            mealReportData(
                children = childInfo,
                date = testDate,
                preschoolTerms = preschoolTerms,
                DefaultMealTypeMapper,
            )

        val expectedMealTypes = setOf(MealType.LUNCH_PRESCHOOL, MealType.SNACK)
        val actualMealTypes = report.map { it.mealType }.toSet()

        assertEquals(
            expectedMealTypes,
            actualMealTypes,
            "Expected lunch + snack meals for preschool type placements",
        )
    }

    @Test
    fun `mealReportData should provide individual rows for meals when child has special diet`() {
        val testDate = LocalDate.of(2023, 5, 10)
        val glutenFreeDiet = SpecialDiet(id = 101, abbreviation = "G")
        val lactoseFreeDiet = SpecialDiet(id = 42, abbreviation = "L")
        val childInfo =
            listOf(
                MealReportChildInfo( // child with glutenFreeDiet
                    placementType = PlacementType.DAYCARE,
                    firstName = "Ella",
                    lastName = "Brown",
                    reservations = listOf(TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))),
                    absences = null, // No absences
                    dietInfo = glutenFreeDiet,
                    mealTextureInfo = null,
                    dailyPreschoolTime = null,
                    dailyPreparatoryTime = null,
                    mealTimes =
                        DaycareMealtimes(
                            breakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                            lunch = TimeRange(LocalTime.of(12, 0), LocalTime.of(12, 20)),
                            snack = TimeRange(LocalTime.of(15, 30), LocalTime.of(15, 50)),
                            supper = null,
                            eveningSnack = null,
                        ),
                ),
                MealReportChildInfo( // child with glutenFreeDiet
                    placementType = PlacementType.DAYCARE,
                    firstName = "Mike",
                    lastName = "Brown",
                    reservations = listOf(TimeRange(LocalTime.of(12, 0), LocalTime.of(13, 0))),
                    absences = null, // No absences
                    dietInfo = glutenFreeDiet,
                    mealTextureInfo = null,
                    dailyPreschoolTime = null,
                    dailyPreparatoryTime = null,
                    mealTimes =
                        DaycareMealtimes(
                            breakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                            lunch = TimeRange(LocalTime.of(12, 0), LocalTime.of(12, 20)),
                            snack = TimeRange(LocalTime.of(15, 30), LocalTime.of(15, 50)),
                            supper = null,
                            eveningSnack = null,
                        ),
                ),
                MealReportChildInfo( // child with lactoseFreeDiet
                    placementType = PlacementType.DAYCARE,
                    firstName = "Mikko",
                    lastName = "Mallikas",
                    reservations = listOf(TimeRange(LocalTime.of(12, 0), LocalTime.of(13, 0))),
                    absences = null, // No absences
                    dietInfo = lactoseFreeDiet,
                    mealTextureInfo = null,
                    dailyPreschoolTime = null,
                    dailyPreparatoryTime = null,
                    mealTimes =
                        DaycareMealtimes(
                            breakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                            lunch = TimeRange(LocalTime.of(12, 0), LocalTime.of(12, 20)),
                            snack = TimeRange(LocalTime.of(15, 30), LocalTime.of(15, 50)),
                            supper = null,
                            eveningSnack = null,
                        ),
                ),
            )
        val preschoolTerms = emptyList<PreschoolTerm>()

        val report =
            mealReportData(
                children = childInfo,
                date = testDate,
                preschoolTerms = preschoolTerms,
                DefaultMealTypeMapper,
            )

        val expectedRowsForElla =
            setOf(
                MealReportRow(
                    MealType.BREAKFAST,
                    143,
                    1,
                    glutenFreeDiet.id,
                    glutenFreeDiet.abbreviation,
                    "Brown Ella",
                ),
                MealReportRow(
                    MealType.LUNCH,
                    145,
                    1,
                    glutenFreeDiet.id,
                    glutenFreeDiet.abbreviation,
                    "Brown Ella",
                ),
                MealReportRow(
                    MealType.SNACK,
                    160,
                    1,
                    glutenFreeDiet.id,
                    glutenFreeDiet.abbreviation,
                    "Brown Ella",
                ),
            )
        val rowsForElla = report.filter { it.additionalInfo.equals("Brown Ella") }.toSet()

        assertEquals(
            expectedRowsForElla,
            rowsForElla,
            "Expected individual meal rows for each meal type with special diet details",
        )

        val expectedRowsForMike =
            setOf(
                MealReportRow(
                    MealType.LUNCH,
                    145,
                    1,
                    glutenFreeDiet.id,
                    glutenFreeDiet.abbreviation,
                    "Brown Mike",
                )
            )
        val rowsForMike = report.filter { it.additionalInfo.equals("Brown Mike") }.toSet()

        assertEquals(
            expectedRowsForMike,
            rowsForMike,
            "Expected individual meal rows for each meal type with special diet details",
        )

        val expectedRowsForMikko =
            setOf(
                MealReportRow(
                    MealType.LUNCH,
                    145,
                    1,
                    lactoseFreeDiet.id,
                    lactoseFreeDiet.abbreviation,
                    "Mallikas Mikko",
                )
            )
        val rowsForMikko = report.filter { it.additionalInfo.equals("Mallikas Mikko") }.toSet()

        assertEquals(
            expectedRowsForMikko,
            rowsForMikko,
            "Expected individual meal rows for each meal type with special diet details",
        )
    }

    @Test
    fun `mealReportData should sum meals correctly for multiple children with no special diet`() {
        val testDate = LocalDate.of(2023, 5, 10)
        val childInfo =
            listOf(
                MealReportChildInfo(
                    placementType = PlacementType.DAYCARE,
                    firstName = "Ella",
                    lastName = "Brown",
                    reservations = listOf(TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))),
                    absences = null, // No absences
                    dietInfo = null,
                    mealTextureInfo = null,
                    dailyPreschoolTime = null,
                    dailyPreparatoryTime = null,
                    mealTimes =
                        DaycareMealtimes(
                            breakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                            lunch = TimeRange(LocalTime.of(12, 0), LocalTime.of(12, 20)),
                            snack = TimeRange(LocalTime.of(15, 30), LocalTime.of(15, 50)),
                            supper = null,
                            eveningSnack = null,
                        ),
                ),
                MealReportChildInfo(
                    placementType = PlacementType.DAYCARE,
                    firstName = "Mike",
                    lastName = "Johnson",
                    reservations = listOf(TimeRange(LocalTime.of(10, 0), LocalTime.of(16, 0))),
                    absences = null, // No absences
                    dietInfo = null,
                    mealTextureInfo = null,
                    dailyPreschoolTime = null,
                    dailyPreparatoryTime = null,
                    mealTimes =
                        DaycareMealtimes(
                            breakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                            lunch = TimeRange(LocalTime.of(12, 0), LocalTime.of(12, 20)),
                            snack = TimeRange(LocalTime.of(15, 30), LocalTime.of(15, 50)),
                            supper = null,
                            eveningSnack = null,
                        ),
                ),
            )
        val preschoolTerms = emptyList<PreschoolTerm>()

        val report =
            mealReportData(
                children = childInfo,
                date = testDate,
                preschoolTerms = preschoolTerms,
                DefaultMealTypeMapper,
            )

        val expectedMealCounts =
            mapOf(MealType.BREAKFAST to 1, MealType.LUNCH to 2, MealType.SNACK to 2)
        val actualMealCounts =
            report.groupBy { it.mealType }.mapValues { entry -> entry.value.sumOf { it.mealCount } }

        assertEquals(
            expectedMealCounts,
            actualMealCounts,
            "Expected summed meal counts for multiple children with no special diet",
        )
    }

    @Test
    fun `getMealReportForUnit should return no meals on weekends for a normal unit`() {
        val testDate = LocalDate.of(2023, 5, 14) // May 14, 2023, is a Sunday

        val childId1 = ChildId(UUID.randomUUID())
        val childId2 = ChildId(UUID.randomUUID())

        val childPlacements =
            mapOf(childId1 to PlacementType.DAYCARE, childId2 to PlacementType.PRESCHOOL)

        val childData =
            mapOf(
                childId1 to
                    ChildData(
                        child =
                            UnitAttendanceReservations.Child(
                                firstName = "John",
                                lastName = "Doe",
                                id = childId1,
                                serviceNeeds = emptyList(),
                                dateOfBirth = LocalDate.of(2020, 1, 1),
                                preferredName = "",
                            ),
                        reservations = mapOf(),
                        absences = mapOf(),
                        attendances = mapOf(),
                        operationalDays = emptySet(),
                    ),
                childId2 to
                    ChildData(
                        child =
                            UnitAttendanceReservations.Child(
                                firstName = "Jane",
                                lastName = "Smith",
                                id = childId1,
                                serviceNeeds = emptyList(),
                                dateOfBirth = LocalDate.of(2020, 1, 1),
                                preferredName = "",
                            ),
                        reservations = mapOf(),
                        absences = mapOf(),
                        attendances = mapOf(),
                        operationalDays = emptySet(),
                    ),
            )

        val daycare =
            object : DaycareInfo {
                override val name = "Test Daycare"
                override val operationDays = setOf(1, 2, 3, 4, 5) // Monday to Friday
                override val shiftCareOperationDays: Set<Int>? = null
                override val shiftCareOpenOnHolidays: Boolean = false
                override val mealTimes =
                    DaycareMealtimes(
                        breakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                        lunch = TimeRange(LocalTime.of(11, 0), LocalTime.of(11, 20)),
                        snack = TimeRange(LocalTime.of(14, 0), LocalTime.of(14, 20)),
                        supper = null,
                        eveningSnack = null,
                    )
                override val dailyPreschoolTime = null
                override val dailyPreparatoryTime = null
            }

        val unitData =
            DaycareUnitData(
                daycare = daycare,
                holidays = emptySet(),
                childPlacements = childPlacements,
                childrenWithShiftCare = emptySet(),
                childData = childData,
                specialDiets = emptyMap(),
                mealTextures = emptyMap(),
                preschoolTerms = emptyList(),
            )

        val report = getMealReportForUnit(unitData, testDate, DefaultMealTypeMapper)

        assertTrue(report?.meals.isNullOrEmpty(), "Expected no meals on a Sunday for a normal unit")
    }

    @Test
    fun `getMealReportForUnit should return no meals on weekends for children without shift care`() {
        val testDate = LocalDate.of(2023, 5, 14) // May 14, 2023, is a Sunday

        val childId1 = ChildId(UUID.randomUUID())
        val childId2 = ChildId(UUID.randomUUID())

        val childPlacements =
            mapOf(childId1 to PlacementType.DAYCARE, childId2 to PlacementType.PRESCHOOL)

        val childData =
            mapOf(
                childId1 to
                    ChildData(
                        child =
                            UnitAttendanceReservations.Child(
                                firstName = "John",
                                lastName = "Doe",
                                id = childId1,
                                serviceNeeds =
                                    listOf(createServiceNeedInfo(childId1, ShiftCareType.NONE)),
                                dateOfBirth = LocalDate.of(2020, 1, 1),
                                preferredName = "",
                            ),
                        reservations = mapOf(),
                        absences = mapOf(),
                        attendances = mapOf(),
                        operationalDays = emptySet(),
                    ),
                childId2 to
                    ChildData(
                        child =
                            UnitAttendanceReservations.Child(
                                firstName = "Jane",
                                lastName = "Smith",
                                id = childId1,
                                serviceNeeds = emptyList(),
                                dateOfBirth = LocalDate.of(2020, 1, 1),
                                preferredName = "",
                            ),
                        reservations = mapOf(),
                        absences = mapOf(),
                        attendances = mapOf(),
                        operationalDays = emptySet(),
                    ),
            )

        val daycare =
            object : DaycareInfo {
                override val name = "Test Daycare"
                override val operationDays = setOf(1, 2, 3, 4, 5)
                override val shiftCareOperationDays: Set<Int>? = setOf(1, 2, 3, 4, 5, 6, 7)
                override val shiftCareOpenOnHolidays: Boolean = false
                override val mealTimes =
                    DaycareMealtimes(
                        breakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                        lunch = TimeRange(LocalTime.of(11, 0), LocalTime.of(11, 20)),
                        snack = TimeRange(LocalTime.of(14, 0), LocalTime.of(14, 20)),
                        supper = null,
                        eveningSnack = null,
                    )
                override val dailyPreschoolTime = null
                override val dailyPreparatoryTime = null
            }

        val unitData =
            DaycareUnitData(
                daycare = daycare,
                holidays = emptySet(),
                childPlacements = childPlacements,
                childrenWithShiftCare = emptySet(),
                childData = childData,
                specialDiets = emptyMap(),
                mealTextures = emptyMap(),
                preschoolTerms = emptyList(),
            )

        val report = getMealReportForUnit(unitData, testDate, DefaultMealTypeMapper)

        assertTrue(
            report?.meals.isNullOrEmpty(),
            "Expected no meals on a Sunday for children without shift care",
        )
    }

    @Test
    fun `getMealReportForUnit should provide meals based on reservations for children with shift care on weekends`() {
        val testDate = LocalDate.of(2023, 5, 13) // May 13, 2023, is a Saturday

        val childId1 = ChildId(UUID.randomUUID())
        val childId2 = ChildId(UUID.randomUUID())

        val childPlacements =
            mapOf(childId1 to PlacementType.DAYCARE, childId2 to PlacementType.DAYCARE)

        val breakfastTime = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20))
        val lunchTime = TimeRange(LocalTime.of(11, 0), LocalTime.of(11, 20))
        val snackTime = TimeRange(LocalTime.of(14, 0), LocalTime.of(14, 20))

        val childData =
            mapOf(
                childId1 to
                    ChildData(
                        child =
                            UnitAttendanceReservations.Child(
                                firstName = "John",
                                lastName = "Doe",
                                id = childId1,
                                serviceNeeds =
                                    listOf(createServiceNeedInfo(childId1, ShiftCareType.FULL)),
                                dateOfBirth = LocalDate.of(2020, 1, 1),
                                preferredName = "",
                            ),
                        reservations =
                            mapOf(
                                testDate to
                                    listOf(
                                        ReservationResponse.Times(
                                            TimeRange(LocalTime.of(8, 0), LocalTime.of(14, 20)),
                                            false,
                                        )
                                    )
                            ),
                        absences = emptyMap(),
                        attendances = emptyMap(),
                        operationalDays = setOf(testDate),
                    ),
                childId2 to
                    ChildData(
                        child =
                            UnitAttendanceReservations.Child(
                                firstName = "Jane",
                                lastName = "Smith",
                                id = childId2,
                                serviceNeeds =
                                    listOf(
                                        createServiceNeedInfo(childId2, ShiftCareType.INTERMITTENT)
                                    ),
                                dateOfBirth = LocalDate.of(2020, 1, 1),
                                preferredName = "",
                            ),
                        reservations =
                            mapOf(
                                testDate to
                                    listOf(
                                        ReservationResponse.Times(
                                            TimeRange(LocalTime.of(10, 0), LocalTime.of(14, 20)),
                                            false,
                                        )
                                    )
                            ),
                        absences = emptyMap(),
                        attendances = emptyMap(),
                        operationalDays = setOf(testDate),
                    ),
            )

        val daycare =
            object : DaycareInfo {
                override val name = "24/7 Daycare"
                override val operationDays = setOf(1, 2, 3, 4, 5)
                override val shiftCareOperationDays: Set<Int>? = setOf(1, 2, 3, 4, 5, 6, 7)
                override val shiftCareOpenOnHolidays: Boolean = false
                override val mealTimes =
                    DaycareMealtimes(
                        breakfast = breakfastTime,
                        lunch = lunchTime,
                        snack = snackTime,
                        supper = null,
                        eveningSnack = null,
                    )
                override val dailyPreschoolTime = null
                override val dailyPreparatoryTime = null
            }

        val unitData =
            DaycareUnitData(
                daycare = daycare,
                holidays = emptySet(),
                childPlacements = childPlacements,
                childrenWithShiftCare = setOf(childId1, childId2),
                childData = childData,
                specialDiets = emptyMap(),
                mealTextures = emptyMap(),
                preschoolTerms = emptyList(),
            )

        val report = getMealReportForUnit(unitData, testDate, DefaultMealTypeMapper)
        assertFalse(
            report?.meals.isNullOrEmpty(),
            "Expected meals based on reservations for shift care children in a round-the-clock unit on weekends",
        )
        assertEquals(
            3,
            report?.meals?.size,
            "Expected three meals orders based on the children's reservations",
        )
    }

    @Test
    fun `getMealReportForUnit should return no meals on a holiday for a normal unit`() {
        val testDate = LocalDate.of(2023, 12, 25) // December 25, 2023, is a holiday

        val childId1 = ChildId(UUID.randomUUID())
        val childId2 = ChildId(UUID.randomUUID())

        val childPlacements =
            mapOf(childId1 to PlacementType.DAYCARE, childId2 to PlacementType.DAYCARE)

        val breakfastTime = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20))
        val lunchTime = TimeRange(LocalTime.of(11, 0), LocalTime.of(11, 20))
        val snackTime = TimeRange(LocalTime.of(14, 0), LocalTime.of(14, 20))

        val childData =
            mapOf(
                childId1 to
                    ChildData(
                        child =
                            UnitAttendanceReservations.Child(
                                firstName = "John",
                                lastName = "Doe",
                                id = childId1,
                                serviceNeeds = emptyList(),
                                dateOfBirth = LocalDate.of(2020, 1, 1),
                                preferredName = "",
                            ),
                        reservations = mapOf(),
                        absences = emptyMap(),
                        attendances = emptyMap(),
                        operationalDays = emptySet(),
                    ),
                childId2 to
                    ChildData(
                        child =
                            UnitAttendanceReservations.Child(
                                firstName = "Jane",
                                lastName = "Smith",
                                id = childId2,
                                serviceNeeds = emptyList(),
                                dateOfBirth = LocalDate.of(2020, 1, 1),
                                preferredName = "",
                            ),
                        reservations = mapOf(),
                        absences = emptyMap(),
                        attendances = emptyMap(),
                        operationalDays = emptySet(),
                    ),
            )

        val daycare =
            object : DaycareInfo {
                override val name = "Regular Daycare"
                override val operationDays = setOf(1, 2, 3, 4, 5) // Monday to Friday
                override val shiftCareOperationDays: Set<Int>? = null
                override val shiftCareOpenOnHolidays: Boolean = false
                override val mealTimes =
                    DaycareMealtimes(
                        breakfast = breakfastTime,
                        lunch = lunchTime,
                        snack = snackTime,
                        supper = null,
                        eveningSnack = null,
                    )
                override val dailyPreschoolTime = null
                override val dailyPreparatoryTime = null
            }

        val unitData =
            DaycareUnitData(
                daycare = daycare,
                holidays = setOf(testDate),
                childPlacements = childPlacements,
                childrenWithShiftCare = emptySet(),
                childData = childData,
                specialDiets = emptyMap(),
                mealTextures = emptyMap(),
                preschoolTerms = emptyList(),
            )

        val report = getMealReportForUnit(unitData, testDate, DefaultMealTypeMapper)

        assertTrue(
            report?.meals.isNullOrEmpty(),
            "Expected no meals on a holiday for a normal unit",
        )
    }

    @Test
    fun `getMealReportForUnit should return set of meals on a holiday for a children with shift care`() {
        val testDate = LocalDate.of(2023, 12, 25) // December 25, 2023, is a holiday

        val childId1 = ChildId(UUID.randomUUID())
        val childId2 = ChildId(UUID.randomUUID())

        val childPlacements =
            mapOf(childId1 to PlacementType.DAYCARE, childId2 to PlacementType.DAYCARE)

        val breakfastTime = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20))
        val lunchTime = TimeRange(LocalTime.of(11, 0), LocalTime.of(11, 20))
        val snackTime = TimeRange(LocalTime.of(14, 0), LocalTime.of(14, 20))

        val childData =
            mapOf(
                childId1 to
                    ChildData(
                        child =
                            UnitAttendanceReservations.Child(
                                firstName = "John",
                                lastName = "Doe",
                                id = childId1,
                                serviceNeeds =
                                    listOf(createServiceNeedInfo(childId1, ShiftCareType.FULL)),
                                dateOfBirth = LocalDate.of(2020, 1, 1),
                                preferredName = "",
                            ),
                        reservations = mapOf(),
                        absences = emptyMap(),
                        attendances = emptyMap(),
                        operationalDays = setOf(testDate),
                    ),
                childId2 to
                    ChildData(
                        child =
                            UnitAttendanceReservations.Child(
                                firstName = "Jane",
                                lastName = "Smith",
                                id = childId2,
                                serviceNeeds =
                                    listOf(
                                        createServiceNeedInfo(childId2, ShiftCareType.INTERMITTENT)
                                    ),
                                dateOfBirth = LocalDate.of(2020, 1, 1),
                                preferredName = "",
                            ),
                        reservations = mapOf(),
                        absences = emptyMap(),
                        attendances = emptyMap(),
                        operationalDays = setOf(testDate),
                    ),
            )

        val daycare =
            object : DaycareInfo {
                override val name = "Daycare"
                override val operationDays = setOf(1, 2, 3, 4, 5)
                override val shiftCareOperationDays: Set<Int>? = setOf(1, 2, 3, 4, 5, 6, 7)
                override val shiftCareOpenOnHolidays: Boolean = true
                override val mealTimes =
                    DaycareMealtimes(
                        breakfast = breakfastTime,
                        lunch = lunchTime,
                        snack = snackTime,
                        supper = null,
                        eveningSnack = null,
                    )
                override val dailyPreschoolTime = null
                override val dailyPreparatoryTime = null
            }

        val unitData =
            DaycareUnitData(
                daycare = daycare,
                holidays = setOf(testDate),
                childPlacements = childPlacements,
                childrenWithShiftCare = setOf(childId1, childId2),
                childData = childData,
                specialDiets = emptyMap(),
                mealTextures = emptyMap(),
                preschoolTerms = emptyList(),
            )

        val report = getMealReportForUnit(unitData, testDate, DefaultMealTypeMapper)

        assertFalse(
            report?.meals.isNullOrEmpty(),
            "Expected default meals for shift care children in a round-the-clock unit on weekends",
        )
    }
}

private fun createServiceNeedInfo(childId: ChildId, shiftCare: ShiftCareType) =
    ChildServiceNeedInfo(
        childId = childId,
        hasContractDays = false,
        daycareHoursPerMonth = null,
        optionName = "",
        validDuring = FiniteDateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2050, 1, 1)),
        shiftCare = shiftCare,
        partWeek = false,
    )
