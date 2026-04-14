<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Citizen frontend PWA install button

## Background

The eVaka citizen frontend is currently a regular web app. The login page
shows a Finnish/Swedish/English message titled *"Haluatko löytää tälle sivulle
helpommin?"* with collapsible iOS/Android instructions for adding the page to
the home screen as a bookmark shortcut. This text lives in
`frontend/src/citizen-frontend/login/LoginPage.tsx` (component
`AddToHomeScreenInstructions`, lines 194–251) and is wrapped in `<MobileOnly>`.

That bookmark approach is the only "install" the citizen app supports today —
there is **no PWA infrastructure** in the citizen build:

- no `manifest.json` linked from `src/citizen-frontend/index.html`
- no service worker
- no PWA-related meta tags (`apple-mobile-web-app-capable`, `theme-color`, …)
- no `beforeinstallprompt` handling

The employee mobile build (`src/employee-mobile-frontend/`) does have a full
PWA setup — manifest at `public/employee/mobile/manifest.json`, service worker
at `src/employee-mobile-frontend/service-worker.js`, and a Vite plugin in
`vite.config.ts` (lines 112–151) that builds the worker into
`dist/bundle/employee/mobile/service-worker.js`. We can model the citizen-side
work on this existing setup.

## Goals

1. Make the eVaka citizen frontend installable as a real PWA on platforms
   that support it (Chrome/Edge on Android, Chrome/Edge on desktop, recent
   Safari on iOS via "Add to Home Screen").
2. Replace the existing `AddToHomeScreenInstructions` block on the login page
   with a single button that:
   - Triggers the **native install dialog** when `beforeinstallprompt` has
     fired (Chromium-based browsers).
   - Falls back to **user-agent-aware step-by-step instructions** when the
     native dialog is unavailable (e.g. iOS Safari).
   - Hides itself or shows an "already installed" state when the app is
     running standalone.
3. Reuse the existing translated step-by-step text (Finnish, Swedish,
   English) in the fallback so we don't redo translation work.

## Non-goals

- Push notifications. The greenhouse-solar-heater PWA we are loosely
  modelling on uses the same SW for push, but eVaka does not need that here.
  The service worker will be the minimum required for installability.
- Offline caching. The citizen app requires backend connectivity to be
  useful; we will not implement any offline strategies.
- Desktop install support. Per the user's decision, the install button stays
  inside the existing `<MobileOnly>` wrapper. Desktop users continue to use
  the app in a browser tab.
- Per-municipality icon/branding customization. Future work may wire the
  manifest into `lib-customizations`; for now we use the existing eVaka
  icons.

## Architecture overview

```
src/citizen-frontend/
├── index.html                 # add manifest link + PWA meta tags
├── service-worker.ts          # NEW — minimal install/activate/fetch SW
├── pwa/
│   ├── usePwaInstall.ts       # NEW — React hook owning install state
│   ├── PwaInstallButton.tsx   # NEW — button + fallback dialog
│   └── detectPlatform.ts      # NEW — UA detection helper
└── login/
    └── LoginPage.tsx          # MODIFY — replace AddToHomeScreenInstructions

public/citizen/
├── manifest.json              # NEW
├── evaka-192px.png            # NEW (copied from public/employee/mobile/)
├── evaka-512px.png            # NEW (copied)
└── evaka-180px.png            # NEW (copied, used as apple-touch-icon)

vite.config.ts                 # MODIFY — add a second SW build entry
```

## Components

### Manifest (`public/citizen/manifest.json`)

```json
{
  "name": "eVaka",
  "short_name": "eVaka",
  "start_url": "/",
  "scope": "/",
  "display": "standalone",
  "background_color": "#ffffff",
  "theme_color": "#3273c9",
  "icons": [
    { "src": "/citizen/evaka-192px.png", "sizes": "192x192", "type": "image/png", "purpose": "any" },
    { "src": "/citizen/evaka-512px.png", "sizes": "512x512", "type": "image/png", "purpose": "any" }
  ]
}
```

`theme_color` and `background_color` will be confirmed against the actual
eVaka brand values during implementation (the employee mobile manifest is the
reference). The icons are byte-for-byte copies of the existing employee
mobile icons placed at a citizen-scoped path so the asset ownership is clear.
A `.license` file accompanies each PNG to satisfy the existing REUSE
compliance checks visible in `public/employee/mobile/`.

### Service worker (`src/citizen-frontend/service-worker.ts`)

Minimal worker — just enough to make Chrome consider the app installable:

