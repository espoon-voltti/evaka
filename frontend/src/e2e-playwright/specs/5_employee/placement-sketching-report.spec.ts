// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  insertApplications,
  insertDaycarePlacementFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import {
  applicationFixture,
  createDaycarePlacementFixture,
  daycareFixture,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { Application } from 'e2e-test-common/dev-api/types'
import ReportsPage from '../../pages/employee/reports'
import { employeeLogin } from '../../utils/user'
import { Page } from '../../utils/page'
import config from 'e2e-test-common/config'
import LocalDate from 'lib-common/local-date'

let fixtures: AreaAndPersonFixtures
let page: Page

const mockToday = LocalDate.of(2021, 2, 1)

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  const admin = (await Fixture.employeeAdmin().save()).data

  page = await Page.open({ mockedTime: mockToday.toSystemTzDate() })
  await employeeLogin(page, admin)
})

async function openPlacementSketchingReport() {
  await page.goto(config.employeeUrl + '/reports')
  const reports = new ReportsPage(page)
  return await reports.openPlacementSketchingReport()
}

describe('Placement sketching report', () => {
  test('Not placed child shows on report', async () => {
    const fixture = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'PRESCHOOL',
      'AGREED'
    )

    const preferredStartDate = LocalDate.of(2021, 8, 13)
    const sentDate = preferredStartDate.subMonths(4)
    const applicationId = uuidv4()

    const createdApplication: Application = {
      ...fixture,
      form: {
        ...fixture.form,
        preferences: {
          ...fixture.form.preferences,
          preferredStartDate
        }
      },
      sentDate: sentDate.formatIso(),
      status: 'SENT',
      id: applicationId
    }

    await insertApplications([createdApplication])

    const preferredUnit = daycareFixture

    const report = await openPlacementSketchingReport()
    await page.pause()
    await report.assertRow(
      createdApplication.id,
      preferredUnit.name,
      `${fixtures.enduserChildFixtureJari.lastName} ${fixtures.enduserChildFixtureJari.firstName}`
    )
  })

  test('Placed child shows on report', async () => {
    const fixture = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'PRESCHOOL',
      'AGREED'
    )

    const preferredStartDate = LocalDate.of(2021, 8, 13)
    const sentDate = preferredStartDate.subMonths(4)
    const applicationId = uuidv4()

    const createdApplication: Application = {
      ...fixture,
      form: {
        ...fixture.form,
        preferences: {
          ...fixture.form.preferences,
          preferredStartDate
        }
      },
      sentDate: sentDate.formatIso(),
      status: 'SENT',
      id: applicationId
    }

    await insertApplications([createdApplication])

    const placementStartDate = LocalDate.of(2021, 1, 1)
    const preferredUnit = daycareFixture
    const currentUnit = preferredUnit

    const daycarePlacementFixture = createDaycarePlacementFixture(
      uuidv4(),
      createdApplication.childId,
      preferredUnit.id,
      placementStartDate.formatIso()
    )
    await insertDaycarePlacementFixtures([daycarePlacementFixture])

    const report = await openPlacementSketchingReport()
    await report.assertRow(
      createdApplication.id,
      preferredUnit.name,
      `${fixtures.enduserChildFixtureJari.lastName} ${fixtures.enduserChildFixtureJari.firstName}`,
      currentUnit.name
    )
  })
})
