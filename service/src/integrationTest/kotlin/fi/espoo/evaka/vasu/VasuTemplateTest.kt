// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class VasuTemplateTest {

    private fun currentlyValidTemplate(): VasuTemplateSummary {
        val today = HelsinkiDateTime.now().toLocalDate()
        return VasuTemplateSummary(
            id = UUID.randomUUID(),
            name = "foo",
            valid = FiniteDateRange(today.minusDays(5), today.plusDays(5)),
            language = VasuLanguage.FI,
            documentCount = 5
        )
    }

    private fun templateUpdate(valid: FiniteDateRange): VasuTemplateUpdate =
        VasuTemplateUpdate(name = "foo", language = VasuLanguage.FI, valid = valid)

    @Test
    fun `start date of a currently valid template cannot be changed`() {
        val today = HelsinkiDateTime.now().toLocalDate()
        val template = currentlyValidTemplate()
        assertThrows<BadRequest> {
            validateTemplateUpdate(
                template = template,
                body = templateUpdate(FiniteDateRange(today.minusDays(1), today))
            )
        }
        assertThrows<BadRequest> {
            validateTemplateUpdate(
                template = template,
                body = templateUpdate(FiniteDateRange(today.plusDays(1), today.plusDays(2)))
            )
        }
        assertDoesNotThrow {
            validateTemplateUpdate(
                template = template,
                body = templateUpdate(FiniteDateRange(template.valid.start, today.plusDays(2)))
            )
        }
    }

    @Test
    fun `end date of a currently valid template cannot be before yesterday`() {
        val today = HelsinkiDateTime.now().toLocalDate()
        val template = currentlyValidTemplate()
        assertThrows<BadRequest> {
            validateTemplateUpdate(
                template = template,
                body = templateUpdate(FiniteDateRange(template.valid.start, today.minusDays(2)))
            )
        }
        assertDoesNotThrow {
            validateTemplateUpdate(
                template = template,
                body = templateUpdate(FiniteDateRange(template.valid.start, today.minusDays(1)))
            )
        }
    }

    @Test
    fun `start date of a template valid in the future cannot be changed to the past`() {
        val today = HelsinkiDateTime.now().toLocalDate()
        val template = currentlyValidTemplate().copy(valid = FiniteDateRange(today.plusMonths(1), today.plusMonths(2)))
        assertThrows<BadRequest> {
            validateTemplateUpdate(
                template = template,
                body = templateUpdate(FiniteDateRange(today.minusDays(1), template.valid.end))
            )
        }
        assertDoesNotThrow {
            validateTemplateUpdate(
                template = template,
                body = templateUpdate(FiniteDateRange(today, template.valid.end))
            )
        }
    }

    @Test
    fun `start date of an expired valid template cannot be changed`() {
        val today = HelsinkiDateTime.now().toLocalDate()
        val template =
            currentlyValidTemplate().copy(valid = FiniteDateRange(today.minusMonths(2), today.minusMonths(1)))
        assertThrows<BadRequest> {
            validateTemplateUpdate(
                template = template,
                body = templateUpdate(FiniteDateRange(template.valid.start.plusDays(1), template.valid.end))
            )
        }
        assertThrows<BadRequest> {
            validateTemplateUpdate(
                template = template,
                body = templateUpdate(FiniteDateRange(template.valid.start.minusDays(1), template.valid.end))
            )
        }
        assertDoesNotThrow {
            validateTemplateUpdate(
                template = template,
                body = templateUpdate(FiniteDateRange(template.valid.start, template.valid.end))
            )
        }
    }

    @Test
    fun `end date of and expired template cannot be advanced`() {
        val today = HelsinkiDateTime.now().toLocalDate()
        val template =
            currentlyValidTemplate().copy(valid = FiniteDateRange(today.minusMonths(2), today.minusMonths(1)))
        assertThrows<BadRequest> {
            validateTemplateUpdate(
                template = template,
                body = templateUpdate(FiniteDateRange(template.valid.start, template.valid.end.minusDays(1)))
            )
        }
        assertDoesNotThrow {
            validateTemplateUpdate(
                template = template,
                body = templateUpdate(FiniteDateRange(template.valid.start, template.valid.end.plusDays(1)))
            )
        }
    }
}
