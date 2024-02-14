// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { insertIncomeStatements } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareFixture,
  enduserChildFixtureJari,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createDaycarePlacements,
  resetDatabase
} from '../../generated/api-clients'
import ChildInformationPage from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let personId: UUID

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  personId = fixtures.enduserChildFixtureJari.id

  const financeAdmin = await Fixture.employeeFinanceAdmin().save()

  page = await Page.open()
  await employeeLogin(page, financeAdmin.data)
  await page.goto(config.employeeUrl + '/child-information/' + personId)
})

describe('Child profile income statements', () => {
  test('Shows income statements', async () => {
    const daycarePlacementFixture = createDaycarePlacementFixture(
      uuidv4(),
      enduserChildFixtureJari.id,
      daycareFixture.id
    )
    await createDaycarePlacements({ body: [daycarePlacementFixture] })

    await insertIncomeStatements(enduserChildFixtureJari.id, [
      {
        type: 'CHILD_INCOME',
        otherInfo: 'Test info',
        startDate: LocalDate.todayInSystemTz(),
        endDate: LocalDate.todayInSystemTz(),
        attachmentIds: []
      }
    ])

    const profilePage = new ChildInformationPage(page)
    await profilePage.navigateToChild(enduserChildFixtureJari.id)
    await profilePage.waitUntilLoaded()

    const incomeSection = await profilePage.openCollapsible('income')
    await incomeSection.assertIncomeStatementRowCount(1)
    const incomeStatementPage = await incomeSection.openIncomeStatement(0)
    await incomeStatementPage.assertChildIncomeStatement('Test info', 0)
  })
})
