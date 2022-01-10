// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import ChildInformationPage, {
  VasuAndLeopsSection
} from 'e2e-playwright/pages/employee/child-information'
import { employeeLogin } from 'e2e-playwright/utils/user'
import config from 'e2e-test-common/config'
import {
  insertDaycarePlacementFixtures,
  insertDefaultServiceNeedOptions,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { EmployeeDetail } from 'e2e-test-common/dev-api/types'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { Page } from '../../utils/page'

let page: Page
let admin: EmployeeDetail
let childInformationPage: ChildInformationPage
let childId: UUID

beforeAll(async () => {
  await resetDatabase()

  admin = (await Fixture.employeeAdmin().save()).data

  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()

  const unitId = fixtures.preschoolFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const preschooldPlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId,
    LocalDate.today().formatIso(),
    LocalDate.today().addYears(1).formatIso(),
    'PRESCHOOL'
  )

  await insertDaycarePlacementFixtures([preschooldPlacementFixture])
})

describe('Child Information - Leops documents section', () => {
  let section: VasuAndLeopsSection
  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${childId}`)
    childInformationPage = new ChildInformationPage(page)
  })

  test('Can add a new vasu document', async () => {
    await page.pause()
    section = await childInformationPage.openCollapsible('vasuAndLeops')
    await section.addNew()
  })
})
