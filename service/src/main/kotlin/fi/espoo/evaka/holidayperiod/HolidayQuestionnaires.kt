// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
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

data class QuestionnaireConditions(val continuousPlacement: FiniteDateRange? = null)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class HolidayQuestionnaire(val type: QuestionnaireType) {
    abstract val id: HolidayQuestionnaireId
    abstract val absenceType: AbsenceType
    abstract val requiresStrongAuth: Boolean
    abstract val active: FiniteDateRange
    abstract val title: Translatable
    abstract val description: Translatable
    abstract val descriptionLink: Translatable
    /**
     * Conditions are optional and will prevent the questionnaire from being shown unless all
     * conditions are satisfied.
     */
    abstract val conditions: QuestionnaireConditions

    abstract fun getPeriods(): List<FiniteDateRange>

    @JsonTypeName("FIXED_PERIOD")
    data class FixedPeriodQuestionnaire(
        override val id: HolidayQuestionnaireId,
        override val absenceType: AbsenceType,
        override val requiresStrongAuth: Boolean,
        override val active: FiniteDateRange,
        @Json override val title: Translatable,
        @Json override val description: Translatable,
        @Json override val descriptionLink: Translatable,
        @Nested("condition") override val conditions: QuestionnaireConditions,
        val periodOptions: List<FiniteDateRange>,
        @Json val periodOptionLabel: Translatable,
    ) : HolidayQuestionnaire(QuestionnaireType.FIXED_PERIOD) {
        override fun getPeriods(): List<FiniteDateRange> = periodOptions
    }

    @JsonTypeName("OPEN_RANGES")
    data class OpenRangesQuestionnaire(
        override val id: HolidayQuestionnaireId,
        override val absenceType: AbsenceType,
        override val requiresStrongAuth: Boolean,
        override val active: FiniteDateRange,
        @Json override val title: Translatable,
        @Json override val description: Translatable,
        @Json override val descriptionLink: Translatable,
        @Nested("condition") override val conditions: QuestionnaireConditions,
        val period: FiniteDateRange,
        val absenceTypeThreshold: Int,
    ) : HolidayQuestionnaire(QuestionnaireType.OPEN_RANGES) {
        override fun getPeriods(): List<FiniteDateRange> = listOf(period)
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class QuestionnaireBody {
    abstract val absenceType: AbsenceType
    abstract val requiresStrongAuth: Boolean
    abstract val active: FiniteDateRange
    abstract val title: Translatable
    abstract val description: Translatable
    abstract val descriptionLink: Translatable
    abstract val conditions: QuestionnaireConditions

    @JsonTypeName("FIXED_PERIOD")
    data class FixedPeriodQuestionnaireBody(
        override val absenceType: AbsenceType,
        override val requiresStrongAuth: Boolean,
        override val active: FiniteDateRange,
        @Json override val title: Translatable,
        @Json override val description: Translatable,
        @Json override val descriptionLink: Translatable,
        override val conditions: QuestionnaireConditions,
        val periodOptions: List<FiniteDateRange>,
        @Json val periodOptionLabel: Translatable,
    ) : QuestionnaireBody()

    @JsonTypeName("OPEN_RANGES")
    data class OpenRangesQuestionnaireBody(
        override val absenceType: AbsenceType,
        override val requiresStrongAuth: Boolean,
        override val active: FiniteDateRange,
        @Json override val title: Translatable,
        @Json override val description: Translatable,
        @Json override val descriptionLink: Translatable,
        override val conditions: QuestionnaireConditions,
        val period: FiniteDateRange,
        val absenceTypeThreshold: Int,
    ) : QuestionnaireBody()
}

data class HolidayQuestionnaireAnswer(
    val questionnaireId: HolidayQuestionnaireId,
    val childId: ChildId,
    val fixedPeriod: FiniteDateRange?,
    val openRanges: List<FiniteDateRange>,
)
