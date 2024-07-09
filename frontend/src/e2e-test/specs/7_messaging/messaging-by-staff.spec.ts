// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
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
import {
  createDaycareGroups,
  createMessageAccounts,
  insertGuardians,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import MessagesPage from '../../pages/employee/messages/messages-page'
import { waitUntilEqual } from '../../utils'
import { KeycloakRealmClient } from '../../utils/keycloak'
import { Page } from '../../utils/page'
import {
  defaultCitizenWeakAccount,
  employeeLogin,
  enduserLogin,
  enduserLoginWeak
} from '../../utils/user'

let staffPage: Page
let unitSupervisorPage: Page
let citizenPage: Page
let childId: UUID
let staff: DevEmployee
let unitSupervisor: DevEmployee
let fixtures: AreaAndPersonFixtures

const mockedDate = LocalDate.of(2020, 5, 21)
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
  fixtures = await initializeAreaAndPersonData()
  await createDaycareGroups({ body: [daycareGroupFixture] })

  const keycloak = await KeycloakRealmClient.createCitizenClient()
  await keycloak.deleteAllUsers()
  await keycloak.createUser({
    ...defaultCitizenWeakAccount,
    enabled: true
  })

  staff = await Fixture.employeeStaff(fixtures.daycareFixture.id)
    .withGroupAcl(daycareGroupFixture.id)
    .save()

  unitSupervisor = await Fixture.employeeUnitSupervisor(
    fixtures.daycareFixture.id
  ).save()

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.enduserChildFixtureJari.id // born 7.7.2014

  const daycarePlacementFixture1 = await Fixture.placement()
    .with({
      childId,
      unitId,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: daycarePlacementFixture1.id,
      daycareGroupId: daycareGroupFixture.id,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()

  const daycarePlacementFixture2 = await Fixture.placement()
    .with({
      childId: fixtures.enduserChildFixtureKaarina.id,
      unitId,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: daycarePlacementFixture2.id,
      daycareGroupId: daycareGroupFixture.id,
      startDate: mockedDate,
      endDate: mockedDate.addYears(1)
    })
    .save()

  await insertGuardians({
    body: [
      {
        childId: childId,
        guardianId: fixtures.enduserGuardianFixture.id
      }
    ]
  })

  await insertGuardians({
    body: [
      {
        childId: fixtures.enduserChildFixtureKaarina.id,
        guardianId: fixtures.enduserGuardianFixture.id
      }
    ]
  })

  await createMessageAccounts()
})

async function initStaffPage(mockedTime: HelsinkiDateTime) {
  staffPage = await Page.open({ mockedTime })
  await employeeLogin(staffPage, staff)
}

async function initUnitSupervisorPage(mockedTime: HelsinkiDateTime) {
  unitSupervisorPage = await Page.open({
    mockedTime: mockedTime
  })
  await employeeLogin(unitSupervisorPage, unitSupervisor)
}

async function initCitizenPage(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({ mockedTime })
  await enduserLogin(citizenPage)
}

async function initOtherCitizenPage(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({ mockedTime })
  await enduserLogin(
    citizenPage,
    fixtures.enduserChildJariOtherGuardianFixture.ssn
  )
}

async function initCitizenPageWeak(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({ mockedTime })
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
    staff = await Fixture.employeeSpecialEducationTeacher(
      fixtures.daycareFixture.id
    )
      .withGroupAcl(daycareGroupFixture.id)
      .save()
    // create messaging account for newly created VEO account
    await createMessageAccounts()

    const sensitiveMessage = {
      ...defaultMessage,
      sensitive: true,
      receivers: [enduserChildFixtureKaarina.id]
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
      receivers: [fixtures.daycareFixture.id]
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
      receivers: [fixtures.enduserChildFixtureKaarina.id]
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
      receivers: [daycareGroupFixture.id]
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

describe('Additional filters', () => {
  test('Additional filters are visible to unit supervisor on personal account', async () => {
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(unitSupervisorPage)
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.filtersButton.waitUntilVisible()
    await messageEditor.filtersButton.click()
    await messageEditor.assertFiltersVisible()
  })

  test('Additional filters are not visible to unit supervisor on group account', async () => {
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(unitSupervisorPage)
    await messagesPage.unitReceived.click()
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.sendButton.waitUntilVisible()
    expect(await messageEditor.filtersButtonCount).toBe(0)
  })

  test('Citizen receives a message when recipient filter matches', async () => {
    await insertGuardians({
      body: [
        {
          childId: childId,
          guardianId: fixtures.enduserChildJariOtherGuardianFixture.id
        }
      ]
    })
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus rajatulle joukolle',
      content: 'Ilmoituksen sisältö rajatulle joukolle',
      receivers: [fixtures.daycareFixture.id],
      yearsOfBirth: [2014]
    }
    const messageEditor = await new MessagesPage(
      unitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage(message)
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initOtherCitizenPage(mockedDateAt11)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await citizenMessagesPage.assertThreadContent(message)
    await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 1)
  })

  test(`Citizen doesn't receive a message when recipient filter doesn't match`, async () => {
    await insertGuardians({
      body: [
        {
          childId: childId,
          guardianId: fixtures.enduserChildJariOtherGuardianFixture.id
        }
      ]
    })
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus rajatulle joukolle',
      content: 'Ilmoituksen sisältö rajatulle joukolle',
      receivers: [fixtures.daycareFixture.id]
    }
    let messageEditor = await new MessagesPage(
      unitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage({
      ...message,
      yearsOfBirth: [2017]
    })
    messageEditor = await new MessagesPage(
      unitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage({
      ...message,
      shiftcare: true
    })
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initOtherCitizenPage(mockedDateAt11)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await citizenMessagesPage.assertInboxIsEmpty()
  })
})
