// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.archival

import com.profium.reception._2022._03.Collections
import com.profium.sahke2.Agent
import evaka.core.document.childdocument.ChildDocumentDetails
import evaka.core.s3.Document
import evaka.trevaka.archival.status
import evaka.trevaka.jaxb.localDateToXMLGregorianCalendar
import java.time.LocalDate
import org.apache.tika.mime.MimeTypes

internal fun transform(
    childDocumentDetails: ChildDocumentDetails,
    document: Document,
    ownerDetails: OwnerDetails,
    authorDetails: List<AuthorDetails>,
): Pair<Collections.Collection, Map<String, Document>> {
    val originalId = childDocumentDetails.id.toString()
    val publicationDate =
        childDocumentDetails.publishedAt?.toLocalDate()?.let { localDateToXMLGregorianCalendar(it) }
            ?: error(
                "Document publication date missing, child document: ${childDocumentDetails.id}"
            )
    return Collections.Collection().apply {
        type = "record"
        folder = childDocumentDetails.template.processDefinitionNumber
        metadata =
            Collections.Collection.Metadata().apply {
                title =
                    "${childDocumentDetails.template.name}, ${status(childDocumentDetails)}, ${ownerDetails.name}, ${ownerDetails.dateOfBirth.format(ARCHIVAL_DATE_FORMATTER)}"
                calculationBaseDate = publicationDate
                created = publicationDate
                authorDetails.forEach {
                    agent.add(
                        Agent().apply {
                            agentRole = it.role
                            agentName = it.name
                            // agent corporateName left empty
                        }
                    )
                }
            }
        content =
            Collections.Collection.Content().apply {
                file.add(
                    Collections.Collection.Content.File().apply {
                        val mimeTypes = MimeTypes.getDefaultMimeTypes()
                        val mimeType = mimeTypes.forName(document.contentType)
                        name = "${childDocumentDetails.template.name}${mimeType.extension}"
                        this.originalId = originalId
                    }
                )
            }
    } to mapOf(originalId to document)
}

data class AuthorDetails(val name: String, val role: String)

data class OwnerDetails(val name: String, val dateOfBirth: LocalDate)
