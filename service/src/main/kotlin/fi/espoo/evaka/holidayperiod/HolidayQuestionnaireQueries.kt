// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

private val questionnaireSelect = """
SELECT q.id,
       q.type,
       q.absence_type,
       q.requires_strong_auth,
       q.title,
       q.active,
       q.description,
       q.description_link,
       q.period_options,
       q.period_option_label,
       q.condition_continuous_placement,
       hp.id AS holiday_period_id,
       hp.period
FROM holiday_period_questionnaire q
JOIN holiday_period hp ON q.holiday_period_id = hp.id
""".trimIndent()

fun Database.Read.getActiveFixedPeriodQuestionnaire(date: LocalDate): FixedPeriodQuestionnaire? =
    this.createQuery("$questionnaireSelect WHERE active @> :date")
        .bind("date", date)
        .mapTo<FixedPeriodQuestionnaire>()
        .firstOrNull()

fun Database.Read.getFixedPeriodQuestionnaire(id: HolidayQuestionnaireId): FixedPeriodQuestionnaire? =
    this.createQuery("$questionnaireSelect WHERE q.id = :id")
        .bind("id", id)
        .mapTo<FixedPeriodQuestionnaire>()
        .firstOrNull()

fun Database.Read.getHolidayQuestionnaires(): List<FixedPeriodQuestionnaire> =
    this.createQuery("$questionnaireSelect")
        .mapTo<FixedPeriodQuestionnaire>()
        .list()

fun Database.Transaction.createFixedPeriodQuestionnaire(data: FixedPeriodQuestionnaireBody): HolidayQuestionnaireId =
    this.createQuery(
        """
INSERT INTO holiday_period_questionnaire (
    holiday_period_id,
    type,
    absence_type,
    requires_strong_auth,
    active,
    title,
    description,
    description_link,
    period_options,
    period_option_label,
    condition_continuous_placement
)
VALUES (
    :holidayPeriodId,
    :type,
    :absenceType,
    :requiresStrongAuth,
    :active,
    :title,
    :description,
    :descriptionLink,
    :periodOptions,
    :periodOptionLabel,
    :conditions.continuousPlacement
)
RETURNING id
        """.trimIndent()
    )
        .bindKotlin(data)
        .bind("type", QuestionnaireType.FIXED_PERIOD)
        .mapTo<HolidayQuestionnaireId>()
        .one()

fun Database.Transaction.updateFixedPeriodQuestionnaire(id: HolidayQuestionnaireId, data: FixedPeriodQuestionnaireBody) =
    this.createUpdate(
        """
UPDATE holiday_period_questionnaire
SET
    holiday_period_id = :holidayPeriodId,
    type = :type,
    absence_type = :absenceType,
    requires_strong_auth = :requiresStrongAuth,
    active = :active,
    title = :title,
    description = :description,
    description_link = :descriptionLink,
    period_options = :periodOptions,
    period_option_label = :periodOptionLabel,
    condition_continuous_placement = :conditions.continuousPlacement
WHERE id = :id
    """
            .trimIndent()
    )
        .bindKotlin(data)
        .bind("id", id)
        .bind("type", QuestionnaireType.FIXED_PERIOD)
        .updateExactlyOne()

fun Database.Transaction.deleteHolidayQuestionnaire(id: HolidayQuestionnaireId) =
    this.createUpdate("DELETE FROM holiday_period_questionnaire WHERE id = :id")
        .bind("id", id)
        .execute()
