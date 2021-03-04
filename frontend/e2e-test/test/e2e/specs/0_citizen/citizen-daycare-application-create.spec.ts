// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import CitizenHomePage from '../../pages/citizen/citizen-homepage'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenNewApplicationPage from '../../pages/citizen/citizen-application-new'
import CitizenApplicationEditor from '../../pages/citizen/citizen-application-editor'
import CitizenApplicationVerificationPage from '../../pages/citizen/citizen-application-verification'
import { logConsoleMessages } from '../../utils/fixture'
import { enduserRole } from '../../config/users'
import {
  deleteApplication,
  execSimpleApplicationActions,
  getApplication,
  getDecisionsByApplication,
  insertApplications,
  runPendingAsyncJobs
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  applicationFixture,
  daycareFixture,
  Fixture
} from '../../dev-api/fixtures'
import {
  FormInput,
  fullDaycareForm,
  minimalDaycareForm
} from '../../utils/application-forms'
import { add, sub } from 'date-fns'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenDecisionResponsePage from '../../pages/citizen/citizen-decision-response'
import path from 'path'
import { RequestLogger } from 'testcafe'

const logger = RequestLogger(
  { url: /\/attachments\/[a-fA-F0-9-]+\/download$/, method: 'GET' },
  { logResponseHeaders: true }
)

const testFilePath = '../../assets/test_file.jpg'
const testFileName = path.basename(testFilePath)

const citizenHomePage = new CitizenHomePage()
const citizenApplicationsPage = new CitizenApplicationsPage()
const citizenNewApplicationPage = new CitizenNewApplicationPage()
const citizenApplicationEditor = new CitizenApplicationEditor()
const citizenApplicationVerification = new CitizenApplicationVerificationPage()
const citizenDecisionsPage = new CitizenDecisionsPage()
const citizenDecisionResponsePage = new CitizenDecisionResponsePage()

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
    if (applicationId) await deleteApplication(applicationId)
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

  await citizenApplicationEditor.assertChildStreetAddress(
    'Kamreerintie 1, 00340 Espoo'
  )

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

test('Notification on duplicate application is visible', async (t) => {
  await t.useRole(enduserRole)

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

  await execSimpleApplicationActions(applicationId, [
    'move-to-waiting-placement',
    'create-default-placement-plan',
    'send-decisions-without-proposal'
  ])

  await runPendingAsyncJobs()

  await t.click(citizenHomePage.nav.applications)
  await citizenApplicationsPage.createApplication(
    fixtures.enduserChildFixtureJari.id
  )
  await citizenNewApplicationPage.selectType('PRESCHOOL')

  await t
    .expect(citizenNewApplicationPage.duplicateApplicationNotification.visible)
    .ok()
})

test('Notification on transfer application is visible', async (t) => {
  await t.useRole(enduserRole)

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

  await execSimpleApplicationActions(applicationId, [
    'move-to-waiting-placement',
    'create-default-placement-plan',
    'send-decisions-without-proposal'
  ])

  await runPendingAsyncJobs()

  const decisions = await getDecisionsByApplication(applicationId)
  const decisionId = decisions[0]?.id

  await t.click(citizenHomePage.nav.decisions)
  await t.click(citizenDecisionsPage.goRespondToDecisionBtn(applicationId))

  await citizenDecisionResponsePage.acceptDecision(decisionId)

  await runPendingAsyncJobs()

  await t.click(citizenHomePage.nav.applications)
  await citizenApplicationsPage.createApplication(
    fixtures.enduserChildFixtureJari.id
  )
  await citizenNewApplicationPage.selectType('PRESCHOOL')

  await t
    .expect(citizenNewApplicationPage.transferApplicationNotification.visible)
    .ok()
})

