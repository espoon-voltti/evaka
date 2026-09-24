// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.dataremoval.ExpirationRule.After
import evaka.core.dataremoval.ExpirationRule.AllOf
import evaka.core.dataremoval.ExpirationRule.Always
import evaka.core.dataremoval.ExpirationRule.AnyOf
import evaka.core.dataremoval.ExpirationRule.ArchivedIfRequired
import evaka.core.dataremoval.ExpirationRule.Coalesce
import evaka.core.dataremoval.ExpirationRule.Never
import evaka.core.dataremoval.ExpirationRule.SafeForIntegrations
import evaka.core.koski.KOSKI_INPUT_TABLES
import evaka.core.shared.ChildId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.varda.VARDA_INPUT_TABLES
import java.time.LocalDate
import java.util.UUID

/** Unique identifier for an own node in a person's graph */
sealed interface OwnNodeId {
    val table: String

    /** All the own rows of a table, evaluated and deleted as a whole */
    data class WholeTable(override val table: String) : OwnNodeId {
        override fun toString(): String = table
    }

    /** One row of a table with independent rows */
    data class SingleRow(override val table: String, val rowIdentity: RowIdentity) : OwnNodeId {
        override fun toString(): String = "$table$rowIdentity"
    }
}

/**
 * A row whose primary reference path leads to the target person and may be deleted in the current
 * run
 */
data class OwnRow(
    val id: RowIdentity,
    /** The ids the row references within the schema definition, by reference column */
    val referencedIdByColumn: Map<String, UUID?>,
    /** The ids the row references outside the schema definition, by reference column */
    val outsideGraphReferencedIdByColumn: Map<String, UUID?>,
    val dateByColumn: Map<String, LocalDate?>,
    val dateByCustomSource: Map<DateSource.Custom, LocalDate?>,
    /** Whether the row may expire, that is be deleted, under each archived rule of its table */
    val mayExpireByArchivedRule: Map<ArchivedIfRequired, Boolean>,
    /** The values needed for async jobs planned on delete, by column */
    val valueForJobByColumn: Map<String, String?>,
)

/** All the own rows of a table, or one row of a table with independent rows */
class OwnNode(val id: OwnNodeId, val table: HandledTable, val rows: List<OwnRow>) {
    override fun toString(): String = id.toString()
}

/**
 * A foreign row is a row not owned by the target person (i.e. its primary reference does not lead
 * to the target person or it belongs to an externally handled table), but which has a secondary
 * reference pointing to one of the own rows and blocking its deletion. Conceptually, a foreign node
 * holds these foreign rows, grouped by the secondary reference column. The rows themselves or their
 * IDs are not needed, however, so we only fetch the IDs of the own rows that they reference, i.e.
 * only the edges.
 */
data class ForeignNode(
    val table: Table,
    val referenceColumn: String,
    val referencedIds: Set<UUID>,
) {
    val referencedTable: String
        get() =
            table.blockingReferences.first { it.referenceColumn == referenceColumn }.referencedTable
}

data class ChildIntegrationFacts(
    val dateOfBirth: LocalDate,
    val sentToKoski: Boolean,
    val sentToVarda: Boolean,
) {
    fun sentTo(integration: Integration): Boolean =
        when (integration) {
            Integration.KOSKI -> sentToKoski
            Integration.VARDA -> sentToVarda
        }

    fun safeToDeleteFor(integration: Integration, today: LocalDate): Boolean =
        !sentTo(integration) || dateOfBirth.plusYears(SAFE_DATA_REMOVAL_AGE) < today
}

data class SingleIdRowRef(val table: String, val id: UUID)

/**
 * One person's data as the loader found it. The `person` and `child` nodes are part of every graph,
 * the child node with a row or without. None of the other rows are empty.
 */
