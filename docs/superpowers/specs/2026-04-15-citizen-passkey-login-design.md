<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Citizen passkey login — design

**Status:** PoC
**Date:** 2026-04-15
**Scope:** eVaka citizen frontend + apigw + Kotlin service

## 1. Goal

Add WebAuthn/passkey login as the preferred sign-in method for eVaka citizens. The feature must:

- Behave like the existing email/password weak login: same session lifetime, same weak auth level, same refusal of strong-auth-only features.
- Require a prior suomi.fi (strong) login to enrol a passkey onto a citizen's account.
- Offer enrolled-passkey management in user settings: list, rename, view last-used date, revoke.
- Nudge non-enrolled users via a dismissible post-login toast.
- Redesign the citizen `/login` page so passkey is the primary option. The layout adapts to whether the app is installed as a PWA (installed → email/password demoted to a "more options" disclosure; browser → all three options visible with passkey promoted).
- Ship as a PoC with no database migrations and no infrastructure changes. Credential persistence reuses the S3-backed pattern introduced by the citizen web push feature.

## 2. Architecture overview

Responsibilities split three ways:

- **Frontend (`citizen-frontend`)** — new `passkey/` module. Reuses `@simplewebauthn/browser` to drive `startRegistration` and `startAuthentication`. Calls apigw for options/verify and for management (list/rename/revoke). Renders a new section in `PersonalDetails` and modifies `LoginPage`. Hosts a dismissible enrolment nudge toast.
- **api-gateway (`apigw/src`)** — new passkey route files. WebAuthn ceremony via `@simplewebauthn/server`. Pending challenges kept in Redis under a per-ceremony token with a 5-minute TTL. Persistence is delegated to the Kotlin service over internal HTTP. Session establishment reuses the existing `sessions.login(req, user)` machinery with a new discriminated-union variant on `CitizenSessionUser`.
- **Kotlin service (`service/`)** — new `citizenpasskey/` package. `CitizenPasskeyCredentialStore` persists one JSON document per citizen to the shared S3 data bucket, mirroring `CitizenPushSubscriptionStore`. `CitizenPasskeyController` exposes citizen-facing endpoints (list, rename, revoke) and two `/internal/...` endpoints used only by apigw.

This matches how the rest of eVaka is wired: apigw owns auth ceremonies (SAML, weak login), Kotlin owns persistence.

### 2.1 Login data flow (discoverable credentials)

1. Browser → `POST /api/citizen/auth/passkey/login/options`. apigw generates a WebAuthn challenge via `generateAuthenticationOptions` with no `allowCredentials` list (discoverable credentials), stores `{challenge, flow: 'login'}` in Redis under a random 128-bit token key, returns `{token, options}`.
2. Browser calls `startAuthentication(options)`. On success the assertion contains the `userHandle` — which we set to the citizen's `PersonId` as 16 bytes at enrolment time.
3. Browser → `POST /api/citizen/auth/passkey/login/verify` with `{token, assertion}`.
4. apigw reads and deletes the Redis entry, decodes `userHandle` → `PersonId`, HTTP-calls Kotlin `GET /internal/citizen-passkey/credentials/{personId}`, finds the matching `credentialId` in the returned list, then runs `verifyAuthenticationResponse` using the stored public key and sign counter.
5. On success, apigw HTTP-calls Kotlin `POST /internal/citizen-passkey/credentials/{personId}/{credentialId}/touch` (bumps `signCounter`, updates `lastUsedAt`), then `sessions.login(req, { authType: 'citizen-passkey', userType: 'CITIZEN_WEAK', id: personId, credentialId })`. Returns `200`.
6. Frontend invalidates `authStatusQuery` and navigates to the `?next=` target (or `/`).

### 2.2 Enrolment data flow

Symmetric to login, with the pre-condition that `req.user.userType === 'CITIZEN_STRONG'`:

1. Browser → `POST /api/citizen/auth/passkey/register/options`. apigw verifies the strong-auth gate, calls `generateRegistrationOptions` with `userID = PersonId as 16 bytes`, `userName` and `userDisplayName` taken from the citizen's name, `residentKey: 'required'`, `userVerification: 'preferred'`, `attestationType: 'none'`. apigw queries Kotlin for the current list of this user's credentials and passes them as `excludeCredentials` so the same authenticator cannot be double-registered. Challenge stored in Redis under a token.
2. Browser calls `startRegistration(options)`, returns attestation.
3. Browser → `POST /api/citizen/auth/passkey/register/verify` with `{token, attestation}`.
4. apigw reads and deletes the Redis entry, asserts the challenge's bound `personId` matches `req.user.id` (a defence against session mid-flow substitution), runs `verifyRegistrationResponse`, then HTTP-calls Kotlin `POST /internal/citizen-passkey/credentials` with the new credential payload. Device hint is derived from the request's `User-Agent` at this step.
5. Returns `200 { credentialId, label }`.

### 2.3 Management data flow

Plain pass-through. apigw exposes:

- `GET /api/citizen/passkey/credentials` → Kotlin `GET /citizen/passkey/credentials`
- `PATCH /api/citizen/passkey/credentials/:id` → Kotlin `PATCH /citizen/passkey/credentials/:id`
- `DELETE /api/citizen/passkey/credentials/:id` → Kotlin `DELETE /citizen/passkey/credentials/:id`

All three are allowed from any authenticated citizen session (weak or strong). The current `credentialId` is exposed via `AuthStatus` so the frontend can flag "this device" in the list. On delete of the current-session credential, apigw additionally destroys the caller's session.

## 3. Data model

### 3.1 S3 store

Bucket: shared `bucketEnv.data` — the same one used by `CitizenPushSubscriptionStore`.

Key layout:

```
citizen-passkey-credentials/<personId>.json
```

One file per citizen. Last-write-wins — no ETag, no optimistic locking. Same PoC tradeoff as the push store.

### 3.2 Store document

```kotlin
data class CitizenPasskeyStoreFile(
    val personId: PersonId,
    val credentials: List<CitizenPasskeyCredential>,
)

data class CitizenPasskeyCredential(
    val credentialId: String,      // base64url of raw credential ID bytes
    val publicKey: String,         // base64url of COSE public key bytes
    val signCounter: Long,
    val transports: List<String>,  // e.g. ["internal", "hybrid"]
    val createdAt: HelsinkiDateTime,
    val lastUsedAt: HelsinkiDateTime?,
    val label: String,             // user-editable display name
    val deviceHint: String?,       // parsed UA fragment set at enrolment
)
```

Notes:

- `credentialId` and `publicKey` are stored as base64url strings because `@simplewebauthn/server@^13` accepts strings directly in its verify call — no byte-array conversion gymnastics on the apigw side.
- No `userHandle` on the credential. The `userHandle` emitted during enrolment is always `PersonId` as 16 bytes, and on login verify we decode it back to `PersonId`. Uniqueness is guaranteed by `PersonId`, which is already a UUID.
- `deviceHint` is set from the request's `User-Agent` header at enrolment via a small helper (`ua-parser-js`-free, grep-based) that returns short strings like `"iPhone (Safari)"`, `"Android (Chrome)"`, `"Mac (Safari)"`. Fallback: `"Unknown device"`. The initial `label` is the `deviceHint`. Users can rename via `PATCH`.
- No credential-count limit (PoC).
- No IP tracking and no UA archival — only the enrolment-time `deviceHint`.

### 3.3 Jackson mapper

Local-instance pattern identical to `CitizenPushSubscriptionStore`:

```kotlin
private val jsonMapper = defaultJsonMapperBuilder().build()
```

No injection of the Spring `JsonMapper` bean.

### 3.4 Session variant

Add to `CitizenSessionUser` in `apigw/src/shared/auth/index.ts`:

```ts
| { id: string; authType: 'citizen-passkey'; userType: 'CITIZEN_WEAK'; credentialId: string }
```

`createUserHeader` maps this variant to the same downstream header as `citizen-weak` (`type: 'citizen_weak'`) so the Kotlin service sees no difference between the two weak auth flavours — all existing weak-auth business-rule checks apply automatically.

The `credentialId` on the session is returned via `AuthStatus` so the management UI can flag "this device" in the credential list.

### 3.5 Redis challenge entry

