// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.PureJdbiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jdbi.v3.core.mapper.Nested
import org.junit.jupiter.api.BeforeAll

/**
 * Compares the schema definition against the live schema, so that a forgotten table or foreign key
 * is caught before a run deletes rows it references.
 *
 * NOTE: If this test fails you have likely added a table or a foreign key that points at a table of
 * the schema definition. Decide what the retention algorithm should do with it:
 * 1) declare the table in [dataRetentionSchema] with its references and expiration rule, or
 * 2) if the database handles its rows with ON DELETE CASCADE or SET NULL and it needs no rule of
 *    its own, add the foreign key to [handledByDatabase]
 */
class DataRetentionSchemaTest : PureJdbiTest(resetDbBeforeEach = false) {
    data class ColumnRef(val tableName: String, val columnName: String) {
        override fun toString() = "$tableName.$columnName"
    }

    data class Column(@Nested val ref: ColumnRef, val nullable: Boolean, val dataType: String)

    data class ForeignKey(
        @Nested val ref: ColumnRef,
        @Nested("references_") val referencesRef: ColumnRef,
        /**
         * pg_constraint.confdeltype: a = no action, r = restrict, c = cascade, n = set null, d =
         * set default
         */
        val onDelete: String,
        val columnCount: Int,
    )

    data class UniqueKey(
        val tableName: String,
        val columns: List<String>,
        /** The WHERE clause of a partial unique index */
        val predicate: String?,
    )

    data class CheckConstraint(
        val tableName: String,
        val columns: List<String>,
        val definition: String,
    )

    private lateinit var columns: List<Column>
    private lateinit var foreignKeys: List<ForeignKey>

    /** The first column of every index */
    private lateinit var indexedColumns: Set<ColumnRef>
    private lateinit var uniqueKeys: List<UniqueKey>
    private lateinit var checkConstraints: List<CheckConstraint>

    private val schema = dataRetentionSchema
    private val declaredTables = schema.tablesByName.values
    private val handledTables = schema.handledTables

    /**
     * The rows of these tables are deleted by this algorithm, so every foreign key into them
     * matters
     */
    private val deletedTables = handledTables.map { it.name }.toSet()

    /**
     * A reference declared to child may target person until the foreign keys are possibly migrated
     */
    private val childTargets = setOf(CHILD_TABLE, PERSON_TABLE)

    private val declaredReferences: Set<ColumnRef> =
        declaredTables
            .flatMap { table -> table.references.map { ColumnRef(table.name, it.referenceColumn) } }
            .toSet()

    /**
     * Foreign keys of the tables excluded by exception: the database cascade or set null handles
     * their rows, and they need no rule of their own
     */
    private val handledByDatabase =
        columnRefs(
            "attachment.application_id",
            "attachment.income_id",
            "attachment.income_statement_id",
            "citizen_passkey_registration.person_id",
            "citizen_push_subscription.person_id",
            "citizen_user.id",
            "decision_reasoning_individual_selection.decision_id",
            "evaka_user.citizen_id",
            "invoiced_fee_decision.fee_decision_id",
            "person_email_verification.person_id",
            "sfi_message.decision_id",
            "sfi_message.document_id",
            "sfi_message.fee_decision_id",
            "sfi_message.guardian_id",
            "sfi_message.voucher_value_decision_id",
        )

    /** The job refuses a person with a value in these columns, or referenced through them */
    private val refused = columnRefs("person.duplicate_of")

    /** Composite, so never declared: a partnership is deleted by its id, both rows at once */
    private val compositeSelfReference = columnRefs("fridge_partner.partnership_id")

    /**
     * A partnership's two rows share its id, which is unique only among the rows of one partner, so
     * a run, which loads only the target's rows, still identifies them by it
     */
    private val idUniqueOnlyWithPerson = mapOf("fridge_partner" to "person_id")

