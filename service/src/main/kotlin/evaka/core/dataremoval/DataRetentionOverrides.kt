// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

/**
 * A municipality's replacements for the expiration rule and deletedWithParent of the handled tables
 * it names. The safety conditions of a table cannot be overridden.
 */
class DataRetentionOverrides(val overridesByTable: Map<String, TableOverride>) {
    companion object {
        val NONE = DataRetentionOverrides(emptyMap())
    }
}

class TableOverride(val expirationRule: ExpirationRule, val deletedWithParent: Boolean)

/** The tables with the overridden ones replaced. Every override must name a handled table. */
fun DataRetentionOverrides.applyTo(tables: List<Table>): List<Table> {
    val handledTableNames = tables.filterIsInstance<HandledTable>().map { it.name }.toSet()
    val unknown = overridesByTable.keys - handledTableNames
    require(unknown.isEmpty()) {
        "Data retention overrides name tables that are not declared as handled tables: $unknown"
    }
    return tables.map { table ->
        val override = overridesByTable[table.name]
        if (table is HandledTable && override != null) table.overriddenBy(override) else table
    }
}

private fun HandledTable.overriddenBy(override: TableOverride) =
    HandledTable(
        name = name,
        handledBy = handledBy,
        references = references,
        deletedWithParent = override.deletedWithParent,
        independentRows = independentRows,
        identifiedByCols = identifiedByCols,
        auditIdType = auditIdType,
        expirationRule = override.expirationRule,
        financeFreeze = financeFreeze,
        safeForIntegrations = safeForIntegrations,
        asyncJobsPlannedOnDelete = asyncJobsPlannedOnDelete,
        orphansToDelete = orphansToDelete,
    )
