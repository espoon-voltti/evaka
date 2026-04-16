<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Citizen Push Notification Auth-Failure Handling — Design

**Date:** 2026-04-15
**Status:** Draft
**Scope:** `frontend/src/citizen-frontend` only (service worker, RequireAuth, LoginPage, thread view, citizen i18n). No backend, apigw, or infra changes.

## Goal

When a citizen interacts with a push notification and the resulting navigation or inline-reply send fails because their session has expired, surface that reason on the login screen and — for the inline-reply case — skip the current "error notification the user must tap" step and open the PWA straight onto the login page. Additionally, all notification clicks should deep-link to the specific thread and scroll to the most recent message, regardless of auth state.

## Non-goals

- No changes to the apigw's auth/session behaviour. The signal we rely on is the existing `401` response from `apigw/src/shared/session.ts:130`.
- No change to draft persistence. `frontend/src/citizen-frontend/service-worker.js:153` already saves the draft to IndexedDB before the reply POST, and `frontend/src/citizen-frontend/webpush/useReplyDraftRestore.ts` already restores it when the user lands back on the thread. The auth-failure flow relies on this without modification.
- No auto-retry of the failed reply after re-authentication. The user re-clicks "Send" manually; draft restore handles the content.
- No changes for non-auth failures (network error, 5xx, 403). The existing "reply error" notification (`service-worker.js:194-202`) stays exactly as it is today for those cases.
- No new reason values beyond the two listed below. Unknown `reason` query values are ignored silently by the login page.
- No changes to the employee-mobile app. This design is citizen-only.

## User-facing behaviour

### Scenario 1 — plain notification click, session expired

1. Citizen receives a push notification for a new message and taps the notification body (not the inline reply action).
2. The service worker opens `/messages/${threadId}?fromNotification=1&scrollTo=latest`.
3. `RequireAuth` sees the user is not authenticated, strips only `fromNotification` from the target URL (keeping `scrollTo` so the post-login return still scrolls to the latest message), and redirects to
   `/login?next=${encodeURIComponent('/messages/${threadId}?scrollTo=latest')}&reason=session-expired-open-thread`.
4. `LoginPage` reads the `reason` param and renders an `AlertBox` above the existing system-notification block with the text **"Viestiketjun avaaminen vaatii kirjautumisen. Ole hyvä ja kirjaudu sisään."** (fi; sv/en placeholder, see i18n section).
5. After the citizen completes strong or weak login, they land on `/messages/${threadId}?scrollTo=latest`; the thread view scrolls to the most recent message.

### Scenario 2 — inline reply POST fails with 401

1. Citizen types a reply in the notification action's text input and submits.
2. Service worker saves the draft to IDB, then POSTs to `/api/citizen/messages/reply-to/${threadId}`.
3. apigw returns `401`.
4. Service worker detects `response.status === 401`, **skips the current "reply error" notification**, and calls
   `clients.openWindow('/login?next=${encodeURIComponent('/messages/${threadId}?focus=reply&scrollTo=latest')}&reason=session-expired-reply-failed')`.
5. `LoginPage` renders an `AlertBox` with the text **"Vastauksen lähetys vaatii kirjautumisen."**
6. After login, the citizen lands on `/messages/${threadId}?focus=reply&scrollTo=latest`; the thread view scrolls to the most recent message, the reply editor opens, and `useReplyDraftRestore` prefills it with the draft they had typed. They click "Send" manually to retry.

### Scenario 3 — inline reply POST fails with anything other than 401

Existing behaviour unchanged: service worker shows the "reply error" notification with `requireInteraction: true` and the existing `replyAction.errorTitle` / `errorBody` text. The user taps it and lands on `/messages/${threadId}?focus=reply` (the `scrollTo=latest` addition applies here too — see below).

### Scenario 4 — plain notification click, session still valid

Service worker opens `/messages/${threadId}?fromNotification=1&scrollTo=latest`. `RequireAuth` passes through (user is authenticated). `ThreadView` mounts, scrolls to the most recent message, and strips `fromNotification` from its URL on mount so refreshes don't re-trigger special handling. No login page, no reason banner.

### "Scroll to latest" as a universal notification behaviour

All URLs the service worker opens from a notification click — plain click, reply-action click, reply-success notification click, reply-error notification click — include `scrollTo=latest` in addition to any other params. `ThreadView` reads the param on mount and calls the existing `scrollRefIntoView(autoScrollRef, undefined, 'end')` pattern (`ThreadView.tsx:290`) once, then removes `scrollTo` from the URL via history replace so a subsequent user-initiated scroll or refresh does not keep re-anchoring to the bottom.

## Technical design

### Service worker changes (`frontend/src/citizen-frontend/service-worker.js`)