    /**
     * Tables whose handling is decided later. Their foreign keys are NO ACTION or RESTRICT, so a
     * run that reaches their rows fails and rolls back instead of deleting them unseen.
     */
    private val notYetDeclared =
        columnRefs(
            "absence.child_id",
            "absence_application.child_id",
            "assistance_need_voucher_coefficient.child_id",
            "attendance_reservation.child_id",
            "calendar_event_time.child_id",
            "child_attendance.child_id",
            "child_sticky_note.child_id",
            "foster_parent.child_id",
            "foster_parent.parent_id",
            "holiday_questionnaire_answer.child_id",
            "invoice.codebtor",
            "invoice_correction.child_id",
            "message_thread_children.child_id",
            "nekku_special_diet_choices.child_id",
            "pedagogical_document.child_id",
            "pedagogical_document_read.person_id",
            "service_application.child_id",
            "service_application.person_id",
        )

    /**
     * Also decided later, but these cascade: a run that deletes the person or child row would
     * delete their rows unseen, with no rule, audit event or integration freeze. The job must not
     * run on real data before every one of them is declared.
     */
    private val notYetDeclaredCascading =
        columnRefs(
            "assistance_action.child_id",
            "assistance_factor.child_id",
            "backup_pickup.child_id",
            "calendar_event_attendee.child_id",
            "child_daily_note.child_id",
            "daily_service_time.child_id",
            "daily_service_time_notification.guardian_id",
            "daycare_assistance.child_id",
            "fee_alteration.person_id",
            "finance_note.person_id",
            "guardian_blocklist.child_id",
            "guardian_blocklist.guardian_id",
            "income_notification.receiver_id",
            "invoice.head_of_family",
            "invoice_correction.head_of_family_id",
            "invoice_row.child",
            "other_assistance_measure.child_id",
            "preschool_assistance.child_id",
        )

    private fun columnRefs(vararg names: String): Set<ColumnRef> =
        names.map { ColumnRef(it.substringBefore('.'), it.substringAfter('.')) }.toSet()

    @BeforeAll
    override fun beforeAll() {
        super.beforeAll()
        db.read { tx ->
            columns =
                tx.createQuery {
                        sql(
                            """
SELECT c.table_name, c.column_name, c.is_nullable AS nullable, c.data_type
FROM information_schema.columns c
JOIN information_schema.tables t USING (table_schema, table_name)
WHERE t.table_schema = 'public' AND t.table_type = 'BASE TABLE'
"""
                        )
                    }
                    .toList<Column>()
            foreignKeys =
                tx.createQuery {
                        sql(
                            """
SELECT
  src.relname AS table_name,
  a.attname AS column_name,
  dst.relname AS references_table_name,
  ra.attname AS references_column_name,
  c.confdeltype::text AS on_delete,
  cardinality(c.conkey) AS column_count
FROM pg_constraint c
JOIN pg_class src ON src.oid = c.conrelid
JOIN pg_class dst ON dst.oid = c.confrelid
JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
JOIN pg_attribute ra ON ra.attrelid = c.confrelid AND ra.attnum = c.confkey[1]
WHERE c.contype = 'f' AND src.relnamespace = 'public'::regnamespace
"""
                        )
                    }
                    .toList<ForeignKey>()
            indexedColumns =
                tx.createQuery {
                        sql(
                            """
SELECT t.relname AS table_name, a.attname AS column_name
FROM pg_index i
JOIN pg_class t ON t.oid = i.indrelid
JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = i.indkey[0]
WHERE t.relnamespace = 'public'::regnamespace
"""
                        )
                    }
                    .toSet<ColumnRef>()
            uniqueKeys =
                tx.createQuery {
                        sql(
                            """
SELECT
  t.relname AS table_name,
  array_agg(a.attname ORDER BY k.ord) AS columns,
  pg_get_expr(i.indpred, i.indrelid) AS predicate
FROM pg_index i
JOIN pg_class t ON t.oid = i.indrelid
JOIN LATERAL unnest(i.indkey::smallint[]) WITH ORDINALITY AS k(attnum, ord) ON true
JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum
WHERE i.indisunique AND t.relnamespace = 'public'::regnamespace
GROUP BY i.indexrelid, t.relname, i.indpred, i.indrelid
"""
                        )
                    }
                    .toList<UniqueKey>()
            checkConstraints =
                tx.createQuery {
                        sql(
                            """
SELECT
  t.relname AS table_name,
  array_agg(a.attname) AS columns,
  pg_get_constraintdef(c.oid) AS definition
FROM pg_constraint c
JOIN pg_class t ON t.oid = c.conrelid
JOIN LATERAL unnest(c.conkey) AS k(attnum) ON true
JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum
WHERE c.contype = 'c' AND t.relnamespace = 'public'::regnamespace
GROUP BY c.oid, t.relname
"""
                        )
                    }
                    .toList<CheckConstraint>()
        }
    }

