// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  testDaycare,
  testChild,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createDaycarePlacements,
  createIncomeStatements,
  resetServiceState
} from '../../generated/api-clients'
import ChildInformationPage from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let personId: UUID

beforeEach(async () => {
  await resetServiceState()

  await initializeAreaAndPersonData()
  await Fixture.person().with(testChild).saveChild()
  personId = testChild.id

  const financeAdmin = await Fixture.employeeFinanceAdmin().save()

  page = await Page.open()
  await employeeLogin(page, financeAdmin)
  await page.goto(config.employeeUrl + '/child-information/' + personId)
})

describe('Child profile income statements', () => {
  test('Shows income statements', async () => {
    const daycarePlacementFixture = createDaycarePlacementFixture(
      uuidv4(),
      testChild.id,
      testDaycare.id
    )
    await createDaycarePlacements({ body: [daycarePlacementFixture] })

    await createIncomeStatements({
      body: {
        personId: testChild.id,
        data: [
          {
            type: 'CHILD_INCOME',
            otherInfo: 'Test info',
            startDate: LocalDate.todayInSystemTz(),
            endDate: LocalDate.todayInSystemTz(),
            attachmentIds: []
          }
        ]
      }
    })

    const profilePage = new ChildInformationPage(page)
    await profilePage.navigateToChild(testChild.id)
    await profilePage.waitUntilLoaded()

    const incomeSection = await profilePage.openCollapsible('income')
    await incomeSection.assertIncomeStatementRowCount(1)
    const incomeStatementPage = await incomeSection.openIncomeStatement(0)
    await incomeStatementPage.assertChildIncomeStatement('Test info', 0)
  })
})
