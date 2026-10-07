<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# iOS Safari smoke tests for the citizen app: design and implementation plan

Status: **proof of concept implemented and working locally** on branch
`ios-smoke-tests-design` (`frontend/src/ios-smoke/`, run with
`yarn ios-smoke`; see its README). Both stages pass on master and both fail
with the #9840 bug reintroduced. Written 2026-10-07, updated the same day
after the research round and again after the implementation. The research
findings and sources are in
[ios-safari-smoke-tests-research.md](ios-safari-smoke-tests-research.md).

## Why

On 2026-10-05 PR #9840 (modal accessibility improvements) moved the citizen
modals out of the `#modal-container` portal and rendered them inline inside the
app shell's scrolling area. On iOS, in the installed (home screen) app, the
`position: fixed` modal then got clipped to the scroll area: the modal's footer
buttons (Send / Cancel, Edit / New absence) ended up under the bottom
navigation and could not be tapped. PR #9975 hotfixed it on 2026-10-07 by
portaling the modals again
(`frontend/src/lib-components/molecules/modals/ModalBackground.tsx`).

The Playwright e2e suite cannot catch this:

- It runs Chromium only (`frontend/src/e2e-test/playwright.config.ts`).
- A draft spec that emulates the installed app (`navigator.standalone`
  override) and asserts that the modal footer buttons are inside the viewport
  and are the topmost element at their centre point **passes in Chromium even
  with the #9840 bug reintroduced**. Chromium does not clip a fixed element
  inside an overflow container the way iOS WebKit does. The draft is on this
  branch at
  `frontend/src/e2e-test/specs/0_citizen/citizen-installed-app-modals.spec.ts`
  as the reference for the assertions (it passes in Chromium, so it has no
  regression value there).
- Playwright's bundled WebKit is desktop WebKit, not iOS WebKit, and it crashes
  on the citizen dev bundle anyway. Playwright cannot attach to iOS Safari or
  the simulator at all (open issue since 2020).

iOS layout regressions can only be caught by running the real iOS WebKit.

## Goals

1. A small **smoke test suite** that developers run from a macOS dev machine
   against the local dev environment, in **real iOS Safari in the iOS
   Simulator**.
2. The same stack must be able to drive the **installed home screen web app**
   (web clip, `navigator.standalone === true`), because the app shell layout
   (`html[data-standalone]`, `frontend/src/citizen-frontend/App.tsx`) only
   exists there.
3. Later: a separate GitHub Actions job.

Non-goals: porting the Playwright suite, physical devices in CI, visual
regression testing, any commercial device cloud.

## Decisions

| Decision | Choice | Why |
|---|---|---|
| Driver | **Appium 3.8.0 + appium-xcuitest-driver 12.15.1** | Only stack that covers both goals. iOS 26.4 needs driver >= 10.23.2, which needs Appium 3 (not 2). Verified end to end locally against the citizen frontend. |
| Client / runner | **WebdriverIO 10.0.0** testrunner, Mocha specs, `@wdio/appium-service` | TypeScript, `browser.execute` for DOM assertions, `switchContext` for native/webview. Appium is started by the service, no second terminal. |
| Appium install location | project-local `APPIUM_HOME=frontend/.appium` (gitignored), driver installed by a `yarn ios-smoke:setup` script | Nothing global; versions pinned. |
| Login | **In-page `fetch` POST to `/api/dev-api/auth/citizen-sfi-login`** from `browser.execute`, then `browser.url('/calendar')` | Verified: returns 200 and the session cookie sticks. No apigw change needed. Works inside the web clip too (own cookie jar, same trick). WebDriver `setCookie` cannot set HttpOnly cookies, so it is not an option. |
| Stage 1 standalone switch | app reads a **`localStorage` test config** (`evaka.testConfig`) into `window.evaka` at startup; `forceStandalone: true` makes `isRunningInstalled()` return true | WebDriver has no init script. Storage written before the next navigation is the only pre-load hook. Keeps the existing `window.evaka` plumbing. |
| Stage 2 web clip install | **Seed a `.webclip` folder** into the simulator's data directory and restart SpringBoard, not the share sheet | Verified on iOS 26.4 simulator: the icon appears, tapping it opens the page full screen with `navigator.standalone === true`. Share sheet automation needs coordinate taps and is flaky. |
| Simulator | dedicated `evaka-smoke` simulator (iPhone 17, newest iOS runtime), booted headless with `simctl boot` + `bootstatus -b`, never Simulator.app | ~7 s boot locally. Does not interfere with the developer's own simulators. |
| Mocked time | not used in the first iteration; fixtures are relative to today | Keeps the test config minimal. `mockedTime` can be added to the same `localStorage` config later. |
| Alternatives rejected | safaridriver (Safari only, cannot reach the web clip), pymobiledevice3 (real devices only), Maestro (no DOM access, web mode is Chromium only), Playwright (cannot attach to iOS), `.mobileconfig` web clips (need MDM, `simctl` cannot install profiles) | Details in the research doc. |