    private fun columnOf(ref: ColumnRef): Column? = columns.find { it.ref == ref }

    @Test
    fun `every foreign key into a table whose rows are deleted here is declared, handled by the database, refused or listed as not yet declared`() {
        val into = foreignKeys.filter { it.referencesRef.tableName in deletedTables }
        assertEquals(
            compositeSelfReference,
            into.filter { it.columnCount > 1 }.map { it.ref }.toSet(),
            "composite foreign keys are never declared",
        )
        val refs = into.filter { it.columnCount == 1 }.map { it.ref }.toSet()
        val leftOut = handledByDatabase + refused + notYetDeclared + notYetDeclaredCascading
        assertEquals(
            emptySet(),
            refs - declaredReferences - leftOut,
            "foreign keys that are neither declared nor explicitly left out",
        )
        assertEquals(
            emptySet(),
            leftOut - refs,
            "left-out columns that are declared or no longer exist",
        )
        assertEquals(emptySet(), declaredReferences intersect leftOut)
    }

    @Test
    fun `every declared reference is a foreign key to the id column of the declared table`() {
        val invalid = declaredTables.flatMap { table ->
            table.references.mapNotNull { reference ->
                val targets =
                    if (reference.referencedTable == CHILD_TABLE) childTargets
                    else setOf(reference.referencedTable)
                val idColumn =
                    schema.tablesByName
                        .getValue(reference.referencedTable)
                        .identifiedByCols
                        .single()
                val valid = foreignKeys.any {
                    it.ref == ColumnRef(table.name, reference.referenceColumn) &&
                        it.referencesRef.tableName in targets &&
                        it.referencesRef.columnName == idColumn &&
                        it.columnCount == 1
                }
                if (valid) null
                else "${table.name}.${reference.referenceColumn} -> ${reference.referencedTable}"
            }
        }
        assertEquals(emptyList(), invalid)
    }

    @Test
    fun `the column of every declared reference is indexed`() {
        assertEquals(emptySet(), declaredReferences - indexedColumns)
    }

    @Test
    fun `the id columns of every table are its primary key or a unique constraint or index`() {
        val invalid =
            declaredTables
                .filter { table ->
                    val uniqueColumns =
                        table.identifiedByCols.toSet() +
                            listOfNotNull(idUniqueOnlyWithPerson[table.name])
                    uniqueKeys.none {
                        it.tableName == table.name &&
                            it.predicate == null &&
                            it.columns.toSet() == uniqueColumns
                    }
                }
                .map { "${it.name} ${it.identifiedByCols}" }
        assertEquals(emptyList(), invalid)
    }

    @Test
    fun `every id column is a uuid`() {
        val invalid = declaredTables.flatMap { table ->
            table.identifiedByCols
                .map { ColumnRef(table.name, it) }
                .filter { columnOf(it)?.dataType != "uuid" }
        }
        assertEquals(emptyList(), invalid)
    }

