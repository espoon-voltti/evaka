// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import CitizenMessagesPage from 'e2e-playwright/pages/citizen/citizen-messages'
import ChildInformationPage from 'e2e-playwright/pages/employee/child-information'
import MessagesPage from 'e2e-playwright/pages/employee/messages/messages-page'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { employeeLogin, enduserLogin } from 'e2e-playwright/utils/user'
import config from 'e2e-test-common/config'
import {
  insertDaycareGroupFixtures,
  insertGuardianFixtures,
  resetDatabase,
  upsertMessageAccounts
} from 'e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import { daycareGroupFixture, Fixture } from 'e2e-test-common/dev-api/fixtures'
import { UUID } from 'lib-common/types'
import { Page } from '../../utils/page'

let unitSupervisorPage: Page
let citizenPage: Page
let childId: UUID
let fixtures: AreaAndPersonFixtures

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  await Fixture.employee()
    .with({
      id: config.unitSupervisorAad,
      externalId: `espoo-ad:${config.unitSupervisorAad}`,
      firstName: 'Essi',
      lastName: 'Esimies',
      roles: []
    })
    .withDaycareAcl(fixtures.daycareFixture.id, 'UNIT_SUPERVISOR')
    .save()

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.enduserChildFixtureJari.id

  const daycarePlacementFixture = await Fixture.placement()
    .with({
      childId,
      unitId
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: daycarePlacementFixture.data.id,
      daycareGroupId: daycareGroupFixture.id
    })
    .save()
  await upsertMessageAccounts()
  await insertGuardianFixtures([
    {
      childId: childId,
      guardianId: fixtures.enduserGuardianFixture.id
    }
  ])
})

async function initSupervisorPage() {
  unitSupervisorPage = await Page.open()
  await employeeLogin(unitSupervisorPage, 'UNIT_SUPERVISOR')
}

async function initCitizenPage() {
  citizenPage = await Page.open()
  await enduserLogin(citizenPage)
}

describe('Sending and receiving messages', () => {
  describe('Interactions', () => {
    beforeEach(async () => {
      await Promise.all([initSupervisorPage(), initCitizenPage()])
    })

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

    test('Employee can send attachments', async () => {
      const title = 'Otsikko'
      const content = 'Testiviestin sisältö'

      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      await messagesPage.sendNewMessage(title, content, 2)

      await citizenPage.goto(config.enduserMessagesUrl)
      const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
      await citizenMessagesPage.assertThreadContent(title, content)
      await waitUntilEqual(
        () => citizenMessagesPage.getThreadAttachmentCount(),
        2
      )
    })

    test('Admin sends a message and blocked guardian does not get it', async () => {
      const title = 'Kielletty viesti'
      const content = 'Tämän ei pitäisi mennä perille'

      // Add child's guardian to block list
      const adminPage = await Page.open()
      await employeeLogin(adminPage, 'ADMIN')
      await adminPage.goto(`${config.employeeUrl}/child-information/${childId}`)
      const childInformationPage = new ChildInformationPage(adminPage)
      const blocklistSection = await childInformationPage.openCollapsible(
        'messageBlocklist'
      )
      await blocklistSection.addParentToBlockList(
        fixtures.enduserGuardianFixture.id
      )

      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      await messagesPage.sendNewMessage(title, content)

      await citizenPage.goto(config.enduserMessagesUrl)
      const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
      await citizenMessagesPage.assertInboxIsEmpty()
    })

    test('Citizen sends a message to the unit supervisor', async () => {
      const title = 'Otsikko'
      const content = 'Testiviestin sisältö'
      const receivers = ['Esimies Essi']
      await citizenPage.goto(config.enduserMessagesUrl)
      const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
      await citizenMessagesPage.sendNewMessage(title, content, receivers)

      await employeeLogin(unitSupervisorPage, 'UNIT_SUPERVISOR')
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      await messagesPage.openInbox(0)
      await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
      await messagesPage.openInbox(1)
      await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 0)
    })

    test('Citizen sends message to the unit supervisor and the group', async () => {
      const title = 'Otsikko'
      const content = 'Testiviestin sisältö'
      const receivers = ['Esimies Essi', 'Kosmiset vakiot (Henkilökunta)']
      await citizenPage.goto(config.enduserMessagesUrl)
      const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
      await citizenMessagesPage.sendNewMessage(title, content, receivers)

      await employeeLogin(unitSupervisorPage, 'UNIT_SUPERVISOR')
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      await messagesPage.openInbox(0)
      await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
      await messagesPage.openInbox(1)
      await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
    })
  })

  describe('Drafts', () => {
    beforeEach(async () => {
      await initSupervisorPage()
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

    test("A draft is not saved when it's discarded", async () => {
      const title = 'Luonnos'
      const content = 'Tässä luonnostellaan'

      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      await messagesPage.draftNewMessage(title, content)
      await messagesPage.discardMessage()
      await messagesPage.assertNoDrafts()
    })
  })
})
