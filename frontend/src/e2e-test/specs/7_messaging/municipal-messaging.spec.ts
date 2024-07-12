// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import {
  testCareArea2,
  testDaycare2,
  testDaycareGroup,
  Fixture,
  uuidv4,
  testAdult,
  testChild,
  testChild2,
  testCareArea,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createMessageAccounts,
  insertGuardians,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee, DevPerson } from '../../generated/api-types'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import MessagesPage from '../../pages/employee/messages/messages-page'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

let messagingPage: Page
let messenger: DevEmployee
let staffPage: Page
let staff: DevEmployee
let citizenPage: Page
let childInAreaA: DevPerson
let childInAreaB: DevPerson

const mockedDate = LocalDate.of(2022, 11, 8)
const messageSendTime = HelsinkiDateTime.fromLocal(
  mockedDate,
  LocalTime.of(10, 49, 10)
)
const messageReadTime = HelsinkiDateTime.fromLocal(
  mockedDate,
  LocalTime.of(10, 49, 32)
)

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare().with(testDaycare).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  childInAreaA = testChild
  childInAreaB = testChild2
  await Fixture.careArea().with(testCareArea2).save()
  await Fixture.daycare().with(testDaycare2).save()
  await Fixture.daycareGroup().with(testDaycareGroup).save()
  const daycareGroup2Fixture = {
    ...testDaycareGroup,
    id: uuidv4(),
    daycareId: testDaycare2.id
  }
  await Fixture.daycareGroup().with(daycareGroup2Fixture).save()
  await Fixture.placement()
    .with({
      childId: childInAreaA.id,
      unitId: testDaycare.id,
      startDate: mockedDate,
      endDate: mockedDate
    })
    .save()
    .then((placement) =>
      Fixture.groupPlacement()
        .with({
          daycarePlacementId: placement.id,
          daycareGroupId: testDaycareGroup.id,
          startDate: placement.startDate,
          endDate: placement.endDate
        })
        .save()
    )
  await Fixture.placement()
    .with({
      childId: childInAreaB.id,
      unitId: testDaycare2.id,
      startDate: mockedDate,
      endDate: mockedDate
    })
    .save()
    .then((placement) =>
      Fixture.groupPlacement()
        .with({
          daycarePlacementId: placement.id,
          daycareGroupId: daycareGroup2Fixture.id,
          startDate: placement.startDate,
          endDate: placement.endDate
        })
        .save()
    )
  const guardian2 = await Fixture.person().with({ ssn: null }).saveAdult()
  const guardian3 = await Fixture.person().with({ ssn: null }).saveAdult()
  await insertGuardians({
    body: [
      {
        childId: childInAreaA.id,
        guardianId: testAdult.id
      },
      {
        childId: childInAreaB.id,
        guardianId: guardian2.id
      },
      {
        childId: childInAreaB.id,
        guardianId: guardian3.id
      }
    ]
  })
  await createMessageAccounts()
  messenger = await Fixture.employeeMessenger().save()
  staff = await Fixture.employeeStaff(testDaycare.id)
    .withGroupAcl(testDaycareGroup.id)
    .save()
})

async function openMessagingPage(mockedTime: HelsinkiDateTime) {
  messagingPage = await Page.open({ mockedTime })
  await employeeLogin(messagingPage, messenger)
}

async function openStaffPage(mockedTime: HelsinkiDateTime) {
  staffPage = await Page.open({ mockedTime })
  await employeeLogin(staffPage, staff)
}

async function openCitizenPage(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({ mockedTime })
  await enduserLogin(citizenPage, testAdult)
}

const defaultMessage = {
  title: 'Tiedote',
  content: 'Viestin sisältö'
}

describe('Municipal messaging -', () => {
  test('Messaging role can send messages to multiple areas', async () => {
    await openMessagingPage(messageSendTime)
    await messagingPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(messagingPage)
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.sendNewMessage({
      ...defaultMessage,
      receivers: [testCareArea.id, testCareArea2.id],
      confirmManyRecipients: true
    })
    await runPendingAsyncJobs(messageSendTime.addMinutes(1))

    await openCitizenPage(messageReadTime)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await citizenMessagesPage.assertThreadContent(defaultMessage)
  })

  test('Sent municipal message has area name as recipients', async () => {
    await openMessagingPage(messageSendTime)
    await messagingPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(messagingPage)
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.sendNewMessage({
      ...defaultMessage,
      receivers: [testCareArea.id]
    })

    const sentMessagesPage = await messagesPage.openSentMessages()
    await sentMessagesPage.assertMessageParticipants(0, testCareArea.name)

    const messagePage = await sentMessagesPage.openMessage(0)
    await messagePage.assertMessageRecipients(testCareArea.name)
  })

  test('Messages sent by messaging role creates a copy for the staff', async () => {
    await openMessagingPage(messageSendTime)
    await messagingPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(messagingPage)
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.sendNewMessage({
      ...defaultMessage,
      receivers: [testCareArea.id]
    })
    await runPendingAsyncJobs(messageSendTime.addMinutes(1))

    await openStaffPage(messageReadTime)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    await new MessagesPage(staffPage).assertCopyContent(
      defaultMessage.title,
      defaultMessage.content
    )
  })

  test('Additional filters are visible to messaging role', async () => {
    await openMessagingPage(messageSendTime)
    await messagingPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(messagingPage)
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.filtersButton.waitUntilVisible()
    await messageEditor.filtersButton.click()
    await messageEditor.assertFiltersVisible()
  })
})
