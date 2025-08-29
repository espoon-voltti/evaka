// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../config'
import type { DevPerson } from '../generated/api-types'
import CitizenCalendarPage from '../pages/citizen/citizen-calendar'

import type { Page } from './page'
import { TextInput } from './page'

export async function enduserLogin(page: Page, person: DevPerson) {
  if (!person.ssn) {
    throw new Error('Person does not have an SSN: cannot login')
  }

  await page.goto(`${config.apiUrl}/citizen/auth/sfi/login?RelayState=%2F`)
  await page.find(`[id="${person.ssn}"]`).locator.check()
  await page.find('[type=submit]').findText('Kirjaudu').click()
  await page.find('[type=submit]').findText('Jatka').click()

  await page.findByDataQa('header-city-logo').waitUntilVisible()
  await page.waitForUrl(/.*\/(calendar|applications)/)

  if (page.url.includes('/calendar')) {
    // Need to wait until calendar page is fully loaded, as it may  otherwise interrupt
    // header dropdown navigation in multiple places
    const calendarPage = new CitizenCalendarPage(page, 'desktop')
    await calendarPage.waitUntilLoaded()
  }
}

export async function enduserLoginWeak(
  page: Page,
  credentials: { username: string; password: string }
) {
  await page.goto(config.enduserLoginUrl)
  await page.findByDataQa('weak-login').click()

  const form = page.findByDataQa('weak-login-form')
  await new TextInput(form.find('[id="username"]')).fill(credentials.username)
  await new TextInput(form.find('[id="password"]')).fill(credentials.password)
  await form.findByDataQa('login').click()
  await form.findByDataQa('login').waitUntilHidden()

  await page.findByDataQa('header-city-logo').waitUntilVisible()
}

export async function employeeLogin(
  page: Page,
  {
    externalId,
    firstName,
    lastName,
    email
  }: {
    externalId?: string | null
    firstName: string
    lastName: string
    email?: string | null
  }
) {
  const authUrl = `${config.apiUrl}/employee/auth/ad/login/callback?RelayState=%2Femployee`
  const preset = JSON.stringify({
    externalId,
    firstName,
    lastName,
    email: email ?? ''
  })

  if (!page.url.startsWith(config.employeeUrl)) {
    // We must be in the correct domain to be able to fetch()
    await page.goto(config.employeeLoginUrl)
  }

  await page.page.evaluate(
    ({ preset, authUrl }: { preset: string; authUrl: string }) => {
      const params = new URLSearchParams()
      params.append('preset', preset)
      return fetch(authUrl, {
        method: 'POST',
        body: params,
        redirect: 'manual'
      }).then((response) => {
        if (response.status >= 400) {
          throw new Error(
            `Fetch to {authUrl} failed with status ${response.status}`
          )
        }
      })
    },
    { preset, authUrl }
  )
}
