package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.document.DocumentTemplate
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.PersonId
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json

data class AnsweredQuestion(val questionId: String, val answer: Any)

@Json data class DocumentContent(val answers: List<AnsweredQuestion>)

data class ChildDocument(
    val id: ChildDocumentId,
    val childId: PersonId,
    @Nested("template") val template: DocumentTemplate,
    val published: Boolean,
    @Json val content: DocumentContent
)

data class ChildDocumentCreateRequest(val childId: PersonId, val templateId: DocumentTemplateId)
