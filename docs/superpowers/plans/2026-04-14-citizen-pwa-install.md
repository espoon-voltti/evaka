# Citizen frontend PWA install Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the eVaka citizen frontend installable as a PWA, and replace the existing iOS/Android home-screen instruction message on the login page with a single "Add to home screen" button that triggers the native install dialog when supported and falls back to UA-aware instructions when not.

**Architecture:** Add the missing PWA infrastructure (manifest + minimal service worker + meta tags) to the citizen build, then introduce a small React module under `src/citizen-frontend/pwa/` consisting of (1) a pure platform-detection helper, (2) a `usePwaInstall` hook that owns `beforeinstallprompt` capture and standalone detection, and (3) a `PwaInstallButton` component that renders a button + a single platform-specific fallback panel. Wire it into `LoginPage.tsx`, replacing the current `AddToHomeScreenInstructions`.

**Tech Stack:** React 19, TypeScript, styled-components, Vite, Vitest (jsdom), `@testing-library/react`, existing `lib-components` UI atoms, existing `lib-customizations` i18n.

**Spec:** `docs/superpowers/specs/2026-04-14-citizen-pwa-install-design.md`

---

## File map

**Created:**
- `frontend/public/citizen/manifest.json` — Web app manifest (name, icons, theme).
- `frontend/public/citizen/manifest.json.license` — REUSE compliance sidecar.
- `frontend/public/citizen/evaka-180px.png` — Apple touch icon (copy of employee mobile).
- `frontend/public/citizen/evaka-180px.png.license`
- `frontend/public/citizen/evaka-192px.png` — Manifest 192px icon.
- `frontend/public/citizen/evaka-192px.png.license`
- `frontend/public/citizen/evaka-512px.png` — Manifest 512px icon.
- `frontend/public/citizen/evaka-512px.png.license`
- `frontend/src/citizen-frontend/service-worker.js` — Minimal SW (install/activate/no-op fetch) required for installability.
- `frontend/src/citizen-frontend/pwa/detectPlatform.ts` — Pure UA → platform helper.
- `frontend/src/citizen-frontend/pwa/detectPlatform.spec.ts` — Unit tests.
- `frontend/src/citizen-frontend/pwa/usePwaInstall.ts` — React hook owning install state.
- `frontend/src/citizen-frontend/pwa/usePwaInstall.spec.tsx` — Hook tests.
- `frontend/src/citizen-frontend/pwa/PwaInstallButton.tsx` — Button + fallback panel component.
- `frontend/src/citizen-frontend/pwa/PwaInstallButton.spec.tsx` — Component tests.

**Modified:**
- `frontend/vite.config.ts` — Parameterise the existing `serviceWorker()` plugin so it builds both the employee mobile worker (existing) and the new citizen worker.
- `frontend/src/citizen-frontend/index.html` — Add manifest link, apple-touch-icon, theme-color, apple-mobile-web-app-* meta tags.
- `frontend/src/citizen-frontend/index.tsx` — Register the service worker after React mounts.
- `frontend/src/citizen-frontend/login/LoginPage.tsx` — Delete `AddToHomeScreenInstructions` (lines 194–251), drop now-unused imports (`useCallback`, `useLocalStorage`, `CollapsibleContentArea`), render `<PwaInstallButton />` in the existing `<MobileOnly>` slot at lines 107–110.
- `frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx` — Replace `loginPage.addToHomeScreen` block with a `loginPage.pwaInstall` block (button label, iOS-use-Safari note, not-supported message, and the existing instructions JSX moved over).
- `frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx` — Same restructure in Swedish.
- `frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx` — Same restructure in English.

---

## Task 1: Add manifest and icons to `public/citizen/`

**Files:**
- Create: `frontend/public/citizen/manifest.json`
- Create: `frontend/public/citizen/manifest.json.license`
- Create: `frontend/public/citizen/evaka-180px.png` (binary copy)
- Create: `frontend/public/citizen/evaka-180px.png.license`
- Create: `frontend/public/citizen/evaka-192px.png` (binary copy)
- Create: `frontend/public/citizen/evaka-192px.png.license`
- Create: `frontend/public/citizen/evaka-512px.png` (binary copy)
- Create: `frontend/public/citizen/evaka-512px.png.license`

- [ ] **Step 1: Create the directory and copy the icons from employee mobile**

```bash
mkdir -p frontend/public/citizen
cp frontend/public/employee/mobile/evaka-180px.png       frontend/public/citizen/evaka-180px.png
cp frontend/public/employee/mobile/evaka-180px.png.license frontend/public/citizen/evaka-180px.png.license
cp frontend/public/employee/mobile/evaka-192px.png       frontend/public/citizen/evaka-192px.png
cp frontend/public/employee/mobile/evaka-192px.png.license frontend/public/citizen/evaka-192px.png.license
cp frontend/public/employee/mobile/evaka-512px.png       frontend/public/citizen/evaka-512px.png
cp frontend/public/employee/mobile/evaka-512px.png.license frontend/public/citizen/evaka-512px.png.license
```

- [ ] **Step 2: Write the citizen manifest**

Create `frontend/public/citizen/manifest.json`:

```json
{
  "icons": [
    {
      "src": "/citizen/evaka-512px.png",
      "sizes": "512x512",
      "type": "image/png",
      "purpose": "maskable any"
    },
    {
      "src": "/citizen/evaka-192px.png",
      "sizes": "192x192",
      "type": "image/png",
      "purpose": "maskable any"
    },
    {
      "src": "/citizen/evaka-180px.png",
      "sizes": "180x180",
      "type": "image/png",
      "purpose": "maskable any"
    }
  ],
  "name": "eVaka",
  "short_name": "eVaka",
  "orientation": "portrait",
  "display": "standalone",
  "start_url": "/",
  "scope": "/",
  "background_color": "#ffffff",
  "theme_color": "#3273c9"
}
```

(Values mirror the existing `frontend/public/employee/mobile/manifest.json`. The only differences are `start_url` / `scope` which point at `/` for the citizen app, and the icon paths.)

- [ ] **Step 3: Add the manifest license sidecar**

