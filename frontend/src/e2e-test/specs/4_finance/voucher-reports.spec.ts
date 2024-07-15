// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import assert from 'assert'

import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  testCareArea2,
  testDaycare2,
  testDaycare,
  Fixture,
  voucherValueDecisionsFixture,
  testAdult,
  testChild,
  testChild2,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  createVoucherValueDecisions,
  createVoucherValues,
  resetServiceState
} from '../../generated/api-clients'
import { DevPerson } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage, {
  ServiceVoucherUnitReport,
  VoucherServiceProvidersReport
} from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let report: VoucherServiceProvidersReport
let startDate: LocalDate
let endDate: LocalDate
let child: DevPerson
let otherChild: DevPerson
let guardian: DevPerson

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  const careArea = await Fixture.careArea().with(testCareArea2).save()
  await Fixture.daycare({ ...testDaycare2, areaId: careArea.id }).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  await createDefaultServiceNeedOptions()
  await createVoucherValues()

  startDate = LocalDate.of(2020, 1, 1)
  endDate = LocalDate.of(2020, 12, 31)
  child = testChild2
  otherChild = testChild
  guardian = testAdult

  await createVoucherValueDecisions({
    body: [
      voucherValueDecisionsFixture(
        'e2d75fa4-7359-406b-81b8-1703785ca649',
        guardian.id,
        child.id,
        testDaycare.id,
        null,
        'SENT',
        startDate,
        endDate
      ),
      voucherValueDecisionsFixture(
        'ed462aca-f74e-4384-910f-628823201023',
        guardian.id,
        otherChild.id,
        testDaycare2.id,
        null,
        'SENT',
        startDate,
        endDate
      )
    ]
  })
  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open({ acceptDownloads: true })
  await employeeLogin(page, admin)

  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')

  report = await new ReportsPage(page).openVoucherServiceProvidersReport()
})

describe('Reporting - voucher reports', () => {
  test('voucher service providers are reported correctly, respecting the area filter', async () => {
    await report.selectMonth('Tammikuu')
    await report.selectYear(2020)
    await report.selectArea('Superkeskus')

    await report.assertRowCount(1)
    await report.assertRow(testDaycare.id, '1', '581,00')

    const csvReport = await report.getCsvReport()
    assert(
      csvReport.includes(testDaycare.name),
      `Expected csv report to contain ${testDaycare.name}`
    )
    assert(
      !csvReport.includes(testDaycare2.name),
      `Expected csv report to not contain ${testDaycare2.name}`
    )
  })

  test('voucher service provider unit report', async () => {
    await report.selectMonth('Tammikuu')
    await report.selectYear(2020)
    await report.selectArea('Superkeskus')

    await report.assertRowCount(1)
    await report.openUnitReport(testDaycare.name)

    const unitReport = new ServiceVoucherUnitReport(page)
    await unitReport.assertChildRowCount(1)
    await unitReport.assertChild(
      0,
      `${child.lastName} ${child.firstName}`,
      870,
      289,
      581
    )
  })
})
