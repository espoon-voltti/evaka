// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import {
  testCareArea,
  testDaycare,
  testChild2,
  testAdult,
  Fixture
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const today = LocalDate.of(2022, 1, 14)
const yesterday = today.subDays(1)
let page: Page

async function openCalendarPage() {
  page = await Page.open({
    mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0)),
    citizenCustomizations: {
      featureFlags: { calendarMonthView: false }
    }
  })
  await enduserLogin(page, testAdult)
  const header = new CitizenHeader(page, 'desktop')
  await header.selectTab('calendar')
  return new CitizenCalendarPage(page, 'desktop')
}

describe('Service time usage', () => {
  beforeEach(async () => {
    await resetServiceState()

    await Fixture.careArea(testCareArea).save()
    await Fixture.daycare(testDaycare).save()
    const child = await Fixture.person(testChild2).saveChild({
      updateMockVtj: true
    })
    const guardian = await Fixture.person(testAdult).saveAdult({
      updateMockVtjWithDependants: [child]
    })
    await Fixture.guardian(child, guardian).save()

    const daycareSupervisor = await Fixture.employee()
      .unitSupervisor(testDaycare.id)
      .save()

    const serviceNeedOption = await Fixture.serviceNeedOption({
      validPlacementType: 'DAYCARE',
      defaultOption: false,
      nameFi: 'Kokopäiväinen',
      nameSv: 'Kokopäiväinen (sv)',
      nameEn: 'Kokopäiväinen (en)',
      daycareHoursPerMonth: 140
    }).save()

    const placement = await Fixture.placement({
      childId: testChild2.id,
      unitId: testDaycare.id,
      type: 'DAYCARE',
      startDate: yesterday,
      endDate: today.addYears(1)
    }).save()
    await Fixture.serviceNeed({
      placementId: placement.id,
      startDate: yesterday,
      endDate: today.addYears(1),
      optionId: serviceNeedOption.id,
      confirmedBy: daycareSupervisor.id
    }).save()
  })

  it('Reservation time shown in monthly summary', async () => {
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      date: yesterday,
      childId: testChild2.id,
      reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
      secondReservation: null
    }).save()

    const calendarPage = await openCalendarPage()
    const summary = await calendarPage.openMonthlySummary(
      today.year,
      today.month
    )
    await summary.title.assertTextEquals('Läsnäolot 01.01. - 31.01.2022')
    await summary.textElement.assertTextEquals(
      'Kaarina\n' + '\n' + 'Suunnitelma 8 h / 140 h\n' + 'Toteuma 8 h / 140 h'
    )
  })

  it('Attendance time shown in monthly summary', async () => {
    await Fixture.childAttendance({
      childId: testChild2.id,
      unitId: testDaycare.id,
      date: yesterday,
      arrived: LocalTime.of(8, 0),
      departed: LocalTime.of(15, 30)
    }).save()

    const calendarPage = await openCalendarPage()
    const summary = await calendarPage.openMonthlySummary(
      today.year,
      today.month
    )
    await summary.title.assertTextEquals('Läsnäolot 01.01. - 31.01.2022')
    await summary.textElement.assertTextEquals(
      'Kaarina\n' +
        '\n' +
        'Suunnitelma - / 140 h\n' +
        'Toteuma 7 h 30 min / 140 h'
    )
  })

  it('Service time usage based on reservation shown in day view', async () => {
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      date: yesterday,
      childId: testChild2.id,
      reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
      secondReservation: null
    }).save()

    const calendarPage = await openCalendarPage()
    const dayView = await calendarPage.openDayView(yesterday)
    await dayView
      .getUsedService(testChild2.id)
      .assertTextEquals('08:00–16:00 (8 h)')
  })

  it('Service time usage based on attendance shown in day view', async () => {
    await Fixture.childAttendance({
      childId: testChild2.id,
      unitId: testDaycare.id,
      date: today,
      arrived: LocalTime.of(8, 0),
      departed: LocalTime.of(15, 30)
    }).save()

    const calendarPage = await openCalendarPage()
    const dayView = await calendarPage.openDayView(today)
    await dayView
      .getUsedService(testChild2.id)
      .assertTextEquals('08:00–15:30 (7 h 30 min)')
    await dayView
      .getServiceUsageWarning(testChild2.id)
      .assertTextEquals('Toteunut läsnäoloaika ylittää ilmoitetun ajan.')
  })

  it('Service time warning when attendance is longer than reservation', async () => {
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      date: today,
      childId: testChild2.id,
      reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(15, 30)),
      secondReservation: null
    }).save()
    await Fixture.childAttendance({
      childId: testChild2.id,
      unitId: testDaycare.id,
      date: today,
      arrived: LocalTime.of(7, 55),
      departed: LocalTime.of(16, 0)
    }).save()

    const calendarPage = await openCalendarPage()
    const dayView = await calendarPage.openDayView(today)
    await dayView
      .getUsedService(testChild2.id)
      .assertTextEquals('07:55–16:00 (8 h 5 min)')
    await dayView
      .getServiceUsageWarning(testChild2.id)
      .assertTextEquals(
        'Saapunut ilmoitettua aikaisemmin. Lähtenyt ilmoitettua myöhemmin.'
      )
  })
})
describe('Service time alert', () => {
  beforeEach(async () => {
    await resetServiceState()

    await Fixture.careArea(testCareArea).save()
    await Fixture.daycare(testDaycare).save()
    const child = await Fixture.person(testChild2).saveChild({
      updateMockVtj: true
    })
    const guardian = await Fixture.person(testAdult).saveAdult({
      updateMockVtjWithDependants: [child]
    })
    await Fixture.guardian(child, guardian).save()

    const daycareSupervisor = await Fixture.employee()
      .unitSupervisor(testDaycare.id)
      .save()

    const serviceNeedOption = await Fixture.serviceNeedOption({
      validPlacementType: 'DAYCARE',
      defaultOption: false,
      nameFi: 'Kokopäiväinen',
      nameSv: 'Kokopäiväinen (sv)',
      nameEn: 'Kokopäiväinen (en)',
      daycareHoursPerMonth: 75
    }).save()

    const placement = await Fixture.placement({
      childId: testChild2.id,
      unitId: testDaycare.id,
      type: 'DAYCARE',
      startDate: LocalDate.of(2022, 1, 1),
      endDate: today.addYears(1)
    }).save()
    await Fixture.serviceNeed({
      placementId: placement.id,
      startDate: LocalDate.of(2022, 1, 1),
      endDate: today.addYears(1),
      optionId: serviceNeedOption.id,
      confirmedBy: daycareSupervisor.id
    }).save()
  })

  it('Service time alert shown in month heading', async () => {
    let i = 1
    while (i < 31) {
      const date = LocalDate.of(2022, 1, i)
      i++

      // skip holidays and weekends
      if (date.isEqual(LocalDate.of(2022, 1, 1))) continue
      if (date.isEqual(LocalDate.of(2022, 1, 6))) continue
      if (date.isWeekend()) continue

      await Fixture.attendanceReservation({
        type: 'RESERVATIONS',
        date: date,
        childId: testChild2.id,
        reservation: new TimeRange(LocalTime.of(12, 0), LocalTime.of(15, 0)),
        secondReservation: null
      }).save()
      await Fixture.childAttendance({
        childId: testChild2.id,
        unitId: testDaycare.id,
        date: date,
        arrived: LocalTime.of(8, 0),
        departed: LocalTime.of(16, 30)
      }).save()
    }

    const calendarPage = await openCalendarPage()
    const summary = await calendarPage.openMonthlySummary(
      today.year,
      today.month
    )
    await summary.title.assertTextEquals('Läsnäolot 01.01. - 31.01.2022')
    await summary.textElement.assertTextEquals(
      'Kaarina\n' +
        '\n' +
        'Suunnitelma 57 h / 75 h\n' +
        'Toteuma 76 h 30 min / 75 h'
    )
  })

  it('Too much reservations show info box initially', async () => {
    let i = 1
    while (i < 15) {
      const date = LocalDate.of(2022, 2, i)
      i++
      if (date.getIsoDayOfWeek() === 6 || date.getIsoDayOfWeek() === 7) continue
      await Fixture.attendanceReservation({
        type: 'RESERVATIONS',
        date: date,
        childId: testChild2.id,
        reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
        secondReservation: null
      }).save()
    }

    const calendarPage = await openCalendarPage()
    // Should be open initially, so we call getMonthlySummary instead of openMonthlySummary
    const summary = calendarPage.getMonthlySummary(today.year, 2)
    await summary.title.assertTextEquals('Läsnäolot 01.02. - 28.02.2022')
    await summary.warningElement.assertTextEquals(
      'Läsnäoloja suunniteltu sopimuksen ylittävä määrä:'
    )
    await summary.textElement.assertTextEquals(
      'Kaarina\n' + '\n' + 'Suunnitelma 80 h / 75 h\n' + 'Toteuma - / 75 h'
    )
  })
})
