<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Audit Logging

eVaka uses structured audit logging to track user actions for security compliance, accountability, and debugging. Every controller endpoint must emit an audit event.

## Goals

1. **Finding entries** — given an entity ID (e.g. a child), find all related audit log entries
2. **Understanding entries** — given a single entry, understand what each ID refers to without reading source code

## Architecture

IDs are collected via `AuditContext`, an aggregator owned by `Database.AuditableConnection`. When using `db.connectWithAudit`, the connection creates a shared `AuditContext` that is automatically passed to every `read` and `transaction` block. Service and query code calls `tx.audit.add(id)` to register IDs. After the connection closes, the result is returned as an `AuditedResult<T>` containing both the result and the audit context.

This means:
- IDs from deep call chains bubble up automatically — no need to thread them through return values
- Audit events are emitted only after a successful transaction — if the endpoint throws, no entry is produced
- Multiple transactions within the same connection share the same audit context

### Key derivation

`AuditContext` derives the key from the `DatabaseTable` type parameter of the ID. Both `ChildId` and `PersonId` are `Id<DatabaseTable.Person>`, so they both map to `"personId"` automatically.

## Standard Pattern

```kotlin
// Controller — read endpoint returning a single entity
fun getPlacement(db: Database, ..., @PathVariable id: PlacementId): Placement =
    db.connectWithAudit { dbc ->
        dbc.read { tx ->
            accessControl.requirePermissionFor(tx, user, clock, Action.Placement.READ, id)
            tx.getPlacement(id)
        }
    }.logThenResult { context ->
        AuditV2.PlacementRead.log(returnedResultsCount = 1, context = context.add(id))
    }

// Controller — void write endpoint
fun acceptDecision(db: Database, ..., @PathVariable applicationId: ApplicationId) {
    db.connectWithAudit { dbc ->
        dbc.transaction { tx ->
            applicationStateService.acceptDecision(tx, user, clock, applicationId, ...)
        }
    }.logThenResult { context ->
        AuditV2.DecisionAccept.log(returnedResultsCount = null, context = context.add(applicationId))
    }
}

// Service layer — adds its own IDs, calls deeper code that adds more
fun acceptDecision(tx: Database.Transaction, ...) {
    tx.audit.add(decisionId).add(decision.childId).add(application.guardianId)

    placementPlanService.applyPlacementPlan(tx, ...)  // adds unitId, placementId, etc.
}
```

### Where to add IDs

IDs that are trivially available in the controller (path parameters, request body fields) should be added in the `logThenResult` callback via `context.add(id)`. Service layers should add IDs that they discover during their own logic (e.g. from DB lookups) via `tx.audit.add(id)`.

Always use `connectWithAudit` and `logThenResult` (or destructure the `AuditedResult`) — even if the endpoint currently adds no IDs. This ensures IDs added later in service methods are captured automatically.

### Security-critical operations

For security-critical operations (login, credential changes), log at **both the start and end**. Use separate event types — one for the attempt (e.g. `CitizenWeakLoginAttempt`) and another for success (e.g. `CitizenWeakLogin`). The attempt event is logged before the transaction since the operation may fail.

## Log Entry Structure

Every audit log entry contains:

| Field | Description |
|-------|-------------|
| `eventCode` | Event name from the `AuditV2` enum (e.g. `PlacementCreate`) |
| `userType` | Auto-set from `AuthenticatedUser`: `citizen`, `citizen_weak`, `employee`, `mobile`, `system`, `integration` |
| `returnedResultsCount` | Count of entities in the HTTP response body. `null` for void returns. |
| `context` | Type-keyed ID map from `AuditContext`. Keys are `DatabaseTable` subclass names in lowerCamelCase + "Id". Values are always lists. |
| `meta` | Free-form metadata. Defaults to empty. |
| `securityEvent` / `securityLevel` | Enum properties, set per event. |

These fields are included automatically via MDC:
`httpMethod`, `path`, `queryString`, `httpRoute`, `httpPathParam.*`, `traceId`, `spanId`, `userId`, `userIdHash`

Avoid calling `tx.audit.add(user.id)` based on the request maker alone. However, log all person IDs found in the accessed data, even if one happens to equal the requester. For example, when handling applications, they are data owned by / related to guardian, other_guardian and child --> all of their IDs should be logged, even if one of them is also the request maker.

