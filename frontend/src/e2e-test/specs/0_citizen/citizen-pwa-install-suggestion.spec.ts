// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import {
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const today = LocalDate.of(2022, 1, 12)
const mockedTime = HelsinkiDateTime.fromLocal(today, LocalTime.of(12, 0))

test.use({ evakaOptions: { mockedTime } })

// Chromium only fires beforeinstallprompt when it decides the app is
// installable, which it does not do under test, so the test supplies the event
// the app is waiting for.
const fireInstallPrompt = (page: Page) =>
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

type InstallAnswer = { outcome: 'accepted' | 'dismissed' }

interface AnswerableWindow {
  answerInstallPrompt: (answer: InstallAnswer) => void
}

// The suggestion has to survive the whole time the browser dialog is open, so
// this variant leaves the citizen's answer for the test to give.
const fireInstallPromptAwaitingAnswer = (page: Page) =>
  page.page.evaluate(() => {
    const event = new Event('beforeinstallprompt')
    Object.assign(event, {
      prompt: () => Promise.resolve(),
      userChoice: new Promise<InstallAnswer>((resolve) => {
        ;(window as unknown as AnswerableWindow).answerInstallPrompt = resolve
      })
    })
    window.dispatchEvent(event)
  })

const answerInstallPrompt = (page: Page, answer: InstallAnswer) =>
  page.page.evaluate(
    (answer) =>
      (window as unknown as AnswerableWindow).answerInstallPrompt(answer),
    answer
  )

const iosUserAgent =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15'

const androidUserAgent =
  'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36'

const setUpFamily = async (withPlacement: boolean) => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  if (withPlacement) {
    await Fixture.placement({
      childId: testChild.id,
      unitId: testDaycare.id,
      startDate: today,
      endDate: today.addYears(1)
    }).save()
  }
}

test.describe('Citizen PWA install suggestion', () => {
  test.use({ hasTouch: true, userAgent: androidUserAgent })

  test('is shown to a citizen whose child has a placement', async ({
    evaka
  }) => {
    await setUpFamily(true)
    await enduserLogin(evaka, testAdult, '/')
    await fireInstallPrompt(evaka)

    await expect(evaka.findByDataQa('pwa-install-suggestion')).toBeVisible()
  })

  test('is not shown when no child has a placement', async ({ evaka }) => {
    await setUpFamily(false)
    await enduserLogin(evaka, testAdult, '/')
    await fireInstallPrompt(evaka)

    await expect(evaka.findByDataQa('pwa-install-suggestion')).toBeHidden()
  })

  test('opens the browser install dialog on Chromium', async ({ evaka }) => {
    await setUpFamily(true)
    await enduserLogin(evaka, testAdult, '/')
    await fireInstallPrompt(evaka)

    await evaka.findByDataQa('pwa-install-suggestion-action').click()

    expect(
      await evaka.page.evaluate(
        () => (window as unknown as { promptShown?: boolean }).promptShown
      )
    ).toBe(true)
  })

  test('stays on screen while the browser install dialog is open', async ({
    evaka
  }) => {
    await setUpFamily(true)
    await enduserLogin(evaka, testAdult, '/')
    await fireInstallPromptAwaitingAnswer(evaka)

    await evaka.findByDataQa('pwa-install-suggestion-action').click()
    await expect(evaka.findByDataQa('pwa-install-suggestion')).toBeVisible()

    await answerInstallPrompt(evaka, { outcome: 'accepted' })
    await expect(evaka.findByDataQa('pwa-install-suggestion')).toBeHidden()
  })

  test('stays dismissed after the citizen closes it', async ({ evaka }) => {
    await setUpFamily(true)
    await enduserLogin(evaka, testAdult, '/')
    await fireInstallPrompt(evaka)

    await evaka.findByDataQa('pwa-install-suggestion-close').click()
    await expect(evaka.findByDataQa('pwa-install-suggestion')).toBeHidden()

    await evaka.goto('/calendar')
    await fireInstallPrompt(evaka)
    await expect(evaka.findByDataQa('pwa-install-suggestion')).toBeHidden()
  })
})

