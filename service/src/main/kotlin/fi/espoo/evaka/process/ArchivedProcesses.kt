// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.document.childdocument.ChildDocumentDetails
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ArchivedProcessId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.user.EvakaUser
import org.jdbi.v3.json.Json

data class ArchivedProcess(
    val id: ArchivedProcessId,
    val processDefinitionNumber: String,
    val year: Int,
    val number: Int,
    val organization: String,
    val archiveDurationMonths: Int,
    val migrated: Boolean,
    @Json val history: List<ArchivedProcessHistoryRow>,
) {
    val processNumber: String
        get() = "$number/$processDefinitionNumber/$year"
}

data class ArchivedProcessHistoryRow(
    val rowIndex: Int,
    val state: ArchivedProcessState,
    val enteredAt: HelsinkiDateTime,
    val enteredBy: EvakaUser,
)

enum class ArchivedProcessState : DatabaseEnum {
    INITIAL,
    PREPARATION,
    DECIDING,
    COMPLETED;

    override val sqlType: String = "archived_process_state"
}

fun Database.Transaction.insertProcess(
    processDefinitionNumber: String,
    year: Int,
    organization: String,
    archiveDurationMonths: Int,
    migrated: Boolean = false,
): ArchivedProcess =
    createQuery {
            sql(
                """
    INSERT INTO archived_process (process_definition_number, year, number, organization, archive_duration_months, migrated)
    VALUES (
        ${bind(processDefinitionNumber)}, 
        ${bind(year)},
        coalesce((
            SELECT max(number)
            FROM archived_process
            WHERE process_definition_number = ${bind(processDefinitionNumber)} AND year = ${bind(year)}
        ), 0) + 1,
        ${bind(organization)},
        ${bind(archiveDurationMonths)},
        ${bind(migrated)}
    )
    RETURNING id, process_definition_number, year, number, organization, archive_duration_months, migrated, '[]'::jsonb AS history
"""
            )
        }
        .exactlyOne()

