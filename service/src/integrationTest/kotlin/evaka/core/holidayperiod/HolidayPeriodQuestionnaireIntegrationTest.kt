// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.holidayperiod

import evaka.core.PureJdbiTest
import evaka.core.absence.AbsenceType
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.Translatable
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class HolidayPeriodQuestionnaireIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    val emptyTranslatable = Translatable("", "", "")
    val summerRange =
        FiniteDateRange(start = LocalDate.of(2021, 6, 1), end = LocalDate.of(2021, 8, 31))
    val active = FiniteDateRange(summerRange.start.minusMonths(2), summerRange.start.minusMonths(1))

    @Test
    fun `fixed period questionnaires can be saved`() {
        val options =
            listOf(
                FiniteDateRange(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 7)),
                FiniteDateRange(LocalDate.of(2021, 7, 8), LocalDate.of(2021, 7, 14)),
            )
        val summerQuestionnaire =
            createFixedPeriodQuestionnaire(
                QuestionnaireBody.FixedPeriodQuestionnaireBody(
                    description =
                        Translatable("Varaathan \n 'quote' \"double\" loma-aikasi", "", ""),
                    active = active,
                    descriptionLink = emptyTranslatable,
                    periodOptionLabel = emptyTranslatable,
                    periodOptions = options,
                    title = Translatable("8 vko maksuton", "8 veckor gratis", "8 weeks free"),
                    conditions =
                        QuestionnaireConditions(
                            continuousPlacement =
                                FiniteDateRange(LocalDate.of(2020, 9, 1), summerRange.end)
                        ),
                    absenceType = AbsenceType.FREE_ABSENCE,
                    requiresStrongAuth = false,
                )
            )

        assertEquals("8 vko maksuton", summerQuestionnaire.title.fi)
        assertEquals(active, summerQuestionnaire.active)
        assertEquals(options, summerQuestionnaire.periodOptions)
    }

    @Test
    fun `open ranges questionnaires can be saved`() {
        val period = FiniteDateRange(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 8, 31))
        val summerQuestionnaire =
            createOpenRangesQuestionnaire(
                QuestionnaireBody.OpenRangesQuestionnaireBody(
                    description =
                        Translatable("Varaathan \n 'quote' \"double\" loma-aikasi", "", ""),
                    active = active,
                    descriptionLink = emptyTranslatable,
                    period = period,
                    absenceTypeThreshold = 10,
                    title = Translatable("avoin maksuton", "öppna gratis", "open free"),
                    conditions =
                        QuestionnaireConditions(
                            continuousPlacement =
                                FiniteDateRange(LocalDate.of(2020, 9, 1), summerRange.end)
                        ),
                    absenceType = AbsenceType.FREE_ABSENCE,
                    requiresStrongAuth = false,
                )
            )

        assertEquals("avoin maksuton", summerQuestionnaire.title.fi)
        assertEquals(active, summerQuestionnaire.active)
        assertEquals(period, summerQuestionnaire.period)
    }

    private fun createFixedPeriodQuestionnaire(
        body: QuestionnaireBody.FixedPeriodQuestionnaireBody
    ) = db.transaction {
        it.createFixedPeriodQuestionnaire(body).let { id -> it.getFixedPeriodQuestionnaire(id)!! }
    }

    private fun createOpenRangesQuestionnaire(body: QuestionnaireBody.OpenRangesQuestionnaireBody) =
        db.transaction {
            it.createOpenRangesQuestionnaire(body).let { id -> it.getOpenRangesQuestionnaire(id)!! }
        }
}
