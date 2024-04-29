// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.OfficialLanguage
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class VasuTemplateTest {

    private fun currentlyValidTemplate(): VasuTemplateSummary {
        val today = HelsinkiDateTime.now().toLocalDate()
        return VasuTemplateSummary(
            id = VasuTemplateId(UUID.randomUUID()),
            name = "foo",
            valid = FiniteDateRange(today.minusDays(5), today.plusDays(5)),
            type = CurriculumType.DAYCARE,
            language = OfficialLanguage.FI,
            documentCount = 5
        )
    }

    private fun templateUpdate(valid: FiniteDateRange): VasuTemplateUpdate =
        VasuTemplateUpdate(name = "foo", valid = valid)

    private fun assertFailure(template: VasuTemplateSummary, templateUpdate: VasuTemplateUpdate) {
        assertFailsWith<BadRequest> {
            validateTemplateUpdate(template = template, body = templateUpdate)
        }
    }

    private fun assertValid(template: VasuTemplateSummary, templateUpdate: VasuTemplateUpdate) {
        assertTrue { validateTemplateUpdate(template = template, body = templateUpdate) }
    }

    @Test
    fun `start date of a currently valid template cannot be changed`() {
        val today = HelsinkiDateTime.now().toLocalDate()
        val template = currentlyValidTemplate()
        assertFailure(template, templateUpdate(FiniteDateRange(today.minusDays(1), today)))
        assertFailure(
            template,
            templateUpdate(FiniteDateRange(today.plusDays(1), today.plusDays(2)))
        )
        assertValid(
            template,
            templateUpdate(FiniteDateRange(template.valid.start, today.plusDays(2)))
        )
    }

    @Test
    fun `end date of a currently valid template cannot be before yesterday`() {
        val today = HelsinkiDateTime.now().toLocalDate()
        val template = currentlyValidTemplate()
        assertFailure(
            template,
            templateUpdate(FiniteDateRange(template.valid.start, today.minusDays(2)))
        )
        assertValid(
            template,
            templateUpdate(FiniteDateRange(template.valid.start, today.minusDays(1)))
        )
    }

    @Test
    fun `start date of a template valid in the future cannot be changed to the past`() {
        val today = HelsinkiDateTime.now().toLocalDate()
        val template =
            currentlyValidTemplate()
                .copy(valid = FiniteDateRange(today.plusMonths(1), today.plusMonths(2)))
        assertFailure(
            template,
            templateUpdate(FiniteDateRange(today.minusDays(1), template.valid.end))
        )
        assertValid(template, templateUpdate(FiniteDateRange(today, template.valid.end)))
    }

    @Test
    fun `start date of an expired valid template cannot be changed`() {
        val today = HelsinkiDateTime.now().toLocalDate()
        val template =
            currentlyValidTemplate()
                .copy(valid = FiniteDateRange(today.minusMonths(2), today.minusMonths(1)))
        assertFailure(
            template,
            templateUpdate(FiniteDateRange(template.valid.start.plusDays(1), template.valid.end))
        )
        assertFailure(
            template,
            templateUpdate(FiniteDateRange(template.valid.start.minusDays(1), template.valid.end))
        )
        assertValid(
            template,
            templateUpdate(FiniteDateRange(template.valid.start, template.valid.end))
        )
    }

    @Test
    fun `end date of and expired template cannot be advanced`() {
        val today = HelsinkiDateTime.now().toLocalDate()
        val template =
            currentlyValidTemplate()
                .copy(valid = FiniteDateRange(today.minusMonths(2), today.minusMonths(1)))
        assertFailure(
            template,
            templateUpdate(FiniteDateRange(template.valid.start, template.valid.end.minusDays(1)))
        )

        assertValid(
            template,
            templateUpdate(FiniteDateRange(template.valid.start, template.valid.end.plusDays(1)))
        )
    }

    @Test
    fun `name of a used template cannot be changed`() {
        val template = currentlyValidTemplate()
        assertFailure(template, templateUpdate(template.valid).copy(name = "bar"))
    }
}
