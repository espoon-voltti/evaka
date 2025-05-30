// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.setApplicationProcessId
import fi.espoo.evaka.invoicing.data.setFeeDecisionProcessId
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ArchiveProcessConfig
import fi.espoo.evaka.shared.ArchiveProcessType
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.time.LocalTime

val logger = KotlinLogging.logger {}

fun migrateProcessMetadata(
    dbc: Database.Connection,
    clock: EvakaClock,
    featureConfig: FeatureConfig,
) {
    migrateApplicationMetadata(
        dbc,
        clock,
        featureConfig.archiveMetadataOrganization,
        featureConfig.archiveMetadataConfigs,
    )

    val feeDecisionConfig = featureConfig.archiveMetadataConfigs[ArchiveProcessType.FEE_DECISION]
    if (feeDecisionConfig != null) {
        migrateFeeDecisionMetadata(
            dbc,
            featureConfig.archiveMetadataOrganization,
            feeDecisionConfig,
        )
    } else {
        logger.warn { "Missing metadata config for FEE_DECISION" }
    }
}

private data class ApplicationMigrationData(
    val id: ApplicationId,
    val type: ApplicationType,
    val sentDate: LocalDate,
    val status: ApplicationStatus,
    val statusModifiedAt: HelsinkiDateTime?,
)

private fun migrateApplicationMetadata(
    dbc: Database.Connection,
    clock: EvakaClock,
    archiveMetadataOrganization: String,
    metadataConfigs: Map<ArchiveProcessType, ArchiveProcessConfig>,
) {
    val systemInternalUser = AuthenticatedUser.SystemInternalUser.evakaUserId
    dbc.transaction { tx ->
        val applications =
            tx.createQuery {
                    sql(
                        """
                        SELECT id, type, sentdate, status, status_modified_at
                        FROM application
                        WHERE process_id IS NULL AND sentdate IS NOT NULL
                        ORDER BY sentdate
                        """
                    )
                }
                .toList<ApplicationMigrationData>()

        applications.forEach { application ->
            val config = metadataConfigs[ArchiveProcessType.fromApplicationType(application.type)]

            if (config == null) {
                logger.warn { "Missing metadata config for application type ${application.type}" }
                return@forEach
            }

            val processId =
                tx.insertProcess(
                        processDefinitionNumber = config.processDefinitionNumber,
                        year = application.sentDate.year,
                        organization = archiveMetadataOrganization,
                        archiveDurationMonths = config.archiveDurationMonths,
                        migrated = true,
                    )
                    .id
            tx.setApplicationProcessId(application.id, processId, clock.now(), systemInternalUser)
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.INITIAL,
                now = HelsinkiDateTime.of(application.sentDate, LocalTime.MIDNIGHT),
                userId = systemInternalUser,
            )

            if (
                (application.status == ApplicationStatus.ACTIVE ||
                    application.status == ApplicationStatus.REJECTED ||
                    application.status == ApplicationStatus.CANCELLED) &&
                    application.statusModifiedAt != null
            ) {
                tx.insertProcessHistoryRow(
                    processId = processId,
                    state = ArchivedProcessState.COMPLETED,
                    now = application.statusModifiedAt,
                    userId = systemInternalUser,
                )
            }
        }
    }
}

private data class FeeDecisionMigrationData(
    val id: FeeDecisionId,
    val created: HelsinkiDateTime,
    val approvedAt: HelsinkiDateTime,
    val approvedBy: EvakaUserId?,
    val sentAt: HelsinkiDateTime?,
)

private fun migrateFeeDecisionMetadata(
    dbc: Database.Connection,
    archiveMetadataOrganization: String,
    config: ArchiveProcessConfig,
) {
    val systemInternalUser = AuthenticatedUser.SystemInternalUser.evakaUserId
    dbc.transaction { tx ->
        val feeDecisions =
            tx.createQuery {
                    sql(
                        """
                        SELECT id, created, approved_at, approved_by_id AS approved_by, sent_at
                        FROM fee_decision
                        WHERE process_id IS NULL AND approved_at IS NOT NULL
                        ORDER BY created
                        """
                    )
                }
                .toList<FeeDecisionMigrationData>()

        feeDecisions.forEach { decision ->
            val processId =
                tx.insertProcess(
                        processDefinitionNumber = config.processDefinitionNumber,
                        year = decision.created.year,
                        organization = archiveMetadataOrganization,
                        archiveDurationMonths = config.archiveDurationMonths,
                        migrated = true,
                    )
                    .id
            tx.setFeeDecisionProcessId(decision.id, processId)
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.INITIAL,
                now = decision.created,
                userId = systemInternalUser,
            )
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.DECIDING,
                now = decision.approvedAt,
                userId = decision.approvedBy ?: systemInternalUser,
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
    }
}