```ts
self.addEventListener('install', () => {
  ;(self as unknown as ServiceWorkerGlobalScope).skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    (self as unknown as ServiceWorkerGlobalScope).clients.claim()
  )
})

self.addEventListener('fetch', (event: FetchEvent) => {
  if (event.request.method !== 'GET') return
  event.respondWith(fetch(event.request))
})
```

Built via the same `vite.config.ts` lib-mode pattern used for
`employee-mobile-frontend/service-worker.js`. The plugin around lines
112–151 is duplicated (or parameterised) to also emit
`dist/bundle/citizen/service-worker.js`. The worker is registered from the
citizen entry point (`index.tsx` or equivalent) using
`navigator.serviceWorker.register('/service-worker.js', { scope: '/' })`,
guarded by `'serviceWorker' in navigator` and a `try/catch`.

### `index.html` changes (`src/citizen-frontend/index.html`)

Add inside `<head>`:

```html
<link rel="manifest" href="/citizen/manifest.json" />
<link rel="apple-touch-icon" href="/citizen/evaka-180px.png" />
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style" content="default" />
<meta name="apple-mobile-web-app-title" content="eVaka" />
<meta name="theme-color" content="#3273c9" />
```

Existing viewport meta is left untouched.

### `usePwaInstall` hook

A small React hook owning all install-related state. Returns a discriminated
union so the component renders one of three states:

```ts
type PwaInstallState =
  | { kind: 'standalone' }                    // already installed / running standalone
  | { kind: 'native'; promptInstall: () => Promise<void> } // beforeinstallprompt captured
  | { kind: 'fallback'; platform: Platform }  // no native prompt; show instructions
```

Behaviour:

- **Initial state**: `standalone` if `display-mode: standalone` matches at
  mount time; otherwise `fallback` with the detected platform. The hook
  starts pessimistic — we assume no native prompt and only upgrade to
  `native` when the browser tells us we can. On Chromium-based mobile
  browsers `beforeinstallprompt` typically fires within a few hundred
  milliseconds of page load, well before the user interacts with the login
  form, so the brief window where a Chrome-on-Android user might see the
  fallback variant is acceptable.
- On mount, attaches a `beforeinstallprompt` listener that calls
  `e.preventDefault()`, stores the event in a ref, and transitions the
  state to `native`. This **must** be attached as early as possible — the
  hook lives on the login page, which is the citizen app's entry route, so
  this is fine.
- On mount, attaches an `appinstalled` listener that transitions to
  `standalone`.
- On mount, checks `window.matchMedia('(display-mode: standalone)').matches`
  and `(navigator as any).standalone` to detect the already-installed case.
- `promptInstall()` calls `deferredEvent.prompt()`, awaits `userChoice`, and
  on `'accepted'` clears the stored event (the browser will not fire
  `beforeinstallprompt` again for the same session).
- The hook never crashes if `window` APIs are missing — all access is
  guarded.

### `detectPlatform`

Returns one of: `'ios'`, `'android'`, `'other'`. Plus a flag for whether the
browser is the OS's "blessed" browser for installation (Safari on iOS, Chrome
on Android — only those see the official install paths).

```ts
export type Platform =
  | { os: 'ios'; canInstallNatively: false; isSafari: boolean }
  | { os: 'android'; canInstallNatively: false; isChrome: boolean }
  | { os: 'other'; canInstallNatively: false }
```

The `canInstallNatively: false` is intentional — by the time we render the
`fallback` state, we already know the native prompt is unavailable, so this
type only describes the fallback world. Detection uses `navigator.userAgent`
and `navigator.userAgentData` where available, with the standard regex
guards (`/iPad|iPhone|iPod/`, `/Android/`, `/CriOS|FxiOS/`, etc.).

### `PwaInstallButton` component

Renders the button and the fallback dialog. Always visible (when not in
`standalone`) inside `<MobileOnly>` on the login page.

```tsx
<MobileOnly>
  <PwaInstallButton />
</MobileOnly>
```

Render rules based on `usePwaInstall()` state:

- **`standalone`**: render nothing. The user has already installed; we don't
  need to nag them on the login page.
- **`native`**: render a primary button labelled by the new translation key
  `loginPage.pwaInstall.button` (Finnish *"Lisää kotivalikkoon"*, Swedish
  *"Lägg till på hemskärmen"*, English *"Add to home screen"*). Clicking
  calls `promptInstall()`.
