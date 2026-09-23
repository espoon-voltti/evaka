// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.AuditContext
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.getServiceNeedOptions
import evaka.core.shared.AreaId
import evaka.core.shared.McpAuthorizationId
import evaka.core.shared.McpTestDataBatchId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.NotFound
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

/** Everything a tool needs to do its work. Each tool call runs in its own database transaction. */
class McpToolContext(
    val tx: Database.Transaction,
    val user: AuthenticatedUser.Employee,
    val clock: EvakaClock,
    val authorizationId: McpAuthorizationId,
    val config: McpServerConfig,
    /** Collects the ids the tool touches for the audit log entry of the tool call */
    val audit: AuditContext,
) {
    val now
        get() = clock.now()

    val today
        get() = clock.today()

    fun batch(name: String): McpTestDataBatchId =
        McpTestDataService.getOrCreateBatch(tx, name, user.evakaUserId, authorizationId, now)

    fun track(
        batchId: McpTestDataBatchId,
        table: String,
        id: evaka.core.shared.Id<*>,
        description: String,
    ) = McpTestDataService.track(tx, batchId, table, id, description, now)

    fun trackAll(
        batchId: McpTestDataBatchId,
        table: String,
        ids: Collection<UUID>,
        description: String,
    ) = ids.forEach { tx.insertMcpTestDataEntity(batchId, table, it, description, now) }

    /**
     * Tracks rows that were created indirectly (by a `Dev*` insert helper or by service code) and
     * are found via a foreign key column, so that they count as part of the batch instead of
     * showing up as data created outside MCP in the deletion preview.
     */
    @IgnorableReturnValue
    fun trackRowsReferencing(
        batchId: McpTestDataBatchId,
        table: String,
        column: String,
        parentIds: Collection<UUID>,
        description: String,
    ): List<UUID> {
        if (parentIds.isEmpty()) return emptyList()
        val ids =
            tx.createQuery { sql("SELECT id FROM $table WHERE $column = ANY(${bind(parentIds)})") }
                .toList<UUID>()
        ids.forEach { tx.insertMcpTestDataEntity(batchId, table, it, description, now) }
        return ids
    }
}

class McpToolDefinition<T : Any>(
    val name: String,
    val description: String,
    val inputClass: KClass<T>,
    val readOnly: Boolean,
    val destructive: Boolean,
    val handler: (ctx: McpToolContext, input: T) -> Any,
) {
    val inputSchema: Map<String, Any> by lazy { McpToolSchema.forClass(inputClass) }

    fun call(ctx: McpToolContext, arguments: JsonNode?, jsonMapper: JsonMapper): Any {
        val node = arguments ?: jsonMapper.createObjectNode()
        val input =
            try {
                jsonMapper.treeToValue(node, inputClass.java)
            } catch (e: Exception) {
                throw BadRequest("Invalid arguments for tool $name: ${e.message}")
            }
        return handler(ctx, input)
    }
}

@Component
@Profile("enable_mcp")
class McpTools {
    data class ListTestDataInput(
        @McpDoc("If given, also lists the individual entities of this batch (your own batches)")
        val batchName: String? = null,
        @McpDoc("Number of entities to skip when listing the entities of a batch")
        val offset: Int = 0,
        @McpDoc("Maximum number of entities to list, 1-500") val limit: Int = 100,
    )

    data class DeleteTestDataInput(
        @McpDoc("Name of the batch to delete. Either batchName or batchId is required.")
        val batchName: String? = null,
        @McpDoc("Id of the batch to delete") val batchId: McpTestDataBatchId? = null,
        @McpDoc("If true, only reports what would be deleted without deleting anything")
        val dryRun: Boolean = false,
        @McpDoc(
            "Rows that depend on the batch but were not created by the MCP tools (e.g. placements a tester made in the UI for a test child) are reported as untrackedRowCounts and are only deleted if this is true. Use dryRun first and ask the user before setting this."
        )
        val allowUntrackedRows: Boolean = false,
    )

    val tools: List<McpToolDefinition<*>> =
        listOf(
            mcpTool<EmptyInput>(
                name = "get_environment_info",
                description =
                    "Returns today's date, the deployed eVaka commit (appCommit), care areas, service need options and your test data batches.",
                readOnly = true,
            ) { ctx, _ ->
                getEnvironmentInfo(ctx)
            },
            mcpTool<ListTestDataInput>(
                name = "list_test_data",
                description =
                    "Lists test data batches created via MCP (all users), with entity counts per type. Optionally lists the entities of one of your own batches, in pages.",
                readOnly = true,
            ) { ctx, input ->
                listTestData(ctx, input)
            },
            mcpTool<DeleteTestDataInput>(
                name = "delete_test_data",
                description =
                    "Deletes one of your own test data batches: every entity created in it and everything that depends on them (placements, applications, decisions, messages, ...). Refuses if the data is referenced by another batch, and refuses to delete rows that were not created via MCP unless allowUntrackedRows is true. Irreversible unless dryRun is true; always use dryRun first to see what would be deleted.",
                destructive = true,
            ) { ctx, input ->
                deleteTestData(ctx, input)
            },
        )

