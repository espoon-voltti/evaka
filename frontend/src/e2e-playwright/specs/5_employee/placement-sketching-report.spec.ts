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
import { format, sub } from 'date-fns'
import { OtherGuardianAgreementStatus } from 'e2e-test-common/dev-api/types'
import ReportsPage from '../../pages/employee/reports'
import { ApplicationStatus } from 'lib-common/generated/enums'
import { employeeLogin } from '../../utils/user'
import { Page } from '../../utils/page'
import config from 'e2e-test-common/config'

let fixtures: AreaAndPersonFixtures
let page: Page

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  const admin = (await Fixture.employeeAdmin().save()).data

  page = await Page.open()
  await employeeLogin(page, admin)
})

async function openPlacementSketchingReport() {
  await page.goto(config.employeeUrl + '/reports')
  const reports = new ReportsPage(page)
  return await reports.openPlacementSketchingReport()
}

describe('Placement sketching report', () => {
  test('Not placed child shows on report', async () => {
    const now = new Date()

    const fixture = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'PRESCHOOL'
    )

    const preferredStartDate = new Date(now.getFullYear(), 7, 13)
    const applicationId = uuidv4()

    const createdApplication = {
      ...fixture,
      form: {
        ...fixture.form,
        preferredStartDate: preferredStartDate.toISOString(),
        otherGuardianAgreementStatus: 'AGREED' as OtherGuardianAgreementStatus
      },
      sentDate: format(sub(preferredStartDate, { months: 4 }), 'yyyy-MM-dd'),
      status: 'SENT' as ApplicationStatus,
      id: applicationId
    }

    await insertApplications([createdApplication])

    const preferredUnit = daycareFixture

    const report = await openPlacementSketchingReport()
    await report.assertRow(
      createdApplication.id,
      preferredUnit.name,
      `${fixtures.enduserChildFixtureJari.lastName} ${fixtures.enduserChildFixtureJari.firstName}`
    )
  })

  test('Placed child shows on report', async () => {
    const now = new Date()

    const fixture = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'PRESCHOOL'
    )

    const preferredStartDate = new Date(now.getFullYear(), 7, 13)
    const applicationId = uuidv4()

    const createdApplication = {
      ...fixture,
      form: {
        ...fixture.form,
        preferredStartDate: preferredStartDate.toISOString(),
        otherGuardianAgreementStatus: 'AGREED' as OtherGuardianAgreementStatus
      },
      sentDate: format(sub(preferredStartDate, { months: 4 }), 'yyyy-MM-dd'),
      status: 'SENT' as ApplicationStatus,
      id: applicationId
    }

    await insertApplications([createdApplication])

    const placementStartDate = new Date(now.getFullYear(), 0, 1)
    const preferredUnit = daycareFixture
    const currentUnit = preferredUnit

    const daycarePlacementFixture = createDaycarePlacementFixture(
      uuidv4(),
      createdApplication.childId,
      preferredUnit.id,
      format(placementStartDate, 'yyyy-MM-dd')
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
