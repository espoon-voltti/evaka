// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import {
  execSimpleApplicationActions,
  getApplication,
  insertApplications,
  insertDaycarePlacementFixtures,
  resetDatabase
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  applicationFixture,
  daycareFixture,
  uuidv4
} from '../../dev-api/fixtures'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenHeader from '../../pages/citizen/citizen-header'
import {
  fullDaycareForm,
  minimalDaycareForm
} from '../../utils/application-forms'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let applicationsPage: CitizenApplicationsPage
let fixtures: AreaAndPersonFixtures

const testFileName = 'test_file.png'
const testFilePath = `src/e2e-test/assets/${testFileName}`
const mockedNow = HelsinkiDateTime.of(2021, 4, 1, 15, 0)
const mockedDate = mockedNow.toLocalDate()

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  page = await Page.open({ mockedTime: mockedNow.toSystemTzDate() })
  await enduserLogin(page)
  header = new CitizenHeader(page)
  applicationsPage = new CitizenApplicationsPage(page)
})

describe('Citizen daycare applications', () => {
  test('Sending incomplete daycare application gives validation error', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
    await editorPage.goToVerification()
    await editorPage.assertErrorsExist()
  })

  test('Minimal valid daycare application can be sent', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()

    const applicationForm = minimalDaycareForm({
      otherGuardianAgreementStatus: 'AGREED'
    })
    await editorPage.fillData(applicationForm.form)
    await editorPage.assertChildAddress('Kamreerintie 1, 00340 Espoo')
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const application = await getApplication(applicationId)
    applicationForm.validateResult(application, [
      fixtures.enduserChildFixtureKaarina
    ])
  })

  test('Full valid daycare application can be sent', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()

    const applicationForm = fullDaycareForm({
      otherGuardianAgreementStatus: 'AGREED'
    })
    await editorPage.fillData(applicationForm.form)
    await editorPage.assertChildAddress('Kamreerintie 1, 00340 Espoo')
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const application = await getApplication(applicationId)
    applicationForm.validateResult(application, [
      fixtures.enduserChildFixtureKaarina
    ])
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
    await execSimpleApplicationActions(
      application.id,
      ['move-to-waiting-placement'],
      mockedNow
    )

    await header.selectTab('applications')
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
        startDate: mockedDate.subYears(1),
        endDate: mockedDate.addYears(1),
        placeGuarantee: false
      }
    ])

    await header.selectTab('applications')
    await applicationsPage.assertTransferNotificationIsShown(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
  })

  test('A warning is shown if preferred start date is very soon', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )

    await editorPage.fillData(
      minimalDaycareForm({
        otherGuardianAgreementStatus: 'AGREED'
      }).form
    )
    await editorPage.setPreferredStartDate(mockedDate.format())
    await editorPage.assertPreferredStartDateWarningIsShown(true)
  })

  test('A validation error message is shown if preferred start date is not valid', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )

    await editorPage.setPreferredStartDate(mockedDate.addYears(2).format())
    await editorPage.assertPreferredStartDateInfo('Valitse aikaisempi päivä')

    await editorPage.setPreferredStartDate(mockedDate.addMonths(4).format())
    await editorPage.assertPreferredStartDateInfo(undefined)
  })
  //TODO
  test('Citizen cannot move preferred start date before a previously selected date', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()
    await editorPage.fillData(
      minimalDaycareForm({
        otherGuardianAgreementStatus: 'AGREED'
      }).form
    )
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    await applicationsPage.editApplication(applicationId)
    await editorPage.setPreferredStartDate(mockedDate.subDays(1).format())
    await editorPage.assertPreferredStartDateInfo('Valitse myöhäisempi päivä')
  })

  test('Previously selected preferred units exists', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()
    await editorPage.fillData(
      minimalDaycareForm({
        otherGuardianAgreementStatus: 'AGREED'
      }).form
    )
    await editorPage.saveAsDraftButton.click()
    await editorPage.modalOkBtn.click()
    await page.reload()
    await applicationsPage.editApplication(applicationId)
    await editorPage.assertSelectedPreferredUnits([daycareFixture.id])
  })

  test('Application can be made for restricted child', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixturePorriHatterRestricted.id,
      'DAYCARE'
    )
    await editorPage.fillData(minimalDaycareForm().form)
    await editorPage.assertChildAddress('')
    await editorPage.verifyAndSend({ hasOtherGuardian: false })
    await editorPage.waitUntilLoaded()
  })

  test('Urgent application attachment can be uploaded and downloaded by citizen', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixturePorriHatterRestricted.id,
      'DAYCARE'
    )
    await editorPage.fillData(minimalDaycareForm().form)
    await editorPage.markApplicationUrgentAndAddAttachment(testFilePath)
    await editorPage.assertAttachmentUploaded(testFileName)
    await editorPage.goToVerification()
    await editorPage.assertUrgencyFileDownload()
  })

  test('Other guardian can see an application after it has been sent, and cannot see person details', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()

    const applicationForm = minimalDaycareForm({
      otherGuardianAgreementStatus: 'AGREED'
    })
    await editorPage.fillData(applicationForm.form)
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const otherGuardianPage = await Page.open({
      mockedTime: mockedNow.toSystemTzDate()
    })
    await enduserLogin(
      otherGuardianPage,
      fixtures.enduserChildJariOtherGuardianFixture.ssn
    )

    const applications = new CitizenApplicationsPage(otherGuardianPage)
    await applications.assertApplicationExists(applicationId)
    const applicationReadView =
      await applications.viewApplication(applicationId)

    await applicationReadView.unitPreferenceSection.waitUntilVisible()
    await applicationReadView.contactInfoSection.waitUntilHidden()
  })

  test('Application can be saved as draft', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.enduserChildFixtureJari.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()
    await editorPage.fillData({
      ...minimalDaycareForm().form,
      contactInfo: {
        guardianPhone: '040123456789',
        noGuardianEmail: true,
        otherGuardianAgreementStatus: 'AGREED'
      }
    })
    await editorPage.saveAsDraftButton.click()
    await editorPage.modalOkBtn.click()
    await applicationsPage.editApplication(applicationId)
    await editorPage.guardianPhoneInput.assertValueEquals('040123456789')
  })
})