## Verified facts (local Mac, 2026-10-07)

Environment: Xcode 26.4.1, iOS 26.4 runtime (23E254a), Node 24.11.1, dev stack
instance 2 (frontend `http://localhost:9101`).

- `appium driver run xcuitest build-wda` built WebDriverAgent in 37 s. First
  session 53 s (WDA install + launch), later sessions 8 to 12 s.
- Safari session with `browserName: Safari` starts in the webview context.
  `getContexts()` returned `['NATIVE_APP', 'WEBVIEW_<pid>.1']`;
  `appium:fullContextList: true` adds `{title, url, bundleId:
  com.apple.mobilesafari}`.
- Page facts in Safari: `navigator.standalone === false`, viewport 402x714
  at DPR 3, UA contains `Version/26.4 Mobile/15E148 Safari/604.1` (the
  `iPhone OS 18_7` part is frozen by Apple, ignore it).
- In-page login fetch returned 200; `browser.url('/calendar')` then showed
  the logged-in shell (the calendar itself stayed empty because the probe SSN
  had no fixtures, which is expected).
- `switchContext('NATIVE_APP')` and back works; the session survives
  `browser.url()` navigation.
- Web clip (library-level test with `appium-remote-debugger` 17.5.8 on a
  throwaway simulator): a hand-written
  `data/Library/WebClips/<UUID>.webclip/Info.plist` with `FullScreen=true`
  plus `simctl spawn <udid> launchctl stop com.apple.SpringBoard` produced a
  working home screen icon. The page was listed by the remote debugger with
  `bundleId: com.apple.SafariViewService` (host app `com.apple.webapp`), and
  `execute()` returned `navigator.standalone === true`. `matchMedia('(display-
  mode: standalone)')` was **false** because that comes from the
  `ApplicationManifest` file Safari writes only when installing through the
  share sheet. The app only needs `navigator.standalone`, so this is fine.
- After the clip has been opened once by an icon tap, `simctl terminate` /
  `simctl launch <udid> com.apple.webapp` relaunches it. A cold
  `simctl launch` with no saved scene shows a blank page. `simctl openurl
  webapp:<UUID>` does not work.

### Pitfalls found by the probe (must be handled in the code)

1. `browser.execute(async () => ...)` goes to `/execute/async` and times out
   at 0 ms unless `browser.setTimeout({ script: 30000 })` is called first.
   WebdriverIO 9+ has no `executeAsync`.
2. **Never return an object with a top-level `status` key from `execute()`.**
   Appium's Safari adapter reads it as a legacy error code and throws
   `UnknownError`. Use `httpStatus`.
3. Safari keeps tabs between sessions. A stale `about:blank` tab became the
   active webview and every command hung for 120 s. Run
   `xcrun simctl terminate <udid> com.apple.mobilesafari` (and
   `com.apple.webapp`) before each session.
4. On simulators the driver filters webviews by the session's current URL
   (`listWebFrames` in `lib/commands/context.ts`). When switching to the
   web clip, use `appium:fullContextList` and pick the context by URL prefix,
   with `appium:additionalWebviewBundleIds: ['*']` as a safety net.
5. `build-wda --name 'iPhone 17'` matches by prefix (it built against
   "iPhone 17 Pro"); the build is reusable on any simulator, so it does not
   matter, but pass the UDID where possible.

## Architecture

```
developer Mac (later: macOS runner)
├── eVaka dev stack (mise start; frontend :9099 or instance port e.g. :9101)
├── Appium 3 (:4723, started by @wdio/appium-service, APPIUM_HOME=frontend/.appium)
│     └── WebDriverAgent prebuilt into the simulator
├── iOS Simulator "evaka-smoke" (headless, iPhone 17, newest runtime)
│     ├── Mobile Safari            → stage 1 specs (forceStandalone)
│     └── eVaka web clip           → stage 2 specs (real navigator.standalone)
└── WebdriverIO runner (frontend/src/ios-smoke/)
      ├── fixtures via dev API (axios clients reused from e2e-test/)
      └── specs: calendar modals, bottom nav, message editor, web clip
```

