// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

import '../../jest'

let admin: DevEmployee

beforeEach(async () => {
  await resetServiceState()

  admin = await Fixture.employee().admin().save()
})

describe('Occupancies report', () => {
  test('unit filter', async () => {
    const area = await Fixture.careArea().save()
    const unit1 = await Fixture.daycare({
      areaId: area.id,
      name: 'Yksikkö 1'
    }).save()
    await Fixture.daycareGroup({ daycareId: unit1.id }).save()
    const unit2 = await Fixture.daycare({
      areaId: area.id,
      name: 'Yksikkö 2'
    }).save()
    await Fixture.daycareGroup({ daycareId: unit2.id }).save()
    const unit3 = await Fixture.daycare({
      areaId: area.id,
      name: 'Yksikkö 3'
    }).save()
    await Fixture.daycareGroup({ daycareId: unit3.id }).save()

    const mockedToday = LocalDate.of(2025, 10, 2)
    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(6, 11))
    })
    const report = await navigateToReport(page, admin)
    await report.unitsSelect.fillAndSelectFirst('Yksikkö 1')
    await report.assertReportUnitNameRows(['Yksikkö 1'])
    await report.unitsSelect.fillAndSelectFirst('Yksikkö 3')
    await report.assertReportUnitNameRows(['Yksikkö 1', 'Yksikkö 3'])
    await report.unitsSelect.fillAndSelectFirst('Yksikkö 3')
    await report.assertReportUnitNameRows(['Yksikkö 1'])
    await report.typeCombobox.selectItem('filter-type-GROUPS-CONFIRMED')
    await report.assertReportUnitNameRows(['Yksikkö 1'])
    await report.unitsSelect.fillAndSelectFirst('Yksikkö 2')
    await report.assertReportUnitNameRows(['Yksikkö 1', 'Yksikkö 2'])
  })

  test('confirmed type shows date columns for whole month', async () => {
    const mockedToday = LocalDate.of(2025, 10, 2)
    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(6, 11))
    })
    const report = await navigateToReport(page, admin)
    await report.areaCombobox.fillAndSelectFirst('Kaikki')
    // 'Vahvistettu täyttöaste yksikössä' selected by default
    await report.assertReportDateColumns(
      LocalDate.range(
        LocalDate.of(2025, 10, 1),
        LocalDate.of(2025, 10, 31)
      ).filter((date) => !date.isWeekend())
    )
  })

  test('planned type shows date columns for whole month', async () => {
    const mockedToday = LocalDate.of(2025, 10, 2)
    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(6, 11))
    })
    const report = await navigateToReport(page, admin)
    await report.areaCombobox.fillAndSelectFirst('Kaikki')
    await report.typeCombobox.selectItem('filter-type-UNITS-PLANNED')
    await report.assertReportDateColumns(
      LocalDate.range(
        LocalDate.of(2025, 10, 1),
        LocalDate.of(2025, 10, 31)
      ).filter((date) => !date.isWeekend())
    )
  })

  test('realized type shows date columns only from past', async () => {
    const mockedToday = LocalDate.of(2025, 10, 3)
    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(6, 11))
    })
    const report = await navigateToReport(page, admin)
    await report.areaCombobox.fillAndSelectFirst('Kaikki')
    await report.typeCombobox.selectItem('filter-type-UNITS-REALIZED')
    await report.assertReportDateColumns([
      LocalDate.of(2025, 10, 1),
      LocalDate.of(2025, 10, 2)
    ])
  })

  test('realized type shows no date columns on first day of month', async () => {
    const mockedToday = LocalDate.of(2025, 10, 1)
    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(6, 11))
    })
    const report = await navigateToReport(page, admin)
    await report.areaCombobox.fillAndSelectFirst('Kaikki')
    await report.typeCombobox.selectItem('filter-type-UNITS-REALIZED')
    await report.assertReportDateColumns([])
  })
})

const navigateToReport = async (page: Page, user: DevEmployee) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openOccupanciesReport()
}
