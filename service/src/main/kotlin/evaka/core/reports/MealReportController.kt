// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.Audit
import evaka.core.daycare.getDaycare
import evaka.core.daycare.getPreschoolTerm
import evaka.core.mealintegration.MealTypeMapper
import evaka.core.reservations.getChildData
import evaka.core.serviceneed.ShiftCareType
import evaka.core.serviceneed.getChildServiceNeedInfos
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.getHolidays
import evaka.core.shared.domain.toFiniteDateRange
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

fun unitDataFromDatabase(tx: Database.Read, date: LocalDate, unitId: DaycareId): DaycareUnitData {
    val daycare = tx.getDaycare(unitId)
    val holidays = getHolidays(date.toFiniteDateRange())
    val childPlacements = tx.childPlacementsForDay(unitId, date)
    val childrenWithShiftCare =
        tx.getChildServiceNeedInfos(unitId, childPlacements.keys, FiniteDateRange(date, date))
            .filter { it.shiftCare != ShiftCareType.NONE }
            .map { it.childId }
            .toSet()
    val childIds = childPlacements.keys
    val childData = tx.getChildData(unitId, childIds, date.toFiniteDateRange())
    val specialDiets = tx.specialDietsForChildren(childIds)
    val mealTextures = tx.mealTexturesForChildren(childIds)
    val preschoolTerm = tx.getPreschoolTerm(date)

    return DaycareUnitData(
        daycare,
        holidays,
        childPlacements,
        childrenWithShiftCare,
        childData,
        specialDiets,
        mealTextures,
        preschoolTerm,
    )
}

@RestController
class MealReportController(
    private val accessControl: AccessControl,
    private val mealTypeMapper: MealTypeMapper,
) {

    @GetMapping("/employee/reports/meal/{unitId}")
    fun getMealReportByUnit(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): MealReportData {
        return db.connect { dbc ->
            dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Unit.READ_MEAL_REPORT,
                        unitId,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    getMealReportForUnit(
                        unitDataFromDatabase(it, date, unitId),
                        date,
                        mealTypeMapper,
                    ) ?: throw BadRequest("Daycare not found for $unitId")
                }
                .also {
                    Audit.MealReportRead.log(
                        meta =
                            mapOf(
                                "unitId" to unitId,
                                "unitName" to it.reportName,
                                "date" to date,
                                "count" to it.meals.size,
                            )
                    )
                }
        }
    }
}
