# Reorganize the employee auth-status response

**Status:** Design approved, ready for implementation plan
**Date:** 2026-06-05
**Scope:** Employee (desktop) auth status only. Citizen and employee-mobile are out of scope.

## Problem

The employee auth-status response (`GET /employee/auth/status`, served by apigw from the
backend `GET /system/employee/{id}` → `EmployeeUserResponse`) carries an
`accessibleFeatures` object whose design has drifted:

1. **21 of its 27 fields are redundant** — each is literally
   `permittedGlobalActions.contains(Action.Global.X)`. Since the response already
   includes the full `permittedGlobalActions` set, these duplicate information the
   frontend can derive itself.
2. **5 fields are not features at all** — they are deployment/feature configuration
   (`replacementInvoices`, `decisionReasoningGenericRemoval`,
   `openRangesHolidayQuestionnaire`, `allowEnglishChildDocumentsForAllTypes`,
   `messageSupportEmail`). They are returned from the backend so the frontend has a
   single source of truth and cannot drift out of sync, but lumping them under
   "accessible features" hides that intent.
3. **1 field is genuinely derived and not a global action** — `createPlacements` is
   `isPermittedForSomeTarget(Action.Unit.CREATE_PLACEMENT)`, a scoped permission summary
   the frontend cannot compute from `permittedGlobalActions`.

Separately, large parts of the frontend gate UI on **roles** rather than **permitted
actions**, which is a latent bug class (see below), and the response ships role data the
frontend mostly shouldn't be reasoning about.

## Background: two coexisting authorization systems

eVaka has two authz mechanisms that the frontend mixes:

- **Roles** (`UserRole`) — older, coarse. `globalRoles` are clear. `allScopedRoles` is
  built by `EmployeeQueries.kt` as
  `SELECT array_agg(DISTINCT role) FROM daycare_acl WHERE employee_id = …` — i.e. the set
  of scoped role *types* the employee holds across *all* units, **with the unit identity
  dropped**. apigw then flattens both into one `roles` array. So `hasRole(roles,
  'UNIT_SUPERVISOR')` means only "is a unit supervisor of *some* unit, unknown which".
- **Actions/permissions** (`Action.Global`, `Action.Unit`, `Action.Child`, …) — newer,
  fine-grained, scope-aware. Global actions arrive via `permittedGlobalActions`; scoped
  actions arrive as `permittedActions` attached to each scoped resource response.

**Why role-based gating is a bug:** each municipality configures which roles map to which
actions differently. A frontend check like `hasRole('ADMIN')` hard-codes an assumption
about that mapping and silently drifts out of sync with backend config. The source of
truth is the permitted-action set. Roles should be needed by the frontend only where we
genuinely care about the role itself — and the one such case (landing-page selection) is
moved to the backend by this design, so **the frontend ends up role-free**.

`UserRole` classification (authoritative, `UserRole.kt:24-39`):
- **Global (7):** `ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF, REPORT_VIEWER, DIRECTOR, MESSAGING`
- **Scoped (4):** `UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, EARLY_CHILDHOOD_EDUCATION_SECRETARY`

## Goals

- Stop duplicating permission information in `accessibleFeatures`; the frontend gates on
  `permittedGlobalActions` directly.
- Extract genuine configuration into a clearly named, top-level `featureConfig`.
- Migrate frontend UI gating from roles to permitted actions wherever a ready target
  exists.
- Move landing-page selection to the backend so the frontend no longer consumes any role
  data from the auth status.
- Do it with a **backwards-compatible, phased rollout** so no deployment window breaks
  already-logged-in employees.

## Non-goals

- Touching the citizen or employee-mobile auth status (mobile has no
  `accessibleFeatures`/`permittedGlobalActions`; citizen features are data-driven, not
  permission-derived, and there is no `permittedGlobalActions` on the citizen side).
- Re-architecting the role/action systems themselves (roles remain backend-internal and
  in the apigw session for session-refresh detection).
- The 3 deferred hard sites (see *Deferred to follow-up*).

## Feasibility (already verified)

Every scoped resource the role-checks touch **already returns `permittedActions`** for the
current user, and the frontend already uses this pattern in ~147 places:

| Resource | Response type | Action type |
|---|---|---|
| Unit (daycare) | `DaycareResponse.permittedActions` | `Action.Unit` |
| Group | `DaycareGroupResponse.permittedActions` | `Action.Group` |
| Child | `ChildResponse.permittedActions` + `permittedPersonActions` | `Action.Child` / `Action.Person` |
| Application (detail) | `ApplicationResponse.permittedActions` | `Action.Application` |
| Person | `PersonResponse.permittedActions` | `Action.Person` |
| Income | `IncomeWithPermittedActions.permittedActions` | `Action.Income` |

Most global role-checks map to an existing `Action.Global`. No new backend plumbing is
needed for the PR1 migrations.

## Target response shape (end state, after PR2)

```
AuthStatus {
  loggedIn, apiVersion,
  user: {
    id, name, userType,
    permittedGlobalActions: Action.Global[]      // the gating source of truth
    accessibleFeatures: { createPlacements }      // slimmed: scoped-derived summaries only
    startPage: EmployeeStartPage                  // backend-computed landing page (enum)
  },
  featureConfig: {                                // top-level: not user-specific
    replacementInvoices, decisionReasoningGenericRemoval,
    openRangesHolidayQuestionnaire,
    allowEnglishChildDocumentsForAllTypes, messageSupportEmail
  }
  // combined `roles`            → removed in PR2
  // globalRoles / allScopedRoles → never exposed to the frontend
}
```

The frontend auth contract carries **no role data** at end state. It gates on permitted
actions (global + scoped) plus `startPage`.

- **`featureConfig` is top-level** because it does not depend on the user. apigw rebuilds
  the user object field-by-field anyway, so routing one more field to the envelope top
  level costs the same as nesting it; the `EmployeeFeatureConfig` type is generated from
  Kotlin regardless of placement. The backend `/system/employee/{id}` returns
  `featureConfig` as a sibling to the user (a small generated wrapper, not an
  apigw-invented shape), and apigw routes it to the `AuthStatus` top level.
- **`startPage` is on `user`** because it is user-specific. It is an **enum**
  (`EmployeeStartPage`: `APPLICATIONS | UNITS | REPORTS | MESSAGES | …` + a default); the
  frontend maps enum→route, so the backend stays decoupled from frontend routing. The
  backend computes it by **porting the current `App.tsx` role logic verbatim**, keeping
  roles purely backend-internal and preserving today's behavior exactly. (Current logic:
  SERVICE_WORKER/SPECIAL_EDUCATION_TEACHER → applications; UNIT_SUPERVISOR/STAFF → units;
  DIRECTOR/REPORT_VIEWER → reports; MESSAGING → messages; plus the existing default. The
  exact branches and default are read from `App.tsx:123-145` during implementation.) It
  could later be made permission-based, but a verbatim port is the safe default since some
  branches key off scoped roles that do not map cleanly to global page-actions.

## The three buckets of `EmployeeFeatures` (verified `SystemController.kt:280-346`)

**Bucket A — 21 redundant → frontend reads `permittedGlobalActions`:**

| field | Action.Global | field | Action.Global |
|---|---|---|---|
| applications | `APPLICATIONS_PAGE` | personalMobileDevice | `PERSONAL_MOBILE_DEVICE_PAGE` |
| employees | `EMPLOYEES_PAGE` | pinCode | `PIN_CODE_PAGE` |
| financeBasics | `FINANCE_BASICS_PAGE` | createDraftInvoices | `CREATE_DRAFT_INVOICES` |
| finance | `FINANCE_PAGE` | submitPatuReport | `SUBMIT_PATU_REPORT` |
| messages | `MESSAGES_PAGE` | placementTool | `PLACEMENT_TOOL` |
| personSearch | `PERSON_SEARCH_PAGE` | outOfOffice | `OUT_OF_OFFICE_PAGE` |
| reports | `REPORTS_PAGE` | decisionReasonings | `WRITE_DECISION_REASONINGS` |
| settings | `SETTINGS_PAGE` | createUnits | `CREATE_UNIT` |
| systemNotifications | `READ_SYSTEM_NOTIFICATIONS` | units | `UNITS_PAGE` |
| holidayAndTermPeriods | `HOLIDAY_AND_TERM_PERIODS_PAGE` | unitFeatures | `UNIT_FEATURES_PAGE` |
| documentTemplates | `DOCUMENT_TEMPLATES_PAGE` | | |

Note several field names differ from their action names (`decisionReasonings` →
`WRITE_DECISION_REASONINGS`, `createUnits` → `CREATE_UNIT`,
`systemNotifications` → `READ_SYSTEM_NOTIFICATIONS`). This table is the source of truth.

