// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'
import type { UUID } from 'lib-common/types'

import config from '../../config'
import {
  createDaycarePlacementFixture,
  testDaycare,
  testAdult,
  Fixture,
  testChildRestricted,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createDaycarePlacements,
  insertGuardians,
  resetServiceState
} from '../../generated/api-clients'
import type { DevPerson } from '../../generated/api-types'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let personId: UUID
let child: DevPerson

const mockedNow = HelsinkiDateTime.of(2022, 7, 31, 13, 0)

beforeEach(async () => {
  await resetServiceState()

  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChildRestricted]
  }).save()
  personId = testAdult.id
  child = testChildRestricted

  const financeAdmin = await Fixture.employee().financeAdmin().save()

  page = await Page.open({ mockedTime: mockedNow })
  await employeeLogin(page, financeAdmin)
})

describe('Guardian income statements', () => {
  test("Shows placed child's income statements", async () => {
    const daycarePlacementFixture = createDaycarePlacementFixture(
      randomId(),
      child.id,
      testDaycare.id
    )
    await createDaycarePlacements({ body: [daycarePlacementFixture] })

    await insertGuardians({
      body: [
        {
          guardianId: testAdult.id,
          childId: child.id
        }
      ]
    })

    await Fixture.incomeStatement({
      personId: child.id,
      data: {
        type: 'CHILD_INCOME',
        otherInfo: 'Test other info',
        startDate: mockedNow.toLocalDate(),
        endDate: mockedNow.toLocalDate(),
        attachmentIds: []
      }
    }).save()

    await page.goto(config.employeeUrl + '/profile/' + personId)
    const guardianPage = new GuardianInformationPage(page)
    await guardianPage.navigateToGuardian(testAdult.id)

    const incomesSection = guardianPage.getCollapsible('incomes')
    await incomesSection.assertIncomeStatementChildName(
      0,
      `${child.firstName} ${child.lastName}`
    )
    await incomesSection.assertIncomeStatementRowCount(1)
    const incomeStatementPage = await incomesSection.openIncomeStatement(0)
    await incomeStatementPage.assertChildIncomeStatement('Test other info', 0)
  })
})
