// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.DataRemovalEnv
import evaka.core.childimages.deleteImageFile
import evaka.core.document.childdocument.deleteExpiredChildDocuments
import evaka.core.s3.DocumentService
import evaka.core.shared.ChildId
import evaka.core.shared.ChildImageId
import evaka.core.shared.Id
import evaka.core.shared.PersonId
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

@Service
class DataRemovalService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val dataRemovalEnv: DataRemovalEnv,
    private val documentClient: DocumentService,
) {
    init {
        asyncJobRunner.registerHandler(::deleteExpiredData)
        asyncJobRunner.registerHandler(::deleteChildImage)
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

        unsetExpiredChildReferences(
            dbc,
            expireDate = today.minusYears(1),
            limit,
            columns = listOf("diet_id", "meal_texture_id", "nekku_diet"),
        )

        deleteExpiredChildImages(dbc, now, expireDate = today.minusMonths(1), limit)

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
        logger.infoChunked(
            items = deleted,
            meta = { chunk ->
                mapOf(
                    "deletedDocumentIds" to chunk.map { it.documentId },
                    "deletedDecisionIds" to chunk.mapNotNull { it.decisionId },
                    "deletedProcessIds" to chunk.mapNotNull { it.processId },
                    "deletedDocumentKeys" to chunk.flatMap { it.documentKeys },
                    "deletedSfiMessageIds" to chunk.flatMap { it.sfiMessageIds },
                )
            },
            message = { index, total -> "Deleted expired child documents (chunk $index/$total)" },
        )
        if (deleted.size >= limit) {
            logger.info {
                "Child document deletion hit batch limit of $limit; remaining backlog will be processed on the next run"
            }
        }
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

        logger.infoChunked(
            items = ids,
            meta = { chunk -> mapOf("deletedIds" to chunk) },
            message = { index, total -> "Deleted expired child images (chunk $index/$total)" },
        )
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
        logger.infoChunked(
            items = deleted,
            meta = { chunk -> mapOf("table" to table, "deletedIds" to chunk) },
            message = { index, total ->
                "Deleted expired rows from table $table (chunk $index/$total)"
            },
        )
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
            logger.infoChunked(
                items = childIds,
                meta = { chunk -> mapOf("column" to column, "childIds" to chunk) },
                message = { index, total ->
                    "Unset expired child.$column values (chunk $index/$total)"
                },
            )
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

fun deleteExpiredCitizenUsers(dbc: Database.Connection, expireDate: LocalDate, limit: Int) {
    logger.info { "Deleting at most $limit expired citizen users" }
    val deleted = dbc.transaction { tx -> tx.deleteExpiredCitizenUsersBatch(expireDate, limit) }
    logger.infoChunked(
        items = deleted,
        meta = { chunk -> mapOf("deletedIds" to chunk) },
        message = { index, total -> "Deleted expired citizen users (chunk $index/$total)" },
    )
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
