<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# ACL (Access Control) API

## Core Concepts

The ACL framework has three building blocks:

- **Actions** — what operation is being performed (e.g. `Action.Child.READ`, `Action.Global.APPLICATIONS_PAGE`). Scoped actions target a specific resource ID; unscoped actions don't.
- **Rules** — who is allowed to perform an action. Each action declares its own rules. Rules are OR'd — user only needs to match one.
- **User types** — `AuthenticatedUser.Employee`, `AuthenticatedUser.Citizen`, `AuthenticatedUser.MobileDevice`. Rules are aware of these distinctions.

Roles come in two flavors:

- **Global roles** (e.g. `ADMIN`, `SERVICE_WORKER`) — like traditional RBAC: a role is either assigned or not.
- **Scoped roles** (e.g. `UNIT_SUPERVISOR`, `STAFF`) — assigned to a specific daycare unit via `daycare_acl`, or to a specific group via `daycare_group_acl`. Rules built from scoped roles can be complex: they typically check which children are placed in that unit/group, but vary in time dimension (e.g. placed today vs. placed at any point) and may apply other business logic.

## Quick Reference

```kotlin
// Require permission (throws Forbidden if denied)
accessControl.requirePermissionFor(tx, user, clock, Action.Child.READ, childId)

// Check permission (returns boolean)
val canEdit = accessControl.hasPermissionFor(tx, user, clock, Action.Child.UPDATE, childId)

// Filter a query to only resources the user can access (throws Forbidden if no access at all)
val filter = accessControl.requireAuthorizationFilter(tx, user, clock, Action.Child.READ)
// Use in query: WHERE ${predicate(filter.forTable("child"))}
```

## Checking Permissions

Call `requirePermissionFor` at the start of a controller endpoint before any business logic:

```kotlin
fun getChild(
    db: Database,
    user: AuthenticatedUser,
    clock: EvakaClock,
    @PathVariable id: ChildId
): Child {
    return db.connect { dbc ->
        dbc.read { tx ->
            accessControl.requirePermissionFor(tx, user, clock, Action.Child.READ, id)
            tx.getChild(id)
        }
    }
}
```

For unscoped actions (no target resource):

```kotlin
accessControl.requirePermissionFor(tx, user, clock, Action.Global.APPLICATIONS_PAGE)
```

For multiple targets (single DB query for all):

```kotlin
accessControl.requirePermissionFor(tx, user, clock, Action.Child.READ, childIds)
```

Use `hasPermissionFor` when you need a boolean rather than a hard gate — for example, deciding what to include in a response:

```kotlin
val canEdit = accessControl.hasPermissionFor(tx, user, clock, Action.Child.UPDATE, id)
```

## Querying Accessible Resources

For list endpoints, use `requireAuthorizationFilter` to let the database do the filtering rather than fetching everything and filtering in memory:

```kotlin
fun getChildren(db: Database, user: AuthenticatedUser, clock: EvakaClock): List<Child> {
    return db.connect { dbc ->
        dbc.read { tx ->
            val filter = accessControl.requireAuthorizationFilter(tx, user, clock, Action.Child.READ)
            tx.getChildren(filter)
        }
    }
}

fun Database.Read.getChildren(filter: AccessControlFilter<ChildId>): List<Child> =
    createQuery {
        sql("""
            SELECT * FROM child
            WHERE ${predicate(filter.forTable("child"))}
        """)
    }.toList()
```

Admins get `AccessControlFilter.PermitAll` (no WHERE clause added); other roles get a targeted predicate.

## Finding the Right Action

All actions are defined in `Action.kt`, organized as nested enums by resource type:

```
Action.Global        — unscoped (page access, global operations)
Action.Unit          — targeting DaycareId
Action.Child         — targeting ChildId
Action.Application   — targeting ApplicationId
Action.Employee      — targeting EmployeeId
Action.Citizen.*     — citizen-facing actions
... (many more)
```

Search `Action.kt` for a matching action before adding a new one.

## Understanding Rules

Actions are strictly separated by user type:

- `Action.Citizen.*` — used exclusively in citizen endpoints
- Everything else — used exclusively in employee and mobile endpoints

If both employees and citizens need access to the same resource, there are two separate actions (e.g. `Action.Application.READ` and `Action.Citizen.Application.READ`).

To understand who can perform an action, read its rule declarations in `Action.kt`:

```kotlin
// Action.Child (employee) — global roles or scoped unit roles
READ(
    HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
    HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
        .inPlacementUnitOfChild(),
)

// Action.Citizen.Child — citizens related to the child
READ_PLACEMENT(
    IsCitizen(allowWeakLogin = false).guardianOfChild(),
    IsCitizen(allowWeakLogin = false).fosterParentOfChild(),
)
```

Rules are OR'd — any match grants access. The rule suffix (`.inPlacementUnitOfChild()`, `.guardianOfChild()`, `.inUnit()`) describes the required relationship.

## Adding a New Action

Each endpoint should have its own unique action, even if the rules are currently identical to an existing one. Only reuse an existing action when the operations are clearly coupled and would always change together.

Add the new action to the appropriate enum in `Action.kt`:

```kotlin
enum class Child(...) : ScopedAction<ChildId> {
    // ... existing actions ...
    READ_THERAPY_NOTES(
        HasGlobalRole(ADMIN),
        HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
    ),
}
```

Steps:
1. Find the right enum (`Action.Child`, `Action.Citizen.Child`, etc.)
2. Pick rules by looking at similar existing actions for reference
3. Use the action in your endpoint via `requirePermissionFor` or `requireAuthorizationFilter`

**Note on customization:** The rules declared in `Action.kt` are defaults. Other eVaka installations can override rules for specific actions by implementing `ActionRuleMapping`. Within this repo, `EspooActionRuleMapping` does this for a handful of Espoo-specific cases. When adding a new action, the defaults in `Action.kt` should reflect the sensible baseline.

## Adding a New Rule Method

When introducing a new resource type, you may find that no existing rule suffix captures the relationship you need — for example, there's no `.inPlacementUnitOfChildOfYourNewThing()` yet. In that case, add one to the relevant rule class(es) (`HasUnitRole.kt`, `HasGroupRole.kt`, `IsEmployee.kt`, `IsCitizen.kt`, `IsMobile.kt`), all of which follow the same two-helper pattern. Examples below use `HasUnitRole.kt`.

### `rule<T>` — write the full SQL including the ACL join

Use this when your query needs custom join conditions (e.g. date range checks). Must return `(id, role, unit_id)`:

```kotlin
fun inPlacementUnitOfChildOfAbsenceApplication() =
    rule<AbsenceApplicationId> { user, _ ->
        sql("""
            SELECT absence_application.id, acl.role, acl.daycare_id AS unit_id
            FROM absence_application
            JOIN placement ON absence_application.child_id = placement.child_id
            JOIN daycare_acl acl ON placement.unit_id = acl.daycare_id
            WHERE acl.employee_id = ${bind(user.id)}
              AND absence_application.start_date BETWEEN placement.start_date AND placement.end_date
        """)
    }
```

### `ruleViaChildAcl<T>` — provide only the resource-to-child mapping

Use this for simpler relationships where the ACL join is standard. You provide a query returning `(id, child_id)` and the framework handles the rest:

```kotlin
fun inPlacementUnitOfChildOfNewThing(cfg: ChildAclConfig = ChildAclConfig()) =
    ruleViaChildAcl<NewThingId>(cfg) { _, _ ->
        sql("""
            SELECT new_thing.id, new_thing.child_id
            FROM new_thing
        """)
    }
```

The framework handles caching and the query/evaluate split automatically — you only write the SQL.
