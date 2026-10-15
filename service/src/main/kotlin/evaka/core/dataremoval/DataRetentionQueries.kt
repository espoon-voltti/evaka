// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.koski.childSentToKoski
import evaka.core.koski.freezeKoskiSync
import evaka.core.shared.ChildId
import evaka.core.shared.DatabaseTable
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.db.Predicate
import evaka.core.shared.db.QuerySql
import evaka.core.shared.db.Row
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.varda.childSentToVarda
import evaka.core.varda.freezeVardaSync
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass

fun Database.Transaction.loadPersonGraph(
    schema: SchemaDefinition,
    personId: PersonId,
    today: LocalDate,
    optimized: Boolean,
): PersonGraph? {
    // Jobs working on people related to each other might interfere with each other.
    // This makes sure that different instances do not run this job in parallel
    execute { sql("SELECT pg_advisory_xact_lock(hashtext('data_retention'))") }

    // TODO: duplicate children are rejected until it is decided how to handle them
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
            .exactlyOneOrNull<Boolean>() ?: return null // Null if the person does not exist
    check(!hasDuplicates) {
        "Person $personId is a duplicate of another person or has duplicates, so it is refused"
    }

    val idsOfOwnRowsByTable = mutableMapOf<String, List<UUID>>()
    val preloadedNodes =
        readOwnNodes(
            schema,
            personId,
            schema.preloadedTables,
            tablesKnownToStay = emptySet(),
            idsByTable = idsOfOwnRowsByTable,
        )
    val integrationFacts = readIntegrationFacts(setOf(personId)).getValue(personId)

    val tablesKnownToStay =
        if (optimized) {
            schema.tablesKnownToStay(preloadedNodes, integrationFacts, today)
        } else {
            emptySet()
        }
    val skippedTables = schema.tablesToSkip(tablesKnownToStay)

    val ownNodes =
        preloadedNodes +
            readOwnNodes(
                schema,
                personId,
                schema.handledTables
                    .filter { it !in schema.preloadedTables && it.name !in skippedTables }
                    .toSet(),
                tablesKnownToStay,
                idsOfOwnRowsByTable,
            )
    val foreignNodes = readForeignNodes(schema, idsOfOwnRowsByTable, tablesKnownToStay)

    val otherChildIds = ownNodes.flatMapTo(mutableSetOf()) { it.referencedChildIds() } - personId
    val integrationFactsByPerson =
        mapOf(personId to integrationFacts) + readIntegrationFacts(otherChildIds)

    return PersonGraph(
        schema,
        personId,
        ownNodes,
        foreignNodes,
        integrationFactsByPerson,
        tablesKnownToStay,
    )
}

/**
 * Own nodes contain the rows that are owned by the target person and potentially deleted in this
 * run.
 *
 * The ids of the rows read are added to [idsByTable], where each table finds the ids of its parent
 * rows. A later call finds there the ids that an earlier call read.
 */
private fun Database.Read.readOwnNodes(
    schema: SchemaDefinition,
    personId: PersonId,
    tablesToRead: Set<HandledTable>,
    tablesKnownToStay: Set<String>,
    idsByTable: MutableMap<String, List<UUID>>,
): List<OwnNode> {
    val nodes = mutableListOf<OwnNode>()
    for (table in schema.rootFirstOrder.filter { it in tablesToRead }) {
        val knownToStay = table.name in tablesKnownToStay
        val rows =
            when (table) {
                schema.person,
                schema.child ->
                    readOwnRows(
                        schema,
                        table,
                        Predicate { where("$it.id = ${bind(personId)}") },
                        knownToStay,
                    )
                else -> {
                    val primaryReference = checkNotNull(schema.primaryReference(table))
                    val parentIds = idsByTable[primaryReference.referencedTable] ?: continue
                    readOwnRows(
                        schema,
                        table,
                        Predicate {
                            where(
                                "$it.${primaryReference.referenceColumn} = ANY(${bind(parentIds)})"
                            )
                        },
                        knownToStay,
                    )
                }
            }

        // The child row may be missing, but its node is kept so that blocking still passes through
        // it to person and to the nodes that must be deleted with it
        if (table == schema.child) idsByTable[table.name] = listOf(personId.raw)
        else if (rows.isEmpty()) continue
        else if (table.identifiedByCols.size == 1)
            idsByTable[table.name] = rows.map { it.id.single() }

        nodes +=
            if (table.independentRows)
                rows.map { OwnNode(OwnNodeId.SingleRow(table.name, it.id), table, listOf(it)) }
            else listOf(OwnNode(OwnNodeId.WholeTable(table.name), table, rows))
    }
    return nodes
}

