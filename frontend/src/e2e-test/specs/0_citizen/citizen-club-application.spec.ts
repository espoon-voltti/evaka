// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import {
  clubTerm2020,
  clubTerm2021,
  Fixture,
  testAdult,
  testAdult2,
  testCareArea,
  testChild,
  testChild2,
  testClub
} from '../../dev-api/fixtures'
import { getApplication, resetServiceState } from '../../generated/api-clients'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { fullClubForm, minimalClubForm } from '../../utils/application-forms'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let applicationsPage: CitizenApplicationsPage

const mockedDate = LocalDate.of(2021, 3, 1)

beforeEach(async () => {
  await resetServiceState()
  await clubTerm2020.save()
  await clubTerm2021.save()
  await testCareArea.save()
  await testClub.save()
  await Fixture.family({
    guardian: testAdult,
    otherGuardian: testAdult2,
    children: [testChild, testChild2]
  }).save()

  page = await Page.open({
    mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  await enduserLogin(page, testAdult)
  header = new CitizenHeader(page)
  applicationsPage = new CitizenApplicationsPage(page)
})

describe('Citizen club applications', () => {
  test('Sending incomplete club application gives validation error', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'CLUB'
    )
    await editorPage.goToVerification()
    await editorPage.assertErrorsExist()
  })

  test('Minimal valid club application can be sent', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'CLUB'
    )
    const applicationId = editorPage.getNewApplicationId()

    await editorPage.fillData(minimalClubForm.form)
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const application = await getApplication({ applicationId })
    minimalClubForm.validateResult(application)
  })

  test('Full valid club application can be sent', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'CLUB'
    )
    const applicationId = editorPage.getNewApplicationId()

    await editorPage.fillData(fullClubForm.form)
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const application = await getApplication({ applicationId })
    fullClubForm.validateResult(application)
  })

  test('Citizen cannot move preferred start date before a previously selected date', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'CLUB'
    )
    const applicationId = editorPage.getNewApplicationId()
    await editorPage.fillData({
      ...minimalClubForm.form,
      serviceNeed: {
        ...minimalClubForm.form.serviceNeed,
        preferredStartDate: '24.08.2021'
      }
    })
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    await applicationsPage.editApplication(applicationId)
    await editorPage.setPreferredStartDate('23.08.2021')
    await editorPage.assertPreferredStartDateInfo('Valitse myöhäisempi päivä')
    await editorPage.setPreferredStartDate('01.02.2021')
    await editorPage.assertPreferredStartDateInfo('Valitse myöhäisempi päivä')
    await editorPage.setPreferredStartDate('24.08.2021')
    await editorPage.assertPreferredStartDateInfo(undefined)
    await editorPage.setPreferredStartDate('25.08.2021')
    await editorPage.assertPreferredStartDateInfo(undefined)
  })
})