    /**
     * The orphans are deleted by the declared column after the rows that referenced them, so the
     * reference must be the only way in, and unique so that no surviving row shares the orphan
     */
    @Test
    fun `an outside graph reference is the only foreign key into its table, points at the declared uuid column, and its column is unique`() {
        val invalid = handledTables.flatMap { table ->
            table.orphansToDelete.flatMap { outside ->
                val ref = ColumnRef(table.name, outside.referenceColumn)
                val orphanId = ColumnRef(outside.referencedTable, outside.referencedColumn)
                val into = foreignKeys.filter {
                    it.referencesRef.tableName == outside.referencedTable
                }
                val unique = uniqueKeys.any {
                    it.tableName == table.name &&
                        it.columns == listOf(outside.referenceColumn) &&
                        (it.predicate == null ||
                            it.predicate == "(${outside.referenceColumn} IS NOT NULL)")
                }
                listOfNotNull(
                    "$ref is not the only foreign key into ${outside.referencedTable}: ${into.map { it.ref }}"
                        .takeIf { into.map { it.ref } != listOf(ref) },
                    "$ref does not reference $orphanId alone"
                        .takeIf {
                            into.none {
                                it.ref == ref &&
                                    it.referencesRef.columnName == outside.referencedColumn &&
                                    it.columnCount == 1
                            }
                        },
                    "$orphanId is not a uuid".takeIf { columnOf(orphanId)?.dataType != "uuid" },
                    "$ref is not unique".takeIf { !unique },
                )
            }
        }
        assertEquals(emptyList(), invalid)
    }

    @Test
    fun `the column of an optional reference and its alsoNull columns are nullable`() {
        val invalid = declaredTables.flatMap { table ->
            table.optionalReferences
                .flatMap { reference -> listOf(reference.referenceColumn) + reference.alsoNull }
                .map { ColumnRef(table.name, it) }
                .filter { columnOf(it)?.nullable != true }
        }
        assertEquals(emptyList(), invalid)
    }

    private val expectedTypes =
        mapOf(
            DateColumnType.DATE to "date",
            DateColumnType.TIMESTAMP_WITH_TIME_ZONE to "timestamp with time zone",
            DateColumnType.DATE_RANGE_END to "daterange",
            DateColumnType.FINITE_DATE_RANGE_END to "daterange",
        )

    private data class DateColumnRead(val ref: ColumnRef, val columnType: DateColumnType)

    private fun columnRead(table: HandledTable, source: DateSource): DateColumnRead? =
        when (source) {
            is DateSource.OwnColumn ->
                DateColumnRead(ColumnRef(table.name, source.column), source.columnType)
            is DateSource.GraphTableColumn ->
                DateColumnRead(ColumnRef(source.table, source.column), source.columnType)
            is DateSource.Custom -> null
        }

    @Test
    fun `every column a date source reads exists with the declared type`() {
        val invalid = handledTables.flatMap { table ->
            table.expirationRule.usedDateSources().mapNotNull { source ->
                val read = columnRead(table, source) ?: return@mapNotNull null
                val column = columnOf(read.ref) ?: return@mapNotNull "${read.ref} does not exist"
                val expected = expectedTypes.getValue(read.columnType)
                if (column.dataType != expected)
                    "${read.ref} is ${column.dataType} but read as ${read.columnType}"
                else null
            }
        }
        assertEquals(emptyList(), invalid)
    }

    /**
     * The end of a finite date range always exists, which the check constraint on the column
     * guarantees; the test below requires it
     */
    @Test
    fun `a column read as a finite date range end is not null and has a check constraint that forbids an open end`() {
        val invalid = handledTables.flatMap { table ->
            table.expirationRule.usedDateSources().mapNotNull { source ->
                val read = columnRead(table, source) ?: return@mapNotNull null
                if (read.columnType != DateColumnType.FINITE_DATE_RANGE_END) return@mapNotNull null
                val closedEnd = checkConstraints.any {
                    it.tableName == read.ref.tableName &&
                        read.ref.columnName in it.columns &&
                        "upper_inf" in it.definition
                }
                when {
                    columnOf(read.ref)?.nullable != false -> "${read.ref} is nullable"
                    !closedEnd -> "${read.ref} has no check constraint forbidding an open end"
                    else -> null
                }
            }
        }
        assertEquals(emptyList(), invalid)
    }

