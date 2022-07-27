// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertBackupCareFixtures,
  insertDaycareGroupFixtures,
  insertGuardianFixtures,
  resetDatabase,
  upsertMessageAccounts
} from '../../dev-api'
import type { AreaAndPersonFixtures } from '../../dev-api/data-init'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  daycare2Fixture,
  daycareGroupFixture,
  enduserChildFixtureKaarina,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import type { CareArea, EmployeeDetail } from '../../dev-api/types'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import ChildInformationPage from '../../pages/employee/child-information'
import MessagesPage from '../../pages/employee/messages/messages-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin, enduserLoginWeak } from '../../utils/user'

let unitSupervisorPage: Page
let citizenPage: Page
let childId: UUID
let unitSupervisor: EmployeeDetail
let fixtures: AreaAndPersonFixtures
let careArea: CareArea
let backupDaycareId: UUID
let backupGroupFixtureId: UUID

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  careArea = fixtures.careAreaFixture
  await insertDaycareGroupFixtures([daycareGroupFixture])

  unitSupervisor = (
    await Fixture.employeeUnitSupervisor(fixtures.daycareFixture.id).save()
  ).data

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

  await Fixture.daycare()
    .with(daycare2Fixture)
    .with({
      careAreaId: careArea.id
    })
    .save()

  backupDaycareId = daycare2Fixture.id
  backupGroupFixtureId = uuidv4()
  await insertDaycareGroupFixtures([
    {
      id: backupGroupFixtureId,
      daycareId: backupDaycareId,
      name: 'Varayksikön ryhmä',
      startDate: '2000-01-01'
    }
  ])

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
  await employeeLogin(unitSupervisorPage, unitSupervisor)
}

async function initCitizenPage() {
  citizenPage = await Page.open()
  await enduserLogin(citizenPage)
}

async function initCitizenPageWeak() {
  citizenPage = await Page.open()
  await enduserLoginWeak(citizenPage)
}

const defaultTitle = 'Otsikko'
const defaultContent = 'Testiviestin sisältö'
const defaultReply = 'Testivastaus testiviestiin'

const defaultMessage = {
  title: 'Otsikko',
  content: 'Testiviestin sisältö'
}

