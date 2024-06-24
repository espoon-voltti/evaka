// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

private fun Database.Read.questionnaireQuery(where: Predicate): Database.Query =
    createQuery {
        sql(
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
WHERE ${predicate(where.forTable("q"))}
"""
        )
    }

fun Database.Read.getActiveFixedPeriodQuestionnaire(date: LocalDate): FixedPeriodQuestionnaire? =
    questionnaireQuery(Predicate { where("$it.active @> ${bind(date)}") })
        .exactlyOneOrNull<FixedPeriodQuestionnaire>()

fun Database.Read.getChildrenWithContinuousPlacement(
    today: LocalDate,
    userId: PersonId,
    period: FiniteDateRange
): List<ChildId> =
    createQuery {
        sql(
            """
WITH children AS (
    SELECT child_id FROM guardian WHERE guardian_id = ${bind(userId)}
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = ${bind(userId)} AND valid_during @> ${bind(today)}
)
SELECT c.child_id
FROM children c, generate_series(${bind(period.start)}::date, ${bind(period.end)}::date, '1 day') d
GROUP BY c.child_id
HAVING bool_and(d::date <@ ANY (
    SELECT daterange(p.start_date, p.end_date, '[]')
    FROM placement p
    WHERE p.child_id = c.child_id
))
"""
        )
    }.toList<ChildId>()

fun Database.Read.getUserChildIds(
    today: LocalDate,
    userId: PersonId
): List<ChildId> =
    createQuery {
        sql(
            """
SELECT child_id FROM guardian WHERE guardian_id = ${bind(userId)}
UNION
SELECT child_id FROM foster_parent WHERE parent_id = ${bind(userId)} AND valid_during @> ${bind(today)}
"""
        )
    }.toList<ChildId>()

fun Database.Read.getFixedPeriodQuestionnaire(id: HolidayQuestionnaireId): FixedPeriodQuestionnaire? =
    questionnaireQuery(Predicate { where("$it.id = ${bind(id)}") })
        .exactlyOneOrNull<FixedPeriodQuestionnaire>()

fun Database.Read.getHolidayQuestionnaires(): List<FixedPeriodQuestionnaire> =
    questionnaireQuery(Predicate.alwaysTrue()).toList<FixedPeriodQuestionnaire>()

fun Database.Transaction.createFixedPeriodQuestionnaire(data: FixedPeriodQuestionnaireBody): HolidayQuestionnaireId =
    createQuery {
        sql(
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
    ${bind(QuestionnaireType.FIXED_PERIOD)},
    ${bind(data.absenceType)},
    ${bind(data.requiresStrongAuth)},
    ${bind(data.active)},
    ${bindJson(data.title)},
    ${bindJson(data.description)},
    ${bindJson(data.descriptionLink)},
    ${bind(data.periodOptions)},
    ${bindJson(data.periodOptionLabel)},
    ${bind(data.conditions.continuousPlacement)}
)
RETURNING id
"""
        )
    }.exactlyOne<HolidayQuestionnaireId>()

fun Database.Transaction.updateFixedPeriodQuestionnaire(
    id: HolidayQuestionnaireId,
    data: FixedPeriodQuestionnaireBody
) = createUpdate {
    sql(
        """
UPDATE holiday_period_questionnaire
SET
    type = ${bind(QuestionnaireType.FIXED_PERIOD)},
    absence_type = ${bind(data.absenceType)},
    requires_strong_auth = ${bind(data.requiresStrongAuth)},
    active = ${bind(data.active)},
    title = ${bindJson(data.title)},
    description = ${bindJson(data.description)},
    description_link = ${bindJson(data.descriptionLink)},
    period_options = ${bind(data.periodOptions)},
    period_option_label = ${bindJson(data.periodOptionLabel)},
    condition_continuous_placement = ${bind(data.conditions.continuousPlacement)}
WHERE id = ${bind(id)}
"""
    )
}.updateExactlyOne()

fun Database.Transaction.deleteHolidayQuestionnaire(id: HolidayQuestionnaireId) =
    createUpdate { sql("DELETE FROM holiday_period_questionnaire WHERE id = ${bind(id)}") }
        .execute()

fun Database.Transaction.insertQuestionnaireAnswers(
    modifiedBy: PersonId,
    answers: List<HolidayQuestionnaireAnswer>
) {
    executeBatch(answers) {
        sql(
            """
INSERT INTO holiday_questionnaire_answer (
    questionnaire_id,
    child_id,
    fixed_period,
    modified_by
)
VALUES (
    ${bind { it.questionnaireId }},
    ${bind { it.childId }},
    ${bind { it.fixedPeriod }},
    ${bind(modifiedBy)}
)
ON CONFLICT(questionnaire_id, child_id)
    DO UPDATE SET fixed_period = EXCLUDED.fixed_period,
                  modified_by  = EXCLUDED.modified_by
"""
        )
    }
}

fun Database.Read.getQuestionnaireAnswers(
    id: HolidayQuestionnaireId,
    childIds: List<ChildId>
): List<HolidayQuestionnaireAnswer> =
    createQuery {
        sql(
            """
SELECT questionnaire_id, child_id, fixed_period
FROM holiday_questionnaire_answer
WHERE questionnaire_id = ${bind(id)} AND child_id = ANY(${bind(childIds)})
        """
        )
    }.toList<HolidayQuestionnaireAnswer>()
