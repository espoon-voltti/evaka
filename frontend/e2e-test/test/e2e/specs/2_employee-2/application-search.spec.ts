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
  applicationFixtureId,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { deleteApplication, insertApplications } from '../../dev-api'
import { seppoAdminRole } from '../../config/users'
import ApplicationListView from '../../pages/employee/applications/application-list-view'

const home = new Home()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Employee searches applications')
  .meta({ type: 'regression', subType: 'applications2' })
  .page(home.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
  })
  .beforeEach(async (t) => {
    await t.useRole(seppoAdminRole)
  })
  .afterEach(logConsoleMessages)
  .afterEach(async () => {
    await deleteApplication(applicationFixtureId)
  })
  .after(async () => {
    await Fixture.cleanup()
    await cleanUp()
  })

test('Duplicate applications are found', async (t) => {
  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture
  )

  const duplicateFixture = {
    ...applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture
    ),
    id: '9dd0e1ba-9b3b-11ea-bb37-0242ac130222'
  }

  const nonDuplicateFixture = {
    ...applicationFixture(
      fixtures.enduserChildFixtureKaarina,
      fixtures.enduserGuardianFixture
    ),
    id: '9dd0e1ba-9b3b-11ea-bb37-0242ac130224'
  }

  await insertApplications([fixture, duplicateFixture, nonDuplicateFixture])

  try {
    await t.click(ApplicationListView.specialFilterItems.duplicate)
    await t
      .expect(ApplicationListView.application(applicationFixtureId).visible)
      .ok()
    await t
      .expect(ApplicationListView.application(duplicateFixture.id).visible)
      .ok()
    await t.expect(ApplicationListView.applications.count).eql(2)
  } finally {
    await deleteApplication(duplicateFixture.id)
    await deleteApplication(nonDuplicateFixture.id)
  }
})

test('Care area filters work', async (t) => {
  const careArea1 = await Fixture.careArea().save()
  const daycare1 = await Fixture.daycare().careArea(careArea1).save()
  const careArea2 = await Fixture.careArea().save()
  const daycare2 = await Fixture.daycare().careArea(careArea2).save()
  const careArea3 = await Fixture.careArea().save()
  const daycare3 = await Fixture.daycare().careArea(careArea3).save()

  const createApplicationForUnit = (unitId: string) => ({
    ...applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'daycare',
      null,
      [unitId]
    ),
    id: uuidv4()
  })

  const app1 = createApplicationForUnit(daycare1.data.id)
  const app2 = createApplicationForUnit(daycare2.data.id)
  const app3 = createApplicationForUnit(daycare3.data.id)

  await insertApplications([app1, app2, app3])
  await t.eval(() => location.reload())

  try {
    await t.click(ApplicationListView.areaFilterItems('All'))
    await t.expect(ApplicationListView.applications.count).eql(3)

    await t.click(ApplicationListView.areaFilterItems(careArea1.data.shortName))
    await t.expect(ApplicationListView.applications.count).eql(2)
    await t.expect(ApplicationListView.application(app2.id).visible).ok()
    await t.expect(ApplicationListView.application(app3.id).visible).ok()
  } finally {
    await deleteApplication(app1.id)
    await deleteApplication(app2.id)
    await deleteApplication(app3.id)
  }
})

test('Unit filter works', async (t) => {
  const careArea1 = await Fixture.careArea().save()
  const daycare1 = await Fixture.daycare().careArea(careArea1).save()
  const daycare2 = await Fixture.daycare().careArea(careArea1).save()

  const createApplicationForUnit = (unitId: string) => ({
    ...applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'daycare',
      null,
      [unitId]
    ),
    id: uuidv4()
  })

  const app1 = createApplicationForUnit(daycare1.data.id)
  const app2 = createApplicationForUnit(daycare2.data.id)

  await insertApplications([app1, app2])
  await t.eval(() => location.reload())

  try {
    await t.expect(ApplicationListView.applications.count).eql(2)

    await t.click(ApplicationListView.unitFilter)
    await t.typeText(ApplicationListView.unitFilter, daycare1.data.name, {
      replace: true
    })
    await t.pressKey('enter')
    await t.expect(ApplicationListView.application(app1.id).visible).ok()
    await t.expect(ApplicationListView.application(app2.id).visible).notOk()
    await t.expect(ApplicationListView.applications.count).eql(1)
  } finally {
    await deleteApplication(app1.id)
    await deleteApplication(app2.id)
  }
})
