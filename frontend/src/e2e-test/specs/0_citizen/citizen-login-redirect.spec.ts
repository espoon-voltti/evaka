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
import { test } from '../../playwright'
import type { Page } from '../../utils/page'
import { enduserLogin, enduserLoginWeak } from '../../utils/user'

const credentials = {
  username: 'test@example.com',
  password: 'TestPassword456!'
}

test.describe('Citizen login redirects - direct login', () => {
  let page: Page

  test.describe('Without children with a placement', () => {
    test.beforeEach(async ({ evaka }) => {
      await resetServiceState()
      await testAdult.saveAdult({
        updateMockVtjWithDependants: []
      })
      await upsertWeakCredentials({
        id: testAdult.id,
        body: credentials
      })

      page = evaka
    })

    test('Login takes to the desired page', async () => {
      await enduserLogin(page, testAdult, '/')
      await page.page.waitForURL(`${config.enduserUrl}/applications`)
    })
  })

  test.describe('Having a child with a placement', () => {
    const child = testChild
    let daycare: DevDaycare
    let guardian: DevPerson

    test.beforeEach(async ({ evaka }) => {
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
      page = evaka
    })

    test('Login with placement takes to the desired page', async () => {
      await enduserLogin(page, testAdult, '/')
      await page.page.waitForURL(`${config.enduserUrl}/calendar`)
    })
  })
})

test.describe('Citizen login redirects - weak login', () => {
  let page: Page

  test.describe('Without children with a placement', () => {
    test.beforeEach(async ({ evaka }) => {
      await resetServiceState()
      await testAdult.saveAdult({
        updateMockVtjWithDependants: []
      })
      await upsertWeakCredentials({
        id: testAdult.id,
        body: credentials
      })

      page = evaka
    })

    test('Login takes to the desired page', async () => {
      await enduserLoginWeak(page, credentials)
      await page.page.waitForURL(`${config.enduserUrl}/map`)
    })
  })

  test.describe('Having a child with a placement', () => {
    const child = testChild
    let daycare: DevDaycare
    let guardian: DevPerson

    test.beforeEach(async ({ evaka }) => {
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
      page = evaka
    })

    test('Login with placement takes to the desired page', async () => {
      await enduserLoginWeak(page, credentials)
      await page.page.waitForURL(`${config.enduserUrl}/calendar`)
    })
  })
})

test.describe('Citizen login redirects - already logged in', () => {
  let page: Page

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await testAdult.saveAdult({
      updateMockVtjWithDependants: []
    })
    await upsertWeakCredentials({
      id: testAdult.id,
      body: credentials
    })

    page = evaka
  })

  test('Login page takes a logged in user to the next page', async () => {
    await enduserLogin(page, testAdult)
    await page.goto(`${config.enduserLoginUrl}?next=%2Fincome`)
    await page.page.waitForURL(`${config.enduserUrl}/income`)
  })

  test('Weak login form takes a logged in user to the next page', async () => {
    await enduserLoginWeak(page, credentials)
    await page.goto(
      `${config.enduserUrl}/login/form?next=${encodeURIComponent('/messages?x=1')}`
    )
    await page.page.waitForURL(`${config.enduserUrl}/messages?x=1`)
  })

  test('Login page ignores a next page on another origin', async () => {
    await enduserLogin(page, testAdult)
    await page.goto(
      `${config.enduserLoginUrl}?next=${encodeURIComponent('https://example.com/')}`
    )
    await page.page.waitForURL(`${config.enduserUrl}/applications`)
  })
})
