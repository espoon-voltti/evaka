// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.caseprocess

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.setApplicationProcessId
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.invoicing.data.setFeeDecisionProcessId
import fi.espoo.evaka.invoicing.data.setVoucherValueDecisionProcessId
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ArchiveProcessType
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.noopTracer
import fi.espoo.evaka.shared.security.upsertEmployeeUser
import fi.espoo.evaka.shared.withSpan
import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.api.trace.Tracer
import java.time.LocalDate
import java.time.LocalTime

val logger = KotlinLogging.logger {}

fun migrateProcessMetadata(
    dbc: Database.Connection,
    clock: EvakaClock,
    featureConfig: FeatureConfig,
    tracer: Tracer = noopTracer(),
    batchSize: Int = 1000,
) {
    val metadata = CaseProcessMetadataService(featureConfig)

    runBatches("application", tracer) {
        migrateApplicationMetadata(dbc, batchSize, clock, metadata)
    }

    runBatches("fee decision", tracer) { migrateFeeDecisionMetadata(dbc, batchSize, metadata) }

    runBatches("voucher value decision", tracer) {
        migrateVoucherValueDecisionMetadata(dbc, batchSize, metadata)
    }

    runBatches("child document", tracer) {
        migrateDocuments(dbc, batchSize, featureConfig.archiveMetadataOrganization)
    }
}

private fun runBatches(dataName: String, tracer: Tracer, migrate: () -> Int) {
    var totalMigrated = 0
    logger.info { "Starting metadata migration of $dataName" }
    do {
        val migrated = tracer.withSpan("migrate $dataName metadata") { migrate() }
        totalMigrated += migrated
        if (migrated > 0) {
            logger.info { "Migrating $dataName: Migrated $migrated records, total $totalMigrated" }
        }
    } while (migrated > 0)
    logger.info { "Completed metadata migration of $dataName" }
}

private data class ApplicationMigrationData(
    val id: ApplicationId,
    val type: ApplicationType,
    val sentDate: LocalDate,
    val status: ApplicationStatus,
    val modifiedAt: HelsinkiDateTime,
    val statusModifiedAt: HelsinkiDateTime?,
    val decisionResolved: HelsinkiDateTime?,
    val decisionResolvedBy: EvakaUserId?,
)

private fun migrateApplicationMetadata(
    dbc: Database.Connection,
    batchSize: Int,
    clock: EvakaClock,
    metadata: CaseProcessMetadataService,
): Int {
    val systemInternalUser = AuthenticatedUser.SystemInternalUser.evakaUserId
    return dbc.transaction { tx ->
        val applications =
            tx.createQuery {
                    sql(
                        """
                        SELECT
                            a.id,
                            a.type,
                            a.sentdate,
                            a.status,
                            a.modified_at,
                            a.status_modified_at,
                            d.resolved AS decision_resolved,
                            d.resolved_by AS decision_resolved_by
                        FROM application a
                        LEFT JOIN decision d ON d.application_id = a.id
                        WHERE process_id IS NULL AND sentdate IS NOT NULL
                        ORDER BY sentdate
                        LIMIT ${bind(batchSize)}
                        """
                    )
                }
                .toList<ApplicationMigrationData>()

        var migratedCount = 0
        applications.forEach { application ->
            val process =
                metadata.getProcessParams(
                    ArchiveProcessType.fromApplicationType(application.type),
                    application.sentDate.year,
                )
            if (process == null) {
                logger.warn { "Missing metadata config for application type ${application.type}" }
                return@forEach
            }

            val processId = tx.insertCaseProcess(process, migrated = true).id
            tx.setApplicationProcessId(application.id, processId, clock.now(), systemInternalUser)
            tx.insertCaseProcessHistoryRow(
                processId = processId,
                state = CaseProcessState.INITIAL,
                now = HelsinkiDateTime.of(application.sentDate, LocalTime.MIDNIGHT),
                userId = systemInternalUser,
            )

            if (
                application.status == ApplicationStatus.ACTIVE ||
                    application.status == ApplicationStatus.REJECTED
            ) {
                tx.insertCaseProcessHistoryRow(
                    processId = processId,
                    state = CaseProcessState.COMPLETED,
                    now =
                        application.decisionResolved
                            ?: application.statusModifiedAt
                            ?: application.modifiedAt,
                    userId = application.decisionResolvedBy ?: systemInternalUser,
                )
            } else if (application.status == ApplicationStatus.CANCELLED) {
                tx.insertCaseProcessHistoryRow(
                    processId = processId,
                    state = CaseProcessState.COMPLETED,
                    now = application.statusModifiedAt ?: application.modifiedAt,
                    userId = systemInternalUser,
                )
            }
            migratedCount++
        }
        migratedCount
    }
}

