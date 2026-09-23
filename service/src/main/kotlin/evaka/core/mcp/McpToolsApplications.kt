// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.application.ApplicationDetails
import evaka.core.application.ApplicationStateService
import evaka.core.application.ApplicationStatus
import evaka.core.application.DaycarePlacementPlan
import evaka.core.application.SimpleApplicationAction
import evaka.core.application.fetchApplicationDetails
import evaka.core.decision.DecisionStatus
import evaka.core.decision.getDecisionsByApplication
import evaka.core.messaging.MessageRecipient
import evaka.core.messaging.MessageService
import evaka.core.messaging.MessageType
import evaka.core.messaging.NewMessageStub
import evaka.core.messaging.upsertEmployeeMessageAccount
import evaka.core.messaging.upsertRecipientThreadParticipants
import evaka.core.pis.getEmployeeRoles
import evaka.core.placement.PlacementPlanConfirmationStatus
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.EmployeeId
import evaka.core.shared.McpTestDataBatchId
import evaka.core.shared.MessageThreadId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate
import java.util.UUID

class McpToolsApplications(
    private val applicationStateService: ApplicationStateService,
    private val messageService: MessageService,
) {
    enum class ApplicationAction {
        MOVE_TO_WAITING_PLACEMENT,
        RETURN_TO_SENT,
        CREATE_PLACEMENT_PLAN,
        CANCEL_PLACEMENT_PLAN,
        SEND_DECISIONS_WITHOUT_PROPOSAL,
        SEND_PLACEMENT_PROPOSAL,
        ACCEPT_PLACEMENT_PROPOSAL,
        CONFIRM_DECISION_MAILED,
        ACCEPT_DECISIONS,
        REJECT_DECISIONS,
    }

    data class AdvanceApplicationInput(
        @McpDoc("Application created via MCP") val applicationId: ApplicationId,
        @McpDoc(
            "Lifecycle step. Typical daycare flow: MOVE_TO_WAITING_PLACEMENT -> CREATE_PLACEMENT_PLAN -> SEND_DECISIONS_WITHOUT_PROPOSAL (or SEND_PLACEMENT_PROPOSAL -> ACCEPT_PLACEMENT_PROPOSAL) -> CONFIRM_DECISION_MAILED -> ACCEPT_DECISIONS. Every step is done as the authorized admin, like an employee in the UI; ACCEPT_DECISIONS / REJECT_DECISIONS are recorded as done by the admin on the guardian's behalf. ACCEPT_PLACEMENT_PROPOSAL confirms all pending proposals of the unit, like the UI, and refuses if any of them belong to applications not created via MCP."
        )
        val action: ApplicationAction,
        @McpDoc(
            "CREATE_PLACEMENT_PLAN: unit to place the child in. Defaults to the first preferred unit."
        )
        val unitId: DaycareId? = null,
        @McpDoc(
            "CREATE_PLACEMENT_PLAN: placement start. Defaults to the preferred start date. ACCEPT_DECISIONS: requested start date."
        )
        val startDate: LocalDate? = null,
        @McpDoc(
            "CREATE_PLACEMENT_PLAN: placement end. Defaults to ~1 year after start (end of July)."
        )
        val endDate: LocalDate? = null,
    )

    data class SendMessageInput(
        @McpDoc(BATCH_DOC) val batch: String,
        @McpDoc(
            "Employee (created via MCP) whose personal message account sends the message. The employee must have a unit role (e.g. UNIT_SUPERVISOR) in the children's unit."
        )
        val senderEmployeeId: EmployeeId,
        @McpDoc("Children whose guardians receive the message") val childIds: List<ChildId>,
        val title: String,
        val content: String,
        @McpDoc("MESSAGE can be replied to, BULLETIN cannot")
        val type: MessageType = MessageType.MESSAGE,
        val urgent: Boolean = false,
        val sensitive: Boolean = false,
    )

    fun tools(): List<McpToolDefinition<*>> =
        listOf(
            mcpTool<AdvanceApplicationInput>(
                name = "advance_application",
                description =
                    "Moves an application forward in its lifecycle: verification, placement plan, decisions, placement proposals and the guardian's acceptance. Runs the same business logic as the UI, so decisions and placements are created for real (decision PDFs are generated asynchronously). Only works for applications created via MCP.",
            ) { ctx, input ->
                advance(ctx, input)
            },
            mcpTool<SendMessageInput>(
                name = "send_message",
                description =
                    "Sends a message from a test employee's personal message account to the guardians of the given children, creating message threads visible in the employee and citizen UIs. The sender must be an employee created via MCP.",
            ) { ctx, input ->
                sendMessage(ctx, input)
            },
        )

    /**
     * Confirming a placement proposal is a unit-level operation in eVaka: it finalizes every
     * accepted proposal in the unit and returns every rejected one to placement. Refuse if that
     * would touch applications not created via MCP, so a test scenario never decides real
     * applications waiting in the same unit.
     */
    private fun requireNoForeignPendingProposals(
        ctx: McpToolContext,
        unitId: DaycareId,
        applicationId: ApplicationId,
    ) {
        val foreignApplicationIds =
            ctx.tx
                .createQuery {
                    sql(
                        """
SELECT pp.application_id
FROM placement_plan pp
JOIN application a ON a.id = pp.application_id
WHERE pp.unit_id = ${bind(unitId)}
  AND pp.deleted = false
  AND pp.application_id != ${bind(applicationId)}
  AND (
    pp.unit_confirmation_status = 'REJECTED_NOT_CONFIRMED'
    OR (pp.unit_confirmation_status = 'ACCEPTED' AND a.status = 'WAITING_UNIT_CONFIRMATION')
  )
  AND NOT EXISTS (
    SELECT FROM mcp_test_data_entity e WHERE e.table_name = 'application' AND e.entity_id = a.id
  )
"""
                    )
                }
                .toList<ApplicationId>()
        if (foreignApplicationIds.isNotEmpty())
            throw Conflict(
                "Confirming the placement proposal would also finalize ${foreignApplicationIds.size} other pending proposal(s) in the same unit that were not created via MCP (applications ${foreignApplicationIds.joinToString()}). Handle them in the employee UI first, or use a unit with no other pending proposals."
            )
    }

    /**
     * Rows the application workflow creates for the application and its child. They are tracked in
     * the application's batch as they appear, so that the batch deletion preview does not report
     * the workflow's own rows as data created outside MCP.
     */
    private class WorkflowRows(
        val placementPlans: Set<UUID>,
        val decisions: Set<UUID>,
        val placements: Set<UUID>,
    ) {
        companion object {
            fun load(ctx: McpToolContext, application: ApplicationDetails): WorkflowRows {
                fun ids(table: String, column: String, id: UUID): Set<UUID> =
                    ctx.tx
                        .createQuery { sql("SELECT id FROM $table WHERE $column = ${bind(id)}") }
                        .toSet<UUID>()
                return WorkflowRows(
                    placementPlans = ids("placement_plan", "application_id", application.id.raw),
                    decisions = ids("decision", "application_id", application.id.raw),
                    placements = ids("placement", "child_id", application.childId.raw),
                )
            }
        }

        fun trackNewRowsSince(
            before: WorkflowRows,
            ctx: McpToolContext,
            batchId: McpTestDataBatchId,
        ) {
            ctx.trackAll(
                batchId,
                "placement_plan",
                placementPlans - before.placementPlans,
                "Placement plan",
            )
            ctx.trackAll(batchId, "decision", decisions - before.decisions, "Decision")
            val newPlacements = placements - before.placements
            ctx.trackAll(batchId, "placement", newPlacements, "Placement from decision")
            ctx.trackRowsReferencing(
                batchId,
                "service_need",
                "placement_id",
                newPlacements,
                "Service need from decision",
            )
            ctx.trackRowsReferencing(
                batchId,
                "daycare_group_placement",
                "daycare_placement_id",
                newPlacements,
                "Group placement from decision",
            )
        }
    }

    private fun advance(ctx: McpToolContext, input: AdvanceApplicationInput): Any {
        val batchId = ctx.requireTestApplication(input.applicationId)
        val application =
            ctx.tx.fetchApplicationDetails(input.applicationId)
                ?: throw NotFound("Application ${input.applicationId} not found")
        val audit = ctx.audit
        val rowsBefore = WorkflowRows.load(ctx, application)
        when (input.action) {
            ApplicationAction.MOVE_TO_WAITING_PLACEMENT ->
                applicationStateService.doSimpleAction(
                    ctx.tx,
                    ctx.user,
                    ctx.clock,
                    audit,
                    SimpleApplicationAction.MOVE_TO_WAITING_PLACEMENT,
                    application.id,
                )
            ApplicationAction.RETURN_TO_SENT ->
                applicationStateService.doSimpleAction(
                    ctx.tx,
                    ctx.user,
                    ctx.clock,
                    audit,
                    SimpleApplicationAction.RETURN_TO_SENT,
                    application.id,
                )
            ApplicationAction.CANCEL_PLACEMENT_PLAN ->
                applicationStateService.doSimpleAction(
                    ctx.tx,
                    ctx.user,
                    ctx.clock,
                    audit,
                    SimpleApplicationAction.CANCEL_PLACEMENT_PLAN,
                    application.id,
                )
            ApplicationAction.SEND_DECISIONS_WITHOUT_PROPOSAL ->
                try {
                    applicationStateService.doSimpleAction(
                        ctx.tx,
                        ctx.user,
                        ctx.clock,
                        audit,
                        SimpleApplicationAction.SEND_DECISIONS_WITHOUT_PROPOSAL,
                        application.id,
                    )
                } catch (e: Conflict) {
                    throw BadRequest(
                        "Could not send decisions: ${e.message}. If decision reasoning texts are missing in this environment, an admin must add them under 'Päätösten perustelut' in the employee UI first."
                    )
                }
            ApplicationAction.SEND_PLACEMENT_PROPOSAL ->
                applicationStateService.doSimpleAction(
                    ctx.tx,
                    ctx.user,
                    ctx.clock,
                    audit,
                    SimpleApplicationAction.SEND_PLACEMENT_PROPOSAL,
                    application.id,
                )
            ApplicationAction.CONFIRM_DECISION_MAILED ->
                applicationStateService.doSimpleAction(
                    ctx.tx,
                    ctx.user,
                    ctx.clock,
                    audit,
                    SimpleApplicationAction.CONFIRM_DECISION_MAILED,
                    application.id,
                )
            ApplicationAction.CREATE_PLACEMENT_PLAN -> {
                if (application.status != ApplicationStatus.WAITING_PLACEMENT)
                    throw BadRequest(
                        "Application must be in WAITING_PLACEMENT status (now ${application.status})"
                    )
                if (!application.checkedByAdmin || application.confidential == null) {
                    // Same as the service worker ticking "checked" and setting confidentiality in
                    // the UI
                    applicationStateService.setVerified(
                        ctx.tx,
                        ctx.user,
                        ctx.clock,
                        audit,
                        application.id,
                        confidential = if (application.confidential == null) false else null,
                    )
                }
                val unitId =
                    input.unitId
                        ?: application.form.preferences.preferredUnits.firstOrNull()?.id
                        ?: throw BadRequest("Application has no preferred units; give unitId")
                val start =
                    input.startDate
                        ?: application.form.preferences.preferredStartDate
                        ?: throw BadRequest(
                            "Application has no preferred start date; give startDate"
                        )
                val end =
                    input.endDate
                        ?: LocalDate.of(
                            if (start.monthValue >= 8) start.year + 1 else start.year,
                            7,
                            31,
                        )
                if (end.isBefore(start)) throw BadRequest("endDate must not be before startDate")
                applicationStateService.createPlacementPlan(
                    ctx.tx,
                    ctx.user,
                    ctx.clock,
                    audit,
                    application.id,
                    DaycarePlacementPlan(unitId = unitId, period = FiniteDateRange(start, end)),
                )
            }
            ApplicationAction.ACCEPT_PLACEMENT_PROPOSAL -> {
                applicationStateService.respondToPlacementProposal(
                    ctx.tx,
                    ctx.user,
                    ctx.clock,
                    audit,
                    application.id,
                    PlacementPlanConfirmationStatus.ACCEPTED,
                )
                val unitId =
                    ctx.tx
                        .createQuery {
                            sql(
                                "SELECT unit_id FROM placement_plan WHERE application_id = ${bind(application.id)} AND deleted = false"
                            )
                        }
                        .exactlyOneOrNull<DaycareId>()
                        ?: throw BadRequest("Application has no placement plan")
                requireNoForeignPendingProposals(ctx, unitId, application.id)
                applicationStateService.confirmPlacementProposalChanges(
                    ctx.tx,
                    ctx.user,
                    ctx.clock,
                    audit,
                    unitId,
                    emptyMap(),
                )
            }
            ApplicationAction.ACCEPT_DECISIONS,
            ApplicationAction.REJECT_DECISIONS -> {
                val decisions =
                    ctx.tx
                        .getDecisionsByApplication(application.id, AccessControlFilter.PermitAll)
                        .filter { it.status == DecisionStatus.PENDING }
                        .sortedBy { it.type.ordinal }
                if (decisions.isEmpty()) throw BadRequest("Application has no pending decisions")
                decisions.forEach { decision ->
                    if (input.action == ApplicationAction.ACCEPT_DECISIONS) {
                        applicationStateService.acceptDecision(
                            ctx.tx,
                            ctx.user,
                            ctx.clock,
                            audit,
                            application.id,
                            decision.id,
                            requestedStartDate = input.startDate ?: decision.startDate,
                            allowUnmailedDecisions = true,
                        )
                    } else {
                        applicationStateService.rejectDecision(
                            ctx.tx,
                            ctx.user,
                            ctx.clock,
                            audit,
                            application.id,
                            decision.id,
                        )
                    }
                }
            }
        }
        WorkflowRows.load(ctx, application).trackNewRowsSince(rowsBefore, ctx, batchId)
        val updated = ctx.tx.fetchApplicationDetails(input.applicationId)!!
        val decisions =
            ctx.tx.getDecisionsByApplication(application.id, AccessControlFilter.PermitAll).map {
                mapOf(
                    "decisionId" to it.id,
                    "type" to it.type,
                    "status" to it.status,
                    "unit" to it.unit.name,
                    "period" to "${it.startDate} – ${it.endDate}",
                )
            }
        return mapOf(
            "applicationId" to input.applicationId,
            "action" to input.action,
            "status" to updated.status,
            "decisions" to decisions,
            "applicationUrl" to
                "${ctx.config.baseUrl}/employee/applications/${input.applicationId}",
        )
    }

    private fun sendMessage(ctx: McpToolContext, input: SendMessageInput): Any {
        val batchId = ctx.batch(input.batch)
        val senderName = ctx.requireTestEmployee(input.senderEmployeeId)
        if (input.childIds.isEmpty()) throw BadRequest("At least one child is required")
        input.childIds.forEach { ctx.requireChild(it) }
        val senderAccount = ctx.tx.upsertEmployeeMessageAccount(input.senderEmployeeId)
        val senderUser =
            AuthenticatedUser.Employee(
                input.senderEmployeeId,
                ctx.tx.getEmployeeRoles(input.senderEmployeeId),
            )
        val (contentId, recipientCount) =
            messageService.sendMessageAsEmployee(
                tx = ctx.tx,
                user = senderUser,
                now = ctx.now,
                sender = senderAccount,
                type = input.type,
                msg =
                    NewMessageStub(
                        title = input.title,
                        content = input.content,
                        urgent = input.urgent,
                        sensitive = input.sensitive,
                    ),
                recipients = input.childIds.map { MessageRecipient.Child(it) }.toSet(),
                recipientNames = emptyList(),
                attachments = emptySet(),
                relatedApplication = null,
                filters = null,
            )
        if (contentId == null || recipientCount == 0)
            throw BadRequest(
                "No recipients found. The sender ($senderName) must have a unit role in the children's placement unit, and the children must have active placements."
            )
        val threadIds =
            ctx.tx
                .createQuery {
                    sql(
                        "SELECT DISTINCT thread_id FROM message WHERE content_id = ${bind(contentId)}"
                    )
                }
                .toList<MessageThreadId>()
        threadIds.forEach {
            ctx.track(batchId, "message_thread", it, "Message thread '${input.title}'")
        }
        ctx.track(
            batchId,
            "message_content",
            contentId,
            "Message '${input.title}' from $senderName",
        )
        val threadIdsRaw = threadIds.map { it.raw }
        val messageIds =
            ctx.trackRowsReferencing(
                batchId,
                "message",
                "content_id",
                listOf(contentId.raw),
                "Message '${input.title}'",
            )
        ctx.trackRowsReferencing(
            batchId,
            "message_recipients",
            "message_id",
            messageIds,
            "Message recipient",
        )
        // The recipients would be added to the threads only by the async job delivering the
        // message, after the threads have been tracked. The job's upsert keeps these rows.
        ctx.tx.upsertRecipientThreadParticipants(contentId, ctx.now)
        ctx.trackRowsReferencing(
            batchId,
            "message_thread_participant",
            "thread_id",
            threadIdsRaw,
            "Message thread participant",
        )
        ctx.trackRowsReferencing(
            batchId,
            "message_thread_children",
            "thread_id",
            threadIdsRaw,
            "Message thread child",
        )
        // Messaging creates the message accounts of the participants on demand. The accounts of
        // participants that belong to this batch go with it; other participants' accounts (a real
        // guardian's) are left alone, because deleting one would take their message history too.
        val batchMemberAccountIds =
            ctx.tx
                .createQuery {
                    sql(
                        """
SELECT DISTINCT ma.id
FROM message_thread_participant p
JOIN message_account ma ON ma.id = p.participant_id
JOIN mcp_test_data_entity e ON e.batch_id = ${bind(batchId)}
  AND ((e.table_name = 'person' AND e.entity_id = ma.person_id)
    OR (e.table_name = 'employee' AND e.entity_id = ma.employee_id))
WHERE p.thread_id = ANY(${bind(threadIdsRaw)})
"""
                    )
                }
                .toList<UUID>()
        ctx.trackAll(batchId, "message_account", batchMemberAccountIds, "Message account")
        return mapOf(
            "messageContentId" to contentId,
            "recipientCount" to recipientCount,
            "sender" to senderName,
        )
    }
}
