// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.service.getGuardianChildIds
import fi.espoo.evaka.reservations.AbsenceInsert
import fi.espoo.evaka.reservations.clearOldCitizenEditableAbsences
import fi.espoo.evaka.reservations.clearOldReservations
import fi.espoo.evaka.reservations.deleteAbsencesCreatedFromQuestionnaire
import fi.espoo.evaka.reservations.insertAbsences
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
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

data class ActiveQuestionnaire(
    val questionnaire: FixedPeriodQuestionnaire,
    val eligibleChildren: List<ChildId>,
    val previousAnswers: List<HolidayQuestionnaireAnswer>
)

@RestController
@RequestMapping("/citizen/holiday-period")
class HolidayPeriodControllerCitizen(private val accessControl: AccessControl) {
    @GetMapping
    fun getHolidayPeriods(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<HolidayPeriod> {
        Audit.HolidayPeriodsList.log()
        accessControl.requirePermissionFor(user, clock, Action.Global.READ_HOLIDAY_PERIODS)
        return db.connect { dbc -> dbc.read { it.getHolidayPeriods() } }
    }

    @GetMapping("/questionnaire")
    fun getActiveQuestionnaires(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<ActiveQuestionnaire> {
        Audit.HolidayPeriodsList.log()
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Global.READ_ACTIVE_HOLIDAY_QUESTIONNAIRES
        )
        return db.connect { dbc ->
            dbc.read { tx ->
                val activeQuestionnaire =
                    tx.getActiveFixedPeriodQuestionnaire(clock.today()) ?: return@read listOf()

                val continuousPlacementPeriod = activeQuestionnaire.conditions.continuousPlacement
                val eligibleChildren =
                    if (continuousPlacementPeriod != null) {
                        tx.getChildrenWithContinuousPlacement(
                            user.id,
                            continuousPlacementPeriod,
                        )
                    } else {
                        tx.getGuardianChildIds(user.id)
                    }
                if (eligibleChildren.isEmpty()) {
                    listOf()
                } else {
                    listOf(
                        ActiveQuestionnaire(
                            questionnaire = activeQuestionnaire,
                            eligibleChildren = eligibleChildren,
                            previousAnswers =
                                tx.getQuestionnaireAnswers(activeQuestionnaire.id, eligibleChildren)
                        )
                    )
                }
            }
        }
    }

    @PostMapping("/questionnaire/fixed-period/{id}")
    fun answerFixedPeriodQuestionnaire(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: HolidayQuestionnaireId,
        @RequestBody body: FixedPeriodsBody
    ) {
        val childIds = body.fixedPeriods.keys
        Audit.HolidayAbsenceCreate.log(id, childIds.toSet().joinToString())
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Citizen.Child.CREATE_HOLIDAY_ABSENCE,
            childIds
        )

        db.connect { dbc ->
            dbc.transaction { tx ->
                val questionnaire =
                    tx.getFixedPeriodQuestionnaire(id)?.also {
                        if (!it.active.includes(clock.today()))
                            throw BadRequest("Questionnaire is not open")
                    }
                        ?: throw BadRequest("Questionnaire not found")
                if (questionnaire.conditions.continuousPlacement != null) {
                    val eligibleChildren =
                        tx.getChildrenWithContinuousPlacement(
                            user.id,
                            questionnaire.conditions.continuousPlacement
                        )
                    if (
                        childIds.any {
                            body.fixedPeriods[it] != null && !eligibleChildren.contains(it)
                        }
                    ) {
                        throw BadRequest("Some children are not eligible to answer")
                    }
                }

                val invalidPeriod =
                    body.fixedPeriods.values.find { !questionnaire.periodOptions.contains(it) }

                if (invalidPeriod != null) {
                    throw BadRequest("Invalid option provided ($invalidPeriod)")
                }

                val absences =
                    body.fixedPeriods.entries.flatMap { (childId, period) ->
                        period?.dates()?.map {
                            AbsenceInsert(
                                childId = childId,
                                date = it,
                                absenceType = questionnaire.absenceType,
                                questionnaireId = questionnaire.id
                            )
                        }
                            ?: emptySequence()
                    }

                absences
                    .map { absence -> absence.childId to absence.date }
                    .let {
                        tx.clearOldReservations(it)
                        tx.clearOldCitizenEditableAbsences(it)
                    }
                tx.deleteAbsencesCreatedFromQuestionnaire(questionnaire.id, childIds)
                tx.insertAbsences(user.id, absences)
                tx.insertQuestionnaireAnswers(
                    user.id,
                    body.fixedPeriods.entries.map { (childId, period) ->
                        HolidayQuestionnaireAnswer(questionnaire.id, childId, period)
                    }
                )
            }
        }
    }
}

data class FixedPeriodsBody(val fixedPeriods: Map<ChildId, FiniteDateRange?>)
