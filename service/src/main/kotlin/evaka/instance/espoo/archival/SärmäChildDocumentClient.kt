// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.archival

import evaka.core.ArchiveEnv
import evaka.core.caseprocess.*
import evaka.core.decision.Decision
import evaka.core.document.archival.ArchivalClient
import evaka.core.document.archival.ArchivalIntegrationClient
import evaka.core.document.childdocument.*
import evaka.core.invoicing.domain.FeeDecisionDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionDetailed
import evaka.core.pis.service.PersonDTO
import evaka.core.s3.Document
import evaka.core.shared.ChildDocumentId
import evaka.core.user.EvakaUser
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import org.apache.http.client.utils.URLEncodedUtils

private val logger = KotlinLogging.logger {}

class SärmäChildDocumentClient(
    private val uploadClient: ArchivalClient,
    private val archiveEnv: ArchiveEnv,
) : ArchivalIntegrationClient {
    override fun uploadDecisionToArchive(
        caseProcess: CaseProcess,
        child: PersonDTO,
        decision: Decision,
        document: Document,
        user: EvakaUser,
    ): String {
        TODO("Not yet implemented")
    }

    override fun uploadFeeDecisionToArchive(
        caseProcess: CaseProcess,
        decision: FeeDecisionDetailed,
        document: Document,
        user: EvakaUser,
    ): String {
        TODO("Not yet implemented")
    }

    override fun uploadVoucherValueDecisionToArchive(
        caseProcess: CaseProcess,
        decision: VoucherValueDecisionDetailed,
        document: Document,
        user: EvakaUser,
    ): String {
        TODO("Not yet implemented")
    }

    override fun uploadChildDocumentToArchive(
        documentId: ChildDocumentId,
        caseProcess: CaseProcess?,
        childInfo: PersonDTO,
        childDocumentDetails: ChildDocumentDetails,
        documentMetadata: DocumentMetadata,
        documentContent: Document,
        evakaUser: EvakaUser,
    ): String? {
        val classId =
            childDocumentDetails.template.processDefinitionNumber
                ?: throw IllegalStateException(
                    "No class ID found in document template ${childDocumentDetails.template.id}"
                )

        val metadata =
            createDocumentMetadata(
                childDocumentDetails,
                documentMetadata,
                caseProcess,
                Paths.get(documentContent.name).fileName.toString(),
                childInfo.identity,
                childInfo.dateOfBirth,
                masterId = archiveEnv.masterId,
                virtualArchiveId = archiveEnv.virtualArchiveId,
            )
        val metadataXml =
            marshalMetadata(
                metadata,
                mainNamespace = archiveEnv.metadataMainNamespace,
                policyNamespace = archiveEnv.metadataPolicyNamespace,
            )

        val (responseCode, responseBody) =
            uploadClient.putDocument(
                documentContent,
                metadataXml,
                archiveEnv.masterId,
                classId,
                archiveEnv.virtualArchiveId,
            )
        logger.info { "HTTP response code: $responseCode" }
        logger.info { "Response body: $responseBody" }

        val responseData = URLEncodedUtils.parse(responseBody, StandardCharsets.UTF_8)
        val statusCode = responseData.find { it.name == "status_code" }?.value?.toIntOrNull()

        val instanceId = responseData.find { it.name == "instance_ids" }?.value

        logger.info(mapOf("documentId" to documentId)) {
            "Parsed status code from response body: $statusCode"
        }

        // Audit log the instance ID
        if (instanceId == null) {
            logger.error { "No instance ID found in response body" }
        } else {
            logger.info(mapOf("documentId" to documentId, "instanceId" to instanceId)) {
                "Parsed instance ID from response body: $instanceId"
            }
        }

        if (responseCode != 200 || statusCode != 200) {
            logger.error {
                "Failed to archive document $documentId. Response code: $responseCode, Response body: ${responseBody ?: "No response body"}, Parsed status code: $statusCode"
            }
            throw RuntimeException("Failed to archive document $documentId")
        }

        return instanceId
    }
}
