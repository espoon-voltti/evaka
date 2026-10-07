<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# iOS Safari smoke tests: research notes

Companion to [ios-safari-smoke-tests.md](ios-safari-smoke-tests.md). Collected
2026-10-07 from GitHub, project docs, blog posts and hands-on probes on a Mac
with Xcode 26.4.1 and the iOS 26.4 simulator runtime. Constraint for every
option: no third-party commercial services. Only open source, Apple tooling
and GitHub Actions.

## Summary

Nothing found gives both goals (real iOS WebKit in the simulator; driving the
installed web clip) in one tool other than Appium with the XCUITest driver,
so we are not reinventing a wheel. The closest prior art for installed-PWA
automation is a 2019 to 2021 example by a WebdriverIO maintainer for iOS 12
to 14; the technique (native taps to install and launch, then pick the
webview context by URL) still applies, but the install flow is flaky and
seeding the web clip on disk is a better fit for the simulator. Nobody has
published an iOS 26 web clip automation example; big PWA projects only mock
`display-mode` in unit tests.

## Tool evaluation

| Tool | Real iOS WebKit in simulator | Drives the web clip | Notes | Verdict |
|---|---|---|---|---|
| **Appium 3 + appium-xcuitest-driver** | Yes | Yes (native context for taps, webview context through the simulator's `webinspectord_sim` socket) | Verified locally end to end against the citizen frontend. iOS 26.4 needs driver >= 10.23.2, which needs Appium 3. Appium's own CI runs Safari tests on GitHub-hosted `macos-26`. | **Chosen** |
| Apple `safaridriver` (`platformName: iOS`, `safari:useSimulator`, `safari:deviceUDID`) | Yes, verified locally (session reported browserVersion 26.4, navigation and `execute` worked) | No. WebDriver on iOS cannot leave Safari. | Fewest moving parts for Safari-only tests. One session per Safari host. Needs Remote Automation enabled in the simulator (`defaults write com.apple.mobilesafari RemoteAutomationEnabled -bool YES`, `sudo safaridriver --enable`). A public repo runs it on `macos-26` with mostly green runs. Our local probe saw a click that did not fire and a hung screenshot, but Appium was using the same simulator at the time, so unconfirmed. | Fallback for stage 1 only |
| `appium-safari-driver` | Same as safaridriver | No | Appium front end to safaridriver. | Not needed |
| `pymobiledevice3` (`webinspector` automation and CDP bridge) | No, real devices only (lockdown / RemoteXPC) | Real devices only | Natural next step for USB iPhones later: `webinspector cdp` + Playwright `connectOverCDP`. | Out for simulators |
| Playwright | No. Its WebKit is desktop WebKit on the host. Issue #1122 open since 2020: no way to attach to MobileSafari. | No | `connectOverCDP` is Chromium only. | Out |
| Maestro (Apache-2.0, runs locally without their cloud) | Drives system UI via accessibility | Could tap through install and launch | No JavaScript in the page, no DOM access, web mode is Chromium only. | Out for assertions |
| facebook/idb | Native simulator driver only | No web access | Open iOS 26 bugs: wrong tap coordinates on 3x devices, accessibility tree broken, web content missing. | Out |
| `inspect-webkit` (MIT, 2026) / `opensafari` (MIT) / `ios-webkit-debug-proxy` | CDP bridges to the simulator's Web Inspector | Page access only, no native taps | Useful references for the inspector protocol; too young to depend on. | Reference only |
| `.mobileconfig` WebClip payload | n/a | Would install a clip without UI | `xcrun simctl` has no profile command; manual attempts get "profile must be installed by a Mobile Device Management server"; pymobiledevice3's profile service is real-device only. | Out |
| iOS PWA Runner (`/Users/wnt/Documents/iOS-pwa-runner`) | Yes | Yes, via its own inspector client | Streaming / remote access tool, coordinate taps, synchronous eval only. Reference for headless simulator control. | Reference only |

## Appium and the XCUITest driver

Versions on 2026-10-07: Appium 3.8.0 (latest), 4.0.0-beta.3; driver 12.15.1
(latest), 13.0.0-beta.1 for Appium 4; WebdriverIO 10.0.0; remote-debugger
17.4.x.

- Compatibility table (driver docs `getting-started/system-requirements.md`):
  iOS >= 26.4 needs driver >= 10.23.2 (WDA >= 11.1.5); drivers >= 10 need
  Appium 3; Xcode >= 16 fully supported from driver 11.1.2. The handover
  draft said "Appium 2/3"; Appium 2 is out.
- Driver 12.15.1 package.json: peer `appium ^3.0.0-rc.2`, Node `^20.19 ||
  ^22.12 || >=24`, depends on `appium-remote-debugger ^17.4.2` and
  `appium-webdriveragent ^16.12.11`. Scripts: `build-wda`, `download-wda`,
  `open-wda`.
- Safari session: `browserName: Safari` sets bundle `com.apple.mobilesafari`,
  starts WDA, calls `activateRecentWebview()` so the session begins in the
  web context, then navigates to `appium:safariInitialUrl`. `setUrl` goes
  through the remote debugger in a web context and through `simctl openurl`
  in the native context.
- Capabilities that matter: `appium:udid`, `appium:webviewConnectTimeout`
  (default 5000 ms, raise on CI), `appium:webviewConnectRetries` (20 at
  500 ms), `appium:additionalWebviewBundleIds` (`'*'` returns all),
  `appium:includeSafariInWebviews`, `appium:fullContextList` (adds title,
  url, bundleId), `appium:ignoredWebviewBundleIds`,
  `appium:showSafariConsoleLog`, `appium:simulatorStartupTimeout`,
  `appium:usePreinstalledWDA` + `appium:prebuiltWDAPath`.
- Remote debugger on simulators: unix socket
  `/private/tmp/com.apple.launchd.*/com.apple.webinspectord_sim.socket`
  (find with `lsof -aUc launchd_sim`). It always searches
  `com.apple.WebKit.WebContent`, `process-SafariViewService` and
  `com.apple.SafariViewService` in addition to the session bundle id, which
  is why the web clip page (reported under `com.apple.SafariViewService`)
  appeared without extra capabilities in the library-level test.
- `listWebFrames` (driver `lib/commands/context.ts`) filters pages by the
  session's current URL on simulators. `getRecentWebviewContextId` first
  matches the current URL exactly, then title/url regexes. For the web clip,
  list with `fullContextList` and choose by URL prefix.
- iOS 26 breakages and fixes: iOS 26.2 "'Runtime' domain was not found"
  (appium/appium#21705) fixed by remote-debugger 15.2.x, driver 10.5.0+;
  iOS 26.4 beta `driver.get()` broken, fixed in driver 10.23.2; real-device
  iOS 26.4.2 hang fixed in remote-debugger 15.10.3 (#498). Pin versions.
- WebDriverAgent: `appium driver run xcuitest build-wda` took 37 s locally.
  `download-wda --kind=sim` fetches a prebuilt simulator build (about 5.6 MB)
  from WebDriverAgent GitHub releases; Appium's CI uses it with
  `usePreinstalledWDA` + `prebuiltWDAPath` and never runs xcodebuild.
- Driver 12.9.0 added "native Safari automation support"
  (`mobile: startAutomationSession`, WebKit AutomationSession backend). Try
  it if atom-based clicks misbehave inside the web clip.
- Appium's `e2e (web, 26.4)` CI job on `macos-26` was failing on
  2026-10-07 (1 of 107 in one run, 14 in another) while 16.4 and 27.0
  passed. Expect some flakiness on 26.4.

### Local probe results (Appium 3.8.0, driver 12.15.1, WebdriverIO 10.0.0)

- Simulator `iPhone 17` booted headless in about 7 s (`simctl boot`,
  `bootstatus -b`; the odd `Status=4294967295` line is harmless).
- First session 52.9 s (WDA install and launch), later ones 8.4 to 12.2 s.
- Contexts `['NATIVE_APP', 'WEBVIEW_<pid>.1']`; with `fullContextList`
  `{ id: 'WEBVIEW_<pid>.1', title: 'Varhaiskasvatus', url:
  'http://localhost:9101/', bundleId: 'com.apple.mobilesafari' }`.
- In-page `fetch` POST to `/api/dev-api/auth/citizen-sfi-login` returned 200
  and the session cookie persisted across `browser.url()`.
- `switchContext('NATIVE_APP')` gave an XCUI tree rooted at
  `XCUIElementTypeApplication 'Safari'` (402x874 points); switching back kept
  the URL.
- Session discovery (`GET /appium/sessions`) is off by default; cleaning up
  leftover sessions that way needs `--allow-insecure session_discovery`.
- Pitfalls: `execute(async)` needs `setTimeout({ script })`; a returned
  object with a top-level `status` key becomes `UnknownError`; stale Safari
  tabs caused 120 s hangs until Safari was terminated before the session.

## Installed web clip on the simulator

Facts verified on a throwaway iOS 26.4 simulator (hand-written clip, pages
read with `appium-remote-debugger` 17.5.8 directly):

- Clip storage: `<sim>/data/Library/WebClips/<32 hex>.webclip/` with
  `Info.plist`, `icon.png`, `ApplicationManifest` (NSKeyedArchiver plist with
  the raw manifest JSON; only Safari writes it), `Cookies/`, `Storage/`.
  `Info.plist` keys are listed in the design doc.
- Writing a clip folder and running `simctl spawn <udid> launchctl stop
  com.apple.SpringBoard` makes the icon appear (SpringBoard adds it to
  `IconState.plist`, page 2). Tapping it opens the URL full screen.
- In that clip `navigator.standalone === true` but `matchMedia('(display-
  mode: standalone)').matches === false`. Copying an `ApplicationManifest`
  from another origin made `display-mode` true but put the page out of scope
  (Safari-style navigation UI appeared). A fixture with a real manifest must
  therefore be captured from the same origin the tests use. The citizen app
  accepts `navigator.standalone`, so the hand-written clip is enough for
  layout tests.
- The remote debugger listed apps `com.apple.webapp` ("Web") and
  `com.apple.SafariViewService` (proxy); the page came back with bundleId
  `com.apple.SafariViewService`. The `udid` and `platformVersion` options
  were required, otherwise connect timed out.
- Web clips run under host app `com.apple.webapp` (`Web.app` in the
  runtime). It has a private `webapp` URL scheme; `simctl openurl webapp:<UUID>`
  fails (LSApplicationWorkspace error 115). A cold `simctl launch
  com.apple.webapp` shows a blank page; after one icon tap, terminate +
  launch restores the clip and the page is inspectable again.
- Whether `mobile: launchApp` / `activateApp` can launch `com.apple.webapp`
  through WDA was not tested. `simctl launch` is enough for the PoC.
- The full XCUITest driver session (`getContexts` / `switchContext`) was
  **not** run against a web clip. That is the main open verification.
- Older prior art: wswebcreation/pwa-example (iOS 12 to 14) found the clip
  context under `com.apple.SafariViewService` too, picked it with
  `mobile: getContexts` by title, logged in through the UI or by writing
  non-HttpOnly cookies with JS, and reported flakiness from per-version
  install flows, stale webviews and animations. appium/appium#13380 (iOS
  12): PWA webview context disappeared after backgrounding; closed as
  enhancement.

### Share sheet install flow (optional, flaky)

iOS 26 path: More (⋯) > Share > View More > Add to Home Screen > Add. Every
item added from the share sheet becomes a web app by default ("Open as Web
App" toggle on). The iOS PWA Runner notes that share-sheet elements are not
reliably in the accessibility tree (rendered by RemoteViewService), so it
uses fixed normalized coordinates (View More 0.85,0.915; Add to Home Screen
0.45,0.731; Add 0.875,0.143), detects the install by a new
`com.apple.WebKit.PushBundle.*` entry in `simctl listapps`, and restarts
SpringBoard afterwards to fix a white-screen render bug. UIKit menus only
open from WebDriverAgent taps, not raw HID events. A first-run coaching
popover in Safari covers the More button and reappears per navigation;
dismiss it by label (Close / Continue / Done / OK).

Cookie copy from Safari into a new clip at install time is unverified (Apple
documents it for macOS Dock web apps only). Each clip has its own cookie and
storage partition, so the plan is to log in inside the clip.

## iOS PWA Runner notes (reference project)

- Simulator creation: newest runtime from `simctl list runtimes --json`,
  device type id from `simctl list devicetypes --json`, reuse by name or
  `simctl create`, `simctl boot || true`, `simctl bootstatus -b` with a
  timeout, poll device state as fallback. Runs with no Simulator.app.
- Cleanup: name-prefix and process-pattern based reaping of simulators, WDA
  runners (`devicekit-iosUITests-Runner`), `simctl spawn ... log stream`
  processes, with a protected-device list.
- Inspector protocol on iOS 26: page commands wrapped in
  `Target.sendMessageToTarget`, replies in `Target.dispatchMessageFromTarget`,
  service worker targets not wrapped; `Target.setPauseOnStart {false}` after
  connect. A clip can list stale page targets from a previous process; try
  the highest page id first. Only one debugger client can hold the session.
- Inspector defaults it writes (persist across reboots):
  `com.apple.WebInspector EnableRemoteInspection`, `com.apple.WebInspector
  RemoteInspectorEnabled`, `com.apple.webinspectord RemoteInspectorEnabled
  -int 1`, `com.apple.webapp WebKitDeveloperExtrasEnabled`. The throwaway
  experiment attached without them; they may only matter for service
  workers.
- idb video streaming is broken on iOS 26; screen capture comes from WDA
  (MJPEG) or `simctl io screenshot`. `simctl io recordVideo` works for CI
  artifacts.
- `pushManager.subscribe()` rejects with NotAllowedError on the simulator;
  not relevant to layout tests.

## GitHub Actions

- GitHub-hosted `macos-26` (arm64, image 20260907): Xcode 26.6 default,
  26.0.1 to 26.5 installed, iOS 26.2 / 26.4 / 26.5 runtimes. Intel images
  also exist (`macos-26-intel`, `macos-15-intel`).
- **No Docker on arm64 macOS runners** (nested virtualization unsupported).
  `douglascamata/setup-docker-macos-action` (Colima) supports Intel images
  only. The eVaka e2e backend is entirely Docker Compose (test-db PG18,
  valkey 9.2, s3mock, sftp, service, apigw, frontend-common images).
- Native backend on arm64 is possible in principle: Homebrew `postgresql@18`
  and `valkey` have arm64 bottles, s3mock runs as a jar, service jar can come
  from the build job, apigw and frontend run with node. Needs the test-db
  init (extensions, users) reproduced as a script.
- Cloudflared quick tunnels (as in `mise tunnel`) would make a two-job setup
  possible but are a Cloudflare service with no uptime guarantee and request
  caps; ruled out under the no-third-party-services constraint.
- Simulator boot on hosted runners: 7 to 13 minutes through
  `futureware-tech/simulator-action` in Appium's CI; CircleCI attributes
  post-boot CPU load to first-boot indexing and `diagnosticd`, fixes are a
  cached warm simulator data folder and `launchctl unload -w
  com.apple.diagnosticd`. Local boot is about 7 s.
- Video: `xcrun simctl io <udid> recordVideo --codec h264 --force out.mp4 &`,
  wait for "Recording started" on stderr, `kill -INT` in an `always()` step.
- Free and Team plans allow 5 concurrent macOS jobs; run nightly or on a
  path filter, not on every PR.
- GitHub advises against self-hosted runners for public repositories'
  pull requests from forks. Trusted triggers only (schedule, push to master,
  workflow_dispatch).
- A public example of `safaridriver` on `macos-26`:
  Azovgroup/sulamita-youth-ios `.github/workflows/safari-shot.yml`
  (boot, defaults write for Remote Automation, hide TipKit tips,
  `sudo safaridriver --enable`, session retries, screenshots via
  `simctl io screenshot`).

## Sources

- appium-xcuitest-driver: https://github.com/appium/appium-xcuitest-driver
  (`docs/getting-started/system-requirements.md`, `docs/reference/capabilities.md`,
  `lib/commands/context.ts`, `lib/driver.ts`, `.github/workflows/functional-test.yml`, `CHANGELOG.md`)
- Capabilities reference: https://appium.github.io/appium-xcuitest-driver/latest/reference/capabilities/
- appium-remote-debugger: https://github.com/appium/appium-remote-debugger
  (`CHANGELOG.md`, `development-notes.md`, `lib/mixins/connect.ts`, PR #498, issue #497)
- iOS 26.2 webview breakage: https://github.com/appium/appium/issues/21705
- PWA webview context lost after backgrounding (iOS 12): https://github.com/appium/appium/issues/13380
- WebdriverIO + Appium PWA example (iOS 12 to 14): https://github.com/wswebcreation/pwa-example
- Apple, WebDriver for Safari: https://developer.apple.com/documentation/webkit/about-webdriver-for-safari
- WebKit blog, WebDriver on iOS 13: https://webkit.org/blog/9395/webdriver-is-coming-to-safari-in-ios-13/
- safaridriver on GitHub macOS images (2020 regression): https://github.com/actions/runner-images/issues/1453
- safaridriver on macos-26 example: https://github.com/Azovgroup/sulamita-youth-ios/blob/main/.github/workflows/safari-shot.yml
- appium-safari-driver: https://github.com/appium/appium-safari-driver
- pymobiledevice3: https://github.com/doronz88/pymobiledevice3 (`docs/guides/webview-debugging.md`, `services/webinspector.py`, `services/mobile_config.py`)
- Playwright cannot attach to iOS: https://github.com/microsoft/playwright/issues/1122, https://github.com/microsoft/playwright/issues/21420
- inspect-webkit: https://github.com/EvanBacon/inspect-webkit
- opensafari: https://github.com/shaun0927/opensafari
- ios-webkit-debug-proxy: https://github.com/google/ios-webkit-debug-proxy
- Maestro: https://github.com/mobile-dev-inc/maestro, https://docs.maestro.dev/get-started/supported-platform/ios.md
- idb iOS 26 issues: https://github.com/facebook/idb/issues/964, https://github.com/facebook/idb/issues/956
- Apple WebClip payload: https://developer.apple.com/documentation/devicemanagement/webclip
- mobileconfig on simulator (unanswered): https://developer.apple.com/forums/thread/708388
- iOS 26 Add to Home Screen / "Open as Web App": https://heise.de/-10749652
- WWDC23 web apps (cookie copy on macOS): https://developer.apple.com/videos/play/wwdc2023/10120/
- GitHub-hosted runners: https://docs.github.com/en/actions/reference/runners/github-hosted-runners
- macos-26 image contents: https://github.com/actions/runner-images/blob/main/images/macos/macos-26-Readme.md
- Docker on Intel macOS runners: https://github.com/douglascamata/setup-docker-macos-action
- Simulator boot time on CI: https://support.circleci.com/hc/en-us/articles/46737392770203-Reducing-iOS-Simulator
- Ionic WebdriverIO + Appium example: https://github.com/ionic-team/ionic-e2e-example
- iOS PWA Runner: https://github.com/Wnt/iOS-pwa-runner (local checkout `/Users/wnt/Documents/iOS-pwa-runner`:
  `docs/simulator-limitations.md`, `docs/operations.md`, `docs/web-push-capture.md`,
  `src/device/SimulatorDevice.ts`, `src/push/InspectorClient.ts`, `scripts/create-simulator.sh`, `scripts/cleanup-orphans.sh`)
