// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { testAdult } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { test, expect } from '../../playwright'
import { enduserLogin } from '../../utils/user'

test.describe('Citizen header customization', () => {
  test.use({
    evakaOptions: {
      citizenCustomizations: {
        langs: ['fi', 'sv']
      }
    }
  })

  test.beforeEach(async () => {
    await resetServiceState()
    await testAdult.saveAdult({ updateMockVtjWithDependants: [] })
  })

  test('English language can be disabled', async ({ evaka }) => {
    await enduserLogin(evaka, testAdult)
    const header = new CitizenHeader(evaka)
    expect(await header.listLanguages()).toStrictEqual({
      fi: true,
      sv: true,
      en: false
    })
  })
})
