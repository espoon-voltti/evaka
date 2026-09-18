// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.shared.async.AsyncJob
import java.util.UUID

const val PERSON_TABLE = "person"
const val CHILD_TABLE = "child"

/** Whose run deletes the rows of a table */
enum class Handler {
    CHILD,
    ADULT,
}

/** A foreign key from a table of the schema definition to a handled table */
sealed interface Reference {
    val column: String
    val toTable: String

    /**
     * Keeps the referenced rows alive: they cannot be deleted while a row that references them
     * exists
     */
    sealed interface Blocking : Reference

    /**
     * Leads to the target person: the own rows of the table are the rows whose primary reference
     * holds the id of a loaded row
     */
    data class Primary(override val column: String, override val toTable: String) : Blocking

    /** Blocks like a primary reference, but the rows found through it are foreign rows */
    data class Secondary(override val column: String, override val toTable: String) : Blocking

    /**
     * Does not keep the referenced rows alive: the column and [alsoNull] are set null in every
     * referencing row before they are deleted
     */
    data class Optional(
        override val column: String,
        override val toTable: String,
        val alsoNull: List<String>,
    ) : Reference
}

fun primaryReference(column: String, toTable: String): Reference =
    Reference.Primary(column, toTable)

fun secondaryReference(column: String, toTable: String): Reference =
    Reference.Secondary(column, toTable)

fun optionalReference(
    column: String,
    toTable: String,
    alsoNull: List<String> = emptyList(),
): Reference = Reference.Optional(column, toTable, alsoNull)

/** The values of a row's id columns by column name, the columns being [Table.identifiedByCols] */
data class RowId(val valueByColumn: Map<String, UUID>) {
    /** The id of a row identified by one column */
    fun single(): UUID = valueByColumn.values.single()

    override fun toString(): String = valueByColumn.values.joinToString(prefix = "[", postfix = "]")
}

/**
 * A row the plan deletes: its id and the values of the columns in [AsyncJobsOnDelete.columnsToRead]
 */
data class DeletedRow(val id: RowId, val valuesByColumn: Map<String, String?>)

/**
 * Async jobs to queue in the transaction that deletes the rows, such as deleting their files. The
 * text columns named are read from every deleted row for [jobsToPlan].
 */
class AsyncJobsOnDelete(
    val columnsToRead: List<String>,
    val jobsToPlan: (row: DeletedRow) -> List<AsyncJob>,
)

/**
 * A foreign key from a table of the schema definition to a table outside it, whose row belongs to
 * the referencing row alone. The referenced row would be left as an orphan, so it is deleted right
 * after the row that references it, by [referencedColumn], the primary key column the reference
 * points at.
 */
data class OutsideGraphReference(
    val referenceColumn: String,
    val referencedTable: String,
    val referencedColumn: String = "id",
)

sealed interface Table {
    val name: String
    val references: List<Reference>

    /** The columns by which a row is identified and deleted */
    val identifiedByCols: List<String>

    val blockingReferences: List<Reference.Blocking>
        get() = references.filterIsInstance<Reference.Blocking>()

    val secondaryReferences: List<Reference.Secondary>
        get() = references.filterIsInstance<Reference.Secondary>()

    val optionalReferences: List<Reference.Optional>
        get() = references.filterIsInstance<Reference.Optional>()
}

/** A table whose rows a child's or an adult's run evaluates and deletes */
class HandledTable(
    override val name: String,
    val handledBy: Handler,
    override val references: List<Reference>,
    /**
     * An ancestor on the primary path whose rows these rows always go with, never one without the
     * other
     */
    val bundledBy: String? = null,
    /**
     * When true, the rows may expire at different times and each is evaluated and deleted on its
     * own. When false, the rows expire all at once and are evaluated and deleted as one node.
     */
    val independentRows: Boolean = false,
    override val identifiedByCols: List<String> = listOf("id"),
    val expirationRule: ExpirationRule,
    val asyncJobsPlannedOnDelete: AsyncJobsOnDelete? = null,
    /** Rows outside the schema definition that only the rows of this table reference */
    val orphansToDelete: List<OutsideGraphReference> = emptyList(),
) : Table {
    override fun toString(): String = name
}

/**
 * A table whose rows some other job deletes: this algorithm only waits for them, finding them as
 * foreign rows through the secondary references
 */
class ExternalTable(
    override val name: String,
    override val references: List<Reference>,
    override val identifiedByCols: List<String> = listOf("id"),
) : Table {
    override fun toString(): String = name
}

/** An optional reference with the table that declares it */
data class DeclaredOptionalReference(val table: Table, val reference: Reference.Optional)

/** The tables whose rows this algorithm deletes or waits for, and the references between them */
class SchemaDefinition(tables: List<Table>) {
    /** In declaration order */
    val tablesByName: Map<String, Table>
    val handledTables: List<HandledTable>
    val person: HandledTable
    val child: HandledTable

