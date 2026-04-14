# Mobile Action Bar Liquid Glass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reskin the citizen-frontend mobile bottom action bar as a floating liquid-glass pill, matching the greenhouse-solar-heater reference, with graceful fallback to a frosted blur on browsers that don't support SVG-filter-based `backdrop-filter`.

**Architecture:** The existing `BottomBar` styled-component becomes a floating 360×72 pill positioned 16px above the safe area. Glass visuals live in a `::before` pseudo-element with a three-tier cascade — solid translucent white, frosted blur, then Chromium-only SVG refraction via `backdrop-filter: url()`. The SVG filter is inlined inside `MobileNav.tsx` and references two baked PNG maps (displacement + specular) colocated with the component. The menu drawer extends to the viewport floor so the pill refracts its white surface when open.

**Tech Stack:** React + styled-components, Vite asset imports (PNG → URL), SVG `<filter>` with `feDisplacementMap` / `feImage` / `feBlend`, CSS `backdrop-filter` (both url() and blur() forms).

**Spec:** `docs/superpowers/specs/2026-04-14-mobile-action-bar-liquid-glass-design.md`

---

## File Structure

**New files (colocated with MobileNav):**

- `frontend/src/citizen-frontend/navigation/liquid-glass/displacement.png` — RG-channel normal map for `feDisplacementMap`, 360×72, copied verbatim from greenhouse
- `frontend/src/citizen-frontend/navigation/liquid-glass/specular.png` — alpha highlight mask for the rim, 360×72, copied verbatim from greenhouse
- `frontend/src/citizen-frontend/navigation/liquid-glass/meta.json` — records bake parameters (`{width, height, radius, bezelWidth, thickness, n, light, specularPower, maxDisplacement}`), copied verbatim from greenhouse
- `frontend/src/citizen-frontend/navigation/liquid-glass/generate.mjs` — Playwright-based regeneration script, copied from greenhouse with output paths rewritten; not invoked at build time

**Modified files:**

- `frontend/src/citizen-frontend/navigation/const.ts` — `mobileBottomNavHeight` constant goes from `60` → `88` with updated semantics comment
- `frontend/src/citizen-frontend/navigation/MobileNav.tsx` — inline SVG filter added; `BottomBar` styled.nav rewritten for the floating pill + glass `::before`; `MenuContainer`'s `bottom` and `height` updated to extend under the pill; two PNG URL imports added

**Explicitly not touched:** `App.tsx`, `ActionPickerModal.tsx`, `CalendarListView.tsx`, `ThreadList.tsx`, `frontend/index.html`, any i18n file, any employee-side file, any test file.

---

## Task 1: Copy the baked assets and regeneration script

**Files:**
- Create: `frontend/src/citizen-frontend/navigation/liquid-glass/displacement.png`
- Create: `frontend/src/citizen-frontend/navigation/liquid-glass/specular.png`
- Create: `frontend/src/citizen-frontend/navigation/liquid-glass/meta.json`
- Create: `frontend/src/citizen-frontend/navigation/liquid-glass/generate.mjs`

No tests for this task — it's a file copy. The assets get exercised end-to-end in Task 4.

- [ ] **Step 1: Create the target directory**

```bash
mkdir -p /Volumes/evaka/evaka-glass/frontend/src/citizen-frontend/navigation/liquid-glass
```

- [ ] **Step 2: Copy the two PNG assets verbatim**

```bash
cp /Volumes/evaka/greenhouse-solar-heater/playground/assets/liquid-glass-displacement.png \
   /Volumes/evaka/evaka-glass/frontend/src/citizen-frontend/navigation/liquid-glass/displacement.png

cp /Volumes/evaka/greenhouse-solar-heater/playground/assets/liquid-glass-specular.png \
   /Volumes/evaka/evaka-glass/frontend/src/citizen-frontend/navigation/liquid-glass/specular.png
```

- [ ] **Step 3: Verify the PNGs are 360×72**

```bash
file /Volumes/evaka/evaka-glass/frontend/src/citizen-frontend/navigation/liquid-glass/displacement.png
file /Volumes/evaka/evaka-glass/frontend/src/citizen-frontend/navigation/liquid-glass/specular.png
```

