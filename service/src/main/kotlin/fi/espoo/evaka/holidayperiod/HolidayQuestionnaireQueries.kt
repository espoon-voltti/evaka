// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

private val questionnaireSelect =
    """
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
       q.condition_continuous_placement
FROM holiday_period_questionnaire q
"""
        .trimIndent()

fun Database.Read.getActiveFixedPeriodQuestionnaire(date: LocalDate): FixedPeriodQuestionnaire? =
    @Suppress("DEPRECATION")
    this.createQuery("$questionnaireSelect WHERE active @> :date")
        .bind("date", date)
        .exactlyOneOrNull<FixedPeriodQuestionnaire>()

fun Database.Read.getChildrenWithContinuousPlacement(
    today: LocalDate,
    userId: PersonId,
    period: FiniteDateRange
): List<ChildId> {
    @Suppress("DEPRECATION")
    return createQuery(
            """
WITH children AS (
    SELECT child_id FROM guardian WHERE guardian_id = :userId
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = :userId AND valid_during @> :today
)
SELECT c.child_id
FROM children c, generate_series(:periodStart::date, :periodEnd::date, '1 day') d
GROUP BY c.child_id
HAVING bool_and(d::date <@ ANY (
    SELECT daterange(p.start_date, p.end_date, '[]')
    FROM placement p
    WHERE p.child_id = c.child_id
))
"""
        )
        .bind("today", today)
        .bind("userId", userId)
        .bind("periodStart", period.start)
        .bind("periodEnd", period.end)
        .toList<ChildId>()
}

fun Database.Read.getUserChildIds(today: LocalDate, userId: PersonId): List<ChildId> {
    // language=sql
    val sql =
        """
SELECT child_id FROM guardian WHERE guardian_id = :userId
UNION
SELECT child_id FROM foster_parent WHERE parent_id = :userId AND valid_during @> :today
"""

    @Suppress("DEPRECATION")
    return createQuery(sql).bind("today", today).bind("userId", userId).toList<ChildId>()
}

fun Database.Read.getFixedPeriodQuestionnaire(
    id: HolidayQuestionnaireId
): FixedPeriodQuestionnaire? =
    @Suppress("DEPRECATION")
    this.createQuery("$questionnaireSelect WHERE q.id = :id")
        .bind("id", id)
        .exactlyOneOrNull<FixedPeriodQuestionnaire>()

fun Database.Read.getHolidayQuestionnaires(): List<FixedPeriodQuestionnaire> =
    @Suppress("DEPRECATION")
    this.createQuery("$questionnaireSelect").toList<FixedPeriodQuestionnaire>()

fun Database.Transaction.createFixedPeriodQuestionnaire(
    data: FixedPeriodQuestionnaireBody
): HolidayQuestionnaireId =
    @Suppress("DEPRECATION")
    this.createQuery(
            """
INSERT INTO holiday_period_questionnaire (
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
        """
                .trimIndent()
        )
        .bindKotlin(data)
        .bind("type", QuestionnaireType.FIXED_PERIOD)
        .exactlyOne<HolidayQuestionnaireId>()

fun Database.Transaction.updateFixedPeriodQuestionnaire(
    id: HolidayQuestionnaireId,
    data: FixedPeriodQuestionnaireBody
) =
    @Suppress("DEPRECATION")
    this.createUpdate(
            """
UPDATE holiday_period_questionnaire
SET
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
    @Suppress("DEPRECATION")
    this.createUpdate("DELETE FROM holiday_period_questionnaire WHERE id = :id")
        .bind("id", id)
        .execute()

fun Database.Transaction.insertQuestionnaireAnswers(
    modifiedBy: PersonId,
    answers: List<HolidayQuestionnaireAnswer>
) {
    val batch =
        prepareBatch(
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

    answers.forEach { answer -> batch.bindKotlin(answer).bind("modifiedBy", modifiedBy).add() }

    batch.execute()
}

fun Database.Read.getQuestionnaireAnswers(
    id: HolidayQuestionnaireId,
    childIds: List<ChildId>
): List<HolidayQuestionnaireAnswer> =
    @Suppress("DEPRECATION")
    this.createQuery(
            """
SELECT questionnaire_id, child_id, fixed_period
FROM holiday_questionnaire_answer
WHERE questionnaire_id = :questionnaireId AND child_id = ANY(:childIds)
        """
                .trimIndent()
        )
        .bind("questionnaireId", id)
        .bind("childIds", childIds)
        .toList<HolidayQuestionnaireAnswer>()
