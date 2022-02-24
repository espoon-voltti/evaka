// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.reservations.AbsenceInsert
import fi.espoo.evaka.reservations.clearOldAbsences
import fi.espoo.evaka.reservations.insertAbsences
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/holiday-period")
class HolidayPeriodControllerCitizen(private val accessControl: AccessControl) {
    @GetMapping
    fun getHolidayPeriods(
        db: Database,
        user: AuthenticatedUser,
    ): List<HolidayPeriod> {
        Audit.HolidayPeriodsList.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_HOLIDAY_PERIODS)
        return db.connect { dbc -> dbc.read { it.getHolidayPeriods() } }
    }

    @PostMapping("/holidays")
    fun postAbsences(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @RequestBody body: HolidayAbsenceRequest
    ) {
        val childIds = body.childHolidays.keys
        Audit.HolidayAbsenceCreate.log(targetId = childIds.toSet().joinToString())
        accessControl.requirePermissionFor(user, Action.Global.CREATE_HOLIDAY_ABSENCE)
        accessControl.requireGuardian(user, childIds)

        db.connect { dbc ->
            dbc.transaction { tx ->
                val activeHolidayPeriod = tx.getActiveFreeHolidayPeriod(evakaClock.today())
                if (activeHolidayPeriod?.freePeriod == null) {
                    throw BadRequest("No active free holiday period found")
                }

                val childToDateRange = body.childHolidays.entries.filter { it.value != null }.map { it.key to it.value!! }
                    .onEach { (_, dateRange) ->
                        if (!activeHolidayPeriod.freePeriod.periodOptions.contains(dateRange)) {
                            throw BadRequest("Free holiday period not found")
                        }
                    }

                val childToDates = childToDateRange.flatMap { (childId, dateRange) ->
                    dateRange.dates().map { childId to it }
                }

                tx.clearOldAbsences(childToDates)

                tx.insertAbsences(PersonId(user.id), childToDateRange.map { (childId, dateRange) -> AbsenceInsert(setOf(childId), dateRange, AbsenceType.FREE_ABSENCE) })
            }
        }
    }
}

data class HolidayAbsenceRequest(
    val childHolidays: Map<ChildId, FiniteDateRange?>
)