**Bucket B — 5 config → `featureConfig`:** `replacementInvoices` (env),
`decisionReasoningGenericRemoval` (env), `openRangesHolidayQuestionnaire` (config),
`allowEnglishChildDocumentsForAllTypes` (config), `messageSupportEmail: String?` (config).

**Bucket C — 1 derived/scoped → stays in `accessibleFeatures`:** `createPlacements`.

## Frontend role-check disposition

**① Migrate to `permittedGlobalActions`:**

| Site | Current role check | → Action.Global |
|---|---|---|
| `Units.tsx:145` | ADMIN | `CREATE_UNIT` (exact) |
| `ChildApplications.tsx:54` | SERVICE_WORKER, ADMIN | `CREATE_PAPER_APPLICATION` (exact) |
| `Search.tsx:87` | SERVICE_WORKER, FINANCE_ADMIN | `CREATE_PERSON` + `CREATE_PERSON_FROM_VTJ` (drift: backend also grants ADMIN) |
| `ApplicationsList.tsx:408/520` | SERVICE_WORKER | `READ`/`WRITE_SERVICE_WORKER_APPLICATION_NOTES` |
| `PreschoolAbsenceReport.tsx:92/126` | ADMIN | `READ_PRESCHOOL_ABSENCE_REPORT_FOR_AREA` (exact) |
| `AttendanceReservationByChild.tsx:279` | ADMIN (+aromi flag) | `READ_AROMI_ORDERS` (keep flag) |

**② Migrate to scoped `permittedActions` (resource already returns them):**

| Site | Current role check | → Scoped source |
|---|---|---|
| `AdditionalInformation.tsx:213` (edit) | 7 roles | `ChildContext` → `Action.Child.UPDATE_ADDITIONAL_INFO` |
| `IncomeItemBody.tsx:42` (app link) | ADMIN | `PersonContext` → `Action.Person.READ_APPLICATIONS` |

**Reclassified to deferred during planning:** `Groups.tsx:213` (filter-edit — no clean
`Action.Unit`), `Group.tsx:249` (needs a Child/Placement service-need action it lacks in
scope), and `BackupPickup.tsx:209/222` (needs per-row `BackupPickup` actions the response
does not yet expose). These have no ready, unambiguous target and join the deferred set.

**③ Landing-page routing → backend `startPage`:**

| Site | Treatment |
|---|---|
| `App.tsx:130-138` (starting-page routing) | Replace role logic with `user.startPage`; logic ports to the backend |

**Behavior-change policy (approved):** several ① migrations are *not*
behavior-preserving where the current role-set diverges from the backend action's role
config (e.g. `Search`, and `ApplicationsList.enableApplicationActions` in PR2). We **trust
the backend action config and accept the change as the bug fix it is**. Each such site is
called out in the PR description for QA. We do **not** alter backend role→action mappings
to preserve today's (possibly-buggy) visibility.

## Deferred to follow-up (PR2 / separate)

Not in this spec — no ready target or too large. Because PR1 keeps the combined `roles`
field alive (deprecated), these three sites continue to work unchanged and need **no**
edits in PR1:

1. **layouts** — `layouts[roles[0]]` / `getLayout(layouts, roles)` in `layouts.ts:15`,
   `PersonProfile.tsx:268`, `ChildInformation.tsx:390` choose which page *sections* to
   show by role. Target: each section self-gates on the `permittedActions` /
   `permittedPersonActions` already returned by `PersonResponse`/`ChildResponse`, and the
   role-keyed layout mechanism is removed. Large.
2. **`VoucherServiceProviders.tsx:121`** — inverse "all areas" logic; no matching global
   action. Needs a new report/area-scoped action.
3. **`ApplicationsList.tsx:184`** (`enableApplicationActions`) — cleanest as per-row
   application `permittedActions`, which the applications *list* endpoint does not yet
   return. Needs backend support.
4. **`Groups.tsx:213`** (filter-edit) — no clean `Action.Unit` for "edit occupancy/attendance
   filter date range".
5. **`Group.tsx:249`** (service need) — the relevant action lives on Child/Placement, not the
   `Action.Group` the component has in scope.
6. **`BackupPickup.tsx:209/222`** (edit/delete) — needs per-row `BackupPickup` actions
   (`Action.BackupPickup.UPDATE`/`DELETE`) that the response does not yet expose.

(Landing-page selection was originally a separate deferred candidate; it is now **in scope**
in PR1 as the backend-computed `user.startPage`. Sites 4-6 were reclassified out of PR1's ②
bucket during planning. The deferred set is these 6.)

