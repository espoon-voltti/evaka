// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { execSimpleApplicationActions } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
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
import { ApplicationWorkbenchPage } from '../../pages/admin/application-workbench-page'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedTime = LocalDate.of(2021, 8, 16)
let page: Page
let applicationWorkbench: ApplicationWorkbenchPage

let serviceWorker: DevEmployee

beforeEach(async () => {
  await resetServiceState()
  await cleanUpMessages()
  await initializeAreaAndPersonData()
  await Fixture.preschoolTerm().with(preschoolTerm2021).save()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare().with(testDaycare).save()
  await Fixture.daycare().with(testPreschool).save()
  await Fixture.family({ guardian: testAdult, children: [testChild2] }).save()
  await Fixture.family(familyWithTwoGuardians).save()
  serviceWorker = await Fixture.employeeServiceWorker().save()
  await createDefaultServiceNeedOptions()
  await Fixture.feeThresholds().save()

  page = await Page.open({
    mockedTime: mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0)),
    employeeCustomizations: {
      featureFlags: { decisionDraftMultipleUnits: true }
    }
  })
  applicationWorkbench = new ApplicationWorkbenchPage(page)
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
      id: '6a9b1b1e-3fdf-11eb-b378-0242ac130002'
    }
    const applicationId = fixture.id
    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['move-to-waiting-placement'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 40))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openPlacementQueue()
    const placementDraftPage =
      await applicationWorkbench.openDaycarePlacementDialogById(applicationId)
    await placementDraftPage.waitUntilLoaded()

    await placementDraftPage.placeToUnit(testPreschool.id)
    await placementDraftPage.submit()
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openDecisionQueue()
    const decisionEditorPage =
      await applicationWorkbench.openDecisionEditorById(applicationId)
    await decisionEditorPage.waitUntilLoaded()

    await decisionEditorPage.save()
    await applicationWorkbench.waitUntilLoaded()

    await execSimpleApplicationActions(
      applicationId,
      ['send-decisions-without-proposal'],
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
      id: '6a9b1b1e-3fdf-11eb-b378-0242ac130002'
    }
    const applicationId = fixture.id
    await createApplications({ body: [fixture] })

    await execSimpleApplicationActions(
      applicationId,
      ['move-to-waiting-placement'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 40))
    )

    await employeeLogin(page, serviceWorker)
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openPlacementQueue()
    const placementDraftPage =
      await applicationWorkbench.openDaycarePlacementDialogById(applicationId)
    await placementDraftPage.waitUntilLoaded()

    await placementDraftPage.placeToUnit(testPreschool.id)
    await placementDraftPage.submit()
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openDecisionQueue()
    const decisionEditorPage =
      await applicationWorkbench.openDecisionEditorById(applicationId)
    await decisionEditorPage.waitUntilLoaded()

    await decisionEditorPage.selectUnit('PRESCHOOL_DAYCARE', testDaycare.id)
    await decisionEditorPage.save()
    await applicationWorkbench.waitUntilLoaded()

    await execSimpleApplicationActions(
      applicationId,
      ['send-decisions-without-proposal'],
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