- **`fallback`**: same button label, but clicking opens a
  `CollapsibleContentArea` (the same container the current
  `AddToHomeScreenInstructions` already uses, so the visual idiom is
  preserved) showing **only** the steps for the detected platform — never
  both. The instructions are pulled from the existing
  `i18n.loginPage.addToHomeScreen.instructions.{ios,android}` translation
  keys.
  - On iOS (any browser): show the existing iOS Safari steps. If the user is
    in Chrome/Firefox iOS we additionally render a one-line note that "to
    install, please open this page in Safari" (new translation key
    `loginPage.pwaInstall.iosUseSafariNote`).
  - On Android (any non-Chromium browser, or Chromium where the prompt
    failed to fire for whatever reason): show the existing Android Chrome
    steps.
  - On `other` (rare on mobile — e.g. an embedded webview that has no PWA
    install path at all): show a generic message, new translation key
    `loginPage.pwaInstall.notSupported`, e.g. *"Tämä selain ei tue eVakan
    asentamista kotivalikkoon."*

The component never renders both iOS and Android instructions
simultaneously, per the user's requirement.

### Translation changes

In each of `src/lib-customizations/defaults/citizen/i18n/{fi,sv,en}.tsx`:

- **Add** new keys under `loginPage`:
  - `pwaInstall.button` — button label
  - `pwaInstall.iosUseSafariNote` — short note shown on iOS non-Safari
  - `pwaInstall.notSupported` — fallback for unknown platforms
- **Keep** the existing `loginPage.addToHomeScreen.instructions.{ios,android}`
  JSX trees — they are reused inside the fallback dialog.
- **Remove** the now-unused top-level keys
  `loginPage.addToHomeScreen.{title,subTitle,ios,android}` (they were the
  collapsible labels, no longer needed) — unless TypeScript or a sibling
  surface still references them, in which case keep and let the
  implementation plan flag it.

### `LoginPage.tsx` changes

- Delete the `AddToHomeScreenInstructions` component (lines 194–251) and its
  internal localStorage `'add-to-homescreen-instructions'` state — no longer
  needed.
- Replace its render site (around line 107–110) with `<PwaInstallButton />`
  inside the existing `<MobileOnly>` wrapper.
- Drop unused imports (`CollapsibleContentArea`, possibly `UnorderedList`,
  `LinkButton` if no other login-page code uses them).

## Build / Vite configuration

`vite.config.ts` currently has a single SW build block (lines 112–151) for
the employee mobile worker. The cleanest change is to extract that block
into a small helper that takes `{ entry, outDir, fileName }` and call it
twice — once for employee mobile, once for citizen. Output:

- `dist/bundle/employee/mobile/service-worker.js` (existing, unchanged)
- `dist/bundle/citizen/service-worker.js` (new)

The citizen entry already exists in the `build.rollupOptions.input` map
(lines 204–211); only the SW plugin needs duplication.

## Testing

- **Unit**: a small Vitest suite for `detectPlatform.ts` covering the UA
  strings for iOS Safari, iOS Chrome, Android Chrome, Android Firefox,
  desktop Chrome (mobile-only wrapper means desktop never reaches the
  component, but the helper is still pure), and an unknown UA.
- **Unit**: a Vitest test for `usePwaInstall` using `@testing-library/react`
  that fires a mock `beforeinstallprompt` event and asserts the state
  transitions to `native`, then calls `promptInstall()` and asserts the
  deferred event's `prompt()` is invoked.
- **Component**: a render test for `PwaInstallButton` in each of the three
  states (`standalone` → renders null; `native` → renders button; `fallback`
  → renders button + correct instruction set when expanded).
- **E2E**: no new Playwright spec. PWA install flows are notoriously hard to
  drive in headless browsers and the existing E2E suite does not cover the
  current home-screen instructions. The implementation plan should record
  this as an explicit non-test rather than a gap.
- **Manual**: smoke-test on (a) Android Chrome — native prompt fires, install
  succeeds, button hides on next visit; (b) iOS Safari — fallback shows iOS
  steps only; (c) iOS Chrome — fallback shows iOS steps + "use Safari" note;
  (d) desktop Chrome — button is not rendered (MobileOnly).

## Open questions for implementation

- Exact `theme_color` / `background_color` for the manifest. The employee
  mobile manifest holds the canonical values; verify and reuse.
- Where exactly in the citizen entry point to register the service worker.
  Likely `src/citizen-frontend/index.tsx` immediately after React mounts,
  inside a `'serviceWorker' in navigator` guard. The implementation plan
  should pin this down by reading the current entry file.
- Whether removing the old `addToHomeScreen.{title,subTitle,ios,android}`
  keys breaks any other surface. A grep pass during implementation will
  decide.