## Rollout strategy — additive first, remove later

A big-bang removal would break every employee whose browser still has the old frontend
cached the moment the backend stops sending `accessibleFeatures.*` / `roles` — persistent
breakage for everyone until they hard-refresh, not a tolerable transient blip. eVaka is
otherwise relaxed about transient (rolling-deploy-window) failures.

### PR 1 (this spec) — additive + deprecate, zero removals

**Backend (`service`):**
- Add `EmployeeFeatureConfig` data class (5 fields). Return `featureConfig` as a sibling
  to the employee user from `/system/employee/{id}` (small generated wrapper).
- Add `EmployeeStartPage` enum and compute `user.startPage` by porting `App.tsx` routing
  logic verbatim.
- Keep all 27 `EmployeeFeatures` fields populated. Mark the 26 transitional fields (21
  redundant + 5 config; **not** `createPlacements`) `@Deprecated` with a pointer to this
  doc / the follow-up issue.

**apigw:**
- Route `featureConfig` to the `AuthStatus` top level (add to the `AuthStatus` interface
  and the response builder) and pass `startPage` through on the user.
- Keep the combined `roles` field; mark deprecated in the interface. Do **not** add
  `globalRoles`/`allScopedRoles` to the frontend contract.
- Minor, optional, compatibility-neutral cleanups: drop the impossible
  `accessibleFeatures ?? {}` fallback; make `permittedGlobalActions` non-optional in the
  service-client type and drop `?? []`.

**Frontend:**
- Add `featureConfig` (top level) and `user.startPage` to the hand-written employee
  `AuthStatus`/`User` types (`employee-auth.ts`); surface them via the user context.
- Add a typed `hasGlobalAction(user, action)` helper aligned with the existing
  `RequirePermittedGlobalAction` (`utils/roles.tsx`).
- Migrate the 21 Bucket-A consumers (18 in `Header.tsx`, plus `MessageContext`, `Units`,
  `Invoices`, `Raw`, `GenericReasoningsSection`, etc.) to `hasGlobalAction`.
- Repoint the 5 Bucket-B consumers (`QuestionnaireEditor`, `TemplateModal`,
  `TemplateContentEditor`, `SingleThreadView`, `PersonInvoices`, `Filters`) to
  `featureConfig`.
- Leave the `createPlacements` consumer (`Placements.tsx:44`) reading
  `user.accessibleFeatures.createPlacements`.
- Migrate the ① and ② role-check sites. Replace `App.tsx` routing with `user.startPage`.

All PR1 frontend changes read data already present in today's responses
(`permittedGlobalActions`, scoped `permittedActions`) or new additive fields
(`featureConfig`, `startPage`), so the only cross-version risk is the brief rolling-deploy
window — an acceptable transient. Old (cached) frontends keep working because every field
they read remains present.

### PR 2 (follow-up, after deploy + old frontends drain)

- Remove the 26 deprecated `EmployeeFeatures` fields (leaving `{ createPlacements }`) and
  their generated types. Gated only on PR1's frontend draining.
- Migrate the 3 deferred sites off `roles` (incl. the two needing new backend support),
  **then** remove the combined `roles` field. The `roles` removal is gated on those
  migrations. After this, the frontend auth contract is fully role-free.

These may land as more than one follow-up PR (the `accessibleFeatures` cleanup and the
`roles` removal have different prerequisites).

## Risks

- **Intended behavior changes.** ① drift sites change who sees UI to match real backend
  permissions. Verify each against the role→action mapping table; list them for QA.
- **Wrong field→action mapping** at a Bucket-A call site. The table above is the single
  source of truth; check each migrated site against it.
- **`startPage` parity.** The backend port must reproduce `App.tsx`'s branches and default
  exactly; verify with a per-role check.
- **Rolling-deploy window.** New frontend briefly hitting old apigw (missing
  `featureConfig`/`startPage`) degrades those features for seconds — acceptable transient,
  and old frontends are unaffected because the deprecated fields remain.

## Verification

- Backend: update/keep tests asserting the `EmployeeUserResponse` shape; add coverage for
  `featureConfig` and `startPage` (per-role).
- Frontend: type-check (generated types regenerate from Kotlin); targeted e2e for header
  navigation, finance, messages, units/groups, child information edit, applications, the
  migrated report/search gates, and post-login landing for representative roles.
- Manually confirm each ① drift site against the intended (backend-config) visibility.
