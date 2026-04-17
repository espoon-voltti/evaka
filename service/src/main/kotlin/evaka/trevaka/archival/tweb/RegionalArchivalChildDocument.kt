// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.archival.tweb

import com.profium.reception._2022._03.Collections
import evaka.core.caseprocess.CaseProcess
import evaka.core.document.childdocument.ChildDocumentDetails
import evaka.core.pis.service.PersonDTO
import evaka.core.s3.Document
import evaka.trevaka.archival.status
import evaka.trevaka.jaxb.localDateToXMLGregorianCalendar
import org.apache.tika.mime.MimeTypes

internal fun transformChildDocument(
    childDocumentDetails: ChildDocumentDetails,
    document: Document,
    childInfo: PersonDTO,
    caseProcess: CaseProcess,
): Pair<Collections.Collection, Map<String, Document>> {
    val originalId = childDocumentDetails.id.toString()
    return Collections.Collection().apply {
        type = "record"
        folder = childDocumentDetails.template.processDefinitionNumber
        metadata =
            Collections.Collection.Metadata().apply {
                title =
                    childTitle(
                        childDocumentDetails.template.name,
                        status(childDocumentDetails),
                        childInfo,
                    )
                calculationBaseDate =
                    localDateToXMLGregorianCalendar(childDocumentDetails.template.validity.start)
                created =
                    childDocumentDetails.publishedAt?.toLocalDate()?.let {
                        localDateToXMLGregorianCalendar(it)
                    }
                agent.addAll(transformToAgents(caseProcess))
            }
        content =
            Collections.Collection.Content().apply {
                file.add(
                    Collections.Collection.Content.File().apply {
                        val mimeTypes = MimeTypes.getDefaultMimeTypes()
                        val mimeType = mimeTypes.forName(document.contentType)
                        name = "$originalId${mimeType.extension}"
                        this.originalId = originalId
                    }
                )
            }
    } to mapOf(originalId to document)
}
