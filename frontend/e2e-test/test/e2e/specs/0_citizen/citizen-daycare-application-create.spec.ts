// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import CitizenHomePage from '../../pages/citizen/citizen-homepage'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenNewApplicationPage from '../../pages/citizen/citizen-application-new'
import CitizenApplicationEditor from '../../pages/citizen/citizen-application-editor'
import { logConsoleMessages } from '../../utils/fixture'
import { enduserRole } from '../../config/users'
import { deleteApplication, getApplication } from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { Fixture } from '../../dev-api/fixtures'
import {
  fullDaycareForm,
  minimalDaycareForm
} from '../../utils/application-forms'

const citizenHomePage = new CitizenHomePage()
const citizenApplicationsPage = new CitizenApplicationsPage()
const citizenNewApplicationPage = new CitizenNewApplicationPage()
const citizenApplicationEditor = new CitizenApplicationEditor()

let applicationId: string
let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Citizen daycare applications create')
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

test('Sending invalid daycare application gives validation error', async (t) => {
  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.applications)

  await citizenApplicationsPage.createApplication(
    fixtures.enduserChildFixtureJari.id
  )
  await t
    .expect(citizenNewApplicationPage.title.textContent)
    .eql('Valitse hakemustyyppi')
  await citizenNewApplicationPage.createApplication('DAYCARE')

  applicationId = await citizenApplicationEditor.getApplicationId()

  await t
    .expect(citizenApplicationEditor.applicationTypeTitle.textContent)
    .eql('Varhaiskasvatushakemus')
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

test('Minimal valid daycare application can be sent', async (t) => {
  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.applications)
  await citizenApplicationsPage.createApplication(
    fixtures.enduserChildFixtureJari.id
  )
  await citizenNewApplicationPage.createApplication('DAYCARE')
  applicationId = await citizenApplicationEditor.getApplicationId()

  await citizenApplicationEditor.fillData(minimalDaycareForm.form)

  await citizenApplicationEditor.verifyAndSend()
  await citizenApplicationEditor.acknowledgeSendSuccess()

  const application = await getApplication(applicationId)
  minimalDaycareForm.validateResult(application)
})

test('Full valid daycare application can be sent', async (t) => {
  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.applications)
  await citizenApplicationsPage.createApplication(
    fixtures.enduserChildFixtureJari.id
  )
  await citizenNewApplicationPage.createApplication('DAYCARE')
  applicationId = await citizenApplicationEditor.getApplicationId()

  await citizenApplicationEditor.fillData(fullDaycareForm.form)

  await citizenApplicationEditor.verifyAndSend()
  await citizenApplicationEditor.acknowledgeSendSuccess()

  const application = await getApplication(applicationId)
  fullDaycareForm.validateResult(application)
})
