// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.setApplicationProcessId
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.invoicing.data.setFeeDecisionProcessId
import fi.espoo.evaka.invoicing.data.setVoucherValueDecisionProcessId
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ArchiveProcessType
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
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
    val metadata = MetadataService(featureConfig)

    runBatches("application", tracer, batchSize) {
        migrateApplicationMetadata(dbc, batchSize, clock, metadata)
    }

    runBatches("fee decision", tracer, batchSize) {
        migrateFeeDecisionMetadata(dbc, batchSize, metadata)
    }

    runBatches("voucher value decision", tracer, batchSize) {
        migrateVoucherValueDecisionMetadata(dbc, batchSize, metadata)
    }

    runBatches("assistance need daycare decision", tracer, batchSize) {
        migrateAssistanceNeedDecisionDaycare(dbc, batchSize, metadata)
    }

    runBatches("assistance need preschool decision", tracer, batchSize) {
        migrateAssistanceNeedDecisionPreschool(dbc, batchSize, metadata)
    }

    runBatches("child document", tracer, batchSize) {
        migrateDocuments(dbc, batchSize, featureConfig.archiveMetadataOrganization)
    }
}

private fun runBatches(dataName: String, tracer: Tracer, batchSize: Int, migrate: () -> Int) {
    var totalMigrated = 0
    logger.info { "Starting metadata migration of $dataName" }
    do {
        val migrated = tracer.withSpan("migrate $dataName metadata") { migrate() }
        totalMigrated += migrated
        if (migrated > 0) {
            logger.info { "Migrating $dataName: Migrated $migrated records, total $totalMigrated" }
        }
    } while (migrated == batchSize)
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
)

