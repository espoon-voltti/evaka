<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# iOS Safari smoke tests

A handful of WebdriverIO + Appium specs that drive the citizen app in real
iOS Safari in the iOS Simulator. They exist because the Playwright suite runs
Chromium only, and Chromium does not reproduce iOS WebKit layout bugs such as
a `position: fixed` modal getting clipped by the installed app's scrolling
area (PR #9840, hotfixed in #9975). Business logic stays in Playwright; keep
this suite to a couple of minutes.

Design and background: [docs/ios-safari-smoke-tests.md](../../../docs/ios-safari-smoke-tests.md).

macOS only.

## What is covered

Stage 1 runs in Mobile Safari with the app forced into its installed app
layout (`html[data-standalone]`). The specs store
`{"automatedTest":true,"forceStandalone":true}` in
`localStorage['evaka.testConfig']`, which the app reads into `window.evaka` on
load.

- `specs/calendar-modals.spec.ts`: reservation modal, absence modal and day
  view buttons are inside the viewport and not covered by anything
- `specs/navigation.spec.ts`: bottom navigation buttons are reachable and the
  messages link works
- `specs/messages.spec.ts`: the new message editor's send button is reachable

"Reachable" (`support/reachable.ts`) means fully inside the viewport, on top
in hit testing, **and actually painted**: the element is temporarily coloured
magenta and the simulator screenshot must show at least 95% of its area. The
paint check is the one that catches the #9840 regression: iOS 26 WebKit
paints the clipped modal footer under the bottom navigation while
`getBoundingClientRect()`, `elementFromPoint()` and even real taps still
reach the button.

Stage 2 runs the app as a real home screen web clip, where iOS sets
`navigator.standalone === true` and the app sets `html[data-standalone]`
itself. Only `{"automatedTest":true}` is stored in the clip, no
`forceStandalone`. It is opt-in with `IOS_SMOKE_WEBCLIP=1`.

- `specs/webclip/installed-app-modals.spec.ts`: the clip really runs
  standalone and fills the screen (no Safari style toolbar), and the
  reservation and absence modal buttons are reachable

The second capability in `wdio.conf.ts` has no `browserName`, so the session
starts on the home screen. Before the session, `support/webclip.ts` writes an
`eVaka` clip (URL `<frontend>/calendar`) into the simulator's
`data/Library/WebClips`, replacing earlier eVaka clips, and restarts
SpringBoard. The spec presses home, swipes to the page with the icon, taps it
and switches to the webview whose URL is under the frontend URL. The clip has
its own cookies and storage, so it logs in separately from Safari.

## Setup

1. Xcode with an iOS simulator runtime:

   ```sh
   sudo xcodebuild -runFirstLaunch
   xcodebuild -downloadPlatform iOS
   ```

2. Install the Appium XCUITest driver into `frontend/.appium` and prebuild
   WebDriverAgent (about a minute):

   ```sh
   cd frontend
   yarn install
   yarn ios-smoke:setup
   ```

3. Start the dev environment with `mise start`. If you use a dev instance
   (`mise instance <n>`), run the tests in a shell where mise has loaded the
   instance environment, or export at least `EVAKA_FRONTEND_PORT` and
   `EVAKA_IDP_PORT` yourself (read by `src/e2e-test/config.ts`, like for
   Playwright; the fixtures write VTJ data to the dummy IdP). `TUNNEL_URL`
   must be unset (`mise tunnel stop`), the simulator reaches `localhost`
   directly.

## Running

```sh
cd frontend
yarn ios-smoke
EVAKA_FRONTEND_PORT=9101 EVAKA_IDP_PORT=9092 yarn ios-smoke
IOS_SMOKE_WEBCLIP=1 yarn ios-smoke   # stage 1 and stage 2
```

Each stage runs in its own Appium session, one after the other. Stage 2 adds
about 25 seconds plus the SpringBoard restart.

The config finds or creates a simulator named `evaka-smoke` (iPhone 17,
newest installed iOS runtime) and boots it headless. Simulator.app is not
needed; open it if you want to watch. The first session after a simulator
boot installs WebDriverAgent and takes up to a minute, later sessions about
10 seconds.

Results go to `frontend/ios-smoke-results/`: a screenshot per failed test and
the Appium server log `appium.log`.

## Troubleshooting

- **Every command hangs for minutes after the session starts**: Safari kept a
  stale tab from an earlier session and it became the active webview. The
  config terminates Safari before each session; if it still happens, run
  `xcrun simctl terminate evaka-smoke com.apple.mobilesafari`.
- **`browser.execute(async ...)` times out immediately**: the script timeout
  is 0 by default in this stack. The config sets it in the `before` hook; keep
  it there.
- **`UnknownError` from `browser.execute`**: the script returned an object
  with a top-level `status` key, which Appium's Safari adapter reads as a
  legacy error code. Use another key name (the login helper uses
  `httpStatus`).
- **`html[data-standalone] did not appear`**: the app did not pick up the
  test config. Check that the frontend is the dev build and that
  `lib-common/utils/helpers.ts` still reads `evaka.testConfig`.
- **Session creation fails with a WebDriverAgent error**: rerun
  `yarn ios-smoke:setup`, which rebuilds WebDriverAgent. The build works on
  any simulator of the same runtime.
- **`fetch failed` in `upsertDummyIdpVtjDataset`**: the dummy IdP port is
  wrong, see `EVAKA_IDP_PORT` above.
- **`TypeError: Load failed` from the login fetch**: the fetch ran in a page
  that was being replaced. Navigate with `support/navigate.ts`, not
  `browser.url()` directly.
- **`is not painted on screen`**: the failure message gives the painted box
  size next to the element size. A box with the full width but a smaller
  height means the element is clipped at the top or bottom. The check retries
  for 5 seconds (`paintTimeoutMs`) before failing, so slow painting is not
  the cause.
- **System notifications cover the page** (e.g. "Ready for Apple
  Intelligence"): they can break the paint check near the top of the screen.
  Rerun, or dismiss them once in Simulator.app.
- **Web clip icon not found or no webview appears**: open Simulator.app and
  check that the eVaka icon is on the home screen. The seed restarts
  SpringBoard and waits 5 seconds; on a slow machine that may not be enough.
  The webview belongs to `com.apple.SafariViewService` and its context id
  changes on every launch, so it is always picked by URL.
- **Web clip shows a blank white page right after the tap**: the dev bundle is
  still loading; the spec waits for the app to render before logging in.
- **Start from scratch**: `xcrun simctl delete evaka-smoke`; the next run
  creates it again.
