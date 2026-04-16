<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Citizen Push Notification Auth-Failure Handling — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When a citizen interacts with a push notification and their session has expired, show a context-specific reason banner on the login page and — for the inline-reply case — open the PWA directly on the login screen instead of a second error notification. Also, make every notification click deep-link to the specific thread and scroll to the latest message.

**Architecture:** The service worker at `frontend/src/citizen-frontend/service-worker.js` splits the reply-POST failure path on HTTP `401` and calls `clients.openWindow('/login?next=…&reason=session-expired-reply-failed')`, falling back to the existing error-notification path if `openWindow` fails. Plain notification clicks get a `fromNotification=1&scrollTo=latest` suffix; `RequireAuth` reads `fromNotification=1` from the current location and routes the login redirect through a new pure helper that strips the flag from the preserved `next=` URL and appends `reason=session-expired-open-thread`. `LoginPage` reads the `reason` query param and renders an `AlertBox` with the matching translation. `ThreadView` reads `scrollTo=latest` on mount and scrolls `lastMessageRef` into view, then strips the param from the URL via `history.replaceState`. No backend, apigw, or infra changes.

**Tech Stack:** TypeScript, React 19, wouter (routing), vitest + @testing-library/react (citizen-frontend project), styled-components, `lib-components` (`AlertBox`, `scrollRefIntoView`).

**Spec:** `docs/superpowers/specs/2026-04-15-citizen-notification-auth-failure-design.md`

---

## Working directory

All commands in this plan run from `/Volumes/Evaka/evaka-pwa-test-fix` unless otherwise stated. Frontend commands run from `/Volumes/Evaka/evaka-pwa-test-fix/frontend`.

**Toolchain note (from `CLAUDE.md`):** Invoke bundled binaries directly — `node` and `yarn` are not on the default sandbox PATH.

```bash
# Type-check
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend && node_modules/.bin/tsc --build --force .

# Run citizen-frontend vitest project
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend && node_modules/.bin/vitest run --project citizen-frontend
```

## File map

- **Create:**
  - `frontend/src/citizen-frontend/auth/loginRedirect.ts` — pure helper `buildLoginRedirectPath` turning `(path, searchString)` into a login URL with the right `next=` and optional `reason=`.
  - `frontend/src/citizen-frontend/auth/loginRedirect.spec.ts` — unit tests for the helper.
- **Modify:**
  - `frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx` — add `loginPage.reasons` strings (final Finnish copy).
  - `frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx` — add `loginPage.reasons` strings (placeholder Swedish copy with `// TODO: copy review`).
  - `frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx` — add `loginPage.reasons` strings (placeholder English copy with `// TODO: copy review`).
  - `frontend/src/citizen-frontend/RequireAuth.tsx` — call `buildLoginRedirectPath` instead of string-concatenating the login URL.
  - `frontend/src/citizen-frontend/login/LoginPage.tsx` — read `reason` query param and render an `AlertBox` with the matching translation.
  - `frontend/src/citizen-frontend/login/LoginPage.spec.tsx` — add reason-banner test cases; make the `useSearchParams` mock driven by a mutable module variable; extend the inline i18n mock with the new `reasons` strings.
  - `frontend/src/citizen-frontend/messages/ThreadView.tsx` — add `scrollTo=latest` effect that scrolls `lastMessageRef` into view and strips the param via `history.replaceState`.
  - `frontend/src/citizen-frontend/service-worker.js` — add `notificationClickUrl` helper; use it for both the generic notification click and the inline-reply fall-through; split the `handleInlineReply` catch on `err.status === 401` and call `clients.openWindow` with a fallback; add `err.status` to the thrown error in the non-OK response path.

## Important pre-reads (for context, do not modify in this task)

These files contain patterns you'll be following; read them before starting a task that touches a related file.

- `frontend/src/citizen-frontend/login/LoginPage.tsx` — current `useSearchParams` + `AlertBox` usage (`searchParams.get('next')` at line 42; `<AlertBox title={...} message={...} wide noMargin data-qa="system-notification" />` at line 69-91).
- `frontend/src/citizen-frontend/RequireAuth.tsx` — current redirect shape (`<Redirect to={'/login?next=' + encodeURIComponent(returnUrl)} />` at line 40).
- `frontend/src/citizen-frontend/messages/ThreadView.tsx` — `useReplyDraftRestore` usage at line 273, `searchParams.get('focus')` effect at line 281-286, `lastMessageRef` definition at line 307, `scrollRefIntoView(autoScrollRef, undefined, 'end')` pattern at line 290.
- `frontend/src/citizen-frontend/service-worker.js` — current `handleInlineReply` implementation (lines 149-204), `openUrl` helper (lines 210-221), `notificationclick` handler (lines 125-147).
- `frontend/src/citizen-frontend/webpush/useReplyDraftRestore.ts` — existing draft restore, runs on mount; no changes required.
- `apigw/src/shared/session.ts:130` — the `res.sendStatus(401)` that the SW keys off (no modification).