class PersonGraph(
    val schema: SchemaDefinition,
    val targetPersonId: PersonId,
    val ownNodes: List<OwnNode>,
    val foreignNodes: List<ForeignNode>,
    /** For the target and every child the own rows reference */
    val integrationFactsByChild: Map<ChildId, ChildIntegrationFacts>,
) {
    private val ownNodesById: Map<OwnNodeId, OwnNode> = ownNodes.associateBy { it.id }

    /**
     * Map from row ID to the node that contains it. Only includes rows that can be identified by a
     * single ID.
     */
    private val ownNodeIdByRow: Map<SingleIdRowRef, OwnNodeId>

    /** The edges: from an own node to the own nodes holding the rows its rows reference */
    internal val edgesByOwnNode: Map<OwnNodeId, List<OwnNodeId>>

    /** From an own node to the own nodes it bundles */
    internal val ownNodeIdsByBundler: Map<OwnNodeId, List<OwnNodeId>>

    init {
        require(ownNodesById.size == ownNodes.size) { "Duplicate own nodes" }
        require(OwnNodeId.WholeTable(PERSON_TABLE) in ownNodesById) {
            "The $PERSON_TABLE node is part of every graph"
        }
        require(OwnNodeId.WholeTable(CHILD_TABLE) in ownNodesById) {
            "The $CHILD_TABLE node is part of every graph"
        }
        ownNodeIdByRow = buildMap {
            put(SingleIdRowRef(CHILD_TABLE, targetPersonId.raw), OwnNodeId.WholeTable(CHILD_TABLE))
            for (node in ownNodes) {
                if (node.table.identifiedByCols.size != 1) continue
                for (row in node.rows) {
                    put(
                        SingleIdRowRef(node.table.name, row.id.single()),
                        node.id,
                    )
                }
            }
        }
        edgesByOwnNode = ownNodes.associate { node ->
            node.id to
                if (node.table == schema.child) {
                    listOf(OwnNodeId.WholeTable(PERSON_TABLE))
                } else {
                    node.rows
                        .flatMap { row ->
                            node.table.blockingReferences.mapNotNull { reference ->
                                row.referencedIdByColumn[reference.referenceColumn]?.let {
                                    ownNodeIdByRow[SingleIdRowRef(reference.referencedTable, it)]
                                }
                            }
                        }
                        .distinct()
                        .filter { it != node.id }
                }
        }
        val bundledOwnNodeIdsByBundler = mutableMapOf<OwnNodeId, MutableList<OwnNodeId>>()
        for (node in ownNodes) {
            val bundler = schema.bundlerOf(node.table) ?: continue
            var current = node
            while (current.table != bundler) {
                current =
                    checkNotNull(primaryParent(current)) {
                        "$node: the bundling table $bundler is not on its loaded primary path"
                    }
            }
            bundledOwnNodeIdsByBundler.getOrPut(current.id) { mutableListOf() }.add(node.id)
        }
        this.ownNodeIdsByBundler = bundledOwnNodeIdsByBundler
    }

    fun ownNode(id: OwnNodeId): OwnNode = ownNodesById.getValue(id)

    /** The own nodes holding the rows the foreign node's rows reference */
    fun ownNodesReferencedBy(foreignNode: ForeignNode): List<OwnNodeId> =
        foreignNode.referencedIds
            .mapNotNull { ownNodeIdByRow[SingleIdRowRef(foreignNode.referencedTable, it)] }
            .distinct()

    /** The parent node where this node's primary reference points to */
    private fun primaryParent(node: OwnNode): OwnNode? {
        val primaryReference = schema.primaryReference(node.table) ?: return null
        val parentTable =
            schema.tablesByName.getValue(primaryReference.referencedTable) as HandledTable
        val parentNodeId =
            if (node.table.independentRows) {
                node.rows.single().referencedIdByColumn[primaryReference.referenceColumn]?.let {
                    rowId ->
                    ownNodeIdByRow[SingleIdRowRef(primaryReference.referencedTable, rowId)]
                }
            } else OwnNodeId.WholeTable(parentTable.name).takeIf { !parentTable.independentRows }
        return parentNodeId?.let { ownNodesById[it] }
    }
}

/** A column that needs to be set null to clear an optional reference. */
data class ReferenceClearing(
    val table: String,
    val column: String,
    /** Additional columns that should also be set to null */
    val alsoNull: List<String>,
    /** The ids of the deleted rows, which the column is matched against */
    val referencedIds: List<UUID>,
)

/**
 * Orphan rows of a table outside the schema definition that the deleted rows referenced and that
 * should also be deleted.
 */
data class OrphanDeletion(val table: String, val idColumn: String, val ids: List<UUID>)

class OwnNodeDeletion(
    val node: OwnNode,
    val referenceClearings: List<ReferenceClearing>,
    val orphanDeletions: List<OrphanDeletion>,
)

class DeletionPlan(
    /** Leaf-first */
    val ownNodeDeletions: List<OwnNodeDeletion>,
    val childrenToFreezeForKoski: Set<ChildId>,
    val childrenToFreezeForVarda: Set<ChildId>,
    val asyncJobs: List<AsyncJob>,
) {
    /** Rows to delete by table, in deletion order */
    fun rowCountsByTable(): Map<String, Int> {
        val counts = linkedMapOf<String, Int>()
        for (deletion in ownNodeDeletions) {
            counts.merge(deletion.node.table.name, deletion.node.rows.size, Int::plus)
            for (orphan in deletion.orphanDeletions) {
                counts.merge(orphan.table, orphan.ids.size, Int::plus)
            }
        }
        return counts
    }

    fun describe(): String {
        val cleared =
            ownNodeDeletions
                .flatMap { deletion ->
                    deletion.referenceClearings.map { "${it.table}.${it.column}" }
                }
                .distinct()
        return "delete ${describeRowCounts(rowCountsByTable())}, clear ${cleared.joinToString().ifEmpty { "nothing" }}, freeze ${describeFreezes(childrenToFreezeForKoski, childrenToFreezeForVarda)}, queue ${asyncJobs.size} jobs"
    }
}

