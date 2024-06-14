// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import config from '../../config'
import {
  DaycareBuilder,
  Fixture,
  PersonBuilder,
  PreschoolTermBuilder
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedToday = LocalDate.of(2023, 12, 13)
let term: PreschoolTermBuilder
let child: PersonBuilder
let unit: DaycareBuilder

beforeEach(async () => {
  await resetServiceState()
  term = await Fixture.preschoolTerm().save()
  const area = await Fixture.careArea().save()
  unit = await Fixture.daycare()
    .with({
      areaId: area.data.id,
      name: 'TestiEO',
      type: ['PRESCHOOL'],
      language: 'fi',
      dailyPreschoolTime: new TimeRange(LocalTime.of(9, 0), LocalTime.of(14, 0))
    })
    .save()
  child = await Fixture.person()
    .with({
      firstName: 'Esko',
      lastName: 'Beck'
    })
    .save()

  await Fixture.child(child.data.id).save()

  await Fixture.placement()
    .with({
      type: 'PRESCHOOL',
      childId: child.data.id,
      unitId: unit.data.id,
      startDate: mockedToday.subDays(4),
      endDate: mockedToday.addDays(4)
    })
    .save()

  await Fixture.absence()
    .with({
      absenceType: 'SICKLEAVE',
      date: mockedToday,
      childId: child.data.id
    })
    .save()
  await Fixture.childAttendance()
    .with({
      childId: child.data.id,
      date: mockedToday.subDays(1),
      arrived: LocalTime.of(8, 0),
      departed: LocalTime.of(12, 30),
      unitId: unit.data.id
    })
    .save()
})

describe('Preschool absence report', () => {
  test('report data is shown', async () => {
    const admin = await Fixture.employeeAdmin().save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0)),
      employeeCustomizations: {
        featureFlags: { personDuplicate: true }
      }
    })

    const report = await navigateToReport(page, admin.data)
    await report.selectUnit(unit.data.name)
    await report.selectTerm(term.data.finnishPreschool.format())
    const initialExpectation = [
      {
        firstName: child.data.firstName,
        lastName: child.data.lastName,
        TOTAL: '6',
        OTHER_ABSENCE: '1',
        SICKLEAVE: '5',
        UNKNOWN_ABSENCE: '0'
      }
    ]

    await report.assertRows(initialExpectation)
  })
})

const navigateToReport = async (page: Page, user: DevEmployee) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openPreschoolAbsenceReport()
}
