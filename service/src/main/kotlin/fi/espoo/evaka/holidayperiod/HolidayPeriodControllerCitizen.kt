// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.reservations.AbsenceInsert
import fi.espoo.evaka.reservations.clearAbsencesWithinPeriod
import fi.espoo.evaka.reservations.clearOldReservations
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
    fun postHolidays(
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
                // TODO handle reservable days
                // TODO handle deadlines
                val holidayPeriod = tx.getActiveHolidayPeriod(evakaClock.today()) ?: throw BadRequest("No active holiday period")

                // TODO use previous FREE_ABSENCES after deadline, do not modify
                val freePeriods = body.childHolidays.entries
                    .mapNotNull { (childId, selections) -> if (selections.freePeriod != null) childId to selections.freePeriod else null }
                    .onEach { (_, freePeriod) ->
                        if (holidayPeriod.freePeriod == null || !holidayPeriod.freePeriod.periodOptions.contains(freePeriod)) {
                            throw BadRequest("Free holiday period not found")
                        }
                    }

                val otherHolidays = body.childHolidays.entries
                    .map { (childId, selections) ->
                        if (selections.holidays.any { !holidayPeriod.period.contains(it) }) {
                            throw BadRequest("Holiday period does not contain selected holiday", "OUT_OF_HOLIDAY_PERIOD")
                        }
                        val allPeriods = (selections.holidays + selections.freePeriod).filterNotNull().toSet()
                        if (selections.holidays.any { holiday -> allPeriods.any { it != holiday && it.overlaps(holiday) } }) {
                            throw BadRequest("Overlapping holidays", "HOLIDAYS_OVERLAP")
                        }
                        childId to selections.holidays
                    }

                val absenceInserts = freePeriods.map { (childId, dateRange) ->
                    AbsenceInsert(childId, dateRange, AbsenceType.FREE_ABSENCE)
                } + otherHolidays.flatMap { (childId, dateRanges) ->
                    dateRanges.map { AbsenceInsert(childId, it, AbsenceType.OTHER_ABSENCE) }
                }

                // clear reservations only from days with absences
                tx.clearOldReservations(absenceInserts.flatMap { absence -> absence.dateRange.dates().map { absence.childId to it } })
                // clear all absences within holiday period
                tx.clearAbsencesWithinPeriod(holidayPeriod.period, childIds)

                tx.insertAbsences(PersonId(user.id), absenceInserts)
            }
        }
    }
}

data class ChildHolidays(
    val holidays: List<FiniteDateRange>,
    val freePeriod: FiniteDateRange? = null
)

data class HolidayAbsenceRequest(
    val childHolidays: Map<ChildId, ChildHolidays>
)
