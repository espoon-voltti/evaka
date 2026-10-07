// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../e2e-test/config'
import type { DevPerson } from '../../e2e-test/generated/api-types'

import { navigate } from './navigate'

export async function citizenLogin(person: DevPerson, path: string) {
  if (!person.ssn) {
    throw new Error('Person does not have an SSN: cannot login')
  }
  // Returning `status` from execute() makes Appium's Safari adapter throw
  const { httpStatus } = await browser.execute(
    async (url: string, ssn: string) => {
      const response = await fetch(url, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ssn })
      })
      return { httpStatus: response.status }
    },
    `${config.devApiGwUrl}/auth/citizen-sfi-login`,
    person.ssn
  )
  if (httpStatus !== 200) {
    throw new Error(`Citizen login failed with HTTP ${httpStatus}`)
  }
  await navigate(config.enduserUrl + path)
}
