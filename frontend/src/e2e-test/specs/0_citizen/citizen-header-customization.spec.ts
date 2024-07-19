// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { startTest } from '../../browser'
import { testAdult, Fixture } from '../../dev-api/fixtures'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

beforeEach(async () => {
  await startTest()
  await Fixture.person(testAdult).saveAdult({ updateMockVtjWithDependants: [] })
})

describe('Citizen header customization', () => {
  test('English language can be disabled', async () => {
    const page = await Page.open({
      citizenCustomizations: {
        langs: ['fi', 'sv']
      }
    })
    await enduserLogin(page, testAdult)
    const header = new CitizenHeader(page)
    expect(await header.listLanguages()).toStrictEqual({
      fi: true,
      sv: true,
      en: false
    })
  })
})
