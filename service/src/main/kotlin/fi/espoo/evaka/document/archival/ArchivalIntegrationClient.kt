// SPDX-FileCopyrightText: 2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.caseprocess.CaseProcess
import fi.espoo.evaka.caseprocess.DocumentMetadata
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.document.childdocument.ChildDocumentDetails
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.user.EvakaUser

interface ArchivalIntegrationClient {
    fun uploadDecisionToArchive(
        caseProcess: CaseProcess,
        child: PersonDTO,
        decision: Decision,
        document: Document,
        user: EvakaUser,
    ): String?

    fun uploadChildDocumentToArchive(
        documentId: ChildDocumentId,
        caseProcess: CaseProcess?,
        childInfo: PersonDTO,
        childDocumentDetails: ChildDocumentDetails,
        documentMetadata: DocumentMetadata,
        documentContent: Document,
        evakaUser: EvakaUser,
    ): String?

    class FailingClient() : ArchivalIntegrationClient {
        override fun uploadDecisionToArchive(
            caseProcess: CaseProcess,
            child: PersonDTO,
            decision: Decision,
            document: Document,
            user: EvakaUser,
        ): String {
            throw RuntimeException("Decision archival not in use")
        }

        override fun uploadChildDocumentToArchive(
            documentId: ChildDocumentId,
            caseProcess: CaseProcess?,
            childInfo: PersonDTO,
            childDocumentDetails: ChildDocumentDetails,
            documentMetadata: DocumentMetadata,
            documentContent: Document,
            evakaUser: EvakaUser,
        ): String {
            throw RuntimeException("Child document archival not in use")
        }
    }
}
