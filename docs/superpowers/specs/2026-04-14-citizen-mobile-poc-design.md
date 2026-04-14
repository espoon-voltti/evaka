<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Citizen Mobile App — PoC Design

## Goals

Prove that a React Native (Expo) citizen app can deliver two high-value improvements over the responsive web client:

1. **Longer sessions** — municipality security policy permits longer-lived sessions in a native mobile app (device possession acts as a second factor), eliminating the repeated password-entry friction citizens experience today.
2. **Push notifications** on new messages, replacing today's email-only notification.

PoC scope is a **single municipality (Espoo)**, **weak login only** (username + password; no registration flow), and a **subset of messaging** (inbox + thread view + reply). Multi-municipality, strong auth, and the remaining citizen features are explicit follow-ups.

## Non-goals

- Strong authentication (Suomi.fi SAML) on mobile.
- Starting new threads, attachments, drafts, archive/delete.
- Multi-tenant app or municipality-selection UI.
- iOS push verification (requires Apple Developer account + physical device; Android-only for the PoC).
- Offline / background sync.
- Automated test coverage on the Expo side beyond a single Maestro stretch-goal flow.

## Architecture

```
[Expo app]  --HTTPS-->  [apigw / Node.js]  --HTTP + X-User header-->  [service / Kotlin]
                              |                                              |
                              Redis                                          PostgreSQL
                              (web cookie sessions +
                               new mobile bearer sessions)                   (+ new citizen_push_subscription table)
```

Front door, session store, and service process are unchanged. Three new surfaces:

- `citizen-mobile/` — new top-level standalone Expo project.
- `/api/citizen-mobile/*` — new apigw routes for mobile login/logout/status and a proxy for authenticated mobile traffic.
- `/citizen-mobile/v1/*` — new service controllers with per-endpoint versioning and their own DTOs.

Existing surfaces are touched minimally: a new sealed subclass in `AuthenticatedUser`, a new variant in the apigw session user union, a new codegen target, and a new migration. Existing `/citizen/*` endpoints and controllers are unchanged.

## Authentication & session model

### Flow

1. User opens the app and enters username + password.
2. App → `POST /api/citizen-mobile/auth/login/v1` with `{ username, password }`.
3. apigw applies the same per-username, per-hour rate limit the web weak-login route uses.
4. apigw calls the existing internal `citizenWeakLogin` service-client function (the same one the web `auth-weak-login` route uses); on success, receives `{ id }`.
5. apigw mints a **mobile session**: a random 32-byte opaque token, stored in Redis with a long TTL (30 days, sliding), carrying `{ userId, clientType: "mobile", authLevel: "WEAK", createdAt, expiresAt }`. The session is added to the existing per-citizen session index (`usess:<userIdHash>`) so it is covered by the existing "log out all sessions" behavior on credential updates.
6. apigw responds with `{ token, expiresAt, user }`.
7. The app stores `token` in `expo-secure-store` (Keychain / Android Keystore).

On subsequent requests the app sends `Authorization: Bearer <token>`. apigw looks the token up in Redis, refreshes its sliding TTL, builds the internal `X-User` header, and forwards to service.

### Session type separation (defense in depth)

Citizen-mobile and citizen-web sessions are two entirely distinct flavors in Redis and at the service level. A citizen can hold both simultaneously; logging out of one does not affect the other.

- **apigw**: `CitizenSessionUser` union gains a new variant `{ id, authType: "citizen-mobile-weak", userType: "CITIZEN_MOBILE_WEAK" }`. `createUserHeader` emits `{ type: "citizen_mobile_weak", id }`.
- **service**: a new sealed subclass `AuthenticatedUser.CitizenMobile(id, authLevel)` and enum entries `citizen_mobile`, `citizen_mobile_weak` in `AuthenticatedUserType`. `AuthenticatedUserJsonDeserializer` learns the new type tags. `SpringMvcConfig` adds a resolver line for `AuthenticatedUser.CitizenMobile?`.

Endpoint eligibility is enforced by the existing argument-resolver pattern — **no new interceptors or annotations**:

- New `/citizen-mobile/v1/*` controllers declare `user: AuthenticatedUser.CitizenMobile` → the `as? T` cast fails for web sessions and Spring returns `Unauthorized`.
- Existing `/citizen/*` controllers keep declaring `user: AuthenticatedUser.Citizen` → mobile sessions cannot call them.

Password changes invalidate both web and mobile sessions for the affected citizen. This comes for free once mobile sessions are registered in the existing `usess:<userIdHash>` index — the current `auth-weak-update-credentials` route already enumerates that set and deletes every referenced session key.

### Session expiry UX

Any 401 from an authenticated request → the app clears its stored token and routes to the login screen. No silent refresh (PoC).

## API versioning

