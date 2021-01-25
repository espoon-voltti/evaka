// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { logConsoleMessages } from '../../utils/fixture'
import { enduserRole } from '../../config/users'
import CitizenHomePage from '../../pages/citizen/citizen-homepage'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  deleteApplication,
  execSimpleApplicationActions,
  insertApplications,
  runPendingAsyncJobs
} from '../../dev-api'
import {
  applicationFixture,
  Fixture,
  daycareFixture
} from '../../dev-api/fixtures'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import { format } from 'date-fns'

const citizenHomePage = new CitizenHomePage()
const citizenApplicationsPage = new CitizenApplicationsPage()

let applicationId: string
let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Citizen applications list')
  .meta({ type: 'regression', subType: 'citizen-applications-list' })
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
  })
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    await deleteApplication(applicationId)
  })
  .after(async () => {
    await Fixture.cleanup()
    await cleanUp()
  })

test('Citizen sees her children and applications', async (t) => {
  const application = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'preschool',
    null,
    [daycareFixture.id],
    true
  )
  applicationId = application.id
  await insertApplications([application])

  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.applications)

  await t
    .expect(
      citizenApplicationsPage.childTitle(fixtures.enduserChildFixtureJari.id)
        .textContent
    )
    .contains(
      `${fixtures.enduserChildFixtureJari.firstName} ${fixtures.enduserChildFixtureJari.lastName}`
    )

  await citizenApplicationsPage.assertApplication(
    applicationId,
    'Esiopetushakemus',
    daycareFixture.name,
    format(new Date(application.form.preferredStartDate), 'dd.MM.yyyy'),
    'Lähetetty'
  )
})

test('Citizen sees a link to accept a decision', async (t) => {
  const application = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'daycare',
    null,
    [daycareFixture.id],
    true
  )
  applicationId = application.id
  await insertApplications([application])

  await execSimpleApplicationActions(applicationId, [
    'move-to-waiting-placement',
    'create-default-placement-plan',
    'send-decisions-without-proposal'
  ])
  await runPendingAsyncJobs()

  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.applications)

  await t
    .expect(
      citizenApplicationsPage.childTitle(fixtures.enduserChildFixtureJari.id)
        .textContent
    )
    .contains(
      `${fixtures.enduserChildFixtureJari.firstName} ${fixtures.enduserChildFixtureJari.lastName}`
    )

  await citizenApplicationsPage.assertApplication(
    applicationId,
    'Varhaiskasvatushakemus',
    daycareFixture.name,
    format(new Date(application.form.preferredStartDate), 'dd.MM.yyyy'),
    'Vahvistettavana huoltajalla'
  )
})
