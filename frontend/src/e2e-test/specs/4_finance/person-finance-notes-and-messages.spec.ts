// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import { Fixture, testAdult } from '../../dev-api/fixtures'
import {
  createFinanceNotes,
  createMessageAccounts,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let guardianPage: GuardianInformationPage
let financeAdmin: DevEmployee

beforeEach(async () => {
  await resetServiceState()
  await Fixture.person(testAdult).saveAdult()
  financeAdmin = await Fixture.employee().financeAdmin().save()
  await createMessageAccounts()

  page = await Page.open({})
  await employeeLogin(page, financeAdmin)

  await page.goto(config.employeeUrl)
  guardianPage = new GuardianInformationPage(page)
})

describe('person finance notes', () => {
  test('Notes are sorted by created date', async () => {
    const createdAtFirst = HelsinkiDateTime.now().subHours(3 * 24)
    const createdAtSecond = createdAtFirst.addHours(24)
    const createdAtThird = createdAtSecond.addHours(24)

    const createFinanceNoteFixture = async (createdAt: HelsinkiDateTime) => {
      await createFinanceNotes(
        {
          body: [{ personId: testAdult.id, content: 'foobar' }]
        },
        {
          mockedTime: createdAt
        }
      )
    }

    await createFinanceNoteFixture(createdAtFirst)
    await createFinanceNoteFixture(createdAtSecond)
    await createFinanceNoteFixture(createdAtThird)

    await guardianPage.navigateToGuardian(testAdult.id)
    const notesAndMessages = await guardianPage.openCollapsible(
      'financeNotesAndMessages'
    )

    await notesAndMessages.checkNoteCreatedAt(0, createdAtThird)
    await notesAndMessages.checkNoteCreatedAt(1, createdAtSecond)
    await notesAndMessages.checkNoteCreatedAt(2, createdAtFirst)
  })
})

describe('person finance messages', () => {
  test('Message threads are sorted by latest message sent date', async () => {
    const mockedTime1 = HelsinkiDateTime.of(2025, 3, 1, 10, 0, 0, 0)
    page = await Page.open({ mockedTime: mockedTime1 })
    await employeeLogin(page, financeAdmin)
    await page.goto(config.employeeUrl)
    guardianPage = new GuardianInformationPage(page)
    await guardianPage.navigateToGuardian(testAdult.id)
    let notesAndMessages = await guardianPage.openCollapsible(
      'financeNotesAndMessages'
    )

    let messageEditor = await notesAndMessages.openNewMessageEditor()
    await messageEditor.sendNewMessage({
      title: 'First message sent',
      content: 'First message sent'
    })
    await runPendingAsyncJobs(mockedTime1.addMinutes(1))

    const mockedTime2 = HelsinkiDateTime.of(2025, 3, 1, 11, 0, 0, 0)
    page = await Page.open({ mockedTime: mockedTime2 })
    await employeeLogin(page, financeAdmin)
    await page.goto(config.employeeUrl)
    guardianPage = new GuardianInformationPage(page)
    await guardianPage.navigateToGuardian(testAdult.id)
    notesAndMessages = await guardianPage.openCollapsible(
      'financeNotesAndMessages'
    )

    messageEditor = await notesAndMessages.openNewMessageEditor()
    await messageEditor.sendNewMessage({
      title: 'Second message sent',
      content: 'Second message sent'
    })
    await runPendingAsyncJobs(mockedTime2.addMinutes(1))

    const mockedTime3 = HelsinkiDateTime.of(2025, 3, 1, 12, 0, 0, 0)
    page = await Page.open({ mockedTime: mockedTime3 })
    await employeeLogin(page, financeAdmin)
    await page.goto(config.employeeUrl)
    guardianPage = new GuardianInformationPage(page)
    await guardianPage.navigateToGuardian(testAdult.id)
    notesAndMessages = await guardianPage.openCollapsible(
      'financeNotesAndMessages'
    )

    messageEditor = await notesAndMessages.openReplyMessageEditor()
    await messageEditor.sendNewMessage({
      title: 'Reply to first message',
      content: 'Reply to first message'
    })
    await runPendingAsyncJobs(mockedTime3.addMinutes(1))

    await notesAndMessages.checkThreadLastMessageSentAt(0, mockedTime3)
    await notesAndMessages.checkThreadLastMessageSentAt(1, mockedTime2)
  })
})
