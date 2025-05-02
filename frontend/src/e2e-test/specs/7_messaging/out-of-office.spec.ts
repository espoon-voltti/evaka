// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DevEmployee } from 'e2e-test/generated/api-types'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import {
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createMessageAccounts,
  resetServiceState
} from '../../generated/api-clients'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import { OutOfOfficePage } from '../../pages/employee/messages/out-of-office-page'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

let supervisor: DevEmployee

beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  const unit = await testDaycare.save()
  supervisor = await Fixture.employee().unitSupervisor(unit.id).save()

  await Fixture.family({
    guardian: testAdult,
    children: [testChild]
  }).save()

  await Fixture.placement({
    childId: testChild.id,
    unitId: testDaycare.id,
    startDate: LocalDate.of(2022, 1, 1),
    endDate: LocalDate.of(2022, 12, 31)
  }).save()

  await createMessageAccounts()
})

describe('Out of Office', () => {
  test('Out of Office flow', async () => {
    // Employee sets an out of office period and edits it
    const employeePage = await Page.open({
      mockedTime: LocalDate.of(2022, 12, 1).toHelsinkiDateTime(
        LocalTime.of(12, 0)
      )
    })
    await employeeLogin(employeePage, supervisor)
    const outOfOfficePage = await OutOfOfficePage.open(employeePage)
    await outOfOfficePage.assertNoPeriods()

    const startDate = LocalDate.of(2022, 12, 1)
    const endDate = LocalDate.of(2022, 12, 7)
    await outOfOfficePage.addOutOfOfficePeriod(startDate, endDate)
    await outOfOfficePage.assertPeriodExists(startDate, endDate)

    const newStartDate = LocalDate.of(2022, 12, 2)
    await outOfOfficePage.editStartOfPeriod(newStartDate)
    await outOfOfficePage.assertPeriodExists(newStartDate, endDate)
    await outOfOfficePage.assertPeriodDoesNotExist(startDate, endDate)

    // Citizen doesn't see the out of office period because it starts the next day
    const citizenPage1 = await Page.open({
      mockedTime: LocalDate.of(2022, 12, 1).toHelsinkiDateTime(
        LocalTime.of(13, 0)
      )
    })
    const { editor: editor1 } = await getCitizenMessageEditor(citizenPage1)
    await editor1.assertNoOutOfOffice()

    // Citizen sees the out of office period the next day
    const citizenPage2 = await Page.open({
      mockedTime: LocalDate.of(2022, 12, 2).toHelsinkiDateTime(
        LocalTime.of(13, 0)
      )
    })
    const { editor: editor2, messagesPage: messagesPage2 } =
      await getCitizenMessageEditor(citizenPage2)
    await editor2.assertOutOfOffice({
      name: getSupervisorName(),
      period: FiniteDateRange.tryCreate(newStartDate, endDate)!
    })

    // Citizen sends the message (so that a message that can be replied to is available)
    await editor2.fillMessage('Test title', 'Test content')
    await editor2.sendMessage()

    // Reply editor shows the out of office period
    await messagesPage2.startReplyToFirstThread()
    await messagesPage2.assertThreadOutOfOffice({
      name: getSupervisorName(),
      period: FiniteDateRange.tryCreate(newStartDate, endDate)!
    })

    // After the out of office period, citizen doesn't see the out of office period anymore
    const citizenPage3 = await Page.open({
      mockedTime: LocalDate.of(2022, 12, 8).toHelsinkiDateTime(
        LocalTime.of(13, 0)
      )
    })
    const { editor: editor3 } = await getCitizenMessageEditor(citizenPage3)
    await editor3.assertNoOutOfOffice()

    // Employee removes the out of office period
    await outOfOfficePage.removeOutOfOfficePeriod()
    await outOfOfficePage.assertNoPeriods()
  })
})

function getSupervisorName() {
  return `${supervisor.lastName} ${supervisor.firstName}`
}

async function getCitizenMessageEditor(citizenPage: Page) {
  await enduserLogin(citizenPage, testAdult)
  await citizenPage.goto(config.enduserMessagesUrl)
  const messagesPage = new CitizenMessagesPage(citizenPage, 'desktop')
  const editor = await messagesPage.createNewMessage()
  const supervisorName = getSupervisorName()
  await editor.selectRecipients([supervisorName])
  return { editor, messagesPage }
}
