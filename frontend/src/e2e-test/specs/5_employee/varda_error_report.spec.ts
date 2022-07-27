// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { UUID } from 'lib-common/types'

import config from '../../config'
import {
  addVardaReset,
  addVardaServiceNeed,
  insertDaycareGroupFixtures,
  insertDefaultServiceNeedOptions,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import type { ServiceNeedBuilder } from '../../dev-api/fixtures'
import { daycareGroupFixture, Fixture } from '../../dev-api/fixtures'
import type { EmployeeDetail } from '../../dev-api/types'
import EmployeeNav from '../../pages/employee/employee-nav'
import type { VardaErrorsReport } from '../../pages/employee/reports'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let admin: EmployeeDetail
let childId: UUID
let serviceNeed: ServiceNeedBuilder

beforeAll(async () => {
  await resetDatabase()

  admin = (await Fixture.employeeAdmin().save()).data

  const fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])
  await insertDefaultServiceNeedOptions()

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const placement = await Fixture.placement()
    .with({
      childId,
      unitId
    })
    .save()

  const serviceNeedOption = await Fixture.serviceNeedOption()
    .with({ validPlacementType: placement.data.type })
    .save()

  serviceNeed = await Fixture.serviceNeed()
    .with({
      placementId: placement.data.id,
      optionId: serviceNeedOption.data.id,
      confirmedBy: admin.id
    })
    .save()
})

describe('Varda error report', () => {
  let vardaErrorsReportPage: VardaErrorsReport

  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, admin)

    const employeeNav = new EmployeeNav(page)
    await page.goto(`${config.employeeUrl}`)
    await employeeNav.reportsTab.click()
    vardaErrorsReportPage = await new ReportsPage(page).openVardaErrorsReport()
  })

  test('Can mark child for reset', async () => {
    await vardaErrorsReportPage.assertErrorRowCount(0)

    await addVardaReset({
      evakaChildId: childId,
      resetTimestamp: new Date()
    })

    await addVardaServiceNeed({
      evakaServiceNeedId: serviceNeed.data.id,
      evakaChildId: childId,
      evakaServiceNeedUpdated: new Date(),
      updateFailed: true,
      errors: ['test error']
    })

    await page.reload()

    await vardaErrorsReportPage.assertErrorRowCount(1)
    await vardaErrorsReportPage.assertErrorsContains(childId, 'test error')
    await vardaErrorsReportPage.resetChild(childId)
    await page.reload()
    await vardaErrorsReportPage.assertErrorRowCount(0)
  })
})
