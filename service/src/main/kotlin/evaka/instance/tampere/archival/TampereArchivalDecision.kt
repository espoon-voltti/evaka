// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.archival

import com.profium.reception._2022._03.Collections
import evaka.core.caseprocess.CaseProcess
import evaka.core.decision.Decision
import evaka.core.decision.DecisionType
import evaka.core.pis.service.PersonDTO
import evaka.core.s3.Document
import evaka.trevaka.archival.status
import evaka.trevaka.jaxb.localDateToXMLGregorianCalendar
import org.apache.tika.mime.MimeTypes

internal fun transform(
    caseProcess: CaseProcess,
    decision: Decision,
    document: Document,
    child: PersonDTO,
): Pair<Collections.Collection, Map<String, Document>> {
    val originalId = decision.id.toString()
    val title = title(decision)
    val decisionSentDate =
        decision.sentDate?.let { localDateToXMLGregorianCalendar(it) }
            ?: error("Decision sent date missing, decision: ${decision.id}")
    return Collections.Collection().apply {
        type = "record"
        folder = caseProcess.processDefinitionNumber
        metadata =
            Collections.Collection.Metadata().apply {
                this.title =
                    "$title, ${status(decision)}, ${child.firstName} ${child.lastName}, ${child.dateOfBirth.format(ARCHIVAL_DATE_FORMATTER)}"
                calculationBaseDate = decisionSentDate
                created = decisionSentDate
            }
        content =
            Collections.Collection.Content().apply {
                file.add(
                    Collections.Collection.Content.File().apply {
                        val mimeTypes = MimeTypes.getDefaultMimeTypes()
                        val mimeType = mimeTypes.forName(document.contentType)
                        name = "$title${mimeType.extension}"
                        this.originalId = originalId
                    }
                )
            }
    } to mapOf(originalId to document)
}

private fun title(decision: Decision): String =
    when (decision.type) {
        DecisionType.CLUB -> "Kerhopäätös"
        DecisionType.DAYCARE -> "Varhaiskasvatuspäätös"
        DecisionType.DAYCARE_PART_TIME -> "Varhaiskasvatuspäätös"
        DecisionType.PRESCHOOL -> "Esiopetuspäätös"
        DecisionType.PRESCHOOL_DAYCARE -> "Täydentävän varhaiskasvatuksen päätös"
        DecisionType.PRESCHOOL_CLUB -> "Esiopetuksen kerhon päätös"
        DecisionType.PREPARATORY_EDUCATION ->
            throw UnsupportedOperationException("Preparatory education")
    }
