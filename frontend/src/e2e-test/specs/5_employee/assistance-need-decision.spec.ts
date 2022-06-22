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
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { AssistanceNeedDecision, EmployeeDetail } from '../../dev-api/types'
import AssistanceNeedDecisionEditPage from '../../pages/employee/assistance-need-decision/assistance-need-decision-edit-page'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let serviceWorker: EmployeeDetail
let assistanceNeedDecisionEditPage: AssistanceNeedDecisionEditPage
let childId: UUID
let assistanceNeedDecision: AssistanceNeedDecision

beforeAll(async () => {
  await resetDatabase()

  serviceWorker = (await Fixture.employeeServiceWorker().save()).data

  const fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )

  assistanceNeedDecision = (
    await Fixture.assistanceNeedDecision().withChild(childId).save()
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
      }/child-information/${childId}/assistance-need-decision/${
        assistanceNeedDecision?.id ?? ''
      }/edit`
    )
    assistanceNeedDecisionEditPage = new AssistanceNeedDecisionEditPage(page)
  })

  test('Some fields are visible', async () => {
    await assistanceNeedDecisionEditPage.assertDeciderSelectVisible()
  })
})
