// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.DataRemovalEnv
import evaka.core.caseprocess.deleteCaseProcesses
import evaka.core.childimages.deleteImageFile
import evaka.core.document.childdocument.deleteExpiredChildDocuments
import evaka.core.s3.DocumentKey
import evaka.core.s3.DocumentService
import evaka.core.shared.ApplicationId
import evaka.core.shared.AttachmentId
import evaka.core.shared.CaseProcessId
import evaka.core.shared.ChildId
import evaka.core.shared.ChildImageId
import evaka.core.shared.DecisionId
import evaka.core.shared.FinanceNoteId
import evaka.core.shared.Id
import evaka.core.shared.PersonId
import evaka.core.shared.PlacementId
import evaka.core.shared.ServiceApplicationId
import evaka.core.shared.SfiMessageId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.db.QuerySql
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

private fun auditExpiredDelete(
    entity: String,
    targetId: AuditId,
    meta: Map<String, Any?> = emptyMap(),
) = Audit.DataRemovalExpiredDelete.log(targetId = targetId, meta = meta + ("entity" to entity))

private fun auditExpiredUnset(
    entity: String,
    targetId: AuditId,
    meta: Map<String, Any?> = emptyMap(),
) = Audit.DataRemovalExpiredUnset.log(targetId = targetId, meta = meta + ("entity" to entity))

