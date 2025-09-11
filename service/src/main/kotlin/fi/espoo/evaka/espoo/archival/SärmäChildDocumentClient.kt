// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.archival

import fi.espoo.evaka.caseprocess.*
import fi.espoo.evaka.document.archival.ArchivalClient
import fi.espoo.evaka.document.archival.ArchivalIntegrationClient
import fi.espoo.evaka.document.childdocument.*
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.s3.DocumentKey
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import org.apache.http.client.utils.URLEncodedUtils

private val logger = KotlinLogging.logger {}

class SärmäChildDocumentClient(
    private val uploadClient: ArchivalClient,
    private val documentClient: DocumentService,
) : ArchivalIntegrationClient {

    override fun uploadChildDocumentToArchive(
        documentId: ChildDocumentId,
        caseProcess: CaseProcess?,
        childInfo: PersonDTO,
        childDocumentDetails: ChildDocumentDetails,
        documentMetadata: DocumentMetadata,
        documentKey: String,
    ): String? {
        // Get the document from the original location
        val originalLocation = documentClient.locate(DocumentKey.ChildDocument(documentKey))
        val documentContent = documentClient.get(originalLocation)
        val masterId =
            "yleinen" // Defined as fixed value in Evaka_Särmä_metatietomääritykset.xlsx specs. TODO
        // should be mapped from metadata
        val classId =
            childDocumentDetails.template.processDefinitionNumber
                ?: throw IllegalStateException(
                    "No class ID found in document template ${childDocumentDetails.template.id}"
                )
        val virtualArchiveId =
            "YLEINEN" // Defined as fixed value in Evaka_Särmä_metatietomääritykset.xlsx specs. TODO
        // should be mapped from metadata

        // Create metadata object and convert to XML
        val metadata =
            createDocumentMetadata(
                childDocumentDetails,
                documentMetadata,
                caseProcess,
                Paths.get(documentContent.name).fileName.toString(),
                childInfo.identity,
                childInfo.dateOfBirth,
            )
        val metadataXml = marshalMetadata(metadata)

        val (responseCode, responseBody) =
            uploadClient.putDocument(
                documentContent,
                metadataXml,
                masterId,
                classId,
                virtualArchiveId,
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