    /** `person` first, then every handled table after the one its primary reference points to */
    val rootFirstOrder: List<HandledTable>

    private val primaryReferenceByTable: Map<String, Reference.Primary>
    private val optionalReferencesByTargetTable: Map<String, List<DeclaredOptionalReference>>
    private val dateColumnTypesByTable: Map<String, Map<String, DateColumnType>>

    init {
        val duplicates = tables.groupBy { it.name }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate tables: $duplicates" }
        tablesByName = tables.associateBy { it.name }
        handledTables = tables.filterIsInstance<HandledTable>()

        person =
            requireNotNull(tablesByName[PERSON_TABLE] as? HandledTable) {
                "The root table $PERSON_TABLE is not declared as a handled table"
            }
        require(
            person.handledBy == Handler.ADULT &&
                person.references.isEmpty() &&
                person.bundledBy == null &&
                !person.independentRows &&
                person.identifiedByCols == listOf("id")
        ) {
            "$PERSON_TABLE is deleted in the run of the person it is: it is handled by adult, references nothing, is evaluated as a whole and is identified by id"
        }
        child =
            requireNotNull(tablesByName[CHILD_TABLE] as? HandledTable) {
                "The table $CHILD_TABLE is not declared as a handled table"
            }
        require(
            child.handledBy == Handler.CHILD &&
                child.references == listOf(Reference.Primary("id", PERSON_TABLE)) &&
                !child.independentRows &&
                child.identifiedByCols == listOf("id")
        ) {
            "$CHILD_TABLE is deleted in the run of the child it is: it is handled by child and its only reference is id to $PERSON_TABLE"
        }

        for (table in tables) {
            require(table.identifiedByCols.isNotEmpty()) { "$table has no id columns" }
            val referenceColumns = table.references.map { it.column }
            require(referenceColumns.distinct().size == referenceColumns.size) {
                "$table declares several references on the same column"
            }
            for (reference in table.references) {
                require(reference.toTable != table.name) {
                    "$table: a self-reference is never declared (column ${reference.column})"
                }
                val target =
                    requireNotNull(tablesByName[reference.toTable]) {
                        "$table: column ${reference.column} references unknown table ${reference.toTable}"
                    }
                require(target is HandledTable) {
                    "$table: column ${reference.column} references external table $target, which no reference may lead to"
                }
                require(target.identifiedByCols.size == 1) {
                    "$table: column ${reference.column} references $target, which is identified by several columns"
                }
            }
            if (table !is HandledTable) {
                require(table.references.none { it is Reference.Primary }) {
                    "$table is an external table, which declares no primary reference"
                }
                continue
            }
            val orphanColumns = table.orphansToDelete.map { it.referenceColumn }
            require(orphanColumns.distinct().size == orphanColumns.size) {
                "$table declares several outside graph references on the same column"
            }
            for (outside in table.orphansToDelete) {
                require(outside.referencedTable !in tablesByName) {
                    "$table: ${outside.referencedTable} is a declared table, not one outside the graph"
                }
                require(outside.referenceColumn !in referenceColumns) {
                    "$table: column ${outside.referenceColumn} is a declared reference, not one outside the graph"
                }
            }
        }

        val tablesKnownToReachRoot = mutableSetOf<String>()
        for (table in tables) requireReachesRoot(table, emptyList(), tablesKnownToReachRoot)

        primaryReferenceByTable =
            handledTables
                .filter { it.name != PERSON_TABLE }
                .associate { table -> table.name to primaryReferenceOf(table) }

        val tablesByPrimaryTargetTable =
            primaryReferenceByTable.entries.groupBy(
                { it.value.toTable },
                { tablesByName.getValue(it.key) as HandledTable },
            )
        rootFirstOrder = buildList {
            val queue = ArrayDeque(listOf(person))
            while (queue.isNotEmpty()) {
                val table = queue.removeFirst()
                add(table)
                queue.addAll(tablesByPrimaryTargetTable[table.name].orEmpty())
            }
        }

        for (table in handledTables) {
            val bundler = table.bundledBy ?: continue
            val bundlerTable =
                requireNotNull(tablesByName[bundler]) {
                    "$table is bundled by unknown table $bundler"
                }
            require(primaryPath(table).drop(1).any { it == bundlerTable }) {
                "$table is bundled by $bundler, which is not on its primary path"
            }
        }
        requireBundlesAgreeOnIndependentRows()

        val dateColumnTypes = mutableMapOf<String, MutableMap<String, DateColumnType>>()
        fun recordDateColumn(
            table: HandledTable,
            source: DateSource,
            fromTable: String,
            column: String,
            columnType: DateColumnType,
        ) {
            val previous =
                dateColumnTypes.getOrPut(fromTable) { mutableMapOf() }.put(column, columnType)
            require(previous == null || previous == columnType) {
                "$table: $source reads $fromTable.$column as $columnType, but it is read as $previous elsewhere"
            }
        }
        for (table in handledTables) {
            for (source in table.expirationRule.dateSources()) {
                when (source) {
                    is DateSource.OwnColumn ->
                        recordDateColumn(
                            table,
                            source,
                            table.name,
                            source.column,
                            source.columnType,
                        )
                    is DateSource.GraphTableColumn -> {
                        val other =
                            requireNotNull(tablesByName[source.table] as? HandledTable) {
                                "$table: $source reads a table that is not declared as a handled table"
                            }
                        require(other == person || other.handledBy == table.handledBy) {
                            "$table: $source reads a table with another handler, whose rows are not in the graph of a ${table.handledBy} run"
                        }
                        recordDateColumn(
                            table,
                            source,
                            other.name,
                            source.column,
                            source.columnType,
                        )
                    }
                    is DateSource.Custom ->
                        require(table.identifiedByCols.size == 1) {
                            "$table: a custom date source needs rows identified by a single column"
                        }
                }
            }
            require(
                table.expirationRule.archivedIfRequiredRules().isEmpty() ||
                    table.identifiedByCols.size == 1
            ) {
                "$table: an archived if required rule needs rows identified by a single column"
            }
        }
        dateColumnTypesByTable = dateColumnTypes

        optionalReferencesByTargetTable =
            tables
                .flatMap { table ->
                    table.optionalReferences.map { DeclaredOptionalReference(table, it) }
                }
                .groupBy { it.reference.toTable }
    }

