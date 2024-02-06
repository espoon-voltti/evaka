// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { resetDatabase } from '../../dev-api'
import {
  careAreaFixture,
  daycareFixture,
  enduserChildFixtureKaarina,
  enduserGuardianFixture,
  Fixture
} from '../../dev-api/fixtures'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const today = LocalDate.of(2022, 1, 14)
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
describe('Monthly summary', () => {
  beforeEach(async () => {
    await resetDatabase()

    await Fixture.careArea().with(careAreaFixture).save()
    await Fixture.daycare().with(daycareFixture).save()
    const guardian = await Fixture.person().with(enduserGuardianFixture).save()
    const child = await Fixture.person().with(enduserChildFixtureKaarina).save()
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
        startDate: today,
        endDate: today.addYears(1)
      })
      .save()
    await Fixture.serviceNeed()
      .with({
        placementId: placement.data.id,
        startDate: today,
        endDate: today.addYears(1),
        optionId: serviceNeedOption.data.id,
        confirmedBy: daycareSupervisor.data.id
      })
      .save()
  })

  it('Reservation time shown in summary', async () => {
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      date: today,
      childId: enduserChildFixtureKaarina.id,
      reservation: {
        start: LocalTime.of(8, 0),
        end: LocalTime.of(16, 0)
      },
      secondReservation: null
    }).save()

    const calendarPage = await openCalendarPage()
    const summary = await calendarPage.openMonthlySummary(
      today.year,
      today.month
    )
    await summary.assertContent(
      'Läsnäolot 01.01. - 31.01.2022',
      'Kaarina\n' + '\n' + 'Suunnitelma 8 h / 140 h\n' + 'Toteuma 0 h / 140 h'
    )
  })

  it('Attendance time shown in summary', async () => {
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
    const summary = await calendarPage.openMonthlySummary(
      today.year,
      today.month
    )
    await summary.assertContent(
      'Läsnäolot 01.01. - 31.01.2022',
      'Kaarina\n' +
        '\n' +
        'Suunnitelma 0 h / 140 h\n' +
        'Toteuma 7 h 30 min / 140 h'
    )
  })
})
