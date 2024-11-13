// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.mealintegration.MealTypeMapper
import fi.espoo.evaka.reservations.getChildData
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.getChildServiceNeedInfos
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

fun unitDataFromDatabase(tx: Database.Read, date: LocalDate, unitId: DaycareId): DaycareUnitData {
    val daycare = tx.getDaycare(unitId)
    val holidays = getHolidays(FiniteDateRange(date, date))
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
    val preschoolTerms = tx.getPreschoolTerms()

    return DaycareUnitData(
        daycare,
        holidays,
        childPlacements,
        childrenWithShiftCare,
        childData,
        specialDiets,
        mealTextures,
        preschoolTerms,
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
