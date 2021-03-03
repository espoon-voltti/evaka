// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// This will be used with Testcafe's useRole.

import { Role } from 'testcafe'
import Home from '../pages/home'
import config from './index'

const home = new Home()

export const seppoAdminRole = Role(
  config.employeeUrl,
  async () => {
    await home.login('admin')
  },
  { preserveUrl: true }
)

export const seppoManagerRole = Role(
  config.employeeUrl,
  async () => {
    await home.login('manager')
  },
  { preserveUrl: true }
)

export const enduserRole = Role(
  config.enduserUrl,
  async () => {
    await home.login('enduser')
  },
  { preserveUrl: true }
)

export const mobileAutoSignInRole = (token: string) =>
  Role(
    config.mobileUrl,
    async (t) => {
      await t.navigateTo(
        `${config.mobileBaseUrl}/api/internal/auth/mobile-e2e-signup?token=${token}`
      )
    },
    { preserveUrl: true }
  )

export const mobileRole = Role(config.mobileUrl, () => Promise.resolve(), {
  preserveUrl: true
})