```
key:   passkey-challenge:<random-128-bit-hex>
value: JSON { challenge: string, flow: 'register' | 'login', personId?: string }
TTL:   300 seconds
```

Registration challenges bind the currently-strong-authenticated `personId` at options time. The verify handler asserts this matches the session's current `req.user.id` — a defence against mid-flow session substitution. Login challenges carry no `personId`.

### 3.6 AuthStatus response

The existing `getAuthStatus()` response gains one optional field: `passkeyCredentialId: string | null`, present only when the session is a passkey session. Populated from `req.user.credentialId`. Consumed by the management UI.

## 4. Login page redesign

### 4.1 Detection

New hook `useIsStandalone()`:

```ts
const isStandalone =
  (typeof window !== 'undefined' &&
    window.matchMedia('(display-mode: standalone)').matches) ||
  (typeof navigator !== 'undefined' &&
    (navigator as unknown as { standalone?: boolean }).standalone === true)
```

Captured once at mount, not subscribed — a user changing display modes mid-session is out of scope.

### 4.2 Browser mode (not standalone)

Three login options, passkey promoted to primary:

1. **Primary** — "Sign in with passkey" (large button, key icon, subtitle "Fast, passwordless sign-in on this device"). Calls the passkey login flow.
2. **Secondary** — "Sign in with suomi.fi" (unchanged strong login link, rendered as secondary button).
3. **Tertiary** — "Sign in with email and password" (link to `/login/form`, rendered as tertiary/text button).

Visual hierarchy achieved via existing lib-components button variants and vertical spacing — no custom styles.

### 4.3 Standalone mode (installed PWA)

Two-tier layout:

1. **Primary** — "Sign in with passkey" (identical to browser mode).
2. **Secondary** — "Sign in with suomi.fi".
3. **"More sign-in options" disclosure** — collapsed by default. When expanded, reveals the "Sign in with email and password" link. Uses an existing lib-components expander or a minimal `<details>`/`<summary>` pair — no new expander component.

### 4.4 Zero-passkey handling

The passkey primary button is rendered unconditionally. We cannot know if this browser/device has any discoverable credentials until the prompt runs. On `NotAllowedError` (no matches / user cancel / timeout), the frontend shows an inline hint beneath the button:

> "No passkeys found on this device. Sign in with suomi.fi to enroll one, or use another device that has one."

The button stays active for retry.

### 4.5 Error surfaces

All inline or as toasts via `NotificationsContext`. No full-page error states.

- User cancel / no matches → silent or the inline hint above.
- Network failure during options/verify → toast `"Sign-in failed. Please try again."`.
- Unknown `userHandle`, bad signature → toast `"Sign-in failed."` — generic, to avoid user enumeration.

### 4.6 File layout

- `citizen-frontend/login/LoginPage.tsx` — modified to use `useIsStandalone` and render the tiered layout.
- `citizen-frontend/passkey/PasskeyLoginButton.tsx` — new. Wraps the passkey button and its inline error hint. Calls `usePasskeyAuth().login()`.
- `citizen-frontend/passkey/usePasskeyAuth.ts` — new. Shared hook for login and enrolment ceremonies. Wraps `@simplewebauthn/browser` and the apigw fetch calls.
- `citizen-frontend/hooks/useIsStandalone.ts` — new. Small helper hook.

### 4.7 i18n

New strings under `login.passkey.*` in `fi.tsx`, `sv.tsx`, `en.tsx`: `title`, `subtitle`, `noCredentialsHint`, `failed`, `moreOptionsDisclosure`. Modelled on the existing `webPushSection.*` block.

## 5. Personal-details passkey management section

### 5.1 Placement

New `PasskeySection` component embedded in `PersonalDetails.tsx`, rendered as a peer section between `LoginDetailsSection` and `NotificationSettingsSection`, separated by `<HorizontalLine>`. Anchor: `#passkeys`. Matches the existing `#notifications` hash-scroll pattern already in `PersonalDetails.tsx`.

### 5.2 File layout

- `citizen-frontend/passkey/PasskeySection.tsx` — section title, intro, list, add button.
- `citizen-frontend/passkey/PasskeyListItem.tsx` — one row per credential.
- `citizen-frontend/passkey/queries.ts` — `Queries` block exposing `listPasskeysQuery`, `renamePasskeyMutation`, `revokePasskeyMutation`.
- `citizen-frontend/passkey/usePasskeyAuth.ts` — shared with login.

