// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import {
  DevDaycare,
  DevDaycareGroup,
  DevEmployee,
  DevPerson
} from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedToday = LocalDate.of(2024, 9, 9)
let period: HolidayPeriod
let child: DevPerson
let child2: DevPerson
let unit: DevDaycare
let group: DevDaycareGroup
const dailyTime = new TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0))

beforeEach(async () => {
  await resetServiceState()
  await createDefaultServiceNeedOptions()
  period = await Fixture.holidayPeriod({
    period: new FiniteDateRange(mockedToday, mockedToday.addDays(6)),
    reservationsOpenOn: mockedToday.subWeeks(4),
    reservationDeadline: mockedToday.addDays(1)
  }).save()

  const area = await Fixture.careArea().save()
  unit = await Fixture.daycare({
    areaId: area.id,
    name: 'Testiyksikkö',
    type: ['CENTRE'],
    language: 'fi',
    operationTimes: [
      dailyTime,
      dailyTime,
      dailyTime,
      dailyTime,
      dailyTime,
      null,
      null
    ],
    enabledPilotFeatures: ['MESSAGING', 'MOBILE', 'RESERVATIONS']
  }).save()

  group = await Fixture.daycareGroup({
    daycareId: unit.id,
    name: 'Testgroup'
  }).save()

  child = await Fixture.person({
    firstName: 'Lasse',
    lastName: 'Lomailija',
    dateOfBirth: mockedToday.subDays(2).subYears(2)
  }).saveChild()

  child2 = await Fixture.person({
    firstName: 'Riku',
    lastName: 'Ryhmätön',
    ssn: null,
    dateOfBirth: mockedToday.subDays(2).subYears(4)
  }).saveChild()

  const placement = await Fixture.placement({
    type: 'DAYCARE',
    childId: child.id,
    unitId: unit.id,
    startDate: period.reservationsOpenOn,
    endDate: period.period.end
  }).save()

  await Fixture.placement({
    type: 'DAYCARE',
    childId: child2.id,
    unitId: unit.id,
    startDate: period.reservationsOpenOn,
    endDate: period.period.end
  }).save()

  await Fixture.groupPlacement({
    daycarePlacementId: placement.id,
    startDate: placement.startDate,
    endDate: placement.endDate,
    daycareGroupId: group.id
  }).save()

  await Fixture.assistanceFactor({
    childId: child.id,
    capacityFactor: 2.5,
    validDuring: period.period
  }).save()

  await Fixture.daycareAssistance({
    childId: child.id,
    validDuring: period.period
  }).save()

  //Lasse
  await Fixture.absence({
    absenceType: 'OTHER_ABSENCE',
    absenceCategory: 'BILLABLE',
    date: mockedToday,
    childId: child.id
  }).save()
  await Fixture.absence({
    absenceType: 'OTHER_ABSENCE',
    absenceCategory: 'BILLABLE',
    date: mockedToday.addDays(4),
    childId: child.id
  }).save()
  await Fixture.attendanceReservationRaw({
    childId: child.id,
    date: mockedToday.addDays(1),
    range: null
  }).save()
  await Fixture.attendanceReservationRaw({
    childId: child.id,
    date: mockedToday.addDays(3),
    range: null
  }).save()

  //Riku
  await Fixture.attendanceReservationRaw({
    childId: child2.id,
    date: mockedToday.addDays(1),
    range: null
  }).save()
  await Fixture.attendanceReservationRaw({
    childId: child2.id,
    date: mockedToday.addDays(2),
    range: null
  }).save()
  await Fixture.absence({
    absenceType: 'OTHER_ABSENCE',
    absenceCategory: 'BILLABLE',
    date: mockedToday,
    childId: child2.id
  }).save()
})

describe('Holiday period attendance report', () => {
  test('correct report data is shown for full unit', async () => {
    const admin = await Fixture.employee().admin().save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const report = await navigateToReport(page, admin)
    await report.selectUnit(unit.name)
    await report.selectPeriod(period.period.format())
    await report.sendButton.click()

    const childName = `${child.lastName} ${child.firstName.split(' ')[0]}`
    const childName2 = `${child2.lastName} ${child2.firstName.split(' ')[0]}`
    const initialExpectation = [
      {
        date: 'Ma 09.09.2024',
        presentChildren: [],
        assistanceChildren: [],
        coefficientSum: '0,00',
        staffCount: '0',
        absenceCount: '2',
        noResponseChildren: []
      },
      {
        date: 'Ti 10.09.2024',
        presentChildren: [childName, childName2],
        assistanceChildren: [childName],
        coefficientSum: '5,38',
        staffCount: '1',
        absenceCount: '0',
        noResponseChildren: []
      },
      {
        date: 'Ke 11.09.2024',
        presentChildren: [childName2],
        assistanceChildren: [],
        coefficientSum: '1,00',
        staffCount: '1',
        absenceCount: '0',
        noResponseChildren: [childName]
      },
      {
        date: 'To 12.09.2024',
        presentChildren: [childName],
        assistanceChildren: [childName],
        coefficientSum: '4,38',
        staffCount: '1',
        absenceCount: '0',
        noResponseChildren: [childName2]
      },
      {
        date: 'Pe 13.09.2024',
        presentChildren: [],
        assistanceChildren: [],
        coefficientSum: '0,00',
        staffCount: '0',
        absenceCount: '1',
        noResponseChildren: [childName2]
      }
    ]

    await report.assertRows(initialExpectation)
  })

  test('correct report data is shown for selected group', async () => {
    const admin = await Fixture.employee().admin().save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const report = await navigateToReport(page, admin)
    await report.selectUnit(unit.name)
    await report.selectPeriod(period.period.format())
    await report.groupSelector.fillAndSelectFirst(group.name)
    await report.sendButton.click()

    const childName = `${child.lastName} ${child.firstName.split(' ')[0]}`
    const initialExpectation = [
      {
        date: 'Ma 09.09.2024',
        presentChildren: [],
        assistanceChildren: [],
        coefficientSum: '0,00',
        staffCount: '0',
        absenceCount: '1',
        noResponseChildren: []
      },
      {
        date: 'Ti 10.09.2024',
        presentChildren: [childName],
        assistanceChildren: [childName],
        coefficientSum: '4,38',
        staffCount: '1',
        absenceCount: '0',
        noResponseChildren: []
      },
      {
        date: 'Ke 11.09.2024',
        presentChildren: [],
        assistanceChildren: [],
        coefficientSum: '0,00',
        staffCount: '0',
        absenceCount: '0',
        noResponseChildren: [childName]
      },
      {
        date: 'To 12.09.2024',
        presentChildren: [childName],
        assistanceChildren: [childName],
        coefficientSum: '4,38',
        staffCount: '1',
        absenceCount: '0',
        noResponseChildren: []
      },
      {
        date: 'Pe 13.09.2024',
        presentChildren: [],
        assistanceChildren: [],
        coefficientSum: '0,00',
        staffCount: '0',
        absenceCount: '1',
        noResponseChildren: []
      }
    ]

    await report.assertRows(initialExpectation)
  })
})

const navigateToReport = async (page: Page, user: DevEmployee) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return new ReportsPage(page).openHolidayPeriodAttendanceReport()
}
