// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.shared.ArchivedProcessId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.EvakaUserId
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
    @Json val history: List<ArchivedProcessHistoryRow>
) {
    val processNumber: String
        get() = "$number/$processDefinitionNumber/$year"
}

data class ArchivedProcessHistoryRow(
    val rowIndex: Int,
    val state: ArchivedProcessState,
    val enteredAt: HelsinkiDateTime,
    val enteredBy: EvakaUser
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
    archiveDurationMonths: Int
): ArchivedProcess =
    createQuery {
            sql(
                """
    INSERT INTO archived_process (process_definition_number, year, number, organization, archive_duration_months)
    VALUES (
        ${bind(processDefinitionNumber)}, 
        ${bind(year)},
        coalesce((
            SELECT max(number)
            FROM archived_process
            WHERE process_definition_number = ${bind(processDefinitionNumber)} AND year = ${bind(year)}
        ), 0) + 1,
        ${bind(organization)},
        ${bind(archiveDurationMonths)}
    )
    RETURNING id, process_definition_number, year, number, organization, archive_duration_months, '[]'::jsonb AS history
"""
            )
        }
        .exactlyOne()

fun Database.Read.getProcess(id: ArchivedProcessId): ArchivedProcess? =
    createQuery {
            sql(
                """
    SELECT ap.id, ap.process_definition_number, ap.year, ap.number, ap.organization, ap.archive_duration_months,
    (
        SELECT coalesce(
            jsonb_agg(json_build_object(
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
    decisionId: AssistanceNeedDecisionId
) {
    tx.createQuery {
            sql("SELECT process_id FROM assistance_need_decision WHERE id = ${bind(decisionId)}")
        }
        .exactlyOneOrNull<ArchivedProcessId?>()
        ?.also { processId -> tx.deleteProcessById(processId) }
}

fun deleteProcessByAssistanceNeedPreschoolDecisionId(
    tx: Database.Transaction,
    decisionId: AssistanceNeedPreschoolDecisionId
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
    userId: EvakaUserId
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
    documentId: ChildDocumentId,
    newStatus: DocumentStatus,
    now: HelsinkiDateTime,
    userId: EvakaUserId
) {
    data class Document(val status: DocumentStatus, val processId: ArchivedProcessId?)
    val document =
        tx.createQuery {
                sql("SELECT status, process_id FROM child_document WHERE id = ${bind(documentId)}")
            }
            .exactlyOne<Document>()
    document.processId?.let { processId ->
        val processState = tx.getProcess(processId)?.history?.lastOrNull()?.state
        val newProcessState =
            when {
                newStatus == DocumentStatus.COMPLETED -> ArchivedProcessState.COMPLETED
                processState == ArchivedProcessState.COMPLETED &&
                    newStatus != DocumentStatus.COMPLETED -> ArchivedProcessState.INITIAL
                else -> null
            }
        newProcessState?.let {
            tx.insertProcessHistoryRow(
                processId = processId,
                state = it,
                now = now,
                userId = userId
            )
        }
    }
}