---

## Task 1: Add i18n strings for the two reason banners

**Files:**
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx` (inside the `loginPage` object)
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx` (inside the `loginPage` object)
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx` (inside the `loginPage` object)

The i18n structure is inferred from `fi.tsx`; `sv.tsx` and `en.tsx` must match the same shape or TypeScript will flag them (see `CLAUDE.md` "i18n files are `.tsx`"). Add the same `reasons` sub-object to all three files.

- [ ] **Step 1: Find the `loginPage` object in `fi.tsx`**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
grep -n "loginPage:" src/lib-customizations/defaults/citizen/i18n/fi.tsx | head
```

Note the starting line of the `loginPage` object and scan to find the end of it (next top-level key at the same indentation).

- [ ] **Step 2: Add `reasons` to `fi.tsx`**

Inside the `loginPage` object, near the existing `systemNotification` key, add:

```tsx
reasons: {
  sessionExpiredOpenThread:
    'Viestiketjun avaaminen vaatii kirjautumisen. Ole hyvä ja kirjaudu sisään.',
  sessionExpiredReplyFailed: 'Vastauksen lähetys vaatii kirjautumisen.'
},
```

- [ ] **Step 3: Add `reasons` to `sv.tsx`**

Inside the `loginPage` object, near the existing `systemNotification` key, add:

```tsx
reasons: {
  // TODO: copy review — placeholder translation
  sessionExpiredOpenThread:
    'Öppnandet av meddelandetråden kräver inloggning. Var vänlig logga in.',
  // TODO: copy review — placeholder translation
  sessionExpiredReplyFailed: 'Sändning av svaret kräver inloggning.'
},
```

- [ ] **Step 4: Add `reasons` to `en.tsx`**

Inside the `loginPage` object, near the existing `systemNotification` key, add:

```tsx
reasons: {
  // TODO: copy review — placeholder translation
  sessionExpiredOpenThread:
    'Opening the message thread requires login. Please log in.',
  // TODO: copy review — placeholder translation
  sessionExpiredReplyFailed: 'Sending the reply requires login.'
},
```

- [ ] **Step 5: Type-check all three i18n files together**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/tsc --build --force .
```

Expected: PASS. Any "property 'reasons' is missing" error means a language file wasn't updated — go back and fix.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx \
        frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx \
        frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx
git commit -m "$(cat <<'EOF'
feat(citizen/i18n): add loginPage.reasons strings for notification-auth flow

Finnish copy is final; sv/en are first-draft placeholders marked with
TODO: copy review comments.
EOF
)"
```

---

## Task 2: Extract `buildLoginRedirectPath` helper (TDD)

This task introduces a pure function that turns the current path + search string into the correct `/login?next=…&reason=…` URL. Pulling the URL manipulation out of `RequireAuth.tsx` makes it easy to unit-test without setting up React context.

**Files:**
- Create: `frontend/src/citizen-frontend/auth/loginRedirect.ts`
- Create: `frontend/src/citizen-frontend/auth/loginRedirect.spec.ts`

- [ ] **Step 1: Write the failing test file**

Create `frontend/src/citizen-frontend/auth/loginRedirect.spec.ts`:

```ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import { buildLoginRedirectPath } from './loginRedirect'

describe('buildLoginRedirectPath', () => {
  it('builds a /login?next= URL for a plain path with no query', () => {
    expect(buildLoginRedirectPath('/messages/abc', '')).toBe(
      '/login?next=' + encodeURIComponent('/messages/abc')
    )
  })

  it('preserves non-fromNotification query params in next', () => {
    expect(
      buildLoginRedirectPath('/messages/abc', 'focus=reply&scrollTo=latest')
    ).toBe(
      '/login?next=' +
        encodeURIComponent('/messages/abc?focus=reply&scrollTo=latest')
    )
  })

  it('strips fromNotification=1 from the preserved next URL', () => {
    expect(
      buildLoginRedirectPath(
        '/messages/abc',
        'fromNotification=1&scrollTo=latest'
      )
    ).toBe(
      '/login?next=' +
        encodeURIComponent('/messages/abc?scrollTo=latest') +
        '&reason=session-expired-open-thread'
    )
  })

  it('appends reason=session-expired-open-thread when fromNotification=1 is present even with no other params', () => {
    expect(buildLoginRedirectPath('/messages/abc', 'fromNotification=1')).toBe(
      '/login?next=' +
        encodeURIComponent('/messages/abc') +
        '&reason=session-expired-open-thread'
    )
  })

  it('does not append reason when fromNotification is absent', () => {
    expect(buildLoginRedirectPath('/messages/abc', 'focus=reply')).toBe(
      '/login?next=' + encodeURIComponent('/messages/abc?focus=reply')
    )
  })

  it('ignores fromNotification values other than "1"', () => {
    expect(
      buildLoginRedirectPath('/messages/abc', 'fromNotification=0')
    ).toBe(
      '/login?next=' +
        encodeURIComponent('/messages/abc?fromNotification=0')
    )
  })

  it('handles an empty search string without producing a trailing ?', () => {
    expect(buildLoginRedirectPath('/', '')).toBe(
      '/login?next=' + encodeURIComponent('/')
    )
  })
})
```

- [ ] **Step 2: Run the test to verify it fails with the expected error**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/vitest run --project citizen-frontend \
  src/citizen-frontend/auth/loginRedirect.spec.ts