fun describeRowCounts(counts: Map<String, Int>): String =
    if (counts.isEmpty()) "nothing"
    else counts.entries.joinToString { "${it.key} (${it.value} rows)" }

fun describeFreezes(koski: Collection<ChildId>, varda: Collection<ChildId>): String =
    listOfNotNull(
            "Koski for $koski".takeIf { koski.isNotEmpty() },
            "Varda for $varda".takeIf { varda.isNotEmpty() },
        )
        .joinToString()
        .ifEmpty { "nothing" }

/**
 * Decides what to delete. First, every node whose own rule is met is marked expired. Then a node
 * that is not expired, or a foreign node, blocks the nodes it references and the nodes it bundles,
 * transitively. What stays expired can be deleted leaf-first.
 */
fun PersonGraph.evaluate(today: LocalDate): DeletionPlan {
    val expiredOwnNodeIds = ownNodes.filter { isExpired(it, today) }.mapTo(mutableSetOf()) { it.id }

    fun block(id: OwnNodeId) {
        for (other in edgesByOwnNode.getValue(id) + ownNodeIdsByBundler[id].orEmpty()) {
            if (expiredOwnNodeIds.remove(other)) block(other)
        }
    }
    ownNodes.filter { it.id !in expiredOwnNodeIds }.forEach { block(it.id) }
    foreignNodes.forEach { foreign ->
        ownNodesReferencedBy(foreign).forEach { if (expiredOwnNodeIds.remove(it)) block(it) }
    }

    val ownNodesToDelete =
        leafFirstOrder().filter { it.id in expiredOwnNodeIds && it.rows.isNotEmpty() }
    val childDeleted = ownNodesToDelete.any { it.table == schema.child }

    fun childrenToFreeze(integration: Integration): Set<ChildId> =
        ownNodesToDelete
            .filter { it.table.name in integration.inputTables() }
            .flatMap { node ->
                childrenConcernedBy(node).filter { childId ->
                    // Children never yet sent in integration do not need to be frozen.
                    integrationFacts(childId, node).sentTo(integration)
                }
            }
            // If the child row is deleted, freeze cannot be persisted to it
            .filterNot { childId -> childId == targetPersonId && childDeleted }
            .toSet()

    return DeletionPlan(
        ownNodeDeletions =
            ownNodesToDelete.map { node ->
                OwnNodeDeletion(
                    node,
                    referenceClearings =
                        schema.optionalReferencesInto(node.table).map { declared ->
                            ReferenceClearing(
                                declared.referencingTable,
                                declared.reference.referenceColumn,
                                declared.reference.alsoNull,
                                node.rows.map { it.id.single() },
                            )
                        },
                    orphanDeletions =
                        node.table.orphansToDelete.mapNotNull { outside ->
                            val ids =
                                node.rows.mapNotNull {
                                    it.outsideGraphReferencedIdByColumn[outside.referenceColumn]
                                }
                            if (ids.isEmpty()) null
                            else
                                OrphanDeletion(
                                    outside.referencedTable,
                                    outside.referencedColumn,
                                    ids,
                                )
                        },
                )
            },
        childrenToFreezeForKoski = childrenToFreeze(Integration.KOSKI),
        childrenToFreezeForVarda = childrenToFreeze(Integration.VARDA),
        asyncJobs =
            ownNodesToDelete.flatMap { node ->
                val jobs = node.table.asyncJobsPlannedOnDelete ?: return@flatMap emptyList()
                node.rows.flatMap { jobs.jobsToPlan(DeletedRow(it.id, it.valueForJobByColumn)) }
            },
    )
}

private fun Integration.inputTables(): Set<String> =
    when (this) {
        Integration.KOSKI -> KOSKI_INPUT_TABLES
        Integration.VARDA -> VARDA_INPUT_TABLES
    }

/** Every node comes before the nodes it references, so its rows are deleted first */
private fun PersonGraph.leafFirstOrder(): List<OwnNode> {
    val incoming = ownNodes.associateTo(mutableMapOf()) { it.id to 0 }
    edgesByOwnNode.values.flatten().forEach { incoming[it] = incoming.getValue(it) + 1 }
    val queue = ArrayDeque(ownNodes.filter { incoming.getValue(it.id) == 0 })
    val result = mutableListOf<OwnNode>()
    while (queue.isNotEmpty()) {
        val next = queue.removeFirst()
        result += next
        for (referenced in edgesByOwnNode.getValue(next.id)) {
            val left = incoming.getValue(referenced) - 1
            incoming[referenced] = left
            if (left == 0) queue.addLast(ownNode(referenced))
        }
    }
    check(result.size == ownNodes.size) { "The person graph has a cycle" }
    return result
}