    /** Null for `person` only */
    fun primaryReference(table: HandledTable): Reference.Primary? =
        primaryReferenceByTable[table.name]

    /** From the table up to and including `person`, along primary references */
    fun primaryPath(table: HandledTable): List<HandledTable> =
        generateSequence(table) {
                primaryReference(it)?.let { reference ->
                    tablesByName.getValue(reference.toTable) as HandledTable
                }
            }
            .toList()

    fun bundlerOf(table: HandledTable): HandledTable? =
        table.bundledBy?.let { tablesByName.getValue(it) as HandledTable }

    /** The optional references whose target is the table */
    fun optionalReferencesInto(table: Table): List<DeclaredOptionalReference> =
        optionalReferencesByTargetTable[table.name].orEmpty()

    /**
     * The date columns the loader reads from the table's rows, for the rules of the table itself
     * and of the tables that read its column
     */
    fun dateColumnsToRead(table: HandledTable): Map<String, DateColumnType> =
        dateColumnTypesByTable[table.name].orEmpty()

    /** The primary reference leads to the table's handler, except from `child` to `person` */
    private fun primaryReferenceOf(table: HandledTable): Reference.Primary {
        val primaries = table.references.filterIsInstance<Reference.Primary>()
        require(primaries.isNotEmpty()) { "$table declares no primary reference" }
        require(primaries.size == 1) {
            "$table declares ${primaries.size} primary references, exactly one is required"
        }
        val primary = primaries.single()
        val targetHandler = (tablesByName.getValue(primary.toTable) as HandledTable).handledBy
        require(table.name == CHILD_TABLE || targetHandler == table.handledBy) {
            "$table is handled by ${table.handledBy}, but its primary reference ${primary.column} leads to $targetHandler rows"
        }
        return primary
    }

    /** Every table is expanded once */
    private fun requireReachesRoot(
        table: Table,
        path: List<Table>,
        tablesKnownToReachRoot: MutableSet<String>,
    ) {
        // A cycle through an optional reference is rejected too, although the graph would not
        // deadlock on it: nothing has needed that case yet
        require(table !in path) {
            "References form a cycle: ${(path + table).joinToString(" -> ")}"
        }
        if (table == person || table.name in tablesKnownToReachRoot) return
        require(table.references.isNotEmpty()) {
            "$table references nothing, so it cannot reach $PERSON_TABLE"
        }
        for (reference in table.references) {
            requireReachesRoot(
                tablesByName.getValue(reference.toTable),
                path + table,
                tablesKnownToReachRoot,
            )
        }
        tablesKnownToReachRoot += table.name
    }

    /**
     * The tables between a bundled table and its bundler form one deletion bundle, and a bundle
     * either evaluates every table as a whole or every table row by row
     */
    private fun requireBundlesAgreeOnIndependentRows() {
        val bundleRepresentativeOf =
            handledTables.associateTo(mutableMapOf()) { it.name to it.name }
        fun find(name: String): String =
            if (bundleRepresentativeOf.getValue(name) == name) name
            else find(bundleRepresentativeOf.getValue(name))
        for (table in handledTables) {
            val bundler = bundlerOf(table) ?: continue
            val path = primaryPath(table)
            for (member in path.take(path.indexOf(bundler) + 1)) {
                bundleRepresentativeOf[find(member.name)] = find(bundler.name)
            }
        }
        for (bundle in handledTables.groupBy { find(it.name) }.values) {
            require(bundle.map { it.independentRows }.distinct().size == 1) {
                "Deletion bundle $bundle mixes tables with independent rows and tables evaluated as a whole"
            }
        }
    }
}
