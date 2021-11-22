// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import config from 'e2e-test-common/config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertVasuTemplateFixture,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import ChildInformationPage, {
  VasuAndLeopsSection
} from 'e2e-playwright/pages/employee/child-information'
import { newBrowserContext } from 'e2e-playwright/browser'
import { employeeLogin } from 'e2e-playwright/utils/user'
import { UUID } from 'lib-common/types'

let page: Page
let childInformationPage: ChildInformationPage
let childId: UUID

jest.setTimeout(100000)

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )
  await insertDaycarePlacementFixtures([daycarePlacementFixture])
  await insertVasuTemplateFixture()

  page = await (await newBrowserContext()).newPage()
  await employeeLogin(page, 'ADMIN')
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
})

describe('Child Information - Vasu documents', () => {
  let section: VasuAndLeopsSection
  beforeEach(async () => {
    section = await childInformationPage.openCollapsible('vasuAndLeops')
  })

  test('Can add a new vasu document', async () => {
    await section.addNew()
  })
})
