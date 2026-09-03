// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { resetServiceState } from '../../generated/api-clients'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'

// The offline page is only reachable once the service worker has claimed the
// page, which happens some time after the first load finishes.
const waitUntilServiceWorkerControls = (page: Page) =>
  page.page.waitForFunction(() => navigator.serviceWorker.controller !== null)

test.describe('Citizen PWA', () => {
  let page: Page

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    page = evaka
  })

  test('a citizen navigation that fails offline is answered with the offline page', async () => {
    await page.goto('/login')
    await waitUntilServiceWorkerControls(page)

    await page.context.setOffline(true)
    await page.goto('/calendar')

    await expect(page.findByDataQa('offline-page')).toBeVisible()
  })

  test('an employee navigation that fails offline is left to the browser', async () => {
    await page.goto('/login')
    await waitUntilServiceWorkerControls(page)

    await page.context.setOffline(true)
    const navigationError = await page.goto('/employee/').then(
      () => null,
      (err: Error) => err
    )

    expect(navigationError).not.toBeNull()
    await expect(page.findByDataQa('offline-page')).toBeHidden()
  })
})