```

Expected: FAIL with `Cannot find module './loginRedirect'` (file doesn't exist yet).

- [ ] **Step 3: Implement the helper**

Create `frontend/src/citizen-frontend/auth/loginRedirect.ts`:

```ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// `fromNotification=1` is a transient marker added to every URL the citizen
// service worker opens from a push notification click. When the SPA routes
// through an auth wall, RequireAuth uses this helper to (a) strip the marker
// from the return URL so a successful post-login landing doesn't re-mark
// itself, and (b) signal the reason to the login page so it can show a
// context-specific banner.
export function buildLoginRedirectPath(
  returnPath: string,
  returnSearch: string
): string {
  const params = new URLSearchParams(returnSearch)
  const fromNotification = params.get('fromNotification') === '1'
  params.delete('fromNotification')
  const remainingSearch = params.toString()
  const returnUrl = remainingSearch
    ? `${returnPath}?${remainingSearch}`
    : returnPath
  const base = `/login?next=${encodeURIComponent(returnUrl)}`
  return fromNotification
    ? `${base}&reason=session-expired-open-thread`
    : base
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/vitest run --project citizen-frontend \
  src/citizen-frontend/auth/loginRedirect.spec.ts
```

Expected: PASS, 7 tests.

- [ ] **Step 5: Type-check**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/tsc --build --force .
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/citizen-frontend/auth/loginRedirect.ts \
        frontend/src/citizen-frontend/auth/loginRedirect.spec.ts
git commit -m "$(cat <<'EOF'
feat(citizen/auth): extract pure buildLoginRedirectPath helper

Centralises the fromNotification-marker → reason-param translation so
RequireAuth and any future callers build the same /login redirect URL.
EOF
)"
```

---

## Task 3: Wire `RequireAuth` to use the helper

**Files:**
- Modify: `frontend/src/citizen-frontend/RequireAuth.tsx`

The existing component builds the return URL and hand-rolls the `/login?next=…` string at line 40. Replace that with a call to `buildLoginRedirectPath`.

- [ ] **Step 1: Read the current file to confirm its shape**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix
cat frontend/src/citizen-frontend/RequireAuth.tsx
```

Confirm it matches: uses `useLocation` and `useSearch` from wouter, concatenates `/login?next=${encodeURIComponent(returnUrl)}` at line 40.

- [ ] **Step 2: Replace the redirect URL construction**

Open `frontend/src/citizen-frontend/RequireAuth.tsx` and make these changes:

1. Add an import for the new helper:

```tsx
import { buildLoginRedirectPath } from './auth/loginRedirect'
```

2. Remove `returnUrl` construction and the inline `/login?next=…` string. Replace the current `<Redirect to={/login?next=${encodeURIComponent(returnUrl)}} />` line with a call to the helper. The full `return` block of the component becomes:

```tsx
return isLoggedIn ? (
  strength === 'STRONG' && !isStrong ? (
    refreshRedirect(getStrongLoginUri(returnUrlPath + (params ? `?${params}` : '')))
  ) : (
    <>{children}</>
  )
) : (
  <Redirect to={buildLoginRedirectPath(returnUrlPath, params ?? '')} />
)
```

Note: `params` comes from `useSearch()` — in wouter v3 it is already a string (the search *without* the leading `?`), which is exactly what `URLSearchParams` and our helper expect. If your local `useSearch()` return type differs, normalise with `String(params ?? '')` before passing.

Remove the local `const returnUrl = …` line at line 31-32 of the old file — it's no longer needed.

- [ ] **Step 3: Type-check**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/tsc --build --force .
```

Expected: PASS.

- [ ] **Step 4: Run the helper tests to confirm nothing regressed**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/vitest run --project citizen-frontend \
  src/citizen-frontend/auth/loginRedirect.spec.ts
```

Expected: PASS, 7 tests.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/citizen-frontend/RequireAuth.tsx
git commit -m "$(cat <<'EOF'
refactor(citizen/auth): route RequireAuth through buildLoginRedirectPath

No behavioural change yet for pre-existing callers — the helper strips
fromNotification=1 and appends reason=session-expired-open-thread only
when the incoming path carries that marker, which nothing adds until the
service-worker change lands.
EOF
)"
```

---

## Task 4: LoginPage reason banner (TDD)

**Files:**
- Modify: `frontend/src/citizen-frontend/login/LoginPage.tsx`
- Modify: `frontend/src/citizen-frontend/login/LoginPage.spec.tsx`

- [ ] **Step 1: Make the `useSearchParams` mock configurable in the spec**

Open `frontend/src/citizen-frontend/login/LoginPage.spec.tsx`. Today the `wouter` mock at lines 114-121 returns a hard-coded empty `URLSearchParams`. Refactor it to read from a mutable module-level variable so tests can override the search string per case. Replace the current mock block with:

```tsx
let searchParamsForTest = new URLSearchParams()

vi.mock('wouter', () => ({
  useSearchParams: () => [searchParamsForTest, vi.fn()],
  useLocation: () => ['/', vi.fn()],
  Redirect: () => null,
  Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
    <a href={to}>{children}</a>
  )
}))
```

Add a `beforeEach` inside the existing `describe('LoginPage', …)` block to reset the variable:

```tsx
beforeEach(() => {
  searchParamsForTest = new URLSearchParams()
})
```

And add `beforeEach` to the imports from `vitest` at the top:

```tsx
import { beforeEach, describe, expect, it, vi } from 'vitest'
```

- [ ] **Step 2: Extend the inline i18n mock with the new reason strings**

In the same spec file, find the `loginPage: { ... }` object inside the `vi.mock('../localization', …)` factory (around line 37) and add a `reasons` sub-object matching the real shape:

```tsx
loginPage: {
  welcomeHeadline: 'Welcome',
  title: 'Login',
  systemNotification: 'Notice',
  reasons: {
    sessionExpiredOpenThread: 'Open-thread reason banner',
    sessionExpiredReplyFailed: 'Reply-failed reason banner'
  },
  // … rest unchanged
```

Use distinctive English placeholders (the ones above) so test assertions can match on them without worrying about i18n wiring.

- [ ] **Step 3: Add three failing test cases**

Inside the existing `describe('LoginPage', …)` block in the spec, add:

```tsx
it('renders the open-thread reason banner when reason=session-expired-open-thread', () => {
  searchParamsForTest = new URLSearchParams('reason=session-expired-open-thread')
  render(wrap(<LoginPage />))
  expect(
    screen.getByTestId('login-reason-banner').textContent
  ).toContain('Open-thread reason banner')
})

it('renders the reply-failed reason banner when reason=session-expired-reply-failed', () => {
  searchParamsForTest = new URLSearchParams('reason=session-expired-reply-failed')
  render(wrap(<LoginPage />))
  expect(
    screen.getByTestId('login-reason-banner').textContent
  ).toContain('Reply-failed reason banner')
})

it('renders no reason banner for unknown reason values', () => {
  searchParamsForTest = new URLSearchParams('reason=totally-made-up')
  render(wrap(<LoginPage />))
  expect(screen.queryByTestId('login-reason-banner')).toBeNull()
})
```

- [ ] **Step 4: Run the new tests and verify they fail**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/vitest run --project citizen-frontend \
  src/citizen-frontend/login/LoginPage.spec.tsx
```

Expected: the new three tests fail with `Unable to find an element by: [data-testid="login-reason-banner"]` (banner not implemented yet). The existing `'renders the passkey primary button'` test keeps passing.

- [ ] **Step 5: Implement the banner in `LoginPage.tsx`**

Open `frontend/src/citizen-frontend/login/LoginPage.tsx`. Add near the top of the component body, after the `searchParams` hook at line 41:

```tsx
const reasonParam = searchParams.get('reason')
const reasonMessage =
  reasonParam === 'session-expired-open-thread'
    ? i18n.loginPage.reasons.sessionExpiredOpenThread
    : reasonParam === 'session-expired-reply-failed'
      ? i18n.loginPage.reasons.sessionExpiredReplyFailed
      : null
```

Then inside the JSX, immediately above the existing `systemNotifications.isSuccess && …` `AlertBox` (line 67), add:

```tsx
{reasonMessage && (
  <AlertBox
    message={reasonMessage}
    wide
    noMargin
    data-qa="login-reason-banner"
  />
)}
```

`AlertBox` is already imported at line 21. `data-qa` doubles as the `data-testid` in the test because the spec uses `getByTestId` — verify by looking at how `AlertBox` props are forwarded in `lib-components/molecules/MessageBoxes`. If `AlertBox` forwards only `data-qa` and not `data-testid`, change the spec to use `screen.getByText` on the English placeholder instead.

**Verification substep (read-only):**

```bash
grep -n "data-qa\|data-testid" /Volumes/Evaka/evaka-pwa-test-fix/frontend/src/lib-components/molecules/MessageBoxes.tsx | head
```

If `data-qa` is forwarded as `data-qa` only (not auto-mapped to `data-testid`), update the spec's three new tests to use `screen.getByText('Open-thread reason banner')` / `'Reply-failed reason banner'` / `screen.queryByText('…')` and rely on the banner text instead of a testid. Keep the `data-qa="login-reason-banner"` on the component — it's how playwright e2e specs find elements in this codebase.

- [ ] **Step 6: Run the LoginPage tests and verify all four pass**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/vitest run --project citizen-frontend \
  src/citizen-frontend/login/LoginPage.spec.tsx
```

Expected: PASS, 4 tests.

- [ ] **Step 7: Type-check**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/tsc --build --force .
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/citizen-frontend/login/LoginPage.tsx \
        frontend/src/citizen-frontend/login/LoginPage.spec.tsx
git commit -m "$(cat <<'EOF'
feat(citizen/login): show reason banner for known ?reason values

Supports session-expired-open-thread (plain notification click with
expired session) and session-expired-reply-failed (inline-reply POST
returned 401). Unknown reason values render nothing.
EOF
)"
```

---

## Task 5: ThreadView `scrollTo=latest` handling

**Files:**
- Modify: `frontend/src/citizen-frontend/messages/ThreadView.tsx`

Add an effect that runs when the thread's `searchParams` contain `scrollTo=latest` and the messages have loaded. It scrolls `lastMessageRef` (already defined at line 307 of the current file and attached to the last message element at line 394) into view, then strips `scrollTo` from the URL via `history.replaceState` so a subsequent refresh or mid-thread scroll doesn't re-anchor.

No unit test is added — `ThreadView` has no existing spec and creating a test harness for its many dependencies is disproportionate to a three-line effect. Covered by manual verification in Task 7.

- [ ] **Step 1: Add the scroll effect**

Open `frontend/src/citizen-frontend/messages/ThreadView.tsx`. Find the existing `searchParams.get('focus') === 'reply'` effect at lines 281-286. Immediately after that effect (before the `autoScrollRef` declaration at line 288), add:

```tsx
useEffect(() => {
  if (searchParams.get('scrollTo') !== 'latest') return
  if (messages.length === 0) return
  scrollRefIntoView(lastMessageRef, undefined, 'end')
  const url = new URL(window.location.href)
  url.searchParams.delete('scrollTo')
  // Also strip the notification marker if it happens to still be here;
  // RequireAuth already strips it from the post-login return URL but
  // direct notification clicks when the session is valid land here with
  // the marker still present.
  url.searchParams.delete('fromNotification')
  window.history.replaceState(null, '', url.toString())
}, [searchParams, messages.length])
```

`scrollRefIntoView` is already imported at line 27 of the current file. `lastMessageRef` is already declared at line 307 but is referenced further down in the JSX — since `useEffect` runs after render, the ref will be populated by the time the effect fires (once messages are non-empty).

**Ordering note:** this effect runs before the existing `autoScrollRef` effect at line 289, which also scrolls when `replyEditorVisible` changes. Both scrolls happen in a single layout pass; the last one wins visually, and for our case (fresh mount, reply editor not yet open) only the `scrollTo=latest` effect fires on initial render, so there's no conflict. If `focus=reply` is also present, the reply editor opens → the `autoScrollRef` effect fires → it scrolls to the reply editor container which is just below the last message. Acceptable: the user sees the latest message and the reply editor both in view.

- [ ] **Step 2: Type-check**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/tsc --build --force .
```

