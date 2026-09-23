// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.absence.application.AbsenceApplication
import evaka.core.serviceneed.ServiceNeedOption
import evaka.core.shared.DaycareId
import evaka.core.shared.EmployeeId
import evaka.core.shared.Id
import evaka.core.shared.McpTestDataBatchId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.auth.insertDaycareAclRow
import evaka.core.shared.dev.DevAbsence
import evaka.core.shared.dev.DevApplicationWithForm
import evaka.core.shared.dev.DevAssistanceAction
import evaka.core.shared.dev.DevAssistanceFactor
import evaka.core.shared.dev.DevBackupCare
import evaka.core.shared.dev.DevBackupPickup
import evaka.core.shared.dev.DevCalendarEvent
import evaka.core.shared.dev.DevCalendarEventAttendee
import evaka.core.shared.dev.DevCalendarEventTime
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildAttendance
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevClubTerm
import evaka.core.shared.dev.DevDailyServiceTimes
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareAssistance
import evaka.core.shared.dev.DevDaycareCaretaker
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupAcl
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevEmployeePin
import evaka.core.shared.dev.DevFamilyContact
import evaka.core.shared.dev.DevFeeAlteration
import evaka.core.shared.dev.DevFeeDecision
import evaka.core.shared.dev.DevFeeDecisionChild
import evaka.core.shared.dev.DevFosterParent
import evaka.core.shared.dev.DevFridgePartnership
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevHolidayPeriod
import evaka.core.shared.dev.DevHolidayQuestionnaire
import evaka.core.shared.dev.DevHolidayQuestionnaireAnswer
import evaka.core.shared.dev.DevIncome
import evaka.core.shared.dev.DevIncomeStatement
import evaka.core.shared.dev.DevInvoice
import evaka.core.shared.dev.DevOtherAssistanceMeasure
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPedagogicalDocument
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevPreschoolAssistance
import evaka.core.shared.dev.DevPreschoolTerm
import evaka.core.shared.dev.DevReservation
import evaka.core.shared.dev.DevServiceApplication
import evaka.core.shared.dev.DevServiceNeed
import evaka.core.shared.dev.DevStaffAttendance
import evaka.core.shared.dev.DevStaffAttendancePlan
import evaka.core.shared.dev.DevVoucherValueDecision
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertApplication
import evaka.core.shared.domain.BadRequest
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass
import org.jdbi.v3.core.JdbiException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class McpToolsRows(private val jsonMapper: JsonMapper) {
    data class RowGroup(
        @McpDoc("Row type, one of the types listed in the tool description") val type: String,
        val rows: List<JsonNode>,
    )

    data class InsertRowsInput(@McpDoc(BATCH_DOC) val batch: String, val rows: List<RowGroup>)

    data class DaycareAclRow(
        val daycareId: DaycareId,
        val employeeId: EmployeeId,
        val role: UserRole,
        val endDate: LocalDate? = null,
    )

    /** [table] is where the inserted row is tracked, null for link tables without a uuid id */
    class RowType(
        val clazz: KClass<*>,
        val table: String?,
        val insert: McpToolContext.(Any) -> Any?,
    )

    private inline fun <reified T : Any> row(
        type: String,
        table: String? = type,
        noinline insert: McpToolContext.(T) -> Any?,
    ) = type to RowType(T::class, table) { insert(it as T) }

    val rowTypes: Map<String, RowType> =
        mapOf(
            row<DevCareArea>("care_area") { tx.insert(it) },
            row<DevDaycare>("daycare") { tx.insert(it) },
            row<DevDaycareGroup>("daycare_group") { tx.insert(it) },
            row<DevDaycareCaretaker>("daycare_caretaker") { tx.insert(it) },
            row<DevEmployee>("employee") { tx.insert(it) },
            row<DaycareAclRow>("daycare_acl", null) {
                tx.insertDaycareAclRow(it.daycareId, it.employeeId, it.role, it.endDate)
            },
            row<DevDaycareGroupAcl>("daycare_group_acl", null) { tx.insert(it) },
            row<DevEmployeePin>("employee_pin") {
                requireTestEmployee(it.userId ?: throw BadRequest("userId is required"))
                tx.insert(it)
            },
            row<DevPerson>("adult", "person") { tx.insert(it, DevPersonType.ADULT) },
            row<DevPerson>("child", "person") { tx.insert(it, DevPersonType.CHILD) },
            row<DevGuardian>("guardian", null) { tx.insert(it) },
            row<DevParentship>("fridge_child") { tx.insert(it) },
            row<DevFridgePartnership>("fridge_partnership", null) { tx.insert(it) },
            row<DevFosterParent>("foster_parent") { tx.insert(it) },
            row<DevFamilyContact>("family_contact") { tx.insert(it) },
            row<DevBackupPickup>("backup_pickup") { tx.insert(it) },
            row<DevPlacement>("placement") { tx.insert(it) },
            row<DevServiceNeed>("service_need") { tx.insert(it) },
            row<DevDaycareGroupPlacement>("daycare_group_placement") { tx.insert(it) },
            row<DevBackupCare>("backup_care") { tx.insert(it) },
            row<DevDailyServiceTimes>("daily_service_times", "daily_service_time") {
                tx.insert(it)
            },
            row<DevReservation>("attendance_reservation") { tx.insert(it) },
            row<DevAbsence>("absence") { tx.insert(it) },
            row<DevChildAttendance>("child_attendance") { tx.insert(it) },
            row<AbsenceApplication>("absence_application") { tx.insert(it) },
            row<DevStaffAttendance>("staff_attendance_realtime") { tx.insert(it) },
            row<DevStaffAttendancePlan>("staff_attendance_plan") { tx.insert(it) },
            row<DevPreschoolTerm>("preschool_term") { tx.insert(it) },
            row<DevClubTerm>("club_term") { tx.insert(it) },
            row<DevHolidayPeriod>("holiday_period") {
                tx.insert(it)
                it.id
            },
            row<DevHolidayQuestionnaire>("holiday_period_questionnaire") {
                tx.insert(it)
                it.id
            },
            row<DevHolidayQuestionnaireAnswer>("holiday_questionnaire_answer") {
                tx.insert(it)
                it.id
            },
            row<ServiceNeedOption>("service_need_option") {
                tx.insert(it)
                it.id
            },
            row<DevServiceApplication>("service_application") { tx.insert(it) },
            row<DevDocumentTemplate>("document_template") { tx.insert(it) },
            row<DevChildDocument>("child_document") { tx.insert(it) },
            row<DevPedagogicalDocument>("pedagogical_document") { tx.insert(it) },
            row<DevAssistanceFactor>("assistance_factor") { tx.insert(it) },
            row<DevDaycareAssistance>("daycare_assistance") { tx.insert(it) },
            row<DevPreschoolAssistance>("preschool_assistance") { tx.insert(it) },
            row<DevOtherAssistanceMeasure>("other_assistance_measure") { tx.insert(it) },
            row<DevAssistanceAction>("assistance_action") { tx.insert(it) },
            row<DevCalendarEvent>("calendar_event") { tx.insert(it) },
            row<DevCalendarEventAttendee>("calendar_event_attendee") { tx.insert(it) },
            row<DevCalendarEventTime>("calendar_event_time") { tx.insert(it) },
            row<DevIncome>("income") { tx.insert(it) },
            row<DevIncomeStatement>("income_statement") { tx.insert(it) },
            row<DevFeeAlteration>("fee_alteration") { tx.insert(it) },
            row<DevFeeDecision>("fee_decision") { tx.insert(it) },
            row<DevFeeDecisionChild>("fee_decision_child") {
                tx.insert(it)
                it.id
            },
            row<DevVoucherValueDecision>("voucher_value_decision") { tx.insert(it) },
            row<DevInvoice>("invoice") { tx.insert(it) },
            row<DevApplicationWithForm>("application") { tx.insertApplication(it) },
        )

    fun tools(): List<McpToolDefinition<*>> =
        listOf(
            mcpTool<InsertRowsInput>(
                name = "insert_rows",
                description =
                    "Inserts rows into the database with the Kotlin test fixture classes and their Database.Transaction.insert functions in service/src/main/kotlin/evaka/core/shared/dev/. Each row is the JSON of its type's class (the same shape as in frontend/src/e2e-test/dev-api/fixtures.ts); see docs/db/schema.sql for constraints. Groups are inserted in the given order in one transaction: if any row fails, nothing is saved. Give ids yourself to rows that later rows reference. For more than a few dozen rows, write the arguments to a file and send it with create_upload_url. Types: " +
                        rowTypes.entries.joinToString {
                            "${it.key} (${it.value.clazz.simpleName})"
                        },
            ) { ctx, input ->
                insertRows(ctx, input)
            }
        )

    private fun insertRows(ctx: McpToolContext, input: InsertRowsInput): Any {
        val batchId = ctx.batch(input.batch)
        ctx.audit.add(batchId)
        val inserted = linkedMapOf<String, Int>()
        val insertedIds = mutableMapOf<String, MutableSet<UUID>>()
        input.rows.forEachIndexed { groupIndex, group ->
            val type =
                rowTypes[group.type]
                    ?: throw BadRequest(
                        "Unknown row type '${group.type}' in group $groupIndex. Types: ${rowTypes.keys.joinToString()}"
                    )
            val ids =
                group.rows.mapIndexed { index, json ->
                    try {
                        type.insert(ctx, jsonMapper.treeToValue(json, type.clazz.java))
                    } catch (e: Exception) {
                        val message = if (e is JdbiException) e.cause?.message else e.message
                        throw BadRequest(
                            "Row $index of group $groupIndex (${group.type}): $message"
                        )
                    }
                }
            inserted.merge(group.type, ids.size, Int::plus)
            if (type.table != null) {
                val rawIds = ids.map { if (it is Id<*>) it.raw else it as UUID }
                track(ctx, batchId, type.table, rawIds, group.type)
                insertedIds.getOrPut(type.table) { mutableSetOf() }.addAll(rawIds)
            }
        }
        insertedIds["person"]?.let { markVtjRelationsFetched(ctx, it) }
        return mapOf(
            "batch" to input.batch,
            "inserted" to inserted,
            "trackedDependentRows" to trackDependentRows(ctx, batchId, insertedIds),
        )
    }

    /**
     * Tracks the rows that insert helpers or triggers created on top of the inserted rows (e.g.
     * message accounts), found via the foreign keys that deleting the batch would follow. The
     * inserted ids are new in this transaction, so every row referencing them is new too.
     */
    private fun trackDependentRows(
        ctx: McpToolContext,
        batchId: McpTestDataBatchId,
        insertedIds: MutableMap<String, MutableSet<UUID>>,
    ): Map<String, Int> {
        val catalog = McpSchemaCatalog(ctx.tx)
        val counts = sortedMapOf<String, Int>()
        var parents: Map<String, List<UUID>> = insertedIds.mapValues { it.value.toList() }
        while (parents.isNotEmpty()) {
            val found = mutableMapOf<String, MutableSet<UUID>>()
            for ((table, ids) in parents) {
                for (fk in catalog.foreignKeysByParent[table].orEmpty()) {
                    if (
                        fk.onDelete != McpSchemaCatalog.OnDelete.DELETE_ROW ||
                            fk.childTable !in catalog.tablesWithUuidId
                    )
                        continue
                    requireIdentifier(fk.childTable)
                    requireIdentifier(fk.childColumn)
                    val known = insertedIds.getOrPut(fk.childTable) { mutableSetOf() }
                    ctx.tx
                        .createQuery {
                            sql(
                                "SELECT id FROM ${fk.childTable} WHERE ${fk.childColumn} = ANY(${bind(ids)})"
                            )
                        }
                        .toList<UUID>()
                        .filter { known.add(it) }
                        .forEach { found.getOrPut(fk.childTable) { mutableSetOf() }.add(it) }
                }
            }
            found.forEach { (table, ids) ->
                track(ctx, batchId, table, ids, "Created by the inserted rows")
                counts.merge(table, ids.size, Int::plus)
            }
            parents = found.mapValues { it.value.toList() }
        }
        return counts
    }

    private fun track(
        ctx: McpToolContext,
        batchId: McpTestDataBatchId,
        table: String,
        ids: Collection<UUID>,
        description: String,
    ) =
        ctx.tx.execute {
            sql(
                """
INSERT INTO mcp_test_data_entity (created_at, batch_id, table_name, entity_id, description)
SELECT ${bind(ctx.now)}, ${bind(batchId)}, ${bind(table)}, unnest(${bind(ids.toList())}), ${bind(description)}
ON CONFLICT (table_name, entity_id) DO NOTHING
"""
            )
        }

    /**
     * Generated test SSNs are unknown to the population register (VTJ), so the inserted relations
     * are marked as the fetched ones instead of eVaka refetching them from VTJ.
     */
    private fun markVtjRelationsFetched(ctx: McpToolContext, personIds: Collection<UUID>) =
        ctx.tx.execute {
            sql(
                """
UPDATE person SET vtj_guardians_queried = ${bind(ctx.now)}, vtj_dependants_queried = ${bind(ctx.now)}
WHERE id = ANY(${bind(personIds.toList())})
"""
            )
        }
}