Expected output contains `360 x 72` for both files.

- [ ] **Step 4: Create the meta.json file (copied verbatim from greenhouse)**

Create `frontend/src/citizen-frontend/navigation/liquid-glass/meta.json` with the exact content below:

```json
{
  "width": 360,
  "height": 72,
  "radius": 36,
  "bezelWidth": 28,
  "thickness": 140,
  "n": 1.5,
  "light": {
    "x": -0.707,
    "y": -0.707
  },
  "specularPower": 2,
  "maxDisplacement": 32.376661632244854
}
```

The `maxDisplacement` value must match exactly — it's used as the `scale` attribute on `feDisplacementMap` in Task 4. The pre-commit `add-license-headers.sh` hook does not add headers to JSON files, so no header is needed.

- [ ] **Step 5: Create the regeneration script (copied from greenhouse with output paths rewritten)**

Create `frontend/src/citizen-frontend/navigation/liquid-glass/generate.mjs` with this content. It's functionally identical to `greenhouse-solar-heater/scripts/generate-liquid-glass.mjs` — only the three `path.resolve` lines at the top of the output-paths block change so the script writes into this directory (relative to where it's run, which should be the `frontend/` dir).

```javascript
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { chromium } from 'playwright'
import { writeFileSync } from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

// Regeneration script for the liquid-glass displacement + specular maps.
// Not invoked at build time. Run manually from the `frontend/` directory:
//
//   node src/citizen-frontend/navigation/liquid-glass/generate.mjs
//
// Requires playwright to be installed (it already is, for e2e tests).

const here = path.dirname(fileURLToPath(import.meta.url))

const config = {
  width: 360,
  height: 72,
  radius: 36,
  bezelWidth: 28,
  thickness: 140,
  n: 1.5,
  light: { x: -0.707, y: -0.707 },
  specularPower: 2.0
}

const displacementOut = path.join(here, 'displacement.png')
const specularOut = path.join(here, 'specular.png')
const metaOut = path.join(here, 'meta.json')

const browser = await chromium.launch()
const page = await browser.newPage()

const result = await page.evaluate(async (cfg) => {
  const {
    width,
    height,
    radius,
    bezelWidth,
    thickness,
    n,
    light,
    specularPower
  } = cfg

  const samples = 256
  const magLut = new Array(samples)
  const sinThetaLut = new Array(samples)
  let maxMag = 0
  for (let i = 0; i < samples; i++) {
    const u = i / (samples - 1)
    const h = thickness * Math.sqrt(Math.max(0, 2 * u - u * u))
    const denom = Math.sqrt(Math.max(1e-10, 2 * u - u * u))
    const slope = (1 - u) / denom
    const theta1 = Math.atan(slope)
    const sinTheta2 = Math.min(1, Math.sin(theta1) / n)
    const theta2 = Math.asin(sinTheta2)
    const mag = h * Math.tan(theta1 - theta2)
    magLut[i] = mag
    sinThetaLut[i] = Math.sin(theta1)
    if (mag > maxMag) maxMag = mag
  }

  const sdf = (px, py) => {
    const qx = Math.abs(px - width / 2) - (width / 2 - radius)
    const qy = Math.abs(py - height / 2) - (height / 2 - radius)
    const ux = Math.max(qx, 0)
    const uy = Math.max(qy, 0)
    return Math.sqrt(ux * ux + uy * uy) + Math.min(Math.max(qx, qy), 0) - radius
  }
  const sdfGrad = (px, py) => {
    const e = 0.5
    const dx = sdf(px + e, py) - sdf(px - e, py)
    const dy = sdf(px, py + e) - sdf(px, py - e)
    const len = Math.hypot(dx, dy)
    if (len < 1e-6) return [0, 0]
    return [dx / len, dy / len]
  }

  const mkCanvas = () => {
    const c = document.createElement('canvas')
    c.width = width
    c.height = height
    return c
  }

  const dispCanvas = mkCanvas()
  const specCanvas = mkCanvas()
  const dispCtx = dispCanvas.getContext('2d')
  const specCtx = specCanvas.getContext('2d')
  const dispImg = dispCtx.createImageData(width, height)
  const specImg = specCtx.createImageData(width, height)

  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const px = x + 0.5
      const py = y + 0.5
      const d = sdf(px, py)
      let r = 128
      let g = 128
      let specA = 0
      if (d < 0 && -d < bezelWidth) {
        const u = -d / bezelWidth
        const idxF = u * (samples - 1)
        const i0 = Math.floor(idxF)
        const i1 = Math.min(samples - 1, i0 + 1)
        const t = idxF - i0
        const mag = magLut[i0] * (1 - t) + magLut[i1] * t
        const sinTheta = sinThetaLut[i0] * (1 - t) + sinThetaLut[i1] * t
        const [gx, gy] = sdfGrad(px, py)
        const inX = -gx
        const inY = -gy
        const dispX = inX * mag
        const dispY = inY * mag
        const nx = dispX / maxMag
        const ny = dispY / maxMag
        r = Math.max(0, Math.min(255, Math.round(128 + nx * 127)))
        g = Math.max(0, Math.min(255, Math.round(128 + ny * 127)))

        const dot = inX * light.x + inY * light.y
        const rim = Math.max(0, dot)
        const intensity = Math.pow(rim * sinTheta, specularPower)
        specA = Math.max(0, Math.min(255, Math.round(intensity * 255)))
      }
      const idx = (y * width + x) * 4
      dispImg.data[idx] = r
      dispImg.data[idx + 1] = g
      dispImg.data[idx + 2] = 128
      dispImg.data[idx + 3] = 255
      specImg.data[idx] = 255
      specImg.data[idx + 1] = 255
      specImg.data[idx + 2] = 255
      specImg.data[idx + 3] = specA
    }
  }
  dispCtx.putImageData(dispImg, 0, 0)
  specCtx.putImageData(specImg, 0, 0)
  return {
    dispDataUrl: dispCanvas.toDataURL('image/png'),
    specDataUrl: specCanvas.toDataURL('image/png'),
    maxMag
  }
}, config)

const writePng = (outPath, dataUrl) => {
  const base64 = dataUrl.split(',')[1]
  const buf = Buffer.from(base64, 'base64')
  writeFileSync(outPath, buf)
  return buf.length
}
const dispBytes = writePng(displacementOut, result.dispDataUrl)
const specBytes = writePng(specularOut, result.specDataUrl)

writeFileSync(
  metaOut,
  JSON.stringify({ ...config, maxDisplacement: result.maxMag }, null, 2)
)

console.log(`wrote ${displacementOut} (${dispBytes} bytes)`)
console.log(`wrote ${specularOut} (${specBytes} bytes)`)
console.log(
  `maxDisplacement = ${result.maxMag.toFixed(2)} px  (use as <feDisplacementMap scale>)`
)

await browser.close()
```

