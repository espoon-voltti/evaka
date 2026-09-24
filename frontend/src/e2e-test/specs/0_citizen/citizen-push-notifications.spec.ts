// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { testAdult } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import CitizenPersonalDetailsPage from '../../pages/citizen/citizen-personal-details'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

// Push is suggested only inside the installed app, which Playwright cannot
// launch. Headless Chromium also has no push service to subscribe to, and it
// reports the notification permission as denied even after
// context.grantPermissions. The script makes the app believe it runs from the
// home screen with the given permission, and gives it a browser-side
// subscription that survives page loads, so the backend sees the same
// endpoint the way it would from a real device.
const emulateInstalledApp = (
  page: Page,
  permission: NotificationPermission = 'granted'
) =>
  page.page.addInitScript((permission) => {
    Object.defineProperty(navigator, 'standalone', { get: () => true })
    Object.defineProperty(Notification, 'permission', {
      get: () => permission
    })

    const storageKey = 'e2e-push-subscription'
    const decode = (base64: string) =>
      Uint8Array.from(atob(base64), (c) => c.charCodeAt(0))
    const encode = (bytes: ArrayBuffer) =>
      btoa(String.fromCharCode(...new Uint8Array(bytes)))

    interface Stored {
      endpoint: string
      auth: string
      p256dh: string
    }
    const subscriptionOf = (stored: Stored) => ({
      endpoint: stored.endpoint,
      expirationTime: null,
      getKey: (name: string) =>
        decode(name === 'auth' ? stored.auth : stored.p256dh).buffer,
      unsubscribe: () => {
        localStorage.removeItem(storageKey)
        pushManager.subscription = null
        return Promise.resolve(true)
      }
    })
    const pushManager = {
      subscription: null as ReturnType<typeof subscriptionOf> | null,
      getSubscription: () => Promise.resolve(pushManager.subscription),
      subscribe: async () => {
        const keyPair = await crypto.subtle.generateKey(
          { name: 'ECDH', namedCurve: 'P-256' },
          true,
          ['deriveBits']
        )
        const stored: Stored = {
          endpoint: `https://push.example.com/${crypto.randomUUID()}`,
          auth: encode(crypto.getRandomValues(new Uint8Array(16)).buffer),
          p256dh: encode(
            await crypto.subtle.exportKey('raw', keyPair.publicKey)
          )
        }
        localStorage.setItem(storageKey, JSON.stringify(stored))
        pushManager.subscription = subscriptionOf(stored)
        return pushManager.subscription
      }
    }
    const stored = localStorage.getItem(storageKey)
    if (stored) {
      pushManager.subscription = subscriptionOf(JSON.parse(stored) as Stored)
    }
    Object.defineProperty(ServiceWorkerRegistration.prototype, 'pushManager', {
      get: () => pushManager
    })
  }, permission)

