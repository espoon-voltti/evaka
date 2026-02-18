<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Logging

## General Application Logging

For general application logging (operational information, debugging, errors), use KotlinLogging:

```kotlin
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

logger.info { "Operation completed successfully" }
logger.error(exception) { "Failed to process request" }
```

## Audit Logging

eVaka uses structured audit logging to track all user actions for security compliance, accountability, and debugging. Every controller endpoint must log appropriate audit events to create a traceable record of who did what and when.

### When to Log

For most operations, log **after** successful completion.

For security-critical operations, log at **both the start and end**. Use separate event types - one for the attempt (e.g., `CitizenWeakLoginAttempt`) and another for success (e.g., `CitizenWeakLogin`).

### Standard Pattern

```kotlin
fun getEmployee(
    db: Database,
    user: AuthenticatedUser,
    @PathVariable id: EmployeeId
): Employee {
    return db.connect { dbc ->
        dbc.read {
            accessControl.requirePermissionFor(it, user, Action.Employee.READ, id)
            it.getEmployee(id)
        }
    }.also {
        Audit.EmployeeRead.log(targetId = AuditId(id))
    }
}
```

### Log Parameters

The `log()` function accepts three optional parameters:

- **`targetId`** - The primary resource being acted upon (required for most operations)
- **`objectId`** - Related or secondary entities, or the result of the operation
- **`meta`** - Additional context as key-value pairs (counts, dates, filters, etc.)

User information is automatically included, so it should not be logged manually.

Multiple IDs can be passed as `Audit.OccupancySpeculatedRead.log(targetId = AuditId(listOf(unitId, applicationId)))`

**For detailed documentation**, see [`Audit.kt`](../../../service/src/main/kotlin/fi/espoo/evaka/Audit.kt).

### Finding Event Types

All available audit events are defined in the `Audit` enum in [`Audit.kt`](../../../service/src/main/kotlin/fi/espoo/evaka/Audit.kt).

For operations involving children where child IDs must be explicitly logged, use `ChildAudit`:

```kotlin
ChildAudit.ApplicationRead.log(
    targetId = AuditId(userId),
    childId = AuditId(childId)  // Always required
)
```