- [ ] **Step 6: Stage and commit**

```bash
cd /Volumes/evaka/evaka-glass
git add frontend/src/citizen-frontend/navigation/liquid-glass/
git status
```

Verify all four files (two PNGs, meta.json, generate.mjs) are staged.

```bash
git commit -m "$(cat <<'EOF'
feat(citizen/mobile-nav): add baked liquid-glass assets

Copies displacement + specular maps and the generator script from
greenhouse-solar-heater. These are consumed by the upcoming MobileNav
liquid-glass reskin via an inline SVG filter.
EOF
)"
```

The pre-commit hook will add SPDX headers to `generate.mjs` if missing; re-stage and re-run commit if it reports changes.

---

## Task 2: Update the navigation height constant

**Files:**
- Modify: `frontend/src/citizen-frontend/navigation/const.ts:17`

This task is a standalone constant change. No tests — the constant is consumed by styled-components at render time, not by runtime logic. Verification is through the full type-check in Task 5.

- [ ] **Step 1: Update the constant and its meaning**

Edit `frontend/src/citizen-frontend/navigation/const.ts`. Change line 17 from:

```typescript
export const mobileBottomNavHeight = 60
```

to:

```typescript
// Total bottom space reserved by the floating mobile nav pill.
// = 72 (pill height) + 16 (gap above safe-area inset).
// Consumers (ScrollableMain padding, ActionPickerModal, CalendarListView,
// ThreadList) use this to keep content clear of the pill.
export const mobileBottomNavHeight = 88
```

