// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.archival.tweb

import com.profium.reception._2022._03.Collections
import evaka.core.caseprocess.CaseProcess
import evaka.core.invoicing.domain.VoucherValueDecisionDetailed
import evaka.core.s3.Document
import evaka.trevaka.archival.status
import evaka.trevaka.jaxb.localDateToXMLGregorianCalendar
import org.apache.tika.mime.MimeTypes

internal fun transformVoucherDecision(
    caseProcess: CaseProcess,
    voucherValueDecision: VoucherValueDecisionDetailed,
    document: Document,
): Pair<Collections.Collection, Map<String, Document>> {
    val originalId = voucherValueDecision.id.toString()
    return Collections.Collection().apply {
        type = "record"
        folder = caseProcess.processDefinitionNumber
        metadata =
            Collections.Collection.Metadata().apply {
                title =
                    hofTitle(
                        "Arvopäätös",
                        status(voucherValueDecision),
                        voucherValueDecision.headOfFamily,
                    )
                calculationBaseDate =
                    localDateToXMLGregorianCalendar(voucherValueDecision.validTo.plusDays(1))
                created =
                    voucherValueDecision.approvedAt?.let {
                        localDateToXMLGregorianCalendar(it.toLocalDate())
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
