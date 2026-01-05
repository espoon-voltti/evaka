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
import { test } from '../../playwright'
import { fullClubForm, minimalClubForm } from '../../utils/application-forms'
import { enduserLogin } from '../../utils/user'

const mockedDate = LocalDate.of(2021, 3, 1)

test.describe('Citizen club applications', () => {
  test.use({
    evakaOptions: {
      mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
    }
  })

  test.beforeEach(async () => {
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
  })

  test('Sending incomplete club application gives validation error', async ({
    evaka
  }) => {
    await enduserLogin(evaka, testAdult)
    const header = new CitizenHeader(evaka)
    const applicationsPage = new CitizenApplicationsPage(evaka)

    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      testChild.id,
      'CLUB'
    )
    await editorPage.goToVerification()
    await editorPage.assertErrorsExist()
  })

  test('Minimal valid club application can be sent', async ({ evaka }) => {
    await enduserLogin(evaka, testAdult)
    const header = new CitizenHeader(evaka)
    const applicationsPage = new CitizenApplicationsPage(evaka)

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

  test('Full valid club application can be sent', async ({ evaka }) => {
    await enduserLogin(evaka, testAdult)
    const header = new CitizenHeader(evaka)
    const applicationsPage = new CitizenApplicationsPage(evaka)

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

  test('Citizen cannot move preferred start date before a previously selected date', async ({
    evaka
  }) => {
    await enduserLogin(evaka, testAdult)
    const header = new CitizenHeader(evaka)
    const applicationsPage = new CitizenApplicationsPage(evaka)

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
