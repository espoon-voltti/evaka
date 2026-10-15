// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.dataremoval.ExpirationRule.After
import evaka.core.dataremoval.ExpirationRule.AllOf
import evaka.core.dataremoval.ExpirationRule.Always
import evaka.core.dataremoval.ExpirationRule.AnyOf
import evaka.core.dataremoval.ExpirationRule.Coalesce
import evaka.core.dataremoval.ExpirationRule.Never
import evaka.core.dataremoval.ExpirationRule.NotWhile
import java.time.LocalDate

fun SchemaDefinition.tablesKnownToStay(
    preloadedNodes: List<OwnNode>,
    integrationFacts: IntegrationFacts,
    today: LocalDate,
): Set<String> {
    val preloadedData = PreloadedData(this, preloadedNodes, integrationFacts, today)
    val preloadedTablesWithRows =
        preloadedNodes.filter { it.rows.isNotEmpty() }.mapTo(mutableSetOf()) { it.table.name }
    val tablesDeletedWithParentByParent =
        handledTables.filter { it.deletedWithParent }.groupBy { checkNotNull(parentOf(it)).name }

    val knownToStay = mutableSetOf<String>()
    val toFollow = ArrayDeque<HandledTable>()
    fun add(table: HandledTable) {
        if (knownToStay.add(table.name)) toFollow.addLast(table)
    }

    for (table in handledTables) {
        val cannotExpire =
            if (table in preloadedTables) {
                table.name in preloadedTablesWithRows &&
                    !preloadedData.mightExpire(
                        table,
                        node = preloadedNodes.single { it.table == table },
                    )
            } else {
                !preloadedData.mightExpire(table, node = null)
            }
        if (cannotExpire) add(table)
    }
    while (toFollow.isNotEmpty()) {
        val table = toFollow.removeFirst()
        tablesDeletedWithParentByParent[table.name].orEmpty().forEach(::add)
        // The rows block the rows on their primary path, which are single rows of person and child
        if (table.name in preloadedTablesWithRows) getPrimaryPath(table).forEach(::add)
    }
    return knownToStay
}

/** The tables known to stay that are not loaded at all */
fun SchemaDefinition.tablesToSkip(tablesKnownToStay: Set<String>): Set<String> {
    val skipped = mutableSetOf<String>()
    val parentsOfLoadedTables = mutableSetOf<String>()
    // Leaves first, so that a table is decided after every table whose parent it is
    for (table in rootFirstOrder.asReversed()) {
        val skip =
            table.name in tablesKnownToStay &&
                table !in preloadedTables &&
                tablesReadingDatesOf(table).all { it.name in tablesKnownToStay } &&
                table.blockingReferences.all { it.referencedTable in tablesKnownToStay } &&
                table.name !in parentsOfLoadedTables
        if (skip) skipped += table.name
        else parentOf(table)?.let { parentsOfLoadedTables += it.name }
    }
    return skipped
}

/** What a rule might return, judged from the preloaded tables alone */
private data class PossibleResults(val mightBeMet: Boolean, val mightReturnNoVerdict: Boolean) {
    companion object {
        /** For a rule that reads other data than the preloaded tables */
        val UNKNOWN = PossibleResults(mightBeMet = true, mightReturnNoVerdict = true)
    }
}

/** The own nodes of the preloaded tables and the integration facts of the target */
private class PreloadedData(
    private val schema: SchemaDefinition,
    private val nodes: List<OwnNode>,
    private val integrationFacts: IntegrationFacts,
    private val today: LocalDate,
) {
    /** [node] is a node of a preloaded table, or null for a table that is not preloaded */
    fun mightExpire(table: HandledTable, node: OwnNode?): Boolean {
        val financeFreeze = table.financeFreeze
        val integrationCheckFails =
            table.mayHoldChildData &&
                table.safeForIntegrations.any { !integrationFacts.safeToDeleteFor(it, today) }
        return possibleResults(table.expirationRule, node).mightBeMet &&
            (financeFreeze == null || possibleResults(financeFreeze, node).mightBeMet) &&
            !integrationCheckFails
    }

    private fun possibleResults(rule: ExpirationRule, node: OwnNode?): PossibleResults =
        when (rule) {
            Always -> PossibleResults(mightBeMet = true, mightReturnNoVerdict = false)
            Never -> PossibleResults(mightBeMet = false, mightReturnNoVerdict = false)
            is AllOf ->
                PossibleResults(
                    mightBeMet = rule.rules.all { possibleResults(it, node).mightBeMet },
                    mightReturnNoVerdict = false,
                )
            is AnyOf ->
                PossibleResults(
                    mightBeMet = rule.rules.any { possibleResults(it, node).mightBeMet },
                    mightReturnNoVerdict = false,
                )
            is Coalesce -> coalesced(rule.rules.map { possibleResults(it, node) })
            is After -> possibleResults(rule, node)
            is NotWhile ->
                PossibleResults(
                    mightBeMet = node?.mayExpireUnder(rule) ?: true,
                    mightReturnNoVerdict = false,
                )
        }

    /** The rules after the first one that always returns a verdict are never reached */
    private fun coalesced(members: List<PossibleResults>): PossibleResults {
        var mightBeMet = false
        for (member in members) {
            mightBeMet = mightBeMet || member.mightBeMet
            if (!member.mightReturnNoVerdict) {
                return PossibleResults(mightBeMet, mightReturnNoVerdict = false)
            }
        }
        return PossibleResults(mightBeMet, mightReturnNoVerdict = true)
    }

    private fun possibleResults(rule: After, node: OwnNode?): PossibleResults {
        val date =
            when (val source = rule.dateSource) {
                is DateSource.GraphTableColumn ->
                    if (schema.preloadedTables.any { it.name == source.table }) {
                        nodes.latestDateOf(source)
                    } else {
                        return PossibleResults.UNKNOWN
                    }
                is DateSource.OwnColumn ->
                    (node ?: return PossibleResults.UNKNOWN).latestDateOf(source)
                is DateSource.Custom ->
                    (node ?: return PossibleResults.UNKNOWN).latestDateOf(source)
            }
        return PossibleResults(
            mightBeMet = date != null && date.plus(rule.period) < today,
            mightReturnNoVerdict = date == null,
        )
    }
}
