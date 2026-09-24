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
    val referenceColumn: String
    val referencedTable: String

    /**
     * Keeps the referenced rows alive: they cannot be deleted while a row that references them
     * exists. A foreign key column with NOT NULL has to be blocking, as it cannot be cleared.
     */
    sealed interface Blocking : Reference

    /**
     * Leads to the target person: the rows found through it are the table's own rows. A table can
     * have only one primary reference.
     */
    data class Primary(override val referenceColumn: String, override val referencedTable: String) :
        Blocking

    /** Blocks like a primary reference, but the rows found through it are foreign rows */
    data class Secondary(
        override val referenceColumn: String,
        override val referencedTable: String,
    ) : Blocking

    /**
     * Does not keep the referenced rows alive: [referenceColumn] and [alsoNull] columns are set
     * null before deleting the referenced row
     */
    data class Optional(
        override val referenceColumn: String,
        override val referencedTable: String,
        val alsoNull: List<String>,
    ) : Reference
}

fun primaryReference(referenceColumn: String, referencedTable: String): Reference =
    Reference.Primary(referenceColumn, referencedTable)

fun secondaryReference(referenceColumn: String, referencedTable: String): Reference =
    Reference.Secondary(referenceColumn, referencedTable)

fun optionalReference(
    referenceColumn: String,
    referencedTable: String,
    alsoNull: List<String> = emptyList(),
): Reference = Reference.Optional(referenceColumn, referencedTable, alsoNull)

/**
 * This class holds the uuid column values that are needed to uniquely identify a row.
 *
 * Most tables are identified by a primary key, usually a single column named `id`, but some need to
 * be identified by multiple columns. The identifying columns are defined in
 * [Table.identifiedByCols].
 */
data class RowIdentity(val idByColumn: Map<String, UUID>) {
    /**
     * The id of a row identified by one column. Throws if the row is identified by multiple
     * columns.
     */
    fun single(): UUID = idByColumn.values.single()

    override fun toString(): String = idByColumn.values.joinToString(prefix = "[", postfix = "]")
}

/**
 * A row the plan deletes: its id and the values of the columns in [AsyncJobsOnDelete.columnsToRead]
 */
data class DeletedRow(val id: RowIdentity, val valuesByColumn: Map<String, String?>)

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
 * the referencing row alone. Such a row would be left behind as an orphan. If that reference is
 * declared using this, it will be deleted after the row that referenced it.
 */
data class OutsideGraphReference(
    val referenceColumn: String,
    val referencedTable: String,
    val referencedColumn: String = "id",
)

sealed interface Table {
    val name: String
    val references: List<Reference>

    /** The columns by which a row is identified and deleted (usually just "id") */
    val identifiedByCols: List<String>

    val blockingReferences: List<Reference.Blocking>
        get() = references.filterIsInstance<Reference.Blocking>()

    val secondaryReferences: List<Reference.Secondary>
        get() = references.filterIsInstance<Reference.Secondary>()

    val optionalReferences: List<Reference.Optional>
        get() = references.filterIsInstance<Reference.Optional>()
}

/**
 * A table whose rows a child's or an adult's run evaluates and deletes, as opposed to
 * [ExternalTable].
 */
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
data class DeclaredOptionalReference(
    val referencingTable: String,
    val reference: Reference.Optional,
)

/** The tables whose rows this algorithm deletes or waits for, and the references between them */
class SchemaDefinition(tables: List<Table>) {
    /** In declaration order */
    val tablesByName: Map<String, Table>
    val handledTables: List<HandledTable>
    val person: HandledTable
    val child: HandledTable

    /** `person` first, then every handled table after the one its primary reference points to */
    val rootFirstOrder: List<HandledTable>

