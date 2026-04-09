// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.archival.tweb

import com.profium.reception._2022._03.Collections
import evaka.core.caseprocess.CaseProcess
import evaka.core.decision.Decision
import evaka.core.decision.DecisionType
import evaka.core.pis.service.PersonDTO
import evaka.core.s3.Document
import evaka.core.shared.ArchiveProcessType
import evaka.core.shared.FeatureConfig
import evaka.trevaka.archival.status
import evaka.trevaka.jaxb.localDateToXMLGregorianCalendar
import org.apache.tika.mime.MimeTypes

internal fun transformDecision(
    caseProcess: CaseProcess,
    decision: Decision,
    document: Document,
    child: PersonDTO,
    featureConfig: FeatureConfig,
): Pair<Collections.Collection, Map<String, Document>> {
    val originalId = decision.id.toString()
    return Collections.Collection().apply {
        type = "record"
        folder =
            // use application-based process definition number, unless preschool daycare decision
            // as it may also derive from a preschool application
            when (decision.type) {
                DecisionType.PRESCHOOL_DAYCARE ->
                    // yearly config is not currently used
                    featureConfig
                        .archiveMetadataConfigs(ArchiveProcessType.APPLICATION_DAYCARE, 2026)
                        ?.processDefinitionNumber
                        ?: error("No process definition number for daycare application defined")

                else -> caseProcess.processDefinitionNumber
            }
        metadata =
            Collections.Collection.Metadata().apply {
                title = childTitle(type(decision), status(decision), child)
                calculationBaseDate = localDateToXMLGregorianCalendar(decision.endDate.plusDays(1))
                created = decision.sentDate?.let { localDateToXMLGregorianCalendar(it) }
                agent.addAll(transformToAgents(caseProcess, false))
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

private fun type(decision: Decision): String =
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