test.describe('Citizen PWA install suggestion on desktop', () => {
  test('is not shown even when the browser offers an install', async ({
    evaka
  }) => {
    await setUpFamily(true)
    await enduserLogin(evaka, testAdult, '/')
    await fireInstallPrompt(evaka)

    await expect(evaka.findByDataQa('pwa-install-suggestion')).toBeHidden()
  })
})

test.describe('Citizen PWA install suggestion on iOS', () => {
  test('shows the home screen instructions instead of an install button', async ({
    newEvakaPage
  }) => {
    await setUpFamily(true)
    const page = await newEvakaPage({
      mockedTime,
      userAgent: iosUserAgent,
      hasTouch: true
    })
    await enduserLogin(page, testAdult, '/')

    await expect(page.findByDataQa('pwa-install-suggestion')).toBeVisible()
    await expect(
      page.findByDataQa('pwa-install-suggestion-instructions')
    ).toBeHidden()

    await page.findByDataQa('pwa-install-suggestion-action').click()

    await expect(
      page.findByDataQa('pwa-install-suggestion-instructions')
    ).toBeVisible()
  })
})

test.describe('Citizen PWA install suggestion on the calendar', () => {
  test('survives the calendar scrolling itself to the current date', async ({
    newEvakaPage
  }) => {
    await setUpFamily(true)
    const page = await newEvakaPage({
      mockedTime,
      userAgent: iosUserAgent,
      hasTouch: true,
      viewport: { width: 375, height: 667 }
    })
    await enduserLogin(page, testAdult, '/')
    await expect(page.findByDataQa('pwa-install-suggestion')).toBeVisible()

    // Without the scroll this test would pass for the wrong reason. The
    // calendar scrolls on a requestAnimationFrame once its data renders, so
    // this has to be polled rather than read once.
    await expect
      .poll(() => page.page.evaluate(() => window.scrollY))
      .toBeGreaterThan(0)

    await expect(page.findByDataQa('pwa-install-suggestion')).toBeInViewport()
  })
})

test.describe('Home screen section in personal details', () => {
  test('shows the instructions after the email login section', async ({
    newEvakaPage
  }) => {
    await setUpFamily(true)
    const page = await newEvakaPage({
      mockedTime,
      userAgent: iosUserAgent,
      hasTouch: true
    })
    await enduserLogin(page, testAdult, '/personal-details')

    await expect(page.findByDataQa('home-screen-section')).toBeVisible()
    await expect(page.findByDataQa('login-details-section')).toBeVisible()

    const order = await page.page.evaluate(() => {
      const login = document.querySelector('[data-qa="login-details-section"]')
      const home = document.querySelector('[data-qa="home-screen-section"]')
      if (!login || !home) return 'missing'
      return login.compareDocumentPosition(home) &
        Node.DOCUMENT_POSITION_FOLLOWING
        ? 'after'
        : 'before'
    })
    expect(order).toBe('after')

    await page.findByDataQa('home-screen-action').click()
    await expect(page.findByDataQa('home-screen-section')).toContainText(
      'Toimi näin'
    )
  })

  test('is offered as a task pointing at the instructions', async ({
    newEvakaPage
  }) => {
    await setUpFamily(true)
    const page = await newEvakaPage({
      mockedTime,
      userAgent: iosUserAgent,
      hasTouch: true
    })
    await enduserLogin(page, testAdult, '/personal-details')

    const task = page.findByDataQa('task-add-to-home-screen')
    await expect(task).toBeVisible()
    await expect(task).toContainText('Saat muistutukset suoraan puhelimeesi.')

    await task.click()
    await expect(page.findByDataQa('home-screen-section')).toBeVisible()
  })

  test('offers no task and no section on desktop', async ({ evaka }) => {
    await setUpFamily(true)
    await enduserLogin(evaka, testAdult, '/personal-details')

    await expect(evaka.findByDataQa('task-add-to-home-screen')).toBeHidden()
  })

  test('is not shown on desktop', async ({ evaka }) => {
    await setUpFamily(true)
    await enduserLogin(evaka, testAdult, '/personal-details')

    await expect(evaka.findByDataQa('home-screen-section')).toBeHidden()
  })
})
