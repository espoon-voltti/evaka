// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'fs'

import FiniteDateRange from 'lib-common/finite-date-range'
import {
  DaycareId,
  GroupId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { evakaUserId, randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import {
  Fixture,
  testAdult,
  testAdult2,
  testCareArea,
  testChild,
  testChild2,
  testChildRestricted,
  testDaycare,
  testDaycare2,
  testDaycareGroup,
  testDaycarePrivateVoucher
} from '../../dev-api/fixtures'
import {
  addAclRoleForDaycare,
  createDaycareGroups,
  createMessageAccounts,
  insertGuardians,
  resetServiceState,
  upsertWeakCredentials
} from '../../generated/api-clients'
import {
  DevCareArea,
  DevEmployee,
  DevPlacement
} from '../../generated/api-types'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import MessagesPage from '../../pages/employee/messages/messages-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin, enduserLoginWeak } from '../../utils/user'

let unitSupervisorPage: Page
let citizenPage: Page
let childId: PersonId
let unitSupervisor: DevEmployee
let careArea: DevCareArea
let daycarePlacementFixture: DevPlacement
let backupDaycareId: DaycareId
let backupGroupFixtureId: GroupId

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

const credentials = {
  username: 'test@example.com',
  password: 'TestPassword456!'
}
beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await testDaycarePrivateVoucher.save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2, testChildRestricted]
  }).save()
  await testAdult2.saveAdult({
    updateMockVtjWithDependants: [testChild]
  })

  careArea = testCareArea
  await createDaycareGroups({ body: [testDaycareGroup] })

  await upsertWeakCredentials({
    id: testAdult.id,
    body: credentials
  })

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
  backupGroupFixtureId = randomId()
  await createDaycareGroups({
    body: [
      {
        id: backupGroupFixtureId,
        daycareId: backupDaycareId,
        name: 'Varayksikön ryhmä',
        startDate: LocalDate.of(2000, 1, 1),
        endDate: null,
        jamixCustomerNumber: null,
        aromiCustomerId: null,
        nekkuCustomerNumber: null
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
    mockedTime: mockedTime,
    ignoreHTTPSErrors: true
  })
  await enduserLogin(citizenPage, testAdult)
}

async function openCitizenPageWeak(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({
    mockedTime: mockedTime,
    ignoreHTTPSErrors: true
  })
  await enduserLoginWeak(citizenPage, credentials)
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
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
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
        await Fixture.backupCare({
          childId: testChild2.id,
          unitId: testDaycare.id,
          groupId: testDaycareGroup.id,
          period: new FiniteDateRange(mockedDate, mockedDate)
        }).save()

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
          recipientKeys: [`${testChild2.id}+false`]
        })
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        const sentMessagesPage = await messagesPage.openSentMessages()
        await sentMessagesPage.assertMessageParticipants(
          0,
          `${testChild2.lastName} ${testChild2.firstName}`
        )

        await openCitizen(mockedDateAt11)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
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
        await Fixture.backupCare({
          childId,
          unitId: backupDaycareId,
          groupId: backupGroupFixtureId,
          period: new FiniteDateRange(mockedDate, mockedDate)
        }).save()

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
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
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
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
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
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await waitUntilEqual(
          () => citizenMessagesPage.getThreadAttachmentCount(),
          2
        )

        // Download and verify first attachment
        await citizenMessagesPage.openFirstThread()
        const downloadedContent =
          await citizenMessagesPage.downloadFirstAttachment()
        const originalContent = await fs.promises.readFile(
          'src/e2e-test/assets/test_file.odt'
        )
        expect(downloadedContent).toEqual(originalContent)
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
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
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
        const recipients = ['Esimies Essi']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          recipients,
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
        const recipients = ['Esimies Essi']
        await openCitizen(tenDaysbeforePlacementAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          recipients,
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
        const citizenMessagesPageLater = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPageLater.openFirstThread()
        await waitUntilEqual(
          () => citizenMessagesPageLater.getMessageCount(),
          2
        )
        await citizenMessagesPageLater.replyToFirstThread(defaultReply)
      })

      test('Citizen can send a message and receive a notification on success', async () => {
        const recipients = ['Esimies Essi']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          recipients,
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
          confirmedBy: evakaUserId(unitSupervisor.id),
          shiftCare: 'FULL',
          startDate: daycarePlacementFixture.startDate,
          endDate: daycarePlacementFixture.endDate
        }).save()

        const recipients = ['Esimies Essi']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          recipients,
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
        const recipients = ['Esimies Essi']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          recipients,
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
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
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

      test('Recipients are visible and selectable only when they are available for all selected children and in the same unit', async () => {
        // Child 2 is in a different group
        const daycarePlacement2 = await Fixture.placement({
          childId: testChild2.id,
          unitId: testDaycare.id,
          startDate: mockedDate,
          endDate: mockedDate
        }).save()
        const daycareGroup2 = await Fixture.daycareGroup({
          daycareId: testDaycare.id,
          name: 'Toinen ryhmä'
        }).save()
        await Fixture.groupPlacement({
          daycarePlacementId: daycarePlacement2.id,
          daycareGroupId: daycareGroup2.id,
          startDate: mockedDate,
          endDate: mockedDate
        }).save()

        // Child 3 (testChildRestricted) is in a different unit
        await addAclRoleForDaycare({
          daycareId: testDaycare2.id,
          body: {
            externalId: unitSupervisor.externalId!,
            role: 'UNIT_SUPERVISOR'
          }
        })
        const daycarePlacementFixture3 = await Fixture.placement({
          childId: testChildRestricted.id,
          unitId: testDaycare2.id,
          startDate: mockedDate,
          endDate: mockedDate
        }).save()
        const daycareGroup3 = await Fixture.daycareGroup({
          daycareId: testDaycare2.id,
          name: 'Kolmas ryhmä'
        }).save()
        await Fixture.groupPlacement({
          daycarePlacementId: daycarePlacementFixture3.id,
          daycareGroupId: daycareGroup3.id,
          startDate: mockedDate,
          endDate: mockedDate
        }).save()
        await createMessageAccounts()

        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        const editor = await citizenMessagesPage.createNewMessage()
        await editor.assertChildrenSelectable([
          testChild.id,
          testChild2.id,
          testChildRestricted.id
        ])

        // No recipients available before selecting a child
        await editor.assertNoRecipients()

        // Selecting child 1 makes the supervisor and group selectable
        await editor.selectChildren([testChild.id])
        await editor.assertRecipients([
          'Esimies Essi',
          'Kosmiset vakiot (Henkilökunta)'
        ])

        // Selecting child 2 makes both groups selectable since they are in the same unit
        await editor.selectChildren([testChild2.id])
        await editor.assertRecipients([
          'Esimies Essi',
          'Kosmiset vakiot (Henkilökunta)',
          'Toinen ryhmä (Henkilökunta)'
        ])

        // Selecting child 3 makes no recipients selectable
        // because of different unit, even if the supervisor is the same
        await editor.selectChildren([testChildRestricted.id])
        await editor.assertNoRecipients()
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
          citizenPage,
          'desktop'
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
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
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
        const recipients = ['Esimies Essi', 'Kosmiset vakiot (Henkilökunta)']
        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          recipients,
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
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.startReplyToFirstThread()
        await citizenMessagesPage.discardMessageButton.waitUntilVisible()
        await citizenMessagesPage.messageReplyContent.fill(defaultContent)
        await citizenMessagesPage.discardReplyEditor()
        await citizenMessagesPage.discardMessageButton.waitUntilHidden()
        await citizenMessagesPage.startReplyToFirstThread()
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
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await citizenMessagesPage.assertAriaLiveExistsAndIncludesNotification()
        await citizenMessagesPage.assertTimedNotification(
          'message-sent-notification',
          'Viesti lähetetty'
        )
      })

      test('Citizen can mark message as unread', async () => {
        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        const header = new CitizenHeader(citizenPage, 'desktop')

        await header.assertUnreadMessagesCount(1)

        await citizenMessagesPage.openFirstThread()
        await header.assertUnreadMessagesCount(0)

        await citizenMessagesPage.markUnreadButton.click()
        await header.assertUnreadMessagesCount(1)

        await citizenMessagesPage.openFirstThread()
        await header.assertUnreadMessagesCount(0)
      })

      test('Staff can mark message as unread, members in same group', async () => {
        const employee = await Fixture.employee()
          .staff(testDaycare.id)
          .groupAcl(testDaycareGroup.id, mockedDateAt10, mockedDateAt10)
          .save()
        const employee2 = await Fixture.employee()
          .staff(testDaycare.id)
          .groupAcl(testDaycareGroup.id, mockedDateAt10, mockedDateAt10)
          .save()

        await openCitizen(mockedDateAt10)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        const recipients = ['Kosmiset vakiot (Henkilökunta)']

        await citizenMessagesPage.sendNewMessage(
          defaultTitle,
          defaultContent,
          [],
          recipients,
          false
        )

        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        const employeePage = await Page.open({
          mockedTime: mockedDateAt11
        })
        await employeeLogin(employeePage, employee)
        await employeePage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(employeePage)

        await messagesPage.unitReceived.click()

        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.openFirstThread()
        await messagesPage.assertMessageContent(0, defaultContent)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 0)

        await messagesPage.markUnreadButton.waitUntilVisible()
        await messagesPage.markUnreadButton.click()
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)

        await employeePage.close()

        // Another staff role in the same group should be able to repeat the same process
        const employeePage2 = await Page.open({
          mockedTime: mockedDateAt11.addMinutes(5)
        })
        await employeeLogin(employeePage2, employee2)
        await employeePage2.goto(`${config.employeeUrl}/messages`)
        const messagesPage2 = new MessagesPage(employeePage2)

        await messagesPage2.unitReceived.click()

        await waitUntilEqual(() => messagesPage2.getReceivedMessageCount(), 1)
        await messagesPage2.openFirstThread()
        await messagesPage2.assertMessageContent(0, defaultContent)
        await waitUntilEqual(() => messagesPage2.getReceivedMessageCount(), 0)

        await messagesPage2.markUnreadButton.waitUntilVisible()
        await messagesPage2.markUnreadButton.click()
        await waitUntilEqual(() => messagesPage2.getReceivedMessageCount(), 1)

        await employeePage2.close()
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
          const citizenMessagesPage = new CitizenMessagesPage(
            citizenPage,
            'desktop'
          )
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
          const citizenMessagesPage = new CitizenMessagesPage(
            citizenPage,
            'desktop'
          )
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

  describe('Session keepalive while typing', () => {
    test('Citizen session is kept alive as long as user keeps typing', async () => {
      citizenPage = await Page.open({
        mockedTime: mockedDateAt10
      })
      await enduserLoginWeak(citizenPage, credentials)
      await citizenPage.goto(config.enduserMessagesUrl)
      await citizenPage.page.evaluate(() => {
        if (window.evaka) window.evaka.keepSessionAliveThrottleTime = 300
      })
      const citizenMessagesPage = new CitizenMessagesPage(
        citizenPage,
        'desktop'
      )
      const editor = await citizenMessagesPage.createNewMessage()
      const initialExpiry = await citizenMessagesPage.getSessionExpiry()

      const slowTypedText =
        'Olen aika hidas kirjoittamaan näitä viestejä, mutta yritän parhaani: Lapseni ovat sitä ja tätä...'
      const msDelayPerCharToTypeSlowly = 1200 / slowTypedText.length
      const authStatusRequests: string[] = []
      citizenPage.page.on('request', (request) => {
        if (request.url().includes('/api/citizen/auth/status')) {
          authStatusRequests.push(request.url())
        }
      })

      await editor.title.locator.pressSequentially('Asiaa lapsista', {
        delay: msDelayPerCharToTypeSlowly
      })
      await editor.content.locator.pressSequentially(slowTypedText, {
        delay: msDelayPerCharToTypeSlowly
      })

      const finalExpiry = await citizenMessagesPage.getSessionExpiry()
      // session expiry should be at least 0.3s longer than initially
      expect(finalExpiry).toBeGreaterThan(initialExpiry + 0.3)
      // slow typing should have caused at least 3 auth status requests to keep session alive
      expect(authStatusRequests.length).toBeGreaterThanOrEqual(3)
    })
  })

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
