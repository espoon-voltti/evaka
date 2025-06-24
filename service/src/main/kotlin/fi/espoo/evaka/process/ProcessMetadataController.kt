// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.document.childdocument.getChildDocument
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.user.EvakaUser
import java.util.UUID
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

enum class DocumentOrigin {
    ELECTRONIC,
    PAPER,
}

enum class SfiMethod {
    ELECTRONIC,
    PAPER_MAIL,
    PENDING,
}

data class SfiDelivery(
    val time: HelsinkiDateTime,
    val method: SfiMethod,
    val recipientName: String,
)

data class DocumentConfidentiality(val durationYears: Int, @PropagateNull val basis: String)

data class DocumentMetadata(
    val documentId: UUID,
    val name: String,
    val createdAt: HelsinkiDateTime?,
    @Nested("created_by") val createdBy: EvakaUser?,
    val confidential: Boolean?,
    @Nested("confidentiality") val confidentiality: DocumentConfidentiality?,
    val downloadPath: String?,
    val receivedBy: DocumentOrigin?,
    val sfiDeliveries: List<SfiDelivery>,
)

data class ProcessMetadata(
    val process: ArchivedProcess,
    val processName: String?,
    val primaryDocument: DocumentMetadata,
    val secondaryDocuments: List<DocumentMetadata>,
) {
    fun redactForCitizen() =
        this.copy(
            process = this.process.copy(history = emptyList(), archiveDurationMonths = null),
            primaryDocument =
                this.primaryDocument.copy(createdBy = null, sfiDeliveries = emptyList()),
            secondaryDocuments =
                this.secondaryDocuments.map {
                    it.copy(createdBy = null, sfiDeliveries = emptyList())
                },
        )
}

// wrapper that is needed because currently returning null
// from an endpoint is not serialized correctly
data class ProcessMetadataResponse(val data: ProcessMetadata?)

