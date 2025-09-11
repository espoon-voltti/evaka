// SPDX-FileCopyrightText: 2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.caseprocess.CaseProcess
import fi.espoo.evaka.caseprocess.DocumentMetadata
import fi.espoo.evaka.document.childdocument.ChildDocumentDetails
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.user.EvakaUser
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

interface ArchivalIntegrationClient {
    fun uploadChildDocumentToArchive(
        documentId: ChildDocumentId,
        caseProcess: CaseProcess?,
        childInfo: PersonDTO,
        childDocumentDetails: ChildDocumentDetails,
        documentMetadata: DocumentMetadata,
        documentKey: String,
        evakaUser: EvakaUser,
    ): String?

    class MockClient() : ArchivalIntegrationClient {
        override fun uploadChildDocumentToArchive(
            documentId: ChildDocumentId,
            caseProcess: CaseProcess?,
            childInfo: PersonDTO,
            childDocumentDetails: ChildDocumentDetails,
            documentMetadata: DocumentMetadata,
            documentKey: String,
            evakaUser: EvakaUser,
        ): String? {
            logger.info {
                "Mock child document archival implementation, received archival request for $documentId"
            }
            return null
        }
    }

    class FailingClient() : ArchivalIntegrationClient {
        override fun uploadChildDocumentToArchive(
            documentId: ChildDocumentId,
            caseProcess: CaseProcess?,
            childInfo: PersonDTO,
            childDocumentDetails: ChildDocumentDetails,
            documentMetadata: DocumentMetadata,
            documentKey: String,
            evakaUser: EvakaUser,
        ): String {
            throw RuntimeException("Child document archival not in use")
        }
    }
}