private data class FeeDecisionMigrationData(
    val id: FeeDecisionId,
    val created: HelsinkiDateTime,
    val status: FeeDecisionStatus,
    val approvedAt: HelsinkiDateTime,
    val approvedBy: EmployeeId?,
    val sentAt: HelsinkiDateTime?,
)

private fun migrateFeeDecisionMetadata(
    dbc: Database.Connection,
    batchSize: Int,
    metadata: CaseProcessMetadataService,
): Int {
    val systemInternalUser = AuthenticatedUser.SystemInternalUser.evakaUserId
    return dbc.transaction { tx ->
        val feeDecisions =
            tx.createQuery {
                    sql(
                        """
                        SELECT id, created, status, approved_at, approved_by_id AS approved_by, sent_at
                        FROM fee_decision
                        WHERE process_id IS NULL AND approved_at IS NOT NULL
                        ORDER BY created
                        LIMIT ${bind(batchSize)}
                        """
                    )
                }
                .toList<FeeDecisionMigrationData>()

        var migratedCount = 0
        feeDecisions.forEach { decision ->
            val process =
                metadata.getProcessParams(ArchiveProcessType.FEE_DECISION, decision.created.year)
            if (process == null) {
                logger.warn { "Missing metadata config for FEE_DECISION" }
                return@forEach
            }
            val processId = tx.insertCaseProcess(process, migrated = true).id
            tx.setFeeDecisionProcessId(decision.id, processId)
            tx.insertCaseProcessHistoryRow(
                processId = processId,
                state = CaseProcessState.INITIAL,
                now = decision.created,
                userId = systemInternalUser,
            )
            val approvedBy =
                if (decision.approvedBy != null) {
                    tx.upsertEmployeeUser(decision.approvedBy)
                    EvakaUserId(decision.approvedBy.raw)
                } else {
                    systemInternalUser
                }
            tx.insertCaseProcessHistoryRow(
                processId = processId,
                state = CaseProcessState.DECIDING,
                now = decision.approvedAt,
                userId = approvedBy,
            )
            if (
                decision.status == FeeDecisionStatus.SENT ||
                    decision.status == FeeDecisionStatus.ANNULLED
            ) {
                tx.insertCaseProcessHistoryRow(
                    processId = processId,
                    state = CaseProcessState.COMPLETED,
                    now = decision.sentAt ?: decision.approvedAt,
                    userId = systemInternalUser,
                )
            }
            migratedCount++
        }
        migratedCount
    }
}

private data class VoucherValueDecisionMigrationData(
    val id: VoucherValueDecisionId,
    val created: HelsinkiDateTime,
    val status: VoucherValueDecisionStatus,
    val approvedAt: HelsinkiDateTime,
    val approvedBy: EmployeeId?,
    val sentAt: HelsinkiDateTime?,
)

