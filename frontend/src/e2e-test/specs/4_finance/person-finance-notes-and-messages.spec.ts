// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { Fixture, testAdult } from '../../dev-api/fixtures'
import {
  createFinanceNotes,
  createMessageAccounts,
  resetServiceState
} from '../../generated/api-clients'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let guardianPage: GuardianInformationPage

beforeEach(async () => {
  await resetServiceState()
  await Fixture.person(testAdult).saveAdult()
  const financeAdmin = await Fixture.employee().financeAdmin().save()
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
    const notes = await guardianPage.openCollapsible('financeNotesAndMessages')

    await notes.checkNoteCreatedAt(0, createdAtThird)
    await notes.checkNoteCreatedAt(1, createdAtSecond)
    await notes.checkNoteCreatedAt(2, createdAtFirst)
  })
})
