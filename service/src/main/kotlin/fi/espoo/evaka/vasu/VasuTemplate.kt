// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.OfficialLanguage
import org.jdbi.v3.json.Json

data class VasuTemplate(
    val id: VasuTemplateId,
    val name: String,
    val type: CurriculumType,
    val valid: FiniteDateRange,
    val language: OfficialLanguage,
    @Json val content: VasuContent,
    val documentCount: Int
)

data class VasuTemplateSummary(
    val id: VasuTemplateId,
    val name: String,
    val valid: FiniteDateRange,
    val type: CurriculumType,
    val language: OfficialLanguage,
    val documentCount: Int
)

data class VasuTemplateUpdate(val name: String, val valid: FiniteDateRange)

enum class CurriculumTemplateError {
    EXPIRED_START,
    EXPIRED_END,
    FUTURE_START,
    CURRENT_START,
    CURRENT_END,
    TEMPLATE_NAME
}

fun validateTemplateUpdate(template: VasuTemplateSummary, body: VasuTemplateUpdate): Boolean {
    if (template.documentCount == 0) {
        return true
    }

    if (body.name != template.name)
        throw BadRequest(
            "Name of a used template cannot be changed",
            CurriculumTemplateError.TEMPLATE_NAME.name
        )

    val now = HelsinkiDateTime.now().toLocalDate()
    if (now.isAfter(template.valid.end)) { // template is in past
        if (body.valid.start != template.valid.start)
            throw BadRequest(
                "Start date of an expired valid template cannot be changed",
                CurriculumTemplateError.EXPIRED_START.name
            )
        if (body.valid.end.isBefore(template.valid.end))
            throw BadRequest(
                "End date of and expired template cannot be advanced",
                CurriculumTemplateError.EXPIRED_END.name
            )
    } else if (now.isBefore(template.valid.start)) { // template is in future
        if (body.valid.start.isBefore(now))
            throw BadRequest(
                "Start date of a template valid in the future cannot be changed to the past",
                CurriculumTemplateError.FUTURE_START.name
            )
    } else { // template is active
        if (body.valid.start != template.valid.start)
            throw BadRequest(
                "Start date of a currently valid template cannot be changed",
                CurriculumTemplateError.CURRENT_START.name
            )
        if (body.valid.end.isBefore(now.minusDays(1)))
            throw BadRequest(
                "End date of a currently valid template cannot be before yesterday",
                CurriculumTemplateError.CURRENT_END.name
            )
    }
    return true
}