@Service
class DataRemovalService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val dataRemovalEnv: DataRemovalEnv,
    private val documentClient: DocumentService,
) {
    init {
        asyncJobRunner.registerHandler(::deleteExpiredData)
        asyncJobRunner.registerHandler(::deleteChildImage)
        asyncJobRunner.registerHandler(::deleteDecisionPdf)
    }

    fun planDataRemoval(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType.ofPayload(AsyncJob.DeleteExpiredData)))
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.DeleteExpiredData),
                retryCount = 1,
                runAt = clock.now(),
            )
        }
        logger.info { "Planned data removal job" }
    }

    fun deleteExpiredData(
        dbc: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.DeleteExpiredData,
    ) {
        val now = clock.now()
        val today = now.toLocalDate()
        val limit = dataRemovalEnv.limit

        if (limit == 0) {
            logger.warn { "Data removal limit is set to 0, not deleting anything" }
            return
        }

        deleteExpiredChildDocuments(dbc, now, limit)

        deleteExpiredApplications(dbc, now, expireDate = today.minusYears(10), limit)

        deleteExpiredChildLeafRows(
            dbc,
            expireDate = today.minusYears(1),
            limit,
            leafTables =
                listOf(
                    "calendar_event_attendee",
                    "calendar_event_time",
                    "child_sticky_note",
                    "family_contact",
                    "nekku_special_diet_choices",
                ),
        )

        deleteExpiredChildLeafRows(
            dbc,
            expireDate = today.minusYears(10),
            limit,
            leafTables =
                listOf(
                    "assistance_factor",
                    "assistance_action",
                    "daycare_assistance",
                    "preschool_assistance",
                    "other_assistance_measure",
                    "daily_service_time",
                    "attendance_reservation",
                    "backup_care",
                    "child_attendance",
                    "absence_application",
                    "backup_pickup",
                    "holiday_questionnaire_answer",
                ),
        )

        deleteExpiredServiceApplications(dbc, expireDate = today.minusYears(10), limit)

        unsetExpiredChildReferences(
            dbc,
            expireDate = today.minusYears(1),
            limit,
            columns = listOf("diet_id", "meal_texture_id", "nekku_diet"),
        )

        deleteExpiredChildImages(dbc, now, expireDate = today.minusMonths(1), limit)

        deleteExpiredFinanceNotes(dbc, expireDate = today.minusYears(5), limit)

        deleteExpiredCitizenUsers(dbc, expireDate = today.minusYears(1), limit)
    }

    fun deleteExpiredChildDocuments(db: Database.Connection, now: HelsinkiDateTime, limit: Int) {
        logger.info { "Deleting at most $limit expired child documents" }
        val deleted = db.transaction { tx ->
            val results = tx.deleteExpiredChildDocuments(now, limit = limit)
            val pdfKeys = results.flatMap { it.documentKeys }
            if (pdfKeys.isNotEmpty()) {
                asyncJobRunner.plan(
                    tx = tx,
                    payloads = pdfKeys.map { AsyncJob.DeleteChildDocumentPdf(it) },
                    runAt = now,
                )
            }
            results
        }
        logger.info { "Deleted ${deleted.size} expired child document(s)" }
        deleted.forEach { doc ->
            auditExpiredDelete(
                entity = "child_document",
                targetId = AuditId(doc.documentId),
                meta =
                    mapOf(
                        "decisionId" to doc.decisionId,
                        "processId" to doc.processId,
                        "documentKeys" to doc.documentKeys,
                        "sfiMessageIds" to doc.sfiMessageIds,
                    ),
            )
        }
        if (deleted.size >= limit) {
            logger.info {
                "Child document deletion hit batch limit of $limit; remaining backlog will be processed on the next run"
            }
        }
    }

    fun deleteExpiredApplications(
        db: Database.Connection,
        now: HelsinkiDateTime,
        expireDate: LocalDate,
        limit: Int,
    ) {
        logger.info { "Deleting at most $limit expired applications" }
        val deleted = db.transaction { tx ->
            val results = tx.deleteExpiredApplicationsBatch(expireDate, limit)
            val decisionKeys = results.flatMap { it.decisionDocumentKeys }
            if (decisionKeys.isNotEmpty()) {
                asyncJobRunner.plan(
                    tx = tx,
                    payloads = decisionKeys.map { AsyncJob.DeleteDecisionPdf(it) },
                    runAt = now,
                )
            }
            val attachmentIds = results.flatMap { it.attachmentIds }
            if (attachmentIds.isNotEmpty()) {
                asyncJobRunner.plan(
                    tx = tx,
                    payloads = attachmentIds.map { AsyncJob.DeleteAttachment(it) },
                    runAt = now,
                )
            }
            results
        }
        logger.info { "Deleted ${deleted.size} expired application(s)" }
        deleted.forEach { app ->
            auditExpiredDelete(
                entity = "application",
                targetId = AuditId(app.applicationId),
                meta =
                    mapOf(
                        "childId" to app.childId,
                        "decisionIds" to app.decisionIds,
                        "processId" to app.processId,
                        "decisionDocumentKeys" to app.decisionDocumentKeys,
                        "sfiMessageIds" to app.sfiMessageIds,
                        "attachmentIds" to app.attachmentIds,
                        "expireDate" to expireDate,
                    ),
            )
        }
        if (deleted.size >= limit) {
            logger.info {
                "Application deletion hit batch limit of $limit; remaining backlog will be processed on the next run"
            }
        }
    }

    fun deleteDecisionPdf(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.DeleteDecisionPdf,
    ) {
        documentClient.delete(DocumentKey.Decision(msg.key))
    }

    fun deleteExpiredChildImages(
        dbc: Database.Connection,
        now: HelsinkiDateTime,
        expireDate: LocalDate,
        limit: Int,
    ) {
        val ids = dbc.transaction { tx ->
            val childImageIds: List<ChildImageId> =
                tx.deleteExpiredChildLeafRowsFromTable(expireDate, limit, table = "child_images")
            asyncJobRunner.plan(
                tx,
                childImageIds.map { AsyncJob.DeleteChildImage(it) },
                runAt = now,
            )
            childImageIds
        }

        ids.forEach { imageId ->
            auditExpiredDelete(
                entity = "child_images",
                targetId = AuditId(imageId),
                meta = mapOf("expireDate" to expireDate),
            )
        }
    }

    fun deleteChildImage(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.DeleteChildImage,
    ) {
        deleteImageFile(documentClient, msg.imageId)
    }
}

fun deleteExpiredChildLeafRows(
    dbc: Database.Connection,
    expireDate: LocalDate,
    limit: Int,
    leafTables: List<String>,
) {
    leafTables.forEach { table ->
        logger.info { "Deleting at most $limit expired rows in table $table" }
        val deleted = dbc.transaction { tx ->
            tx.deleteExpiredChildLeafRowsFromTable<Id<*>>(expireDate, limit, table)
        }
        deleted.forEach { id ->
            auditExpiredDelete(
                entity = table,
                targetId = AuditId(id),
                meta = mapOf("expireDate" to expireDate),
            )
        }
    }
}

