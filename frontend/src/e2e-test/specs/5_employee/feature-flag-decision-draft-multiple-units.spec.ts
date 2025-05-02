// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { fromUuid } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { execSimpleApplicationActions } from '../../dev-api'
import {
  applicationFixture,
  testDaycare,
  Fixture,
  testPreschool,
  familyWithTwoGuardians,
  testAdult,
  testChild2,
  testCareArea,
  preschoolTerm2021
} from '../../dev-api/fixtures'
import {
  cleanUpMessages,
  createApplications,
  createDefaultServiceNeedOptions,
  getApplicationDecisions,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedTime = LocalDate.of(2021, 8, 16)
let page: Page
let applicationListView: ApplicationListView

let serviceWorker: DevEmployee

beforeEach(async () => {
  await resetServiceState()
  await cleanUpMessages()
  await preschoolTerm2021.save()
  await testCareArea.save()
  await testDaycare.save()
  await testPreschool.save()
  await Fixture.family({ guardian: testAdult, children: [testChild2] }).save()
  await familyWithTwoGuardians.save()
  serviceWorker = await Fixture.employee().serviceWorker().save()
  await createDefaultServiceNeedOptions()
  await Fixture.feeThresholds().save()

  page = await Page.open({
    mockedTime: mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0)),
    employeeCustomizations: {
      featureFlags: { decisionDraftMultipleUnits: true }
    }
  })
  applicationListView = new ApplicationListView(page)
})

describe('Application transitions', () => {
  test('Decision draft page works without unit selection', async () => {
    const fixture = {
      ...applicationFixture(
        testChild2,
        familyWithTwoGuardians.guardian,
        undefined,
        'PRESCHOOL',
        null,
        [testPreschool.id],
        true,
        'SENT',
        mockedTime
      ),
      id: fromUuid<ApplicationId>('6a9b1b1e-3fdf-11eb-b378-0242ac130002')
    }
    const applicationId = fixture.id
    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['MOVE_TO_WAITING_PLACEMENT'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 40))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)

    await applicationListView.filterByApplicationStatus('WAITING_PLACEMENT')
    await applicationListView.searchButton.click()
    const placementDraftPage = await applicationListView
      .applicationRow(applicationId)
      .primaryActionCreatePlacementPlan()
    await placementDraftPage.waitUntilLoaded()
    await placementDraftPage.placeToUnit(testPreschool.id)
    await placementDraftPage.submit()

    await applicationListView.filterByApplicationStatus('WAITING_DECISION')
    await applicationListView.searchButton.click()
    const decisionEditorPage = await applicationListView
      .applicationRow(applicationId)
      .primaryActionEditDecisions()
    await decisionEditorPage.waitUntilLoaded()
    await decisionEditorPage.save()

    await applicationListView.searchButton.click()

    await execSimpleApplicationActions(
      applicationId,
      ['SEND_DECISIONS_WITHOUT_PROPOSAL'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 41))
    )

    const decisions = await getApplicationDecisions({ applicationId })
    expect(
      decisions
        .map(({ type, unit: { id: unitId } }) => ({ type, unitId }))
        .sort((a, b) => a.type.localeCompare(b.type))
    ).toEqual([
      { type: 'PRESCHOOL', unitId: testPreschool.id },
      { type: 'PRESCHOOL_DAYCARE', unitId: testPreschool.id }
    ])
  })

  test('Decision draft page works with unit selection', async () => {
    const fixture = {
      ...applicationFixture(
        testChild2,
        familyWithTwoGuardians.guardian,
        undefined,
        'PRESCHOOL',
        null,
        [testPreschool.id],
        true,
        'SENT',
        mockedTime
      ),
      id: fromUuid<ApplicationId>('6a9b1b1e-3fdf-11eb-b378-0242ac130002')
    }
    const applicationId = fixture.id
    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['MOVE_TO_WAITING_PLACEMENT'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 40))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)

    await applicationListView.filterByApplicationStatus('WAITING_PLACEMENT')
    await applicationListView.searchButton.click()
    const placementDraftPage = await applicationListView
      .applicationRow(applicationId)
      .primaryActionCreatePlacementPlan()
    await placementDraftPage.waitUntilLoaded()
    await placementDraftPage.placeToUnit(testPreschool.id)
    await placementDraftPage.submit()

    await applicationListView.filterByApplicationStatus('WAITING_DECISION')
    await applicationListView.searchButton.click()
    const decisionEditorPage = await applicationListView
      .applicationRow(applicationId)
      .primaryActionEditDecisions()
    await decisionEditorPage.selectUnit('PRESCHOOL_DAYCARE', testDaycare.id)
    await decisionEditorPage.save()

    await applicationListView.searchButton.click()

    await execSimpleApplicationActions(
      applicationId,
      ['SEND_DECISIONS_WITHOUT_PROPOSAL'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 41))
    )

    const decisions = await getApplicationDecisions({ applicationId })
    expect(
      decisions
        .map(({ type, unit: { id: unitId } }) => ({ type, unitId }))
        .sort((a, b) => a.type.localeCompare(b.type))
    ).toStrictEqual([
      { type: 'PRESCHOOL', unitId: testPreschool.id },
      { type: 'PRESCHOOL_DAYCARE', unitId: testDaycare.id }
    ])
  })
})
