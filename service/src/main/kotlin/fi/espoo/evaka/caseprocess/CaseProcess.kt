// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.caseprocess

import fi.espoo.evaka.document.childdocument.ChildDocumentDetails
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ArchiveProcessType
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.CaseProcessId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.user.EvakaUser
import org.jdbi.v3.json.Json

data class CaseProcessParams(
    val organization: String,
    val processDefinitionNumber: String,
    val archiveDurationMonths: Int,
    val year: Int,
)

class CaseProcessMetadataService(private val featureConfig: FeatureConfig) {
    fun getProcessParams(processType: ArchiveProcessType, year: Int): CaseProcessParams? {
        val config = featureConfig.archiveMetadataConfigs(processType, year) ?: return null
        return CaseProcessParams(
            organization = featureConfig.archiveMetadataOrganization,
            processDefinitionNumber = config.processDefinitionNumber,
            archiveDurationMonths = config.archiveDurationMonths,
            year = year,
        )
    }
}

data class CaseProcess(
    val id: CaseProcessId,
    val caseIdentifier: String,
    val processDefinitionNumber: String,
    val year: Int,
    val number: Int,
    val organization: String,
    val archiveDurationMonths: Int?,
    val migrated: Boolean,
    @Json val history: List<CaseProcessHistoryRow>,
)

data class CaseProcessHistoryRow(
    val rowIndex: Int,
    val state: CaseProcessState,
    val enteredAt: HelsinkiDateTime,
    val enteredBy: EvakaUser,
)

enum class CaseProcessState : DatabaseEnum {
    INITIAL,
    PREPARATION,
    DECIDING,
    COMPLETED;

    // enum name is different from kotlin type because of legacy reasons
    override val sqlType: String = "archived_process_state"
}

fun Database.Transaction.insertCaseProcess(
    params: CaseProcessParams,
    migrated: Boolean = false,
): CaseProcess =
    insertCaseProcess(
        processDefinitionNumber = params.processDefinitionNumber,
        year = params.year,
        organization = params.organization,
        archiveDurationMonths = params.archiveDurationMonths,
        migrated = migrated,
    )

fun Database.Transaction.insertCaseProcess(
    processDefinitionNumber: String,
    year: Int,
    organization: String,
    archiveDurationMonths: Int,
    migrated: Boolean = false,
): CaseProcess =
    createQuery {
            sql(
                """
    INSERT INTO case_process (process_definition_number, year, number, organization, archive_duration_months, migrated)
    VALUES (
        ${bind(processDefinitionNumber)}, 
        ${bind(year)},
        coalesce((
            SELECT max(number)
            FROM case_process
            WHERE process_definition_number = ${bind(processDefinitionNumber)} AND year = ${bind(year)}
        ), 0) + 1,
        ${bind(organization)},
        ${bind(archiveDurationMonths)},
        ${bind(migrated)}
    )
    RETURNING id, case_identifier, process_definition_number, year, number, organization, archive_duration_months, migrated, '[]'::jsonb AS history
"""
            )
        }
        .exactlyOne()

