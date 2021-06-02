// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import AdminHome from '../../pages/home'
import EmployeeHome from '../../pages/employee/home'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import { supervisor, uuidv4 } from 'e2e-test-common/dev-api/fixtures'
import {
  applicationFixture,
  createDaycarePlacementFixture,
  daycareFixture,
  daycareGroupFixture,
  decisionFixture,
  enduserGuardianFixture,
  enduserChildFixtureJari
} from 'e2e-test-common/dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { t } from 'testcafe'
import { DaycarePlacement } from 'e2e-test-common/dev-api/types'
import {
  insertApplications,
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertDecisionFixtures,
  insertEmployeeFixture,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { employeeLogin, seppoAdmin } from '../../config/users'
import GuardianPage from '../../pages/employee/guardian-page'

const adminHome = new AdminHome()
const employeeHome = new EmployeeHome()
const guardianPage = new GuardianPage()

let fixtures: AreaAndPersonFixtures

let daycarePlacementFixture: DaycarePlacement
let supervisorId: string

fixture('Employee - Guardian Information')
  .meta({ type: 'regression', subType: 'guardianinformation' })
  .beforeEach(async () => {
    await resetDatabase()
    ;[fixtures] = await initializeAreaAndPersonData()
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
    await employeeLogin(t, seppoAdmin, adminHome.homePage('admin'))
    await employeeHome.navigateToGuardianInformation(enduserGuardianFixture.id)
  })
  .afterEach(logConsoleMessages)

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
