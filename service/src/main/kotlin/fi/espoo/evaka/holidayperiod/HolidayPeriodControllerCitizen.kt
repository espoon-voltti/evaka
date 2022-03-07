// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.Audit
import fi.espoo.evaka.reservations.AbsenceInsert
import fi.espoo.evaka.reservations.clearAbsencesWithinPeriod
import fi.espoo.evaka.reservations.clearOldAbsences
import fi.espoo.evaka.reservations.clearOldReservations
import fi.espoo.evaka.reservations.insertAbsences
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

    @GetMapping("/questionnaire")
    fun getActiveQuestionnaires(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
    ): List<FixedPeriodQuestionnaire> {
        Audit.HolidayPeriodsList.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_ACTIVE_HOLIDAY_QUESTIONNAIRES)
        return db.connect { dbc -> dbc.read { listOfNotNull(it.getActiveFixedPeriodQuestionnaire(evakaClock.today())) } }
    }

    @PostMapping("/questionnaire/fixed-period/{id}")
    fun answerFixedPeriodQuestionnaire(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable id: HolidayQuestionnaireId,
        @RequestBody body: FixedPeriodsBody
    ) {
        val childIds = body.fixedPeriods.keys
        Audit.HolidayAbsenceCreate.log(id, childIds.toSet().joinToString())
        accessControl.requirePermissionFor(user, Action.Child.CREATE_HOLIDAY_ABSENCE, childIds)

        db.connect { dbc ->
            dbc.transaction { tx ->
                val questionnaire = tx.getFixedPeriodQuestionnaire(id)
                    ?.also { if (!it.active.includes(evakaClock.today())) throw BadRequest("Questionnaire is not open") }
                    ?: throw BadRequest("Questionnaire not found")
                val absences = body.fixedPeriods.entries
                    .mapNotNull { (childId, period) -> if (period != null) AbsenceInsert(childId, period, questionnaire.absenceType) else null }
                    .onEach { (_, period) ->
                        if (!questionnaire.periodOptions.contains(period)) {
                            throw BadRequest("Invalid option provided ($period)")
                        }
                    }

                absences.flatMap { absence -> absence.dateRange.dates().map { absence.childId to it } }.let {
                    tx.clearOldReservations(it)
                    tx.clearOldAbsences(it)
                }
                tx.clearAbsencesWithinPeriod(questionnaire.period, questionnaire.absenceType, childIds)
                tx.insertAbsences(PersonId(user.id), absences)
            }
        }
    }
}

data class FixedPeriodsBody(
    val fixedPeriods: Map<ChildId, FiniteDateRange?>
)
