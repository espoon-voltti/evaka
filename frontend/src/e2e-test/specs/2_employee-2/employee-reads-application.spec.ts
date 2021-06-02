// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Home from '../../pages/home'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import { applicationFixture } from 'e2e-test-common/dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { insertApplications, resetDatabase } from 'e2e-test-common/dev-api'
import { employeeLogin, seppoAdmin } from '../../config/users'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'

const home = new Home()
const applicationReadView = new ApplicationReadView()

let fixtures: AreaAndPersonFixtures

fixture('Employee reads applications')
  .meta({ type: 'regression', subType: 'applications2' })
  .beforeEach(async () => {
    await resetDatabase()
    ;[fixtures] = await initializeAreaAndPersonData()
  })
  .afterEach(logConsoleMessages)

test('Daycare application opens by link', async (t) => {
  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture
  )
  await insertApplications([fixture])
  await employeeLogin(t, seppoAdmin, home.homePage('admin'))

  await applicationReadView.openApplicationByLink(fixture.id)
  await applicationReadView.assertPageTitle('Varhaiskasvatushakemus')
})

test('Preschool application opens by link', async (t) => {
  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'PRESCHOOL'
  )
  await insertApplications([fixture])
  await employeeLogin(t, seppoAdmin, home.homePage('admin'))

  await applicationReadView.openApplicationByLink(fixture.id)
  await applicationReadView.assertPageTitle('Esiopetushakemus')
})

test('Other VTJ guardian information is shown', async (t) => {
  const fixture = applicationFixture(
    fixtures.familyWithTwoGuardians.children[0],
    fixtures.familyWithTwoGuardians.guardian,
    fixtures.familyWithTwoGuardians.otherGuardian
  )
  await insertApplications([fixture])
  await employeeLogin(t, seppoAdmin, home.homePage('admin'))

  await applicationReadView.openApplicationByLink(fixture.id)
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

test('If there is no other VTJ guardian it is mentioned', async (t) => {
  const fixture = applicationFixture(
    fixtures.enduserChildFixtureKaarina,
    fixtures.enduserGuardianFixture
  )
  await insertApplications([fixture])
  await employeeLogin(t, seppoAdmin, home.homePage('admin'))

  await applicationReadView.openApplicationByLink(fixture.id)
  await applicationReadView.assertPageTitle('Varhaiskasvatushakemus')
  await applicationReadView.assertOtherVtjGuardianMissing()
})
