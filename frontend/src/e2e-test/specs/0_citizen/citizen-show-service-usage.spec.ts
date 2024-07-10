// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import {
  careAreaFixture,
  daycareFixture,
  enduserChildFixtureKaarina,
  enduserGuardianFixture,
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
    mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  await enduserLogin(page)
  const header = new CitizenHeader(page, 'desktop')
  await header.selectTab('calendar')
  return new CitizenCalendarPage(page, 'desktop')
}

describe('Service time usage', () => {
  beforeEach(async () => {
    await resetServiceState()

    await Fixture.careArea().with(careAreaFixture).save()
    await Fixture.daycare().with(daycareFixture).save()
    const child = await Fixture.person()
      .with(enduserChildFixtureKaarina)
      .saveChild({ updateMockVtj: true })
    const guardian = await Fixture.person()
      .with(enduserGuardianFixture)
      .saveAdult({ updateMockVtjWithDependants: [child] })
    await Fixture.child(enduserChildFixtureKaarina.id).save()
    await Fixture.guardian(child, guardian).save()

    const daycareSupervisor = await Fixture.employeeUnitSupervisor(
      daycareFixture.id
    ).save()

    const serviceNeedOption = await Fixture.serviceNeedOption()
      .with({
        validPlacementType: 'DAYCARE',
        defaultOption: false,
        nameFi: 'Kokopäiväinen',
        nameSv: 'Kokopäiväinen (sv)',
        nameEn: 'Kokopäiväinen (en)',
        daycareHoursPerMonth: 140
      })
      .save()

    const placement = await Fixture.placement()
      .with({
        childId: enduserChildFixtureKaarina.id,
        unitId: daycareFixture.id,
        type: 'DAYCARE',
        startDate: yesterday,
        endDate: today.addYears(1)
      })
      .save()
    await Fixture.serviceNeed()
      .with({
        placementId: placement.id,
        startDate: yesterday,
        endDate: today.addYears(1),
        optionId: serviceNeedOption.id,
        confirmedBy: daycareSupervisor.id
      })
      .save()
  })

  it('Reservation time shown in monthly summary', async () => {
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      date: yesterday,
      childId: enduserChildFixtureKaarina.id,
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
    await Fixture.childAttendance()
      .with({
        childId: enduserChildFixtureKaarina.id,
        unitId: daycareFixture.id,
        date: yesterday,
        arrived: LocalTime.of(8, 0),
        departed: LocalTime.of(15, 30)
      })
      .save()

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
      childId: enduserChildFixtureKaarina.id,
      reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
      secondReservation: null
    }).save()

    const calendarPage = await openCalendarPage()
    const dayView = await calendarPage.openDayView(yesterday)
    await dayView
      .getUsedService(enduserChildFixtureKaarina.id)
      .assertTextEquals('08:00–16:00 (8 h)')
  })

  it('Service time usage based on attendance shown in day view', async () => {
    await Fixture.childAttendance()
      .with({
        childId: enduserChildFixtureKaarina.id,
        unitId: daycareFixture.id,
        date: today,
        arrived: LocalTime.of(8, 0),
        departed: LocalTime.of(15, 30)
      })
      .save()

    const calendarPage = await openCalendarPage()
    const dayView = await calendarPage.openDayView(today)
    await dayView
      .getUsedService(enduserChildFixtureKaarina.id)
      .assertTextEquals('08:00–15:30 (7 h 30 min)')
    await dayView
      .getServiceUsageWarning(enduserChildFixtureKaarina.id)
      .assertTextEquals('Toteunut läsnäoloaika ylittää ilmoitetun ajan.')
  })

  it('Service time warning when attendance is longer than reservation', async () => {
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      date: today,
      childId: enduserChildFixtureKaarina.id,
      reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(15, 30)),
      secondReservation: null
    }).save()
    await Fixture.childAttendance()
      .with({
        childId: enduserChildFixtureKaarina.id,
        unitId: daycareFixture.id,
        date: today,
        arrived: LocalTime.of(7, 55),
        departed: LocalTime.of(16, 0)
      })
      .save()

    const calendarPage = await openCalendarPage()
    const dayView = await calendarPage.openDayView(today)
    await dayView
      .getUsedService(enduserChildFixtureKaarina.id)
      .assertTextEquals('07:55–16:00 (8 h 5 min)')
    await dayView
      .getServiceUsageWarning(enduserChildFixtureKaarina.id)
      .assertTextEquals(
        'Saapunut ilmoitettua aikaisemmin. Lähtenyt ilmoitettua myöhemmin.'
      )
  })
})
describe('Service time alert', () => {
  beforeEach(async () => {
    await resetServiceState()

    await Fixture.careArea().with(careAreaFixture).save()
    await Fixture.daycare().with(daycareFixture).save()
    const child = await Fixture.person()
      .with(enduserChildFixtureKaarina)
      .saveChild({ updateMockVtj: true })
    const guardian = await Fixture.person()
      .with(enduserGuardianFixture)
      .saveAdult({ updateMockVtjWithDependants: [child] })
    await Fixture.child(enduserChildFixtureKaarina.id).save()
    await Fixture.guardian(child, guardian).save()

    const daycareSupervisor = await Fixture.employeeUnitSupervisor(
      daycareFixture.id
    ).save()

    const serviceNeedOption = await Fixture.serviceNeedOption()
      .with({
        validPlacementType: 'DAYCARE',
        defaultOption: false,
        nameFi: 'Kokopäiväinen',
        nameSv: 'Kokopäiväinen (sv)',
        nameEn: 'Kokopäiväinen (en)',
        daycareHoursPerMonth: 75
      })
      .save()

    const placement = await Fixture.placement()
      .with({
        childId: enduserChildFixtureKaarina.id,
        unitId: daycareFixture.id,
        type: 'DAYCARE',
        startDate: LocalDate.of(2022, 1, 1),
        endDate: today.addYears(1)
      })
      .save()
    await Fixture.serviceNeed()
      .with({
        placementId: placement.id,
        startDate: LocalDate.of(2022, 1, 1),
        endDate: today.addYears(1),
        optionId: serviceNeedOption.id,
        confirmedBy: daycareSupervisor.id
      })
      .save()
  })

  it('Service time alert shown in month heading', async () => {
    let i = 1
    while (i < 31) {
      const date = LocalDate.of(2022, 1, i)
      i++
      if (date.getIsoDayOfWeek() === 6 || date.getIsoDayOfWeek() === 7) continue
      await Fixture.attendanceReservation({
        type: 'RESERVATIONS',
        date: date,
        childId: enduserChildFixtureKaarina.id,
        reservation: new TimeRange(LocalTime.of(12, 0), LocalTime.of(15, 0)),
        secondReservation: null
      }).save()
      await Fixture.childAttendance()
        .with({
          childId: enduserChildFixtureKaarina.id,
          unitId: daycareFixture.id,
          date: date,
          arrived: LocalTime.of(8, 0),
          departed: LocalTime.of(15, 32)
        })
        .save()
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
        'Suunnitelma 60 h / 75 h\n' +
        'Toteuma 75 h 20 min / 75 h'
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
        childId: enduserChildFixtureKaarina.id,
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