/**
 * A foreign node contains the IDs of own rows where a specific secondary reference points. If a
 * table has multiple secondary references, each forms a separate foreign node. If some row points
 * to an own row through both the primary and a secondary reference, then it is considered an own
 * row and its secondary reference does not get included in a foreign node.
 */
private fun Database.Read.readForeignNodes(
    schema: SchemaDefinition,
    idsOfOwnRowsByTable: Map<String, List<UUID>>,
    tablesKnownToStay: Set<String>,
): List<ForeignNode> =
    schema.tablesByName.values.flatMap { table ->
        val notAnOwnRow = notAnOwnRowPredicate(schema, table, idsOfOwnRowsByTable)
        table.secondaryReferences.mapNotNull { secondaryReference ->
            // The foreign rows could only block rows that stay anyway
            if (secondaryReference.referencedTable in tablesKnownToStay) return@mapNotNull null
            val referencedIds =
                idsOfOwnRowsByTable[secondaryReference.referencedTable] ?: return@mapNotNull null
            val foreignRowsPredicate =
                Predicate.all(
                    Predicate {
                        where(
                            "$it.${secondaryReference.referenceColumn} = ANY(${bind(referencedIds)})"
                        )
                    },
                    notAnOwnRow,
                )
            val foreignReferencedIds = createQuery {
                sql(
                    """
SELECT DISTINCT ${secondaryReference.referenceColumn}
FROM ${table.name}
WHERE ${predicate(foreignRowsPredicate.forTable(table.name))}
"""
                )
            }
                .toSet<UUID>()
            if (foreignReferencedIds.isEmpty()) null
            else ForeignNode(table, secondaryReference.referenceColumn, foreignReferencedIds)
        }
    }

private fun notAnOwnRowPredicate(
    schema: SchemaDefinition,
    table: Table,
    idsOfOwnRowsByTable: Map<String, List<UUID>>,
): Predicate {
    // An external table has no primary references and does not contain own rows
    val primaryReference =
        (table as? HandledTable)?.let { schema.primaryReference(it) }
            ?: return Predicate.alwaysTrue()

    val parentIds = idsOfOwnRowsByTable[primaryReference.referencedTable]
    if (parentIds.isNullOrEmpty()) return Predicate.alwaysTrue()

    val column = primaryReference.referenceColumn
    return Predicate { where("$it.$column IS NULL OR $it.$column <> ALL(${bind(parentIds)})") }
}

/** The sent conditions are the ones the freezes and the sync refusals use */
private fun Database.Read.readIntegrationFacts(
    personIds: Set<PersonId>
): Map<PersonId, IntegrationFacts> {
    if (personIds.isEmpty()) return emptyMap()
    return createQuery {
        sql(
            """
SELECT
  p.id,
  p.date_of_birth,
  ${predicate(childSentToKoski.forTable("p"))} AS sent_to_koski,
  ${predicate(childSentToVarda.forTable("p"))} AS sent_to_varda
FROM person p
WHERE p.id = ANY(${bind(personIds)})
"""
        )
    }
        .toList {
            column<PersonId>("id") to
                IntegrationFacts(
                    dateOfBirth = column("date_of_birth"),
                    sentToKoski = column("sent_to_koski"),
                    sentToVarda = column("sent_to_varda"),
                )
        }
        .toMap()
}

private fun Database.Read.readOwnRows(
    schema: SchemaDefinition,
    table: HandledTable,
    where: Predicate,
    knownToStay: Boolean,
): List<OwnRow> {
    val dateColumnTypes = schema.dateColumnsToRead(table)
    val jobColumns = table.asyncJobsPlannedOnDelete?.columnsToRead.orEmpty()
    val orphanReferenceColumns = table.orphansToDelete.map { it.referenceColumn }
    val columnsToSelect =
        (table.identifiedByCols +
                table.blockingReferences.map { it.referenceColumn } +
                orphanReferenceColumns +
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
            OwnRow(
                id = rowIdentity(table.identifiedByCols),
                idByBlockingReferenceColumn =
                    table.blockingReferences.associate {
                        it.referenceColumn to column<UUID?>(it.referenceColumn)
                    },
                orphanIdByReferenceColumn =
                    orphanReferenceColumns.associateWith { column<UUID?>(it) },
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
                valueForJobByColumn = jobColumns.associateWith { column<String?>(it) },
                // Filled in below
                dateByCustomSource = emptyMap(),
                mayExpireByNotWhileRule = emptyMap(),
            )
        }

    val customSources = table.usedCustomDateSources()
    val notWhileRules = table.usedNotWhileRules()
    if (knownToStay || rows.isEmpty() || (customSources.isEmpty() && notWhileRules.isEmpty())) {
        return rows
    }

    // Tables that have customSources or notWhileRules are guaranteed by validation to have a
    // single identifying column
    val rowIds = rows.map { it.id.single() }
    val dateByRowIdBySource = customSources.associateWith { it.query(this, rowIds) }
    val idsWhereConditionHoldsByRule = notWhileRules.associateWith {
        it.idsWhereConditionHolds(this, rowIds)
    }

    return rows.map { row ->
        val id = row.id.single()
        row.copy(
            dateByCustomSource = dateByRowIdBySource.mapValues { it.value[id] },
            mayExpireByNotWhileRule = idsWhereConditionHoldsByRule.mapValues { id !in it.value },
        )
    }
}