private fun migrateApplicationMetadata(
    dbc: Database.Connection,
    batchSize: Int,
    clock: EvakaClock,
    metadata: MetadataService,
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
                            d.resolved AS decision_resolved
                        FROM application a
                        LEFT JOIN decision d ON d.application_id = a.id
                        WHERE process_id IS NULL AND sentdate IS NOT NULL
                        ORDER BY sentdate
                        LIMIT ${bind(batchSize)}
                        """
                    )
                }
                .toList<ApplicationMigrationData>()

        applications.forEach { application ->
            val process =
                metadata.getProcess(
                    ArchiveProcessType.fromApplicationType(application.type),
                    application.sentDate.year,
                )
            if (process == null) {
                logger.warn { "Missing metadata config for application type ${application.type}" }
                return@forEach
            }

            val processId = tx.insertProcess(process, migrated = true).id
            tx.setApplicationProcessId(application.id, processId, clock.now(), systemInternalUser)
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.INITIAL,
                now = HelsinkiDateTime.of(application.sentDate, LocalTime.MIDNIGHT),
                userId = systemInternalUser,
            )

            if (
                application.status == ApplicationStatus.ACTIVE ||
                    application.status == ApplicationStatus.REJECTED
            ) {
                tx.insertProcessHistoryRow(
                    processId = processId,
                    state = ArchivedProcessState.COMPLETED,
                    now =
                        application.decisionResolved
                            ?: application.statusModifiedAt
                            ?: application.modifiedAt,
                    userId = systemInternalUser,
                )
            } else if (application.status == ApplicationStatus.CANCELLED) {
                tx.insertProcessHistoryRow(
                    processId = processId,
                    state = ArchivedProcessState.COMPLETED,
                    now = application.statusModifiedAt ?: application.modifiedAt,
                    userId = systemInternalUser,
                )
            }
        }
        applications.size
    }
}

private data class FeeDecisionMigrationData(
    val id: FeeDecisionId,
    val created: HelsinkiDateTime,
    val approvedAt: HelsinkiDateTime,
    val approvedBy: EmployeeId?,
    val sentAt: HelsinkiDateTime?,
)

private fun migrateFeeDecisionMetadata(
    dbc: Database.Connection,
    batchSize: Int,
    metadata: MetadataService,
): Int {
    val systemInternalUser = AuthenticatedUser.SystemInternalUser.evakaUserId
    return dbc.transaction { tx ->
        val feeDecisions =
            tx.createQuery {
                    sql(
                        """
                        SELECT id, created, approved_at, approved_by_id AS approved_by, sent_at
                        FROM fee_decision
                        WHERE process_id IS NULL AND approved_at IS NOT NULL
                        ORDER BY created
                        LIMIT ${bind(batchSize)}
                        """
                    )
                }
                .toList<FeeDecisionMigrationData>()

        feeDecisions.forEach { decision ->
            val process =
                metadata.getProcess(ArchiveProcessType.FEE_DECISION, decision.created.year)
            if (process == null) {
                logger.warn { "Missing metadata config for FEE_DECISION" }
                return@forEach
            }
            val processId = tx.insertProcess(process, migrated = true).id
            tx.setFeeDecisionProcessId(decision.id, processId)
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.INITIAL,
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
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.DECIDING,
                now = decision.approvedAt,
                userId = approvedBy,
            )
            if (decision.sentAt != null) {
                tx.insertProcessHistoryRow(
                    processId = processId,
                    state = ArchivedProcessState.COMPLETED,
                    now = decision.sentAt,
                    userId = systemInternalUser,
                )
            }
        }
        feeDecisions.size
    }
}

private data class VoucherValueDecisionMigrationData(
    val id: VoucherValueDecisionId,
    val created: HelsinkiDateTime,
    val approvedAt: HelsinkiDateTime,
    val approvedBy: EmployeeId?,
    val sentAt: HelsinkiDateTime?,
)

private fun migrateVoucherValueDecisionMetadata(
    dbc: Database.Connection,
    batchSize: Int,
    metadata: MetadataService,
): Int {
    val systemInternalUser = AuthenticatedUser.SystemInternalUser.evakaUserId
    return dbc.transaction { tx ->
        val voucherValueDecisions =
            tx.createQuery {
                    sql(
                        """
                        SELECT id, created, approved_at, approved_by, sent_at
                        FROM voucher_value_decision
                        WHERE process_id IS NULL AND approved_at IS NOT NULL
                        ORDER BY created
                        LIMIT ${bind(batchSize)}
                        """
                    )
                }
                .toList<VoucherValueDecisionMigrationData>()

        voucherValueDecisions.forEach { decision ->
            val process =
                metadata.getProcess(
                    ArchiveProcessType.VOUCHER_VALUE_DECISION,
                    decision.created.year,
                )
            if (process == null) {
                logger.warn { "Missing metadata config for VOUCHER_VALUE_DECISION" }
                return@forEach
            }
            val processId = tx.insertProcess(process, migrated = true).id
            tx.setVoucherValueDecisionProcessId(decision.id, processId)
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.INITIAL,
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
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.DECIDING,
                now = decision.approvedAt,
                userId = approvedBy,
            )
            if (decision.sentAt != null) {
                tx.insertProcessHistoryRow(
                    processId = processId,
                    state = ArchivedProcessState.COMPLETED,
                    now = decision.sentAt,
                    userId = systemInternalUser,
                )
            }
        }
        voucherValueDecisions.size
    }
}

private data class AssistaceNeedDaycareDecisionData(
    val id: AssistanceNeedDecisionId,
    val createdAt: HelsinkiDateTime,
    val createdBy: EmployeeId?,
    val sentForDecision: LocalDate?,
    val decisionMakerEmployeeId: EmployeeId?,
    val decisionMade: LocalDate?,
    val status: AssistanceNeedDecisionStatus,
    val documentKey: String?,
)

private fun migrateAssistanceNeedDecisionDaycare(
    dbc: Database.Connection,
    batchSize: Int,
    metadata: MetadataService,
): Int {
    val systemInternalUser = AuthenticatedUser.SystemInternalUser.evakaUserId
    return dbc.transaction { tx ->
        val assistanceNeedDecisions =
            tx.createQuery {
                    sql(
                        """
                        SELECT
                            id,
                            created_at,
                            created_by,
                            sent_for_decision,
                            decision_made,
                            decision_maker_employee_id,
                            status,
                            document_key
                        FROM assistance_need_decision
                        WHERE process_id IS NULL
                        ORDER BY created_at
                        LIMIT ${bind(batchSize)}
                        """
                    )
                }
                .toList<AssistaceNeedDaycareDecisionData>()

        assistanceNeedDecisions.forEach { decision ->
            val process =
                metadata.getProcess(
                    ArchiveProcessType.ASSISTANCE_NEED_DECISION_DAYCARE,
                    decision.createdAt.year,
                )
            if (process == null) {
                logger.warn { "Missing metadata config for ASSISTANCE_NEED_DECISION_DAYCARE" }
                return@forEach
            }
            val processId = tx.insertProcess(process, migrated = true).id
            tx.execute {
                sql(
                    "UPDATE assistance_need_decision SET process_id = ${bind(processId)} WHERE id = ${bind(decision.id)}"
                )
            }
            val createdBy =
                if (decision.createdBy != null) {
                    tx.upsertEmployeeUser(decision.createdBy)
                    EvakaUserId(decision.createdBy.raw)
                } else {
                    systemInternalUser
                }
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.INITIAL,
                now = decision.createdAt,
                userId = createdBy,
            )

            if (decision.sentForDecision == null) return@forEach
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.PREPARATION,
                now = HelsinkiDateTime.of(decision.sentForDecision, LocalTime.MIDNIGHT),
                userId = systemInternalUser,
            )

            if (decision.decisionMade == null) return@forEach
            val decisionMaker =
                if (decision.decisionMakerEmployeeId != null) {
                    tx.upsertEmployeeUser(decision.decisionMakerEmployeeId)
                    EvakaUserId(decision.decisionMakerEmployeeId.raw)
                } else {
                    systemInternalUser
                }
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.DECIDING,
                now = HelsinkiDateTime.of(decision.decisionMade, LocalTime.MIDNIGHT),
                userId = decisionMaker,
            )

            if (!decision.status.isDecided() || decision.documentKey.isNullOrEmpty()) {
                return@forEach
            }
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.COMPLETED,
                now = HelsinkiDateTime.of(decision.decisionMade, LocalTime.MIDNIGHT),
                userId = systemInternalUser,
            )
        }
        assistanceNeedDecisions.size
    }
}

private data class AssistanceNeedPreschoolDecisionData(
    val id: AssistanceNeedPreschoolDecisionId,
    val createdAt: HelsinkiDateTime,
    val createdBy: EmployeeId?,
    val sentForDecision: LocalDate?,
    val decisionMakerEmployeeId: EmployeeId?,
    val decisionMade: LocalDate?,
    val status: AssistanceNeedDecisionStatus,
    val documentKey: String?,
)

private fun migrateAssistanceNeedDecisionPreschool(
    dbc: Database.Connection,
    batchSize: Int,
    metadata: MetadataService,
): Int {
    val systemInternalUser = AuthenticatedUser.SystemInternalUser.evakaUserId
    return dbc.transaction { tx ->
        val assistanceNeedPreschoolDecisions =
            tx.createQuery {
                    sql(
                        """
                        SELECT
                            id,
                            created_at,
                            created_by,
                            sent_for_decision,
                            decision_made,
                            decision_maker_employee_id,
                            status,
                            document_key
                        FROM assistance_need_preschool_decision
                        WHERE process_id IS NULL
                        ORDER BY created_at
                        LIMIT ${bind(batchSize)}
                        """
                    )
                }
                .toList<AssistanceNeedPreschoolDecisionData>()

        assistanceNeedPreschoolDecisions.forEach { decision ->
            val process =
                metadata.getProcess(
                    ArchiveProcessType.ASSISTANCE_NEED_DECISION_PRESCHOOL,
                    decision.createdAt.year,
                )
            if (process == null) {
                logger.warn { "Missing metadata config for ASSISTANCE_NEED_DECISION_PRESCHOOL" }
                return@forEach
            }
            val processId = tx.insertProcess(process, migrated = true).id
            tx.execute {
                sql(
                    "UPDATE assistance_need_preschool_decision SET process_id = ${bind(processId)} WHERE id = ${bind(decision.id)}"
                )
            }
            val createdBy =
                if (decision.createdBy != null) {
                    tx.upsertEmployeeUser(decision.createdBy)
                    EvakaUserId(decision.createdBy.raw)
                } else {
                    systemInternalUser
                }
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.INITIAL,
                now = decision.createdAt,
                userId = createdBy,
            )

            if (decision.sentForDecision == null) return@forEach
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.PREPARATION,
                now = HelsinkiDateTime.of(decision.sentForDecision, LocalTime.MIDNIGHT),
                userId = systemInternalUser,
            )

            if (decision.decisionMade == null) return@forEach
            val decisionMaker =
                if (decision.decisionMakerEmployeeId != null) {
                    tx.upsertEmployeeUser(decision.decisionMakerEmployeeId)
                    EvakaUserId(decision.decisionMakerEmployeeId.raw)
                } else {
                    systemInternalUser
                }
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.DECIDING,
                now = HelsinkiDateTime.of(decision.decisionMade, LocalTime.MIDNIGHT),
                userId = decisionMaker,
            )

            if (!decision.status.isDecided() || decision.documentKey.isNullOrEmpty()) {
                return@forEach
            }
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.COMPLETED,
                now = HelsinkiDateTime.of(decision.decisionMade, LocalTime.MIDNIGHT),
                userId = systemInternalUser,
            )
        }
        assistanceNeedPreschoolDecisions.size
    }
}

private data class DocumentData(
    val id: ChildDocumentId,
    val created: HelsinkiDateTime,
    val createdBy: EvakaUserId?,
    val modifiedAt: HelsinkiDateTime,
    val status: DocumentStatus,
    val documentKey: String?,
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
                        d.document_key,
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

        documents.forEach { document ->
            val processId =
                tx.insertProcess(
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
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.INITIAL,
                now = document.created,
                userId = document.createdBy ?: systemInternalUser,
            )
            if (
                document.status == DocumentStatus.COMPLETED && !document.documentKey.isNullOrEmpty()
            ) {
                tx.insertProcessHistoryRow(
                    processId = processId,
                    state = ArchivedProcessState.COMPLETED,
                    now = document.modifiedAt,
                    userId = systemInternalUser,
                )
            }
        }
        documents.size
    }
}
