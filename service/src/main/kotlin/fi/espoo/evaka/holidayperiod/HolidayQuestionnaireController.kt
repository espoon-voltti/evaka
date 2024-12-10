// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.EvakaClock
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
@RequestMapping("/employee/holiday-period/questionnaire")
class HolidayQuestionnaireController(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig,
) {
    @GetMapping
    fun getQuestionnaires(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<HolidayQuestionnaire> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_HOLIDAY_QUESTIONNAIRES,
                    )
                    it.getHolidayQuestionnaires(featureConfig.holidayQuestionnaireType)
                }
            }
            .also { Audit.HolidayQuestionnairesList.log(meta = mapOf("count" to it.size)) }
    }

    @GetMapping("/{id}")
    fun getQuestionnaire(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: HolidayQuestionnaireId,
    ): HolidayQuestionnaire {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_HOLIDAY_QUESTIONNAIRE,
                    )
                    when (featureConfig.holidayQuestionnaireType) {
                        QuestionnaireType.FIXED_PERIOD -> it.getFixedPeriodQuestionnaire(id)
                        QuestionnaireType.OPEN_RANGES -> it.getOpenRangesQuestionnaire(id)
                    } ?: throw NotFound()
                }
            }
            .also { Audit.HolidayQuestionnaireRead.log(targetId = AuditId(id)) }
    }

    @PostMapping
    fun createHolidayQuestionnaire(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: QuestionnaireBody,
    ) {
        val id =
            db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.CREATE_HOLIDAY_QUESTIONNAIRE,
                    )
                    try {
                        when (body) {
                            is QuestionnaireBody.FixedPeriodQuestionnaireBody ->
                                it.createFixedPeriodQuestionnaire(body)
                            is QuestionnaireBody.OpenRangesQuestionnaireBody ->
                                it.createOpenRangesQuestionnaire(body)
                        }
                    } catch (e: Exception) {
                        throw mapPSQLException(e)
                    }
                }
            }
        Audit.HolidayQuestionnaireCreate.log(targetId = AuditId(id))
    }

    @PutMapping("/{id}")
    fun updateHolidayQuestionnaire(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: HolidayQuestionnaireId,
        @RequestBody body: QuestionnaireBody,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Global.UPDATE_HOLIDAY_QUESTIONNAIRE,
                )
                try {
                    when (body) {
                        is QuestionnaireBody.FixedPeriodQuestionnaireBody ->
                            it.updateFixedPeriodQuestionnaire(id, body)
                        is QuestionnaireBody.OpenRangesQuestionnaireBody ->
                            it.updateOpenRangesQuestionnaire(id, body)
                    }
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
        Audit.HolidayQuestionnaireUpdate.log(targetId = AuditId(id))
    }

    @DeleteMapping("/{id}")
    fun deleteHolidayQuestionnaire(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: HolidayQuestionnaireId,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Global.DELETE_HOLIDAY_QUESTIONNAIRE,
                )
                it.deleteHolidayQuestionnaire(id)
            }
        }
        Audit.HolidayQuestionnaireDelete.log(targetId = AuditId(id))
    }
}
