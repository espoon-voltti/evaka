// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import { insertApplications, resetDatabase } from 'e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { EmployeeDetail } from 'e2e-test-common/dev-api/types'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures
let admin: EmployeeDetail

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  admin = (await Fixture.employeeAdmin().save()).data
})

async function openPage() {
  const page = await Page.open()
  await employeeLogin(page, admin)
  await page.goto(config.adminUrl)
  return new ApplicationListView(page)
}

describe('Employee searches applications', () => {
  test('Duplicate applications are found', async () => {
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
    const applicationListView = await openPage()

    await applicationListView.specialFilterItems.duplicate.click()
    await applicationListView.assertApplicationIsVisible(applicationFixtureId)
    await applicationListView.assertApplicationIsVisible(duplicateFixture.id)
    await applicationListView.assertApplicationCount(2)
  })

  test('Care area filters work', async () => {
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
    const applicationListView = await openPage()

    await applicationListView.assertApplicationCount(3)

    await applicationListView.toggleArea(careArea1.data.name)
    await applicationListView.assertApplicationCount(1)
    await applicationListView.assertApplicationIsVisible(app1.id)

    await applicationListView.toggleArea(careArea2.data.name)
    await applicationListView.assertApplicationCount(2)
    await applicationListView.assertApplicationIsVisible(app1.id)
    await applicationListView.assertApplicationIsVisible(app2.id)
  })

  test('Unit filter works', async () => {
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
    const applicationListView = await openPage()

    await applicationListView.assertApplicationCount(2)

    await applicationListView.toggleUnit(daycare1.data.name)
    await applicationListView.assertApplicationIsVisible(app1.id)
    await applicationListView.assertApplicationCount(1)
  })

  test('Voucher application filter works', async () => {
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
    const applicationListView = await openPage()

    await applicationListView.voucherUnitFilter.noFilter.click()
    await applicationListView.assertApplicationCount(3)

    await applicationListView.voucherUnitFilter.firstChoice.click()
    await applicationListView.assertApplicationIsVisible(
      applicationWithVoucherUnitFirst.id
    )
    await applicationListView.assertApplicationCount(1)

    await applicationListView.voucherUnitFilter.voucherOnly.click()
    await applicationListView.assertApplicationIsVisible(
      applicationWithVoucherUnitFirst.id
    )
    await applicationListView.assertApplicationIsVisible(
      applicationWithVoucherUnitSecond.id
    )
    await applicationListView.assertApplicationCount(2)

    await applicationListView.voucherUnitFilter.voucherHide.click()
    await applicationListView.assertApplicationIsVisible(
      applicationWithNoVoucherUnit.id
    )
    await applicationListView.assertApplicationCount(1)
  })
})