Per-endpoint versioning, version as a **URL path suffix**:

- `GET /citizen-mobile/messages/threads/v1`
- `GET /citizen-mobile/messages/thread/{id}/v1`
- `POST /citizen-mobile/messages/thread/{id}/reply/v1`
- `POST /citizen-mobile/messages/thread/{id}/mark-read/v1`
- `GET /citizen-mobile/messages/my-account/v1`
- `GET /citizen-mobile/messages/unread-count/v1`
- `POST /citizen-mobile/push-subscriptions/v1`
- `DELETE /citizen-mobile/push-subscriptions/v1`

Each versioned endpoint is an ordinary Kotlin handler method on a regular (non-versioned) controller class. When an endpoint needs a breaking change: add a sibling `@PostMapping(".../v2")` method, copy the DTO to a new type (e.g. `FooRequestV2`), leave `v1` in place, ship the app update, then retire `v1` on a deprecation schedule. Mobile DTOs live in a `messaging/mobile/` package and are never imported by the old `/citizen/*` controllers — compiler-enforced isolation.

This matches the evaka design philosophy that endpoints may serve specific frontend needs and use RPC style; strict REST is not required.

## Push notifications

### Provider

**Expo Push Service** (free; part of the Expo ecosystem). The alternative — direct FCM + APNS — adds native setup without changing the data-residency picture (FCM and APNS are US-operated regardless of whether Expo is in the path).

### Privacy / data residency

Push payloads carry **no personal data**. The push body is a localized generic string (`"You have a new message in eVaka"`) plus a minimal `data` object containing only `{ threadId }`. The app opens the actual message by calling the evaka mobile API over TLS. Nothing sensitive transits Expo's or Google's/Apple's push infrastructure.

### Registration flow

After successful login, on every app launch:

1. `expo-notifications` requests permission (user consent).
2. `Notifications.getExpoPushTokenAsync()` returns the device's Expo push token (`ExponentPushToken[...]`).
3. The app generates a stable device ID on first run (random UUID stored in `expo-secure-store`) and reuses it thereafter.
4. The app `POST`s `{ deviceId, expoPushToken }` to `/api/citizen-mobile/push-subscriptions/v1`.
5. Service **upserts** into a new `citizen_push_subscription` table (PK `(citizen_id, device_id)`).

On logout, the app calls `DELETE /api/citizen-mobile/push-subscriptions/v1` for its current `deviceId`, then clears the stored token.

### Send flow (async-job-based, per `async-jobs.md` conventions)

Two-step fan-out so each outbound push is idempotent per device (matching the pattern in `async-jobs.md`):

**Step 1** — when `MessageService` persists a new message, it plans one `AsyncJob.NotifyCitizenOfNewMessage(messageId, recipientId)` per citizen recipient, via `asyncJobRunner.plan(tx, ...)` inside the same transaction. Planning inside the transaction gives exactly-once semantics: pushes fire only if the message write commits.

**Step 2** — the `NotifyCitizenOfNewMessage` handler loads all `citizen_push_subscription` rows for that citizen and plans one `AsyncJob.SendCitizenMessagePushNotification(messageId, citizenId, deviceId)` per subscription.

The `SendCitizenMessagePushNotification` handler:

- Loads the subscription row for `(citizenId, deviceId)`; if missing (user logged out in the meantime), exits silently.
- Builds a minimal payload with localized title/body and `data: { threadId }`.
- POSTs to `https://exp.host/--/api/v2/push/send`.
- On `DeviceNotRegistered` (or equivalent) in the Expo response, deletes the offending subscription row so we stop trying.

Each send job covers exactly one device, so retries don't duplicate pushes to unaffected devices.

### Pool assignment

The new job type is added to the existing `main` pool to keep PoC scope small. If production throughput reveals contention, a dedicated `citizenPush` pool is a trivial follow-up.

### Platform coverage

- **Android emulator** (dev build + Google Play Services image + FCM credentials configured in Expo once via `eas credentials`): full end-to-end verification.
- **Android physical device**: same story.
- **iOS simulator**: APNS is not wired to simulators, so remote push is not testable there. Local notifications and `xcrun simctl push` work for UI-side checks.
- **iOS physical device**: requires an Apple Developer account ($99/year). Out of PoC scope; covered as a follow-up.

## Database changes

One new migration:

```sql
CREATE TABLE citizen_push_subscription (
    citizen_id       uuid        NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    device_id        uuid        NOT NULL,
    expo_push_token  text        NOT NULL,
    created_at       timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (citizen_id, device_id)
);

CREATE INDEX idx_citizen_push_subscription_citizen ON citizen_push_subscription (citizen_id);
```

No other schema changes.

## Expo app structure

Top-level `citizen-mobile/` (standalone — not a yarn workspace; own `package.json` and lockfile, avoids Metro-vs-workspace friction):