Create `frontend/public/citizen/manifest.json.license` with the exact contents (matching `manifest.json.license` in the employee mobile folder):

```
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
```

- [ ] **Step 4: Verify everything is in place**

Run: `ls frontend/public/citizen/`
Expected output (8 files):
```
evaka-180px.png
evaka-180px.png.license
evaka-192px.png
evaka-192px.png.license
evaka-512px.png
evaka-512px.png.license
manifest.json
manifest.json.license
```

- [ ] **Step 5: Commit**

```bash
git add frontend/public/citizen/
git commit -m "Add citizen PWA manifest and icons"
```

---

## Task 2: Add PWA meta tags to citizen `index.html`

**Files:**
- Modify: `frontend/src/citizen-frontend/index.html`

- [ ] **Step 1: Add the manifest link, apple-touch-icon, and PWA meta tags inside `<head>`**

In `frontend/src/citizen-frontend/index.html`, find the existing `<head>` block (lines 9–17). After the existing `<link rel="icon" href="/favicon.ico" />` line, insert:

```html
  <link rel="manifest" href="/citizen/manifest.json" />
  <link rel="apple-touch-icon" href="/citizen/evaka-180px.png" />
  <meta name="apple-mobile-web-app-capable" content="yes" />
  <meta name="apple-mobile-web-app-status-bar-style" content="default" />
  <meta name="apple-mobile-web-app-title" content="eVaka" />
  <meta name="theme-color" content="#3273c9" />
```

The full updated `<head>` should look like:

```html
<head>
  <meta charset="utf-8" />
  <meta
    name="viewport"
    content="width=device-width, initial-scale=1, minimum-scale=1"
  />
  <title>Varhaiskasvatus</title>
  <link rel="icon" href="/favicon.ico" />
  <link rel="manifest" href="/citizen/manifest.json" />
  <link rel="apple-touch-icon" href="/citizen/evaka-180px.png" />
  <meta name="apple-mobile-web-app-capable" content="yes" />
  <meta name="apple-mobile-web-app-status-bar-style" content="default" />
  <meta name="apple-mobile-web-app-title" content="eVaka" />
  <meta name="theme-color" content="#3273c9" />
</head>
```

- [ ] **Step 2: Type-check still passes (no TS impact, but sanity check the build pipeline)**

Run: `cd frontend && yarn type-check`
Expected: completes successfully with no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/citizen-frontend/index.html
git commit -m "Wire citizen manifest and PWA meta tags into index.html"
```

---

## Task 3: Create the citizen service worker file

**Files:**
- Create: `frontend/src/citizen-frontend/service-worker.js`

The citizen worker is intentionally minimal — install/activate/no-op fetch — just enough for Chrome's installability heuristics. No push, no caching.

- [ ] **Step 1: Write the worker**

Create `frontend/src/citizen-frontend/service-worker.js`:

```js
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* global self */

/// <reference lib="WebWorker" />
/** @type {ServiceWorkerGlobalScope} */
const serviceWorker = self

serviceWorker.addEventListener('install', (event) => {
  event.waitUntil(serviceWorker.skipWaiting())
})

serviceWorker.addEventListener('activate', (event) => {
  event.waitUntil(serviceWorker.clients.claim())
})

// Pass-through fetch handler. Chrome's PWA installability check requires the
// service worker to have a `fetch` listener — we don't actually want to cache
// anything because the citizen app needs live backend connectivity to be
// useful.
serviceWorker.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return
  event.respondWith(fetch(event.request))
})
```

(Style mirrors `frontend/src/employee-mobile-frontend/service-worker.js` — same JSDoc reference, same `const serviceWorker = self` pattern, plain JS not TS.)

- [ ] **Step 2: Commit**

```bash
git add frontend/src/citizen-frontend/service-worker.js
git commit -m "Add citizen service worker"
```

---

## Task 4: Build the citizen service worker via vite.config.ts

**Files:**
- Modify: `frontend/vite.config.ts`

The current `serviceWorker()` plugin (lines 112–151) hard-codes the employee mobile paths. Refactor it to take a config object and call it twice from the plugin list.

- [ ] **Step 1: Replace the `serviceWorker()` plugin definition with a parameterised version**

In `frontend/vite.config.ts`, replace the existing function (lines 112–151):

```ts
function serviceWorker(): Plugin {
  const urlPath = '/employee/mobile/service-worker.js'
  const sourcePath = 'src/employee-mobile-frontend/service-worker.js'
  return {
    name: 'build-service-worker-prod',
    configureServer(server) {
      server.middlewares.use(urlPath, async (_req, res, next) => {
        try {
          const code = await server.transformRequest(sourcePath)
          res.setHeader('Content-Type', 'text/javascript')
          res.end(code?.code ?? '')
        } catch (err) {
          next(err)
        }
      })
    },
    async generateBundle() {
      const dirName = path.dirname(urlPath)
      const fileName = path.basename(urlPath)
      await build({
        configFile: false,
        build: {
          outDir: `${outDir}/${dirName}`,
          emptyOutDir: false,
          lib: {
            entry: path.resolve(__dirname, sourcePath),
            formats: ['es'],
            fileName: () => fileName
          },
          rolldownOptions: {
            output: {
              entryFileNames: fileName
            }
          },
          minify: true
        }
      })
    }
  }
}
```

…with:

```ts
interface ServiceWorkerConfig {
  name: string
  urlPath: string
  sourcePath: string
}

