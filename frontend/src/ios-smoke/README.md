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
- `specs/app-shell-layout.spec.ts`: the installed app layout from PR #9798
  that keeps the header and the bottom navigation in place when iOS leaves
  the visual viewport stale (after the software keyboard closes or the device
  rotates, WebKit bugs 254861 and 297779). Here the bug is not reproduced
  (stage 2 does that, see `stale-viewport.spec.ts` below);
  `expectAppShellLayout()` in `support/app-shell.ts` checks the layout that
  avoids it, on the calendar and on the messages page:
  - `html[data-standalone]` is set, the document does not scroll
    (`scrollY` 0, `scrollHeight` within the viewport) and `html`, `body` and
    `#app` have `overflow: hidden`
  - `[data-qa="app-shell"]` is `position: relative`, `overflow: hidden` and
    exactly fills the viewport
  - `[data-qa="scroll-area"]` is inside the shell and scrolls
    (`overflow-y: scroll` or `auto`; on the calendar the content overflows it)
  - nothing inside the shell is `position: fixed` (except the development
    environment label, focus lock guards and the floating "new reservation"
    and "new message" buttons, which are known gaps listed in
    `knownFixedButtons`), and every `position: sticky` element has its scroll
    container inside the shell
  - `[data-qa="mobile-nav"]` is `position: static`, the last in-flow child of
    the shell and ends at its bottom; `[data-qa="header"]` starts at 0
  - scrolling the scroll area by 400 px moves the content but not the
    document, and the header and the bottom navigation stay where they are
    painted on screen (`paintedBox()`). Skipped with a warning when the page
    is too short (the messages page with one thread)

  The spec also checks that the mobile menu is `position: absolute` inside
  the shell and that the calendar actions modal is rendered in
  `#modal-container`, outside the shell.

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
  standalone and fills the screen (no Safari style toolbar), the calendar
  passes `expectAppShellLayout()`, and the reservation and absence modal
  buttons are reachable
- `specs/webclip/stale-viewport.spec.ts`: drives the simulator into the
  WebKit stale visual viewport state and checks that the header, the bottom
  navigation and the floating calendar button stay where they are laid out
  and painted, and that the document does not scroll (see below)

Where an element really is on screen comes from the screenshot, not from the
page's own geometry: `paintedBox()` in `support/reachable.ts` colours the
element magenta like the paint check and returns the bounding box of the
magenta pixels in screen points. In the clip the page starts below the 62
point status bar and `innerHeight` is 812 of the 874 point screen, so screen
y = page y + 62 in portrait. iOS 26 tints the status bar with the colour at
the page edge, so a magenta header or navigation turns the status bar magenta
too; pass the status bar height to `paintedBox()` as `ignoreAbove`.

#### Stale visual viewport

`support/stale-viewport.ts` replays a manually recorded sequence with native
touches (`mobile: tap`, W3C touch actions) and the recorded timings:
messages tab, open the thread, Vastaa (the reply editor focuses its text
field and the keyboard opens), rotate to landscape, tap the keyboard's J key
at (488,279) (`mobile: keys` as a fallback), a 60 ms flick up and a 1.6 s
drag down, bring Lähetä to y 60..150, tap it (the keyboard closes), wait
4.7 s, rotate to portrait, wait 2.6 s, open the calendar. **It never
triggers on the first loop after the clip is launched, but does on the
second loop in the same clip process**, so the trigger runs up to three
loops (with a native calendar scroll between them) and stops at the first
stale measurement: portrait, no focused text field and
`visualViewport.height` differs from `innerHeight` (or `offsetTop` > 0).
Taps go to the element's centre computed from the page (screen y = page y -
`visualViewport.pageTop` + 62 in portrait, + 0 in landscape) and fall back to
the recorded point. `support/orientation.ts` rotates with `setOrientation`,
falling back to WebDriverAgent's `POST /session/:id/rotation`.

The spec creates a thread through the UI first, then compares the stale
state (right after rotating back, and again after scrolling the calendar)
with the clean calendar before the loops. If the visual viewport does not
go stale in three loops the test fails with "The WebKit precondition was not
reached", so a broken trigger never passes silently.

Results on 2026-10-08 (iPhone 17 simulator, iOS 26.4): the stale state was
reached on loop 2 in every run (visualViewport.height 336 vs innerHeight
812). On the current app shell layout the header stays at 0..60, the
navigation at 746..812 and the floating button at 685..730, painted at the
same screen positions, and `scrollY` stays 0. On the pre-#9798 layout the
same state has `offsetTop` 476, the header at -476 and the fixed navigation
at 276..336 (painted at screen y 338..398), and the spec fails.

The push notification suggestion banner (`push-suggestion`) takes about 290
points above the scroll area and leaves no room for the reply editor in
landscape, so the spec sets the dismissal cookie from `pwa/dismissal.ts`
before logging in.