- [ ] **Step 2: Verify no other consumers need manual adjustment**

Run a grep to confirm the consumer list matches what the spec expected:

```bash
cd /Volumes/evaka/evaka-glass
```

Use the Grep tool (`pattern: "mobileBottomNavHeight"`, `path: "frontend/src/citizen-frontend"`) and verify the hits are the ones the spec already accounts for:

- `navigation/const.ts` — definition
- `App.tsx` — `ScrollableMain` padding-bottom (adapts automatically, correct)
- `calendar/ActionPickerModal.tsx` — modal positioning (adapts automatically)
- `calendar/CalendarListView.tsx` — FAB offset (adapts automatically)
- `messages/ThreadList.tsx` — FAB offset (adapts automatically)
- `navigation/MobileNav.tsx` — `MenuContainer` (intentionally overridden in Task 3)

If any additional unexpected consumer appears, stop and raise it before continuing.

- [ ] **Step 3: Commit**

```bash
cd /Volumes/evaka/evaka-glass
git add frontend/src/citizen-frontend/navigation/const.ts
git commit -m "$(cat <<'EOF'
refactor(citizen/mobile-nav): reserve 88px for floating pill

Re-interprets mobileBottomNavHeight as total reserved bottom space
(pill height + gap above safe area) instead of element height. All
downstream consumers pick up the new value automatically.
EOF
)"
```

---

## Task 3: Rewrite BottomBar as floating pill with glass ::before

**Files:**
- Modify: `frontend/src/citizen-frontend/navigation/MobileNav.tsx` — `BottomBar` styled.nav (currently `MobileNav.tsx:149-176`)
- Modify: `frontend/src/citizen-frontend/navigation/MobileNav.tsx` — `MenuContainer` styled.div (currently `MobileNav.tsx:444-464`)

This task handles the layout-only part of the reskin: the pill shape, the `::before` pseudo, the baseline tint, and the two blur tiers. It does **not** yet add the SVG filter or the refraction tier — that's Task 4. Splitting keeps each task verifiable in isolation.

- [ ] **Step 1: Rewrite the `BottomBar` styled.nav**

Replace the existing `BottomBar` definition (starts at `MobileNav.tsx:149`) with:

```typescript
const BottomBar = styled.nav`
  position: fixed;
  left: 50%;
  bottom: calc(16px + env(safe-area-inset-bottom, 0px));
  transform: translateX(-50%);
  width: calc(100% - 24px);
  max-width: 360px;
  height: 72px;
  border-radius: 36px;
  z-index: 25;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 12px;
  background: transparent;
  isolation: isolate;
  color: ${colors.grayscale.g100};

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: rgba(255, 255, 255, 0.55);
    backdrop-filter: blur(20px) saturate(1.2);
    -webkit-backdrop-filter: blur(20px) saturate(1.2);
    box-shadow:
      0 8px 24px rgba(0, 0, 0, 0.12),
      inset 0 0 0 1px rgba(255, 255, 255, 0.6);
    z-index: -1;
  }

  @media (min-width: ${desktopMin}) {
    display: none;
  }

  @media screen and (max-width: ${zoomedMobileMax}) {
    padding: 0 8px;
  }

  @media print {
    display: none;
  }
`
```

Things that were intentionally removed vs. the old version:

- `width: 100%` + `left: 0` + `bottom: 0` — pill is centered and floats now
- `background-color: ${colors.grayscale.g0}` — glass `::before` handles the background
- `box-shadow: 0px -2px 4px rgba(0, 0, 0, 0.15)` — the new shadow lives on the `::before`
- `height: ${mobileBottomNavHeight}px` — the pill uses its own literal `72px`; the constant represents reserved layout space, not pill height
- `overflow-x: auto` on the zoomed-mobile breakpoint — pill doesn't horizontally scroll; padding shrinks slightly instead
- `padding: ${defaultMargins.xs}` / `xxs` — replaced with literal `0 12px` and `0 8px` to match greenhouse's horizontal-only padding

