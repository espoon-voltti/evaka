// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import {
  Fixture,
  preschoolTerm2021,
  testAdult,
  testAdult2,
  testCareArea,
  testChild,
  testChild2,
  testDaycare
} from '../../dev-api/fixtures'
import {
  getApplication,
  resetServiceState,
  setPersonEmail
} from '../../generated/api-clients'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenPersonalDetailsPage from '../../pages/citizen/citizen-personal-details'
import {
  fullPreschoolForm,
  minimalPreschoolForm
} from '../../utils/application-forms'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let applicationsPage: CitizenApplicationsPage

const mockedDate = LocalDate.of(2021, 1, 15)

beforeEach(async () => {
  await resetServiceState()
  await Fixture.preschoolTerm(preschoolTerm2021).save()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  await Fixture.person()
    .with(testAdult2)
    .saveAdult({ updateMockVtjWithDependants: [testChild] })

  page = await Page.open({
    mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  await enduserLogin(page, testAdult)
  header = new CitizenHeader(page)
  applicationsPage = new CitizenApplicationsPage(page)
})

describe('Citizen preschool applications', () => {
  test('Sending incomplete preschool application gives validation error', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'PRESCHOOL'
    )
    await editorPage.goToVerification()
    await editorPage.assertErrorsExist()
  })

  test('Minimal valid preschool application can be sent', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'PRESCHOOL'
    )
    const applicationId = editorPage.getNewApplicationId()

    await editorPage.fillData(minimalPreschoolForm.form)
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const application = await getApplication({ applicationId })
    minimalPreschoolForm.validateResult(application)
  })

  test('Full valid preschool application can be sent', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'PRESCHOOL'
    )
    const applicationId = editorPage.getNewApplicationId()

    await editorPage.fillData(fullPreschoolForm.form)
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const application = await getApplication({ applicationId })
    fullPreschoolForm.validateResult(application, [testChild2])
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
      'PRESCHOOL'
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
      'PRESCHOOL'
    )
    await editorPage.goToVerification()
    await editorPage.assertErrorsExist()
    await editorPage.openSection('contactInfo')
    await page.findByDataQa('guardianEmail-input-info').waitUntilVisible()
  })
})
