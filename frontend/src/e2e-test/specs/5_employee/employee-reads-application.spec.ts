// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  applicationFixture,
  familyWithTwoGuardians,
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testChild2,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createApplications,
  resetServiceState
} from '../../generated/api-clients'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let applicationReadView: ApplicationReadView

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  await Fixture.family(familyWithTwoGuardians).save()
  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open()
  await employeeLogin(page, admin)

  applicationReadView = new ApplicationReadView(page)
})

describe('Employee reads applications', () => {
  test('Daycare application opens by link', async () => {
    const fixture = applicationFixture(testChild, testAdult)
    await createApplications({ body: [fixture] })

    await applicationReadView.navigateToApplication(fixture.id)
    await applicationReadView.assertPageTitle('Varhaiskasvatushakemus')
  })

  test('Preschool application opens by link', async () => {
    const fixture = applicationFixture(
      testChild,
      testAdult,
      undefined,
      'PRESCHOOL'
    )
    await createApplications({ body: [fixture] })

    await applicationReadView.navigateToApplication(fixture.id)
    await applicationReadView.assertPageTitle('Esiopetushakemus')
  })

  test('Other VTJ guardian information is shown', async () => {
    const fixture = applicationFixture(
      familyWithTwoGuardians.children[0],
      familyWithTwoGuardians.guardian,
      familyWithTwoGuardians.otherGuardian
    )
    await createApplications({ body: [fixture] })

    await applicationReadView.navigateToApplication(fixture.id)
    await applicationReadView.assertPageTitle('Varhaiskasvatushakemus')

    await applicationReadView.assertOtherVtjGuardian(
      `${familyWithTwoGuardians.otherGuardian.lastName} ${familyWithTwoGuardians.otherGuardian.firstName}`,
      familyWithTwoGuardians.otherGuardian.phone,
      familyWithTwoGuardians.otherGuardian.email!
    )

    await applicationReadView.assertGivenOtherGuardianInfo(
      familyWithTwoGuardians.otherGuardian.phone,
      familyWithTwoGuardians.otherGuardian.email!
    )
  })

  test('If there is no other VTJ guardian it is mentioned', async () => {
    const fixture = applicationFixture(testChild2, testAdult)
    await createApplications({ body: [fixture] })

    await applicationReadView.navigateToApplication(fixture.id)
    await applicationReadView.assertPageTitle('Varhaiskasvatushakemus')
    await applicationReadView.assertOtherVtjGuardianMissing()
  })
})
