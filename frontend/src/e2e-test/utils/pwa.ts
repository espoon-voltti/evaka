// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page } from './page'

export const iosUserAgent =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15'

export const androidUserAgent =
  'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36'

// Chromium only fires beforeinstallprompt when it decides the app is
// installable, which it does not do under test, so the test supplies the event
// the app is waiting for.
export const fireInstallPrompt = (page: Page) =>
  page.page.evaluate(() => {
    const event = new Event('beforeinstallprompt')
    Object.assign(event, {
      prompt: () => {
        ;(window as unknown as { promptShown: boolean }).promptShown = true
        return Promise.resolve()
      },
      userChoice: Promise.resolve({ outcome: 'accepted' })
    })
    window.dispatchEvent(event)
  })

// Chromium has no way to open a page as an installed app under test, so this
// uses the flag iOS sets for home screen apps, which the app also accepts.
export const emulateRunningInstalled = (page: Page) =>
  page.page.addInitScript(() => {
    Object.defineProperty(Navigator.prototype, 'standalone', {
      configurable: true,
      get: () => true
    })
  })

// Notification.permission stays 'denied' in Playwright's Chromium even when the
// context grants notifications
export const emulateNotificationPermission = (
  page: Page,
  permission: NotificationPermission
) =>
  page.page.addInitScript((permission) => {
    Object.defineProperty(Notification, 'permission', {
      configurable: true,
      get: () => permission
    })
  }, permission)

export const removePushSupport = (page: Page) =>
  page.page.addInitScript(() => {
    delete (window as { PushManager?: unknown }).PushManager
  })