describe('Sending and receiving messages', () => {
  const initConfigurations = [
    { init: initCitizenPage, name: 'direct login' },
    { init: initCitizenPageWeak, name: 'weak login' }
  ]

  for (const configuration of initConfigurations) {
    describe(`Interactions with ${configuration.name}`, () => {
      beforeEach(async () => {
        await Promise.all([initSupervisorPage(), configuration.init()])
      })

      test('Unit supervisor sends message and citizen replies', async () => {
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.sendNewMessage(defaultMessage)

        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)

        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.assertMessageContent(1, defaultReply)
      })

      test('Unit supervisor sends a message to backup care child and citizen replies', async () => {
        await insertBackupCareFixtures([
          {
            id: uuidv4(),
            childId: enduserChildFixtureKaarina.id,
            unitId: fixtures.daycareFixture.id,
            groupId: daycareGroupFixture.id,
            period: {
              start: LocalDate.todayInSystemTz().formatIso(),
              end: LocalDate.todayInSystemTz().formatIso()
            }
          }
        ])

        await insertGuardianFixtures([
          {
            childId: enduserChildFixtureKaarina.id,
            guardianId: fixtures.enduserGuardianFixture.id
          }
        ])

        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)

        await messagesPage.sendNewMessage({
          ...defaultMessage,
          receiver: `${enduserChildFixtureKaarina.firstName} ${enduserChildFixtureKaarina.lastName}`
        })

        await messagesPage.assertMessageIsSentForParticipants(
          0,
          `${enduserChildFixtureKaarina.firstName} ${enduserChildFixtureKaarina.lastName} `
        )

        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)

        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.assertMessageContent(1, defaultReply)
      })

      test('Citizen sends a message to backup care child', async () => {
        await insertBackupCareFixtures([
          {
            id: uuidv4(),
            childId,
            unitId: backupDaycareId,
            groupId: backupGroupFixtureId,
            period: {
              start: LocalDate.todayInSystemTz().formatIso(),
              end: LocalDate.todayInSystemTz().formatIso()
            }
          }
        ])

        await insertGuardianFixtures([
          {
            childId,
            guardianId: fixtures.enduserGuardianFixture.id
          }
        ])

        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.sendNewMessage(
          'Test message',
          'Test message content',
          ['Varayksikön ryhmä (Henkilökunta)']
        )
      })

      test('Unit supervisor sends an urgent message and citizen replies', async () => {
        const message = { ...defaultMessage, urgent: true }
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.sendNewMessage(message)

        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(message)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)

        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.assertMessageContent(1, defaultReply)
      })

      test('Employee can send attachments', async () => {
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.sendNewMessage({
          ...defaultMessage,
          attachmentCount: 2
        })

        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await waitUntilEqual(
          () => citizenMessagesPage.getThreadAttachmentCount(),
          2
        )
      })

      test('Employee can discard thread reply', async () => {
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.sendNewMessage(defaultMessage)

        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)

        await messagesPage.openInbox(0)

        await messagesPage.openFirstThreadReplyEditor()
        await messagesPage.discardMessageButton.waitUntilVisible()
        await messagesPage.fillReplyContent(defaultContent)
        await messagesPage.discardReplyEditor()
        await messagesPage.discardMessageButton.waitUntilHidden()
        await messagesPage.openReplyEditor()
        await messagesPage.assertReplyContentIsEmpty()
      })

      test('Admin sends a message and blocked guardian does not get it', async () => {
        const title = 'Kielletty viesti'
        const content = 'Tämän ei pitäisi mennä perille'

        // Add child's guardian to block list
        const admin = await Fixture.employeeAdmin().save()
        const adminPage = await Page.open()
        await employeeLogin(adminPage, admin.data)

        await adminPage.goto(
          `${config.employeeUrl}/child-information/${childId}`
        )

        const childInformationPage = new ChildInformationPage(adminPage)
        const blocklistSection = await childInformationPage.openCollapsible(
          'messageBlocklist'
        )
        await blocklistSection.addParentToBlockList(
          fixtures.enduserGuardianFixture.id
        )

        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.sendNewMessage({ title, content })

        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertInboxIsEmpty()
      })

      test('Citizen sends a message to the unit supervisor', async () => {
        const receivers = ['Esimies Essi']
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          receivers
        )

        await employeeLogin(unitSupervisorPage, unitSupervisor)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.openInbox(0)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.openInbox(1)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 0)
      })

      test('Citizen sends message to the unit supervisor and the group', async () => {
        const receivers = ['Esimies Essi', 'Kosmiset vakiot (Henkilökunta)']
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          receivers
        )

        await employeeLogin(unitSupervisorPage, unitSupervisor)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.openInbox(0)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.openInbox(1)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
      })

      test('Citizen can discard message', async () => {
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.sendNewMessage(defaultMessage)

        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.openFirstThreadReplyEditor()
        await citizenMessagesPage.discardMessageButton.waitUntilVisible()
        await citizenMessagesPage.fillReplyContent(defaultContent)
        await citizenMessagesPage.discardReplyEditor()
        await citizenMessagesPage.discardMessageButton.waitUntilHidden()
        await citizenMessagesPage.openFirstThreadReplyEditor()
        await citizenMessagesPage.assertReplyContentIsEmpty()
      })
    })

    describe('Drafts', () => {
      beforeEach(async () => {
        await initSupervisorPage()
      })
      test('A draft is saved correctly', async () => {
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.draftNewMessage(defaultTitle, defaultContent)
        await messagesPage.closeMessageEditor()
        await messagesPage.assertDraftContent(defaultTitle, defaultContent)
      })

      test('A draft is not saved when a message is sent', async () => {
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.draftNewMessage(defaultTitle, defaultContent)
        await messagesPage.sendEditedMessage()
        await messagesPage.assertNoDrafts()
      })

      test("A draft is not saved when it's discarded", async () => {
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.draftNewMessage(defaultTitle, defaultContent)
        await messagesPage.discardMessage()
        await messagesPage.assertNoDrafts()
      })
    })
  }
})