    /** Whether the source can give no date for some node, judged from the database schema */
    private fun mayHaveNoDate(table: HandledTable, source: DateSource): Boolean =
        when (source) {
            is DateSource.OwnColumn ->
                source.columnType == DateColumnType.DATE_RANGE_END ||
                    columnOf(ColumnRef(table.name, source.column))?.nullable != false
            is DateSource.GraphTableColumn ->
                source.columnType == DateColumnType.DATE_RANGE_END ||
                    columnOf(ColumnRef(source.table, source.column))?.nullable != false ||
                    !isRequiredAncestor(table, source.table)
            is DateSource.Custom -> source.mayReturnNoDate
        }

    /**
     * A table on the primary path reached through NOT NULL reference columns has a row whenever the
     * table has one, except `child`, whose row may be missing in rare cases.
     */
    private fun isRequiredAncestor(table: HandledTable, ancestor: String): Boolean {
        var current = table
        while (true) {
            val reference = schema.primaryReference(current) ?: return false
            if (columnOf(ColumnRef(current.name, reference.referenceColumn))?.nullable != false)
                return false
            if (reference.referencedTable == ancestor) return ancestor != CHILD_TABLE
            current = schema.tablesByName.getValue(reference.referencedTable) as HandledTable
        }
    }

    @Test
    fun `a rule whose date source may give no date has a fallback`() {
        val errors = handledTables.mapNotNull { table ->
            runCatching {
                table.expirationRule.validate(table.name) { mayHaveNoDate(table, it) }
            }
                .exceptionOrNull()
                ?.message
        }
        assertEquals(emptyList(), errors)
    }

    @Test
    fun `every custom date source and archived rule runs against the database`() {
        db.read { tx ->
            for (table in handledTables) {
                for (source in table.expirationRule.usedCustomDateSources()) {
                    assertEquals(emptyMap(), source.query(tx, emptyList()))
                }
                for (rule in table.expirationRule.usedArchivedIfRequiredRules()) {
                    assertEquals(emptySet(), rule.idsAwaitingArchival(tx, emptyList()))
                }
            }
        }
    }

    @Test
    fun `every id column and async job column exists, and job columns are text`() {
        val jobColumns = { table: Table ->
            (table as? HandledTable)?.asyncJobsPlannedOnDelete?.columnsToRead.orEmpty()
        }
        val missing = declaredTables.flatMap { table ->
            (table.identifiedByCols + jobColumns(table))
                .map { ColumnRef(table.name, it) }
                .filter { columnOf(it) == null }
        }
        assertEquals(emptyList(), missing)
        val notText = declaredTables.flatMap { table ->
            jobColumns(table)
                .map { ColumnRef(table.name, it) }
                .filter { columnOf(it)?.dataType != "text" }
        }
        assertEquals(emptyList(), notText)
    }

    @Test
    fun `the foreign keys not yet declared fail a run loudly, apart from the ones listed as cascading`() {
        fun action(ref: ColumnRef) = foreignKeys.first { it.ref == ref }.onDelete
        assertEquals(emptyList(), notYetDeclared.filter { action(it) !in setOf("a", "r") })
        assertEquals(emptyList(), notYetDeclaredCascading.filter { action(it) !in setOf("c", "n") })
    }

    @Test
    fun `the foreign keys handled by the database cascade or set null`() {
        val invalid =
            foreignKeys
                .filter { it.ref in handledByDatabase && it.onDelete !in setOf("c", "n") }
                .map { it.ref }
        assertEquals(emptyList(), invalid)
    }

    @Test
    fun `every child_id column has a foreign key to child or person`() {
        val childColumns = columns.map { it.ref }.filter { it.columnName == "child_id" }.toSet()
        val withForeignKey =
            foreignKeys.filter { it.referencesRef.tableName in childTargets }.map { it.ref }.toSet()
        assertEquals(emptySet(), childColumns - withForeignKey)
    }
}
