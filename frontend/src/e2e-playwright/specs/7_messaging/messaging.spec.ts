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
  createDaycareGroupPlacementFixture
} from 'e2e-test-common/dev-api/fixtures'
import { UUID } from 'lib-common/types'
import { employeeLogin, enduserLogin } from 'e2e-playwright/utils/user'
import config from 'e2e-test-common/config'
import MessagesPage from 'e2e-playwright/pages/employee/messages/messages-page'
import CitizenMessagesPage from 'e2e-playwright/pages/citizen/citizen-messages'
import ChildInformationPage from 'e2e-playwright/pages/employee/child-information-page'
import { waitUntilEqual } from 'e2e-playwright/utils'

let adminPage: Page
let unitSupervisorPage: Page
let citizenPage: Page
let childId: UUID
let fixtures: AreaAndPersonFixtures

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

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
      headOfChildId: fixtures.enduserGuardianFixture.id,
      startDate: '2021-01-01',
      endDate: '2028-01-01'
    }
  ])

  adminPage = await (await newBrowserContext()).newPage()
  await employeeLogin(adminPage, 'ADMIN')
  unitSupervisorPage = await (await newBrowserContext()).newPage()
  await employeeLogin(unitSupervisorPage, 'UNIT_SUPERVISOR')
  citizenPage = await (await newBrowserContext()).newPage()
  await enduserLogin(citizenPage)
})
afterEach(async () => {
  await adminPage.close()
  await unitSupervisorPage.close()
  await citizenPage.close()
})

describe('Sending and receiving messages', () => {
  test('Unit supervisor sends message and citizen replies', async () => {
    const title = 'Otsikko'
    const content = 'Testiviestin sisältö'
    const reply = 'Testivastaus testiviestiin'

    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(unitSupervisorPage)
    await messagesPage.sendNewMessage(title, content)

    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await citizenMessagesPage.assertThreadContent(title, content)
    await citizenMessagesPage.replyToFirstThread(reply)
    await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)

    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
    await messagesPage.assertMessageContent(1, reply)
  })

  test('Admin sends a message and blocked guardian does not get it', async () => {
    const title = 'Kielletty viesti'
    const content = 'Tämän ei pitäisi mennä perille'

    // Add child's guardian to block list
    await adminPage.goto(`${config.employeeUrl}/child-information/${childId}`)
    const childInformationPage = new ChildInformationPage(adminPage)
    await childInformationPage.addParentToBlockList(
      fixtures.enduserGuardianFixture.id
    )

    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(unitSupervisorPage)
    await messagesPage.sendNewMessage(title, content)

    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 0)
  })

  test('A draft is saved correctly', async () => {
    const title = 'Luonnos'
    const content = 'Tässä luonnostellaan'

    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(unitSupervisorPage)
    await messagesPage.draftNewMessage(title, content)
    await messagesPage.closeMessageEditor()
    await messagesPage.assertDraftContent(title, content)
  })

  test('A draft is not saved when a message is sent', async () => {
    const title = 'Luonnos'
    const content = 'Tässä luonnostellaan'

    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(unitSupervisorPage)
    await messagesPage.draftNewMessage(title, content)
    await messagesPage.sendEditedMessage()
    await messagesPage.assertNoDrafts()
  })

  test('A draft is not saved when its discarded', async () => {
    const title = 'Luonnos'
    const content = 'Tässä luonnostellaan'

    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(unitSupervisorPage)
    await messagesPage.draftNewMessage(title, content)
    await messagesPage.discardMessage()
    await messagesPage.assertNoDrafts()
  })
})
