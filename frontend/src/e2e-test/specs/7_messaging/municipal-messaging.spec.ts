// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  careArea2Fixture,
  daycare2Fixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { PersonDetail } from '../../dev-api/types'
import {
  createMessageAccounts,
  insertGuardians,
  resetDatabase
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import MessagesPage from '../../pages/employee/messages/messages-page'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

let messagingPage: Page
let messenger: DevEmployee
let staffPage: Page
let staff: DevEmployee
let fixtures: AreaAndPersonFixtures
let citizenPage: Page
let childInAreaA: PersonDetail
let childInAreaB: PersonDetail

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
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  childInAreaA = fixtures.enduserChildFixtureJari
  childInAreaB = fixtures.enduserChildFixtureKaarina
  await Fixture.careArea().with(careArea2Fixture).save()
  await Fixture.daycare().with(daycare2Fixture).save()
  await Fixture.daycareGroup().with(daycareGroupFixture).save()
  const daycareGroup2Fixture = {
    ...daycareGroupFixture,
    id: uuidv4(),
    daycareId: daycare2Fixture.id
  }
  await Fixture.daycareGroup().with(daycareGroup2Fixture).save()
  await Fixture.placement()
    .with({
      childId: childInAreaA.id,
      unitId: fixtures.daycareFixture.id,
      startDate: mockedDate,
      endDate: mockedDate
    })
    .save()
    .then((placement) =>
      Fixture.groupPlacement()
        .with({
          daycarePlacementId: placement.data.id,
          daycareGroupId: daycareGroupFixture.id,
          startDate: placement.data.startDate,
          endDate: placement.data.endDate
        })
        .save()
    )
  await Fixture.placement()
    .with({
      childId: childInAreaB.id,
      unitId: daycare2Fixture.id,
      startDate: mockedDate,
      endDate: mockedDate
    })
    .save()
    .then((placement) =>
      Fixture.groupPlacement()
        .with({
          daycarePlacementId: placement.data.id,
          daycareGroupId: daycareGroup2Fixture.id,
          startDate: placement.data.startDate,
          endDate: placement.data.endDate
        })
        .save()
    )
  const guardian2 = Fixture.person().with({ ssn: undefined })
  await guardian2.save()
  const guardian3 = Fixture.person().with({ ssn: undefined })
  await guardian3.save()
  await insertGuardians({
    body: [
      {
        childId: childInAreaA.id,
        guardianId: fixtures.enduserGuardianFixture.id
      },
      {
        childId: childInAreaB.id,
        guardianId: guardian2.data.id
      },
      {
        childId: childInAreaB.id,
        guardianId: guardian3.data.id
      }
    ]
  })
  await createMessageAccounts()
  messenger = (await Fixture.employeeMessenger().save()).data
  staff = (
    await Fixture.employeeStaff(fixtures.daycareFixture.id)
      .withGroupAcl(daycareGroupFixture.id)
      .save()
  ).data
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
  await enduserLogin(citizenPage)
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
      receivers: [fixtures.careAreaFixture.id, careArea2Fixture.id],
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
      receivers: [fixtures.careAreaFixture.id]
    })

    const sentMessagesPage = await messagesPage.openSentMessages()
    await sentMessagesPage.assertMessageParticipants(
      0,
      fixtures.careAreaFixture.name
    )

    const messagePage = await sentMessagesPage.openMessage(0)
    await messagePage.assertMessageRecipients(fixtures.careAreaFixture.name)
  })

  test('Messages sent by messaging role creates a copy for the staff', async () => {
    await openMessagingPage(messageSendTime)
    await messagingPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(messagingPage)
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.sendNewMessage({
      ...defaultMessage,
      receivers: [fixtures.careAreaFixture.id]
    })
    await runPendingAsyncJobs(messageSendTime.addMinutes(1))

    await openStaffPage(messageReadTime)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    await new MessagesPage(staffPage).assertCopyContent(
      defaultMessage.title,
      defaultMessage.content
    )
  })
})
