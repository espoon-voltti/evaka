// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import {
  testDaycareGroup,
  Fixture,
  familyWithTwoGuardians,
  testDaycare,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDefaultServiceNeedOptions,
  createVardaReset,
  createVardaServiceNeed,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee, DevServiceNeed } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage, { VardaErrorsReport } from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let admin: DevEmployee
let childId: UUID
let serviceNeed: DevServiceNeed

beforeAll(async () => {
  await resetServiceState()

  admin = await Fixture.employeeAdmin().save()

  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare().with(testDaycare).save()
  await Fixture.family(familyWithTwoGuardians).save()
  await createDaycareGroups({ body: [testDaycareGroup] })
  await createDefaultServiceNeedOptions()

  const unitId = testDaycare.id
  childId = familyWithTwoGuardians.children[0].id

  const placement = await Fixture.placement()
    .with({
      childId,
      unitId
    })
    .save()

  const serviceNeedOption = await Fixture.serviceNeedOption()
    .with({ validPlacementType: placement.type })
    .save()

  serviceNeed = await Fixture.serviceNeed()
    .with({
      placementId: placement.id,
      optionId: serviceNeedOption.id,
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

  test('Varda errors are shown and children successfully reset', async () => {
    await vardaErrorsReportPage.assertErrorRowCount(0)

    await createVardaReset({
      body: {
        evakaChildId: childId,
        resetTimestamp: HelsinkiDateTime.now()
      }
    })

    await createVardaServiceNeed({
      body: {
        evakaServiceNeedId: serviceNeed.id,
        evakaChildId: childId,
        evakaServiceNeedUpdated: HelsinkiDateTime.now(),
        updateFailed: true,
        errors: ['test error']
      }
    })

    await page.reload()

    await vardaErrorsReportPage.assertErrorRowCount(1)
    await vardaErrorsReportPage.assertErrorsContains(childId, 'test error')
    await vardaErrorsReportPage.resetChild(childId)
    await runPendingAsyncJobs(HelsinkiDateTime.now())
    await page.reload()
    await vardaErrorsReportPage.assertErrorRowCount(0)
  })
})
