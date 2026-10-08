<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# iOS smoke tests

Two tests that run the citizen app as a home screen web clip in the iOS
Simulator, where iOS WebKit lays the page out differently from Safari and
from Chromium. The Playwright suite cannot reproduce the layout bugs these
tests guard against:

- **calendar modal buttons are reachable**: the reservation and absence modal
  footer buttons are inside the viewport, on top in hit testing and actually
  painted on screen. iOS WebKit can paint a `position: fixed` modal clipped by
  the app's scrolling area while geometry, hit testing and even real taps
  still report the button as visible (fixed in PR #9975).
- **header and bottom navigation stay in place with a stale visual viewport**:
  after a reply is sent in landscape and the device rotates back, iOS WebKit
  can leave the visual viewport stale and resolve `position: fixed` against
  it (WebKit bugs 254861 and 297779). The test drives the simulator into that
  state with a recorded user sequence and checks that the header and the
  bottom navigation are laid out and painted where they were before (the app
  shell layout of PR #9798).

The painted position is measured from a lossless simulator screenshot: the
element is coloured magenta and the bounding box of the magenta pixels is
compared with the layout.

## Running

macOS with Xcode and an iOS 26 simulator runtime. Appium, its XCUITest
driver and a prebuilt WebDriverAgent are installed into the gitignored
`.appium` directory, so they are not dependencies of the project:

```sh
yarn ios-smoke:setup
```

The tests need a running local eVaka with the dev API (the same as the
Playwright tests). With the default ports:

```sh
yarn ios-smoke
```

The first run creates an `evaka-smoke` iPhone 17 simulator, puts an eVaka
icon on its home screen and reboots it (SpringBoard reads the icons only on
start). Later runs reuse the icon and only close the app, so that it starts
in a fresh process. Every run launches the clip from the icon and logs in
through the dev API inside the clip. A run takes about two minutes. Appium's
log and the screenshots of failed tests are written to `ios-smoke-results/`.

## Notes

- Everything in the page is driven through the WebDriver `execute` commands
  and synthetic clicks; the software keyboard, rotation and the scrolls of the
  stale viewport sequence are native (WebDriverAgent), because the WebKit
  state only appears with a real keyboard and rotation.
- The stale viewport sequence taps the J key by its recorded position on the
  Finnish landscape keyboard. It has never triggered on the first loop after
  the app was launched, so the test runs up to three loops and fails if the
  state is not reached.
- If the simulator stops rotating ("Unable To Rotate Device"), reboot it:
  `xcrun simctl shutdown evaka-smoke`.
