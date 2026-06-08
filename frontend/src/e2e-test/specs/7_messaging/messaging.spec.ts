// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'fs'

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import type {
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
  createFosterParent,
  createMessageAccounts,
  insertGuardians,
  resetServiceState,
  updateIncomeStatementHandled,
  upsertWeakCredentials
} from '../../generated/api-clients'
import type {
  DevCareArea,
  DevEmployee,
  DevPlacement
} from '../../generated/api-types'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import MessagesPage from '../../pages/employee/messages/messages-page'
import type { NewEvakaPage } from '../../playwright'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin, enduserLogin, enduserLoginWeak } from '../../utils/user'

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

const defaultTitle = 'Otsikko'
const defaultContent = 'Testiviestin sisältö'
const defaultReply = 'Testivastaus testiviestiin'

const defaultMessage = {
  title: 'Otsikko',
  content: 'Testiviestin sisältö'
}

const deletedPlaceholderLines = [
  'Lähettäjä on poistanut viestin. Sinun ei tarvitse tehdä mitään.',
  'Avsändaren har tagit bort meddelandet. Du behöver inte göra något.',
  'The sender has deleted this message. No action is needed on your part.'
]

test.describe('Sending and receiving messages', () => {
  let unitSupervisorPage: Page
  let citizenPage: Page
  let childId: PersonId
  let unitSupervisor: DevEmployee
  let careArea: DevCareArea
  let daycarePlacementFixture: DevPlacement
  let backupDaycareId: DaycareId
  let backupGroupFixtureId: GroupId
  let newPage: NewEvakaPage

  test.beforeEach(async ({ newEvakaPage }) => {
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

    newPage = newEvakaPage
  })

  async function openSupervisorPage(mockedTime: HelsinkiDateTime) {
    unitSupervisorPage = await newPage({ mockedTime })
    await employeeLogin(unitSupervisorPage, unitSupervisor)
  }

  async function openCitizenPage(mockedTime: HelsinkiDateTime, url?: string) {
    citizenPage = await newPage({ mockedTime })
    await enduserLogin(citizenPage, testAdult, url)
  }

  async function openCitizenPageWeak(
    mockedTime: HelsinkiDateTime,
    path?: string
  ) {
    citizenPage = await newPage({ mockedTime })
    await enduserLoginWeak(citizenPage, credentials)
    if (path) await citizenPage.goto(config.enduserUrl + path)
  }

  for (const [name, openCitizen] of [
    ['direct login', openCitizenPage],
    ['weak login', openCitizenPageWeak]
  ] as const) {
    test.describe(`Interactions with ${name}`, () => {
      test('Unit supervisor sends message and citizen replies', async () => {
        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        let messagesPage = new MessagesPage(unitSupervisorPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11, '/messages')
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))
        await expect(citizenMessagesPage.threadMessages).toHaveCount(2)

        await openSupervisorPage(mockedDateAt12)
        messagesPage = new MessagesPage(unitSupervisorPage)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        await expect(messagesPage.receivedMessages).toHaveCount(1)
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

        await openCitizen(mockedDateAt11, '/messages')
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))
        await expect(citizenMessagesPage.threadMessages).toHaveCount(2)

        await openSupervisorPage(mockedDateAt12)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        messagesPage = new MessagesPage(unitSupervisorPage)
        await expect(messagesPage.receivedMessages).toHaveCount(1)
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

        await openCitizen(mockedDateAt10, '/messages')
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

        await openCitizen(mockedDateAt11, '/messages')
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.assertThreadContent(message)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))
        await expect(citizenMessagesPage.threadMessages).toHaveCount(2)

        await openSupervisorPage(mockedDateAt12)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        messagesPage = new MessagesPage(unitSupervisorPage)
        await expect(messagesPage.receivedMessages).toHaveCount(1)
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

        await openCitizen(mockedDateAt11, '/messages')
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await expect(citizenMessagesPage.threadAttachments).toHaveCount(2)

        await citizenMessagesPage.openFirstThread()
        const downloadedContent =
          await citizenMessagesPage.downloadFirstAttachment()
        const originalContent = await fs.promises.readFile(
          'src/e2e-test/assets/test_file.odt'
        )
        expect(downloadedContent).toEqual(originalContent)
      })

      test('Employee can send video attachment', async () => {
        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage({
          ...defaultMessage,
          attachmentCount: 1,
          attachmentFilePath: 'src/e2e-test/assets/test_file.mp4'
        })
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11, '/messages')
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await expect(citizenMessagesPage.threadAttachments).toHaveCount(1)
      })

      test('Employee can discard thread reply', async () => {
        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        let messagesPage = new MessagesPage(unitSupervisorPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11, '/messages')
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
        await expect(messagesPage.discardMessageButton).toBeVisible()
        await messagesPage.fillReplyContent(defaultContent)
        await messagesPage.discardReplyEditor()
        await expect(messagesPage.discardMessageButton).toBeHidden()
        await messagesPage.openReplyEditor()
        await messagesPage.assertReplyContentIsEmpty()
      })

      test('Citizen sends a message to the unit supervisor', async () => {
        const recipients = ['Esimies Essi']
        await openCitizen(mockedDateAt10, '/messages')
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
        await expect(messagesPage.receivedMessages).toHaveCount(1)
        await messagesPage.openInbox(1)
        await expect(messagesPage.receivedMessages).toHaveCount(0)
      })

      test('Citizen sends a message to the unit supervisor before the placement, both will reply', async () => {
        const tenDaysbeforePlacementAt10 = HelsinkiDateTime.fromLocal(
          mockedDate.addDays(-10),
          LocalTime.of(10, 2)
        )
        const recipients = ['Esimies Essi']
        await openCitizen(tenDaysbeforePlacementAt10, '/messages')
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
        await expect(messagesPage.receivedMessages).toHaveCount(1)
        await messagesPage.openFirstThreadReplyEditor()
        await messagesPage.fillReplyContent(defaultReply)
        await messagesPage.sendReplyButton.click()
        await expect(messagesPage.sendReplyButton).toBeHidden()
        await runPendingAsyncJobs(tenDaysBeforePlacementAt11.addMinutes(1))

        const tenDaysBeforePlacementAt12 = HelsinkiDateTime.fromLocal(
          mockedDate.addDays(-10),
          LocalTime.of(12, 13)
        )
        await openCitizen(tenDaysBeforePlacementAt12, '/messages')
        const citizenMessagesPageLater = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPageLater.openFirstThread()
        await expect(citizenMessagesPageLater.threadMessages).toHaveCount(2)
        await citizenMessagesPageLater.replyToFirstThread(defaultReply)
      })

      test('Citizen can send a message and receive a notification on success', async () => {
        const recipients = ['Esimies Essi']
        await openCitizen(mockedDateAt10, '/messages')
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
        await openCitizen(mockedDateAt10, '/messages')
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
        await expect(messagesPage.receivedMessages).toHaveCount(1)
        await messagesPage.receivedMessage.click()
        await messagesPage.assertMessageContent(0, defaultContent)
        await expect(messagesPage.threadAttachments).toHaveCount(1)
      })

      test('Unit supervisor sees the name of the child in a message sent by citizen', async () => {
        const recipients = ['Esimies Essi']
        await openCitizen(mockedDateAt10, '/messages')
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
        await expect(messagesPage.receivedMessages).toHaveCount(1)
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
        await openCitizen(mockedDateAt10, '/messages')
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
        await expect(messagesPage.receivedMessages).toHaveCount(1)
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

        await openCitizen(mockedDateAt10, '/messages')
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

        await openCitizen(dayAfterPlacementEnds, '/messages')
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
        await openCitizen(mockedDateAt10, '/messages')
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
        await expect(messagesPage.receivedMessages).toHaveCount(1)
        await messagesPage.assertReceivedMessageParticipantsContains(
          0,
          `${otherGuardian.lastName} ${otherGuardian.firstName}`
        )
      })

      test('Citizen sends message to the unit supervisor and the group', async () => {
        const recipients = ['Esimies Essi', 'Kosmiset vakiot (Henkilökunta)']
        await openCitizen(mockedDateAt10, '/messages')
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
        await expect(messagesPage.receivedMessages).toHaveCount(1)
        await messagesPage.openInbox(1)
        await expect(messagesPage.receivedMessages).toHaveCount(1)
      })

      test('Citizen can discard message', async () => {
        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11, '/messages')
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.startReplyToFirstThread()
        await expect(citizenMessagesPage.discardMessageButton).toBeVisible()
        await citizenMessagesPage.messageReplyContent.fill(defaultContent)
        await citizenMessagesPage.discardReplyEditor()
        await expect(citizenMessagesPage.discardMessageButton).toBeHidden()
        await citizenMessagesPage.startReplyToFirstThread()
        await expect(citizenMessagesPage.messageReplyContent).toHaveText('')
      })

      test('Citizen can reply to a thread and receive a notification on success', async () => {
        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11, '/messages')
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

        await openCitizen(mockedDateAt11, '/messages')
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

      test('Mark unread button is hidden for citizen if there are no messages from staff in the thread', async () => {
        const recipients = ['Kosmiset vakiot (Henkilökunta)']
        await openCitizen(mockedDateAt10, '/messages')
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

        await citizenMessagesPage.openFirstThread()
        await citizenMessagesPage.assertHasNoMarkUnreadButton()
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

        await openCitizen(mockedDateAt10, '/messages')
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

        const employeePage = await newPage({
          mockedTime: mockedDateAt11
        })
        await employeeLogin(employeePage, employee)
        await employeePage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(employeePage)

        await messagesPage.unitReceived.click()

        await expect(messagesPage.receivedMessages).toHaveCount(1)
        await messagesPage.openFirstThread()
        await messagesPage.assertMessageContent(0, defaultContent)
        await expect(messagesPage.receivedMessages).toHaveCount(0)

        await expect(messagesPage.markUnreadButton).toBeVisible()
        await messagesPage.markUnreadButton.click()
        await expect(messagesPage.receivedMessages).toHaveCount(1)

        const employeePage2 = await newPage({
          mockedTime: mockedDateAt11.addMinutes(5)
        })
        await employeeLogin(employeePage2, employee2)
        await employeePage2.goto(`${config.employeeUrl}/messages`)
        const messagesPage2 = new MessagesPage(employeePage2)

        await messagesPage2.unitReceived.click()

        await expect(messagesPage2.receivedMessages).toHaveCount(1)
        await messagesPage2.openFirstThread()
        await messagesPage2.assertMessageContent(0, defaultContent)
        await expect(messagesPage2.receivedMessages).toHaveCount(0)

        await expect(messagesPage2.markUnreadButton).toBeVisible()
        await messagesPage2.markUnreadButton.click()
        await expect(messagesPage2.receivedMessages).toHaveCount(1)
      })

      test('Citizen reply recipient toggles persist while typing', async () => {
        await insertGuardians({
          body: [{ childId, guardianId: testAdult2.id }]
        })

        await openSupervisorPage(mockedDateAt10)
        await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(unitSupervisorPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await openCitizen(mockedDateAt11, '/messages')
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.startReplyToFirstThread()

        const otherGuardian = citizenMessagesPage.secondaryRecipient(
          `${testAdult2.lastName} ${testAdult2.firstName}`
        )
        await otherGuardian.assertIsUnselected()

        await otherGuardian.click()
        await otherGuardian.assertIsSelected()
        await citizenMessagesPage.messageReplyContent.fill('Test reply')
        await otherGuardian.assertIsSelected()
      })

      test.describe('Messages can be deleted / archived', () => {
        test('Unit supervisor sends message and citizen deletes the message', async () => {
          await openSupervisorPage(mockedDateAt10)
          await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
          const messagesPage = new MessagesPage(unitSupervisorPage)
          const messageEditor = await messagesPage.openMessageEditor()
          await messageEditor.sendNewMessage(defaultMessage)
          await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

          await openCitizen(mockedDateAt11, '/messages')
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

          await openCitizen(mockedDateAt11, '/messages')
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
    })
  }

  test.describe('Session keepalive while typing', () => {
    test('Citizen session is kept alive as long as user keeps typing', async () => {
      citizenPage = await newPage({
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
      expect(finalExpiry).toBeGreaterThan(initialExpiry + 0.3)
      expect(authStatusRequests.length).toBeGreaterThanOrEqual(3)
    })
  })

  test.describe('Drafts', () => {
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

  test.describe('Finance threads', () => {
    test('Citizen can only reply to finance threads when there is an income statement sent or being handled', async ({
      newEvakaPage
    }) => {
      const mockedTime = HelsinkiDateTime.of(2022, 5, 21, 10, 0, 0, 0)
      await resetServiceState()
      await testAdult.saveAdult({ updateMockVtjWithDependants: [] })
      const financeAdmin = await Fixture.employee().financeAdmin().save()
      await createMessageAccounts()

      const page = await newEvakaPage({ mockedTime })
      await employeeLogin(page, financeAdmin)
      await page.goto(config.employeeUrl)
      const guardianPage = new GuardianInformationPage(page)
      await guardianPage.navigateToGuardian(testAdult.id)
      const notesAndMessages = await guardianPage.openCollapsible(
        'financeNotesAndMessages'
      )
      const messageEditor = await notesAndMessages.openNewMessageEditor()
      await messageEditor.sendNewMessage({
        title: 'Tuloselvitys',
        content: 'Toimita tuloselvitys'
      })
      await runPendingAsyncJobs(mockedTime.addMinutes(1))

      async function openCitizenThread(time: HelsinkiDateTime) {
        const citizenPage = await newEvakaPage({ mockedTime: time })
        await enduserLogin(citizenPage, testAdult, '/messages')
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.openFirstThread()
        return citizenMessagesPage
      }

      let citizenMessagesPage = await openCitizenThread(mockedTime.addHours(1))
      await citizenMessagesPage.assertFinanceReplyInfo(true)
      await citizenMessagesPage.assertOpenReplyEditorButtonIsHidden()

      const incomeStatement = await Fixture.incomeStatement({
        personId: testAdult.id,
        status: 'SENT',
        sentAt: mockedTime.addHours(2)
      }).save()
      citizenMessagesPage = await openCitizenThread(mockedTime.addHours(2))
      await citizenMessagesPage.assertFinanceReplyInfo(false)
      await citizenMessagesPage.replyToFirstThread('Reply to finance thread')

      await updateIncomeStatementHandled({
        body: {
          incomeStatementId: incomeStatement.id,
          employeeId: financeAdmin.id,
          status: 'HANDLING',
          note: ''
        }
      })
      citizenMessagesPage = await openCitizenThread(mockedTime.addHours(3))
      await citizenMessagesPage.assertFinanceReplyInfo(false)
      await citizenMessagesPage.replyToFirstThread('Reply during handling')

      await updateIncomeStatementHandled({
        body: {
          incomeStatementId: incomeStatement.id,
          employeeId: financeAdmin.id,
          status: 'HANDLED',
          note: ''
        }
      })
      citizenMessagesPage = await openCitizenThread(mockedTime.addHours(4))
      await citizenMessagesPage.assertFinanceReplyInfo(true)
      await citizenMessagesPage.assertOpenReplyEditorButtonIsHidden()
    })
  })

  test.describe('Message deletion', () => {
    test('Sender deletes own message, views the original, and sees the alert on later views', async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      const messageEditor = await messagesPage.openMessageEditor()
      await messageEditor.sendNewMessage(defaultMessage)
      await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

      let sentMessage = await (
        await messagesPage.openSentMessages()
      ).openMessage(0)
      await sentMessage.deleteMessage()

      await expect(sentMessage.messageDeletedBanner).toBeVisible()
      await expect(sentMessage.viewDeletedMessageButton).toBeVisible()
      await expect(sentMessage.deletedMessageAlert).toBeHidden()

      await expect(sentMessage.threadTitle).toContainText(
        'Viesti poistettu 21.05.2022'
      )
      await expect(sentMessage.threadTitle).not.toContainText(
        defaultMessage.title
      )

      await expect(sentMessage.deletedMessageOriginal).toBeHidden()
      await sentMessage.viewDeletedContent()
      await expect(sentMessage.deletedMessageOriginal).toContainText(
        defaultMessage.content
      )
      await expect(sentMessage.threadTitle).toContainText(
        `Viesti poistettu [${defaultMessage.title}]`
      )
      await sentMessage.hideDeletedContent()
      await expect(sentMessage.deletedMessageOriginal).toBeHidden()
      await expect(sentMessage.viewDeletedMessageButton).toBeVisible()

      await expect(sentMessage.threadTitle).toContainText(
        'Viesti poistettu 21.05.2022'
      )
      await expect(sentMessage.threadTitle).not.toContainText(
        defaultMessage.title
      )

      // Re-viewing within the same page load must hit the backend again so
      // every view is audit-logged (there is no remount to trigger a refetch)
      const [secondViewRequest] = await Promise.all([
        unitSupervisorPage.page.waitForRequest((req) =>
          req.url().includes('view-deleted-content')
        ),
        sentMessage.viewDeletedContent()
      ])
      expect(secondViewRequest.method()).toEqual('GET')
      await expect(sentMessage.deletedMessageOriginal).toContainText(
        defaultMessage.content
      )

      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const sentList = await messagesPage.openSentMessages()
      await expect(
        sentList.sentMessages.nth(0).findByDataQa('thread-list-item-title')
      ).toContainText('Viesti poistettu 21.05.2022')
      sentMessage = await sentList.openMessage(0)
      await expect(sentMessage.messageDeletedBanner).toBeHidden()
      await expect(sentMessage.deletedMessageAlert).toBeVisible()
      await sentMessage.viewDeletedContent()
      await expect(sentMessage.deletedMessageOriginal).toContainText(
        defaultMessage.content
      )
    })

    test('A still-visible reply shows the thread-title-removed wording when the first message is deleted', async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      let messagesPage = new MessagesPage(unitSupervisorPage)
      const messageEditor = await messagesPage.openMessageEditor()
      await messageEditor.sendNewMessage(defaultMessage)
      await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

      await openCitizenPage(mockedDateAt11, '/messages')
      const citizenMessagesPage = new CitizenMessagesPage(
        citizenPage,
        'desktop'
      )
      await citizenMessagesPage.replyToFirstThread(defaultReply)
      await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))

      await openSupervisorPage(mockedDateAt12)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      messagesPage = new MessagesPage(unitSupervisorPage)
      await messagesPage.openInbox(0)
      await messagesPage.openFirstThreadReplyEditor()
      await messagesPage.fillReplyContent(defaultReply)
      await messagesPage.sendReplyButton.click()
      await expect(messagesPage.sendReplyButton).toBeHidden()
      await runPendingAsyncJobs(mockedDateAt12.addMinutes(1))

      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const firstSent = await (
        await messagesPage.openSentMessages()
      ).openMessage(1)
      await firstSent.deleteMessage()

      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const sentList = await messagesPage.openSentMessages()
      await expect(
        sentList.sentMessages.nth(0).findByDataQa('thread-list-item-title')
      ).toContainText('Viestiketjun otsikko poistettu 21.05.2022')
      await expect(
        sentList.sentMessages.nth(1).findByDataQa('thread-list-item-title')
      ).toContainText('Viesti poistettu 21.05.2022')

      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      await messagesPage.openInbox(0)
      await expect(
        messagesPage.receivedMessages
          .nth(0)
          .findByDataQa('thread-list-item-title')
      ).toContainText('Viesti poistettu 21.05.2022')
      await messagesPage.openFirstThread()
      await expect(
        unitSupervisorPage.findByDataQa('thread-title')
      ).toContainText('Viesti poistettu 21.05.2022')
    })

    test('Recipient sees the multilingual placeholder for a deleted message', async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      const messageEditor = await messagesPage.openMessageEditor()
      await messageEditor.sendNewMessage(defaultMessage)
      await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

      const sentMessage = await (
        await messagesPage.openSentMessages()
      ).openMessage(0)
      await sentMessage.deleteMessage()

      await openCitizenPage(mockedDateAt11, '/messages')
      const citizenMessagesPage = new CitizenMessagesPage(
        citizenPage,
        'desktop'
      )
      await citizenMessagesPage.openFirstThread()
      const deletedMessage = citizenMessagesPage.threadMessages.only()

      for (const line of deletedPlaceholderLines) {
        await expect(deletedMessage).toContainText(line)
      }

      await expect(deletedMessage).not.toContainText(defaultMessage.content)
    })

    test('Delete button is hidden after the deletion window has passed', async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      let messagesPage = new MessagesPage(unitSupervisorPage)
      const messageEditor = await messagesPage.openMessageEditor()
      await messageEditor.sendNewMessage(defaultMessage)
      await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

      await openSupervisorPage(
        HelsinkiDateTime.fromLocal(mockedDate.addDays(8), LocalTime.of(10, 2))
      )
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      messagesPage = new MessagesPage(unitSupervisorPage)
      const sentMessage = await (
        await messagesPage.openSentMessages()
      ).openMessage(0)
      await expect(sentMessage.deleteMessageButton).toBeHidden()
    })

    test('Own bulletins can be deleted', async () => {
      await openSupervisorPage(mockedDateAt10)
      await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(unitSupervisorPage)
      const messageEditor = await messagesPage.openMessageEditor()
      await messageEditor.sendNewMessage({
        ...defaultMessage,
        type: 'BULLETIN'
      })
      await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

      const sentMessage = await (
        await messagesPage.openSentMessages()
      ).openMessage(0)
      await sentMessage.deleteMessage()

      await expect(sentMessage.messageDeletedBanner).toBeVisible()
      await expect(sentMessage.viewDeletedMessageButton).toBeVisible()
    })

    test('Municipal bulletins cannot be deleted', async () => {
      const messenger = await Fixture.employee().messenger().save()
      const messengerPage = await newPage({ mockedTime: mockedDateAt10 })
      await employeeLogin(messengerPage, messenger)
      await messengerPage.goto(`${config.employeeUrl}/messages`)
      const messagesPage = new MessagesPage(messengerPage)
      const messageEditor = await messagesPage.openMessageEditor()
      await messageEditor.sendNewMessage({
        ...defaultMessage,
        recipientKeys: [`${careArea.id}+false`]
      })
      await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

      const sentMessage = await (
        await messagesPage.openSentMessages()
      ).openMessage(0)
      await expect(sentMessage.deleteMessageButton).toBeHidden()
    })

    test('Deleting an already-deleted message shows an "already deleted" notice, not the deleted-by-you banner', async () => {
      // two sessions on the same account
      const firstPage = await newPage({ mockedTime: mockedDateAt10 })
      await employeeLogin(firstPage, unitSupervisor)
      await firstPage.goto(`${config.employeeUrl}/messages`)
      const firstMessages = new MessagesPage(firstPage)
      const messageEditor = await firstMessages.openMessageEditor()
      await messageEditor.sendNewMessage(defaultMessage)
      await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

      const secondPage = await newPage({ mockedTime: mockedDateAt10 })
      await employeeLogin(secondPage, unitSupervisor)
      await secondPage.goto(`${config.employeeUrl}/messages`)
      const secondMessages = new MessagesPage(secondPage)

      // both sessions open the same sent message before either deletes
      const firstSent = await (
        await firstMessages.openSentMessages()
      ).openMessage(0)
      const secondSent = await (
        await secondMessages.openSentMessages()
      ).openMessage(0)

      // the first session deletes it (real deletion → banner)
      await firstSent.deleteMessage()
      await expect(firstSent.messageDeletedBanner).toBeVisible()

      // the second (now stale) session tries to delete the same message and is
      // told it was already deleted — without the "you deleted this" banner
      await secondSent.deleteMessage()
      await expect(secondSent.alreadyDeletedModal).toBeVisible()
      await expect(secondSent.messageDeletedBanner).toBeHidden()
      await secondSent.dismissAlreadyDeleted()
      // the view reflects the already-deleted state
      await expect(secondSent.deleteMessageButton).toBeHidden()
    })
  })
})

