// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import { startTest } from '../../browser'
import {
  Fixture,
  testAdult,
  testAdultRestricted,
  testChild,
  testChild2
} from '../../dev-api/fixtures'
import ChildInformationPage from '../../pages/employee/child-information'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let guardianInformation: GuardianInformationPage
let childInformation: ChildInformationPage

beforeEach(async () => {
  await startTest()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  await Fixture.person(testAdultRestricted).saveAdult({
    updateMockVtjWithDependants: []
  })

  const admin = await Fixture.employee().admin().save()
  page = await Page.open()
  await employeeLogin(page, admin)

  guardianInformation = new GuardianInformationPage(page)
  childInformation = new ChildInformationPage(page)
})

describe('Foster parents', () => {
  test('adding, editing and deleting foster children works', async () => {
    await guardianInformation.navigateToGuardian(testAdultRestricted.id)
    const section = await guardianInformation.openCollapsible('fosterChildren')

    const startDate = LocalDate.of(2020, 5, 14)
    await section.addFosterChild(testChild.firstName, startDate, null)
    await section.addFosterChild(testChild2.firstName, startDate, null)
    await section.assertRowExists(testChild.id, startDate, null)
    await section.assertRowExists(testChild2.id, startDate, null)

    const newEndDate = LocalDate.of(2022, 10, 4)
    await section.editFosterChild(testChild.id, startDate, newEndDate)
    await section.assertRowExists(testChild.id, startDate, newEndDate)
    await section.assertRowExists(testChild2.id, startDate, null)

    await section.deleteFosterChild(testChild2.id)
    await section.assertRowExists(testChild.id, startDate, newEndDate)
    await section.assertRowDoesNotExist(testChild2.id)
  })

  test('added foster child is shown in child information', async () => {
    await guardianInformation.navigateToGuardian(testAdultRestricted.id)
    await guardianInformation
      .openCollapsible('fosterChildren')
      .then((section) =>
        section.addFosterChild(
          testChild.firstName,
          LocalDate.todayInSystemTz(),
          null
        )
      )

    await childInformation.navigateToChild(testChild.id)
    await childInformation
      .openCollapsible('guardians')
      .then((section) =>
        section.assertFosterParentExists(
          testAdultRestricted.id,
          LocalDate.todayInSystemTz(),
          null
        )
      )
  })
})
