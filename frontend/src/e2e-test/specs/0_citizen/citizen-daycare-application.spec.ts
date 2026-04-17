// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { DaycareId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'

import { execSimpleApplicationActions } from '../../dev-api'
import {
  applicationFixture,
  clubTerm2021,
  testClub,
  testDaycare,
  testDaycare2,
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
  getApplication,
  resetServiceState,
  runJobs,
  setPersonEmail
} from '../../generated/api-clients'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenPersonalDetailsPage from '../../pages/citizen/citizen-personal-details'
import { UnitEditor } from '../../pages/employee/units/unit'
import { test, expect } from '../../playwright'
import {
  fullDaycareForm,
  minimalDaycareForm
} from '../../utils/application-forms'
import { getVerificationCodeFromEmail } from '../../utils/email'
import type { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

const testFileName = 'test_file.png'
const testFilePath = `src/e2e-test/assets/${testFileName}`
const mockedNow = HelsinkiDateTime.of(2021, 4, 1, 15, 0)
const mockedDate = mockedNow.toLocalDate()

test.describe('Citizen daycare applications', () => {
  let page: Page
  let header: CitizenHeader
  let applicationsPage: CitizenApplicationsPage

  test.use({ evakaOptions: { mockedTime: mockedNow } })

  test.beforeEach(async ({ evaka }) => {
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

    page = evaka
    await enduserLogin(page, testAdult, '/applications')
    header = new CitizenHeader(page)
    applicationsPage = new CitizenApplicationsPage(page)
  })

  test('Sending incomplete daycare application gives validation error', async () => {
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
    await expect(page.findByDataQa('guardianEmail-input-info')).toBeHidden()
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
    await expect(page.findByDataQa('guardianEmail-input-info')).toBeVisible()
  })

  test('Minimal valid daycare application can be sent', async () => {
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
    await Fixture.placement({
      type: 'DAYCARE',
      childId: testChild.id,
      unitId: testDaycare.id,
      startDate: mockedDate.subYears(1),
      endDate: mockedDate.addYears(1)
    }).save()

    await header.selectTab('applications')
    await applicationsPage.assertTransferNotificationIsShown(
      testChild.id,
      'DAYCARE'
    )
  })

  test('A warning is shown if preferred start date is very soon', async () => {
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

  test('An error message is shown if part time is chosen and daily hours exceed 5 hours', async () => {
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'DAYCARE'
    )

    await editorPage.fillData(
      minimalDaycareForm({ otherGuardianAgreementStatus: 'AGREED' }).form
    )
    await editorPage.setPartTime(true)
    await editorPage.assertPartTimeErrorIsShown(true)
  })

  test('Previously selected preferred units exists', async () => {
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
    const editorPage = await applicationsPage.createApplication(
      testChildRestricted.id,
      'DAYCARE'
    )
    await editorPage.fillData(minimalDaycareForm().form)
    await editorPage.markApplicationUrgentAndAddAttachment(testFilePath)
    await editorPage.goToVerification()
    await editorPage.assertUrgencyFileDownload()
  })

  test('Other guardian can see an application after it has been sent, and cannot see person details or attachments', async ({
    newEvakaPage
  }) => {
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

    const otherGuardianPage = await newEvakaPage({ mockedTime: mockedNow })
    await enduserLogin(otherGuardianPage, testAdult2, '/applications')

    const applications = new CitizenApplicationsPage(otherGuardianPage)
    await applications.assertApplicationExists(applicationId)
    const applicationReadView =
      await applications.viewApplication(applicationId)

    await expect(applicationReadView.unitPreferenceSection).toBeVisible()
    await expect(applicationReadView.contactInfoSection).toBeHidden()
    await expect(applicationReadView.urgencyAttachments).toBeHidden()
    await expect(applicationReadView.shiftCareAttachments).toBeHidden()
    await expect(applicationReadView.assistanceNeedDescription).toHaveText('')
  })

  test('Application can be saved as draft', async () => {
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
    await expect(editorPage.guardianPhoneInput).toHaveValue('040123456789')
  })

  test('If user has a verified email, that one is used in the application and cannot be changed', async () => {
    // given user has a draft application with an email
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
    await expect(section.unverifiedEmailStatus).toBeVisible()
    await section.sendVerificationCode.click()
    await expect(section.verificationCodeField).toBeVisible()
    await runJobs({ mockedTime: mockedNow })
    const verificationCode = await getVerificationCodeFromEmail()
    expect(verificationCode).toBeTruthy()
    await section.verificationCodeField.fill(verificationCode ?? '')
    await section.verifyEmail.click()
    await expect(section.verifiedEmailStatus).toBeVisible()

    // when user goes back to the application
    await page.reload()
    await header.selectTab('applications')
    await applicationsPage.editApplication(applicationId)

    // then the email in the application updates and cannot be edited
    await editorPage.openSection('contactInfo')
    await editorPage.assertVerifiedReadOnlyEmail('new-email@example.com')
  })

  test('Language filter limits selectable units to chosen languages', async () => {
    const swedishDaycare = {
      ...testDaycare2,
      id: randomId<DaycareId>(),
      areaId: testCareArea.id,
      name: 'Svensk daghem',
      language: 'sv' as const,
      daycareApplyPeriod: testDaycare.daycareApplyPeriod
    }
    const englishDaycare = {
      ...testDaycare2,
      id: randomId<DaycareId>(),
      areaId: testCareArea.id,
      name: 'English Daycare',
      language: 'en' as const,
      daycareApplyPeriod: testDaycare.daycareApplyPeriod
    }
    await Fixture.daycare(swedishDaycare).save()
    await Fixture.daycare(englishDaycare).save()

    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'DAYCARE'
    )
    await editorPage.fillData({
      serviceNeed: {
        preferredStartDate: '16.08.2021',
        startTime: '09:00',
        endTime: '17:00'
      },
      contactInfo: {
        guardianPhone: '040123456',
        noGuardianEmail: true
      }
    })
    await editorPage.openSection('unitPreference')

    // All three language chips should be visible
    await expect(editorPage.unitLanguageFilterChip('fi')).toBeVisible()
    await expect(editorPage.unitLanguageFilterChip('sv')).toBeVisible()
    await expect(editorPage.unitLanguageFilterChip('en')).toBeVisible()

    // Default: fi pre-selected → only Finnish unit is selectable
    await editorPage.assertPreferredUnitOptions([testDaycare.name])

    // Deselecting all chips empties the dropdown
    await editorPage.setUnitLanguageFilter('fi', false)
    await editorPage.assertNoPreferredUnitOptions()
    await editorPage.setUnitLanguageFilter('fi', true)

    // Add sv → fi + sv units selectable
    await editorPage.setUnitLanguageFilter('sv', true)
    await editorPage.assertPreferredUnitOptions([
      testDaycare.name,
      swedishDaycare.name
    ])

    // Remove fi → only sv units selectable
    await editorPage.setUnitLanguageFilter('fi', false)
    await editorPage.assertPreferredUnitOptions([swedishDaycare.name])

    // Switch to en only → only English units selectable
    await editorPage.setUnitLanguageFilter('sv', false)
    await editorPage.setUnitLanguageFilter('en', true)
    await editorPage.assertPreferredUnitOptions([englishDaycare.name])
    await editorPage.assertHiddenFromPreferredUnits([
      testDaycare.name,
      swedishDaycare.name
    ])

    // The filtered unit is still selectable
    await editorPage.selectUnit('English Daycare')
    await editorPage.assertSelectedPreferredUnits([englishDaycare.id])
  })

  test('Language filter is hidden when only one language exists', async () => {
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'DAYCARE'
    )
    await editorPage.fillData({
      serviceNeed: {
        preferredStartDate: '16.08.2021',
        startTime: '09:00',
        endTime: '17:00'
      },
      contactInfo: {
        guardianPhone: '040123456',
        noGuardianEmail: true
      }
    })
    await editorPage.openSection('unitPreference')

    // Only Finnish units exist, so the language filter should be hidden
    await editorPage.assertLanguageFilterVisible(false)

    // The Finnish unit must still be selectable when the filter is hidden
    await editorPage.selectUnit(testDaycare.name)
    await editorPage.assertSelectedPreferredUnits([testDaycare.id])
  })

  test('Default chip selection includes all available languages when no Finnish units exist', async () => {
    // testDaycare (Finnish) does not match a CLUB application, so this scenario
    // exercises the no-fi-units fallback path of the language filter default.
    const swedishClub = {
      ...testClub,
      id: randomId<DaycareId>(),
      areaId: testCareArea.id,
      name: 'Svensk klubb',
      language: 'sv' as const
    }
    const englishClub = {
      ...testClub,
      id: randomId<DaycareId>(),
      areaId: testCareArea.id,
      name: 'English Club',
      language: 'en' as const
    }
    await Fixture.daycare(swedishClub).save()
    await Fixture.daycare(englishClub).save()
    await clubTerm2021.save()

    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'CLUB'
    )
    await editorPage.fillData({
      serviceNeed: { preferredStartDate: '16.08.2021' },
      contactInfo: { guardianPhone: '040123456', noGuardianEmail: true }
    })
    await editorPage.openSection('unitPreference')

    // No Finnish units → fi chip hidden, sv + en chips visible
    await expect(editorPage.unitLanguageFilterChip('fi')).toBeHidden()
    await expect(editorPage.unitLanguageFilterChip('sv')).toBeVisible()
    await expect(editorPage.unitLanguageFilterChip('en')).toBeVisible()

    // Default selection is ['fi'], but no Finnish units exist in the CLUB
    // care type → the stale-selection fallback shows all units rather than
    // locking the user out of an empty dropdown.
    await editorPage.assertPreferredUnitOptions([
      englishClub.name,
      swedishClub.name
    ])

    // The fallback default lets the user actually apply
    await editorPage.selectUnit(englishClub.name)
    await editorPage.assertSelectedPreferredUnits([englishClub.id])
  })

  test('Language filter chip is shown only if there is at least one unit with that language', async ({
    newEvakaPage
  }) => {
    const swedishDaycare = {
      ...testDaycare2,
      id: randomId<DaycareId>(),
      areaId: testCareArea.id,
      name: 'Svensk daghem',
      language: 'sv' as const,
      daycareApplyPeriod: testDaycare.daycareApplyPeriod
    }
    await Fixture.daycare(swedishDaycare).save()

    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'DAYCARE'
    )
    const applicationId = editorPage.getNewApplicationId()
    await editorPage.fillData({
      serviceNeed: {
        preferredStartDate: '16.08.2021',
        startTime: '09:00',
        endTime: '17:00'
      },
      contactInfo: {
        guardianPhone: '040123456',
        noGuardianEmail: true
      }
    })
    await editorPage.openSection('unitPreference')

    // Initially: fi + sv chips visible, en chip hidden
    await expect(editorPage.unitLanguageFilterChip('fi')).toBeVisible()
    await expect(editorPage.unitLanguageFilterChip('sv')).toBeVisible()
    await expect(editorPage.unitLanguageFilterChip('en')).toBeHidden()

    // Persist the draft so the form state survives the reload below
    await editorPage.saveAsDraftButton.click()
    await editorPage.modalOkBtn.click()

    // Admin updates the Finnish unit's language to English
    const admin = await Fixture.employee().admin().save()
    const adminPage = await newEvakaPage({ mockedTime: mockedNow })
    await employeeLogin(adminPage, admin)
    const unitEditor = await UnitEditor.openById(adminPage, testDaycare.id)
    await unitEditor.selectLanguage('en')
    await unitEditor.fillManagerData(
      'Päiväkodin Johtaja',
      '01234567',
      'manager@example.com'
    )
    await unitEditor.setInvoiceByMunicipality(false)
    await unitEditor.submit()

    // Citizen re-enters the draft and revisits the unit preference section
    await applicationsPage.editApplication(applicationId)
    await editorPage.openSection('unitPreference')

    // Now: sv + en chips visible, fi chip hidden
    await expect(editorPage.unitLanguageFilterChip('fi')).toBeHidden()
    await expect(editorPage.unitLanguageFilterChip('sv')).toBeVisible()
    await expect(editorPage.unitLanguageFilterChip('en')).toBeVisible()

    // The persisted ['fi'] chip selection no longer overlaps with available
    // languages → the stale-selection fallback fires and the dropdown still
    // shows the (now-English) unit and the Swedish unit, instead of being empty.
    await editorPage.assertPreferredUnitOptions([
      testDaycare.name,
      swedishDaycare.name
    ])
  })
})
