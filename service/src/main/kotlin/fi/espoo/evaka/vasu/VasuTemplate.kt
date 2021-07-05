package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.domain.FiniteDateRange
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
