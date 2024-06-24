// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Translatable
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class HolidayPeriodQuestionnaireIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun `fixed period questionnaires can be saved`() {
        val options =
            listOf(
                FiniteDateRange(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 7)),
                FiniteDateRange(LocalDate.of(2021, 7, 8), LocalDate.of(2021, 7, 14))
            )
        val active =
            FiniteDateRange(summerRange.start.minusMonths(2), summerRange.start.minusMonths(1))
        val summerQuestionnaire =
            createFixedPeriodQuestionnaire(
                FixedPeriodQuestionnaireBody(
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
                    requiresStrongAuth = false
                )
            )

        assertEquals("8 vko maksuton", summerQuestionnaire.title.fi)
        assertEquals(active, summerQuestionnaire.active)
        assertEquals(options, summerQuestionnaire.periodOptions)
    }

    private fun createFixedPeriodQuestionnaire(body: FixedPeriodQuestionnaireBody) =
        db.transaction {
            it.createFixedPeriodQuestionnaire(body).let { id ->
                it.getFixedPeriodQuestionnaire(id)!!
            }
        }
}
