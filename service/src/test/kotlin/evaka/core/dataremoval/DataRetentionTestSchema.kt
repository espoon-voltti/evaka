// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.dataremoval.ExpirationRule.Always
import evaka.core.shared.ChildId
import evaka.core.shared.PersonId
import java.time.LocalDate
import java.util.UUID

internal val personTable =
    HandledTable("person", Handler.ADULT, references = emptyList(), expirationRule = Always)

internal fun childTable(expirationRule: ExpirationRule = Always) =
    HandledTable(
        "child",
        Handler.CHILD,
        references = listOf(primaryReference("id", toTable = "person")),
        expirationRule = expirationRule,
    )

/** A schema definition of the given tables together with person and child */
internal fun schema(
    vararg tables: Table,
    person: HandledTable = personTable,
    child: HandledTable = childTable(),
): SchemaDefinition = SchemaDefinition(listOf(person, child) + tables)

/** A handled table whose blocking references are given as column to table pairs */
internal fun table(
    name: String,
    handledBy: Handler = Handler.CHILD,
    primary: Pair<String, String>? = null,
    vararg secondary: Pair<String, String>,
    optional: List<Reference> = emptyList(),
    expirationRule: ExpirationRule = Always,
    bundledBy: String? = null,
    independentRows: Boolean = false,
    identifiedByCols: List<String> = listOf("id"),
    asyncJobsPlannedOnDelete: AsyncJobsOnDelete? = null,
    orphansToDelete: List<OutsideGraphReference> = emptyList(),
) =
    HandledTable(
        name = name,
        handledBy = handledBy,
        references =
            listOfNotNull(primary?.let { primaryReference(it.first, toTable = it.second) }) +
                secondary.map { secondaryReference(it.first, toTable = it.second) } +
                optional,
        bundledBy = bundledBy,
        independentRows = independentRows,
        identifiedByCols = identifiedByCols,
        expirationRule = expirationRule,
        asyncJobsPlannedOnDelete = asyncJobsPlannedOnDelete,
        orphansToDelete = orphansToDelete,
    )

internal fun externalTable(
    name: String,
    vararg secondary: Pair<String, String>,
    optional: List<Reference> = emptyList(),
) =
    ExternalTable(
        name,
        secondary.map { secondaryReference(it.first, toTable = it.second) } + optional,
    )

internal val today: LocalDate = LocalDate.of(2026, 9, 12)

internal val targetId = PersonId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
internal val targetChildId = ChildId(targetId.raw)

/** A child under the safe data removal age that no integration has seen */
internal val neverSent =
    ChildIntegrationFacts(today.minusYears(5), sentToKoski = false, sentToVarda = false)

/** The unit tests give the dates and results of custom sources and archived rules by hand */
internal fun customSource(mayHaveNoDate: Boolean = true) =
    DateSource.Custom(mayHaveNoDate) { _, _ -> error("The query of a test source is never run") }

internal fun archivedIfRequiredRule() = ExpirationRule.ArchivedIfRequired { _, _ ->
    error("The query of a test rule is never run")
}

internal fun row(
    vararg references: Pair<String, UUID?>,
    id: UUID = UUID.randomUUID(),
    outsideGraphReferences: Map<String, UUID?> = emptyMap(),
    dateByColumn: Map<String, LocalDate?> = emptyMap(),
    dateByCustomSource: Map<DateSource.Custom, LocalDate?> = emptyMap(),
    mayExpireByArchivedRule: Map<ExpirationRule.ArchivedIfRequired, Boolean> = emptyMap(),
    valueForJobByColumn: Map<String, String?> = emptyMap(),
) =
    LoadedRow(
        id = RowId(mapOf("id" to id)),
        referencedIdByColumn = references.toMap(),
        outsideGraphReferencedIdByColumn = outsideGraphReferences,
        dateByColumn = dateByColumn,
        dateByCustomSource = dateByCustomSource,
        mayExpireByArchivedRule = mayExpireByArchivedRule,
        valueForJobByColumn = valueForJobByColumn,
    )

internal fun SchemaDefinition.handled(table: String): HandledTable =
    tablesByName.getValue(table) as HandledTable

/** All the own rows of the table as one node */
internal fun SchemaDefinition.ownNode(table: String, vararg rows: LoadedRow) =
    OwnNode(OwnNodeId.WholeTable(table), handled(table), rows.toList()).also {
        it.requireReferences()
    }

/** One row of a table with independent rows */
internal fun SchemaDefinition.ownRowNode(table: String, row: LoadedRow) =
    OwnNode(OwnNodeId.SingleRow(table, row.id), handled(table), listOf(row)).also {
        it.requireReferences()
    }

/**
 * The loader always reads every reference column, and a row without one would make no edge and
 * delete no orphan, so a hand-written row must give them all
 */
private fun OwnNode.requireReferences() {
    for (row in rows) {
        val expected =
            table.blockingReferences.map { it.column } +
                table.orphansToDelete.map { it.referenceColumn }
        val missing =
            expected - row.referencedIdByColumn.keys - row.outsideGraphReferencedIdByColumn.keys
        require(missing.isEmpty()) { "$id: the row does not give the references $missing" }
    }
}

/**
 * The loader forms foreign nodes only through secondary references, so a hand-written one must too
 */
internal fun SchemaDefinition.foreign(
    table: String,
    column: String,
    vararg ids: UUID,
): ForeignNode {
    val declared = tablesByName.getValue(table)
    require(declared.secondaryReferences.any { it.column == column }) {
        "$table.$column is not a secondary reference"
    }
    return ForeignNode(declared, column, ids.toSet())
}

/**
 * A graph of the target person's person and child rows and the given nodes. Without the child row
 * the child node is empty, as for a person who has never had one. The target is a child no
 * integration has seen unless the facts say otherwise.
 */
internal fun SchemaDefinition.graph(
    vararg ownNodes: OwnNode,
    childRow: Boolean = true,
    foreignNodes: List<ForeignNode> = emptyList(),
    integrationFactsByChild: Map<ChildId, ChildIntegrationFacts> =
        mapOf(targetChildId to neverSent),
): PersonGraph {
    val id = targetId.raw
    val child = if (childRow) ownNode("child", row("id" to id, id = id)) else ownNode("child")
    return PersonGraph(
        this,
        targetId,
        listOf(ownNode("person", row(id = id)), child) + ownNodes,
        foreignNodes,
        integrationFactsByChild,
    )
}

internal fun DeletionPlan.deleted(): List<OwnNodeId> = ownNodeDeletions.map { it.node.id }
