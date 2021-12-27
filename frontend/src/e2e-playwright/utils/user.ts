// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import { UUID } from 'lib-common/types'
import { Page } from './page'

export type UserRole =
  | 'ADMIN'
  | 'FINANCE_ADMIN'
  | 'UNIT_SUPERVISOR'
  | 'SERVICE_WORKER'
  | 'STAFF'
  | 'DIRECTOR'
  | 'REPORT_VIEWER'
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
  'REPORT_VIEWER',
  'ADMIN'
] as const

function getLoginUser(role: UserRole): DevLoginUser | string {
  switch (role) {
    case 'ADMIN':
      return {
        aad: config.adminAad,
        roles: ['SERVICE_WORKER', 'FINANCE_ADMIN', 'ADMIN']
      }
    case 'SERVICE_WORKER':
      return {
        aad: config.serviceWorkerAad,
        roles: ['SERVICE_WORKER']
      }
    case 'FINANCE_ADMIN':
      return config.financeAdminAad
    case 'DIRECTOR':
      return config.directorAad
    case 'REPORT_VIEWER':
      return config.reportViewerAad
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
  await page.find('[data-qa="login-btn"]').click()
  await page.find('[data-qa="user-menu-title-desktop"]').waitUntilVisible()
}

export async function employeeLogin(page: Page, role: UserRole) {
  const authUrl = `${config.apiUrl}/auth/saml/login/callback?RelayState=%2Femployee`
  const user = getLoginUser(role)

  if (!page.url.startsWith(config.employeeUrl)) {
    // We must be in the correct domain to be able to fetch()
    await page.goto(config.employeeLoginUrl)
  }

  await page.page.evaluate(
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