function serviceWorker(config: ServiceWorkerConfig): Plugin {
  const { name, urlPath, sourcePath } = config
  return {
    name: `build-service-worker-prod-${name}`,
    configureServer(server) {
      server.middlewares.use(urlPath, async (_req, res, next) => {
        try {
          const code = await server.transformRequest(sourcePath)
          res.setHeader('Content-Type', 'text/javascript')
          res.end(code?.code ?? '')
        } catch (err) {
          next(err)
        }
      })
    },
    async generateBundle() {
      const dirName = path.dirname(urlPath)
      const fileName = path.basename(urlPath)
      await build({
        configFile: false,
        build: {
          outDir: `${outDir}/${dirName}`,
          emptyOutDir: false,
          lib: {
            entry: path.resolve(__dirname, sourcePath),
            formats: ['es'],
            fileName: () => fileName
          },
          rolldownOptions: {
            output: {
              entryFileNames: fileName
            }
          },
          minify: true
        }
      })
    }
  }
}
```

- [ ] **Step 2: Update the `plugins` array to call `serviceWorker()` twice**

In the same file, find the existing `serviceWorker(),` entry inside `plugins:` (currently around line 176) and replace it with both calls:

```ts
      serviceWorker({
        name: 'employee-mobile',
        urlPath: '/employee/mobile/service-worker.js',
        sourcePath: 'src/employee-mobile-frontend/service-worker.js'
      }),
      serviceWorker({
        name: 'citizen',
        urlPath: '/service-worker.js',
        sourcePath: 'src/citizen-frontend/service-worker.js'
      }),
```

The citizen worker is served from `/service-worker.js` (root) so its scope covers the entire citizen app at `/`. Output goes to `dist/bundle/service-worker.js`.

- [ ] **Step 3: Verify the build still works**

Run: `cd frontend && yarn type-check`
Expected: completes with no errors.

Run: `cd frontend && yarn build 2>&1 | tail -30`
Expected: build succeeds. Verify the citizen worker was emitted:
```bash
ls frontend/dist/bundle/service-worker.js
ls frontend/dist/bundle/employee/mobile/service-worker.js
```
Both files should exist.

- [ ] **Step 4: Commit**

```bash
git add frontend/vite.config.ts
git commit -m "Parameterise service worker vite plugin and build citizen SW"
```

---

## Task 5: Register the service worker from the citizen entry point

**Files:**
- Modify: `frontend/src/citizen-frontend/index.tsx`

- [ ] **Step 1: Add a registration block after `root.render(<Root />)`**

In `frontend/src/citizen-frontend/index.tsx`, after the existing `root.render(<Root />)` line (around line 34) and before the `if (!window.evaka)` block, add:

```ts
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register('/service-worker.js', { scope: '/' })
      .catch((err) => {
        Sentry.captureException(err)
      })
  })
}
```

(Sentry is already imported at the top of the file as `import * as Sentry from '@sentry/browser'`, so no new imports are needed. Registering on the `load` event keeps SW registration off the critical path. We swallow registration errors via Sentry rather than letting them surface — a failed SW registration must not break the app.)

- [ ] **Step 2: Type-check**

Run: `cd frontend && yarn type-check`
Expected: completes with no errors.

- [ ] **Step 3: Manual smoke check (dev server)**

Run: `cd frontend && yarn dev`
In a browser, open the citizen app and check DevTools → Application → Service Workers. A service worker for `/` should be registered and activated.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/citizen-frontend/index.tsx
git commit -m "Register citizen service worker on load"
```

---

## Task 6: TDD `detectPlatform` helper

**Files:**
- Create: `frontend/src/citizen-frontend/pwa/detectPlatform.ts`
- Create: `frontend/src/citizen-frontend/pwa/detectPlatform.spec.ts`

This is a pure function from a UA string to one of three platform tags. Used by the fallback panel to pick which instruction set to render.

- [ ] **Step 1: Write the failing test first**

Create `frontend/src/citizen-frontend/pwa/detectPlatform.spec.ts`:

```ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import { detectPlatform } from './detectPlatform'

describe('detectPlatform', () => {
  it('detects iOS Safari (iPhone)', () => {
    const ua =
      'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1'
    expect(detectPlatform(ua)).toEqual({ os: 'ios', isSafari: true })
  })

  it('detects iOS Chrome (CriOS) as iOS, not Safari', () => {
    const ua =
      'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/120.0.6099.119 Mobile/15E148 Safari/604.1'
    expect(detectPlatform(ua)).toEqual({ os: 'ios', isSafari: false })
  })

  it('detects iPad as iOS', () => {
    const ua =
      'Mozilla/5.0 (iPad; CPU OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1'
    expect(detectPlatform(ua).os).toBe('ios')
  })

  it('detects Android Chrome', () => {
    const ua =
      'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36'
    expect(detectPlatform(ua)).toEqual({ os: 'android' })
  })

  it('detects Android Firefox', () => {
    const ua =
      'Mozilla/5.0 (Android 14; Mobile; rv:121.0) Gecko/121.0 Firefox/121.0'
    expect(detectPlatform(ua)).toEqual({ os: 'android' })
  })

  it('returns "other" for an unknown UA', () => {
    expect(detectPlatform('SomeBot/1.0')).toEqual({ os: 'other' })
  })

  it('returns "other" for desktop Chrome', () => {
    const ua =
      'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
    expect(detectPlatform(ua)).toEqual({ os: 'other' })
  })
})
```

- [ ] **Step 2: Run the test and confirm it fails**

Run: `cd frontend && yarn test --project citizen-frontend src/citizen-frontend/pwa/detectPlatform.spec.ts`
Expected: FAIL — `Cannot find module './detectPlatform'`.

- [ ] **Step 3: Implement `detectPlatform`**

Create `frontend/src/citizen-frontend/pwa/detectPlatform.ts`:

```ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type Platform =
  | { os: 'ios'; isSafari: boolean }
  | { os: 'android' }
  | { os: 'other' }

export function detectPlatform(
  userAgent: string = typeof navigator !== 'undefined'
    ? navigator.userAgent
    : ''
): Platform {
  // iOS detection: iPhone, iPad, iPod. iPadOS 13+ also matches "Macintosh"
  // with touch support, but we deliberately only treat the explicit
  // identifiers as iOS to avoid false positives on macOS Safari.
  if (/iPad|iPhone|iPod/.test(userAgent)) {
    // On iOS, the only browser engine is WebKit. "Real" Safari is the only
    // one that exposes the official "Add to Home Screen" UI; CriOS, FxiOS,
    // EdgiOS etc. are wrappers that don't.
    const isSafari = !/CriOS|FxiOS|EdgiOS|OPiOS/.test(userAgent)
    return { os: 'ios', isSafari }
  }

  if (/Android/.test(userAgent)) {
    return { os: 'android' }
  }

  return { os: 'other' }
}
```

