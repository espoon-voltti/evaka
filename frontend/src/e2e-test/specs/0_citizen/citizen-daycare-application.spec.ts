// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'

import { execSimpleApplicationActions } from '../../dev-api'
import {
  applicationFixture,
  testDaycare,
  testAdult,
  Fixture,
  testAdult2,
  testChild,
  testChild2,
  testChildRestricted,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createApplications,
  createDaycarePlacements,
  getApplication,
  resetServiceState,
  runJobs,
  setPersonEmail
} from '../../generated/api-clients'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenPersonalDetailsPage from '../../pages/citizen/citizen-personal-details'
import {
  fullDaycareForm,
  minimalDaycareForm
} from '../../utils/application-forms'
import { getVerificationCodeFromEmail } from '../../utils/email'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let applicationsPage: CitizenApplicationsPage

const testFileName = 'test_file.png'
const testFilePath = `src/e2e-test/assets/${testFileName}`
const mockedNow = HelsinkiDateTime.of(2021, 4, 1, 15, 0)
const mockedDate = mockedNow.toLocalDate()

beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2, testChildRestricted]
  }).save()
  await testAdult2.saveAdult({
    updateMockVtjWithDependants: [testChild]
  })

  page = await Page.open({ mockedTime: mockedNow })
  await enduserLogin(page, testAdult)
  header = new CitizenHeader(page)
  applicationsPage = new CitizenApplicationsPage(page)
})