data class DeletedRows(
    val auditIdType: KClass<out DatabaseTable>?,
    val ids: List<RowIdentity>,
) {
    operator fun plus(other: DeletedRows) = copy(ids = ids + other.ids)
}

data class DeletionResult(
    val deletedRowsByTable: Map<String, DeletedRows>,
    val clearedRowCountsByTable: Map<String, Int>,
    val childrenFrozenForKoski: List<ChildId>,
    val childrenFrozenForVarda: List<ChildId>,
) {
    val deletedRowCountsByTable: Map<String, Int>
        get() = deletedRowsByTable.mapValues { it.value.ids.size }
}

fun Database.Transaction.executeDeletionPlan(
    plan: DeletionPlan,
    now: HelsinkiDateTime,
): DeletionResult {
    val deletedRowsByTable = linkedMapOf<String, DeletedRows>()
    val clearedRowCountsByTable = linkedMapOf<String, Int>()
    for (deletion in plan.ownNodeDeletions) {
        val table = deletion.node.table

        // Set optional references to null first
        for (clearing in deletion.referenceClearings) {
            val setNullClauses =
                (listOf(clearing.column) + clearing.alsoNull).joinToString { "$it = NULL" }
            val count = executeAndReturnCount {
                sql(
                    "UPDATE ${clearing.table} SET $setNullClauses WHERE ${clearing.column} = ANY(${bind(clearing.referencedIds)})"
                )
            }
            if (count > 0) clearedRowCountsByTable.merge(clearing.table, count, Int::plus)
        }

        // Delete the rows in the table
        val deletedRows = createUpdate {
            sql(
                "DELETE FROM ${table.name} WHERE ${idPredicate(table.identifiedByCols, deletion.node.rows.map { it.id })} RETURNING ${table.identifiedByCols.joinToString()}"
            )
        }
            .executeAndReturnGeneratedKeys()
            .toList { rowIdentity(table.identifiedByCols) }
        deletedRowsByTable.merge(
            table.name,
            DeletedRows(table.auditIdType, deletedRows),
            DeletedRows::plus,
        )

        // Delete orphan rows
        for (orphan in deletion.orphanDeletions) {
            val deletedOrphans = createUpdate {
                sql(
                    "DELETE FROM ${orphan.table} WHERE ${orphan.idColumn} = ANY(${bind(orphan.ids)}) RETURNING ${orphan.idColumn}"
                )
            }
                .executeAndReturnGeneratedKeys()
                .toList { rowIdentity(listOf(orphan.idColumn)) }
            deletedRowsByTable.merge(
                orphan.table,
                DeletedRows(orphan.auditIdType, deletedOrphans),
                DeletedRows::plus,
            )
        }
    }

    // Freeze children for Koski and Varda if needed
    val childrenFrozenForKoski =
        if (plan.childrenToFreezeForKoski.isEmpty()) emptyList()
        else freezeKoskiSync(plan.childrenToFreezeForKoski, now)
    val childrenFrozenForVarda =
        if (plan.childrenToFreezeForVarda.isEmpty()) emptyList()
        else freezeVardaSync(plan.childrenToFreezeForVarda, now)

    return DeletionResult(
        deletedRowsByTable = deletedRowsByTable,
        clearedRowCountsByTable = clearedRowCountsByTable,
        childrenFrozenForKoski = childrenFrozenForKoski,
        childrenFrozenForVarda = childrenFrozenForVarda,
    )
}

private fun Row.rowIdentity(identifiedByCols: List<String>): RowIdentity =
    RowIdentity(identifiedByCols.associateWith { column<UUID>(it) })

private fun QuerySql.Builder.idPredicate(columns: List<String>, ids: List<RowIdentity>): String {
    if (columns.size == 1) return "${columns.single()} = ANY(${bind(ids.map { it.single() })})"

    // For multiple columns, we use a subquery with unnest to match the composite keys
    return """
        (${columns.joinToString()}) IN (
            SELECT * FROM unnest(${columns.joinToString { column ->
                bind(ids.map { it.idByColumn.getValue(column) }).toString() }
            })
        )
    """
}
