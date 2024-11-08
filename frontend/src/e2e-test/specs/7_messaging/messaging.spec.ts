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
import {
  DevCareArea,
  DevEmployee,
  DevPlacement
} from '../../generated/api-types'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
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
let daycarePlacementFixture: DevPlacement
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

  unitSupervisor = await Fixture.employee({
    firstName: 'Essi',
    lastName: 'Esimies'
  })
    .unitSupervisor(testDaycare.id)
    .save()

  const unitId = testDaycare.id
  childId = testChild.id

  daycarePlacementFixture = await Fixture.placement({
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
          ['Varayksikön ryhmä (Henkilökunta)'],
          false
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

      test('Citizen sends a message to the unit supervisor', async () => {
        const receivers = ['Esimies Essi']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          receivers,
          false
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

      test('Citizen sends a message to the unit supervisor before the placement, both will reply', async () => {
        const tenDaysbeforePlacementAt10 = HelsinkiDateTime.fromLocal(
          mockedDate.addDays(-10),
          LocalTime.of(10, 2)
        )
        const receivers = ['Esimies Essi']
        await openCitizen(tenDaysbeforePlacementAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          receivers,
          false
        )
        await runPendingAsyncJobs(tenDaysbeforePlacementAt10.addMinutes(1))

        const tenDaysBeforePlacementAt11 = HelsinkiDateTime.fromLocal(
          mockedDate.addDays(-10),
          LocalTime.of(11, 31)
        )
        await openSupervisorPage(tenDaysBeforePlacementAt11)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await messagesPage.openInbox(0)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.openFirstThreadReplyEditor()
        await messagesPage.fillReplyContent(defaultReply)
        await messagesPage.sendReplyButton.click()
        await messagesPage.sendReplyButton.waitUntilHidden()
        await runPendingAsyncJobs(tenDaysBeforePlacementAt11.addMinutes(1))

        const tenDaysBeforePlacementAt12 = HelsinkiDateTime.fromLocal(
          mockedDate.addDays(-10),
          LocalTime.of(12, 13)
        )
        await openCitizen(tenDaysBeforePlacementAt12)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPageLater = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPageLater.openFirstThread()
        await waitUntilEqual(
          () => citizenMessagesPageLater.getMessageCount(),
          2
        )
        await citizenMessagesPageLater.replyToFirstThread(defaultReply)
      })

      test('Citizen can send a message and receive a notification on success', async () => {
        const receivers = ['Esimies Essi']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          receivers,
          false
        )
        await citizenMessagesPage.assertAriaLiveExistsAndIncludesNotification()
        await citizenMessagesPage.assertTimedNotification(
          'message-sent-notification',
          'Viesti lähetetty'
        )
        await citizenMessagesPage.assertNewMessageButtonIsFocused()
      })

      test('Citizen with shift care child can send attachments', async () => {
        const serviceNeedOption = await Fixture.serviceNeedOption({
          validPlacementType: daycarePlacementFixture.type,
          defaultOption: false
        }).save()
        await Fixture.serviceNeed({
          placementId: daycarePlacementFixture.id,
          optionId: serviceNeedOption.id,
          confirmedBy: unitSupervisor.id,
          shiftCare: 'FULL',
          startDate: daycarePlacementFixture.startDate,
          endDate: daycarePlacementFixture.endDate
        }).save()

        const receivers = ['Esimies Essi']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          receivers,
          true
        )
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openSupervisorPage(mockedDateAt11)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.receivedMessage.click()
        await messagesPage.assertMessageContent(0, defaultContent)
        await waitUntilEqual(() => messagesPage.getThreadAttachmentCount(), 1)
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
          receivers,
          false
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
          receivers,
          false
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
        await citizenMessagesPage.messageReplyContent.fill(defaultContent)
        await citizenMessagesPage.discardReplyEditor()
        await citizenMessagesPage.discardMessageButton.waitUntilHidden()
        await citizenMessagesPage.openFirstThreadReplyEditor()
        await citizenMessagesPage.messageReplyContent.assertTextEquals('')
      })

      test('Citizen can reply to a thread and receive a notification on success', async () => {
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
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await citizenMessagesPage.assertAriaLiveExistsAndIncludesNotification()
        await citizenMessagesPage.assertTimedNotification(
          'message-sent-notification',
          'Viesti lähetetty'
        )
      })

      test('Citizen session is kept alive as long as user keeps typing', async () => {
        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11)

        await citizenPage.goto(config.enduserMessagesUrl)
        await citizenPage.page.evaluate(() => {
          if (window.evaka) window.evaka.keepSessionAliveThrottleTime = 300 // Set to 300ms for tests
        })
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)

        const initialExpiry = await citizenMessagesPage.getSessionExpiry()

        await citizenMessagesPage.startReplyToFirstThread()

        const slowTypedText =
          'Olen aika hidas kirjoittamaan näitä viestejä, mutta yritän parhaani: Lapseni ovat sitä ja tätä...'

        const msDelayPerCharToTypeSlowly = 1000 / slowTypedText.length
        const authStatusRequests: string[] = []
        citizenPage.page.on('request', (request) => {
          if (request.url().includes('/api/application/auth/status')) {
            authStatusRequests.push(request.url())
          }
        })
        // typing this takes so long that session keepalive mechanism should renew the session
        await citizenMessagesPage.messageReplyContent.locator.pressSequentially(
          slowTypedText,
          {
            delay: msDelayPerCharToTypeSlowly
          }
        )

        const finalExpiry = await citizenMessagesPage.getSessionExpiry()
        expect(authStatusRequests.length).toBeGreaterThanOrEqual(3)
        expect(finalExpiry).toBeGreaterThan(initialExpiry + 0.3)
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
        test('Citizen deletes a message thread and receives a notification on success', async () => {
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
          await citizenMessagesPage.assertTimedNotification(
            'thread-deleted-notification',
            'Viestiketju poistettu'
          )
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
