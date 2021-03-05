// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import AdminHome from '../../pages/home'
import EmployeeHome from '../../pages/employee/home'
import config from '../../config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { supervisor, uuidv4 } from '../../dev-api/fixtures'
import {
  applicationFixture,
  createDaycarePlacementFixture,
  daycareFixture,
  daycareGroupFixture,
  decisionFixture,
  enduserGuardianFixture,
  enduserChildFixtureJari
} from '../../dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { t } from 'testcafe'
import { DaycarePlacement } from '../../dev-api/types'
import {
  deleteEmployeeFixture,
  insertApplications,
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertDecisionFixtures,
  insertEmployeeFixture,
  deleteApplication
} from '../../dev-api'
import { seppoAdminRole } from '../../config/users'
import GuardianPage from '../../pages/employee/guardian-page'

const adminHome = new AdminHome()
const employeeHome = new EmployeeHome()
const guardianPage = new GuardianPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

let daycarePlacementFixture: DaycarePlacement
let supervisorId: string
let applicationId: string

fixture('Employee - Guardian Information')
  .meta({ type: 'regression', subType: 'guardianinformation' })
  .page(adminHome.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    await insertDaycareGroupFixtures([daycareGroupFixture])
    supervisorId = await insertEmployeeFixture(supervisor)

    daycarePlacementFixture = createDaycarePlacementFixture(
      uuidv4(),
      fixtures.enduserChildFixtureJari.id,
      daycareFixture.id
    )
    const application = applicationFixture(
      enduserChildFixtureJari,
      enduserGuardianFixture
    )
    applicationId = application.id
    await insertDaycarePlacementFixtures([daycarePlacementFixture])
    await insertApplications([application])
    await insertDecisionFixtures([
      {
        ...decisionFixture(
          application.id,
          application.form.preferredStartDate,
          application.form.preferredStartDate
        ),
        employeeId: supervisorId
      }
    ])
  })
  .beforeEach(async () => {
    await t.useRole(seppoAdminRole)
    await employeeHome.navigateToGuardianInformation(enduserGuardianFixture.id)
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    applicationId && (await deleteApplication(applicationId))
    await cleanUp()
    await deleteEmployeeFixture(config.supervisorExternalId)
  })

test('guardian information is shown', async () => {
  const expectedChildName = `${enduserChildFixtureJari.firstName} ${enduserChildFixtureJari.lastName}`
  await guardianPage.assertPersonInfo(
    enduserGuardianFixture.lastName,
    enduserGuardianFixture.firstName,
    enduserGuardianFixture.ssn
  )
  await guardianPage.containsDependantChild(expectedChildName)
  await guardianPage.containsApplicationSummary(
    expectedChildName,
    daycareFixture.name
  )
  await guardianPage.containsDecisionForChild(
    expectedChildName,
    daycareFixture.name,
    'Odottaa vastausta'
  )
})
