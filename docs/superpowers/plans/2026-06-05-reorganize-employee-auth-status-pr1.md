# Reorganize Employee Auth-Status Response (PR1) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize the employee auth-status response so the frontend gates on permitted actions (not roles or redundant feature flags), extract deployment config into a top-level `featureConfig`, and move landing-page selection to the backend — all additively (PR1 removes nothing) so no logged-in employee breaks during deploy.

**Architecture:** Backend `GET /system/employee/{id}` gains a `featureConfig` sibling and a `user.startPage` enum, while keeping all existing `EmployeeFeatures` fields populated (marked deprecated). apigw routes `featureConfig` to the `AuthStatus` top level and `startPage` onto the user, and keeps the combined `roles` field. The frontend switches its consumers to `permittedGlobalActions` (via a new `hasGlobalAction` helper / the existing `RequirePermittedGlobalAction`), to `featureConfig`, to scoped `permittedActions`, and to `user.startPage`. Removal of the deprecated fields and the combined `roles` is a follow-up PR after old frontends drain.

**Tech Stack:** Kotlin + Spring (service), Gradle codegen (`./gradlew codegen`), Node/Express + TypeScript (apigw), React + TypeScript + Vitest + Playwright (frontend). Generated TS types live in `frontend/src/lib-common/generated/api-types/`.

**Reference spec:** `docs/superpowers/specs/2026-06-05-reorganize-employee-auth-status-design.md`

**Paths in this plan are relative to the repo root** (the worktree at
`.worktrees/reorganize-auth-status-response`). Run backend commands from `service/`,
apigw commands from `apigw/`, frontend commands from `frontend/`.

---

## Scope note — what is and isn't in PR1

**In PR1 (this plan):** the auth-status reshape (additive + deprecate), the
`accessibleFeatures`→`permittedGlobalActions` migration (Bucket A), the
config→`featureConfig` migration (Bucket B), backend `startPage`, and the *clean*
role→action migrations.

**Deferred to a follow-up PR** (left on the deprecated `roles`, untouched here):
`layouts` section-gating; `VoucherServiceProviders.tsx:121`;
`ApplicationsList.tsx:184` (`enableApplicationActions`); `Groups.tsx:213` (no clean
`Action.Unit`); `Group.tsx:249` (needs a Child/Placement action it lacks in scope);
`BackupPickup.tsx:209/222` (needs per-row `BackupPickup` actions not yet in the response).
The last three were reclassified out of PR1 during planning because they have no
ready, unambiguous action target.

---

## Phase 1 — Backend (service)

### Task 1: Add `EmployeeStartPage` enum and `employeeStartPage()` function (TDD)

**Files:**
- Create: `service/src/main/kotlin/evaka/core/shared/security/EmployeeStartPage.kt`
- Create test: `service/src/test/kotlin/evaka/core/shared/security/EmployeeStartPageTest.kt`

- [ ] **Step 1: Write the failing test**

Create `service/src/test/kotlin/evaka/core/shared/security/EmployeeStartPageTest.kt`:

```kotlin
// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.security

import evaka.core.shared.auth.UserRole
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class EmployeeStartPageTest {
    @Test
    fun `service worker lands on applications`() {
        assertEquals(
            EmployeeStartPage.APPLICATIONS,
            employeeStartPage(setOf(UserRole.SERVICE_WORKER), emptySet()),
        )
    }

    @Test
    fun `scoped special education teacher lands on applications`() {
        assertEquals(
            EmployeeStartPage.APPLICATIONS,
            employeeStartPage(emptySet(), setOf(UserRole.SPECIAL_EDUCATION_TEACHER)),
        )
    }

    @Test
    fun `unit supervisor and staff land on units`() {
        assertEquals(
            EmployeeStartPage.UNITS,
            employeeStartPage(emptySet(), setOf(UserRole.UNIT_SUPERVISOR)),
        )
        assertEquals(
            EmployeeStartPage.UNITS,
            employeeStartPage(emptySet(), setOf(UserRole.STAFF)),
        )
    }

    @Test
    fun `director and report viewer land on reports`() {
        assertEquals(
            EmployeeStartPage.REPORTS,
            employeeStartPage(setOf(UserRole.DIRECTOR), emptySet()),
        )
        assertEquals(
            EmployeeStartPage.REPORTS,
            employeeStartPage(setOf(UserRole.REPORT_VIEWER), emptySet()),
        )
    }

    @Test
    fun `messaging lands on messages`() {
        assertEquals(
            EmployeeStartPage.MESSAGES,
            employeeStartPage(setOf(UserRole.MESSAGING), emptySet()),
        )
    }

    @Test
    fun `no roles lands on welcome`() {
        assertEquals(EmployeeStartPage.WELCOME, employeeStartPage(emptySet(), emptySet()))
    }

    @Test
    fun `other roles land on search`() {
        assertEquals(
            EmployeeStartPage.SEARCH,
            employeeStartPage(setOf(UserRole.FINANCE_ADMIN), emptySet()),
        )
    }

    @Test
    fun `applications takes priority over units`() {
        assertEquals(
            EmployeeStartPage.APPLICATIONS,
            employeeStartPage(setOf(UserRole.SERVICE_WORKER), setOf(UserRole.UNIT_SUPERVISOR)),
        )
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd service && ./gradlew test --tests "evaka.core.shared.security.EmployeeStartPageTest"`
Expected: FAIL/compile error — `EmployeeStartPage` and `employeeStartPage` do not exist.

- [ ] **Step 3: Write the implementation**

Create `service/src/main/kotlin/evaka/core/shared/security/EmployeeStartPage.kt`:

```kotlin
// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.security

import evaka.core.shared.auth.UserRole

/** The page an employee is routed to immediately after login. */
enum class EmployeeStartPage {
    APPLICATIONS,
    UNITS,
    REPORTS,
    MESSAGES,
    WELCOME,
    SEARCH,
}

/**
 * Computes the post-login landing page from the employee's roles. Ported verbatim from the
 * previous frontend logic in App.tsx (RedirectToMainPage) so behavior is preserved.
 */
fun employeeStartPage(
    globalRoles: Set<UserRole>,
    allScopedRoles: Set<UserRole>,
): EmployeeStartPage {
    val roles = globalRoles + allScopedRoles
    return when {
        UserRole.SERVICE_WORKER in roles || UserRole.SPECIAL_EDUCATION_TEACHER in roles ->
            EmployeeStartPage.APPLICATIONS
        UserRole.UNIT_SUPERVISOR in roles || UserRole.STAFF in roles -> EmployeeStartPage.UNITS
        UserRole.DIRECTOR in roles || UserRole.REPORT_VIEWER in roles -> EmployeeStartPage.REPORTS
        UserRole.MESSAGING in roles -> EmployeeStartPage.MESSAGES
        roles.isEmpty() -> EmployeeStartPage.WELCOME
        else -> EmployeeStartPage.SEARCH
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd service && ./gradlew test --tests "evaka.core.shared.security.EmployeeStartPageTest"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add service/src/main/kotlin/evaka/core/shared/security/EmployeeStartPage.kt \
        service/src/test/kotlin/evaka/core/shared/security/EmployeeStartPageTest.kt
git commit -m "Add EmployeeStartPage enum and backend landing-page computation"
```

