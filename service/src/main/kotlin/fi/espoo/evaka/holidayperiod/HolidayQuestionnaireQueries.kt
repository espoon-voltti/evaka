// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.FiniteDateRange
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

fun Database.Read.getChildrenWithContinuousPlacement(guardianId: PersonId, period: FiniteDateRange): List<ChildId> {
    return createQuery(
        """
SELECT g.child_id
FROM guardian g, generate_series(:periodStart::date, :periodEnd::date, '1 day') d
WHERE g.guardian_id = :guardianId
GROUP BY g.child_id
HAVING bool_and(d::date <@ ANY (
    SELECT daterange(p.start_date, p.end_date, '[]')
    FROM placement p
    WHERE p.child_id = g.child_id
))
"""
    )
        .bind("guardianId", guardianId)
        .bind("periodStart", period.start)
        .bind("periodEnd", period.end)
        .mapTo<ChildId>()
        .toList()
}

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

fun Database.Transaction.updateFixedPeriodQuestionnaire(
    id: HolidayQuestionnaireId,
    data: FixedPeriodQuestionnaireBody
) =
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

fun Database.Transaction.insertQuestionnaireAnswers(modifiedBy: PersonId, answers: List<HolidayQuestionnaireAnswer>) {
    val batch = prepareBatch(
        """
INSERT INTO holiday_questionnaire_answer (
    questionnaire_id,
    child_id,
    fixed_period,
    modified_by
)
VALUES (
    :questionnaireId,
    :childId,
    :fixedPeriod,
    :modifiedBy
)
ON CONFLICT(questionnaire_id, child_id)
    DO UPDATE SET fixed_period = EXCLUDED.fixed_period,
                  modified_by  = EXCLUDED.modified_by
"""
    )

    answers.forEach { answer ->
        batch.bindKotlin(answer)
            .bind("modifiedBy", modifiedBy)
            .add()
    }

    batch.execute()
}

fun Database.Read.getQuestionnaireAnswers(id: HolidayQuestionnaireId, childIds: List<ChildId>): List<HolidayQuestionnaireAnswer> =
    this.createQuery(
        """
SELECT questionnaire_id, child_id, fixed_period
FROM holiday_questionnaire_answer
WHERE questionnaire_id = :questionnaireId AND child_id = ANY(:childIds)
        """.trimIndent()
    )
        .bind("questionnaireId", id)
        .bind("childIds", childIds.toTypedArray())
        .mapTo<HolidayQuestionnaireAnswer>()
        .list()
