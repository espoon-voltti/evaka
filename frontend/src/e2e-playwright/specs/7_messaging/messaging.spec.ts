// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
  insertEmployeeFixture,
  insertParentshipFixtures,
  resetDatabase,
  setAclForDaycares,
  upsertMessageAccounts
} from 'e2e-test-common/dev-api'
import {
  daycareGroupFixture,
  createDaycarePlacementFixture,
  uuidv4,
  createDaycareGroupPlacementFixture,
  enduserGuardianFixture
} from 'e2e-test-common/dev-api/fixtures'
import { UUID } from 'lib-common/types'
import { employeeLogin, enduserLogin } from 'e2e-playwright/utils/user'
import config from 'e2e-test-common/config'
import MessagesPage from 'e2e-playwright/pages/employee/messages/messages-page'
import CitizenMessagesPage from 'e2e-playwright/pages/citizen/citizen-messages'
import { waitUntilEqual } from 'e2e-playwright/utils'

let page: Page
let childId: UUID
let fixtures: AreaAndPersonFixtures

beforeAll(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  await insertEmployeeFixture({
    id: config.unitSupervisorAad,
    externalId: `espoo-ad:${config.unitSupervisorAad}`,
    email: 'essi.esimies@espoo.fi',
    firstName: 'Essi',
    lastName: 'Esimies',
    roles: []
  })
  await setAclForDaycares(
    `espoo-ad:${config.unitSupervisorAad}`,
    fixtures.daycareFixture.id,
    'UNIT_SUPERVISOR'
  )

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.enduserChildFixtureJari.id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )
  await insertDaycarePlacementFixtures([daycarePlacementFixture])
  const groupPlacementFixture = createDaycareGroupPlacementFixture(
    daycarePlacementFixture.id,
    daycareGroupFixture.id
  )
  await insertDaycareGroupPlacementFixtures([groupPlacementFixture])
  await upsertMessageAccounts()
  await insertParentshipFixtures([
    {
      childId: childId,
      headOfChildId: enduserGuardianFixture.id,
      startDate: '2021-01-01',
      endDate: '2028-01-01'
    }
  ])
})
beforeEach(async () => {
  page = await (await newBrowserContext()).newPage()
})
afterEach(async () => {
  await page.close()
})

describe('Sending and receiving messages', () => {
  test('Unit supervisor sends message and citizen replies', async () => {
    const title = 'Otsikko'
    const content = 'Testiviestin sisältö'
    const reply = 'Testivastaus testiviestiin'

    await employeeLogin(page, 'UNIT_SUPERVISOR')
    await page.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(page)
    await messagesPage.sendNewMessage(title, content)

    await enduserLogin(page)
    await page.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(page)
    await citizenMessagesPage.replyToFirstThread(reply)
    await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)

    await page.goto(`${config.employeeUrl}/messages`)
    await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
  })
})
