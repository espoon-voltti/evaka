// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import config from 'e2e-test-common/config'
import {
  execSimpleApplicationActions,
  getApplication,
  insertApplications,
  insertDaycarePlacementFixtures,
  resetDatabase,
  runPendingAsyncJobs
} from 'e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import { enduserLogin } from 'e2e-playwright/utils/user'
import {
  fullDaycareForm,
  minimalDaycareForm
} from 'e2e-playwright/utils/application-forms'
import { newBrowserContext } from '../../browser'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import { applicationFixture, uuidv4 } from 'e2e-test-common/dev-api/fixtures'
import LocalDate from 'lib-common/local-date'

let page: Page
let header: CitizenHeader
let applicationsPage: CitizenApplicationsPage
let fixtures: AreaAndPersonFixtures

const testFileName = 'test_file.png'
const testFilePath = `src/e2e-playwright/assets/${testFileName}`

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  page = await (await newBrowserContext()).newPage()
  await page.goto(config.enduserUrl)
  header = new CitizenHeader(page)
  applicationsPage = new CitizenApplicationsPage(page)
})
afterEach(async () => {
  await page.close()
})

describe('Citizen daycare applications', () => {
  test('Sending incomplete daycare application gives validation error', async () => {
    await enduserLogin(page)
    await header.applicationsTab.click()
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
    await editorPage.goToVerification()
    await editorPage.assertErrorsExist()
  })

  test('Minimal valid daycare application can be sent', async () => {
    await enduserLogin(page)
    await header.applicationsTab.click()
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()

    await editorPage.fillData(minimalDaycareForm.form)
    await editorPage.assertChildAddress('Kamreerintie 1, 00340 Espoo')
    await editorPage.verifyAndSend()

    const application = await getApplication(applicationId)
    minimalDaycareForm.validateResult(application)
  })

  test('Full valid daycare application can be sent', async () => {
    await enduserLogin(page)
    await header.applicationsTab.click()
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()

    await editorPage.fillData(fullDaycareForm.form)
    await editorPage.assertChildAddress('Kamreerintie 1, 00340 Espoo')
    await editorPage.verifyAndSend()

    const application = await getApplication(applicationId)
    fullDaycareForm.validateResult(application)
  })

  test('Notification on duplicate application is visible', async () => {
    const application = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'DAYCARE',
      null,
      [fixtures.daycareFixture.id],
      true
    )
    await insertApplications([application])
    await execSimpleApplicationActions(application.id, [
      'move-to-waiting-placement'
    ])
    await runPendingAsyncJobs()

    await enduserLogin(page)
    await header.applicationsTab.click()
    await applicationsPage.assertDuplicateWarningIsShown(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
  })

  test('Notification on transfer application is visible', async () => {
    await insertDaycarePlacementFixtures([
      {
        id: uuidv4(),
        type: 'DAYCARE',
        childId: fixtures.enduserChildFixtureJari.id,
        unitId: fixtures.daycareFixture.id,
        startDate: LocalDate.today().subYears(1).formatIso(),
        endDate: LocalDate.today().addYears(1).formatIso()
      }
    ])

    await enduserLogin(page)
    await header.applicationsTab.click()
    await applicationsPage.assertTransferNotificationIsShown(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
  })

  test('A warning is shown if preferred start date is very soon', async () => {
    await enduserLogin(page)
    await header.applicationsTab.click()
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )

    await editorPage.fillData(minimalDaycareForm.form)
    await editorPage.setPreferredStartDate(LocalDate.today().format())
    await editorPage.assertPreferredStartDateWarningIsShown(true)
  })

  test('A validation error message is shown if preferred start date is not valid', async () => {
    await enduserLogin(page)
    await header.applicationsTab.click()
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )

    await editorPage.setPreferredStartDate(
      LocalDate.today().addYears(2).format()
    )
    await editorPage.assertPreferredStartDateInfo(
      'Aloituspäivä ei ole sallittu'
    )

    await editorPage.setPreferredStartDate(
      LocalDate.today().addMonths(4).format()
    )
    await editorPage.assertPreferredStartDateInfo(undefined)
  })

  test('Citizen cannot move preferred start date before a previously selected date', async () => {
    await enduserLogin(page)
    await header.applicationsTab.click()
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()
    await editorPage.fillData(minimalDaycareForm.form)
    await editorPage.verifyAndSend()

    await applicationsPage.editApplication(applicationId)
    await editorPage.setPreferredStartDate(
      LocalDate.parseFiOrThrow(
        minimalDaycareForm.form.serviceNeed?.preferredStartDate ?? ''
      )
        ?.subDays(1)
        .format()
    )
    await editorPage.assertPreferredStartDateInfo(
      'Aloituspäivä ei ole sallittu'
    )
  })

  test('Application can be made for restricted child', async () => {
    await enduserLogin(page)
    await header.applicationsTab.click()
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixturePorriHatterRestricted.id,
      'DAYCARE'
    )
    await editorPage.fillData(minimalDaycareForm.form)
    await editorPage.assertChildAddress('')
    await editorPage.verifyAndSend()
  })

  test('Urgent application attachment can be uploaded and downloaded by citizen', async () => {
    await enduserLogin(page)
    await header.applicationsTab.click()
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixturePorriHatterRestricted.id,
      'DAYCARE'
    )
    await editorPage.fillData(minimalDaycareForm.form)
    await editorPage.markApplicationUrgentAndAddAttachment(testFilePath)
    await editorPage.assertAttachmentUploaded(testFileName)
    await editorPage.goToVerification()
    await editorPage.assertUrgencyFileDownload(testFileName)
  })
})
