// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.pis.getEmployee
import evaka.core.pis.getPersonById
import evaka.core.pis.service.PersonDTO
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildId
import evaka.core.shared.EmployeeId
import evaka.core.shared.Id
import evaka.core.shared.McpTestDataBatchId
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.NotFound

const val BATCH_DOC =
    "Name of the test data set (batch) this data belongs to, e.g. 'placement-desktop-demo'. Use the same name for all data of one scenario so the whole set can be listed and deleted together. Created automatically if it doesn't exist."

@McpDoc("No parameters") class EmptyInput

inline fun <reified T : Any> mcpTool(
    name: String,
    description: String,
    readOnly: Boolean = false,
    destructive: Boolean = false,
    noinline handler: (ctx: McpToolContext, input: T) -> Any,
) = McpToolDefinition(name, description, T::class, readOnly, destructive, handler)

@IgnorableReturnValue
fun McpToolContext.requireChild(childId: ChildId): PersonDTO {
    val isChild =
        tx.createQuery { sql("SELECT EXISTS (SELECT FROM child WHERE id = ${bind(childId)})") }
            .exactlyOne<Boolean>()
    if (!isChild) throw NotFound("Child $childId not found (the person must be created as a child)")
    return tx.getPersonById(childId) ?: throw NotFound("Child $childId not found")
}

@IgnorableReturnValue
fun McpToolContext.requireEmployee(employeeId: EmployeeId): String {
    val employee = tx.getEmployee(employeeId) ?: throw NotFound("Employee $employeeId not found")
    return "${employee.firstName} ${employee.lastName}"
}

/**
 * Some tools act *as* an employee (log in with their PIN, send messages from their account). They
 * are limited to employees created via MCP, so that a test scenario can never take over the account
 * of a real user of the environment.
 */
@IgnorableReturnValue
fun McpToolContext.requireTestEmployee(employeeId: EmployeeId): String {
    val name = requireEmployee(employeeId)
    if (!isMcpTestEntity("employee", employeeId))
        throw Forbidden(
            "Employee $employeeId ($name) was not created via MCP. This tool only works for test employees; insert one with insert_rows."
        )
    return name
}

/**
 * `advance_application` runs the real application workflow (placement plans, decisions, emails to
 * the guardian), so it is limited to applications created via MCP: a test scenario must never
 * decide a real application that happens to exist in the environment.
 */
fun McpToolContext.requireTestApplication(applicationId: ApplicationId): McpTestDataBatchId =
    McpTestDataService.getBatchOf(tx, "application", applicationId)
        ?: throw Forbidden(
            "Application $applicationId was not created via MCP. This tool only works for test applications; insert one with insert_rows."
        )

fun McpToolContext.isMcpTestEntity(table: String, id: Id<*>): Boolean =
    McpTestDataService.getBatchOf(tx, table, id) != null