- [ ] **Step 2: Update `MenuContainer` to extend under the pill**

Replace the existing `MenuContainer` definition (starts at `MobileNav.tsx:444`):

```typescript
const MenuContainer = styled.div`
  z-index: 24;
  position: fixed;
  overflow-y: scroll;
  top: ${headerHeightMobile}px;
  bottom: 0;
  right: 0;
  background: ${colors.grayscale.g0};
  box-sizing: border-box;
  width: 100vw;
  height: calc(100% - ${headerHeightMobile}px);
  padding: ${defaultMargins.s};
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  text-align: right;

  @media (min-width: ${desktopMin}) {
    display: none;
  }
`
```

Only two lines change from the current version:
- `bottom: ${mobileBottomNavHeight}px;` → `bottom: 0;`
- `height: calc(100% - ${headerHeightMobile}px - ${mobileBottomNavHeight}px);` → `height: calc(100% - ${headerHeightMobile}px);`

This makes the white drawer extend all the way to the viewport floor, so the floating pill refracts/blurs the drawer's white surface when the menu is open.

- [ ] **Step 3: Run type-check**

```bash
cd /Volumes/evaka/evaka-glass/frontend
node .yarn/releases/yarn-4.13.0.cjs type-check
```

Expected: clean (0 errors). If missing, use `node_modules/.bin/tsc --build --force .` directly. Do **not** use `tsc --noEmit` — the repo uses project references.

- [ ] **Step 4: Visually verify blur fallback via Chrome DevTools**

Start the dev stack if not already running:

```bash
cd /Volumes/evaka/evaka-glass && mise start
```

Wait ~1–3 minutes for the Spring Boot cold start. Then via Chrome MCP:

1. `list_pages` / `new_page` → navigate to `http://localhost:9099/`
2. Log in via the suomi.fi "Tunnistaudu" button with the default test user
3. Emulate a mobile device (iPhone 12 / 390×844 works well — gets us into the `≥ 384px` refraction-viable range, but refraction isn't wired yet so we should see blur only)
4. `take_screenshot` → verify the floating pill renders with:
   - Rounded 36px corners
   - Centered horizontally with ~12px gap on each side
   - ~16px gap above the viewport floor
   - Frosted-blur background showing hints of the page content underneath
   - Dark icons + labels legible over the glass
5. Try viewport 360×800 to confirm the narrow-viewport path (still blur, pill is `calc(100% - 24px)` = 336px)
6. Tap the hamburger menu; verify the white drawer extends down past where the pill used to end, and the pill now shows as frosted-white over the white drawer

If any of those are off, fix before proceeding. The blur + layout is what this task is signing off on.

- [ ] **Step 5: Commit**

```bash
cd /Volumes/evaka/evaka-glass
git add frontend/src/citizen-frontend/navigation/MobileNav.tsx
git commit -m "$(cat <<'EOF'
feat(citizen/mobile-nav): float pill and add frosted-glass backdrop

BottomBar becomes a fixed 360x72 pill centered 16px above the safe
area, with a ::before pseudo carrying a translucent-white background
and a backdrop-filter blur. MenuContainer extends under the pill so
the drawer's white surface shows through the glass when the menu
is open.
EOF
)"
```

---

## Task 4: Add the SVG refraction filter

**Files:**
- Modify: `frontend/src/citizen-frontend/navigation/MobileNav.tsx` — add PNG URL imports at the top and inline SVG filter JSX inside the returned tree
- Modify: `frontend/src/citizen-frontend/navigation/MobileNav.tsx` — add the `@media (min-width: 384px)` refraction override inside the `BottomBar` styled-component

- [ ] **Step 1: Add the baked-asset URL imports**

At the top of `frontend/src/citizen-frontend/navigation/MobileNav.tsx`, alongside the other imports, add:

```typescript
import displacementUrl from './liquid-glass/displacement.png'
import specularUrl from './liquid-glass/specular.png'
```

Place these after the `lib-*` imports but before the local `./` imports, following the existing import-ordering convention in the file. If ESLint's `import/order` auto-fixer on the pre-commit hook relocates them, accept the diff.

- [ ] **Step 2: Inline the SVG filter as the first element of the mounted tree**

In the returned JSX (inside `UnwrapResult`'s render prop, where the user is already non-null — currently around `MobileNav.tsx:87-132`), wrap the existing `<><BottomBar>…</BottomBar>{menu…}</>` content so the new SVG is the first sibling:

```tsx
return (
  <UnwrapResult result={user} loading={() => null}>
    {(user) =>
      user ? (
        <>
          <LiquidGlassFilter />
          <BottomBar>
            …existing children unchanged…
          </BottomBar>
          {menuOpen === 'submenu' ? (
            …existing menu rendering unchanged…
          ) : null}
        </>
      ) : null
    }
  </UnwrapResult>
)
```

Then add the `LiquidGlassFilter` component definition near the other local component definitions in the file (e.g., just above `BottomBar`'s styled-component declaration, or just below `BottomBarLink`). It's a pure static component, so memoizing is optional — keep it simple:

```tsx
const LiquidGlassFilter = React.memo(function LiquidGlassFilter() {
  return (
    <svg
      width="0"
      height="0"
      style={{ position: 'absolute' }}
      aria-hidden="true"
    >
      <defs>
        <filter
          id="liquid-glass-bottom-nav"
          x="0%"
          y="0%"
          width="100%"
          height="100%"
          colorInterpolationFilters="sRGB"
        >
          <feGaussianBlur
            in="SourceGraphic"
            stdDeviation="2.4"
            result="blurred_source"
          />
          <feImage
            href={displacementUrl}
            x="0"
            y="0"
            width="360"
            height="72"
            result="displacement_map"
          />
          <feDisplacementMap
            in="blurred_source"
            in2="displacement_map"
            scale="32.376661632244854"
            xChannelSelector="R"
            yChannelSelector="G"
            result="displaced"
          />
          <feColorMatrix
            in="displaced"
            type="saturate"
            values="14"
            result="displaced_saturated"
          />
          <feImage
            href={specularUrl}
            x="0"
            y="0"
            width="360"
            height="72"
            result="specular_layer"
          />
          <feComposite
            in="displaced_saturated"
            in2="specular_layer"
            operator="in"
            result="specular_saturated"
          />
          <feComponentTransfer in="specular_layer" result="specular_faded">
            <feFuncA type="linear" slope="0.24" />
          </feComponentTransfer>
          <feBlend
            in="specular_saturated"
            in2="displaced"
            mode="normal"
            result="withSaturation"
          />
          <feBlend in="specular_faded" in2="withSaturation" mode="normal" />
        </filter>
      </defs>
    </svg>
  )
})
```

Key points:
- `scale="32.376661632244854"` is the exact `maxDisplacement` from `meta.json`. Keep the literal string to avoid float-precision drift. (If the JSX pragma rejects it, use `scale="32.38"` — greenhouse uses the truncated form too; both work.)
- `xChannelSelector` / `yChannelSelector` / `colorInterpolationFilters` are camelCase in JSX (they are `xChannelSelector="R"` lowercase in plain HTML but React normalizes them). Check that the builder doesn't warn; if it does, consult `react-dom`'s SVG attribute list.
- `href={displacementUrl}` — NOT `xlinkHref`. Modern React + modern browsers accept the plain `href` for `<feImage>`.
- `<feFuncA>` is inside `<feComponentTransfer>` — JSX is case-sensitive for SVG tags, get this right the first time.

- [ ] **Step 3: Add the refraction tier to `BottomBar`**

Inside the `BottomBar` styled-component template literal (from Task 3), add a new `@media` block right after the existing `@media screen and (max-width: ${zoomedMobileMax})` block and before `@media print`:

```css
  @media (min-width: 384px) {
    &::before {
      backdrop-filter: url(#liquid-glass-bottom-nav);
    }
  }
```

Only the refraction override lives here — the blur tier from Task 3 remains untouched. Browsers that honor `backdrop-filter: url()` will override the earlier `blur(20px) saturate(1.2)`; browsers that don't will keep the blur. Safari ignores `url()`, Firefox ignores `url()`, older Chromium ignores `url()` — they all gracefully fall through to blur tier.

**Do not add `-webkit-backdrop-filter: url(#…)`** — it does not exist. `-webkit-backdrop-filter` only supports keyword forms (blur, saturate, etc.) in all Safari versions.

- [ ] **Step 4: Run type-check**

```bash
cd /Volumes/evaka/evaka-glass/frontend
node .yarn/releases/yarn-4.13.0.cjs type-check
```

Expected: clean (0 errors). If PNG imports trip TypeScript, check for an existing `*.png` declaration in the citizen-frontend's ambient type files (it's standard for Vite projects; should already be present). If it isn't, add a minimal `declare module '*.png' { const url: string; export default url }` to `frontend/src/citizen-frontend/vite-env.d.ts` (or equivalent — find where it lives via Grep for other `declare module '*.`).

- [ ] **Step 5: Visually verify refraction engages on Chromium at ≥384px**

With the stack still running from Task 3 (or restart via `mise start`):

1. Navigate to `http://localhost:9099/` and log in as before
2. Emulate iPhone 12 (390×844) — puts us above the 384px gate
3. `take_screenshot` of a content-rich page (e.g., `/calendar` in month view)
4. Verify: the pill's rim shows visible displacement/warping of the content beneath it (not just a uniform blur). The inner tint is still frosted-white.
5. Switch viewport to 360×800 (below 384px) and screenshot again — verify the pill falls back to plain blur (no rim displacement)
6. Open the menu; pill should now read as frosted-white (displacement still visible on the rim, but the interior is near-white because everything behind is white)

If refraction doesn't engage on the ≥384px path:
- Check the browser console for CSP violations on the `href` attribute of `<feImage>`
- Confirm the `#liquid-glass-bottom-nav` filter ID matches between the SVG and the `backdrop-filter: url(...)` reference
- Confirm the `<svg>` element is actually present in the DOM (inspect in Chrome DevTools → Elements)
- Confirm `backdrop-filter: url(#liquid-glass-bottom-nav)` is applied on `BottomBar::before` (inspect the computed styles)

- [ ] **Step 6: Visually verify fallback in Firefox or Safari**

If available, open `http://localhost:9099/` in Safari or Firefox (on macOS) and confirm the pill shows the frosted-blur fallback — no console errors about unknown filters. If Firefox/Safari isn't readily available via Chrome MCP, skip this step and note it in the commit message.

- [ ] **Step 7: Commit**

```bash
cd /Volumes/evaka/evaka-glass
git add frontend/src/citizen-frontend/navigation/MobileNav.tsx
git commit -m "$(cat <<'EOF'
feat(citizen/mobile-nav): layer SVG refraction over the blur tier

Adds an inline <filter id="liquid-glass-bottom-nav"> that chains
feGaussianBlur, feDisplacementMap (against the baked displacement PNG)
and a feBlend over a specular highlight. The filter is applied via
backdrop-filter: url() under @media (min-width: 384px) so narrower
viewports and non-Chromium browsers keep the plain-blur fallback.
EOF
)"
```

---

## Task 5: Full-stack verification

- [ ] **Step 1: Type-check, lint, full build**

```bash
cd /Volumes/evaka/evaka-glass/frontend
node .yarn/releases/yarn-4.13.0.cjs type-check
node .yarn/releases/yarn-4.13.0.cjs lint
node_modules/.bin/vite build
```

Expected: all three clean. If `lint` complains about the import order around the new PNG imports, run `node .yarn/releases/yarn-4.13.0.cjs lint --fix` and stage the result as a **new commit** titled `chore(citizen/mobile-nav): eslint --fix import ordering`. Do not amend previous commits.

- [ ] **Step 2: Smoke-test the downstream consumers**

The constant change in Task 2 raised `mobileBottomNavHeight` from 60 → 88. Walk the consumers manually via Chrome MCP (stack running):

- [ ] **2a.** `/calendar` — scroll to the bottom; verify the calendar list's "create reservation" FAB clears the pill (it should sit above it with visible gap)
- [ ] **2b.** `/calendar` (month view) — open the action picker (plus button); verify the modal positioning still floats above the pill
- [ ] **2c.** `/messages` — verify the "compose" FAB in `ThreadList` clears the pill
- [ ] **2d.** Scroll to the bottom of any long page (e.g., `/personal-details`) and verify content doesn't disappear under the pill

If any consumer doesn't clear, the fault is **not** in the constant (it's consistent across all consumers) — check whether that consumer has a hardcoded offset alongside the constant that also needs adjusting.

- [ ] **Step 3: Cross-route visual smoke test**

Via Chrome MCP, iPhone 12 emulation:

- [ ] `/calendar` — pill refraction visible against calendar content
- [ ] `/messages` — pill refraction visible against thread list
- [ ] `/children` (if accessible) — pill refraction visible
- [ ] Open the hamburger menu from any route — pill reads as frosted-white over the drawer
- [ ] Close the drawer — pill returns to refracting the page content
- [ ] Desktop viewport (≥1024px) — `@media (min-width: ${desktopMin})` hides the pill entirely (unchanged behavior)

- [ ] **Step 4: No commit**

This task is verification only; nothing to commit. If any of the steps surfaced a bug, fix it in a small follow-up commit scoped tightly to the issue.

---

## Task 6: Clean up and final commit (if needed)

- [ ] **Step 1: Review the branch**

```bash
cd /Volumes/evaka/evaka-glass
git log --oneline master..HEAD
git diff master...HEAD --stat
```

Expected commits (in order):
1. `feat(citizen/mobile-nav): add baked liquid-glass assets`
2. `refactor(citizen/mobile-nav): reserve 88px for floating pill`
3. `feat(citizen/mobile-nav): float pill and add frosted-glass backdrop`
4. `feat(citizen/mobile-nav): layer SVG refraction over the blur tier`
5. (optional) any cleanup commits from Task 5

Expected files changed (ignoring the spec + plan docs from earlier):
- `frontend/src/citizen-frontend/navigation/const.ts`
- `frontend/src/citizen-frontend/navigation/MobileNav.tsx`
- `frontend/src/citizen-frontend/navigation/liquid-glass/displacement.png`
- `frontend/src/citizen-frontend/navigation/liquid-glass/specular.png`
- `frontend/src/citizen-frontend/navigation/liquid-glass/meta.json`
- `frontend/src/citizen-frontend/navigation/liquid-glass/generate.mjs`

- [ ] **Step 2: Decide if consolidation is needed**

If the intermediate commits from Tasks 1–4 tell a clean story, leave them alone. If there's a mid-task fix that should be squashed into its parent, handle it as a separate interactive rebase **only if the user explicitly asks** — default is to leave the commit history as-is.

- [ ] **Step 3: Report completion**

Summarize what was built, what was verified, and any caveats or known issues (e.g., "didn't test on Firefox/Safari"). The branch is then ready for PR.

---

## Verification Criteria

The implementation is complete when:

1. **Type-check clean:** `yarn type-check` exits 0
2. **Lint clean:** `yarn lint` exits 0
3. **Build clean:** `vite build` produces no errors
4. **Visual — Chromium desktop-mobile-emulation:** at viewports ≥ 384px, the pill shows visible rim refraction; at < 384px, the pill shows blur only; the pill is always centered, floating, and legible
5. **Visual — menu drawer:** tapping the hamburger opens a white drawer that extends under the pill, and the pill reads as frosted-white with visible rim displacement
6. **Visual — downstream layout:** calendar FAB, thread-list FAB, action-picker modal, and scrollable-main content all clear the pill
7. **Behavior unchanged:** all four nav slots navigate correctly, attention indicators still appear, unread counts still render, desktop viewport still hides the mobile nav entirely
8. **Safari/Firefox fallback:** if tested, pill shows frosted blur with no console errors

---

## Out of Scope (Do Not Do)

- Test files — spec explicitly calls out no new unit tests
- Reskinning the menu drawer background
- Reskinning the citizen-frontend header
- Any touching of the employee-frontend or employee-mobile apps
- Dark mode support
- Feature flagging
- Regenerating the displacement/specular PNGs (use greenhouse's baked output as-is)
- Any refactoring of `MobileNav.tsx` beyond what's listed (the file is doing a lot already, but cleanup is a separate effort)