### 5.3 Section contents

1. **Title + short intro paragraph** explaining what passkeys are in one sentence.
2. **List of enrolled credentials**, via `listPasskeysQuery`. Each row:
   - Label (click to edit inline — same pattern as the email field in `PersonalDetailsSection`).
   - `deviceHint` as muted subtitle.
   - `Created <date>` and `Last used <date>` (relative-ish formatting: `"today"`, `"yesterday"`, or `YYYY-MM-DD`).
   - A `"This device"` badge when the credential's `credentialId` matches `authStatus.passkeyCredentialId`.
   - A `"Revoke"` button opening a confirmation modal.
3. **Empty state** — `"You haven't added any passkeys yet."` + the Add button.
4. **Add passkey button** — always rendered. Behaviour depends on current auth level:
   - Strong session → triggers `usePasskeyAuth().enroll()` directly.
   - Weak session → hard-navigates to `getStrongLoginUri('/personal-details?enroll=1#passkeys')`. A secondary hint is rendered next to the button: `"Requires suomi.fi identification"`.
5. **Auto-enrol on return.** A `useEffect` at the top of `PersonalDetails.tsx` reads `?enroll=1` from `useSearchParams`. If present and `user.authLevel === 'STRONG'`, it clears the param via `setSearchParams`, calls `enroll()` once (guarded by a ref so it cannot re-trigger under StrictMode re-mounts).

### 5.4 Renaming

Inline edit submits via `renamePasskeyMutation`, which invalidates `listPasskeysQuery`. Client-side validation: trimmed, non-empty, ≤ 80 chars. Server-side validation mirrors.

### 5.5 Revoking

Confirmation modal using the existing `AsyncButton` + `InfoModal` pattern. Message: `"Revoke <label>?"`. Warning appended when it's the only credential *and* the current session is a passkey session: `"You are currently signed in with this passkey. Revoking it will sign you out."`.

On success:
- Invalidate `listPasskeysQuery`.
- If the revoked credential is the current-session one, invalidate `authStatusQuery` — the auth status will come back anonymous, `RequireAuth` bounces to `/login`.

### 5.6 i18n

Under `personalDetails.passkeySection.*`, modelled on `webPushSection.*`:

`title`, `intro`, `empty`, `addButton`, `strongRequired`, `thisDevice`, `createdAt`, `lastUsedAt`, `neverUsed`, `revoke`, `revokeConfirmTitle`, `revokeConfirmText`, `revokeConfirmLastWarning`, `rename`, `labelLabel`, `labelValidationEmpty`, `labelValidationTooLong`.

## 6. Enrolment nudge toast

### 6.1 Hook location

New hook `usePasskeyEnrollNudge()` called once near the top of the citizen app tree, adjacent to the existing `ChildStartingNotificationHook`. Consumes `AuthContext` and `NotificationsContext` directly — no new context.

### 6.2 Trigger conditions (all must hold)

1. User is authenticated (`user.authLevel` is `STRONG` or `WEAK`).
2. `listPasskeysQuery` has loaded and returned an empty list.
3. No dismissal cookie for the current `user.id` is set.
4. Browser supports WebAuthn: `typeof window.PublicKeyCredential === 'function'`. Silent skip otherwise.

### 6.3 Dismissal storage

Cookie, per the `ChildStartingNotificationHook` precedent:

```
name:    evaka-passkey-enroll-dismissed-<user.id>
value:   'true'
expires: now + 30 days
path:    /
SameSite: Strict
```

The cookie is set from the toast's `onClose`. If the user enrols via another entry point (settings), the next effect run sees a non-empty credential list and removes the toast anyway — no explicit cleanup needed.

### 6.4 Toast content

Single toast via `addNotification(...)` with stable `customId` `'passkey-enroll-nudge'` to prevent double-add under StrictMode. Uses the existing `lib-components/molecules/Toast` styling.

- Title: `"Try signing in with a passkey"`
- Body: one short sentence about faster sign-in on this device.
- Primary action: `"Set up passkey"` button.
- Close button: standard dismissal → writes the cookie.