**Helper: URL builder for notification-origin links.** A small pure function `notificationClickUrl(targetUrl, extras)` that takes the SW-known target (e.g. `payload.url`, `/messages/${threadId}?focus=reply`) and appends `fromNotification=1&scrollTo=latest` plus any extras, preserving existing query params. Centralises the "mark as notification-origin" behaviour so every caller in the SW uses the same shape.

**`notificationclick` handler.** Replace the two current `openUrl` calls (generic click at line 146, inline-reply fall-through at line 141) with `openUrl(notificationClickUrl(url))` / `openUrl(notificationClickUrl('/messages/${threadId}?focus=reply'))`. No behaviour change for the auth-ok path beyond the new query params.

**`handleInlineReply`, 401 branch.** In the existing `try/catch` around the POST (line 161-203), split the failure path:

```js
} catch (err) {
  const status = err?.status ?? null
  if (status === 401) {
    const loginUrl = `/login?next=${encodeURIComponent(
      `/messages/${threadId}?focus=reply&scrollTo=latest`
    )}&reason=session-expired-reply-failed`
    try {
      const win = await serviceWorker.clients.openWindow(loginUrl)
      if (win) return
    } catch (openErr) {
      console.warn('openWindow failed for auth-expired reply', openErr)
    }
    // Fall through to the existing error-notification path so the user
    // is never left with a silent failure.
  }
  // existing "reply error" notification code, unchanged
}
```

The throw on non-OK response at line 174 is updated to include the status code:

```js
if (!response.ok) {
  const e = new Error(`reply POST failed: ${response.status}`)
  e.status = response.status
  throw e
}
```

**Rationale for the `openWindow` fallback.** `clients.openWindow` can fail when the browser decides the user activation window has lapsed (the notification click is the activation, and the POST roundtrip counts against it). On failure we want the user to see *something*, so we drop back to the current error-notification path. The reason banner will still be missed in that fallback, but the user gets an interactable surface.

**iOS caveat.** iOS Safari 16.4+ supports `clients.openWindow` from a service worker during `notificationclick`. On iOS, if the citizen has installed the PWA to the home screen, `openWindow` opens the PWA in standalone mode; if they have not, it opens in a Safari tab. Both land on the login page correctly. This needs to be verified on a real iOS device during testing — device mode in DevTools does not exercise this path.

### `RequireAuth.tsx` changes

`RequireAuth` today builds `returnUrl` from `useLocation()` + `useSearch()` and redirects to `/login?next=${encodeURIComponent(returnUrl)}`. Two surgical changes:

1. When building `returnUrl`, strip `fromNotification` from the preserved query string. `scrollTo`, `focus`, and any other params are kept as-is so the post-login landing still scrolls to the latest message and/or focuses the reply editor.
2. If the incoming path had `fromNotification=1`, append `&reason=session-expired-open-thread` to the login redirect URL.

The second change reuses the `reason` query-param mechanism added for the inline-reply case, so `LoginPage` needs exactly one code path.

The `fromNotification` strip also prevents a post-login bounce loop where the next target would re-mark itself as notification-origin on every authenticated entry.

### `LoginPage.tsx` changes

- Define a union type in a new small module or co-located in `LoginPage.tsx`:
  ```ts
  type LoginPageReason =
    | 'session-expired-open-thread'
    | 'session-expired-reply-failed'
  ```
- Read `searchParams.get('reason')`, narrow to `LoginPageReason`, and if it matches render an `AlertBox` above the existing `systemNotifications` block. Uses the same `AlertBox` component already imported at line 21.
- Unknown reason values are silently ignored — no render, no console log. Keeps the page forward-compatible with future reason codes added server-side or by other flows.

### `ThreadView.tsx` changes

Add a sibling effect to the existing `focus=reply` handler at line 282:

```tsx
const scrollTo = searchParams.get('scrollTo')
useEffect(() => {
  if (scrollTo !== 'latest') return
  // One-shot: scroll to bottom, then strip the param so a subsequent
  // refresh or navigation doesn't keep re-anchoring.
  scrollRefIntoView(autoScrollRef, undefined, 'end')
  const url = new URL(window.location.href)
  url.searchParams.delete('scrollTo')
  window.history.replaceState(null, '', url.toString())
}, [threadId, scrollTo])
```

Ordering concern: the existing `autoScrollRef` effect at line 289 scrolls when `replyEditorVisible` changes. If both effects run in the same render, the `scrollTo=latest` scroll and the reply-editor scroll target the same ref and should be idempotent. No coordination needed.

### i18n strings (`frontend/src/lib-customizations/defaults/citizen/i18n/{fi,sv,en}.tsx`)

Add under `loginPage`:

```ts
reasons: {
  sessionExpiredOpenThread: 'Viestiketjun avaaminen vaatii kirjautumisen. Ole hyvä ja kirjaudu sisään.',
  sessionExpiredReplyFailed: 'Vastauksen lähetys vaatii kirjautumisen.'
}
```

Finnish copy is final (from the user). Swedish and English ship as placeholders with a `// TODO: copy review` comment alongside each, per Q1-A from brainstorming. Initial placeholders:

- `sv`: `Öppnandet av meddelandetråden kräver inloggning. Var vänlig logga in.` / `Sändning av svaret kräver inloggning.`
- `en`: `Opening the message thread requires login. Please log in.` / `Sending the reply requires login.`

These are reasonable first-draft translations and will be replaced when copy review happens.

## Error handling and edge cases

- **`openWindow` returns null / throws.** Fallback to the existing error-notification path so the user always has a surface to act on. The reason banner is lost in this fallback; acceptable because the fallback is rare and the notification still points the user at a login-requiring page.
- **`openWindow` opens a new window while the citizen has an existing open client.** Matches current behaviour of `openUrl` at line 210 — we reuse its focus-existing-client logic for non-auth navigation. For the 401 path we intentionally call `clients.openWindow` directly (not `openUrl`) because the destination is `/login`, which an existing client probably isn't already showing, so focus-existing is less useful and we want a deterministic "arrive on login".
- **User clicks multiple inline replies rapidly across notifications.** Each click runs an independent `handleInlineReply`. If the session expires between them, each will take the 401 path and each will try to `openWindow`. The second and later calls will focus-or-open the same URL; browsers dedupe this reasonably. No explicit coordination.
- **Citizen was already authenticated but something other than auth caused the 401** (e.g. apigw bug). Current user perception: "session expired, I need to log in again". Acceptable false positive — the user re-authenticates and the problem self-clears or reveals itself on retry.
- **Notification click when the thread has been deleted or the citizen no longer has access.** Out of scope for this design. The SPA handles the 404 / 403 on its own today; this change doesn't regress it.
- **`scrollTo=latest` on a thread page that hasn't finished loading messages.** `scrollRefIntoView` targets a ref; if the ref isn't mounted when the effect runs, the call is a no-op. When the ref later mounts and messages render, the reply-editor effect or other existing logic handles the scroll. Not ideal but matches the pattern ThreadView already uses.

## Testing

- **Unit — `LoginPage.spec.tsx`:** add cases for `?reason=session-expired-open-thread` and `?reason=session-expired-reply-failed` rendering the correct banner, and for an unknown `reason` value rendering no banner.
- **Unit — new tiny test file for the `notificationClickUrl` helper** if the helper ends up non-trivial; otherwise inlined and skipped.
- **Manual — service worker flow:**
  1. Install the PWA on a real iOS device (tunnel URL).
  2. Log out to simulate expired session (or shorten session TTL in local apigw and wait).
  3. Send a test push notification from citizen settings.
  4. Tap the notification → expect to land on the login page with the "Viestiketjun avaaminen..." banner.
  5. With session still valid, reply inline → success notification, click → expect to land on the thread scrolled to the latest message.
  6. With session expired, reply inline → expect no error notification, PWA opens directly on the login page with the "Vastauksen lähetys..." banner; draft is restored after login.
- **Manual — Android Chrome**: repeat steps 4–6 on an Android device.
- **Regression — non-auth failure:** stop the apigw or return a 500 from the reply endpoint; verify the existing "reply error" notification still appears unchanged.
- **Type check + vitest citizen-frontend project** pass cleanly.

## Files touched

1. `frontend/src/citizen-frontend/service-worker.js` — split 401 vs. non-401 in `handleInlineReply`; add `notificationClickUrl` helper; include `scrollTo=latest` + `fromNotification=1` in all notification-origin URLs.
2. `frontend/src/citizen-frontend/RequireAuth.tsx` — strip `fromNotification` from the preserved return URL; append `reason=session-expired-open-thread` to the login redirect when `fromNotification=1`.
3. `frontend/src/citizen-frontend/login/LoginPage.tsx` — read `reason` param, render `AlertBox` with the corresponding translation.
4. `frontend/src/citizen-frontend/login/LoginPage.spec.tsx` — new test cases.
5. `frontend/src/citizen-frontend/messages/ThreadView.tsx` — read `scrollTo=latest` on mount, scroll to the end, strip the param from the URL via history replace.
6. `frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx` — add final Finnish reason strings.
7. `frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx` — add placeholder Swedish reason strings with `// TODO: copy review`.
8. `frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx` — add placeholder English reason strings with `// TODO: copy review`.

## Rollout

Ship behind no feature flag. The failure modes this design replaces are already worse than the replacement, and the non-auth-failure path is untouched, so there's no "gradually enable" axis that helps here.
