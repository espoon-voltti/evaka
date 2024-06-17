// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.user.EvakaUser
import org.jdbi.v3.core.mapper.Nested
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/process-metadata")
class ProcessMetadataController(private val accessControl: AccessControl) {
    data class ProcessMetadata(val process: ArchivedProcess, val primaryDocument: DocumentMetadata)

    data class DocumentMetadata(
        val name: String,
        val createdAt: HelsinkiDateTime?,
        @Nested("created_by") val createdBy: EvakaUser?,
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
                    val process =
                        tx.getArchiveProcessByChildDocumentId(childDocumentId)
                            ?: return@read ProcessMetadataResponse(null)
                    val document = tx.getChildDocumentMetadata(childDocumentId)
                    ProcessMetadataResponse(
                        ProcessMetadata(
                            process = process,
                            primaryDocument =
                                document.copy(
                                    downloadPath =
                                        document.downloadPath?.takeIf {
                                            accessControl.hasPermissionFor(
                                                tx,
                                                user,
                                                clock,
                                                Action.ChildDocument.DOWNLOAD,
                                                childDocumentId
                                            )
                                        }
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
                    val decisionDocument = tx.getAssistanceNeedDecisionDocumentMetadata(decisionId)

                    ProcessMetadataResponse(
                        ProcessMetadata(
                            process = process,
                            primaryDocument =
                                decisionDocument.copy(
                                    downloadPath =
                                        decisionDocument.downloadPath?.takeIf {
                                            accessControl.hasPermissionFor(
                                                tx,
                                                user,
                                                clock,
                                                Action.AssistanceNeedDecision.DOWNLOAD,
                                                decisionId
                                            )
                                        }
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
                    val decisionDocument =
                        tx.getAssistanceNeedPreschoolDecisionDocumentMetadata(decisionId)

                    ProcessMetadataResponse(
                        ProcessMetadata(
                            process = process,
                            primaryDocument =
                                decisionDocument.copy(
                                    downloadPath =
                                        decisionDocument.downloadPath?.takeIf {
                                            accessControl.hasPermissionFor(
                                                tx,
                                                user,
                                                clock,
                                                Action.AssistanceNeedPreschoolDecision.DOWNLOAD,
                                                decisionId
                                            )
                                        }
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

    private fun Database.Read.getChildDocumentMetadata(
        documentId: ChildDocumentId
    ): DocumentMetadata =
        createQuery {
                sql(
                    """
        SELECT 
            dt.name,
            cd.created AS created_at,
            e.id AS created_by_id,
            e.name AS created_by_name,
            e.type AS created_by_type,
            dt.confidential,
            cd.process_id,
            CASE WHEN cd.document_key IS NOT NULL 
                THEN '/employee/child-documents/' || cd.id || '/pdf'
            END AS download_path
        FROM child_document cd
        JOIN document_template dt ON dt.id = cd.template_id
        LEFT JOIN evaka_user e ON e.employee_id = cd.created_by
        WHERE cd.id = ${bind(documentId)}
    """
                )
            }
            .exactlyOne()

    private fun Database.Read.getAssistanceNeedDecisionDocumentMetadata(
        decisionId: AssistanceNeedDecisionId
    ): DocumentMetadata =
        createQuery {
                sql(
                    """
        SELECT 
            'Päätös tuesta varhaiskasvatuksessa' AS name,
            d.created AS created_at,
            e.id AS created_by_id,
            e.name AS created_by_name,
            e.type AS created_by_type,
            TRUE AS confidential,
            CASE WHEN d.document_key IS NOT NULL 
                THEN '/employee/assistance-need-decision/' || d.id || '/pdf'
            END AS download_path
        FROM assistance_need_decision d
        LEFT JOIN evaka_user e ON e.employee_id = d.created_by
        WHERE d.id = ${bind(decisionId)}
    """
                )
            }
            .exactlyOne()

    private fun Database.Read.getAssistanceNeedPreschoolDecisionDocumentMetadata(
        decisionId: AssistanceNeedPreschoolDecisionId
    ): DocumentMetadata =
        createQuery {
                sql(
                    """
        SELECT 
            'Päätös tuesta esiopetuksessa' AS name,
            d.created AS created_at,
            e.id AS created_by_id,
            e.name AS created_by_name,
            e.type AS created_by_type,
            TRUE AS confidential,
            CASE WHEN d.document_key IS NOT NULL 
                THEN '/employee/assistance-need-preschool-decisions/' || d.id || '/pdf'
            END AS download_path
        FROM assistance_need_preschool_decision d
        LEFT JOIN evaka_user e ON e.employee_id = d.created_by
        WHERE d.id = ${bind(decisionId)}
    """
                )
            }
            .exactlyOne()
}
