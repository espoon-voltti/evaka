// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { logConsoleMessages } from '../../utils/fixture'
import { enduserRole } from '../../config/users'
import CitizenHomePage from '../../pages/citizen/citizen-homepage'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  deleteApplication,
  execSimpleApplicationActions,
  insertApplications,
  resetDatabase,
  runPendingAsyncJobs
} from 'e2e-test-common/dev-api'
import {
  applicationFixture,
  daycareFixture
} from 'e2e-test-common/dev-api/fixtures'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import { format } from 'date-fns'

const citizenHomePage = new CitizenHomePage()
const citizenApplicationsPage = new CitizenApplicationsPage()

let applicationId: string
let fixtures: AreaAndPersonFixtures

fixture('Citizen applications list')
  .meta({ type: 'regression', subType: 'citizen-applications-list' })
  .before(async () => {
    await resetDatabase()
    ;[fixtures] = await initializeAreaAndPersonData()
  })
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    await deleteApplication(applicationId)
  })

test('Citizen sees her children and applications', async (t) => {
  const application = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'PRESCHOOL',
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
    'DAYCARE',
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

test('Citizen can delete a draft application', async (t) => {
  const application = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'DAYCARE',
    null,
    [daycareFixture.id],
    true,
    'CREATED'
  )
  applicationId = application.id
  await insertApplications([application])

  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.applications)

  await t
    .expect(
      citizenApplicationsPage.removeApplicationButton(applicationId).textContent
    )
    .contains('Poista hakemus')

  // Check that cancel does nothing
  await t.click(citizenApplicationsPage.removeApplicationButton(applicationId))
  await t.click(citizenApplicationsPage.modalCancelBtn)
  await t
    .expect(
      citizenApplicationsPage.removeApplicationButton(applicationId).textContent
    )
    .contains('Poista hakemus')

  // Delete application
  await t.click(citizenApplicationsPage.removeApplicationButton(applicationId))
  await t.click(citizenApplicationsPage.modalOkBtn)

  await citizenApplicationsPage.assertApplicationDoesNotExist(applicationId)
})

test('Citizen can cancel a sent application', async (t) => {
  const application = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'DAYCARE',
    null,
    [daycareFixture.id],
    true,
    'SENT'
  )
  applicationId = application.id
  await insertApplications([application])

  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.applications)

  await t
    .expect(
      citizenApplicationsPage.removeApplicationButton(applicationId).textContent
    )
    .contains('Peruuta hakemus')

  // Check that cancel does nothing
  await t.click(citizenApplicationsPage.removeApplicationButton(applicationId))
  await t.click(citizenApplicationsPage.modalCancelBtn)
  await t
    .expect(
      citizenApplicationsPage.removeApplicationButton(applicationId).textContent
    )
    .contains('Peruuta hakemus')

  // Cance application
  await t.click(citizenApplicationsPage.removeApplicationButton(applicationId))
  await t.click(citizenApplicationsPage.modalOkBtn)

  await citizenApplicationsPage.assertApplicationDoesNotExist(applicationId)
})
