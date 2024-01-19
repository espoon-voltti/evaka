// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { resetDatabase } from '../../dev-api'
import { Fixture, PersonBuilder } from '../../dev-api/fixtures'
import { EmployeeDetail } from '../../dev-api/types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage, { NonSsnChildrenReport } from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedToday = LocalDate.of(2023, 6, 12)
let child: PersonBuilder
let child2: PersonBuilder
let child3: PersonBuilder
let child4: PersonBuilder

beforeEach(async () => {
  await resetDatabase()
  const area = await Fixture.careArea().save()
  const unit = await Fixture.daycare().with({ areaId: area.data.id }).save()
  child = await Fixture.person()
    .with({
      firstName: 'Esko',
      lastName: 'Beck',
      ssn: undefined,
      ophPersonOid: 'mock-oid-1'
    })
    .save()
  child2 = await Fixture.person()
    .with({ firstName: 'Maija', lastName: 'Äänikolu', ssn: undefined })
    .save()

  child3 = await Fixture.person()
    .with({ firstName: 'Pasi', lastName: 'Pastplacement', ssn: undefined })
    .save()

  child4 = await Fixture.person()
    .with({ firstName: 'Sami', lastName: 'Ssnhaver', ssn: '050520A999M' })
    .save()
  await Fixture.child(child.data.id).save()
  await Fixture.child(child2.data.id).save()
  await Fixture.child(child3.data.id).save()
  await Fixture.child(child4.data.id).save()

  await Fixture.placement()
    .with({
      type: 'DAYCARE',
      childId: child.data.id,
      unitId: unit.data.id,
      startDate: mockedToday,
      endDate: mockedToday.addDays(4)
    })
    .save()
  await Fixture.placement()
    .with({
      type: 'DAYCARE',
      childId: child2.data.id,
      unitId: unit.data.id,
      startDate: mockedToday.addDays(7),
      endDate: mockedToday.addDays(14)
    })
    .save()

  await Fixture.placement()
    .with({
      type: 'DAYCARE',
      childId: child3.data.id,
      unitId: unit.data.id,
      startDate: mockedToday.subMonths(6),
      endDate: mockedToday.subMonths(3)
    })
    .save()

  await Fixture.placement()
    .with({
      type: 'DAYCARE',
      childId: child4.data.id,
      unitId: unit.data.id,
      startDate: mockedToday.subMonths(1),
      endDate: mockedToday.addMonths(1)
    })
    .save()
})

describe('Non SSN children report', () => {
  test('report data is shown to admin', async () => {
    const admin = await Fixture.employeeAdmin().save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0)),
      employeeCustomizations: {
        featureFlags: { personDuplicate: true }
      }
    })

    const report = await navigateToReport(page, admin.data)
    await assertReport(report)
  })

  test('report data is shown to finance admin', async () => {
    const financeAdmin = await Fixture.employeeFinanceAdmin().save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const report = await navigateToReport(page, financeAdmin.data)
    await assertReport(report)
  })

  test('report data is shown to service worker', async () => {
    const serviceWorker = await Fixture.employeeServiceWorker().save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0)),
      employeeCustomizations: {
        featureFlags: { personDuplicate: true }
      }
    })

    await employeeLogin(page, serviceWorker.data)
    const report = await navigateToReport(page, serviceWorker.data)
    await assertReport(report)
  })
})

const assertReport = async (report: NonSsnChildrenReport) => {
  const initialExpectation = [
    {
      childName: `${child.data.lastName} ${child.data.firstName}`,
      dateOfBirth: child.data.dateOfBirth.format(),
      personOid: child.data.ophPersonOid ?? '',
      vardaOid: ''
    },
    {
      childName: `${child2.data.lastName} ${child2.data.firstName}`,
      dateOfBirth: child2.data.dateOfBirth.format(),
      personOid: child2.data.ophPersonOid ?? '',
      vardaOid: ''
    }
  ]

  await report.assertRows(initialExpectation)

  await report.changeSortOrder()

  await report.assertRows(initialExpectation.reverse())
}

const navigateToReport = async (page: Page, user: EmployeeDetail) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openNonSsnChildrenReport()
}
