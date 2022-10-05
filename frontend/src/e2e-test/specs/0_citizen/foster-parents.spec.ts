// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'

import {
  execSimpleApplicationActions,
  getDecisionsByApplication,
  insertFosterParents,
  resetDatabase,
  runPendingAsyncJobs
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { Fixture } from '../../dev-api/fixtures'
import { PersonDetail } from '../../dev-api/types'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { minimalDaycareForm } from '../../utils/application-forms'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let fixtures: AreaAndPersonFixtures
let fosterParent: PersonDetail
let fosterChild: PersonDetail
const mockedDate = LocalDate.of(2021, 4, 1)

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  fosterParent = fixtures.enduserGuardianFixture
  fosterChild = (await Fixture.person().with({ ssn: '290413A902C' }).save())
    .data
  await insertFosterParents([
    {
      childId: fosterChild.id,
      parentId: fosterParent.id,
      validDuring: new DateRange(mockedDate, null)
    }
  ])

  page = await Page.open({ mockedTime: mockedDate.toSystemTzDate() })
  await enduserLogin(page)
  header = new CitizenHeader(page)
})

test('Foster parent can create a daycare application and accept a daycare decision', async () => {
  const applicationsPage = new CitizenApplicationsPage(page)

  await header.selectTab('applications')
  const editorPage = await applicationsPage.createApplication(
    fosterChild.id,
    'DAYCARE'
  )
  const applicationId = editorPage.getNewApplicationId()

  await editorPage.fillData(minimalDaycareForm.form)
  await editorPage.assertChildAddress(
    `${fosterChild.streetAddress ?? ''}, ${fosterChild.postalCode ?? ''} ${
      fosterChild.postOffice ?? ''
    }`
  )
  await editorPage.verifyAndSend()

  await applicationsPage.assertApplicationIsListed(
    applicationId,
    'Varhaiskasvatushakemus',
    minimalDaycareForm.form.unitPreference?.preferredUnits?.[0].name ?? '',
    minimalDaycareForm.form.serviceNeed?.preferredStartDate ?? '',
    'Lähetetty'
  )

  await execSimpleApplicationActions(applicationId, [
    'move-to-waiting-placement',
    'create-default-placement-plan',
    'send-decisions-without-proposal',
    'confirm-decision-mailed'
  ])
  await runPendingAsyncJobs()

  const citizenDecisionsPage = new CitizenDecisionsPage(page)
  const decisions = await getDecisionsByApplication(applicationId)
  const decisionId = decisions[0].id
  await header.selectTab('decisions')
  const responsePage = await citizenDecisionsPage.navigateToDecisionResponse(
    applicationId
  )
  await responsePage.assertUnresolvedDecisionsCount(1)
  await responsePage.acceptDecision(decisionId)
  await responsePage.assertDecisionStatus(decisionId, 'Hyväksytty')
  await responsePage.assertUnresolvedDecisionsCount(0)
})