fun Database.Read.getProcess(id: ArchivedProcessId): ArchivedProcess? =
    createQuery {
            sql(
                """
    SELECT
        ap.id,
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
        FROM archived_process_history aph
        JOIN public.evaka_user eu on eu.id = aph.entered_by
        WHERE aph.process_id = ap.id
    ) AS history
    FROM archived_process ap
    WHERE ap.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Read.getArchiveProcessByChildDocumentId(
    documentId: ChildDocumentId
): ArchivedProcess? {
    return createQuery {
            sql("SELECT process_id FROM child_document WHERE id = ${bind(documentId)}")
        }
        .exactlyOneOrNull<ArchivedProcessId?>()
        ?.let { processId -> getProcess(processId) }
}

fun Database.Read.getArchiveProcessByAssistanceNeedDecisionId(
    decisionId: AssistanceNeedDecisionId
): ArchivedProcess? {
    return createQuery {
            sql("SELECT process_id FROM assistance_need_decision WHERE id = ${bind(decisionId)}")
        }
        .exactlyOneOrNull<ArchivedProcessId?>()
        ?.let { processId -> getProcess(processId) }
}

fun Database.Read.getArchiveProcessByAssistanceNeedPreschoolDecisionId(
    decisionId: AssistanceNeedPreschoolDecisionId
): ArchivedProcess? {
    return createQuery {
            sql(
                "SELECT process_id FROM assistance_need_preschool_decision WHERE id = ${bind(decisionId)}"
            )
        }
        .exactlyOneOrNull<ArchivedProcessId?>()
        ?.let { processId -> getProcess(processId) }
}

fun Database.Read.getArchiveProcessByApplicationId(applicationId: ApplicationId): ArchivedProcess? {
    return createQuery {
            sql("SELECT process_id FROM application WHERE id = ${bind(applicationId)}")
        }
        .exactlyOneOrNull<ArchivedProcessId?>()
        ?.let { processId -> getProcess(processId) }
}

fun Database.Read.getArchiveProcessByFeeDecisionId(feeDecisionId: FeeDecisionId): ArchivedProcess? {
    return createQuery {
            sql("SELECT process_id FROM fee_decision WHERE id = ${bind(feeDecisionId)}")
        }
        .exactlyOneOrNull<ArchivedProcessId?>()
        ?.let { processId -> getProcess(processId) }
}

fun Database.Read.getArchiveProcessByVoucherValueDecisionId(
    voucherValueDecisionId: VoucherValueDecisionId
): ArchivedProcess? {
    return createQuery {
            sql(
                "SELECT process_id FROM voucher_value_decision WHERE id = ${bind(voucherValueDecisionId)}"
            )
        }
        .exactlyOneOrNull<ArchivedProcessId?>()
        ?.let { processId -> getProcess(processId) }
}

fun Database.Transaction.deleteProcessById(processId: ArchivedProcessId) {
    execute { sql("DELETE FROM archived_process WHERE id = ${bind(processId)}") }
}

fun deleteProcessByDocumentId(tx: Database.Transaction, documentId: ChildDocumentId) {
    tx.createQuery { sql("SELECT process_id FROM child_document WHERE id = ${bind(documentId)}") }
        .exactlyOneOrNull<ArchivedProcessId?>()
        ?.also { processId -> tx.deleteProcessById(processId) }
}

fun deleteProcessByAssistanceNeedDecisionId(
    tx: Database.Transaction,
    decisionId: AssistanceNeedDecisionId,
) {
    tx.createQuery {
            sql("SELECT process_id FROM assistance_need_decision WHERE id = ${bind(decisionId)}")
        }
        .exactlyOneOrNull<ArchivedProcessId?>()
        ?.also { processId -> tx.deleteProcessById(processId) }
}

fun deleteProcessByAssistanceNeedPreschoolDecisionId(
    tx: Database.Transaction,
    decisionId: AssistanceNeedPreschoolDecisionId,
) {
    tx.createQuery {
            sql(
                "SELECT process_id FROM assistance_need_preschool_decision WHERE id = ${bind(decisionId)}"
            )
        }
        .exactlyOneOrNull<ArchivedProcessId?>()
        ?.also { processId -> tx.deleteProcessById(processId) }
}

fun Database.Transaction.insertProcessHistoryRow(
    processId: ArchivedProcessId,
    state: ArchivedProcessState,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
) {
    execute {
        sql(
            """
    INSERT INTO archived_process_history (process_id, row_index, state, entered_at, entered_by) VALUES (
        ${bind(processId)},
        coalesce((
            SELECT max(row_index)
            FROM archived_process_history
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

fun updateDocumentProcessHistory(
    tx: Database.Transaction,
    document: ChildDocumentDetails,
    newStatus: DocumentStatus,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
) {
    val process = tx.getArchiveProcessByChildDocumentId(document.id) ?: return

    val currentProcessState = process.history.lastOrNull()?.state ?: return
    val currentStateIndex = ArchivedProcessState.entries.indexOf(currentProcessState)

    val newProcessState =
        when (newStatus) {
            DocumentStatus.DRAFT -> ArchivedProcessState.INITIAL
            DocumentStatus.PREPARED -> ArchivedProcessState.PREPARATION
            DocumentStatus.CITIZEN_DRAFT -> ArchivedProcessState.INITIAL
            DocumentStatus.DECISION_PROPOSAL -> ArchivedProcessState.PREPARATION
            DocumentStatus.COMPLETED ->
                // decision documents are completed only once the SFI message is sent
                if (document.template.type.decision) ArchivedProcessState.DECIDING
                else ArchivedProcessState.COMPLETED
        }
    val newStateIndex = ArchivedProcessState.entries.indexOf(newProcessState)

    when {
        newStateIndex > currentStateIndex ->
            // moving forwards
            tx.insertProcessHistoryRow(
                processId = process.id,
                state = newProcessState,
                now = now,
                userId = userId,
            )
        newStateIndex < currentStateIndex ->
            // moving backwards
            tx.cancelLastProcessHistoryRow(
                processId = process.id,
                stateToCancel = currentProcessState,
            )
    }
}

fun autoCompleteDocumentProcessHistory(
    tx: Database.Transaction,
    documentId: ChildDocumentId,
    now: HelsinkiDateTime,
) {
    val process = tx.getArchiveProcessByChildDocumentId(documentId) ?: return
    val currentProcessState = process.history.lastOrNull()?.state ?: return

    if (currentProcessState != ArchivedProcessState.COMPLETED) {
        tx.insertProcessHistoryRow(
            processId = process.id,
            state = ArchivedProcessState.COMPLETED,
            now = now,
            userId = AuthenticatedUser.SystemInternalUser.evakaUserId,
        )
    }
}

fun Database.Transaction.cancelLastProcessHistoryRow(
    processId: ArchivedProcessId,
    stateToCancel: ArchivedProcessState,
) {
    createUpdate {
            sql(
                """
    DELETE FROM archived_process_history aph
    WHERE aph.process_id = ${bind(processId)} AND aph.state = ${bind(stateToCancel)} AND 
        aph.row_index = (
            SELECT max(row_index) 
            FROM archived_process_history
            WHERE process_id = ${bind(processId)}
        )
"""
            )
        }
        .updateExactlyOne()
}