Expected: PASS.

- [ ] **Step 3: Run the citizen-frontend vitest project to confirm nothing regressed**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/vitest run --project citizen-frontend
```

Expected: all tests pass, including the four in `LoginPage.spec.tsx` and the seven in `loginRedirect.spec.ts`.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/citizen-frontend/messages/ThreadView.tsx
git commit -m "$(cat <<'EOF'
feat(citizen/messages): honor ?scrollTo=latest on thread mount

Notification-origin thread URLs now carry scrollTo=latest (added by the
service worker in a follow-up commit). When present, the thread view
scrolls the last message into view and strips the marker via
history.replaceState so a manual scroll or refresh does not re-anchor.
EOF
)"
```

---

## Task 6: Service worker — 401 handling and notification-URL helper

**Files:**
- Modify: `frontend/src/citizen-frontend/service-worker.js`

This task has no unit test — `service-worker.js` is a classic script (not an ES module; see the comment at lines 12-15 explaining why the existing draft-store logic is duplicated inline). The SW flow is covered by the existing e2e spec `frontend/src/e2e-test/specs/7_messaging/service-worker-messaging.spec.ts` and the manual test plan in Task 7. Follow the existing inline-duplication pattern for the new `notificationClickUrl` helper.

- [ ] **Step 1: Add `notificationClickUrl` helper**