The simulator shares the Mac's network, so `http://localhost:<port>` works
from inside it without a tunnel.

## Implementation (as built; see frontend/src/ios-smoke/README.md)

### 1. Dependencies and scripts (`frontend/package.json`)

```
yarn add -D appium@3.8.0 webdriverio@10.0.0 @wdio/cli@10.0.0 \
  @wdio/local-runner@10.0.0 @wdio/mocha-framework@10.0.0 \
  @wdio/spec-reporter@10.0.0 @wdio/appium-service@10.0.0 @wdio/globals@10.0.0
```

(This was run once during the research session and worked with Yarn 4.18;
it was reverted to keep the tree clean.) The xcuitest driver is **not** an npm
dependency; it is installed into the project-local Appium home.

Scripts:

```
"ios-smoke:setup": "APPIUM_HOME=.appium appium driver install xcuitest@12.15.1 && APPIUM_HOME=.appium appium driver run xcuitest build-wda",
"ios-smoke": "APPIUM_HOME=.appium wdio run src/ios-smoke/wdio.conf.ts"
```

Add `.appium/` and `ios-smoke-results/` to `frontend/.gitignore`. Add
`src/ios-smoke` to `frontend/tsconfig.json` references (composite project
like `e2e-test`, `types: ["node", "@wdio/globals/types", "@wdio/mocha-framework"]`).
`lib-common` resolves through the `link:` dependency in `node_modules`, so no
path mapping tricks are needed for `tsx`.

### 2. App change: test config from `localStorage`

`frontend/src/lib-common/utils/helpers.ts`: when `window.evaka` is not set
(no Playwright init script), read `localStorage.getItem('evaka.testConfig')`
(JSON) and use it as `window.evaka`. Add `forceStandalone?: boolean` to
`EvakaWindowConfig`. Wrap the storage read in try/catch.

`frontend/src/citizen-frontend/pwa/installed.ts`: `isRunningInstalled()`
returns true also when `isAutomatedTest && window.evaka?.forceStandalone`.

The test sets `localStorage['evaka.testConfig'] =
'{"automatedTest":true,"forceStandalone":true}'` via `browser.execute` on a
static page of the same origin and then navigates to the app. `automatedTest: true` also gives the usual test behaviour (no scroll
animations, short async-button timeouts, no double-click guard).

### 3. Folder layout

```
frontend/src/ios-smoke/
├── wdio.conf.ts          # capabilities, Appium service, simulator boot in onPrepare,
│                         # terminate Safari/web clip in beforeSession, script timeout in before,
│                         # screenshot on failure in afterTest
├── tsconfig.json
├── README.md             # setup steps (below) and troubleshooting (pitfalls above)
├── support/
│   ├── simulator.ts      # find/create "evaka-smoke", boot, bootstatus, data dir lookup
│   ├── login.ts          # in-page fetch to dev-api citizen-sfi-login, then navigate
│   ├── test-config.ts    # write evaka.testConfig to localStorage (read on next page load)
│   ├── reachable.ts      # expectReachable(): rect fully in viewport + elementFromPoint hits self
│   ├── fixtures.ts       # thin wrappers over e2e-test/dev-api/fixtures.ts (family, placement, messaging)
│   └── webclip.ts        # seed .webclip into <sim>/data/Library/WebClips, restart SpringBoard,
│                         # tap icon in NATIVE_APP, switch to the context whose url starts with baseUrl
└── specs/
    ├── calendar-modals.spec.ts   # stage 1
    ├── navigation.spec.ts        # stage 1
    ├── messages.spec.ts          # stage 1
    └── webclip.spec.ts           # stage 2, skipped unless IOS_SMOKE_WEBCLIP=1 until proven stable
```

Base URL comes from `frontend/src/e2e-test/config.ts` (`EVAKA_FRONTEND_PORT`
or `BASE_URL`), same as Playwright. Fixtures and `resetServiceState()` are
imported from `../e2e-test/dev-api/fixtures` and
`../e2e-test/generated/api-clients` (plain axios, no Playwright dependency).
Page objects from `e2e-test/pages` cannot be reused (Playwright locators).

### 4. Capabilities

