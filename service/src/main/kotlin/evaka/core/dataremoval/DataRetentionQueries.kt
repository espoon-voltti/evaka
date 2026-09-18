// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.koski.childSentToKoski
import evaka.core.koski.freezeKoskiSync
import evaka.core.shared.ChildId
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.db.Predicate
import evaka.core.shared.db.QuerySql
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.varda.childSentToVarda
import evaka.core.varda.freezeVardaSync
import java.time.LocalDate
import java.util.UUID

/** An arbitrary key that no other advisory lock uses */
private const val DATA_RETENTION_LOCK: Long = 5_318_008

/**
 * Loads the person graph root-first along the primary references, and then the foreign rows, or
 * returns null if the person no longer exists. A person that is a duplicate of another person, or
 * has duplicates, is refused before anything is loaded.
 */
fun Database.Transaction.loadPersonGraph(
    schema: SchemaDefinition,
    personId: PersonId,
): PersonGraph? {
    // Clearing optional references may touch rows of other persons, and foreign nodes are read
    // from them, so runs are serialised across every service instance for the transaction
    execute { sql("SELECT pg_advisory_xact_lock(${bind(DATA_RETENTION_LOCK)})") }
    val hasDuplicates =
        createQuery {
            sql(
                """
SELECT p.duplicate_of IS NOT NULL OR EXISTS (SELECT FROM person d WHERE d.duplicate_of = p.id)
FROM person p
WHERE p.id = ${bind(personId)}
"""
            )
        }
            .exactlyOneOrNull<Boolean>() ?: return null
    check(!hasDuplicates) {
        "Person $personId is a duplicate of another person or has duplicates, so it is refused"
    }

    /** The ids of the loaded rows, for the tables whose rows can be referenced */
    val loadedIdsByTable = mutableMapOf<String, List<UUID>>()
    val ownNodes = mutableListOf<OwnNode>()
    for (table in schema.rootFirstOrder) {
        val rows =
            when (table) {
                schema.person,
                schema.child ->
                    readRows(schema, table, Predicate { where("$it.id = ${bind(personId)}") })
                else -> {
                    val primary = checkNotNull(schema.primaryReference(table))
                    val parentIds = loadedIdsByTable[primary.toTable] ?: continue
                    readRows(
                        schema,
                        table,
                        Predicate { where("$it.${primary.column} = ANY(${bind(parentIds)})") },
                    )
                }
            }
        if (table == schema.child) loadedIdsByTable[table.name] = listOf(personId.raw)
        else if (rows.isEmpty()) continue
        else if (table.identifiedByCols.size == 1)
            loadedIdsByTable[table.name] = rows.map { it.id.single() }
        ownNodes +=
            if (table.independentRows)
                rows.map { OwnNode(OwnNodeId.SingleRow(table.name, it.id), table, listOf(it)) }
            else listOf(OwnNode(OwnNodeId.WholeTable(table.name), table, rows))
    }

    val foreignNodes =
        schema.tablesByName.values.flatMap { table ->
            table.secondaryReferences.mapNotNull { reference ->
                val referencedIds = loadedIdsByTable[reference.toTable] ?: return@mapNotNull null
                // A row is an own row exactly when its primary reference holds a loaded id
                val primary = (table as? HandledTable)?.let { schema.primaryReference(it) }
                val parentIds = primary?.let { loadedIdsByTable[it.toTable] }
                val foreignReferencedIds = createQuery {
                    sql(
                        """
SELECT DISTINCT ${reference.column}
FROM ${table.name}
WHERE ${reference.column} = ANY(${bind(referencedIds)})
${if (primary != null && parentIds != null) "AND NOT coalesce(${primary.column} = ANY(${bind(parentIds)}), false)" else ""}
"""
                    )
                }
                    .toSet<UUID>()
                if (foreignReferencedIds.isEmpty()) null
                else ForeignNode(table, reference.column, foreignReferencedIds)
            }
        }

    val childIds = buildSet {
        add(ChildId(personId.raw))
        for (node in ownNodes) {
            val childReferenceColumns =
                node.table.blockingReferences.filter { it.toTable == CHILD_TABLE }.map { it.column }
            for (row in node.rows) {
                for (column in childReferenceColumns) {
                    row.referencedIdByColumn[column]?.let { add(ChildId(it)) }
                }
            }
        }
    }
    return PersonGraph(
        schema,
        personId,
        ownNodes,
        foreignNodes,
        readChildIntegrationFacts(childIds),
    )
}

/** The sent conditions are the ones the freezes and the sync refusals use */
private fun Database.Read.readChildIntegrationFacts(
    childIds: Set<ChildId>
): Map<ChildId, ChildIntegrationFacts> = createQuery {
    sql(
        """
SELECT
  p.id,
  p.date_of_birth,
  ${predicate(childSentToKoski.forTable("p"))} AS sent_to_koski,
  ${predicate(childSentToVarda.forTable("p"))} AS sent_to_varda
FROM person p
WHERE p.id = ANY(${bind(childIds)})
"""
    )
}
    .toList {
        column<ChildId>("id") to
            ChildIntegrationFacts(
                dateOfBirth = column("date_of_birth"),
                sentToKoski = column("sent_to_koski"),
                sentToVarda = column("sent_to_varda"),
            )
    }
    .toMap()

/**
 * Reads the ids, the ids referenced inside and outside the schema definition, the date columns the
 * rules read and the columns needed for the async jobs, and then runs the custom date sources and
 * archived rules of the table for the rows
 */
