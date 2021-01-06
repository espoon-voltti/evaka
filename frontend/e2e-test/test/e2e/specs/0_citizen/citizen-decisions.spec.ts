// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { logConsoleMessages } from '../../utils/fixture'
import { enduserRole } from '../../config/users'
import CitizenHomePage from '../../pages/citizen/citizen-homepage'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  deleteApplication,
  deleteEmployeeById,
  insertApplications,
  insertEmployeeFixture
} from '../../dev-api'
import {
  applicationFixture,
  enduserChildFixtureJari,
  enduserGuardianFixture,
  Fixture,
  supervisor
} from '../../dev-api/fixtures'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import { format } from 'date-fns'

const citizenHomePage = new CitizenHomePage()
const citizenDecisionsPage = new CitizenDecisionsPage()

let applicationId: string

fixture('Citizen decisions')
  .meta({ type: 'regression', subType: 'citizen-decisions' })
  .before(async () => {
    await initializeAreaAndPersonData()

    const uniqueSupervisor = {
      ...supervisor,
      email: `${Math.random().toString(36).substring(7)}@espoo.fi`
    }
    await insertEmployeeFixture(uniqueSupervisor)
  })
  .afterEach(logConsoleMessages)
  .afterEach(async () => {
    await deleteApplication(applicationId)
  })
  .after(async () => {
    await Fixture.cleanup()
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    await deleteEmployeeById(supervisor.id!)
  })

test('Citizen sees her decisions', async (t) => {
  const application = applicationFixture(
    enduserChildFixtureJari,
    enduserGuardianFixture
  )
  applicationId = application.id
  await insertApplications([application])

  const decision = await Fixture.decision()
    .with({
      applicationId: application.id,
      employeeId: supervisor.id,
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
