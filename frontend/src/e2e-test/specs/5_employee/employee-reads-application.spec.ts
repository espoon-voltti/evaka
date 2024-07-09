// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { applicationFixture, Fixture } from '../../dev-api/fixtures'
import {
  createApplications,
  resetServiceState
} from '../../generated/api-clients'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures
let page: Page
let applicationReadView: ApplicationReadView

beforeEach(async () => {
  await resetServiceState()
  fixtures = await initializeAreaAndPersonData()
  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open()
  await employeeLogin(page, admin)

  applicationReadView = new ApplicationReadView(page)
})

describe('Employee reads applications', () => {
  test('Daycare application opens by link', async () => {
    const fixture = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture
    )
    await createApplications({ body: [fixture] })

    await applicationReadView.navigateToApplication(fixture.id)
    await applicationReadView.assertPageTitle('Varhaiskasvatushakemus')
  })

  test('Preschool application opens by link', async () => {
    const fixture = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'PRESCHOOL'
    )
    await createApplications({ body: [fixture] })

    await applicationReadView.navigateToApplication(fixture.id)
    await applicationReadView.assertPageTitle('Esiopetushakemus')
  })

  test('Other VTJ guardian information is shown', async () => {
    const fixture = applicationFixture(
      fixtures.familyWithTwoGuardians.children[0],
      fixtures.familyWithTwoGuardians.guardian,
      fixtures.familyWithTwoGuardians.otherGuardian
    )
    await createApplications({ body: [fixture] })

    await applicationReadView.navigateToApplication(fixture.id)
    await applicationReadView.assertPageTitle('Varhaiskasvatushakemus')

    await applicationReadView.assertOtherVtjGuardian(
      `${fixtures.familyWithTwoGuardians.otherGuardian.lastName} ${fixtures.familyWithTwoGuardians.otherGuardian.firstName}`,
      fixtures.familyWithTwoGuardians.otherGuardian.phone,
      fixtures.familyWithTwoGuardians.otherGuardian.email
    )

    await applicationReadView.assertGivenOtherGuardianInfo(
      fixtures.familyWithTwoGuardians.otherGuardian.phone,
      fixtures.familyWithTwoGuardians.otherGuardian.email
    )
  })

  test('If there is no other VTJ guardian it is mentioned', async () => {
    const fixture = applicationFixture(
      fixtures.enduserChildFixtureKaarina,
      fixtures.enduserGuardianFixture
    )
    await createApplications({ body: [fixture] })

    await applicationReadView.navigateToApplication(fixture.id)
    await applicationReadView.assertPageTitle('Varhaiskasvatushakemus')
    await applicationReadView.assertOtherVtjGuardianMissing()
  })
})