---

### Task 2: Add `EmployeeFeatureConfig` data class

**Files:**
- Create: `service/src/main/kotlin/evaka/core/shared/security/EmployeeFeatureConfig.kt`

- [ ] **Step 1: Write the data class**

Create `service/src/main/kotlin/evaka/core/shared/security/EmployeeFeatureConfig.kt`:

```kotlin
// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.security

/**
 * Deployment / feature configuration returned to the employee frontend so it has a single
 * source of truth and cannot drift out of sync. Independent of the user.
 */
data class EmployeeFeatureConfig(
    val replacementInvoices: Boolean,
    val decisionReasoningGenericRemoval: Boolean,
    val openRangesHolidayQuestionnaire: Boolean,
    val allowEnglishChildDocumentsForAllTypes: Boolean,
    val messageSupportEmail: String?,
)
```

- [ ] **Step 2: Verify it compiles**

Run: `cd service && ./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add service/src/main/kotlin/evaka/core/shared/security/EmployeeFeatureConfig.kt
git commit -m "Add EmployeeFeatureConfig data class"
```

---

### Task 3: Return `featureConfig` + `startPage` from the endpoint; deprecate the 26 transitional `EmployeeFeatures` fields

**Files:**
- Modify: `service/src/main/kotlin/evaka/core/shared/security/EmployeeFeatures.kt`
- Modify: `service/src/main/kotlin/evaka/core/pis/SystemController.kt:267-360` and `:490-498`

- [ ] **Step 1: Deprecate the 26 transitional fields in `EmployeeFeatures.kt`**

Add `@Deprecated(...)` to every field **except** `createPlacements`. Replace the body of
`service/src/main/kotlin/evaka/core/shared/security/EmployeeFeatures.kt` (the `data class`)
with:

```kotlin
data class EmployeeFeatures(
    @Deprecated("Derive from permittedGlobalActions (APPLICATIONS_PAGE)")
    val applications: Boolean,
    @Deprecated("Derive from permittedGlobalActions (EMPLOYEES_PAGE)")
    val employees: Boolean,
    @Deprecated("Derive from permittedGlobalActions (FINANCE_BASICS_PAGE)")
    val financeBasics: Boolean,
    @Deprecated("Derive from permittedGlobalActions (FINANCE_PAGE)")
    val finance: Boolean,
    @Deprecated("Derive from permittedGlobalActions (MESSAGES_PAGE)")
    val messages: Boolean,
    @Deprecated("Derive from permittedGlobalActions (PERSON_SEARCH_PAGE)")
    val personSearch: Boolean,
    @Deprecated("Derive from permittedGlobalActions (REPORTS_PAGE)")
    val reports: Boolean,
    @Deprecated("Derive from permittedGlobalActions (SETTINGS_PAGE)")
    val settings: Boolean,
    @Deprecated("Derive from permittedGlobalActions (READ_SYSTEM_NOTIFICATIONS)")
    val systemNotifications: Boolean,
    @Deprecated("Derive from permittedGlobalActions (HOLIDAY_AND_TERM_PERIODS_PAGE)")
    val holidayAndTermPeriods: Boolean,
    @Deprecated("Derive from permittedGlobalActions (UNIT_FEATURES_PAGE)")
    val unitFeatures: Boolean,
    @Deprecated("Derive from permittedGlobalActions (UNITS_PAGE)")
    val units: Boolean,
    @Deprecated("Derive from permittedGlobalActions (CREATE_UNIT)")
    val createUnits: Boolean,
    @Deprecated("Derive from permittedGlobalActions (DOCUMENT_TEMPLATES_PAGE)")
    val documentTemplates: Boolean,
    @Deprecated("Derive from permittedGlobalActions (PERSONAL_MOBILE_DEVICE_PAGE)")
    val personalMobileDevice: Boolean,
    @Deprecated("Derive from permittedGlobalActions (PIN_CODE_PAGE)")
    val pinCode: Boolean,
    @Deprecated("Derive from permittedGlobalActions (CREATE_DRAFT_INVOICES)")
    val createDraftInvoices: Boolean,
    // Scoped permission summary the frontend cannot derive; kept permanently.
    val createPlacements: Boolean,
    @Deprecated("Derive from permittedGlobalActions (SUBMIT_PATU_REPORT)")
    val submitPatuReport: Boolean,
    @Deprecated("Derive from permittedGlobalActions (PLACEMENT_TOOL)")
    val placementTool: Boolean,
    @Deprecated("Moved to EmployeeFeatureConfig.replacementInvoices")
    val replacementInvoices: Boolean,
    @Deprecated("Moved to EmployeeFeatureConfig.openRangesHolidayQuestionnaire")
    val openRangesHolidayQuestionnaire: Boolean,
    @Deprecated("Derive from permittedGlobalActions (OUT_OF_OFFICE_PAGE)")
    val outOfOffice: Boolean,
    @Deprecated("Derive from permittedGlobalActions (WRITE_DECISION_REASONINGS)")
    val decisionReasonings: Boolean,
    @Deprecated("Moved to EmployeeFeatureConfig.decisionReasoningGenericRemoval")
    val decisionReasoningGenericRemoval: Boolean,
    @Deprecated("Moved to EmployeeFeatureConfig.allowEnglishChildDocumentsForAllTypes")
    val allowEnglishChildDocumentsForAllTypes: Boolean,
    @Deprecated("Moved to EmployeeFeatureConfig.messageSupportEmail")
    val messageSupportEmail: String?,
)
```

- [ ] **Step 2: Add `startPage` to `EmployeeUserResponse` and add the `EmployeeAuthResponse` wrapper**

In `service/src/main/kotlin/evaka/core/pis/SystemController.kt`, replace the
`EmployeeUserResponse` data class (lines 490-498) with:

```kotlin
    data class EmployeeUserResponse(
        val id: EmployeeId,
        val firstName: String,
        val lastName: String,
        val globalRoles: Set<UserRole> = setOf(),
        val allScopedRoles: Set<UserRole> = setOf(),
        val accessibleFeatures: EmployeeFeatures,
        val permittedGlobalActions: Set<Action.Global>,
        val startPage: EmployeeStartPage,
    )

    data class EmployeeAuthResponse(
        val user: EmployeeUserResponse,
        val featureConfig: EmployeeFeatureConfig,
    )
```