- [ ] **Step 4: Run the test again and confirm it passes**

Run: `cd frontend && yarn test --project citizen-frontend src/citizen-frontend/pwa/detectPlatform.spec.ts`
Expected: PASS — all 7 tests pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/citizen-frontend/pwa/detectPlatform.ts frontend/src/citizen-frontend/pwa/detectPlatform.spec.ts
git commit -m "Add detectPlatform helper for PWA install fallback"
```

---

## Task 7: TDD `usePwaInstall` hook

**Files:**
- Create: `frontend/src/citizen-frontend/pwa/usePwaInstall.ts`
- Create: `frontend/src/citizen-frontend/pwa/usePwaInstall.spec.tsx`

The hook starts in `fallback` (assume no native prompt), upgrades to `native` when `beforeinstallprompt` fires, and switches to `standalone` when the page is running standalone (either at mount or after `appinstalled`).

- [ ] **Step 1: Write the failing tests first**

Create `frontend/src/citizen-frontend/pwa/usePwaInstall.spec.tsx`:

```tsx
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { usePwaInstall } from './usePwaInstall'

class MockBeforeInstallPromptEvent extends Event {
  prompt = vi.fn().mockResolvedValue(undefined)
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }> =
    Promise.resolve({ outcome: 'accepted', platform: 'web' })

  constructor() {
    super('beforeinstallprompt', { cancelable: true })
  }
}

const setMatchMediaStandalone = (matches: boolean) => {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: vi.fn().mockImplementation((query: string) => ({
      matches: query === '(display-mode: standalone)' ? matches : false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn()
    }))
  })
}

