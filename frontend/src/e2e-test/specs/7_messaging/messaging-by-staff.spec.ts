// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
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
  daycareGroupFixture,
  enduserChildFixtureKaarina,
  enduserGuardianFixture,
  Fixture
} from '../../dev-api/fixtures'
import { EmployeeDetail } from '../../dev-api/types'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import MessagesPage from '../../pages/employee/messages/messages-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin, enduserLoginWeak } from '../../utils/user'

let staffPage: Page
let unitSupervisorPage: Page
let citizenPage: Page
let childId: UUID
let staff: EmployeeDetail
let unitSupervisor: EmployeeDetail
let fixtures: AreaAndPersonFixtures

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
  await insertDaycareGroupFixtures([daycareGroupFixture])

  staff = (
    await Fixture.employeeStaff(fixtures.daycareFixture.id)
      .withGroupAcl(daycareGroupFixture.id)
      .save()
  ).data

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

  await Fixture.placement()
    .with({
      childId: fixtures.enduserChildFixtureKaarina.id,
      unitId,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()
    .then((placement) =>
      Fixture.groupPlacement()
        .with({
          daycarePlacementId: placement.data.id,
          daycareGroupId: daycareGroupFixture.id,
          startDate: mockedDate,
          endDate: mockedDate.addYears(1)
        })
        .save()
    )

  await insertGuardianFixtures([
    {
      childId: childId,
      guardianId: fixtures.enduserGuardianFixture.id
    }
  ])

  await insertGuardianFixtures([
    {
      childId: fixtures.enduserChildFixtureKaarina.id,
      guardianId: fixtures.enduserGuardianFixture.id
    }
  ])

  await upsertMessageAccounts()
})

async function initStaffPage(mockedTime: HelsinkiDateTime) {
  staffPage = await Page.open({ mockedTime: mockedTime.toSystemTzDate() })
  await employeeLogin(staffPage, staff)
}

async function initUnitSupervisorPage(mockedTime: HelsinkiDateTime) {
  unitSupervisorPage = await Page.open({
    mockedTime: mockedTime.toSystemTzDate()
  })
  await employeeLogin(unitSupervisorPage, unitSupervisor)
}

async function initCitizenPage(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({ mockedTime: mockedTime.toSystemTzDate() })
  await enduserLogin(citizenPage)
}

async function initCitizenPageWeak(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({ mockedTime: mockedTime.toSystemTzDate() })
  await enduserLoginWeak(citizenPage)
}

const defaultReply = 'Testivastaus testiviestiin'

const defaultMessage = {
  title: 'Otsikko',
  content: 'Testiviestin sisältö'
}

describe('Sending and receiving messages', () => {
  const initConfigurations = [
    ['direct login', initCitizenPage] as const,
    ['weak login', initCitizenPageWeak] as const
  ]

  describe.each(initConfigurations)(
    `Interactions with %s`,
    (_name, initCitizen) => {
      test('Staff sends message and citizen replies', async () => {
        await initStaffPage(mockedDateAt10)
        await staffPage.goto(`${config.employeeUrl}/messages`)
        let messagesPage = new MessagesPage(staffPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await initCitizen(mockedDateAt11)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)
        await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))

        await initStaffPage(mockedDateAt12)
        await staffPage.goto(`${config.employeeUrl}/messages`)
        messagesPage = new MessagesPage(staffPage)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.receivedMessage.click()
        await messagesPage.assertMessageContent(1, defaultReply)
      })

      test('Staff can archive a message', async () => {
        await initStaffPage(mockedDateAt10)
        await staffPage.goto(`${config.employeeUrl}/messages`)
        let messagesPage = new MessagesPage(staffPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await initCitizen(mockedDateAt11)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)
        await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))

        await initStaffPage(mockedDateAt12)
        await staffPage.goto(`${config.employeeUrl}/messages`)
        messagesPage = new MessagesPage(staffPage)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)

        await messagesPage.deleteFirstThread()
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 0)
      })
    }
  )
})

describe('Sending and receiving sensitive messages', () => {
  test('VEO sends sensitive message, citizen needs strong auth and after strong auth sees message', async () => {
    staff = (
      await Fixture.employeeSpecialEducationTeacher(fixtures.daycareFixture.id)
        .withGroupAcl(daycareGroupFixture.id)
        .save()
    ).data
    // create messaging account for newly created VEO account
    await upsertMessageAccounts()

    const sensitiveMessage = {
      ...defaultMessage,
      sensitive: true,
      receiver: enduserChildFixtureKaarina.id
    }

    await initStaffPage(mockedDateAt10)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(staffPage)
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.sendNewMessage(sensitiveMessage)

    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initCitizenPageWeak(mockedDateAt11)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await citizenMessagesPage.assertThreadIsRedacted()

    const authPage = await citizenMessagesPage.openStrongAuthPage()
    const strongAuthCitizenMessagePage = await authPage.login(
      enduserGuardianFixture.ssn!
    )

    await strongAuthCitizenMessagePage.assertThreadContent(sensitiveMessage)
  })
})

describe('Staff copies', () => {
  test('Message sent by supervisor to the whole unit creates a copy for the staff', async () => {
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus',
      content: 'Ilmoituksen sisältö',
      receiver: fixtures.daycareFixture.id
    }
    const messageEditor = await new MessagesPage(
      unitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage(message)
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initStaffPage(mockedDateAt11)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    await new MessagesPage(staffPage).assertCopyContent(
      message.title,
      message.content
    )
  })

  test('Message sent by supervisor to a single child does not create a copy for the staff', async () => {
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus',
      content: 'Ilmoituksen sisältö',
      receiver: fixtures.enduserChildFixtureKaarina.id
    }
    const messageEditor = await new MessagesPage(
      unitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage(message)
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initStaffPage(mockedDateAt11)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    await new MessagesPage(staffPage).assertNoCopies()
  })

  test('Message sent by supervisor from a group account to a single child does not create a copy for the staff', async () => {
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus',
      content: 'Ilmoituksen sisältö',
      sender: `${fixtures.daycareFixture.name} - ${daycareGroupFixture.name}`,
      receiver: daycareGroupFixture.id
    }
    const messageEditor = await new MessagesPage(
      unitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage(message)
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initStaffPage(mockedDateAt11)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    await new MessagesPage(staffPage).assertNoCopies()
  })
})