### Example output

```json
{
  "eventCode": "DecisionAccept",
  "userType": "citizen",
  "returnedResultsCount": null,
  "context": {
    "personId": ["<child-uuid>", "<guardian-uuid>"],
    "applicationId": ["<app-uuid>"],
    "decisionId": ["<decision-uuid>"],
    "daycareId": ["<unit-uuid>"],
    "placementId": ["<placement-uuid>"]
  },
  "securityLevel": "low",
  "securityEvent": false
}
```

## Event Naming

- **Pattern: `EntityAction`** — e.g. `PlacementCreate`, `ApplicationSearch`, `DecisionAccept`
- **One event per endpoint.** The event represents the user's intent. Side effects are captured via context IDs, not separate events.
- **Citizen vs employee:** share the event when the semantic action and data scope are the same (distinguished by `userType`). Use separate events when semantics differ — name them to reflect the semantic difference, not the user type. E.g. `OwnChildrenRead` (citizen reads their own children) vs `ChildrenByGuardianRead` (employee looks up a guardian's children).

## What to Log

### Core principle

Log IDs when the endpoint operates on specific, identifiable entities. Don't log IDs when the endpoint queries over an unspecific set.

### Person IDs

- Log all person IDs (children, guardians, adults) directly related to the action.
- For person-scoped or family-scoped endpoints (a guardian's applications, a child's placements), log all closely related people — typically the child and their guardians.
- For unit-scoped or broad administrative endpoints (all children in a unit), don't — the set is too large and loosely related.
- **Rule of thumb:** if the set of people is determined by a family relationship, log them. If determined by an organizational unit, don't.
- When the response contains data **about** other people (e.g. a timeline with partner/child details, a decision with child information), log those person IDs — someone accessed their data. This is distinct from IDs that merely appear as foreign key references without exposing the person's data.
- Conversely, when an endpoint is related to a child entity but does not return or modify data closely related to the child (e.g. listing eligible decision makers for a child document returns employee data, not child data), logging the child ID is optional.

### Other context IDs

- Log the primary entity being acted on (the decision ID, the placement ID).
- Log intermediate entities that aid searchability, even if derivable from the database (the application ID when accepting a decision).
- For list/search endpoints returning an unspecific set, don't log individual result IDs — rely on `returnedResultsCount`.
- IDs used as filters (a unit ID scoping a query) are worth logging since they aid searching.

### `returnedResultsCount`

This tracks the number of entities in the HTTP response body, not entities affected by the operation.

- `1` for single-entity endpoints that return the entity
- The list size for list/search endpoints
- `null` for void returns (writes that return nothing)

### `meta`

Rarely needed. Available when specific non-ID information is important.

- Don't log filter parameters that are already in the query string — they're captured via MDC.
- Can be used for POST body search parameters that aren't IDs.
- See deletion guidelines below.

### Deletion endpoints

Deletions warrant extra logging because once the entity is gone, the audit log may be the only record of what it was and how it related to other things.

- Log all associated IDs — the deleted entity's own ID, related person IDs, and parent/context entity IDs.
- Consider logging non-sensitive descriptive fields into `meta` to preserve what was deleted (the date of a deleted absence, the date range of a deleted placement).
- **Never log sensitive data** — free text, personal details, SSNs, notes, or anything that could contain sensitive information.

## Examples

| Scenario | context (must) | context (if easy) | returnedResultsCount | meta |
|----------|----------------|-------------------|---------------------|------|
| Create placement for child | `personId` (child), `placementId` | `daycareId` | 1 | |
| Accept decision on application | `personId` (child + guardians), `decisionId` | `applicationId`, `daycareId` | null | |
| Read guardian's applications | `personId` (guardian + children), `applicationId` list | | count | |
| Search applications (broad) | | | count | |
| Read unit's groups | `daycareId` | | count | |
| Generate occupancy report | | `daycareId` (if filter) | count | |
| Delete backup care | `personId` (child), `backupCareId`, `placementId` | `daycareId` | null | period dates |
| Delete absence | `personId` (child), `absenceId` | | null | absence date, category |

## General Application Logging

For operational logging (debugging, errors), use KotlinLogging:

```kotlin
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

logger.info { "Operation completed successfully" }
logger.error(exception) { "Failed to process request" }
```