private fun Database.Read.readRows(
    schema: SchemaDefinition,
    table: HandledTable,
    where: Predicate,
): List<LoadedRow> {
    val dateColumnTypes = schema.dateColumnsToRead(table)
    val jobColumns = table.asyncJobsPlannedOnDelete?.columnsToRead.orEmpty()
    val outsideGraphReferenceColumns = table.orphansToDelete.map { it.referenceColumn }
    val columnsToSelect =
        (table.identifiedByCols +
                table.blockingReferences.map { it.column } +
                outsideGraphReferenceColumns +
                dateColumnTypes.keys +
                jobColumns)
            .distinct()
    val rows = createQuery {
        sql(
            """
SELECT ${columnsToSelect.joinToString()}
FROM ${table.name}
WHERE ${predicate(where.forTable(table.name))}
"""
        )
    }
        .toList {
            LoadedRow(
                id = RowId(table.identifiedByCols.associateWith { column<UUID>(it) }),
                referencedIdByColumn =
                    table.blockingReferences.associate { it.column to column<UUID?>(it.column) },
                outsideGraphReferencedIdByColumn =
                    outsideGraphReferenceColumns.associateWith { column<UUID?>(it) },
                dateByColumn =
                    dateColumnTypes.mapValues { (name, type) ->
                        when (type) {
                            DateColumnType.DATE -> column<LocalDate?>(name)
                            DateColumnType.TIMESTAMP_WITH_TIME_ZONE ->
                                column<HelsinkiDateTime?>(name)?.toLocalDate()
                            DateColumnType.DATE_RANGE_END -> column<DateRange?>(name)?.end
                            DateColumnType.FINITE_DATE_RANGE_END ->
                                column<FiniteDateRange?>(name)?.end
                        }
                    },
                dateByCustomSource = emptyMap(),
                mayExpireByArchivedRule = emptyMap(),
                valueForJobByColumn = jobColumns.associateWith { column<String?>(it) },
            )
        }
    val customSources = table.expirationRule.customDateSources()
    val archivedRules = table.expirationRule.archivedIfRequiredRules()
    if (rows.isEmpty() || (customSources.isEmpty() && archivedRules.isEmpty())) return rows
    val rowIds = rows.map { it.id.single() }
    val dateByRowIdBySource = customSources.associateWith { it.query(this, rowIds) }
    val idsAwaitingArchivalByRule = archivedRules.associateWith {
        it.idsAwaitingArchival(this, rowIds)
    }
    return rows.map { row ->
        val id = row.id.single()
        row.copy(
            dateByCustomSource = dateByRowIdBySource.mapValues { it.value[id] },
            mayExpireByArchivedRule = idsAwaitingArchivalByRule.mapValues { id !in it.value },
        )
    }
}

data class DeletionResult(
    /** By table, in deletion order */
    val deletedRowCountsByTable: Map<String, Int>,
    /** By table and column, only where rows were cleared */
    val clearedRowCountsByColumn: Map<Pair<String, String>, Int>,
    val childrenFrozenForKoski: List<ChildId>,
    val childrenFrozenForVarda: List<ChildId>,
)

/**
 * Deletes the nodes leaf-first, each by the ids of its rows, clearing the optional references into
 * it first and deleting the orphans its rows referenced after it. The integration freezes come
 * last: when the plan deletes the child row too, there is nothing left to freeze.
 */
fun Database.Transaction.executeDeletionPlan(
    plan: DeletionPlan,
    now: HelsinkiDateTime,
): DeletionResult {
    val deletedRowCountsByTable = linkedMapOf<String, Int>()
    val clearedRowCountsByColumn = linkedMapOf<Pair<String, String>, Int>()
    for (deletion in plan.ownNodeDeletions) {
        for (clearing in deletion.referenceClearings) {
            val setNullClauses =
                (listOf(clearing.column) + clearing.alsoNull).joinToString { "$it = NULL" }
            val count = executeAndReturnCount {
                sql(
                    "UPDATE ${clearing.table} SET $setNullClauses WHERE ${clearing.column} = ANY(${bind(clearing.referencedIds)})"
                )
            }
            if (count > 0) {
                clearedRowCountsByColumn.merge(clearing.table to clearing.column, count, Int::plus)
            }
        }
        val table = deletion.node.table
        val count = executeAndReturnCount {
            sql(
                "DELETE FROM ${table.name} WHERE ${idPredicate(table.identifiedByCols, deletion.node.rows.map { it.id })}"
            )
        }
        deletedRowCountsByTable.merge(table.name, count, Int::plus)
        for (orphan in deletion.orphanDeletions) {
            val orphanCount = executeAndReturnCount {
                sql(
                    "DELETE FROM ${orphan.table} WHERE ${orphan.idColumn} = ANY(${bind(orphan.ids)})"
                )
            }
            deletedRowCountsByTable.merge(orphan.table, orphanCount, Int::plus)
        }
    }
    return DeletionResult(
        deletedRowCountsByTable = deletedRowCountsByTable,
        clearedRowCountsByColumn = clearedRowCountsByColumn,
        childrenFrozenForKoski =
            if (plan.childrenToFreezeForKoski.isEmpty()) emptyList()
            else freezeKoskiSync(plan.childrenToFreezeForKoski, now),
        childrenFrozenForVarda =
            if (plan.childrenToFreezeForVarda.isEmpty()) emptyList()
            else freezeVardaSync(plan.childrenToFreezeForVarda, now),
    )
}

/** An id of several columns is matched against the rows of the id column arrays, one per column */
private fun QuerySql.Builder.idPredicate(columns: List<String>, ids: List<RowId>): String =
    if (columns.size == 1) "${columns.single()} = ANY(${bind(ids.map { it.single() })})"
    else
        "(${columns.joinToString()}) IN (SELECT * FROM unnest(${columns.joinToString { column -> bind(ids.map { it.valueByColumn.getValue(column) }).toString() }}))"
