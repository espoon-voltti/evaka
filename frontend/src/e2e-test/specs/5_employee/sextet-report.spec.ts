// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevDaycare, DevEmployee } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

beforeEach(async () => await resetServiceState())

describe('Sextet report', () => {
  const now = HelsinkiDateTime.of(2022, 1, 1, 12, 15, 53)
  let unitMunicipal: DevDaycare
  let unitPurchased: DevDaycare

  beforeEach(async () => {
    const area = await Fixture.careArea().save()
    unitMunicipal = await Fixture.daycare({
      areaId: area.id,
      name: 'kunnallinen',
      providerType: 'MUNICIPAL'
    }).save()
    unitPurchased = await Fixture.daycare({
      areaId: area.id,
      name: 'ostopalvelu',
      providerType: 'PURCHASED'
    }).save()
    const child1 = await Fixture.person({ ssn: null }).saveChild()
    const child2 = await Fixture.person({ ssn: null }).saveChild()

    const startDate = LocalDate.of(2021, 12, 1)
    const endDate = LocalDate.of(2022, 12, 15)
    await Fixture.placement({
      unitId: unitMunicipal.id,
      childId: child1.id,
      startDate,
      endDate
    }).save()
    await Fixture.placement({
      unitId: unitPurchased.id,
      childId: child2.id,
      startDate,
      endDate
    }).save()
  })

  test('unit provider type filter works', async () => {
    const admin = await Fixture.employee().admin().save()
    const report = await openSextetReport(now, admin)

    await report.selectYear(2021)
    await report.selectPlacementType('DAYCARE')
    await report.assertRows([
      `${unitMunicipal.name}\tVarhaiskasvatus\t21`,
      `${unitPurchased.name}\tVarhaiskasvatus\t21`
    ])
    await report.assertSum(42)

    await report.toggleUnitProviderType('MUNICIPAL')
    await report.assertRows([`${unitMunicipal.name}\tVarhaiskasvatus\t21`])
    await report.assertSum(21)

    await report.toggleUnitProviderType('PURCHASED')
    await report.assertRows([
      `${unitMunicipal.name}\tVarhaiskasvatus\t21`,
      `${unitPurchased.name}\tVarhaiskasvatus\t21`
    ])
    await report.assertSum(42)

    await report.toggleUnitProviderType('MUNICIPAL')
    await report.assertRows([`${unitPurchased.name}\tVarhaiskasvatus\t21`])
    await report.assertSum(21)
  })
})

const openSextetReport = async (now: HelsinkiDateTime, user: DevEmployee) => {
  const page = await Page.open({ mockedTime: now })
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openSextetReport()
}