Other notes for driving the keyboard: a `nativeWebTap` click opens the
software keyboard, a plain `click()` only focuses the field; `mobile:
isKeyboardShown` needs `defaultActiveApplication:
'com.apple.SafariViewService'` in the clip.

The second capability in `wdio.conf.ts` has no `browserName`, so the session
starts on the home screen. Before the session, `support/webclip.ts` writes an
`eVaka` clip (URL `<frontend>/`) into the simulator's
`data/Library/WebClips`, replacing earlier eVaka clips, and reboots the
simulator so that SpringBoard reads it. Restarting only SpringBoard
(`launchctl stop com.apple.SpringBoard`) also shows the icon, but leaves the
simulator unable to rotate (`Unable To Rotate Device`) until a reboot. The spec presses home, swipes to the page with the icon, taps it
and switches to the webview whose URL is under the frontend URL. The clip has
its own cookies and storage, so it logs in separately from Safari.

The clip starts at `/` on purpose. The citizen app has no web app manifest,
and iOS scopes such a clip to its start URL: a clip starting at `/calendar`
shows `/messages` with a Safari style toolbar (`innerHeight` 676 instead of
812).

The clip's `WebClipStatusBarStyle` is `UIWebClipStatusBarStyleDefault`, as in
a clip that iOS 26 Safari creates. With
`UIWebClipStatusBarStyleLegacyBlackTranslucent` the page is drawn from the
top of the screen while `innerHeight` still leaves out the status bar, so the
app ends 62 points above the screen bottom.

### Verifying that the app shell spec guards the layout

Break the layout in the running dev frontend (Vite reloads it within a few
seconds), run the spec and restore the file with `git checkout -- <file>`:

```sh
EVAKA_FRONTEND_PORT=9101 EVAKA_IDP_PORT=9092 \
  yarn ios-smoke --spec src/ios-smoke/specs/app-shell-layout.spec.ts
```

Results on 2026-10-07 (iOS 26.4 simulator, stage 1):

| Mutation                                                                                       | Caught by                                                                                                          |
| ---------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `MobileNav.tsx`: remove `position: static;` from `BottomBar`'s `html[data-standalone] &` block | no `position: fixed` inside the shell (`nav in [data-qa="mobile-nav"]`)                                            |
| `index.css`: remove `overflow: hidden;` from the `html[data-standalone]` rule                  | `html has overflow-x/y "hidden scroll"`                                                                            |
| `App.tsx`: remove `overflow-y: scroll;` from `ScrollArea`'s standalone block                   | not caught, and correctly so: `overflow-x: hidden` makes `overflow-y` compute to `auto`, so the area still scrolls |
| `App.tsx`: remove both `overflow-x: hidden;` and `overflow-y: scroll;` from `ScrollArea`       | `scroll-area has overflow-y visible`                                                                               |

The calendar actions modal test passes under every mutation, because these
mutations do not move the modal portal.

### Verifying a test against an old frontend

The commit before a fix can be served from a separate worktree with its own
vite dev server (`EVAKA_FRONTEND_PORT=<port> EVAKA_APIGW_PORT=<apigw>
EVAKA_IDP_PORT=<idp> npx vite --host` in that worktree's `frontend/`), and
the suite pointed at it with `EVAKA_FRONTEND_PORT`. The API gateway is
shared, so login and fixtures work unchanged. Note that a frontend older than
the `evaka.testConfig` switch cannot run stage 1, only stage 2.

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

Each stage runs in its own Appium session, one after the other, and each
stage 2 spec file in a session of its own after a simulator reboot. Stage 2
adds about three minutes, most of it the stale viewport loops.

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
  check that the eVaka icon is on the home screen. The seed reboots the
  simulator and waits for `simctl bootstatus`.
  The webview belongs to `com.apple.SafariViewService` and its context id
  changes on every launch, so it is always picked by URL.
- **Web clip shows a blank white page right after the tap**: the dev bundle is
  still loading; the spec waits for the app to render before logging in.
- **The simulator does not rotate** (Device > Rotate in Simulator.app does
  nothing, WebDriverAgent says `Unable To Rotate Device`): seen on iOS 26.4
  after SpringBoard had been restarted. The web clip seed reboots the
  simulator for this reason; after a manual SpringBoard restart reboot it
  yourself (`xcrun simctl shutdown` and `boot`).
- **"The WebKit precondition was not reached"**: the stale viewport trigger
  ran three loops without the visual viewport going stale. Look at the loop
  measurements in the output and the failure screenshot: a banner or dialog
  that moves the reply editor, a different keyboard layout (the J key is
  tapped by coordinates) or a frontend with a different status bar inset.
- **`simctl launch`/`terminate`/`listapps` hang and WebDriverAgent never
  starts**: CoreSimulator stopped serving the device. Seen after
  Simulator.app lost its windows; restart Simulator.app (this also affects
  other booted simulators) and reboot the device.
- **Start from scratch**: `xcrun simctl delete evaka-smoke`; the next run
  creates it again.