Open `frontend/src/citizen-frontend/service-worker.js`. Immediately above the `serviceWorker.addEventListener('install', …)` line (currently line 66), add:

```js
// Adds the notification-origin markers the SPA looks for on arrival:
//   - fromNotification=1: consumed by RequireAuth to append
//     reason=session-expired-open-thread when the user is not logged in.
//     Stripped from the post-login return URL.
//   - scrollTo=latest: consumed by ThreadView to scroll to the last
//     message on mount. Stripped from the URL via history.replaceState
//     after the first scroll.
// Preserves any existing query params on the input URL.
function notificationClickUrl(targetUrl) {
  try {
    const url = new URL(targetUrl, self.location.origin)
    url.searchParams.set('fromNotification', '1')
    url.searchParams.set('scrollTo', 'latest')
    return url.pathname + url.search + url.hash
  } catch {
    // Fallback: if targetUrl is unparseable (shouldn't happen for the
    // backend-supplied URLs we see today), return it unchanged so we
    // at least open something.
    return targetUrl
  }
}
```

- [ ] **Step 2: Use the helper in the `notificationclick` handler**

In the same file, update the `notificationclick` handler (currently lines 125-147) to route both navigation paths through the helper:

```js
serviceWorker.addEventListener('notificationclick', (event) => {
  const notification = event.notification
  const data = notification.data ?? {}
  const url = data.url ?? '/messages'
  const replyAction = data.replyAction

  if (event.action === 'reply' && replyAction) {
    const reply = event.reply
    if (typeof reply === 'string' && reply.trim().length > 0) {
      notification.close()
      event.waitUntil(handleInlineReply(replyAction, reply.trim()))
      return
    }
    // Platform rendered the action as a plain button (no inline text input).
    // Fall through to opening the thread with the reply textarea focused.
    notification.close()
    event.waitUntil(
      openUrl(
        notificationClickUrl(`/messages/${replyAction.threadId}?focus=reply`)
      )
    )
    return
  }

  notification.close()
  event.waitUntil(openUrl(notificationClickUrl(url)))
})
```

Remove the now-unused `openThreadForReply` function (currently lines 206-208) since it's no longer called from anywhere.

- [ ] **Step 3: Make the reply POST failure path status-aware**

In `handleInlineReply` (currently lines 149-204), change the throw on non-OK responses from:

```js
if (!response.ok) throw new Error(`reply POST failed: ${response.status}`)
```

to:

```js
if (!response.ok) {
  const err = new Error(`reply POST failed: ${response.status}`)
  err.status = response.status
  throw err
}
```

- [ ] **Step 4: Split the `handleInlineReply` catch on `err.status === 401`**

Replace the existing catch block (currently at lines 192-203) with:

```js
} catch (err) {
  console.warn('Reply POST failed', { threadId, err })

  if (err && err.status === 401) {
    // Session expired. Open the PWA straight on the login page with the
    // reason banner so the citizen can re-authenticate without a second
    // notification tap. The draft was already saved to IDB before the
    // POST, so useReplyDraftRestore repopulates the reply editor when
    // the citizen lands back on the thread.
    const returnUrl = `/messages/${threadId}?focus=reply&scrollTo=latest`
    const loginUrl =
      `/login?next=${encodeURIComponent(returnUrl)}` +
      `&reason=session-expired-reply-failed`
    try {
      const win = await serviceWorker.clients.openWindow(loginUrl)
      if (win) return
    } catch (openErr) {
      console.warn('openWindow failed for auth-expired reply', openErr)
    }
    // Fall through to the error-notification path so the user is not
    // left with a silent failure. They'll land on the same thread and
    // the draft will be restored, they just lose the reason banner.
  }

  await serviceWorker.registration.showNotification(replyAction.errorTitle, {
    body: replyAction.errorBody,
    icon: '/citizen/notifications/reply-error.png',
    badge: '/citizen/evaka-badge-72.png',
    tag: `msg-reply-error-${threadId}`,
    requireInteraction: true,
    data: {
      url: notificationClickUrl(`/messages/${threadId}?focus=reply`)
    },
    actions: []
  })
}
```

Note the last change: the error-notification's `data.url` is now wrapped in `notificationClickUrl(...)` so clicking it also benefits from the scroll-to-latest behaviour. `notificationclick` then passes this URL through `notificationClickUrl` again — which is idempotent because setting `fromNotification=1` and `scrollTo=latest` on an URL that already has them is a no-op.

- [ ] **Step 5: Update the success-notification `data.url` for consistency**

In the success branch of `handleInlineReply` (currently near line 188), the notification's `data.url` is `/messages/${threadId}`. Wrap it too:

```js
data: { url: notificationClickUrl(`/messages/${threadId}`) },
```

This keeps all SW-originated notification click URLs behaviourally consistent: clicking any of them opens the thread and scrolls to the latest message.

- [ ] **Step 6: Lint and type-check the service worker file**

The service worker is plain JS with a `@type` JSDoc reference at line 7. It doesn't go through `tsc`, but ESLint will catch obvious issues.

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/tsc --build --force .
node_modules/.bin/eslint --max-warnings 0 src/citizen-frontend/service-worker.js
```

Expected: PASS. If ESLint complains about `err.status` on a generic `Error`, either add a JSDoc type annotation or cast via `/** @type {any} */`:

```js
/** @type {any} */
const err = new Error(`reply POST failed: ${response.status}`)
err.status = response.status
throw err
```

- [ ] **Step 7: Commit**

```bash
git add frontend/src/citizen-frontend/service-worker.js
git commit -m "$(cat <<'EOF'
feat(citizen/service-worker): open login page directly on reply 401

When the inline-reply POST fails with HTTP 401, skip the "reply error"
notification and call clients.openWindow on the login page with a
reason=session-expired-reply-failed query param. Fall back to the
existing error notification if openWindow fails (e.g. user activation
window lapsed). Also make all notification-click URLs carry
fromNotification=1 and scrollTo=latest via the new notificationClickUrl
helper, so RequireAuth can surface a reason banner on the login page
and ThreadView can scroll to the latest message on arrival.
EOF
)"
```

---

## Task 7: End-to-end verification and manual test plan

This task runs the full verification suite and documents the manual tests needed on real mobile devices.

- [ ] **Step 1: Full type-check**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/tsc --build --force .
```

Expected: PASS, no errors.

- [ ] **Step 2: Full citizen-frontend vitest run**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/vitest run --project citizen-frontend
```

Expected: all tests pass. Confirm the four new tests in `LoginPage.spec.tsx` and the seven in `loginRedirect.spec.ts` are included and green.

- [ ] **Step 3: Lint the touched files**

```bash
cd /Volumes/Evaka/evaka-pwa-test-fix/frontend
node_modules/.bin/eslint --max-warnings 0 \
  src/citizen-frontend/auth/loginRedirect.ts \
  src/citizen-frontend/auth/loginRedirect.spec.ts \
  src/citizen-frontend/RequireAuth.tsx \
  src/citizen-frontend/login/LoginPage.tsx \
  src/citizen-frontend/login/LoginPage.spec.tsx \
  src/citizen-frontend/messages/ThreadView.tsx \
  src/citizen-frontend/service-worker.js \
  src/lib-customizations/defaults/citizen/i18n/fi.tsx \
  src/lib-customizations/defaults/citizen/i18n/sv.tsx \
  src/lib-customizations/defaults/citizen/i18n/en.tsx
