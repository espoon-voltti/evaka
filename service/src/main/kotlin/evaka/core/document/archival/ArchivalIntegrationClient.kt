// SPDX-FileCopyrightText: 2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.document.archival

import evaka.core.caseprocess.CaseProcess
import evaka.core.caseprocess.DocumentMetadata
import evaka.core.decision.Decision
import evaka.core.document.childdocument.ChildDocumentDetails
import evaka.core.invoicing.domain.FeeDecisionDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionDetailed
import evaka.core.pis.service.PersonDTO
import evaka.core.s3.Document
import evaka.core.shared.ChildDocumentId
import evaka.core.user.EvakaUser

interface ArchivalIntegrationClient {
    fun uploadDecisionToArchive(
        caseProcess: CaseProcess,
        child: PersonDTO,
        decision: Decision,
        document: Document,
        user: EvakaUser,
    ): String?

    fun uploadFeeDecisionToArchive(
        caseProcess: CaseProcess,
        decision: FeeDecisionDetailed,
        document: Document,
        user: EvakaUser,
    ): String?

    fun uploadVoucherValueDecisionToArchive(
        caseProcess: CaseProcess,
        decision: VoucherValueDecisionDetailed,
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

    class MockClient : ArchivalIntegrationClient {
        override fun uploadDecisionToArchive(
            caseProcess: CaseProcess,
            child: PersonDTO,
            decision: Decision,
            document: Document,
            user: EvakaUser,
        ): String = "mock-instance-id"

        override fun uploadFeeDecisionToArchive(
            caseProcess: CaseProcess,
            decision: FeeDecisionDetailed,
            document: Document,
            user: EvakaUser,
        ): String = "mock-instance-id"

        override fun uploadVoucherValueDecisionToArchive(
            caseProcess: CaseProcess,
            decision: VoucherValueDecisionDetailed,
            document: Document,
            user: EvakaUser,
        ): String = "mock-instance-id"

        override fun uploadChildDocumentToArchive(
            documentId: ChildDocumentId,
            caseProcess: CaseProcess?,
            childInfo: PersonDTO,
            childDocumentDetails: ChildDocumentDetails,
            documentMetadata: DocumentMetadata,
            documentContent: Document,
            evakaUser: EvakaUser,
        ): String = "mock-instance-id"
    }

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

        override fun uploadFeeDecisionToArchive(
            caseProcess: CaseProcess,
            decision: FeeDecisionDetailed,
            document: Document,
            user: EvakaUser,
        ): String {
            throw RuntimeException("Fee decision archival not in use")
        }

        override fun uploadVoucherValueDecisionToArchive(
            caseProcess: CaseProcess,
            decision: VoucherValueDecisionDetailed,
            document: Document,
            user: EvakaUser,
        ): String {
            throw RuntimeException("Voucher value decision archival not in use")
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