private fun migrateVoucherValueDecisionMetadata(
    dbc: Database.Connection,
    batchSize: Int,
    metadata: CaseProcessMetadataService,
): Int {
    val systemInternalUser = AuthenticatedUser.SystemInternalUser.evakaUserId
    return dbc.transaction { tx ->
        val voucherValueDecisions =
            tx.createQuery {
                    sql(
                        """
                        SELECT id, created, status, approved_at, approved_by, sent_at
                        FROM voucher_value_decision
                        WHERE process_id IS NULL AND approved_at IS NOT NULL
                        ORDER BY created
                        LIMIT ${bind(batchSize)}
                        """
                    )
                }
                .toList<VoucherValueDecisionMigrationData>()

        var migratedCount = 0
        voucherValueDecisions.forEach { decision ->
            val process =
                metadata.getProcessParams(
                    ArchiveProcessType.VOUCHER_VALUE_DECISION,
                    decision.created.year,
                )
            if (process == null) {
                logger.warn { "Missing metadata config for VOUCHER_VALUE_DECISION" }
                return@forEach
            }
            val processId = tx.insertCaseProcess(process, migrated = true).id
            tx.setVoucherValueDecisionProcessId(decision.id, processId)
            tx.insertCaseProcessHistoryRow(
                processId = processId,
                state = CaseProcessState.INITIAL,
                now = decision.created,
                userId = systemInternalUser,
            )
            val approvedBy =
                if (decision.approvedBy != null) {
                    tx.upsertEmployeeUser(decision.approvedBy)
                    EvakaUserId(decision.approvedBy.raw)
                } else {
                    systemInternalUser
                }
            tx.insertCaseProcessHistoryRow(
                processId = processId,
                state = CaseProcessState.DECIDING,
                now = decision.approvedAt,
                userId = approvedBy,
            )
            if (
                decision.status == VoucherValueDecisionStatus.SENT ||
                    decision.status == VoucherValueDecisionStatus.ANNULLED
            ) {
                tx.insertCaseProcessHistoryRow(
                    processId = processId,
                    state = CaseProcessState.COMPLETED,
                    now = decision.sentAt ?: decision.approvedAt,
                    userId = systemInternalUser,
                )
            }
            migratedCount++
        }
        migratedCount
    }
}

private data class DocumentData(
    val id: ChildDocumentId,
    val created: HelsinkiDateTime,
    val createdBy: EvakaUserId?,
    val modifiedAt: HelsinkiDateTime,
    val status: DocumentStatus,
    val hasDocumentKey: Boolean,
    val processDefinitionNumber: String,
    val archiveDurationMonths: Int,
)

private fun migrateDocuments(
    dbc: Database.Connection,
    batchSize: Int,
    archiveMetadataOrganization: String,
): Int {
    val systemInternalUser = AuthenticatedUser.SystemInternalUser.evakaUserId
    return dbc.transaction { tx ->
        val documents =
            tx.createQuery {
                    sql(
                        """
                    SELECT
                        d.id,
                        d.created,
                        d.created_by,
                        d.modified_at,
                        d.status,
                        EXISTS(
                            SELECT 1
                            FROM child_document_published_version v
                            WHERE v.child_document_id = d.id
                            AND v.document_key IS NOT NULL
                        ) AS has_document_key,
                        t.process_definition_number,
                        t.archive_duration_months
                    FROM child_document d
                    JOIN document_template t ON t.id = d.template_id
                    WHERE
                        d.process_id IS NULL AND
                        t.process_definition_number IS NOT NULL AND
                        t.archive_duration_months IS NOT NULL
                    ORDER BY d.created
                    LIMIT ${bind(batchSize)}
                    """
                    )
                }
                .toList<DocumentData>()

        var migratedCount = 0
        documents.forEach { document ->
            val processId =
                tx.insertCaseProcess(
                        processDefinitionNumber = document.processDefinitionNumber,
                        year = document.created.year,
                        organization = archiveMetadataOrganization,
                        archiveDurationMonths = document.archiveDurationMonths,
                        migrated = true,
                    )
                    .id
            tx.execute {
                sql(
                    "UPDATE child_document SET process_id = ${bind(processId)} WHERE id = ${bind(document.id)}"
                )
            }
            tx.insertCaseProcessHistoryRow(
                processId = processId,
                state = CaseProcessState.INITIAL,
                now = document.created,
                userId = document.createdBy ?: systemInternalUser,
            )
            if (document.status == DocumentStatus.COMPLETED && document.hasDocumentKey) {
                tx.insertCaseProcessHistoryRow(
                    processId = processId,
                    state = CaseProcessState.COMPLETED,
                    now = document.modifiedAt,
                    userId = systemInternalUser,
                )
            }
            migratedCount++
        }
        migratedCount
    }
}
