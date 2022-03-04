// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.reservations.AbsenceInsert
import fi.espoo.evaka.reservations.clearFreeAbsencesWithinPeriod
import fi.espoo.evaka.reservations.clearOldAbsences
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

    @PostMapping("/holidays/free-period")
    fun saveFreeHolidayPeriod(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @RequestBody body: FreePeriodsBody
    ) {
        val childIds = body.freePeriods.keys
        Audit.HolidayAbsenceCreate.log(targetId = childIds.toSet().joinToString())
        accessControl.requirePermissionFor(user, Action.Child.CREATE_HOLIDAY_ABSENCE, childIds)

        db.connect { dbc ->
            dbc.transaction { tx ->
                // TODO replace with questionnaire
                val holidayPeriod = tx.getActiveHolidayPeriod(evakaClock.today()) ?: throw BadRequest("No active holiday period")
                val freePeriodInserts = body.freePeriods.entries
                    .mapNotNull { (childId, freePeriod) -> if (freePeriod != null) AbsenceInsert(childId, freePeriod, AbsenceType.FREE_ABSENCE) else null }
                    .onEach { (_, freePeriod) ->
                        if (holidayPeriod.freePeriod == null || !holidayPeriod.freePeriod.periodOptions.contains(freePeriod)) {
                            throw BadRequest("Free holiday period not found")
                        }
                    }

                freePeriodInserts.flatMap { absence -> absence.dateRange.dates().map { absence.childId to it } }.let {
                    // clear reservations on new free period
                    tx.clearOldReservations(it)
                    // clear absences on new free period
                    tx.clearOldAbsences(it)
                }
                // clear all old free absences within holiday period
                tx.clearFreeAbsencesWithinPeriod(holidayPeriod.period, childIds)

                tx.insertAbsences(PersonId(user.id), freePeriodInserts)
            }
        }
    }
}

data class FreePeriodsBody(
    val freePeriods: Map<ChildId, FiniteDateRange?>
)
