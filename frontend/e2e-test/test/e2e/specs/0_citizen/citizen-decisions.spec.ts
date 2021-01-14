// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { logConsoleMessages } from '../../utils/fixture'
import { enduserRole } from '../../config/users'
import CitizenHomePage from '../../pages/citizen/citizen-homepage'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { deleteApplication, insertApplications } from '../../dev-api'
import {
  applicationFixture,
  enduserChildFixtureJari,
  Fixture,
  EmployeeBuilder
} from '../../dev-api/fixtures'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import { format } from 'date-fns'

const citizenHomePage = new CitizenHomePage()
const citizenDecisionsPage = new CitizenDecisionsPage()

let applicationId: string
let employee: EmployeeBuilder
let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Citizen decisions')
  .meta({ type: 'regression', subType: 'citizen-decisions' })
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()

    employee = await Fixture.employee()
      .with({ roles: ['SERVICE_WORKER'] })
      .save()
  })
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    await deleteApplication(applicationId)
  })
  .after(async () => {
    await Fixture.cleanup()
    await cleanUp()
  })

test('Citizen sees her decisions', async (t) => {
  const application = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture
  )
  applicationId = application.id
  await insertApplications([application])

  const decision = await Fixture.decision()
    .with({
      applicationId: application.id,
      employeeId: employee.data.id,
      unitId: application.form.apply.preferredUnits[0],
      startDate: application.form.preferredStartDate
    })
    .save()

  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.decisions)

  await t
    .expect(citizenDecisionsPage.unresolvedDecisionsInfoBox.textContent)
    .contains('1 päätös odottaa vahvistusta')

  await citizenDecisionsPage.assertApplicationDecision(
    applicationId,
    decision.data.id,
    `${enduserChildFixtureJari.firstName} ${enduserChildFixtureJari.lastName}`,
    'Päätös varhaiskasvatuksesta',
    format(new Date(application.form.preferredStartDate), 'dd.MM.yyyy'),
    'Vahvistettavana huoltajalla'
  )
})
