// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import config from 'e2e-test-common/config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertDefaultServiceNeedOptions,
  insertEmployeeFixture,
  resetDatabase,
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { newBrowserContext } from 'e2e-playwright/browser'
import { employeeLogin } from 'e2e-playwright/utils/user'
import { UUID } from 'lib-common/types'
import ChildInformationPage, {
  ChildAssistanceNeed
} from '../../pages/employee/child-information-page'

let page: Page
let childInformationPage: ChildInformationPage
let assistanceNeeds: ChildAssistanceNeed
let childId: UUID

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )
  await insertDaycarePlacementFixtures([daycarePlacementFixture])

  await insertEmployeeFixture({
    id: config.unitSupervisorAad,
    externalId: `espoo-ad:${config.unitSupervisorAad}`,
    email: 'essi.esimies@evaka.test',
    firstName: 'Essi',
    lastName: 'Esimies',
    roles: []
  })
  await setAclForDaycares(
    `espoo-ad:${config.unitSupervisorAad}`,
    fixtures.daycareFixture.id
  )

  page = await (await newBrowserContext()).newPage()
  await employeeLogin(page, 'UNIT_SUPERVISOR')
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
  await childInformationPage.assistanceCollapsible.click()
  assistanceNeeds = new ChildAssistanceNeed(page)
})

describe('Child Information assistance need functionality for unit supervisor', () => {
  test('assistance need can be added', async () => {
    await assistanceNeeds.createNewAssistanceNeed()
    await assistanceNeeds.setAssistanceNeedDescription('a description')
    await assistanceNeeds.confirmAssistanceNeed()
    await assistanceNeeds.assertAssistanceNeedDescription('a description')
  })
})