Add the imports near the other `evaka.core.shared.security` imports at the top of the file:

```kotlin
import evaka.core.shared.security.EmployeeAuthResponse
```

(`EmployeeFeatureConfig`, `EmployeeFeatures`, `EmployeeStartPage`, and `employeeStartPage`
are in the same `evaka.core.shared.security` package; add imports for any not already
present. If `EmployeeAuthResponse` is declared inside the controller class as shown above,
no import is needed for it — declare `EmployeeUserResponse`/`EmployeeAuthResponse` together
where `EmployeeUserResponse` currently lives.)

- [ ] **Step 3: Change the endpoint to build and return `EmployeeAuthResponse`**

In `SystemController.kt`, change the `employeeUser` function (lines 267-360). Update the
return type and the construction. Replace the `EmployeeUserResponse(...)` construction
block (currently lines 348-356) and wrap it, and annotate the `accessibleFeatures` block:

```kotlin
    @GetMapping("/system/employee/{id}")
    fun employeeUser(
        db: Database,
        systemUser: AuthenticatedUser.SystemInternalUser,
        clock: EvakaClock,
        @PathVariable id: EmployeeId,
    ): EmployeeAuthResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    val employeeUser = tx.getEmployeeUser(id) ?: throw NotFound()
                    val user = AuthenticatedUser.Employee(employeeUser)
                    val permittedGlobalActions =
                        accessControl.getPermittedActions<Action.Global>(tx, user, clock)
                    @Suppress("DEPRECATION")
                    val accessibleFeatures =
                        EmployeeFeatures(
                            // ... keep the existing 27-field construction exactly as-is ...
                        )

                    val featureConfig =
                        EmployeeFeatureConfig(
                            replacementInvoices = env.replacementInvoicesStart != null,
                            decisionReasoningGenericRemoval =
                                env.decisionReasoningGenericRemovalEnabled,
                            openRangesHolidayQuestionnaire =
                                featureConfig.holidayQuestionnaireType ==
                                    QuestionnaireType.OPEN_RANGES,
                            allowEnglishChildDocumentsForAllTypes =
                                featureConfig.allowEnglishChildDocumentsForAllTypes,
                            messageSupportEmail = featureConfig.messageSupportEmail,
                        )

                    EmployeeAuthResponse(
                        user =
                            EmployeeUserResponse(
                                id = employeeUser.id,
                                firstName =
                                    employeeUser.preferredFirstName ?: employeeUser.firstName,
                                lastName = employeeUser.lastName,
                                globalRoles = employeeUser.globalRoles,
                                allScopedRoles = employeeUser.allScopedRoles,
                                accessibleFeatures = accessibleFeatures,
                                permittedGlobalActions = permittedGlobalActions,
                                startPage =
                                    employeeStartPage(
                                        employeeUser.globalRoles,
                                        employeeUser.allScopedRoles,
                                    ),
                            ),
                        featureConfig = featureConfig,
                    )
                }
            }
            .also { Audit.EmployeeUserDetailsRead.log(targetId = AuditId(id)) }
    }
```

Notes:
- Keep the existing 27-field `EmployeeFeatures(...)` construction body (lines 282-345)
  exactly as it is today — only add the `@Suppress("DEPRECATION")` annotation immediately
  above the `val accessibleFeatures =` line so the controller still compiles cleanly now
  that the fields are `@Deprecated`.
- `env` and `featureConfig` here are the controller's existing dependencies already used by
  the current code at lines 331-345; reuse them.
- There is a local `featureConfig` (the new `EmployeeFeatureConfig` value) and an injected
  `featureConfig` (the controller dependency). Rename the local to `employeeFeatureConfig`
  if the names collide in scope, and reference the dependency as before. Verify by reading
  how `featureConfig.holidayQuestionnaireType` is currently resolved in the file (it refers
  to the injected dependency).

- [ ] **Step 4: Verify the backend compiles and existing tests pass**

Run: `cd service && ./gradlew compileKotlin compileTestKotlin`
Expected: BUILD SUCCESSFUL (no deprecation warnings escape the `@Suppress`).

Run: `cd service && ./gradlew test --tests "*SystemController*"`
Expected: PASS (or "no tests found" — there is currently no integration test asserting the
`EmployeeUserResponse` shape; confirm with
`grep -rl "EmployeeAuthResponse\|/system/employee/" service/src/test` → if a test exists,
update its expected JSON to the wrapped `{ user, featureConfig }` shape and re-run).

- [ ] **Step 5: Commit**

```bash
git add service/src/main/kotlin/evaka/core/shared/security/EmployeeFeatures.kt \
        service/src/main/kotlin/evaka/core/pis/SystemController.kt
git commit -m "Return featureConfig and startPage from employee system endpoint; deprecate redundant EmployeeFeatures fields"
```

---

### Task 4: Regenerate TypeScript API types

**Files:**
- Modify (generated): `frontend/src/lib-common/generated/api-types/shared.ts`,
  `frontend/src/lib-common/generated/api-types/pis.ts` (and any other files codegen touches)

- [ ] **Step 1: Run codegen**

Run: `cd service && ./gradlew codegen`
Expected: BUILD SUCCESSFUL; `git status` shows modifications under
`frontend/src/lib-common/generated/`.

- [ ] **Step 2: Verify the generated output**

Run: `git diff --stat frontend/src/lib-common/generated/`
Expected to see, among others:
- `shared.ts`: new `EmployeeFeatureConfig` interface, new `EmployeeStartPage` string-union
  type, and `@deprecated` JSDoc on the 26 `EmployeeFeatures` fields (if the generator
  propagates `@Deprecated`).
- `pis.ts`: `EmployeeUserResponse` gains `startPage: EmployeeStartPage`; a new
  `EmployeeAuthResponse` interface with `user` + `featureConfig`.

Run: `cd service && ./gradlew codegenCheck`
Expected: PASS (generated files match what codegen produces).

- [ ] **Step 3: Commit**

```bash
git add frontend/src/lib-common/generated/
git commit -m "Regenerate API types for featureConfig, startPage, EmployeeAuthResponse"
```

---

## Phase 2 — apigw

### Task 5: Route `featureConfig` to the top level and `startPage` onto the user; keep `roles` (deprecated)

**Files:**
- Modify: `apigw/src/shared/service-client.ts:76-145`
- Modify: `apigw/src/internal/routes/auth-status.ts`

- [ ] **Step 1: Update the service-client types and `getEmployeeDetails`**

In `apigw/src/shared/service-client.ts`, replace the `EmployeeUser` /
`EmployeeUserResponse` interfaces (lines 76-87) with:

```typescript
export interface EmployeeUser {
  id: string
  firstName: string
  lastName: string
  globalRoles: UserRole[]
  allScopedRoles: UserRole[]
}

export interface EmployeeUserResponse extends EmployeeUser {
  accessibleFeatures: object
  permittedGlobalActions: string[]
  startPage: string
}

export interface EmployeeAuthResponse {
  user: EmployeeUserResponse
  featureConfig: object
}
```

Then change `getEmployeeDetails` (lines 128-145) to fetch and return the wrapper:

```typescript
export async function getEmployeeDetails(
  req: express.Request,
  employeeId: string
): Promise<EmployeeAuthResponse | undefined> {
  try {
    const { data } = await client.get<EmployeeAuthResponse>(
      `/system/employee/${employeeId}`,
      { headers: createServiceRequestHeaders(req, systemUserHeader) }
    )
    return data
  } catch (e: unknown) {
    if (axios.isAxiosError(e) && e.response?.status === 404) {
      return undefined
    } else {
      throw e
    }
  }
}
```

- [ ] **Step 2: Update `auth-status.ts` interfaces**

In `apigw/src/internal/routes/auth-status.ts`, update the `AuthStatus` interface
(lines 21-28), the `EmployeeUser` interface (lines 30-36), and the `ValidatedUser`
interface (lines 49-53):

```typescript
export interface AuthStatus {
  loggedIn: boolean
  user?: EmployeeUser | MobileUser
  featureConfig?: object
  /** @deprecated removed in a follow-up PR; frontend gates on permitted actions / startPage */
  roles?: string[]
  globalRoles?: string[]
  allScopedRoles?: string[]
  apiVersion: string
}

interface EmployeeUser {
  userType: 'EMPLOYEE'
  id: UUID
  name: string
  accessibleFeatures: object
  permittedGlobalActions: string[]
  startPage: string
}

interface ValidatedUser {
  user: EmployeeUser | MobileUser
  featureConfig?: object
  globalRoles: string[]
  allScopedRoles: string[]
}
```

- [ ] **Step 3: Update `validateUser` (both branches) to thread `featureConfig` and `startPage`**

Replace the `MOBILE` branch return (lines 70-83) so it also returns `featureConfig`:

```typescript
      return {
        user: {
          id,
          name,
          userType: 'MOBILE',
          unitIds,
          employeeId,
          pinLoginActive,
          personalDevice,
          pushApplicationServerKey
        },
        featureConfig: undefined,
        globalRoles: [],
        allScopedRoles: []
      }
```

Replace the `EMPLOYEE` branch (lines 85-110) with:

```typescript
    case 'EMPLOYEE': {
      const response = await getEmployeeDetails(req, user.id)
      if (!response) {
        return undefined
      }
      const { user: employee, featureConfig } = response
      const {
        id,
        firstName,
        lastName,
        globalRoles,
        allScopedRoles,
        accessibleFeatures,
        permittedGlobalActions,
        startPage
      } = employee
      const name = [firstName, lastName].filter((x) => !!x).join(' ')
      return {
        user: {
          userType: 'EMPLOYEE',
          id,
          name,
          accessibleFeatures,
          permittedGlobalActions,
          startPage
        },
        featureConfig,
        globalRoles,
        allScopedRoles
      }
    }
```

- [ ] **Step 4: Include `featureConfig` in the response envelope**

In `internalAuthStatus` (lines 134-154), update the destructure and the `status` build:

```typescript
      const { user, featureConfig, globalRoles, allScopedRoles } = validUser
      // Refresh roles if necessary
      if (
        sessionUser?.userType === 'EMPLOYEE' &&
        userChanged(sessionUser, validUser)
      ) {
        await sessions.updateUser(req, {
          ...sessionUser,
          globalRoles,
          allScopedRoles
        })
      }
      status = {
        loggedIn: true,
        user,
        featureConfig,
        globalRoles,
        allScopedRoles,
        roles: [...globalRoles, ...allScopedRoles],
        apiVersion: appCommit
      }
```

- [ ] **Step 5: Type-check, lint, and test apigw**

Run: `cd apigw && yarn lint && yarn tsc --noEmit`
Expected: no errors. (If apigw has no standalone `tsc` script, run `yarn build`.)

Run: `cd apigw && yarn test`
Expected: PASS (update any auth-status test that asserted the old non-wrapped
`getEmployeeDetails` shape).

- [ ] **Step 6: Commit**

```bash
git add apigw/src/shared/service-client.ts apigw/src/internal/routes/auth-status.ts
git commit -m "apigw: route featureConfig top-level and startPage onto employee user; deprecate roles"
```

---

## Phase 3 — Frontend plumbing

### Task 6: Add `featureConfig` and `startPage` to the frontend auth types and context

**Files:**
- Modify: `frontend/src/lib-common/api-types/employee-auth.ts`
- Modify: `frontend/src/employee-frontend/state/user.tsx`

- [ ] **Step 1: Update `employee-auth.ts`**

Replace lines 5-19 and 49-54 of
`frontend/src/lib-common/api-types/employee-auth.ts`:

```typescript
import type { MobileDeviceDetails } from 'lib-common/generated/api-types/pairing'

import type { Action } from '../generated/action'
import type {
  EmployeeFeatureConfig,
  EmployeeFeatures,
  EmployeeId,
  EmployeeStartPage
} from '../generated/api-types/shared'

export interface User {
  id: EmployeeId
  name: string
  userType: 'EMPLOYEE'
  accessibleFeatures: EmployeeFeatures
  permittedGlobalActions: Action.Global[]
  startPage: EmployeeStartPage
}
```

and:

```typescript
export interface AuthStatus<U extends User | MobileUser> {
  loggedIn: boolean
  user?: U
  featureConfig?: EmployeeFeatureConfig
  /** @deprecated removed in a follow-up PR; gate on permitted actions / startPage */
  roles?: AdRole[]
  apiVersion: string
}
```

