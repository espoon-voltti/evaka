<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# CLAUDE.md — eVaka working notes

Gotchas and shortcuts discovered while working in this repo. Read this first; it'll save you from rediscovering the same footguns.

## Toolchain / PATH

`node` and `yarn` are **not** on the default sandbox PATH. Two working options:

```bash
# Option 1: use mise shims (cleanest)
export PATH="/home/wnt.guest/.local/share/mise/shims:$PATH"
cd /Volumes/Evaka/evaka/frontend
node .yarn/releases/yarn-4.13.0.cjs <command>

# Option 2: invoke bundled binaries directly
cd /Volumes/Evaka/evaka/frontend
node_modules/.bin/vitest run --project citizen-frontend <path>
node_modules/.bin/vite build
node_modules/.bin/tsc --build --force .
```

## `yarn dev` concurrently subshell bug

`yarn dev` runs `concurrently -n tsc,vite -c blue,green 'yarn type-check:watch' 'vite --host'`. In this sandbox the `vite` subshell cannot find the `vite` binary on PATH and exits 127 immediately, killing the combined process. **Workaround**: start the Vite dev server via `mise start` (which uses pm2, below) or `node_modules/.bin/vite --host` directly.

## Full stack via `mise start`

`mise start` (task defined in `mise.toml`, runs in `compose/`) is the one-liner for the full dev stack:

```bash
mise start   # docker compose up -d + pm2 start + waits for ports 3000/8888/9099
mise stop    # tears both halves down
mise status  # shows current instance config
pm2 status   # shows apigw/frontend/service process state
pm2 logs <name>   # tails one service
```

After `mise start`:
- citizen frontend at <http://localhost:9099/>
- employee at <http://localhost:9099/employee>
- employee mobile at <http://localhost:9099/employee/mobile>
- apigw on `:3000`, Spring Boot service on `:8888`

Spring Boot cold-start usually takes 1–3 minutes; first page load may 503 briefly while the `service` process warms up.

## `yarn type-check` uses project references

Don't use `tsc --noEmit` for verification — the repo uses project references and needs `tsc --build --force .` (which is what `yarn type-check` runs). `--noEmit` alone silently skips referenced projects and will falsely report "clean".

## Pre-commit hook (lefthook)

Configured in the repo. You don't need to do anything special, but know what it does:

- **`./bin/add-license-headers.sh`** auto-writes SPDX headers into new files missing them (e.g. `SPDX-FileCopyrightText: 2017-<year> City of Espoo` + `LGPL-2.1-or-later`). If you create a new file and forget the header, the hook adds it during commit — accept the diff.
- **`eslint --fix`** runs on staged `.ts`/`.tsx` files and auto-rewrites where possible (e.g. it will rewrite `/Android/.test(ua)` to `ua.includes('Android')`). Don't fight it — re-stage and amend if needed.
- **`ktfmtPrecommit`** runs on staged Kotlin files.
- The trailing `Can't find lefthook in PATH` message after a successful commit is a harmless post-hook runner warning.
- **Never** use `--no-verify` to bypass; fix the underlying issue.

## Frontend testing patterns

- **Vitest projects**: spec files are picked up by project based on path. Citizen specs must match `src/citizen-frontend/**/*.spec.{ts,tsx}` to run in the `citizen-frontend` vitest project. The `vitest.config.ts` defines five projects: `citizen-frontend`, `employee-frontend`, `lib-components`, `lib-common`, `eslint-plugin`.
- **ThemeProvider for lib-components**: any spec that renders a styled-component from `lib-components` must be wrapped in `<TestContextProvider>` — otherwise you'll hit `Cannot read properties of undefined (reading 'main')` from theme lookups. Import from `'lib-components/utils/TestContextProvider'` and use the provided `testTranslations` for the lib-components i18n context:

  ```tsx
  import {
    TestContextProvider,
    testTranslations
  } from 'lib-components/utils/TestContextProvider'

  const wrap = (child: React.JSX.Element) => (
    <TestContextProvider translations={testTranslations}>
      {child}
    </TestContextProvider>
  )

  render(wrap(<MyComponent />))
  ```

- **`data-qa` vs `data-testid`**: the app uses `data-qa` attributes throughout for E2E selectors. Vitest is NOT configured to make `getByTestId` look at `data-qa` globally — don't add that config (it would affect all tests). In component specs, prefer `getByRole`/`getByText` over testids; when you do need testids, use `data-testid` in the mock/fixture JSX.
- **`Button` atom** (`lib-components/atoms/buttons/Button`) takes `text: string` prop (not `children`), plus `appearance`, `primary`, `onClick`. It has **built-in double-click throttling** — no need to add your own.

## i18n files are `.tsx`

Translations live at `src/lib-customizations/defaults/citizen/i18n/{fi,sv,en}.tsx`. They're `.tsx` because the values can be JSX (e.g. `<OrderedList>` / `<P>` trees for rich instruction blocks), not just strings. The shape is inferred from `fi.tsx`; the other two files must match it or TypeScript will flag them.

## Mobile testing via cloudflared quick tunnel

For testing PWA install / iOS Safari / mobile UX on a real device without deploying anywhere:

```bash
# One-time: install cloudflared (no sudo needed, static binary)
curl -fsSL -o /tmp/cloudflared \
  https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64
chmod +x /tmp/cloudflared

# Start tunnel (prints a https://<random>.trycloudflare.com URL)
/tmp/cloudflared tunnel --url http://localhost:9099
```

**Vite `allowedHosts`**: by default Vite blocks any Host header that isn't localhost and returns *"Blocked request. This host (…) is not allowed."* `frontend/vite.config.ts`'s `server.allowedHosts` already lists `.trycloudflare.com` so quick-tunnel URLs work out of the box. If you need to expose the dev server under a different hostname, add it to that array — Vite picks up `vite.config.ts` edits automatically via config-file HMR, no `pm2 restart` needed.

**Chrome DevTools device-mode caveat**: device-mode does **not** fire `beforeinstallprompt`. To exercise the native PWA install path you need a real mobile device hitting the tunnel URL.

## Citizen vs employee app scoping

The citizen app is served at root `/`; employee apps are at `/employee/` and `/employee/mobile/`. Service worker scopes and manifest paths mirror this split:

- Citizen: SW at `/service-worker.js` (scope `/`), manifest at `/citizen/manifest.json`, icons under `/citizen/`
- Employee mobile: SW at `/employee/mobile/service-worker.js` (scope `/employee/mobile/`), manifest + icons under `/employee/mobile/`

The vite `serviceWorker()` plugin in `vite.config.ts` is parameterised and invoked once per SW — don't duplicate the plugin body. The Plugin.name must be unique per call (suffix with the target name).