fun Database.Read.getCaseProcess(id: CaseProcessId): CaseProcess? =
    createQuery {
            sql(
                """
    SELECT
        ap.id,
        ap.case_identifier,
        ap.process_definition_number,
        ap.year,
        ap.number,
        ap.organization,
        ap.archive_duration_months,
        ap.migrated,
    (
        SELECT coalesce(
            jsonb_agg(jsonb_build_object(
                'rowIndex', row_index,
                'state', state,
                'enteredAt', entered_at,
                'enteredBy', jsonb_build_object(
                    'id', eu.id, 'name', eu.name, 'type', eu.type
                )
            ) ORDER BY row_index),
            '[]'::jsonb
        )
        FROM case_process_history aph
        JOIN public.evaka_user eu on eu.id = aph.entered_by
        WHERE aph.process_id = ap.id
    ) AS history
    FROM case_process ap
    WHERE ap.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Read.getCaseProcessByChildDocumentId(documentId: ChildDocumentId): CaseProcess? {
    return createQuery {
            sql("SELECT process_id FROM child_document WHERE id = ${bind(documentId)}")
        }
        .exactlyOneOrNull<CaseProcessId?>()
        ?.let { processId -> getCaseProcess(processId) }
}

fun Database.Read.getCaseProcessByAssistanceNeedDecisionId(
    decisionId: AssistanceNeedDecisionId
): CaseProcess? {
    return createQuery {
            sql("SELECT process_id FROM assistance_need_decision WHERE id = ${bind(decisionId)}")
        }
        .exactlyOneOrNull<CaseProcessId?>()
        ?.let { processId -> getCaseProcess(processId) }
}

fun Database.Read.getCaseProcessByAssistanceNeedPreschoolDecisionId(
    decisionId: AssistanceNeedPreschoolDecisionId
): CaseProcess? {
    return createQuery {
            sql(
                "SELECT process_id FROM assistance_need_preschool_decision WHERE id = ${bind(decisionId)}"
            )
        }
        .exactlyOneOrNull<CaseProcessId?>()
        ?.let { processId -> getCaseProcess(processId) }
}

fun Database.Read.getCaseProcessByApplicationId(applicationId: ApplicationId): CaseProcess? {
    return createQuery {
            sql("SELECT process_id FROM application WHERE id = ${bind(applicationId)}")
        }
        .exactlyOneOrNull<CaseProcessId?>()
        ?.let { processId -> getCaseProcess(processId) }
}

fun Database.Read.getCaseProcessByFeeDecisionId(feeDecisionId: FeeDecisionId): CaseProcess? {
    return createQuery {
            sql("SELECT process_id FROM fee_decision WHERE id = ${bind(feeDecisionId)}")
        }
        .exactlyOneOrNull<CaseProcessId?>()
        ?.let { processId -> getCaseProcess(processId) }
}

fun Database.Read.getCaseProcessByVoucherValueDecisionId(
    voucherValueDecisionId: VoucherValueDecisionId
): CaseProcess? {
    return createQuery {
            sql(
                "SELECT process_id FROM voucher_value_decision WHERE id = ${bind(voucherValueDecisionId)}"
            )
        }
        .exactlyOneOrNull<CaseProcessId?>()
        ?.let { processId -> getCaseProcess(processId) }
}

fun Database.Transaction.deleteProcessById(processId: CaseProcessId) {
    execute { sql("DELETE FROM case_process WHERE id = ${bind(processId)}") }
}

fun deleteProcessByDocumentId(tx: Database.Transaction, documentId: ChildDocumentId) {
    tx.createQuery { sql("SELECT process_id FROM child_document WHERE id = ${bind(documentId)}") }
        .exactlyOneOrNull<CaseProcessId?>()
        ?.also { processId -> tx.deleteProcessById(processId) }
}

fun deleteProcessByAssistanceNeedDecisionId(
    tx: Database.Transaction,
    decisionId: AssistanceNeedDecisionId,
) {
    tx.createQuery {
            sql("SELECT process_id FROM assistance_need_decision WHERE id = ${bind(decisionId)}")
        }
        .exactlyOneOrNull<CaseProcessId?>()
        ?.also { processId -> tx.deleteProcessById(processId) }
}

fun deleteCaseProcessByAssistanceNeedPreschoolDecisionId(
    tx: Database.Transaction,
    decisionId: AssistanceNeedPreschoolDecisionId,
) {
    tx.createQuery {
            sql(
                "SELECT process_id FROM assistance_need_preschool_decision WHERE id = ${bind(decisionId)}"
            )
        }
        .exactlyOneOrNull<CaseProcessId?>()
        ?.also { processId -> tx.deleteProcessById(processId) }
}

fun Database.Transaction.insertCaseProcessHistoryRow(
    processId: CaseProcessId,
    state: CaseProcessState,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
) {
    execute {
        sql(
            """
    INSERT INTO case_process_history (process_id, row_index, state, entered_at, entered_by) VALUES (
        ${bind(processId)},
        coalesce((
            SELECT max(row_index)
            FROM case_process_history
            WHERE process_id = ${bind(processId)}
        ), 0) + 1,
        ${bind(state)},
        ${bind(now)},
        ${bind(userId)}
    )
"""
        )
    }
}

fun updateDocumentCaseProcessHistory(
    tx: Database.Transaction,
    document: ChildDocumentDetails,
    newStatus: DocumentStatus,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
) {
    val process = tx.getCaseProcessByChildDocumentId(document.id) ?: return

    val currentProcessState = process.history.lastOrNull()?.state ?: return
    val currentStateIndex = CaseProcessState.entries.indexOf(currentProcessState)

    val newProcessState =
        when (newStatus) {
            DocumentStatus.DRAFT -> CaseProcessState.INITIAL
            DocumentStatus.PREPARED -> CaseProcessState.PREPARATION
            DocumentStatus.CITIZEN_DRAFT -> CaseProcessState.INITIAL
            DocumentStatus.DECISION_PROPOSAL -> CaseProcessState.PREPARATION
            DocumentStatus.COMPLETED ->
                // decision documents are completed only once the SFI message is sent
                if (document.template.type.decision) CaseProcessState.DECIDING
                else CaseProcessState.COMPLETED
        }
    val newStateIndex = CaseProcessState.entries.indexOf(newProcessState)

    when {
        newStateIndex > currentStateIndex ->
            // moving forwards
            tx.insertCaseProcessHistoryRow(
                processId = process.id,
                state = newProcessState,
                now = now,
                userId = userId,
            )
        newStateIndex < currentStateIndex ->
            // moving backwards
            tx.cancelLastCaseProcessHistoryRow(
                processId = process.id,
                stateToCancel = currentProcessState,
            )
    }
}

fun autoCompleteDocumentCaseProcessHistory(
    tx: Database.Transaction,
    documentId: ChildDocumentId,
    now: HelsinkiDateTime,
) {
    val process = tx.getCaseProcessByChildDocumentId(documentId) ?: return
    val currentProcessState = process.history.lastOrNull()?.state ?: return

    if (currentProcessState != CaseProcessState.COMPLETED) {
        tx.insertCaseProcessHistoryRow(
            processId = process.id,
            state = CaseProcessState.COMPLETED,
            now = now,
            userId = AuthenticatedUser.SystemInternalUser.evakaUserId,
        )
    }
}

fun Database.Transaction.cancelLastCaseProcessHistoryRow(
    processId: CaseProcessId,
    stateToCancel: CaseProcessState,
) {
    createUpdate {
            sql(
                """
    DELETE FROM case_process_history aph
    WHERE aph.process_id = ${bind(processId)} AND aph.state = ${bind(stateToCancel)} AND 
        aph.row_index = (
            SELECT max(row_index) 
            FROM case_process_history
            WHERE process_id = ${bind(processId)}
        )
"""
            )
        }
        .updateExactlyOne()
}