```ts
{
  platformName: 'iOS',
  'appium:automationName': 'XCUITest',
  browserName: 'Safari',
  'appium:udid': udid,                      // resolved in onPrepare
  'appium:safariInitialUrl': baseUrl,
  'appium:webviewConnectTimeout': 20000,
  'appium:newCommandTimeout': 120,
  'appium:wdaLaunchTimeout': 240000,
  'appium:fullContextList': true,
  'appium:additionalWebviewBundleIds': ['com.apple.webapp', '*'],  // stage 2
  'appium:simulatorStartupTimeout': 180000
}
```

Hooks: `beforeSession` runs `xcrun simctl terminate <udid>
com.apple.mobilesafari` and `... com.apple.webapp` (ignore errors); `before`
runs `browser.setTimeout({ script: 30000 })`.

### 5. Specs (port of the draft Playwright spec plus the doc's scope)

Common `beforeEach`: `resetServiceState()`, `testCareArea`, `testDaycare`,
`Fixture.family({ guardian: testAdult, children: [testChild] })`,
`Fixture.placement({ childId: testChild.id, unitId: testDaycare.id, startDate:
today, endDate: today.addYears(1) })`, open `/offline.html`, set the test config,
log in, go to `/calendar`, assert `html[data-standalone]` is present,
wait for `[data-qa="calendar-page"][data-isloading="false"]`.

`expectReachable(selector)` runs in `browser.execute`: element exists, its
`getBoundingClientRect()` is fully inside `innerWidth`/`innerHeight`, and
`document.elementFromPoint(centre)` is the element or a descendant (return
`{ coveredBy: tagName }` on failure, never a `status` key).

- `calendar-modals.spec.ts`
  - reservation modal: tap `open-calendar-actions-modal`, then
    `calendar-action-reservations`; `modal-cancelBtn` and `modal-okBtn`
    reachable.
  - absence modal: `open-calendar-actions-modal`, `calendar-action-absences`;
    same buttons.
  - day view: tap `mobile-calendar-day-<iso date>` for today + 7 days;
    `create-absence` reachable in `calendar-dayview`.
- `navigation.spec.ts`: `nav-calendar-mobile`, `nav-messages-mobile`,
  `nav-children-mobile`, `sub-nav-menu-mobile` visible and reachable; tapping
  `nav-messages-mobile` navigates to `/messages`.
- `messages.spec.ts`: extra fixtures as in
  `e2e-test/specs/7_messaging/messaging-by-staff.spec.ts` lines 78-151
  (`createDaycareGroups`, `Fixture.employee().groupAcl(...)`,
  `Fixture.groupPlacement`, `createMessageAccounts()`); tap
  `new-message-btn-mobile`, assert `message-editor` opens and
  `send-message-btn` is reachable.
- `webclip.spec.ts` (stage 2): seed the web clip with `Title: eVaka`, `URL:
  <baseUrl>/calendar`; restart SpringBoard; `switchContext('NATIVE_APP')`,
  `$('~eVaka').click()` on the home screen (swipe pages if needed);
  `getContexts()` until one has `url` starting with `baseUrl`;
  `switchContext(id)`; log in with the in-page fetch (clip storage is
  separate from Safari); assert `navigator.standalone === true` and
  `html[data-standalone]` set **by the app**; then run the reservation modal
  check. First open must be an icon tap; later relaunches may use
  `simctl terminate/launch com.apple.webapp`.

Keep the suite to a couple of minutes. Business logic stays in Playwright.

### 6. Web clip seed (`support/webclip.ts`)

Data dir: `~/Library/Developer/CoreSimulator/Devices/<udid>/data/Library/WebClips/<UUID>.webclip/`
where `<UUID>` is 32 uppercase hex characters. Write `Info.plist` (XML plist
is accepted) with these keys taken from a real iOS 26.4 clip:

```
ApplicationBundleVersion 1, ClassicMode false, ConfigurationIsManaged false,
ContentMode UIWebClipContentModeRecommended, Eligibility 0, FullScreen true,
IconIsPrecomposed false, IconIsScreenShotBased false, IgnoreManifestScope false,
IsAppClip false, Orientations 0,
PlaceholderBundleIdentifier com.apple.WebKit.PushBundle.<UUID>,
ScenelessBackgroundLaunch false, Title eVaka,
TrustedClientBundleIdentifiers [com.apple.mobilesafari],
URL http://localhost:<port>/calendar,
WebClipStatusBarStyle UIWebClipStatusBarStyleLegacyBlackTranslucent
```