```

Expected: PASS. If any file has pre-existing warnings not caused by this task, commit the `--fix` output separately and note it.

- [ ] **Step 4: Desktop smoke test**

```bash
mise start
```

Wait for ports 3000/8888/9099. Open http://localhost:9099/ in Chrome.

1. Navigate to `/login?reason=session-expired-open-thread` — verify the open-thread reason banner appears above the system-notification area.
2. Navigate to `/login?reason=session-expired-reply-failed` — verify the reply-failed reason banner appears.
3. Navigate to `/login?reason=totally-made-up` — verify no banner appears.
4. Log in as a citizen. Navigate to any message thread URL with `?scrollTo=latest` — verify the thread view scrolls to the last message on arrival and the URL loses the `scrollTo` param shortly after (check the address bar).

Leave `mise start` running for the next steps.

- [ ] **Step 5: Mobile tunnel setup**

From `CLAUDE.md`, set up a cloudflared quick tunnel so a real phone can reach the dev server:

```bash
/tmp/cloudflared tunnel --url http://localhost:9099
```

Note the `https://<random>.trycloudflare.com` URL. `vite.config.ts` already allows `.trycloudflare.com`, so no extra config.

- [ ] **Step 6: Manual test — iOS Safari (real device required)**

DevTools device mode does not fire `beforeinstallprompt` or deliver push notifications reliably. Use a real iOS device (iOS 16.4+ required for SW push + `clients.openWindow` from notification).

1. On the iOS device, open the tunnel URL in Safari and "Add to Home Screen" to install the PWA.
2. Log in as a citizen. Enable push notifications from the settings section.
3. Send a test push notification via the citizen settings page's test-notification button.
4. **Plain click test (session valid):** tap the notification body. Expected: PWA opens on the thread, scrolled to the latest message.
5. **Plain click test (session expired):** from the employee-frontend or apigw debug endpoint, clear the citizen's session (or wait for it to expire). Send another test push. Tap the notification body. Expected: PWA opens on the login page with the "Viestiketjun avaaminen…" banner.
6. **Inline reply, session valid:** re-authenticate, send another test push. Type a reply in the notification's inline text input and submit. Expected: success notification appears; tapping it opens the thread scrolled to latest.
7. **Inline reply, session expired:** clear the session again, send a test push, type a reply, submit. Expected: no error notification; PWA opens directly on the login page with the "Vastauksen lähetys…" banner; after logging in, the citizen lands on the thread with the reply editor open and the typed draft restored.

- [ ] **Step 7: Manual test — Android Chrome (real device required)**

Repeat Step 6 on an Android device with the PWA installed. Android Chrome's notification handling is more permissive than iOS, so this should reproduce cleanly if iOS worked.

- [ ] **Step 8: Manual test — non-auth failure path regression**

Stop the apigw (`pm2 stop apigw`) or point the reply endpoint at a URL that returns a 500. Send a test push, type a reply, submit. Expected: the existing "reply error" notification appears with `requireInteraction: true` — no login page redirect, no silent failure. This confirms Task 6 only affected the 401 branch.

Restart the apigw afterwards (`pm2 start apigw`).

- [ ] **Step 9: No commit needed for this task**

This task is verification only — no code changes. If any step surfaces a bug, return to the relevant earlier task, fix it, and create a new commit there.

---

## Self-review checklist

Before handing the plan off to an executor, confirm these:

1. **Spec coverage**
   - Scenario 1 (plain click, session expired, banner): Tasks 2 + 3 (helper + RequireAuth) + Task 4 (LoginPage banner) + Task 6 (SW adds `fromNotification=1`). Covered.
   - Scenario 2 (inline reply 401, direct PWA open): Task 6 steps 3-4. Covered.
   - Scenario 3 (non-401 reply failure unchanged): Task 6 step 4 preserves the existing path; Task 7 step 8 verifies. Covered.
   - Scenario 4 (plain click, session valid, scrollTo=latest): Task 5 (ThreadView effect) + Task 6 (SW adds `scrollTo=latest`). Covered.
   - Universal scroll-to-latest: Task 6 uses `notificationClickUrl` for all three SW navigation call sites (plain click, inline-reply fall-through, reply-error notification, success notification). Covered.
   - i18n placeholder sv/en with TODOs: Task 1. Covered.
   - `openWindow` fallback to error notification: Task 6 step 4. Covered.
2. **Placeholder scan:** no "TBD", "TODO implement later", or bare "add tests" instructions — every step either shows the exact code or runs a specific command. Placeholder i18n strings are intentional and documented.
3. **Type consistency:** `buildLoginRedirectPath(path, search)` signature used identically in Task 2 (definition), Task 2 spec (seven test cases), and Task 3 (`RequireAuth` call site). Error `.status` field typed via `/** @type {any} */` in Task 6 to match how the SW file already uses loose JSDoc types.
