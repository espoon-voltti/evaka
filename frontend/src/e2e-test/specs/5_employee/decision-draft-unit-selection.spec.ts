// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ApplicationId } from 'lib-common/generated/api-types/shared'
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
  testChild,
  testChild2,
  testChildRestricted,
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
import type { DevEmployee } from '../../generated/api-types'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedDate = LocalDate.of(2021, 8, 16)

test.use({
  evakaOptions: {
    mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
  }
})

test.describe('Decision draft unit selection', () => {
  let page: Page
  let applicationListView: ApplicationListView

  let serviceWorker: DevEmployee

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await cleanUpMessages()
    await preschoolTerm2021.save()
    await testCareArea.save()
    await testDaycare.save()
    await testPreschool.save()
    await Fixture.family({
      guardian: testAdult,
      children: [testChild, testChild2, testChildRestricted]
    }).save()
    await familyWithTwoGuardians.save()
    serviceWorker = await Fixture.employee().serviceWorker().save()
    await createDefaultServiceNeedOptions()
    await Fixture.feeThresholds().save()

    page = evaka
    applicationListView = new ApplicationListView(page)
  })

  test('Decision draft page works with shared unit selection', async () => {
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
        mockedDate
      ),
      id: fromUuid<ApplicationId>('6a9b1b1e-3fdf-11eb-b378-0242ac130002')
    }
    const applicationId = fixture.id
    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['MOVE_TO_WAITING_PLACEMENT'],
      HelsinkiDateTime.fromLocal(mockedDate, LocalTime.of(13, 40))
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

    const decisionDraftPage = await applicationListView
      .applicationRow(applicationId)
      .primaryActionEditDecisionsRedesign()
    await decisionDraftPage.waitUntilLoaded()

    await decisionDraftPage.selectSharedUnit(testDaycare.id)
    await decisionDraftPage.save()
    await applicationListView.searchButton.click()

    await execSimpleApplicationActions(
      applicationId,
      ['SEND_DECISIONS_WITHOUT_PROPOSAL'],
      HelsinkiDateTime.fromLocal(mockedDate, LocalTime.of(13, 41))
    )

    const decisions = await getApplicationDecisions({ applicationId })
    expect(
      decisions
        .map(({ type, unit: { id: unitId } }) => ({ type, unitId }))
        .sort((a, b) => a.type.localeCompare(b.type))
    ).toStrictEqual([
      { type: 'PRESCHOOL', unitId: testDaycare.id },
      { type: 'PRESCHOOL_DAYCARE', unitId: testDaycare.id }
    ])
  })
})