    private val primaryReferenceByReferencingTable: Map<String, Reference.Primary>
    /** Note: Key is the referenced table, while the value holds the referencing table */
    private val optionalReferencesByReferencedTable: Map<String, List<DeclaredOptionalReference>>
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
            val referenceColumns = table.references.map { it.referenceColumn }
            require(referenceColumns.distinct().size == referenceColumns.size) {
                "$table declares several references on the same column"
            }
            for (reference in table.references) {
                require(reference.referencedTable != table.name) {
                    "$table: a self-reference is never declared (column ${reference.referenceColumn})"
                }
                val target =
                    requireNotNull(tablesByName[reference.referencedTable]) {
                        "$table: column ${reference.referenceColumn} references unknown table ${reference.referencedTable}"
                    }
                require(target is HandledTable) {
                    "$table: column ${reference.referenceColumn} references external table $target, which no reference may lead to"
                }
                require(target.identifiedByCols.size == 1) {
                    "$table: column ${reference.referenceColumn} references $target, which is identified by several columns"
                }
            }
            if (table !is HandledTable) {
                require(table.references.none { it is Reference.Primary }) {
                    "$table is an external table and should not have a primary reference"
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
        for (table in tables) requireReachesRoot(table, tablesKnownToReachRoot, emptyList())

        primaryReferenceByReferencingTable =
            handledTables
                .filter { it.name != PERSON_TABLE }
                .associate { table -> table.name to primaryReferenceOf(table) }

        val tablesByPrimaryTargetTable =
            primaryReferenceByReferencingTable.entries.groupBy(
                { it.value.referencedTable },
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
            require(getPrimaryPath(table).drop(1).any { it == bundlerTable }) {
                "$table is bundled by $bundler, which is not on its primary path"
            }
        }
        requireBundlesAgreeOnIndependentRows()

        val dateColumnTypesAccumulator = mutableMapOf<String, MutableMap<String, DateColumnType>>()
        fun recordDateColumn(
            tableUsingDateSource: HandledTable,
            source: DateSource,
            tableToRead: String,
            columnToRead: String,
            columnType: DateColumnType,
        ) {
            val previous =
                dateColumnTypesAccumulator
                    .getOrPut(tableToRead) { mutableMapOf() }
                    .put(columnToRead, columnType)
            require(previous == null || previous == columnType) {
                "$tableUsingDateSource: $source reads $tableToRead.$columnToRead as $columnType, but it is read as $previous elsewhere"
            }
        }
        for (table in handledTables) {
            for (source in table.expirationRule.usedDateSources()) {
                when (source) {
                    is DateSource.OwnColumn ->
                        recordDateColumn(
                            table,
                            source,
                            tableToRead = table.name,
                            columnToRead = source.column,
                            columnType = source.columnType,
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
                            tableToRead = other.name,
                            columnToRead = source.column,
                            columnType = source.columnType,
                        )
                    }
                    is DateSource.Custom ->
                        require(table.identifiedByCols.size == 1) {
                            "$table: a custom date source needs rows identified by a single column"
                        }
                }
            }
            require(
                table.expirationRule.usedArchivedIfRequiredRules().isEmpty() ||
                    table.identifiedByCols.size == 1
            ) {
                "$table: an archived if required rule needs rows identified by a single column"
            }
        }
        dateColumnTypesByTable = dateColumnTypesAccumulator

        optionalReferencesByReferencedTable =
            tables
                .flatMap { table ->
                    table.optionalReferences.map { DeclaredOptionalReference(table.name, it) }
                }
                .groupBy { it.reference.referencedTable }
    }

    /** Null for `person` only */
    fun primaryReference(table: HandledTable): Reference.Primary? =
        primaryReferenceByReferencingTable[table.name]

    /** From the table up to and including `person`, along primary references */
    fun getPrimaryPath(table: HandledTable): List<HandledTable> =
        generateSequence(table) {
                primaryReference(it)?.let { reference ->
                    tablesByName.getValue(reference.referencedTable) as HandledTable
                }
            }
            .toList()

    fun bundlerOf(table: HandledTable): HandledTable? =
        table.bundledBy?.let { tablesByName.getValue(it) as HandledTable }

    /** The optional references whose target is the table */
    fun optionalReferencesInto(table: Table): List<DeclaredOptionalReference> =
        optionalReferencesByReferencedTable[table.name].orEmpty()

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
        val targetHandler =
            (tablesByName.getValue(primary.referencedTable) as HandledTable).handledBy
        require(table.name == CHILD_TABLE || targetHandler == table.handledBy) {
            "$table is handled by ${table.handledBy}, but its primary reference ${primary.referenceColumn} leads to $targetHandler rows"
        }
        return primary
    }

    private fun requireReachesRoot(
        table: Table,
        tablesKnownToReachRoot: MutableSet<String>,
        path: List<Table> = emptyList(),
    ) {
        // A cycle through an optional reference is currently rejected too, but it is not a strict
        // requirement
        require(table !in path) {
            "References form a cycle: ${(path + table).joinToString(" -> ")}"
        }
        if (table == person || table.name in tablesKnownToReachRoot) return
        require(table.references.isNotEmpty()) {
            "$table references nothing, so it cannot reach $PERSON_TABLE"
        }
        for (reference in table.references) {
            requireReachesRoot(
                tablesByName.getValue(reference.referencedTable),
                tablesKnownToReachRoot,
                path + table,
            )
        }
        tablesKnownToReachRoot += table.name
    }

    /**
     * The tables between a bundled table and its bundler form one deletion bundle. A bundle should
     * either evaluate every table as a whole or every table row by row
     */
    private fun requireBundlesAgreeOnIndependentRows() {
        // Each table starts as its own bundle
        val bundleByTable: MutableMap<String, MutableSet<HandledTable>> =
            handledTables.associateTo(mutableMapOf()) { it.name to mutableSetOf(it) }

        // The tables between a table and its bundler are merged into one bundle, together with
        // whatever they were already bundled with
        for (table in handledTables) {
            val bundler = bundlerOf(table) ?: continue
            val path = getPrimaryPath(table)
            val bundledTogether = path.take(path.indexOf(bundler) + 1)
            val mergedBundle =
                bundledTogether.flatMapTo(mutableSetOf()) { bundleByTable.getValue(it.name) }
            for (member in mergedBundle) bundleByTable[member.name] = mergedBundle
        }

        for (bundle in bundleByTable.values.distinct()) {
            require(bundle.map { it.independentRows }.distinct().size == 1) {
                "Deletion bundle $bundle mixes tables with independent rows and tables evaluated as a whole"
            }
        }
    }
}
