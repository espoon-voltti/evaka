// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mobileViewport } from '../../browser'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenPersonalDetailsPage from '../../pages/citizen/citizen-personal-details'
import { test } from '../../playwright'
import type { Page } from '../../utils/page'
import {
  androidUserAgent,
  emulateNotificationPermission,
  emulateRunningInstalled,
  fireInstallPrompt,
  iosUserAgent,
  removePushSupport
} from '../../utils/pwa'
import { enduserLogin } from '../../utils/user'

const email = 'test@example.com'
const citizenWithoutLogin = Fixture.person({
  email,
  verifiedEmail: email,
  phone: '123456789'
})
const citizenWithoutContacts = Fixture.person({ email: null, phone: '' })

const openPersonalDetails = async (
  page: Page,
  citizen: typeof citizenWithoutLogin
) => {
  await citizen.saveAdult({ updateMockVtjWithDependants: [] })
  await enduserLogin(page, citizen, '/personal-details')
  return {
    personalDetails: new CitizenPersonalDetailsPage(page),
    header: new CitizenHeader(page, 'mobile')
  }
}

test.beforeEach(async () => {
  await resetServiceState()
})

test.describe('Personal details tasks in an Android browser', () => {
  test.use({
    userAgent: androidUserAgent,
    hasTouch: true,
    viewport: mobileViewport
  })

  test('an offered install holds back the login task until it is answered', async ({
    evaka
  }) => {
    const { personalDetails, header } = await openPersonalDetails(
      evaka,
      citizenWithoutLogin
    )
    await personalDetails.assertTasks(['task-add-weak-login'])

    await fireInstallPrompt(evaka)
    await personalDetails.assertTasks(['task-add-to-home-screen'])
    await header.checkPersonalDetailsAttentionIndicatorsAreShown()

    await personalDetails.addToHomeScreenTask.click()
    await evaka.findByDataQa('home-screen-action').click()
    await personalDetails.assertTasks(['task-add-weak-login'])
  })

  test('the combined contact task is shown alongside the install task', async ({
    evaka
  }) => {
    const { personalDetails } = await openPersonalDetails(
      evaka,
      citizenWithoutContacts
    )
    await fireInstallPrompt(evaka)
    await personalDetails.assertTasks([
      'task-add-email-and-phone',
      'task-add-to-home-screen'
    ])
  })
})

test.describe('Personal details tasks in iOS Safari', () => {
  test.use({
    userAgent: iosUserAgent,
    hasTouch: true,
    viewport: mobileViewport
  })

  test('the home screen task always holds back the login task', async ({
    evaka
  }) => {
    const { personalDetails, header } = await openPersonalDetails(
      evaka,
      citizenWithoutLogin
    )
    await personalDetails.assertTasks(['task-add-to-home-screen'])
    await header.checkPersonalDetailsAttentionIndicatorsAreShown()
  })
})

test.describe('Personal details tasks in the installed app', () => {
  test.use({
    userAgent: androidUserAgent,
    hasTouch: true,
    viewport: mobileViewport
  })

  test.beforeEach(async ({ evaka }) => {
    await emulateRunningInstalled(evaka)
    await emulateNotificationPermission(evaka, 'default')
  })

  test('push and login tasks are offered together', async ({ evaka }) => {
    const { personalDetails, header } = await openPersonalDetails(
      evaka,
      citizenWithoutLogin
    )
    await personalDetails.assertTasks([
      'task-enable-push-notifications',
      'task-add-weak-login'
    ])
    await header.checkPersonalDetailsAttentionIndicatorsAreShown()
  })

  test('only the login task is offered without push support', async ({
    evaka
  }) => {
    await removePushSupport(evaka)
    const { personalDetails } = await openPersonalDetails(
      evaka,
      citizenWithoutLogin
    )
    await personalDetails.assertTasks(['task-add-weak-login'])
  })

  test('the combined contact task is shown alongside the push task', async ({
    evaka
  }) => {
    const { personalDetails } = await openPersonalDetails(
      evaka,
      citizenWithoutContacts
    )
    await personalDetails.assertTasks([
      'task-add-email-and-phone',
      'task-enable-push-notifications'
    ])
  })
})
