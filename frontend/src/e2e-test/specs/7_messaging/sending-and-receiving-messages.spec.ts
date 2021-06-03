// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import {
  deleteMessages,
  insertDaycareGroupFixtures,
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  insertEmployeeFixture,
  resetDatabase,
  setAclForDaycares,
  upsertMessageAccounts
} from 'e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  createDaycareGroupPlacementFixture,
  createDaycarePlacementFixture,
  daycareGroupFixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import {
  DaycareGroupPlacement,
  DaycarePlacement
} from 'e2e-test-common/dev-api/types'
import MessagesPage from '../../pages/employee/messaging/messages-page'
import { employeeLogin, enduserRole } from '../../config/users'
import ChildInformationPage from '../../pages/employee/child-information/child-information-page'
import EmployeeHome from '../../pages/employee/home'
import { logConsoleMessages } from '../../utils/fixture'
import CitizenHomepage from '../../pages/citizen/citizen-homepage'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'

const home = new EmployeeHome()
const citizenHome = new CitizenHomepage()
const messagesPage = new MessagesPage()
const citizenMessagesPage = new CitizenMessagesPage()
const childInformationPage = new ChildInformationPage()

let fixtures: AreaAndPersonFixtures
let daycarePlacementFixture: DaycarePlacement
let daycareGroupPlacementFixture: DaycareGroupPlacement

fixture('Sending and receiving bulletins')
  .meta({ type: 'regression', subType: 'bulletins' })
  .beforeEach(async () => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()
    await insertEmployeeFixture({
      externalId: config.supervisorExternalId,
      firstName: 'Seppo',
      lastName: 'Sorsa',
      email: 'seppo.sorsa@espoo.fi',
      roles: []
    })
    await setAclForDaycares(
      config.supervisorExternalId,
      fixtures.daycareFixture.id
    )
    await insertDaycareGroupFixtures([daycareGroupFixture])

    daycarePlacementFixture = createDaycarePlacementFixture(
      uuidv4(),
      fixtures.enduserChildFixtureJari.id,
      fixtures.daycareFixture.id
    )
    await insertDaycarePlacementFixtures([daycarePlacementFixture])

    daycareGroupPlacementFixture = createDaycareGroupPlacementFixture(
      daycarePlacementFixture.id,
      daycareGroupFixture.id
    )
    await insertDaycareGroupPlacementFixtures([daycareGroupPlacementFixture])
    await upsertMessageAccounts()
  })
  .afterEach(logConsoleMessages)

test('Supervisor sends a message and guardian reads it', async (t) => {
  const title = 'Testiviesti'
  const content = 'Testiviestin sisältö'

  // login as a citizen first to init data in guardian table
  await t.useRole(enduserRole)

  await employeeLogin(
    t,
    { aad: config.supervisorAad, roles: [] },
    config.adminUrl
  )
  await home.navigateToMessages()
  await messagesPage.sendNewMessage(title, content)
  await t.useRole(enduserRole)
  await t.click(citizenHome.nav.messages)
  await t.click(citizenMessagesPage.thread(0))
  await t.expect(citizenMessagesPage.messageReaderTitle.textContent).eql(title)
  await t
    .expect(citizenMessagesPage.messageReaderContent.textContent)
    .eql(content)
  await deleteMessages()
})

test('Admin sends a message and blocked guardian does not get it', async (t) => {
  const title = 'Kielletty viesti'
  const content = 'Tämän ei pitäisi mennä perille'

  // login as a citizen first to init data in guardian table
  await t.useRole(enduserRole)

  await employeeLogin(
    t,
    { aad: config.supervisorAad, roles: ['ADMIN'] },
    config.adminUrl
  )
  await home.navigateToMessages()

  await home.navigateToChildInformation(fixtures.enduserChildFixtureJari.id)

  await childInformationPage.openChildMessageBlocklistCollapsible()
  await childInformationPage.clickBlockListForParent(
    fixtures.enduserGuardianFixture.id
  )

  await t.navigateTo(config.adminUrl)
  await home.navigateToMessages()
  await messagesPage.sendNewMessage(title, content)
  await t.useRole(enduserRole)
  await t.click(citizenHome.nav.messages)
  await t.expect(citizenMessagesPage.threads.count).eql(0)
})

test('A draft is saved correctly', async (t) => {
  const title = 'Luonnos'
  const content = 'Tässä luonnostellaan'

  // login as a citizen first to init data in guardian table
  await t.useRole(enduserRole)

  await employeeLogin(
    t,
    { aad: config.supervisorAad, roles: [] },
    config.adminUrl
  )
  await home.navigateToMessages()
  await messagesPage.draftNewMessage(title, content)
  await t.click(messagesPage.closeEditorBtn)
  await t.click(messagesPage.draftBox(1))
  await t.click(messagesPage.draftMessageRow(0))
  await t.expect(messagesPage.messageEditorTitle.value).eql(title)
  await t.expect(messagesPage.messageEditorContent.value).eql(content)
})

test('A draft is not saved when a message is sent', async (t) => {
  const title = 'Viesti'
  const content = 'Tämä ei tallennu, koska viesti lähetetään'

  // login as a citizen first to init data in guardian table
  await t.useRole(enduserRole)

  await t.navigateTo(config.adminUrl)
  await employeeLogin(
    t,
    { aad: config.supervisorAad, roles: [] },
    config.adminUrl
  )
  await home.navigateToMessages()
  await messagesPage.draftNewMessage(title, content)
  await t.click(messagesPage.sendMessageBtn)
  await t.click(messagesPage.draftBox(1))
  await t.expect(messagesPage.draftMessageRows.count).eql(0)
})

test('A draft is not saved when its discarded', async (t) => {
  const title = 'Luonnos'
  const content = 'Tämä ei tallennu, koska luonnos hylätään'

  // login as a citizen first to init data in guardian table
  await t.useRole(enduserRole)

  await employeeLogin(
    t,
    { aad: config.supervisorAad, roles: [] },
    config.adminUrl
  )
  await home.navigateToMessages()
  await messagesPage.draftNewMessage(title, content)
  await t.click(messagesPage.discardDraftBtn)
  await t.click(messagesPage.draftBox(1))
  await t.expect(messagesPage.draftMessageRows.count).eql(0)
})
