// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { fromUuid } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { execSimpleApplicationActions } from '../../dev-api'
import { applicationFixture, Fixture } from '../../dev-api/fixtures'
import {
  createApplications,
  resetServiceState
} from '../../generated/api-clients'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenHeader from '../../pages/citizen/citizen-header'
import type { DecisionEditorPage } from '../../pages/employee/applications/application-list-view'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import { test } from '../../playwright'
import type { NewEvakaPage } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

const mockedDate = LocalDate.of(2021, 8, 16)
const mockedTime = mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))

const preschoolTerm = Fixture.preschoolTerm({
  finnishPreschool: new FiniteDateRange(
    LocalDate.of(2021, 8, 11),
    LocalDate.of(2022, 6, 3)
  ),
  swedishPreschool: new FiniteDateRange(
    LocalDate.of(2021, 8, 11),
    LocalDate.of(2022, 6, 3)
  ),
  extendedTerm: new FiniteDateRange(
    LocalDate.of(2021, 8, 1),
    LocalDate.of(2022, 6, 3)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2021, 1, 8),
    LocalDate.of(2022, 6, 3)
  )
})

const careArea = Fixture.careArea()

const daycareA = Fixture.daycare({
  areaId: careArea.id,
  type: ['CENTRE', 'PRESCHOOL']
})

const daycareB = Fixture.daycare({
  areaId: careArea.id,
  type: ['CENTRE', 'PRESCHOOL']
})

const child = Fixture.person({
  ssn: '160616A977T',
  dateOfBirth: LocalDate.of(2016, 6, 16)
})

const adult = Fixture.person({
  ssn: '010180-999A',
  dateOfBirth: LocalDate.of(1980, 1, 1)
})

const serviceWorker = Fixture.employee().serviceWorker()

let page: Page

test.describe('Additional daycare application decision drafts', () => {
  test.use({ evakaOptions: { mockedTime } })

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await preschoolTerm.save()
    await careArea.save()
    await daycareA.save()
    await daycareB.save()
    await Fixture.family({
      guardian: adult,
      children: [child]
    }).save()
    await serviceWorker.save()

    page = evaka
  })

  test('Same unit — only daycare decision is planned by default', async ({
    newEvakaPage
  }) => {
    await setupExistingPreschoolPlacement()
    const applicationId = await citizenCreatesPreschoolDaycareApplication(
      daycareA,
      newEvakaPage
    )
    const decisionEditorPage = await navigateToDecisionDrafts(applicationId)

    await decisionEditorPage
      .plannedCheckbox('PRESCHOOL')
      .waitUntilChecked(false)
    await decisionEditorPage
      .plannedCheckbox('PRESCHOOL_DAYCARE')
      .waitUntilChecked(true)
  })

  test('Different unit — both decisions should be planned by default', async ({
    newEvakaPage
  }) => {
    await setupExistingPreschoolPlacement()
    const applicationId = await citizenCreatesPreschoolDaycareApplication(
      daycareB,
      newEvakaPage
    )
    const decisionEditorPage = await navigateToDecisionDrafts(applicationId)

    await decisionEditorPage.plannedCheckbox('PRESCHOOL').waitUntilChecked(true)
    await decisionEditorPage
      .plannedCheckbox('PRESCHOOL_DAYCARE')
      .waitUntilChecked(true)
  })
})

async function setupExistingPreschoolPlacement() {
  const fixture = {
    ...applicationFixture(
      child,
      adult,
      undefined,
      'PRESCHOOL',
      null,
      [daycareA.id],
      false, // no connected daycare
      'SENT',
      mockedDate
    ),
    id: fromUuid<ApplicationId>('6a9b1b1e-3fdf-11eb-b378-0242ac130002')
  }
  await createApplications({ body: [fixture] })
  await execSimpleApplicationActions(
    fixture.id,
    [
      'MOVE_TO_WAITING_PLACEMENT',
      'CREATE_DEFAULT_PLACEMENT_PLAN',
      'SEND_DECISIONS_WITHOUT_PROPOSAL'
    ],
    HelsinkiDateTime.fromLocal(mockedDate, LocalTime.of(13, 0))
  )

  await employeeLogin(page, serviceWorker)
  const applicationReadView = new ApplicationReadView(page)
  await applicationReadView.navigateToApplication(fixture.id)
  await applicationReadView.acceptDecision('PRESCHOOL')
  await applicationReadView.waitUntilLoaded()
}

async function citizenCreatesPreschoolDaycareApplication(
  unit: { id: DaycareId; name: string },
  newEvakaPage: NewEvakaPage
): Promise<ApplicationId> {
  const citizenPage = await newEvakaPage({ mockedTime })
  await enduserLogin(citizenPage, adult)
  const header = new CitizenHeader(citizenPage)
  await header.selectTab('applications')
  const applicationsPage = new CitizenApplicationsPage(citizenPage)
  const editorPage = await applicationsPage.createApplication(
    child.id,
    'PRESCHOOL'
  )
  const applicationId = editorPage.getNewApplicationId()

  await editorPage.fillData({
    serviceNeed: {
      preferredStartDate: mockedDate.format(),
      connectedDaycare: true,
      connectedDaycarePreferredStartDate: mockedDate.format(),
      startTime: '08:00',
      endTime: '16:00'
    },
    unitPreference: {
      preferredUnits: [{ id: unit.id, name: unit.name }]
    },
    contactInfo: {
      guardianPhone: '(+358) 50-1234567',
      noGuardianEmail: true
    }
  })
  await editorPage.verifyAndSend({ hasOtherGuardian: false })

  return applicationId
}

async function navigateToDecisionDrafts(
  applicationId: ApplicationId
): Promise<DecisionEditorPage> {
  await execSimpleApplicationActions(
    applicationId,
    ['MOVE_TO_WAITING_PLACEMENT', 'CREATE_DEFAULT_PLACEMENT_PLAN'],
    HelsinkiDateTime.fromLocal(mockedDate, LocalTime.of(14, 0))
  )

  await employeeLogin(page, serviceWorker)
  const applicationListView = new ApplicationListView(page)
  await page.goto(ApplicationListView.url)
  await applicationListView.filterByApplicationStatus('WAITING_DECISION')
  await applicationListView.searchButton.click()
  const decisionEditorPage = await applicationListView
    .applicationRow(applicationId)
    .primaryActionEditDecisions()
  await decisionEditorPage.waitUntilLoaded()
  return decisionEditorPage
}
