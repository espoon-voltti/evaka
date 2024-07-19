// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { startTest } from '../../browser'
import { Fixture, testAdult, testChild } from '../../dev-api/fixtures'
import ChildInformationPage from '../../pages/employee/child-information'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childInformation: ChildInformationPage
let guardianInformation: GuardianInformationPage
const mockedDate = LocalDate.of(2021, 4, 1)

beforeEach(async () => {
  await startTest()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()

  page = await Page.open({
    mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  const admin = await Fixture.employee().admin().save()
  await employeeLogin(page, admin)
  childInformation = new ChildInformationPage(page)
  guardianInformation = new GuardianInformationPage(page)
})

test('adding and removing evaka rights from guardians works', async () => {
  const child = testChild
  const blockedGuardian = testAdult
  await childInformation.navigateToChild(child.id)
  const childGuardiansSection =
    await childInformation.openCollapsible('guardians')
  await childGuardiansSection.removeGuardianEvakaRights(blockedGuardian.id)
  await childGuardiansSection.assertGuardianStatusDenied(blockedGuardian.id)
  await guardianInformation.navigateToGuardian(blockedGuardian.id)
  const guardianDependantsSection =
    await guardianInformation.openCollapsible('dependants')
  await guardianDependantsSection.assertDoesNotContainDependantChild(child.id)

  await childInformation.navigateToChild(child.id)
  await childInformation.openCollapsible('guardians')
  await childGuardiansSection.restoreGuardianEvakaRights(blockedGuardian.id)
  await childGuardiansSection.assertGuardianStatusAllowed(blockedGuardian.id)
  await guardianInformation.navigateToGuardian(blockedGuardian.id)
  await guardianInformation.openCollapsible('dependants')
  await guardianDependantsSection.assertContainsDependantChild(child.id)
})