private fun PersonGraph.isExpired(node: OwnNode, today: LocalDate): Boolean {
    // A missing child row counts as expired, so that it blocks nothing on its own
    if (node.table == schema.child && node.rows.isEmpty()) return true
    return requireResult(node.table.expirationRule, node, today)
}

private fun PersonGraph.requireResult(
    rule: ExpirationRule,
    node: OwnNode,
    today: LocalDate,
): Boolean =
    checkNotNull(evaluateRule(rule, node, today)) {
        "$node: rule $rule returned no result, although validation guarantees one"
    }

/** Null means no result: an [After] whose date is missing */
private fun PersonGraph.evaluateRule(
    rule: ExpirationRule,
    node: OwnNode,
    today: LocalDate,
): Boolean? =
    when (rule) {
        Always -> true
        Never -> false
        is AllOf -> rule.rules.all { requireResult(it, node, today) }
        is AnyOf -> rule.rules.any { requireResult(it, node, today) }
        is Coalesce -> rule.rules.firstNotNullOfOrNull { evaluateRule(it, node, today) }
        is After -> dateOf(rule.dateSource, node)?.let { it.plus(rule.period) < today }
        is ArchivedIfRequired -> node.rows.all { it.mayExpireUnder(rule, node) }
        is SafeForIntegrations ->
            evaluateRule(rule.rule, node, today)?.let { expired ->
                expired && isSafeForIntegrations(rule.integrations, node, today)
            }
    }

private fun PersonGraph.isSafeForIntegrations(
    integrations: Set<Integration>,
    node: OwnNode,
    today: LocalDate,
): Boolean {
    val children = childrenConcernedBy(node)
    return integrations.all { integration ->
        children.all { integrationFacts(it, node).safeToDeleteFor(integration, today) }
    }
}

/** Returns all the children that would be affected by removing the node */
private fun PersonGraph.childrenConcernedBy(node: OwnNode): Set<ChildId> = buildSet {
    if (node.table.handledBy == Handler.CHILD || node.table == schema.person) {
        add(targetPersonId)
    }
    val childReferenceColumns =
        node.table.blockingReferences
            .filter { it.referencedTable == CHILD_TABLE }
            .map { it.referenceColumn }
    for (row in node.rows) {
        for (column in childReferenceColumns) {
            row.referencedIdByColumn[column]?.let { add(ChildId(it)) }
        }
    }
    for (bundledNodeId in ownNodeIdsByBundler[node.id].orEmpty()) {
        addAll(childrenConcernedBy(ownNode(bundledNodeId)))
    }
}

private fun PersonGraph.integrationFacts(childId: ChildId, node: OwnNode): ChildIntegrationFacts =
    checkNotNull(integrationFactsByChild[childId]) {
        "$node: the loader did not read the integration facts of child $childId"
    }

/**
 * The latest date the source gives for the node, or none when any row lacks a date: the fallback of
 * the rule decides then, not the other rows
 */
private fun PersonGraph.dateOf(source: DateSource, node: OwnNode): LocalDate? =
    when (source) {
        is DateSource.OwnColumn -> node.rows.map { it.date(source.column, node) }.latestOrNone()
        is DateSource.GraphTableColumn ->
            ownNodes
                .filter { it.table.name == source.table }
                .flatMap { other -> other.rows.map { it.date(source.column, other) } }
                .latestOrNone()
        is DateSource.Custom -> node.rows.map { it.customDate(source, node) }.latestOrNone()
    }

private fun List<LocalDate?>.latestOrNone(): LocalDate? =
    if (isEmpty() || any { it == null }) null else filterNotNull().max()

/** A loaded row must carry every date and archived result the rules of its table read */
private fun OwnRow.date(column: String, node: OwnNode): LocalDate? {
    check(column in dateByColumn) { "$node: the loader did not read column $column" }
    return dateByColumn[column]
}

private fun OwnRow.customDate(source: DateSource.Custom, node: OwnNode): LocalDate? {
    check(source in dateByCustomSource) { "$node: the loader did not read $source" }
    return dateByCustomSource[source]
}

private fun OwnRow.mayExpireUnder(rule: ArchivedIfRequired, node: OwnNode): Boolean =
    checkNotNull(mayExpireByArchivedRule[rule]) { "$node: the loader did not evaluate $rule" }
