// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertBackupCareFixtures,
  insertDaycareGroupFixtures,
  insertGuardianFixtures,
  resetDatabase,
  runPendingAsyncJobs,
  upsertMessageAccounts
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  daycare2Fixture,
  daycareGroupFixture,
  enduserChildFixtureKaarina,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { CareArea, EmployeeDetail } from '../../dev-api/types'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import ChildInformationPage from '../../pages/employee/child-information'
import MessagesPage from '../../pages/employee/messages/messages-page'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
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

const mockedDate = LocalDate.of(2022, 5, 21)
const mockedDateAt10 = HelsinkiDateTime.fromLocal(
  mockedDate,
  LocalTime.of(10, 2)
)
const mockedDateAt11 = HelsinkiDateTime.fromLocal(
  mockedDate,
  LocalTime.of(11, 31)
)
const mockedDateAt12 = HelsinkiDateTime.fromLocal(
  mockedDate,
  LocalTime.of(12, 17)
)

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
      unitId,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: daycarePlacementFixture.data.id,
      daycareGroupId: daycareGroupFixture.id,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()

  await Fixture.daycare()
    .with(daycare2Fixture)
    .with({
      areaId: careArea.id
    })
    .save()

  backupDaycareId = daycare2Fixture.id
  backupGroupFixtureId = uuidv4()
  await insertDaycareGroupFixtures([
    {
      id: backupGroupFixtureId,
      daycareId: backupDaycareId,
      name: 'Varayksikön ryhmä',
      startDate: LocalDate.of(2000, 1, 1)
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

async function openSupervisorPage(mockedTime: HelsinkiDateTime) {
  unitSupervisorPage = await Page.open({
    mockedTime: mockedTime.toSystemTzDate()
  })
  await employeeLogin(unitSupervisorPage, unitSupervisor)
}

async function openCitizenPage(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({
    mockedTime: mockedTime.toSystemTzDate()
  })
  await enduserLogin(citizenPage)
}

async function openCitizenPageWeak(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({
    mockedTime: mockedTime.toSystemTzDate()
  })
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
    ['direct login', openCitizenPage] as const,
    ['weak login', openCitizenPageWeak] as const
  ]

  describe.each(initConfigurations)(
    'Interactions with %s',
    (_name, openCitizen) => {
      test('Unit supervisor sends message and citizen replies', async () => {
        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        let messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))
        await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)

        await openSupervisorPage(mockedDateAt12)
        messagesPage = new MessagesPage(unitSupervisorPage)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.receivedMessage.click()
        await messagesPage.assertMessageContent(1, defaultReply)
      })

      test('Unit supervisor sends a message to backup care child and citizen replies', async () => {
        await Fixture.placement()
          .with({
            childId: enduserChildFixtureKaarina.id,
            unitId: fixtures.daycareFixturePrivateVoucher.id,
            startDate: mockedDate,
            endDate: mockedDate
          })
          .save()
        await insertBackupCareFixtures([
          {
            id: uuidv4(),
            childId: enduserChildFixtureKaarina.id,
            unitId: fixtures.daycareFixture.id,
            groupId: daycareGroupFixture.id,
            period: {
              start: mockedDate,
              end: mockedDate
            }
          }
        ])

        await insertGuardianFixtures([
          {
            childId: enduserChildFixtureKaarina.id,
            guardianId: fixtures.enduserGuardianFixture.id
          }
        ])

        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        let messagesPage = new MessagesPage(unitSupervisorPage)

        await messagesPage.sendNewMessage({
          ...defaultMessage,
          receiver: enduserChildFixtureKaarina.id
        })
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await messagesPage.assertMessageIsSentForParticipants(
          0,
          `${enduserChildFixtureKaarina.lastName} ${enduserChildFixtureKaarina.firstName}`
        )

        await openCitizen(mockedDateAt11)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))
        await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)

        await openSupervisorPage(mockedDateAt12)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        messagesPage = new MessagesPage(unitSupervisorPage)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.receivedMessage.click()
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
              start: mockedDate,
              end: mockedDate
            }
          }
        ])

        await insertGuardianFixtures([
          {
            childId,
            guardianId: fixtures.enduserGuardianFixture.id
          }
        ])

        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.sendNewMessage(
          'Test message',
          'Test message content',
          [],
          ['Varayksikön ryhmä (Henkilökunta)']
        )
      })

      test('Unit supervisor sends an urgent message and citizen replies', async () => {
        const message = { ...defaultMessage, urgent: true }
        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        let messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.sendNewMessage(message)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(message)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))
        await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)

        await openSupervisorPage(mockedDateAt12)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        messagesPage = new MessagesPage(unitSupervisorPage)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.receivedMessage.click()
        await messagesPage.assertMessageContent(1, defaultReply)
      })

      test('Employee can send attachments', async () => {
        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.sendNewMessage({
          ...defaultMessage,
          attachmentCount: 2
        })
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await waitUntilEqual(
          () => citizenMessagesPage.getThreadAttachmentCount(),
          2
        )
      })

      test('Employee can discard thread reply', async () => {
        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        let messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))

        await openSupervisorPage(mockedDateAt12)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        messagesPage = new MessagesPage(unitSupervisorPage)
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

        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.sendNewMessage({ title, content })
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertInboxIsEmpty()
      })

      test('Citizen sends a message to the unit supervisor', async () => {
        const receivers = ['Esimies Essi']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          receivers
        )
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openSupervisorPage(mockedDateAt11)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.openInbox(0)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.openInbox(1)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 0)
      })

      test('Unit supervisor sees the name of the child in a message sent by citizen', async () => {
        const receivers = ['Esimies Essi']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          receivers
        )
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openSupervisorPage(mockedDateAt11)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.openInbox(0)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.assertReceivedMessageParticipantsContains(
          0,
          '(Karhula Jari)'
        )
      })
      test('The citizen must select the child that the message is in regards to', async () => {
        const daycarePlacementFixture = await Fixture.placement()
          .with({
            childId: enduserChildFixtureKaarina.id,
            unitId: fixtures.daycareFixture.id,
            startDate: mockedDate,
            endDate: mockedDate
          })
          .save()
        await Fixture.groupPlacement()
          .with({
            daycarePlacementId: daycarePlacementFixture.data.id,
            daycareGroupId: daycareGroupFixture.id,
            startDate: mockedDate,
            endDate: mockedDate
          })
          .save()
        await insertGuardianFixtures([
          {
            childId: enduserChildFixtureKaarina.id,
            guardianId: fixtures.enduserGuardianFixture.id
          }
        ])

        const recipients = ['Esimies Essi']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        const editor = await citizenMessagesPage.createNewMessage()
        await editor.assertChildrenSelectable([
          fixtures.enduserChildFixtureJari.id,
          enduserChildFixtureKaarina.id
        ])

        // No recipients available before selecting a child
        await editor.assertNoRecipients()

        await editor.selectChildren([enduserChildFixtureKaarina.id])
        await editor.selectRecipients(recipients)
        await editor.fillMessage(defaultTitle, defaultContent)
        await editor.sendMessage()
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openSupervisorPage(mockedDateAt11)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.openInbox(0)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.assertReceivedMessageParticipantsContains(
          0,
          '(Karhula Kaarina)'
        )
      })

      test('Messages cannot be sent after placement(s) end', async () => {
        const placementEndDate = mockedDate
        const daycarePlacementFixture = await Fixture.placement()
          .with({
            childId: enduserChildFixtureKaarina.id,
            unitId: fixtures.daycareFixture.id,
            startDate: mockedDate,
            endDate: placementEndDate
          })
          .save()
        await Fixture.groupPlacement()
          .with({
            daycarePlacementId: daycarePlacementFixture.data.id,
            daycareGroupId: daycareGroupFixture.id,
            startDate: mockedDate,
            endDate: placementEndDate
          })
          .save()
        await insertGuardianFixtures([
          {
            childId: enduserChildFixtureKaarina.id,
            guardianId: fixtures.enduserGuardianFixture.id
          }
        ])

        const dayAfterPlacementEnds = placementEndDate
          .addDays(1)
          .toHelsinkiDateTime(LocalTime.of(12, 0))

        await openCitizen(dayAfterPlacementEnds)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPageAfterPlacement = new CitizenMessagesPage(
          citizenPage
        )
        const editor =
          await citizenMessagesPageAfterPlacement.createNewMessage()
        await editor.assertChildrenSelectable([])
      })

      test('The guardian can select another guardian as an recipient', async () => {
        const otherGuardian = fixtures.enduserChildJariOtherGuardianFixture
        await insertGuardianFixtures([
          {
            childId,
            guardianId: otherGuardian.id
          }
        ])

        const recipients = ['Esimies Essi']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        const editor = await citizenMessagesPage.createNewMessage()
        await editor.selectRecipients(recipients)
        await editor
          .secondaryRecipient(
            `${otherGuardian.lastName} ${otherGuardian.firstName}`
          )
          .click()
        await editor.fillMessage(defaultTitle, defaultContent)
        await editor.sendMessage()
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openSupervisorPage(mockedDateAt11)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.openInbox(0)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.assertReceivedMessageParticipantsContains(
          0,
          `${otherGuardian.lastName} ${otherGuardian.firstName}`
        )
      })

      test('Citizen sends message to the unit supervisor and the group', async () => {
        const receivers = ['Esimies Essi', 'Kosmiset vakiot (Henkilökunta)']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          receivers
        )
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openSupervisorPage(mockedDateAt11)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.openInbox(0)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.openInbox(1)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
      })

      test('Citizen can discard message', async () => {
        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11)
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

      describe('Messages can be deleted / archived', () => {
        test('Unit supervisor sends message and citizen deletes the message', async () => {
          await openSupervisorPage(mockedDateAt10)
          await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
          const messagesPage = new MessagesPage(unitSupervisorPage)
          await messagesPage.sendNewMessage(defaultMessage)
          await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

          await openCitizen(mockedDateAt11)
          await citizenPage.goto(config.enduserMessagesUrl)
          const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
          await citizenMessagesPage.assertThreadContent(defaultMessage)
          await citizenMessagesPage.deleteFirstThread()
          await citizenMessagesPage.confirmThreadDeletion()
          await citizenMessagesPage.assertInboxIsEmpty()
        })
      })
    }
  )

  describe('Drafts', () => {
    test('A draft is saved correctly', async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      await messagesPage.draftNewMessage(defaultTitle, defaultContent)
      await messagesPage.closeMessageEditor()
      await messagesPage.assertDraftContent(defaultTitle, defaultContent)
    })

    test('A draft is not saved when a message is sent', async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      await messagesPage.draftNewMessage(defaultTitle, defaultContent)
      await messagesPage.sendEditedMessage()
      await messagesPage.assertNoDrafts()
    })

    test("A draft is not saved when it's discarded", async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      await messagesPage.draftNewMessage(defaultTitle, defaultContent)
      await messagesPage.discardMessage()
      await messagesPage.assertNoDrafts()
    })
  })

  describe('Undoing', () => {
    test('A new message can be undone by employees', async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      await messagesPage.sendNewMessage(defaultMessage)
      await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

      await openCitizenPage(mockedDateAt11)
      await citizenPage.goto(config.enduserMessagesUrl)
      const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
      await citizenMessagesPage.openFirstThread()
      await citizenMessagesPage.assertThreadContent(defaultMessage)

      await messagesPage.undoMessage()
      await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))
      await citizenPage.goto(config.enduserMessagesUrl)
      await citizenMessagesPage.assertInboxIsEmpty()
    })

    test('A new message with an attachment can be undone by employees', async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      await messagesPage.sendNewMessage({
        ...defaultMessage,
        attachmentCount: 1
      })
      await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

      await openCitizenPage(mockedDateAt11)
      await citizenPage.goto(config.enduserMessagesUrl)
      const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
      await citizenMessagesPage.openFirstThread()
      await citizenMessagesPage.assertThreadContent(defaultMessage)

      await messagesPage.undoMessage()
      await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))
      await citizenPage.goto(config.enduserMessagesUrl)
      await citizenMessagesPage.assertInboxIsEmpty()
    })

    test('A new reply to a thread can be undone by employees', async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      let messagesPage = new MessagesPage(unitSupervisorPage)
      await messagesPage.sendNewMessage(defaultMessage)
      await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

      await openCitizenPage(mockedDateAt11)
      await citizenPage.goto(config.enduserMessagesUrl)
      let citizenMessagesPage = new CitizenMessagesPage(citizenPage)
      await citizenMessagesPage.assertThreadContent(defaultMessage)
      await citizenMessagesPage.replyToFirstThread(defaultReply)
      await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)
      await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))

      await openSupervisorPage(mockedDateAt12)
      messagesPage = new MessagesPage(unitSupervisorPage)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      await messagesPage.openInbox(0)
      await messagesPage.openFirstThreadReplyEditor()
      await messagesPage.sendReplyButton.waitUntilVisible()
      await messagesPage.fillReplyContent(defaultContent)
      await messagesPage.sendReplyButton.click()
      await waitUntilTrue(() => messagesPage.sendReplyButton.disabled)
      await runPendingAsyncJobs(mockedDateAt12.addMinutes(1))

      await openCitizenPage(mockedDateAt12.addMinutes(1))
      await citizenPage.goto(config.enduserMessagesUrl)
      citizenMessagesPage = new CitizenMessagesPage(citizenPage)
      await citizenMessagesPage.openFirstThread()
      await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 3)

      await messagesPage.undoMessage()
      await citizenPage.goto(config.enduserMessagesUrl)
      await citizenMessagesPage.openFirstThread()
      await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)
    })
  })
})
