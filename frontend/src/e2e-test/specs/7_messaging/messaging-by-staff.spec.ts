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
let citizenPage: Page
let childId: UUID
let staff: EmployeeDetail
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
  await upsertMessageAccounts()
  await insertGuardianFixtures([
    {
      childId: childId,
      guardianId: fixtures.enduserGuardianFixture.id
    }
  ])
})

async function initStaffPage() {
  staffPage = await Page.open()
  await employeeLogin(staffPage, staff)
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
    })
  }
})
