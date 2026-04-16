<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Citizen Web Push Notifications — POC Design

**Date:** 2026-04-14
**Status:** Draft — proof of concept
**Scope:** `evaka/frontend` (citizen app), `evaka/service` (Kotlin backend)

## Goal

Let citizens receive web push notifications for new messages and replies to existing threads. The POC must be deployable to staging without any database migration, any new S3 bucket, any new IAM, or any new VAPID key provisioning. Everything reuses infrastructure that is already deployed.

## Non-goals (explicit)

- No database migration. A `citizen_push_subscription` table is a follow-up.
- No new S3 bucket or IAM changes. We write to the existing `evaka-{env}-data` bucket, which the service already has read/write/delete access to.
- No new VAPID key pair. We reuse the keys `webpush/WebPush.kt` already consumes for employee-mobile push.
- No async-job queue or background worker. The message-send hook is synchronous and best-effort.
- No rate limiting.
- No per-category notification icons — one shared evaka icon for all three categories.
- No analytics/metrics beyond existing Sentry breadcrumbs.

## User-facing behaviour

A citizen opens **Personal details → Notification settings**. Below the existing email-notification section, a new "Push notifications" section appears. It contains:

1. A master enable toggle. When the citizen flips it on for the first time, the browser prompts for notification permission. On grant, a subscription is created and posted to the server, and the citizen immediately receives a test notification so they can verify it works.
2. Three category checkboxes, enabled only when the master toggle is on:
   - **Urgent messages** — new messages flagged urgent, regardless of thread type
   - **Normal messages** — non-urgent new messages and replies in `MESSAGE`-type threads
   - **Bulletins** — new messages in `BULLETIN`-type threads
3. A "Send test notification" button (always available when subscribed) so the citizen can re-verify anytime.
4. An inline expandable **PermissionGuide** that appears when the browser or OS has push disabled or unsupported. The guide shows user-agent-specific instructions covering Chrome/Edge/Firefox/Samsung-Internet on Android, Safari on iOS (including the "install as PWA first" prerequisite), Chrome/Edge/Firefox on desktop, and Safari on macOS.

When a push arrives and the citizen clicks the notification, the browser opens (or focuses) the tab at `/messages/{threadId}`, deep-linking straight to the thread.

## Architecture overview

```
┌─ Browser (citizen) ─────────────┐     ┌─ evaka-service (Kotlin) ────┐     ┌─ S3 ────────────┐
│ NotificationSettingsSection     │     │ CitizenWebPushController    │     │ evaka-{env}-data│
│  └─ WebPushSettingsSection      │ ──► │  GET  /citizen/push/vapid-key│     │                 │
│     └─ PermissionGuide          │     │  PUT  /citizen/push/subscription│◄►│ citizen-push-   │
│                                 │     │  DELETE ...                 │     │ subscriptions/  │
│ service-worker.js               │     │  POST /citizen/push/test    │     │ {personId}.json │
│  ├─ push handler  ──────────┐   │     │                             │     └─────────────────┘
│  └─ click handler  ──► /msg │   │     │ CitizenPushSubscriptionStore│
└───────────────────────────┬─┘   │     │  (reads/writes S3 JSON)     │     ┌─ Push service ──┐
                            │     │     │                             │     │ FCM / APNs /    │
  Notification.requestPermission()│     │ CitizenPushSender ──────────┼───► │ Mozilla autoPush│
                            │     │     │  (reuses webpush/WebPush.kt │     └─────────────────┘
                            ▼     │     │   for VAPID + encryption)   │
                     pushManager  │     │                             │
                     .subscribe() │     │ MessageService.sendMessage  │
                            │     │     │  └─ emails (existing)       │
                            ▼     │     │  └─ CitizenPushSender.send()│ ◄── new hook
            POST /citizen/push/subscription                        └──┘
```

### Request flows

