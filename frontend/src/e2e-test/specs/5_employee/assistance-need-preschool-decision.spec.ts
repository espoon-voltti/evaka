// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  resetDatabase
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import {
  DevAssistanceNeedPreschoolDecision,
  EmployeeDetail
} from '../../dev-api/types'
import AssistanceNeedPreschoolDecisionEditPage from '../../pages/employee/assistance-need-decision/assistance-need-preschool-decision-edit-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let fixtures: AreaAndPersonFixtures
let serviceWorker: EmployeeDetail
let assistanceNeedDecisionEditPage: AssistanceNeedPreschoolDecisionEditPage
let childId: UUID
let assistanceNeedDecision: DevAssistanceNeedPreschoolDecision

beforeEach(async () => {
  await resetDatabase()

  serviceWorker = (await Fixture.employeeServiceWorker().save()).data

  fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  // staff = (await Fixture.employeeStaff(unitId).save()).data
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )

  assistanceNeedDecision = (
    await Fixture.assistanceNeedPreschoolDecision()
      .withChild(childId)
      .withGuardian(fixtures.familyWithTwoGuardians.guardian.id)
      .withGuardian(fixtures.familyWithTwoGuardians.otherGuardian.id)
      .save()
  ).data

  await insertDaycarePlacementFixtures([daycarePlacementFixture])
})

describe('Assistance Need Decisions - Edit page', () => {
  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, serviceWorker)
    await page.goto(
      `${
        config.employeeUrl
      }/child-information/${childId}/assistance-need-preschool-decisions/${
        assistanceNeedDecision?.id ?? ''
      }/edit`
    )
    assistanceNeedDecisionEditPage =
      new AssistanceNeedPreschoolDecisionEditPage(page)
  })

  test('Some fields are visible', async () => {
    await assistanceNeedDecisionEditPage.primaryGroupInput.waitUntilVisible()
  })

  test('Info header shows correct information', async () => {
    await assistanceNeedDecisionEditPage.status.assertTextEquals('Luonnos')
    await assistanceNeedDecisionEditPage.decisionNumber.assertText((s) =>
      s.includes(assistanceNeedDecision.decisionNumber.toString(10))
    )
  })

  test('Autosave works', async () => {
    await assistanceNeedDecisionEditPage.primaryGroupInput.fill(
      'this is a motivation'
    )
    await waitUntilEqual(
      () =>
        assistanceNeedDecisionEditPage.autoSaveIndicator.getAttribute(
          'data-status'
        ),
      'saving'
    )
    await waitUntilEqual(
      () =>
        assistanceNeedDecisionEditPage.autoSaveIndicator.getAttribute(
          'data-status'
        ),
      'saved'
    )
  })
})
