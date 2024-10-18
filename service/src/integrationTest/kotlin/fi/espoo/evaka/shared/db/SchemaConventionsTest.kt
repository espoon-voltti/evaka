// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.PureJdbiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jdbi.v3.core.mapper.Nested
import org.junit.jupiter.api.BeforeAll

class SchemaConventionsTest : PureJdbiTest(resetDbBeforeEach = false) {
    @Test
    fun `'timestamp without timezone' is forbidden and 'timestamp with timezone' should be used instead`() {
        val permittedViolations =
            setOf(
                ColumnRef("language_emphasis", "created"),
                ColumnRef("language_emphasis", "updated"),
                ColumnRef("pedagogical_document", "email_job_created_at"),
                ColumnRef("pedagogical_document_read", "read_at"),
            )
        val violations =
            columns.filter { it.dataType == "timestamp without time zone" }.map { it.ref }.toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `creation timestamp should be called 'created_at' instead of 'created'`() {
        val permittedViolations =
            setOf(
                "application",
                "application_form",
                "application_note",
                "application_other_guardian",
                "assistance_action",
                "assistance_action_option",
                "assistance_action_option_ref",
                "assistance_basis_option",
                "assistance_basis_option_ref",
                "assistance_factor",
                "assistance_need",
                "assistance_need_decision",
                "assistance_need_decision_guardian",
                "assistance_need_preschool_decision",
                "assistance_need_preschool_decision_guardian",
                "assistance_need_voucher_coefficient",
                "attachment",
                "attendance_reservation",
                "backup_care",
                "backup_curriculum_content",
                "backup_curriculum_document",
                "backup_curriculum_document_event",
                "backup_curriculum_template",
                "backup_messaging_blocklist",
                "care_area",
                "child_attendance",
                "child_daily_note",
                "child_document",
                "child_images",
                "child_sticky_note",
                "club_term",
                "daily_service_time",
                "daycare_acl",
                "daycare_assistance",
                "daycare_caretaker",
                "daycare",
                "daycare_group_acl",
                "daycare_group_placement",
                "decision",
                "document_template",
                "dvv_modification_token",
                "employee",
                "employee_pin",
                "fee_decision_child",
                "fee_decision",
                "fee_thresholds",
                "foster_parent",
                "group_note",
                "guardian_blocklist",
                "guardian",
                "holiday_period",
                "holiday_period_questionnaire",
                "holiday_questionnaire_answer",
                "income_notification",
                "income_statement",
                "invoice_correction",
                "koski_study_right",
                "language_emphasis",
                "message_account",
                "message_content",
                "message",
                "message_draft",
                "message_recipients",
                "message_thread_children",
                "message_thread",
                "message_thread_folder",
                "message_thread_participant",
                "mobile_device",
                "mobile_device_push_subscription",
                "other_assistance_measure",
                "pairing",
                "payment",
                "person",
                "placement",
                "placement_plan",
                "preschool_assistance",
                "preschool_term",
                "service_need",
                "service_need_option",
                "service_need_option_fee",
                "service_need_option_voucher_value",
                "setting",
                "staff_attendance",
                "staff_attendance_external",
                "staff_attendance_plan",
                "staff_attendance_realtime",
                "staff_occupancy_coefficient",
                "vapid_jwt",
                "varda_reset_child",
                "varda_service_need",
                "voucher_value_decision",
                "voucher_value_report_snapshot",
            )
        val violations =
            columns.filter { it.ref.columnName == "created" }.map { it.ref.tableName }.toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `update timestamp should be called 'updated_at' instead of 'updated'`() {
        val permittedViolations =
            setOf(
                "application",
                "application_form",
                "application_note",
                "application_other_guardian",
                "assistance_action",
                "assistance_action_option",
                "assistance_basis_option",
                "assistance_factor",
                "assistance_need",
                "assistance_need_decision",
                "assistance_need_preschool_decision",
                "assistance_need_voucher_coefficient",
                "attachment",
                "attendance_reservation",
                "backup_care",
                "backup_curriculum_content",
                "backup_curriculum_document",
                "backup_curriculum_document_event",
                "backup_curriculum_template",
                "backup_messaging_blocklist",
                "care_area",
                "child_attendance",
                "child_daily_note",
                "child_document",
                "child_images",
                "child_sticky_note",
                "club_term",
                "daily_service_time",
                "daycare",
                "daycare_acl",
                "daycare_assistance",
                "daycare_caretaker",
                "daycare_group_acl",
                "daycare_group_placement",
                "decision",
                "document_template",
                "employee",
                "employee_pin",
                "fee_decision",
                "fee_decision_child",
                "fee_thresholds",
                "foster_parent",
                "fridge_child",
                "fridge_partner",
                "group_note",
                "guardian_blocklist",
                "holiday_period",
                "holiday_period_questionnaire",
                "holiday_questionnaire_answer",
                "income_notification",
                "income_statement",
                "invoice_correction",
                "koski_study_right",
                "language_emphasis",
                "message",
                "message_account",
                "message_content",
                "message_draft",
                "message_recipients",
                "message_thread",
                "message_thread_children",
                "message_thread_folder",
                "message_thread_participant",
                "mobile_device",
                "mobile_device_push_subscription",
                "other_assistance_measure",
                "pairing",
                "payment",
                "person",
                "placement",
                "placement_plan",
                "preschool_assistance",
                "preschool_term",
                "service_need",
                "service_need_option",
                "service_need_option_fee",
                "service_need_option_voucher_value",
                "setting",
                "staff_attendance",
                "staff_attendance_external",
                "staff_attendance_plan",
                "staff_attendance_realtime",
                "staff_occupancy_coefficient",
                "vapid_jwt",
                "varda_reset_child",
                "varda_service_need",
                "voucher_value_decision",
            )
        val violations =
            columns.filter { it.ref.columnName == "updated" }.map { it.ref.tableName }.toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `modification timestamp should be called 'modified_at' instead of 'modified'`() {
        val permittedViolations =
            setOf(
                "assistance_factor",
                "daycare_assistance",
                "other_assistance_measure",
                "preschool_assistance",
            )
        val violations =
            columns.filter { it.ref.columnName == "modified" }.map { it.ref.tableName }.toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `creation timestamp should be 'timestamp with time zone' and NOT NULL`() {
        val permittedViolations =
            setOf(
                Column(
                    ColumnRef("backup_care", "created"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("backup_messaging_blocklist", "created"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("daycare_group_placement", "created"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("decision", "created"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("fridge_child", "created_at"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("guardian", "created"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("invoice", "created_at"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("language_emphasis", "created"),
                    "timestamp without time zone",
                    nullable = false,
                ),
                Column(
                    ColumnRef("mobile_device_push_group", "created_at"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("placement_plan", "created"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("staff_attendance", "created"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("voucher_value_decision", "created"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("voucher_value_report_snapshot", "created"),
                    "timestamp with time zone",
                    nullable = true,
                ),
            )
        val violations =
            columns
                .filter { it.ref.columnName == "created" || it.ref.columnName == "created_at" }
                .filterNot { it.dataType == "timestamp with time zone" && !it.nullable }
                .toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `update timestamp should be 'timestamp with time zone' and NOT NULL`() {
        val permittedViolations =
            setOf(
                Column(
                    ColumnRef("backup_care", "updated"),
                    dataType = "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("backup_messaging_blocklist", "updated"),
                    dataType = "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("daycare_group_placement", "updated"),
                    dataType = "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("decision", "updated"),
                    dataType = "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("fridge_child", "updated"),
                    dataType = "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("fridge_partner", "updated"),
                    dataType = "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("language_emphasis", "updated"),
                    dataType = "timestamp without time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("placement_plan", "updated"),
                    dataType = "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("staff_occupancy_coefficient", "updated"),
                    dataType = "timestamp with time zone",
                    nullable = true,
                ),
            )
        val violations =
            columns
                .filter { it.ref.columnName == "updated" || it.ref.columnName == "updated_at" }
                .filterNot { it.dataType == "timestamp with time zone" && !it.nullable }
                .toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `modification timestamp should be 'timestamp with time zone' and NOT NULL`() {
        val permittedViolations =
            setOf(
                Column(
                    ColumnRef("fridge_partner", "modified_at"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("fridge_child", "modified_at"),
                    "timestamp with time zone",
                    nullable = true,
                ),
                Column(
                    ColumnRef("placement", "modified_at"),
                    "timestamp with time zone",
                    nullable = true,
                ),
            )
        val violations =
            columns
                .filter { it.ref.columnName == "modified" || it.ref.columnName == "modified_at" }
                .filterNot { it.dataType == "timestamp with time zone" && !it.nullable }
                .toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `'created_by' column should be 'uuid' and NOT NULL`() {
        val permittedViolations =
            setOf(
                Column(ColumnRef("application", "created_by"), "uuid", nullable = true),
                Column(
                    ColumnRef("assistance_need_decision", "created_by"),
                    "uuid",
                    nullable = true,
                ),
                Column(
                    ColumnRef("assistance_need_preschool_decision", "created_by"),
                    "uuid",
                    nullable = true,
                ),
                Column(ColumnRef("child_document", "created_by"), "uuid", nullable = true),
                Column(ColumnRef("fridge_partner", "created_by"), "uuid", nullable = true),
            )
        val violations =
            columns
                .filter { it.ref.columnName == "created_by" }
                .filterNot { it.dataType == "uuid" && !it.nullable }
                .toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `'updated_by' column should not exist and modified_at+modified_by pair should be used instead for user-visible modification timestamps`() {
        val permittedViolations =
            setOf(
                ColumnRef("application_note", "updated_by"),
                ColumnRef("assistance_action", "updated_by"),
                ColumnRef("assistance_need", "updated_by"),
                ColumnRef("fee_alteration", "updated_by"),
                ColumnRef("income", "updated_by"),
            )
        val violations = columns.filter { it.ref.columnName == "updated_by" }.map { it.ref }.toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `'modified_by' column should be 'uuid' and NOT NULL`() {
        val permittedViolations =
            setOf(
                Column(
                    ColumnRef("assistance_need_voucher_coefficient", "modified_by"),
                    "uuid",
                    nullable = true,
                ),
                Column(ColumnRef("fridge_partner", "modified_by"), "uuid", nullable = true),
                Column(
                    ColumnRef("holiday_questionnaire_answer", "modified_by"),
                    "uuid",
                    nullable = true,
                ),
                Column(ColumnRef("placement", "modified_by"), "uuid", nullable = true),
            )
        val violations =
            columns
                .filter { it.ref.columnName == "modified_by" }
                .filterNot { it.dataType == "uuid" && !it.nullable }
                .toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `'created_by' column should have a foreign key to evaka_user`() {
        val permittedViolations =
            setOf(
                ColumnsRef("assistance_need_decision", "created_by") to
                    ColumnsRef("employee", "id"),
                ColumnsRef("assistance_need_preschool_decision", "created_by") to
                    ColumnsRef("employee", "id"),
                ColumnsRef("child_document", "created_by") to ColumnsRef("employee", "id"),
            )
        val violations =
            columns
                .filter { it.ref.columnName == "created_by" }
                .map { column ->
                    val columnsRef = column.ref.toColumnsRef()
                    Pair(
                        columnsRef,
                        foreignKeys.singleOrNull { it.ref == columnsRef }?.referencesRef,
                    )
                }
                .filterNot { (_, referencesRef) -> referencesRef == ColumnsRef("evaka_user", "id") }
                .toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `'modified_by' column should have a foreign key to evaka_user`() {
        val permittedViolations = setOf<Pair<ColumnsRef, ColumnsRef?>>()
        val violations =
            columns
                .filter { it.ref.columnName == "modified_by" }
                .map { column ->
                    val columnsRef = column.ref.toColumnsRef()
                    Pair(
                        columnsRef,
                        foreignKeys.singleOrNull { it.ref == columnsRef }?.referencesRef,
                    )
                }
                .filterNot { (_, referencesRef) -> referencesRef == ColumnsRef("evaka_user", "id") }
                .toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `every column with a foreign key should have at least one index that has the column as the first key`() {
        val permittedViolations =
            setOf(
                ColumnRef("archived_process_history", "entered_by"),
                ColumnRef("assistance_action_option_ref", "option_id"),
                ColumnRef("assistance_basis_option_ref", "option_id"),
                ColumnRef("assistance_factor", "modified_by"),
                ColumnRef("child_document", "content_modified_by"),
                ColumnRef("child_document_read", "person_id"),
                ColumnRef("daycare", "finance_decision_handler"),
                ColumnRef("daycare", "language_emphasis_id"),
                ColumnRef("daycare_assistance", "modified_by"),
                ColumnRef("decision", "resolved_by"),
                ColumnRef("fridge_child", "created_by_application"),
                ColumnRef("fridge_child", "created_by_user"),
                ColumnRef("fridge_child", "modified_by_user"),
                ColumnRef("holiday_questionnaire_answer", "modified_by"),
                ColumnRef("invoice_correction", "unit_id"),
                ColumnRef("message", "replies_to"),
                ColumnRef("message_thread_participant", "folder_id"),
                ColumnRef("other_assistance_measure", "modified_by"),
                ColumnRef("pairing", "employee_id"),
                ColumnRef("pairing", "mobile_device_id"),
                ColumnRef("pairing", "unit_id"),
                ColumnRef("payment", "sent_by"),
                ColumnRef("placement", "modified_by"),
                ColumnRef("placement", "terminated_by"),
                ColumnRef("preschool_assistance", "modified_by"),
            )
        val violations =
            foreignKeys
                .mapNotNull { it.ref.toColumnRef() }
                .filterNot { col ->
                    indices.any {
                        it.tableName == col.tableName && it.keys.firstOrNull() == col.columnName
                    }
                }
                .toSet()
        assertEquals(permittedViolations, violations)
    }

    @Test
    fun `every daterange and datemultirange column should have a constraint that limits its bound(s)`() {
        val permittedViolations =
            setOf(
                ColumnRef("assistance_need_decision", "validity_period"),
                ColumnRef("assistance_need_voucher_coefficient", "validity_period"),
                ColumnRef("backup_curriculum_template", "valid"),
                ColumnRef("calendar_event", "period"),
                ColumnRef("daily_service_time", "validity_period"),
                ColumnRef("daycare", "club_apply_period"),
                ColumnRef("daycare", "daycare_apply_period"),
                ColumnRef("daycare", "preschool_apply_period"),
                ColumnRef("fee_decision", "valid_during"),
                ColumnRef("fee_thresholds", "valid_during"),
                ColumnRef("foster_parent", "valid_during"),
                ColumnRef("holiday_period", "period"),
                ColumnRef("holiday_period_questionnaire", "active"),
                ColumnRef("holiday_period_questionnaire", "condition_continuous_placement"),
                ColumnRef("holiday_questionnaire_answer", "fixed_period"),
                ColumnRef("invoice_correction", "period"),
                ColumnRef("payment", "period"),
                ColumnRef("service_need_option_fee", "validity"),
                ColumnRef("service_need_option_voucher_value", "validity"),
                ColumnRef("voucher_value_report_decision", "realized_period"),
            )

        val dateRangeCheck = { columnName: String -> "CHECK ((NOT lower_inf($columnName)))" }
        val finiteDateRangeCheck = { columnName: String ->
            "CHECK ((NOT (lower_inf($columnName) OR upper_inf($columnName))))"
        }

        val violations =
            columns
                .filter { it.dataType == "daterange" || it.dataType == "datemultirange" }
                .filterNot { column ->
                    checkConstraints
                        .filter { it.ref == column.ref }
                        .any { constraint ->
                            val columnName = column.ref.columnName
                            if (column.dataType == "datemultirange") {
                                // datemultirange maps to DateSet which must be finite
                                constraint.checkClause == finiteDateRangeCheck(columnName)
                            } else {
                                constraint.checkClause == dateRangeCheck(columnName) ||
                                    constraint.checkClause == finiteDateRangeCheck(columnName)
                            }
                        }
                }
                .map { it.ref }
                .toSet()
        assertEquals(permittedViolations, violations)
    }

    data class ColumnRef(val tableName: String, val columnName: String) {
        fun toColumnsRef() = ColumnsRef(tableName, listOf(columnName))
    }

    data class ColumnsRef(val tableName: String, val columnNames: List<String>) {
        constructor(
            tableName: String,
            vararg columnNames: String,
        ) : this(tableName, columnNames.toList())

        fun toColumnRef(): ColumnRef? = columnNames.singleOrNull()?.let { ColumnRef(tableName, it) }
    }

    data class Column(@Nested val ref: ColumnRef, val dataType: String, val nullable: Boolean)

    data class ForeignKey(
        @Nested val ref: ColumnsRef,
        @Nested("references_") val referencesRef: ColumnsRef,
        val constraintName: String,
    )

    data class Index(
        val tableName: String,
        val indexName: String,
        val keys: List<String>,
        val accessMethod: String,
        val primary: Boolean,
        val exclude: Boolean,
        val unique: Boolean,
        val predicate: String?,
        val definition: String,
    )

    data class CheckConstraint(
        @Nested val ref: ColumnRef,
        val constraintName: String,
        val checkClause: String,
    )

    private lateinit var columns: List<Column>

    private lateinit var foreignKeys: List<ForeignKey>

    private lateinit var indices: List<Index>

    private lateinit var checkConstraints: List<CheckConstraint>

    @BeforeAll
    override fun beforeAll() {
        super.beforeAll()
        db.read { tx ->
            columns =
                tx.createQuery {
                        sql(
                            """
SELECT
  table_name,
  column_name,
  CASE data_type
    WHEN 'USER-DEFINED' THEN udt_name
    WHEN 'ARRAY' THEN trim(LEADING '_' FROM udt_name) || '[]'
    ELSE data_type
  END AS data_type,
  is_nullable AS nullable
FROM information_schema.columns
LEFT JOIN information_schema.tables t USING (table_name)
WHERE t.table_schema = 'public'
AND t.table_name != 'flyway_schema_history'
AND t.table_type != 'VIEW'
"""
                        )
                    }
                    .toList<Column>()
            foreignKeys =
                tx.createQuery {
                        sql(
                            """
SELECT
  tc.table_name,
  src_attrs.columns AS column_names,
  dst_cls.relname AS references_table_name,
  ref_attrs.columns AS references_column_names,
  tc.constraint_name
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.tables t USING (table_name)
LEFT JOIN pg_class src_cls ON src_cls.relname = tc.table_name
AND src_cls.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')
LEFT JOIN pg_constraint c ON conname = tc.constraint_name AND conrelid = src_cls.oid
LEFT JOIN pg_class dst_cls ON dst_cls.oid = c.confrelid
JOIN LATERAL (
    SELECT array_agg(a.attname ORDER BY key_order) AS columns
    FROM unnest(c.conkey) WITH ORDINALITY AS t(key_att, key_order)
    JOIN pg_attribute a ON key_att = a.attnum AND attrelid = c.conrelid
) src_attrs ON true
JOIN LATERAL (
    SELECT array_agg(a.attname ORDER BY key_order) AS columns
    FROM unnest(c.confkey) WITH ORDINALITY AS t(key_att, key_order)
    JOIN pg_attribute a ON key_att = a.attnum AND attrelid = c.confrelid
) ref_attrs ON true
WHERE tc.constraint_type = 'FOREIGN KEY'
AND t.table_schema = 'public'
AND t.table_name != 'flyway_schema_history'
AND t.table_type != 'VIEW'
"""
                        )
                    }
                    .toList<ForeignKey>()
            indices =
                tx.createQuery {
                        sql(
                            """
SELECT
    tbl_cls.relname AS table_name,
    ind_cls.relname AS index_name,
    keys,
    access_method,
    i.indisprimary AS primary,
    i.indisexclusion AS exclude,
    i.indisunique AS unique,
    pg_get_expr(i.indpred, i.indrelid, true) AS predicate,
    pg_get_indexdef(i.indexrelid) AS definition
FROM pg_index i
JOIN pg_class ind_cls ON ind_cls.oid = i.indexrelid
JOIN pg_class tbl_cls ON tbl_cls.oid = i.indrelid
JOIN LATERAL (
    SELECT array_agg(coalesce(a.attname, pg_get_indexdef(i.indexrelid, key_order::int, true)) ORDER BY key_order) AS keys,
      array_agg(DISTINCT am.amname) AS access_method
    FROM unnest(i.indkey) WITH ORDINALITY AS t(key_att, key_order)
    LEFT JOIN pg_attribute a ON a.attnum != 0 AND key_att = a.attnum AND attrelid = i.indrelid
    LEFT JOIN pg_opclass opc ON opc.oid = i.indclass[key_order - 1] AND opc.oid != 0
    LEFT JOIN pg_am am ON am.oid = opc.opcmethod
) attrs ON true
WHERE tbl_cls.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')
AND tbl_cls.relname != 'flyway_schema_history'
"""
                        )
                    }
                    .toList<Index>()
            checkConstraints =
                tx.createQuery {
                        sql(
                            """
SELECT
    con.conrelid::regclass AS table_name,
    a.attname AS column_name,
    con.conname AS constraint_name,
    pg_get_constraintdef(con.oid) AS check_clause
FROM pg_constraint con
JOIN pg_attribute a ON a.attnum = ANY(con.conkey) AND a.attrelid = con.conrelid
WHERE con.contype = 'c'
"""
                        )
                    }
                    .toList<CheckConstraint>()
        }
    }
}