private inline fun <reified T : Id<*>> Database.Transaction.deleteExpiredChildLeafRowsFromTable(
    expireDate: LocalDate,
    limit: Int,
    table: String,
): List<T> =
    createUpdate {
            sql(
                """
WITH del_batch AS (
    SELECT id
    FROM $table
    WHERE child_id = ANY(${subquery(childIdsWithPlacementsEndingBefore(expireDate))})
    FOR UPDATE
    LIMIT ${bind(limit)}
)
DELETE FROM $table
USING del_batch
WHERE $table.id = del_batch.id
RETURNING $table.id
        """
            )
        }
        .executeAndReturnGeneratedKeys()
        .toList()

fun unsetExpiredChildReferences(
    dbc: Database.Connection,
    expireDate: LocalDate,
    limit: Int,
    columns: List<String>,
) {
    columns.forEach { column ->
        if (limit > 0) {
            logger.info { "Unsetting at most $limit expired references in child.$column" }
            val childIds = dbc.transaction { tx ->
                tx.unsetExpiredChildReference(expireDate, limit, column)
            }
            childIds.forEach { childId ->
                auditExpiredUnset(
                    entity = "child",
                    targetId = AuditId(childId),
                    meta = mapOf("column" to column, "expireDate" to expireDate),
                )
            }
        }
    }
}

