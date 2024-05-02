// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.reports

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.daycare.*
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.AbsenceTypeResponse
import fi.espoo.evaka.reservations.ChildData
import fi.espoo.evaka.reservations.ReservationResponse
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.specialdiet.SpecialDiet
import java.time.LocalDate

val preschoolPlacementTypes =
    listOf(
        PlacementType.PRESCHOOL,
        PlacementType.PRESCHOOL_DAYCARE,
        PlacementType.PRESCHOOL_CLUB,
        PlacementType.PRESCHOOL_DAYCARE_ONLY,
        PlacementType.PREPARATORY_DAYCARE,
        PlacementType.PREPARATORY_DAYCARE_ONLY
    )

enum class MealType {
    BREAKFAST,
    LUNCH,
    LUNCH_PRESCHOOL,
    SNACK,
    SUPPER,
    EVENING_SNACK
}

fun mealtypeToMealIdTranslator(mealType: MealType, specialDiet: Boolean): Int =
    if (specialDiet) {
        when (mealType) {
            MealType.BREAKFAST -> 143
            MealType.LUNCH -> 145
            MealType.LUNCH_PRESCHOOL -> 24
            MealType.SNACK -> 160
            MealType.SUPPER -> 28
            MealType.EVENING_SNACK -> 31
        }
    } else
        when (mealType) {
            MealType.BREAKFAST -> 162
            MealType.LUNCH -> 175
            MealType.LUNCH_PRESCHOOL -> 22
            MealType.SNACK -> 152
            MealType.SUPPER -> 27
            MealType.EVENING_SNACK -> 30
        }

data class MealReportRow(
    val mealType: MealType,
    val mealId: Int,
    val mealCount: Int,
    val dietId: Int? = null,
    val dietName: String? = null,
    val dietAbbreviation: String? = null,
    val additionalInfo: String? = null
)

data class MealReportData(
    val date: LocalDate,
    val reportName: String,
    val meals: List<MealReportRow>
)

data class MealInfo(
    val mealType: MealType,
    val dietId: Int? = null,
    val dietName: String? = null,
    val dietAbbreviation: String? = null,
    val additionalInfo: String? = null
)

private fun childMeals(
    fixedScheduleRange: TimeRange?,
    reservations: List<ReservationResponse>,
    absent: Boolean,
    mealtimes: DaycareMealtimes,
    usePreschoolMealTypes: Boolean
): Set<MealType> {
    // if absent -> no meals
    if (absent) {
        return emptySet()
    }
    // list of time ranges when child will be present according to fixed schedule or reservation
    // times
    val presentTimeRanges =
        if (fixedScheduleRange != null) listOf(fixedScheduleRange)
        else reservations.filterIsInstance<ReservationResponse.Times>().map { it.range }
    // if we don't have data about when child will be present, default to breakfast + lunch + snack
    if (presentTimeRanges.isEmpty()) {
        return setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.SNACK)
    }
    // otherwise check unit meal times against the present time ranges
    val meals = mutableSetOf<MealType>()

    fun addMealIfPresent(mealTime: TimeRange?, mealType: MealType) {
        if (mealTime != null && presentTimeRanges.any { it.overlaps(mealTime) }) {
            meals.add(mealType)
        }
    }

    addMealIfPresent(mealtimes.breakfast, MealType.BREAKFAST)
    addMealIfPresent(
        mealtimes.lunch,
        if (usePreschoolMealTypes) MealType.LUNCH_PRESCHOOL else MealType.LUNCH
    )
    addMealIfPresent(mealtimes.snack, MealType.SNACK)
    addMealIfPresent(mealtimes.supper, MealType.SUPPER)
    addMealIfPresent(mealtimes.eveningSnack, MealType.EVENING_SNACK)

    return meals
}

data class MealReportChildInfo(
    val placementType: PlacementType,
    val firstName: String,
    val lastName: String,
    val reservations: List<ReservationResponse>?,
    val absences: Map<AbsenceCategory, AbsenceTypeResponse>?,
    val dietInfo: SpecialDiet?,
    val daycare: DaycareTimeProps
)

fun mealReportData(
    children: Collection<MealReportChildInfo>,
    date: LocalDate,
    preschoolTerms: List<PreschoolTerm>,
    reportName: String
): MealReportData {
    val mealInfoMap =
        children
            .flatMap { childInfo ->
                val fixedScheduleRange =
                    childInfo.placementType.fixedScheduleOnlyRange(
                        date,
                        childInfo.daycare.dailyPreschoolTime,
                        childInfo.daycare.dailyPreparatoryTime,
                        preschoolTerms
                    )
                val absent =
                    childInfo.absences?.size == childInfo.placementType.absenceCategories().size
                val usePreschoolMealTypes =
                    preschoolPlacementTypes.contains(childInfo.placementType)

                childMeals(
                        fixedScheduleRange,
                        childInfo.reservations ?: emptyList(),
                        absent,
                        childInfo.daycare.mealTimes,
                        usePreschoolMealTypes,
                    )
                    .map {
                        MealInfo(
                            it,
                            additionalInfo =
                                if (childInfo.dietInfo != null) {
                                    childInfo.lastName + " " + childInfo.firstName
                                } else null,
                            dietId = childInfo.dietInfo?.id,
                            dietName = childInfo.dietInfo?.name,
                            dietAbbreviation = childInfo.dietInfo?.abbreviation
                        )
                    }
            }
            .groupBy { it }
            .mapValues { it.value.size }

    return MealReportData(
        date,
        reportName,
        mealInfoMap.map {
            MealReportRow(
                it.key.mealType,
                mealtypeToMealIdTranslator(it.key.mealType, it.key.dietId != null),
                it.value,
                it.key.dietId,
                it.key.dietName,
                it.key.dietAbbreviation,
                it.key.additionalInfo
            )
        }
    )
}

data class DaycareUnitData(
    val daycare: DaycareInfo?,
    val holidays: Set<LocalDate>,
    val childPlacements: Map<ChildId, PlacementType>,
    val childData: Map<ChildId, ChildData>,
    val specialDiets: Map<ChildId, SpecialDiet?>,
    val preschoolTerms: List<PreschoolTerm>
)

fun getMealReportForUnit(unitData: DaycareUnitData, date: LocalDate): MealReportData? {
    val daycare = unitData.daycare ?: return null

    if (!daycare.operationDays.contains(date.dayOfWeek.value))
        return MealReportData(date, daycare.name, emptyList())

    val isRoundTheClockUnit = daycare.operationDays == setOf(1, 2, 3, 4, 5, 6, 7)
    if (!isRoundTheClockUnit) {
        if (unitData.holidays.contains(date)) {
            return MealReportData(date, daycare.name, emptyList())
        }
    }

    val childrenToPlacementTypeMap = unitData.childPlacements
    val childrenReservationsAndAttendances = unitData.childData

    val dietInfos = unitData.specialDiets
    val childInfos =
        childrenToPlacementTypeMap
            .mapValues { (childId, placementType) ->
                MealReportChildInfo(
                    placementType = placementType,
                    firstName = childrenReservationsAndAttendances[childId]!!.child.firstName,
                    lastName = childrenReservationsAndAttendances[childId]!!.child.lastName,
                    reservations = childrenReservationsAndAttendances[childId]!!.reservations[date],
                    absences = childrenReservationsAndAttendances[childId]!!.absences[date],
                    dietInfo = dietInfos[childId],
                    daycare = daycare
                )
            }
            .values

    val preschoolTerms = unitData.preschoolTerms
    return mealReportData(childInfos, date, preschoolTerms, daycare.name)
}