(`EmployeeFeatureConfig` and `EmployeeStartPage` are generated into the same `shared`
module as `EmployeeFeatures`; verify the exact export names match Task 4's output.)

- [ ] **Step 2: Expose `featureConfig` on the user context**

In `frontend/src/employee-frontend/state/user.tsx`:

Update the import on line 13:

```typescript
import type { AdRole, User } from 'lib-common/api-types/employee-auth'
import type { EmployeeFeatureConfig } from 'lib-common/generated/api-types/shared'
```

Add `featureConfig` to the `UserState` interface (after line 23 `user`):

```typescript
  user: User | undefined
  featureConfig: EmployeeFeatureConfig | undefined
```

Add it to the default context value (after line 34 `user: undefined`):

```typescript
  user: undefined,
  featureConfig: undefined,
```

Add it to the memoized `value` (after line 78 `user: authStatus?.user,`):

```typescript
      user: authStatus?.user,
      featureConfig: authStatus?.featureConfig,
```

Note: `getAuthStatus` in `frontend/src/employee-frontend/api/auth.ts` already spreads
`...status` (which now carries `featureConfig`), so no change is needed there.

- [ ] **Step 3: Type-check**

Run: `cd frontend && yarn type-check`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/lib-common/api-types/employee-auth.ts \
        frontend/src/employee-frontend/state/user.tsx
git commit -m "Frontend: add featureConfig and startPage to employee auth types and context"
```

---

### Task 7: Add the `hasGlobalAction` helper (TDD)

**Files:**
- Modify: `frontend/src/employee-frontend/utils/roles.tsx`
- Create test: `frontend/src/employee-frontend/utils/roles.test.tsx`

- [ ] **Step 1: Write the failing test**

Create `frontend/src/employee-frontend/utils/roles.test.tsx`:

```typescript
// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { User } from 'lib-common/api-types/employee-auth'

import { hasGlobalAction } from './roles'

const user = (permittedGlobalActions: User['permittedGlobalActions']): User => ({
  id: '00000000-0000-0000-0000-000000000000' as User['id'],
  name: 'Test',
  userType: 'EMPLOYEE',
  accessibleFeatures: {} as User['accessibleFeatures'],
  permittedGlobalActions,
  startPage: 'SEARCH'
})

describe('hasGlobalAction', () => {
  it('returns true when the action is permitted', () => {
    expect(hasGlobalAction(user(['CREATE_UNIT']), 'CREATE_UNIT')).toBe(true)
  })

  it('returns false when the action is not permitted', () => {
    expect(hasGlobalAction(user(['REPORTS_PAGE']), 'CREATE_UNIT')).toBe(false)
  })

  it('returns false when the user is undefined', () => {
    expect(hasGlobalAction(undefined, 'CREATE_UNIT')).toBe(false)
  })
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd frontend && yarn test src/employee-frontend/utils/roles.test.tsx`
Expected: FAIL — `hasGlobalAction` is not exported.

- [ ] **Step 3: Add the helper**

In `frontend/src/employee-frontend/utils/roles.tsx`, add the import for the `User` type and
the helper near the top (after the existing imports, before `requireRole`):

```typescript
import type { AdRole, User } from 'lib-common/api-types/employee-auth'
import type { Action } from 'lib-common/generated/action'

import { UserContext } from '../state/user'

export const hasGlobalAction = (
  user: User | undefined,
  action: Action.Global
): boolean => user?.permittedGlobalActions.includes(action) ?? false
```

(The `AdRole` and `Action` imports already exist on lines 8-9; merge the `User` import into
the existing `lib-common/api-types/employee-auth` import line.)

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd frontend && yarn test src/employee-frontend/utils/roles.test.tsx`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/employee-frontend/utils/roles.tsx \
        frontend/src/employee-frontend/utils/roles.test.tsx
git commit -m "Frontend: add hasGlobalAction helper"
```

---

## Phase 4 — Bucket A: `accessibleFeatures` → `permittedGlobalActions`

> These tasks change UI gating. They are verified by `yarn type-check` + `yarn lint`
> (and the Phase 7 e2e), not by new unit tests — the codebase gates UI here and exercises
> it via Playwright, so a per-site unit test would add no signal.

### Task 8: Migrate `Header.tsx` navigation (18 sites)

**Files:**
- Modify: `frontend/src/employee-frontend/components/Header.tsx`

- [ ] **Step 1: Import the helper**

Add to the imports (near line 33 where `UserContext` is imported):

```typescript
import { hasGlobalAction } from '../utils/roles'
```

- [ ] **Step 2: Replace each `accessibleFeatures` read with `hasGlobalAction`**

For each line below, replace `user.accessibleFeatures.X` / `user?.accessibleFeatures.X`
with `hasGlobalAction(user, 'ACTION')`. The helper accepts `User | undefined`, so the
`user?.` form becomes a plain `hasGlobalAction(user, ...)`. Exact mapping:

| Line | From | To |
|---|---|---|
| 242 | `user.accessibleFeatures.applications` | `hasGlobalAction(user, 'APPLICATIONS_PAGE')` |
| 255 | `user.accessibleFeatures.units` | `hasGlobalAction(user, 'UNITS_PAGE')` |
| 268 | `user.accessibleFeatures.personSearch` | `hasGlobalAction(user, 'PERSON_SEARCH_PAGE')` |
| 283 | `user.accessibleFeatures.finance` | `hasGlobalAction(user, 'FINANCE_PAGE')` |
| 296 | `user.accessibleFeatures.reports` | `hasGlobalAction(user, 'REPORTS_PAGE')` |
| 320 | `user.accessibleFeatures.messages` | `hasGlobalAction(user, 'MESSAGES_PAGE')` |
| 390 | `user?.accessibleFeatures.employees` | `hasGlobalAction(user, 'EMPLOYEES_PAGE')` |
| 399 | `user?.accessibleFeatures.financeBasics` | `hasGlobalAction(user, 'FINANCE_BASICS_PAGE')` |
| 408 | `user?.accessibleFeatures.documentTemplates` | `hasGlobalAction(user, 'DOCUMENT_TEMPLATES_PAGE')` |
| 417 | `user?.accessibleFeatures.decisionReasonings` | `hasGlobalAction(user, 'WRITE_DECISION_REASONINGS')` |
| 426 | `user?.accessibleFeatures.holidayAndTermPeriods` | `hasGlobalAction(user, 'HOLIDAY_AND_TERM_PERIODS_PAGE')` |
| 435 | `user?.accessibleFeatures.settings` | `hasGlobalAction(user, 'SETTINGS_PAGE')` |
| 444 | `user?.accessibleFeatures.systemNotifications` | `hasGlobalAction(user, 'READ_SYSTEM_NOTIFICATIONS')` |
| 453 | `user?.accessibleFeatures.unitFeatures` | `hasGlobalAction(user, 'UNIT_FEATURES_PAGE')` |
| 462 | `user?.accessibleFeatures.placementTool` | `hasGlobalAction(user, 'PLACEMENT_TOOL')` |
| 471 | `user?.accessibleFeatures.personalMobileDevice` | `hasGlobalAction(user, 'PERSONAL_MOBILE_DEVICE_PAGE')` |
| 480 | `user?.accessibleFeatures.pinCode` | `hasGlobalAction(user, 'PIN_CODE_PAGE')` |
| 489 | `user?.accessibleFeatures.outOfOffice` | `hasGlobalAction(user, 'OUT_OF_OFFICE_PAGE')` |

- [ ] **Step 3: Type-check and lint**

Run: `cd frontend && yarn type-check && yarn lint`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/employee-frontend/components/Header.tsx
git commit -m "Frontend: gate Header navigation on permittedGlobalActions"
```

---

### Task 9: Migrate the remaining Bucket-A consumers

**Files:**
- Modify: `frontend/src/employee-frontend/components/messages/MessageContext.tsx`
- Modify: `frontend/src/employee-frontend/components/Units.tsx`
- Modify: `frontend/src/employee-frontend/components/invoices/Invoices.tsx`
- Modify: `frontend/src/employee-frontend/components/reports/Raw.tsx`

- [ ] **Step 1: `MessageContext.tsx` — 3 `messages` reads → `MESSAGES_PAGE`**

Add `import { hasGlobalAction } from '../../utils/roles'` near line 56. Then:

- Line 225: `user?.accessibleFeatures.messages` → `hasGlobalAction(user, 'MESSAGES_PAGE')`
- Line 231: `user?.accessibleFeatures.messages` → `hasGlobalAction(user, 'MESSAGES_PAGE')`
- Line 298: `user?.accessibleFeatures.messages` → `hasGlobalAction(user, 'MESSAGES_PAGE')`

- [ ] **Step 2: `Units.tsx:92` — `createUnits` redirect → `CREATE_UNIT`**

Add `hasGlobalAction` to the `../utils/roles` import. Replace line 92:

```typescript
    !hasGlobalAction(user, 'CREATE_UNIT')
```

- [ ] **Step 3: `Invoices.tsx:100` — `createDraftInvoices` → `CREATE_DRAFT_INVOICES`**

Add `import { hasGlobalAction } from '../../utils/roles'`. Replace line 100:

```typescript
      {hasGlobalAction(user, 'CREATE_DRAFT_INVOICES') && (
```

(`user` is the `User | undefined` prop already passed into `Invoices`.)

- [ ] **Step 4: `Raw.tsx:250` — `submitPatuReport` → `SUBMIT_PATU_REPORT`**

Add `import { hasGlobalAction } from '../../utils/roles'`. Replace line 250:

```typescript
        {hasGlobalAction(user, 'SUBMIT_PATU_REPORT') && (
```

- [ ] **Step 5: Type-check and lint**

Run: `cd frontend && yarn type-check && yarn lint`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/employee-frontend/components/messages/MessageContext.tsx \
        frontend/src/employee-frontend/components/Units.tsx \
        frontend/src/employee-frontend/components/invoices/Invoices.tsx \
        frontend/src/employee-frontend/components/reports/Raw.tsx
git commit -m "Frontend: gate remaining feature consumers on permittedGlobalActions"
```

---

## Phase 5 — Bucket B: config → `featureConfig`

### Task 10: Repoint the 5 config fields to `featureConfig`

**Files:**
- Modify: `frontend/src/employee-frontend/components/holiday-term-periods/QuestionnaireEditor.tsx`
- Modify: `frontend/src/employee-frontend/components/messages/SingleThreadView.tsx`
- Modify: `frontend/src/employee-frontend/components/person-profile/PersonInvoices.tsx`
- Modify: `frontend/src/employee-frontend/components/common/Filters.tsx`
- Modify: `frontend/src/employee-frontend/components/document-templates/template-editor/TemplateModal.tsx`
- Modify: `frontend/src/employee-frontend/components/document-templates/template-editor/TemplateContentEditor.tsx`
- Modify: `frontend/src/employee-frontend/components/decision-reasonings/GenericReasoningsSection.tsx`

Each consumer reads `featureConfig` from `UserContext` instead of `user.accessibleFeatures`.

- [ ] **Step 1: `QuestionnaireEditor.tsx:45` — `openRangesHolidayQuestionnaire`**

Line 25: change `const { user } = useContext(UserContext)` to
`const { featureConfig } = useContext(UserContext)`.
Line 45: `user?.accessibleFeatures.openRangesHolidayQuestionnaire` →
`featureConfig?.openRangesHolidayQuestionnaire`.

- [ ] **Step 2: `SingleThreadView.tsx:399` — `messageSupportEmail`**

Line 279: change `const { user } = useContext(UserContext)` to also pull `featureConfig`:
`const { featureConfig } = useContext(UserContext)` (verify `user` isn't used elsewhere in
the component; if it is, use `const { user, featureConfig } = useContext(UserContext)`).
Line 399:
`const supportEmail = featureConfig?.messageSupportEmail ?? null`.

- [ ] **Step 3: `PersonInvoices.tsx:39` — `replacementInvoices`**

`PersonInvoices` binds the whole context: `const user = useContext(UserContext)` (line 35).
`featureConfig` is now a sibling on that context. Replace line 39:

```typescript
      {user?.featureConfig?.replacementInvoices &&
```

- [ ] **Step 4: `Filters.tsx:733` — `replacementInvoices`**

Same whole-context binding (`const user = useContext(UserContext)` at line 731). Replace
lines 733-734:

```typescript
  const statuses: InvoiceStatus[] = user?.featureConfig?.replacementInvoices
    ? allStatuses
    : statusesWithoutReplacements
```

- [ ] **Step 5: `TemplateModal.tsx:103` and `TemplateContentEditor.tsx:347` — `allowEnglishChildDocumentsForAllTypes`**

In each file, change `const { user } = useContext(UserContext)` to
`const { featureConfig } = useContext(UserContext)`, and:

```typescript
  const allowEnglishForAllTypes =
    featureConfig?.allowEnglishChildDocumentsForAllTypes
```

- [ ] **Step 6: `GenericReasoningsSection.tsx:295` — `decisionReasoningGenericRemoval`**

Line 224: change `const { user } = useContext(UserContext)` to
`const { featureConfig } = useContext(UserContext)` (verify `user` isn't used elsewhere; if
it is, pull both). Line 295:
`featureConfig?.decisionReasoningGenericRemoval && (`.

- [ ] **Step 7: Type-check and lint**

Run: `cd frontend && yarn type-check && yarn lint`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/employee-frontend/components/holiday-term-periods/QuestionnaireEditor.tsx \
        frontend/src/employee-frontend/components/messages/SingleThreadView.tsx \
        frontend/src/employee-frontend/components/person-profile/PersonInvoices.tsx \
        frontend/src/employee-frontend/components/common/Filters.tsx \
        frontend/src/employee-frontend/components/document-templates/template-editor/TemplateModal.tsx \
        frontend/src/employee-frontend/components/document-templates/template-editor/TemplateContentEditor.tsx \
        frontend/src/employee-frontend/components/decision-reasonings/GenericReasoningsSection.tsx
git commit -m "Frontend: read deployment config from featureConfig"
```

(`Placements.tsx:42` keeps reading `user?.accessibleFeatures.createPlacements` — no change.)

---

## Phase 6 — Role → permitted-action migrations

### Task 11: ① Global-action JSX wrappers

**Files:**
- Modify: `frontend/src/employee-frontend/components/Units.tsx`
- Modify: `frontend/src/employee-frontend/components/child-information/ChildApplications.tsx`
- Modify: `frontend/src/employee-frontend/components/person-search/Search.tsx`
- Modify: `frontend/src/employee-frontend/components/applications/ApplicationsList.tsx`

Reuse the existing `RequirePermittedGlobalAction` component (already in
`utils/roles.tsx`), which takes `oneOf: Action.Global[]`.

- [ ] **Step 1: `Units.tsx:145` — Create Unit button → `CREATE_UNIT`**

Update the `../utils/roles` import to `{ RequirePermittedGlobalAction, hasGlobalAction }`
(drop `RequireRole`; `hasGlobalAction` was added in Task 9 Step 2). Replace the wrapper:

```tsx
          <RequirePermittedGlobalAction oneOf={['CREATE_UNIT']}>
            <div>
              <LegacyButton
                data-qa="create-new-unit"
                className="units-wrapper-create"
                onClick={() => navigate('/units/new')}
                text={i18n.unit.create}
              />
            </div>
          </RequirePermittedGlobalAction>
```

- [ ] **Step 2: `ChildApplications.tsx:54` — Create Application → `CREATE_PAPER_APPLICATION`**

Replace the import `import { RequireRole } from '../../utils/roles'` (line 20) with
`import { RequirePermittedGlobalAction } from '../../utils/roles'`. Replace the wrapper:

```tsx
      <RequirePermittedGlobalAction oneOf={['CREATE_PAPER_APPLICATION']}>
        <AddButtonRow
          text={i18n.childInformation.application.create.createButton}
          onClick={() => toggleUiMode('create-new-application')}
          data-qa="button-create-application"
        />
      </RequirePermittedGlobalAction>
```

- [ ] **Step 3: `Search.tsx:87` — split into two action gates**

Replace `import { RequireRole } from '../../utils/roles'` (line 30) with
`import { RequirePermittedGlobalAction } from '../../utils/roles'`. Replace the single
`RequireRole` wrapper with per-button action gates (VTJ → `CREATE_PERSON_FROM_VTJ`,
create → `CREATE_PERSON`):

```tsx
          <ButtonsContainer>
            <RequirePermittedGlobalAction oneOf={['CREATE_PERSON_FROM_VTJ']}>
              <AddButton
                text={i18n.personSearch.addPersonFromVTJ.title}
                onClick={() => setShowAddPersonFromVTJModal(true)}
                data-qa="add-vtj-person-button"
              />
            </RequirePermittedGlobalAction>
            <Gap $size="s" $horizontal />
            <RequirePermittedGlobalAction oneOf={['CREATE_PERSON']}>
              <AddButton
                text={i18n.personSearch.createNewPerson.title}
                onClick={() => setShowCreatePersonModal(true)}
                data-qa="create-person-button"
              />
            </RequirePermittedGlobalAction>
          </ButtonsContainer>
```

- [ ] **Step 4: `ApplicationsList.tsx:408` and `:520` — service-worker note column → `READ_SERVICE_WORKER_APPLICATION_NOTES`**

Update the import (line 57) from `{ hasRole, RequireRole }` to
`{ hasRole, RequirePermittedGlobalAction }` (`hasRole` stays — still used by the deferred
`enableApplicationActions` at line 184). Replace both `<RequireRole oneOf={['SERVICE_WORKER']}>`
wrappers (the `Td` at line 408 and the `Th` at line 520) with
`<RequirePermittedGlobalAction oneOf={['READ_SERVICE_WORKER_APPLICATION_NOTES']}>` (and the
matching closing tags).

- [ ] **Step 5: Type-check and lint**

Run: `cd frontend && yarn type-check && yarn lint`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/employee-frontend/components/Units.tsx \
        frontend/src/employee-frontend/components/child-information/ChildApplications.tsx \
        frontend/src/employee-frontend/components/person-search/Search.tsx \
        frontend/src/employee-frontend/components/applications/ApplicationsList.tsx
git commit -m "Frontend: gate create/notes UI on permitted global actions"
```

---

### Task 12: ① Global-action inline checks (reports)

**Files:**
- Modify: `frontend/src/employee-frontend/components/reports/PreschoolAbsenceReport.tsx`
- Modify: `frontend/src/employee-frontend/components/reports/AttendanceReservationByChild.tsx`

- [ ] **Step 1: `PreschoolAbsenceReport.tsx` — switch from `roles` to `user` + `hasGlobalAction`**

Replace the import `import { hasRole } from '../../utils/roles'` (line 39) with
`import { hasGlobalAction } from '../../utils/roles'`.
Line 52: change `const { roles } = useContext(UserContext)` to
`const { user } = useContext(UserContext)`.
Line 92: `if (roles.includes('ADMIN')) return true` →
`if (hasGlobalAction(user, 'READ_PRESCHOOL_ABSENCE_REPORT_FOR_AREA')) return true`.
Line 100 (the `useMemo` dependency array `[terms, roles]`): change to `[terms, user]`.
Line 126: `{hasRole(roles, 'ADMIN') && (` →
`{hasGlobalAction(user, 'READ_PRESCHOOL_ABSENCE_REPORT_FOR_AREA') && (`.

- [ ] **Step 2: `AttendanceReservationByChild.tsx:279` — keep the feature flag, swap the role check**

Add `import { hasGlobalAction } from '../../utils/roles'`.
Line 66: change `const { roles } = useContext(UserContext)` to
`const { user } = useContext(UserContext)`.
Line 279:

```tsx
        {featureFlags.aromiIntegration &&
          hasGlobalAction(user, 'READ_AROMI_ORDERS') && (
```

- [ ] **Step 3: Type-check and lint**

Run: `cd frontend && yarn type-check && yarn lint`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/employee-frontend/components/reports/PreschoolAbsenceReport.tsx \
        frontend/src/employee-frontend/components/reports/AttendanceReservationByChild.tsx
git commit -m "Frontend: gate report filters on permitted global actions"
```

---

### Task 13: ② Scoped-action migrations (the two clean sites)

**Files:**
- Modify: `frontend/src/employee-frontend/components/child-information/person-details/AdditionalInformation.tsx`
- Modify: `frontend/src/employee-frontend/components/person-profile/income/IncomeItemBody.tsx`
- Possibly modify: `frontend/src/employee-frontend/components/person-profile/income/IncomeList.tsx` and `PersonIncome.tsx` (only if `PersonContext` is not reachable from `IncomeItemBody`)

- [ ] **Step 1: `AdditionalInformation.tsx:213` — edit button → `Action.Child.UPDATE_ADDITIONAL_INFO`**

This component currently has no permitted-actions access. Use `ChildContext`, the same
source `BackupPickup.tsx` already uses (`const { permittedActions } = useContext(ChildContext)`,
which is a `Set<Action.Child>`). Add the import:

```typescript
import { ChildContext } from '../../../state/child'
```

(verify the exact path/name by reading the `ChildContext` import in
`frontend/src/employee-frontend/components/child-information/BackupPickup.tsx`.)

Add inside the component, near its other hooks:

```typescript
  const { permittedActions } = useContext(ChildContext)
```

Replace the `<RequireRole oneOf={[...7 roles...]}>` wrapper (lines 213-231) with a
permitted-action check, and drop the now-unused `RequireRole` import:

```tsx
{!editing && permittedActions.has('UPDATE_ADDITIONAL_INFO') && (
  <Button
    appearance="inline"
    icon={faPen}
    onClick={startEdit}
    data-qa="edit-child-settings-button"
    text={i18n.common.edit}
  />
)}
```

- [ ] **Step 2: `IncomeItemBody.tsx:42` — application link → `Action.Person.READ_APPLICATIONS`**

The visibility should follow the person's permitted actions. First check whether
`PersonContext` (which exposes `permittedActions: Set<Action.Person>`, used in
`PersonIncome.tsx`) is reachable from `IncomeItemBody`. Read
`frontend/src/employee-frontend/components/person-profile/PersonContext.tsx` (or the file
that defines `PersonContext`) to confirm the provider wraps the income subtree.

If reachable, in `IncomeItemBody.tsx` replace the `UserContext`/`roles` usage (lines 40,42):

```typescript
  const { permittedActions } = useContext(PersonContext)
  // ...
  const applicationLinkVisible = permittedActions.has('READ_APPLICATIONS')
```

and add `import { PersonContext } from '../../person-profile/PersonContext'` (fix the
relative path to match the actual location), removing the now-unused `UserContext` import
if nothing else uses it.

If `PersonContext` is **not** reachable, thread a prop instead: in `PersonIncome.tsx` read
`const { permittedActions } = useContext(PersonContext)` (already present) and pass
`applicationLinkVisible={permittedActions.has('READ_APPLICATIONS')}` down through
`IncomeList` to `IncomeItemBody`, replacing the role computation. Add the boolean prop to
the `IncomeList` and `IncomeItemBody` prop types.

- [ ] **Step 3: Type-check and lint**

Run: `cd frontend && yarn type-check && yarn lint`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/employee-frontend/components/child-information/person-details/AdditionalInformation.tsx \
        frontend/src/employee-frontend/components/person-profile/income/IncomeItemBody.tsx
# include IncomeList.tsx / PersonIncome.tsx if the prop-threading fallback was used
git commit -m "Frontend: gate child-info edit and income application link on scoped permitted actions"
```

---

### Task 14: ③ Replace App.tsx landing-page routing with `user.startPage`

**Files:**
- Modify: `frontend/src/employee-frontend/App.tsx:122-145`

- [ ] **Step 1: Rewrite `RedirectToMainPage`**

Replace lines 122-145 with an exhaustive switch over `user.startPage`:

```tsx
export function RedirectToMainPage() {
  const { loggedIn, user } = useContext(UserContext)

  if (!loggedIn || !user) {
    return <Navigate replace to="~/employee/login" />
  }

  switch (user.startPage) {
    case 'APPLICATIONS':
      return <Navigate replace to="~/employee/applications" />
    case 'UNITS':
      return <Navigate replace to="~/employee/units" />
    case 'REPORTS':
      return <Navigate replace to="~/employee/reports" />
    case 'MESSAGES':
      return <Navigate replace to="~/employee/messages" />
    case 'WELCOME':
      return <Navigate replace to="~/employee/welcome" />
    case 'SEARCH':
      return <Navigate replace to="~/employee/search" />
  }
}
```

- [ ] **Step 2: Remove the now-unused `hasRole` import**

If `App.tsx` imported `hasRole` only for this function, remove it from the
`../utils/roles` import. Run `yarn lint` to confirm no unused imports remain.

- [ ] **Step 3: Type-check and lint**

Run: `cd frontend && yarn type-check && yarn lint`
Expected: PASS (the exhaustive switch covers all `EmployeeStartPage` members, so no
fallthrough/return-type error).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/employee-frontend/App.tsx
git commit -m "Frontend: route post-login landing via backend-computed startPage"
```

---

## Phase 7 — Whole-stack verification

### Task 15: Type-check, lint, and targeted e2e

- [ ] **Step 1: Full frontend type-check and lint**

Run: `cd frontend && yarn type-check && yarn lint`
Expected: PASS.

- [ ] **Step 2: Frontend unit tests**

Run: `cd frontend && yarn test`
Expected: PASS (includes the new `roles.test.tsx`).

- [ ] **Step 3: Backend build + tests**

Run: `cd service && ./gradlew build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: apigw build + tests**

Run: `cd apigw && yarn lint && yarn test`
Expected: PASS.

- [ ] **Step 5: Targeted e2e**

Run the e2e suites covering the touched areas (header navigation, login landing, messages,
finance/invoices, units, applications, child information, reports). Example:

Run: `cd frontend && yarn e2e src/e2e-test/specs/`
(Scope to the relevant spec files; the full suite is large. At minimum run the
employee navigation / login and applications specs.)
Expected: PASS.

- [ ] **Step 6: Manual drift verification**

For each ① "drift" site, confirm the new (backend-config) visibility is the intended one,
and note it in the PR description for QA:
- `Search` add-from-VTJ / create-person: now also visible to ADMIN (backend grants
  `CREATE_PERSON` / `CREATE_PERSON_FROM_VTJ` to ADMIN), where the old role check did not.
- Spot-check that header nav, report filters, and the child-info edit button appear for the
  expected roles via the seeded e2e users.

- [ ] **Step 7: Final commit (if anything was adjusted during verification)**

```bash
git add -A
git commit -m "Fixes from auth-status reorg verification"
```

---

## Done criteria for PR1

- Backend returns `featureConfig` (sibling) and `user.startPage`; the 26 transitional
  `EmployeeFeatures` fields are still populated and marked `@Deprecated`; nothing removed.
- apigw routes `featureConfig` to the `AuthStatus` top level and `startPage` onto the user;
  combined `roles` still sent (deprecated).
- Frontend gates on `permittedGlobalActions` / `featureConfig` / scoped `permittedActions` /
  `user.startPage`; the only remaining `roles` consumers are the six explicitly-deferred
  sites.
- `yarn type-check`, `yarn lint`, `yarn test`, `./gradlew build`, apigw tests, and the
  targeted e2e all pass.

## Follow-up PR (not in scope here)

Once PR1 is deployed and old frontends have drained: remove the 26 deprecated
`EmployeeFeatures` fields (leaving `{ createPlacements }`); migrate the six deferred sites
off `roles` (some need new backend support — `VoucherServiceProviders` area action,
applications-list per-row `permittedActions`, `BackupPickup` per-row actions, a
service-need action reachable from `Group`); then remove the combined `roles` field. End
state: the frontend auth contract carries no role data.