Copy `frontend/public/icons/evaka-180px.png` as `icon.png`. Then
`xcrun simctl spawn <udid> launchctl stop com.apple.SpringBoard` and wait a
few seconds. Optional but cheap, persists across reboots, may only matter for
service worker targets:

```
xcrun simctl spawn <udid> defaults write com.apple.WebInspector EnableRemoteInspection -bool YES
xcrun simctl spawn <udid> defaults write com.apple.WebInspector RemoteInspectorEnabled -bool YES
xcrun simctl spawn <udid> defaults write com.apple.webinspectord RemoteInspectorEnabled -int 1
xcrun simctl spawn <udid> defaults write com.apple.webapp WebKitDeveloperExtrasEnabled -bool YES
```

Idempotency: remove any existing `*.webclip` whose `Title` is `eVaka` before
seeding, or `xcrun simctl erase` the dedicated simulator.

### 7. Verification steps for the PoC

1. `yarn ios-smoke` passes on master.
2. Reintroduce the bug: `git show 65af22012e:frontend/src/lib-components/molecules/modals/ModalBackground.tsx > frontend/src/lib-components/molecules/modals/ModalBackground.tsx`,
   wait for Vite HMR, run again: the modal specs **must fail** in stage 1. If
   they only fail in stage 2, stage 2 becomes the priority. Restore with
   `git checkout -- frontend/src/lib-components/molecules/modals/ModalBackground.tsx`.
3. `yarn lint` and `yarn type-check` pass with the new folder included.

### 8. Local developer setup (goes to the README)

1. Xcode with an iOS runtime: `sudo xcodebuild -runFirstLaunch && xcodebuild -downloadPlatform iOS`.
2. `yarn install && yarn ios-smoke:setup` (installs the driver into
   `frontend/.appium`, prebuilds WebDriverAgent, about 1 minute).
3. `mise start`; if an instance is active, `EVAKA_FRONTEND_PORT` is read by
   `e2e-test/config.ts`. `TUNNEL_URL` must be unset (`mise tunnel stop`).
4. `yarn ios-smoke`. The config creates and boots the `evaka-smoke`
   simulator on first run. `IOS_SMOKE_WEBCLIP=1 yarn ios-smoke` includes stage 2.
5. Screenshots of failures land in `frontend/ios-smoke-results/`, the Appium
   log in `frontend/ios-smoke-results/appium.log`.

## GitHub Actions (after the PoC)

Separate workflow `.github/workflows/ios-smoke.yml`, `workflow_dispatch` +
nightly, not required for merging. Never `pull_request` from forks on a
self-hosted runner.

The deciding constraint: **GitHub-hosted arm64 macOS runners cannot run
Docker** (no nested virtualization), and the e2e backend is Docker Compose
(test-db PG18, valkey, s3mock, sftp, service, apigw, frontend). Options:

1. **Self-hosted Apple Silicon Mac** running the existing `compose/test-e2e`
   stack under colima. Simplest, fastest (warm simulator and WDA persist).
   Recommended first.
2. GitHub-hosted `macos-26`: run the backend natively (Homebrew
   `postgresql@18` and `valkey` have arm64 bottles, s3mock as a jar, service
   jar from the build job, apigw and frontend with node). Needs the test-db
   init scripts reproduced. No third-party tunnel: cloudflared quick tunnels
   are a Cloudflare service and were ruled out.
3. Intel `macos-26-intel` with colima (`douglascamata/setup-docker-macos-action`):
   weak fallback, slow simulator, no x86 PG18 bottles.

