// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.dataremoval.DateSource.Custom
import evaka.core.dataremoval.DateSource.GraphTableColumn
import evaka.core.dataremoval.DateSource.OwnColumn
import evaka.core.dataremoval.ExpirationRule.After
import evaka.core.dataremoval.ExpirationRule.AllOf
import evaka.core.dataremoval.ExpirationRule.Always
import evaka.core.dataremoval.ExpirationRule.AnyOf
import evaka.core.dataremoval.ExpirationRule.Coalesce
import evaka.core.dataremoval.ExpirationRule.Never
import evaka.core.dataremoval.ExpirationRule.NotWhile
import evaka.core.shared.auth.AuthenticatedUser
import java.time.Period
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class DataRetentionSchemaGraph(
    val tables: List<DataRetentionTableNode>,
    val references: List<DataRetentionReferenceEdge>,
)

data class DataRetentionTableNode(
    val name: String,
    val external: Boolean,
    val handledBy: Handler?,
    val deletedWithParent: Boolean,
    /** Null when the table is deleted with no other table */
    val deletionBundleRoot: String?,
    val independentRows: Boolean,
    val identifiedByCols: List<String>,
    val expirationRule: String?,
    val financeFreeze: String?,
    val safeForIntegrations: List<Integration>,
    val orphansToDelete: List<DataRetentionOrphan>,
    val queuesAsyncJobs: Boolean,
    val asyncJobColumns: List<String>,
)

data class DataRetentionOrphan(val referenceColumn: String, val referencedTable: String)

data class DataRetentionReferenceEdge(
    val referencingTable: String,
    val referenceColumn: String,
    val referencedTable: String,
    val kind: ReferenceKind,
    val role: Handler,
    val alsoNull: List<String>,
)

enum class ReferenceKind {
    PRIMARY,
    SECONDARY,
    OPTIONAL,
}

@RestController
@RequestMapping("/employee/data-retention")
class DataRetentionSchemaController(dataRetentionService: DataRetentionService) {
    private val graph = dataRetentionService.schema.toGraph()

    // public data, no authorization required
    @GetMapping("/schema")
    fun getDataRetentionSchema(user: AuthenticatedUser.Employee): DataRetentionSchemaGraph = graph
}

fun SchemaDefinition.toGraph(): DataRetentionSchemaGraph {
    val bundleRootByTable = handledTables.associateWith { deletionBundleRoot(it) }
    val bundleRootsWithMembers =
        bundleRootByTable.entries.filter { it.key != it.value }.map { it.value }.toSet()
    return DataRetentionSchemaGraph(
        tables =
            tablesByName.values.map { table ->
                val handled = table as? HandledTable
                DataRetentionTableNode(
                    name = table.name,
                    external = handled == null,
                    handledBy = handled?.handledBy,
                    deletedWithParent = handled?.deletedWithParent ?: false,
                    deletionBundleRoot =
                        bundleRootByTable[handled]?.takeIf { it in bundleRootsWithMembers }?.name,
                    independentRows = handled?.independentRows ?: false,
                    identifiedByCols = table.identifiedByCols,
                    expirationRule = handled?.expirationRule?.describe(),
                    financeFreeze = handled?.financeFreeze?.describe(),
                    safeForIntegrations = handled?.safeForIntegrations.orEmpty().sorted(),
                    orphansToDelete =
                        handled?.orphansToDelete.orEmpty().map {
                            DataRetentionOrphan(it.referenceColumn, it.referencedTable)
                        },
                    queuesAsyncJobs = handled?.asyncJobsPlannedOnDelete != null,
                    asyncJobColumns = handled?.asyncJobsPlannedOnDelete?.columnsToRead.orEmpty(),
                )
            },
        references =
            tablesByName.values.flatMap { table ->
                table.references.map { reference ->
                    DataRetentionReferenceEdge(
                        referencingTable = table.name,
                        referenceColumn = reference.referenceColumn,
                        referencedTable = reference.referencedTable,
                        kind =
                            when (reference) {
                                is Reference.Primary -> ReferenceKind.PRIMARY
                                is Reference.Secondary -> ReferenceKind.SECONDARY
                                is Reference.Optional -> ReferenceKind.OPTIONAL
                            },
                        role =
                            (tablesByName.getValue(reference.referencedTable) as HandledTable)
                                .handledBy,
                        alsoNull = (reference as? Reference.Optional)?.alsoNull.orEmpty(),
                    )
                }
            },
    )
}

private fun ExpirationRule.describe(): String =
    when (this) {
        Always -> "always"
        Never -> "never"
        is AllOf -> rules.joinToString(" AND ") { it.describeGrouped() }
        is AnyOf -> rules.joinToString(" OR ") { it.describeGrouped() }
        is Coalesce -> rules.joinToString(" ELSE ") { it.describeGrouped() }
        is After ->
            dateSource.describe().let {
                if (period.isZero) it else "${period.describe()} after $it"
            }
        is NotWhile -> "not while $description"
    }

private fun ExpirationRule.describeGrouped(): String =
    when (this) {
        is AllOf,
        is AnyOf,
        is Coalesce -> "(${describe()})"
        else -> describe()
    }

private fun DateSource.describe(): String =
    when (this) {
        is OwnColumn -> column + columnType.describeReading()
        is GraphTableColumn -> "the latest $table.$column" + columnType.describeReading()
        is Custom -> description
    }

private fun DateColumnType.describeReading(): String =
    when (this) {
        DateColumnType.DATE,
        DateColumnType.TIMESTAMP_WITH_TIME_ZONE -> ""
        DateColumnType.DATE_RANGE_END,
        DateColumnType.FINITE_DATE_RANGE_END -> " (range end)"
    }

private fun Period.describe(): String =
    listOfNotNull(
            years.takeIf { it != 0 }?.let { amount(it, "year") },
            months.takeIf { it != 0 }?.let { amount(it, "month") },
            days.takeIf { it != 0 }?.let { amount(it, "day") },
        )
        .joinToString(" ")

private fun amount(value: Int, unit: String) = "$value $unit" + if (value == 1) "" else "s"
