// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import {
  testDaycare2,
  testDaycareGroup,
  testChild2,
  Fixture,
  uuidv4,
  testAdult2,
  testAdult,
  testChild,
  testDaycarePrivateVoucher,
  testDaycare,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createBackupCares,
  createDaycareGroups,
  createMessageAccounts,
  insertGuardians,
  resetServiceState
} from '../../generated/api-clients'
import { DevCareArea, DevEmployee } from '../../generated/api-types'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import ChildInformationPage from '../../pages/employee/child-information'
import MessagesPage from '../../pages/employee/messages/messages-page'
import { waitUntilEqual } from '../../utils'
import { KeycloakRealmClient } from '../../utils/keycloak'
import { Page } from '../../utils/page'
import {
  CitizenWeakAccount,
  citizenWeakAccount,
  employeeLogin,
  enduserLogin,
  enduserLoginWeak
} from '../../utils/user'

let unitSupervisorPage: Page
let citizenPage: Page
let childId: UUID
let unitSupervisor: DevEmployee
let account: CitizenWeakAccount
let careArea: DevCareArea
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
  await resetServiceState()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.daycare(testDaycarePrivateVoucher).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  await Fixture.person(testAdult2).saveAdult({
    updateMockVtjWithDependants: [testChild]
  })

  careArea = testCareArea
  await createDaycareGroups({ body: [testDaycareGroup] })

  const keycloak = await KeycloakRealmClient.createCitizenClient()
  await keycloak.deleteAllUsers()
  account = citizenWeakAccount(testAdult)
  await keycloak.createUser({ ...account, enabled: true })

  unitSupervisor = await Fixture.employeeUnitSupervisor(testDaycare.id).save()

  const unitId = testDaycare.id
  childId = testChild.id

  const daycarePlacementFixture = await Fixture.placement({
    childId,
    unitId,
    startDate: mockedDate,
    endDate: mockedDate.addYears(1)
  }).save()
  await Fixture.groupPlacement({
    daycarePlacementId: daycarePlacementFixture.id,
    daycareGroupId: testDaycareGroup.id,
    startDate: mockedDate,
    endDate: mockedDate.addYears(1)
  }).save()

  await Fixture.daycare({
    ...testDaycare2,
    areaId: careArea.id
  }).save()

  backupDaycareId = testDaycare2.id
  backupGroupFixtureId = uuidv4()
  await createDaycareGroups({
    body: [
      {
        id: backupGroupFixtureId,
        daycareId: backupDaycareId,
        name: 'Varayksikön ryhmä',
        startDate: LocalDate.of(2000, 1, 1),
        endDate: null,
        jamixCustomerNumber: null
      }
    ]
  })

  await createMessageAccounts()
  await insertGuardians({
    body: [
      {
        childId: childId,
        guardianId: testAdult.id
      }
    ]
  })
})

async function openSupervisorPage(mockedTime: HelsinkiDateTime) {
  unitSupervisorPage = await Page.open({
    mockedTime: mockedTime
  })
  await employeeLogin(unitSupervisorPage, unitSupervisor)
}

async function openCitizenPage(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({
    mockedTime: mockedTime
  })
  await enduserLogin(citizenPage, testAdult)
}

async function openCitizenPageWeak(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({
    mockedTime: mockedTime
  })
  await enduserLoginWeak(citizenPage, account)
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
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
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
        await Fixture.placement({
          childId: testChild2.id,
          unitId: testDaycarePrivateVoucher.id,
          startDate: mockedDate,
          endDate: mockedDate
        }).save()
        await createBackupCares({
          body: [
            {
              id: uuidv4(),
              childId: testChild2.id,
              unitId: testDaycare.id,
              groupId: testDaycareGroup.id,
              period: new FiniteDateRange(mockedDate, mockedDate)
            }
          ]
        })

        await insertGuardians({
          body: [
            {
              childId: testChild2.id,
              guardianId: testAdult.id
            }
          ]
        })

        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        let messagesPage = new MessagesPage(unitSupervisorPage)

        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage({
          ...defaultMessage,
          receivers: [testChild2.id]
        })
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        const sentMessagesPage = await messagesPage.openSentMessages()
        await sentMessagesPage.assertMessageParticipants(
          0,
          `${testChild2.lastName} ${testChild2.firstName}`
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
        await createBackupCares({
          body: [
            {
              id: uuidv4(),
              childId,
              unitId: backupDaycareId,
              groupId: backupGroupFixtureId,
              period: new FiniteDateRange(mockedDate, mockedDate)
            }
          ]
        })

        await insertGuardians({
          body: [
            {
              childId,
              guardianId: testAdult.id
            }
          ]
        })

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
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(message)
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
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage({
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
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
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
        await employeeLogin(adminPage, admin)

        await adminPage.goto(
          `${config.employeeUrl}/child-information/${childId}`
        )

        const childInformationPage = new ChildInformationPage(adminPage)
        const blocklistSection =
          await childInformationPage.openCollapsible('messageBlocklist')
        await blocklistSection.addParentToBlockList(testAdult.id)

        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage({ title, content })
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
        const daycarePlacementFixture = await Fixture.placement({
          childId: testChild2.id,
          unitId: testDaycare.id,
          startDate: mockedDate,
          endDate: mockedDate
        }).save()
        await Fixture.groupPlacement({
          daycarePlacementId: daycarePlacementFixture.id,
          daycareGroupId: testDaycareGroup.id,
          startDate: mockedDate,
          endDate: mockedDate
        }).save()
        await insertGuardians({
          body: [
            {
              childId: testChild2.id,
              guardianId: testAdult.id
            }
          ]
        })

        const recipients = ['Esimies Essi']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        const editor = await citizenMessagesPage.createNewMessage()
        await editor.assertChildrenSelectable([testChild.id, testChild2.id])

        // No recipients available before selecting a child
        await editor.assertNoRecipients()

        await editor.selectChildren([testChild2.id])
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
        const daycarePlacementFixture = await Fixture.placement({
          childId: testChild2.id,
          unitId: testDaycare.id,
          startDate: mockedDate,
          endDate: placementEndDate
        }).save()
        await Fixture.groupPlacement({
          daycarePlacementId: daycarePlacementFixture.id,
          daycareGroupId: testDaycareGroup.id,
          startDate: mockedDate,
          endDate: placementEndDate
        }).save()
        await insertGuardians({
          body: [
            {
              childId: testChild2.id,
              guardianId: testAdult.id
            }
          ]
        })

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
        const otherGuardian = testAdult2
        await insertGuardians({
          body: [{ childId, guardianId: otherGuardian.id }]
        })

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
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
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
          const messageEditor = await messagesPage.openMessageEditor()
          await messageEditor.sendNewMessage(defaultMessage)
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
      const messageEditor = await messagesPage.openMessageEditor()
      await messageEditor.draftNewMessage(defaultTitle, defaultContent)
      await messageEditor.closeButton.click()
      await messagesPage.assertDraftContent(defaultTitle, defaultContent)
    })

    test('A draft is not saved when a message is sent', async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      const messageEditor = await messagesPage.openMessageEditor()
      await messageEditor.draftNewMessage(defaultTitle, defaultContent)
      await messageEditor.sendButton.click()
      await messagesPage.assertNoDrafts()
    })

    test("A draft is not saved when it's discarded", async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      const messageEditor = await messagesPage.openMessageEditor()
      await messageEditor.draftNewMessage(defaultTitle, defaultContent)
      await messageEditor.discardButton.click()
      await messagesPage.assertNoDrafts()
    })
  })
})
