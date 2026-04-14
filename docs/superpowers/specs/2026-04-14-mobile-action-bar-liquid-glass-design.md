# Citizen mobile action bar — liquid glass

**Status:** Design
**Date:** 2026-04-14
**Scope:** `frontend/src/citizen-frontend` — `navigation/MobileNav.tsx` and its const file

## Goal

Reskin the citizen app's mobile bottom action bar to use a floating liquid-glass pill, visually matching the reference implementation in `greenhouse-solar-heater/playground` (Helios Canopy). On browsers that don't support SVG-filter-based `backdrop-filter`, degrade gracefully to a plain frosted-blur pill with an identical shape. Keep the component's behavior, routes, i18n, and accessibility unchanged.

## Non-goals

- Reskinning the full-screen menu drawer (it stays solid white; only its vertical bounds change so the pill floats over it).
- Reworking the citizen app header, sidebar, or any employee-side navigation.
- Changing icons, labels, routes, active-state logic, attention indicators, notification counts, or any navigation behavior.
- Dark mode support — the citizen app is light-themed and stays that way.
- Feature-flagging or A/B-ing the new style.

## Reference implementation

The greenhouse project already ships a working version of exactly this effect. The key files:

- `greenhouse-solar-heater/playground/public/style.css:159` — `.bottom-nav` + `::before` + `@media (min-width: 384px)` refraction gate
- `greenhouse-solar-heater/playground/index.html:33` — inline `<svg>` with `<defs><filter id="liquid-glass-bottom-nav">`
- `greenhouse-solar-heater/scripts/generate-liquid-glass.mjs` — the Playwright-based baking script that produces `displacement.png`, `specular.png`, and `meta.json`
- `greenhouse-solar-heater/playground/assets/liquid-glass-{displacement,specular}.png` — the baked outputs, 360×72 at radius 36 / bezel 28 / thickness 140 / n=1.5

We are **reusing the greenhouse PNGs verbatim** — no re-baking. The baked parameters (360×72, radius 36) define the pill's exact dimensions in the citizen app.

## Visual design

### Pill shape and positioning

- **Floating bottom pill**, detached from the viewport edge.
- `position: fixed; left: 50%; bottom: calc(16px + env(safe-area-inset-bottom, 0px)); transform: translateX(-50%)`
- `width: calc(100% - 24px); max-width: 360px; height: 72px; border-radius: 36px`
- `z-index: 25` (unchanged from current `BottomBar`)
- `isolation: isolate` — establishes a stacking context so the `::before` pseudo-element layers predictably and the backdrop-filter only samples what's under the pill, not the whole document.
- Inner padding: `0 12px`, matching greenhouse. Items use `justify-content: space-between; align-items: center` — same as today.
- The pill itself is transparent at the element level; all visual background comes from `::before`.

### Glass tint (light variant)

Evaka is a light-themed app. Unlike greenhouse's dark `rgba(20, 24, 32, 0.6)` glass, the citizen pill uses a **light frosted** treatment:

- Base background: `rgba(255, 255, 255, 0.55)`
- Outer shadow: `box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12)` (softer than greenhouse's dramatic dark shadow)
- Inner highlight: `inset 0 0 0 1px rgba(255, 255, 255, 0.6)` — gives the rim a crisp white edge that reads as glass thickness
- Icon + label colors: unchanged from current (`colors.grayscale.g100` dark icons, `colors.main.m2` for the active state)

Exact shadow and tint numbers will be eyeball-tuned during Chrome MCP verification — the values above are the starting point.

### Fallback chain (in CSS order)

The `::before` pseudo's background effect is declared as a three-tier chain so each tier progressively overrides the previous only when supported:

1. **Baseline (everything, including ancient browsers):**
   `background: rgba(255, 255, 255, 0.55)` + shadow + inner highlight.
   Works with zero browser support requirements. Just a translucent white pill.

2. **Blur tier (Safari, Firefox, older Chromium, any viewport):**
   `backdrop-filter: blur(20px) saturate(1.2); -webkit-backdrop-filter: blur(20px) saturate(1.2);`
   Gives the frosted-glass look. Vendor prefix covers Safari.

3. **Refraction tier (Chromium ≥ the version that supports `backdrop-filter: url()`, and only at viewports ≥ 384px):**
   Inside `BottomBar`'s styled-component template literal:
   ```js
   @media (min-width: 384px) {
     &::before {
       backdrop-filter: url(#liquid-glass-bottom-nav);
     }
   }
   ```
   Replaces the blur with the SVG filter. The SVG filter itself starts with a `feGaussianBlur` internally, so refraction adds displacement + specular highlights **on top of** a blur — we're not losing the blur by swapping, we're upgrading it.

The 384px media-query gate exists because the baked displacement map is exactly 360 pixels wide. On narrower viewports the pill's `width: calc(100% - 24px)` would fall below 360px and the map would be misaligned, producing visible seams. Below 384px the map isn't applied at all and the pill stays in blur-tier.

### Browser behavior matrix

| Environment | Effect |
|---|---|
| Any browser, viewport `< 384px` | translucent white pill + (blur if supported) |
| Safari any version, any width | translucent white pill + `-webkit-backdrop-filter` blur |
| Firefox any version, any width | translucent white pill + `backdrop-filter` blur |
| Chromium with `backdrop-filter: url()` support, viewport `≥ 384px` | full liquid-glass refraction |
| Ancient browser without any `backdrop-filter` | translucent white pill — still readable, just flat |

No JavaScript feature detection. No class toggling. The cascade does the work.

## Technical design

### SVG filter

The SVG with the filter definition is rendered **inside `MobileNav.tsx`** as the first element of the returned JSX tree (inside the `UnwrapResult` branch that actually mounts the nav). Static JSX, no hooks, reconciled once. `aria-hidden="true"` so screen readers skip it. `width="0" height="0" style={{ position: 'absolute' }}` so it takes no layout space.

The filter pipeline (copied verbatim from `greenhouse-solar-heater/playground/index.html:35`):

```jsx
<svg width="0" height="0" style={{ position: 'absolute' }} aria-hidden="true">
  <defs>
    <filter
      id="liquid-glass-bottom-nav"
      x="0%" y="0%" width="100%" height="100%"
      colorInterpolationFilters="sRGB"
    >
      <feGaussianBlur in="SourceGraphic" stdDeviation="2.4" result="blurred_source" />
      <feImage href={displacementUrl} x="0" y="0" width="360" height="72" result="displacement_map" />
      <feDisplacementMap
        in="blurred_source" in2="displacement_map"
        scale="32.38"
        xChannelSelector="R" yChannelSelector="G"
        result="displaced"
      />
      <feColorMatrix in="displaced" type="saturate" values="14" result="displaced_saturated" />
      <feImage href={specularUrl} x="0" y="0" width="360" height="72" result="specular_layer" />
      <feComposite in="displaced_saturated" in2="specular_layer" operator="in" result="specular_saturated" />
      <feComponentTransfer in="specular_layer" result="specular_faded">
        <feFuncA type="linear" slope="0.24" />
      </feComponentTransfer>
      <feBlend in="specular_saturated" in2="displaced" mode="normal" result="withSaturation" />
      <feBlend in="specular_faded" in2="withSaturation" mode="normal" />
    </filter>
  </defs>
</svg>
```

Key numbers are locked to the baked-map metadata:
- `width="360" height="72"` for both `<feImage>` elements must match the PNG resolution
- `scale="32.38"` is the `maxDisplacement` value from `meta.json`
- Changing any of these without re-running `generate.mjs` will break the effect

### Assets

New directory: `frontend/src/citizen-frontend/navigation/liquid-glass/`

| File | Source | Purpose |
|---|---|---|
| `displacement.png` | Copied from `greenhouse/playground/assets/liquid-glass-displacement.png` | RG-channel normal map for `feDisplacementMap` |
| `specular.png` | Copied from `greenhouse/playground/assets/liquid-glass-specular.png` | Alpha mask for the highlight rim |
| `meta.json` | Copied from `greenhouse/playground/assets/liquid-glass.json` | Records the bake parameters so regeneration is reproducible |
| `generate.mjs` | Copied from `greenhouse/scripts/generate-liquid-glass.mjs`, output paths adjusted | Playwright-based regeneration script; not invoked at build time |

Vite handles PNG imports natively — `MobileNav.tsx` does:

```ts
import displacementUrl from './liquid-glass/displacement.png'
import specularUrl from './liquid-glass/specular.png'
```

and passes the URLs into the `<feImage href={…}>` attributes. This gives the assets content hashes, cache-friendly headers, and fingerprinted filenames through the normal build pipeline.

SPDX headers: the PNGs stay un-headered (the hook only operates on text files). `meta.json` and `generate.mjs` get headers automatically via the lefthook pre-commit hook on first commit.

### Component structure

`BottomBar` becomes a transparent container, with the glass visuals in a `::before` pseudo-element:

```
BottomBar (transparent, establishes stacking context via isolation: isolate)
  ├─ ::before                              ← z-index: -1, glass background
  │      background / backdrop-filter / box-shadow
  └─ children (normal flow, above ::before)
        SVG filter defs (absolute, zero-size)
        BottomBarLink × 2-3 (calendar, messages, children)
        StyledButton (hamburger menu)
```

The `::before` pseudo is the only thing that carries the `backdrop-filter`, which keeps the glass effect sampling what's *behind the pill* while the children (icons, text) stay above it and render sharply. This is the greenhouse technique and is the only way to get crisp children over a filtered backdrop.

### Menu drawer: extend to cover the pill

Current `MenuContainer` (`MobileNav.tsx:444`) sits between the header and the bottom bar:

```css
top: ${headerHeightMobile}px;
bottom: ${mobileBottomNavHeight}px;
height: calc(100% - ${headerHeightMobile}px - ${mobileBottomNavHeight}px);
```

With the pill floating and glass-backed, the drawer should extend **all the way to the viewport bottom** so the pill refracts the drawer's white surface when the menu is open. Changes:

```css
bottom: 0;
height: calc(100% - ${headerHeightMobile}px);
```

This produces a pleasing effect: open the menu, the whole area below the header turns white, and the pill — still floating above — displays as a frosted-white island with subtle rim displacement (the displacement is visible; the tint reads as near-white because there's white everywhere behind it). Closing the menu restores the normal refraction of underlying page content.

### The `mobileBottomNavHeight` constant

`navigation/const.ts:17` — changes from `60` → `88`.

The constant's semantics shift slightly: from "height of the bar element" to "total bottom space reserved for the floating pill", which is `72` (pill height) + `16` (bottom gap above the safe area). A one-line comment explains the new meaning.

Every downstream consumer picks this up automatically:

- `App.tsx:152` — `ScrollableMain`'s `padding-bottom` grows from 60 → 88, so page content clears the floating pill
- `ActionPickerModal.tsx` — modal positioning offsets re-align to the new pill space
- `CalendarListView.tsx`, `ThreadList.tsx` — "create new" FABs lift to sit above the pill instead of over it
- `MobileNav.tsx:449` (`MenuContainer`) — this consumer is **deliberately overridden** (see above) because the drawer needs to extend *under* the pill, not stop at its top edge

Nothing else references the constant.

## File change list

| File | Change |
|---|---|
| `frontend/src/citizen-frontend/navigation/const.ts` | `mobileBottomNavHeight`: `60` → `88`, with a one-line comment explaining semantics |
| `frontend/src/citizen-frontend/navigation/MobileNav.tsx` | Add inline `<svg>` filter defs; rewrite `BottomBar` styled.nav into the pill + `::before` structure; update `MenuContainer`'s `bottom` and `height` to extend to the viewport floor; import the two baked PNG URLs |
| `frontend/src/citizen-frontend/navigation/liquid-glass/displacement.png` | **New** — copied from greenhouse |
| `frontend/src/citizen-frontend/navigation/liquid-glass/specular.png` | **New** — copied from greenhouse |
| `frontend/src/citizen-frontend/navigation/liquid-glass/meta.json` | **New** — copied from greenhouse (documents bake params) |
| `frontend/src/citizen-frontend/navigation/liquid-glass/generate.mjs` | **New** — copied from greenhouse with output paths adjusted |

**Explicitly not touched:** `App.tsx`, `MainContainer`/`ScrollableMain`, `ActionPickerModal`, `CalendarListView`, `ThreadList`, `frontend/index.html`, any i18n file, any icon file, any employee-side code, any test file.

## Accessibility

No regressions:

- Pill height increases from 60 → 72 ≥ the WCAG 24×24 AA target-size minimum.
- Icons and labels keep their current dark color (`colors.grayscale.g100`) on a light frosted background — contrast ratio is *better* than today's pure white because the glass still reads light even when content is behind it.
- `aria-hidden` on the inline `<svg>` so the filter defs are invisible to screen readers.
- Focus-visible styling on the links/buttons is inherited from existing `NavLink` / button styles — unchanged.
- The drawer change (extending to viewport floor) has no a11y impact: it was already a full-width overlay, just taller now.

## Testing strategy

- **No new unit tests.** The component's *behavior* — route matching, menu open/close, attention indicators, notification counts, responsive hiding on desktop — is unchanged. Adding render tests for styled-component output would verify the test's own mock rather than the real visual result.
- **Manual visual verification** via the toolchain documented in `CLAUDE.md`:
  1. `mise start` to bring up the full stack
  2. Chrome MCP navigates to `http://localhost:9099/`, logs in via suomi.fi "Tunnistaudu" with the default test user
  3. Resize to mobile widths (360, 375, 414) and verify the pill renders at each
  4. Verify the `< 384px` → blur fallback engages at 360px width
  5. Open the menu drawer and verify the pill reads as frosted-white over the drawer
  6. Visit `/calendar`, `/messages`, and the children view to confirm existing floating UI (the "new event" FAB in `CalendarListView`, the compose FAB in `ThreadList`, the action picker modal) still clears the pill
- **Type check:** `yarn type-check` (project references mode — don't use `tsc --noEmit`)
- **Lint:** lefthook's `eslint --fix` runs on commit. Project lint mustn't regress.
- **Cross-browser smoke:** out of scope for this task unless issues surface. The fallback chain is designed to degrade silently.
- **Real-device check:** optional, via the cloudflared quick tunnel documented in `CLAUDE.md`. The brainstorming session left this as the user's call; default is no, unless requested.

## Risks and mitigations

| Risk | Likelihood | Mitigation |
|---|---|---|
| Baked map misalignment at unusual widths | Medium | The `@media (min-width: 384px)` gate already handles this; pill is `max-width: 360px` so it caps at the baked width |
| Chromium refraction looks wrong on the citizen app's white-heavy pages (glass-on-white has very little to refract) | Medium | Light tint + subtle shadow means the pill still reads as a distinct surface even when there's nothing behind it; the inner highlight provides perceived thickness |
| `padding-bottom: 88px` creates noticeable empty space at the bottom of short pages | Low | Same as today for short pages, just 28px more; the floating pill needs the clearance anyway |
| Vite chunking the baked PNGs with a hash breaks the `<feImage href>` reference | Very low | Vite's image-import URL is a plain string at runtime; `feImage` accepts any URL |
| Safari treating the inline `<svg>` + `feImage` as a tainted canvas for `backdrop-filter` | Low | Not relevant — Safari ignores `backdrop-filter: url()` entirely and falls back to the blur tier |
| The constant rename (`60` → `88`) subtly breaks an unnoticed consumer | Low | Grep shows 6 consumers; all reviewed; 5 adapt automatically, 1 (`MenuContainer`) is overridden by design |

## Open questions

None. All pre-implementation choices have been confirmed:

- Shape: floating pill (A)
- Tint: light (white frosted)
- Dimensions: 360 × 72, radius 36 — reuse greenhouse PNGs verbatim
- Fallback: CSS cascade only (no JS detection); blur tier + refraction tier gated at `@media (min-width: 384px)`
- Drawer: stays solid white; extends to viewport floor so the pill refracts it when open
- Asset location: colocated under `navigation/liquid-glass/`
- Filter inlining: inside `MobileNav.tsx`, not `index.html`
