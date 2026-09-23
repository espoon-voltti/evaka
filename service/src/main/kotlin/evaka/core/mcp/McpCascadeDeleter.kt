// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.shared.McpTestDataBatchId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import java.util.UUID

private val identifierRegex = Regex("^[a-z_][a-z0-9_]*$")

/**
 * Deletes the rows tracked in one test data batch and, recursively, every row in any table that
 * references them via a foreign key.
 *
 * eVaka's schema mostly avoids `ON DELETE CASCADE`, so removing a test entity (e.g. a person or a
 * unit) by hand would require knowing every dependent table. This helper instead discovers the
 * dependencies from the PostgreSQL system catalogs at runtime, so it also handles rows created
 * later by normal UI usage (e.g. a placement plan made for a test application).
 *
 * Foreign keys declared `ON DELETE SET NULL` / `SET DEFAULT` are honoured instead of followed, so
 * e.g. deleting a test employee who was picked as a unit's finance decision handler only clears
 * that column and leaves the unit alone. This also covers `evaka_user`, whose links to
 * person/employee/mobile device rows are all `ON DELETE SET NULL`.
 *
 * The cascade refuses to delete a row that is tracked in another batch, because that batch would
 * silently lose data another tester still relies on. Rows not tracked in any batch are deleted but
 * counted separately as "untracked", so the caller can show what the cascade reached beyond the
 * batch and refuse unless that was explicitly accepted. Rows of tables without a uuid `id` column
 * (pure link rows such as `guardian` or `daycare_acl`) can never be tracked, so they are not
 * counted as untracked.
 */
class McpCascadeDeleter(
    private val tx: Database.Transaction,
    private val batchId: McpTestDataBatchId,
) {
    private val catalog = McpSchemaCatalog(tx)

    private data class TrackedEntity(
        val tableName: String,
        val entityId: UUID,
        val batchId: McpTestDataBatchId,
        val batchName: String,
    )

    private val trackedEntities: Map<Pair<String, UUID>, TrackedEntity> by lazy {
        tx.createQuery {
                sql(
                    """
SELECT e.table_name, e.entity_id, e.batch_id, b.name AS batch_name
FROM mcp_test_data_entity e
JOIN mcp_test_data_batch b ON b.id = e.batch_id
"""
                )
            }
            .toList<TrackedEntity>()
            .associateBy { it.tableName to it.entityId }
    }

    private val visited = HashSet<Pair<String, UUID>>()

    /** Number of deleted rows per table */
    val deletedRowCounts: MutableMap<String, Int> = sortedMapOf()

    /** Number of deleted rows per table that were not tracked in the batch being deleted */
    val untrackedRowCounts: MutableMap<String, Int> = sortedMapOf()

    /** Checks all tables up front so that an unknown one fails before a long cascade */
    fun requireDeletableTables(tables: Collection<String>) {
        tables.toSortedSet().forEach(::requireDeletableTable)
    }

    private fun requireDeletableTable(table: String) {
        requireIdentifier(table)
        if (table !in catalog.tablesWithUuidId)
            throw BadRequest("Table $table does not exist or has no uuid id column")
    }

    fun delete(table: String, id: UUID) {
        requireDeletableTable(table)
        if (!visited.add(table to id)) return
        val tracked = trackedEntities[table to id]
        if (tracked != null && tracked.batchId != batchId) {
            throw Conflict(
                "Cannot delete: $table $id belongs to test data batch '${tracked.batchName}'. Delete that batch first."
            )
        }
        for (fk in catalog.foreignKeysByParent[table].orEmpty()) {
            requireIdentifier(fk.childTable)
            requireIdentifier(fk.childColumn)
            when (fk.onDelete) {
                McpSchemaCatalog.OnDelete.SET_NULL ->
                    tx.execute {
                        sql(
                            "UPDATE ${fk.childTable} SET ${fk.childColumn} = NULL WHERE ${fk.childColumn} = ${bind(id)}"
                        )
                    }
                McpSchemaCatalog.OnDelete.SET_DEFAULT ->
                    tx.execute {
                        sql(
                            "UPDATE ${fk.childTable} SET ${fk.childColumn} = DEFAULT WHERE ${fk.childColumn} = ${bind(id)}"
                        )
                    }
                McpSchemaCatalog.OnDelete.DELETE_ROW ->
                    if (fk.childTable in catalog.tablesWithUuidId) {
                        val childIds =
                            tx.createQuery {
                                    sql(
                                        "SELECT id FROM ${fk.childTable} WHERE ${fk.childColumn} = ${bind(id)}"
                                    )
                                }
                                .toList<UUID>()
                        childIds.forEach { delete(fk.childTable, it) }
                    } else {
                        val count =
                            tx.createUpdate {
                                    sql(
                                        "DELETE FROM ${fk.childTable} WHERE ${fk.childColumn} = ${bind(id)}"
                                    )
                                }
                                .executeAndReturnCount()
                        countDeleted(fk.childTable, count, untracked = false)
                    }
            }
        }
        val count =
            tx.createUpdate { sql("DELETE FROM $table WHERE id = ${bind(id)}") }
                .executeAndReturnCount()
        countDeleted(table, count, untracked = tracked == null)
    }

    private fun countDeleted(table: String, count: Int, untracked: Boolean) {
        if (count == 0) return
        deletedRowCounts.merge(table, count, Int::plus)
        if (untracked) untrackedRowCounts.merge(table, count, Int::plus)
    }
}

