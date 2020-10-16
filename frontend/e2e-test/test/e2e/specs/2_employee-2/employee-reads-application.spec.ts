// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Home from '../../pages/home'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId
} from '../../dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { deleteApplication, insertApplications } from '../../dev-api'
import { seppoAdminRole } from '../../config/users'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import { ApplicationWorkbenchPage } from '../../pages/admin/application-workbench-page'

const home = new Home()
const applicationWorkbench = new ApplicationWorkbenchPage()
const applicationReadView = new ApplicationReadView()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Employee reads applications')
  .meta({ type: 'regression', subType: 'applications2' })
  .page(home.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
  })
  .afterEach(logConsoleMessages)
  .afterEach(async () => {
    await deleteApplication(applicationFixtureId)
  })
  .after(async () => {
    await cleanUp()
  })

test('Daycare application opens by link', async (t) => {
  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture
  )
  await insertApplications([fixture])
  await t.useRole(seppoAdminRole)

  await applicationReadView.openApplicationByLink(fixture.id)
  await applicationReadView.assertPageTitle('Varhaiskasvatushakemus')
})

test('Preschool application opens by link', async (t) => {
  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'preschool'
  )
  await insertApplications([fixture])
  await t.useRole(seppoAdminRole)

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
  await t.useRole(seppoAdminRole)

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
  await t.useRole(seppoAdminRole)

  await applicationReadView.openApplicationByLink(fixture.id)
  await applicationReadView.assertPageTitle('Varhaiskasvatushakemus')
  await applicationReadView.assertOtherVtjGuardianMissing()
})

test('Move application from review queue to placement, place it, and confirm the placement without creating a decision', async (t) => {
  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture
  )
  const applicationId = fixture.id
  await insertApplications([fixture])
  await t.useRole(seppoAdminRole)

  await applicationWorkbench.moveToWaitingPlacement(applicationId)

  await applicationWorkbench.openPlacementQueue()
  await applicationWorkbench.openDaycarePlaementById(applicationId)
  await applicationWorkbench.placementPage.placeIn(0)
  await applicationWorkbench.placementPage.sendPlacement()

  await applicationWorkbench.openDecisionQueue()
  await applicationWorkbench.confirmPlacementWithoutDecision(applicationId)

  await applicationReadView.openApplicationByLink(applicationId)
  await t
    .expect(applicationReadView.applicationStatus.innerText)
    .contains('Paikka vastaanotettu')
})