### 6.5 Action behaviour

Same smart-gate as the settings Add button:
- Strong session → navigate to `/personal-details?enroll=1#passkeys`. The auto-enrol effect on that page triggers the enrolment flow.
- Weak session → hard-navigate to `getStrongLoginUri('/personal-details?enroll=1#passkeys')`.

In both cases the action also calls `removeNotification('passkey-enroll-nudge')` before navigating.

### 6.6 i18n

Under `personalDetails.passkeySection.enrollNudge.*`: `title`, `body`, `action`, `dismiss`.

## 7. Error handling and edge cases

### 7.1 WebAuthn client errors

| Error | Login | Enrolment |
|---|---|---|
| `NotAllowedError` | Silent on explicit cancel; inline hint when it signals no credentials | Silent |
| `InvalidStateError` (already registered) | n/a | Inline `"This device already has a passkey for your account"` |
| `NotSupportedError` | Toast `"Your device does not support this sign-in method"` | Same |
| `SecurityError` (RPID mismatch / insecure context) | Toast `"Sign-in failed"`; log server-side | Same |
| Any other throw | Generic toast; apigw logs full error |

### 7.2 apigw failures

- **Challenge token missing or expired in Redis** → `410 Gone`, error code `passkey-challenge-expired`. Frontend: `"The sign-in prompt took too long. Please try again."`.
- **Verification failure** (bad signature, counter regression, RPID/origin mismatch) → `401`, code `passkey-verification-failed`. Counter regression additionally logs a warning — possible cloned credential.
- **Unknown `userHandle` on login verify** → `401`, same generic code as bad signature. Never distinguish (avoids user enumeration).
- **Enrolment without strong auth** → `403`, code `strong-auth-required`. Authoritative guard, even though the UI should already block.
- **Enrolment challenge `personId` mismatch** → `403`, code `strong-auth-required`. Same treatment.
- **Internal HTTP call to Kotlin fails** → `502`, code `upstream-unavailable`. Generic retry toast.

### 7.3 Kotlin failures

- **S3 `NoSuchKey` on load** → treated as empty credential list. No 404.
- **Concurrent S3 write race** (two enrolments in milliseconds) → last-write-wins, documented as PoC limitation. Acceptable given enrolments require strong auth and are infrequent.
- **Any other S3 error** → 500; apigw maps to 502.
- **Touch on non-existent credential ID** → `404 credential-not-found`. Only happens if another tab just revoked the credential mid-login.

### 7.4 Session-level edges

- **Revoking the current-session passkey** → apigw calls `sessions.destroy(req)` after a successful Kotlin delete. Frontend re-fetches auth status, gets anonymous, `RequireAuth` bounces to `/login`.
- **Passkey login while already logged in** → apigw calls `sessions.destroy(req)` before `sessions.login(req, ...)` so stale data from the previous session is gone.
- **Counter regression** → SimpleWebAuthn reports `newCounter < storedCounter`; apigw rejects the verification and logs. Standard clone-detection signal.

### 7.5 Rate limiting

- Login options+verify: reuse the existing `authWeakLogin` rate limiter (per-IP). `login/options` and `login/verify` share one bucket.
- Enrolment: no extra limiter beyond the strong-auth gate — SAML login already rate-limits upstream.

## 8. Local development and deployment

### 8.1 apigw environment variables

Add to `apigw/src/shared/config.ts`:

```
PASSKEY_RPID            - default "localhost"
PASSKEY_ORIGIN          - default "http://localhost:9099"
PASSKEY_RP_NAME         - default "eVaka"
```

In staging the values become the real domain (e.g. `RPID=<staging-host>`, `ORIGIN=https://<staging-host>`). Passkeys are domain-bound — a passkey enrolled against `localhost` cannot be used against staging and vice versa.

### 8.2 Local dev

- `compose/ecosystem.config.js` injects the three env vars with localhost defaults.
- Chrome DevTools Virtual Authenticator panel is the recommended manual test tool: enable it, enrol a passkey via the settings flow, then sign out and sign in again with the passkey button.
- Local apigw runs behind the same Vite proxy already used for citizen-frontend; no TLS is required in dev because localhost is a secure context.