fun Database.Transaction.unsetExpiredChildReference(
    expireDate: LocalDate,
    limit: Int,
    column: String,
): List<ChildId> =
    createUpdate {
            sql(
                """
WITH update_batch AS (
    SELECT id
    FROM child
    WHERE
        id = ANY(${subquery(childIdsWithPlacementsEndingBefore(expireDate))}) AND
        $column IS NOT NULL
    FOR UPDATE
    LIMIT ${bind(limit)}
)
UPDATE child
SET $column = NULL
FROM update_batch
WHERE child.id = update_batch.id
RETURNING child.id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .toList()

private fun childIdsWithPlacementsEndingBefore(date: LocalDate) = QuerySql {
    sql(
        """
SELECT child_id
FROM placement
GROUP BY child_id
HAVING max(end_date) < ${bind(date)}
"""
    )
}

data class DeletedApplication(
    val applicationId: ApplicationId,
    val childId: ChildId,
    val decisionIds: List<DecisionId>,
    val processId: CaseProcessId?,
    val decisionDocumentKeys: List<String>,
    val sfiMessageIds: List<SfiMessageId>,
    val attachmentIds: List<AttachmentId>,
)

private fun Database.Transaction.deleteExpiredApplicationsBatch(
    expireDate: LocalDate,
    limit: Int,
): List<DeletedApplication> {
    val deletableRows =
        createQuery {
                sql(
                    """
WITH del_batch AS (
    SELECT id
    FROM application
    WHERE child_id = ANY(${subquery(childIdsWithPlacementsEndingBefore(expireDate))})
    FOR UPDATE
    LIMIT ${bind(limit)}
)
SELECT
    a.id AS application_id,
    a.child_id,
    a.process_id,
    COALESCE(array_agg(DISTINCT d.id) FILTER (WHERE d.id IS NOT NULL), '{}') AS decision_ids,
    COALESCE(array_agg(DISTINCT k.key) FILTER (WHERE k.key IS NOT NULL), '{}') AS decision_document_keys,
    COALESCE(array_agg(DISTINCT sm.id) FILTER (WHERE sm.id IS NOT NULL), '{}') AS sfi_message_ids,
    COALESCE(array_agg(DISTINCT att.id) FILTER (WHERE att.id IS NOT NULL), '{}') AS attachment_ids
FROM del_batch
JOIN application a ON a.id = del_batch.id
LEFT JOIN decision d ON d.application_id = a.id
LEFT JOIN LATERAL (VALUES (d.document_key), (d.other_guardian_document_key)) AS k(key)
    ON k.key IS NOT NULL
LEFT JOIN sfi_message sm ON sm.decision_id = d.id
LEFT JOIN attachment att ON att.application_id = a.id
GROUP BY a.id
"""
                )
            }
            .toList<DeletedApplication>()

    val deletableIds = deletableRows.map { it.applicationId }
    val decisionIds = deletableRows.flatMap { it.decisionIds }
    val processIds = deletableRows.mapNotNull { it.processId }
    val sfiMessageIds = deletableRows.flatMap { it.sfiMessageIds }

    if (deletableIds.isNotEmpty()) {
        execute {
            sql(
                "UPDATE placement SET source_application_id = NULL WHERE source_application_id = ANY(${bind(deletableIds)})"
            )
        }
        execute {
            sql(
                "UPDATE income SET application_id = NULL WHERE application_id = ANY(${bind(deletableIds)})"
            )
        }
        execute {
            sql(
                "UPDATE fridge_child SET created_by_application = NULL WHERE created_by_application = ANY(${bind(deletableIds)})"
            )
        }
        execute {
            sql(
                "UPDATE fridge_partner SET created_from_application = NULL WHERE created_from_application = ANY(${bind(deletableIds)})"
            )
        }
        execute {
            sql(
                "UPDATE message_thread SET application_id = NULL WHERE application_id = ANY(${bind(deletableIds)})"
            )
        }
        if (sfiMessageIds.isNotEmpty()) {
            execute {
                sql("DELETE FROM sfi_message_event WHERE message_id = ANY(${bind(sfiMessageIds)})")
            }
            execute { sql("DELETE FROM sfi_message WHERE id = ANY(${bind(sfiMessageIds)})") }
        }
        if (decisionIds.isNotEmpty()) {
            execute { sql("DELETE FROM decision WHERE id = ANY(${bind(decisionIds)})") }
        }
        execute {
            sql("DELETE FROM placement_plan WHERE application_id = ANY(${bind(deletableIds)})")
        }
        execute { sql("DELETE FROM application WHERE id = ANY(${bind(deletableIds)})") }
        deleteCaseProcesses(processIds)
    }

    return deletableRows
}

fun deleteExpiredFinanceNotes(dbc: Database.Connection, expireDate: LocalDate, limit: Int) {
    logger.info { "Deleting at most $limit expired finance notes" }
    val deleted = dbc.transaction { tx -> tx.deleteExpiredFinanceNotesBatch(expireDate, limit) }
    deleted.forEach { id ->
        auditExpiredDelete(
            entity = "finance_note",
            targetId = AuditId(id),
            meta = mapOf("expireDate" to expireDate),
        )
    }
}

private fun Database.Transaction.deleteExpiredFinanceNotesBatch(
    expireDate: LocalDate,
    limit: Int,
): List<FinanceNoteId> =
    createUpdate {
            sql(
                """
WITH del_batch AS (
    SELECT id
    FROM finance_note
    WHERE person_id = ANY(${subquery(personIdsWithExpiredFinanceConnections(expireDate))})
    FOR UPDATE
    LIMIT ${bind(limit)}
)
DELETE FROM finance_note
USING del_batch
WHERE finance_note.id = del_batch.id
RETURNING finance_note.id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .toList()

fun deleteExpiredServiceApplications(dbc: Database.Connection, expireDate: LocalDate, limit: Int) {
    logger.info { "Deleting at most $limit expired service applications" }
    val (deleted, clearedPlacements) =
        dbc.transaction { tx -> tx.deleteExpiredServiceApplicationsBatch(expireDate, limit) }
    deleted.forEach { id ->
        auditExpiredDelete(
            entity = "service_application",
            targetId = AuditId(id),
            meta = mapOf("expireDate" to expireDate),
        )
    }
    clearedPlacements.forEach { id ->
        auditExpiredUnset(
            entity = "placement",
            targetId = AuditId(id),
            meta =
                mapOf(
                    "expireDate" to expireDate,
                    "clearedColumns" to listOf("source", "source_service_application_id"),
                ),
        )
    }
}

private fun Database.Transaction.deleteExpiredServiceApplicationsBatch(
    expireDate: LocalDate,
    limit: Int,
): Pair<List<ServiceApplicationId>, List<PlacementId>> {
    val batch =
        createQuery {
                sql(
                    """
SELECT id
FROM service_application
WHERE child_id = ANY(${subquery(childIdsWithPlacementsEndingBefore(expireDate))})
FOR UPDATE
LIMIT ${bind(limit)}
"""
                )
            }
            .toList<ServiceApplicationId>()

    if (batch.isEmpty()) return emptyList<ServiceApplicationId>() to emptyList()

    // A placement created from an accepted service application points back at it
    // (source = 'SERVICE_APPLICATION'). The FK blocks deletion and
    // check$source_service_application_ref
    // forbids nulling the pointer while source = 'SERVICE_APPLICATION', so clear both: with
    // source = NULL the CHECK's first disjunct is NULL (not FALSE) and the constraint passes.
    val clearedPlacements =
        createUpdate {
                sql(
                    """
UPDATE placement
SET source = NULL, source_service_application_id = NULL
WHERE source_service_application_id = ANY(${bind(batch)})
RETURNING id
"""
                )
            }
            .executeAndReturnGeneratedKeys()
            .toList<PlacementId>()

    execute { sql("DELETE FROM service_application WHERE id = ANY(${bind(batch)})") }

    return batch to clearedPlacements
}

fun deleteExpiredCitizenUsers(dbc: Database.Connection, expireDate: LocalDate, limit: Int) {
    logger.info { "Deleting at most $limit expired citizen users" }
    val deleted = dbc.transaction { tx -> tx.deleteExpiredCitizenUsersBatch(expireDate, limit) }
    deleted.forEach { id ->
        auditExpiredDelete(
            entity = "citizen_user",
            targetId = AuditId(id),
            meta = mapOf("expireDate" to expireDate),
        )
    }
}

private fun Database.Transaction.deleteExpiredCitizenUsersBatch(
    expireDate: LocalDate,
    limit: Int,
): List<PersonId> =
    createUpdate {
            sql(
                """
WITH del_batch AS (
    SELECT id
    FROM citizen_user
    WHERE id = ANY(${subquery(citizenUserIdsWithPlacementsEndingBefore(expireDate))})
    FOR UPDATE
    LIMIT ${bind(limit)}
)
DELETE FROM citizen_user
USING del_batch
WHERE citizen_user.id = del_batch.id
RETURNING citizen_user.id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .toList()

private fun citizenUserIdsWithPlacementsEndingBefore(date: LocalDate) = QuerySql {
    sql(
        """
WITH guardian_child AS (
    SELECT guardian_id AS person_id, child_id FROM guardian
    UNION ALL
    SELECT parent_id AS person_id, child_id FROM foster_parent
)
SELECT gc.person_id
FROM guardian_child gc
JOIN citizen_user cu ON cu.id = gc.person_id
JOIN placement p ON p.child_id = gc.child_id
GROUP BY gc.person_id
HAVING max(p.end_date) < ${bind(date)}
"""
    )
}

private fun personIdsWithExpiredFinanceConnections(date: LocalDate) = QuerySql {
    sql(
        """
WITH finance_connection AS (
    SELECT g.guardian_id AS person_id, g.child_id
    FROM guardian g
    UNION ALL
    SELECT fp.parent_id AS person_id, fp.child_id
    FROM foster_parent fp
    UNION ALL
    SELECT fc.head_of_child AS person_id, fc.child_id
    FROM fridge_child fc
    WHERE fc.conflict = false
    UNION ALL
    SELECT partner.person_id AS person_id, fc.child_id
    FROM fridge_child fc
    JOIN fridge_partner partner_head ON partner_head.person_id = fc.head_of_child
    JOIN fridge_partner partner
        ON partner.partnership_id = partner_head.partnership_id
        AND partner.person_id <> partner_head.person_id
    WHERE fc.conflict = false AND partner_head.conflict = false AND partner.conflict = false
)
SELECT fconn.person_id
FROM finance_connection fconn
JOIN placement p ON p.child_id = fconn.child_id
GROUP BY fconn.person_id
HAVING max(p.end_date) < ${bind(date)}
"""
    )
}
