// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// This will be used with Testcafe's useRole.

import { Role } from 'testcafe'
import { DevLoginUser } from '../pages/dev-login-form'
import Home from '../pages/home'
import config from 'e2e-test-common/config'

const home = new Home()

export const seppoAdmin: DevLoginUser = {
  aad: config.adminAad,
  roles: ['SERVICE_WORKER', 'FINANCE_ADMIN', 'ADMIN']
}

export const seppoManager: DevLoginUser = {
  aad: config.supervisorAad,
  roles: []
}

export async function employeeLogin(
  t: TestController,
  user: string | DevLoginUser,
  landingUrl?: string
) {
  await t.navigateTo(config.employeeUrl)
  const authUrl =
    'http://localhost:9099/api/internal/auth/saml/login/callback?RelayState=%2Femployee'

  await t.eval(
    () => {
      const params = new URLSearchParams()
      if (typeof user === 'string') {
        params.append('preset', user)
      } else {
        params.append('preset', 'custom')
        params.append('firstName', 'Seppo')
        params.append('lastName', 'Sorsa')
        params.append('email', 'seppo.sorsa@espoo.fi')
        params.append('aad', user.aad)
        user.roles.forEach((role) => params.append('roles', role))
      }
      return fetch(authUrl, { method: 'POST', body: params }).then(
        () => undefined
      )
    },
    { dependencies: { authUrl, user } }
  )
  if (landingUrl) {
    await t.navigateTo(landingUrl)
  }
}

export async function mobileLogin(t: TestController, token: string) {
  await t.navigateTo(
    `${config.mobileBaseUrl}/api/internal/auth/mobile-e2e-signup?token=${token}`
  )
}

export const enduserRole = Role(
  config.enduserUrl,
  async () => {
    await home.login('enduser')
  },
  { preserveUrl: true }
)