**Enable (first time).** UI calls `Notification.requestPermission()`, then `pushManager.subscribe(vapidKey)`, then `PUT /citizen/push/subscription` with the new subscription and the citizen's selected categories. The controller writes the file; if no file previously existed for this `personId`, the controller additionally calls `CitizenPushSender.sendTest(personId)` so the citizen receives a real push confirming delivery works. Response: `{ sentTest: true }`.

**Update categories.** Same `PUT /citizen/push/subscription` endpoint. Upsert keyed on `endpoint`. No test notification.

**Unsubscribe one device.** `DELETE /citizen/push/subscription` with `{ endpoint }`. Removes just that subscription. If the array becomes empty, the file is deleted.

**Message arrives.** `MessageService` already determines per-recipient email-notification eligibility. We add a parallel call in the same place: for each citizen recipient, compute `category = if (urgent) URGENT_MESSAGE else if (messageType == BULLETIN) BULLETIN else MESSAGE` and call `citizenPushSender.notifyMessage(personId, threadId, category, senderName)`. Sender loads the store, filters subscriptions whose `enabledCategories` contains the category, builds a payload, and delegates each send to `WebPush.kt`. On HTTP 410/404 from the push service, the dead subscription is removed from the file.

## Storage format

**S3 key:** `citizen-push-subscriptions/{personId}.json` in `evaka-{env}-data`.

```json
{
  "personId": "00000000-0000-0000-0000-000000000000",
  "subscriptions": [
    {
      "endpoint": "https://fcm.googleapis.com/fcm/send/...",
      "ecdhKey": "BNc...",
      "authSecret": "tBHI...",
      "enabledCategories": ["URGENT_MESSAGE", "MESSAGE", "BULLETIN"],
      "userAgent": "Mozilla/5.0 (...)",
      "createdAt": "2026-04-14T10:23:45Z"
    }
  ]
}
```

- **One file per citizen** avoids cross-user write contention. Per-citizen contention (same user toggling from two tabs) is tolerated with last-write-wins — acceptable for a POC.
- **Categories live per-subscription.** A citizen can opt into urgent-only on their phone while their desktop gets everything.
- **Field names match the existing `webpush/WebPush.kt` data classes** (`ecdhKey`, `authSecret`) so values can be passed straight through to the sender.
- **"No file" = "not subscribed."** First-time enable is detected by "file did not exist before this write."
- **Empty subscriptions array** after a 410/404 cleanup or an explicit delete → the file is removed entirely.

## Backend components

New Kotlin package: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/`

### `CitizenPushCategory.kt`

```kotlin
enum class CitizenPushCategory {
    URGENT_MESSAGE,
    MESSAGE,
    BULLETIN,
}
```

Intentionally separate from the existing `webpush/PushNotificationCategory` (which is employee-mobile-only). Decoupling lets the two systems evolve independently.

### `CitizenPushSubscription.kt`

Jackson-annotated data classes for the JSON schema above:

```kotlin
data class CitizenPushStoreFile(
    val personId: PersonId,
    val subscriptions: List<CitizenPushSubscription>,
)

data class CitizenPushSubscription(
    val endpoint: String,
    val ecdhKey: String,
    val authSecret: String,
    val enabledCategories: Set<CitizenPushCategory>,
    val userAgent: String?,
    val createdAt: HelsinkiDateTime,
)
```

### `CitizenPushSubscriptionStore.kt`

Wraps the existing S3 client already injected elsewhere in evaka (the attachment/document code path — the exact bean is confirmed during writing-plans).

```kotlin
class CitizenPushSubscriptionStore(private val s3: S3Client, private val bucket: String) {
    fun load(personId: PersonId): CitizenPushStoreFile?            // null if file missing
    fun save(personId: PersonId, file: CitizenPushStoreFile): SaveResult
    fun delete(personId: PersonId)
    fun removeSubscription(personId: PersonId, endpoint: String)
}