test.describe('Citizen push notifications', () => {
  test.beforeEach(async () => {
    await resetServiceState()
    await testAdult.saveAdult({ updateMockVtjWithDependants: [] })
  })

  test('the suggestion is not shown outside the installed app', async ({
    evaka
  }) => {
    await enduserLogin(evaka, testAdult, '/')

    await expect(evaka.findByDataQa('applications-list')).toBeVisible()
    await expect(evaka.findByDataQa('push-suggestion')).toBeHidden()
  })

  test('the suggestion enables push notifications in the installed app', async ({
    evaka
  }) => {
    await emulateInstalledApp(evaka)
    await enduserLogin(evaka, testAdult, '/')

    const suggestion = evaka.findByDataQa('push-suggestion')
    await expect(suggestion).toBeVisible()
    await expect(suggestion).toHaveAttribute('data-status', 'disabled')

    await evaka.findByDataQa('push-suggestion-enable').click()

    await expect(suggestion).toHaveAttribute('data-status', 'enabled')
    await expect(evaka.findByDataQa('push-suggestion-test')).toBeVisible()

    await evaka.goto('/personal-details')
    const section = new CitizenPersonalDetailsPage(evaka)
      .pushNotificationsSection
    await expect(section.accountStatus).toBeVisible()
    await expect(section.sendTest).toBeVisible()
    await expect(section.devices).toHaveCount(1)
    await expect(section.deviceIsCurrent(0)).toBeVisible()
    await expect(section.deviceName(0)).toContainText('Kotinäytön sovellus')
  })

  test('the suggestion is not shown when push was enabled elsewhere', async ({
    evaka
  }) => {
    await emulateInstalledApp(evaka)
    await enduserLogin(evaka, testAdult, '/personal-details')

    const section = new CitizenPersonalDetailsPage(evaka)
      .pushNotificationsSection
    await section.enable.click()
    await expect(section.devices).toHaveCount(1)
    await expect(evaka.findByDataQa('push-suggestion')).toBeHidden()

    await evaka.goto('/')
    await expect(evaka.findByDataQa('applications-list')).toBeVisible()
    await expect(evaka.findByDataQa('push-suggestion')).toBeHidden()
  })

  test('the suggestion is only a note when notifications are blocked', async ({
    evaka
  }) => {
    await emulateInstalledApp(evaka, 'denied')
    await enduserLogin(evaka, testAdult, '/')

    await expect(evaka.findByDataQa('push-suggestion')).toBeHidden()
    const note = evaka.findByDataQa('push-suggestion-note')
    await expect(note).toContainText(
      'Push-ilmoitusohjeet löytyvät Omat tiedot -sivulta.'
    )

    await evaka.findByDataQa('push-suggestion-note-action').click()
    await expect(note).toBeHidden()

    await evaka.reload()
    await expect(evaka.findByDataQa('applications-list')).toBeVisible()
    await expect(note).toBeHidden()
  })

  test('personal details report a blocked device without a task', async ({
    evaka
  }) => {
    await emulateInstalledApp(evaka, 'denied')
    await enduserLogin(evaka, testAdult, '/personal-details')

    const page = new CitizenPersonalDetailsPage(evaka)
    await expect(page.pushNotificationsSection.thisDeviceState).toContainText(
      'estetty'
    )
    await expect(page.pushNotificationsSection.enable).toBeHidden()
    await expect(page.enablePushNotificationsTask).toBeHidden()
  })

  test('"Myöhemmin" replaces the suggestion with a note that dismisses it', async ({
    evaka
  }) => {
    await emulateInstalledApp(evaka)
    await enduserLogin(evaka, testAdult, '/')

    await evaka.findByDataQa('push-suggestion-later').click()
    await expect(evaka.findByDataQa('push-suggestion')).toBeHidden()

    await evaka.findByDataQa('push-suggestion-note-action').click()
    await expect(evaka.findByDataQa('push-suggestion-note')).toBeHidden()

    await evaka.reload()
    await expect(evaka.findByDataQa('applications-list')).toBeVisible()
    await expect(evaka.findByDataQa('push-suggestion')).toBeHidden()
  })

  test('enabling push is offered as a task in personal details', async ({
    evaka
  }) => {
    await emulateInstalledApp(evaka)
    await enduserLogin(evaka, testAdult, '/personal-details')

    const page = new CitizenPersonalDetailsPage(evaka)
    const section = page.pushNotificationsSection
    await expect(page.enablePushNotificationsTask).toBeVisible()
    await expect(section.accountStatus).toBeHidden()

    await page.enablePushNotificationsTask.click()
    await expect(section.enable).toBeVisible()
    await section.enable.click()

    await expect(page.enablePushNotificationsTask).toBeHidden()
    await expect(section.accountStatus).toBeVisible()
    await expect(section.devices).toHaveCount(1)
  })

  test('a device can be revoked from personal details', async ({ evaka }) => {
    await emulateInstalledApp(evaka)
    await enduserLogin(evaka, testAdult, '/personal-details')

    const section = new CitizenPersonalDetailsPage(evaka)
      .pushNotificationsSection
    await section.enable.click()
    await expect(section.devices).toHaveCount(1)

    await section.revokeDevice(0).click()

    await expect(section.devices).toHaveCount(0)
    await expect(section.accountStatus).toBeHidden()
    await expect(section.enable).toBeVisible()
  })
})
