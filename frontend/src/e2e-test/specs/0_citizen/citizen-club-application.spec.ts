// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { Fixture, testAdult2 } from '../../dev-api/fixtures'
import { getApplication, resetServiceState } from '../../generated/api-clients'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { fullClubForm, minimalClubForm } from '../../utils/application-forms'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let applicationsPage: CitizenApplicationsPage
let fixtures: AreaAndPersonFixtures

const mockedDate = LocalDate.of(2021, 3, 1)

beforeEach(async () => {
  await resetServiceState()
  fixtures = await initializeAreaAndPersonData()
  await Fixture.person()
    .with(testAdult2)
    .saveAdult({ updateMockVtjWithDependants: [fixtures.testChild] })

  page = await Page.open({
    mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  await enduserLogin(page)
  header = new CitizenHeader(page)
  applicationsPage = new CitizenApplicationsPage(page)
})

describe('Citizen club applications', () => {
  test('Sending incomplete club application gives validation error', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.testChild.id,
      'CLUB'
    )
    await editorPage.goToVerification()
    await editorPage.assertErrorsExist()
  })

  test('Minimal valid club application can be sent', async () => {
    await header.selectTab('applications')
    const editorPage = await applicationsPage.createApplication(
      fixtures.testChild.id,
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
      fixtures.testChild.id,
      'CLUB'
    )
    const applicationId = editorPage.getNewApplicationId()

    await editorPage.fillData(fullClubForm.form)
    await editorPage.verifyAndSend({ hasOtherGuardian: true })

    const application = await getApplication({ applicationId })
    fullClubForm.validateResult(application)
  })
})
