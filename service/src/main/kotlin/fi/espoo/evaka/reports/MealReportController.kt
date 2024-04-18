// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.Daycare
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.PlacementType.*
import fi.espoo.evaka.reservations.ReservationResponse
import fi.espoo.evaka.reservations.getChildData
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.*
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

val preschoolPlacementTypes =
    listOf(
        PRESCHOOL_DAYCARE,
        PRESCHOOL_CLUB,
        PRESCHOOL_DAYCARE_ONLY,
        PREPARATORY_DAYCARE,
        PREPARATORY_DAYCARE_ONLY
    )

enum class MealType {
    BREAKFAST,
    LUNCH,
    LUNCH_PRESCHOOL,
    SNACK,
    SUPPER,
    EVENING_SNACK
}

fun mealtypeToMealIdTranslator(mealType: MealType): Int =
    when (mealType) {
        MealType.BREAKFAST -> 162
        MealType.LUNCH -> 175
        MealType.LUNCH_PRESCHOOL -> 22
        MealType.SNACK -> 152
        MealType.SUPPER -> 27
        MealType.EVENING_SNACK -> 30
    }

val mealIdsSpecialDiet =
    mapOf(
        MealType.BREAKFAST to 143,
        MealType.LUNCH to 145,
        MealType.LUNCH_PRESCHOOL to 24,
        MealType.SNACK to 160,
        MealType.SUPPER to 28,
        MealType.EVENING_SNACK to 31
    )

data class MealReportRow(
    val mealType: MealType,
    val mealId: Int,
    val mealCount: Int,
    val dietId: Int? = null,
    val additionalInfo: String? = null
)

data class MealReportData(
    val date: LocalDate,
    val unitName: String,
    val meals: List<MealReportRow>
)

data class MealInfo(
    val mealType: MealType
    // TODO to support special diets add extra info: additionalInfo (name), dietId
)

private fun childMeals(
    fixedScheduleRange: TimeRange?,
    reservations: List<ReservationResponse>,
    absent: Boolean,
    daycare: Daycare,
    usePreschoolMealTypes: Boolean
): Sequence<MealInfo> {
    // if absent -> no meals
    if (absent) {
        return emptySequence<MealInfo>()
    }
    // list of time ranges when child will be present according to fixed schedule or reservation
    // times
    val presentTimeRanges =
        if (fixedScheduleRange != null) listOf(fixedScheduleRange)
        else reservations.filterIsInstance<ReservationResponse.Times>().map { it.range }
    // if we don't have data about when child will be present, default to breakfast + lunch + snack
    if (presentTimeRanges.isEmpty()) {
        return sequenceOf(
            MealInfo(MealType.BREAKFAST),
            MealInfo(MealType.LUNCH),
            MealInfo(MealType.SNACK)
        )
    }
    // otherwise check unit meal times against the present time ranges
    val meals = mutableListOf<MealInfo>()

    fun addMealIfPresent(mealTime: TimeRange?, mealType: MealType) {
        if (mealTime != null && presentTimeRanges.any { it.overlaps(mealTime) }) {
            meals.add(MealInfo(mealType))
        }
    }

    addMealIfPresent(daycare.mealtimeBreakfast, MealType.BREAKFAST)
    addMealIfPresent(
        daycare.mealtimeLunch,
        if (usePreschoolMealTypes) MealType.LUNCH_PRESCHOOL else MealType.LUNCH
    )
    addMealIfPresent(daycare.mealtimeSnack, MealType.SNACK)
    addMealIfPresent(daycare.mealtimeSupper, MealType.SUPPER)
    addMealIfPresent(daycare.mealtimeEveningSnack, MealType.EVENING_SNACK)

    return meals.asSequence()
}

private fun getMealReport(tx: Database.Read, date: LocalDate, unitId: DaycareId): MealReportData? {
    val daycare = tx.getDaycare(unitId) ?: return null

    if (!daycare.operationDays.contains(date.dayOfWeek.value))
        return MealReportData(date, daycare.name, emptyList())

    val isRoundTheClockUnit = daycare.operationDays == setOf(1, 2, 3, 4, 5, 6, 7)
    if (!isRoundTheClockUnit) {
        val holidays = tx.getHolidays(FiniteDateRange(date, date))
        if (holidays.contains(date)) {
            return MealReportData(date, daycare.name, emptyList())
        }
    }

    val preschoolTerms = tx.getPreschoolTerms()

    val childrenToPlacementTypeMap = tx.childPlacementsForDay(unitId, date)
    val childrenReservationsAndAttendances =
        tx.getChildData(unitId, childrenToPlacementTypeMap.keys, date.toFiniteDateRange())

    val mealInfoMap =
        childrenToPlacementTypeMap
            .asSequence()
            .flatMap { (childId, placementType) ->
                val childReservationsAndAttendances = childrenReservationsAndAttendances[childId]!!
                val fixedScheduleRange =
                    placementType.fixedScheduleOnlyRange(
                        date,
                        daycare.dailyPreschoolTime,
                        daycare.dailyPreparatoryTime,
                        preschoolTerms
                    )
                val absent =
                    childReservationsAndAttendances.absences[date]?.size ==
                        placementType.absenceCategories().size
                val usePreschoolMealTypes = preschoolPlacementTypes.contains(placementType)
                childMeals(
                    fixedScheduleRange,
                    childReservationsAndAttendances.reservations[date] ?: emptyList(),
                    absent,
                    daycare,
                    usePreschoolMealTypes
                )
            }
            .groupBy { it }
            .mapValues { it.value.size }

    return MealReportData(
        date,
        daycare.name,
        mealInfoMap.map {
            MealReportRow(it.key.mealType, mealtypeToMealIdTranslator(it.key.mealType), it.value)
        }
    )
}

@RestController
class MealReportController(private val accessControl: AccessControl) {

    @GetMapping("/reports/meal/{unitId}")
    fun getMealReportByUnit(
        tx: Database,
        clock: EvakaClock,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): MealReportData {
        return tx.connect { dbc ->
            dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Unit.READ_MEAL_REPORT,
                        unitId
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    getMealReport(it, date, unitId)
                        ?: throw BadRequest("Daycare not found for $unitId")
                }
                .also {
                    Audit.MealReportRead.log(
                        meta =
                            mapOf(
                                "unitId" to unitId,
                                "unitName" to it.unitName,
                                "date" to date,
                                "count" to it.meals.size
                            )
                    )
                }
        }
    }
}

fun Database.Read.childPlacementsForDay(
    daycareId: DaycareId,
    date: LocalDate
): Map<ChildId, PlacementType> =
    createQuery {
            sql(
                """
SELECT child_id, placement_type 
FROM realized_placement_one(${bind(date)}) 
WHERE unit_id = ${bind(daycareId)}
                    """
            )
        }
        .toMap { columnPair("child_id", "placement_type") }
