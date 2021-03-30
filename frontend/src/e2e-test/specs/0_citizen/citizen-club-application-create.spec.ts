// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import CitizenHomePage from '../../pages/citizen/citizen-homepage'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenNewApplicationPage from '../../pages/citizen/citizen-application-new'
import CitizenApplicationEditor from '../../pages/citizen/citizen-application-editor'
import { logConsoleMessages } from '../../utils/fixture'
import { enduserRole } from '../../config/users'
import { deleteApplication, getApplication } from 'e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import { Fixture } from 'e2e-test-common/dev-api/fixtures'
import { fullClubForm, minimalClubForm } from '../../utils/application-forms'

const citizenHomePage = new CitizenHomePage()
const citizenApplicationsPage = new CitizenApplicationsPage()
const citizenNewApplicationPage = new CitizenNewApplicationPage()
const citizenApplicationEditor = new CitizenApplicationEditor()

let applicationId: string
let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Citizen club applications create')
  .meta({ type: 'regression', subType: 'citizen-applications-create' })
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

test('Sending invalid club application gives validation error', async (t) => {
  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.applications)

  await citizenApplicationsPage.createApplication(
    fixtures.enduserChildFixtureJari.id
  )
  await t
    .expect(citizenNewApplicationPage.title.textContent)
    .eql('Valitse hakemustyyppi')
  await citizenNewApplicationPage.createApplication('CLUB')

  applicationId = await citizenApplicationEditor.getApplicationId()

  await t
    .expect(citizenApplicationEditor.applicationTypeTitle.textContent)
    .eql('Kerhohakemus')
  await t
    .expect(citizenApplicationEditor.applicationChildNameTitle.textContent)
    .eql(
      `${fixtures.enduserChildFixtureJari.firstName} ${fixtures.enduserChildFixtureJari.lastName}`
    )

  await citizenApplicationEditor.goToVerify()
  await t
    .expect(citizenApplicationEditor.applicationHasErrorsTitle.visible)
    .ok()
})

test('Minimal valid club application can be sent', async (t) => {
  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.applications)
  await citizenApplicationsPage.createApplication(
    fixtures.enduserChildFixtureJari.id
  )
  await citizenNewApplicationPage.createApplication('CLUB')
  applicationId = await citizenApplicationEditor.getApplicationId()

  await citizenApplicationEditor.fillData(minimalClubForm.form)

  await citizenApplicationEditor.verifyAndSend()
  await citizenApplicationEditor.acknowledgeSendSuccess()

  const application = await getApplication(applicationId)
  minimalClubForm.validateResult(application)
})

test('Full valid club application can be sent', async (t) => {
  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.applications)
  await citizenApplicationsPage.createApplication(
    fixtures.enduserChildFixtureJari.id
  )
  await citizenNewApplicationPage.createApplication('CLUB')
  applicationId = await citizenApplicationEditor.getApplicationId()

  await citizenApplicationEditor.fillData(fullClubForm.form)

  await citizenApplicationEditor.verifyAndSend()
  await citizenApplicationEditor.acknowledgeSendSuccess()

  const application = await getApplication(applicationId)
  fullClubForm.validateResult(application)
})
