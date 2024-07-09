// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

beforeEach(async (): Promise<void> => resetServiceState())

describe('Placement guarantee report', () => {
  test('child with placement guarantee is in the report', async () => {
    const mockedToday = LocalDate.of(2023, 9, 14)

    const admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare().with({ areaId: area.id }).save()
    const child = await Fixture.person().save()
    await Fixture.child(child.id).save()
    await Fixture.placement()
      .with({
        childId: child.id,
        unitId: unit.id,
        startDate: mockedToday.subDays(1),
        endDate: mockedToday.subDays(1),
        placeGuarantee: false
      })
      .save()
    await Fixture.placement()
      .with({
        childId: child.id,
        unitId: unit.id,
        startDate: mockedToday.addDays(1),
        endDate: mockedToday.addDays(1),
        placeGuarantee: true
      })
      .save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const report = await navigateToReport(page, admin)
    await report.assertRows([
      {
        areaName: area.name,
        unitName: unit.name,
        childName: `${child.lastName}, ${child.firstName}`,
        placementPeriod: '15.09.2023 - 15.09.2023'
      }
    ])
  })

  test('child without placement guarantee is not in the report', async () => {
    const mockedToday = LocalDate.of(2023, 9, 14)

    const admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare().with({ areaId: area.id }).save()
    const child = await Fixture.person().save()
    await Fixture.child(child.id).save()
    await Fixture.placement()
      .with({
        childId: child.id,
        unitId: unit.id,
        startDate: mockedToday.subDays(1),
        endDate: mockedToday.subDays(1),
        placeGuarantee: false
      })
      .save()
    await Fixture.placement()
      .with({
        childId: child.id,
        unitId: unit.id,
        startDate: mockedToday.addDays(1),
        endDate: mockedToday.addDays(1),
        placeGuarantee: false
      })
      .save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const report = await navigateToReport(page, admin)
    await report.assertRows([])
  })

  test('date picker works', async () => {
    const mockedToday = LocalDate.of(2023, 9, 14)

    const admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare().with({ areaId: area.id }).save()
    const child1 = await Fixture.person().with({ ssn: undefined }).save()
    await Fixture.child(child1.id).save()
    await Fixture.placement()
      .with({
        childId: child1.id,
        unitId: unit.id,
        startDate: mockedToday.subDays(3),
        endDate: mockedToday.subDays(3),
        placeGuarantee: false
      })
      .save()
    await Fixture.placement()
      .with({
        childId: child1.id,
        unitId: unit.id,
        startDate: mockedToday.subDays(1),
        endDate: mockedToday.subDays(1),
        placeGuarantee: true
      })
      .save()
    const child2 = await Fixture.person().with({ ssn: undefined }).save()
    await Fixture.child(child2.id).save()
    await Fixture.placement()
      .with({
        childId: child2.id,
        unitId: unit.id,
        startDate: mockedToday.subDays(3),
        endDate: mockedToday.subDays(1),
        placeGuarantee: false
      })
      .save()
    await Fixture.placement()
      .with({
        childId: child2.id,
        unitId: unit.id,
        startDate: mockedToday.addDays(1),
        endDate: mockedToday.addDays(1),
        placeGuarantee: true
      })
      .save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const report = await navigateToReport(page, admin)
    await report.selectDate('12.09.2023')
    await report.assertRows([
      {
        areaName: area.name,
        unitName: unit.name,
        childName: `${child1.lastName}, ${child1.firstName}`,
        placementPeriod: '13.09.2023 - 13.09.2023'
      }
    ])
    await report.selectDate('14.09.2023')
    await report.assertRows([
      {
        areaName: area.name,
        unitName: unit.name,
        childName: `${child2.lastName}, ${child2.firstName}`,
        placementPeriod: '15.09.2023 - 15.09.2023'
      }
    ])
  })

  test('unit selector works', async () => {
    const mockedToday = LocalDate.of(2023, 9, 14)

    const admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const unit1 = await Fixture.daycare().with({ areaId: area.id }).save()
    const unit2 = await Fixture.daycare().with({ areaId: area.id }).save()
    const child1 = await Fixture.person().with({ ssn: undefined }).save()
    await Fixture.child(child1.id).save()
    await Fixture.placement()
      .with({
        childId: child1.id,
        unitId: unit1.id,
        startDate: mockedToday.subDays(1),
        endDate: mockedToday.subDays(1),
        placeGuarantee: false
      })
      .save()
    await Fixture.placement()
      .with({
        childId: child1.id,
        unitId: unit1.id,
        startDate: mockedToday.addDays(1),
        endDate: mockedToday.addDays(1),
        placeGuarantee: true
      })
      .save()
    const child2 = await Fixture.person().with({ ssn: undefined }).save()
    await Fixture.child(child2.id).save()
    await Fixture.placement()
      .with({
        childId: child2.id,
        unitId: unit2.id,
        startDate: mockedToday.subDays(1),
        endDate: mockedToday.subDays(1),
        placeGuarantee: false
      })
      .save()
    await Fixture.placement()
      .with({
        childId: child2.id,
        unitId: unit2.id,
        startDate: mockedToday.addDays(1),
        endDate: mockedToday.addDays(1),
        placeGuarantee: true
      })
      .save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const report = await navigateToReport(page, admin)
    await report.selectUnit(unit1.name)
    await report.assertRows([
      {
        areaName: area.name,
        unitName: unit1.name,
        childName: `${child1.lastName}, ${child1.firstName}`,
        placementPeriod: '15.09.2023 - 15.09.2023'
      }
    ])
    await report.selectUnit(unit2.name)
    await report.assertRows([
      {
        areaName: area.name,
        unitName: unit2.name,
        childName: `${child2.lastName}, ${child2.firstName}`,
        placementPeriod: '15.09.2023 - 15.09.2023'
      }
    ])
  })
})

const navigateToReport = async (page: Page, user: DevEmployee) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openPlacementGuaranteeReport()
}
