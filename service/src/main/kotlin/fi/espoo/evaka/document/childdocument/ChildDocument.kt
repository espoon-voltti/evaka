package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.document.DocumentTemplate
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.PersonId
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json

data class AnsweredQuestion(val questionId: String, val answer: Any)

@Json data class DocumentContent(val answers: List<AnsweredQuestion>)

data class ChildDocumentSummary(
    val id: ChildDocumentId,
    val type: DocumentType,
    val published: Boolean
)

data class ChildBasics(
    val id: PersonId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate?
)

data class ChildDocumentDetails(
    val id: ChildDocumentId,
    val published: Boolean,
    @Json val content: DocumentContent,
    @Nested("child") val child: ChildBasics,
    @Nested("template") val template: DocumentTemplate
)

data class ChildDocumentCreateRequest(val childId: PersonId, val templateId: DocumentTemplateId)
