// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Home from '../../pages/home'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { insertApplications, resetDatabase } from 'e2e-test-common/dev-api'
import { employeeLogin, seppoAdmin } from '../../config/users'
import ApplicationListView from '../../pages/employee/applications/application-list-view'

const home = new Home()

let fixtures: AreaAndPersonFixtures

fixture('Employee searches applications')
  .meta({ type: 'regression', subType: 'applications2' })
  .beforeEach(async (t) => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()
    await employeeLogin(t, seppoAdmin, home.homePage('admin'))
  })
  .afterEach(logConsoleMessages)

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

  await t.click(ApplicationListView.specialFilterItems.duplicate)
  await t
    .expect(ApplicationListView.application(applicationFixtureId).visible)
    .ok()
  await t
    .expect(ApplicationListView.application(duplicateFixture.id).visible)
    .ok()
  await t.expect(ApplicationListView.applications.count).eql(2)
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
      'DAYCARE',
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

  await t.expect(ApplicationListView.applications.count).eql(3)

  await ApplicationListView.toggleArea(careArea1.data.name)
  await t.expect(ApplicationListView.applications.count).eql(1)
  await t.expect(ApplicationListView.application(app1.id).visible).ok()

  await ApplicationListView.toggleArea(careArea2.data.name)
  await t.expect(ApplicationListView.applications.count).eql(2)
  await t.expect(ApplicationListView.application(app1.id).visible).ok()
  await t.expect(ApplicationListView.application(app2.id).visible).ok()
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
      'DAYCARE',
      null,
      [unitId]
    ),
    id: uuidv4()
  })

  const app1 = createApplicationForUnit(daycare1.data.id)
  const app2 = createApplicationForUnit(daycare2.data.id)

  await insertApplications([app1, app2])
  await t.eval(() => location.reload())

  await t.expect(ApplicationListView.applications.count).eql(2)

  await ApplicationListView.toggleUnit(daycare1.data.name)
  await t.expect(ApplicationListView.application(app1.id).visible).ok()
  await t.expect(ApplicationListView.application(app2.id).visible).notOk()
  await t.expect(ApplicationListView.applications.count).eql(1)
})

test('Voucher application filter works', async (t) => {
  const careArea1 = await Fixture.careArea().save()
  const voucherUnit = await Fixture.daycare()
    .with({ providerType: 'PRIVATE_SERVICE_VOUCHER' })
    .careArea(careArea1)
    .save()
  const municipalUnit = await Fixture.daycare()
    .with({ providerType: 'MUNICIPAL' })
    .careArea(careArea1)
    .save()

  const applicationWithVoucherUnitFirst = {
    ...applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'DAYCARE',
      null,
      [voucherUnit.data.id]
    ),
    id: uuidv4()
  }
  const applicationWithVoucherUnitSecond = {
    ...applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'DAYCARE',
      null,
      [municipalUnit.data.id, voucherUnit.data.id]
    ),
    id: uuidv4()
  }
  const applicationWithNoVoucherUnit = {
    ...applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'DAYCARE',
      null,
      [municipalUnit.data.id]
    ),
    id: uuidv4()
  }

  await insertApplications([
    applicationWithVoucherUnitFirst,
    applicationWithVoucherUnitSecond,
    applicationWithNoVoucherUnit
  ])
  await t.eval(() => location.reload())

  await t.click(ApplicationListView.voucherUnitFilter.noFilter)
  await t.expect(ApplicationListView.applications.count).eql(3)

  await t.click(ApplicationListView.voucherUnitFilter.firstChoice)
  await t.expect(ApplicationListView.applications.count).eql(1)
  await t
    .expect(
      ApplicationListView.application(applicationWithVoucherUnitFirst.id)
        .visible
    )
    .ok()
  await t
    .expect(
      ApplicationListView.application(applicationWithVoucherUnitSecond.id)
        .visible
    )
    .notOk({ timeout: 200 })
  await t
    .expect(
      ApplicationListView.application(applicationWithNoVoucherUnit.id).visible
    )
    .notOk({ timeout: 200 })

  await t.click(ApplicationListView.voucherUnitFilter.voucherOnly)
  await t.expect(ApplicationListView.applications.count).eql(2)
  await t
    .expect(
      ApplicationListView.application(applicationWithVoucherUnitFirst.id)
        .visible
    )
    .ok()
  await t
    .expect(
      ApplicationListView.application(applicationWithVoucherUnitSecond.id)
        .visible
    )
    .ok()
  await t
    .expect(
      ApplicationListView.application(applicationWithNoVoucherUnit.id).visible
    )
    .notOk({ timeout: 200 })

  await t.click(ApplicationListView.voucherUnitFilter.voucherHide)
  await t.expect(ApplicationListView.applications.count).eql(1)
  await t
    .expect(
      ApplicationListView.application(applicationWithNoVoucherUnit.id).visible
    )
    .ok()
  await t
    .expect(
      ApplicationListView.application(applicationWithVoucherUnitFirst.id)
        .visible
    )
    .notOk({ timeout: 200 })
  await t
    .expect(
      ApplicationListView.application(applicationWithVoucherUnitSecond.id)
        .visible
    )
    .notOk({ timeout: 200 })
})