describe('Citizen daycare applications', () => {
  test('Sending incomplete daycare application gives validation error', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'DAYCARE'
    )
    await editorPage.goToVerification()
    await editorPage.assertErrorsExist()
  })

  test('If user has no email selected in settings the application assumes user has no email', async () => {
    await header.selectTab('personal-details')
    const personalDetailsPage = new CitizenPersonalDetailsPage(page)
    const section = personalDetailsPage.personalDetailsSection
    await section.editPersonalData(
      {
        preferredName: testAdult.firstName.split(' ')[1],
        phone: '123123123',
        backupPhone: '456456',
        email: null // This sets the no email flag and email to ''
      },
      true
    )

    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'DAYCARE'
    )
    await editorPage.goToVerification()
    await editorPage.assertErrorsExist()
    await editorPage.openSection('contactInfo')
    await page.findByDataQa('guardianEmail-input-info').waitUntilHidden()
  })

  test('If user has not selected any email setting in own settings the application requires it by default', async () => {
    await setPersonEmail({
      body: { personId: testAdult.id, email: null }
    })
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'DAYCARE'
    )
    await editorPage.goToVerification()
    await editorPage.assertErrorsExist()
    await editorPage.openSection('contactInfo')
    await page.findByDataQa('guardianEmail-input-info').waitUntilVisible()
  })

  test('Minimal valid daycare application can be sent', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()

    const applicationForm = minimalDaycareForm({
      otherGuardianAgreementStatus: 'AGREED'
    })
    await editorPage.fillData(applicationForm.form)
    await editorPage.assertChildAddress('Kamreerintie 1, 00340 Espoo')
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const application = await getApplication({ applicationId })
    applicationForm.validateResult(application, [testChild2])
  })

  test('Full valid daycare application can be sent', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()

    const applicationForm = fullDaycareForm({
      otherGuardianAgreementStatus: 'AGREED'
    })
    await editorPage.fillData(applicationForm.form)
    await editorPage.assertChildAddress('Kamreerintie 1, 00340 Espoo')
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const application = await getApplication({ applicationId })
    applicationForm.validateResult(application, [testChild2])
  })

  test('Notification on duplicate application is visible', async () => {
    const application = applicationFixture(
      testChild,
      testAdult,
      undefined,
      'DAYCARE',
      null,
      [testDaycare.id],
      true
    )
    await createApplications({ body: [application] })
    await execSimpleApplicationActions(
      application.id,
      ['MOVE_TO_WAITING_PLACEMENT'],
      mockedNow
    )

    await header.selectTab('applications')
    await applicationsPage.assertDuplicateWarningIsShown(
      testChild.id,
      'DAYCARE'
    )
  })

  test('Notification on transfer application is visible', async () => {
    await createDaycarePlacements({
      body: [
        {
          id: randomId(),
          type: 'DAYCARE',
          childId: testChild.id,
          unitId: testDaycare.id,
          startDate: mockedDate.subYears(1),
          endDate: mockedDate.addYears(1),
          placeGuarantee: false,
          terminatedBy: null,
          terminationRequestedDate: null
        }
      ]
    })

    await header.selectTab('applications')
    await applicationsPage.assertTransferNotificationIsShown(
      testChild.id,
      'DAYCARE'
    )
  })

  test('A warning is shown if preferred start date is very soon', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
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
      testChild.id,
      'DAYCARE'
    )

    await editorPage.setPreferredStartDate(mockedDate.addYears(2).format())
    await editorPage.assertPreferredStartDateInfo('Valitse aikaisempi päivä')

    await editorPage.setPreferredStartDate(mockedDate.addMonths(4).format())
    await editorPage.assertPreferredStartDateInfo(undefined)
  })

  test('Citizen cannot move preferred start date before a previously selected date', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
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
      testChild.id,
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
    await editorPage.assertSelectedPreferredUnits([testDaycare.id])
  })

  test('Application can be made for restricted child', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChildRestricted.id,
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
      testChildRestricted.id,
      'DAYCARE'
    )
    await editorPage.fillData(minimalDaycareForm().form)
    await editorPage.markApplicationUrgentAndAddAttachment(testFilePath)
    await editorPage.goToVerification()
    await editorPage.assertUrgencyFileDownload()
  })

  test('Other guardian can see an application after it has been sent, and cannot see person details or attachments', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()

    const applicationForm = minimalDaycareForm({
      otherGuardianAgreementStatus: 'AGREED'
    })
    await editorPage.fillData(applicationForm.form)
    await editorPage.markApplicationUrgentAndAddAttachment(testFilePath)
    await editorPage.selectShiftCareAndAddAttachment(testFilePath)
    await editorPage.writeAssistanceNeedDescription('Child has assistance need')
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const otherGuardianPage = await Page.open({
      mockedTime: mockedNow
    })
    await enduserLogin(otherGuardianPage, testAdult2)

    const applications = new CitizenApplicationsPage(otherGuardianPage)
    await applications.assertApplicationExists(applicationId)
    const applicationReadView =
      await applications.viewApplication(applicationId)

    await applicationReadView.unitPreferenceSection.waitUntilVisible()
    await applicationReadView.contactInfoSection.waitUntilHidden()
    await applicationReadView.urgencyAttachments.waitUntilHidden()
    await applicationReadView.shiftCareAttachments.waitUntilHidden()
    await applicationReadView.assistanceNeedDescription.assertTextEquals('')
  })

  test('Application can be saved as draft', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
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
    await editorPage.openSection('contactInfo')
    await editorPage.guardianPhoneInput.assertValueEquals('040123456789')
  })

  test('If user has a verified email, that one is used in the application and cannot be changed', async () => {
    // given user has a draft application with an email
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()
    await editorPage.fillData({
      ...minimalDaycareForm().form,
      contactInfo: {
        guardianPhone: testAdult.phone,
        guardianEmail: 'old-email@test.com',
        noGuardianEmail: false,
        otherGuardianAgreementStatus: 'AGREED'
      }
    })
    await editorPage.saveAsDraftButton.click()
    await editorPage.modalOkBtn.click()

    // and given user verifies another email
    await header.selectTab('personal-details')
    const section = new CitizenPersonalDetailsPage(page).personalDetailsSection
    await section.editPersonalData(
      {
        preferredName: testAdult.firstName.split(' ')[1],
        email: 'new-email@example.com',
        phone: testAdult.phone,
        backupPhone: testAdult.backupPhone
      },
      true
    )
    await section.unverifiedEmailStatus.waitUntilVisible()
    await section.sendVerificationCode.click()
    await section.verificationCodeField.waitUntilVisible()
    await runJobs({ mockedTime: mockedNow })
    const verificationCode = await getVerificationCodeFromEmail()
    expect(verificationCode).toBeTruthy()
    await section.verificationCodeField.fill(verificationCode ?? '')
    await section.verifyEmail.click()
    await section.verifiedEmailStatus.waitUntilVisible()

    // when user goes back to the application
    await page.reload()
    await header.selectTab('applications')
    await applicationsPage.editApplication(applicationId)

    // then the email in the application updates and cannot be edited
    await editorPage.openSection('contactInfo')
    await editorPage.assertVerifiedReadOnlyEmail('new-email@example.com')
  })
})
