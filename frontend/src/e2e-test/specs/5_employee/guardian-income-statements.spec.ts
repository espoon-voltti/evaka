// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { UUID } from 'lib-common/types'

import { startTest } from '../../browser'
import config from '../../config'
import {
  createDaycarePlacementFixture,
  testDaycare,
  testAdult,
  Fixture,
  uuidv4,
  testChildRestricted,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createDaycarePlacements,
  createIncomeStatements,
  insertGuardians
} from '../../generated/api-clients'
import { DevPerson } from '../../generated/api-types'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let personId: UUID
let child: DevPerson

const mockedNow = HelsinkiDateTime.of(2022, 7, 31, 13, 0)

beforeEach(async () => {
  await startTest()

  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
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
      uuidv4(),
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

    await createIncomeStatements({
      body: {
        personId: child.id,
        data: [
          {
            type: 'CHILD_INCOME',
            otherInfo: 'Test other info',
            startDate: mockedNow.toLocalDate(),
            endDate: mockedNow.toLocalDate(),
            attachmentIds: []
          }
        ]
      }
    })

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
