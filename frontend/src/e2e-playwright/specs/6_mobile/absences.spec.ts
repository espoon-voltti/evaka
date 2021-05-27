// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  createDaycareGroupPlacementFixture,
  createDaycarePlacementFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { newBrowserContext } from '../../browser'
import { Page } from 'playwright'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  insertDaycareGroupFixtures,
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { DaycareGroup } from 'e2e-test-common/dev-api/types'
import { pairMobileDevice } from 'e2e-playwright/utils/mobile'
import LocalDate from 'lib-common/local-date'
import MobileListPage from 'e2e-playwright/pages/mobile/list-page'
import MobileAbsencesPage from 'e2e-playwright/pages/mobile/absences-page'
import MobileChildPage from 'e2e-playwright/pages/mobile/child-page'

let fixtures: AreaAndPersonFixtures
let page: Page
let listPage: MobileListPage
let childPage: MobileChildPage
let absencesPage: MobileAbsencesPage

const daycareGroup2Fixture: DaycareGroup = {
  ...daycareGroupFixture,
  id: uuidv4(),
  name: 'Ryhmä 2'
}

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  await insertDaycareGroupFixtures([daycareGroupFixture, daycareGroup2Fixture])
  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    fixtures.familyWithTwoGuardians.children[0].id,
    fixtures.daycareFixture.id
  )
  await insertDaycarePlacementFixtures([daycarePlacementFixture])
  const groupPlacementFixture = createDaycareGroupPlacementFixture(
    daycarePlacementFixture.id,
    daycareGroupFixture.id
  )
  await insertDaycareGroupPlacementFixtures([groupPlacementFixture])

  const employee = await Fixture.employee().save()

  page = await (await newBrowserContext()).newPage()
  listPage = new MobileListPage(page)
  childPage = new MobileChildPage(page)
  absencesPage = new MobileAbsencesPage(page)

  const mobileSignupUrl = await pairMobileDevice(
    employee.data.id!, // eslint-disable-line
    fixtures.daycareFixture.id
  )
  await page.goto(mobileSignupUrl)
})
afterEach(async () => {
  await page.close()
})

describe('Future absences', () => {
  test.skip('User can set and delete future absence periods', async () => {
    await listPage.selectChild(fixtures.familyWithTwoGuardians.children[0].id)
    await childPage.markFutureAbsences()
    await waitUntilEqual(() => absencesPage.getAbsencesCount(), 0)

    await absencesPage.markNewAbsencePeriod(
      LocalDate.today().addWeeks(1),
      LocalDate.today().addWeeks(2),
      'SICKLEAVE'
    )
    await childPage.markFutureAbsences()
    await waitUntilEqual(() => absencesPage.getAbsencesCount(), 1)

    await absencesPage.markNewAbsencePeriod(
      LocalDate.today().addWeeks(4),
      LocalDate.today().addWeeks(5),
      'SICKLEAVE'
    )
    await childPage.markFutureAbsences()
    await waitUntilEqual(() => absencesPage.getAbsencesCount(), 2)

    await absencesPage.deleteFirstAbsencePeriod()
    await waitUntilEqual(() => absencesPage.getAbsencesCount(), 1)
  })
})