    fun findTool(name: String): McpToolDefinition<*>? = tools.find { it.name == name }

    private data class CareAreaInfo(
        val id: AreaId,
        val name: String,
        val shortName: String,
        val unitCount: Int,
    )

    private data class ServiceNeedOptionInfo(
        val id: ServiceNeedOptionId,
        val nameFi: String,
        val validPlacementType: PlacementType,
        val defaultOption: Boolean,
        val validFrom: LocalDate,
        val validTo: LocalDate?,
    )

    private fun getEnvironmentInfo(ctx: McpToolContext): Any {
        val careAreas =
            ctx.tx
                .createQuery {
                    sql(
                        """
SELECT ca.id, ca.name, ca.short_name, (SELECT count(*) FROM daycare d WHERE d.care_area_id = ca.id) AS unit_count
FROM care_area ca
ORDER BY ca.name
"""
                    )
                }
                .toList<CareAreaInfo>()
        val serviceNeedOptions =
            ctx.tx
                .getServiceNeedOptions()
                .filter { it.validTo == null || !it.validTo.isBefore(ctx.today) }
                .map {
                    ServiceNeedOptionInfo(
                        it.id,
                        it.nameFi,
                        it.validPlacementType,
                        it.defaultOption,
                        it.validFrom,
                        it.validTo,
                    )
                }
        val myBatches =
            McpTestDataService.getBatchSummaries(ctx.tx, createdBy = ctx.user.evakaUserId)
        return mapOf(
            "today" to ctx.today,
            "appCommit" to System.getenv("APP_COMMIT"),
            "employeeFrontendUrl" to "${ctx.config.baseUrl}/employee",
            "careAreas" to careAreas,
            "serviceNeedOptions" to serviceNeedOptions,
            "testDataBatches" to
                myBatches.map {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "createdBy" to it.createdByName,
                        "createdAt" to it.createdAt,
                        "entityCounts" to it.entityCounts,
                    )
                },
        )
    }

    private fun listTestData(ctx: McpToolContext, input: ListTestDataInput): Any {
        if (input.offset < 0) throw BadRequest("offset must not be negative")
        if (input.limit !in 1..500) throw BadRequest("limit must be between 1 and 500")
        val batches = McpTestDataService.getBatchSummaries(ctx.tx)
        val entityPage =
            input.batchName?.let { name ->
                val batch =
                    ctx.tx.getMcpTestDataBatchByName(name.trim(), ctx.user.evakaUserId)
                        ?: throw NotFound("You have no batch named '$name'")
                val entities = ctx.tx.getMcpTestDataEntities(batch.id)
                mapOf(
                    "totalEntities" to entities.size,
                    "offset" to input.offset,
                    "entities" to
                        entities.drop(input.offset).take(input.limit).map {
                            mapOf(
                                "table" to it.tableName,
                                "id" to it.entityId,
                                "description" to it.description,
                                "createdAt" to it.createdAt,
                            )
                        },
                )
            }
        return mapOf(
            "batches" to
                batches.map {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "createdBy" to it.createdByName,
                        "client" to it.clientName,
                        "createdAt" to it.createdAt,
                        "entityCounts" to it.entityCounts,
                        "totalEntities" to it.totalEntities,
                    )
                },
            "entityPage" to entityPage,
        )
    }

    private fun deleteTestData(ctx: McpToolContext, input: DeleteTestDataInput): Any {
        val batch =
            when {
                input.batchId != null ->
                    ctx.tx.getMcpTestDataBatch(input.batchId)
                        ?: throw NotFound("Batch ${input.batchId} not found")
                input.batchName != null ->
                    ctx.tx.getMcpTestDataBatchByName(input.batchName.trim(), ctx.user.evakaUserId)
                        ?: throw NotFound("You have no batch named '${input.batchName}'")
                else -> throw BadRequest("Either batchName or batchId is required")
            }
        if (batch.createdBy != ctx.user.evakaUserId)
            throw Forbidden(
                "Batch '${batch.name}' was created by ${batch.createdByName}. Only its creator can delete it via MCP; admins can delete any batch in the employee UI."
            )
        val result =
            if (input.dryRun) McpTestDataService.previewDeletion(ctx.tx, batch.id)
            else
                McpTestDataService.deleteBatch(
                    ctx.tx,
                    batch.id,
                    allowUntrackedRows = input.allowUntrackedRows,
                )
        ctx.audit.add(batch.id)
        return mapOf(
            "dryRun" to input.dryRun,
            "batchId" to result.batchId,
            "batchName" to result.batchName,
            "trackedEntities" to result.trackedEntities,
            "deletedRowCounts" to result.deletedRowCounts,
            "untrackedRowCounts" to result.untrackedRowCounts,
            "totalDeletedRows" to result.deletedRowCounts.values.sum(),
        )
    }
}