internal fun requireIdentifier(name: String) {
    if (!identifierRegex.matches(name)) error("Invalid SQL identifier: $name")
}

/**
 * The foreign keys that reference a table's `id` column, and the tables that have a uuid `id`
 * column, read from the PostgreSQL system catalogs
 */
class McpSchemaCatalog(private val tx: Database.Read) {
    enum class OnDelete {
        SET_NULL,
        SET_DEFAULT,
        DELETE_ROW,
    }

    data class ForeignKey(
        val childTable: String,
        val childColumn: String,
        val parentTable: String,
        val onDelete: OnDelete,
    )

    private data class ForeignKeyRow(
        val childTable: String,
        val childColumns: List<String>,
        val parentTable: String,
        val parentColumns: List<String>,
        val onDelete: String,
    )

    val tablesWithUuidId: Set<String> by lazy {
        tx.createQuery {
                sql(
                    """
SELECT c.table_name
FROM information_schema.columns c
JOIN information_schema.tables t ON t.table_schema = c.table_schema AND t.table_name = c.table_name
WHERE c.table_schema = 'public' AND c.column_name = 'id' AND c.data_type = 'uuid' AND t.table_type = 'BASE TABLE'
"""
                )
            }
            .toSet<String>()
    }

    val foreignKeysByParent: Map<String, List<ForeignKey>> by lazy {
        tx.createQuery {
                sql(
                    """
SELECT
    child.relname AS child_table,
    (SELECT array_agg(a.attname ORDER BY k.ord)
     FROM unnest(con.conkey) WITH ORDINALITY AS k(attnum, ord)
     JOIN pg_attribute a ON a.attrelid = con.conrelid AND a.attnum = k.attnum) AS child_columns,
    parent.relname AS parent_table,
    (SELECT array_agg(a.attname ORDER BY k.ord)
     FROM unnest(con.confkey) WITH ORDINALITY AS k(attnum, ord)
     JOIN pg_attribute a ON a.attrelid = con.confrelid AND a.attnum = k.attnum) AS parent_columns,
    con.confdeltype::text AS on_delete
FROM pg_constraint con
JOIN pg_class child ON child.oid = con.conrelid
JOIN pg_class parent ON parent.oid = con.confrelid
JOIN pg_namespace ns ON ns.oid = parent.relnamespace
WHERE con.contype = 'f' AND ns.nspname = 'public'
"""
                )
            }
            .toList<ForeignKeyRow>()
            .mapNotNull { row ->
                // Only follow the part of the key that references the parent's `id` column
                val idIndex = row.parentColumns.indexOf("id")
                if (idIndex < 0) null
                else
                    ForeignKey(
                        childTable = row.childTable,
                        childColumn = row.childColumns[idIndex],
                        parentTable = row.parentTable,
                        onDelete =
                            when (row.onDelete) {
                                "n" -> OnDelete.SET_NULL
                                "d" -> OnDelete.SET_DEFAULT
                                else -> OnDelete.DELETE_ROW
                            },
                    )
            }
            .groupBy { it.parentTable }
    }
}
