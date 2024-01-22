// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { resetDatabase } from '../../dev-api'
import { Fixture } from '../../dev-api/fixtures'
import { EmployeeDetail } from '../../dev-api/types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

beforeEach(resetDatabase)

describe('Placement guarantee report', () => {
  test('child with placement guarantee is in the report', async () => {
    const mockedToday = LocalDate.of(2023, 9, 14)

    const admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare().with({ areaId: area.data.id }).save()
    const child = await Fixture.person().save()
    await Fixture.child(child.data.id).save()
    await Fixture.placement()
      .with({
        childId: child.data.id,
        unitId: unit.data.id,
        startDate: mockedToday.subDays(1),
        endDate: mockedToday.subDays(1),
        placeGuarantee: false
      })
      .save()
    await Fixture.placement()
      .with({
        childId: child.data.id,
        unitId: unit.data.id,
        startDate: mockedToday.addDays(1),
        endDate: mockedToday.addDays(1),
        placeGuarantee: true
      })
      .save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const report = await navigateToReport(page, admin.data)
    await report.assertRows([
      {
        areaName: area.data.name,
        unitName: unit.data.name,
        childName: `${child.data.lastName}, ${child.data.firstName}`,
        placementPeriod: '15.09.2023 - 15.09.2023'
      }
    ])
  })

  test('child without placement guarantee is not in the report', async () => {
    const mockedToday = LocalDate.of(2023, 9, 14)

    const admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare().with({ areaId: area.data.id }).save()
    const child = await Fixture.person().save()
    await Fixture.child(child.data.id).save()
    await Fixture.placement()
      .with({
        childId: child.data.id,
        unitId: unit.data.id,
        startDate: mockedToday.subDays(1),
        endDate: mockedToday.subDays(1),
        placeGuarantee: false
      })
      .save()
    await Fixture.placement()
      .with({
        childId: child.data.id,
        unitId: unit.data.id,
        startDate: mockedToday.addDays(1),
        endDate: mockedToday.addDays(1),
        placeGuarantee: false
      })
      .save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const report = await navigateToReport(page, admin.data)
    await report.assertRows([])
  })

  test('date picker works', async () => {
    const mockedToday = LocalDate.of(2023, 9, 14)

    const admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare().with({ areaId: area.data.id }).save()
    const child1 = await Fixture.person().with({ ssn: undefined }).save()
    await Fixture.child(child1.data.id).save()
    await Fixture.placement()
      .with({
        childId: child1.data.id,
        unitId: unit.data.id,
        startDate: mockedToday.subDays(3),
        endDate: mockedToday.subDays(3),
        placeGuarantee: false
      })
      .save()
    await Fixture.placement()
      .with({
        childId: child1.data.id,
        unitId: unit.data.id,
        startDate: mockedToday.subDays(1),
        endDate: mockedToday.subDays(1),
        placeGuarantee: true
      })
      .save()
    const child2 = await Fixture.person().with({ ssn: undefined }).save()
    await Fixture.child(child2.data.id).save()
    await Fixture.placement()
      .with({
        childId: child2.data.id,
        unitId: unit.data.id,
        startDate: mockedToday.subDays(3),
        endDate: mockedToday.subDays(1),
        placeGuarantee: false
      })
      .save()
    await Fixture.placement()
      .with({
        childId: child2.data.id,
        unitId: unit.data.id,
        startDate: mockedToday.addDays(1),
        endDate: mockedToday.addDays(1),
        placeGuarantee: true
      })
      .save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const report = await navigateToReport(page, admin.data)
    await report.selectDate('12.09.2023')
    await report.assertRows([
      {
        areaName: area.data.name,
        unitName: unit.data.name,
        childName: `${child1.data.lastName}, ${child1.data.firstName}`,
        placementPeriod: '13.09.2023 - 13.09.2023'
      }
    ])
    await report.selectDate('14.09.2023')
    await report.assertRows([
      {
        areaName: area.data.name,
        unitName: unit.data.name,
        childName: `${child2.data.lastName}, ${child2.data.firstName}`,
        placementPeriod: '15.09.2023 - 15.09.2023'
      }
    ])
  })

  test('unit selector works', async () => {
    const mockedToday = LocalDate.of(2023, 9, 14)

    const admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const unit1 = await Fixture.daycare().with({ areaId: area.data.id }).save()
    const unit2 = await Fixture.daycare().with({ areaId: area.data.id }).save()
    const child1 = await Fixture.person().with({ ssn: undefined }).save()
    await Fixture.child(child1.data.id).save()
    await Fixture.placement()
      .with({
        childId: child1.data.id,
        unitId: unit1.data.id,
        startDate: mockedToday.subDays(1),
        endDate: mockedToday.subDays(1),
        placeGuarantee: false
      })
      .save()
    await Fixture.placement()
      .with({
        childId: child1.data.id,
        unitId: unit1.data.id,
        startDate: mockedToday.addDays(1),
        endDate: mockedToday.addDays(1),
        placeGuarantee: true
      })
      .save()
    const child2 = await Fixture.person().with({ ssn: undefined }).save()
    await Fixture.child(child2.data.id).save()
    await Fixture.placement()
      .with({
        childId: child2.data.id,
        unitId: unit2.data.id,
        startDate: mockedToday.subDays(1),
        endDate: mockedToday.subDays(1),
        placeGuarantee: false
      })
      .save()
    await Fixture.placement()
      .with({
        childId: child2.data.id,
        unitId: unit2.data.id,
        startDate: mockedToday.addDays(1),
        endDate: mockedToday.addDays(1),
        placeGuarantee: true
      })
      .save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const report = await navigateToReport(page, admin.data)
    await report.selectUnit(unit1.data.name)
    await report.assertRows([
      {
        areaName: area.data.name,
        unitName: unit1.data.name,
        childName: `${child1.data.lastName}, ${child1.data.firstName}`,
        placementPeriod: '15.09.2023 - 15.09.2023'
      }
    ])
    await report.selectUnit(unit2.data.name)
    await report.assertRows([
      {
        areaName: area.data.name,
        unitName: unit2.data.name,
        childName: `${child2.data.lastName}, ${child2.data.firstName}`,
        placementPeriod: '15.09.2023 - 15.09.2023'
      }
    ])
  })
})

const navigateToReport = async (page: Page, user: EmployeeDetail) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openPlacementGuaranteeReport()
}