@RestController
@RequestMapping("/employee/process-metadata")
class ProcessMetadataController(
    private val accessControl: AccessControl,
    private val processMetadataService: ProcessMetadataService,
) {

    @GetMapping("/child-documents/{childDocumentId}")
    fun getChildDocumentMetadata(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childDocumentId: ChildDocumentId,
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.READ_METADATA,
                        childDocumentId,
                    )
                    val process =
                        tx.getArchiveProcessByChildDocumentId(childDocumentId)
                            ?: return@read ProcessMetadataResponse(null)
                    val type =
                        tx.getChildDocument(childDocumentId)?.template?.type ?: throw NotFound()
                    val document = tx.getChildDocumentMetadata(childDocumentId)
                    ProcessMetadataResponse(
                        ProcessMetadata(
                            process = process,
                            processName =
                                when (type) {
                                    ChildDocumentType.VASU,
                                    ChildDocumentType.MIGRATED_VASU ->
                                        "Lapsen varhaiskasvatussuunnitelma"
                                    ChildDocumentType.LEOPS,
                                    ChildDocumentType.MIGRATED_LEOPS ->
                                        "Lapsen esiopetuksen oppimissuunnitelma"
                                    ChildDocumentType.HOJKS ->
                                        "Henkilökohtainen opetuksen järjestämistä koskeva suunnitelma"
                                    ChildDocumentType.PEDAGOGICAL_ASSESSMENT ->
                                        "Esiopetuksen pedagoginen arvio"
                                    ChildDocumentType.PEDAGOGICAL_REPORT ->
                                        "Esiopetuksen pedagoginen selvitys"
                                    else -> null
                                },
                            primaryDocument =
                                document.copy(
                                    downloadPath =
                                        document.downloadPath?.takeIf {
                                            accessControl.hasPermissionFor(
                                                tx,
                                                user,
                                                clock,
                                                Action.ChildDocument.DOWNLOAD,
                                                childDocumentId,
                                            )
                                        }
                                ),
                            secondaryDocuments = emptyList(),
                        )
                    )
                }
            }
            .also { response ->
                Audit.ChildDocumentReadMetadata.log(
                    targetId = AuditId(childDocumentId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke),
                )
            }
    }

    @GetMapping("/assistance-need-decisions/{decisionId}")
    fun getAssistanceNeedDecisionMetadata(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable decisionId: AssistanceNeedDecisionId,
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedDecision.READ_METADATA,
                        decisionId,
                    )
                    val process =
                        tx.getArchiveProcessByAssistanceNeedDecisionId(decisionId)
                            ?: return@read ProcessMetadataResponse(null)
                    val decisionDocument = tx.getAssistanceNeedDecisionDocumentMetadata(decisionId)

                    ProcessMetadataResponse(
                        ProcessMetadata(
                            process = process,
                            processName = "Varhaiskasvatuksen tuen päätös",
                            primaryDocument =
                                decisionDocument.copy(
                                    downloadPath =
                                        decisionDocument.downloadPath?.takeIf {
                                            accessControl.hasPermissionFor(
                                                tx,
                                                user,
                                                clock,
                                                Action.AssistanceNeedDecision.READ,
                                                decisionId,
                                            )
                                        }
                                ),
                            secondaryDocuments = emptyList(),
                        )
                    )
                }
            }
            .also { response ->
                Audit.AssistanceNeedDecisionReadMetadata.log(
                    targetId = AuditId(decisionId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke),
                )
            }
    }

    @GetMapping("/assistance-need-preschool-decisions/{decisionId}")
    fun getAssistanceNeedPreschoolDecisionMetadata(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable decisionId: AssistanceNeedPreschoolDecisionId,
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedPreschoolDecision.READ_METADATA,
                        decisionId,
                    )
                    val process =
                        tx.getArchiveProcessByAssistanceNeedPreschoolDecisionId(decisionId)
                            ?: return@read ProcessMetadataResponse(null)
                    val decisionDocument =
                        tx.getAssistanceNeedPreschoolDecisionDocumentMetadata(decisionId)

                    ProcessMetadataResponse(
                        ProcessMetadata(
                            process = process,
                            processName = "Esiopetuksen tuen päätös",
                            primaryDocument =
                                decisionDocument.copy(
                                    downloadPath =
                                        decisionDocument.downloadPath?.takeIf {
                                            accessControl.hasPermissionFor(
                                                tx,
                                                user,
                                                clock,
                                                Action.AssistanceNeedPreschoolDecision.READ,
                                                decisionId,
                                            )
                                        }
                                ),
                            secondaryDocuments = emptyList(),
                        )
                    )
                }
            }
            .also { response ->
                Audit.AssistanceNeedPreschoolDecisionReadMetadata.log(
                    targetId = AuditId(decisionId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke),
                )
            }
    }

    @GetMapping("/applications/{applicationId}")
    fun getApplicationMetadata(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Application.READ_METADATA,
                        applicationId,
                    )
                    val process =
                        tx.getArchiveProcessByApplicationId(applicationId)
                            ?: return@read ProcessMetadataResponse(null)
                    val processMetadata =
                        processMetadataService.getApplicationProcessMetadata(
                            tx,
                            user,
                            clock,
                            applicationId,
                            process,
                        )
                    ProcessMetadataResponse(processMetadata)
                }
            }
            .also { response ->
                Audit.ApplicationReadMetadata.log(
                    targetId = AuditId(applicationId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke),
                )
            }
    }

    @GetMapping("/fee-decisions/{feeDecisionId}")
    fun getFeeDecisionMetadata(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable feeDecisionId: FeeDecisionId,
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.FeeDecision.READ_METADATA,
                        feeDecisionId,
                    )
                    val process =
                        tx.getArchiveProcessByFeeDecisionId(feeDecisionId)
                            ?: return@read ProcessMetadataResponse(null)
                    val decisionDocument = tx.getFeeDecisionDocumentMetadata(feeDecisionId)

                    ProcessMetadataResponse(
                        ProcessMetadata(
                            process = process,
                            processName = "Varhaiskasvatuksen maksupäätös",
                            primaryDocument = decisionDocument,
                            secondaryDocuments = emptyList(),
                        )
                    )
                }
            }
            .also { response ->
                Audit.FeeDecisionReadMetadata.log(
                    targetId = AuditId(feeDecisionId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke),
                )
            }
    }

    @GetMapping("/voucher-value-decisions/{voucherValueDecisionId}")
    fun getVoucherValueDecisionMetadata(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable voucherValueDecisionId: VoucherValueDecisionId,
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.VoucherValueDecision.READ_METADATA,
                        voucherValueDecisionId,
                    )
                    val process =
                        tx.getArchiveProcessByVoucherValueDecisionId(voucherValueDecisionId)
                            ?: return@read ProcessMetadataResponse(null)
                    val decisionDocument =
                        tx.getVoucherValueDecisionDocumentMetadata(voucherValueDecisionId)

                    ProcessMetadataResponse(
                        ProcessMetadata(
                            process = process,
                            processName = "Varhaiskasvatuksen palvelusetelin arvopäätös",
                            primaryDocument = decisionDocument,
                            secondaryDocuments = emptyList(),
                        )
                    )
                }
            }
            .also { response ->
                Audit.VoucherValueDecisionReadMetadata.log(
                    targetId = AuditId(voucherValueDecisionId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke),
                )
            }
    }

    private fun Database.Read.getAssistanceNeedDecisionDocumentMetadata(
        decisionId: AssistanceNeedDecisionId
    ): DocumentMetadata =
        createQuery {
                sql(
                    """
        SELECT 
            d.id,
            d.created_at,
            e.id AS created_by_id,
            e.name AS created_by_name,
            e.type AS created_by_type,
            TRUE AS confidential,
            d.document_key
        FROM assistance_need_decision d
        LEFT JOIN evaka_user e ON e.employee_id = d.created_by
        WHERE d.id = ${bind(decisionId)}
    """
                )
            }
            .map {
                DocumentMetadata(
                    documentId = column("id"),
                    name = "Päätös tuesta varhaiskasvatuksessa",
                    createdAt = column("created_at"),
                    createdBy =
                        column<EvakaUserId?>("created_by_id")?.let {
                            EvakaUser(
                                id = it,
                                name = column("created_by_name"),
                                type = column("created_by_type"),
                            )
                        },
                    confidential = true,
                    confidentiality =
                        DocumentConfidentiality(
                            durationYears = 100,
                            basis = "Varhaiskasvatuslaki 40 § 2 mom.",
                        ),
                    downloadPath =
                        column<String?>("document_key")?.let {
                            "/employee/assistance-need-decision/$decisionId/pdf"
                        },
                    receivedBy = null,
                    sfiDeliveries = emptyList(),
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
            d.id,
            d.created_at,
            e.id AS created_by_id,
            e.name AS created_by_name,
            e.type AS created_by_type,
            d.document_key
        FROM assistance_need_preschool_decision d
        LEFT JOIN evaka_user e ON e.employee_id = d.created_by
        WHERE d.id = ${bind(decisionId)}
    """
                )
            }
            .map {
                DocumentMetadata(
                    documentId = column("id"),
                    name = "Päätös tuesta esiopetuksessa",
                    createdAt = column("created_at"),
                    createdBy =
                        column<EvakaUserId?>("created_by_id")?.let {
                            EvakaUser(
                                id = it,
                                name = column("created_by_name"),
                                type = column("created_by_type"),
                            )
                        },
                    confidential = true,
                    confidentiality =
                        DocumentConfidentiality(durationYears = 100, basis = "JulkL 24.1 §"),
                    downloadPath =
                        column<String?>("document_key")?.let {
                            "/employee/assistance-need-preschool-decisions/$decisionId/pdf"
                        },
                    receivedBy = null,
                    sfiDeliveries = emptyList(),
                )
            }
            .exactlyOne()

    private fun Database.Read.getFeeDecisionDocumentMetadata(
        decisionId: FeeDecisionId
    ): DocumentMetadata =
        createQuery {
                sql(
                    """
        SELECT 
            d.id,
            d.created,
            e.id AS created_by_id,
            e.name AS created_by_name,
            e.type AS created_by_type,
            d.document_key,
            (
                $sfiDeliverySelect
                WHERE sm.fee_decision_id = d.id
            ) AS sfi_deliveries
        FROM fee_decision d
        LEFT JOIN evaka_user e ON e.employee_id = d.approved_by_id
        WHERE d.id = ${bind(decisionId)}
    """
                )
            }
            .map {
                DocumentMetadata(
                    documentId = column("id"),
                    name = "Maksupäätös",
                    createdAt = column("created"),
                    createdBy =
                        column<EvakaUserId?>("created_by_id")?.let {
                            EvakaUser(
                                id = it,
                                name = column("created_by_name"),
                                type = column("created_by_type"),
                            )
                        },
                    confidential = true,
                    confidentiality =
                        DocumentConfidentiality(durationYears = 25, basis = "JulkL 24.1 §"),
                    downloadPath =
                        column<String?>("document_key")?.let { "/employee/fee-decisions/pdf/$it" },
                    receivedBy = null,
                    sfiDeliveries = jsonColumn("sfi_deliveries"),
                )
            }
            .exactlyOne()

    private fun Database.Read.getVoucherValueDecisionDocumentMetadata(
        voucherValueDecisionId: VoucherValueDecisionId
    ): DocumentMetadata =
        createQuery {
                sql(
                    """
        SELECT
            d.id,
            d.created,
            e.id AS created_by_id,
            e.name AS created_by_name,
            e.type AS created_by_type,
            d.document_key,
            (
                $sfiDeliverySelect
                WHERE sm.voucher_value_decision_id = d.id
            ) AS sfi_deliveries
        FROM voucher_value_decision d
        LEFT JOIN evaka_user e ON e.employee_id = d.approved_by
        WHERE d.id = ${bind(voucherValueDecisionId)}
    """
                )
            }
            .map {
                DocumentMetadata(
                    documentId = column("id"),
                    name = "Arvopäätös",
                    createdAt = column("created"),
                    createdBy =
                        column<EvakaUserId?>("created_by_id")?.let {
                            EvakaUser(
                                id = it,
                                name = column("created_by_name"),
                                type = column("created_by_type"),
                            )
                        },
                    confidential = true,
                    confidentiality =
                        DocumentConfidentiality(durationYears = 25, basis = "JulkL 24.1 §"),
                    downloadPath =
                        column<String?>("document_key")?.let {
                            "/employee/value-decisions/pdf/$it"
                        },
                    receivedBy = null,
                    sfiDeliveries = jsonColumn("sfi_deliveries"),
                )
            }
            .exactlyOne()
}
