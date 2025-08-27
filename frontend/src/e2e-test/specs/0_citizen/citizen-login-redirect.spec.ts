// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare,
  testDaycareGroup
} from '../../dev-api/fixtures'
import {
  resetServiceState,
  upsertWeakCredentials
} from '../../generated/api-clients'
import type { DevDaycare, DevPerson } from '../../generated/api-types'
import { Page } from '../../utils/page'
import { enduserLogin, enduserLoginWeak } from '../../utils/user'

describe('Citizen login redirects', () => {
  let page: Page
  const credentials = {
    username: 'test@example.com',
    password: 'TestPassword456!'
  }

  const initConfigurations = [
    [
      'direct login',
      async (page: Page) => enduserLogin(page, testAdult)
    ] as const,
    [
      'weak login',
      async (page: Page) => enduserLoginWeak(page, credentials)
    ] as const
  ]

  describe.each(initConfigurations)(`Interactions with %s`, (name, login) => {
    describe('Without children with a placement', () => {
      beforeEach(async () => {
        await resetServiceState()
        await testAdult.saveAdult({
          updateMockVtjWithDependants: []
        })
        await upsertWeakCredentials({
          id: testAdult.id,
          body: credentials
        })

        page = await Page.open()
      })
      test('Login takes to the desired page', async () => {
        const expectedEndurl =
          name === 'weak login'
            ? `${config.enduserUrl}/map`
            : `${config.enduserUrl}/applications`
        await login(page)
        await page.page.waitForURL(expectedEndurl)
      })
    })

    describe('Having a child with a placement', () => {
      const child = testChild
      let daycare: DevDaycare
      let guardian: DevPerson

      beforeEach(async () => {
        await resetServiceState()
        const area = await testCareArea.save()
        daycare = await Fixture.daycare({
          ...testDaycare,
          areaId: area.id
        }).save()
        await Fixture.daycareGroup({
          ...testDaycareGroup,
          daycareId: daycare.id
        }).save()

        const child1 = await child.saveChild({ updateMockVtj: true })
        guardian = await testAdult.saveAdult({
          updateMockVtjWithDependants: [child1]
        })
        await Fixture.guardian(child1, guardian).save()
        await Fixture.placement({
          childId: child1.id,
          unitId: daycare.id,
          startDate: LocalDate.of(2020, 1, 1),
          endDate: LocalDate.of(2036, 6, 30)
        }).save()
        await Fixture.dailyServiceTime({
          childId: child1.id
        }).save()
        await upsertWeakCredentials({
          id: testAdult.id,
          body: credentials
        })
        page = await Page.open()
      })
      test('Login with placement takes to the desired page', async () => {
        await login(page)
        await page.page.waitForURL(`${config.enduserUrl}/calendar`)
      })
    })
  })
})
