// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/holiday-period/questionnaire")
class HolidayQuestionnaireController(private val accessControl: AccessControl) {
    @GetMapping
    fun getQuestionnaires(
        db: Database,
        user: AuthenticatedUser,
    ): List<FixedPeriodQuestionnaire> {
        Audit.HolidayQuestionnairesList.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_HOLIDAY_QUESTIONNAIRES)
        return db.connect { dbc -> dbc.read { it.getHolidayQuestionnaires() } }
    }

    @GetMapping("/{id}")
    fun getQuestionnaire(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: HolidayQuestionnaireId
    ): FixedPeriodQuestionnaire {
        Audit.HolidayQuestionnaireRead.log(id)
        accessControl.requirePermissionFor(user, Action.Global.READ_HOLIDAY_QUESTIONNAIRE)
        return db.connect { dbc -> dbc.read { it.getFixedPeriodQuestionnaire(id) ?: throw NotFound() } }
    }

    @PostMapping
    fun createHolidayQuestionnaire(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: FixedPeriodQuestionnaireBody
    ) {
        Audit.HolidayQuestionnaireCreate.log()
        accessControl.requirePermissionFor(user, Action.Global.CREATE_HOLIDAY_QUESTIONNAIRE)
        return db.connect { dbc ->
            dbc.transaction {
                try {
                    it.createFixedPeriodQuestionnaire(body)
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
    }

    @PutMapping("/{id}")
    fun updateHolidayQuestionnaire(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: HolidayQuestionnaireId,
        @RequestBody body: FixedPeriodQuestionnaireBody
    ) {
        Audit.HolidayQuestionnaireUpdate.log(id)
        accessControl.requirePermissionFor(user, Action.Global.UPDATE_HOLIDAY_QUESTIONNAIRE)
        return db.connect { dbc ->
            dbc.transaction {
                try {
                    it.updateFixedPeriodQuestionnaire(id, body)
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
    }

    @DeleteMapping("/{id}")
    fun deleteHolidayQuestionnaire(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: HolidayQuestionnaireId
    ) {
        Audit.HolidayQuestionnaireDelete.log(id)
        accessControl.requirePermissionFor(user, Action.Global.DELETE_HOLIDAY_QUESTIONNAIRE)
        db.connect { dbc -> dbc.transaction { it.deleteHolidayQuestionnaire(id) } }
    }
}