### 8.3 No infrastructure changes

- No new S3 bucket, no Terraform diff, no IAM change — the `bucketEnv.data` bucket and its existing IAM grant are reused.
- No new SSM parameters.
- No database migrations.
- No new Redis keys beyond the ephemeral `passkey-challenge:<token>` namespace which lives in the existing Redis instance.

## 9. Testing (PoC minimum)

### 9.1 Kotlin

- `CitizenPasskeyCredentialStoreTest` — one integration test that does the full round-trip: save a credential, load it, upsert a second one, rename, revoke, assert the JSON shape at each step.

### 9.2 apigw

- `passkey-auth.spec.ts` — two Vitest cases with `@simplewebauthn/server` and Redis mocked:
  1. Register options → verify happy path, asserting the internal POST to the Kotlin service is made with the expected body.
  2. Login options → verify happy path, asserting `sessions.login` is called with `authType: 'citizen-passkey'` and the correct `personId`.

No error-branch coverage.

### 9.3 Frontend

- `PasskeySection.spec.tsx` — one render smoke test covering: list rendering, empty state, and the "this device" badge when the current `credentialId` matches.
- `LoginPage.spec.tsx` — one added case asserting the passkey primary button renders, and a brief `matchMedia`-mocked assertion that the standalone branch hides the email/password tertiary button behind the "more options" disclosure.

### 9.4 End-to-end

Skipped for the PoC. The frontend + apigw smoke tests are enough to catch the "main flow broken" class of regressions we care about for demo iteration.

## 10. File layout summary

### Kotlin

```
service/src/main/kotlin/fi/espoo/evaka/citizenpasskey/
  CitizenPasskeyConfig.kt              # Spring @Configuration, @Bean wiring
  CitizenPasskeyController.kt          # /citizen/passkey/* + /internal/citizen-passkey/*
  CitizenPasskeyCredentialStore.kt     # S3-backed store
  CitizenPasskeyStoreFile.kt           # data classes
service/src/integrationTest/kotlin/fi/espoo/evaka/citizenpasskey/
  CitizenPasskeyCredentialStoreTest.kt # one integration test
```

### apigw

```
apigw/src/enduser/routes/
  passkey-auth.ts                      # register + login options/verify
  passkey-credentials.ts               # list/rename/revoke pass-through
apigw/src/shared/
  auth/index.ts                        # modified: add citizen-passkey variant
  session.ts                           # modified: include passkey session save/load
apigw/src/enduser/routes/auth-status.ts  # modified: expose passkeyCredentialId
apigw/src/app.ts                       # modified: mount new routes
apigw/src/__tests__/
  passkey-auth.spec.ts                 # two smoke cases
```

New dependency: `@simplewebauthn/server` (matches greenhouse-solar-heater).

### Frontend

```
frontend/src/citizen-frontend/passkey/
  PasskeySection.tsx
  PasskeyListItem.tsx
  PasskeyLoginButton.tsx
  usePasskeyAuth.ts
  usePasskeyEnrollNudge.ts
  queries.ts
frontend/src/citizen-frontend/hooks/
  useIsStandalone.ts
frontend/src/citizen-frontend/login/LoginPage.tsx          # modified
frontend/src/citizen-frontend/personal-details/PersonalDetails.tsx # modified: embed PasskeySection + auto-enrol effect
frontend/src/citizen-frontend/App.tsx                      # modified: mount usePasskeyEnrollNudge
frontend/src/citizen-frontend/passkey/PasskeySection.spec.tsx
frontend/src/citizen-frontend/login/LoginPage.spec.tsx     # modified
frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx  # modified
frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx  # modified
frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx  # modified
```

New dependency: `@simplewebauthn/browser`.

## 11. Out of scope

- Conditional mediation (`autocomplete="username webauthn"` + silent autofill).
- Passkey sync across authenticators beyond what the OS provides natively.
- Cross-device add-another-device flows beyond what the browser's native "use passkey on another device" button offers.
- Migration from email/password to passkey as a single-action upgrade.
- Server-persisted dismissal state for the enrolment toast.
- Any database table or migration.
- Any Terraform or IAM change.
- End-to-end Playwright tests.
