// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import ChildInformationPage from '../../pages/employee/child-information'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let guardianInformation: GuardianInformationPage
let childInformation: ChildInformationPage

let fixtures: AreaAndPersonFixtures

beforeEach(async () => {
  await resetServiceState()
  fixtures = await initializeAreaAndPersonData()

  const admin = await Fixture.employeeAdmin().save()
  page = await Page.open()
  await employeeLogin(page, admin)

  guardianInformation = new GuardianInformationPage(page)
  childInformation = new ChildInformationPage(page)
})

describe('Foster parents', () => {
  test('adding, editing and deleting foster children works', async () => {
    await guardianInformation.navigateToGuardian(
      fixtures.testAdultRestricted.id
    )
    const section = await guardianInformation.openCollapsible('fosterChildren')

    const startDate = LocalDate.of(2020, 5, 14)
    await section.addFosterChild(fixtures.testChild.firstName, startDate, null)
    await section.addFosterChild(fixtures.testChild2.firstName, startDate, null)
    await section.assertRowExists(fixtures.testChild.id, startDate, null)
    await section.assertRowExists(fixtures.testChild2.id, startDate, null)

    const newEndDate = LocalDate.of(2022, 10, 4)
    await section.editFosterChild(fixtures.testChild.id, startDate, newEndDate)
    await section.assertRowExists(fixtures.testChild.id, startDate, newEndDate)
    await section.assertRowExists(fixtures.testChild2.id, startDate, null)

    await section.deleteFosterChild(fixtures.testChild2.id)
    await section.assertRowExists(fixtures.testChild.id, startDate, newEndDate)
    await section.assertRowDoesNotExist(fixtures.testChild2.id)
  })

  test('added foster child is shown in child information', async () => {
    await guardianInformation.navigateToGuardian(
      fixtures.testAdultRestricted.id
    )
    await guardianInformation
      .openCollapsible('fosterChildren')
      .then((section) =>
        section.addFosterChild(
          fixtures.testChild.firstName,
          LocalDate.todayInSystemTz(),
          null
        )
      )

    await childInformation.navigateToChild(fixtures.testChild.id)
    await childInformation
      .openCollapsible('guardians')
      .then((section) =>
        section.assertFosterParentExists(
          fixtures.testAdultRestricted.id,
          LocalDate.todayInSystemTz(),
          null
        )
      )
  })
})