Recipe pieces (copy from `appium/appium-xcuitest-driver`
`.github/workflows/functional-test.yml`): `maxim-lobanov/setup-xcode` 26.4,
`simctl boot` + `bootstatus -b` + a CPU-idle wait (the `futureware-tech/
simulator-action` route took 7 to 13 minutes in Appium's own CI),
`appium driver run xcuitest download-wda --kind=sim` with
`appium:usePreinstalledWDA` + `appium:prebuiltWDAPath` instead of building,
`xcrun simctl io <udid> recordVideo --codec h264` started in the background
and stopped with `kill -INT` in an `always()` step, upload `appium.log`,
screenshots and video. Budget 45 to 60 minutes per run, mostly simulator
boot and settling. Appium's own `e2e (web, 26.4)` job on macos-26 was red
on 2026-10-07 while 26.5 and 27.0 passed, so pin the runtime deliberately
and expect some flakiness.

## Findings from the implementation (2026-10-07)

- **Geometry and hit testing are not enough.** With the #9840 bug the iOS
  26.4 simulator reproduces the clipping, but `getBoundingClientRect()`
  reports the button inside the viewport, `elementFromPoint()` returns the
  button and even a native tap reaches it. Only the painting is wrong. The
  suite therefore adds a **paint check**: the element is coloured magenta,
  a lossless WebDriverAgent screenshot (`appium:settings[screenshotQuality]:
  0`) is decoded, and the bounding box of magenta pixels must cover 95% of
  the element. With the bug the modal footer buttons show as 177x11 of
  177x45 points.
- **Stage 1 (Safari + forced app shell) is enough to catch #9840.** Stage 2
  fails the same way, so it is a confirmation, not a requirement.
- **Stage 2 works through the full driver session.** A session without
  `browserName` starts on the home screen, taps the seeded `eVaka` icon
  (home screen page 2, so swipe once), and `getContexts()` lists the clip
  page within a couple of seconds with `bundleId:
  com.apple.SafariViewService`. The id changes on every relaunch, so the
  context is picked by URL prefix. The context survives `location.assign`,
  `browser.url`, `simctl terminate` + `simctl launch com.apple.webapp` and
  WDA `mobile: launchApp` / `activateApp`.
- Never use `mobile: getContexts` with `waitForWebviewMs`: with no webview it
  busy-loops and wrote about a million log lines in 20 s.
- The first screenshot after the icon tap can be blank white while the dev
  bundle loads; wait for a DOM selector.
- Login must happen on a static page (`/offline.html`): logging in while the
  app is running races the app's own auth check. `browser.url()` can return
  before the old document is replaced, so `support/navigate.ts` waits for a
  marker to disappear and the URL to match.
- The dev instance needs `EVAKA_IDP_PORT` as well as `EVAKA_FRONTEND_PORT`
  (fixtures write VTJ data to the dummy IdP).
- Many screenshots in a tight loop stalled WebDriverAgent for two minutes
  and the next navigation hung indefinitely. The paint check now waits
  between retries and a failed Safari test continues in a fresh session.
- `innerHeight` in the clip is 812 of the 874 point screen (status bar); a
  Safari style toolbar seen once in a logged-out run took about 200 points.
  The stage 2 precondition test allows a 70 point gap.
- Two agents sharing one WebDriverAgent port (8100) attached to each
  other's simulators. With more than one simulator, set a distinct
  `appium:wdaLocalPort` per simulator.

## Open questions

- Self-hosted Mac or GitHub-hosted runner? This decides whether a native
  backend recipe has to be built.
- Does iOS 26 copy Safari cookies into a new web clip at install time?
  Irrelevant while the clip logs in itself.
- The stage 2 failure screenshot shows an empty 62 point strip below the
  bottom navigation in the web clip; worth checking on a real device.

## Pointers

- Research findings, alternative tools, sources:
  [ios-safari-smoke-tests-research.md](ios-safari-smoke-tests-research.md).
- Slack thread about the production bug: Voltti Slack, channel `C8DEUN2DB`,
  message `p1791353360710449` (2026-10-07 09:09).
- Breaking PR #9840 (merge commit `65af22012e`), hotfix PR #9975.
- App shell layout: `frontend/src/citizen-frontend/App.tsx` (comment above
  `AppShell`), `frontend/src/citizen-frontend/index.css`,
  `frontend/src/citizen-frontend/pwa/installed.ts`.
- Existing test plumbing: `frontend/src/e2e-test/browser.ts` (`window.evaka`
  init script), `frontend/src/e2e-test/utils/pwa.ts`,
  `frontend/src/e2e-test/utils/user.ts` (`enduserLogin`),
  `frontend/src/e2e-test/config.ts`, `frontend/src/e2e-test/dev-api/fixtures.ts`.
- Draft assertions: `frontend/src/e2e-test/specs/0_citizen/citizen-installed-app-modals.spec.ts` (on this branch).
- Reference project for headless simulator control (not a building block):
  `/Users/wnt/Documents/iOS-pwa-runner`, see the research doc.
- The suite uses its own `evaka-smoke` simulator. The `iPhone 17`
  simulator (`00ABCA4C-…`) was used by the probes and may still hold an
  `eVaka` web clip; `xcrun simctl shutdown` or delete it if it is in the way.