```
citizen-mobile/
  app/
    _layout.tsx                -- Paper ThemeProvider, QueryClient, auth state, i18n
    login.tsx                  -- weak login form
    (authed)/
      _layout.tsx              -- redirects to /login when no token present
      index.tsx                -- inbox (thread list, pull-to-refresh, unread badge)
      thread/[id].tsx          -- thread view + reply composer
      settings.tsx             -- logout, language picker
  src/
    api/
      client.ts                -- fetch wrapper: base URL, bearer header, 401 → logout trigger
      auth.ts                  -- login / logout / status
      messages.ts              -- thread list, thread, reply, unread count, mark-read
      push.ts                  -- register / unregister push subscription
    auth/
      storage.ts               -- expo-secure-store wrapper
      state.tsx                -- AuthProvider + useAuth hook; exposes logout()
    i18n/
      fi.ts, sv.ts, en.ts      -- ported subset (auth + messaging + common)
      index.ts                 -- device-language detection + runtime switcher
    push/
      register.ts              -- permission + token + subscription call
      deviceId.ts              -- stable-UUID-in-secure-store helper
    lib-common/                -- copied from frontend/src/lib-common: query.ts, Result, a couple of tiny helpers
    generated/
      api-clients/             -- emitted by codegen
      api-types/               -- emitted by codegen (self-contained, includes helper types)
  maestro/
    login-and-reply.yaml       -- single stretch-goal flow
  app.json, eas.json, package.json, tsconfig.json, metro.config.js
```

### Key dependencies

`expo`, `expo-router`, `expo-secure-store`, `expo-notifications`, `expo-device`, `expo-localization`, `@tanstack/react-query`, `react-native-paper`, a small i18n library (e.g. `i18n-js`).

### UI and navigation

- **React Native Paper** (Material 3). Espoo brand colors from `lib-customizations` dropped into a Paper theme at the `ThemeProvider`. Covers login inputs, inbox list rows, app bar, badges, activity indicators, snackbars without custom-component work.
- **Expo Router** (file-based). The small number of screens (login, inbox, thread, settings) suits file routing well and matches Expo's current recommendation.

### State management

Follows the evaka convention: server-synced state via the in-repo query framework wrapper (`Queries`, `useQueryResult`, `useMutationResult`, `Result<T>`), local state for UI concerns. The wrapper files are copied from `frontend/src/lib-common/query.ts` into `citizen-mobile/src/lib-common/` — standalone because no workspace link exists. Thread/inbox/unread queries and reply mutations go through the wrapper exactly the same way as in citizen-frontend.

### Localization

All three locales (fi / sv / en) wired through an i18n layer from day one. Initial translations are ported from the web client's existing dictionaries for the auth-screen and messaging subsets. Device locale is the default; users can switch in the settings screen.

### Error handling

| Situation | Behavior |
|---|---|
| Wrong credentials on login | Localized error under form, stay on login |
| Weak-login lockout (existing service behavior) | Localized message, disable submit briefly |
| Network failure during login | Snackbar "Check connection", allow retry |
| 401 on any authenticated request | Clear token, route to `/login` |
| 403 on authenticated request | Localized snackbar, stay on screen |
| Thread list fetch fails | React Query error state → inline retry button |
| Thread detail 404 (deleted) | "This conversation is no longer available" screen |
| Reply fails (network) | Keep draft text in composer, inline error + retry; never lose typed content |
| Reply fails (403, e.g. archived server-side) | Specific error, disable composer |
| Notification permission denied | App works without push; info shown once, no re-prompt loop |
| Push token rotation | Re-fetched on every launch (post-login) and upserted; idempotent |
| Expo returns `DeviceNotRegistered` | Service deletes subscription row |
| Duplicate subscriptions | PK `(citizen_id, device_id)` ensures upsert semantics |
| Push received while app foregrounded | No OS notification; React Query polling / manual invalidation keeps UI fresh |

## apigw additions

- `apigw/src/enduser/routes/auth-mobile-login.ts` — `POST /api/citizen-mobile/auth/login/v1`.
- `apigw/src/enduser/routes/auth-mobile-logout.ts` — `POST /api/citizen-mobile/auth/logout/v1`.
- `apigw/src/enduser/routes/auth-mobile-status.ts` — `GET /api/citizen-mobile/auth/status/v1`.
- `apigw/src/shared/session.ts` — extend with mobile session helpers (create by bearer token, get, delete, refresh sliding TTL). Redis key prefix `mobile-session:<token>`.
- `apigw/src/shared/auth/index.ts` — add the `citizen-mobile-weak` variant; extend `createUserHeader`.
- Middleware that on `/api/citizen-mobile/*` extracts `Authorization: Bearer <token>`, loads the session, refreshes TTL, attaches the user.
- New mount point in `apigw/src/enduser/mapRoutes.ts` (or a sibling `citizen-mobile/mapRoutes.ts`) that proxies authenticated mobile requests to service with the correct `X-User` header.