data class SaveResult(val wasFirstWrite: Boolean)
```

No caching, no optimistic concurrency, no locks. Last-write-wins is fine for the POC.

### `CitizenWebPushController.kt`

WEAK-auth Spring MVC controller under `/citizen/push/`:

| Method | Path             | Body                                                                                  | Response                |
|--------|------------------|---------------------------------------------------------------------------------------|-------------------------|
| GET    | `/vapid-key`     | —                                                                                     | `{ publicKey }` or 503  |
| PUT    | `/subscription`  | `{ endpoint, ecdhKey, authSecret, enabledCategories, userAgent }`                     | `{ sentTest: bool }`    |
| DELETE | `/subscription`  | `{ endpoint }`                                                                        | 204                     |
| POST   | `/test`          | —                                                                                     | 204                     |

`PUT /subscription` upserts keyed on `endpoint`. On first-ever write for the citizen (`SaveResult.wasFirstWrite == true`), it immediately calls `CitizenPushSender.sendTest(personId)` and returns `sentTest: true`. On any subsequent update it returns `sentTest: false`.

`POST /test` is the "Send test notification" button — always sends, bypassing first-write detection and category filtering.

All endpoints use the same WEAK authentication as `/citizen/personal-data/notification-settings` and the same CSRF header contract.

### `CitizenPushSender.kt`

Core send logic, thin wrapper around existing `webpush/WebPush.kt`:

```kotlin
class CitizenPushSender(
    private val store: CitizenPushSubscriptionStore,
    private val webPush: WebPush,     // the existing class
    private val i18n: CitizenPushMessages, // localized titles/bodies
) {
    fun notifyMessage(
        personId: PersonId,
        threadId: MessageThreadId,
        category: CitizenPushCategory,
        senderName: String,
        language: OfficialLanguage,
    )

    fun sendTest(personId: PersonId, language: OfficialLanguage)
}
```

`notifyMessage` loads the citizen's store file, filters subscriptions whose `enabledCategories` contains `category`, builds a payload `{ title, body, url: "/messages/{threadId}", tag: "msg-{threadId}" }`, and delegates each send to `WebPush.kt`. A `tag` collapses duplicate notifications for the same thread so rapid replies don't pile up.

On HTTP 410/404 from the push service, the dead subscription is removed from the store. Any other error is logged and swallowed — message delivery must not depend on push.

### VAPID key reuse

`CitizenPushSender` and the `GET /vapid-key` endpoint both read from the same Spring bean or config property that `webpush/WebPush.kt` already consumes. No new keys, no new secrets, no infra work.

**Caveat:** if that configuration turns out not to be trivially reusable (for example, the private key is wired only into a specific bean scope), the writing-plans step will surface the issue. The minimal fix would be to extract the VAPID config to a shared source — still no new provisioning, just a local refactor. We do not introduce a second key pair.

### Hook into `MessageService`

`MessageService` (or its citizen-specific helper — exact function name pinned down during writing-plans) already contains the code path that evaluates `EmailMessageType.MESSAGE_NOTIFICATION` for each citizen recipient. We add a parallel call in the same place, reusing the recipient list the email path already built. For each citizen recipient:

```kotlin
val category = when {
    message.urgent           -> CitizenPushCategory.URGENT_MESSAGE
    message.type == BULLETIN -> CitizenPushCategory.BULLETIN
    else                     -> CitizenPushCategory.MESSAGE
}
citizenPushSender.notifyMessage(recipient.id, thread.id, category, sender.name, recipient.language)
```

Synchronous, best-effort. Exceptions are caught and logged but never fail the HTTP request that created the message. We do **not** roll our own recipient expansion — we reuse whatever the email path resolves.

## Frontend components

### Service worker — extend existing `citizen-frontend/service-worker.js`

Currently 27 lines (install/activate/passthrough-fetch). We add two event listeners without touching the existing handlers:

```js
self.addEventListener('push', (event) => {
  const data = event.data?.json() ?? {}
  event.waitUntil(self.registration.showNotification(data.title, {
    body: data.body,
    icon: '/citizen/evaka-192px.png',
    badge: '/citizen/evaka-180px.png',
    tag: data.tag,
    data: { url: data.url ?? '/messages' },
  }))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = event.notification.data?.url ?? '/messages'
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((list) => {
      for (const c of list) {
        if (c.url.includes(url) && 'focus' in c) return c.focus()
      }
      return clients.openWindow(url)
    })
  )
})
```

The click handler focuses an existing tab whose URL already contains the target path, otherwise opens a new window. No i18n in the service worker — all titles and bodies are pre-localized server-side and arrive in the push payload.

### New directory `citizen-frontend/webpush/`

- **`webpush-api.ts`** — thin fetch wrappers around `GET /citizen/push/vapid-key`, `PUT /citizen/push/subscription`, `DELETE /citizen/push/subscription`, `POST /citizen/push/test`, using the same axios `client` from `api-client.ts`.

- **`webpush-state.ts`** — a hook `useWebPushState()` that returns `{ status, permission, subscription, vapidKey, categories }` and exposes `subscribe(categories)`, `updateCategories(categories)`, `unsubscribe()`, `sendTest()`. `status` is one of:
  - `unsupported` — no `ServiceWorker` or `PushManager` in this browser, or server returns 503 for the VAPID key
  - `unregistered` — supported, not yet subscribed, permission is `default`
  - `denied` — permission is `denied`
  - `subscribed` — permission is `granted` and a live subscription exists on the server
  
  Registration, `getSubscription()`, and permission checks all live here so components stay dumb.

- **`WebPushSettingsSection.tsx`** — new section rendered **inside** `NotificationSettingsSection.tsx` (nested, not sibling — so email and push preferences sit together in the same card). Renders the master toggle, three category checkboxes, the test-send button, and `<PermissionGuide>` when `status ∈ { unsupported, denied }`.

- **`PermissionGuide.tsx`** — calls `useDetectPlatform()`, picks a localized guide variant, and renders an inline expandable `<details>`-style accordion. Variants:
  - Chrome/Edge on Android
  - Samsung Internet on Android
  - Firefox on Android
  - Safari on iOS (emphasizes the "must be installed as a PWA first" prerequisite; links to the existing `PwaInstallButton`)
  - Chrome/Edge on desktop
  - Firefox on desktop
  - Safari on macOS
  - Fallback: generic "check your browser and OS notification settings"

### Extend `pwa/detectPlatform.ts`

The file is currently ~30 lines and only distinguishes iOS/Android. We extend it — since both the PWA install flow and the notification guide now need browser-level detection, a single shared detector is the right home.

New fields: `isChrome`, `isSafari`, `isFirefox`, `isEdge`, `isSamsungInternet`, `isMacOS`, `isWindows`, `isLinux`, `isStandalone` (checkable via `matchMedia('(display-mode: standalone)')`). Existing `isIOS` and `isAndroid` remain unchanged. Export a memoized `useDetectPlatform()` hook.

### Localization

New namespace `t.personalDetails.webPushSection` added to `lib-customizations/defaults/citizen/i18n/{fi,sv,en}.tsx`:

- `title`, `info`
- `enable`, `enabling`, `enabled`
- `categoryUrgent`, `categoryMessage`, `categoryBulletin` (label + one-line description each)
- `sendTest`, `testSent`, `testFailed`
- `unsupported`, `denied`
- `guide.{chromeAndroid, samsungAndroid, firefoxAndroid, safariIOS, chromeDesktop, firefoxDesktop, safariMacos, fallback}` — each a short JSX block with a numbered list

Backend-side notification text uses existing evaka localization infrastructure. The backend resolves the citizen's preferred language (the same source the email notifications already read), picks `"Uusi viesti" / "Nytt meddelande" / "New message"`, and puts the pre-localized strings into the push payload. The service worker just renders what it receives.

## Error handling

| Failure                                      | Behaviour                                                                                           |
|----------------------------------------------|------------------------------------------------------------------------------------------------------|
| S3 GET misses (file not found)               | Treated as empty subscriptions, not an error. Used to detect first-enable.                           |
| S3 write failure                              | HTTP 500 to UI; UI shows toast with retry. No in-memory fallback.                                    |
| Push service returns 410/404                  | Dead subscription removed from the S3 file; best-effort, log on removal failure.                    |
| Push service other error                     | Logged and swallowed. Message delivery already succeeded via email/UI.                               |
| VAPID not configured                          | `GET /vapid-key` returns 503. Frontend shows "Push notifications unavailable on this server" in the guide area. |
| Permission denied during first-enable         | Master toggle snaps back to off. `PermissionGuide` appears inline.                                   |
| Hook errors in `MessageService`               | Caught and logged. Message-create request never fails because of push.                              |

**Deliberately omitted for POC:** rate limiting, retry/backoff, metrics beyond existing Sentry breadcrumbs. Real messages are not high-frequency per-citizen; we revisit if staging shows a problem.

## Testing strategy

### Backend (Kotlin, JUnit)

- `CitizenPushSubscriptionStoreTest` — load/save/delete roundtrip against a MockS3Client (evaka already has one for attachment tests).
- `CitizenWebPushControllerTest` — subscribe flow, first-write triggers test, update categories, delete, unauthenticated rejection.
- `CitizenPushSenderTest` — category filtering, 410/404 cleanup, payload shape.

### Frontend (vitest + React Testing Library)

- `detectPlatform.test.ts` — fixture user agents for Chrome/Android, Safari/iOS, Firefox/desktop, Samsung Internet, Edge, etc.
- `webpush-state.test.ts` — status transitions across `unsupported`/`default`/`granted`/`denied`.
- `WebPushSettingsSection.test.tsx` — renders correctly per permission state; toggling master calls subscribe; checkboxes disabled when master is off.
- `PermissionGuide.test.tsx` — picks the right variant per detected platform.

### Manual verification in staging

- Real Chrome on Android, real Safari on an installed iOS PWA, real Firefox/Chrome on desktop.
- First-enable test notification arrives.
- A real new message triggers a real push.
- Clicking the notification opens the right thread.
- Revoking OS-level permission surfaces the guide with the correct platform variant.
- Logging into a second browser and subscribing there — both devices receive the message.

## File-by-file summary

### New files

- `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushCategory.kt`
- `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSubscription.kt`
- `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSubscriptionStore.kt`
- `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenWebPushController.kt`
- `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSender.kt`
- `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushMessages.kt` — localized title/body bundles
- `service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/*Test.kt` (three test classes)
- `frontend/src/citizen-frontend/webpush/webpush-api.ts`
- `frontend/src/citizen-frontend/webpush/webpush-state.ts`
- `frontend/src/citizen-frontend/webpush/WebPushSettingsSection.tsx`
- `frontend/src/citizen-frontend/webpush/PermissionGuide.tsx`
- `frontend/src/citizen-frontend/webpush/*.test.{ts,tsx}` (four test files)

### Modified files

- `frontend/src/citizen-frontend/service-worker.js` — add push + notificationclick handlers
- `frontend/src/citizen-frontend/personal-details/NotificationSettingsSection.tsx` — nest `<WebPushSettingsSection>`
- `frontend/src/citizen-frontend/pwa/detectPlatform.ts` — add browser family + OS + standalone detection
- `frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx` — new `webPushSection` namespace
- `frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx` — new `webPushSection` namespace
- `frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx` — new `webPushSection` namespace
- `service/src/main/kotlin/fi/espoo/evaka/messaging/MessageService.kt` (or equivalent) — call `CitizenPushSender.notifyMessage` alongside the email-notification path
- Spring config wiring — register new beans

## Open questions for writing-plans

1. Exact name of the `MessageService` function where the email-notification path iterates citizen recipients — pinned down when the implementer reads that file.
2. Exact bean/config property name that `WebPush.kt` reads VAPID keys from — pinned down when the implementer reads `WebPush.kt` and its Spring config.
3. Exact S3 client bean that's already injected in evaka — likely `DocumentService` or `S3DocumentClient`; pinned down when the implementer reads the attachment code path.

None of these change the architecture; they're implementation-time lookups.
