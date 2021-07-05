package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.json.Json
import java.util.UUID

data class VasuTemplate(
    val id: UUID,
    val name: String,
    val valid: FiniteDateRange,
    val language: VasuLanguage,
    @Json
    val content: VasuContent
)

data class VasuTemplateSummary(
    val id: UUID,
    val name: String,
    val valid: FiniteDateRange,
    val language: VasuLanguage,
    val documentCount: Int
)

data class VasuTemplateUpdate(
    val name: String,
    val valid: FiniteDateRange,
    val language: VasuLanguage
)

fun validateTemplateUpdate(
    template: VasuTemplateSummary,
    body: VasuTemplateUpdate,
) {
    if (template.documentCount == 0) {
        return
    }
    val now = HelsinkiDateTime.now().toLocalDate()
    if (now.isAfter(template.valid.end)) {
        if (body.valid.start != template.valid.start) throw Forbidden("Start date of an expired valid template cannot be changed")
        if (body.valid.end.isBefore(template.valid.end)) throw Forbidden("End date of and expired template cannot be advanced")
    } else if (now.isBefore(template.valid.start)) {
        if (body.valid.start.isBefore(now)) throw Forbidden("Start date of a template valid in the future cannot be changed to the past")
    } else {
        if (body.valid.start != template.valid.start) throw Forbidden("Start date of a currently valid template cannot be changed")
        if (body.valid.end.isBefore(now.minusDays(1))) throw Forbidden("End date of a currently valid template cannot be before yesterday")
    }
}
