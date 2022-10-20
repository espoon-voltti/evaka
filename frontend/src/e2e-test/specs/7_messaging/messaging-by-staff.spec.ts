// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertGuardianFixtures,
  resetDatabase,
  upsertMessageAccounts
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { daycareGroupFixture, Fixture } from '../../dev-api/fixtures'
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
      unitId
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: daycarePlacementFixture.data.id,
      daycareGroupId: daycareGroupFixture.id
    })
    .save()

  await Fixture.placement()
    .with({
      childId: fixtures.enduserChildFixtureKaarina.id,
      unitId
    })
    .save()
    .then((placement) =>
      Fixture.groupPlacement()
        .with({
          daycarePlacementId: placement.data.id,
          daycareGroupId: daycareGroupFixture.id
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

async function initStaffPage() {
  staffPage = await Page.open()
  await employeeLogin(staffPage, staff)
}

async function initUnitSupervisorPage() {
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
        await Promise.all([initStaffPage(), configuration.init()])
      })

      const sendMessageAsStaff = async () => {
        await staffPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(staffPage)
        await messagesPage.sendNewMessage(defaultMessage)
      }

      const replyToMessageAsCitizen = async () => {
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)
      }

      test('Staff sends message and citizen replies', async () => {
        await staffPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(staffPage)
        await messagesPage.sendNewMessage(defaultMessage)

        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)

        await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)
        await staffPage.goto(`${config.employeeUrl}/messages`)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.assertMessageContent(1, defaultReply)
      })

      test('Staff can archive a message', async () => {
        await sendMessageAsStaff()
        await replyToMessageAsCitizen()

        await staffPage.goto(`${config.employeeUrl}/messages`)
        const messagesPage = new MessagesPage(staffPage)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.deleteFirstThread()
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 0)
      })
    })
  }
})

describe('Staff copies', () => {
  test('Message sent by supervisor to the whole unit creates a copy for the staff', async () => {
    await initUnitSupervisorPage()
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus',
      content: 'Ilmoituksen sisältö',
      receiver: fixtures.daycareFixture.id
    }
    await new MessagesPage(unitSupervisorPage).sendNewMessage(message)

    await initStaffPage()
    await staffPage.goto(`${config.employeeUrl}/messages`)
    await new MessagesPage(staffPage).assertCopyContent(
      message.title,
      message.content
    )
  })

  test('Message sent by supervisor to a single child does not create a copy for the staff', async () => {
    await initUnitSupervisorPage()
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus',
      content: 'Ilmoituksen sisältö',
      receiver: fixtures.enduserChildFixtureKaarina.id
    }
    await new MessagesPage(unitSupervisorPage).sendNewMessage(message)

    await initStaffPage()
    await staffPage.goto(`${config.employeeUrl}/messages`)
    await new MessagesPage(staffPage).assertNoCopies()
  })

  test('Message sent by supervisor from a group account to a single child does not create a copy for the staff', async () => {
    await initUnitSupervisorPage()
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus',
      content: 'Ilmoituksen sisältö',
      sender: `${fixtures.daycareFixture.name} - ${daycareGroupFixture.name}`,
      receiver: daycareGroupFixture.id
    }
    await new MessagesPage(unitSupervisorPage).sendNewMessage(message)

    await initStaffPage()
    await staffPage.goto(`${config.employeeUrl}/messages`)
    await new MessagesPage(staffPage).assertNoCopies()
  })
})
