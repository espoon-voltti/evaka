// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import config from 'e2e-test-common/config'
import {
  clearBulletins,
  deleteEmployeeFixture,
  insertDaycareGroupFixtures,
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  insertEmployeeFixture,
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import EmployeeHome from '../../pages/employee/home'
import MessagesPage from '../../pages/employee/messaging/messages-page'
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
import { enduserRole } from '../../config/users'
import CitizenHomepage from '../../pages/citizen/citizen-homepage'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import ChildInformationPage from '../../pages/employee/child-information/child-information-page'

const home = new EmployeeHome()
const messagesPage = new MessagesPage()

const citizenHome = new CitizenHomepage()
const citizenMessages = new CitizenMessagesPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>
let daycarePlacementFixture: DaycarePlacement
let daycareGroupPlacementFixture: DaycareGroupPlacement

fixture('Sending and receiving bulletins')
  .meta({ type: 'regression', subType: 'bulletins' })
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
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
  })
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    await clearBulletins()
  })
  .after(async () => {
    await deleteEmployeeFixture(config.supervisorExternalId)
    await cleanUp()
  })

test('Supervisor sends bulletin and guardian reads it', async (t) => {
  const daycareId = fixtures.daycareFixture.id

  // login as a citizen first to init data in guardian table
  await t.useRole(enduserRole)

  await t.navigateTo(config.adminUrl)
  await home.login({
    aad: config.supervisorAad,
    roles: []
  })
  await home.navigateToMessages()

  await t
    .expect(messagesPage.unitsListUnit(daycareId).textContent)
    .contains(fixtures.daycareFixture.name)

  await t.click(messagesPage.unitsListUnit(daycareId))

  await messagesPage.createNewBulletin(
    'Alkuräjähdyksen päiväkoti',
    'Hello',
    'This is a test'
  )

  await t.useRole(enduserRole)

  await t.expect(citizenHome.nav.messages.textContent).contains('1')
  await t.click(citizenHome.nav.messages)

  const messageItem = citizenMessages.message(0)
  await t.expect(messageItem.textContent).contains('Hello')
  await t.click(messageItem)

  await t.expect(citizenMessages.messageReaderTitle.textContent).eql('Hello')
  await t
    .expect(await citizenMessages.messageReaderSender.textContent)
    .eql('Alkuräjähdyksen päiväkoti')
  await t
    .expect(citizenMessages.messageReaderContent.textContent)
    .eql('This is a test')
  await t.expect(citizenHome.nav.messages.textContent).notContains('1')
})

const employeeHome = new EmployeeHome()
const childInformationPage = new ChildInformationPage()

test('Admin sends bulletin and blocked guardian does not get it', async (t) => {
  // login as a citizen first to init data in guardian table
  await t.useRole(enduserRole)

  await t.navigateTo(config.adminUrl)
  await home.login({
    aad: config.supervisorAad,
    roles: ['ADMIN']
  })
  await home.navigateToMessages()

  const daycareId = fixtures.daycareFixture.id

  await employeeHome.navigateToChildInformation(
    fixtures.enduserChildFixtureJari.id
  )

  await childInformationPage.openChildMessageBlocklistCollapsible()
  await childInformationPage.clickBlockListForParent(
    fixtures.enduserGuardianFixture.id
  )

  await t.navigateTo(config.adminUrl)
  await home.navigateToMessages()

  await t
    .expect(messagesPage.unitsListUnit(daycareId).textContent)
    .contains(fixtures.daycareFixture.name)

  await t.click(messagesPage.unitsListUnit(daycareId))

  await messagesPage.createNewBulletin(
    'Alkuräjähdyksen päiväkoti',
    'Hello',
    'This is a test'
  )

  await t.useRole(enduserRole)
  await t.expect(citizenHome.nav.messages.textContent).notContains('1')
  await t.click(citizenHome.nav.messages)
  await t.expect(citizenMessages.bulletins.exists).notOk()
})
