// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import {
  cleanUpMessages,
  execSimpleApplicationActions,
  getDecisionsByApplication,
  insertApplications,
  insertDefaultServiceNeedOptions,
  resetDatabase
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  applicationFixture,
  daycareFixture,
  Fixture,
  preschoolFixture
} from '../../dev-api/fixtures'
import { EmployeeDetail } from '../../dev-api/types'
import { ApplicationWorkbenchPage } from '../../pages/admin/application-workbench-page'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedTime = LocalDate.of(2021, 8, 16)
let page: Page
let applicationWorkbench: ApplicationWorkbenchPage

let fixtures: AreaAndPersonFixtures
let serviceWorker: EmployeeDetail

beforeEach(async () => {
  await resetDatabase()
  await cleanUpMessages()
  fixtures = await initializeAreaAndPersonData()
  serviceWorker = (await Fixture.employeeServiceWorker().save()).data
  await insertDefaultServiceNeedOptions()
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
        fixtures.enduserChildFixtureKaarina,
        fixtures.familyWithTwoGuardians.guardian,
        undefined,
        'PRESCHOOL',
        null,
        [preschoolFixture.id],
        true,
        'SENT',
        mockedTime
      ),
      id: '6a9b1b1e-3fdf-11eb-b378-0242ac130002'
    }
    const applicationId = fixture.id
    await insertApplications([fixture])

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

    await placementDraftPage.placeToUnit(fixtures.preschoolFixture.id)
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

    const decisions = await getDecisionsByApplication(applicationId)
    expect(
      decisions
        .map(({ type, unit: { id: unitId } }) => ({ type, unitId }))
        .sort((a, b) => a.type.localeCompare(b.type))
    ).toEqual([
      { type: 'PRESCHOOL', unitId: preschoolFixture.id },
      { type: 'PRESCHOOL_DAYCARE', unitId: preschoolFixture.id }
    ])
  })

  test('Decision draft page works with unit selection', async () => {
    const fixture = {
      ...applicationFixture(
        fixtures.enduserChildFixtureKaarina,
        fixtures.familyWithTwoGuardians.guardian,
        undefined,
        'PRESCHOOL',
        null,
        [preschoolFixture.id],
        true,
        'SENT',
        mockedTime
      ),
      id: '6a9b1b1e-3fdf-11eb-b378-0242ac130002'
    }
    const applicationId = fixture.id
    await insertApplications([fixture])

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

    await placementDraftPage.placeToUnit(fixtures.preschoolFixture.id)
    await placementDraftPage.submit()
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openDecisionQueue()
    const decisionEditorPage =
      await applicationWorkbench.openDecisionEditorById(applicationId)
    await decisionEditorPage.waitUntilLoaded()

    await decisionEditorPage.selectUnit('PRESCHOOL_DAYCARE', daycareFixture.id)
    await decisionEditorPage.save()
    await applicationWorkbench.waitUntilLoaded()

    await execSimpleApplicationActions(
      applicationId,
      ['send-decisions-without-proposal'],
      HelsinkiDateTime.fromLocal(mockedTime, LocalTime.of(13, 41))
    )

    const decisions = await getDecisionsByApplication(applicationId)
    expect(
      decisions
        .map(({ type, unit: { id: unitId } }) => ({ type, unitId }))
        .sort((a, b) => a.type.localeCompare(b.type))
    ).toStrictEqual([
      { type: 'PRESCHOOL', unitId: preschoolFixture.id },
      { type: 'PRESCHOOL_DAYCARE', unitId: daycareFixture.id }
    ])
  })
})
