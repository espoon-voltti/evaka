// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.shared.EvakaUserId
import evaka.core.shared.Id
import evaka.core.shared.McpAuthorizationId
import evaka.core.shared.McpTestDataBatchId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound

/**
 * Keeps track of test data created via the MCP server so that it can be listed and deleted as a set
 * ("batch") afterwards, either by an AI assistant or by an admin in the employee UI.
 */
object McpTestDataService {
    fun getOrCreateBatch(
        tx: Database.Transaction,
        name: String,
        createdBy: EvakaUserId,
        authorizationId: McpAuthorizationId?,
        now: HelsinkiDateTime,
    ): McpTestDataBatchId {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw BadRequest("Batch name must not be empty")
        if (trimmed.length > 200) throw BadRequest("Batch name is too long")
        return tx.getMcpTestDataBatchByName(trimmed, createdBy)?.id
            ?: tx.insertMcpTestDataBatch(
                name = trimmed,
                description = "",
                createdBy = createdBy,
                authorizationId = authorizationId,
                now = now,
            )
    }

    fun track(
        tx: Database.Transaction,
        batchId: McpTestDataBatchId,
        tableName: String,
        entityId: Id<*>,
        description: String,
        now: HelsinkiDateTime,
    ) = tx.insertMcpTestDataEntity(batchId, tableName, entityId.raw, description, now)

    fun getBatchOf(tx: Database.Read, tableName: String, entityId: Id<*>): McpTestDataBatchId? =
        tx.getMcpTestDataEntityBatch(tableName, entityId.raw)

    data class DeletionResult(
        val batchId: McpTestDataBatchId,
        val batchName: String,
        val trackedEntities: Int,
        /** Deleted rows per table, including rows that depended on the tracked entities */
        val deletedRowCounts: Map<String, Int>,
        /**
         * The subset of deleted rows per table that were not tracked in the batch itself, i.e. rows
         * that something else (a tester in the UI, a real workflow) created on top of the test data
         */
        val untrackedRowCounts: Map<String, Int>,
    )

    /**
     * Deletes every tracked entity of the batch (and all rows depending on them) and finally the
     * batch itself. Entities are removed in reverse creation order so that dependents (e.g.
     * placements) go before the things they depend on (e.g. persons and units).
     *
     * Unless [allowUntrackedRows] is set, the deletion is refused (and rolled back) if it would
     * also remove rows that were not tracked in the batch, so that a careless call can never wipe
     * out data somebody created on top of the test data. [previewDeletion] shows those rows.
     */
    fun deleteBatch(
        tx: Database.Transaction,
        batchId: McpTestDataBatchId,
        allowUntrackedRows: Boolean,
    ): DeletionResult {
        val batch = tx.getMcpTestDataBatch(batchId) ?: throw NotFound("Batch $batchId not found")
        val entities = tx.getMcpTestDataEntities(batchId)
        val deleter = McpCascadeDeleter(tx, batchId)
        deleter.requireDeletableTables(entities.map { it.tableName })
        entities.asReversed().forEach { deleter.delete(it.tableName, it.entityId) }
        tx.deleteMcpTestDataBatch(batchId)
        val result =
            DeletionResult(
                batchId = batchId,
                batchName = batch.name,
                trackedEntities = entities.size,
                deletedRowCounts = deleter.deletedRowCounts,
                untrackedRowCounts = deleter.untrackedRowCounts,
            )
        if (!allowUntrackedRows && result.untrackedRowCounts.isNotEmpty()) {
            val rows = result.untrackedRowCounts.entries.joinToString { "${it.key}: ${it.value}" }
            throw Conflict(
                "Deleting batch '${batch.name}' would also delete ${result.untrackedRowCounts.values.sum()} row(s) that were not created via MCP ($rows). Review them first and then explicitly allow deleting untracked rows."
            )
        }
        return result
    }

    private class DeletionPreview(val result: DeletionResult) : RuntimeException("preview")

    /** Reports what [deleteBatch] would delete, without changing anything */
    fun previewDeletion(tx: Database.Transaction, batchId: McpTestDataBatchId): DeletionResult =
        try {
            tx.subTransaction {
                throw DeletionPreview(deleteBatch(tx, batchId, allowUntrackedRows = true))
            }
        } catch (e: DeletionPreview) {
            e.result
        }

    data class BatchSummary(
        val id: McpTestDataBatchId,
        val name: String,
        val description: String,
        val createdAt: HelsinkiDateTime,
        val createdBy: EvakaUserId,
        val createdByName: String,
        val clientName: String?,
        val entityCounts: Map<String, Int>,
        val totalEntities: Int,
    )

    fun getBatchSummaries(tx: Database.Read, createdBy: EvakaUserId? = null): List<BatchSummary> {
        val counts = tx.getMcpTestDataEntityCounts().groupBy { it.batchId }
        return tx.getMcpTestDataBatches(createdBy).map { batch ->
            val batchCounts =
                counts[batch.id].orEmpty().associateTo(sortedMapOf()) { it.tableName to it.count }
            BatchSummary(
                id = batch.id,
                name = batch.name,
                description = batch.description,
                createdAt = batch.createdAt,
                createdBy = batch.createdBy,
                createdByName = batch.createdByName,
                clientName = batch.clientName,
                entityCounts = batchCounts,
                totalEntities = batchCounts.values.sum(),
            )
        }
    }
}
