// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.archival

import com.profium.reception._2022._03.Collections
import evaka.core.caseprocess.CaseProcess
import evaka.core.invoicing.domain.FeeDecisionDetailed
import evaka.core.s3.Document
import evaka.trevaka.archival.status
import evaka.trevaka.jaxb.localDateToXMLGregorianCalendar
import org.apache.tika.mime.MimeTypes

internal fun transform(
    caseProcess: CaseProcess,
    feeDecision: FeeDecisionDetailed,
    document: Document,
): Pair<Collections.Collection, Map<String, Document>> {
    val originalId = feeDecision.id.toString()
    val decisionApprovalDate =
        feeDecision.approvedAt?.let { localDateToXMLGregorianCalendar(it.toLocalDate()) }
            ?: error("Fee decision approval date missing, decision: ${feeDecision.id}")
    return Collections.Collection().apply {
        type = "record"
        folder = caseProcess.processDefinitionNumber
        metadata =
            Collections.Collection.Metadata().apply {
                title =
                    "Maksupäätös, ${status(feeDecision)}, ${feeDecision.headOfFamily.firstName} ${feeDecision.headOfFamily.lastName}, ${feeDecision.headOfFamily.dateOfBirth.format(ARCHIVAL_DATE_FORMATTER)}"
                calculationBaseDate = decisionApprovalDate
                created = decisionApprovalDate
                agent.addAll(
                    createDecisionMakerAgent(
                        feeDecision.financeDecisionHandlerFirstName,
                        feeDecision.financeDecisionHandlerLastName,
                    )
                )
            }
        content =
            Collections.Collection.Content().apply {
                file.add(
                    Collections.Collection.Content.File().apply {
                        val mimeTypes = MimeTypes.getDefaultMimeTypes()
                        val mimeType = mimeTypes.forName(document.contentType)
                        name = "Maksupäätös${mimeType.extension}"
                        this.originalId = originalId
                    }
                )
            }
    } to mapOf(originalId to document)
}