describe('usePwaInstall', () => {
  beforeEach(() => {
    setMatchMediaStandalone(false)
    Object.defineProperty(navigator, 'userAgent', {
      writable: true,
      configurable: true,
      value:
        'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1'
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('starts in fallback state with the detected platform', () => {
    const { result } = renderHook(() => usePwaInstall())
    expect(result.current.kind).toBe('fallback')
    if (result.current.kind === 'fallback') {
      expect(result.current.platform.os).toBe('ios')
    }
  })

  it('starts in standalone state when display-mode: standalone matches at mount', () => {
    setMatchMediaStandalone(true)
    const { result } = renderHook(() => usePwaInstall())
    expect(result.current.kind).toBe('standalone')
  })

  it('transitions to native state when beforeinstallprompt fires', () => {
    const { result } = renderHook(() => usePwaInstall())
    expect(result.current.kind).toBe('fallback')

    const event = new MockBeforeInstallPromptEvent()
    act(() => {
      window.dispatchEvent(event)
    })

    expect(result.current.kind).toBe('native')
  })

  it('promptInstall calls the deferred event prompt and clears state on accept', async () => {
    const { result } = renderHook(() => usePwaInstall())

    const event = new MockBeforeInstallPromptEvent()
    act(() => {
      window.dispatchEvent(event)
    })

    expect(result.current.kind).toBe('native')

    await act(async () => {
      if (result.current.kind === 'native') {
        await result.current.promptInstall()
      }
    })

    expect(event.prompt).toHaveBeenCalledOnce()
  })

  it('transitions to standalone when appinstalled fires', () => {
    const { result } = renderHook(() => usePwaInstall())

    act(() => {
      window.dispatchEvent(new Event('appinstalled'))
    })

    expect(result.current.kind).toBe('standalone')
  })
})
```

- [ ] **Step 2: Run the tests and confirm they fail**

Run: `cd frontend && yarn test --project citizen-frontend src/citizen-frontend/pwa/usePwaInstall.spec.tsx`
Expected: FAIL — `Cannot find module './usePwaInstall'`.

- [ ] **Step 3: Implement the hook**

Create `frontend/src/citizen-frontend/pwa/usePwaInstall.ts`:

```ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useEffect, useRef, useState } from 'react'

import { detectPlatform, type Platform } from './detectPlatform'

interface BeforeInstallPromptEvent extends Event {
  readonly userChoice: Promise<{
    outcome: 'accepted' | 'dismissed'
    platform: string
  }>
  prompt(): Promise<void>
}

export type PwaInstallState =
  | { kind: 'standalone' }
  | { kind: 'native'; promptInstall: () => Promise<void> }
  | { kind: 'fallback'; platform: Platform }

const isStandalone = (): boolean => {
  if (typeof window === 'undefined') return false
  if (window.matchMedia('(display-mode: standalone)').matches) return true
  // Older iOS Safari exposes a non-standard `navigator.standalone` flag.
  const nav = window.navigator as Navigator & { standalone?: boolean }
  return nav.standalone === true
}

export function usePwaInstall(): PwaInstallState {
  const deferredPromptRef = useRef<BeforeInstallPromptEvent | null>(null)

  const [state, setState] = useState<PwaInstallState>(() => {
    if (isStandalone()) return { kind: 'standalone' }
    return { kind: 'fallback', platform: detectPlatform() }
  })

  const promptInstall = useCallback(async () => {
    const deferred = deferredPromptRef.current
    if (!deferred) return
    await deferred.prompt()
    try {
      await deferred.userChoice
    } finally {
      deferredPromptRef.current = null
    }
  }, [])

  useEffect(() => {
    const onBeforeInstallPrompt = (event: Event) => {
      event.preventDefault()
      deferredPromptRef.current = event as BeforeInstallPromptEvent
      setState({ kind: 'native', promptInstall })
    }

    const onAppInstalled = () => {
      deferredPromptRef.current = null
      setState({ kind: 'standalone' })
    }

    window.addEventListener('beforeinstallprompt', onBeforeInstallPrompt)
    window.addEventListener('appinstalled', onAppInstalled)

    return () => {
      window.removeEventListener('beforeinstallprompt', onBeforeInstallPrompt)
      window.removeEventListener('appinstalled', onAppInstalled)
    }
  }, [promptInstall])

  return state
}
```

- [ ] **Step 4: Run the tests and confirm they all pass**

Run: `cd frontend && yarn test --project citizen-frontend src/citizen-frontend/pwa/usePwaInstall.spec.tsx`
Expected: PASS — all 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/citizen-frontend/pwa/usePwaInstall.ts frontend/src/citizen-frontend/pwa/usePwaInstall.spec.tsx
git commit -m "Add usePwaInstall hook for citizen PWA install state"
```

---

## Task 8: Add `pwaInstall` translation keys (Finnish)

**Files:**
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx`

Restructure the `loginPage.addToHomeScreen` block into `loginPage.pwaInstall`. The instruction JSX trees move over verbatim; we add three new short strings (`button`, `iosUseSafariNote`, `notSupported`) and drop the old collapsible labels (`title`, `subTitle`, `ios`, `android`).

- [ ] **Step 1: Replace the `addToHomeScreen` block (lines 206–244)**

In `frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx`, replace this block:

```tsx
    addToHomeScreen: {
      title: 'Haluatko löytää tälle sivulle helpommin?',
      subTitle: 'Lisää eVaka puhelimesi kotivalikkoon!',
      ios: 'Lisää pikakuvake iOS-laitteella (Safari)',
      android: 'Lisää pikakuvake Android-laitteella (Chrome)',
      instructions: {
        ios: (
          <>
            <OrderedList>
              <li>
                Paina “jaa”-ikonia selaimen alalaidassa (neliö, jossa on
                ylöspäin osoittava nuoli)
              </li>
              <li>Skrollaa alas ja valitse “lisää kotivalikkoon”</li>
              <li>Kirjoita halutessasi pikakuvakkeelle nimi</li>
              <li>Paina “Lisää” sivun yläreunasta</li>
            </OrderedList>
            <P>
              Nyt kotivalikossasi pitäisi näkyä ikoni, jonka painaminen avaa
              tämän sivun.
            </P>
          </>
        ),
        android: (
          <>
            <OrderedList>
              <li>Paina “valikko”-ikonia (⋮) selaimen oikeassa ylänurkassa</li>
              <li>Valitse “Lisää aloitusnäyttöön”</li>
              <li>Kirjoita halutessasi pikakuvakkeelle nimi</li>
              <li>Paina “Luo pikakuvake”</li>
            </OrderedList>
            <P>
              Nyt kotivalikossasi pitäisi näkyä ikoni, jonka painaminen avaa
              tämän sivun.
            </P>
          </>
        )
      }
    },
```

…with:

```tsx
    pwaInstall: {
      button: 'Lisää kotivalikkoon',
      iosUseSafariNote:
        'Asentaaksesi eVakan kotivalikkoon avaa tämä sivu Safari-selaimella.',
      notSupported:
        'Tämä selain ei tue eVakan asentamista kotivalikkoon. Kokeile uudelleen Safarilla (iOS) tai Chromella (Android).',
      instructions: {
        ios: (
          <>
            <OrderedList>
              <li>
                Paina “jaa”-ikonia selaimen alalaidassa (neliö, jossa on
                ylöspäin osoittava nuoli)
              </li>
              <li>Skrollaa alas ja valitse “lisää kotivalikkoon”</li>
              <li>Kirjoita halutessasi pikakuvakkeelle nimi</li>
              <li>Paina “Lisää” sivun yläreunasta</li>
            </OrderedList>
            <P>
              Nyt kotivalikossasi pitäisi näkyä ikoni, jonka painaminen avaa
              tämän sivun.
            </P>
          </>
        ),
        android: (
          <>
            <OrderedList>
              <li>Paina “valikko”-ikonia (⋮) selaimen oikeassa ylänurkassa</li>
              <li>Valitse “Lisää aloitusnäyttöön”</li>
              <li>Kirjoita halutessasi pikakuvakkeelle nimi</li>
              <li>Paina “Luo pikakuvake”</li>
            </OrderedList>
            <P>
              Nyt kotivalikossasi pitäisi näkyä ikoni, jonka painaminen avaa
              tämän sivun.
            </P>
          </>
        )
      }
    },
```

- [ ] **Step 2: Type-check**

Run: `cd frontend && yarn type-check`
Expected: errors in `sv.tsx` and `en.tsx` (they still have the old shape and now don't match the type derived from `fi.tsx`). That is fine — Tasks 9 and 10 fix them. We will type-check again at the end of Task 10.

- [ ] **Step 3: No commit yet**

We commit all three i18n files together at the end of Task 10 to keep the build green between commits.

---

## Task 9: Add `pwaInstall` translation keys (Swedish)

**Files:**
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx`

- [ ] **Step 1: Replace the `addToHomeScreen` block (lines 206–238)**

In `frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx`, replace this block:

```tsx
    addToHomeScreen: {
      title: 'Vill du hitta till denna sida lättare?',
      subTitle: 'Lägg till eVaka på din telefons hemskärm!',
      ios: 'Lägg till genväg på iOS (Safari)',
      android: 'Lägg till genväg på Android (Chrome)',
      instructions: {
        ios: (
          <>
            <OrderedList>
              <li>
                Tryck på “dela”-ikonen längst ner i webbläsaren (en fyrkant med
                en uppåtpil)
              </li>
              <li>Scrolla ner och välj “Lägg till på hemskärmen”</li>
              <li>Ge genvägen ett namn om du vill</li>
              <li>Tryck på “Lägg till” högst upp på sidan</li>
            </OrderedList>
            <P>Nu borde du se en ikon på din hemskärm som öppnar denna sida.</P>
          </>
        ),
        android: (
          <>
            <OrderedList>
              <li>Tryck på “meny”-ikonen (⋮) uppe till höger i webbläsaren</li>
              <li>Välj “Lägg till på startskärmen”</li>
              <li>Ge genvägen ett namn om du vill</li>
              <li>Tryck på “Skapa genväg”</li>
            </OrderedList>
            <P>Nu borde du se en ikon på din hemskärm som öppnar denna sida.</P>
          </>
        )
      }
    },
```

…with:

```tsx
    pwaInstall: {
      button: 'Lägg till på hemskärmen',
      iosUseSafariNote:
        'För att installera eVaka på hemskärmen, öppna denna sida i Safari.',
      notSupported:
        'Den här webbläsaren stöder inte installation av eVaka på hemskärmen. Försök igen med Safari (iOS) eller Chrome (Android).',
      instructions: {
        ios: (
          <>
            <OrderedList>
              <li>
                Tryck på “dela”-ikonen längst ner i webbläsaren (en fyrkant med
                en uppåtpil)
              </li>
              <li>Scrolla ner och välj “Lägg till på hemskärmen”</li>
              <li>Ge genvägen ett namn om du vill</li>
              <li>Tryck på “Lägg till” högst upp på sidan</li>
            </OrderedList>
            <P>Nu borde du se en ikon på din hemskärm som öppnar denna sida.</P>
          </>
        ),
        android: (
          <>
            <OrderedList>
              <li>Tryck på “meny”-ikonen (⋮) uppe till höger i webbläsaren</li>
              <li>Välj “Lägg till på startskärmen”</li>
              <li>Ge genvägen ett namn om du vill</li>
              <li>Tryck på “Skapa genväg”</li>
            </OrderedList>
            <P>Nu borde du se en ikon på din hemskärm som öppnar denna sida.</P>
          </>
        )
      }
    },
```

- [ ] **Step 2: No commit yet**

---

## Task 10: Add `pwaInstall` translation keys (English) and verify type-check

**Files:**
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx`

- [ ] **Step 1: Replace the `addToHomeScreen` block (lines 208–248)**

In `frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx`, replace this block:

```tsx
    addToHomeScreen: {
      title: 'Would you like to find this page more easily?',
      subTitle: 'Add eVaka to your phone’s home screen!',
      ios: 'Add a shortcut on iOS (Safari)',
      android: 'Add a shortcut on Android (Chrome)',
      instructions: {
        ios: (
          <>
            <OrderedList>
              <li>
                Tap the “share” icon at the bottom of the browser (a square with
                an upward arrow)
              </li>
              <li>Scroll down and select “Add to Home Screen”</li>
              <li>Optionally, enter a name for the shortcut</li>
              <li>Tap “Add” at the top of the page</li>
            </OrderedList>
            <P>
              Now you should see an icon on your home screen that opens this
              page.
            </P>
          </>
        ),
        android: (
          <>
            <OrderedList>
              <li>
                Tap the “menu” icon (⋮) at the top right corner of the browser
              </li>
              <li>Select “Add to Home screen”</li>
              <li>Optionally, enter a name for the shortcut</li>
              <li>Tap “Create shortcut”</li>
            </OrderedList>
            <P>
              Now you should see an icon on your home screen that opens this
              page.
            </P>
          </>
        )
      }
    },
```

…with:

```tsx
    pwaInstall: {
      button: 'Add to home screen',
      iosUseSafariNote:
        'To install eVaka to your home screen, open this page in Safari.',
      notSupported:
        'This browser does not support installing eVaka to the home screen. Please try again in Safari (iOS) or Chrome (Android).',
      instructions: {
        ios: (
          <>
            <OrderedList>
              <li>
                Tap the “share” icon at the bottom of the browser (a square with
                an upward arrow)
              </li>
              <li>Scroll down and select “Add to Home Screen”</li>
              <li>Optionally, enter a name for the shortcut</li>
              <li>Tap “Add” at the top of the page</li>
            </OrderedList>
            <P>
              Now you should see an icon on your home screen that opens this
              page.
            </P>
          </>
        ),
        android: (
          <>
            <OrderedList>
              <li>
                Tap the “menu” icon (⋮) at the top right corner of the browser
              </li>
              <li>Select “Add to Home screen”</li>
              <li>Optionally, enter a name for the shortcut</li>
              <li>Tap “Create shortcut”</li>
            </OrderedList>
            <P>
              Now you should see an icon on your home screen that opens this
              page.
            </P>
          </>
        )
      }
    },
```

- [ ] **Step 2: Verify no other call sites still reference the old keys**

Run:
```bash
cd frontend && grep -rn "addToHomeScreen" src/ --include="*.ts" --include="*.tsx"
```
Expected: only matches inside `LoginPage.tsx` (the existing `AddToHomeScreenInstructions` component, which Task 12 deletes). If anything else turns up, update it to read from `loginPage.pwaInstall` before continuing.

- [ ] **Step 3: Type-check**

Run: `cd frontend && yarn type-check`
Expected: still fails in `LoginPage.tsx` because the old `AddToHomeScreenInstructions` still references `i18n.loginPage.addToHomeScreen.*`. That is expected — Task 12 deletes it. The three i18n files themselves should now be internally consistent and match each other's shape.

- [ ] **Step 4: Commit all three i18n changes together**

```bash
git add frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx
git commit -m "Restructure home-screen install translations under loginPage.pwaInstall"
```

---

## Task 11: TDD `PwaInstallButton` component

**Files:**
- Create: `frontend/src/citizen-frontend/pwa/PwaInstallButton.tsx`
- Create: `frontend/src/citizen-frontend/pwa/PwaInstallButton.spec.tsx`

The component reads `usePwaInstall()` and the i18n context, and renders one of three things:
- `standalone` → nothing
- `native` → a primary button that calls `promptInstall()` on click
- `fallback` → a button + (when expanded) a `CollapsibleContentArea` containing only the iOS or Android instruction set, plus a Safari hint on iOS-non-Safari and a generic message on `other`

- [ ] **Step 1: Write the failing tests first**

Create `frontend/src/citizen-frontend/pwa/PwaInstallButton.spec.tsx`:

```tsx
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import React from 'react'
import { describe, expect, it, vi } from 'vitest'

import { PwaInstallButton } from './PwaInstallButton'
import { usePwaInstall, type PwaInstallState } from './usePwaInstall'

vi.mock('./usePwaInstall', async () => {
  const actual =
    await vi.importActual<typeof import('./usePwaInstall')>('./usePwaInstall')
  return {
    ...actual,
    usePwaInstall: vi.fn()
  }
})

vi.mock('../localization', () => ({
  useTranslation: () => ({
    common: { openExpandingInfo: 'open' },
    loginPage: {
      pwaInstall: {
        button: 'Add to home screen',
        iosUseSafariNote: 'Open in Safari',
        notSupported: 'Not supported',
        instructions: {
          ios: <div data-qa="ios-instructions">iOS steps</div>,
          android: <div data-qa="android-instructions">Android steps</div>
        }
      }
    }
  })
}))

const mockedUsePwaInstall = vi.mocked(usePwaInstall)

const setState = (state: PwaInstallState) => {
  mockedUsePwaInstall.mockReturnValue(state)
}

describe('PwaInstallButton', () => {
  it('renders nothing when running standalone', () => {
    setState({ kind: 'standalone' })
    const { container } = render(<PwaInstallButton />)
    expect(container).toBeEmptyDOMElement()
  })

  it('renders a button that calls promptInstall in native state', async () => {
    const promptInstall = vi.fn().mockResolvedValue(undefined)
    setState({ kind: 'native', promptInstall })

    render(<PwaInstallButton />)

    const button = screen.getByRole('button', { name: 'Add to home screen' })
    await userEvent.click(button)

    expect(promptInstall).toHaveBeenCalledOnce()
  })

  it('shows only iOS instructions in fallback state on iOS Safari', async () => {
    setState({
      kind: 'fallback',
      platform: { os: 'ios', isSafari: true }
    })

    render(<PwaInstallButton />)
    await userEvent.click(
      screen.getByRole('button', { name: 'Add to home screen' })
    )

    expect(screen.getByTestId('ios-instructions')).toBeInTheDocument()
    expect(screen.queryByTestId('android-instructions')).not.toBeInTheDocument()
    expect(screen.queryByText('Open in Safari')).not.toBeInTheDocument()
  })

  it('shows iOS instructions plus a "use Safari" note on iOS non-Safari', async () => {
    setState({
      kind: 'fallback',
      platform: { os: 'ios', isSafari: false }
    })

    render(<PwaInstallButton />)
    await userEvent.click(
      screen.getByRole('button', { name: 'Add to home screen' })
    )

    expect(screen.getByTestId('ios-instructions')).toBeInTheDocument()
    expect(screen.getByText('Open in Safari')).toBeInTheDocument()
  })

  it('shows only Android instructions in fallback state on Android', async () => {
    setState({ kind: 'fallback', platform: { os: 'android' } })

    render(<PwaInstallButton />)
    await userEvent.click(
      screen.getByRole('button', { name: 'Add to home screen' })
    )

    expect(screen.getByTestId('android-instructions')).toBeInTheDocument()
    expect(screen.queryByTestId('ios-instructions')).not.toBeInTheDocument()
  })

  it('shows the not-supported message in fallback state on other platforms', async () => {
    setState({ kind: 'fallback', platform: { os: 'other' } })

    render(<PwaInstallButton />)
    await userEvent.click(
      screen.getByRole('button', { name: 'Add to home screen' })
    )

    expect(screen.getByText('Not supported')).toBeInTheDocument()
    expect(screen.queryByTestId('ios-instructions')).not.toBeInTheDocument()
    expect(screen.queryByTestId('android-instructions')).not.toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Run the tests and confirm they fail**

Run: `cd frontend && yarn test --project citizen-frontend src/citizen-frontend/pwa/PwaInstallButton.spec.tsx`
Expected: FAIL — `Cannot find module './PwaInstallButton'`.

- [ ] **Step 3: Implement the component**

Create `frontend/src/citizen-frontend/pwa/PwaInstallButton.tsx`:

```tsx
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import { Button } from 'lib-components/atoms/buttons/Button'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../localization'

import { usePwaInstall } from './usePwaInstall'

export const PwaInstallButton = React.memo(function PwaInstallButton() {
  const i18n = useTranslation()
  const state = usePwaInstall()
  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(() => setOpen((v) => !v), [])

  if (state.kind === 'standalone') {
    return null
  }

  if (state.kind === 'native') {
    return (
      <Button
        appearance="button"
        primary
        text={i18n.loginPage.pwaInstall.button}
        onClick={() => {
          void state.promptInstall()
        }}
      />
    )
  }

  // fallback
  const platform = state.platform
  return (
    <>
      <Button
        appearance="button"
        primary
        text={i18n.loginPage.pwaInstall.button}
        onClick={toggleOpen}
      />
      {open && (
        <>
          <Gap $size="s" />
          <CollapsibleContentArea
            open
            toggleOpen={toggleOpen}
            $opaque={false}
            title={i18n.loginPage.pwaInstall.button}
            $paddingHorizontal="0"
            $paddingVertical="0"
          >
            {platform.os === 'ios' && (
              <>
                {!platform.isSafari && (
                  <P $noMargin>{i18n.loginPage.pwaInstall.iosUseSafariNote}</P>
                )}
                {i18n.loginPage.pwaInstall.instructions.ios}
              </>
            )}
            {platform.os === 'android' && (
              <>{i18n.loginPage.pwaInstall.instructions.android}</>
            )}
            {platform.os === 'other' && (
              <P $noMargin>{i18n.loginPage.pwaInstall.notSupported}</P>
            )}
          </CollapsibleContentArea>
        </>
      )}
    </>
  )
})
```

The `Button` atom is a named export from `lib-components/atoms/buttons/Button.tsx` and takes `text: string`, optional `appearance` (defaults to `'button'`), `primary?: boolean`, and `onClick`. Throttling is built in so a fast double-tap on the install button does not fire `promptInstall()` twice.

- [ ] **Step 4: Run the tests and confirm they pass**

Run: `cd frontend && yarn test --project citizen-frontend src/citizen-frontend/pwa/PwaInstallButton.spec.tsx`
Expected: PASS — all 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/citizen-frontend/pwa/PwaInstallButton.tsx frontend/src/citizen-frontend/pwa/PwaInstallButton.spec.tsx
git commit -m "Add PwaInstallButton component for citizen login page"
```

---

## Task 12: Wire `PwaInstallButton` into `LoginPage.tsx`

**Files:**
- Modify: `frontend/src/citizen-frontend/login/LoginPage.tsx`

- [ ] **Step 1: Replace the `<AddToHomeScreenInstructions />` render site**

In `frontend/src/citizen-frontend/login/LoginPage.tsx` lines 107–110, replace:

```tsx
            <MobileOnly>
              <Gap $size="m" />
              <AddToHomeScreenInstructions />
            </MobileOnly>
```

…with:

```tsx
            <MobileOnly>
              <Gap $size="m" />
              <PwaInstallButton />
            </MobileOnly>
```

- [ ] **Step 2: Add the import and delete the now-unused component definition**

At the top of the file, add (next to other relative imports such as `import Footer from '../Footer'`):

```tsx
import { PwaInstallButton } from '../pwa/PwaInstallButton'
```

Delete the entire `AddToHomeScreenInstructions` component definition at lines 194–251 (the `const AddToHomeScreenInstructions = React.memo(...)` block).

- [ ] **Step 3: Drop now-unused imports**

After the deletion, these imports at the top of `LoginPage.tsx` are no longer referenced anywhere in the file:

- `useCallback` from `'react'` (only used inside the deleted component) — remove it from the React import line, leaving `import React, { Fragment, useState } from 'react'`.
- `useLocalStorage` from `'lib-common/utils/useLocalStorage'` — delete the entire import line.
- `CollapsibleContentArea` from `'lib-components/layout/Container'` — keep `Container` and `ContentArea`, drop `CollapsibleContentArea`. The line becomes:

```tsx
import Container, { ContentArea } from 'lib-components/layout/Container'
```

Verify by grepping inside the file:
```bash
cd frontend && grep -nE '\b(useCallback|useLocalStorage|CollapsibleContentArea)\b' src/citizen-frontend/login/LoginPage.tsx
```
Expected: no matches.

- [ ] **Step 4: Type-check**

Run: `cd frontend && yarn type-check`
Expected: completes successfully with no errors.

- [ ] **Step 5: Lint**

Run: `cd frontend && yarn lint src/citizen-frontend/login/LoginPage.tsx src/citizen-frontend/pwa/`
Expected: no errors and no warnings.

- [ ] **Step 6: Run the full citizen test project**

Run: `cd frontend && yarn test --project citizen-frontend`
Expected: all tests pass, including the existing citizen suite plus the three new spec files added in Tasks 6, 7, 11.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/citizen-frontend/login/LoginPage.tsx
git commit -m "Replace AddToHomeScreenInstructions with PwaInstallButton on citizen login"
```

---

## Task 13: Verify the build and run a manual smoke test

This task produces no commit. It is a verification gate before declaring the work complete.

- [ ] **Step 1: Full type-check**

Run: `cd frontend && yarn type-check`
Expected: passes.

- [ ] **Step 2: Full lint**

Run: `cd frontend && yarn lint`
Expected: passes with zero warnings (`--max-warnings 0`).

- [ ] **Step 3: Full test suite**

Run: `cd frontend && yarn test`
Expected: all projects (citizen-frontend, employee-frontend, lib-components, lib-common, eslint-plugin) pass.

- [ ] **Step 4: Production build**

Run: `cd frontend && yarn build 2>&1 | tail -40`
Expected: build succeeds. Verify the artifacts:

```bash
ls frontend/dist/bundle/service-worker.js
ls frontend/dist/bundle/employee/mobile/service-worker.js
ls frontend/dist/bundle/citizen/manifest.json   # may be at this path or under public/citizen depending on Vite copy behaviour
ls frontend/dist/bundle/citizen/evaka-192px.png
```

If the `manifest.json` and icons are not under `dist/bundle/citizen/`, check `dist/bundle/` directly — Vite copies the `public/` tree into the build output root, so the citizen assets should appear at `dist/bundle/citizen/`.

- [ ] **Step 5: Manual browser smoke test**

Run: `cd frontend && yarn dev`
Open the citizen app in a browser. For each scenario, verify the listed expectation. The "expected platform" values come from the network's user agent — use Chrome DevTools Device Mode to simulate.

| Scenario | Expectation |
|---|---|
| Desktop Chrome | No install button visible (`<MobileOnly>` hides it). |
| Mobile (DevTools, e.g. iPhone) | Login screen shows a primary "Lisää kotivalikkoon" button instead of the old collapsible block. |
| Mobile Chrome / Android Chrome | Clicking the button triggers Chrome's native install prompt (in real Chrome — DevTools may not fire `beforeinstallprompt`). |
| Mobile Safari (iPhone real device) | Clicking the button expands the iOS instruction set. No Android instructions are shown. |
| Mobile iOS Chrome | Clicking the button expands the iOS instruction set **and** shows the "open in Safari" note. |
| Already-installed (after Add to Home Screen) | Reopening the app from the home icon hides the button entirely (standalone state). |
| DevTools → Application → Manifest | Loads `/citizen/manifest.json` and shows name=eVaka, three icons, theme color #3273c9. |
| DevTools → Application → Service Workers | A service worker for scope `/` is activated. |

- [ ] **Step 6: Done**

If all checks pass, the work is complete. The plan introduced the citizen PWA infrastructure, replaced the iOS/Android collapsible message with a smarter install button, and only ever shows instructions for the user's actual platform.

---

## Notes for the implementer

- **Plain JS service worker, not TypeScript.** The existing employee mobile worker is `.js` with a `@type` JSDoc cast — match that style. Vite's lib-mode build accepts plain JS entries.
- **The `serviceWorker()` plugin is now called twice** with different `name` keys (so the Vite plugin name is unique per call — Vite errors out on duplicate plugin names without the suffix).
- **The hook starts pessimistic.** On Chromium-based browsers there is a brief window between mount and the `beforeinstallprompt` event firing where the user would see the fallback button label. Since the same label string is used in both states, this transition is invisible — only the click behaviour changes.
- **Do not register the service worker from a worker context.** Registration belongs in `index.tsx`, after React mounts, gated by `'serviceWorker' in navigator`.
- **The `addToHomeScreen` translation key is intentionally removed**, not aliased. There is no other call site (Task 10 step 2 verifies this); leaving it would create dead code.
