// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../config'
import type { DevEmployee, DevPerson } from '../generated/api-types'
import { expect } from '../playwright'

import type { Page } from './page'
import { TextInput } from './page'

export async function enduserLogin(
  page: Page,
  person: DevPerson,
  path?: string
) {
  if (!person.ssn) {
    throw new Error('Person does not have an SSN: cannot login')
  }
  await page.page.request.post(`${config.devApiGwUrl}/auth/sfi-login`, {
    data: { type: 'citizen', ssn: person.ssn }
  })
  if (path) {
    await page.goto(config.enduserUrl + path)
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
  await expect(form.findByDataQa('login')).toBeHidden()

  await expect(page.findByDataQa('header-city-logo')).toBeVisible()
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
  await page.page.request.post(authUrl, { form: { preset } })
}

export async function employeeLoginSfi(page: Page, employee: DevEmployee) {
  if (!employee.ssn) {
    throw new Error('Employee does not have an SSN: cannot login')
  }
  await page.page.request.post(`${config.devApiGwUrl}/auth/sfi-login`, {
    data: {
      type: 'employee',
      ssn: employee.ssn,
      firstName: employee.firstName,
      lastName: employee.lastName
    }
  })
}