test.describe('Foster parent messaging', () => {
  let unitSupervisorPage: Page
  let childId: PersonId
  let unitSupervisor: DevEmployee
  let newPage: NewEvakaPage

  test.beforeEach(async ({ newEvakaPage }) => {
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
      areaId: testCareArea.id
    }).save()

    await createMessageAccounts()
    await insertGuardians({
      body: [
        {
          childId: childId,
          guardianId: testAdult.id
        }
      ]
    })

    newPage = newEvakaPage
  })

  async function openSupervisorPage(mockedTime: HelsinkiDateTime) {
    unitSupervisorPage = await newPage({ mockedTime })
    await employeeLogin(unitSupervisorPage, unitSupervisor)
  }

  test('Foster parent sends message to group with guardian as secondary recipient, employee reply has guardian selected by default', async () => {
    const fosterParent = await Fixture.person().saveAdult({
      updateMockVtjWithDependants: []
    })

    await createFosterParent({
      body: [
        {
          id: randomId(),
          childId,
          parentId: fosterParent.id,
          validDuring: new DateRange(mockedDate, mockedDate.addYears(1)),
          modifiedAt: mockedDateAt10,
          modifiedBy: evakaUserId(unitSupervisor.id)
        }
      ]
    })

    const fosterParentPage = await newPage({
      mockedTime: mockedDateAt10
    })
    await enduserLogin(fosterParentPage, fosterParent, '/messages')
    const fosterParentMessagesPage = new CitizenMessagesPage(
      fosterParentPage,
      'desktop'
    )
    const editor = await fosterParentMessagesPage.createNewMessage()
    const recipients = ['Kosmiset vakiot (Henkilökunta)']
    await editor.selectRecipients(recipients)

    const guardianRecipient = editor.secondaryRecipient(
      `${testAdult.lastName} ${testAdult.firstName}`
    )
    await guardianRecipient.assertIsUnselected()

    await guardianRecipient.click()
    await guardianRecipient.assertIsSelected()
    await editor.fillMessage(defaultTitle, defaultContent)
    await editor.sendMessage()
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await openSupervisorPage(mockedDateAt11)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(unitSupervisorPage)
    await messagesPage.unitReceived.click()
    await expect(messagesPage.receivedMessages).toHaveCount(1)
    await messagesPage.openFirstThreadReplyEditor()

    const guardianButton = messagesPage.secondaryRecipient(
      `${testAdult.lastName} ${testAdult.firstName}`
    )
    await guardianButton.assertIsSelected()
  })

  test('Employee sends message from group to child with foster parent and guardian, foster parent reply has guardian unselected by default', async () => {
    const fosterParent = await Fixture.person().saveAdult({
      updateMockVtjWithDependants: []
    })

    await createFosterParent({
      body: [
        {
          id: randomId(),
          childId,
          parentId: fosterParent.id,
          validDuring: new DateRange(mockedDate, mockedDate.addYears(1)),
          modifiedAt: mockedDateAt10,
          modifiedBy: evakaUserId(unitSupervisor.id)
        }
      ]
    })

    await openSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(unitSupervisorPage)
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.sendNewMessage({
      ...defaultMessage,
      recipientKeys: [`${childId}+false`]
    })
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    const fosterParentPage = await newPage({
      mockedTime: mockedDateAt11
    })
    await enduserLogin(fosterParentPage, fosterParent, '/messages')
    const fosterParentMessagesPage = new CitizenMessagesPage(
      fosterParentPage,
      'desktop'
    )
    await fosterParentMessagesPage.startReplyToFirstThread()

    const guardianRecipient = fosterParentMessagesPage.secondaryRecipient(
      `${testAdult.lastName} ${testAdult.firstName}`
    )
    await guardianRecipient.assertIsUnselected()
  })
})
