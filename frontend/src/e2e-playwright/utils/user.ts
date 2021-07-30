// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import config from 'e2e-test-common/config'
import { UUID } from 'e2e-test-common/dev-api/types'

export type UserRole =
  | 'ADMIN'
  | 'FINANCE_ADMIN'
  | 'UNIT_SUPERVISOR'
  | 'SERVICE_WORKER'
  | 'STAFF'
  | 'DIRECTOR'
  | 'MANAGER'
  | 'SPECIAL_EDUCATION_TEACHER'

export interface DevLoginUser {
  aad: UUID
  roles: DevLoginRole[]
}

export type DevLoginRole = typeof devLoginRoles[number]
const devLoginRoles = [
  'SERVICE_WORKER',
  'FINANCE_ADMIN',
  'DIRECTOR',
  'ADMIN'
] as const

function getLoginUser(role: UserRole): DevLoginUser | string {
  switch (role) {
    case 'MANAGER':
      return {
        aad: config.supervisorAad,
        roles: []
      }
    case 'ADMIN':
      return {
        aad: config.adminAad,
        roles: ['SERVICE_WORKER', 'FINANCE_ADMIN', 'ADMIN']
      }
    case 'SERVICE_WORKER':
      return config.serviceWorkerAad
    case 'FINANCE_ADMIN':
      return config.financeAdminAad
    case 'DIRECTOR':
      return config.directorAad
    case 'STAFF':
      return config.staffAad
    case 'UNIT_SUPERVISOR':
      return config.unitSupervisorAad
    case 'SPECIAL_EDUCATION_TEACHER':
      return config.specialEducationTeacher
  }
}

export async function enduserLogin(page: Page) {
  await page.goto(config.enduserUrl)
  await page.click('[data-qa="login-btn"]')
  await page.waitForSelector('[data-qa="logout-btn"]', { state: 'visible' })
}

export async function employeeLogin(page: Page, role: UserRole) {
  const authUrl = `${config.apiUrl}/auth/saml/login/callback?RelayState=%2Femployee`
  const user = getLoginUser(role)

  if (!page.url().startsWith(config.employeeUrl)) {
    // We must be in the correct domain to be able to fetch()
    await page.goto(config.employeeLoginUrl)
  }

  await page.evaluate(
    ({ user, authUrl }: { user: DevLoginUser | string; authUrl: string }) => {
      const params = new URLSearchParams()
      if (typeof user === 'string') {
        params.append('preset', user)
      } else {
        params.append('preset', 'custom')
        params.append('firstName', 'Seppo')
        params.append('lastName', 'Sorsa')
        params.append('email', 'seppo.sorsa@evaka.test')
        params.append('aad', user.aad)
        user.roles.forEach((role) => params.append('roles', role))
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
    { user, authUrl }
  )
}
