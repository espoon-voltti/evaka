// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Translatable
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json

enum class QuestionnaireType : DatabaseEnum {
    FIXED_PERIOD,
    OPEN_RANGES;

    override val sqlType: String = "questionnaire_type"
}

data class QuestionnaireConditions(
    val continuousPlacement: FiniteDateRange? = null
)

// TODO use sealed class when OPEN_RANGES is implemented
data class FixedPeriodQuestionnaire(
    val id: HolidayQuestionnaireId,
    val type: QuestionnaireType,
    val absenceType: AbsenceType,
    val requiresStrongAuth: Boolean,
    val active: FiniteDateRange,
    @Json val title: Translatable,
    @Json val description: Translatable,
    @Json val descriptionLink: Translatable,
    /**
     * Conditions are optional and will prevent the questionnaire from being shown unless all
     * conditions are satisfied.
     */
    @Nested("condition") val conditions: QuestionnaireConditions,
    // fixed period specific
    val periodOptions: List<FiniteDateRange>,
    @Json val periodOptionLabel: Translatable
)

data class FixedPeriodQuestionnaireBody(
    val absenceType: AbsenceType,
    val requiresStrongAuth: Boolean,
    val active: FiniteDateRange,
    @Json val title: Translatable,
    @Json val description: Translatable,
    @Json val descriptionLink: Translatable,
    val conditions: QuestionnaireConditions,
    val periodOptions: List<FiniteDateRange>,
    @Json val periodOptionLabel: Translatable
)

data class HolidayQuestionnaireAnswer(
    val questionnaireId: HolidayQuestionnaireId,
    val childId: ChildId,
    val fixedPeriod: FiniteDateRange?
)
