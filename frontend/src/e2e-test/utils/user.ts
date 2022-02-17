// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../config'

import { Page } from './page'

export type DevLoginRole = typeof devLoginRoles[number]
const devLoginRoles = [
  'SERVICE_WORKER',
  'FINANCE_ADMIN',
  'DIRECTOR',
  'REPORT_VIEWER',
  'ADMIN'
] as const

export async function enduserLogin(page: Page) {
  await page.goto(config.enduserUrl)
  await page.find('[data-qa="login-btn"]').click()
  await page.find('[data-qa="user-menu-title-desktop"]').waitUntilVisible()
}

export async function employeeLogin(page: Page, user: { externalId: string }) {
  const authUrl = `${config.apiUrl}/auth/saml/login/callback?RelayState=%2Femployee`
  const preset = user.externalId.split(':')[1]

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
