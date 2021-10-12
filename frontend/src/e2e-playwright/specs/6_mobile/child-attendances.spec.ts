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
import { pairMobileDevice } from 'e2e-playwright/utils/mobile'
import MobileListPage from 'e2e-playwright/pages/mobile/list-page'
import MobileChildPage from 'e2e-playwright/pages/mobile/child-page'
import ChildAttendancePage from '../../pages/mobile/child-attendance-page'

let fixtures: AreaAndPersonFixtures
let page: Page
let listPage: MobileListPage
let childPage: MobileChildPage
let childAttendancePage: ChildAttendancePage

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  await insertDaycareGroupFixtures([daycareGroupFixture])
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
  childAttendancePage = new ChildAttendancePage(page)

  const mobileSignupUrl = await pairMobileDevice(
    employee.data.id!, // eslint-disable-line
    fixtures.daycareFixture.id
  )
  await page.goto(mobileSignupUrl)
})
afterEach(async () => {
  await page.close()
})

describe('Child mobile attendances', () => {
  test('Child in non free 5y free placement is not suggested to mark absence types', async () => {
    await listPage.selectChild(fixtures.familyWithTwoGuardians.children[0].id)
    await childPage.selectMarkPresentView()
    await childAttendancePage.selectMarkPresent()
    await childAttendancePage.selectPresentTab()
    await childAttendancePage.selectChildLink(0)
    await childAttendancePage.selectMarkDepartedLink()
    await childAttendancePage.assertMarkAbsentByTypeButtonDoesNotExist(
      'OTHER_ABSENCE'
    )
  })
})