## service additions

Under `fi.espoo.evaka.messaging.mobile` (domain-driven organization — all new mobile messaging concerns in one package):

- `MessageControllerMobile.kt` — `@RestController @RequestMapping("/citizen-mobile")` with handler methods listed in the versioning section. Each endpoint authorizes and audit-logs per the evaka convention. Controllers reuse the deep `MessageService` / `MessageQueries` layer — only the DTO shapes and URLs are new.
- `MessageMobileDtos.kt` — mobile-only request/response types; no imports from existing citizen messaging DTOs.
- `CitizenPushSubscriptionController.kt` — subscription upsert/delete endpoints.
- `CitizenPushSubscriptionQueries.kt` — raw-SQL queries for the new table.
- `CitizenMessagePushNotifications.kt` — async-job handler that dispatches Expo pushes.

In `fi.espoo.evaka.shared.async`:

- `AsyncJob.kt` — two new payloads (`NotifyCitizenOfNewMessage`, `SendCitizenMessagePushNotification`), both added to the `main` pool.

In `fi.espoo.evaka.shared.auth`:

- `AuthenticatedUser.kt` — new `CitizenMobile` subclass + enum entries.
- `AuthenticatedUserJsonDeserializer.kt` — parse the new type tags.

In `fi.espoo.evaka.shared.config`:

- `SpringMvcConfig.kt` — one added resolver line for `AuthenticatedUser.CitizenMobile?`.

## Codegen

In `service/codegen/`:

- Add `TsProject.CitizenMobile` to `TsCode.kt`.
- In `ApiFiles.kt`, add a filter+grouping rule for endpoints starting with `/citizen-mobile/`, output to `TsProject.CitizenMobile / "src/generated/api-clients/{package}.ts"`.
- Because the Expo project is standalone (no workspace link to `lib-common`), emit **self-contained** shared types into `TsProject.CitizenMobile / "src/generated/api-types/{package}.ts"`. Helper types (`LocalDate`, `Id`, etc.) are duplicated into the mobile tree rather than imported across project boundaries; duplication is acceptable in exchange for isolation.

## Testing

### Backend (Kotlin, matching existing conventions)

In `service/src/integrationTest/kotlin/fi/espoo/evaka/messaging/mobile/`:

- `MessageControllerMobileIntegrationTest` — list threads, get thread, reply, unread count, mark-read. Happy paths plus the most relevant failures (403 on unowned thread, 404 on missing thread).
- `CitizenPushSubscriptionControllerIntegrationTest` — upsert, delete, PK-uniqueness behavior.
- `CitizenMobileAuthSeparationTest` — asserts that a `citizen_mobile_weak` user header cannot call a `/citizen/*` endpoint and that a `citizen` / `citizen_weak` header cannot call a `/citizen-mobile/*` endpoint. This is the end-to-end check that the argument-resolver-based separation actually prevents crossover.
- `CitizenMessagePushNotificationsTest` — with a fake outbound HTTP client: batching, payload shape, localization lookup, deletion-on-`DeviceNotRegistered`. Uses `runPendingJobsSync()` per the async-jobs testing convention.

### apigw (TypeScript, Vitest)

In `apigw/src/enduser/__tests__/`:

- `auth-mobile-login.test.ts` — happy path returns token + stores session; wrong creds → 401; Redis TTL written correctly.
- `auth-mobile-proxy.test.ts` — bearer token valid → `X-User` populated with `citizen_mobile_weak`; missing/invalid token → 401; session lookup refreshes TTL.

### Expo app

No component/unit tests. Manual testing on an Android emulator (dev build) is the primary feedback loop.

**Stretch goal (end of PoC)**: one Maestro happy-path flow in `citizen-mobile/maestro/login-and-reply.yaml`:

- Launch app → fill username + password → submit
- Assert inbox visible
- Tap first thread → assert messages visible → type reply → tap Send
- Assert new message appears in thread

Runs against a locally-running evaka stack seeded via `dev-api` (same pattern as the existing Playwright suite). Setup documented in `citizen-mobile/README.md`.

## Out of PoC, captured as follow-ups

- Strong authentication via Suomi.fi (in-app browser + token exchange).
- Full messaging parity: new threads, attachments, drafts, archive/delete, redacted-thread view.
- iOS push verification (Apple Developer account + physical iPhone).
- Deployment to Espoo staging + TestFlight / Play Internal Testing.
- Multi-municipality story (separate apps per municipality vs. one app with a picker).
- Automated testing beyond the single Maestro flow.
- Performance / load testing of the push fan-out.
