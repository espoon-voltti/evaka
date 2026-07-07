<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Audit Logging

eVaka uses structured audit logging to track user actions for security compliance, accountability, and debugging. Every controller endpoint must emit an audit event.

## Purpose

The point of audit logging is to know, at a sufficient level, **who did what**.

The practical test is **searchability**: long after the fact, the logs have to answer investigative questions, and keeping those questions in mind is the clearest guide to what's worth logging. For example:

- **Who modified this piece of data?** — so log the IDs of the entities an action creates, changes, or deletes.
- **Who has accessed a particular child's data?** — so log the person IDs whose data an action reads or changes, even on read-only endpoints.
- **Has someone reached far back into the history?** — so record how far into the past an action goes (`minDate`), making old-data access reviewable.

## How to log

Construct an `AuditContext` at the top of the endpoint, contribute IDs, metadata, and dates to it from any layer, and emit the event with `audit.log(event, clock)` once the transaction has succeeded. When a deeper function needs to contribute, pass the `AuditContext` to it as an explicit parameter.

```kotlin
// Controller: build a local audit context, then log once the transaction succeeds.
fun getPlacement(db: Database, clock: EvakaClock, @PathVariable id: PlacementId): Placement {
    // `id` is known from the request, so add it to the context right away.
    val audit = AuditContext().add(id)
    return db.connect { dbc ->
        // Once the placement is fetched, record whose data was read.
        dbc.read { tx -> tx.getPlacement(id).also { audit.add(it.childId) } }
    }.also { audit.log(Audit.PlacementRead, clock) }
}

// Any layer: pass the context in and register IDs, metadata, and dates as you discover them.
fun deleteServiceNeed(tx: Database.Transaction, id: ServiceNeedId, audit: AuditContext) {
    val sn = tx.getServiceNeed(id)
    audit
        .add(id)
        .add(sn.placementId)
        .add(sn.childId)
        .addMeta("optionName", sn.optionName)
        .observeDate(sn.startDate) // earliest date the change reaches -> minDate
    tx.deleteServiceNeed(id)
}
```

- **`add` / `addMeta` / `observeDate`** are available on the `AuditContext` from any layer. Contribute wherever the information appears, passing the context as an explicit parameter to deeper functions when needed.
- **Add request-derived values at construction.** By convention, the values you log that are known directly from the request are added when constructing the context at the beginning of the controller method. Values discovered later by querying are added where they surface.
- Emit after the `db.connect { ... }` call returns, so the event is produced only when the transaction committed. If the endpoint throws, `log` is never reached and no entry is produced.

**The requester is logged automatically.** The acting user's ID and type are always recorded, so never add an ID to the context just because that person made the request.

### Naming the event

- **Pattern: `EntityAction`** — e.g. `PlacementCreate`, `ApplicationSearch`, `DecisionAccept`.
- **One event per endpoint.** The event is the user's intent; side effects are captured via context IDs, not separate events.
- **Citizen vs employee:** share the event when the action and data scope are the same (they're distinguished by the auto-logged user type). Use separate events when the semantics differ, and name them for the difference — e.g. `OwnChildrenRead` vs `ChildrenByGuardianRead`.
- Each event sets `securityEvent` / `securityLevel` as enum properties when it's defined.

### Security-critical operations

For security-critical operations (login, credential changes), log at **both the start and end** with separate events — one for the attempt (e.g. `CitizenWeakLoginAttempt`, logged before the transaction since it may fail) and one for success (e.g. `CitizenWeakLogin`).

## What to log

This is the part that takes judgment. The aim is to log enough to answer the [questions above](#purpose), while not burying real signal in noise, leaking sensitive data, or adding undue complexity and performance cost just to capture an extra ID. Finding that balance is the whole job.

> **Never log sensitive data.** Free-text fields, personal details, SSNs, notes, or anything a user could have typed sensitive information into must never reach the log. When in doubt, leave it out. This rule overrides every "log more" guideline below.

### IDs to log

Every ID in the request path or body should usually be logged.

In addition to logging the id of the **primary entity** being acted on, consider logging **intermediate and closely related entities** that aid searching. Foreign keys of the primary entity are at least good candidates.

Specifically important are person IDs (child or adult). These help to answer whose data was seen or changed.

Entities often reach people **indirectly**.
- A service need has no person column, but it belongs to a placement, which belongs to a child.
- An attachment may be linked to an application which in turn has a child and one or two guardians.

Consider all the relevant people even if you have to go through several joins to get there. Note that the columns referring to people are not necessarily called person_id or child_id, but may also be, for example, guardian, head_of_child, or partner.

### How granular to log

The section above lists which IDs are *candidates*. Which of them to log and which to ignore is a judgment call with no fixed answer. Several factors affect it, and they should be weighed against each other.

- **Read vs. mutate.** Mutations change data and carry more accountability, so they warrant more thorough logging than reads.
- **Deletions.** When something is deleted, it is extra important to log what it was related to because after it is gone from the database, its ID alone does not tell us anything. It may be worth also logging some of the deleted contents in the meta map, but this should be limited to a few key defining fields that do not contain free text or sensitive data.
- **Request scope.** The broader a request — one entity, a family, a whole unit, an open-ended search — the less feasible and useful it is to record every individual ID.
- **Importance.** If the action has high importance, such as sending a decision, it is more important to log it thoroughly.
- **Sensitivity.** More sensitive data (e.g. assistance needs that may contain health information) deserves more thorough logging.
- **Cost to obtain.** Sometimes the IDs are easily available from the code and queries that are needed for the feature's implementation itself, while sometimes even a separate query might be needed to get all the relevant IDs. The additional complexity and possible performance implications should be considered.

### `minDate`

The earliest date the operation reaches into. Set it whenever the operation is scoped to a date or range:

- **Read** — the start of the queried range.
- **Create** — the start date of the created entity (it may be backdated into history).
- **Update** — the earliest date touched, i.e. the minimum of the entity's original and new start dates.
- **Delete** — the start date of the deleted entity.

`minDate` is an accumulator: every `observeDate` keeps the earliest value, so candidates can be registered from any layer without coordination. If the request affects multiple entities with different time ranges, then the minimum will be over all of those.

### `meta`

Needed rarely — for descriptive, non-ID information that aids understanding.

- Use it for defining fields of a deleted entity, POST-body search parameters that aren't IDs, or a result count for a broad search.
- Never put sensitive data or free text here.

## Examples

| Scenario                     | context (must)                                         | context (if easy) | minDate            | meta |
|------------------------------|--------------------------------------------------------|-|--------------------|------|
| Create placement for child   | `personId` (child), `placementId`                      | `daycareId` | placement start    | |
| Accept decision on application | `personId` (child + guardians), `decisionId`           | `applicationId`, `daycareId` | decision start     | |
| Read guardian's applications | `personId` (guardian + children), `applicationId` list | |                    | |
| Read group's children        | `daycareId`, `groupId`                                 | | range start       | |
| Search applications (broad)  |                                                        | |                    | `resultCount` |
| Generate occupancy report    |                                                        | `daycareId` (if filter) | range start        | |
| Send fee decisions (batch)   | `feeDecisionId`                                        | |                    | |
| Delete service need          | `personId` (child), `serviceNeedId`, `placementId`     | | service need start | option name, date range |

## General application logging

For operational logging (debugging, errors), use KotlinLogging — this is separate from audit logging:

```kotlin
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

logger.info { "Operation completed successfully" }
logger.error(exception) { "Failed to process request" }
```