test('A validation warning is shown if preferred start date is not valid', async (t) => {
  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.applications)
  await citizenApplicationsPage.createApplication(
    fixtures.enduserChildFixtureJari.id
  )
  await citizenNewApplicationPage.createApplication('DAYCARE')
  applicationId = await citizenApplicationEditor.getApplicationId()

  await citizenApplicationEditor.fillData(fullDaycareForm.form)

  await citizenApplicationEditor.setPreferredStartDate(new Date())

  await citizenApplicationEditor.assertPreferredStartDateProcessingWarningIsShown(
    true
  )
  await citizenApplicationEditor.assertPreferredStartDateInputInfo(
    true,
    'Aloituspäivä ei ole sallittu'
  )

  await citizenApplicationEditor.setPreferredStartDate(
    add(new Date(), { months: 4 })
  )
  await citizenApplicationEditor.assertPreferredStartDateProcessingWarningIsShown(
    false
  )

  await citizenApplicationEditor.assertPreferredStartDateInputInfo(false)

  const validDate = add(new Date(), { months: 6 })

  await citizenApplicationEditor.setPreferredStartDate(validDate)

  await citizenApplicationEditor.verifyAndSend()
  await citizenApplicationEditor.acknowledgeSendSuccess()

  await citizenApplicationsPage.openApplication(applicationId)

  await citizenApplicationEditor.setPreferredStartDate(
    sub(validDate, { days: 1 })
  )

  // The validation text is not shown in the test browser without this
  await t.click(citizenApplicationEditor.applicationTypeTitle)

  await citizenApplicationEditor.assertPreferredStartDateInputInfo(
    true,
    'Aloituspäivä ei ole sallittu'
  )
})

test('Application can be made for restricted child', async (t) => {
  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.applications)

  await citizenApplicationsPage.createApplication(
    fixtures.enduserChildFixturePorriHatterRestricted.id
  )
  await citizenNewApplicationPage.createApplication('DAYCARE')
  applicationId = await citizenApplicationEditor.getApplicationId()

  await citizenApplicationEditor.fillData(fullDaycareForm.form)
  await citizenApplicationEditor.setPreferredStartDate(
    add(new Date(), { months: 6 })
  )

  await citizenApplicationEditor.assertChildStreetAddress(null)

  await citizenApplicationEditor.verifyAndSend()
  await citizenApplicationEditor.acknowledgeSendSuccess()
})

test.requestHooks(logger)(
  'Urgent application attachment can be uploaded and downloaded by citizen',
  async (t) => {
    await t.useRole(enduserRole)
    await t.click(citizenHomePage.nav.applications)
    await citizenApplicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id
    )
    await citizenNewApplicationPage.createApplication('DAYCARE')
    applicationId = await citizenApplicationEditor.getApplicationId()

    const formWithUrgencyOnly: FormInput = {
      serviceNeed: {
        urgent: true
      }
    }

    await citizenApplicationEditor.fillData(formWithUrgencyOnly)
    await citizenApplicationEditor.uploadUrgentFile(testFilePath)
    await citizenApplicationEditor.assertUrgentFileHasBeenUploaded(testFileName)

    await t.click(
      citizenApplicationEditor.urgentAttachmentDownloadButton(testFileName)
    )
    await t
      .expect(
        logger.contains(
          (r) =>
            r.response.statusCode === 200 &&
            r.response.headers['content-disposition']?.includes(
              `filename=${testFileName}`
            )
        )
      )
      .ok()
  }
)

test.requestHooks(logger)(
  'Urgent application attachment can be downloaded by citizen on application verification page',
  async (t) => {
    await t.useRole(enduserRole)
    await t.click(citizenHomePage.nav.applications)
    await citizenApplicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id
    )
    await citizenNewApplicationPage.createApplication('DAYCARE')
    applicationId = await citizenApplicationEditor.getApplicationId()

    const minimalFormWithUrgency: FormInput = {
      ...minimalDaycareForm.form,
      serviceNeed: {
        ...minimalDaycareForm.form.serviceNeed,
        urgent: true
      }
    }

    await citizenApplicationEditor.fillData(minimalFormWithUrgency)
    await citizenApplicationEditor.uploadUrgentFile(testFilePath)
    await citizenApplicationEditor.assertUrgentFileHasBeenUploaded(testFileName)

    await citizenApplicationEditor.goToVerify()

    await t.click(
      citizenApplicationVerification.serviceNeedUrgencyAttachmentDownload(
        testFileName
      )
    )
    await t
      .expect(
        logger.contains(
          (r) =>
            r.response.statusCode === 200 &&
            r.response.headers['content-disposition']?.includes(
              `filename=${testFileName}`
            )
        )
      )
      .ok()
  }
)
