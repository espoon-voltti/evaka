// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import {
  deleteApplication,
  insertApplications,
  insertDaycarePlacementFixtures
} from '../../dev-api'
import {
  applicationFixture,
  applicationFixtureId,
  createDaycarePlacementFixture,
  daycareFixture,
  uuidv4
} from '../../dev-api/fixtures'
import Home from '../../pages/home'
import { format, sub } from 'date-fns'
import {
  ApplicationStatus,
  OtherGuardianAgreementStatus
} from '../../dev-api/types'
import ReportsPage from '../../pages/reports'
import { seppoAdminRole } from '../../config/users'

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

const home = new Home()
const reports = new ReportsPage()

fixture('Placement sketching report')
  .meta({ type: 'regression', subType: 'reports' })
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

// TODO lisää 2. 3. vaihtoehto, linkki hakemukselle, e2e

test('Placed child shows on report', async (t) => {
  const now = new Date()

  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'preschool'
  )

  const preferredStartDate = new Date(now.getFullYear(), 7, 13)

  const createdApplication = {
    ...fixture,
    form: {
      ...fixture.form,
      preferredStartDate: preferredStartDate.toISOString(),
      otherGuardianAgreementStatus: 'AGREED' as OtherGuardianAgreementStatus
    },
    sentDate: format(sub(preferredStartDate, { months: 4 }), 'yyyy-MM-dd'),
    status: 'SENT' as ApplicationStatus,
    id: uuidv4()
  }

  await insertApplications([createdApplication])

  const placementStartDate = new Date(now.getFullYear(), 0, 1)
  const preferredUnit = daycareFixture
  const currentUnit = preferredUnit

  const daycarePlacementFixture = createDaycarePlacementFixture(
    createdApplication.childId,
    preferredUnit.id,
    format(placementStartDate, 'yyyy-MM-dd')
  )
  await insertDaycarePlacementFixtures([daycarePlacementFixture])

  await t.useRole(seppoAdminRole)
  await reports.selectReportsTab()
  await reports.selectPlacementSketchingReport()
  await reports.assertPlacementSketchingRow(
    preferredUnit.id,
    createdApplication.childId,
    preferredUnit.name,
    `${fixtures.enduserChildFixtureJari.lastName} ${fixtures.enduserChildFixtureJari.firstName}`,
    currentUnit.name
  )
})
