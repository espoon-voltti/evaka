// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.json.Json

data class VasuTemplate(
    val id: VasuTemplateId,
    val name: String,
    val type: CurriculumType,
    val valid: FiniteDateRange,
    val language: VasuLanguage,
    @Json val content: VasuContent,
    val documentCount: Int
)

data class VasuTemplateSummary(
    val id: VasuTemplateId,
    val name: String,
    val valid: FiniteDateRange,
    val type: CurriculumType,
    val language: VasuLanguage,
    val documentCount: Int
)

data class VasuTemplateUpdate(val name: String, val valid: FiniteDateRange)

fun validateTemplateUpdate(
    template: VasuTemplateSummary,
    body: VasuTemplateUpdate,
): Boolean {
    if (template.documentCount == 0) {
        return true
    }

    if (body.name != template.name)
        throw BadRequest("Name of a used template cannot be changed", "TEMPLATE_NAME")

    val now = HelsinkiDateTime.now().toLocalDate()
    if (now.isAfter(template.valid.end)) { // template is in past
        if (body.valid.start != template.valid.start)
            throw BadRequest(
                "Start date of an expired valid template cannot be changed",
                "EXPIRED_START"
            )
        if (body.valid.end.isBefore(template.valid.end))
            throw BadRequest("End date of and expired template cannot be advanced", "EXPIRED_END")
    } else if (now.isBefore(template.valid.start)) { // template is in future
        if (body.valid.start.isBefore(now))
            throw BadRequest(
                "Start date of a template valid in the future cannot be changed to the past",
                "FUTURE_START"
            )
    } else { // template is active
        if (body.valid.start != template.valid.start)
            throw BadRequest(
                "Start date of a currently valid template cannot be changed",
                "CURRENT_START"
            )
        if (body.valid.end.isBefore(now.minusDays(1)))
            throw BadRequest(
                "End date of a currently valid template cannot be before yesterday",
                "CURRENT_END"
            )
    }
    return true
}
