// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import {
  insertApplications,
  insertDaycarePlacementFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import {
  applicationFixture,
  createDaycarePlacementFixture,
  daycareFixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import Home from '../../pages/home'
import { format, sub } from 'date-fns'
import { OtherGuardianAgreementStatus } from 'e2e-test-common/dev-api/types'
import ReportsPage from '../../pages/reports'
import { employeeLogin, seppoAdmin } from '../../config/users'
import { ApplicationStatus } from 'lib-common/api-types/application/enums'

let fixtures: AreaAndPersonFixtures

const home = new Home()
const reports = new ReportsPage()

let applicationId: string | null = null

fixture('Placement sketching report')
  .meta({ type: 'regression', subType: 'reports' })
  .before(async () => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()
  })
  .afterEach(logConsoleMessages)

test('Not placed child shows on report', async (t) => {
  const now = new Date()

  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'PRESCHOOL'
  )

  const preferredStartDate = new Date(now.getFullYear(), 7, 13)
  applicationId = uuidv4()

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

  await employeeLogin(t, seppoAdmin, home.homePage('admin'))
  await reports.selectReportsTab()
  await reports.selectPlacementSketchingReport()
  await reports.assertPlacementSketchingRow(
    createdApplication.id,
    preferredUnit.name,
    `${fixtures.enduserChildFixtureJari.lastName} ${fixtures.enduserChildFixtureJari.firstName}`
  )
})

test('Placed child shows on report', async (t) => {
  const now = new Date()

  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'PRESCHOOL'
  )

  const preferredStartDate = new Date(now.getFullYear(), 7, 13)
  applicationId = uuidv4()

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

  await employeeLogin(t, seppoAdmin, home.homePage('admin'))
  await reports.selectReportsTab()
  await reports.selectPlacementSketchingReport()
  await reports.assertPlacementSketchingRow(
    createdApplication.id,
    preferredUnit.name,
    `${fixtures.enduserChildFixtureJari.lastName} ${fixtures.enduserChildFixtureJari.firstName}`,
    currentUnit.name
  )
})
