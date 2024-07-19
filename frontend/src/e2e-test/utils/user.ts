// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getDatabaseId } from '../browser'
import config from '../config'
import { DevPerson } from '../generated/api-types'

import { Page, TextInput } from './page'

export async function enduserLogin(page: Page, person: DevPerson) {
  if (!person.ssn) {
    throw new Error('Person does not have an SSN: cannot login')
  }

  const authUrl = `${config.citizenApiUrl}/auth/saml/login/callback?RelayState=%2Fapplications`
  if (!page.url.startsWith(config.enduserUrl)) {
    // We must be in the correct domain to be able to fetch()
    await page.goto(config.enduserLoginUrl)
  }

  await page.page.evaluate(
    ({
      ssn,
      authUrl,
      databaseId
    }: {
      ssn: string
      authUrl: string
      databaseId: number | null
    }) => {
      const params = new URLSearchParams()
      if (databaseId !== null) {
        params.append('databaseId', databaseId.toString())
      }
      params.append('preset', ssn)
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
    { ssn: person.ssn, authUrl, databaseId: getDatabaseId() }
  )
  await page.goto(config.enduserUrl + '/applications')
}

export type CitizenWeakAccount = {
  username: string
  password: string
  email: string
  socialSecurityNumber: string
  firstName: string
  lastName: string
}

export const citizenWeakAccount = (person: DevPerson): CitizenWeakAccount => ({
  username: 'test@example.com',
  password: 'test123',
  email: 'test@example.com',
  socialSecurityNumber: person.ssn!,
  firstName: 'Seppo',
  lastName: 'Sorsa'
})

export async function enduserLoginWeak(
  page: Page,
  account: CitizenWeakAccount
) {
  await page.goto(config.enduserLoginUrl)
  await page.findByDataQa('weak-login').click()

  await new TextInput(page.find('[id="username"]')).fill(account.username)
  await new TextInput(page.find('[id="password"]')).fill(account.password)
  await page.find('[id="kc-login"]').click()

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
  const authUrl = `${config.apiUrl}/auth/saml/login/callback?RelayState=%2Femployee`
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
    ({
      preset,
      authUrl,
      databaseId
    }: {
      preset: string
      authUrl: string
      databaseId: number | null
    }) => {
      const params = new URLSearchParams()
      params.append('preset', preset)
      if (databaseId !== null) {
        params.append('databaseId', databaseId.toString())
      }
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
    { preset, authUrl, databaseId: getDatabaseId() }
  )
}
