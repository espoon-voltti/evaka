// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.ArchivedProcessId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/process-metadata")
class ProcessMetadataController(private val accessControl: AccessControl) {
    data class EmployeeBasics(
        @PropagateNull val id: EmployeeId,
        val firstName: String,
        val lastName: String,
        val email: String?
    )

    data class ProcessMetadata(val process: ArchivedProcess, val primaryDocument: Document)

    data class Document(
        val name: String,
        val createdAt: HelsinkiDateTime?,
        val createdBy: EmployeeBasics?,
        val confidential: Boolean,
        val downloadPath: String?
    )

    // wrapper that is needed because currently returning null
    // from an endpoint is not serialized correctly
    data class ProcessMetadataResponse(val data: ProcessMetadata?)

    @GetMapping("/child-documents/{childDocumentId}")
    fun getChildDocumentMetadata(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childDocumentId: ChildDocumentId
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.READ_METADATA,
                        childDocumentId
                    )
                    val document = tx.getChildDocumentBasics(childDocumentId)
                    val process =
                        document.processId?.let { tx.getProcess(it) }
                            ?: return@read ProcessMetadataResponse(null)
                    val downloadAllowed =
                        accessControl.hasPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.ChildDocument.DOWNLOAD,
                            childDocumentId
                        )
                    ProcessMetadataResponse(
                        ProcessMetadata(
                            process = process,
                            primaryDocument =
                                Document(
                                    name = document.name,
                                    createdAt = document.createdAt,
                                    createdBy = document.createdBy,
                                    confidential = document.confidential,
                                    downloadPath =
                                        "/employee/child-documents/$childDocumentId/pdf"
                                            .takeIf { document.downloadable && downloadAllowed }
                                )
                        )
                    )
                }
            }
            .also { response ->
                Audit.ChildDocumentReadMetadata.log(
                    targetId = AuditId(childDocumentId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke)
                )
            }
    }

    @GetMapping("/assistance-need-decisions/{decisionId}")
    fun getAssistanceNeedDecisionMetadata(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable decisionId: AssistanceNeedDecisionId
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedDecision.READ_METADATA,
                        decisionId
                    )
                    val process =
                        tx.getArchiveProcessByAssistanceNeedDecisionId(decisionId)
                            ?: return@read ProcessMetadataResponse(null)
                    val decision = tx.getAssistanceNeedDecisionBasics(decisionId)
                    val downloadAllowed =
                        accessControl.hasPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.AssistanceNeedDecision.DOWNLOAD,
                            decisionId
                        )
                    ProcessMetadataResponse(
                        ProcessMetadata(
                            process = process,
                            primaryDocument =
                                Document(
                                    name = "Päätös tuesta varhaiskasvatuksessa",
                                    createdAt = decision.createdAt,
                                    createdBy = decision.createdBy,
                                    confidential = true,
                                    downloadPath =
                                        "/employee/assistance-need-decision/$decisionId/pdf"
                                            .takeIf { decision.downloadable && downloadAllowed }
                                )
                        )
                    )
                }
            }
            .also { response ->
                Audit.AssistanceNeedDecisionReadMetadata.log(
                    targetId = AuditId(decisionId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke)
                )
            }
    }

    @GetMapping("/assistance-need-preschool-decisions/{decisionId}")
    fun getAssistanceNeedPreschoolDecisionMetadata(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable decisionId: AssistanceNeedPreschoolDecisionId
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedPreschoolDecision.READ_METADATA,
                        decisionId
                    )
                    val process =
                        tx.getArchiveProcessByAssistanceNeedPreschoolDecisionId(decisionId)
                            ?: return@read ProcessMetadataResponse(null)
                    val decision = tx.getAssistanceNeedPreschoolDecisionBasics(decisionId)
                    val downloadAllowed =
                        accessControl.hasPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.AssistanceNeedPreschoolDecision.DOWNLOAD,
                            decisionId
                        )
                    ProcessMetadataResponse(
                        ProcessMetadata(
                            process = process,
                            primaryDocument =
                                Document(
                                    name = "Päätös tuesta esiopetuksessa",
                                    createdAt = decision.createdAt,
                                    createdBy = decision.createdBy,
                                    confidential = true,
                                    downloadPath =
                                        "/employee/assistance-need-preschool-decisions/$decisionId/pdf"
                                            .takeIf { decision.downloadable && downloadAllowed }
                                )
                        )
                    )
                }
            }
            .also { response ->
                Audit.AssistanceNeedPreschoolDecisionReadMetadata.log(
                    targetId = AuditId(decisionId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke)
                )
            }
    }

    private data class ChildDocumentBasics(
        val name: String,
        val confidential: Boolean,
        val processId: ArchivedProcessId?,
        val createdAt: HelsinkiDateTime?,
        @Nested("created_by") val createdBy: EmployeeBasics?,
        val downloadable: Boolean
    )

    private fun Database.Read.getChildDocumentBasics(
        documentId: ChildDocumentId
    ): ChildDocumentBasics =
        createQuery {
                sql(
                    """
        SELECT 
            dt.name,
            dt.confidential,
            cd.process_id,
            cd.created AS created_at,
            e.id AS created_by_id,
            e.first_name AS created_by_first_name,
            e.last_name AS created_by_last_name,
            e.email AS created_by_email,
            cd.document_key IS NOT NULL AS downloadable
        FROM child_document cd
        JOIN document_template dt ON dt.id = cd.template_id
        LEFT JOIN employee e ON e.id = cd.created_by
        WHERE cd.id = ${bind(documentId)}
    """
                )
            }
            .exactlyOne()

    private data class AssistanceNeedDecisionBasics(
        val createdAt: HelsinkiDateTime?,
        @Nested("created_by") val createdBy: EmployeeBasics?,
        val downloadable: Boolean
    )

    private fun Database.Read.getAssistanceNeedDecisionBasics(
        decisionId: AssistanceNeedDecisionId
    ): AssistanceNeedDecisionBasics =
        createQuery {
                sql(
                    """
        SELECT 
            d.created AS created_at,
            e.id AS created_by_id,
            e.first_name AS created_by_first_name,
            e.last_name AS created_by_last_name,
            e.email AS created_by_email,
            d.document_key IS NOT NULL AS downloadable
        FROM assistance_need_decision d
        LEFT JOIN employee e ON e.id = d.created_by
        WHERE d.id = ${bind(decisionId)}
    """
                )
            }
            .exactlyOne()

    private fun Database.Read.getAssistanceNeedPreschoolDecisionBasics(
        decisionId: AssistanceNeedPreschoolDecisionId
    ): AssistanceNeedDecisionBasics =
        createQuery {
                sql(
                    """
        SELECT 
            d.created AS created_at,
            e.id AS created_by_id,
            e.first_name AS created_by_first_name,
            e.last_name AS created_by_last_name,
            e.email AS created_by_email,
            d.document_key IS NOT NULL AS downloadable
        FROM assistance_need_preschool_decision d
        LEFT JOIN employee e ON e.id = d.created_by
        WHERE d.id = ${bind(decisionId)}
    """
                )
            }
            .exactlyOne()
}
