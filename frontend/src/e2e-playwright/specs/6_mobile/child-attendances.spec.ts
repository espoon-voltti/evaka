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
import { PlacementType } from 'lib-common/generated/api-types/placement'

let fixtures: AreaAndPersonFixtures
let page: Page
let listPage: MobileListPage
let childPage: MobileChildPage
let childAttendancePage: ChildAttendancePage

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  await insertDaycareGroupFixtures([daycareGroupFixture])
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

const createPlacement = async (placementType: PlacementType) => {
  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    fixtures.familyWithTwoGuardians.children[0].id,
    fixtures.daycareFixture.id,
    '2021-05-01',
    '2022-08-31',
    placementType
  )
  await insertDaycarePlacementFixtures([daycarePlacementFixture])
  const groupPlacementFixture = createDaycareGroupPlacementFixture(
    daycarePlacementFixture.id,
    daycareGroupFixture.id
  )
  await insertDaycareGroupPlacementFixtures([groupPlacementFixture])
}

const checkAbsentTypeSelectionExistance = async (expectedToExist: boolean) => {
  await listPage.selectChild(fixtures.familyWithTwoGuardians.children[0].id)
  await childPage.selectMarkPresentView()
  await childAttendancePage.selectMarkPresent()
  await childAttendancePage.selectPresentTab()
  await childAttendancePage.selectChildLink(0)
  await childAttendancePage.selectMarkDepartedLink()

  expectedToExist
    ? await childAttendancePage.assertMarkAbsentByTypeButtonExists(
        'OTHER_ABSENCE'
      )
    : await childAttendancePage.assertMarkAbsentByTypeButtonDoesNotExist(
        'OTHER_ABSENCE'
      )
}

describe('Child mobile attendances', () => {
  test('Child free 5y free placement is suggested to mark absence types', async () => {
    await createPlacement('DAYCARE_FIVE_YEAR_OLDS')
    await checkAbsentTypeSelectionExistance(true)
  })

  test('Child free part time 5y free placement is suggested to mark absence types', async () => {
    await createPlacement('DAYCARE_PART_TIME_FIVE_YEAR_OLDS')
    await checkAbsentTypeSelectionExistance(true)
  })

  test('Child in paid daycare is not suggested to mark absence types', async () => {
    await createPlacement('DAYCARE')
    await checkAbsentTypeSelectionExistance(false)
  })
})
